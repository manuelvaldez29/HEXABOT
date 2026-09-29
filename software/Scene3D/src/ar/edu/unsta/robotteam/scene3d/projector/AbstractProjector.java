package ar.edu.unsta.robotteam.scene3d.projector;

import ar.edu.unsta.robotteam.scene3d.Scene3DView;

/**
 *
 * @author gustavo
 */
public abstract class AbstractProjector implements Projector {

    private Scene3DView m_view;

    /**
     * Get the value of view
     *
     * @return the value of view
     */
    public Scene3DView getView() {
        return m_view;
    }

    /**
     * Set the value of view
     *
     * @param p_view new value of view
     */
    public void setView(Scene3DView p_view) {
        this.m_view = p_view;
    }

    public AbstractProjector(Scene3DView p_view) {
        m_view = p_view;
        p_view.setProjector(this);
    }
}
