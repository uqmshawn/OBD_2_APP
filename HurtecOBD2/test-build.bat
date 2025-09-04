@echo off
echo Testing Hurtec OBD-II Build...

set ANDROID_HOME=C:\Users\meher\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\meher\AppData\Local\Android\Sdk

echo Environment:
echo ANDROID_HOME: %ANDROID_HOME%
echo JAVA_HOME: %JAVA_HOME%

echo.
echo Testing Gradle...
gradlew.bat --version

echo.
echo Attempting build...
gradlew.bat clean

echo.
echo Build test completed.
pause
