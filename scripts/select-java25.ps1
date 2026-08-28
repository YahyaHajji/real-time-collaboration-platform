function Test-Java25Home([string]$javaHome) {
    if (-not $javaHome) { return $false }

    $java = Join-Path $javaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $java)) { return $false }

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Java reports its version on stderr; Windows PowerShell 5 treats that as an error record.
        $ErrorActionPreference = 'Continue'
        $versionOutput = (& $java -version 2>&1 | Out-String)
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    return $versionOutput -match '(?:java|openjdk) version "25(?:\.|\")'
}

$javaCandidates = @($env:JAVA_HOME)
$javaCandidates += Get-ChildItem -Path (Join-Path $env:LOCALAPPDATA 'Programs\Java\jdk-25*') `
    -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
$javaCandidates += Join-Path $env:APPDATA 'Code\User\globalStorage\pleiades.java-extension-pack-jdk\java\25'
$javaCandidates += Get-ChildItem -Path 'C:\Program Files\Java\jdk-25*' `
    -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName

$java25Home = $javaCandidates | Where-Object { Test-Java25Home $_ } | Select-Object -First 1
if (-not $java25Home) {
    throw 'Java 25 JDK was not found. Install Java 25 or set JAVA_HOME to a Java 25 JDK.'
}

$env:JAVA_HOME = $java25Home
$env:Path = "$(Join-Path $java25Home 'bin');$env:Path"
Write-Host "Using Java 25 from $java25Home"
