#!/bin/bash

echo "📦 Pulling latest images for Redis and PostgreSQL..."
# Pull updates only for redis and postgres
docker compose pull weather-redis weather-postgres

echo "🚀 Starting updated services (Redis and PostgreSQL)..."
# Bring up the updated services (only redis and postgres)
docker compose up -d weather-redis weather-postgres

echo "🧹 Cleaning up unused Docker resources..."
# Prune the system
docker system prune -f

echo ""
echo "✅ Redis and PostgreSQL updated successfully!"
echo ""
echo "📊 Service Status:"
docker compose ps weather-redis weather-postgres

