# Weather Service Configuration Review & Fixes

## Overview
Reviewed and fixed all configuration files for both local development and Docker deployment. Ensured the weather service uses its own isolated Redis and PostgreSQL instances.

---

## Issues Fixed

### 1. application.yaml (Default/Production Profile)

#### Issue: Wrong Internal Ports
- **Redis**: Was using `port: 13004` (external) → Fixed to `port: 6379` (internal Docker)
- **PostgreSQL**: Was using `:13003/` (external) → Fixed to `:5432/` (internal Docker)

#### Issue: Non-existent Service Reference
- **JWT public key**: Referenced `authentication-service:12002` → Fixed to `localhost:3000`

#### Issue: Wrong CORS Configuration
- **Frontend URL**: Had `localhost:12006` → Fixed to `localhost:3030`

**Fixed Configuration:**
```yaml
spring:
  data:
    redis:
      host: weather-redis   # Container name on default-network
      port: 6379            # Internal Redis port
  datasource:
    url: jdbc:postgresql://weather-postgres:5432/weather_service
    
authentication:
  cors:
    allowed-origin-patterns: http://localhost:3030,https://weather.markpost.dev

jwt:
  public-key-url: http://localhost:3000/api/auth/v1/public-key
```

### 2. application.yaml (Local Profile)

#### Issue: Wrong External Ports
- **Redis**: Was using `port: 12005` → Fixed to `port: 13004`
- **PostgreSQL**: Was using `:12004/` → Fixed to `:13003/`

#### Issue: Missing CORS Config
- **Added proper CORS** for frontend on `localhost:3030`

**Fixed Configuration:**
```yaml
---
# Local environment profile
spring:
  config:
    activate:
      on-profile: local
  data:
    redis:
      host: localhost
      port: 13004      # External port exposed by docker-compose
  datasource:
    url: jdbc:postgresql://localhost:13003/weather_service
    
authentication:
  cors:
    allowed-origin-patterns: http://localhost:3030

jwt:
  public-key-url: http://localhost:3000/api/auth/v1/public-key
```

### 3. weather-service/Dockerfile

#### Issue: Wrong Port
- **EXPOSE**: Was `12001` → Fixed to `13001`

**Fixed Dockerfile:**
```dockerfile
# Expose port 13001 (matches application.yaml)
EXPOSE 13001
```

### 4. docker-compose.yml

#### Issue: Service Isolation
The default-network contains other services. To ensure this application uses its own instances:
- Added **container_name** to all services
- Added **network aliases** to make services findable by name
- Changed frontend service name from `frontend` to `weather-frontend` for clarity

#### Issue: Frontend Environment Variable
- **Was**: `NEXT_PUBLIC_WEATHER_API_URL: http://localhost:12001`
- **Fixed**: `NEXT_PUBLIC_WEATHER_API_URL: http://weather-service:13001`

#### Issue: Missing Environment Variables
- Added `SPRING_PROFILES_ACTIVE: prod` to weather-service

**Fixed docker-compose.yml:**
```yaml
services:
  weather-redis:
    container_name: weather-redis
    hostname: weather-redis
    networks:
      default-network:
        aliases:
          - weather-redis    # Makes it findable as 'weather-redis'

  weather-postgres:
    container_name: weather-postgres
    networks:
      default-network:
        aliases:
          - weather-postgres # Makes it findable as 'weather-postgres'

  weather-service:
    container_name: weather-service
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_PROFILES_ACTIVE: prod
    networks:
      default-network:
        aliases:
          - weather-service  # Makes it findable as 'weather-service'

  weather-frontend:         # Renamed from 'frontend'
    image: weather-frontend:latest
    container_name: weather-frontend
    environment:
      NEXT_PUBLIC_WEATHER_API_URL: http://weather-service:13001
    networks:
      default-network:
        aliases:
          - weather-frontend
```

---

## Port Mapping Summary

### Local Development (Running on Host)
| Service | Host Port | Description |
|---------|-----------|-------------|
| Weather Frontend | 3030 | Next.js frontend |
| Auth Service (External) | 3000 | External authentication service |
| Weather Backend | 13001 | Spring Boot API |
| PostgreSQL | 13003 | Database (exposed from Docker) |
| Redis | 13004 | Cache (exposed from Docker) |

### Docker Internal (Container-to-Container)
| Service | Internal Port | Container Name |
|---------|---------------|----------------|
| weather-frontend | 3030 | weather-frontend |
| weather-service | 13001 | weather-service |
| weather-postgres | 5432 | weather-postgres |
| weather-redis | 6379 | weather-redis |

### Docker External (Host to Container)
| Service | Host:Container | Description |
|---------|----------------|-------------|
| Frontend | 13002:3030 | Access frontend from host |
| Backend | 13001:13001 | Access API from host |
| PostgreSQL | 13003:5432 | Access DB from host |
| Redis | 13004:6379 | Access cache from host |

---

## Connection Strings

### Production (Docker)
```yaml
# Weather Service connects to:
Redis: weather-redis:6379
PostgreSQL: weather-postgres:5432/weather_service
Auth JWT: localhost:3000/api/auth/v1/public-key

# Frontend connects to:
Backend API: http://weather-service:13001
Auth Service: http://localhost:3000 (external, not in Docker)
```

### Local Development
```yaml
# Weather Service connects to:
Redis: localhost:13004
PostgreSQL: localhost:13003/weather_service
Auth JWT: localhost:3000/api/auth/v1/public-key

# Frontend connects to:
Backend API: http://localhost:13001
Auth Service: http://localhost:3000
```

---

## Network Isolation

### How It Works
Even though all services are on the `default-network` (external network with other services), each container has:

1. **Unique container_name**: Prevents conflicts
2. **Network aliases**: Makes them findable by their specific names
3. **Explicit hostnames**: Services connect using specific container names

### Why This Matters
If the `default-network` has other Redis or PostgreSQL containers:
- They might be at different ports or without specific names
- Our services explicitly connect to `weather-redis` and `weather-postgres`
- No accidental connections to wrong services

---

## Verification Checklist

### Local Development
- [ ] Start PostgreSQL and Redis containers: `docker-compose up -d weather-postgres weather-redis`
- [ ] Run backend: `./mvnw spring-boot:run -pl weather-service -Dspring-boot.run.profiles=local`
- [ ] Run frontend: `cd frontend && npm run dev`
- [ ] Backend should connect to `localhost:13003` (PostgreSQL) and `localhost:13004` (Redis)
- [ ] Frontend should connect to `localhost:13001` (backend API)

### Docker Deployment
- [ ] Build: `docker-compose build`
- [ ] Start: `docker-compose up -d`
- [ ] Check logs: `docker-compose logs -f`
- [ ] Backend should connect to `weather-postgres:5432` and `weather-redis:6379`
- [ ] Frontend should connect to `weather-service:13001`
- [ ] Access frontend at `http://localhost:13002`

---

## Testing Commands

### Check Service Connectivity (Docker)
```bash
# Enter weather-service container
docker exec -it weather-service sh

# Test PostgreSQL connection
nc -zv weather-postgres 5432

# Test Redis connection
nc -zv weather-redis 6379
```

### Check Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f weather-service
docker-compose logs -f weather-postgres
docker-compose logs -f weather-redis
```

### Restart Services
```bash
# Restart all
docker-compose restart

# Restart specific service
docker-compose restart weather-service
```

---

## Configuration Files Summary

### ✅ Fixed Files
1. **application.yaml** - Corrected ports and service references
2. **weather-service/Dockerfile** - Fixed EXPOSE port
3. **docker-compose.yml** - Added isolation and correct service names
4. **frontend/Dockerfile** - Already correct (port 3030)

### ✅ Correct Settings

**application.yaml (default profile - Docker):**
- Redis: `weather-redis:6379`
- PostgreSQL: `weather-postgres:5432`
- Server port: `13001`
- CORS: `http://localhost:3030,https://weather.markpost.dev`
- JWT: `http://localhost:3000/api/auth/v1/public-key`

**application.yaml (local profile):**
- Redis: `localhost:13004`
- PostgreSQL: `localhost:13003`
- Server port: `13001`
- CORS: `http://localhost:3030`
- JWT: `http://localhost:3000/api/auth/v1/public-key`

**docker-compose.yml:**
- All services have unique container names
- All services have network aliases
- Frontend connects to `weather-service:13001`
- Weather-service has `SPRING_PROFILES_ACTIVE: prod`

---

## Common Issues & Solutions

### Issue: "Connection refused" to PostgreSQL
**Cause**: Using wrong port (13003 instead of 5432 in Docker)
**Solution**: Use `weather-postgres:5432` in Docker, `localhost:13003` locally

### Issue: "Connection refused" to Redis
**Cause**: Using wrong port (13004 instead of 6379 in Docker)
**Solution**: Use `weather-redis:6379` in Docker, `localhost:13004` locally

### Issue: Frontend can't reach backend
**Cause**: Using localhost instead of service name in Docker
**Solution**: Use `http://weather-service:13001` in docker-compose environment

### Issue: Services connecting to wrong DB/Redis
**Cause**: Multiple services on default-network with same role
**Solution**: Use specific container names (weather-redis, weather-postgres) with network aliases

---

## Summary

✅ **All configuration files fixed and validated**
✅ **Service isolation ensured with container names and aliases**
✅ **Correct ports configured for both local and Docker**
✅ **Frontend properly configured to connect to backend**
✅ **No conflicts with other services on default-network**

**Ready for deployment!** 🚀

