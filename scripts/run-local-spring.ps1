$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$LogDir = Join-Path $Root "logs"
$BackendDir = Join-Path $Root "yolo-moudle\face\yolo_face_detection_springboot"
$JavaExe = "C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe"
$DeepSeekEnvFile = Join-Path $PSScriptRoot "deepseek-env.local.ps1"

if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

if (Test-Path $DeepSeekEnvFile) {
    . $DeepSeekEnvFile
    Write-Host "Loaded local DeepSeek environment."
}
else {
    Write-Host "DeepSeek environment file was not found. Backend will use local fallback replies."
}

Set-Location $BackendDir
& $JavaExe -jar "target\Ece-0.0.1-SNAPSHOT.jar" "--spring.profiles.active=demo" `
    1> (Join-Path $LogDir "spring-demo.out.log") `
    2> (Join-Path $LogDir "spring-demo.err.log")
