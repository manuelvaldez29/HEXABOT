# Modo bridge: panel original + puertos serie virtuales

Es el modo que pide la consigna de Seminario I para HEXABOT: el programa Java
original se ejecuta **intacto**, se simulan **tres puertos serie** y se
redirigen por **tres puertos TCP/IP** hacia el controlador de Webots.

```
Panel Java ORIGINAL ─► COM10 ═══ COM11 ─► bridge.py ─► TCP 1001 ─► Arduino 1 ┐
   (2017, sin tocar)   COM12 ═══ COM13 ─► bridge.py ─► TCP 1002 ─► Arduino 2 ├ Webots
                       COM14 ═══ COM15 ─► bridge.py ─► TCP 1003 ─► Arduino 3 ┘
                       └─ com0com ─┘
                       (pares virtuales)
```

- **com0com** crea pares de puertos serie virtuales unidos como por un cable:
  lo que el panel escribe en COM10 sale por COM11, y al revés.
- **bridge.py** abre el otro extremo de cada par (COM11, COM13, COM15) y copia
  los bytes en ambos sentidos hacia el puerto TCP del Arduino correspondiente.
  No interpreta el protocolo: reenvía el texto tal cual. Si se reinicia Webots,
  se reconecta solo.
- **El panel original** encuentra los Arduinos con su propio botón *Discover*:
  abre cada puerto serie, manda `A` y se queda con el que responde su número.
- **Webots no cambia**: es el mismo controlador que en el modo TCP directo.

## Instalación (una sola vez por PC)

1. **com0com**: ejecutar `setup.exe` de
   `com0com-2.2.2.0-x64-fre-signed.zip` (SourceForge, proyecto com0com).
   El aviso final sobre .NET Framework se puede ignorar: es para la
   herramienta gráfica, que no se usa.
2. **Pares de puertos**: clic derecho en `crear_puertos_com.bat` →
   *Ejecutar como administrador*. Crea COM10 a COM15.
3. **pyserial**: `python -m pip install pyserial`
4. **Java 8 de 32 bits**: ejecutar `descargar_java8.bat` (portable, queda en
   `java8-32/`, no se instala ni toca el Java del sistema).

## Uso

1. Abrir `webots/worlds/hexabot.wbt` en Webots y darle Play.
2. Ejecutar `iniciar_bridge.bat` (doble clic, o desde PowerShell en esta
   carpeta: `.\iniciar_bridge.bat`). Tiene que mostrar *conectado con Webots*
   para los tres Arduinos. Con `.\iniciar_bridge.bat --ver-trafico` muestra
   cada comando y cada respuesta; útil para la defensa. En PowerShell el `.\`
   es obligatorio: sin él no ejecuta programas de la carpeta actual.
3. Ejecutar `ejecutar_panel_original.bat`. **No abrir `PanelControl1.jar` con
   doble clic**: Windows lo ejecutaría con el Java del sistema (Java 9 o
   posterior), RXTX no encontraría ningún puerto serie y el *Discover* no
   haría nada, sin mostrar ningún error.
4. En el panel: **Send Cmd** → **Discover** (tarda unos segundos) → **Up** →
   joystick.

El orden importa: Webots corriendo, después el bridge (esperar los tres
*conectado con Webots*) y recién después el *Discover*. Si el *Discover* se
hace antes, los comandos `A` se pierden y el panel queda sin Arduinos; hay
que repetirlo. El panel original no avisa cuando el *Discover* falla: el
error solo aparece en la pestaña TRACE.

## ¿Por qué Java 8 de 32 bits?

El panel usa la librería de puerto serie **RXTX 2.2pre2** (de 2009), que
necesita una DLL nativa (`rxtxSerial.dll`). Con Java moderno hay dos
problemas, probados en esta PC:

- RXTX lee al iniciar la propiedad `java.ext.dirs`, que se eliminó en Java 9:
  en Java 25 no encuentra ningún puerto.
- La DLL de 64 bits de RXTX 2.2pre2 es inestable: hace caer la máquina
  virtual (`EXCEPTION_ACCESS_VIOLATION`) al abrir un puerto.

Con Java 8 de 32 bits y la DLL de 32 bits del mismo paquete oficial de RXTX
funciona sin ningún cambio. Es además el entorno para el que se hizo el panel
(compilado con Java 7 en 2017).

## Contenido

| Archivo | Qué es |
|---|---|
| `bridge.py` | El puente serie ↔ TCP (Python + pyserial) |
| `crear_puertos_com.bat` | Crea los 3 pares de puertos con com0com |
| `iniciar_bridge.bat` | Inicia el bridge |
| `descargar_java8.bat` | Baja Java 8 de 32 bits y verifica su huella SHA-256 |
| `ejecutar_panel_original.bat` | Abre el panel original con Java 8 |
| `panel-original/PanelControl1.jar` | El panel de 2017, tal cual |
| `panel-original/lib/RXTXcomm-2.2pre2.jar` | Su librería de puerto serie, tal cual |
| `panel-original/lib/rxtxSerial.dll` | DLL de 32 bits de `rxtx-2.2pre2-bins.zip` (rxtx.qbang.org) |
| `panel-original/etc/config.csv` | Configuración original de pines y PID (botón *Configure*) |

## Resultado de la prueba

Con el panel original: el *Discover* encontró los tres Arduinos (COM10 →
Arduino 1, COM12 → Arduino 2, COM14 → Arduino 3) en 3,2 segundos, y el robot
caminó 1,44 m en 30 segundos con el cuerpo estable a 410 mm.
