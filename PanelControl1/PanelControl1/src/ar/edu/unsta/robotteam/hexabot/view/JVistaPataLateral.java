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
public class JVistaPataLateral extends JComponent {

    private double m_angCoxa = 0.0;
    private double m_angFemur = 0.0;
    private double m_angTibia = 0.0;

    @Override
    public void paint(Graphics p_gr) {
        super.paint(p_gr);

        Graphics2D l_gr = (Graphics2D) p_gr;

        l_gr.setColor(Color.DARK_GRAY);
        l_gr.fillRect(0, 0, getWidth(), getHeight());

        // Dibuja coxa
        int l_x0 = 10;
        int l_y0 = getHeight() / 2;
        int l_x1 = 10;
        int l_y1 = 10;

        l_gr.setStroke(new BasicStroke(4));

        l_gr.setColor(Color.red);
        l_gr.drawLine(l_x0, l_y0, l_x1, l_y1);

        // Dibuja fémur. Ángulo negativo: hacia arriba; cero: horizontal
        int l_largoFemur = getWidth() / 2 - 20;
        l_x1 = l_x0 + (int) (l_largoFemur * Math.cos(m_angFemur));
        l_y1 = l_y0 + (int) (l_largoFemur * Math.sin(m_angFemur));
        l_gr.setColor(Color.green);
        l_gr.drawLine(l_x0, l_y0, l_x1, l_y1);

        // Dibuja tibia. Ángulo negativo: hacia adentro (contrae la pata); cero: 90º del fémur
        l_gr.setColor(Color.yellow);
        int l_largoTibia = getWidth() / 2 - 20;
        double l_ef = Math.PI / 2 - m_angFemur + m_angTibia;
        int l_x2 = l_x1 - (int) (l_largoTibia * Math.cos(l_ef));
        int l_y2 = l_y1 + (int) (l_largoTibia * Math.sin(l_ef));
        l_gr.drawLine(l_x1, l_y1, l_x2, l_y2);

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
