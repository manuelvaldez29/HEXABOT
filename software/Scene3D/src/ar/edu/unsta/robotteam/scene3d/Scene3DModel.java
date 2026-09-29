package ar.edu.unsta.robotteam.scene3d;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gustavo
 */
public class Scene3DModel {

    private Map<String, Point3D> m_points;

    private List<Line3DSegment> m_segments;

    public Collection<Point3D> getPoints() {
        return m_points.values();
    }
    
    /**
     * Get the value of segments
     *
     * @return the value of segments
     */
    public List<Line3DSegment> getSegments() {
        return m_segments;
    }

    /**
     * Set the value of segments
     *
     * @param p_segments new value of segments
     */
    public void setSegments(List<Line3DSegment> p_segments) {
        this.m_segments = p_segments;
    }

    public Scene3DModel() {
        m_points = new LinkedHashMap<>();
        m_segments = new LinkedList<>();
    }

    public Point3D getPoint(String p_id) {
        return m_points.get(p_id);
    }

    public void addPoint(String p_id, double p_x, double p_y, double p_z) {
        Point3D l_newPoint = new Point3D(p_id, p_x, p_y, p_z);
        m_points.put(p_id, l_newPoint);
    }

    public void addSegment(String p_idPointA, String p_idPointB, Color p_color,
            int p_width) {

        Line3DSegment l_newSegment = new Line3DSegment(p_idPointA, p_idPointB);
        l_newSegment.setColor(p_color);
        l_newSegment.setWidth(p_width);
        
        m_segments.add(l_newSegment);
        
    }
}
