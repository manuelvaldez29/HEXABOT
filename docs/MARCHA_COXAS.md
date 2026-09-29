# Marcha con 6 motores (solo coxas)

## El problema

En el HEXABOT original cada pata tiene 3 actuadores: coxa (gira la pata en
horizontal), fémur y tibia (la levantan y la bajan). La marcha trípode del
panel (`Hexapodo.WalkerThread.autoStep`) levanta tres patas, las lleva adelante
y las apoya mientras las otras tres empujan.

La consigna es controlar **6 motores, como si hubiera un solo Arduino**: el
Arduino 1, que en el robot real manejaba las 6 coxas. Sin fémur ni tibia, las
patas no se pueden levantar: los seis pies están siempre apoyados. Si se
alternan dos trípodes como en la marcha original, cada trípode que avanza
arrastra al cuerpo tanto como el otro lo retiene (3 pies contra 3 pies) y el
robot solo se balancea en el lugar.

## La solución: recuperar de a dos, empujar con las seis

La marcha usa el rozamiento de los pies con el piso:

1. **Recuperación** (3 fases). Se mueven hacia adelante dos patas opuestas
   (1 y 4, después 2 y 5, después 3 y 6). Esos dos pies resbalan, porque las
   otras cuatro patas sostienen el cuerpo con más rozamiento (4 pies contra 2).
   El cuerpo casi no se mueve.
2. **Empuje** (1 fase). Las seis coxas giran juntas hacia atrás. Nada ancla al
   cuerpo contra los seis pies, así que es el cuerpo el que avanza.

Un ciclo son 4 fases. Antes de pasar a la siguiente fase se espera a que las
coxas lleguen a su setpoint consultando al Arduino con `Q`. Esto corrige uno de
los problemas del panel original, que esperaba un tiempo fijo sin verificar la
llegada.

```
fase        pata: 1    2    3    4    5    6
recup. 1          +A   .    .    -A   .    .      (+A = coxa girada hacia adelante)
recup. 2          .    +A   .    .    -A   .
recup. 3          .    .    +A   .    .    -A
empuje            -A   -A   -A   +A   +A   +A     (el cuerpo avanza)
```

Las patas 1, 2 y 3 están a la derecha y las 4, 5 y 6 a la izquierda. Por eso
"adelante" es un ángulo positivo en un lado y negativo en el otro.

## Dirección y velocidad (joystick)

Con el rumbo `b` del joystick (0º adelante, 90º derecha) y el lado `s` de cada
pata (+1 derecha, −1 izquierda), el ángulo de recuperación es:

```
angulo = A * limitar(s * cos(b) - sin(b), -1, 1)
A      = 25º * velocidad / 100
```

- `b = 0º`: avanza. `b = 180º`: retrocede.
- `b = ±90º`: todas las coxas en el mismo sentido y el robot gira en el lugar.
- Rumbos intermedios combinan avance y giro (un lado empuja más que el otro).

## Limitaciones (para decirlo en la presentación)

- Es una marcha de **arrastre**, no una marcha con fases de vuelo. Funciona
  porque el rozamiento es de Coulomb (no depende de la velocidad) y los pies
  que resbalan tienen menos carga total que los que anclan.
- Es lenta: con 25º de coxa, cada ciclo avanza como mucho lo que recorre el pie
  en su arco (unos 30 cm).
- En el robot real esto desgastaría los pies y exigiría a las coxas vencer el
  rozamiento de una pata cargada. En Webots el torque de la coxa es un valor
  estimado (`torqueCoxa` en `Hexabot.proto`), no sale de la hoja de datos.
