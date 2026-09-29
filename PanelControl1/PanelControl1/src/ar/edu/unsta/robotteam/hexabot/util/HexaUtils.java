package ar.edu.unsta.robotteam.hexabot.util;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class HexaUtils {

    /**
     *
     * @return
     */
    public static List<String> getSerialPorts() {

        List<String> l_toReturn = new LinkedList<String>();
        Enumeration l_enum = CommPortIdentifier.getPortIdentifiers();

        while (l_enum.hasMoreElements()) {
            try {
                CommPortIdentifier l_portIdentifier
                        = (CommPortIdentifier) l_enum.nextElement();

                if (l_portIdentifier.isCurrentlyOwned()) {
                    continue;
                } // end if

                CommPort l_commPort = l_portIdentifier.open("TestRXTX", 1000);
                l_commPort.close();
                if (!(l_commPort instanceof SerialPort)) {
                    continue;
                } //end if

                l_toReturn.add(l_portIdentifier.getName());

            } catch (Exception l_ex) {
                System.err.println(l_ex.getMessage());
                l_ex.printStackTrace(System.err);
            }

        }// end while
        return l_toReturn;
    }

    /**
     *
     * @param p_portName
     * @param p_baudRate
     * @return
     * @throws Exception
     */
    public static SerialPort open(String p_portName, int p_baudRate)
            throws Exception {
        CommPortIdentifier l_portIdentifier = CommPortIdentifier.
                getPortIdentifier(p_portName);
        if (l_portIdentifier.isCurrentlyOwned()) {
            throw new Exception("Error: Port " + p_portName
                    + " is currently in use");
        } // end if

        CommPort l_commPort = l_portIdentifier.open("TestRXTX", 1000); // Espera 1000mS la apertura
        if (!(l_commPort instanceof SerialPort)) {
            throw new Exception(
                    "Error: Only serial ports are handled by this function. "
                    + p_portName + " isn't a serial port");
        } //end if

        SerialPort l_serialPort = (SerialPort) l_commPort;
        l_serialPort.setSerialPortParams(p_baudRate, SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        return l_serialPort;
    }

    /**
     *
     * @param p_args
     * @throws Exception
     */
    public static void main(String[] p_args) throws Exception {
        List<String> l_ports = getSerialPorts();
        System.out.println(l_ports);

        System.out.println("Opening port...");
        ClientServerPort l_port = new ClientServerPort("/dev/ttyACM0", 2000,
                null);
        l_port.open();
        String l_response = l_port.sendCommandWaitResponse("A", null);
        System.out.println(l_response);
        l_response = l_port.sendCommandWaitResponse("I", 1);
        System.out.println(l_response);
        l_response = l_port.sendCommandWaitResponse("H", null);
        System.out.println(l_response);
        l_response = l_port.sendCommandWaitResponse("Q", null);
        System.out.println(l_response);
        System.out.println("Port closed.");
    }

    /**
     *
     * @param p_configFileName
     * @return
     * @throws Exception
     */
    public static Map<Integer, Map<Integer, Integer>> readConfig(
            String p_configFileName) throws Exception {
        Map<Integer, Map<Integer, Integer>> l_toReturn = new LinkedHashMap<>();
        BufferedReader l_reader = new BufferedReader(new FileReader(
                p_configFileName));
        String l_line = l_reader.readLine();
        while (l_line != null) {
            l_line = l_line.trim();
            System.out.println(l_line);
            if (!l_line.startsWith("#")) {
                // No es un comentario
                String[] l_fields = l_line.split(",");
                int l_arduino = Integer.parseInt(l_fields[0]);
                int l_param = Integer.parseInt(l_fields[1]);
                int l_valor = Integer.parseInt(l_fields[2]);

                Map<Integer, Integer> l_arduinoSet = l_toReturn.get(l_arduino);
                if (l_arduinoSet == null) {
                    // Crea ahora
                    l_arduinoSet = new LinkedHashMap<>();
                    l_toReturn.put(l_arduino, l_arduinoSet);
                } // end if

                l_arduinoSet.put(l_param, l_valor);
            } // end if
            l_line = l_reader.readLine();
        } // end while
        l_reader.close();
        return l_toReturn;
    }
}
