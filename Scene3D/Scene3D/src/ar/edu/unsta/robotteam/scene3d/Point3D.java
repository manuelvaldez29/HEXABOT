package ar.edu.unsta.robotteam.scene3d;

/**
 *
 * @author gustavo
 */
public class Point3D {

    /**
     * 
     */
    private String m_id;
    /**
     * 
     */
    private double m_x = 0.0;
    /**
     * 
     */
    private double m_y = 0.0;
    /**
     * 
     */
    private double m_z = 0.0;

    public Point3D() {

    }

    public Point3D(String p_id, double p_x, double p_y, double p_z) {
        m_id = p_id;
        m_x = p_x;
        m_y = p_y;
        m_z = p_z;
    }

    /**
     * @return the m_id
     */
    public String getId() {
        return m_id;
    }

    /**
     * @param id the m_id to set
     */
    public void setId(String id) {
        this.m_id = id;
    }

    /**
     * @return the m_x
     */
    public double getX() {
        return m_x;
    }

    /**
     * @param x the m_x to set
     */
    public void setX(double x) {
        this.m_x = x;
    }

    /**
     * @return the m_y
     */
    public double getY() {
        return m_y;
    }

    /**
     * @param y the m_y to set
     */
    public void setY(double y) {
        this.m_y = y;
    }

    /**
     * @return the m_z
     */
    public double getZ() {
        return m_z;
    }

    /**
     * @param z the m_z to set
     */
    public void setZ(double z) {
        this.m_z = z;
    }

    public String toString() {
        return String.format("%s(%.2f,%.2f,%.2f)", getId(), getX(), getY(),
                getZ());
    }

    public void updateFrom(Point3D p_other) {
        setX(p_other.getX());
        setY(p_other.getY());
        setZ(p_other.getZ());
    }

    public void updateFrom(double p_x, double p_y, double p_z) {
        setX(p_x);
        setY(p_y);
        setZ(p_z);
    }

}
