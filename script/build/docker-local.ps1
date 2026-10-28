[CmdletBinding()]
param(
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
$serviceScript = Join-Path $repoRoot "translationtoolservice\docker-local.ps1"

if (!(Test-Path -LiteralPath $serviceScript -PathType Leaf)) {
    throw "Backend startup script was not found: $serviceScript"
}

if ($Build) {
    & $serviceScript -Build
} else {
    & $serviceScript
}
if ($LASTEXITCODE -ne 0) {
    throw "Backend startup failed with exit code $LASTEXITCODE."
}
