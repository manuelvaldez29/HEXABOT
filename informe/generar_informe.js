// Genera el informe del Trabajo Final de Seminario I (Gemelos Digitales):
//   node generar_informe.js  ->  HEXABOT_Gemelo_Digital.docx
// Requiere el paquete npm "docx" (npm install).

const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell,
  AlignmentType, HeadingLevel, WidthType, ShadingType, BorderStyle, PageBreak,
  Header, Footer, PageNumber, LevelFormat, PageOrientation, VerticalAlign,
  TableLayoutType,
} = require("docx");

const IMG = path.join(__dirname, "imagenes");
const FUENTE = "Times New Roman";
const MONO = "Consolas";
const AZUL = "1F3A68";
const GRIS = "595959";

// A4 con márgenes de 2,5 cm: 16 cm de ancho útil (9070 DXA, ~605 px)
const A4 = { width: 11906, height: 16838 };
const MARGEN = 1418;
const ANCHO = 9070;
const ANCHO_PX = 605;
const ANCHO_APAISADO_PX = 935;

// ---------------------------------------------------------------- utilidades

function texto(t, op = {}) {
  return new TextRun({ text: t, font: FUENTE, ...op });
}

// Párrafo con segmentos: "normal **negrita** `código`"
function p(contenido, op = {}) {
  const runs = [];
  const partes = contenido.split(/(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`)/);
  for (const parte of partes) {
    if (!parte) continue;
    if (parte.startsWith("**")) runs.push(texto(parte.slice(2, -2), { bold: true }));
    else if (parte.startsWith("*")) runs.push(texto(parte.slice(1, -1), { italics: true }));
    else if (parte.startsWith("`")) runs.push(new TextRun({ text: parte.slice(1, -1), font: MONO, size: 20 }));
    else runs.push(texto(parte));
  }
  return new Paragraph({
    children: runs,
    alignment: op.alineacion || AlignmentType.JUSTIFIED,
    spacing: { after: 120, line: 276 },
    ...(op.vineta ? { numbering: { reference: "vinetas", level: 0 } } : {}),
    ...(op.keepNext ? { keepNext: true } : {}),
  });
}

const vineta = (t) => p(t, { vineta: true });
const h1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [texto(t)] });
const h2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [texto(t)] });
const saltoPagina = () => new Paragraph({ children: [new PageBreak()] });

let nroFigura = 0;
function figura(archivo, anchoPx, leyenda, op = {}) {
  const ruta = path.join(IMG, archivo);
  const { ancho, alto } = tamanio(ruta);
  const altoPx = Math.round(anchoPx * alto / ancho);
  nroFigura++;
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      keepNext: true,
      spacing: { before: 120, after: 60 },
      children: [new ImageRun({
        type: archivo.endsWith(".png") ? "png" : "jpg",
        data: fs.readFileSync(ruta),
        transformation: { width: anchoPx, height: altoPx },
        altText: { title: leyenda, description: leyenda, name: archivo },
      })],
    }),
    leyendaFigura(leyenda, op.credito),
  ];
}

function leyendaFigura(leyenda, credito) {
  const runs = [texto(`Figura ${nroFigura}. `, { bold: true, size: 18, color: GRIS }),
    texto(leyenda, { italics: true, size: 18, color: GRIS })];
  if (credito) runs.push(texto(` ${credito}`, { size: 16, color: GRIS }));
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 }, children: runs });
}

// Lee el tamaño de un PNG o JPG sin dependencias
function tamanio(ruta) {
  const b = fs.readFileSync(ruta);
  if (b[0] === 0x89) return { ancho: b.readUInt32BE(16), alto: b.readUInt32BE(20) };
  let i = 2;
  while (i < b.length) {
    const marca = b[i + 1];
    const largo = b.readUInt16BE(i + 2);
    if (marca >= 0xc0 && marca <= 0xc3) return { alto: b.readUInt16BE(i + 5), ancho: b.readUInt16BE(i + 7) };
    i += 2 + largo;
  }
  throw new Error("No se pudo leer el tamaño de " + ruta);
}

// Grilla de fotos (varias por fila) dentro de una tabla sin bordes
function grillaFotos(fotos, porFila) {
  const anchoCelda = Math.floor(ANCHO / porFila);
  const anchoImg = Math.floor(ANCHO_PX / porFila) - 12;
  const sinBorde = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
  const bordes = { top: sinBorde, bottom: sinBorde, left: sinBorde, right: sinBorde };
  const filas = [];
  for (let i = 0; i < fotos.length; i += porFila) {
    const grupo = fotos.slice(i, i + porFila);
    filas.push(new TableRow({
      cantSplit: true,
      children: Array.from({ length: porFila }, (_, k) => {
        const f = grupo[k];
        if (!f) return new TableCell({ borders: bordes, width: { size: anchoCelda, type: WidthType.DXA }, children: [new Paragraph("")] });
        const ruta = path.join(IMG, f.archivo);
        const { ancho, alto } = tamanio(ruta);
        nroFigura++;
        return new TableCell({
          borders: bordes,
          width: { size: anchoCelda, type: WidthType.DXA },
          children: [
            new Paragraph({ alignment: AlignmentType.CENTER, children: [new ImageRun({
              type: "jpg", data: fs.readFileSync(ruta),
              transformation: { width: anchoImg, height: Math.round(anchoImg * alto / ancho) },
              altText: { title: f.leyenda, description: f.leyenda, name: f.archivo },
            })] }),
            leyendaFigura(f.leyenda),
          ],
        });
      }),
    }));
  }
  return new Table({
    width: { size: ANCHO, type: WidthType.DXA },
    columnWidths: Array(porFila).fill(anchoCelda),
    layout: TableLayoutType.FIXED,
    rows: filas,
  });
}

// Tabla con encabezado sombreado. anchos en DXA (deben sumar ANCHO)
function tabla(anchos, encabezado, filas, op = {}) {
  const borde = { style: BorderStyle.SINGLE, size: 4, color: "B4BCCC" };
  const bordes = { top: borde, bottom: borde, left: borde, right: borde };
  const celda = (contenido, ancho, esEncabezado, mono) => new TableCell({
    borders: bordes,
    width: { size: ancho, type: WidthType.DXA },
    verticalAlign: VerticalAlign.CENTER,
    margins: { top: 60, bottom: 60, left: 100, right: 100 },
    shading: esEncabezado ? { type: ShadingType.CLEAR, fill: "DCE3F0", color: "auto" } : undefined,
    children: contenido.split("\n").map((linea) => new Paragraph({
      spacing: { after: 0 },
      children: [mono
        ? new TextRun({ text: linea, font: MONO, size: 18 })
        : texto(linea, { bold: esEncabezado, size: 19 })],
    })),
  });
  return new Table({
    width: { size: ANCHO, type: WidthType.DXA },
    columnWidths: anchos,
    layout: TableLayoutType.FIXED,
    rows: [
      new TableRow({ tableHeader: true, cantSplit: true, children: encabezado.map((t, i) => celda(t, anchos[i], true, false)) }),
      ...filas.map((fila) => new TableRow({
        cantSplit: true,
        children: fila.map((t, i) => celda(t, anchos[i], false, (op.columnasMono || []).includes(i))),
      })),
    ],
  });
}

// Bloque de texto en monoespaciado, sombreado (diálogos y comandos)
function codigo(lineas) {
  return lineas.map((linea, i) => new Paragraph({
    shading: { type: ShadingType.CLEAR, fill: "F2F4F8", color: "auto" },
    spacing: { after: 0, line: 240 },
    keepLines: true,
    keepNext: i < lineas.length - 1,
    indent: { left: 200, right: 200 },
    children: [new TextRun({ text: linea === "" ? " " : linea, font: MONO, size: 17 })],
  }));
}

const espacio = () => new Paragraph({ spacing: { after: 80 }, children: [] });

// ------------------------------------------------------------------ carátula

function caratula() {
  const centrado = (t, op = {}) => new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: op.despues ?? 120, before: op.antes ?? 0 },
    children: [texto(t, { size: op.tam || 24, bold: op.negrita, color: op.color, allCaps: op.mayus })],
  });
  return [
    centrado("UNIVERSIDAD DEL NORTE SANTO TOMAS DE AQUINO", { tam: 56, antes: 600, despues: 400 }),
    centrado("Facultad de ingeniería", { tam: 40, despues: 120 }),
    centrado("Ingeniería Informática – SEMINARIO I", { tam: 32, despues: 800 }),
    centrado("Trabajo Final", { tam: 28, despues: 80 }),
    centrado("GEMELOS DIGITALES", { tam: 32, despues: 80 }),
    centrado("Proyecto: HEXABOT", { tam: 28, despues: 800 }),
    centrado("Equipo: Linus Torvalds", { tam: 28, despues: 80 }),
    centrado("Integrantes:", { tam: 28, despues: 80 }),
    centrado("Bloj, Iván Gustavo", { tam: 28, despues: 80 }),
    centrado("Domfrocht, Julián Davico", { tam: 28, despues: 80 }),
    centrado("Petrelli, Francesco Máximo", { tam: 28, despues: 80 }),
    centrado("Valdez, Manuel Ricardo", { tam: 28, despues: 600 }),
    centrado("Docente: Ing. Gustavo Sbrugnera", { tam: 28, despues: 80 }),
    centrado("Año académico: 2026", { tam: 28 }),
    saltoPagina(),
  ];
}

// ------------------------------------------------------------------ homenaje

function homenaje() {
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 200, after: 300 },
      children: [texto("En homenaje a Linus Torvalds", { size: 36, color: "000000" })],
    }),
    ...figura("linus_torvalds.jpg", 230, "Linus Torvalds en la LinuxCon Europe 2014.",
      { credito: "Foto: Krd; recorte: Von Sprat. Wikimedia Commons, licencia CC BY-SA 4.0." }),
    p("Linus Torvalds nació en Helsinki, Finlandia, en 1969. En 1991, siendo estudiante de la Universidad de Helsinki, empezó a escribir como proyecto personal un núcleo de sistema operativo para su computadora y lo publicó en internet invitando a otros a mejorarlo. Ese proyecto, **Linux**, se convirtió en uno de los ejemplos más grandes de colaboración en la historia del software: hoy funciona en la mayoría de los servidores de internet, en todas las supercomputadoras del ranking TOP500, en los teléfonos Android y en placas como la Raspberry Pi."),
    p("En 2005 creó **Git**, un sistema de control de versiones distribuido pensado para coordinar el trabajo de miles de desarrolladores sobre un mismo código. Git es hoy la herramienta estándar de la industria, y es la que usamos en este trabajo, a través de GitHub, para coordinar al equipo. Su aporte fue reconocido, entre otras distinciones, con el Millennium Technology Prize (2012) y el Computer Pioneer Award de la IEEE Computer Society (2014)."),
    p("Elegimos su nombre por tres motivos. Primero, porque demostró que un estudiante universitario puede empezar algo que transforma la industria. Segundo, porque su forma de trabajar —publicar el código, aceptar mejoras de otros y construir sobre lo que ya existe— es la misma que aplicamos al retomar el HEXABOT, un proyecto desarrollado en 2016 y 2017 por otros estudiantes de esta Facultad, para continuarlo en lugar de empezar de cero. Y tercero, por su pragmatismo: su conocida frase «Talk is cheap. Show me the code» resume el espíritu de un gemelo digital, donde las ideas se validan ejecutándolas."),
    saltoPagina(),
  ];
}

// ------------------------------------------------------ 1. Equipo a simular

function equipo() {
  return [
    h1("1. Equipo a simular"),
    h2("1.1. Descripción general"),
    p("HEXABOT fue el proyecto final de grado de Ingeniería en Informática de E. Álvarez Farhat y A. Chaila (2016–2017), dirigido por el Ing. G. Sbrugnera en el Laboratorio de Robótica de la Facultad. Es un robot hexápodo experimental, con tres grados de libertad por pata, que se comanda manualmente. A bordo lleva una Raspberry Pi con el programa Java de control manual, que se comunica por tres puertos serie USB con tres placas Arduino MEGA; cada placa recibe, mediante un protocolo de texto, los movimientos que debe ejecutar en los seis motores que controla."),
    p("**Estado del proyecto:** construido, disponible y no operativo. El robot real no llegó a caminar como se esperaba; la documentación oficial atribuye las causas principales a la falta de rigidez de la estructura, la holgura y la debilidad de los actuadores, y a la baja resolución de los encoders."),
    h2("1.2. Descripción estructural"),
    p("La máquina es un robot de seis patas de simetría radial, que puede caminar en cualquier rumbo con algoritmos idénticos para todas las patas. El cuerpo es un prisma hexagonal formado por dos placas de chapa de acero de 1,5 mm (300 mm de diámetro) unidas por seis caños roscados de 220 mm, más una tercera placa que sostiene los actuadores de las coxas. Según la documentación oficial (la tesis y la gacetilla de prensa del proyecto), la envergadura con las patas extendidas es de **2 m** (la consigna menciona 1,5 m) y el peso total es de **18 kg**."),
    p("Las patas se numeran de 1 a 6 en sentido horario y cada una tiene tres eslabones en cadena:"),
    vineta("**Coxa:** caño de aluminio de 20×20 mm, vertical, que gira en el plano horizontal sobre un vértice del cuerpo."),
    vineta("**Fémur:** caño de aluminio de 50×50 mm, con 380 mm entre ejes; pivotea verticalmente sobre la coxa y aloja los actuadores del fémur y de la tibia."),
    vineta("**Tibia:** caño de aluminio de 20×20 mm, con 475 mm desde la rodilla hasta el punto de apoyo."),
    h2("1.3. Actuadores"),
    p("HEXABOT es propulsado por **18 actuadores lineales eléctricos** diseñados para el proyecto, uno por articulación. Cada uno es un motor de corriente continua de 24 V que hace girar una varilla roscada (rosca Whitworth) sobre una tuerca de avance, de modo que el giro del motor se convierte en un desplazamiento lineal que abre o cierra la articulación:"),
    vineta("**Coxa:** reducción por engranajes 15:60 y varilla de acero de 5/32\" WW."),
    vineta("**Fémur y tibia:** acople directo a una varilla de bronce de 3/16\" WW, con tuerca de teflón; en el banco de ensayo superaron esfuerzos de 15 kgf."),
    p("Los motores se alimentan desde puentes H comerciales L298N (dos motores por módulo), protegidos por fusibles de 1,5 A. La energía (24 V para los motores y 12 V para la electrónica) proviene de dos baterías de 12 V y 7,5 Ah ubicadas fuera del robot, porque la estructura no podía cargarlas."),
    h2("1.4. Sensores"),
    vineta("**18 encoders rotativos incrementales**, uno por motor: barreras infrarrojas interrumpidas por un disco de 14 ventanas, que dan la posición del actuador en pulsos."),
    vineta("**18 sensores de inicio de carrera**: barreras infrarrojas que detectan cuando el actuador está completamente contraído, y permiten poner en cero el contador de pulsos."),
    vineta("Previstos pero **no implementados** en el robot real: seis sensores de pisada y un LED RGB de señalización por pata."),
    h2("1.5. Unidad de control"),
    p("HEXABOT incorpora a bordo una **Raspberry Pi**, que ejecuta el programa Java de control manual y se comunica con el exterior por Wi-Fi, y **tres Arduino MEGA** conectados a ella por USB. Cada Arduino maneja seis actuadores: genera las señales PWM, cuenta los pulsos de los encoders y cierra un lazo PID de posición cada 50 ms. El reparto es: Arduino 1, las seis coxas; Arduino 2, fémur y tibia de las patas 1 a 3; Arduino 3, fémur y tibia de las patas 4 a 6. Unos *shields* y placas de distribución diseñados para el proyecto llevan las señales hasta cada pata."),
    h2("1.6. Fotografías"),
    grillaFotos([
      { archivo: "robot_1_general.jpg", leyenda: "Vista general del robot real." },
      { archivo: "robot_2_lateral.jpg", leyenda: "Vista lateral: cuerpo, fémures y tibias." },
      { archivo: "robot_3_cuerpo.jpg", leyenda: "Cuerpo: motores de coxa, Arduinos y placas." },
    ], 3),
    grillaFotos([
      { archivo: "robot_4_placa_pata.jpg", leyenda: "Placa de distribución y fusibles de una pata." },
      { archivo: "robot_5_actuador.jpg", leyenda: "Actuador del fémur: motor y soportes impresos en PLA." },
    ], 3),
  ];
}

// ------------------------------------------- 2. Software externo (preexistente)

function softwareExterno() {
  return [
    h1("2. Descripción funcional del software externo"),
    p("El software externo es **PanelControl1**, la aplicación de escritorio en Java (Swing) que desarrollaron los autores del proyecto original y que corría en la Raspberry Pi del robot. Siguiendo la consigna, se usa **intacta**: es el mismo archivo `PanelControl1.jar` compilado en 2017 (versión 2017-03-14), ahora ejecutado en una PC en lugar de la Raspberry Pi."),
    h2("2.1. Responsabilidades"),
    vineta("**Interfaz de mando manual:** un joystick virtual fija el rumbo (ángulo) y la velocidad (distancia al centro); controles adicionales fijan la altura del cuerpo, el largo del paso (STEP) y la altura a la que se levanta cada pie (CLEARANCE)."),
    vineta("**Cinemática inversa:** para cada pata, calcula los ángulos de coxa, fémur y tibia que llevan el pie a un punto del espacio, y los convierte en el largo que debe tener cada actuador lineal y luego en pulsos de encoder."),
    vineta("**Marcha en trípode:** alterna dos grupos de tres patas (1-3-5 y 2-4-6). Mientras un grupo avanza por el aire siguiendo media elipse, el otro empuja apoyado en el piso; cada semipaso se discretiza en cinco puntos."),
    vineta("**Comunicación con los Arduinos:** el botón *Discover* recorre los puertos serie, identifica cuál Arduino está en cada uno y abre las tres conexiones; *Configure* les envía la configuración de pines, recorridos y constantes PID del archivo `etc/config.csv`."),
    vineta("**Maniobras:** *Sit* lleva todos los actuadores al inicio de carrera; *Up* enciende el control y para el robot en la posición de caminata; *STOP* lo detiene."),
    vineta("**Visualización:** el estado de cada pata (VALUES), sus vistas lateral y superior (GRAPHICS), una vista 3D del robot (3D) y el registro de todos los comandos intercambiados con cada Arduino (TRACE)."),
    h2("2.2. Requisitos para su funcionamiento"),
    vineta("**Java 8 de 32 bits.** El panel usa la librería de puerto serie RXTX 2.2pre2 (2009), que no funciona en Java 9 o posterior y cuya DLL nativa de 64 bits es inestable. Se usa Eclipse Temurin 8 de 32 bits en versión portable, sin instalarla en el sistema."),
    vineta("La librería `RXTXcomm-2.2pre2.jar` original y su DLL nativa `rxtxSerial.dll` (32 bits)."),
    vineta("**Tres puertos serie.** En la simulación son puertos virtuales creados con com0com (ver sección 4)."),
    vineta("El archivo `etc/config.csv` en la carpeta de trabajo, usado por el botón *Configure*."),
    h2("2.3. Capturas de la interfaz"),
    p("Las cuatro capturas siguientes se tomaron con el panel original controlando el gemelo digital mientras camina."),
    ...figura("panel_3_values.png", ANCHO_PX, "Pestaña VALUES: ángulos, centro de paso y setpoints de cada pata durante la marcha."),
    ...figura("panel_5_3d.png", ANCHO_PX, "Pestaña 3D: vista isométrica del robot calculada por el panel (coxas en rojo, fémures en verde, tibias en amarillo)."),
    ...figura("panel_4_graphics.png", ANCHO_PX, "Pestaña GRAPHICS: vista lateral y superior de cada pata."),
    ...figura("panel_2_trace_discover.png", ANCHO_PX, "Pestaña TRACE después del Discover: cada shield prueba los puertos serie, envía A y reconoce a su Arduino (OK A 1, 2 y 3)."),
    saltoPagina(),
  ];
}

// ----------------------------------------------------------------- 3. Protocolo

function protocolo() {
  const A = [1500, 2700, 4870];
  const T = [2300, 3570, 3200];
  return [
    h1("3. Protocolo"),
    h2("3.1. Medio de comunicación y roles"),
    p("En el robot real, la Raspberry Pi se conecta con cada Arduino por un **puerto serie USB** (115200 bps, 8 bits de datos, sin paridad, 1 bit de parada). En el gemelo digital, cada uno de esos tres canales se reemplaza por un puerto serie virtual que el *bridge* redirige a una **conexión TCP/IP** hacia el controlador de Webots. Los bytes que viajan son exactamente los mismos en los dos casos."),
    tabla([2600, 6470], ["Aspecto", "Definición"], [
      ["Maestro / esclavo", "El panel es el maestro: inicia cada intercambio enviando un comando y espera la respuesta. Cada Arduino (real o virtual) es esclavo: solo responde."],
      ["Cliente / servidor (TCP)", "El controlador de Webots es el servidor: escucha en los puertos 1001, 1002 y 1003. El bridge es el cliente: se conecta a ellos y se reconecta solo si la simulación se reinicia."],
      ["Canales", "Tres canales independientes, uno por Arduino, cada uno con sus seis motores numerados de 1 a 6."],
      ["Formato", "Texto ASCII, un comando por línea, terminado en CR LF o LF. Los parámetros son enteros separados por espacios."],
      ["Respuestas", "OK <comando> [datos] si el comando es válido; ERR ... si no lo es."],
      ["Tiempos", "El panel espera cada respuesta hasta 500 ms. El firmware descarta un comando incompleto después de 1 s."],
    ]),
    espacio(),
    h2("3.2. Sintaxis y semántica de los comandos"),
    tabla(A, ["Comando", "Sintaxis y respuesta", "Efecto"], [
      ["Identificación", "A\n→ OK A <n>", "Devuelve el número de Arduino (1, 2 o 3). El panel lo usa en el Discover para saber qué Arduino hay en cada puerto."],
      ["Configuración", "C <parámetro> <valor>\n→ OK C <parámetro>", "Configura pines, recorrido máximo y constantes PID (ver 3.3). En la simulación se acepta y se ignora, porque no hay pines físicos."],
      ["Inicio de carrera", "I <motor>\n→ OK I <motor>", "Contrae el actuador hasta el sensor de inicio de carrera, pone en cero su contador de pulsos y apaga su PID."],
      ["Setpoint", "S <motor> <pulsos>\n→ OK S <motor>", "Fija la posición objetivo del actuador, en pulsos de encoder desde el inicio de carrera. Solo produce movimiento si el PID está encendido."],
      ["Activar control", "P\n→ OK P", "Enciende el PID de los seis motores: cada uno empieza a moverse hacia su setpoint."],
      ["Detener", "H\n→ OK H", "Apaga el PID y el PWM de los seis motores. Los tornillos de avance son autobloqueantes, así que el robot queda en la posición en la que está."],
      ["Consulta", "Q\n→ OK Q p1…p6 i1…i6 e1…e6", "Devuelve la posición en pulsos de los seis motores, el estado de sus sensores de inicio de carrera y el de sus encoders."],
      ["Posición (solo simulación)", "G\n→ OK G <x> <y> <z>", "Extensión agregada al gemelo: posición del cuerpo según un GPS simulado, en mm. Sirve para medir distancias recorridas; el panel original no la usa."],
    ], { columnasMono: [1] }),
    espacio(),
    p("Si el comando no es válido, el firmware original responde `ERR st=<estado> char=<código>` (estado del analizador y código del carácter rechazado), y el gemelo digital responde `ERR comando invalido: <línea>`."),
    h2("3.3. Parámetros del comando C"),
    p("El número de parámetro codifica a la vez qué se configura y a qué motor (la unidad es el número de motor, de 1 a 6):"),
    tabla(T, ["Parámetros", "Qué configuran", "Valor"], [
      ["1 a 6 / 11 a 16", "Pines PWM positivo / negativo del motor", "Número de pin del Arduino"],
      ["21 a 26 / 31 a 36", "Pin del encoder / del inicio de carrera", "Número de pin del Arduino"],
      ["41 a 76", "LEDs RGB y sensores de pisada", "Reservados (no implementados)"],
      ["81 a 86", "Recorrido máximo del actuador", "Pulsos"],
      ["91 a 96 / 101 a 106 / 111 a 116", "Constantes Kp / Ki / Kd del PID", "Valor × 1000"],
    ]),
    espacio(),
    h2("3.4. Unidades y reparto de motores"),
    p("Las posiciones viajan en **pulsos de encoder**. El panel convierte ángulos en milímetros de actuador con la geometría de cada articulación, y milímetros en pulsos con la mecánica: 14 pulsos por vuelta y el paso de la rosca, más la reducción de engranajes en las coxas. Resultan unos 70,6 pulsos/mm en las coxas y 13,2 pulsos/mm en fémures y tibias. El gemelo digital hace el camino inverso con las mismas fórmulas."),
    tabla([1400, 1300, 6370], ["Arduino", "Puerto TCP", "Motores 1 a 6"], [
      ["1", "1001", "Coxas de las patas 1 a 6"],
      ["2", "1002", "Fémur 1, tibia 1, fémur 2, tibia 2, fémur 3, tibia 3"],
      ["3", "1003", "Fémur 4, tibia 4, fémur 5, tibia 5, fémur 6, tibia 6"],
    ]),
    espacio(),
    h2("3.5. Ejemplos de diálogo"),
    p("Las líneas siguientes son tráfico real registrado por el bridge (opción `--ver-trafico`) entre el panel original y el gemelo digital. El símbolo `>` indica lo que envía el panel y `<` la respuesta de Webots."),
    p("**Discover:** el panel identifica qué Arduino hay en cada puerto.", { keepNext: true }),
    ...codigo([
      "[Arduino 1]  > A          < OK A 1",
      "[Arduino 2]  > A          < OK A 2",
      "[Arduino 3]  > A          < OK A 3",
    ]),
    espacio(),
    p("**Up:** detiene los motores, enciende el control y envía la posición de cada articulación (aquí, las primeras patas).", { keepNext: true }),
    ...codigo([
      "[Arduino 1]  > H          < OK H",
      "[Arduino 2]  > H          < OK H",
      "[Arduino 3]  > H          < OK H",
      "[Arduino 1]  > P          < OK P",
      "[Arduino 2]  > P          < OK P",
      "[Arduino 3]  > P          < OK P",
      "[Arduino 1]  > S 1 2141   < OK S 1      (coxa de la pata 1)",
      "[Arduino 2]  > S 1 659    < OK S 1      (fémur de la pata 1)",
      "[Arduino 2]  > S 2 685    < OK S 2      (tibia de la pata 1)",
      "[Arduino 1]  > S 2 2284   < OK S 2      (coxa de la pata 2)",
    ]),
    espacio(),
    p("**Consulta y posición:** estado de los seis motores del Arduino 1 y posición del cuerpo en el piso simulado.", { keepNext: true }),
    ...codigo([
      "[Arduino 1]  > Q",
      "             < OK Q 2142 2285 2285 2448 2285 2437 0 0 0 0 0 0 0 0 0 0 0 0",
      "[Arduino 1]  > G          < OK G 39 999 410",
    ]),
    espacio(),
    p("Durante la marcha, el panel envía en cada punto del paso un comando `S` por cada una de las 18 articulaciones. En una caminata de prueba de unos 30 segundos se registraron más de 500 comandos `S`, todos respondidos con `OK`."),
  ];
}

// ------------------------------------------------------ 4. Interconexión lógica

function interconexion() {
  return [
    h1("4. Interconexión lógica"),
    h2("4.1. Equipo físico y software externo"),
    p("En el robot real todo va a bordo. La Raspberry Pi ejecuta el panel y se conecta por USB con los tres Arduino MEGA; cada Arduino maneja, a través de los puentes H, seis motores con sus encoders y sensores de inicio de carrera. El operador accede a la interfaz del panel por Wi-Fi."),
    ...figura("interconexion_original.png", ANCHO_APAISADO_PX, "Interconexión original del HEXABOT."),
    h2("4.2. Software externo y gemelo digital"),
    p("En el gemelo digital, la máquina se reemplaza por Webots y el panel pasa a una PC. Para que el panel se ejecute **sin modificaciones**, se simulan los tres puertos serie y se redirigen por TCP/IP hacia el controlador de Webots, que implementa la lógica de los tres Arduinos. Los componentes nuevos se muestran en naranja."),
    ...figura("interconexion_gemelo.png", ANCHO_APAISADO_PX, "Interconexión del software externo con el gemelo digital."),
  ];
}

function componentes() {
  return [
    h2("4.3. Componentes agregados para la virtualización"),
    vineta("**com0com** (driver de puertos serie virtuales): crea tres pares de puertos unidos como por un cable nulo, COM10↔COM11, COM12↔COM13 y COM14↔COM15. El panel usa los pares (COM10, COM12, COM14) y el bridge los impares."),
    vineta("**bridge.py** (Python + pyserial, desarrollo propio): abre COM11, COM13 y COM15 y copia los bytes en ambos sentidos hacia los puertos TCP 1001, 1002 y 1003. No interpreta el protocolo. Si Webots se reinicia, se reconecta solo cada 2 segundos, sin que el panel lo note. Con `--host` puede apuntar a un Webots en otra PC, y con `--ver-trafico` muestra cada comando y cada respuesta."),
    vineta("**hexabot_tcp.py** (controlador de Webots en Python, desarrollo propio): contiene tres *Arduinos virtuales*, cada uno con su servidor TCP y sus seis actuadores, y reproduce la semántica del firmware `ControlArduino4.ino`. Por ejemplo, un setpoint no mueve el motor hasta que se recibe `P`, y `I` apaga el control de ese motor."),
    vineta("**hexabot.wbt** (mundo de Webots): se genera con el script `generar_mundo.py` a partir de las medidas de la documentación y del código del panel, sin usar un modelador 3D."),
    h2("4.4. Del protocolo al modelo físico"),
    p("El robot real mueve cada articulación con un actuador lineal; el modelo de Webots usa un **motor rotativo** en cada articulación, con su sensor de posición. Para que el panel no note la diferencia, cada Arduino virtual convierte los pulsos recibidos en milímetros de actuador y luego en el ángulo de la articulación, **invirtiendo las mismas fórmulas geométricas que usa el panel** (método `Pata.estableceActuadores`). Las fórmulas son crecientes en el rango de trabajo, así que se invierten por bisección. Para responder a `Q` hace el camino contrario: lee el ángulo y lo convierte en pulsos."),
    tabla([3000, 6070], ["Elemento del modelo", "Valor"], [
      ["Medidas", "Coxas a 150 mm del centro; fémur 380 mm; tibia 475 mm; cuerpo a 410 mm del piso al pararse (Up)"],
      ["Masas", "18 kg en total, como el robot real según la gacetilla de prensa. Reparto estimado: cuerpo 6 kg; por pata, coxa 0,25 kg, fémur 1,5 kg (lleva los dos actuadores) y tibia 0,25 kg"],
      ["Rango de las articulaciones", "Coxa −40° a +42°; fémur −29° a +46°; tibia −38° a +60° (recorrido completo de cada actuador)"],
      ["Motores", "Control de posición de Webots; velocidad máxima 1,0 rad/s (coxa) y 1,5 rad/s (fémur y tibia)"],
      ["Sensores", "18 sensores de posición; un GPS para el comando G"],
      ["Física", "Paso de simulación de 8 ms; rozamiento pie-piso μ = 1,2"],
    ]),
    espacio(),
    h2("4.5. Puesta en marcha"),
    p("Una sola vez por PC: instalar com0com, crear los pares de puertos (`crear_puertos_com.bat`, como administrador), instalar pyserial y descargar Java 8 de 32 bits (`descargar_java8.bat`). Luego, en cada uso y en este orden:"),
    vineta("Abrir `webots/worlds/hexabot.wbt` en Webots y darle Play."),
    vineta("Ejecutar `iniciar_bridge.bat` y esperar los tres mensajes «conectado con Webots»."),
    vineta("Abrir el panel con `ejecutar_panel_original.bat` y presionar Send Cmd, Discover, Up y luego mover el joystick."),
  ];
}

// ------------------------------------------------------- 5. Pila tecnológica

function pila() {
  return [
    h1("5. Pila tecnológica"),
    tabla([2100, 3200, 3770], ["Categoría", "Tecnología y versión", "Uso"], [
      ["Sistema operativo", "Windows 11 Home (compilación 26200)", "Desarrollo y ejecución de todo el sistema"],
      ["Simulador", "Webots R2025a (Cyberbotics, Apache 2.0)", "Modelo físico del robot y ejecución del controlador"],
      ["Lenguajes", "Python 3.12", "Controlador de Webots, bridge, generador del mundo y cliente de prueba"],
      ["", "Java: Eclipse Temurin 8 (32 bits) y JDK 25", "Ejecutar el panel original (Java 8) y compilar herramientas de prueba (JDK 25)"],
      ["Comunicación serie", "com0com 2.2.2.0 (GPL)", "Los tres pares de puertos serie virtuales"],
      ["", "pyserial 3.5 (BSD)", "Acceso a los puertos serie desde el bridge"],
      ["", "RXTX 2.2pre2 (LGPL)", "Librería serie del panel original (sin cambios)"],
      ["IDE", "Visual Studio Code", "Edición del código Python y Java, de los scripts y de la documentación"],
      ["Modeladores 3D", "Ninguno", "El robot se modeló por código (generar_mundo.py) con las primitivas de Webots a partir de las medidas de la documentación; el árbol de escena de Webots se usó para inspeccionarlo"],
      ["Inteligencia artificial", "Claude (Anthropic), mediante Claude Code; suscripción Claude Pro", "Análisis del proyecto original, desarrollo y prueba del controlador, del bridge y del modelo, diagnóstico de problemas y redacción de la documentación"],
      ["Control de versiones", "Git y GitHub (rama main2.0)", "Trabajo en equipo e historial del proyecto"],
      ["Otros", "PowerShell y archivos .bat; Microsoft Word", "Scripts de instalación y ejecución; redacción del informe"],
    ]),
    saltoPagina(),
  ];
}

// --------------------------------------------------- 6. Observaciones finales

function observaciones() {
  return [
    h1("6. Observaciones finales"),
    h2("6.1. Resultados"),
    vineta("El panel de control original de 2017 controla el gemelo digital **sin ninguna modificación**, a través de tres puertos serie simulados redirigidos por TCP/IP, como pide la consigna."),
    vineta("El *Discover* original identifica a los tres Arduinos virtuales en unos 3 segundos, por el mismo mecanismo que usaba con el robot real."),
    vineta("Con la marcha en trípode original, el robot simulado camina de forma estable: a velocidad máxima avanza **alrededor de 1 m cada 30 segundos** (1,04, 0,96 y 0,97 m en tres pruebas consecutivas), con el cuerpo a 410 mm del piso, y responde a los cambios de rumbo."),
    vineta("El controlador reproduce la semántica del firmware original, de modo que el panel no distingue entre el robot simulado y el real."),
    ...figura("webots_vista.png", ANCHO_PX, "El gemelo digital caminando en Webots, comandado por el panel original."),
    h2("6.2. Pendiente de implementación"),
    vineta("**Fuerza realista de los motores.** Los motores simulados son bastante más fuertes que los reales. Según nuestros cálculos, el actuador real del fémur entrega unos 14 N·m, mientras que para sostener los 18 kg del robot quieto sobre tres patas cada fémur necesita unos 26 N·m, y más al caminar; en la simulación se usaron 120 N·m para poder validar el control. El peso total del modelo coincide con el real, pero el reparto por pieza es estimado."),
    vineta("**Comandos de configuración (C).** Se aceptan pero no tienen efecto, porque no hay pines físicos; tampoco se usan las constantes PID del archivo de configuración."),
    vineta("**Sensores de pisada y LEDs.** No se simulan, igual que en el robot real, donde no llegaron a implementarse."),
    vineta("**Imperfecciones del robot real.** No se simulan la holgura de las articulaciones, la flexión de las patas ni la pérdida de pulsos de los encoders."),
    vineta("**Uso en red.** El bridge y el controlador permiten que el panel y Webots estén en PCs distintas, pero solo se probó con todo en la misma PC."),
    h2("6.3. Mejoras para una próxima iteración"),
    vineta("Un escenario con los motores y masas reales, para reproducir en la simulación por qué el HEXABOT no pudo caminar y probar soluciones (otros actuadores, otra geometría) antes de fabricarlas."),
    vineta("Modelar los actuadores lineales con su mecanismo (motores lineales de Webots) en lugar de motores rotativos en las articulaciones."),
    vineta("Que la marcha espere la confirmación de posición (comando Q) antes de pasar al punto siguiente, en lugar de esperar un tiempo fijo; esto requeriría modificar el panel."),
    vineta("Completar las funciones que quedaron sin terminar en el panel original: el modo de paso ortogonal (*Ortho*), los giros (*Left* y *Right*) y la lectura de los valores reales de los actuadores."),
    vineta("Unificar la instalación en un solo script que cree los puertos virtuales, instale las dependencias y descargue Java 8."),
    p("Además del modo con bridge, se evaluó una variante en la que el panel se conecta a Webots por TCP directamente, modificando solo su clase de comunicación (`ClientServerPort`). Funciona y no requiere programas intermedios, pero modifica el panel; por eso, para cumplir la consigna, la solución entregada es la del bridge. Ambas usan el mismo controlador de Webots."),
  ];
}

// ------------------------------------------------------------------- documento

const estilos = {
  default: { document: { run: { font: FUENTE, size: 24 } } },
  paragraphStyles: [
    { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FUENTE, size: 40, color: "000000" },
      paragraph: { spacing: { before: 240, after: 120 }, keepNext: true, outlineLevel: 0 } },
    { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FUENTE, size: 32, color: "000000" },
      paragraph: { spacing: { before: 240, after: 100 }, keepNext: true, outlineLevel: 1 } },
  ],
};

const numeracion = {
  config: [{
    reference: "vinetas",
    levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
      style: { paragraph: { indent: { left: 540, hanging: 270 } } } }],
  }],
};

function encabezado(tamanioLogo = 48) {
  return new Header({ children: [new Paragraph({
    alignment: AlignmentType.RIGHT,
    children: [new ImageRun({
      type: "png", data: fs.readFileSync(path.join(IMG, "logo_unsta.png")),
      transformation: { width: tamanioLogo, height: tamanioLogo },
      altText: { title: "UNSTA", description: "Escudo de la UNSTA", name: "logo_unsta.png" },
    })],
  })] });
}

function pie() {
  return new Footer({ children: [new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [texto("Página ", { size: 16, color: GRIS }),
      new TextRun({ children: [PageNumber.CURRENT], font: FUENTE, size: 16, color: GRIS })],
  })] });
}

const vertical = (extra = {}) => ({
  properties: {
    page: { size: A4, margin: { top: MARGEN, bottom: MARGEN, left: MARGEN, right: MARGEN } },
    ...extra,
  },
  headers: { default: encabezado() },
  footers: { default: pie() },
});

const apaisada = {
  properties: {
    page: { size: { ...A4, orientation: PageOrientation.LANDSCAPE },
      margin: { top: MARGEN, bottom: MARGEN, left: MARGEN, right: MARGEN } },
  },
  headers: { default: encabezado() },
  footers: { default: pie() },
};

const doc = new Document({
  creator: "Equipo Linus Torvalds",
  title: "HEXABOT — Gemelos Digitales",
  description: "Trabajo final de Seminario I 2026, UNSTA",
  styles: estilos,
  numbering: numeracion,
  sections: [
    { ...vertical({ titlePage: true }),
      headers: { default: encabezado(), first: encabezado(72) },
      footers: { default: pie(), first: new Footer({ children: [new Paragraph("")] }) },
      children: [...caratula(), ...homenaje(), ...equipo(), ...softwareExterno(), ...protocolo()] },
    { ...apaisada, children: interconexion() },
    { ...vertical(), children: [...componentes(), ...pila(), ...observaciones()] },
  ],
});

const salida = path.join(__dirname, process.argv[2] || "HEXABOT_Gemelo_Digital.docx");
Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(salida, buf);
  console.log("Informe generado:", salida);
});
