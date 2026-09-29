
/************************************************
   ControlArduino3.ino

   Universidad del Norte Santo Tomás de Aquino
   Facultad de Ingeniería
   Laboratorio de Robótica
   Proyecto final de grado HEXABOT
   Año académico 2016
   Autores: Ávarez Farhat E., Chaila A.
   Director: Sbrugnera G.

   Programa de control de Arduino MEGA
   Controla por PWM y PID, seis motores
   En las dos placas inferiores, además controla los LEDs de las patas
   y los sensores de pisada

   Versión 2017-06-05

 ************************************************/

// Numero de Arduino, 1 a 3. Este valor se debe establecer diferente para cada placa
#define  NRO_ARDUINO    3


// Momento del loop actual, en milisegundos
unsigned long g_momentoActual = 0;

// Momento en que se ejecutó última vez "establecePWMs"
unsigned long g_momentoEstablecePWMs = 0;

// Momento en que se recibió el primer caracter del último comando
unsigned long g_momentoInicioComando = 0;

// Estado del parser
int g_st = 0;

// Variable utilizada por el parser, para recordar el primer parámetro numérico
int g_param1;

// Variable utilizada por el parser, para recordar el segundo parámetro numérico
int g_param2;

/*
   Clase que define estática y dinámicamente un actuador, su control PID, sus salidas PWM,
   su final de carrera, la cuenta de pulsos del encoder
*/
class Actuador {
    // Pin de entrada del encoder
    int m_pinEncoder;

    // Pin de entrada del inicio de recorrido
    int m_pinInicio;

    // Pin de salida PWM positiva
    int m_pinPwmPos;

    // Pin de salida PWM negativa
    int m_pinPwmNeg;

    // Máximo valor de cuenta de pulsos, equivalente al actuador completamente extendido
    int m_maxPulsos;

    // Dirección actual del actuador. UNO=directa=expansión; CERO=inversa=contracción
    int m_dir = 1;

    // Valor del encoder, en el loop anterior
    int m_encAnterior = 0;

    // Pulsos contados; posición actual del motor
    long m_pulsos = 9999;

    // Setpoint
    long m_setpoint = 0;

    // Último valor de PWM enviado, entre -255 y 255
    int m_salida = 0;

    // Momento en que se recibió el comando de inicialización del actuador
    unsigned long m_momentoInicializacion = 0;

    // Factor proporcional de la expresión PID
    double m_kP = 0.0;

    // Factor integral de la expresión PID
    double m_kI = 0.0;

    // Factor derivativa de la expresión PID
    double m_kD = 0.0;

    double m_dt = 1.0;

    // Valor acumulado del término integral en el cálculo PID
    double m_integral = 0.0;

    // Valor de pulsos, en la entrada anterior del PID. Permite calcular la derivada
    long m_pulsosAnt = 9999;
    
    // Encendido / apagado del control PID
    int m_PIDactivo = 0;

    /*
       Constructor de la clase
    */
  public:
    Actuador() {

    }

    Actuador(int p_pinEncoder, int p_pinInicio,
             int p_pinPwmPos, int p_pinPwmNeg,
             int p_maxPulsos) {

      setPinEncoder(p_pinEncoder);
      setPinInicio(p_pinInicio);
      setPinPwmPos(p_pinPwmPos);
      setPinPwmNeg(p_pinPwmNeg);
      setMaxPulsos(p_maxPulsos);

    }

    void setSetpoint(int p_setpoint) {
      m_setpoint = p_setpoint;
    }

    void setPinEncoder(int p_pinEncoder) {
      m_pinEncoder = p_pinEncoder;
      pinMode(m_pinEncoder, INPUT);
    }

    void setPinInicio(int p_pinInicio) {
      m_pinInicio = p_pinInicio;
      pinMode(m_pinInicio, INPUT);
    }

    void setPinPwmPos(int p_pinPwmPos) {
      m_pinPwmPos = p_pinPwmPos;
      pinMode(m_pinPwmPos, OUTPUT);
    }

    void setPinPwmNeg(int p_pinPwmNeg) {
      m_pinPwmNeg = p_pinPwmNeg;
      pinMode(m_pinPwmNeg, OUTPUT);
    }

    void setMaxPulsos(int p_maxPulsos) {
      m_maxPulsos = p_maxPulsos;
    }

    /**
       Establece todos los parámetros del control PID
    */
    void setPIDParams(double p_kP, double p_kI, double p_kD, double p_dt) {
      setKP(p_kP);
      setKI(p_kI);
      setKD(p_kD);
      m_dt = p_dt;
    }

    /**
       Establece el factor proporcional del control PID
    */
    void setKP(double p_kP) {
      m_kP = p_kP;
    }
    /**
       Establece el factor integral del control PID
    */
    void setKI(double p_kI) {
      m_kI = p_kI;
    }
    /**
       Establece el factor derivativo del control PID
    */
    void setKD(double p_kD) {
      m_kD = p_kD;
    }

    void buscaInicioRecorrido() {
      if (m_momentoInicializacion == 0) {
        // No necesita hacer esto.
        return;
      } // end if

      unsigned long l_lap = g_momentoActual - m_momentoInicializacion;

      if ((digitalRead(m_pinInicio) == 1) || (l_lap > 4000)) {
        // Llegó, o se pasó el tiempo. Detiene y resetea
        digitalWrite(m_pinPwmPos, false);
        digitalWrite(m_pinPwmNeg, false);

        m_momentoInicializacion = 0;

        if (l_lap <= 4000) {
          // Llegó a tiempo. Resetea el contador
          m_pulsos = 0;
          m_pulsosAnt = 0;
        } // end if
      } // end if


    }

    /**
       Contrae el actuador hasta el final de carrera
    */
    void iniciaRecorrido() {
      m_momentoInicializacion = g_momentoActual;
      m_pulsos = 9999;
      m_pulsosAnt = 9999;
      m_PIDactivo = 0;
      m_setpoint = 0;

      // Contrae el actuador
      digitalWrite(m_pinPwmPos, false);
      digitalWrite(m_pinPwmNeg, true);

    }

    /*
       Actualiza el contador de pulsos del encoder
       Observa también la barrera de inicio de carrera
       Se ejecuta todas las veces que se invoca
       Pero si está en tránsito hacia el inicio de carrera, no ejecuta

    */
    void cuentaPulsos() {

      if (m_pulsos == 9999) {
        // No cuenta; espera que llegue al principio
        return;
      } // end if

      int l_enc = digitalRead(m_pinEncoder);
      int l_ini = digitalRead(m_pinInicio);

      if (l_ini == 1) {
        // Final de carrera. Resetea el contador de pulsos
        m_pulsos = 0;
        m_pulsosAnt = 0;
      } else {
        // Recorrido normal
        if (l_enc != m_encAnterior) {
          // Cambio en la entrada del encoder
          if (m_dir == 1) {
            // Sentido de giro: directo. Cuenta flancos ascendentes
            if (l_enc == 1) {
              // Transición de 0 a 1
              m_pulsos++;
            } // end if
          } else {
            // Sentido de giro: inverso. Cuenta flancos descendentes
            if (l_enc == 0) {
              // Transición de 1 a 0
              m_pulsos--;
            } // end if
          } // end if
        } // end if
      } // end if
      m_encAnterior = l_enc;

    }

    /*
       Calcula los nuevos valores de PWM para el motor
       Pero si está en tránsito hacia el inicio de carrera, no ejecuta

    */
    void establecePWM() {

      if (m_pulsos == 9999) {
        // No cuenta; espera que llegue al principio
        return;
      } // end if

      if (m_PIDactivo == 0) {
        // PID desactivado
        return;
      } // end if


      int l_error = m_setpoint - m_pulsos;
      int l_errorAbs = abs(l_error);


      // 2017-06-03: apaga el PWM cuando no hay errores
      if (l_errorAbs <= 1) {
        m_integral = 0;
        analogWrite(m_pinPwmPos, 0);
        analogWrite(m_pinPwmNeg, 0);
        return;
      } // end if
      
      m_integral += m_kI * l_error;
      if (m_integral > 255.0) {
        m_integral = 255.0;
      } else {
        if (m_integral < -255.0) {
          m_integral = -255.0;
        } // end if
      } // end if

      double l_derivativo = m_kD * (m_pulsos - m_pulsosAnt);
      
      m_salida = m_kP * l_error + m_integral + l_derivativo;

      m_pulsosAnt = m_pulsos;
      
      if (m_salida > 255) {
        m_salida = 255;
      } else {
        if (m_salida < -255) {
          m_salida = -255;
        } // end if
      } // end if

      if (m_salida > 0) {
        // Debe expandirse
        m_dir = 1;
        analogWrite(m_pinPwmPos, m_salida);
        analogWrite(m_pinPwmNeg, 0);
      } else {
        // Debe contraerse
        m_dir = 0;
        analogWrite(m_pinPwmPos, 0);
        if (m_pulsos > 0) {
          analogWrite(m_pinPwmNeg, -m_salida);
        } else {
          // Evita inmediatamente contraerse más
          analogWrite(m_pinPwmNeg, 0);
        } // end if
      } // end if

    }

    int getSalida() {
      return m_salida;
    }
    int getPulsos() {
      return m_pulsos;
    }

    int getSetpoint() {
      return m_setpoint;
    }

    int getMaxPulsos() {
      return m_maxPulsos;
    }

    void detiene() {
      m_PIDactivo = 0;
      analogWrite(m_pinPwmPos, 0);
      analogWrite(m_pinPwmNeg, 0);
    }

    /**
      Comienza el control PID del actuador. Debe invocarse sólo después de
      haber configurado todos los parámetros operativos, e inicializado
      el inicio de carrera.
    */
    void activa() {
      m_PIDactivo = 1;
    }

    int getSensorInicio() {
      return digitalRead(m_pinInicio);

    }
    int getSensorEncoder() {
      return digitalRead(m_pinEncoder);
    }


};


Actuador g_acts[6];

/*
   Proceso de inicialización del programa
*/
void setup() {
  // Inicializa el puerto serie USB para informar al monitor
  setCeros();
  Serial.begin(115200);

}

/*
   Garantiza que todas las salidas posibles, inicien apagadas
*/
void setCeros() {
  for (int l_pin = 2; l_pin <= 13; l_pin++) {
    pinMode(l_pin, OUTPUT);
    digitalWrite(l_pin, false);
  } // end for
  for (int l_pin = 22; l_pin <= 53; l_pin++) {
    pinMode(l_pin, OUTPUT);
    digitalWrite(l_pin, false);
  } // end for

}

void loop() {
  g_momentoActual = millis();
  analizaEntrada();
  cuentaPulsos();
  buscaInicioRecorridos();
  establecePWMs();
}


/*
   Procesa órdenes provenientes desde el puerto USB
   Si hay algún caracter en el puerto de comunicaciones, lo consume y analiza.
*/
void analizaEntrada() {
  if (!Serial.available()) {
    // Nada por procesar.

    if (g_momentoInicioComando > 0) {
      if (g_momentoActual > g_momentoInicioComando + 1000) {
        // Ha transcurrido más de un segundo. Timeout del parser
        g_momentoInicioComando = 0;
        g_st = 0;
      } // end if
    } // end if
    return;
  } // end if

  char l_in = (char)Serial.read();
  switch (g_st) {
    case 0: {  // Esperando un comando
        switch (l_in) {
          case '\n':
          case '\r':
          case ' ': {
              // Ignora.
              return;
            } break;
          case 'A': {
              g_st = 1;
              g_momentoInicioComando = g_momentoActual;
              return;
            } break;
          case 'C': {
              g_st = 2;
              g_momentoInicioComando = g_momentoActual;
              return;
            } break;
          case 'S': {
              g_st = 5;
              g_momentoInicioComando = g_momentoActual;
              return;
            } break;
          case 'Q': {
              g_st = 8;
              g_momentoInicioComando = g_momentoActual;
              return;
            } break;
          case 'I': {
              g_st = 9;
              g_momentoInicioComando = g_momentoActual;
              return;
            } break;
          case 'H': {
              g_st = 11;
              g_momentoInicioComando = g_momentoActual;
              return;
            } break;
          case 'P': { // Activación del PID
              g_st = 13;
              g_momentoInicioComando = g_momentoActual;
              return;
            } break;
          default: {
              // Política tolerante: acepta cualquier caracter inválido en el estado cero
              return;
            }
        } // end switch
      } break;
    case 1: {  // Esperando cierre de comando A
        switch (l_in) {
          case ' ': {
              // Ignora.
              return;
            } break;
          case '\n':
          case '\r': {
              g_st = 0;
              g_momentoInicioComando = 0;
              cmdA();
              return;
            } break;
        } // end switch
      } break;
    case 2: {  // Esperando primer digito de numero de parametro de comando C
        if (l_in == ' ') {
          // ignora.
          return;
        } // end if

        if (isDigit(l_in)) {
          g_param1 = l_in - '0';
          g_st = 3;
          return;
        } // end if

      } break;
    case 3: {  // Esperando siguientes digitos de numero de parametro de comando C
        if (l_in == ' ') {
          // Pasa al siguiente parámetro
          g_st = 4;
          return;
        } // end if

        if (isDigit(l_in)) {
          g_param1 = g_param1 * 10 + (l_in - '0');
          return;
        } // end if

      } break;
    case 4: {  // Esperando primer digito de valor (segundo parámetro) de comando C
        if (l_in == ' ') {
          // ignora.
          return;
        } // end if

        if (isDigit(l_in)) {
          g_param2 = l_in - '0';
          g_st = 12;
          return;
        } // end if

      } break;
    case 12: {  // Esperando siguientes digitos de valor, o cierre de comando C
        if (l_in == '\r' || l_in == '\n') {
          g_st = 0;
          g_momentoInicioComando = 0;
          cmdC(g_param1, g_param2);
          return;
        } // end if

        if (isDigit(l_in)) {
          g_param2 = g_param2 * 10 + (l_in - '0');
          return;
        } // end if

      } break;
    case 5: {  // Esperando numero de motor de comando S
        if (l_in == ' ') {
          // ignora.
          return;
        } // end if

        if (isNroMotor(l_in)) {
          g_param1 = l_in - '0';
          g_st = 6;
          return;
        } // end if

      } break;
    case 6: {  // Esperando primer digito de posicion de comando S
        if (l_in == ' ') {
          // ignora.
          return;
        } // end if

        if (isDigit(l_in)) {
          g_param2 = l_in - '0';
          g_st = 7;
          return;
        } // end if

      } break;
    case 7: {  // Esperando siguientes digitos de posicion, o cierre de comando S
        if (l_in == '\r' || l_in == '\n') {
          g_st = 0;
          g_momentoInicioComando = 0;
          cmdS(g_param1, g_param2);
          return;
        } // end if

        if (isDigit(l_in)) {
          g_param2 = g_param2 * 10 + (l_in - '0');
          return;
        } // end if

      } break;
    case 8: {  // Esperando cierre de comando Q
        switch (l_in) {
          case ' ': {
              // Ignora.
              return;
            } break;
          case '\n':
          case '\r': {
              g_st = 0;
              g_momentoInicioComando = 0;
              cmdQ();
              return;
            } break;
        } // end switch
      } break;
    case 9: {  // Esperando numero de motor de comando I
        if (l_in == ' ') {
          // ignora.
          return;
        } // end if

        if (isNroMotor(l_in)) {
          g_param1 = l_in - '0';
          g_st = 10;
          return;
        } // end if

      } break;
    case 10: {  // Esperando cierre de comando I
        if (l_in == '\r' || l_in == '\n') {
          g_st = 0;
          g_momentoInicioComando = 0;
          cmdI(g_param1);
          return;
        } // end if

      } break;
    case 11: {  // Esperando cierre de comando H
        switch (l_in) {
          case ' ': {
              // Ignora.
              return;
            } break;
          case '\n':
          case '\r': {
              g_st = 0;
              g_momentoInicioComando = 0;
              cmdH();
              return;
            } break;
        } // end switch
      } break;
    case 13: {  // Esperando cierre de comando P ()
        switch (l_in) {
          case ' ': {
              // Ignora.
              return;
            } break;
          case '\n':
          case '\r': {
              g_st = 0;
              g_momentoInicioComando = 0;
              cmdP();
              return;
            } break;
        } // end switch
      } break;
  } // end switch

  // Entrada errónea
  inputError(l_in);
}

boolean isDigit(char p_char) {
  if (p_char < '0') {
    return false;
  } // end if

  if (p_char > '9') {
    return false;
  } // end if

  return true;
}

boolean isNroMotor(char p_char) {
  if (p_char < '1') {
    return false;
  } // end if

  if (p_char > '6') {
    return false;
  } // end if

  return true;
}

/*
  Reporta un error en el canal de entrada y resetea el parser
*/
void inputError(int p_in) {
  Serial.print("ERR st=");
  Serial.print(g_st);
  Serial.print(" char=");
  Serial.println(p_in);
  g_st = 0;
}




/*
   Ejecuta el conteo de pulsos en todos los motores
*/
void cuentaPulsos() {
  for (int l_mot = 0; l_mot < 6; l_mot++) {
    g_acts[l_mot].cuentaPulsos();
  } // end for
}

/*
   Ejecuta el proceso de búsqueda de inicio de recorrido,
   en los motores que lo necesiten
*/
void buscaInicioRecorridos() {
  for (int l_mot = 0; l_mot < 6; l_mot++) {
    g_acts[l_mot].buscaInicioRecorrido();
  } // end for
}

/*
   Calcula los nuevos valores de PWM para ambos motores
   Se ejecuta cada 50mS
*/
void establecePWMs() {
  if (g_momentoActual < g_momentoEstablecePWMs + 50) {
    // Aun no es momento
    return;
  } // end if

  g_momentoEstablecePWMs = g_momentoActual;

  for (int l_mot = 0; l_mot < 6; l_mot++) {
    g_acts[l_mot].establecePWM();
  } // end for

}

void responde(String p_comando) {
  Serial.print("OK ");
  Serial.print(p_comando);
  Serial.println();
}

void responde(String p_comando, String p_argumentos) {
  Serial.print("OK ");
  Serial.print(p_comando);
  Serial.print(" ");
  Serial.print(p_argumentos);
  Serial.println();
}

void responde(String p_comando, int p_argumento) {
  Serial.print("OK ");
  Serial.print(p_comando);
  Serial.print(" ");
  Serial.print(p_argumento);
  Serial.println();
}

/*
   Comando de identificación de Arduino
*/
void cmdA() {
  responde("A", NRO_ARDUINO);
}

/*
   Comando de configuración de pines
*/
void cmdC(int p_param, int p_valor) {
  if (p_param < 10) {
    // MOT-X+
    g_acts[p_param - 1].setPinPwmPos(p_valor);
  } else {
    if (p_param < 20) {
      // MOT-X-
      g_acts[p_param - 11].setPinPwmNeg(p_valor);
    } else {
      if (p_param < 30) {
        // ENC-X
        g_acts[p_param - 21].setPinEncoder(p_valor);
      } else {
        if (p_param < 40) {
          // FIN-X
          g_acts[p_param - 31].setPinInicio(p_valor);
        } else {
          if (p_param < 50) {
            // LED-R-X
            // TODO
          } else {
            if (p_param < 60) {
              // LED-G-X
              // TODO
            } else {
              if (p_param < 70) {
                // LED-B-X
                // TODO
              } else {
                if (p_param < 80) {
                  // PIS-X
                  // TODO
                } else {
                  if (p_param < 90) {
                    // MAX-X
                    g_acts[p_param - 81].setMaxPulsos(p_valor);
                  } else {
                    if (p_param < 100) {
                      // KP-X
                      g_acts[p_param - 91].setKP(p_valor / 1000.0);
                    } else {
                      if (p_param < 110) {
                        // KI-X
                        g_acts[p_param - 101].setKI(p_valor / 1000.0);
                      } else {
                        if (p_param < 120) {
                          // KD-X
                          g_acts[p_param - 111].setKD(p_valor / 1000.0);
                        } else {
                          // Número de parámetro inválido
                        } // end if
                      } // end if
                    } // end if
                  } // end if
                } // end if
              } // end if
            } // end if
          } // end if
        } // end if
      } // end if
    } // end if
  } // end if
  responde("C", p_param);

}

/*
   Comando de inicialización de un motor
*/
void cmdI(int p_motor) {
  g_acts[p_motor - 1].iniciaRecorrido();
  responde("I", p_motor);
}

/*
   Comando de establecimiento del setpoint
*/
void cmdS(int p_motor, int p_setpoint) {

  g_acts[p_motor - 1].setSetpoint(p_setpoint);

  responde("S", p_motor);
}


/*
   Comando de consulta del estado actual de todos los encoders
*/
void cmdQ() {
  String l_respuesta = "";
  for (int l_mot = 0; l_mot < 6; l_mot++) {
    l_respuesta = l_respuesta + " " + g_acts[l_mot].getPulsos();
  } // end for

  for (int l_mot = 0; l_mot < 6; l_mot++) {
    l_respuesta = l_respuesta + " " + g_acts[l_mot].getSensorInicio();
  } // end for

  for (int l_mot = 0; l_mot < 6; l_mot++) {
    l_respuesta = l_respuesta + " " + g_acts[l_mot].getSensorEncoder();
  } // end for


  responde("Q", l_respuesta);
}

/*
   Comando de detencion inmediata de los motores
*/
void cmdH() {
  for (int l_mot = 0; l_mot < 6; l_mot++) {
    g_acts[l_mot].detiene();
  } // end for
  responde("H");
}

/*
   Comando de inicio de control PID de los motores
*/
void cmdP() {
  for (int l_mot = 0; l_mot < 6; l_mot++) {
    g_acts[l_mot].activa();
  } // end for
  responde("P");
}

