# Prueba de punta a punta sin Webots: modelo del panel Java -> TCP -> Arduino virtual
# (con el modulo "controller" falso de tests\falso_webots).
# Uso: .\tests\probar_panel.ps1

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$puerto = 5056
$compilado = & (Join-Path $raiz "scripts\compilar_panel.ps1")

$clasesPrueba = Join-Path $raiz "build\pruebas"
Remove-Item -Recurse -Force $clasesPrueba -ErrorAction SilentlyContinue
& $compilado.Javac -nowarn --release 8 -encoding UTF-8 -cp $compilado.Classpath -d $clasesPrueba (Join-Path $PSScriptRoot "java\PruebaPanelTcp.java")
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion de la prueba" }

$env:PYTHONPATH = Join-Path $PSScriptRoot "falso_webots"
$controlador = Start-Process python -PassThru -WindowStyle Hidden -ArgumentList @(
    "`"$(Join-Path $raiz 'webots\controllers\hexabot_arduino\hexabot_arduino.py')`"", "--puerto=$puerto")
Start-Sleep -Seconds 2

Push-Location (Join-Path $raiz "software\PanelControl1")
try {
    & $compilado.Java -cp "$clasesPrueba;$($compilado.Classpath)" PruebaPanelTcp "tcp://127.0.0.1:$puerto" 8000
    $resultado = $LASTEXITCODE
} finally {
    Pop-Location
    Stop-Process -Id $controlador.Id -Force -ErrorAction SilentlyContinue
}
if ($resultado -ne 0) { throw "La prueba del panel fallo" }
