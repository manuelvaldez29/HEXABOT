package ar.edu.unsta.robotteam.hexabot.model;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class CanalShield {

    /**
     * Referencia al shield
     */
    private Shield m_shield;

    /**
     * Número de canal, de 1 a 6
     */
    private int m_nroCanal;

    /**
     *
     */
    public CanalShield(Shield p_shield, int p_nroChanal) {
        m_shield = p_shield;
        m_nroCanal = p_nroChanal;
    }

    public int getNroCanal() {
        return m_nroCanal;
    }

    public String cmdInit() throws Exception {
        return m_shield.getPort().sendCommandWaitResponse("I", m_nroCanal);
    }

    public String cmdSet(int p_pulses) throws Exception {
        return m_shield.getPort().sendCommandWaitResponse("S", m_nroCanal,
                p_pulses);
    }
}
