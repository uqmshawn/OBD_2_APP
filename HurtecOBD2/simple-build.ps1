# Simple PowerShell build script for Hurtec OBD-II
Write-Host "Building Hurtec OBD-II..." -ForegroundColor Green

# Set environment variables
$env:ANDROID_HOME = "C:\Users\meher\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Users\meher\AppData\Local\Android\Sdk"

Write-Host "Environment set:" -ForegroundColor Yellow
Write-Host "ANDROID_HOME: $env:ANDROID_HOME"
Write-Host "ANDROID_SDK_ROOT: $env:ANDROID_SDK_ROOT"

# Change to project directory
Set-Location -Path "."

Write-Host "`nTesting Gradle wrapper..." -ForegroundColor Yellow
try {
    & .\gradlew.bat --version
    Write-Host "Gradle wrapper working!" -ForegroundColor Green
} catch {
    Write-Host "Gradle wrapper failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`nCleaning project..." -ForegroundColor Yellow
try {
    & .\gradlew.bat clean
    Write-Host "Clean successful!" -ForegroundColor Green
} catch {
    Write-Host "Clean failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Error details: $($Error[0])" -ForegroundColor Red
}

Write-Host "`nBuilding debug APK..." -ForegroundColor Yellow
try {
    & .\gradlew.bat assembleDebug
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "APK location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
} catch {
    Write-Host "Build failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Error details: $($Error[0])" -ForegroundColor Red
}

Write-Host "`nBuild process completed." -ForegroundColor Green
