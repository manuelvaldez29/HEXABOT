package ar.edu.unsta.robotteam.hexabot.util;

import ar.edu.unsta.robotteam.scene3d.Point3D;
import java.awt.Point;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class MathUtils {

    /**
     * Distancia entre dos puntos bidimensionales
     *
     * @param p_p1
     * @param p_p2
     * @return
     */
    public static double distance(Point.Double p_p1, Point.Double p_p2) {
        return Math.hypot(p_p2.x - p_p1.x, p_p2.y - p_p1.y);
    }

    /**
     * Interpola el valor de una semi-elipse con centro en el origen, radios
     * p_radX y p_radY El parámetro p_p varía entre cero (x=-p_radX) y uno
     * (x=p_radX)
     *
     *
     * @param p_radX
     * @param p_radY
     * @param p_p
     * @return
     */
    public static double ellipticalInterpolator(double p_radX,
            double p_radY, double p_p) {

        double l_rx2 = p_radX * p_radX;
        double l_ry2 = p_radY * p_radY;
        double l_x = (p_p * 2.0 - 1.0) * p_radX;
        double l_x2 = l_x * l_x;

        double l_toReturn = Math.sqrt((l_rx2 * l_ry2 - l_ry2 * l_x2) / l_rx2);

        //System.out.format(
        //        "ellipticalInterpolator(radX=%.2f, radY=%.2f, p=%.2f, x=%.2f)=%.2f\n",
        //        p_radX, p_radY, p_p, l_x, l_toReturn);
        return l_toReturn;
    }

    /**
     * Devuelve el punto interpolado del semi elipse de A a B, que crece
     * verticalmente desde p_A, hasta alcanzar el radio p_radZ, y decrece hasta
     * p_B. Sólo permite interpolar entre dos puntos a la misma altura Z
     *
     * @param p_radZ
     * @param p_A
     * @param p_B
     * @param p_p Parámetro entre 0 (A) y 1 (B)
     * @return
     */
    public static Point3D ellipticalInterpolator(double p_radZ, Point3D p_A,
            Point3D p_B, double p_p) {

        if (p_A.getZ() != p_B.getZ()) {
            throw new RuntimeException("Sólo puede interpolar elipses con "
                    + "puntos A y B a la misma altura Z");
        } // end if

        double l_distance = Math.hypot(p_A.getX() - p_B.getX(), p_A.getY() - p_B.getY());

        Point3D l_f = new Point3D();
        l_f.setX(linearInterpolator(p_A.getX(), p_B.getX(), p_p));
        l_f.setY(linearInterpolator(p_A.getY(), p_B.getY(), p_p));
        l_f.setZ(ellipticalInterpolator(l_distance / 2, p_radZ, p_p) + p_A.getZ());

        return l_f;

    }

    /**
     * Devuelve el punto interpolado del segmento de recta tridimensional que
     * une A con B
     *
     * @param p_A
     * @param p_B
     * @param p_p Parámetro entre 0 (A) y 1 (B)
     * @return
     */
    public static Point3D linearInterpolator(Point3D p_A, Point3D p_B,
            double p_p) {

        Point3D l_f = new Point3D();
        l_f.setX(linearInterpolator(p_A.getX(), p_B.getX(), p_p));
        l_f.setY(linearInterpolator(p_A.getY(), p_B.getY(), p_p));
        l_f.setZ(linearInterpolator(p_A.getZ(), p_B.getZ(), p_p));
        return l_f;
    }

    /**
     *
     * @param p_A
     * @param p_B
     * @param p_p
     * @return
     */
    public static double linearInterpolator(double p_A, double p_B, double p_p) {
        return p_A + (p_B - p_A) * p_p;
    }


    /**
     * Devuelve sólo una raíz: si el centro B está a la derecha del centro A,
     * devuelve la raíz más alta; si no, la más baja
     *
     * @param p_centerA
     * @param p_radiusA
     * @param p_centerB
     * @param p_radiusB
     * @return
     */
    public static Point.Double solveQuadEq(Point.Double p_centerA,
            double p_radiusA, Point.Double p_centerB, double p_radiusB) {
        Point.Double l_newCenterB = new Point.Double(p_centerB.x - p_centerA.x,
                p_centerB.y - p_centerA.y);

        Point.Double l_root = solveQuadEq(p_radiusA, l_newCenterB, p_radiusB);
        l_root.x += p_centerA.x;
        l_root.y += p_centerA.y;

        return l_root;
    }

    /**
     * Devuelve sólo una raíz: si el centro B está a la derecha del eje Y,
     * devuelve la raíz más alta; si no, la más baja
     *
     * @param p_radiusA
     * @param p_centerB
     * @param p_radiusB
     * @return
     */
    public static Point.Double solveQuadEq(double p_radiusA,
            Point.Double p_centerB, double p_radiusB) {

        double l_ra2 = p_radiusA * p_radiusA;
        double l_rb2 = p_radiusB * p_radiusB;
        double l_xb2 = p_centerB.x * p_centerB.x;
        double l_yb2 = p_centerB.y * p_centerB.y;
        double l_ra_min_rb2 = (p_radiusA - p_radiusB) * (p_radiusA - p_radiusB);
        double l_ra_plus_rb2 = (p_radiusA + p_radiusB) * (p_radiusA + p_radiusB);

        double l_x1 = Double.NaN;
        double l_y1 = Double.NaN;
        double l_x2 = Double.NaN;
        double l_y2 = Double.NaN;

        double l_r = 2 * (l_xb2 + l_yb2);
        double l_u = 2 * p_centerB.y * (l_xb2 + l_yb2);

        if (l_r != 0.0) {
            double l_p = p_centerB.x * (l_ra2 - l_rb2 + l_xb2 + l_yb2);
            double l_q = Math.sqrt(-l_yb2 * (-l_ra_min_rb2 + l_xb2 + l_yb2)
                    * (-l_ra_plus_rb2 + l_xb2 + l_yb2));

            l_x1 = (l_p - l_q) / l_r;
            l_x2 = (l_p + l_q) / l_r;
        } // end if

        if (l_u != 0.0) {
            double l_s = l_ra2 * l_yb2 + l_yb2 * (-l_rb2 + l_xb2 + l_yb2);
            double l_t = p_centerB.x * Math.sqrt(-l_yb2 * (-l_ra_min_rb2 + l_xb2
                    + l_yb2) * (-l_ra_plus_rb2 + l_xb2 + l_yb2));

            l_y1 = (l_s + l_t) / l_u;
            l_y2 = (l_s - l_t) / l_u;
        } // end if

        if (p_centerB.x >= 0) {
            // El segundo círculo está a la derecha. Toma la raíz más alta
            if (l_y1 >= l_y2) {
                return new Point.Double(l_x1, l_y1);
            } else {
                return new Point.Double(l_x2, l_y2);
            } // end if
        } else // El segundo círculo está a la izquierda. Toma la raíz más baja
        {
            if (l_y1 <= l_y2) {
                return new Point.Double(l_x1, l_y1);
            } else {
                return new Point.Double(l_x2, l_y2);
            } // end if // end if
        }
    }
}
