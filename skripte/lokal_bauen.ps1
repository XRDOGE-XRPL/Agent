$ErrorActionPreference = "Stop"
$wurzel = Split-Path -Parent $PSScriptRoot
Set-Location $wurzel

if (-not $env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT = "C:\Android\android-sdk"
}
$sdk = $env:ANDROID_SDK_ROOT
@"
sdk.dir=$($sdk -replace '\\', '\\')
"@ | Set-Content -Path (Join-Path $wurzel "local.properties") -Encoding ASCII

Write-Host "=== Nativer Kern (CMake/CTest) ==="
if (Test-Path (Join-Path $wurzel "build-native")) {
    Remove-Item -Recurse -Force (Join-Path $wurzel "build-native")
}
cmake -S (Join-Path $wurzel "native") -B (Join-Path $wurzel "build-native") -G "MinGW Makefiles"
if ($LASTEXITCODE -ne 0) { throw "CMake-Konfiguration fehlgeschlagen" }
cmake --build (Join-Path $wurzel "build-native")
if ($LASTEXITCODE -ne 0) { throw "CMake-Build fehlgeschlagen" }
ctest --test-dir (Join-Path $wurzel "build-native") --output-on-failure
if ($LASTEXITCODE -ne 0) { throw "CTest fehlgeschlagen" }

Write-Host "=== Android-Unit-Tests ==="
if (Get-Command gradle -ErrorAction SilentlyContinue) {
    gradle -p $wurzel :app:testDebugUnitTest --no-daemon
} else {
    Write-Host "Gradle nicht gefunden – Unit-Tests übersprungen"
}

Write-Host "Lokale Validierung abgeschlossen."
