#!/bin/bash
# MailIt Wrapper API - Production Deployment Script
# Usage: ./deploy.sh

set -e

echo "=== MailIt Wrapper API Deployment ==="

# Check for .env file
if [ ! -f .env ]; then
    echo "Creating .env from template..."
    cp .env.example .env
    echo ""
    echo "⚠️  IMPORTANT: Edit .env and set your TRACKINGMORE_API_KEY"
    echo "   Then run this script again."
    exit 1
fi

# Verify TRACKINGMORE_API_KEY is set
source .env
if [ "$TRACKINGMORE_API_KEY" = "your_trackingmore_api_key_here" ] || [ -z "$TRACKINGMORE_API_KEY" ]; then
    echo "❌ Error: TRACKINGMORE_API_KEY not configured in .env"
    exit 1
fi

echo "✅ Environment configured"

# Pull latest images and rebuild
echo "Building and starting containers..."
docker-compose down 2>/dev/null || true
docker-compose up -d --build

# Wait for services to be healthy
echo "Waiting for services to start..."
sleep 10

# Check health
echo ""
echo "=== Service Status ==="
docker-compose ps

echo ""
echo "=== Health Check ==="
curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' || echo "Waiting for API..."

echo ""
echo "=== Deployment Complete ==="
echo "API:     http://localhost:8080"
echo "Swagger: http://localhost:8080/swagger-ui.html"
echo "Health:  http://localhost:8080/actuator/health"
echo ""
echo "Next steps:"
echo "  1. Create a client:  curl -X POST http://localhost:8080/admin/clients -H 'Content-Type: application/json' -d '{\"name\":\"My Client\",\"plan\":\"STARTER\"}'"
echo "  2. See ADMIN_GUIDE.md for complete admin instructions"
