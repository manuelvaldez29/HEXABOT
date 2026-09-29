package ar.edu.unsta.robotteam.hexabot.util;

import ar.edu.unsta.robotteam.hexabot.view.Tracer;
import gnu.io.SerialPort;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class ClientServerPort {

    private String m_portName;
    private SerialPort m_port;
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
        m_port = ar.edu.unsta.robotteam.hexabot.util.HexaUtils.open(m_portName,
                115200);
        InputStream l_in = m_port.getInputStream();
        m_reader = new BufferedReader(new InputStreamReader(l_in, "ASCII"));
        OutputStream l_out = m_port.getOutputStream();
        m_writer = new PrintWriter(new OutputStreamWriter(l_out, "ASCII"), true);
        Thread.sleep(1000);
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
            m_port.close();
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
