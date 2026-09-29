# Compila y abre el panel de control conectado al simulador Webots por TCP/IP.
# Uso: .\scripts\panel_webots.ps1 [-Direccion tcp://127.0.0.1:5000]
# Webots tiene que estar corriendo con webots\worlds\hexabot.wbt (y sin pausa).

param(
    [string]$Direccion = "tcp://127.0.0.1:5000"
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$compilado = & (Join-Path $PSScriptRoot "compilar_panel.ps1")

# El panel lee etc\config.csv con ruta relativa
Push-Location (Join-Path $raiz "software\PanelControl1")
try {
    & $compilado.Java -cp $compilado.Classpath ar.edu.unsta.robotteam.hexabot.view.PanelControl $Direccion
} finally {
    Pop-Location
}
