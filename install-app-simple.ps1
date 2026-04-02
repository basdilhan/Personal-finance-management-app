#!/usr/bin/env powershell
# Installation Script for DreamSaver App
# Automatically installs and launches the app on your Android phone

$ADB = "C:\Android\platform-tools\adb.exe"
$PROJECT = "E:\HDSE25.1F\Mobile\Personal-finance-management-app"
$APK = "$PROJECT\app\build\outputs\apk\debug\app-debug.apk"
$PACKAGE = "com.team.financeapp"

Write-Host "Installing DreamSaver App..." -ForegroundColor Green
Write-Host ""

# Check devices
Write-Host "Checking connected devices..."
& $ADB devices

Write-Host ""
Write-Host "Installing app..."
& $ADB install -r $APK

Write-Host ""
Write-Host "Launching app..."
& $ADB shell am start -n "$PACKAGE/.HomeActivity"

Write-Host ""
Write-Host "Done! Check your phone." -ForegroundColor Green

