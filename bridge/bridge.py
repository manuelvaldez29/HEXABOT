"""
bridge.py - Puente serie <-> TCP/IP del HEXABOT.

Universidad del Norte Santo Tomás de Aquino - Facultad de Ingeniería
Seminario I - Gemelos digitales

El panel de control original (Java, 2017) habla con tres Arduino MEGA por
puerto serie. En la simulación, los Arduinos están dentro de Webots y escuchan
por TCP/IP. Este programa los une sin modificar ninguno de los dos lados:

    Panel Java ── COM10 ══ COM11 ── bridge.py ── TCP 1001 ── Arduino 1 (Webots)
    (original)    COM12 ══ COM13 ── bridge.py ── TCP 1002 ── Arduino 2
                  COM14 ══ COM15 ── bridge.py ── TCP 1003 ── Arduino 3

Los pares COM10/COM11, COM12/COM13 y COM14/COM15 son puertos virtuales creados
con com0com: lo que el panel escribe en COM10 sale por COM11, y viceversa,
como si fuera un cable. El bridge abre el extremo libre de cada par y copia
los bytes en ambos sentidos hacia el puerto TCP del Arduino correspondiente.
No interpreta el protocolo: reenvía el texto tal cual.

Si Webots todavía no arrancó, o se reinicia la simulación, el bridge vuelve a
conectarse solo cada 2 segundos; el panel no necesita reconectarse.

Uso:
    python bridge.py                      Webots en esta misma PC
    python bridge.py --host 192.168.0.20  Webots en otra PC
    python bridge.py --ver-trafico        muestra cada comando y respuesta

Requiere pyserial:  python -m pip install pyserial
"""

import argparse
import socket
import threading
import time

import serial

# Extremo del bridge de cada par de com0com -> puerto TCP del Arduino
CANALES = [
    ("Arduino 1", "COM11", 1001),
    ("Arduino 2", "COM13", 1002),
    ("Arduino 3", "COM15", 1003),
]

ESPERA_RECONEXION = 2.0   # segundos entre intentos de conexión con Webots


class Canal:
    """Une un puerto serie virtual con un puerto TCP de Webots."""

    def __init__(self, nombre, puerto_com, host, puerto_tcp, ver_trafico):
        self.nombre = nombre
        self.puerto_com = puerto_com
        self.destino = (host, puerto_tcp)
        self.ver_trafico = ver_trafico
        self.serie = None
        self.tcp = None              # socket conectado con Webots, o None

    def log(self, mensaje):
        print(f"[{self.nombre} {self.puerto_com} <-> "
              f"{self.destino[0]}:{self.destino[1]}] {mensaje}", flush=True)

    def iniciar(self):
        # La velocidad no importa en un puerto virtual, pero se usa la misma
        # del Arduino (115200 baudios) por prolijidad.
        self.serie = serial.Serial(self.puerto_com, 115200, timeout=0.05)
        self.log("puerto serie abierto")
        threading.Thread(target=self.serie_a_tcp, daemon=True).start()
        threading.Thread(target=self.tcp_a_serie, daemon=True).start()

    def serie_a_tcp(self):
        """Lo que manda el panel por el puerto serie va hacia Webots."""
        while True:
            datos = self.serie.read(self.serie.in_waiting or 1)
            if not datos:
                continue
            if self.ver_trafico:
                self.log(f"panel  -> webots: {mostrar(datos)}")
            conexion = self.tcp
            if conexion is None:
                self.log(f"sin conexión con Webots; se descarta {mostrar(datos)}")
                continue
            try:
                conexion.sendall(datos)
            except OSError:
                self.log("se perdió la conexión con Webots al enviar")

    def tcp_a_serie(self):
        """Mantiene la conexión con Webots y copia sus respuestas al panel."""
        while True:
            try:
                conexion = socket.create_connection(self.destino, timeout=3)
            except OSError:
                time.sleep(ESPERA_RECONEXION)
                continue
            conexion.settimeout(None)
            conexion.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            self.tcp = conexion
            self.log("conectado con Webots")
            try:
                while True:
                    datos = conexion.recv(4096)
                    if not datos:
                        break
                    if self.ver_trafico:
                        self.log(f"webots -> panel : {mostrar(datos)}")
                    self.serie.write(datos)
            except OSError:
                pass
            self.tcp = None
            conexion.close()
            self.log("Webots cerró la conexión; reintentando...")
            time.sleep(ESPERA_RECONEXION)


def mostrar(datos):
    """Texto legible de los bytes, sin los fines de línea."""
    return datos.decode("ascii", errors="replace").strip().replace("\r\n", " | ")


def main():
    argumentos = argparse.ArgumentParser(description=__doc__.split("\n")[1])
    argumentos.add_argument("--host", default="localhost",
                            help="PC donde corre Webots (por defecto localhost)")
    argumentos.add_argument("--ver-trafico", action="store_true",
                            help="muestra cada comando y cada respuesta")
    opciones = argumentos.parse_args()

    canales = [Canal(nombre, com, opciones.host, puerto, opciones.ver_trafico)
               for nombre, com, puerto in CANALES]
    for canal in canales:
        try:
            canal.iniciar()
        except serial.SerialException as error:
            print(f"No se pudo abrir {canal.puerto_com}: {error}")
            print("¿Están creados los pares de puertos de com0com? "
                  "Ver bridge/README.md")
            return

    print("Bridge funcionando. Ctrl+C para terminar.", flush=True)
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nBridge detenido.")


if __name__ == "__main__":
    main()
