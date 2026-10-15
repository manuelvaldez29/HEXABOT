@echo off
rem ------------------------------------------------------------------------
rem  Compila Scene3D y PanelControl1 y genera dist\PanelControl1.jar
rem  Requiere un JDK (javac y jar en el PATH). No hace falta NetBeans ni Ant.
rem ------------------------------------------------------------------------
setlocal
cd /d "%~dp0"
set SCENE3D=..\..\Scene3D\Scene3D
set SALIDA=build\compilado

rem La herramienta "jar" no siempre esta en el PATH: se busca en el JDK
set JAR=jar
where jar > nul 2>&1
if not errorlevel 1 goto jar_encontrado
for /f "tokens=2 delims==" %%h in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do (
  for /f "tokens=*" %%t in ("%%h") do set "JDK=%%t"
)
set JAR="%JDK%\bin\jar.exe"
:jar_encontrado

if exist %SALIDA% rmdir /s /q %SALIDA%
mkdir %SALIDA%\scene3d %SALIDA%\panel

echo Compilando Scene3D...
dir /s /b %SCENE3D%\src\*.java > %SALIDA%\fuentes_scene3d.txt
javac --release 8 -encoding UTF-8 -nowarn -d %SALIDA%\scene3d @%SALIDA%\fuentes_scene3d.txt
if errorlevel 1 goto error
%JAR% cf dist\lib\Scene3D.jar -C %SALIDA%\scene3d .
if errorlevel 1 goto error

echo Compilando PanelControl1...
dir /s /b src\*.java > %SALIDA%\fuentes_panel.txt
javac --release 8 -encoding UTF-8 -nowarn -d %SALIDA%\panel ^
  -cp dist\lib\Scene3D.jar;dist\lib\AbsoluteLayout.jar;lib\RXTXcomm-2.2pre2.jar ^
  @%SALIDA%\fuentes_panel.txt
if errorlevel 1 goto error
copy /y src\ar\edu\unsta\robotteam\hexabot\view\*.png %SALIDA%\panel\ar\edu\unsta\robotteam\hexabot\view\ > nul

(
  echo Main-Class: ar.edu.unsta.robotteam.hexabot.view.PanelControl
  echo Class-Path: lib/AbsoluteLayout.jar lib/RXTXcomm-2.2pre2.jar lib/Scene3D.jar
) > %SALIDA%\manifest.txt
%JAR% cfm dist\PanelControl1.jar %SALIDA%\manifest.txt -C %SALIDA%\panel .
if errorlevel 1 goto error

echo.
echo Listo: dist\PanelControl1.jar
echo Para abrir el panel: ejecutar.bat
exit /b 0

:error
echo.
echo *** Error de compilacion ***
exit /b 1
