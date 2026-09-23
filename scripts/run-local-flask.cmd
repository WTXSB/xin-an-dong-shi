@echo off
setlocal

set "ROOT=%~dp0.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "LOGDIR=%ROOT%\logs"
set "YOLO_CONFIG_DIR=%LOGDIR%\ultralytics"
set "PYTHON_EXE=D:\software\anaconda3\envs\pytorch\python.exe"
set "FLASK_DIR=%ROOT%\yolo-moudle\face\yolo_face_detection_flask"

if not exist "%LOGDIR%" mkdir "%LOGDIR%"
if not exist "%YOLO_CONFIG_DIR%" mkdir "%YOLO_CONFIG_DIR%"

cd /d "%FLASK_DIR%"
"%PYTHON_EXE%" facetry.py 1> "%LOGDIR%\flask-yolo.out.log" 2> "%LOGDIR%\flask-yolo.err.log"
