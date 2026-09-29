package ar.edu.unsta.robotteam.scene3d.projector;

import ar.edu.unsta.robotteam.scene3d.Point3D;
import java.awt.Point;

/**
 *
 * @author gustavo
 */
public interface Projector {

    public Point project(Point3D p_point3D);
}
