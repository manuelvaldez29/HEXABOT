package ar.edu.unsta.robotteam.hexabot.util;

import ar.edu.unsta.robotteam.hexabot.view.Tracer;
import gnu.io.SerialPort;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

/**
 * Canal de comunicación con un Arduino. Puede ser un puerto serie (robot real)
 * o una conexión TCP/IP (robot simulado en Webots). Para TCP, el nombre del
 * puerto tiene la forma "tcp://host:puerto", por ejemplo "tcp://localhost:5000".
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class ClientServerPort {

    public static final String PREFIJO_TCP = "tcp://";

    private String m_portName;
    private SerialPort m_port;
    private Socket m_socket;
    private BufferedReader m_reader;
    private PrintWriter m_writer;
    private int m_timeoutInMs;
    private Tracer m_traceListener;

    /**
     *
     * @param p_portName
     * @param p_timeoutInMs
     * @param p_traceListener
     */
    public ClientServerPort(String p_portName, int p_timeoutInMs,
            Tracer p_traceListener) {
        m_portName = p_portName;
        m_timeoutInMs = p_timeoutInMs;
        m_traceListener = p_traceListener;
    }

    /**
     *
     * @throws Exception
     */
    public void open() throws Exception {
        InputStream l_in;
        OutputStream l_out;
        if (m_portName.startsWith(PREFIJO_TCP)) {
            // Robot simulado: "tcp://host:puerto"
            String l_direccion = m_portName.substring(PREFIJO_TCP.length());
            int l_dosPuntos = l_direccion.lastIndexOf(':');
            String l_host = l_direccion.substring(0, l_dosPuntos);
            int l_puerto = Integer.parseInt(l_direccion.substring(
                    l_dosPuntos + 1));
            m_socket = new Socket();
            m_socket.connect(new InetSocketAddress(l_host, l_puerto), 3000);
            m_socket.setTcpNoDelay(true);
            l_in = m_socket.getInputStream();
            l_out = m_socket.getOutputStream();
        } else {
            // Robot real: puerto serie
            m_port = ar.edu.unsta.robotteam.hexabot.util.HexaUtils.open(
                    m_portName, 115200);
            l_in = m_port.getInputStream();
            l_out = m_port.getOutputStream();
            Thread.sleep(1000);
        } // end if
        m_reader = new BufferedReader(new InputStreamReader(l_in, "ASCII"));
        m_writer = new PrintWriter(new OutputStreamWriter(l_out, "ASCII"), true);
    }

    /**
     *
     */
    public void close() {

        try {
            m_reader.close();
        } catch (Exception l_ex) {
        }
        try {
            m_writer.close();
        } catch (Exception l_ex) {
        }
        try {
            if (m_port != null) {
                m_port.close();
            } // end if
            if (m_socket != null) {
                m_socket.close();
            } // end if
        } catch (Exception l_ex) {
        }
    }

    /**
     *
     * @param p_cmd
     * @param p_args
     */
    public synchronized void sendCommand(String p_cmd, Object p_args) {
        String l_toSend = p_cmd;

        if (p_args != null) {
            l_toSend += " " + p_args;
        } // end if
        m_writer.println(l_toSend);
        if (m_traceListener != null) {
            m_traceListener.trace("> " + l_toSend);
        } // end if
    }

    /**
     *
     * @return @throws Exception
     */
    public synchronized String readResponse() throws Exception {
        if (m_reader.ready()) {
            String l_response = m_reader.readLine();
            if (m_traceListener != null) {
                m_traceListener.trace("< " + l_response);
            } // end if
            return l_response;
        } // end if

        return null;
    }

    /**
     *
     * @param p_cmd
     * @param p_arg1
     * @param p_arg2
     * @return
     * @throws Exception
     */
    public synchronized String sendCommandWaitResponse(String p_cmd, int p_arg1,
            int p_arg2) throws Exception {
        return sendCommandWaitResponse(p_cmd, p_arg1 + " " + p_arg2);
    }

    /**
     *
     * @param p_cmd
     * @param p_args
     * @return
     * @throws Exception
     */
    public synchronized String sendCommandWaitResponse(String p_cmd,
            Object p_args) throws Exception {
        // Consume respuestas previas

        while (m_reader.ready()) {
            m_reader.read();
        };
        sendCommand(p_cmd, p_args);
        String l_response = null;

        int l_waiting = 0;

        while (l_response == null) {
            l_response = readResponse();
            if (l_response == null) {
                l_waiting += 5;
                Thread.sleep(5);

                if (l_waiting > m_timeoutInMs) {
                    throw new TimeoutException(
                            "Timeout waiting response to command " + p_cmd);
                } // end if

            } // end if
        } // end while

        if (l_response.startsWith("OK " + p_cmd)) {
            return l_response.substring(4).trim();
        } // end if

        throw new Exception("Error in response: " + l_response);
    }
}
