#!/bin/bash

echo "🛑 Stopping URL Shortener..."
echo ""

# Stop services first
echo "Stopping services..."
docker compose -f docker-compose-services.yml down

# Stop infrastructure
echo "Stopping infrastructure..."
docker compose -f docker-compose-infra.yml down

echo ""
echo "✅ Everything stopped cleanly"
echo ""
echo "💡 To also remove all data volumes run:"
echo "   docker compose -f docker-compose-infra.yml down -v"