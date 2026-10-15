"""
Cliente de prueba del HEXABOT simulado.

Se conecta por TCP al controlador de Webots y permite escribir comandos a
mano, por ejemplo:

    P              enciende el PID de los 6 motores de ese Arduino
    S 1 300        en el Arduino 2: levanta el fémur de la pata 1 a 300 pulsos
    Q              muestra la posición de los 6 motores de ese Arduino
    H              detiene los motores de ese Arduino

Uso:
    python cliente_prueba.py [host] [puerto]
    python cliente_prueba.py 192.168.0.20 1002

También se pueden pasar comandos por la entrada estándar, uno por línea.
"""

import socket
import sys


def main():
    host = sys.argv[1] if len(sys.argv) > 1 else "localhost"
    puerto = int(sys.argv[2]) if len(sys.argv) > 2 else 1001

    with socket.create_connection((host, puerto), timeout=5) as s:
        lector = s.makefile("r", encoding="ascii", newline="\r\n")
        print(f"Conectado a {host}:{puerto}. Escribí comandos (Ctrl+C para salir).")
        for linea in sys.stdin if not sys.stdin.isatty() else iter(lambda: input("> "), None):
            linea = linea.strip()
            if not linea:
                continue
            s.sendall((linea + "\n").encode("ascii"))
            print(lector.readline().strip())


if __name__ == "__main__":
    try:
        main()
    except (KeyboardInterrupt, EOFError):
        print()
