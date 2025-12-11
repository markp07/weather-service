# Weather Service

A modern weather application that provides real-time weather data and forecasts using the Open-Meteo API, with secure JWT authentication and multi-language support.

## 🌟 Features

- **Real-time Weather Data**: Current weather conditions for any location
- **14-Day Forecast**: Extended weather predictions
- **Hourly Forecasts**: Detailed hourly weather information with interactive graphs
- **Saved Locations**: Save and manage multiple locations
- **Multi-language Support**: Available in English, Dutch, German, and French
- **Responsive Design**: Works seamlessly on desktop and mobile devices
- **JWT Authentication**: Secure authentication via external auth service
- **Docker Support**: Easy deployment with Docker Compose
- **Weather Icons**: Dynamic weather icons that change based on conditions and time of day

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

## 📁 Project Structure

```
weather-service/
├── pom.xml                       # Parent POM
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

## 🚀 Getting Started

### Prerequisites

- Java 21
- Node.js 20+
- Docker & Docker Compose
- PostgreSQL 16 (for local development)
- Redis 7 (for local development)

### Environment Variables

Create a `.env` file in the project root:
```env
POSTGRES_PASSWORD=your_secure_password
```

### Running with Docker Compose

The easiest way to run the entire stack:

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

This will start:
- **PostgreSQL** on port `13003` (internal: 5432)
- **Redis** on port `13004` (internal: 6379)
- **Weather Service** on port `13001`
- **Frontend** on port `13002` (internal: 3030)

Access the application at: `http://localhost:13002`

### Local Development

#### Backend

```bash
# Start dependencies
docker-compose up -d weather-postgres weather-redis

# Build the project
./mvnw clean install

# Run with local profile
cd weather-service
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend will be available at: `http://localhost:13001`

#### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev
```

Frontend will be available at: `http://localhost:3030`

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

The weather service uses JWT tokens for authentication, validated against an external auth service:

- **Production**: `https://auth.markpost.dev/api/auth/v1/public-key`
- **Local Development**: `http://localhost:3000/api/auth/v1/public-key`

### Security Features

- JWT public key validation
- Token refresh mechanism
- Secure HTTP-only cookies
- CORS configuration
- Protected endpoints with Spring Security
- Excluded paths for health checks and API docs

### Authentication Flow

1. User authenticates via external auth service
2. JWT token stored in HTTP-only cookie
3. Weather service validates token using public key
4. Token automatically refreshed when needed
5. On 401, redirect to auth service with callback URL

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

| Service | Internal Port | External Port | Description |
|---------|--------------|---------------|-------------|
| weather-postgres | 5432 | 13003 | PostgreSQL database |
| weather-redis | 6379 | 13004 | Redis cache |
| weather-service | 13001 | 13001 | Spring Boot API |
| weather-frontend | 3030 | 13002 | Next.js frontend |

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
- Email: mark@markpost.nl

## 🙏 Acknowledgments

- [Open-Meteo](https://open-meteo.com/) for free weather API
- Spring Boot team for excellent framework
- Next.js team for React framework
- Vercel for next-intl internationalization library

## 📞 Support

For issues and questions:
- GitHub Issues: https://github.com/markp07/weather-service/issues
- Email: mark@markpost.nl

---

**Last Updated**: December 2024
**Version**: 1.7.1

