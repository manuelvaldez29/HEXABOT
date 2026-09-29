package ar.edu.unsta.robotteam.hexabot.view;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public interface Tracer {

    public void trace(final String p_msg);
    
    public void trace(final Throwable p_ex);
    
}
