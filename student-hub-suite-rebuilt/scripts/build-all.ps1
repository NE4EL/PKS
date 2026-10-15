$ErrorActionPreference = "Stop"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    Push-Location backend
    try { .\mvnw.cmd -B -ntp clean verify } finally { Pop-Location }
    Push-Location javafx-client
    try { .\mvnw.cmd -B -ntp clean verify } finally { Pop-Location }
    Push-Location android-client
    try { .\gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug } finally { Pop-Location }
    python scripts\verify.py --build
} finally { Pop-Location }
