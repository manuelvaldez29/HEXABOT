"""
Pruebas del Arduino virtual (webots/controllers/hexabot_arduino) sin Webots.

Levanta el controlador con el módulo "controller" falso de tests/falso_webots
y le habla por TCP como lo haría el panel. Ejecutar desde la raíz del repo:

    python -m unittest discover -s tests -v
"""

import math
import os
import socket
import subprocess
import sys
import time
import unittest

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(RAIZ, "tools"))
sys.path.insert(0, os.path.join(RAIZ, "webots", "controllers", "hexabot_arduino"))
sys.path.insert(0, os.path.join(RAIZ, "tests", "falso_webots"))

import cliente_hexabot as cliente  # noqa: E402
import hexabot_arduino as arduino  # noqa: E402

PUERTO = 5055


class TestGeometria(unittest.TestCase):
    def test_ida_y_vuelta_angulo_largo(self):
        for grados in (-39.0, -20.0, 0.0, 15.0, 38.0):
            a = math.radians(grados)
            self.assertAlmostEqual(arduino.largo_a_angulo(arduino.angulo_a_largo(a)), a, places=9)

    def test_inicio_de_carrera_cerca_de_menos_40_grados(self):
        # Hexapodo.cmdSit usa -40º "estimado" para la coxa contraída
        grados = math.degrees(arduino.largo_a_angulo(arduino.LONG_MIN_COXA_MM))
        self.assertAlmostEqual(grados, -39.8, delta=0.2)

    def test_cliente_y_controlador_usan_la_misma_conversion(self):
        for pata in range(1, 7):
            act = arduino.ActuadorCoxa.__new__(arduino.ActuadorCoxa)
            act.indice = pata - 1
            for grados in (-25.0, 0.0, 25.0):
                self.assertAlmostEqual(act.angulo_a_pulsos(math.radians(grados)),
                                       cliente.angulo_a_pulsos(pata, grados), delta=1.0)


class TestProtocolo(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        env = dict(os.environ, PYTHONPATH=os.path.join(RAIZ, "tests", "falso_webots"))
        cls.proc = subprocess.Popen(
            [sys.executable, os.path.join(RAIZ, "webots", "controllers", "hexabot_arduino", "hexabot_arduino.py"),
             "--puerto=%d" % PUERTO],
            env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        limite = time.time() + 10
        while True:
            try:
                cls.con = cliente.Conexion("127.0.0.1", PUERTO)
                break
            except OSError:
                if time.time() > limite:
                    raise
                time.sleep(0.1)

    @classmethod
    def tearDownClass(cls):
        cls.con.sock.close()
        cls.proc.kill()
        cls.proc.wait()

    def crudo(self, texto):
        self.con.sock.sendall((texto + "\n").encode("ascii"))
        return self.con.archivo.readline().strip()

    def test_1_identificacion(self):
        self.assertEqual(self.crudo("A"), "OK A 1")

    def test_2_sin_inicializar_responde_9999(self):
        respuesta = self.crudo("Q")
        self.assertTrue(respuesta.startswith("OK Q  "), respuesta)
        campos = respuesta.split()[2:]
        self.assertEqual(len(campos), 18)
        self.assertEqual(campos[:6], ["9999"] * 6)

    def test_3_setpoint_sin_inicializar_no_mueve(self):
        self.assertEqual(self.crudo("P"), "OK P")
        self.assertEqual(self.crudo("S 1 2000"), "OK S 1")
        time.sleep(0.3)
        self.assertEqual(self.con.pulsos()[0], 9999)

    def test_4_inicio_setpoint_y_llegada(self):
        cliente.inicializa(self.con)
        pulsos = self.con.pulsos()
        neutros = [cliente.angulo_a_pulsos(p, 0.0) for p in range(1, 7)]
        for actual, esperado in zip(pulsos, neutros):
            self.assertLessEqual(abs(actual - esperado), cliente.TOLERANCIA_PULSOS)

        sp = cliente.angulo_a_pulsos(2, 20.0)
        self.assertEqual(self.crudo("S2 %d" % sp), "OK S 2")  # formato sin espacio, como el firmware
        self.assertTrue(self.con.espera_llegada({2: sp}))

    def test_5_limite_maximo_configurable(self):
        self.assertEqual(self.crudo("C 83 1000"), "OK C 83")
        self.assertEqual(self.crudo("S 3 4000"), "OK S 3")
        self.assertTrue(self.con.espera_llegada({3: 1000}))

    def test_6_detencion(self):
        self.assertEqual(self.crudo("S 4 3000"), "OK S 4")
        time.sleep(0.2)
        self.assertEqual(self.crudo("H"), "OK H")
        antes = self.con.pulsos()[3]
        time.sleep(0.5)
        self.assertLessEqual(abs(self.con.pulsos()[3] - antes), 5)

    def test_7_comando_invalido(self):
        self.assertTrue(self.crudo("Z 1").startswith("ERR"))

    def test_8_pose(self):
        respuesta = self.crudo("G")
        self.assertEqual(respuesta.split()[:2], ["OK", "G"])
        self.assertEqual(len(respuesta.split()), 6)


class TestMarcha(unittest.TestCase):
    def test_adelante_derechas_positivo_izquierdas_negativo(self):
        angs = cliente.amplitudes(0.0, 25.0)
        self.assertEqual(angs, [25.0, 25.0, 25.0, -25.0, -25.0, -25.0])

    def test_giro_en_el_lugar_mismo_sentido(self):
        izquierda = cliente.amplitudes(-90.0, 25.0)
        for a in izquierda:
            self.assertAlmostEqual(a, 25.0)
        derecha = cliente.amplitudes(90.0, 25.0)
        for a in derecha:
            self.assertAlmostEqual(a, -25.0)


if __name__ == "__main__":
    unittest.main()
