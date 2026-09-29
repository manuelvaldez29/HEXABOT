# Compila Scene3D y el panel de control con javac (no hace falta NetBeans ni Ant).
# Deja las clases en build\ y devuelve el classpath para ejecutarlo.
# Uso: .\scripts\compilar_panel.ps1

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot

function Buscar-Jdk {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) { return "$env:JAVA_HOME\bin" }
    $enPath = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($enPath) { return Split-Path -Parent $enPath.Source }
    $portable = Get-ChildItem "$env:LOCALAPPDATA\Programs" -Directory -Filter "jdk-*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($portable) { return "$($portable.FullName)\bin" }
    throw "No se encontro un JDK (javac). Instala Java 8 o superior, o defini JAVA_HOME."
}

$jdk = Buscar-Jdk
$build = Join-Path $raiz "build"
$s3d = Join-Path $build "Scene3D"
$panel = Join-Path $build "PanelControl1"
$lib = Join-Path $raiz "software\PanelControl1\lib"
Remove-Item -Recurse -Force $s3d, $panel -ErrorAction SilentlyContinue

$fuentes = Get-ChildItem (Join-Path $raiz "software\Scene3D\src") -Recurse -Filter *.java | ForEach-Object FullName
& "$jdk\javac.exe" -nowarn --release 8 -encoding UTF-8 -d $s3d $fuentes
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion de Scene3D" }

$cp = "$s3d;$lib\RXTXcomm-2.2pre2.jar;$lib\AbsoluteLayout.jar"
$fuentes = Get-ChildItem (Join-Path $raiz "software\PanelControl1\src") -Recurse -Filter *.java | ForEach-Object FullName
& "$jdk\javac.exe" -nowarn --release 8 -encoding UTF-8 -cp $cp -d $panel $fuentes
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion del panel" }
# Imagenes (logo) que usa la interfaz
Get-ChildItem (Join-Path $raiz "software\PanelControl1\src") -Recurse -Include *.png | ForEach-Object {
    $destino = Join-Path $panel ($_.FullName.Substring((Join-Path $raiz "software\PanelControl1\src").Length + 1))
    New-Item -ItemType Directory -Force (Split-Path -Parent $destino) | Out-Null
    Copy-Item $_.FullName $destino
}

Write-Host "Panel compilado en $panel"
return @{ Java = "$jdk\java.exe"; Javac = "$jdk\javac.exe"; Classpath = "$panel;$cp" }
