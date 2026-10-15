param(
    [int]$WaitTimeoutSeconds = 420
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$ComposePath = Join-Path $ProjectRoot "docker-compose.yml"
$EnvPath = Join-Path $ProjectRoot ".env"

Set-Location $ProjectRoot

if (-not (Test-Path $ComposePath)) {
    throw "docker-compose.yml not found: $ComposePath"
}

$composeText = Get-Content -LiteralPath $ComposePath -Raw
if ($composeText -notmatch "STUDENT_HUB_COMPOSE_V3") {
    throw "Wrong/old docker-compose.yml is active. Expected marker STUDENT_HUB_COMPOSE_V3."
}

if (-not (Test-Path $EnvPath)) {
    Write-Host ".env not found. Creating it..." -ForegroundColor Yellow
    & (Join-Path $ProjectRoot "init-env.ps1")
}

Write-Host ""
Write-Host "Active Compose:" -ForegroundColor Cyan
Write-Host "  $ComposePath"
Write-Host "Profile marker: STUDENT_HUB_COMPOSE_V3" -ForegroundColor Green
Write-Host ""

# Remove stale containers from this Compose project but preserve named database volume.
Write-Host "Stopping stale containers (database volume is preserved)..." -ForegroundColor Cyan
& docker compose -f $ComposePath down --remove-orphans
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Starting fresh containers..." -ForegroundColor Cyan
& docker compose -f $ComposePath up -d --force-recreate --wait --wait-timeout $WaitTimeoutSeconds

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Startup failed. Effective backend command:" -ForegroundColor Red
    & docker compose -f $ComposePath config | Select-String -Pattern "package|java -jar|spring-boot:run|STUDENT_HUB_COMPOSE"
    Write-Host ""
    Write-Host "Backend logs:" -ForegroundColor Yellow
    & docker compose -f $ComposePath logs --tail 160 backend
    exit $LASTEXITCODE
}

function Read-DotEnv([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $t = $line.Trim()
        if (-not $t -or $t.StartsWith("#")) { continue }
        $eq = $t.IndexOf("=")
        if ($eq -lt 1) { continue }
        $k = $t.Substring(0, $eq).Trim()
        $v = $t.Substring($eq + 1).Trim()
        if ($v.Length -ge 2 -and $v.StartsWith('"') -and $v.EndsWith('"')) {
            $v = $v.Substring(1, $v.Length - 2)
        }
        $values[$k] = $v
    }
    return $values
}

$e = Read-DotEnv $EnvPath
$appPort = if ($e.ContainsKey("APP_PORT")) { $e["APP_PORT"] } else { "8080" }
$pgPort = if ($e.ContainsKey("POSTGRES_PORT")) { $e["POSTGRES_PORT"] } else { "5432" }
$pgDb = if ($e.ContainsKey("POSTGRES_DB")) { $e["POSTGRES_DB"] } else { "student_hub" }
$pgUser = if ($e.ContainsKey("POSTGRES_USER")) { $e["POSTGRES_USER"] } else { "student_hub_app" }
$admin = if ($e.ContainsKey("BOOTSTRAP_ADMIN_EMAIL")) { $e["BOOTSTRAP_ADMIN_EMAIL"] } else { "admin@example.org" }

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " STUDENT HUB IS READY" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host " Web UI:           http://localhost:$appPort/"
Write-Host " Swagger UI:       http://localhost:$appPort/swagger-ui.html"
Write-Host " REST API:         http://localhost:$appPort/api"
Write-Host " OpenAPI JSON:     http://localhost:$appPort/v3/api-docs"
Write-Host " Health:           http://localhost:$appPort/actuator/health"
Write-Host " JavaFX API:       http://localhost:$appPort/api"
Write-Host " Android emulator: http://10.0.2.2:$appPort/api"
Write-Host " PostgreSQL JDBC:  jdbc:postgresql://localhost:$pgPort/$pgDb"
Write-Host " PostgreSQL user:  $pgUser"
Write-Host " Admin login:      $admin"
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Passwords/secrets are not printed." -ForegroundColor DarkGray
Write-Host ""

& docker compose -f $ComposePath ps
