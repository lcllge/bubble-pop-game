@echo off
echo Installing debug APK...
set ADB=C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe

%ADB% devices
echo.
%ADB% install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Installation successful!
    echo To start debugging, use Android Studio:
    echo   1. Open project in Android Studio
    echo   2. Click "Debug" button (bug icon)
    echo   3. Set breakpoints in your code
    echo ========================================
) else (
    echo.
    echo Installation failed! Make sure:
    echo   1. Device/emulator is connected
    echo   2. USB debugging is enabled
    echo   3. APK is built (run: gradlew.bat assembleDebug)
)

echo.
pause
