#!/usr/bin/env pwsh
<#
.SYNOPSIS
  本机 RAG 语料同步：打包 / 还原 data/rag-corpus（不进 Git）。

.DESCRIPTION
  默认把语料打成 zip 放到同步根目录（网盘/移动硬盘路径），或从 zip 还原到仓库。
  同步根：环境变量 RAG_CORPUS_SYNC_ROOT，否则 ./data/_rag-corpus-sync（本机，已建议 ignore）。

.EXAMPLE
  .\scripts\sync-rag-corpus.ps1 pack
  .\scripts\sync-rag-corpus.ps1 restore -ZipPath "D:\backup\rag-corpus-20260723.zip"
  .\scripts\sync-rag-corpus.ps1 pack -SyncRoot "D:\Sieyuan\rag-corpus-sync"
#>
param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidateSet("pack", "restore", "status")]
  [string]$Action,

  [string]$SyncRoot = $env:RAG_CORPUS_SYNC_ROOT,
  [string]$ZipPath,
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$Corpus = Join-Path $RepoRoot "data\rag-corpus"

if (-not $SyncRoot -or $SyncRoot.Trim() -eq "") {
  $SyncRoot = Join-Path $RepoRoot "data\_rag-corpus-sync"
}

function Get-CorpusStats([string]$Path) {
  if (-not (Test-Path $Path)) { return @{ files = 0; mb = 0 } }
  $files = Get-ChildItem $Path -Recurse -File -ErrorAction SilentlyContinue
  $sum = ($files | Measure-Object -Property Length -Sum).Sum
  if (-not $sum) { $sum = 0 }
  return @{ files = @($files).Count; mb = [math]::Round($sum / 1MB, 2) }
}

switch ($Action) {
  "status" {
    $s = Get-CorpusStats $Corpus
    Write-Host "corpus: $Corpus"
    Write-Host "  files=$($s.files) size_mb=$($s.mb)"
    Write-Host "sync_root: $SyncRoot"
    if (Test-Path $SyncRoot) {
      Get-ChildItem $SyncRoot -Filter "rag-corpus-*.zip" | Sort-Object LastWriteTime -Descending | Select-Object -First 5 | ForEach-Object {
        Write-Host ("  {0}  {1:N1} MB  {2}" -f $_.Name, ($_.Length / 1MB), $_.LastWriteTime)
      }
    }
    else {
      Write-Host "  (sync root missing — will create on pack)"
    }
  }
  "pack" {
    if (-not (Test-Path $Corpus)) { throw "missing corpus: $Corpus" }
    New-Item -ItemType Directory -Force -Path $SyncRoot | Out-Null
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $out = if ($ZipPath) { $ZipPath } else { Join-Path $SyncRoot "rag-corpus-$stamp.zip" }
    $outDir = Split-Path -Parent $out
    if ($outDir) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
    if ((Test-Path $out) -and -not $Force) { throw "exists: $out (use -Force)" }
    if (Test-Path $out) { Remove-Item $out -Force }
    Write-Host "packing $Corpus -> $out ..."
    Compress-Archive -Path (Join-Path $Corpus "*") -DestinationPath $out -CompressionLevel Optimal
    $len = (Get-Item $out).Length
    Write-Host ("OK {0:N1} MB" -f ($len / 1MB))
    Write-Host "hint: copy this zip to cloud drive / USB; do NOT git add"
  }
  "restore" {
    if (-not $ZipPath) {
      if (-not (Test-Path $SyncRoot)) { throw "set -ZipPath or create packs under $SyncRoot" }
      $latest = Get-ChildItem $SyncRoot -Filter "rag-corpus-*.zip" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
      if (-not $latest) { throw "no rag-corpus-*.zip in $SyncRoot" }
      $ZipPath = $latest.FullName
    }
    if (-not (Test-Path $ZipPath)) { throw "missing zip: $ZipPath" }
    $dataDir = Join-Path $RepoRoot "data"
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
    if ((Test-Path $Corpus) -and -not $Force) {
      throw "corpus exists: $Corpus (backup first or pass -Force to overwrite)"
    }
    if (Test-Path $Corpus) {
      $bak = Join-Path $dataDir ("rag-corpus.bak-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
      Write-Host "moving existing corpus -> $bak"
      Move-Item $Corpus $bak
    }
    New-Item -ItemType Directory -Force -Path $Corpus | Out-Null
    Write-Host "restoring $ZipPath -> $Corpus ..."
    Expand-Archive -Path $ZipPath -DestinationPath $Corpus -Force
    $s = Get-CorpusStats $Corpus
    Write-Host "OK files=$($s.files) size_mb=$($s.mb)"
    Write-Host "next: python scripts/check-rag-corpus-gates.py"
  }
}
