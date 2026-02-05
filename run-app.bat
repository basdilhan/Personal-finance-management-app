@echo off
REM Set Android SDK path
set ANDROID_HOME=C:\Users\samudu\AppData\Local\Android\Sdk

REM Start emulator in background
echo Starting emulator...
start "" "%ANDROID_HOME%\emulator\emulator.exe" -avd Pixel_7_Pro -no-snapshot-load

REM Wait for emulator to start
echo Waiting for emulator to boot...
timeout /t 10

REM Check if device is connected
"%ANDROID_HOME%\platform-tools\adb.exe" devices

REM Build the app
echo Building Android app...
call gradlew.bat assembleDebug

REM Install on emulator
echo Installing app on emulator...
"%ANDROID_HOME%\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk

REM Launch the app
echo Launching app...
"%ANDROID_HOME%\platform-tools\adb.exe" shell am start -n com.team.financeapp/.HomeActivity

echo Done! App should be running on emulator.
pause
