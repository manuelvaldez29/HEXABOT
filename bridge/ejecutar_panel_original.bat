@echo off
rem ------------------------------------------------------------------------
rem  Abre el panel de control ORIGINAL de 2017, sin ninguna modificacion:
rem    panel-original\PanelControl1.jar        el programa, tal cual
rem    panel-original\lib\RXTXcomm-2.2pre2.jar su libreria de puerto serie
rem    panel-original\lib\rxtxSerial.dll       la DLL nativa de RXTX 2.2pre2
rem                                            (32 bits)
rem
rem  Se ejecuta con Java 8 de 32 bits (java8-32\), la epoca del panel:
rem  la DLL de 64 bits de RXTX es inestable y RXTX no funciona en Java 9+.
rem  Si falta java8-32\, ejecutar antes descargar_java8.bat.
rem ------------------------------------------------------------------------
cd /d "%~dp0panel-original"
if not exist "..\java8-32\bin\javaw.exe" (
  echo Falta Java 8 de 32 bits. Ejecutar primero descargar_java8.bat
  pause
  exit /b 1
)
start "" "..\java8-32\bin\javaw.exe" -Djava.library.path=lib -jar PanelControl1.jar
