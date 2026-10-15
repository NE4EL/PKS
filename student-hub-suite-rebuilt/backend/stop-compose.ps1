$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& docker compose -f (Join-Path $PSScriptRoot "docker-compose.yml") down
exit $LASTEXITCODE
