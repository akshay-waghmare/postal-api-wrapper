#!/bin/bash
# MailIt Wrapper API - Production Deployment (Docker Hub Image)
# Usage: ./deploy_prod.sh

set -e

echo "=== MailIt Wrapper API Production Deployment ==="

# Check for .env file
if [ ! -f .env ]; then
    echo "Creating .env from template..."
    cp .env.example .env
    echo ""
    echo "⚠️  IMPORTANT: Edit .env and set your TRACKINGMORE_API_KEY"
    exit 1
fi

# Verify TRACKINGMORE_API_KEY is set
source .env
if [ "$TRACKINGMORE_API_KEY" = "your_trackingmore_api_key_here" ] || [ -z "$TRACKINGMORE_API_KEY" ]; then
    echo "❌ Error: TRACKINGMORE_API_KEY not configured in .env"
    exit 1
fi

echo "✅ Environment configured"

# Pull latest image
echo "Pulling latest image from Docker Hub..."
docker-compose -f docker-compose.prod.yml pull

# Start services
echo "Starting services..."
docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
docker-compose -f docker-compose.prod.yml up -d

# Wait for services
echo "Waiting for services to start..."
sleep 10

# Check status
echo ""
echo "=== Service Status ==="
docker-compose -f docker-compose.prod.yml ps

echo ""
echo "=== Health Check ==="
curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' || echo "Waiting for API..."

echo ""
echo "=== Deployment Complete ==="
echo "API is running using image: macubex/mailit-wrapper:latest"
