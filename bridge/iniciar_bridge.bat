@echo off
rem Inicia el puente serie <-> TCP entre el panel original y Webots.
rem Para Webots en otra PC:      iniciar_bridge.bat --host 192.168.0.20
rem Para ver todos los comandos: iniciar_bridge.bat --ver-trafico
cd /d "%~dp0"
python bridge.py %*
pause
