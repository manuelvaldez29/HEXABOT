"""
Módulo "controller" falso, para probar hexabot_arduino.py sin Webots.

Imita la parte de la API de Webots que usa el controlador: motores de posición
con velocidad limitada, sensores de posición, GPS e IMU. No simula física: el
GPS queda fijo. Sirve para probar el protocolo TCP y la lógica del Arduino
virtual (inicio de carrera, setpoints, llegada, consultas Q).
"""

import time


class _Motor:
    def __init__(self):
        self.posicion = 0.0
        self.objetivo = 0.0
        self.velocidad = 1.0

    def getMaxVelocity(self):
        return 1.0

    def setVelocity(self, v):
        self.velocidad = v

    def setPosition(self, p):
        self.objetivo = max(-0.72, min(0.80, p))

    def avanza(self, dt):
        paso = self.velocidad * dt
        delta = self.objetivo - self.posicion
        self.posicion += max(-paso, min(paso, delta))


class _Sensor:
    def __init__(self, motor):
        self.motor = motor

    def enable(self, _ms):
        pass

    def getValue(self):
        return self.motor.posicion


class _Gps:
    def enable(self, _ms):
        pass

    def getValues(self):
        return [0.0, 0.0, 0.41]


class _Imu:
    def enable(self, _ms):
        pass

    def getRollPitchYaw(self):
        return [0.0, 0.0, 0.0]


class Robot:
    # Factor de aceleración respecto del tiempo real (1 = tiempo real)
    ACELERACION = 4.0

    def __init__(self):
        self.tiempo = 0.0
        self.motores = {"coxa%d" % i: _Motor() for i in range(1, 7)}
        self.dispositivos = dict(self.motores)
        for i in range(1, 7):
            self.dispositivos["coxa%d_sensor" % i] = _Sensor(self.motores["coxa%d" % i])
        self.dispositivos["gps"] = _Gps()
        self.dispositivos["imu"] = _Imu()

    def getBasicTimeStep(self):
        return 8.0

    def getDevice(self, nombre):
        return self.dispositivos[nombre]

    def getTime(self):
        return self.tiempo

    def step(self, ms):
        dt = ms / 1000.0
        for m in self.motores.values():
            m.avanza(dt)
        self.tiempo += dt
        time.sleep(dt / self.ACELERACION)
        return 0
