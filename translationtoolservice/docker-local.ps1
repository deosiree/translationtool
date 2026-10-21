param(
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$serviceRoot = $PSScriptRoot
$composeFile = Join-Path $repoRoot "docker-compose.yml"
$envFile = Join-Path $serviceRoot ".env.local"
$appJar = Join-Path $serviceRoot "target\app.jar"

if (!(Test-Path $envFile)) {
    throw "Missing local config: $envFile"
}

if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI was not found. Start Docker Desktop and check PATH."
}

$null = & docker info --format "{{.ServerVersion}}" 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop is not running. Start it and retry."
}

$imageName = "translationtool-local-java-backend:latest"
$null = & docker image inspect $imageName 2>&1
$imageMissing = $LASTEXITCODE -ne 0
$buildArtifacts = $Build -or $imageMissing -or !(Test-Path $appJar)

if ($buildArtifacts) {
    $javaHome8 = @(
        $env:JAVA_HOME_8_X64,
        $env:JAVA8_HOME,
        (Join-Path $env:LOCALAPPDATA "Programs\Eclipse Adoptium\jdk-8.0.442.6-hotspot")
    ) | Where-Object {
        $_ -and (Test-Path (Join-Path $_ "bin\javac.exe"))
    } | Select-Object -First 1

    if (!$javaHome8) {
        throw "JDK 8 was not found. Set JAVA_HOME_8_X64 or JAVA8_HOME to a JDK 8 directory."
    }
    if (!(Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "Maven was not found. Install Maven and check PATH."
    }

    $env:JAVA_HOME = $javaHome8
    $env:Path = "$javaHome8\bin;$env:Path"
    & mvn -B -f (Join-Path $serviceRoot "pom.xml") -Pdocker-local -DskipTests clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Java package build failed with exit code $LASTEXITCODE."
    }

    $jar = Get-ChildItem -LiteralPath (Join-Path $serviceRoot "target") -Filter "*.jar" -File |
        Where-Object { $_.Name -notlike "*.original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (!$jar) {
        throw "Spring Boot jar was not found under $serviceRoot\target."
    }
    Copy-Item -LiteralPath $jar.FullName -Destination $appJar -Force

    $jarEntries = & (Join-Path $javaHome8 "bin\jar.exe") tf $appJar
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect the Spring Boot jar."
    }
    if ($jarEntries -notcontains "BOOT-INF/classes/application.yml") {
        throw "The Docker jar is missing its classpath config marker."
    }
}

$composeArgs = @(
    "--env-file", $envFile,
    "-f", $composeFile,
    "--project-directory", $repoRoot,
    "up", "-d"
)
if ($buildArtifacts) {
    $composeArgs += "--build"
} else {
    $composeArgs += "--no-build"
}
$composeArgs += "java-backend"

& docker compose @composeArgs
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed with exit code $LASTEXITCODE."
}

$readyUri = "http://127.0.0.1:18001/userLogin/login"
$readyHeaders = @{
    Origin = "http://localhost:18000"
    "Access-Control-Request-Method" = "POST"
    "Access-Control-Request-Headers" = "content-type,token"
}
$ready = $false
$lastReadinessError = $null
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri $readyUri -Method Options -Headers $readyHeaders -TimeoutSec 2
        $allowOrigin = [string]$response.Headers["Access-Control-Allow-Origin"]
        $allowMethods = ([string]$response.Headers["Access-Control-Allow-Methods"]).ToLowerInvariant()
        $allowHeaders = ([string]$response.Headers["Access-Control-Allow-Headers"]).ToLowerInvariant()
        if (
            $response.StatusCode -eq 200 -and
            ($allowOrigin -eq $readyHeaders.Origin -or $allowOrigin -eq "*") -and
            $allowMethods.Contains("post") -and
            $allowHeaders.Contains("content-type") -and
            $allowHeaders.Contains("token")
        ) {
            $ready = $true
            break
        }
    } catch {
        $lastReadinessError = $_.Exception.GetType().Name
    }
    Start-Sleep -Seconds 1
}
if (!$ready) {
    throw "Java backend did not pass the HTTP CORS readiness check within 60 seconds. Last HTTP error: $lastReadinessError. Check Docker Compose logs."
}

Write-Host "Java backend is ready and CORS preflight passed."
& docker compose --env-file $envFile -f $composeFile --project-directory $repoRoot ps
