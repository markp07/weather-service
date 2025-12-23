# Weather Service

A full-stack weather application providing real-time weather data and forecasts with secure JWT authentication. Built with Spring Boot and Next.js.

## Overview

This application delivers current weather conditions, 14-day forecasts, and hourly predictions for any location worldwide. Users can save and manage multiple locations, with support for four languages (English, Dutch, German, French). Authentication is handled by an external authentication service using JWT tokens.

**Note:** This project requires the [authentication-service](https://github.com/markp07/authentication-service) for user management and JWT token generation.

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.5.7
- **Language:** Java 21
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **Security:** Spring Security with JWT (RS256)
- **API Documentation:** OpenAPI 3.0 with Swagger UI
- **Mapping:** MapStruct 1.6.3
- **Build Tool:** Maven

### Frontend
- **Framework:** Next.js 16 (React 19)
- **Language:** TypeScript 5.9
- **Styling:** Tailwind CSS 4.0
- **Internationalization:** next-intl 4.5.8
- **Charts:** Recharts 2.15.0
- **Drag & Drop:** DnD Kit 6.3.1

### External APIs
- **Open-Meteo Weather API** - Weather data and forecasts (no API key required)
- **Open-Meteo Geocoding API** - Location search (no API key required)
- **BigDataCloud Reverse Geocoding API** - GPS to location conversion (requires API key)
- **Authentication Service** - JWT-based authentication (self-hosted)

## Project Structure

```
weather-service/
├── weather-service/              # Spring Boot backend
│   ├── src/main/
│   │   ├── java/nl/markpost/weather/
│   │   │   ├── client/          # External API clients (Feign)
│   │   │   ├── config/          # Security, caching, CORS
│   │   │   ├── controller/      # REST endpoints
│   │   │   ├── filter/          # JWT authentication filter
│   │   │   ├── service/         # Business logic
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── model/           # Domain entities
│   │   │   ├── mapper/          # DTO mappers
│   │   │   └── exception/       # Error handling
│   │   └── resources/
│   │       ├── application.yaml # Configuration
│   │       └── api/weather-api.yaml # OpenAPI specification
│   └── Dockerfile
├── frontend/                     # Next.js frontend
│   ├── src/
│   │   ├── app/                 # Next.js pages
│   │   ├── components/          # React components
│   │   ├── i18n/                # Internationalization
│   │   ├── types/               # TypeScript definitions
│   │   └── utils/               # API clients, utilities
│   ├── messages/                # Translation files (en, nl, de, fr)
│   └── Dockerfile
├── docker-compose.yml           # Multi-container orchestration
├── .env.example                 # Environment variables template
└── build-and-up.sh             # Automated deployment script
```

## Security Features

### Authentication & Authorization
- **JWT Token Validation:** RS256 public key verification
- **Token Refresh:** Automatic refresh before expiration
- **HTTP-only Cookies:** Secure token storage in browser
- **CORS Protection:** Configurable allowed origins
- **Public Key Caching:** 1-hour TTL to reduce external calls

### Implementation Details
- JWT validation filter intercepts all requests except health checks and documentation endpoints
- Public key retrieved from authentication service at startup and cached in Redis
- User ID extracted from JWT claims for authorization
- Failed authentication returns 401 with redirect to login

## Configuration

### Environment Variables

Create a `.env` file based on `.env.example`:

```bash
# Required
POSTGRES_PASSWORD=your_secure_password
JWT_PUBLIC_KEY_URL=https://auth.yourdomain.com/api/auth/v1/public-key
ALLOWED_ORIGIN_PATTERNS=https://weather.yourdomain.com
REVERSE_GEOCODE_API_KEY=your_bigdatacloud_api_key

# Optional (for frontend)
NEXT_PUBLIC_AUTH_API_URL=https://auth.yourdomain.com
NEXT_PUBLIC_WEATHER_API_URL=https://weather.yourdomain.com
```

### Application Profiles

**Production (default):**
```yaml
server:
  port: 13001
  servlet:
    context-path: /api/weather

spring:
  datasource:
    url: jdbc:postgresql://weather-postgres:5432/weather_service
  data:
    redis:
      host: weather-redis
      port: 6379

jwt:
  public-key-url: ${JWT_PUBLIC_KEY_URL}
  
weather:
  cors:
    allowed-origin-patterns: ${ALLOWED_ORIGIN_PATTERNS}
```

**Local Development:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:13003/weather_service
  data:
    redis:
      host: localhost
      port: 13004

jwt:
  public-key-url: http://localhost:3000/api/auth/v1/public-key
```

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21 (for local development)
- Node.js 20+ (for local development)
- [Authentication Service](https://github.com/markp07/authentication-service) running

### Quick Start with Docker

```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/weather-service.git
cd weather-service

# Create and configure .env
cp .env.example .env
nano .env  # Edit required variables

# Build and start all services
./build-and-up.sh
```

The application will be available at `http://localhost:13002`

### Services and Ports

| Service          | Internal Port | External Port | Description         |
|------------------|---------------|---------------|---------------------|
| weather-postgres | 5432          | 13003         | PostgreSQL database |
| weather-redis    | 6379          | 13004         | Redis cache         |
| weather-service  | 13001         | 13001         | Spring Boot API     |
| weather-frontend | 3030          | 13002         | Next.js frontend    |

### Local Development

**Backend:**
```bash
# Start dependencies
docker-compose up -d weather-postgres weather-redis

# Build and run
./mvnw clean install
cd weather-service
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

Frontend auto-detects localhost and connects to:
- Auth Service: `http://localhost:3000`
- Weather API: `http://localhost:13001`

## API Endpoints

### Weather Endpoints

**Get Weather Forecast**
```
GET /api/weather/v1/forecast?latitude={lat}&longitude={lon}
```
Returns 14-day daily forecast and 3-day hourly predictions.

**Get Current Weather**
```
GET /api/weather/v1/current?latitude={lat}&longitude={lon}
```
Returns current weather conditions.

### Location Endpoints

**Search Locations**
```
GET /api/weather/v1/locations/search?name={query}
```
Search for locations by name (no authentication required).

**Get Saved Locations**
```
GET /api/weather/v1/saved-locations
```
List all saved locations for authenticated user.

**Save Location**
```
POST /api/weather/v1/saved-locations
Content-Type: application/json

{
  "name": "Amsterdam",
  "latitude": 52.3676,
  "longitude": 4.9041,
  "country": "Netherlands"
}
```

**Delete Location**
```
DELETE /api/weather/v1/saved-locations/{id}
```

**Reorder Locations**
```
PUT /api/weather/v1/saved-locations/reorder
Content-Type: application/json

{
  "locationIds": [3, 1, 2]
}
```

### Health Check
```
GET /api/weather/actuator/health
```

### API Documentation

- **Swagger UI:** `http://localhost:13001/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:13001/api/weather/v3/api-docs`
- **OpenAPI YAML:** `weather-service/src/main/resources/api/weather-api.yaml`

**Note:** All endpoints except location search and health checks require JWT authentication via HTTP-only cookies.

## Caching Strategy

Redis caching is implemented for:
- **Weather data:** 5-minute TTL
- **Public keys:** 1-hour TTL
- Cache key format: `weather:{latitude}:{longitude}`

## Testing

**Backend:**
```bash
./mvnw test                           # Run all tests
./mvnw verify                         # Run tests with coverage
./mvnw test -Dtest=WeatherServiceTest # Run specific test
```

**Frontend:**
```bash
cd frontend
npm test                # Run all tests
npm test -- --coverage  # Run with coverage
npm test -- --watch     # Watch mode
```

## Deployment

### Using Docker Compose

```bash
# Build and start
./build-and-up.sh

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Manual Build

**Backend:**
```bash
./mvnw clean package
docker build -t weather-service:latest ./weather-service
```

**Frontend:**
```bash
cd frontend
npm run build
docker build -t weather-frontend:latest .
```

## Troubleshooting

**Database connection issues:**
```bash
docker ps | grep weather-postgres
docker logs weather-postgres
```

**Authentication failures:**
```bash
# Verify auth service is accessible
curl http://localhost:3000/api/auth/v1/public-key

# Check JWT_PUBLIC_KEY_URL configuration
docker exec weather-service env | grep JWT_PUBLIC_KEY_URL
```

**CORS errors:**
```bash
# Verify ALLOWED_ORIGIN_PATTERNS matches frontend URL
docker exec weather-service env | grep ALLOWED_ORIGIN_PATTERNS
```

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Author

**Mark Post**
- GitHub: [@markp07](https://github.com/markp07)

## Acknowledgments

- [Open-Meteo](https://open-meteo.com/) for free weather API
- Spring Boot and Next.js communities

