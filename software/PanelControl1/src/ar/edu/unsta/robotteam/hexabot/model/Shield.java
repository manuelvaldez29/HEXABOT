package ar.edu.unsta.robotteam.hexabot.model;

import ar.edu.unsta.robotteam.hexabot.util.ClientServerPort;
import ar.edu.unsta.robotteam.hexabot.view.Tracer;
import java.util.List;
import java.util.Map;

/**
 * Clase que envuelve un Arduino y su funcionalidad: - seis canales de
 * actuadores - seis sensores de pisada - tres canales RGB
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class Shield {

    private Tracer m_tracer;

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
     * Número de shield, de 1 a 3
     */
    private int m_nroShield;

    private ClientServerPort m_port;

    /**
     * Get the value of nroShield
     *
     * @return the value of nroShield
     */
    public int getNroShield() {
        return m_nroShield;
    }

    /**
     * Set the value of nroShield
     *
     * @param p_nroShield new value of nroShield
     */
    public void setNroShield(int p_nroShield) {
        this.m_nroShield = p_nroShield;
    }

    private CanalShield[] m_canales;

    /**
     *
     */
    public Shield(int p_nroShield) {

        setNroShield(p_nroShield);
        m_canales = new CanalShield[6];

        m_canales[0] = new CanalShield(this, 1);
        m_canales[1] = new CanalShield(this, 2);
        m_canales[2] = new CanalShield(this, 3);
        m_canales[3] = new CanalShield(this, 4);
        m_canales[4] = new CanalShield(this, 5);
        m_canales[5] = new CanalShield(this, 6);
    }

    /**
     *
     * @param p_nroCanal
     * @return
     */
    public CanalShield getCanalShield(int p_nroCanal) {
        if (p_nroCanal < 1) {
            return null;
        } // end if
        if (p_nroCanal > 6) {
            return null;
        } // end if

        return m_canales[p_nroCanal - 1];
    }

    /**
     * Configura el shield
     *
     * @throws Exception
     */
    public void doConfigure(Map<Integer, Integer> p_params) throws Exception {
        m_tracer.trace("CONFIGURE");

        if (m_port == null) {
            throw new Exception("No port @ shield " + m_nroShield);
        } // end if

        for (int l_param : p_params.keySet()) {
            int l_value = p_params.get(l_param);
            m_port.sendCommandWaitResponse("C", l_param + " " + l_value);
        } // end for

    }

    /**
     * Descubre y abre el puerto serie correspondiente al shield
     *
     * @throws Exception
     */
    public void doDiscover(List<String> m_serialPorts) throws Exception {
        m_tracer.trace("DISCOVER");
        for (String l_portName : m_serialPorts) {
            ClientServerPort l_port = null;
            try {

                m_tracer.trace("Discovering port " + l_portName);
                l_port = new ClientServerPort(l_portName, 500, m_tracer);
                l_port.open();
                String l_response = l_port.sendCommandWaitResponse("A", null);
                m_tracer.trace("Response: " + l_response);

                if (l_response.equals(String.valueOf(getNroShield()))) {
                    m_port = l_port;
                    return;
                } else {
                    l_port.close();
                } // end if

            } catch (Exception l_ex) {
                if (l_port != null) {
                    l_port.close();
                } // end if
                m_tracer.trace(l_ex);
            }
        } // end for

    }

    /**
     * Cierra el puerto serie
     */
    public void close() {
        m_tracer.trace("CLOSE");
        if (m_port == null) {
            return;
        } // end if

        m_port.close();
    }

    public ClientServerPort getPort() {
        return m_port;
    }

}
