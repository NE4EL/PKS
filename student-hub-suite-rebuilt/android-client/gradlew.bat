@echo off
setlocal
rem -----------------------------------------------------------------------------
rem Student Hub Gradle bootstrap for Windows.
rem
rem A global Gradle installation is not required. On first run the script downloads
rem the fixed Gradle distribution, verifies SHA-256 and then reuses the local copy.
rem -----------------------------------------------------------------------------

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\gradle-bootstrap.ps1" %*
exit /b %ERRORLEVEL%
