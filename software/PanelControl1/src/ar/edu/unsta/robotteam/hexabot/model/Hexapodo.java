package ar.edu.unsta.robotteam.hexabot.model;

import ar.edu.unsta.robotteam.hexabot.util.ClientServerPort;
import ar.edu.unsta.robotteam.hexabot.util.HexaUtils;
import ar.edu.unsta.robotteam.hexabot.view.Tracer;
import ar.edu.unsta.robotteam.scene3d.Point3D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Modelo de un hexápodo. El centro del sistema de coordenadas (0, 0, 0) se
 * encuentra en el centro del hexágono inferior del cuerpo.
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class Hexapodo {

    public static final String m_PROP_PSTEP = "pStep";

    /**
     * Máximo giro de coxa en la marcha de coxas, en grados, a velocidad 100
     */
    public static final double AMPLITUD_MAX_COXAS = 25.0;

    /**
     * Diferencia aceptada entre setpoint y posición para considerar que un
     * actuador llegó, en pulsos del encoder
     */
    public static final int TOLERANCIA_LLEGADA_PULSOS = 15;

    /**
     * Tiempo máximo de espera de llegada de los actuadores, en ms
     */
    public static final int TIMEOUT_LLEGADA_MS = 10000;

    /**
     *
     */
    private boolean m_autoStep = true;

    /**
     * Rumbo actual, en grados sexagesimales, 0º al Norte, 90º hacia el Este,
     * -90º hacia el Oeste
     */
    private int m_bearing = 0;

    /**
     * Deriva en X de medio paso, según el rumbo actual
     */
    private double m_bearingX;

    /**
     * Deriva en Y de medio paso, según el rumbo actual
     */
    private double m_bearingY;
    /**
     *
     */
    private double m_clearance = 100.0;

    /**
     * Longitud de medio paso, en mm
     */
    private double m_halfStep = 200.0;

    /**
     * Altura actual, desde el plano de piso hasta el origen. En mm
     */
    private int m_height = 410;
    /**
     *
     */
    private double m_pStep;

    /**
     * Array de seis patas
     */
    private Pata[] m_patas;
    /**
     * "true" cuando los comandos son enviados a los shields
     */
    private boolean m_sendCommands;
    /**
     * Array de tres shields (Arduinos)
     */
    private Shield[] m_shields;

    /**
     * Velocidad actual, de 25 a 100, o cero. Si es cero, está detenido
     */
    private int m_speed = 0;
    /**
     *
     */
    private STEP_MODE m_stepMode = STEP_MODE.ORTHOGONAL;

    /**
     * Algoritmo de caminata. TRIPODE es el original (18 motores); COXAS es la
     * marcha del simulador Webots, que solo mueve las 6 coxas
     */
    private MARCHA m_marcha = MARCHA.TRIPODE;

    /**
     * Dirección del Arduino virtual del simulador, por ejemplo
     * "tcp://127.0.0.1:5000". Si es null se usan los puertos serie
     */
    private String m_direccionTcp;
    /**
     *
     */
    private Tracer m_tracer;
    /**
     * Thread responsable de la implementación del algoritmo de caminar
     */
    public WalkerThread m_wThread;
    /**
     *
     */
    private transient final PropertyChangeSupport propertyChangeSupport
            = new PropertyChangeSupport(this);

    /**
     *
     */
    public Hexapodo(Tracer p_globalTracer, Tracer p_tracer1, Tracer p_tracer2,
            Tracer p_tracer3) {
        m_patas = new Pata[6];

        for (int l_nroPata = 0; l_nroPata < 6; l_nroPata++) {
            Pata l_pata = new Pata(l_nroPata + 1);
            m_patas[l_nroPata] = l_pata;
        } // end for

        // 14 pulsos / vuelta, 32 vueltas  / pulgada (rosca WW 5/32"), 25.4mm / pulgada, reducción 16:60
        //m_actCoxa.setMm2pulse(14.0 * 32.0 * (60.0 / 16.0) / 25.4);
        // 60 / 16 = 3.75   err = 0%
        // 60 / 15 = 4      err = 6.7%
        // 60 / 14 = 4.29   err = 14.4%
        // 64 / 15 = 4.27   err = 13.9%
        m_patas[0].getActCoxa().setMm2pulse(14.0 * 32.0 * (60.0 / 16.0) / 25.4);
        m_patas[1].getActCoxa().setMm2pulse(14.0 * 32.0 * (60.0 / 15.0) / 25.4);
        m_patas[2].getActCoxa().setMm2pulse(14.0 * 32.0 * (60.0 / 15.0) / 25.4);
        m_patas[3].getActCoxa().setMm2pulse(14.0 * 32.0 * (60.0 / 14.0) / 25.4);
        m_patas[4].getActCoxa().setMm2pulse(14.0 * 32.0 * (60.0 / 15.0) / 25.4);
        m_patas[5].getActCoxa().setMm2pulse(14.0 * 32.0 * (64.0 / 15.0) / 25.4);

        setTracer(p_globalTracer);

        m_shields = new Shield[3];
        m_shields[0] = new Shield(1);
        m_shields[1] = new Shield(2);
        m_shields[2] = new Shield(3);

        m_shields[0].setTracer(p_tracer1);
        m_shields[1].setTracer(p_tracer2);
        m_shields[2].setTracer(p_tracer3);

        getPata(1).setCanales(m_shields[0].getCanalShield(1), m_shields[1].
                getCanalShield(1), m_shields[1].getCanalShield(2));
        getPata(2).setCanales(m_shields[0].getCanalShield(2), m_shields[1].
                getCanalShield(3), m_shields[1].getCanalShield(4));
        getPata(3).setCanales(m_shields[0].getCanalShield(3), m_shields[1].
                getCanalShield(5), m_shields[1].getCanalShield(6));
        getPata(4).setCanales(m_shields[0].getCanalShield(4), m_shields[2].
                getCanalShield(1), m_shields[2].getCanalShield(2));
        getPata(5).setCanales(m_shields[0].getCanalShield(5), m_shields[2].
                getCanalShield(3), m_shields[2].getCanalShield(4));
        getPata(6).setCanales(m_shields[0].getCanalShield(6), m_shields[2].
                getCanalShield(5), m_shields[2].getCanalShield(6));

        // Dimensiones de las piezas
        double l_radio = 150.0; // mm desde el origen hasta la coxa
        double l_angulo = 60.0; // Grados sexagesimales

        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            Pata l_pata = getPata(l_nroPata);

            Point3D l_cxA = l_pata.getCoxaA();
            Point3D l_cxB = l_pata.getCoxaB();

            l_cxA.setX(l_radio * Math.cos(Math.toRadians(l_angulo)));
            l_cxA.setY(l_radio * Math.sin(Math.toRadians(l_angulo)));
            l_cxA.setZ(0.0); // plano de origen
            l_cxB.setX(l_cxA.getX());
            l_cxB.setY(l_cxA.getY());
            l_cxB.setZ(195.0);
            l_pata.setAngRadial(l_angulo);

            l_pata.recalculaVertices();

            l_angulo -= 60.0;

        } // end for

        m_wThread = new WalkerThread();
        m_wThread.start();

    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Ordena a todos los actuadores de todas las patas, encender el PID
     *
     * @param p_doneListener
     */
    public void cmdPidOn(ActionListener p_doneListener,
            ActionListener p_errListener) {

    }

    /**
     * Ordena a todos los actuadores de todas las patas ir a la posición de
     * inicio
     *
     * @param p_doneListener
     */
    public void cmdSit(ActionListener p_doneListener) throws Exception {

        ActionListener l_doneListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent p_e) {
                // TODO
            }
        };

        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            Pata l_pata = getPata(l_nroPata);
            l_pata.setAngCoxa(-40.0); // estimado
            l_pata.setAngFemur(-30.0); // estimado
            l_pata.setAngTibia(-45.0); // estimado
            l_pata.setCentroPaso(null);
        } //end for
        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            getPata(l_nroPata).getActFemur().cmdInit();
        } // end for

        Thread.sleep(2000);

        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            getPata(l_nroPata).getActTibia().cmdInit();
        } // end for

        Thread.sleep(2000);
        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            getPata(l_nroPata).getActCoxa().cmdInit();
        } // end for

        doSend(1, "Q");
        doSend(2, "Q");
        doSend(3, "Q");
        
        // TODO notificar
    }

    /**
     * Ordena a todos los actuadores de todas las patas, detenerse y apagar el
     * control PID
     *
     * @param p_doneListener
     */
    public void cmdStop(ActionListener p_doneListenerp_doneListener,
            ActionListener p_errListener) {

        try {
            setSpeed(0);
        } catch (Exception l_ex) {
            m_tracer.trace(l_ex);
        }
    }

    /**
     *
     * @param p_doneListener
     */
    public void cmdTurnLeft(ActionListener p_doneListener) {
        Point3D l_endpoint2 = new Point3D("", 400, 200, -m_height);

        Pata l_pata = getPata(2);
        l_pata.recalculaAngulos(l_endpoint2);

        l_pata = getPata(1);
        l_pata.recalculaAngulos(l_endpoint2);

        l_pata = getPata(3);
        //l_pata.recalculaAngulos(l_endpoint2);
    }

    /**
     *
     * @param p_doneListener
     */
    public void cmdTurnRight(ActionListener p_doneListener) {

    }

    public void cmdUp(ActionListener p_doneListener) throws Exception {

        double l_largoTibia = getPata(1).getLargoTibia();
        double l_largoFemur = getPata(1).getLargoFemur();
        double l_h2 = getHeight() - l_largoTibia + getPata(1).getLargoCoxa();
        // Detiene
        setSpeed(0);
        Thread.sleep(1000);

        // Activa los PID
        for (Shield l_shield : m_shields) {
            if (l_shield.getPort() != null) {
                l_shield.getPort().sendCommandWaitResponse("P", null);
            } // end if
        } // end for

        double l_angFemur = Math.toDegrees(Math.asin(l_h2 / l_largoFemur));

        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            Pata l_pata = getPata(l_nroPata);
            l_pata.setAngCoxa(0.0);
            l_pata.setAngFemur(l_angFemur);
            l_pata.setAngTibia(l_angFemur); // Está bien así

            l_pata.recalculaVertices();
            Point3D l_nuevoCentro = new Point3D("CENTRO" + l_nroPata,
                    l_pata.getTibiaB().getX(), l_pata.getTibiaB().getY(),
                    l_pata.getTibiaB().getZ());
            l_pata.setCentroPaso(l_nuevoCentro);

            l_pata.estableceActuadores();

        } // end for

    }

    /**
     * Cierra los puertos de todos los shields
     */
    public void doClose() {
        for (Shield l_shield : m_shields) {
            l_shield.close();
        } // end for

    }

    /**
     * Lee la configuración y parametriza los shields
     */
    public void doConfigure() throws Exception {
        final Map<Integer, Map<Integer, Integer>> l_config = HexaUtils.
                readConfig("etc/config.csv");

        for (int l_shieldNo : l_config.keySet()) {
            Map<Integer, Integer> l_shieldConfig = l_config.get(l_shieldNo);

            if (m_direccionTcp != null
                    && m_shields[l_shieldNo - 1].getPort() == null) {
                // En el simulador solo está conectado el Arduino 1
                continue;
            } // end if
            m_shields[l_shieldNo - 1].doConfigure(l_shieldConfig);

        } // end for

    }

    /**
     * Ordena a cada shield que identifique y abra el puerto serie donde está
     * conectado
     *
     * @throws Exception
     */
    public void doDiscover() throws Exception {
        doClose();
        if (m_direccionTcp != null) {
            // Simulador: un único Arduino (el 1, que controla las coxas)
            m_shields[0].doDiscover(Arrays.asList(m_direccionTcp));
            if (m_shields[0].getPort() == null) {
                throw new Exception("No se pudo conectar con el simulador en "
                        + m_direccionTcp);
            } // end if
            return;
        } // end if
        List<String> l_serialPorts = HexaUtils.getSerialPorts();
        for (Shield l_shield : m_shields) {
            l_shield.doDiscover(l_serialPorts);
        } // end for
    }

    /**
     * Get the value of m_bearing
     *
     * @return the value of m_bearing
     */
    public int getBearing() {
        return m_bearing;
    }

    /**
     * Set the value of m_bearing
     *
     * @param p_bearing new value of m_bearing
     */
    public void setBearing(int p_bearing) {

        m_bearing = p_bearing;

        double l_angle = Math.toRadians(-m_bearing + 90.0);

        m_bearingX = Math.cos(l_angle) * m_halfStep;
        m_bearingY = Math.sin(l_angle) * m_halfStep;

        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            Pata l_pata = getPata(l_nroPata);
            l_pata.setRumboX(m_bearingX);
            l_pata.setRumboY(m_bearingY);
        } // end for
    }

    public double getBearingX() {
        return m_bearingX;
    }

    public double getBearingY() {
        return m_bearingY;
    }

    /**
     * Get the value of clearance
     *
     * @return the value of clearance
     */
    public double getClearance() {
        return m_clearance;
    }

    /**
     * Set the value of clearance
     *
     * @param p_clearance new value of clearance
     */
    public void setClearance(double p_clearance) {
        this.m_clearance = p_clearance;
        for (Pata l_pata : m_patas) {
            l_pata.setClearance(p_clearance);
        } // end for
    }

    /**
     * Get the value of halfStep
     *
     * @return the value of halfStep
     */
    public double getHalfStep() {
        return m_halfStep;
    }

    /**
     * Set the value of halfStep
     *
     * @param p_halfStep new value of halfStep
     */
    public void setHalfStep(double p_halfStep) {
        m_halfStep = p_halfStep;

        for (Pata l_pata : m_patas) {
            l_pata.setSemiPaso(p_halfStep);
        } // end for

    }

    /**
     * Get the value of m_height
     *
     * @return the value of m_height
     */
    public int getHeight() {
        return m_height;
    }

    /**
     * Set the value of m_height
     *
     * @param p_height new value of m_height
     */
    public void setHeight(int p_height) {
        m_height = p_height;

        // Resetea el centro de paso, hasta el próximo cmdUp()
        for (Pata l_pata : m_patas) {
            l_pata.setCentroPaso(null);
        } //end for

    }

    public void setLegsToBackward(String p_legsToBackward) {
        m_wThread.setLegsToBackward(p_legsToBackward);
    }

    public void setLegsToForward(String p_legsToForward) {
        m_wThread.setLegsToForward(p_legsToForward);
    }

    /**
     *
     * @param p_nroPata
     * @return
     */
    public Pata getPata(int p_nroPata) {
        if (p_nroPata < 1) {
            return null;
        } // end if
        if (p_nroPata > 6) {
            return null;
        } // end if
        return m_patas[p_nroPata - 1];
    }

    /**
     * Get the value of m_speed
     *
     * @return the value of m_speed
     */
    public int getSpeed() {
        return m_speed;
    }

    /**
     * Set the value of m_speed
     *
     * @param p_speed new value of m_speed
     */
    public void setSpeed(int p_speed) throws Exception {

        if (p_speed > 0 && m_speed == 0) {
            // Enciende PID
            for (Shield l_shield : m_shields) {
                ClientServerPort l_port = l_shield.getPort();
                if (l_port != null) {
                    if (m_sendCommands) {
                        l_port.sendCommandWaitResponse("P", null);
                    } // end if
                } // end if
            } // end for
        } else if (p_speed == 0) {
            // Apaga PID
            for (Shield l_shield : m_shields) {
                ClientServerPort l_port = l_shield.getPort();
                if (l_port != null) {
                    if (m_sendCommands) {
                        l_port.sendCommandWaitResponse("H", null);
                    } // end if
                } // end if
            } // end for
        } // end if // end if

        m_speed = p_speed;

    }

    /**
     * Get the value of stepMode
     *
     * @return the value of stepMode
     */
    public STEP_MODE getStepMode() {
        return m_stepMode;
    }

    /**
     * Set the value of stepMode
     *
     * @param p_stepMode new value of stepMode
     */
    public void setStepMode(STEP_MODE p_stepMode) {
        m_stepMode = p_stepMode;

        for (Pata l_pata : m_patas) {
            l_pata.setStepMode(p_stepMode);
        } // end if
    }

    /**
     * Get the value of tracer
     *
     * @return the value of tracer
     */
    public Tracer getTracer() {
        return m_tracer;
    }

    /**
     * Set the value of tracer
     *
     * @param p_tracer new value of tracer
     */
    public void setTracer(Tracer p_tracer) {
        this.m_tracer = p_tracer;
    }

    /**
     * Get the value of pStep
     *
     * @return the value of pStep
     */
    public double getpStep() {
        return m_pStep;
    }

    /**
     * Set the value of pStep
     *
     * @param p_pStep new value of pStep
     */
    public void setpStep(double p_pStep) {
        double l_oldpStep = this.m_pStep;
        this.m_pStep = p_pStep;

        propertyChangeSupport.
                firePropertyChange(m_PROP_PSTEP, l_oldpStep, p_pStep);
    }

    /**
     * Get the value of autoStep
     *
     * @return the value of autoStep
     */
    public boolean isAutoStep() {
        return m_autoStep;
    }

    /**
     * Set the value of autoStep
     *
     * @param p_autoStep new value of autoStep
     */
    public void setAutoStep(boolean p_autoStep) {
        this.m_autoStep = p_autoStep;
    }

    /**
     * Get the value of sendCommands
     *
     * @return the value of sendCommands
     */
    public boolean isSendCommands() {
        return m_sendCommands;
    }

    /**
     * Set the value of sendCommands
     *
     * @param p_sendCommands new value of sendCommands
     */
    public void setSendCommands(boolean p_sendCommands) {
        m_sendCommands = p_sendCommands;
        for (Pata l_pata : m_patas) {
            l_pata.setSendCommands(p_sendCommands);
        } // end for

    }

    public void recalculaVertices() {
        for (Pata l_pata : m_patas) {
            l_pata.recalculaVertices();
        } // end for
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public enum STEP_MODE {
        ORTHOGONAL,
        ELLIPTICAL
    }

    public enum MARCHA {
        TRIPODE,
        COXAS
    }

    public MARCHA getMarcha() {
        return m_marcha;
    }

    public void setMarcha(MARCHA p_marcha) {
        m_marcha = p_marcha;
    }

    public String getDireccionTcp() {
        return m_direccionTcp;
    }

    /**
     * Establece la dirección del simulador. Al usar el simulador, la marcha
     * pasa a ser la de coxas
     *
     * @param p_direccionTcp por ejemplo "tcp://127.0.0.1:5000"
     */
    public void setDireccionTcp(String p_direccionTcp) {
        m_direccionTcp = p_direccionTcp;
        if (p_direccionTcp != null) {
            setMarcha(MARCHA.COXAS);
        } // end if
    }

    public class WalkerThread extends Thread {

        /**
         * Parámetro entre 0.0 y 1.0 que marca el avance del paso
         */
        private double m_wtPStep = 0.5;

        /**
         * Resolución del parámetro "p". Diez puntos de avance, diez en
         * retroceso
         */
        private double m_stepResolution = 1.0 / 4;

        /**
         *
         */
        private String m_legsToForward = "135";
        /**
         *
         */
        private String m_legsToBackward = "246";

        /**
         * Fase de la marcha de coxas: 0 a 2 recuperan un par de patas, 3 es
         * el empuje con las seis
         */
        private int m_faseCoxas = 0;

        /**
         * Pares de patas opuestas que se recuperan juntas en la marcha de
         * coxas
         */
        private final int[][] m_paresRecuperacion = {{1, 4}, {2, 5}, {3, 6}};

        /**
         * Lado de cada pata: +1 derecha (1, 2, 3), -1 izquierda (4, 5, 6)
         */
        private final int[] m_lado = {1, 1, 1, -1, -1, -1};

        private void autoStep() throws Exception {

            if (m_marcha == MARCHA.COXAS) {
                pasoCoxas();
                return;
            } // end if

            if (m_speed == 0) {
                // Nada por hacer
                safeSleep(500);
                return;
            } // end if
            muevePatas();
            m_wtPStep += m_stepResolution;
            if (m_wtPStep > 1.0) {
                m_wtPStep = 0.0;
                if (m_legsToForward.equals("135")) {
                    m_legsToForward = "246";
                    m_legsToBackward = "135";
                } else {
                    m_legsToForward = "135";
                    m_legsToBackward = "246";
                } // end if
            } // end if
            // Demora de 100 a 400mS, para velocidades del 100% a 25%
            int l_delay = ((m_speed - 25) * -4 + 400) * 5;
            safeSleep(l_delay);
        }

        /**
         * Marcha con un solo motor por pata (simulador Webots). Como las
         * patas no se levantan, se recuperan de a dos mientras las otras
         * cuatro sostienen el cuerpo por rozamiento, y después las seis
         * empujan juntas. Ver docs/MARCHA_COXAS.md
         */
        private void pasoCoxas() throws Exception {
            if (m_speed == 0) {
                // Nada por hacer
                safeSleep(500);
                return;
            } // end if

            double l_amplitud = AMPLITUD_MAX_COXAS * Math.min(m_speed, 100)
                    / 100.0;
            double l_rumbo = Math.toRadians(m_bearing);

            int[] l_patas;
            double l_signo;
            if (m_faseCoxas < m_paresRecuperacion.length) {
                l_patas = m_paresRecuperacion[m_faseCoxas];
                l_signo = 1.0;
            } else {
                l_patas = new int[]{1, 2, 3, 4, 5, 6};
                l_signo = -1.0;
            } // end if

            for (int l_nroPata : l_patas) {
                // Adelante: las derechas giran a +A y las izquierdas a -A.
                // Girar suma el mismo sentido en las seis
                double l_factor = m_lado[l_nroPata - 1] * Math.cos(l_rumbo)
                        - Math.sin(l_rumbo);
                l_factor = Math.max(-1.0, Math.min(1.0, l_factor));

                Pata l_pata = getPata(l_nroPata);
                l_pata.setAngCoxa(l_signo * l_amplitud * l_factor);
                l_pata.recalculaVertices();
                l_pata.estableceActuadores();
            } // end for

            esperaLlegada(l_patas);
            m_faseCoxas = (m_faseCoxas + 1) % (m_paresRecuperacion.length + 1);
        }

        /**
         * Espera a que las coxas indicadas lleguen a su setpoint, consultando
         * al Arduino 1 con el comando Q. Sin conexión espera un tiempo fijo
         */
        private void esperaLlegada(int[] p_patas) throws Exception {
            ClientServerPort l_port = m_shields[0].getPort();
            if (!m_sendCommands || l_port == null) {
                safeSleep((m_speed - 25) * -4 + 400);
                return;
            } // end if

            long l_limite = System.currentTimeMillis() + TIMEOUT_LLEGADA_MS;
            while (System.currentTimeMillis() < l_limite) {
                String[] l_campos = l_port.sendCommandWaitResponse("Q", null)
                        .trim().split("\\s+");
                boolean l_llegaron = true;
                for (int l_nroPata : p_patas) {
                    int l_actual = Integer.parseInt(l_campos[l_nroPata - 1]);
                    int l_setpoint = getPata(l_nroPata).getActCoxa()
                            .getSetpointPulsos();
                    if (Math.abs(l_actual - l_setpoint)
                            > TOLERANCIA_LLEGADA_PULSOS) {
                        l_llegaron = false;
                        break;
                    } // end if
                } // end for
                if (l_llegaron) {
                    return;
                } // end if
                safeSleep(50);
            } // end while
            if (m_tracer != null) {
                m_tracer.trace("Timeout esperando la llegada de las coxas "
                        + Arrays.toString(p_patas));
            } // end if
        }

        public String getLegsToForward() {
            return m_legsToForward;
        }

        public String getLegsToBackward() {
            return m_legsToBackward;
        }

        public void setLegsToForward(String p_legsToForward) {
            this.m_legsToForward = p_legsToForward;
        }

        public void setLegsToBackward(String p_legsToBackward) {
            this.m_legsToBackward = p_legsToBackward;
        }

        private void manualStep() throws Exception {
            if (m_wtPStep != getpStep()) {
                m_wtPStep = getpStep();
                muevePatas();
            } // end if
            safeSleep(100);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (m_autoStep) {
                        autoStep();
                    } else {
                        manualStep();
                    } // end if
                } catch (Exception l_ex) {
                    safeSleep(100);
                    if (m_tracer != null) {
                        m_tracer.trace(l_ex);
                    } else {
                        System.out.println(l_ex.getMessage());
                        l_ex.printStackTrace();
                    } // end if

                }
            } // end while
        }

        private void muevePatas() throws Exception {
            for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
                Pata l_pata = getPata(l_nroPata);
                if (m_legsToForward.contains(String.valueOf(l_nroPata))) {
                    l_pata.avanzaPaso(m_wtPStep);
                    l_pata.estableceActuadores();
                } else if (m_legsToBackward.contains(String.valueOf(l_nroPata))) {
                    l_pata.retrocedePaso(m_wtPStep);
                    l_pata.estableceActuadores();
                } // end if // end if
            } // end for
        }

        private void safeSleep(int p_millis) {
            try {
                sleep(p_millis);
            } catch (InterruptedException l_ex) {

            }
        }

    }

    public String doSend(int p_shield, String p_toSend) throws Exception {
        if (m_shields[p_shield - 1].getPort() == null) {
            // Shield no conectado (en el simulador solo existe el Arduino 1)
            return null;
        } // end if
        return m_shields[p_shield - 1].getPort().sendCommandWaitResponse(
                p_toSend, null);
    }

}
