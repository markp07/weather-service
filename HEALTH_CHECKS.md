# Docker Compose Health Checks - Summary

## Overview
Added comprehensive health checks to all services in docker-compose.yml to ensure proper startup ordering and service health monitoring.

---

## Health Checks Added

### 1. **weather-redis**
```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 5s
```
- **Test**: Uses `redis-cli ping` to verify Redis is responding
- **Interval**: Checks every 10 seconds
- **Timeout**: Waits 5 seconds for response
- **Retries**: Allows 5 failed attempts before marking unhealthy
- **Start Period**: Gives 5 seconds grace period on startup

---

### 2. **weather-postgres**
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U weather_service -d weather_service"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 10s
```
- **Test**: Uses `pg_isready` to verify PostgreSQL is accepting connections
- **Interval**: Checks every 10 seconds
- **Timeout**: Waits 5 seconds for response
- **Retries**: Allows 5 failed attempts before marking unhealthy
- **Start Period**: Gives 10 seconds grace period on startup

---

### 3. **weather-service**
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:13001/api/weather/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```
- **Test**: Checks Spring Boot actuator health endpoint
- **Interval**: Checks every 30 seconds
- **Timeout**: Waits 10 seconds for response
- **Retries**: Allows 3 failed attempts before marking unhealthy
- **Start Period**: Gives 40 seconds grace period for Spring Boot startup

**Updated depends_on:**
```yaml
depends_on:
  weather-redis:
    condition: service_healthy
  weather-postgres:
    condition: service_healthy
```
- Waits for Redis and PostgreSQL to be **healthy** before starting
- Prevents database connection errors on startup

---

### 4. **weather-frontend**
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3030"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 30s
```
- **Test**: Checks if Next.js is responding on port 3030
- **Interval**: Checks every 30 seconds
- **Timeout**: Waits 10 seconds for response
- **Retries**: Allows 3 failed attempts before marking unhealthy
- **Start Period**: Gives 30 seconds grace period for Next.js startup

**Updated depends_on:**
```yaml
depends_on:
  weather-service:
    condition: service_healthy
```
- Waits for weather-service to be **healthy** before starting
- Ensures backend API is available before frontend starts

---

## Startup Order

With health checks, services start in this order:

```
1. weather-redis        (starts immediately)
   └─ Health check: redis-cli ping
   
2. weather-postgres     (starts immediately)
   └─ Health check: pg_isready
   
3. weather-service      (waits for redis + postgres to be healthy)
   └─ Health check: actuator/health endpoint
   
4. weather-frontend     (waits for weather-service to be healthy)
   └─ Health check: Next.js homepage
```

---

## Benefits

### 1. **Prevents Startup Errors**
- Backend won't start until database is ready
- Frontend won't start until backend is ready
- No more "connection refused" errors on startup

### 2. **Service Monitoring**
- Docker knows if a service is unhealthy
- Can automatically restart unhealthy containers
- Better observability with `docker ps` showing health status

### 3. **Faster Debugging**
```bash
# Check health status
docker ps

# See which service is unhealthy
docker-compose ps

# View health check logs
docker inspect weather-service | grep -A 10 Health
```

### 4. **Production Ready**
- Proper health checks for orchestration systems
- Works with Docker Swarm, Kubernetes, etc.
- Load balancers can use health checks

---

## Testing Health Checks

### View Health Status
```bash
# Check all services
docker-compose ps

# Expected output:
# NAME                 STATUS
# weather-redis        Up (healthy)
# weather-postgres     Up (healthy)
# weather-service      Up (healthy)
# weather-frontend     Up (healthy)
```

### Check Specific Service Health
```bash
# Redis
docker exec weather-redis redis-cli ping
# Expected: PONG

# PostgreSQL
docker exec weather-postgres pg_isready -U weather_service -d weather_service
# Expected: ... accepting connections

# Backend
curl http://localhost:13001/api/weather/actuator/health
# Expected: {"status":"UP"}

# Frontend
curl -I http://localhost:13002
# Expected: HTTP/1.1 200 OK
```

### View Health Check Logs
```bash
# Inspect container health
docker inspect weather-service --format='{{json .State.Health}}' | jq

# Watch health checks in real-time
docker events --filter 'event=health_status'
```

---

## Startup Time Expectations

| Service | Start Period | Typical Startup |
|---------|--------------|-----------------|
| weather-redis | 5s | ~2-3 seconds |
| weather-postgres | 10s | ~5-8 seconds |
| weather-service | 40s | ~20-30 seconds |
| weather-frontend | 30s | ~15-25 seconds |

**Total startup time**: ~60-90 seconds for all services to be healthy

---

## Troubleshooting

### Service Stuck in "starting" State
```bash
# Check logs
docker-compose logs -f <service-name>

# Check health check command manually
docker exec <container-name> <health-check-command>
```

### Health Check Failing
```bash
# For weather-service
docker exec weather-service wget --spider http://localhost:13001/api/weather/actuator/health

# For weather-postgres
docker exec weather-postgres pg_isready -U weather_service -d weather_service

# For weather-redis
docker exec weather-redis redis-cli ping
```

### Increase Start Period if Needed
If services are timing out, increase the `start_period`:
```yaml
healthcheck:
  start_period: 60s  # Increase if service takes longer to start
```

---

## Docker Compose Commands

### Start with Health Checks
```bash
# Start all services
docker-compose up -d

# Watch services become healthy
watch docker-compose ps

# Wait for all services to be healthy
docker-compose up -d --wait
```

### Force Restart Unhealthy Service
```bash
# Restart specific service
docker-compose restart weather-service

# Restart all services
docker-compose restart
```

### View Health Status
```bash
# Quick status
docker-compose ps

# Detailed status
docker ps --format "table {{.Names}}\t{{.Status}}"
```

---

## Summary

✅ **All 4 services now have health checks**
- weather-redis: `redis-cli ping`
- weather-postgres: `pg_isready`
- weather-service: Spring Boot actuator
- weather-frontend: Next.js homepage

✅ **Proper dependency ordering**
- Backend waits for database and cache
- Frontend waits for backend

✅ **Production-ready configuration**
- Appropriate timeouts and retries
- Grace periods for startup
- Works with orchestration systems

**Your services will now start in the correct order and Docker will monitor their health!** 🎉

