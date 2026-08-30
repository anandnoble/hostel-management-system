$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$sdkmanager = "D:\New folder\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat"
$sdkRoot = "D:\New folder\android-sdk"

Write-Host "Accepting licenses..."
# Accept all licenses first
$licensesProcess = Start-Process -FilePath $sdkmanager -ArgumentList "--sdk_root=$sdkRoot", "--licenses" -RedirectStandardInput "D:\New folder\licenses_input.txt" -NoNewWindow -PassThru -Wait

Write-Host "Installing SDK components..."
# We pipe 'y' to accept any prompts during installation
$installProcess = powershell -Command "echo y | & '$sdkmanager' --sdk_root='$sdkRoot' 'platform-tools' 'build-tools;34.0.0' 'platforms;android-34'"

Write-Host "SDK components installed successfully!"
