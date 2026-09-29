# HEXABOT en Webots

HEXABOT es un robot hexápodo desarrollado como proyecto final de grado en la
Facultad de Ingeniería de la Universidad del Norte Santo Tomás de Aquino (UNSTA),
2016/2017. Autores: Álvarez Farhat E. y Chaila A. Director: Sbrugnera G.

El robot real no llegó a caminar como se esperaba. Este repositorio lleva el
proyecto al simulador **Webots**, con la consigna de la materia:

- el simulador controla **6 motores**, como si hubiera un solo Arduino (el
  Arduino 1 original, que manejaba las 6 coxas);
- el robot se controla por **TCP/IP**, con el mismo protocolo de comandos que
  usaba el Arduino por puerto serie;
- el cliente es el **panel de control Java original**, adaptado.

## Estructura

| Carpeta | Contenido |
|---|---|
| `webots/worlds/hexabot.wbt` | Mundo de Webots |
| `webots/protos/Hexabot.proto` | Robot: cuerpo hexagonal, 6 patas con una coxa motorizada cada una. Medidas del panel original |
| `webots/controllers/hexabot_arduino/` | "Arduino virtual": servidor TCP que habla el protocolo de `ControlArduino4.ino` |
| `software/PanelControl1/` | Panel de control Java original, con conexión TCP y marcha de coxas agregadas |
| `software/Scene3D/`, `software/SimuladorPata1/` | Librería 3D y simulador de pata originales (sin cambios) |
| `firmware/ControlArduino4/` | Firmware original del Arduino (sin cambios, de referencia) |
| `tools/cliente_hexabot.py` | Cliente TCP en Python, para probar sin el panel |
| `tests/` | Pruebas del Arduino virtual y del panel sin Webots |
| `docs/` | Tesis original, difusión, y explicación de la marcha (`MARCHA_COXAS.md`) |

## Requisitos

- Webots R2025a (https://cyberbotics.com). Necesita una placa de video con
  OpenGL 3.3 o superior.
- Python 3 (lo usa Webots para el controlador).
- Java 8 o superior (JDK) para compilar el panel.

## Cómo usarlo

1. **Abrir la simulación.** En Webots: *File > Open World* y elegir
   `webots/worlds/hexabot.wbt`. Dar Play. En la consola de Webots aparece
   `HEXABOT: Arduino virtual 1 escuchando en 127.0.0.1:5000`.

2. **Abrir el panel** (en PowerShell, desde la carpeta del repo):

   ```powershell
   .\scripts\panel_webots.ps1
   ```

   Compila el panel y lo abre conectado a `tcp://127.0.0.1:5000`.

3. **En el panel**, en este orden:
   1. Marcar **Send Cmd**.
   2. **Discover**: conecta con el simulador (responde como Arduino 1).
   3. **Configure**: manda la configuración de `etc/config.csv` (solo la del Arduino 1).
   4. **Sit**: las coxas buscan el inicio de carrera. Esperar unos segundos.
   5. **Up**: activa el control y lleva las coxas a 0º.
   6. Mover el **joystick**: arriba avanza, abajo retrocede, a los costados gira.

### Probar sin el panel

Con la simulación corriendo:

```powershell
python tools\cliente_hexabot.py --demo --ciclos 4 --rumbo 0
```

Inicializa el robot, camina 4 ciclos e informa cuánto avanzó. Sin `--demo`
queda en modo interactivo: cada línea que se escribe se manda como comando.

### Controlar desde otra PC

En `webots/worlds/hexabot.wbt`, cambiar `--host=127.0.0.1` por
`--host=0.0.0.0`, y conectar el panel a la IP de la PC que corre Webots:

```powershell
.\scripts\panel_webots.ps1 -Direccion tcp://192.168.0.10:5000
```

Esto deja el robot abierto a cualquiera en la red. Usarlo solo en una red de
confianza.

## Protocolo TCP

Es el mismo protocolo de texto del firmware (`ControlArduino4.ino`): un comando
por línea, y el Arduino responde `OK <comando> ...`.

| Comando | Qué hace | Respuesta |
|---|---|---|
| `A` | Identificación | `OK A 1` |
| `C <param> <valor>` | Configuración. En la simulación solo se usa `MAX-X` (81 a 86); pines y PID se ignoran | `OK C <param>` |
| `I <m>` | Motor `m` (1 a 6) busca el inicio de carrera. Apaga su control | `OK I <m>` |
| `S <m> <pulsos>` | Setpoint del motor `m`, en pulsos de encoder | `OK S <m>` |
| `P` | Activa el control de los 6 motores | `OK P` |
| `H` | Detiene los 6 motores | `OK H` |
| `Q` | Estado: 6 posiciones en pulsos (9999 = sin inicializar), 6 sensores de inicio, 6 encoders | `OK Q  p1 .. p6 i1 .. i6 e1 .. e6` |
| `G` | **Extensión** (no existía en el firmware): posición del robot en el mundo | `OK G <x_mm> <y_mm> <z_mm> <rumbo_grados>` |

Los pulsos significan lo mismo que en el robot real: el controlador convierte
pulsos a ángulo de coxa con la geometría de la biela y las constantes de
pulsos/mm de cada pata que usa el panel (`Pata.estableceActuadores`, `Hexapodo`).
Al igual que el firmware, un motor no obedece `S` hasta que se inicializa con
`I` y se activa con `P`.

## Pruebas

Sin Webots (usan un módulo `controller` falso que imita los motores):

```powershell
python -m unittest discover -s tests -v   # Arduino virtual, protocolo y geometría
.\tests\probar_panel.ps1                   # panel Java -> TCP -> Arduino virtual
```

## Supuestos del modelo

- Fémur y tibia quedan fijos en la pose **Up** del panel: fémur horizontal
  (380 mm) y tibia vertical (475 mm). El cuerpo queda a 410 mm del piso.
- Las masas (12 kg de cuerpo, 1,5 kg por pata), el torque de la coxa (30 N·m)
  y el rozamiento de los pies (0,6) son **estimados**. La tesis no los da con
  ese detalle.
- La velocidad de las coxas es 0,6 rad/s (argumento `--velocidad` del
  controlador).
