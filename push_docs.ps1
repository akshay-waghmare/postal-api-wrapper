# Script to build, tag and push Docs image to Docker Hub
# Usage: .\push_docs.ps1 -Username "macubex"

param (
    [string]$Username = "macubex"
)

$ImageName = "mailit-docs"

Write-Host "=== Building and Pushing $ImageName to Docker Hub ===" -ForegroundColor Cyan

# Build
Write-Host "Building image..."
docker build -t $ImageName -f Dockerfile.docs .

# Tagging
Write-Host "Tagging images..."
docker tag $ImageName "$Username/$ImageName`:latest"

# Pushing
Write-Host "Pushing to Docker Hub..."
docker push "$Username/$ImageName`:latest"

Write-Host "✅ Successfully pushed to $Username/$ImageName" -ForegroundColor Green
