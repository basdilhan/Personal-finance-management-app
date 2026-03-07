@echo off
REM DreamSaver App Launcher
REM This script starts the emulator and installs the app

echo ========================================
echo DreamSaver - Quick Launch
echo ========================================
echo.

set ANDROID_HOME=C:\Users\samudu\AppData\Local\Android\Sdk
set ADB=%ANDROID_HOME%\platform-tools\adb.exe
set EMULATOR=%ANDROID_HOME%\emulator\emulator.exe

echo [1/4] Starting Android Emulator...
start "" "%EMULATOR%" -avd Pixel_9_Pro_2 -no-snapshot-load
echo Emulator started!
echo.

echo [2/4] Waiting for emulator to boot (70 seconds)...
timeout /t 70 /nobreak > nul
echo.

echo [3/4] Waiting for device to be ready...
"%ADB%" wait-for-device
"%ADB%" shell "while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done"
echo Device is ready!
echo.

echo [4/4] Installing and launching app...
"%ADB%" install -r app\build\outputs\apk\debug\app-debug.apk
echo.

echo Launching app...
"%ADB%" shell am start -n com.team.financeapp/.HomeActivity
echo.

echo ========================================
echo DreamSaver launched successfully!
echo Check your emulator window.
echo ========================================
echo.
pause
