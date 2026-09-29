"""
hexabot_arduino.py - "Arduino virtual" del HEXABOT en Webots.

Reemplaza al Arduino Mega N.º 1 del robot original (el que manejaba las 6 coxas)
y atiende el mismo protocolo de texto que ControlArduino4.ino, pero por TCP/IP
en lugar del puerto serie. Así el panel de control Java (u otro cliente) manda
los mismos comandos que mandaba al robot real.

Comandos (una línea por comando, terminada en \\n):
    A              Identificación          -> OK A 1
    C <par> <val>  Configuración           -> OK C <par>
    I <m>          Busca inicio de carrera -> OK I <m>
    S <m> <pulsos> Setpoint del motor m    -> OK S <m>
    P              Activa el control       -> OK P
    H              Detiene los motores     -> OK H
    Q              Consulta                -> OK Q  <6 pulsos> <6 inicio> <6 encoder>
    G              (extensión) Pose        -> OK G <x_mm> <y_mm> <z_mm> <rumbo_grados>

Los pulsos se convierten a ángulo de coxa con la misma geometría de biela y las
mismas constantes pulsos/mm que usa el panel (Pata.estableceActuadores y el
constructor de Hexapodo), así un setpoint significa lo mismo que en el robot real.

Argumentos (controllerArgs en el mundo):
    --host=127.0.0.1   interfaz donde escucha (0.0.0.0 para aceptar otras PCs)
    --puerto=5000      puerto TCP
    --velocidad=0.6    velocidad de las coxas en rad/s (máximo: velocidadCoxa del PROTO)
"""

import math
import re
import socket
import sys

from controller import Robot

NRO_ARDUINO = 1
PULSOS_SIN_INICIALIZAR = 9999
TIMEOUT_INICIO_MS = 4000      # igual que buscaInicioRecorrido() del firmware
TIMEOUT_COMANDO_MS = 1000     # igual que el timeout del parser del firmware
TOLERANCIA_RAD = 0.005

# Geometría del actuador de coxa (Pata.estableceActuadores): la tuerca de avance
# está a 83 mm del eje de la coxa y la biela mide 50 mm. Con ángulo de coxa 0º
# la biela forma 60º con el radio característico.
DIST_TUERCA_MM = 83.0
LARGO_BIELA_MM = 50.0
ANG_BIELA_0_GRADOS = 60.0
LONG_MIN_COXA_MM = 40.0       # Pata: m_actCoxa.setMinLongitud(40.0)

# Pulsos por mm de cada coxa (constructor de Hexapodo): 14 pulsos/vuelta,
# rosca WW 5/32" (32 vueltas/pulgada) y la reducción de cada pata.
_REDUCCIONES = [60.0 / 16.0, 60.0 / 15.0, 60.0 / 15.0, 60.0 / 14.0, 60.0 / 15.0, 64.0 / 15.0]
MM2PULSE = [14.0 * 32.0 * r / 25.4 for r in _REDUCCIONES]

# Máximo de pulsos por coxa (config.csv, parámetros 81 a 86 del Arduino 1)
MAX_PULSOS = [4350, 4200, 4020, 4770, 3850, 4400]


def largo_a_angulo(largo_mm):
    """Largo del actuador (mm) -> ángulo de coxa (rad). Inversa del teorema del coseno."""
    cos_b = (DIST_TUERCA_MM ** 2 + LARGO_BIELA_MM ** 2 - largo_mm ** 2) / (2 * DIST_TUERCA_MM * LARGO_BIELA_MM)
    cos_b = max(-1.0, min(1.0, cos_b))
    return math.acos(cos_b) - math.radians(ANG_BIELA_0_GRADOS)


def angulo_a_largo(angulo_rad):
    """Ángulo de coxa (rad) -> largo del actuador (mm), como Pata.estableceActuadores."""
    b = math.radians(ANG_BIELA_0_GRADOS) + angulo_rad
    return math.hypot(DIST_TUERCA_MM - math.cos(b) * LARGO_BIELA_MM, math.sin(b) * LARGO_BIELA_MM)


class ActuadorCoxa:
    """Equivalente a la clase Actuador del firmware, sobre un motor de Webots."""

    def __init__(self, robot, indice, paso_ms, velocidad):
        self.indice = indice
        self.motor = robot.getDevice("coxa%d" % (indice + 1))
        self.sensor = robot.getDevice("coxa%d_sensor" % (indice + 1))
        self.sensor.enable(paso_ms)
        self.velocidad = min(velocidad, self.motor.getMaxVelocity())
        self.max_pulsos = MAX_PULSOS[indice]
        self.inicializado = False
        self.pid_activo = False
        self.setpoint = 0
        self.momento_inicializacion = None
        # Al arrancar mantiene la posición en la que está
        self.motor.setPosition(0.0)

    def pulsos_a_angulo(self, pulsos):
        return largo_a_angulo(LONG_MIN_COXA_MM + pulsos / MM2PULSE[self.indice])

    def angulo_a_pulsos(self, angulo):
        return (angulo_a_largo(angulo) - LONG_MIN_COXA_MM) * MM2PULSE[self.indice]

    def angulo(self):
        return self.sensor.getValue()

    def pulsos(self):
        if not self.inicializado:
            return PULSOS_SIN_INICIALIZAR
        return max(0, int(round(self.angulo_a_pulsos(self.angulo()))))

    def sensor_inicio(self):
        return 1 if self.angulo() <= self.pulsos_a_angulo(0) + TOLERANCIA_RAD else 0

    def sensor_encoder(self):
        p = self.pulsos()
        return 0 if p == PULSOS_SIN_INICIALIZAR else p & 1

    def mantener(self):
        self.motor.setPosition(self.angulo())

    def inicia_recorrido(self, ahora_ms):
        """Comando I: contrae hasta el inicio de carrera, como iniciaRecorrido()."""
        self.inicializado = False
        self.pid_activo = False
        self.setpoint = 0
        self.momento_inicializacion = ahora_ms
        self.motor.setVelocity(self.motor.getMaxVelocity())
        self.motor.setPosition(self.pulsos_a_angulo(0))

    def busca_inicio_recorrido(self, ahora_ms):
        if self.momento_inicializacion is None:
            return
        lap = ahora_ms - self.momento_inicializacion
        llego = abs(self.angulo() - self.pulsos_a_angulo(0)) <= TOLERANCIA_RAD
        if llego or lap > TIMEOUT_INICIO_MS:
            self.momento_inicializacion = None
            self.inicializado = llego
            self.mantener()

    def aplica_setpoint(self):
        if not (self.pid_activo and self.inicializado):
            return
        pulsos = max(0, min(self.setpoint, self.max_pulsos))
        self.motor.setVelocity(self.velocidad)
        self.motor.setPosition(self.pulsos_a_angulo(pulsos))

    def set_setpoint(self, pulsos):
        self.setpoint = pulsos
        self.aplica_setpoint()

    def activa(self):
        self.pid_activo = True
        self.aplica_setpoint()

    def detiene(self):
        self.pid_activo = False
        self.mantener()


class ArduinoVirtual:
    RE_C = re.compile(r"^C\s*(\d+)\s+(\d+)$")
    RE_S = re.compile(r"^S\s*([1-6])\s+(\d+)$")
    RE_I = re.compile(r"^I\s*([1-6])$")

    def __init__(self, robot, host, puerto, velocidad):
        self.robot = robot
        self.paso_ms = int(robot.getBasicTimeStep())
        self.acts = [ActuadorCoxa(robot, i, self.paso_ms, velocidad) for i in range(6)]
        self.gps = robot.getDevice("gps")
        self.gps.enable(self.paso_ms)
        self.imu = robot.getDevice("imu")
        self.imu.enable(self.paso_ms)

        self.servidor = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.servidor.bind((host, puerto))
        self.servidor.listen(1)
        self.servidor.setblocking(False)
        self.cliente = None
        self.buffer = ""
        self.momento_inicio_comando = None
        print("HEXABOT: Arduino virtual %d escuchando en %s:%d" % (NRO_ARDUINO, host, puerto))

    def ahora_ms(self):
        return int(self.robot.getTime() * 1000)

    # ---- Red -------------------------------------------------------------
    def atiende_red(self):
        try:
            nuevo, direccion = self.servidor.accept()
            nuevo.setblocking(False)
            nuevo.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            if self.cliente is not None:
                self.cliente.close()
            self.cliente = nuevo
            self.buffer = ""
            print("HEXABOT: cliente conectado desde %s:%d" % direccion)
        except BlockingIOError:
            pass

        if self.cliente is None:
            return
        try:
            datos = self.cliente.recv(4096)
            if not datos:
                self._desconecta()
                return
            if not self.buffer:
                self.momento_inicio_comando = self.ahora_ms()
            self.buffer += datos.decode("ascii", errors="replace")
        except BlockingIOError:
            pass
        except OSError:
            self._desconecta()
            return

        while True:
            corte = re.search(r"[\r\n]", self.buffer)
            if corte is None:
                break
            linea = self.buffer[:corte.start()].strip()
            self.buffer = self.buffer[corte.end():]
            if linea:
                self.responde(self.ejecuta(linea))
        if not self.buffer:
            self.momento_inicio_comando = None
        elif self.ahora_ms() - self.momento_inicio_comando > TIMEOUT_COMANDO_MS:
            # Timeout del parser: descarta el comando incompleto
            self.buffer = ""
            self.momento_inicio_comando = None

    def _desconecta(self):
        print("HEXABOT: cliente desconectado")
        self.cliente.close()
        self.cliente = None
        self.buffer = ""

    def responde(self, texto):
        if self.cliente is None:
            return
        try:
            self.cliente.sendall((texto + "\r\n").encode("ascii"))
        except OSError:
            self._desconecta()

    # ---- Protocolo ------------------------------------------------------
    def ejecuta(self, linea):
        if linea == "A":
            return "OK A %d" % NRO_ARDUINO
        if linea == "Q":
            campos = [a.pulsos() for a in self.acts]
            campos += [a.sensor_inicio() for a in self.acts]
            campos += [a.sensor_encoder() for a in self.acts]
            # El firmware arma la respuesta empezando con un espacio: "OK Q  p1 p2 ..."
            return "OK Q  " + " ".join(str(c) for c in campos)
        if linea == "H":
            for a in self.acts:
                a.detiene()
            return "OK H"
        if linea == "P":
            for a in self.acts:
                a.activa()
            return "OK P"
        if linea == "G":
            x, y, z = self.gps.getValues()
            rumbo = math.degrees(self.imu.getRollPitchYaw()[2])
            return "OK G %.0f %.0f %.0f %.1f" % (x * 1000, y * 1000, z * 1000, rumbo)

        m = self.RE_S.match(linea)
        if m:
            motor = int(m.group(1))
            self.acts[motor - 1].set_setpoint(int(m.group(2)))
            return "OK S %d" % motor
        m = self.RE_I.match(linea)
        if m:
            motor = int(m.group(1))
            self.acts[motor - 1].inicia_recorrido(self.ahora_ms())
            return "OK I %d" % motor
        m = self.RE_C.match(linea)
        if m:
            param, valor = int(m.group(1)), int(m.group(2))
            if 81 <= param <= 86:
                self.acts[param - 81].max_pulsos = valor
            # Pines, LEDs y ganancias PID no aplican en la simulación:
            # el control de posición lo hace el motor de Webots.
            return "OK C %d" % param
        return "ERR cmd=%s" % linea

    def paso(self):
        ahora = self.ahora_ms()
        for a in self.acts:
            a.busca_inicio_recorrido(ahora)
        self.atiende_red()


def lee_argumentos(argv):
    args = {"host": "127.0.0.1", "puerto": "5000", "velocidad": "0.6"}
    for arg in argv:
        if arg.startswith("--") and "=" in arg:
            clave, valor = arg[2:].split("=", 1)
            args[clave] = valor
    return args["host"], int(args["puerto"]), float(args["velocidad"])


def main():
    robot = Robot()
    host, puerto, velocidad = lee_argumentos(sys.argv[1:])
    arduino = ArduinoVirtual(robot, host, puerto, velocidad)
    while robot.step(arduino.paso_ms) != -1:
        arduino.paso()


if __name__ == "__main__":
    main()
