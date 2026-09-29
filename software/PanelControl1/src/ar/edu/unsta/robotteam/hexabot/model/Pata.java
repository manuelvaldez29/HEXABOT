package ar.edu.unsta.robotteam.hexabot.model;

import ar.edu.unsta.robotteam.hexabot.util.MathUtils;
import ar.edu.unsta.robotteam.scene3d.Point3D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Modelo de una pata de un hexápodo
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class Pata {

    private Hexapodo.STEP_MODE m_stepMode;

    /**
     * Get the value of stepMode
     *
     * @return the value of stepMode
     */
    public Hexapodo.STEP_MODE getStepMode() {
        return m_stepMode;
    }

    /**
     * Set the value of stepMode
     *
     * @param p_stepMode new value of stepMode
     */
    public void setStepMode(Hexapodo.STEP_MODE p_stepMode) {
        this.m_stepMode = p_stepMode;
    }

    /**
     * Actuador responsable del movimiento horizontal de la coxa
     */
    private Actuador m_actCoxa;

    /**
     * Actuador responsable del movimiento vertical del fémur
     */
    private Actuador m_actFemur;

    /**
     * Actuador responsable del movimiento vertical de la tibia
     */
    private Actuador m_actTibia;

    /**
     * Ángulo actual entre la coxa y el radio característico correspondiente a
     * la pata. Cero: coincidente con el radio de la pata; negativo: en sentido
     * horario (hacia la contracción). En grados sexagesimales.
     */
    private double m_angCoxa = 0.0;

    /**
     * Ángulo actual entre la coxa y el fémur. Cero: 90º; negativo: fémur hacia
     * la coxa (hacia la contracción). En grados sexagesimales.
     */
    private double m_angFemur = 0.0;
    /**
     * Ángulo del radio característico, en grados sexagesimales. 60º pata 1, 0º
     * pata 2, -60º pata 3, etc.
     */
    private double m_angRadial;

    /**
     * Ángulo entre el fémur y la tibia. Cero: 90º, centro de la excursión;
     * negativo: tibia hacia el fémur (hacia la contracción). En grados
     * sexagesimales.
     */
    private double m_angTibia = 0.0;
    /**
     *
     */
    private Point3D m_centroPaso;
    /**
     *
     */
    private double m_clearance = 100.0;

    /**
     * Extremo inferior de la coxa, sobre el plano XY. Una vez definido, no
     * cambia
     */
    private Point3D m_coxaA;

    /**
     * Extremo superior de la coxa. Una vez definido, no cambia
     */
    private Point3D m_coxaB;
    /**
     *
     */
    private Point3D m_extremoPasoAdelante;
    /**
     *
     */
    private Point3D m_extremoPasoAtras;

    /**
     * Extremo interior del fémur. Es un punto de la coxa. Una vez definido, no
     * cambia
     */
    private Point3D m_femurA;

    /**
     * Extremo superior del fémur. Depende del ángulo de coxa y de fémur.
     */
    private Point3D m_femurB;

    /**
     * Altura sobre el plano XY, del nacimiento del fémur. En mm.
     */
    private double m_largoCoxa = 65.0;

    /**
     * Distancia entre la articulación coxa-fémur, y la articulación fémur-tiba.
     * En mm.
     */
    private double m_largoFemur = 380.0;

    /**
     * Distancia entre la articulación fémur-tibia, y el punto de contacto con
     * el piso. En mm.
     */
    private double m_largoTibia = 475.0;
    /**
     * Número de pata, de 1 a 6.
     */
    private int m_nroPata;
    /**
     *
     */
    private double m_rumboX;
    /**
     *
     */
    private double m_rumboY;
    /**
     *
     */
    private double m_semiPaso;
    /**
     * "true" cuando los comandos son enviados a los shields
     */
    private boolean m_sendCommands;

    /**
     * Extremo interior de la tibia (rodilla)
     */
    private Point3D m_tibiaA;
    /**
     * Extremo exterior de la tibia (
     */
    private Point3D m_tibiaB;

    /**
     *
     * @param p_nroPata
     */
    public Pata(int p_nroPata) {
        setNroPata(p_nroPata);
        m_actCoxa = new Actuador();
        m_actFemur = new Actuador();
        m_actTibia = new Actuador();
        // 14 pulsos / vuelta, 32 vueltas  / pulgada (rosca WW 5/32"), 25.4mm / pulgada, reducción 16:60
        //m_actCoxa.setMm2pulse(14.0 * 32.0 * (60.0 / 16.0) / 25.4);

        // 14 pulsos / vuelta, 24 vueltas  / pulgada (rosca WW 3/16"), 25.4mm / pulgada
        m_actFemur.setMm2pulse(14.0 * 24.0 / 25.4);
        m_actTibia.setMm2pulse(14.0 * 24.0 / 25.4);

        m_actCoxa.setMinLongitud(40.0);
        m_actFemur.setMinLongitud(295.0);
        m_actTibia.setMinLongitud(280.0);

        m_coxaA = new Point3D();
        m_coxaB = new Point3D();

        m_femurA = new Point3D();
        m_femurB = new Point3D();

        m_tibiaA = new Point3D();
        m_tibiaB = new Point3D();

    }

    public void avanzaPaso(double p_p) {
        if (m_extremoPasoAdelante == null) {
            return;
        } // end if
        if (m_extremoPasoAtras == null) {
            return;
        } // end if

        Point3D l_nuevoExtremo = MathUtils.ellipticalInterpolator(m_clearance,
                m_extremoPasoAtras,
                m_extremoPasoAdelante, p_p);
        recalculaAngulos(l_nuevoExtremo);
    }

    /**
     * Ordena simultáneamente a todos los actuadores de la pata ir a la posición
     * de inicio
     *
     * @param p_doneListener
     * @throws java.lang.Exception
     */
    public void cmdSit(ActionListener p_doneListener) throws Exception {

        m_actCoxa.cmdInit();
        m_actFemur.cmdInit();
        m_actTibia.cmdInit();

        if (p_doneListener != null) {
            p_doneListener.actionPerformed(new ActionEvent(this, 0, ""));
        } // end if
    }

    /**
     * Calcula los setpoints y mueve los actuadores hasta la posición indicada
     */
    public void estableceActuadores() throws Exception {
        // Recalcula actuador de coxa ----------------------------------------

        // Ángulo de la biela, respecto del radio característico.
        // Cuando m_angCoxa = 0º, toma el valor 60º. Cuando m_angCoxa se cierra,
        // el ángulo de la biela también
        double l_angBiela = Math.toRadians(60 + m_angCoxa);
        // El eje de la coxa está a 50mm del eje sobre la biela. La tuerca de
        // avance, a 83mm del eje de la coxa
        double l_bielaX = Math.cos(l_angBiela) * 50;
        double l_bielaY = Math.sin(l_angBiela) * 50;
        // 40mm es la mínima extensión del actuador de coxa. Pero la resta se 
        // efectúa dentro del actuador
        double l_actCoxa = Math.hypot(83 - l_bielaX, l_bielaY);
        m_actCoxa.setSetpoint(l_actCoxa);

        // Recalcula actuador de fémur --------------------------------------
        // Ángulo interior entre fémur y coxa
        double l_angFemur = Math.toRadians(-m_angFemur);
        // El eje de inicio del fémur está a 330mm de la base del actuador
        double l_femurX = Math.cos(l_angFemur) * 330;
        double l_femurY = Math.sin(l_angFemur) * 330;
        // La aplicación del actuador del fémur está a 100mm arriba del inicio del fémur
        double l_actFemur = Math.hypot(l_femurX, l_femurY - 100);
        m_actFemur.setSetpoint(l_actFemur);

        // Recalcula actuador de tibia
        double l_angTibia = Math.toRadians(90 + m_angTibia);
        // El eje de inicio del actuador de tibia está a 80mm de la rodilla
        double l_TibiaX = Math.cos(l_angTibia) * 80;
        double l_TibiaY = Math.sin(l_angTibia) * 80;
        // La aplicación del actuador de la tibia está a 322mm de la rodilla
        double l_actTibia = Math.hypot(322 - l_TibiaX, l_TibiaY);
        m_actTibia.setSetpoint(l_actTibia);

    }

    /**
     * Get the value of m_actCoxa
     *
     * @return the value of m_actCoxa
     */
    public Actuador getActCoxa() {
        return m_actCoxa;
    }

    /**
     * Set the value of m_actCoxa
     *
     * @param actCoxa new value of m_actCoxa
     */
    public void setActCoxa(Actuador actCoxa) {
        this.m_actCoxa = actCoxa;
    }

    /**
     * Get the value of m_actFemur
     *
     * @return the value of m_actFemur
     */
    public Actuador getActFemur() {
        return m_actFemur;
    }

    /**
     * Set the value of m_actFemur
     *
     * @param actFemur new value of m_actFemur
     */
    public void setActFemur(Actuador actFemur) {
        this.m_actFemur = actFemur;
    }

    /**
     * Get the value of m_actTibia
     *
     * @return the value of m_actTibia
     */
    public Actuador getActTibia() {
        return m_actTibia;
    }

    /**
     * Set the value of m_actTibia
     *
     * @param actTibia new value of m_actTibia
     */
    public void setActTibia(Actuador actTibia) {
        this.m_actTibia = actTibia;
    }

    /**
     * Get the value of m_angCoxa
     *
     * @return the value of m_angCoxa
     */
    public double getAngCoxa() {
        return m_angCoxa;
    }

    /**
     * Set the value of m_angCoxa
     *
     * @param angCoxa new value of m_angCoxa
     */
    public void setAngCoxa(double angCoxa) {
        this.m_angCoxa = angCoxa;
    }

    /**
     * Get the value of m_angFemur
     *
     * @return the value of m_angFemur
     */
    public double getAngFemur() {
        return m_angFemur;
    }

    /**
     * Set the value of m_angFemur
     *
     * @param angFemur new value of m_angFemur
     */
    public void setAngFemur(double angFemur) {
        this.m_angFemur = angFemur;
    }

    /**
     * Get the value of angRadial
     *
     * @return the value of angRadial
     */
    public double getAngRadial() {
        return m_angRadial;
    }

    /**
     * Set the value of angRadial
     *
     * @param p_angRadial new value of angRadial
     */
    public void setAngRadial(double p_angRadial) {
        this.m_angRadial = p_angRadial;
    }

    /**
     * Get the value of m_angTibia
     *
     * @return the value of m_angTibia
     */
    public double getAngTibia() {
        return m_angTibia;
    }

    /**
     * Set the value of m_angTibia
     *
     * @param angTibia new value of m_angTibia
     */
    public void setAngTibia(double angTibia) {
        this.m_angTibia = angTibia;
    }

    /**
     * Get the value of centroPaso
     *
     * @return the value of centroPaso
     */
    public Point3D getCentroPaso() {
        return m_centroPaso;
    }

    /**
     * Set the value of centroPaso
     *
     * @param p_centroPaso new value of centroPaso
     */
    public void setCentroPaso(Point3D p_centroPaso) {
        m_centroPaso = p_centroPaso;

        if (p_centroPaso != null) {
            m_extremoPasoAtras
                    = new Point3D("A", p_centroPaso.getX() - m_rumboX,
                            p_centroPaso.getY() - m_rumboY, p_centroPaso.getZ());
            m_extremoPasoAdelante = new Point3D("B", p_centroPaso.getX()
                    + m_rumboX,
                    p_centroPaso.getY() + m_rumboY, p_centroPaso.getZ());
        } else {
            m_extremoPasoAdelante = null;
            m_extremoPasoAtras = null;
        }

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
    }

    /**
     * Get the value of coxaA
     *
     * @return the value of coxaA
     */
    public Point3D getCoxaA() {
        return m_coxaA;
    }

    /**
     * Set the value of coxaA
     *
     * @param p_coxaA new value of coxaA
     */
    public void setCoxaA(Point3D p_coxaA) {
        this.m_coxaA = p_coxaA;
    }

    /**
     * Get the value of coxaB
     *
     * @return the value of coxaB
     */
    public Point3D getCoxaB() {
        return m_coxaB;
    }

    /**
     * Set the value of coxaB
     *
     * @param p_coxaB new value of coxaB
     */
    public void setCoxaB(Point3D p_coxaB) {
        this.m_coxaB = p_coxaB;
    }

    /**
     * Get the value of femurA
     *
     * @return the value of femurA
     */
    public Point3D getFemurA() {
        return m_femurA;
    }

    /**
     * Set the value of femurA
     *
     * @param p_femurA new value of femurA
     */
    public void setFemurA(Point3D p_femurA) {
        this.m_femurA = p_femurA;
    }

    /**
     * Get the value of femurB
     *
     * @return the value of femurB
     */
    public Point3D getFemurB() {
        return m_femurB;
    }

    /**
     * Set the value of femurB
     *
     * @param p_femurB new value of femurB
     */
    public void setFemurB(Point3D p_femurB) {
        this.m_femurB = p_femurB;
    }

    /**
     * Get the value of m_largoCoxa
     *
     * @return the value of m_largoCoxa
     */
    public double getLargoCoxa() {
        return m_largoCoxa;
    }

    /**
     * Set the value of m_largoCoxa
     *
     * @param largoCoxa new value of m_largoCoxa
     */
    public void setLargoCoxa(double largoCoxa) {
        this.m_largoCoxa = largoCoxa;
    }

    /**
     * Get the value of m_largoFemur
     *
     * @return the value of m_largoFemur
     */
    public double getLargoFemur() {
        return m_largoFemur;
    }

    /**
     * Set the value of m_largoFemur
     *
     * @param largoFemur new value of m_largoFemur
     */
    public void setLargoFemur(double largoFemur) {
        this.m_largoFemur = largoFemur;
    }

    /**
     * Get the value of m_largoTibia
     *
     * @return the value of m_largoTibia
     */
    public double getLargoTibia() {
        return m_largoTibia;
    }

    /**
     * Set the value of m_largoTibia
     *
     * @param largoTibia new value of m_largoTibia
     */
    public void setLargoTibia(double largoTibia) {
        this.m_largoTibia = largoTibia;
    }

    /**
     * Get the value of m_nroPata
     *
     * @return the value of m_nroPata
     */
    public int getNroPata() {
        return m_nroPata;
    }

    /**
     * Set the value of m_nroPata
     *
     * @param nroPata new value of m_nroPata
     */
    public void setNroPata(int nroPata) {
        this.m_nroPata = nroPata;
    }

    /**
     * Get the value of rumboX
     *
     * @return the value of rumboX
     */
    public double getRumboX() {
        return m_rumboX;
    }

    /**
     * Set the value of rumboX
     *
     * @param p_rumboX new value of rumboX
     */
    public void setRumboX(double p_rumboX) {
        this.m_rumboX = p_rumboX;
        setCentroPaso(m_centroPaso);
    }

    /**
     * Get the value of rumboY
     *
     * @return the value of rumboY
     */
    public double getRumboY() {
        return m_rumboY;

    }

    /**
     * Set the value of rumboY
     *
     * @param p_rumboY new value of rumboY
     */
    public void setRumboY(double p_rumboY) {
        this.m_rumboY = p_rumboY;
        setCentroPaso(m_centroPaso);

    }

    /**
     * Get the value of semiPaso
     *
     * @return the value of semiPaso
     */
    public double getSemiPaso() {
        return m_semiPaso;
    }

    /**
     * Set the value of semiPaso
     *
     * @param p_semiPaso new value of semiPaso
     */
    public void setSemiPaso(double p_semiPaso) {
        this.m_semiPaso = p_semiPaso;
    }

    /**
     * Get the value of tibiaA
     *
     * @return the value of tibiaA
     */
    public Point3D getTibiaA() {
        return m_tibiaA;
    }

    /**
     * Set the value of tibiaA
     *
     * @param p_tibiaA new value of tibiaA
     */
    public void setTibiaA(Point3D p_tibiaA) {
        this.m_tibiaA = p_tibiaA;
    }

    /**
     * Get the value of tibiaB
     *
     * @return the value of tibiaB
     */
    public Point3D getTibiaB() {
        return m_tibiaB;
    }

    /**
     * Set the value of tibiaB
     *
     * @param p_tibiaB new value of tibiaB
     */
    public void setTibiaB(Point3D p_tibiaB) {
        this.m_tibiaB = p_tibiaB;
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
        this.m_sendCommands = p_sendCommands;
        m_actCoxa.setSendCommands(p_sendCommands);
        m_actFemur.setSendCommands(p_sendCommands);
        m_actTibia.setSendCommands(p_sendCommands);
    }

    /**
     * Recalcula los ángulos de coxa, fémur y tibia, para que el extremo de la
     * pata esté en el punto solicitado
     *
     * @param p_endpoint
     */
    public void recalculaAngulos(Point3D p_endpoint) {

        //System.out.format("Endpoint: %.2f, %.2f, %.2f\n", p_endpoint.getX(),
        //        p_endpoint.getY(), p_endpoint.getZ());
        // Ángulo horizontal del fémur
        double l_deltaX = p_endpoint.getX() - m_femurA.getX();
        double l_deltaY = p_endpoint.getY() - m_femurA.getY();

        //Ángulo absoluto, en el plano XY, del fémur+tibia
        double l_angHorizFemur = Math.atan2(l_deltaY, l_deltaX);

        m_angCoxa = Math.toDegrees(l_angHorizFemur) - m_angRadial;
        //System.out.format("Ang coxa: %.3f\n", m_angCoxa);

        // Distancia XY del endpoint a la coxa, o inicio del fémur
        double l_distXY = Math.hypot(p_endpoint.getX() - m_femurA.getX(),
                p_endpoint.getY()
                - m_femurA.getY());

        //System.out.format("DistXY: %.3f\n", l_distXY);
        // El sistema de ecuaciones debe resolverse proyectado sobre el plano
        // vertical que pasa por el radio característico
        Point.Double l_centroA = new Point.Double(0.0, m_femurA.getZ());
        Point.Double l_centroB = new Point.Double(l_distXY, p_endpoint.getZ());
        //System.out.format("CentroA: %s   CentroB: %s\n", l_centroA, l_centroB);
        Point.Double l_rodilla = MathUtils.solveQuadEq(l_centroA, m_largoFemur,
                l_centroB, m_largoTibia);

        //System.out.format("Raices: R0: %s R1: %s\n", l_raices[0], l_raices[1]);
        double l_angVertFemur = Math.atan2(l_rodilla.getY() - l_centroA.getY(),
                l_rodilla.getX() - l_centroA.getX());
        m_angFemur = -Math.toDegrees(l_angVertFemur);

        //System.out.format("Ang fémur: %.3f\n", m_angFemur);
        // Ángulo absoluto de la tibia
        double l_angAbsTibia = Math.toDegrees(Math.atan2(l_centroB.getY()
                - l_rodilla.getY(), l_centroB.getX() - l_rodilla.getX()));

        m_angTibia = m_angFemur + l_angAbsTibia + 90.0;
        //System.out.format("Ang abs tibia: %.3f ang relativo tibia: %.3f\n",
        //        l_angAbsTibia, m_angTibia);

    }

    /**
     * Recalcula la posición de los vértices del fémur y tibia, a partir de sus
     * ángulos actuales. Los vértices de la coxa no se mueven.
     */
    public void recalculaVertices() {
        // Movimiento horizontal y vertical del fémur

        m_femurA.setX(m_coxaA.getX());
        m_femurA.setY(m_coxaA.getY());
        m_femurA.setZ(m_coxaA.getZ() + m_largoCoxa);

        double l_angHorizFemur = Math.toRadians(m_angRadial + m_angCoxa);
        double l_cosVertFemur = Math.cos(Math.toRadians(-m_angFemur));
        double l_largoAparenteFemur = m_largoFemur * l_cosVertFemur;
        m_femurB.setX(m_femurA.getX() + Math.cos(l_angHorizFemur)
                * l_largoAparenteFemur);
        m_femurB.setY(m_femurA.getY() + Math.sin(l_angHorizFemur)
                * l_largoAparenteFemur);
        m_femurB.setZ(m_femurA.getZ() + Math.sin(Math.toRadians(-m_angFemur))
                * m_largoFemur);

        // Tibia
        double l_angVertTibia = Math.toRadians(-90 - m_angFemur + m_angTibia);

        m_tibiaA.setX(m_femurB.getX());
        m_tibiaA.setY(m_femurB.getY());
        m_tibiaA.setZ(m_femurB.getZ());

        double l_cosVertTibia = Math.cos(l_angVertTibia);
        double l_largoAparenteTibia = m_largoTibia * l_cosVertTibia;
        m_tibiaB.setX(m_tibiaA.getX() + Math.cos(l_angHorizFemur)
                * l_largoAparenteTibia);
        m_tibiaB.setY(m_tibiaA.getY() + Math.sin(l_angHorizFemur)
                * l_largoAparenteTibia);
        m_tibiaB.setZ(m_tibiaA.getZ() + Math.sin(l_angVertTibia) * m_largoTibia);

    }

    public void retrocedePaso(double p_p) {
        if (m_extremoPasoAdelante == null) {
            return;
        } // end if
        if (m_extremoPasoAtras == null) {
            return;
        } // end if
        Point3D l_nuevoExtremo = MathUtils.linearInterpolator(
                m_extremoPasoAdelante,
                m_extremoPasoAtras, p_p);
        recalculaAngulos(l_nuevoExtremo);
    }

    /**
     *
     * @param p_canalCoxa
     * @param p_canalFemur
     * @param p_canalTibia
     */
    public void setCanales(CanalShield p_canalCoxa, CanalShield p_canalFemur,
            CanalShield p_canalTibia) {

        m_actCoxa.setCanalShield(p_canalCoxa);
        m_actFemur.setCanalShield(p_canalFemur);
        m_actTibia.setCanalShield(p_canalTibia);

    }

    /**
     * Devuelve una cadena con la descripción del estado de la pata
     *
     * @return
     */
    public String toString() {
        StringBuilder l_toReturn = new StringBuilder();
        l_toReturn.append("Estado de la pata: " + getNroPata() + "\n");
        l_toReturn.append(String.format(
                "  Ángulos:   CX=%1.1fº FM=%1.1fº TB=%1.1fº\n", m_angCoxa,
                m_angFemur, m_angTibia));
        if (m_centroPaso == null) {
            l_toReturn.append("  Centro de paso: ** NO ESTABLECIDO **\n");
        } else {
            l_toReturn.append(String.format(
                    "  Centro de paso:  Id=%s X=%.1fmm Y=%.1fmm Z=%.1fmm\n",
                    m_centroPaso.getId(), m_centroPaso.getX(), m_centroPaso.
                    getY(), m_centroPaso.getZ()));
        } // end if
        l_toReturn.append(String.format(
                "  Setpoints: CX=%.1fmm   FM=%.1fmm   TB=%.1fmm\n",
                getActCoxa().getSetpoint(), getActFemur().getSetpoint(),
                getActTibia().getSetpoint()));
        l_toReturn.append(String.format(
                "  Valores: CX=%.1fmm   FM=%.1fmm   TB=%.1fmm\n",
                getActCoxa().getValorActual(), getActFemur().getValorActual(),
                getActTibia().getValorActual()));

        return l_toReturn.toString();
    }

}
