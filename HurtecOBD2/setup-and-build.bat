@echo off
echo ========================================
echo    Hurtec OBD-II Build Environment Setup
echo ========================================

REM Set Android SDK environment variables
set ANDROID_HOME=C:\Users\meher\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\meher\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%ANDROID_HOME%\cmdline-tools\latest\bin

echo.
echo Environment Variables:
echo ANDROID_HOME: %ANDROID_HOME%
echo ANDROID_SDK_ROOT: %ANDROID_SDK_ROOT%
echo JAVA_HOME: %JAVA_HOME%

echo.
echo Checking Java version...
java -version

echo.
echo Checking Gradle version...
gradlew.bat --version

echo.
echo Checking Android SDK components...
if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo ✓ ADB found
) else (
    echo ✗ ADB not found - Please install Android SDK Platform Tools
)

if exist "%ANDROID_HOME%\build-tools" (
    echo ✓ Build tools directory found
    dir "%ANDROID_HOME%\build-tools" /B
) else (
    echo ✗ Build tools not found
)

if exist "%ANDROID_HOME%\platforms" (
    echo ✓ Platforms directory found
    dir "%ANDROID_HOME%\platforms" /B
) else (
    echo ✗ Platforms not found
)

echo.
echo ========================================
echo    Starting Build Process
echo ========================================

echo.
echo Cleaning previous builds...
gradlew.bat clean

echo.
echo Building debug APK...
gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo    BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo Your Hurtec OBD-II APK has been built successfully!
    echo Location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo Next steps:
    echo 1. Install on Android device: adb install app\build\outputs\apk\debug\app-debug.apk
    echo 2. Or copy APK to device and install manually
    echo 3. Start developing your modern UI!
    echo.
) else (
    echo.
    echo ========================================
    echo    BUILD FAILED!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo Common issues:
    echo 1. Missing Android SDK components
    echo 2. Incorrect ANDROID_HOME path
    echo 3. Missing Java JDK
    echo 4. Network connectivity issues
    echo.
)

pause
