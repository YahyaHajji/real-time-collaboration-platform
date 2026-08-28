param([switch]$IncludeE2E)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'start-postgres.ps1')

. (Join-Path $PSScriptRoot 'select-java25.ps1')
Push-Location (Join-Path $projectRoot 'backend')
try { & .\mvnw.cmd test; if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' } }
finally { Pop-Location }

$bundledNodeDirectory = 'C:\tmp\node-v24.18.0-win-x64'
if (Test-Path -LiteralPath (Join-Path $bundledNodeDirectory 'npm.cmd')) {
    $env:PATH = $bundledNodeDirectory + ';' + $env:PATH
    $npm = Join-Path $bundledNodeDirectory 'npm.cmd'
} else {
    $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
}
if (-not $npm) { throw 'npm was not found. Install Node.js 24 LTS first.' }
Push-Location (Join-Path $projectRoot 'frontend')
try {
    & $npm ci
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    & $npm test
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
    & $npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally { Pop-Location }

if ($IncludeE2E) {
    Push-Location (Join-Path $projectRoot 'e2e')
    try {
        & $npm ci
        if ($LASTEXITCODE -ne 0) { throw 'E2E dependency installation failed.' }
        & $npm test
        if ($LASTEXITCODE -ne 0) { throw 'E2E tests failed.' }
    } finally { Pop-Location }
}
