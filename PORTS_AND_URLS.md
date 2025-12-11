# Weather Service - URLs and Ports Reference

## Service URLs

### Development (Local)

| Service | URL | Port | Description |
|---------|-----|------|-------------|
| Weather Frontend | http://localhost:3030 | 3030 | Weather service UI |
| Auth Service | http://localhost:3000 | 3000 | Authentication service (external) |
| Weather Backend | http://localhost:12001 | 12001 | Weather API backend |
| PostgreSQL | localhost:13003 | 13003 | Weather database |
| Redis | localhost:13004 | 13004 | Weather cache |

### Production

| Service | URL | Description |
|---------|-----|-------------|
| Weather Frontend | https://weather.markpost.dev | Weather service UI |
| Auth Service | https://auth.markpost.dev | Authentication service (external) |
| Weather Backend | https://weather.markpost.dev/api | Weather API backend |

## Port Changes Summary

### Before Refactoring
- Frontend: Port **3000** (same as auth service - conflict!)

### After Refactoring
- Frontend: Port **3030** (dedicated port)
- Auth Service: Port **3000** (external service)

## Docker Compose Ports

```yaml
services:
  weather-postgres:
    ports:
      - '13003:5432'
  
  weather-redis:
    ports:
      - '13004:6379'
  
  weather-service:
    ports:
      - '13001:13001'
  
  frontend:
    ports:
      - '13002:3030'  # Changed from 13002:3000
```

## API Endpoints

### Auth Service API (localhost:3000 / auth.markpost.dev)
```
POST   /api/auth/v1/login
POST   /api/auth/v1/register
POST   /api/auth/v1/logout
POST   /api/auth/v1/refresh
GET    /api/auth/v1/user
GET    /api/auth/v1/verify-email
POST   /api/auth/v1/forgot-password
POST   /api/auth/v1/reset-password
```

### Weather Backend API (localhost:12001 / weather.markpost.dev)
```
GET    /api/weather/v1/forecast
GET    /api/weather/v1/saved-locations
POST   /api/weather/v1/saved-locations
PUT    /api/weather/v1/saved-locations/{id}
DELETE /api/weather/v1/saved-locations/{id}
PUT    /api/weather/v1/saved-locations/reorder
```

## Redirect Flows

### Login Redirect
```
User visits: http://localhost:3030
  ↓ (Not authenticated)
Redirect to: http://localhost:3000/login?callback=http%3A%2F%2Flocalhost%3A3030%2F
  ↓ (After login)
Return to: http://localhost:3030/
```

### Profile/Security Redirect
```
User clicks "Profile": 
  ↓
Redirect to: http://localhost:3000/profile?callback=http%3A%2F%2Flocalhost%3A3030%2F
  ↓ (After changes)
Can return to: http://localhost:3030/
```

### Logout Redirect
```
User clicks "Logout":
  ↓
POST to: http://localhost:3000/api/auth/v1/logout
  ↓
Redirect to: http://localhost:3000/login?callback=http%3A%2F%2Flocalhost%3A3030%2F
```

## Cookie Configuration

### Domain Setup
For production, cookies should be set with:
```
Domain: .markpost.dev
Path: /
Secure: true
SameSite: Lax
HttpOnly: true
```

This allows:
- `auth.markpost.dev` to set auth cookies
- `weather.markpost.dev` to read those cookies
- Secure cross-subdomain authentication

### Development
For local development, cookies are set with:
```
Domain: localhost
Path: /
Secure: false
SameSite: Lax
HttpOnly: true
```

## Testing URLs

### Frontend
- Development: http://localhost:3030
- Demo page: http://localhost:3030/demo
- Production: https://weather.markpost.dev

### Auth Service Pages
- Login: http://localhost:3000/login
- Register: http://localhost:3000/register
- Profile: http://localhost:3000/profile
- Security: http://localhost:3000/security

## Health Checks

### Weather Backend
```bash
curl http://localhost:12001/actuator/health
```

### Auth Service
```bash
curl http://localhost:3000/api/auth/v1/health
```

## Quick Start Commands

### Start All Services (Docker)
```bash
docker-compose up -d
```

### Start Frontend Only (Development)
```bash
cd frontend
npm run dev
```

### Check Service Status
```bash
docker-compose ps
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f frontend
docker-compose logs -f weather-service
```

## Troubleshooting Port Conflicts

### Check if port is in use
```bash
# Check port 3030
lsof -i :3030

# Check port 3000
lsof -i :3000
```

### Kill process using port
```bash
kill -9 <PID>
```

## Network Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Browser                          │
│  http://localhost:3030 (Weather Frontend)           │
└────────────┬────────────────────────────────────────┘
             │
             ├──── Authentication ────▶ http://localhost:3000 (Auth Service)
             │                          ├── Login
             │                          ├── Register
             │                          ├── Profile
             │                          └── Security
             │
             └──── Weather Data ──────▶ http://localhost:12001 (Weather Backend)
                                        ├── Forecast API
                                        └── Saved Locations API
```

## Environment Variables Reference

### Frontend
```bash
NEXT_PUBLIC_WEATHER_API_URL=http://localhost:12001
NEXT_PUBLIC_APP_VERSION=1.7.1
NEXT_PUBLIC_BUILD_TIME=2024-12-11T10:00:00Z
```

### Backend
```bash
POSTGRES_PASSWORD=<your-password>
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:13003/weather_service
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=13004
```

