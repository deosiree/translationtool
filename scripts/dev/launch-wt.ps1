param(
  [Parameter(Mandatory = $true)]
  [string]$PayloadJson
)

$ErrorActionPreference = "Stop"
$data = $PayloadJson | ConvertFrom-Json
$panes = @($data.panes)

if ($panes.Count -eq 0) {
  Write-Error "No panes to launch"
  exit 1
}

$first = $panes[0]
$parts = @(
  "-w", "0",
  "new-tab", "--title", $first.title,
  "-d", $first.dir,
  "pwsh", "-NoProfile", "-NoExit", "-Command", $first.command
)

for ($i = 1; $i -lt $panes.Count; $i++) {
  $p = $panes[$i]
  $parts += ";"
  $parts += "split-pane", "-V", "--title", $p.title, "-d", $p.dir,
    "pwsh", "-NoProfile", "-NoExit", "-Command", $p.command
}

Start-Process -FilePath "wt.exe" -ArgumentList $parts
exit 0
