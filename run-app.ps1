#!/usr/bin/env powershell

# Android SDK path
$ANDROID_HOME = "C:\Users\samudu\AppData\Local\Android\Sdk"
$ADB = "$ANDROID_HOME\platform-tools\adb.exe"
$EMULATOR = "$ANDROID_HOME\emulator\emulator.exe"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Android App Runner for VS Code" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Step 1: Start emulator
Write-Host "`n[1/5] Starting emulator..." -ForegroundColor Yellow
& $EMULATOR -avd Pixel_7_Pro -no-snapshot-load -no-audio | Out-Null &

# Step 2: Wait for emulator
Write-Host "[2/5] Waiting 15 seconds for emulator to boot..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Step 3: Check connection
Write-Host "[3/5] Checking device connection..." -ForegroundColor Yellow
& $ADB devices

# Step 4: Build app
Write-Host "`n[4/5] Building Android app..." -ForegroundColor Yellow
& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "[5/5] Installing and running app..." -ForegroundColor Yellow
    
    # Install APK
    & $ADB install -r app\build\outputs\apk\debug\app-debug.apk
    
    # Launch app
    & $ADB shell am start -n com.team.financeapp/.HomeActivity
    
    Write-Host "`n✓ App is launching on emulator!" -ForegroundColor Green
} else {
    Write-Host "`n✗ Build failed!" -ForegroundColor Red
}

Write-Host "`nPress any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
