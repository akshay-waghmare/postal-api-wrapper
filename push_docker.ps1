# Script to tag and push Docker image to Docker Hub
# Usage: .\push_docker.ps1 -Username "your-dockerhub-username"

param (
    [Parameter(Mandatory=$true)]
    [string]$Username
)

$Version = Get-Content "VERSION"
$ImageName = "mailit-wrapper"
$LocalImage = "mailit-postal-wrapper-api-api:latest"

Write-Host "=== Pushing $ImageName v$Version to Docker Hub ===" -ForegroundColor Cyan

# Check if image exists
if (-not (docker images -q $LocalImage)) {
    Write-Host "❌ Image $LocalImage not found. Please run 'docker-compose build' first." -ForegroundColor Red
    exit 1
}

# Tagging
Write-Host "Tagging images..."
docker tag $LocalImage "$Username/$ImageName`:$Version"
docker tag $LocalImage "$Username/$ImageName`:latest"

# Pushing
Write-Host "Pushing to Docker Hub (this may take a while)..."
docker push "$Username/$ImageName`:$Version"
docker push "$Username/$ImageName`:latest"

Write-Host "✅ Successfully pushed to $Username/$ImageName" -ForegroundColor Green
