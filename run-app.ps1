#!/usr/bin/env powershell

# Android SDK path
$ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$ADB = "$ANDROID_HOME\platform-tools\adb.exe"
$EMULATOR = "$ANDROID_HOME\emulator\emulator.exe"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Android App Runner for VS Code" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Step 1: Check connection
Write-Host "`n[1/5] Checking device connection..." -ForegroundColor Yellow
& $ADB devices

# Step 2: Build app
Write-Host "`n[2/5] Building Android app..." -ForegroundColor Yellow
& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "[3/5] Installing and running app..." -ForegroundColor Yellow

    # Install APK
    & $ADB install -r app\build\outputs\apk\debug\app-debug.apk
    
    # Launch app
    & $ADB shell am start -n com.team.financeapp/.HomeActivity
    
    Write-Host "`nApp is launching on emulator!" -ForegroundColor Green
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
}

Write-Host "`nScript completed." -ForegroundColor Gray
