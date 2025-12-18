# MailIt Wrapper API - Production Deployment (Docker Hub Image)
# Usage: .\deploy_prod.ps1

Write-Host "=== MailIt Wrapper API Production Deployment ===" -ForegroundColor Cyan

# Check for .env file
if (-not (Test-Path .env)) {
    Write-Host "Creating .env from template..."
    Copy-Item .env.example .env
    Write-Host ""
    Write-Host "⚠️  IMPORTANT: Edit .env and set your TRACKINGMORE_API_KEY" -ForegroundColor Yellow
    exit 1
}

# Read .env file validation
$envContent = Get-Content .env | Where-Object { $_ -match '=' }
$env:TRACKINGMORE_API_KEY = ($envContent | Where-Object { $_ -match '^TRACKINGMORE_API_KEY=' }) -replace 'TRACKINGMORE_API_KEY=', ''

if ($env:TRACKINGMORE_API_KEY -eq 'your_trackingmore_api_key_here' -or [string]::IsNullOrEmpty($env:TRACKINGMORE_API_KEY)) {
    Write-Host "❌ Error: TRACKINGMORE_API_KEY not configured in .env" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Environment configured" -ForegroundColor Green

# Pull latest image
Write-Host "Pulling latest image from Docker Hub..."
docker-compose -f docker-compose.prod.yml pull

# Start services
Write-Host "Starting services..."
docker-compose -f docker-compose.prod.yml down 2>$null
docker-compose -f docker-compose.prod.yml up -d

# Wait for services
Write-Host "Waiting for services to start..."
Start-Sleep -Seconds 15

# Check status
Write-Host ""
Write-Host "=== Service Status ===" -ForegroundColor Cyan
docker-compose -f docker-compose.prod.yml ps

Write-Host ""
Write-Host "=== Health Check ===" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost:9000/actuator/health" -ErrorAction SilentlyContinue
    Write-Host "Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "API still starting... check again in a few seconds" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Cyan
Write-Host "API is running using image: macubex/mailit-wrapper:latest"
