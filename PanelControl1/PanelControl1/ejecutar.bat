@echo off
rem Abre el panel de control. Se ejecuta desde esta carpeta para que el
rem panel encuentre etc\config.csv.
cd /d "%~dp0"
start "" javaw -jar dist\PanelControl1.jar
