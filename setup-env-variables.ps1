# Set up Android SDK environment variables for existing installation
# Run this script as Administrator or with elevated permissions

Write-Host "Setting up Android SDK environment variables..." -ForegroundColor Green

$androidSdkPath = "C:\Users\meher\AppData\Local\Android\Sdk"

# Verify SDK exists
if (!(Test-Path $androidSdkPath)) {
    Write-Host "Android SDK not found at: $androidSdkPath" -ForegroundColor Red
    exit 1
}

Write-Host "Found Android SDK at: $androidSdkPath" -ForegroundColor Green

# Set environment variables
Write-Host "Setting up environment variables..." -ForegroundColor Yellow

# Set ANDROID_HOME and ANDROID_SDK_ROOT
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $androidSdkPath, [EnvironmentVariableTarget]::User)
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $androidSdkPath, [EnvironmentVariableTarget]::User)

# Add to PATH
$currentPath = [Environment]::GetEnvironmentVariable("PATH", [EnvironmentVariableTarget]::User)
$pathsToAdd = @(
    "$androidSdkPath\cmdline-tools\latest\bin",
    "$androidSdkPath\platform-tools",
    "$androidSdkPath\build-tools\34.0.0",
    "$androidSdkPath\tools",
    "$androidSdkPath\tools\bin"
)

foreach ($pathToAdd in $pathsToAdd) {
    if ($currentPath -notlike "*$pathToAdd*") {
        $currentPath = "$currentPath;$pathToAdd"
    }
}

[Environment]::SetEnvironmentVariable("PATH", $currentPath, [EnvironmentVariableTarget]::User)

# Set for current session
$env:ANDROID_HOME = $androidSdkPath
$env:ANDROID_SDK_ROOT = $androidSdkPath
$env:PATH = "$env:PATH;$androidSdkPath\cmdline-tools\latest\bin;$androidSdkPath\platform-tools;$androidSdkPath\build-tools\34.0.0;$androidSdkPath\tools;$androidSdkPath\tools\bin"

Write-Host "Environment variables set successfully!" -ForegroundColor Green

# Check what's installed
Write-Host "`nChecking installed SDK components..." -ForegroundColor Yellow

$platformToolsPath = "$androidSdkPath\platform-tools"
$buildToolsPath = "$androidSdkPath\build-tools"
$platformsPath = "$androidSdkPath\platforms"

if (Test-Path $platformToolsPath) {
    Write-Host "✓ Platform tools found" -ForegroundColor Green
} else {
    Write-Host "✗ Platform tools missing" -ForegroundColor Red
}

if (Test-Path $buildToolsPath) {
    $buildToolsVersions = Get-ChildItem $buildToolsPath | Select-Object -ExpandProperty Name
    Write-Host "✓ Build tools found: $($buildToolsVersions -join ', ')" -ForegroundColor Green
} else {
    Write-Host "✗ Build tools missing" -ForegroundColor Red
}

if (Test-Path $platformsPath) {
    $platformVersions = Get-ChildItem $platformsPath | Select-Object -ExpandProperty Name
    Write-Host "✓ Platforms found: $($platformVersions -join ', ')" -ForegroundColor Green
} else {
    Write-Host "✗ Platforms missing" -ForegroundColor Red
}

Write-Host "`nEnvironment setup completed!" -ForegroundColor Green
Write-Host "Please restart your terminal/IDE to use the new environment variables." -ForegroundColor Yellow
