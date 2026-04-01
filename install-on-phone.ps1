#!/usr/bin/env powershell

# Script to install and run the Personal Finance App on Android phone
# Make sure your phone is connected via USB with USB Debugging enabled

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Mobile Phone App Installer" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Common ADB paths
$possiblePaths = @(
    "C:\Android\platform-tools\adb.exe",
    "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "C:\Program Files\Android\Android Studio\platform-tools\adb.exe"
)

if ($env:ANDROID_HOME) {
    $possiblePaths += "$env:ANDROID_HOME\platform-tools\adb.exe"
}

$ADB = $null
foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $ADB = $path
        Write-Host "[+] Found adb at: $path" -ForegroundColor Green
        break
    }
}

if ($null -eq $ADB) {
    Write-Host "`n[-] ADB not found!" -ForegroundColor Red
    Write-Host "`nPlease download and install Android SDK Platform Tools:" -ForegroundColor Yellow
    Write-Host "https://developer.android.com/tools/releases/platform-tools" -ForegroundColor Cyan
    Write-Host "`nAfter installation, one of these paths should exist:" -ForegroundColor Yellow
    $possiblePaths | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

# Step 1: Check for connected devices
Write-Host "`n[1/4] Checking for connected devices..." -ForegroundColor Yellow
$devices = & $ADB devices | Select-Object -Skip 1 | Where-Object { $_.Trim() -and -not $_.StartsWith("List") }

if ($devices.Count -eq 0) {
    Write-Host "[-] No devices found!" -ForegroundColor Red
    Write-Host "`nPlease:" -ForegroundColor Yellow
    Write-Host "  1. Connect your Android phone via USB"
    Write-Host "  2. Enable USB Debugging: Settings -> Developer options -> USB Debugging"
    Write-Host "  3. Tap OK when prompted on your phone"
    exit 1
}

Write-Host "[+] Device(s) found:" -ForegroundColor Green
& $ADB devices | Select-Object -Skip 1

# Step 2: Check APK exists
Write-Host "`n[2/4] Checking for APK file..." -ForegroundColor Yellow
$APK = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $APK)) {
    Write-Host "[-] APK not found at: $APK" -ForegroundColor Red
    Write-Host "`nPlease build the project first:" -ForegroundColor Yellow
    Write-Host "  .\gradlew.bat build -x lint" -ForegroundColor Cyan
    exit 1
}
Write-Host "[+] APK found: $APK" -ForegroundColor Green

# Step 3: Install APK
Write-Host "`n[3/4] Installing app on device..." -ForegroundColor Yellow
& $ADB install -r $APK

if ($LASTEXITCODE -ne 0) {
    Write-Host "[-] Installation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "[+] Installation successful!" -ForegroundColor Green

# Step 4: Launch app
Write-Host "`n[4/4] Launching app..." -ForegroundColor Yellow
& $ADB shell am start -n com.team.financeapp/.HomeActivity

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "[+] App installed and launched!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`nThe app should now be running on your phone."
Write-Host "Go to the home screen and look for the app, or check the activity stack."





