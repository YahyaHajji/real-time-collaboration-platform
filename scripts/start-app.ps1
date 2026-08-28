$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runDirectory = Join-Path $projectRoot '.run'
$shell = (Get-Process -Id $PID).Path
if (-not $shell -or -not (Test-Path -LiteralPath $shell)) {
    $shellCommand = Get-Command pwsh.exe, powershell.exe -ErrorAction SilentlyContinue |
        Select-Object -First 1
    $shell = $shellCommand.Source
}
if (-not $shell -or -not (Test-Path -LiteralPath $shell)) {
    throw 'PowerShell executable was not found.'
}

New-Item -ItemType Directory -Force -Path $runDirectory | Out-Null

function Test-HttpEndpoint([string]$url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 -Uri $url
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Start-AppProcess([string]$name, [string]$scriptPath, [string]$healthUrl) {
    if (Test-HttpEndpoint $healthUrl) {
        Write-Host "$name is already running."
        return $null
    }

    $stdout = Join-Path $runDirectory "$name.out.log"
    $stderr = Join-Path $runDirectory "$name.err.log"
    $arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`""
    $process = Start-Process -FilePath $shell -ArgumentList $arguments -PassThru `
        -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    Set-Content -LiteralPath (Join-Path $runDirectory "$name.pid") -Value $process.Id
    Write-Host "Starting $name (PID $($process.Id))..."
    return $process
}

function Wait-ForEndpoint(
    [string]$name,
    [string]$url,
    [System.Diagnostics.Process]$process,
    [int]$timeoutSeconds = 120
) {
    $deadline = [DateTime]::UtcNow.AddSeconds($timeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (Test-HttpEndpoint $url) {
            Write-Host "$name is ready."
            return
        }
        if ($process -and $process.HasExited) {
            $outputLog = Join-Path $runDirectory "$name.out.log"
            $errorLog = Join-Path $runDirectory "$name.err.log"
            if (Test-Path -LiteralPath $outputLog) { Get-Content -Tail 40 -LiteralPath $outputLog }
            if (Test-Path -LiteralPath $errorLog) { Get-Content -Tail 30 -LiteralPath $errorLog }
            throw "$name stopped before it became ready."
        }
        Start-Sleep -Seconds 2
    }
    throw "$name did not become ready within $timeoutSeconds seconds. Check $runDirectory."
}

& (Join-Path $PSScriptRoot 'start-postgres.ps1')
$backend = Start-AppProcess 'backend' (Join-Path $PSScriptRoot 'start-backend.ps1') `
    'http://localhost:8081/actuator/health'
$frontend = Start-AppProcess 'frontend' (Join-Path $PSScriptRoot 'start-frontend.ps1') `
    'http://localhost:4201/'

Wait-ForEndpoint 'Backend' 'http://localhost:8081/actuator/health' $backend
Wait-ForEndpoint 'Frontend' 'http://localhost:4201/' $frontend

Write-Host ''
Write-Host 'Relay is running at http://localhost:4201'
Write-Host 'User login: user@collaboration.local / User123!'
Write-Host 'Run .\scripts\stop-app.ps1 to stop all project services.'
