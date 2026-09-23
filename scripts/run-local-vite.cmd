@echo off
setlocal

set "ROOT=%~dp0.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "LOGDIR=%ROOT%\logs"
set "NPM_CMD=D:\software\nodejs\npm.cmd"
set "FRONTEND_DIR=%ROOT%\yolo-moudle\face\yolo_face_detection_vue"

if not exist "%LOGDIR%" mkdir "%LOGDIR%"

cd /d "%FRONTEND_DIR%"
"%NPM_CMD%" run dev 1> "%LOGDIR%\vue-vite.out.log" 2> "%LOGDIR%\vue-vite.err.log"
