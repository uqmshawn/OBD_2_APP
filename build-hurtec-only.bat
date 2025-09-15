@echo off
echo ========================================
echo Building Hurtec OBD-II App (Isolated)
echo ========================================

cd HurtecOBD2

echo.
echo Cleaning previous build...
call gradlew clean

echo.
echo Compiling Kotlin sources...
call gradlew compileDebugKotlin

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Kotlin compilation failed!
    pause
    exit /b 1
)

echo.
echo Building debug APK...
call gradlew assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo ❌ APK build failed!
    pause
    exit /b 1
)

echo.
echo ✅ Build successful!
echo APK location: HurtecOBD2\app\build\outputs\apk\debug\app-debug.apk
echo.
pause
