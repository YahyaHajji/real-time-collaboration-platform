$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runDirectory = Join-Path $projectRoot '.run'

foreach ($name in @('frontend', 'backend')) {
    $pidFile = Join-Path $runDirectory "$name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { continue }
    $processId = [int](Get-Content -Raw -LiteralPath $pidFile)
    if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
        & taskkill.exe /PID $processId /T /F | Out-Null
        Write-Host "Stopped $name."
    }
    Remove-Item -LiteralPath $pidFile -Force
}

$docker = (Get-Command docker.exe -ErrorAction SilentlyContinue).Source
if (-not $docker) { $docker = 'C:\Program Files\Docker\Docker\resources\bin\docker.exe' }
if (Test-Path -LiteralPath $docker) {
    & $docker compose --project-directory $projectRoot stop postgres
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL did not stop successfully.' }
}
