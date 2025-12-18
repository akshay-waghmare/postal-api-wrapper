# MailIt Wrapper API - Production Deployment Script (Windows)
# Usage: .\deploy.ps1

Write-Host "=== MailIt Wrapper API Deployment ===" -ForegroundColor Cyan

# Check for .env file
if (-not (Test-Path .env)) {
    Write-Host "Creating .env from template..."
    Copy-Item .env.example .env
    Write-Host ""
    Write-Host "⚠️  IMPORTANT: Edit .env and set your TRACKINGMORE_API_KEY" -ForegroundColor Yellow
    Write-Host "   Then run this script again."
    exit 1
}

# Read .env file
$envContent = Get-Content .env | Where-Object { $_ -match '=' }
$env:TRACKINGMORE_API_KEY = ($envContent | Where-Object { $_ -match '^TRACKINGMORE_API_KEY=' }) -replace 'TRACKINGMORE_API_KEY=', ''

if ($env:TRACKINGMORE_API_KEY -eq 'your_trackingmore_api_key_here' -or [string]::IsNullOrEmpty($env:TRACKINGMORE_API_KEY)) {
    Write-Host "❌ Error: TRACKINGMORE_API_KEY not configured in .env" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Environment configured" -ForegroundColor Green

# Pull latest images and rebuild
Write-Host "Building and starting containers..."
docker-compose down 2>$null
docker-compose up -d --build

# Wait for services
Write-Host "Waiting for services to start..."
Start-Sleep -Seconds 15

# Check status
Write-Host ""
Write-Host "=== Service Status ===" -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host "=== Health Check ===" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -ErrorAction SilentlyContinue
    Write-Host "Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "API still starting... check again in a few seconds" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Cyan
Write-Host "API:     http://localhost:8080"
Write-Host "Swagger: http://localhost:8080/swagger-ui.html"
Write-Host "Health:  http://localhost:8080/actuator/health"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host '  1. Create a client:  Invoke-RestMethod -Method Post -Uri "http://localhost:8080/admin/clients" -ContentType "application/json" -Body ''{"name":"My Client","plan":"STARTER"}'''
Write-Host "  2. See ADMIN_GUIDE.md for complete admin instructions"
