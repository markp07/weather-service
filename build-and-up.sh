#!/bin/bash
set -e

# Required environment variables (for backend)
REQUIRED_VARS=("POSTGRES_PASSWORD" "JWT_PUBLIC_KEY_URL" "ALLOWED_ORIGIN_PATTERNS")

# Optional environment variables (for frontend production deployment)
# NEXT_PUBLIC_AUTH_API_URL and NEXT_PUBLIC_WEATHER_API_URL are optional
# Frontend auto-detects localhost in development mode

# Check if .env file exists
if [ ! -f .env ]; then
    echo "❌ .env file not found!"
    echo "📝 Creating .env from .env.example..."
    cp .env.example .env
    echo ""
    echo "✅ .env file created successfully!"
    echo ""
    echo "⚠️  IMPORTANT: Please configure the following variables in .env:"
    echo "   - POSTGRES_PASSWORD (set a secure password)"
    echo "   - JWT_PUBLIC_KEY_URL (auth service public key endpoint)"
    echo "   - ALLOWED_ORIGIN_PATTERNS (frontend origin for CORS)"
    echo "   - NEXT_PUBLIC_AUTH_API_URL (Authentication service URL)"
    echo "   - NEXT_PUBLIC_WEATHER_API_URL (Weather service URL)"
    echo ""
    echo "📖 See .env file for more details and examples."
    echo ""
    echo "🔄 Run this script again after configuring .env"
    exit 1
fi

# Check if all required variables are set in .env
echo "🔍 Checking required environment variables..."
MISSING_VARS=()
EXAMPLE_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
    # Check if variable exists in .env
    if ! grep -q "^${var}=" .env; then
        MISSING_VARS+=("$var")
        # Get the line from .env.example
        EXAMPLE_LINE=$(grep "^${var}=" .env.example || echo "${var}=")
        echo "$EXAMPLE_LINE" >> .env
        EXAMPLE_VARS+=("$var")
    else
        # Check if variable has a placeholder value
        VALUE=$(grep "^${var}=" .env | cut -d'=' -f2-)
        if [[ "$VALUE" == "your_secure_postgres_password" ]] || \
           [[ "$VALUE" == "your-frontend-domain.com"* ]] || \
           [[ "$VALUE" == "your-auth-service.com"* ]] || \
           [[ "$VALUE" == "your-weather-service.com"* ]]; then
            EXAMPLE_VARS+=("$var")
        fi
    fi
done

# If we added missing variables, notify and exit
if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo ""
    echo "⚠️  Missing variables added to .env:"
    for var in "${MISSING_VARS[@]}"; do
        echo "   - $var"
    done
    echo ""
    echo "📝 Please configure these variables in .env and run again."
    exit 1
fi

# If placeholder values found, warn and exit
if [ ${#EXAMPLE_VARS[@]} -gt 0 ]; then
    echo ""
    echo "⚠️  Found placeholder values in .env for:"
    for var in "${EXAMPLE_VARS[@]}"; do
        echo "   - $var"
    done
    echo ""
    echo "📝 Please update these variables with actual values in .env and run again."
    exit 1
fi

echo "✅ All required environment variables are configured!"
echo ""

git pull

# Build all Maven modules (parent POM)
echo "[1/3] Building all Maven modules..."
mvn clean package -DskipTests

echo "[2/3] Building Docker images..."
docker compose build --no-cache

echo "[3/3] Starting all services with docker-compose..."
docker compose up -d

echo ""
echo "✅ All services are up and running!"
echo ""
echo "📊 Service Status:"
docker compose ps
