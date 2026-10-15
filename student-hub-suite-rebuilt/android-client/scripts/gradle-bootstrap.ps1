param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"

# Версия Gradle и контрольная сумма зафиксированы, чтобы проверять загруженный дистрибутив.
$GradleVersion = "8.13"
$ExpectedSha256 = "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
$DistributionUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"

$CacheRoot = Join-Path $env:LOCALAPPDATA "StudentHub\gradle"
$ZipPath = Join-Path $CacheRoot "gradle-$GradleVersion-bin.zip"
$GradleHome = Join-Path $CacheRoot "gradle-$GradleVersion"
$GradleBat = Join-Path $GradleHome "bin\gradle.bat"

New-Item -ItemType Directory -Force -Path $CacheRoot | Out-Null

function Test-DistributionHash([string]$Path) {
    if (-not (Test-Path $Path)) { return $false }
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
    return $actual -eq $ExpectedSha256
}

if (-not (Test-Path $GradleBat)) {
    if (-not (Test-DistributionHash $ZipPath)) {
        if (Test-Path $ZipPath) { Remove-Item -Force $ZipPath }

        Write-Host "[Student Hub] Downloading Gradle $GradleVersion..." -ForegroundColor Cyan
        Invoke-WebRequest -UseBasicParsing -Uri $DistributionUrl -OutFile $ZipPath

        if (-not (Test-DistributionHash $ZipPath)) {
            Remove-Item -Force $ZipPath -ErrorAction SilentlyContinue
            throw "Gradle distribution SHA-256 mismatch. Download rejected."
        }
    }

    Write-Host "[Student Hub] Extracting Gradle $GradleVersion..." -ForegroundColor Cyan
    if (Test-Path $GradleHome) {
        Remove-Item -Recurse -Force $GradleHome
    }
    Expand-Archive -LiteralPath $ZipPath -DestinationPath $CacheRoot -Force
}

if (-not (Test-Path $GradleBat)) {
    throw "Gradle executable was not found after extraction: $GradleBat"
}

& $GradleBat @GradleArgs
exit $LASTEXITCODE
