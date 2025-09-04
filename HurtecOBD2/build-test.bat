@echo off
echo Setting up Hurtec OBD-II Build Environment...

REM Set Android SDK environment variables
set ANDROID_HOME=C:\Users\meher\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\meher\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%ANDROID_HOME%\cmdline-tools\latest\bin

echo ANDROID_HOME: %ANDROID_HOME%
echo ANDROID_SDK_ROOT: %ANDROID_SDK_ROOT%

echo.
echo Testing Gradle...
gradlew.bat --version

echo.
echo Testing Android SDK...
if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo ✓ ADB found
) else (
    echo ✗ ADB not found
)

echo.
echo Attempting to build project...
gradlew.bat clean

echo.
echo Build test completed!
