# Weather Service

[![Build](https://github.com/YOUR_USERNAME/weather-service/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/weather-service/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16-black.svg)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue.svg)](https://www.typescriptlang.org/)

A modern, full-stack weather application that provides real-time weather data and forecasts using the Open-Meteo API, with secure JWT authentication and multi-language support.

> **⚠️ Important:** This project requires the [authentication-service](https://github.com/markp07/authentication-service) for user management and JWT token generation.

## 🔗 Quick Links

- [🚀 Quick Start](#-getting-started) - Get up and running in 5 minutes
- [⚠️ Authentication Setup](AUTHENTICATION_SETUP.md) - Complete auth configuration guide
- [🔐 Environment Variables](ENVIRONMENT_VARIABLES.md) - All configuration options
- [📋 Deployment Checklist](DEPLOYMENT_CHECKLIST.md) - Pre-deployment verification
- [🌐 API Documentation](#-api-documentation) - REST API endpoints
- [🔧 Troubleshooting](#-troubleshooting) - Common issues and solutions

## 🌟 Features

### Weather & UI
- **Real-time Weather Data**: Current weather conditions for any location
- **14-Day Forecast**: Extended weather predictions with daily highs and lows
- **Hourly Forecasts**: Detailed hourly weather information with interactive graphs
- **Saved Locations**: Save, manage, and reorder multiple locations with drag-and-drop
- **Multi-language Support**: Available in English, Dutch, German, and French
- **Responsive Design**: Works seamlessly on desktop and mobile devices
- **Weather Icons**: Dynamic weather icons that change based on conditions and time of day
- **Dark Mode Support**: Automatic theme switching based on user preferences

### Security & Infrastructure
- **JWT Authentication**: Secure authentication via [authentication-service](https://github.com/markp07/authentication-service)
- **Token Refresh**: Automatic token refresh mechanism
- **HTTP-only Cookies**: Secure token storage
- **CORS Protection**: Configurable origin patterns
- **Docker Support**: Complete containerization with health checks
- **Automated Deployment**: Smart build scripts with environment validation
- **Redis Caching**: Fast response times with intelligent caching
- **PostgreSQL**: Reliable data persistence

## 🏗️ Architecture

### Backend (Spring Boot)
- **Java 21** with Spring Boot 3.4.1
- **PostgreSQL** for data persistence (saved locations)
- **Redis** for caching weather data
- **JWT** authentication with public key validation
- **Open-Meteo API** integration for weather data
- **MapStruct** for DTO mapping
- **OpenAPI** specification with code generation

### Frontend (Next.js)
- **Next.js 15.1** with React 19
- **TypeScript** for type safety
- **Tailwind CSS** for styling
- **next-intl** for internationalization
- **Recharts** for data visualization
- **React Bootstrap Icons** for weather icons
- **DnD Kit** for drag-and-drop location reordering

## 🔌 Downstream APIs

This project integrates with the following external services:

### Weather Data
- **[Open-Meteo Weather API](https://open-meteo.com/)** - Free weather forecast API
  - 14-day daily forecasts with temperature, precipitation, and weather codes
  - 3-day hourly forecasts with detailed conditions
  - Wind speed, direction, and atmospheric data
  - No API key required ✨

### Geocoding & Location Services
- **[Open-Meteo Geocoding API](https://geocoding-api.open-meteo.com/)** - Location search
  - Search locations by name worldwide
  - Returns coordinates, country, and administrative regions
  - No API key required ✨

- **[BigDataCloud Reverse Geocoding API](https://www.bigdatacloud.com/free-api/free-reverse-geocode-to-city-api)** - Coordinate to location
  - Converts GPS coordinates to location names
  - Used for "Current Location" feature
  - Free tier available (no API key needed for basic usage) ✨

### Authentication
- **[Authentication Service](https://github.com/markp07/authentication-service)** - JWT authentication
  - User registration, login, and session management
  - JWT access and refresh token generation
  - Public key endpoint for token verification
  - Self-hosted microservice (not a third-party API)

**All external APIs are free to use with no API keys required**, making this project easy to deploy without additional service subscriptions or costs.

## 📁 Project Structure

```
weather-service/
├── .env.example                  # Environment variables template
├── docker-compose.yml            # Docker services configuration
├── pom.xml                       # Parent POM
├── build-and-up.sh              # Automated build & deploy script
├── update.sh                     # Update dependencies script
├── README.md                     # This file
├── VERSIONING.md                 # Versioning guide
├── CHANGELOG.md                  # Version history
├── weather-service/              # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/nl/markpost/weather/
│   │   │   │   ├── client/       # External API clients
│   │   │   │   ├── common/       # Shared utilities and exceptions
│   │   │   │   ├── config/       # Configuration classes
│   │   │   │   ├── controller/   # REST controllers
│   │   │   │   ├── exception/    # Exception handlers
│   │   │   │   ├── filter/       # Security filters (JWT)
│   │   │   │   ├── mapper/       # Entity/DTO mappers
│   │   │   │   ├── model/        # Domain models
│   │   │   │   ├── repository/   # JPA repositories
│   │   │   │   └── service/      # Business logic
│   │   │   └── resources/
│   │   │       ├── application.yaml     # Application config
│   │   │       └── api/
│   │   │           └── weather-api.yaml # OpenAPI spec
│   │   └── test/                 # Unit and integration tests
│   ├── Dockerfile
│   └── pom.xml
└── frontend/                     # Next.js frontend
    ├── src/
    │   ├── app/                  # Next.js pages (dashboard)
    │   ├── components/           # React components
    │   │   ├── HourlyGraphModal.tsx
    │   │   ├── LocationBar.tsx
    │   │   ├── LocationSearch.tsx
    │   │   ├── SavedLocations.tsx
    │   │   └── Sidebar.tsx
    │   ├── i18n/                 # Internationalization config
    │   ├── types/                # TypeScript types
    │   └── utils/                # Utility functions
    ├── messages/                 # Translation files
    │   ├── en.json              # English
    │   ├── nl.json              # Dutch
    │   ├── de.json              # German
    │   └── fr.json              # French
    ├── Dockerfile
    └── package.json
```

## ⚠️ Authentication Requirements

**This project is designed to work with the [authentication-service](https://github.com/markp07/authentication-service)**, which provides:

- JWT token generation and validation
- Public key endpoint at `/api/auth/v1/public-key` for token verification
- User registration, login, logout, and profile management
- Token refresh mechanism
- HTTP-only cookie-based authentication
- Security settings management

### Setup Options

1. **Use the provided authentication service** (recommended):
   - Clone and deploy: https://github.com/markp07/authentication-service
   - Configure the `JWT_PUBLIC_KEY_URL` environment variable to point to your deployed auth service
   - See the authentication-service README for deployment instructions

2. **Use an alternative JWT-based authentication service**:
   - Ensure it exposes a public key endpoint in PEM format
   - Configure JWT tokens with RS256 algorithm
   - Include `sub` (user email) and `userId` claims in tokens

3. **For local development**:
   - Run the authentication-service locally on port 3000
   - Weather service will automatically connect to `http://localhost:3000/api/auth/v1/public-key`


## 🚀 Getting Started

### Prerequisites

- Java 21
- Node.js 20+
- Docker & Docker Compose
- PostgreSQL 16 (for local development)
- Redis 7 (for local development)
- **[authentication-service](https://github.com/markp07/authentication-service)** (see above)

### Quick Start with Docker

The project includes automated scripts for easy deployment:

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/weather-service.git
cd weather-service

# 2. Run build-and-up script (creates .env automatically)
./build-and-up.sh
```

On first run, the script will:
1. Create `.env` from `.env.example` if it doesn't exist
2. Prompt you to configure required variables
3. Exit with instructions

```bash
# 3. Configure .env (only POSTGRES_PASSWORD required for local dev)
nano .env
# Set POSTGRES_PASSWORD to any secure password
# Other variables have sensible defaults for local development

# 4. Run again to build and deploy
./build-and-up.sh
```

### Environment Variables

#### Required Variables (Backend)

| Variable                  | Description                 | Local Default                                  | Production Example                                   |
|---------------------------|-----------------------------|------------------------------------------------|------------------------------------------------------|
| `POSTGRES_PASSWORD`       | PostgreSQL password         | Any password                                   | Strong password                                      |
| `JWT_PUBLIC_KEY_URL`      | Auth service public key URL | `http://localhost:3000/api/auth/v1/public-key` | `https://auth.yourdomain.com/api/auth/v1/public-key` |
| `ALLOWED_ORIGIN_PATTERNS` | CORS allowed origins        | `http://localhost:3030`                        | `https://weather.yourdomain.com`                     |

#### Optional Variables (Frontend Production)

| Variable                      | Description      | Note                       |
|-------------------------------|------------------|----------------------------|
| `NEXT_PUBLIC_AUTH_API_URL`    | Auth service URL | Auto-detected in local dev |
| `NEXT_PUBLIC_WEATHER_API_URL` | Weather API URL  | Auto-detected in local dev |

**For detailed information**, see [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md)

### Deployment Scripts

The project includes convenient scripts for deployment:

#### `./build-and-up.sh` - Build and Deploy
Automated deployment with environment validation:
- ✅ Checks if `.env` exists (creates from `.env.example` if missing)
- ✅ Validates required variables are configured
- ✅ Detects placeholder values and prompts for updates
- ✅ Pulls latest code from git
- ✅ Builds Maven modules
- ✅ Builds Docker images
- ✅ Starts all services

```bash
./build-and-up.sh
```

#### `./update.sh` - Update Dependencies
Updates only PostgreSQL and Redis containers:

```bash
./update.sh
```

### Running with Docker Compose

#### Production Deployment

```bash
# Use the automated script (recommended)
./build-and-up.sh
```

#### Manual Deployment

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**Services and Ports:**
- **PostgreSQL** (`weather-postgres`): `13003` → `5432`
- **Redis** (`weather-redis`): `13004` → `6379`
- **Weather Service** (`weather-service`): `13001`
- **Frontend** (`weather-frontend`): `13002` → `3030`

Access the application at: `http://localhost:13002`

**Health Checks:** All services include health checks for reliable startup

### Local Development

**Prerequisites:** Ensure [authentication-service](https://github.com/markp07/authentication-service) is running on `http://localhost:3000`

#### Backend

```bash
# 1. Create .env with minimal configuration
cat > .env << EOF
POSTGRES_PASSWORD=localpassword
JWT_PUBLIC_KEY_URL=http://localhost:3000/api/auth/v1/public-key
ALLOWED_ORIGIN_PATTERNS=http://localhost:3030
EOF

# 2. Start dependencies
docker-compose up -d weather-postgres weather-redis

# 3. Build the project
./mvnw clean install

# 4. Run with local profile
cd weather-service
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend will be available at: `http://localhost:13001`

**Default Ports:**
- Backend: `13001`
- PostgreSQL: `13003`
- Redis: `13004`

#### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev
```

Frontend will be available at: `http://localhost:3030`

**Auto-Configuration:** Frontend automatically detects localhost and connects to:
- Auth Service: `http://localhost:3000`
- Weather API: `http://localhost:13001`

No environment variables needed for local development! ✨

## 🌐 API Documentation

### OpenAPI Specification

- **Swagger UI**: `http://localhost:13001/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:13001/api/weather/v3/api-docs`
- **Spec File**: `weather-service/src/main/resources/api/weather-api.yaml`

### Key Endpoints

#### Weather
- `GET /api/weather/v1/forecast?latitude={lat}&longitude={lon}` - Get weather forecast
- `GET /api/weather/v1/current?latitude={lat}&longitude={lon}` - Get current weather

#### Saved Locations
- `GET /api/weather/v1/saved-locations` - List saved locations
- `POST /api/weather/v1/saved-locations` - Save a new location
- `PUT /api/weather/v1/saved-locations/{id}` - Update location
- `DELETE /api/weather/v1/saved-locations/{id}` - Delete location
- `PUT /api/weather/v1/saved-locations/reorder` - Reorder locations

#### Health Check
- `GET /api/weather/actuator/health` - Service health status

All weather endpoints require JWT authentication.

## 🔐 Security

### JWT Authentication

This weather service integrates with the [authentication-service](https://github.com/markp07/authentication-service) for user management and JWT token generation. The service validates JWT tokens using public keys retrieved from the configured authentication service.

**How It Works:**
1. User logs in via authentication-service
2. Authentication-service issues JWT tokens (access + refresh)
3. Frontend sends JWT with API requests (HTTP-only cookies)
4. Weather service validates JWT using public key
5. On token expiry, frontend automatically refreshes

**Configuration:**

```env
# Backend - Public key validation
JWT_PUBLIC_KEY_URL=http://localhost:3000/api/auth/v1/public-key

# Backend - CORS configuration
ALLOWED_ORIGIN_PATTERNS=http://localhost:3030

# Frontend - Service URLs (optional for local dev)
NEXT_PUBLIC_AUTH_API_URL=http://localhost:3000
NEXT_PUBLIC_WEATHER_API_URL=http://localhost:13001
```

### Security Features

- JWT public key validation
- Token refresh mechanism
- Secure HTTP-only cookies
- CORS configuration
- Protected endpoints with Spring Security
- Excluded paths for health checks and API docs

### Security Features

- ✅ **JWT Token Validation** - Public key-based RS256 validation
- ✅ **HTTP-only Cookies** - Secure token storage
- ✅ **Token Refresh** - Automatic refresh before expiration
- ✅ **CORS Protection** - Configurable origin patterns
- ✅ **Public Key Caching** - 1-hour TTL for performance
- ✅ **401 Redirect** - Automatic redirect to auth service on unauthorized
- ✅ **Rate Limiting** - Via external authentication service
- ✅ **Health Checks** - Monitoring endpoints for all services

### Security Best Practices

- Never commit `.env` file to version control
- Use strong passwords for PostgreSQL in production
- Enable HTTPS in production deployments
- Restrict CORS to specific origins (no wildcards)
- Rotate JWT keys regularly in auth service
- Monitor authentication failures
- Keep dependencies updated

**For detailed security information**, see [SECURITY.md](SECURITY.md)

## 🌍 Internationalization

The application supports four languages with complete translations:

- 🇬🇧 **English** (en)
- 🇳🇱 **Dutch** (nl)
- 🇩🇪 **German** (de)
- 🇫🇷 **French** (fr)

All UI elements and weather conditions are fully translated, with automatic language detection from browser preferences.

## 📊 Caching Strategy

### Redis Caching

- **Weather Data**: 5-minute TTL
- **Public Key**: 1-hour TTL (automatic refresh)
- Cache key format: `weather:{latitude}:{longitude}`

### Cache Configuration

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: weather-redis
      port: 6379
```

## 🐳 Docker Configuration

### Services

| Service          | Internal Port | External Port  | Description         |
|------------------|---------------|----------------|---------------------|
| weather-postgres | 5432          | 13003          | PostgreSQL database |
| weather-redis    | 6379          | 13004          | Redis cache         |
| weather-service  | 13001         | 13001          | Spring Boot API     |
| weather-frontend | 3030          | 13002          | Next.js frontend    |

### Health Checks

All services include health checks for proper startup ordering:

- **PostgreSQL**: `pg_isready` check
- **Redis**: `redis-cli ping` check
- **Backend**: Spring Boot actuator health endpoint
- **Frontend**: HTTP request to homepage

### Container Names

- `weather-postgres`
- `weather-redis`
- `weather-service`
- `weather-frontend`

## 🧪 Testing

### Backend Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw verify

# Run specific test class
./mvnw test -Dtest=WeatherServiceTest
```

Coverage reports: `target/site/jacoco/index.html`

### Frontend Tests

```bash
cd frontend

# Run all tests
npm test

# Run with coverage
npm test -- --coverage

# Run in watch mode
npm test -- --watch
```

Test results: 71 tests passing

## 📝 Configuration

### Application Profiles

#### Default (Production/Docker)
```yaml
server:
  port: 13001
  
spring:
  datasource:
    url: jdbc:postgresql://weather-postgres:5432/weather_service
  data:
    redis:
      host: weather-redis
      port: 6379
      
jwt:
  public-key-url: https://auth.markpost.dev/api/auth/v1/public-key
```

#### Local Profile
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

## 📦 Build & Deploy

### Build Backend

```bash
# Build JAR
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Build Docker image
docker-compose build weather-service
```

### Build Frontend

```bash
cd frontend

# Production build
npm run build

# Build Docker image
docker-compose build weather-frontend
```

## 🚢 Releases

The project uses semantic versioning with automated releases via GitHub Actions.

### Create a Release

```bash
# Create and push a tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

This triggers:
- Docker image build for both backend and frontend
- Tag with version number
- Push to container registry
- GitHub Release creation

### Version Format

- **Major** (v2.0.0): Breaking changes
- **Minor** (v1.1.0): New features
- **Patch** (v1.0.1): Bug fixes

## 📦 Dependencies

### Backend
- Spring Boot 3.4.1
- Spring Security 6.5.6
- Spring Data JPA
- PostgreSQL Driver 42.7.8
- Redis (Lettuce)
- MapStruct 1.6.3
- Lombok 1.18.36
- JJWT 0.12.6
- OpenAPI Generator

### Frontend
- Next.js 15.1.3
- React 19.0.0
- TypeScript 5.7.2
- Tailwind CSS 4.0.0
- next-intl 3.26.2
- Recharts 2.15.0
- React Bootstrap Icons 1.11.4
- DnD Kit 6.3.1

## 🔗 Related Services

- **Auth Service**: https://auth.markpost.dev
- **Production URL**: https://weather.markpost.dev
- **API Docs**: https://weather.markpost.dev/swagger-ui.html

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Mark Post**
- GitHub: [@markp07](https://github.com/markp07)

## 📚 Additional Documentation

### Configuration & Setup
- **[AUTHENTICATION_SETUP.md](AUTHENTICATION_SETUP.md)** - Complete authentication setup guide
- **[ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md)** - Environment variable reference
- **[DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)** - Pre-deployment checklist

### Development
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute to this project
- **[VERSIONING.md](VERSIONING.md)** - Semantic versioning and release process
- **[CHANGELOG.md](CHANGELOG.md)** - Version history and changes

## 🔧 Troubleshooting

### Common Issues

#### "Cannot connect to database"
```bash
# Check if PostgreSQL is running
docker ps | grep weather-postgres

# Check logs
docker logs weather-postgres

# Verify password in .env matches database
```

#### "401 Unauthorized on all requests"
```bash
# Ensure authentication service is running
curl http://localhost:3000/api/auth/v1/public-key

# Check JWT_PUBLIC_KEY_URL is correct
echo $JWT_PUBLIC_KEY_URL
```

#### "CORS errors in browser"
```bash
# Verify ALLOWED_ORIGIN_PATTERNS matches frontend URL
docker exec weather-service env | grep ALLOWED_ORIGIN_PATTERNS

# Should match: http://localhost:3030 (local) or your production domain
```

#### "Frontend build fails with TypeScript errors"
```bash
# Clear cache and rebuild
cd frontend
rm -rf .next node_modules
npm install
npm run build
```

#### "build-and-up.sh says variables are missing"
```bash
# The script auto-adds missing variables to .env
# Configure them and run again:
nano .env
./build-and-up.sh
```

**For detailed troubleshooting**, see [AUTHENTICATION_SETUP.md](AUTHENTICATION_SETUP.md#troubleshooting)

## 🙏 Acknowledgments

- [Open-Meteo](https://open-meteo.com/) for free weather API
- Spring Boot team for excellent framework
- Next.js team for React framework
- Vercel for next-intl internationalization library

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For issues and questions:
- **Issues**: [GitHub Issues](https://github.com/YOUR_USERNAME/weather-service/issues)
- **Security**: See [SECURITY.md](SECURITY.md) for reporting security vulnerabilities
- **Contributing**: See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines

---

**Last Updated**: 14th December 2025  
**Version**: 1.8.4

