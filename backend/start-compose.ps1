$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if (!(Test-Path .env)) { & (Join-Path $PSScriptRoot "init-env.ps1") }
docker compose up -d --build --wait
exit $LASTEXITCODE
