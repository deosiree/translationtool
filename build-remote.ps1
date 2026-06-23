<#
.SYNOPSIS
    远程构建 translation-ui 镜像（绕过 Docker Desktop 凭据管理器问题）
.DESCRIPTION
    通过 DOCKER_CONFIG 环境变量临时切换配置，绕过远程 PowerShell
    会话中 Windows 凭据管理器不可用的问题。不影响本地 GUI 使用。
#>

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Join-Path $ScriptDir "translation"

$dockerfile = Join-Path $ProjectDir "Dockerfile"
if (-not (Test-Path $dockerfile)) {
    Write-Host "[错误] 找不到 Dockerfile: $ProjectDir" -ForegroundColor Red
    exit 1
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   translation-ui 远程构建脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "项目目录: $ProjectDir" -ForegroundColor White

# 临时 Docker 配置（不含 credsStore）
$tempDockerConfigDir = Join-Path $env:TEMP "docker-config-remote"
New-Item -ItemType Directory -Force -Path $tempDockerConfigDir | Out-Null
'{"auths":{}}' | Set-Content (Join-Path $tempDockerConfigDir "config.json") -Force
$env:DOCKER_CONFIG = $tempDockerConfigDir

Write-Host "[1/3] 临时 Docker 配置已准备" -ForegroundColor Green

# 拉取基础镜像
Write-Host "[2/3] 拉取基础镜像 ..." -ForegroundColor Yellow
docker pull --platform linux/amd64 node:16-alpine
docker pull --platform linux/amd64 nginx:alpine

# 构建镜像
Write-Host "[3/3] 开始构建 translation-ui:latest ..." -ForegroundColor Yellow
Set-Location $ProjectDir
docker build -t translation-ui:latest -f Dockerfile .

if ($LASTEXITCODE -eq 0) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  构建成功!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  重启容器命令:" -ForegroundColor Cyan
    Write-Host "  docker compose -f $ProjectDir\docker-compose.yml up -d translation-ui" -ForegroundColor White
} else {
    Write-Host "[错误] 构建失败，错误码: $LASTEXITCODE" -ForegroundColor Red
}
