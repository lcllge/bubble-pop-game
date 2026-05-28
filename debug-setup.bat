@echo off
echo ========================================
echo Android Debug Environment Setup
echo ========================================
echo.

REM Set ADB path
set ADB=C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe

echo [1/3] Checking ADB...
%ADB% version
echo.

echo [2/3] Checking connected devices...
%ADB% devices
echo.

echo [3/3] Building debug APK...
call gradlew.bat assembleDebug
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ========================================
    echo Build successful!
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
    echo ========================================
    echo.
    echo To install on device:
    echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To start debugging:
    echo   1. Connect Android device via USB (enable USB debugging)
    echo   2. Or start Android Emulator
    echo   3. Run: adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo   4. Use Android Studio or VS Code to attach debugger
) else (
    echo Build failed! Please check the errors above.
)

echo.
pause
