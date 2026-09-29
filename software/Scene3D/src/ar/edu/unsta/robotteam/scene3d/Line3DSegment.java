package ar.edu.unsta.robotteam.scene3d;

import java.awt.Color;

/**
 *
 * @author gustavo
 */
public class Line3DSegment {
    private Color m_color = null;
    private String m_idPointA;
    private String m_idPointB;
    private int m_width = -1;


    public Line3DSegment(String p_idPointA, String p_idPointB) {
        setIdPointA(p_idPointA);
        setIdPointB(p_idPointB);
    }
    /**
     * Get the value of color
     *
     * @return the value of color
     */
    public Color getColor() {
        return m_color;
    }
    
    /**
     * Get the value of idPointA
     *
     * @return the value of idPointA
     */
    public String getIdPointA() {
        return m_idPointA;
    }
    /**
     * Get the value of idPointB
     *
     * @return the value of idPointB
     */
    public String getIdPointB() {
        return m_idPointB;
    }
    /**
     * Get the value of width
     *
     * @return the value of width
     */
    public int getWidth() {
        return m_width;
    }
    /**
     * Set the value of color
     *
     * @param p_color new value of color
     */
    public void setColor(Color p_color) {
        this.m_color = p_color;
    }

    /**
     * Set the value of idPointA
     *
     * @param p_idPointA new value of idPointA
     */
    public void setIdPointA(String p_idPointA) {
        this.m_idPointA = p_idPointA;
    }
    /**
     * Set the value of idPointB
     *
     * @param p_idPointB new value of idPointB
     */
    public void setIdPointB(String p_idPointB) {
        this.m_idPointB = p_idPointB;
    }
    /**
     * Set the value of width
     *
     * @param p_width new value of width
     */
    public void setWidth(int p_width) {
        this.m_width = p_width;
    }
    
    public String toString() {
        return String.format("(%s)-(%s)", m_idPointA, m_idPointB);
    }
}
