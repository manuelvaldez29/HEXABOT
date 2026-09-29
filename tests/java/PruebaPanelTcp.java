import ar.edu.unsta.robotteam.hexabot.model.Hexapodo;
import ar.edu.unsta.robotteam.hexabot.view.Tracer;

/**
 * Prueba sin interfaz gráfica del modelo del panel contra el Arduino virtual.
 * Hace la misma secuencia que un usuario en el panel: Discover, Configure,
 * Sit, Up y joystick hacia adelante. Verifica que la marcha de coxas recorra
 * sus 4 fases esperando la llegada de las coxas.
 *
 * Uso: ver tests/probar_panel.ps1
 */
public class PruebaPanelTcp {

    public static void main(String[] p_args) throws Exception {
        final StringBuilder l_log = new StringBuilder();
        Tracer l_tracer = new Tracer() {
            @Override
            public void trace(String p_msg) {
                synchronized (l_log) {
                    l_log.append(p_msg).append('\n');
                }
            }

            @Override
            public void trace(Throwable p_ex) {
                trace("EXCEPCION " + p_ex);
            }
        };

        Hexapodo l_hexa = new Hexapodo(l_tracer, l_tracer, l_tracer, l_tracer);
        l_hexa.setDireccionTcp(p_args[0]);
        l_hexa.setSendCommands(true);

        l_hexa.doDiscover();
        l_hexa.doConfigure();
        l_hexa.cmdSit(null);
        // Como en el panel: esperar a que las coxas lleguen al inicio de
        // carrera antes de Up (Up manda H, que corta la búsqueda de inicio)
        Thread.sleep(3000);
        l_hexa.cmdUp(null);

        l_hexa.setBearing(0);
        l_hexa.setSpeed(100);
        Thread.sleep(Integer.parseInt(p_args[1]));
        l_hexa.setSpeed(0);
        Thread.sleep(600);
        l_hexa.doClose();

        String l_texto = l_log.toString();
        int l_setpoints = contar(l_texto, "> S ");
        int l_consultas = contar(l_texto, "> Q");
        int l_errores = contar(l_texto, "EXCEPCION") + contar(l_texto, "Timeout");
        System.out.println("Setpoints enviados: " + l_setpoints);
        System.out.println("Consultas Q: " + l_consultas);
        System.out.println("Errores / timeouts: " + l_errores);
        if (l_errores > 0) {
            System.out.println(l_texto);
            System.exit(1);
        } // end if
        // Up (6 coxas) + al menos un ciclo completo de marcha (2+2+2+6)
        if (l_setpoints < 6 + 12) {
            System.out.println("La marcha no completó un ciclo");
            System.exit(1);
        } // end if
        System.out.println("OK");
        System.exit(0);
    }

    private static int contar(String p_texto, String p_patron) {
        int l_n = 0;
        int l_i = p_texto.indexOf(p_patron);
        while (l_i >= 0) {
            l_n++;
            l_i = p_texto.indexOf(p_patron, l_i + 1);
        } // end while
        return l_n;
    }
}
