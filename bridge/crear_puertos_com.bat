@echo off
rem ------------------------------------------------------------------------
rem  Crea los tres pares de puertos serie virtuales del HEXABOT con com0com:
rem    COM10 <-> COM11   (Arduino 1)
rem    COM12 <-> COM13   (Arduino 2)
rem    COM14 <-> COM15   (Arduino 3)
rem  El panel usa los pares (COM10, COM12, COM14) y el bridge los impares.
rem
rem  Se ejecuta UNA SOLA VEZ, despues de instalar com0com.
rem  Windows pide permiso de administrador.
rem ------------------------------------------------------------------------
setlocal
set CARPETA=%ProgramFiles(x86)%\com0com
if not exist "%CARPETA%\setupc.exe" set CARPETA=%ProgramFiles%\com0com
if not exist "%CARPETA%\setupc.exe" (
  echo No se encontro com0com. Instalarlo primero.
  pause
  exit /b 1
)

rem setupc tiene que ejecutarse desde su carpeta para encontrar el driver
cd /d "%CARPETA%"
setupc install PortName=COM10 PortName=COM11
setupc install PortName=COM12 PortName=COM13
setupc install PortName=COM14 PortName=COM15

echo.
echo Pares de puertos instalados:
setupc list
pause
