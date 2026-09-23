@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

start "xinan-flask-yolo" /min "%SCRIPT_DIR%run-local-flask.cmd"
start "xinan-spring-demo" /min "%SCRIPT_DIR%run-local-spring.cmd"
start "xinan-vue-vite" /min "%SCRIPT_DIR%run-local-vite.cmd"

echo Services are starting:
echo   Frontend:    http://127.0.0.1:8100
echo   Backend API: http://127.0.0.1:9999
echo   YOLO Flask:  http://127.0.0.1:5000
