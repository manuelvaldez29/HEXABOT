package ar.edu.unsta.robotteam.hexabot.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class JJoystick extends JComponent {

    private List<ActionListener> m_actionListeners;
    /**
     * Posición X del knob, entre -100 y 100
     */
    private int m_knobX = 0;
    /**
     * Posición Y del knob, entre -100 y 100
     */
    private int m_knobY = 0;
    private int m_draggedAtX;
    private int m_draggedAtY;

    /**
     *
     */
    public JJoystick() {
        super();

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent p_evt) {
                m_draggedAtX = p_evt.getX(); // - getLocation().x;
                m_draggedAtY = p_evt.getY(); // - getLocation().y;
            }

        });
        // Cambia el cursor cuando se desplaza sobre el knob
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent p_evt) {
                int l_cursorX = p_evt.getX();
                int l_cursorY = p_evt.getY();

                Point l_dragged = gui2logical(m_draggedAtX, m_draggedAtY);
                Point l_cursor = gui2logical(l_cursorX, l_cursorY);

                int l_deltaX = l_cursor.x - l_dragged.x;
                int l_deltaY = l_cursor.y - l_dragged.y;

                int l_newKnobX = m_knobX + l_deltaX;
                int l_newKnobY = m_knobY + l_deltaY;

                int l_dist2 = l_newKnobX * l_newKnobX + l_newKnobY * l_newKnobY;
                if (l_dist2 > 100 * 100) {
                    return;
                } // end if

                if (l_deltaX == 0 && l_deltaY == 0) {
                    return;
                } // end if

                m_knobX += l_deltaX;
                m_knobY += l_deltaY;
                repaint();

                m_draggedAtX = l_cursorX;
                m_draggedAtY = l_cursorY;

                fireActionPerformed();
            }

            @Override
            public void mouseMoved(MouseEvent p_evt) {

                int l_cursorX = p_evt.getX();
                int l_cursorY = p_evt.getY();

                int l_maxX = getWidth() - 1;
                int l_maxY = getHeight() - 1;
                int l_maxDiameter = ((l_maxX < l_maxY) ? l_maxX : l_maxY) + 1;
                int l_maxRadius = l_maxDiameter / 2;
                int l_minRadius = l_maxRadius / 4;
                int l_knobRadius = l_minRadius * 3 / 4;

                int l_centerX = getWidth() / 2;
                int l_centerY = getHeight() / 2;

                int l_knobX = l_centerX + l_maxRadius * m_knobX / 100;
                int l_knobY = l_centerY + l_maxRadius * m_knobY / 100;

                if (inCircle(l_knobX, l_knobY, l_knobRadius, l_cursorX,
                        l_cursorY)) {
                    setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    setCursor(
                            new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                } // end if
            }

        });

    }

    @Override
    public void paint(Graphics p_gr) {
        super.paint(p_gr);

        Rectangle l_bounds = p_gr.getClipBounds();

        int l_maxX = l_bounds.width - 1;
        int l_maxY = l_bounds.height - 1;
        int l_centerX = l_bounds.width / 2;
        int l_centerY = l_bounds.height / 2;

        int l_maxDiameter = ((l_maxX < l_maxY) ? l_maxX : l_maxY) + 1;
        int l_maxRadius = l_maxDiameter / 2;

        int l_minDiameter = l_maxDiameter / 4;
        int l_minRadius = l_maxRadius / 4;

        // Dibuja el fondo
        p_gr.setColor(Color.DARK_GRAY);
        p_gr.fillRect(0, 0, l_bounds.width, l_bounds.height);

        // Dibuja la grilla
        p_gr.setColor(Color.CYAN);
        p_gr.drawLine(l_centerX - l_maxRadius, l_centerY, l_centerX
                - l_minRadius, l_centerY);
        p_gr.drawLine(l_centerX + l_maxRadius, l_centerY, l_centerX
                + l_minRadius, l_centerY);
        p_gr.drawLine(l_centerX, l_centerY - l_maxRadius, l_centerX, l_centerY
                - l_minRadius);
        p_gr.drawLine(l_centerX, l_centerY + l_maxRadius, l_centerX, l_centerY
                + l_minRadius);

        int l_maxRadius071 = l_maxRadius * 71 / 100;
        int l_minRadius071 = l_minRadius * 71 / 100;
        p_gr.drawLine(l_centerX - l_maxRadius071, l_centerY + l_maxRadius071,
                l_centerX - l_minRadius071, l_centerY + l_minRadius071);
        p_gr.drawLine(l_centerX + l_maxRadius071, l_centerY - l_maxRadius071,
                l_centerX + l_minRadius071, l_centerY - l_minRadius071);
        p_gr.drawLine(l_centerX - l_maxRadius071, l_centerY - l_maxRadius071,
                l_centerX - l_minRadius071, l_centerY - l_minRadius071);
        p_gr.drawLine(l_centerX + l_maxRadius071, l_centerY + l_maxRadius071,
                l_centerX + l_minRadius071, l_centerY + l_minRadius071);

        drawCircle(p_gr, l_centerX, l_centerY, l_minDiameter);
        drawCircle(p_gr, l_centerX, l_centerY, l_maxDiameter / 2);
        drawCircle(p_gr, l_centerX, l_centerY, l_maxDiameter * 3 / 4);
        drawCircle(p_gr, l_centerX, l_centerY, l_maxDiameter);

        drawKnob(p_gr, l_maxRadius, l_minRadius * 3 / 4, l_centerX, l_centerY);
    }

    private void drawCircle(Graphics p_gr, int p_centerX, int p_centerY,
            int p_diameter) {
        int l_r = p_diameter / 2;
        p_gr.drawOval(p_centerX - l_r, p_centerY - l_r, p_diameter, p_diameter);
    }

    private void drawKnob(Graphics p_gr, int p_joystickRadius, int p_knobRadius,
            int p_centerX, int p_centerY) {

        int l_X0 = p_centerX + (m_knobX * p_joystickRadius / 100) - p_knobRadius;
        int l_Y0 = p_centerY + (m_knobY * p_joystickRadius / 100) - p_knobRadius;
        p_gr.setColor(new Color(0, 128, 0));
        p_gr.fillOval(l_X0, l_Y0, p_knobRadius * 2, p_knobRadius * 2);
        p_gr.setColor(Color.GREEN);
        p_gr.fillOval(l_X0 + 4, l_Y0 + 4, p_knobRadius * 2 - 8, p_knobRadius * 2
                - 8);
    }

    /**
     * Devuelve "true" si el punto está dentro del círculo indicado
     *
     * @param p_circleX
     * @param p_circleY
     * @param p_circleR
     * @param p_pointX
     * @param p_pointY
     * @return
     */
    private static boolean inCircle(int p_circleX, int p_circleY, int p_circleR,
            int p_pointX, int p_pointY) {

        int l_deltaX2 = p_circleX - p_pointX;
        int l_deltaY2 = p_circleY - p_pointY;
        l_deltaX2 = l_deltaX2 * l_deltaX2;
        l_deltaY2 = l_deltaY2 * l_deltaY2;

        return (p_circleR * p_circleR) >= (l_deltaX2 + l_deltaY2);

    }

    private Point gui2logical(int p_guiX, int p_guiY) {
        int l_maxX = getWidth() - 1;
        int l_maxY = getHeight() - 1;
        int l_maxDiameter = ((l_maxX < l_maxY) ? l_maxX : l_maxY) + 1;
        int l_maxRadius = l_maxDiameter / 2;
        int l_centerX = getWidth() / 2;
        int l_centerY = getHeight() / 2;

        Point l_log = new Point((p_guiX - l_centerX) * 100 / l_maxRadius,
                (p_guiY - l_centerY) * 100 / l_maxRadius);

        return l_log;
    }

    private Point logical2gui(int p_logX, int p_logY) {
        int l_maxX = getWidth() - 1;
        int l_maxY = getHeight() - 1;
        int l_maxDiameter = ((l_maxX < l_maxY) ? l_maxX : l_maxY) + 1;
        int l_maxRadius = l_maxDiameter / 2;
        int l_centerX = getWidth() / 2;
        int l_centerY = getHeight() / 2;

        Point l_gui = new Point(l_centerX + p_logX * l_maxRadius / 100,
                l_centerY + p_logY * l_maxRadius / 100);
        return l_gui;
    }

    /**
     * Devuelve un valor de 25 a 100, con la distancia lógica del knob al centro
     * Si está en el centro, devuelve 0
     *
     * @return
     */
    public int getDistance() {
        int l_dist = m_knobX * m_knobX + m_knobY * m_knobY;
        if (l_dist < 25 * 25) {
            return 0;
        } // end if

        return (int) Math.sqrt(l_dist);
    }

    /**
     *
     */
    public void fireActionPerformed() {
        if (m_actionListeners == null) {
            return;
        } // end if
        for (ActionListener l_listener : m_actionListeners) {
            l_listener.actionPerformed(new ActionEvent(this, 0, null));
        } // end for
    }

    /**
     *
     * @param p_listener
     */
    public void addActionListener(ActionListener p_listener) {
        if (p_listener == null) {
            return;
        } // end if
        if (m_actionListeners == null) {
            m_actionListeners = new LinkedList<ActionListener>();
        } // end if
        m_actionListeners.add(p_listener);
    }

    /**
     * Devuelve el rumbo indicado, entre -179..179. Cero hacia arriba. Positivos
     * hacia la derecha. Si la distancia es cero, devuelve cero
     *
     * @return
     */
    public int getBearing() {
        if (getDistance() == 0) {
            return 0;
        } // end if

        return (int) (Math.atan2(m_knobX, -m_knobY) * 180.0 / Math.PI);

    }

    /**
     *
     */
    public void toCenter() {
        m_knobX = 0;
        m_knobY = 0;
    }

}
