@echo off
rem ------------------------------------------------------------------------
rem  Descarga Java 8 de 32 bits (Eclipse Temurin, portable, ~39 MB) en
rem  java8-32\. No se instala ni modifica el Java del sistema.
rem  No se sube a GitHub por su tamanio (ver .gitignore).
rem ------------------------------------------------------------------------
setlocal
cd /d "%~dp0"
set VERSION=jdk8u472-b08
set ARCHIVO=OpenJDK8U-jre_x86-32_windows_hotspot_8u472b08.zip
set SHA256=21a2c5af684a658f1484daa85eabf4961ab9de28c0efbf31da2381d77fce3b5f

if exist java8-32\bin\java.exe (
  echo Java 8 de 32 bits ya esta en java8-32\
  exit /b 0
)

echo Descargando %ARCHIVO%...
curl -L -o %ARCHIVO% https://github.com/adoptium/temurin8-binaries/releases/download/%VERSION%/%ARCHIVO%
if errorlevel 1 goto error

echo Verificando la huella SHA-256...
certutil -hashfile %ARCHIVO% SHA256 | findstr /i %SHA256% > nul
if errorlevel 1 (
  echo La huella no coincide: el archivo puede estar danado.
  del %ARCHIVO%
  goto error
)

tar -xf %ARCHIVO%
if errorlevel 1 goto error
ren %VERSION%-jre java8-32
del %ARCHIVO%
echo Listo: java8-32\
exit /b 0

:error
echo *** No se pudo descargar Java 8 ***
pause
exit /b 1
