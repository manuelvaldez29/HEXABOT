package ar.edu.unsta.robotteam.hexabot.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class JVistaPataSuperior extends JComponent {

    private double m_angCoxa = 0.0;
    private double m_angFemur = 0.0;
    private double m_angTibia = 0.0;

    @Override
    public void paint(Graphics p_gr) {
        super.paint(p_gr);

        p_gr.setColor(Color.DARK_GRAY);
        p_gr.fillRect(0, 0, getWidth(), getHeight());

        Graphics2D l_gr = (Graphics2D) p_gr;

        // Dibuja el fémur
        l_gr.setStroke(new BasicStroke(4));
        int l_largoFemur = getWidth() / 2 - 20;

        int l_x0 = getWidth() / 2;
        int l_y0 = getHeight() - 10;
        int l_x1 = l_x0 - (int) (l_largoFemur * Math.sin(m_angCoxa));
        int l_y1 = l_y0 - (int) (l_largoFemur * Math.cos(m_angCoxa));

        l_gr.setColor(Color.green);
        l_gr.drawLine(l_x0, l_y0, l_x1, l_y1);

        // Dibuja la coxa
        l_gr.setColor(Color.red);
        l_gr.fillOval(l_x0 - 6, l_y0 - 6, 12, 12);

    }

    /**
     *
     * @param p_angCoxa
     * @param p_angFemur
     * @param p_angTibia
     */
    public void setAngulos(double p_angCoxa, double p_angFemur,
            double p_angTibia) {
        m_angCoxa = Math.toRadians(p_angCoxa);
        m_angFemur = Math.toRadians(p_angFemur);
        m_angTibia = Math.toRadians(p_angTibia);
    }

}
