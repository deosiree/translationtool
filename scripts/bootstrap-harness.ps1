param(
    [string]$Database = $env:HARNESS_DB_PATH,
    [string]$Cli = $env:HARNESS_CLI
)

$ErrorActionPreference = "Stop"
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
if ([string]::IsNullOrWhiteSpace($Database)) {
    $Database = Join-Path $root "harness.db"
}
if ([string]::IsNullOrWhiteSpace($Cli)) {
    $Cli = Join-Path $root "scripts/bin/harness-cli.exe"
}
$Database = [System.IO.Path]::GetFullPath($Database)
$Cli = [System.IO.Path]::GetFullPath($Cli)
$defaultDatabase = [System.IO.Path]::GetFullPath((Join-Path $root "harness.db"))
$sourceCheckout = (Test-Path (Join-Path $root "Cargo.toml")) -and
    (Test-Path (Join-Path $root "crates/harness-cli/Cargo.toml"))

if ($sourceCheckout -and $Database -eq $defaultDatabase -and !(Test-Path $Database)) {
    throw "Harness bootstrap failed: authoritative core state is unavailable; restore the verified core epoch instead of initializing an empty replacement"
}

if ($sourceCheckout) {
    if (!(Get-Command cargo -ErrorAction SilentlyContinue)) {
        throw "Harness bootstrap failed: cargo is required in a Harness CLI source checkout"
    }
    & cargo build --quiet --manifest-path (Join-Path $root "Cargo.toml") -p harness-cli --locked
    if ($LASTEXITCODE -ne 0) { throw "Harness bootstrap failed: cargo build failed" }
    $builtCli = Join-Path $root "target/debug/harness-cli.exe"
    if ([System.IO.Path]::GetFullPath($builtCli) -ne [System.IO.Path]::GetFullPath($Cli)) {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Cli) | Out-Null
        Copy-Item -LiteralPath $builtCli -Destination $Cli -Force
    }
} elseif (!(Test-Path $Cli)) {
    throw "Harness bootstrap failed: Harness CLI is missing at $Cli. Run '.\scripts\install-harness-cli.ps1' to download it from the pinned release, or set `$env:HARNESS_CLI` to point at an existing binary."
}

$releaseTagFile = Join-Path $root "scripts/harness-cli-release-tag"
if (!(Test-Path $releaseTagFile)) {
    throw "Harness bootstrap failed: pinned release file is missing: $releaseTagFile"
}
$releaseTag = (Get-Content -LiteralPath $releaseTagFile | Where-Object {
    $_ -match "\S" -and $_ -notmatch "^\s*#"
} | Select-Object -First 1).Trim()
$actualVersion = (& $Cli --version).Split()[-1]
$expectedVersion = $releaseTag -replace '^harness-cli-v', ''
if (!$releaseTag.StartsWith("harness-cli-v") -or $actualVersion -ne $expectedVersion) {
    throw "Harness bootstrap failed: CLI version $actualVersion does not match pinned release $releaseTag"
}

function Get-Contract {
    $env:HARNESS_REPO_ROOT = $root
    $env:HARNESS_DB_PATH = $Database
    $json = & $Cli query contract --json
    if ($LASTEXITCODE -ne 0) { throw "Harness bootstrap failed: query contract failed" }
    return ($json | ConvertFrom-Json).result
}

$contract = Get-Contract
switch ($contract.database_state) {
    "missing" { & $Cli init | Out-Null }
    "needs_migration" { & $Cli migrate | Out-Null }
    "current" { }
    "unsupported" { throw "Harness bootstrap failed: database schema is outside the CLI's supported range" }
    default { throw "Harness bootstrap failed: query contract returned an unknown database state" }
}
if ($LASTEXITCODE -ne 0) { throw "Harness bootstrap failed: database initialization or migration failed" }

$contract = Get-Contract
if ($contract.database_state -ne "current") {
    throw "Harness bootstrap failed: database did not reach current schema"
}
Write-Host "Harness ready: cli=$Cli database=$Database"
