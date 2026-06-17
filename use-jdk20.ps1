# 切换到 JDK 20（仅当前会话生效）
$env:JAVA_HOME = "C:\Program Files\Java\jdk-20"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "已切换到 JDK 20 ($env:JAVA_HOME)" -ForegroundColor Green
java -version
