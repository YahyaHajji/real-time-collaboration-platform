$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $projectRoot 'frontend'
$bundledNodeDirectory = 'C:\tmp\node-v24.18.0-win-x64'
if (Test-Path -LiteralPath (Join-Path $bundledNodeDirectory 'npm.cmd')) {
    $env:PATH = $bundledNodeDirectory + ';' + $env:PATH
    $npm = Join-Path $bundledNodeDirectory 'npm.cmd'
} else {
    $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
}
if (-not $npm) { throw 'npm was not found. Install Node.js 24 LTS first.' }
$node = Join-Path (Split-Path -Parent $npm) 'node.exe'
if (-not (Test-Path -LiteralPath $node)) {
    $node = (Get-Command node.exe -ErrorAction SilentlyContinue).Source
}
if (-not $node) { throw 'Node.js was not found. Install Node.js 24 LTS first.' }
$nodeVersion = [version]((& $node --version).Trim().TrimStart('v'))
if ($nodeVersion.Major -ne 24) {
    throw "Node.js 24 LTS is required. Found $nodeVersion at $node."
}
Push-Location $frontend
try {
    if (-not (Test-Path -LiteralPath (Join-Path $frontend 'node_modules\@angular\cli'))) {
        Write-Host 'Installing frontend dependencies for the first run...'
        & $npm ci
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    }
    Write-Host 'Starting Angular. A cold first build can take up to 90 seconds...'
    & $npm start
    if ($LASTEXITCODE -ne 0) { throw 'The frontend exited with an error.' }
} finally {
    Pop-Location
}
