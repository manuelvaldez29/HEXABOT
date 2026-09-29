package ar.edu.unsta.robotteam.scene3d.projector;

import ar.edu.unsta.robotteam.scene3d.Point3D;
import ar.edu.unsta.robotteam.scene3d.Scene3DView;
import java.awt.Point;

/**
 *
 * @author gustavo
 */
public class IsometricProjector extends AbstractProjector {

    public IsometricProjector(Scene3DView p_view) {
        super(p_view);
    }

    @Override
    public Point project(Point3D p_point3D) {
        double l_scale = getView().getScale();
        Point l_origin = getView().getOrigin();

        double l_x = (0.866 * p_point3D.getX() - 0.866 * p_point3D.getY()) * l_scale  + l_origin.x;
        double l_y = l_origin.y - (p_point3D.getZ() + 0.5 * p_point3D.getX() + 0.5 * p_point3D.getY()) * l_scale;
        
        return new Point((int) l_x, (int) l_y);
    }
    
}
