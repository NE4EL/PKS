$ErrorActionPreference = "Stop"

$api = if ($env:STUDENT_HUB_API) {
    $env:STUDENT_HUB_API
} else {
    "http://127.0.0.1:8080/api"
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven не найден в PATH. Запустите javafx:run из Maven Tool Window IntelliJ IDEA или установите Maven 3.9+."
}

Write-Host "Student Hub JavaFX" -ForegroundColor Cyan
Write-Host "REST API: $api"

mvn "-Dstudenthub.api=$api" javafx:run
