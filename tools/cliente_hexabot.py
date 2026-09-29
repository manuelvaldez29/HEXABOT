"""
cliente_hexabot.py - Cliente TCP de prueba para el HEXABOT en Webots.

Sirve para probar la simulación sin el panel Java.

Modo interactivo (manda cada línea que escribas y muestra la respuesta):
    python tools/cliente_hexabot.py

Demo de caminata (inicializa, se para y camina N ciclos):
    python tools/cliente_hexabot.py --demo --ciclos 4 --rumbo 0
    rumbo: 0 = adelante, 180 = atrás, 90 = gira a la derecha, -90 = a la izquierda

La marcha es la misma que implementa el panel Java en modo coxas
(Hexapodo.WalkerThread.pasoCoxas): ver docs/MARCHA_COXAS.md.
"""

import argparse
import math
import socket
import sys
import time

# Geometría del actuador de coxa y pulsos/mm (iguales al panel y al controlador)
_REDUCCIONES = [60.0 / 16.0, 60.0 / 15.0, 60.0 / 15.0, 60.0 / 14.0, 60.0 / 15.0, 64.0 / 15.0]
MM2PULSE = [14.0 * 32.0 * r / 25.4 for r in _REDUCCIONES]
LONG_MIN_COXA_MM = 40.0

LADO = [1, 1, 1, -1, -1, -1]           # patas 1-3 a la derecha, 4-6 a la izquierda
PARES_RECUPERACION = [(1, 4), (2, 5), (3, 6)]
TOLERANCIA_PULSOS = 15


def angulo_a_pulsos(pata, grados):
    b = math.radians(60.0 + grados)
    largo = math.hypot(83.0 - math.cos(b) * 50.0, math.sin(b) * 50.0)
    return int((largo - LONG_MIN_COXA_MM) * MM2PULSE[pata - 1])


class Conexion:
    def __init__(self, host, puerto, timeout=2.0):
        self.sock = socket.create_connection((host, puerto), timeout=timeout)
        self.archivo = self.sock.makefile("r", encoding="ascii", newline="\n")

    def comando(self, texto):
        self.sock.sendall((texto + "\n").encode("ascii"))
        respuesta = self.archivo.readline().strip()
        if not respuesta.startswith("OK " + texto[0]):
            raise RuntimeError("Respuesta inesperada a %r: %r" % (texto, respuesta))
        return respuesta[4:].strip()

    def pulsos(self):
        return [int(c) for c in self.comando("Q").split()[:6]]

    def pose(self):
        x, y, z, rumbo = self.comando("G").split()
        return float(x), float(y), float(z), float(rumbo)

    def espera_llegada(self, setpoints, timeout=10.0):
        """Espera a que las coxas lleguen al setpoint (lo que el panel original no hacía)."""
        limite = time.time() + timeout
        while time.time() < limite:
            actuales = self.pulsos()
            if all(abs(actuales[p - 1] - sp) <= TOLERANCIA_PULSOS for p, sp in setpoints.items()):
                return True
            time.sleep(0.05)
        print("  aviso: timeout esperando llegada", setpoints, file=sys.stderr)
        return False


def inicializa(con):
    print("Arduino", con.comando("A"))
    for m in range(1, 7):
        con.comando("I %d" % m)
    limite = time.time() + 10
    while 9999 in con.pulsos():
        if time.time() > limite:
            raise RuntimeError("Las coxas no llegaron al inicio de carrera")
        time.sleep(0.1)
    con.comando("P")
    neutro = {p: angulo_a_pulsos(p, 0.0) for p in range(1, 7)}
    for p, sp in neutro.items():
        con.comando("S %d %d" % (p, sp))
    con.espera_llegada(neutro)


def amplitudes(rumbo_grados, amplitud_grados):
    """Ángulo de recuperación de cada pata. El empuje va al ángulo opuesto."""
    b = math.radians(rumbo_grados)
    return [amplitud_grados * max(-1.0, min(1.0, LADO[i] * math.cos(b) - math.sin(b))) for i in range(6)]


def ciclo(con, rumbo, amplitud):
    angs = amplitudes(rumbo, amplitud)
    # 1) Recuperación: de a dos patas opuestas, las otras cuatro anclan el cuerpo
    for par in PARES_RECUPERACION:
        sps = {p: angulo_a_pulsos(p, angs[p - 1]) for p in par}
        for p, sp in sps.items():
            con.comando("S %d %d" % (p, sp))
        con.espera_llegada(sps)
    # 2) Empuje: las seis patas juntas, el cuerpo avanza
    sps = {p: angulo_a_pulsos(p, -angs[p - 1]) for p in range(1, 7)}
    for p, sp in sps.items():
        con.comando("S %d %d" % (p, sp))
    con.espera_llegada(sps)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--puerto", type=int, default=5000)
    ap.add_argument("--demo", action="store_true")
    ap.add_argument("--ciclos", type=int, default=4)
    ap.add_argument("--rumbo", type=float, default=0.0)
    ap.add_argument("--amplitud", type=float, default=25.0, help="grados de giro de coxa (máx. ~38)")
    args = ap.parse_args()

    con = Conexion(args.host, args.puerto)
    if not args.demo:
        for linea in sys.stdin:
            linea = linea.strip()
            if not linea:
                continue
            con.sock.sendall((linea + "\n").encode("ascii"))
            print(con.archivo.readline().strip())
        return

    inicializa(con)
    x0, y0, _, r0 = con.pose()
    print("Inicio: x=%.0f y=%.0f rumbo=%.1f" % (x0, y0, r0))
    for n in range(1, args.ciclos + 1):
        ciclo(con, args.rumbo, args.amplitud)
        x, y, z, r = con.pose()
        print("Ciclo %d: x=%.0f mm  y=%.0f mm  z=%.0f mm  rumbo=%.1f°  (avance %.0f mm)"
              % (n, x, y, z, r, math.hypot(x - x0, y - y0)))
    con.comando("H")


if __name__ == "__main__":
    main()
