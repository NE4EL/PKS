param(
    [string]$AdminEmail = "admin@example.org",
    [string]$AdminName = "System Administrator",
    [int]$Port = 8080,
    [int]$PostgresPort = 5432,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$EnvPath = Join-Path $PSScriptRoot ".env"

if ((Test-Path $EnvPath) -and -not $Force) {
    throw ".env already exists. Use -Force to replace it."
}

function New-RandomBytes([int]$Length) {
    $bytes = New-Object byte[] $Length
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return $bytes
}

function New-Base64Secret([int]$Length) {
    return [Convert]::ToBase64String((New-RandomBytes $Length))
}

# Пароль БД и JWT-секрет создаются криптографически стойким генератором.
$DbPassword = New-Base64Secret 36
$JwtSecret = New-Base64Secret 64
$RandomPasswordPart = ([Convert]::ToHexString((New-RandomBytes 18))).ToLowerInvariant()
$AdminPassword = "Aa1!$RandomPasswordPart"

if ($AdminEmail -match "[\r\n=]") { throw "AdminEmail contains invalid .env characters." }
if ($AdminName -match "[\r\n]") { throw "AdminName must be a single line." }
$AdminNameEscaped = $AdminName.Replace('\', '\\').Replace('"', '\"')

$content = @"
POSTGRES_DB=student_hub
POSTGRES_USER=student_hub_app
POSTGRES_PASSWORD=$DbPassword
POSTGRES_PORT=$PostgresPort
JWT_SECRET_BASE64=$JwtSecret
JWT_ISSUER=student-hub
ACCESS_TOKEN_MINUTES=15
REFRESH_TOKEN_DAYS=14
BOOTSTRAP_ADMIN_EMAIL=$AdminEmail
BOOTSTRAP_ADMIN_NAME="$AdminNameEscaped"
BOOTSTRAP_ADMIN_PASSWORD=$AdminPassword
CORS_ALLOWED_ORIGINS=http://localhost:$Port
COOKIE_SECURE=false
PERMANENT_DELETE_ENABLED=false
APP_PORT=$Port
"@

[IO.File]::WriteAllText($EnvPath, $content, (New-Object Text.UTF8Encoding($false)))

Write-Host "" 
Write-Host "Created: $EnvPath" -ForegroundColor Green
Write-Host "Bootstrap admin email: $AdminEmail" -ForegroundColor Cyan
Write-Host "Temporary admin password: $AdminPassword" -ForegroundColor Yellow
Write-Host "" 
Write-Host "Connection addresses:" -ForegroundColor Cyan
Write-Host "  Web UI:           http://localhost:$Port/"
Write-Host "  Swagger UI:       http://localhost:$Port/swagger-ui.html"
Write-Host "  REST API:         http://localhost:$Port/api"
Write-Host "  Health:           http://localhost:$Port/actuator/health"
Write-Host "  JavaFX API:       http://localhost:$Port/api"
Write-Host "  Android emulator: http://10.0.2.2:$Port/api"
Write-Host "  PostgreSQL JDBC:  jdbc:postgresql://localhost:$PostgresPort/student_hub"
Write-Host "  PostgreSQL user:  student_hub_app"
Write-Host "" 
Write-Host "The backend requires the temporary admin password to be changed after first sign-in." -ForegroundColor Yellow
Write-Host "Do not commit or send the .env file to anyone." -ForegroundColor Red
