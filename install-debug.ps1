# Builds the debug APK, installs it on the connected device and launches it.
# Pass -Serial <id> (see `adb devices`) when more than one device is connected.
param([string]$Serial)

& (Join-Path $PSScriptRoot "gradlew.bat") -p $PSScriptRoot assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Join-Path $PSScriptRoot "app/build/outputs/apk/debug/app-debug.apk"
if (-not (Test-Path $apk)) {
    Write-Error "APK not found at $apk."
    exit 1
}

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb."
    exit 1
}

$adbArgs = @()
if ($Serial) { $adbArgs += @("-s", $Serial) }

& $adb @adbArgs install -r -t $apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $adb @adbArgs shell am start -n com.akreutz.knitting.debug/com.akreutz.knitting.MainActivity
