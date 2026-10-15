"""
hexabot_tcp.py

Universidad del Norte Santo Tomás de Aquino - Facultad de Ingeniería
Proyecto HEXABOT - simulación en Webots

Simula las tres placas Arduino MEGA del robot real. Cada una escucha en su
propio puerto TCP/IP y habla el mismo protocolo de texto de
ControlArduino4.ino, con sus 6 motores numerados de 1 a 6. La Raspberry Pi
queda afuera: su lugar lo ocupa el panel de control (PanelControl1).

Reparto de motores, igual que en el robot real (Hexapodo.java y
etc/config.csv):

    Arduino 1 - puerto 1001   motor 1 a 6: coxas de las patas 1 a 6
    Arduino 2 - puerto 1002   motor 1/2: fémur/tibia pata 1
                              motor 3/4: fémur/tibia pata 2
                              motor 5/6: fémur/tibia pata 3
    Arduino 3 - puerto 1003   motor 1/2: fémur/tibia pata 4
                              motor 3/4: fémur/tibia pata 5
                              motor 5/6: fémur/tibia pata 6

Comandos de cada Arduino (una línea por comando, terminada en \\n o \\r\\n):
    A                Identificación.               -> OK A <número de Arduino>
    C <param> <val>  Configuración de pines/PID.   -> OK C <param>
                     Se acepta y se ignora: en la simulación no hay pines.
    I <motor>        Lleva el actuador al inicio de carrera (0 pulsos)
                     y le apaga el PID.            -> OK I <motor>
    S <motor> <pos>  Setpoint en pulsos de encoder. -> OK S <motor>
    P                Enciende el PID de sus 6 motores. -> OK P
    H                Detiene sus 6 motores donde están. -> OK H
    Q                Estado: 6 posiciones en pulsos, 6 sensores de inicio de
                     carrera y 6 encoders.         -> OK Q p1 ... p6 i1 ... e6
    G                Solo en la simulación: posición del cuerpo según el GPS,
                     en mm (X hacia el este, Y hacia el norte, Z altura).
                                                   -> OK G <x> <y> <z>

Igual que en el Arduino, un setpoint (S) solo mueve el motor si su PID está
encendido (P), y un comando I apaga el PID de ese motor.

Los puertos se toman de controllerArgs en el mundo (por defecto 1001 1002 1003).
"""

import math
import select
import socket
import sys

from controller import Robot

PUERTOS_POR_DEFECTO = [1001, 1002, 1003]

# Qué articulación maneja cada canal (1 a 6) de cada Arduino
REPARTO = {
    1: ["coxa1", "coxa2", "coxa3", "coxa4", "coxa5", "coxa6"],
    2: ["femur1", "tibia1", "femur2", "tibia2", "femur3", "tibia3"],
    3: ["femur4", "tibia4", "femur5", "tibia5", "femur6", "tibia6"],
}


# ---------------------------------------------------------------------------
# Geometría de los actuadores lineales.
# Son exactamente las fórmulas de Pata.estableceActuadores() del panel: dado
# el ángulo de la articulación (en grados, con las convenciones del panel)
# devuelven el largo del actuador en mm.
# ---------------------------------------------------------------------------

def largo_coxa(ang):
    biela = math.radians(60 + ang)
    return math.hypot(83 - math.cos(biela) * 50, math.sin(biela) * 50)


def largo_femur(ang):
    a = math.radians(-ang)
    return math.hypot(math.cos(a) * 330, math.sin(a) * 330 - 100)


def largo_tibia(ang):
    a = math.radians(90 + ang)
    return math.hypot(322 - math.cos(a) * 80, math.sin(a) * 80)


class Actuador:
    """Un actuador lineal del robot real, montado sobre una articulación
    rotativa de Webots."""

    def __init__(self, robot, nombre, largo, rango, largo_min, mm2pulse,
                 max_pulsos, paso):
        self.nombre = nombre
        self.largo = largo            # función ángulo (grados) -> mm
        self.rango = rango            # ángulos válidos (grados) para invertir
        self.largo_min = largo_min    # mm, equivale a 0 pulsos
        self.mm2pulse = mm2pulse
        self.max_pulsos = max_pulsos
        self.motor = robot.getDevice(nombre)
        self.sensor = robot.getDevice(nombre + "_sensor")
        self.sensor.enable(paso)
        self.pid_activo = False
        self.setpoint = None          # en pulsos; se inicializa al arrancar

    # --- conversiones -----------------------------------------------------
    # En Webots las tres articulaciones usan la misma convención de signo que
    # el panel (ver generar_mundo.py), así que solo cambia la unidad.

    def angulo_desde_pulsos(self, pulsos):
        """Invierte largo(): busca el ángulo cuyo largo de actuador
        corresponde a esos pulsos. Las tres funciones son crecientes en su
        rango, así que alcanza con bisección."""
        objetivo = self.largo_min + pulsos / self.mm2pulse
        bajo, alto = self.rango
        for _ in range(50):
            medio = (bajo + alto) / 2
            if self.largo(medio) < objetivo:
                bajo = medio
            else:
                alto = medio
        return math.radians((bajo + alto) / 2)

    def pulsos_desde_angulo(self, angulo_rad):
        largo = self.largo(math.degrees(angulo_rad))
        return int(round((largo - self.largo_min) * self.mm2pulse))

    # --- estado -----------------------------------------------------------

    def pulsos_actuales(self):
        return self.pulsos_desde_angulo(self.sensor.getValue())

    def ir_a_pulsos(self, pulsos):
        pulsos = max(0, min(self.max_pulsos, pulsos))
        self.motor.setPosition(self.angulo_desde_pulsos(pulsos))

    def establecer_setpoint(self, pulsos):
        self.setpoint = max(0, min(self.max_pulsos, pulsos))
        if self.pid_activo:
            self.ir_a_pulsos(self.setpoint)

    def activar(self):
        self.pid_activo = True
        self.ir_a_pulsos(self.setpoint)

    def detener(self):
        """Como el robot real: sin PWM, el tornillo de avance no se mueve.
        El motor se queda en la posición en la que está."""
        self.pid_activo = False
        self.motor.setPosition(self.sensor.getValue())

    def iniciar_recorrido(self):
        """Comando I: contrae el actuador hasta el inicio de carrera."""
        self.pid_activo = False
        self.setpoint = 0
        self.ir_a_pulsos(0)

    def en_inicio_de_carrera(self):
        return 1 if self.pulsos_actuales() <= 1 else 0


def crear_actuadores(robot, paso):
    """Devuelve los 18 actuadores, indexados por nombre ("coxa1", ...)."""
    # Constantes de Hexapodo.java y Pata.java: 14 pulsos por vuelta,
    # rosca Whitworth (32 hilos/pulgada en la coxa, 24 en fémur y tibia) y
    # la reducción de engranajes de cada coxa.
    reduccion_coxa = [60 / 16, 60 / 15, 60 / 15, 60 / 14, 60 / 15, 64 / 15]
    mm2pulse_coxa = [14.0 * 32.0 * r / 25.4 for r in reduccion_coxa]
    mm2pulse_ft = 14.0 * 24.0 / 25.4
    # Máximos de etc/config.csv (MAX-1 a MAX-6 de cada Arduino)
    max_coxa = [4350, 4200, 4020, 4770, 3850, 4400]
    max_ft = 1500

    actuadores = {}
    for pata in range(1, 7):
        actuadores[f"coxa{pata}"] = Actuador(
            robot, f"coxa{pata}", largo_coxa, (-59.0, 119.0), 40.0,
            mm2pulse_coxa[pata - 1], max_coxa[pata - 1], paso)
        actuadores[f"femur{pata}"] = Actuador(
            robot, f"femur{pata}", largo_femur, (-89.0, 89.0), 295.0,
            mm2pulse_ft, max_ft, paso)
        actuadores[f"tibia{pata}"] = Actuador(
            robot, f"tibia{pata}", largo_tibia, (-89.0, 89.0), 280.0,
            mm2pulse_ft, max_ft, paso)
    return actuadores


# ---------------------------------------------------------------------------
# Arduino virtual: un servidor TCP con 6 motores
# ---------------------------------------------------------------------------

class ArduinoVirtual:
    def __init__(self, numero, puerto, motores, gps):
        self.numero = numero
        self.motores = motores        # lista de 6 Actuador; canal 1 = [0]
        self.gps = gps
        self.escucha = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.escucha.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.escucha.bind(("0.0.0.0", puerto))
        self.escucha.listen(4)
        self.escucha.setblocking(False)
        self.clientes = {}            # socket -> texto recibido sin procesar
        print(f"HEXABOT: Arduino {numero} escuchando en el puerto {puerto}")

    def sockets(self):
        return [self.escucha] + list(self.clientes)

    def atender(self, s):
        """Atiende un socket que select() marcó como listo."""
        if s is self.escucha:
            cliente, direccion = self.escucha.accept()
            cliente.setblocking(False)
            cliente.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            self.clientes[cliente] = ""
            print(f"HEXABOT: Arduino {self.numero}: cliente conectado "
                  f"desde {direccion[0]}")
            return
        try:
            datos = s.recv(4096)
        except OSError:
            datos = b""
        if not datos:
            self.desconectar(s)
            return
        self.clientes[s] += datos.decode("ascii", errors="replace")
        texto = self.clientes[s].replace("\r\n", "\n").replace("\r", "\n")
        *lineas, self.clientes[s] = texto.split("\n")
        for linea in lineas:
            if linea.strip():
                self.responder(s, self.ejecutar(linea.split()))

    def desconectar(self, s):
        print(f"HEXABOT: Arduino {self.numero}: cliente desconectado")
        self.clientes.pop(s, None)
        s.close()

    def responder(self, s, respuesta):
        try:
            s.sendall((respuesta + "\r\n").encode("ascii"))
        except OSError:
            self.desconectar(s)

    def ejecutar(self, partes):
        cmd = partes[0].upper()
        try:
            numeros = [int(a) for a in partes[1:]]
        except ValueError:
            return f"ERR argumento invalido: {' '.join(partes)}"
        motor_valido = len(numeros) >= 1 and 1 <= numeros[0] <= 6

        if cmd == "A" and not numeros:
            return f"OK A {self.numero}"
        if cmd == "C" and len(numeros) == 2:
            return f"OK C {numeros[0]}"
        if cmd == "P" and not numeros:
            for act in self.motores:
                act.activar()
            return "OK P"
        if cmd == "H" and not numeros:
            for act in self.motores:
                act.detener()
            return "OK H"
        if cmd == "Q" and not numeros:
            pulsos = [act.pulsos_actuales() for act in self.motores]
            inicio = [act.en_inicio_de_carrera() for act in self.motores]
            encoders = [0] * len(self.motores)
            return "OK Q " + " ".join(str(v) for v in pulsos + inicio + encoders)
        if cmd == "G" and not numeros:
            x, y, z = self.gps.getValues()
            return f"OK G {x * 1000:.0f} {y * 1000:.0f} {z * 1000:.0f}"
        if cmd == "I" and len(numeros) == 1 and motor_valido:
            self.motores[numeros[0] - 1].iniciar_recorrido()
            return f"OK I {numeros[0]}"
        if cmd == "S" and len(numeros) == 2 and motor_valido:
            self.motores[numeros[0] - 1].establecer_setpoint(numeros[1])
            return f"OK S {numeros[0]}"
        return f"ERR comando invalido: {' '.join(partes)}"


def main():
    robot = Robot()
    paso = int(robot.getBasicTimeStep())
    puertos = [int(p) for p in sys.argv[1:4]] or PUERTOS_POR_DEFECTO

    actuadores = crear_actuadores(robot, paso)
    gps = robot.getDevice("gps")
    gps.enable(paso)
    arduinos = [
        ArduinoVirtual(numero, puerto,
                       [actuadores[nombre] for nombre in REPARTO[numero]], gps)
        for numero, puerto in zip((1, 2, 3), puertos)
    ]

    # Un primer paso para que los sensores tengan valor; el setpoint inicial
    # es la posición actual (robot parado), así un "P" no lo hace caer.
    robot.step(paso)
    for act in actuadores.values():
        act.setpoint = act.pulsos_actuales()

    while robot.step(paso) != -1:
        dueno = {}
        for arduino in arduinos:
            for s in arduino.sockets():
                dueno[s] = arduino
        listos, _, _ = select.select(list(dueno), [], [], 0)
        for s in listos:
            dueno[s].atender(s)


main()
