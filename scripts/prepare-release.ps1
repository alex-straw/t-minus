param(
    [string]$OutputDirectory = "release"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$keystoreProperties = Join-Path $projectRoot "keystore.properties"
$apkSource = Join-Path $projectRoot "app\build\outputs\apk\release\app-release.apk"
$outputPath = Join-Path $projectRoot $OutputDirectory

if (-not (Test-Path -LiteralPath $keystoreProperties)) {
    throw "keystore.properties is required for a signed release build."
}

Push-Location $projectRoot
try {
    & .\gradlew.bat clean test lint assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle release verification failed."
    }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $apkSource)) {
    throw "Signed release APK was not produced at $apkSource"
}

New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$apkDestination = Join-Path $outputPath "T.apk"
$checksumDestination = Join-Path $outputPath "T.apk.sha256"
Copy-Item -LiteralPath $apkSource -Destination $apkDestination -Force
$hash = (Get-FileHash -LiteralPath $apkDestination -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath $checksumDestination -Value "$hash  T.apk" -Encoding ascii

Write-Output "Created $apkDestination"
Write-Output "Created $checksumDestination"
