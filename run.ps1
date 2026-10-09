# Запуск JavaFX-клиента КР2 (Windows PowerShell):
#   powershell -ExecutionPolicy Bypass -File .\run.ps1
# Нужны: JDK 21 (переменная JAVA_HOME), запущенная БД (docker compose up -d --wait)
# и файл src/main/resources/database.properties (см. README).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not $env:JAVA_HOME) {
    Write-Host "Не задана переменная JAVA_HOME. Установите JDK 21 и укажите путь к нему в JAVA_HOME." -ForegroundColor Red
    exit 1
}
if (-not (Test-Path "src\main\resources\database.properties")) {
    Write-Host "Нет файла src\main\resources\database.properties." -ForegroundColor Red
    Write-Host "Скопируйте database.properties.example в database.properties и укажите параметры БД." -ForegroundColor Yellow
    exit 1
}

.\mvnw.cmd -q javafx:run
