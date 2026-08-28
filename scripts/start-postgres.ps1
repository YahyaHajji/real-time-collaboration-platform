$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$docker = (Get-Command docker.exe -ErrorAction SilentlyContinue).Source
if (-not $docker) {
    $docker = 'C:\Program Files\Docker\Docker\resources\bin\docker.exe'
}
if (-not (Test-Path -LiteralPath $docker)) {
    throw 'Docker CLI was not found. Install and start Docker Desktop first.'
}
& $docker compose --project-directory $projectRoot up -d --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL did not start successfully.' }
