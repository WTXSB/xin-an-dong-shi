param(
    [string]$CondaBat = "",
    [string]$JavaExe = "",
    [string]$MavenCmd = "",
    [switch]$BuildBackend
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$FlaskDir = Join-Path $Root "yolo-moudle\face\yolo_face_detection_flask"
$BackendDir = Join-Path $Root "yolo-moudle\face\yolo_face_detection_springboot"
$FrontendDir = Join-Path $Root "yolo-moudle\face\yolo_face_detection_vue"
$LogDir = Join-Path $Root "logs"
$DeepSeekEnvFile = Join-Path $PSScriptRoot "deepseek-env.local.ps1"

if ([string]::IsNullOrWhiteSpace($CondaBat)) {
    $condaCommand = Get-Command conda.bat -ErrorAction SilentlyContinue
    if ($null -eq $condaCommand) {
        $condaCommand = Get-Command conda.exe -ErrorAction SilentlyContinue
    }
    if ($null -ne $condaCommand) {
        $CondaBat = $condaCommand.Source
    }
}

if ([string]::IsNullOrWhiteSpace($JavaExe)) {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($null -ne $javaCommand) {
        $JavaExe = $javaCommand.Source
    }
}

if ([string]::IsNullOrWhiteSpace($MavenCmd)) {
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($null -eq $mavenCommand) {
        $mavenCommand = Get-Command mvn.exe -ErrorAction SilentlyContinue
    }
    if ($null -ne $mavenCommand) {
        $MavenCmd = $mavenCommand.Source
    }
    else {
        $localMaven = Join-Path $env:USERPROFILE ".m2\maven-local\apache-maven-3.6.3\bin\mvn.cmd"
        $MavenCmd = if (Test-Path $localMaven) { $localMaven } else { Join-Path $BackendDir "mvnw.cmd" }
    }
}

if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

$env:YOLO_CONFIG_DIR = Join-Path $Root ".ultralytics"
if (-not (Test-Path $env:YOLO_CONFIG_DIR)) {
    New-Item -ItemType Directory -Path $env:YOLO_CONFIG_DIR | Out-Null
}

if (Test-Path $DeepSeekEnvFile) {
    . $DeepSeekEnvFile
    Write-Host "Loaded local DeepSeek environment from scripts\deepseek-env.local.ps1"
}

function Test-PortListening {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $connection
}

function Start-LoggedProcess {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$WorkingDirectory
    )

    $stdout = Join-Path $LogDir "$Name.out.log"
    $stderr = Join-Path $LogDir "$Name.err.log"

    if (Test-Path $stdout) { Remove-Item -LiteralPath $stdout -Force }
    if (Test-Path $stderr) { Remove-Item -LiteralPath $stderr -Force }

    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru

    Write-Host "$Name started. PID: $($process.Id)"
}

if (-not (Test-Path $CondaBat)) {
    throw "Conda was not found. Install Miniconda/Anaconda or pass -CondaBat explicitly."
}

if (-not (Test-Path $JavaExe)) {
    throw "Java was not found. Install a JDK or pass -JavaExe explicitly."
}

if ($BuildBackend) {
    if (-not (Test-Path $MavenCmd)) {
        throw "Maven was not found: $MavenCmd"
    }

    Push-Location $BackendDir
    try {
        $javaHomeLine = & $JavaExe -XshowSettings:properties -version 2>&1 |
            Where-Object { $_ -match '^\s*java\.home\s*=' } |
            Select-Object -First 1
        if ($javaHomeLine -match '=\s*(.+)$') {
            $env:JAVA_HOME = $Matches[1].Trim()
        }
        & $MavenCmd -DskipTests package
    }
    finally {
        Pop-Location
    }
}

if (Test-PortListening 5000) {
    Write-Host "Flask YOLO already listens on http://127.0.0.1:5000"
}
else {
    Start-LoggedProcess `
        -Name "flask-yolo" `
        -FilePath $CondaBat `
        -ArgumentList @("run", "-n", "pytorch", "python", (Join-Path $PSScriptRoot "run-flask-with-certifi.py")) `
        -WorkingDirectory $FlaskDir
}

if (Test-PortListening 9999) {
    Write-Host "Spring Boot already listens on http://127.0.0.1:9999"
}
else {
    $jar = "target\Ece-0.0.1-SNAPSHOT.jar"
    $jarPath = Join-Path $BackendDir $jar
    if (-not (Test-Path $jarPath)) {
        throw "Backend jar was not found. Re-run with -BuildBackend."
    }

    Start-LoggedProcess `
        -Name "spring-demo" `
        -FilePath $JavaExe `
        -ArgumentList @("-jar", $jar, "--spring.profiles.active=demo") `
        -WorkingDirectory $BackendDir
}

if (Test-PortListening 8100) {
    Write-Host "Vue dev server already listens on http://127.0.0.1:8100"
}
else {
    Start-LoggedProcess `
        -Name "vue-vite" `
        -FilePath "cmd.exe" `
        -ArgumentList @("/c", "npm run dev") `
        -WorkingDirectory $FrontendDir
}

Write-Host ""
Write-Host "Services are starting:"
Write-Host "  Frontend:    http://127.0.0.1:8100"
Write-Host "  Backend API: http://127.0.0.1:9999"
Write-Host "  YOLO Flask:  http://127.0.0.1:5000"
Write-Host "Logs: $LogDir"
