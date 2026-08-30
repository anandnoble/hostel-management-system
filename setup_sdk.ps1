$sdkRoot = "D:\New folder\android-sdk"
$zipPath = "D:\New folder\commandlinetools.zip"

if (-not (Test-Path $sdkRoot)) {
    New-Item -ItemType Directory -Force -Path $sdkRoot
}

# Clean up any partial download or extraction
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}

Write-Host "Downloading Android command line tools with curl..."
curl.exe -L -o $zipPath "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

Write-Host "Extracting command line tools..."
$tempExtract = "D:\New folder\android-sdk\temp_cmdline"
if (Test-Path $tempExtract) {
    Remove-Item -Recurse -Force $tempExtract
}
New-Item -ItemType Directory -Force -Path $tempExtract
Expand-Archive -Path $zipPath -DestinationPath $tempExtract

Write-Host "Moving files to latest..."
$latestDir = "D:\New folder\android-sdk\cmdline-tools\latest"
if (Test-Path $latestDir) {
    Remove-Item -Recurse -Force $latestDir
}
New-Item -ItemType Directory -Force -Path $latestDir

Move-Item -Path "$tempExtract\cmdline-tools\*" -Destination $latestDir -Force

Write-Host "Cleaning up zip and temp folders..."
Remove-Item -Recurse -Force $tempExtract
Remove-Item -Force $zipPath

Write-Host "SDK Commandline tools ready at $latestDir"
