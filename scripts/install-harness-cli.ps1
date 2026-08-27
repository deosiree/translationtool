param(
    [string]$BaseUrl = $env:HARNESS_CLI_BASE_URL
)

$ErrorActionPreference = "Stop"
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$binDir = Join-Path $root "scripts/bin"
$releaseTagFile = Join-Path $root "scripts/harness-cli-release-tag"

if (!(Test-Path $releaseTagFile)) {
    throw "pinned release file is missing: $releaseTagFile"
}
$releaseTag = (Get-Content -LiteralPath $releaseTagFile | Where-Object {
    $_ -match "\S" -and $_ -notmatch "^\s*#"
} | Select-Object -First 1).Trim()
if (!$releaseTag.StartsWith("harness-cli-v")) {
    throw "invalid pinned release tag: $releaseTag"
}

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = "https://github.com/hoangnb24/repository-harness/releases/download/$releaseTag"
}

$asset = switch ($true) {
    $IsWindows { "harness-cli-windows-x64.exe" }
    $IsMacOS   { "harness-cli-macos-arm64" }
    $IsLinux   { "harness-cli-linux-x64" }
    default    { throw "unsupported platform" }
}
$destName = if ($IsWindows) { "harness-cli.exe" } else { "harness-cli" }
$dest = Join-Path $binDir $destName
$sumFile = "$asset.sha256"

New-Item -ItemType Directory -Force -Path $binDir | Out-Null
$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("harness-cli-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

try {
    $tmpExe = Join-Path $tmp $asset
    $tmpSum = Join-Path $tmp $sumFile

    Invoke-WebRequest -Uri "$BaseUrl/$asset" -OutFile $tmpExe -UseBasicParsing -TimeoutSec 120
    Invoke-WebRequest -Uri "$BaseUrl/$sumFile" -OutFile $tmpSum -UseBasicParsing -TimeoutSec 60

    $sumText = (Get-Content -LiteralPath $tmpSum -Raw).Trim()
    $tokens = $sumText -split "\s+"
    $expected = $tokens[0]
    $actual = (Get-FileHash -LiteralPath $tmpExe -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected.ToLowerInvariant()) {
        throw "checksum mismatch for $asset (expected $expected, got $actual)"
    }

    Copy-Item -LiteralPath $tmpExe -Destination $dest -Force
    Write-Host "Harness CLI installed: $dest (release $releaseTag)"
} finally {
    Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
}