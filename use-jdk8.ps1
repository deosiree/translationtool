# 切换到 JDK 8（仅当前会话生效）
$env:JAVA_HOME = "C:\Users\Administrator\AppData\Local\Programs\Eclipse Adoptium\jdk-8.0.442.6-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "已切换到 JDK 8 ($env:JAVA_HOME)" -ForegroundColor Green
java -version
