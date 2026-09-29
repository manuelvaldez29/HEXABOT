package ar.edu.unsta.robotteam.hexabot.model;

/**
 * Modelo de un actuador lineal.
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class Actuador {

    /**
     * Canal asignado al control del actuador
     */
    private CanalShield m_canalShield;

    /**
     * Máxima longitud aceptable en el actuador, incluido el largo muerto, en
     * mm.
     */
    private double m_maxLongitud;
    /**
     * Mínima longitud aceptable en el actuador (largo muerto), en mm. El
     * encoder cuenta "0" cuando llega a este punto.
     */
    private double m_minLongitud;
    /**
     * Constante que, al multiplicarse por milímetros, devuelve pulsos
     */
    private double m_mm2pulse;
    /**
     * "true" cuando deben enviarse los comandos al canalShield. "false" cuando
     * se realizan todos los cálculos, pero los comandos no se envían
     *
     */
    private boolean m_sendCommands;

    /**
     * Valor actual del setpoint, en mm. Incluido el largo muerto. Debe variar
     * entre m_minLongitud y m_maxLongitud.
     */
    private double m_setpoint;

    /**
     * Último setpoint enviado, en pulsos del encoder
     */
    private int m_setpointPulsos;

    /**
     * Largo actual actuador, en mm. Incluido el largo muerto. Debe variar entre
     * m_minLongitud y m_maxLongitud.
     */
    private double m_valorActual;

    /**
     *
     */
    public Actuador() {
    }

    public void cmdInit() throws Exception {
        if (m_sendCommands) {
            m_canalShield.cmdInit();
        } // end if
    }

    /**
     * Get the value of m_canalShield
     *
     * @return the value of m_canalShield
     */
    public CanalShield getCanalShield() {
        return m_canalShield;
    }

    /**
     * Set the value of m_canalShield
     *
     * @param p_canalShield new value of m_canalShield
     */
    public void setCanalShield(CanalShield p_canalShield) {
        m_canalShield = p_canalShield;
    }

    /**
     * Get the value of m_maxLongitud
     *
     * @return the value of m_maxLongitud
     */
    public double getMaxLongitud() {
        return m_maxLongitud;
    }

    /**
     * Set the value of m_maxLongitud
     *
     * @param p_maxLongitud new value of m_maxLongitud
     */
    public void setMaxLongitud(double p_maxLongitud) {
        m_maxLongitud = p_maxLongitud;
    }

    /**
     * Get the value of minLongitud
     *
     * @return the value of minLongitud
     */
    public double getMinLongitud() {
        return m_minLongitud;
    }

    /**
     * Set the value of minLongitud
     *
     * @param p_minLongitud new value of minLongitud
     */
    public void setMinLongitud(double p_minLongitud) {
        m_minLongitud = p_minLongitud;
    }

    /**
     * Get the value of mm2pulse
     *
     * @return the value of mm2pulse
     */
    public double getMm2pulse() {
        return m_mm2pulse;
    }

    /**
     * Set the value of mm2pulse
     *
     * @param p_mm2pulse new value of mm2pulse
     */
    public void setMm2pulse(double p_mm2pulse) {
        m_mm2pulse = p_mm2pulse;
    }

    /**
     * Get the value of m_setpoint
     *
     * @return the value of m_setpoint
     */
    public double getSetpoint() {
        return m_setpoint;
    }

    /**
     * Set the value of m_setpoint
     *
     * @param p_setpoint new value of m_setpoint
     */
    public void setSetpoint(double p_setpoint) throws Exception {
        m_setpoint = p_setpoint;

        if (m_canalShield == null) {
            return;
        } // end if

        int l_pulses = (int) ((p_setpoint - m_minLongitud) * m_mm2pulse);
        m_setpointPulsos = l_pulses;

        if (m_sendCommands) {
            m_canalShield.cmdSet(l_pulses);
        } // end if

    }

    /**
     * Último setpoint calculado, en pulsos del encoder
     *
     * @return
     */
    public int getSetpointPulsos() {
        return m_setpointPulsos;
    }

    /**
     * Get the value of m_valorActual
     *
     * @return the value of m_valorActual
     */
    public double getValorActual() {
        return m_valorActual;
    }

    /**
     * Set the value of m_valorActual
     *
     * @param valorActual new value of m_valorActual
     */
    public void setValorActual(double valorActual) {
        m_valorActual = valorActual;
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
    }

}
