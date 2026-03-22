#!/bin/bash

set -e

echo "🚀 Starting URL Shortener..."
echo ""

# Step 1 — Start infrastructure
echo "📦 Starting infrastructure (Redis, Postgres, ClickHouse, Kafka, Grafana)..."
docker compose -f docker-compose-infra.yml up -d

# Step 2 — Wait for infra to be healthy
echo "⏳ Waiting for infrastructure to be healthy..."
sleep 15

# Check all infra containers are healthy
UNHEALTHY=$(docker compose -f docker-compose-infra.yml ps --format json | grep -c '"Health":"unhealthy"' || true)
if [ "$UNHEALTHY" -gt 0 ]; then
  echo "⚠️  Some infrastructure containers are still starting, waiting 15 more seconds..."
  sleep 15
fi

echo "✅ Infrastructure ready"
echo ""

# Step 3 — Build and start services
echo "🔨 Building and starting services..."
docker compose -f docker-compose-services.yml up --build -d

echo ""
echo "⏳ Waiting for services to start (this takes ~30 seconds on first run)..."
sleep 30

echo ""
echo "✅ All services started!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🌐 Frontend        → http://localhost:3001"
echo "  🔗 API Gateway     → http://localhost:80"
echo "  📊 Grafana         → http://localhost:3000"
echo "                       admin / admin123"
echo "  📈 ClickHouse UI   → http://localhost:8123/play"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Run ./stop.sh to stop everything"