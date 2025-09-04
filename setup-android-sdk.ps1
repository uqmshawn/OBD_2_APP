# Android SDK Setup Script for Hurtec OBD-II Development
# Run this script as Administrator

Write-Host "Setting up Android SDK for Hurtec OBD-II Development..." -ForegroundColor Green

# Create Android SDK directory
$androidSdkPath = "C:\Android\sdk"
$cmdlineToolsPath = "$androidSdkPath\cmdline-tools\latest"

if (!(Test-Path $androidSdkPath)) {
    New-Item -ItemType Directory -Path $androidSdkPath -Force
    Write-Host "Created Android SDK directory: $androidSdkPath" -ForegroundColor Yellow
}

# Download Android Command Line Tools
$cmdlineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$cmdlineToolsZip = "$env:TEMP\commandlinetools-win-latest.zip"

Write-Host "Downloading Android Command Line Tools..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $cmdlineToolsUrl -OutFile $cmdlineToolsZip -UseBasicParsing
    Write-Host "Download completed!" -ForegroundColor Green
} catch {
    Write-Host "Failed to download command line tools: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Extract command line tools
Write-Host "Extracting command line tools..." -ForegroundColor Yellow
try {
    Expand-Archive -Path $cmdlineToolsZip -DestinationPath "$androidSdkPath\temp" -Force
    
    # Move to correct location
    if (!(Test-Path "$androidSdkPath\cmdline-tools")) {
        New-Item -ItemType Directory -Path "$androidSdkPath\cmdline-tools" -Force
    }
    
    Move-Item -Path "$androidSdkPath\temp\cmdline-tools" -Destination "$androidSdkPath\cmdline-tools\latest" -Force
    Remove-Item -Path "$androidSdkPath\temp" -Recurse -Force
    Remove-Item -Path $cmdlineToolsZip -Force
    
    Write-Host "Command line tools extracted successfully!" -ForegroundColor Green
} catch {
    Write-Host "Failed to extract command line tools: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Set environment variables
Write-Host "Setting up environment variables..." -ForegroundColor Yellow

# Set ANDROID_HOME
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $androidSdkPath, [EnvironmentVariableTarget]::User)
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $androidSdkPath, [EnvironmentVariableTarget]::User)

# Add to PATH
$currentPath = [Environment]::GetEnvironmentVariable("PATH", [EnvironmentVariableTarget]::User)
$pathsToAdd = @(
    "$androidSdkPath\cmdline-tools\latest\bin",
    "$androidSdkPath\platform-tools",
    "$androidSdkPath\build-tools\34.0.0"
)

foreach ($pathToAdd in $pathsToAdd) {
    if ($currentPath -notlike "*$pathToAdd*") {
        $currentPath = "$currentPath;$pathToAdd"
    }
}

[Environment]::SetEnvironmentVariable("PATH", $currentPath, [EnvironmentVariableTarget]::User)

# Refresh environment variables for current session
$env:ANDROID_HOME = $androidSdkPath
$env:ANDROID_SDK_ROOT = $androidSdkPath
$env:PATH = "$env:PATH;$androidSdkPath\cmdline-tools\latest\bin;$androidSdkPath\platform-tools;$androidSdkPath\build-tools\34.0.0"

Write-Host "Environment variables set successfully!" -ForegroundColor Green

# Install required SDK components
Write-Host "Installing required Android SDK components..." -ForegroundColor Yellow

$sdkmanager = "$cmdlineToolsPath\bin\sdkmanager.bat"

if (Test-Path $sdkmanager) {
    # Accept licenses first
    Write-Host "Accepting Android SDK licenses..." -ForegroundColor Yellow
    echo "y" | & $sdkmanager --licenses
    
    # Install required components
    $components = @(
        "platform-tools",
        "build-tools;34.0.0",
        "platforms;android-34",
        "platforms;android-33",
        "platforms;android-24",
        "sources;android-34",
        "extras;android;m2repository",
        "extras;google;m2repository"
    )
    
    foreach ($component in $components) {
        Write-Host "Installing $component..." -ForegroundColor Cyan
        & $sdkmanager $component
    }
    
    Write-Host "Android SDK setup completed successfully!" -ForegroundColor Green
} else {
    Write-Host "SDK Manager not found at: $sdkmanager" -ForegroundColor Red
    exit 1
}

Write-Host "`nSetup Summary:" -ForegroundColor Green
Write-Host "- Android SDK installed at: $androidSdkPath" -ForegroundColor White
Write-Host "- Environment variables configured" -ForegroundColor White
Write-Host "- Required SDK components installed" -ForegroundColor White
Write-Host "`nPlease restart your terminal/IDE to use the new environment variables." -ForegroundColor Yellow
Write-Host "`nNext steps:" -ForegroundColor Green
Write-Host "1. Restart Visual Studio Code" -ForegroundColor White
Write-Host "2. Run: gradlew build" -ForegroundColor White
Write-Host "3. Start developing Hurtec OBD-II!" -ForegroundColor White
