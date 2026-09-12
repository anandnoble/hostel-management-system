$githubBase = "https://raw.githubusercontent.com/gradle/gradle/v8.2.0"
New-Item -ItemType Directory -Force -Path ".\gradle\wrapper"

Write-Host "Downloading gradlew..."
curl.exe -L -o ".\gradlew" "$githubBase/gradlew"

Write-Host "Downloading gradlew.bat..."
curl.exe -L -o ".\gradlew.bat" "$githubBase/gradlew.bat"

Write-Host "Downloading gradle-wrapper.jar..."
curl.exe -L -o ".\gradle\wrapper\gradle-wrapper.jar" "$githubBase/gradle/wrapper/gradle-wrapper.jar"

Write-Host "Creating gradle-wrapper.properties..."
$propertiesContent = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
Set-Content -Path ".\gradle\wrapper\gradle-wrapper.properties" -Value $propertiesContent

Write-Host "Gradle Wrapper downloaded!"
