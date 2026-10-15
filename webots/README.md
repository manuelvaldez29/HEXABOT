# HEXABOT en Webots

Simulación del robot hexápodo HEXABOT (UNSTA, 2016-2017) en Webots R2025a.

Dentro de Webots están embebidos los **tres Arduino MEGA** del robot real y
sus **18 motores** (6 patas × coxa, fémur y tibia). Cada Arduino escucha en
su propio **puerto TCP/IP** y habla el mismo protocolo de texto que el
firmware original (`ControlArduino4.ino`). La Raspberry Pi queda afuera: su
lugar lo ocupa el panel de control original (`PanelControl1`), adaptado para
conectarse por TCP en lugar de por los puertos serie USB.

```
                         ┌──────────── Webots ─────────────┐
                 :1001   │  Arduino 1 ── 6 coxas           │
PanelControl1  ──:1002── │  Arduino 2 ── fémur y tibia,    │
 (Java, en el            │               patas 1, 2 y 3    │
  lugar de la    :1003   │  Arduino 3 ── fémur y tibia,    │
  Raspberry Pi)          │               patas 4, 5 y 6    │
                         └─────────────────────────────────┘
```

## Cómo usarlo

1. **Abrir la simulación.** En Webots: *File → Open World* y elegir
   `webots/worlds/hexabot.wbt`. El robot aparece parado y en la consola de
   Webots se lee `HEXABOT: Arduino 1 escuchando en el puerto 1001` (y lo
   mismo para el 2 y el 3).
2. **Abrir el panel.** Ejecutar `PanelControl1/PanelControl1/ejecutar.bat`.
   Si cambian el código Java, antes ejecuten `compilar.bat` (solo hace falta
   un JDK; no se necesita NetBeans).
3. En el panel:
   1. **Connect**: pide la PC donde corre Webots. Por defecto `localhost`;
      si Webots corre en otra PC, pongan su IP (por ejemplo `192.168.0.20`).
      El panel abre los puertos 1001, 1002 y 1003, y verifica que en cada
      uno responda el Arduino correcto.
   2. Marcar **Send Cmd**. Sin esa casilla, el panel calcula todo pero no
      envía nada al robot.
   3. **Up**: el robot se para en la posición de caminata.
   4. **Joystick**: el ángulo es el rumbo (arriba = norte, +Y en Webots) y la
      distancia al centro es la velocidad. Volver al centro lo detiene.
      Los controles STEP y CLEARANCE cambian el largo del paso y la
      altura a la que se levantan las patas.

En las pruebas, a velocidad máxima, el robot avanzó 1,5 m en 40 segundos
manteniendo el cuerpo a 410 mm del piso.

## Reparto de motores

Es el del robot real (`Hexapodo.java` y `etc/config.csv`):

| Arduino | Puerto | Motor 1 | Motor 2 | Motor 3 | Motor 4 | Motor 5 | Motor 6 |
|---|---|---|---|---|---|---|---|
| 1 | 1001 | Coxa 1 | Coxa 2 | Coxa 3 | Coxa 4 | Coxa 5 | Coxa 6 |
| 2 | 1002 | Fémur 1 | Tibia 1 | Fémur 2 | Tibia 2 | Fémur 3 | Tibia 3 |
| 3 | 1003 | Fémur 4 | Tibia 4 | Fémur 5 | Tibia 5 | Fémur 6 | Tibia 6 |

Las patas se numeran en sentido horario visto desde arriba: la pata 2
apunta al este (+X) y la pata 1 está a 60° de ella, hacia el norte.

Para repartir los motores de otra forma (por ejemplo, dos patas completas
por Arduino), hay que cambiar la tabla `REPARTO` en `hexabot_tcp.py` y las
llamadas a `setCanales()` en el constructor de `Hexapodo.java`.

## Protocolo TCP

Texto ASCII, un comando por línea; cada comando recibe una respuesta
`OK ...` o `ERR ...`. Cada Arduino maneja solo sus 6 motores, numerados de
1 a 6.

| Comando | Qué hace | Respuesta |
|---|---|---|
| `A` | Identificación | `OK A <1, 2 o 3>` |
| `C <param> <valor>` | Configuración de pines y PID. Se acepta y se ignora: en la simulación no hay pines. | `OK C <param>` |
| `P` | Enciende el control de posición de sus 6 motores | `OK P` |
| `S <motor> <pulsos>` | Setpoint de un motor, en pulsos de encoder. Solo mueve el motor si su control está encendido (`P`). | `OK S <motor>` |
| `I <motor>` | Lleva el actuador al inicio de carrera (0 pulsos) y le apaga el control | `OK I <motor>` |
| `H` | Detiene sus 6 motores donde están | `OK H` |
| `Q` | Estado: 6 posiciones, 6 sensores de inicio de carrera, 6 encoders | `OK Q p1 … p6 i1 … i6 e1 … e6` |
| `G` | **Solo simulación**: posición del cuerpo según el GPS, en mm | `OK G <x> <y> <z>` |

### Probar sin el panel

`herramientas/cliente_prueba.py` es un cliente de consola. Por ejemplo, para
levantar el fémur de la pata 1 (motor 1 del Arduino 2):

```
python webots/herramientas/cliente_prueba.py localhost 1002
> P
OK P
> S 1 300
OK S 1
> Q
OK Q 300 ...
```

Cualquier otro cliente TCP (PuTTY en modo *Raw*, netcat, un programa
propio) funciona igual.

## Cómo está hecho

- **`worlds/hexabot.wbt`**: el mundo. Se genera con
  `herramientas/generar_mundo.py`; para cambiar medidas, masas, la fuerza de
  los motores o los puertos, editen ese script y vuelvan a correrlo.
  Las medidas salen de la documentación oficial y de `Pata.java`
  (fémur de 380 mm, tibia de 475 mm, coxas a 150 mm del centro).
- **`controllers/hexabot_tcp/hexabot_tcp.py`**: los tres Arduinos
  virtuales, dentro de un único controlador de Webots.
  El robot real usa actuadores lineales (motor + tornillo de avance); en la
  simulación cada articulación tiene un motor rotativo, y el controlador
  convierte los pulsos del protocolo a milímetros de actuador y luego a
  ángulo, invirtiendo las mismas fórmulas que usa el panel en
  `Pata.estableceActuadores()`. Así el panel no nota la diferencia.
- **Cambios en el panel Java** (la cinemática y la caminata no se tocaron):
  - `ClientServerPort`: además del puerto serie, acepta `tcp://host:puerto`.
  - `Shield.doConnect()` y `Hexapodo.doConnect()`: cada shield se conecta a
    su puerto TCP y verifica con `A` que responda el Arduino correcto.
    Es el equivalente de `doDiscover()`, que recorría los puertos serie.
  - El botón *Discover* pasó a ser *Connect*.

## Diferencias con el robot real

- Los motores simulados son más fuertes y más rápidos que los reales
  (ver `TORQUE_*` y `VEL_*` en `generar_mundo.py`). Bajarlos es una buena
  forma de estudiar por qué el robot real no pudo caminar.
- No hay holgura en las articulaciones ni flexión en las patas, que fueron
  dos de los problemas principales del robot real.
- Los encoders no pierden pulsos: la posición es exacta.
