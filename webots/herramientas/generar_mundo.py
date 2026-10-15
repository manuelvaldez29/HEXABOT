"""
Generador del mundo de Webots del HEXABOT.

Escribe ../worlds/hexabot.wbt a partir de las medidas del robot real
(documentación oficial, capítulo 4.4, y PanelControl1/model/Pata.java).
Si cambian alguna medida, editen las constantes de abajo y vuelvan a correr:

    python generar_mundo.py

Sistema de coordenadas (el mismo que usa el panel de control):
  - Origen: centro de la placa hexagonal inferior del cuerpo.
  - X/Y horizontales, Z hacia arriba. "Norte" (rumbo 0º) es +Y.
  - Las patas se numeran 1 a 6 en sentido horario. Radio característico:
    pata 1 = 60º, pata 2 = 0º, pata 3 = -60º, pata 4 = -120º,
    pata 5 = 180º, pata 6 = 120º.

Ángulos de las articulaciones (en Webots, todos en 0 = robot parado, "Up"):
  - Coxa:  giro horizontal; positivo = antihorario visto desde arriba.
  - Fémur: positivo = la rodilla baja.
  - Tibia: positivo = el pie se abre hacia afuera.
"""

import math
import os

# --- Medidas en metros (de Pata.java y de la documentación) -----------------
RADIO_COXA = 0.150        # distancia del centro del cuerpo al eje de cada coxa
ALTURA_CUERPO = 0.220     # separación entre las placas A y B
ALTURA_FEMUR = 0.065      # altura de la articulación coxa-fémur sobre la placa A
LARGO_FEMUR = 0.380       # entre ejes (el caño mide 430 mm)
LARGO_TIBIA = 0.475       # de la rodilla al punto de apoyo
ALTURA_INICIAL = 0.410    # del piso al origen (Hexapodo.m_height)

# --- Masas en kg (estimadas: chapa de acero 1,5 mm, caño de aluminio) -------
MASA_CUERPO = 8.0
MASA_COXA = 0.4
MASA_FEMUR = 1.2          # incluye los dos actuadores montados adentro
MASA_TIBIA = 0.4

# --- Motores ----------------------------------------------------------------
# Velocidad máxima de cada articulación (rad/s) y torque máximo (N·m).
# El robot real era más débil y más lento; acá se dejan valores con los que la
# simulación camina. Para imitar al real, bajen torque y velocidad.
VEL_COXA, TORQUE_COXA = 1.0, 60.0
VEL_FEMUR, TORQUE_FEMUR = 1.5, 120.0
VEL_TIBIA, TORQUE_TIBIA = 1.5, 120.0

# Límites de las articulaciones (rad). Corresponden al recorrido completo de
# cada actuador (0 a MAX pulsos), con un pequeño margen.
LIM_COXA = (-0.75, 0.85)
LIM_FEMUR = (-0.55, 0.85)
LIM_TIBIA = (-0.70, 1.10)

RADIALES = [60, 0, -60, -120, 180, 120]   # grados, patas 1 a 6

# --- Colores (como en las fotos) --------------------------------------------
AZUL = "0.10 0.16 0.50"
GRIS_MOTOR = "0.62 0.62 0.64"
VERDE_PLA = "0.62 0.90 0.12"
NEGRO = "0.08 0.08 0.08"
PCB = "0.55 0.30 0.12"
PCB_ARDUINO = "0.05 0.35 0.55"
CELESTE = "0.45 0.75 0.95"


def apariencia(color, rugosidad=0.5, metal=0.2):
    return (f"PBRAppearance {{ baseColor {color} roughness {rugosidad} "
            f"metalness {metal} }}")


def forma(geometria, color, rugosidad=0.5, metal=0.2):
    return (f"Shape {{ appearance {apariencia(color, rugosidad, metal)} "
            f"geometry {geometria} }}")


def pose(x, y, z, hijos, rotacion="0 0 1 0"):
    return (f"Pose {{ translation {x:.4f} {y:.4f} {z:.4f} "
            f"rotation {rotacion} children [ {hijos} ] }}")


def caja(x, y, z):
    return f"Box {{ size {x:.4f} {y:.4f} {z:.4f} }}"


def cilindro(radio, alto, lados=24):
    return (f"Cylinder {{ radius {radio:.4f} height {alto:.4f} "
            f"subdivision {lados} }}")


def eje_angulo_desde_yaw_pitch(yaw, pitch):
    """Rotación Rz(yaw)·Ry(pitch) expresada como eje-ángulo (para Viewpoint)."""
    cy, sy = math.cos(yaw / 2), math.sin(yaw / 2)
    cp, sp = math.cos(pitch / 2), math.sin(pitch / 2)
    # q = qz * qy
    w = cy * cp
    x = -sy * sp
    y = cy * sp
    z = sy * cp
    angulo = 2 * math.acos(max(-1.0, min(1.0, w)))
    s = math.sqrt(max(1e-12, 1 - w * w))
    return f"{x / s:.4f} {y / s:.4f} {z / s:.4f} {angulo:.4f}"


def cuerpo():
    """Formas del cuerpo: placas hexagonales, columnas, actuadores de coxa y
    los tres Arduino. Todo fijo al Robot."""
    piezas = []
    # Placas A (inferior) y B (superior): hexágonos de 300 mm de diámetro.
    # El Cylinder de 6 lados tiene un vértice sobre +X, igual que las coxas.
    for z in (0.0, ALTURA_CUERPO):
        piezas.append(pose(0, 0, z, forma(cilindro(0.16, 0.004, 6), AZUL)))
    # Placa C (bancada de las tuercas de avance de coxa)
    piezas.append(pose(0, 0, ALTURA_CUERPO + 0.05,
                       forma(cilindro(0.115, 0.004, 6), AZUL),
                       "0 0 1 0.5236"))
    # Columnas de caño roscado entre placas
    for k in range(6):
        a = math.radians(30 + 60 * k)
        x, y = 0.11 * math.cos(a), 0.11 * math.sin(a)
        piezas.append(pose(x, y, ALTURA_CUERPO / 2,
                           forma(cilindro(0.005, ALTURA_CUERPO, 10),
                                 GRIS_MOTOR, 0.3, 0.8)))
        piezas.append(pose(x * 0.9, y * 0.9, ALTURA_CUERPO + 0.025,
                           forma(cilindro(0.005, 0.05, 10),
                                 GRIS_MOTOR, 0.3, 0.8)))
    # Motores de los actuadores de coxa, apoyados sobre la placa B
    for radial in RADIALES:
        a = math.radians(radial)
        # El motor queda sobre el radio, hacia adentro de la coxa, acostado
        # en dirección tangencial
        x, y = 0.085 * math.cos(a), 0.085 * math.sin(a)
        rot_tangencial = f"{math.cos(a):.4f} {math.sin(a):.4f} 0 1.5708"
        piezas.append(pose(x, y, ALTURA_CUERPO + 0.025,
                           forma(cilindro(0.018, 0.055), GRIS_MOTOR, 0.4, 0.6),
                           rot_tangencial))
        # Engranaje (corona de 60 dientes)
        piezas.append(pose(x * 1.25, y * 1.25, ALTURA_CUERPO + 0.025,
                           forma(cilindro(0.022, 0.006), "0.85 0.85 0.85",
                                 0.3, 0.1), rot_tangencial))
    # Los tres Arduino MEGA con sus shields, sobre la placa C
    for k in range(3):
        a = math.radians(90 + 120 * k)
        x, y = 0.05 * math.cos(a), 0.05 * math.sin(a)
        piezas.append(pose(x, y, ALTURA_CUERPO + 0.065,
                           forma(caja(0.10, 0.053, 0.012), PCB_ARDUINO, 0.6, 0),
                           f"0 0 1 {a:.4f}"))
    # Placas de distribución de las coxas (marrones, en los costados)
    for k in range(3):
        a = math.radians(30 + 120 * k)
        x, y = 0.135 * math.cos(a), 0.135 * math.sin(a)
        piezas.append(pose(x, y, 0.12,
                           forma(caja(0.003, 0.09, 0.07), PCB, 0.7, 0),
                           f"0 0 1 {a:.4f}"))
    return "\n      ".join(piezas)


def pata(n, radial):
    """Una pata completa: coxa (motor vertical), fémur y tibia."""
    a = math.radians(radial)
    x, y = RADIO_COXA * math.cos(a), RADIO_COXA * math.sin(a)

    tibia = f"""HingeJoint {{
                jointParameters HingeJointParameters {{
                  axis 0 -1 0
                  anchor {LARGO_FEMUR:.4f} 0 0
                }}
                device [
                  RotationalMotor {{
                    name "tibia{n}"
                    minPosition {LIM_TIBIA[0]}
                    maxPosition {LIM_TIBIA[1]}
                    maxVelocity {VEL_TIBIA}
                    maxTorque {TORQUE_TIBIA}
                  }}
                  PositionSensor {{ name "tibia{n}_sensor" }}
                ]
                endPoint Solid {{
                  translation {LARGO_FEMUR:.4f} 0 0
                  children [
                    {pose(0, 0, -LARGO_TIBIA / 2, forma(caja(0.02, 0.02, LARGO_TIBIA), AZUL))}
                    {pose(0, 0, -LARGO_TIBIA + 0.02, forma("Sphere { radius 0.02 subdivision 2 }", NEGRO, 0.9, 0))}
                    {pose(0, 0, -0.03, forma(cilindro(0.012, 0.03), VERDE_PLA, 0.7, 0), "1 0 0 1.5708")}
                  ]
                  name "tibia{n}"
                  contactMaterial "pie"
                  boundingObject Group {{
                    children [
                      {pose(0, 0, -LARGO_TIBIA / 2 + 0.02, caja(0.02, 0.02, LARGO_TIBIA - 0.04))}
                      {pose(0, 0, -LARGO_TIBIA + 0.02, "Sphere { radius 0.02 }")}
                    ]
                  }}
                  physics Physics {{ density -1 mass {MASA_TIBIA} centerOfMass [ 0 0 {-LARGO_TIBIA / 2:.4f} ] }}
                }}
              }}"""

    # Detalle del fémur: caño de 50x50, con los dos motores y sus soportes
    # verdes impresos en PLA, como en las fotos.
    detalles_femur = [
        pose(0.19, 0, 0, forma(caja(0.43, 0.05, 0.05), AZUL)),
        pose(0.12, 0, 0.032, forma(cilindro(0.017, 0.07), GRIS_MOTOR, 0.4, 0.6), "0 1 0 1.5708"),
        pose(0.085, 0, 0.032, forma(caja(0.008, 0.045, 0.045), VERDE_PLA, 0.7, 0)),
        pose(0.155, 0, 0.032, forma(caja(0.008, 0.045, 0.045), VERDE_PLA, 0.7, 0)),
        pose(0.21, 0, 0.032, forma(cilindro(0.008, 0.12), NEGRO, 0.6, 0), "0 1 0 1.5708"),
        pose(0.29, 0, -0.032, forma(cilindro(0.017, 0.07), GRIS_MOTOR, 0.4, 0.6), "0 1 0 1.5708"),
        pose(0.255, 0, -0.032, forma(caja(0.008, 0.045, 0.045), VERDE_PLA, 0.7, 0)),
        pose(0.325, 0, -0.032, forma(caja(0.008, 0.045, 0.045), VERDE_PLA, 0.7, 0)),
        pose(0.30, 0.026, 0, forma(caja(0.08, 0.002, 0.04), CELESTE, 0.8, 0)),
        pose(0.05, -0.027, 0, forma(caja(0.07, 0.003, 0.045), PCB, 0.7, 0)),
    ]
    femur = f"""HingeJoint {{
            jointParameters HingeJointParameters {{
              axis 0 1 0
              anchor 0 0 {ALTURA_FEMUR:.4f}
            }}
            device [
              RotationalMotor {{
                name "femur{n}"
                minPosition {LIM_FEMUR[0]}
                maxPosition {LIM_FEMUR[1]}
                maxVelocity {VEL_FEMUR}
                maxTorque {TORQUE_FEMUR}
              }}
              PositionSensor {{ name "femur{n}_sensor" }}
            ]
            endPoint Solid {{
              translation 0 0 {ALTURA_FEMUR:.4f}
              children [
                {(chr(10) + '                ').join(detalles_femur)}
                {tibia}
              ]
              name "femur{n}"
              boundingObject {pose(0.19, 0, 0, caja(0.43, 0.05, 0.05))}
              physics Physics {{ density -1 mass {MASA_FEMUR} centerOfMass [ 0.19 0 0 ] }}
            }}
          }}"""

    return f"""HingeJoint {{
      jointParameters HingeJointParameters {{
        axis 0 0 1
        anchor {x:.4f} {y:.4f} 0
      }}
      device [
        RotationalMotor {{
          name "coxa{n}"
          minPosition {LIM_COXA[0]}
          maxPosition {LIM_COXA[1]}
          maxVelocity {VEL_COXA}
          maxTorque {TORQUE_COXA}
        }}
        PositionSensor {{ name "coxa{n}_sensor" }}
      ]
      endPoint Solid {{
        translation {x:.4f} {y:.4f} 0
        rotation 0 0 1 {a:.6f}
        children [
          {pose(0, 0, ALTURA_CUERPO / 2, forma(caja(0.02, 0.02, ALTURA_CUERPO + 0.01), AZUL))}
          {pose(-0.025, 0, ALTURA_CUERPO + 0.012, forma(caja(0.07, 0.02, 0.02), AZUL))}
          {pose(0.012, 0, ALTURA_FEMUR, forma(caja(0.012, 0.06, 0.03), AZUL))}
          {femur}
        ]
        name "coxa{n}"
        boundingObject {pose(0, 0, ALTURA_CUERPO / 2, caja(0.02, 0.02, ALTURA_CUERPO))}
        physics Physics {{ density -1 mass {MASA_COXA} }}
      }}
    }}"""


def mundo():
    # Cámara al sudeste del robot, mirando hacia él desde arriba
    distancia, altura = 3.2, 1.9
    cam_x = distancia * math.cos(math.radians(-45))
    cam_y = distancia * math.sin(math.radians(-45))
    yaw = math.atan2(-cam_y, -cam_x)
    pitch = math.atan2(altura - 0.3, distancia)
    orientacion = eje_angulo_desde_yaw_pitch(yaw, pitch)

    patas = "\n    ".join(pata(n + 1, r) for n, r in enumerate(RADIALES))
    return f"""#VRML_SIM R2025a utf8
# Mundo generado por herramientas/generar_mundo.py. No editar a mano:
# los cambios se pierden al regenerarlo.

WorldInfo {{
  info [
    "HEXABOT - Universidad del Norte Santo Tomás de Aquino"
    "Robot hexápodo de 18 motores controlado por TCP/IP"
  ]
  title "HEXABOT"
  basicTimeStep 8
  contactProperties [
    ContactProperties {{
      material1 "pie"
      material2 "piso"
      coulombFriction [ 1.2 ]
      softCFM 0.0001
    }}
  ]
}}
Viewpoint {{
  orientation {orientacion}
  position {cam_x:.3f} {cam_y:.3f} {altura:.3f}
  follow "HEXABOT"
  followType "Pan and Tilt Shot"
}}
Background {{
  skyColor [ 0.78 0.84 0.92 ]
}}
DirectionalLight {{
  direction -0.4 0.6 -1
  intensity 2.5
  castShadows TRUE
}}
DirectionalLight {{
  direction 0.5 -0.3 -0.6
  intensity 0.8
}}
Solid {{
  translation 0 0 -0.01
  children [
    Shape {{
      appearance PBRAppearance {{
        baseColor 0.55 0.57 0.6
        roughness 1
        metalness 0
        baseColorMap ImageTexture {{
          url [ "../herramientas/textura_piso.png" ]
        }}
        textureTransform TextureTransform {{ scale 20 20 }}
      }}
      geometry Box {{ size 20 20 0.02 }}
    }}
  ]
  name "piso"
  contactMaterial "piso"
  boundingObject Box {{ size 20 20 0.02 }}
  locked TRUE
}}
Robot {{
  translation 0 0 {ALTURA_INICIAL + 0.005:.3f}
  children [
      {cuerpo()}
    GPS {{ name "gps" }}
    {patas}
  ]
  name "HEXABOT"
  boundingObject {pose(0, 0, ALTURA_CUERPO / 2, cilindro(0.15, ALTURA_CUERPO, 6))}
  physics Physics {{
    density -1
    mass {MASA_CUERPO}
    centerOfMass [ 0 0 {ALTURA_CUERPO / 2:.3f} ]
  }}
  controller "hexabot_tcp"
  controllerArgs [ "1001" "1002" "1003" ]
}}
"""


def textura_piso(ruta):
    """Baldosa gris de 64x64 con borde más oscuro (PNG sin dependencias)."""
    import struct
    import zlib
    lado = 64
    filas = []
    for j in range(lado):
        fila = bytearray([0])
        for i in range(lado):
            borde = i < 2 or j < 2
            v = 150 if borde else 205
            fila += bytes([v, v, v])
        filas.append(bytes(fila))
    datos = zlib.compress(b"".join(filas), 9)

    def bloque(tipo, contenido):
        return (struct.pack(">I", len(contenido)) + tipo + contenido +
                struct.pack(">I", zlib.crc32(tipo + contenido) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n" +
           bloque(b"IHDR", struct.pack(">IIBBBBB", lado, lado, 8, 2, 0, 0, 0)) +
           bloque(b"IDAT", datos) + bloque(b"IEND", b""))
    with open(ruta, "wb") as f:
        f.write(png)


if __name__ == "__main__":
    aqui = os.path.dirname(os.path.abspath(__file__))
    textura_piso(os.path.join(aqui, "textura_piso.png"))
    destino = os.path.join(aqui, "..", "worlds", "hexabot.wbt")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8") as f:
        f.write(mundo())
    print("Mundo generado en", os.path.normpath(destino))
