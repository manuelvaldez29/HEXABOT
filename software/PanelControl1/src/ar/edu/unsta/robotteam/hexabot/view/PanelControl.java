package ar.edu.unsta.robotteam.hexabot.view;

import ar.edu.unsta.robotteam.hexabot.model.Hexapodo;
import ar.edu.unsta.robotteam.hexabot.model.Pata;
import ar.edu.unsta.robotteam.hexabot.util.ClientServerPort;
import ar.edu.unsta.robotteam.scene3d.Point3D;
import ar.edu.unsta.robotteam.scene3d.Scene3DModel;
import ar.edu.unsta.robotteam.scene3d.Scene3DView;
import ar.edu.unsta.robotteam.scene3d.projector.IsometricProjector;
import ar.edu.unsta.robotteam.scene3d.projector.ObliqueProjector;
import ar.edu.unsta.robotteam.scene3d.projector.XYProjector;
import ar.edu.unsta.robotteam.scene3d.projector.XZProjector;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 *
 * @author Laboratorio de Robótica / Facultad de Ingeniería / UNSTA
 */
public class PanelControl extends javax.swing.JFrame implements Tracer {

    private JJoystick m_joystick;

    private Hexapodo m_hexapodo;
    private Scene3DModel m_3dModel;
    private Scene3DView m_3dView;

    /**
     * Creates new form PanelControl
     */
    public PanelControl() {
        initComponents();

        configure3DView();

        Tracer l_tracer1 = new Tracer() {
            @Override
            public void trace(String p_msg) {
                PanelControl.this.trace(ui_txtTrace1, p_msg);
            }

            @Override
            public void trace(Throwable p_ex) {
                PanelControl.this.trace(ui_txtTrace1, p_ex);
            }
        };
        Tracer l_tracer2 = new Tracer() {
            @Override
            public void trace(String p_msg) {
                PanelControl.this.trace(ui_txtTrace2, p_msg);
            }

            @Override
            public void trace(Throwable p_ex) {
                PanelControl.this.trace(ui_txtTrace2, p_ex);
            }
        };
        Tracer l_tracer3 = new Tracer() {
            @Override
            public void trace(String p_msg) {
                PanelControl.this.trace(ui_txtTrace3, p_msg);
            }

            @Override
            public void trace(Throwable p_ex) {
                PanelControl.this.trace(ui_txtTrace3, p_ex);
            }
        };

        m_hexapodo = new Hexapodo(this, l_tracer1, l_tracer2, l_tracer3);

        ui_pnlGraphics.add(new JVistaPata());
        ui_pnlGraphics.add(new JVistaPata());
        ui_pnlGraphics.add(new JVistaPata());
        ui_pnlGraphics.add(new JVistaPata());
        ui_pnlGraphics.add(new JVistaPata());
        ui_pnlGraphics.add(new JVistaPata());

        m_joystick = new JJoystick();
        // Escucha el cambio de valor del joystick
        m_joystick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent p_evt) {

                try {
                    m_hexapodo.setBearing(m_joystick.getBearing());
                    m_hexapodo.setSpeed(m_joystick.getDistance());

                    updateIndicators();
                } catch (Exception ex) {
                    trace(ex);
                }
            }
        });

        ui_pnlLeft.add(m_joystick, BorderLayout.CENTER);

        TimerTask l_task = new TimerTask() {
            @Override
            public void run() {

                int l_selPage = ((JTabbedPane) ui_pnlRight).getSelectedIndex();

                switch (l_selPage) {
                    case 0: { // Values
                        updateValues();
                    }
                    break;
                    case 1: { // Graphics
                        updateGraphics();

                    }
                    break;
                    case 2: { // 3D

                    }
                    break;
                } // end switch

            }
        };

        m_hexapodo.setHeight(ui_sliHeight.getValue());
        m_hexapodo.setHalfStep(100.0);

        Timer l_timer = new Timer();
        l_timer.schedule(l_task, 500, 100);

        updateIndicators();
    }

    private void configure3DView() {
        m_3dModel = new Scene3DModel();
        m_3dView = new Scene3DView(m_3dModel);
        new IsometricProjector(m_3dView);

        m_3dView.setScale(0.3);
        m_3dView.setAliasing(true);

        // Agrega la grilla del piso
        int l_gridWidth = 1000;

        int l_height = 0; // primera vez

        for (int l_y = -l_gridWidth; l_y <= l_gridWidth; l_y += 50) {
            m_3dModel.addPoint("GR_Y" + l_y + "_A", -l_gridWidth, l_y, l_height);
            m_3dModel.addPoint("GR_Y" + l_y + "_B", l_gridWidth, l_y, l_height);
            m_3dModel.addSegment("GR_Y" + l_y + "_A", "GR_Y" + l_y + "_B",
                    Color.darkGray, 1);
        } // end for
        for (int l_x = -l_gridWidth; l_x <= l_gridWidth; l_x += 50) {
            m_3dModel.addPoint("GR_X" + l_x + "_A", l_x, -l_gridWidth, l_height);
            m_3dModel.addPoint("GR_X" + l_x + "_B", l_x, l_gridWidth, l_height);
            m_3dModel.addSegment("GR_X" + l_x + "_A", "GR_X" + l_x + "_B",
                    Color.darkGray, 1);
        } // end for
        // Agrega el sistema de coordenadas
        m_3dModel.addPoint("O", 0, 0, 0);
        m_3dModel.addPoint("VERSOR_X", 1000.0, 0, 0);
        m_3dModel.addPoint("VERSOR_Y", 0, 1000.0, 0);
        m_3dModel.addPoint("VERSOR_Z", 0, 0, 1000.0);
        m_3dModel.addPoint("ARROW_X1", 980.0, 20, 0);
        m_3dModel.addPoint("ARROW_X2", 980.0, -20, 0);
        m_3dModel.addPoint("ARROW_Y1", 20, 980.0, 0);
        m_3dModel.addPoint("ARROW_Y2", -20, 980.0, 0);

        m_3dModel.addSegment("O", "VERSOR_X", Color.RED, 1);
        m_3dModel.addSegment("O", "VERSOR_Y", Color.GREEN, 1);
        m_3dModel.addSegment("O", "VERSOR_Z", Color.BLUE, 1);
        m_3dModel.addSegment("ARROW_X1", "VERSOR_X", Color.RED, 1);
        m_3dModel.addSegment("ARROW_X2", "VERSOR_X", Color.RED, 1);
        m_3dModel.addSegment("ARROW_Y1", "VERSOR_Y", Color.GREEN, 1);
        m_3dModel.addSegment("ARROW_Y2", "VERSOR_Y", Color.GREEN, 1);

        // Define el robot
        m_3dModel.addSegment("CX1A", "CX2A", Color.CYAN, 2);
        m_3dModel.addSegment("CX2A", "CX3A", Color.CYAN, 2);
        m_3dModel.addSegment("CX3A", "CX4A", Color.CYAN, 2);
        m_3dModel.addSegment("CX4A", "CX5A", Color.CYAN, 2);
        m_3dModel.addSegment("CX5A", "CX6A", Color.CYAN, 2);
        m_3dModel.addSegment("CX6A", "CX1A", Color.CYAN, 2);

        m_3dModel.addSegment("CX1B", "CX2B", Color.CYAN, 2);
        m_3dModel.addSegment("CX2B", "CX3B", Color.CYAN, 2);
        m_3dModel.addSegment("CX3B", "CX4B", Color.CYAN, 2);
        m_3dModel.addSegment("CX4B", "CX5B", Color.CYAN, 2);
        m_3dModel.addSegment("CX5B", "CX6B", Color.CYAN, 2);
        m_3dModel.addSegment("CX6B", "CX1B", Color.CYAN, 2);
        for (int l_pata = 1; l_pata <= 6; l_pata++) {
            m_3dModel.addPoint("CX" + l_pata + "A", 0, 0, 0);
            m_3dModel.addPoint("CX" + l_pata + "B", 0, 0, 0);
            m_3dModel.addPoint("FM" + l_pata + "A", 0, 0, 0);
            m_3dModel.addPoint("FM" + l_pata + "B", 0, 0, 0);
            m_3dModel.addPoint("TB" + l_pata + "A", 0, 0, 0);
            m_3dModel.addPoint("TB" + l_pata + "B", 0, 0, 0);
            m_3dModel.addPoint("TB" + l_pata + "B_P", 0, 0, 0);
            m_3dModel.addPoint("TB" + l_pata + "B_Q", 0, 0, 0);
            m_3dModel.addPoint("TB" + l_pata + "B_R", 0, 0, 0);
            m_3dModel.addPoint("TB" + l_pata + "B_S", 0, 0, 0);

            m_3dModel.addPoint("CENTRO" + l_pata + "_C", 0, 0, 0);
            m_3dModel.addPoint("CENTRO" + l_pata + "_F", 0, 0, 0);
            m_3dModel.addPoint("CENTRO" + l_pata + "_B", 0, 0, 0);

            m_3dModel.addSegment("CX" + l_pata + "A",
                    "CX" + l_pata + "B", Color.RED, 4);
            m_3dModel.addSegment("FM" + l_pata + "A",
                    "FM" + l_pata + "B", Color.GREEN, 4);
            m_3dModel.addSegment("TB" + l_pata + "A",
                    "TB" + l_pata + "B", Color.YELLOW, 4);

            m_3dModel.addSegment("TB" + l_pata + "B_P", "TB" + l_pata + "B_Q",
                    Color.ORANGE, 2);
            m_3dModel.addSegment("TB" + l_pata + "B_Q", "TB" + l_pata + "B_R",
                    Color.ORANGE, 2);
            m_3dModel.addSegment("TB" + l_pata + "B_R", "TB" + l_pata + "B_S",
                    Color.ORANGE, 2);
            m_3dModel.addSegment("TB" + l_pata + "B_S", "TB" + l_pata + "B_P",
                    Color.ORANGE, 2);

            m_3dModel.addSegment("CENTRO" + l_pata + "_F", "CENTRO" + l_pata
                    + "_B",
                    Color.WHITE, 1);
            m_3dModel.addSegment("CENTRO" + l_pata + "_C", "CENTRO" + l_pata
                    + "_C",
                    Color.WHITE, 6);

        } // end for

        m_3dModel.addPoint("CENTER_FLOOR", 0, 0, 0);
        m_3dModel.addPoint("BEARING", 0, 0, 0);
        m_3dModel.addPoint("BEARING+", 0, 0, 0);
        m_3dModel.addPoint("BEARING-", 0, 0, 0);
        m_3dModel.addSegment("CENTER_FLOOR", "BEARING", Color.WHITE, 6);
        m_3dModel.addSegment("BEARING+", "BEARING-", Color.WHITE, 1);

        // Activa el componente
        ui_pnlTab3D.add(m_3dView, BorderLayout.CENTER);

        Timer l_timer = new Timer();
        TimerTask l_task = new TimerTask() {
            @Override
            public void run() {
                update3DView();
            }
        };

        l_timer.schedule(l_task, 1000, 200);

    }

    private void update3DView() {

        m_hexapodo.recalculaVertices();

        // Actualiza la altura de la grilla
        double l_height = -m_hexapodo.getHeight();
        for (Point3D l_point : m_3dModel.getPoints()) {
            if (l_point.getId().startsWith("GR")) {
                l_point.setZ(l_height);
            } // end if
        } // end for

        // Actualiza el rumbo
        m_3dModel.getPoint("CENTER_FLOOR").updateFrom(0, 0, -m_hexapodo.
                getHeight());
        if (m_hexapodo.getSpeed() == 0) {
            m_3dModel.getPoint("BEARING").updateFrom(0, 0, -m_hexapodo.
                    getHeight());
            m_3dModel.getPoint("BEARING+").updateFrom(0, 0, -m_hexapodo.
                    getHeight());
            m_3dModel.getPoint("BEARING-").updateFrom(0, 0, -m_hexapodo.
                    getHeight());
        } else {

            m_3dModel.getPoint("BEARING").updateFrom(m_hexapodo.getBearingX(),
                    m_hexapodo.getBearingY(), -m_hexapodo.getHeight());
            m_3dModel.getPoint("BEARING+").updateFrom(m_hexapodo.getBearingX()
                    * 10,
                    m_hexapodo.getBearingY() * 10, -m_hexapodo.getHeight());
            m_3dModel.getPoint("BEARING-").updateFrom(m_hexapodo.getBearingX()
                    * -10,
                    m_hexapodo.getBearingY() * -10, -m_hexapodo.getHeight());
        } // end if

        // Actualiza cuerpo
        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            Pata l_pata = m_hexapodo.getPata(l_nroPata);
            m_3dModel.getPoint("CX" + l_nroPata + "A").updateFrom(l_pata.
                    getCoxaA());
            m_3dModel.getPoint("CX" + l_nroPata + "B").updateFrom(l_pata.
                    getCoxaB());

            m_3dModel.getPoint("FM" + l_nroPata + "A").updateFrom(l_pata.
                    getFemurA());
            m_3dModel.getPoint("FM" + l_nroPata + "B").updateFrom(l_pata.
                    getFemurB());

            m_3dModel.getPoint("TB" + l_nroPata + "A").updateFrom(l_pata.
                    getTibiaA());
            m_3dModel.getPoint("TB" + l_nroPata + "B").updateFrom(l_pata.
                    getTibiaB());

            // Actualiza las pisadas
            int l_rad;
            if (Math.abs(l_pata.getTibiaB().getZ() + m_hexapodo.getHeight()) < 1) {
                l_rad = 30;
            } else {
                l_rad = 4;
            } // end if

            Point3D l_tibiaB = l_pata.getTibiaB();
            Point3D l_tibiaB_P = new Point3D("TB" + l_nroPata + "B_P",
                    l_tibiaB.getX() + l_rad, l_tibiaB.getY() + l_rad, l_tibiaB.
                    getZ());
            Point3D l_tibiaB_Q = new Point3D("TB" + l_nroPata + "B_Q",
                    l_tibiaB.getX() - l_rad, l_tibiaB.getY() + l_rad, l_tibiaB.
                    getZ());
            Point3D l_tibiaB_R = new Point3D("TB" + l_nroPata + "B_R",
                    l_tibiaB.getX() - l_rad, l_tibiaB.getY() - l_rad, l_tibiaB.
                    getZ());
            Point3D l_tibiaB_S = new Point3D("TB" + l_nroPata + "B_S",
                    l_tibiaB.getX() + l_rad, l_tibiaB.getY() - l_rad, l_tibiaB.
                    getZ());

            m_3dModel.getPoint(l_tibiaB_P.getId()).updateFrom(l_tibiaB_P);
            m_3dModel.getPoint(l_tibiaB_Q.getId()).updateFrom(l_tibiaB_Q);
            m_3dModel.getPoint(l_tibiaB_R.getId()).updateFrom(l_tibiaB_R);
            m_3dModel.getPoint(l_tibiaB_S.getId()).updateFrom(l_tibiaB_S);

            Point3D l_centro = l_pata.getCentroPaso();
            if (l_centro != null) {
                m_3dModel.getPoint("CENTRO" + l_nroPata + "_C").updateFrom(
                        l_centro);
                m_3dModel.getPoint("CENTRO" + l_nroPata + "_F").updateFrom(
                        l_centro.getX() + l_pata.getRumboX(),
                        l_centro.getY() + l_pata.getRumboY(),
                        l_centro.getZ());
                m_3dModel.getPoint("CENTRO" + l_nroPata + "_B").updateFrom(
                        l_centro.getX() - l_pata.getRumboX(),
                        l_centro.getY() - l_pata.getRumboY(),
                        l_centro.getZ());

            } else {
                m_3dModel.getPoint("CENTRO" + l_nroPata + "_C").updateFrom(0, 0,
                        0);
                m_3dModel.getPoint("CENTRO" + l_nroPata + "_F").updateFrom(0, 0,
                        0);
                m_3dModel.getPoint("CENTRO" + l_nroPata + "_B").updateFrom(0, 0,
                        0);
            } // end if 

        } // end for

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_3dView.repaint();
            }
        });
    }

    private void trace(final JTextArea p_area, final String p_msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String l_txt = p_area.getText();
                l_txt = l_txt + "\n" + p_msg;
                if (l_txt.length() > 1000) {
                    l_txt = l_txt.substring(l_txt.length() - 1000);
                } // end if
                p_area.setText(l_txt);
                p_area.setCaretPosition(l_txt.length());

            }
        });
    }

    private void trace(final JTextArea p_area, final Throwable p_ex) {
        if (SwingUtilities.isEventDispatchThread()) {
            trace(p_area, "ERR: " + p_ex.getMessage());
            StackTraceElement[] l_elements = p_ex.getStackTrace();
            for (StackTraceElement l_element : l_elements) {
                String l_className = l_element.getClassName();
                if (l_className.startsWith("ar.edu.unsta")) {
                    trace(p_area, "\t" + l_element.getMethodName() + " ("
                            + l_className + ":" + l_element.getLineNumber()
                            + ")"
                    );
                } // end if
            } // end for
            return;
        } // end if

        // Fuera del EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                trace(p_area, p_ex);
            }
        });
    }

    @Override
    public void trace(final String p_msg) {
        trace(ui_txtGlobalTrace, p_msg);
    }

    @Override
    public void trace(Throwable p_ex) {
        trace(ui_txtGlobalTrace, p_ex);
    }

    private void updateIndicators() {
        ui_lblSpeed.setText(String.valueOf(m_hexapodo.getSpeed()) + "%");
        ui_lblBearing.setText(String.valueOf(m_hexapodo.getBearing()) + "º");
        ui_lblHeight.setText(m_hexapodo.getHeight() + "mm");

    }

    private void updateValues() {
        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            Pata l_pata = m_hexapodo.getPata(l_nroPata);
            String l_estado = l_pata.toString();

            JTextArea l_area = (JTextArea) ui_pnlValues.getComponent(l_nroPata
                    - 1);
            l_area.setText(l_estado);
        } // end for
    }

    private void updateGraphics() {

        for (int l_nroPata = 1; l_nroPata <= 6; l_nroPata++) {
            Pata l_pata = m_hexapodo.getPata(l_nroPata);
            JVistaPata l_vista = (JVistaPata) ui_pnlGraphics.getComponent(
                    l_nroPata - 1);

            l_vista.setAngulos(l_pata.getAngCoxa(), l_pata.getAngFemur(),
                    l_pata.getAngTibia());
            l_vista.repaint();
        } // end for

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ui_grpProjection = new javax.swing.ButtonGroup();
        ui_grpAlgorithm = new javax.swing.ButtonGroup();
        ui_pnlSplit = new javax.swing.JSplitPane();
        ui_pnlLeft = new javax.swing.JPanel();
        ui_pnlLeftNorth = new javax.swing.JPanel();
        ui_cmdStop = new javax.swing.JButton();
        ui_cmdDiscover = new javax.swing.JButton();
        ui_cmdConfigure = new javax.swing.JButton();
        ui_cmdSit = new javax.swing.JButton();
        ui_cmdUp = new javax.swing.JButton();
        ui_cmdTurnLeft = new javax.swing.JButton();
        ui_cmdTurnRight = new javax.swing.JButton();
        ui_cmdClose = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        ui_chkSendCommands = new javax.swing.JCheckBox();
        ui_rdbOrthogonal = new javax.swing.JRadioButton();
        ui_rdbElliptical = new javax.swing.JRadioButton();
        ui_pnlLeftEast = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        ui_sliHeight = new javax.swing.JSlider();
        jPanel1 = new javax.swing.JPanel();
        ui_chkAutoStep = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        ui_lblPStep = new javax.swing.JLabel();
        ui_sldP = new javax.swing.JSlider();
        ui_chk135Fwd = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        ui_sldStep = new javax.swing.JSlider();
        jLabel3 = new javax.swing.JLabel();
        ui_sldClearance = new javax.swing.JSlider();
        ui_pnlLeftSouth = new javax.swing.JPanel();
        ui_lblBearing = new javax.swing.JLabel();
        ui_lblSpeed = new javax.swing.JLabel();
        ui_lblHeight = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        ui_pnlRight = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        ui_pnlValues = new javax.swing.JPanel();
        ui_txtStatus1 = new javax.swing.JTextArea();
        ui_txtStatus2 = new javax.swing.JTextArea();
        ui_txtStatus3 = new javax.swing.JTextArea();
        ui_txtStatus4 = new javax.swing.JTextArea();
        ui_txtStatus5 = new javax.swing.JTextArea();
        ui_txtStatus6 = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        ui_pnlGraphics = new javax.swing.JPanel();
        ui_pnlTab3D = new javax.swing.JPanel();
        ui_pnlProjections = new javax.swing.JPanel();
        ui_tglXY = new javax.swing.JToggleButton();
        ui_tglXZ = new javax.swing.JToggleButton();
        ui_tglOblique = new javax.swing.JToggleButton();
        ui_tglIsometric = new javax.swing.JToggleButton();
        ui_pnlTabTrace = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        ui_btnClearTrace = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        ui_txtGlobalTrace = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        ui_txtTrace1 = new javax.swing.JTextArea();
        ui_txtSend1 = new javax.swing.JTextField();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        ui_txtTrace2 = new javax.swing.JTextArea();
        ui_txtSend2 = new javax.swing.JTextField();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        ui_txtTrace3 = new javax.swing.JTextArea();
        ui_txtSend3 = new javax.swing.JTextField();
        ui_pnlNorth = new javax.swing.JPanel();
        ui_lblLogo = new javax.swing.JLabel();
        ui_lblTitle = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        ui_pnlSplit.setBackground(new java.awt.Color(64, 64, 64));
        ui_pnlSplit.setDividerLocation(200);
        ui_pnlSplit.setDividerSize(20);
        ui_pnlSplit.setResizeWeight(0.5);

        ui_pnlLeft.setBackground(new java.awt.Color(64, 64, 64));
        ui_pnlLeft.setOpaque(false);
        ui_pnlLeft.setLayout(new java.awt.BorderLayout(4, 4));

        ui_pnlLeftNorth.setBackground(new java.awt.Color(64, 64, 64));
        ui_pnlLeftNorth.setLayout(new java.awt.GridLayout(4, 3, 4, 4));

        ui_cmdStop.setBackground(new java.awt.Color(255, 0, 0));
        ui_cmdStop.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdStop.setForeground(new java.awt.Color(255, 255, 255));
        ui_cmdStop.setText("STOP");
        ui_cmdStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdStopActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdStop);

        ui_cmdDiscover.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdDiscover.setText("Discover");
        ui_cmdDiscover.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdDiscoverActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdDiscover);

        ui_cmdConfigure.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdConfigure.setText("Configure");
        ui_cmdConfigure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdConfigureActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdConfigure);

        ui_cmdSit.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdSit.setText("Sit");
        ui_cmdSit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdSitActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdSit);

        ui_cmdUp.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdUp.setText("Up");
        ui_cmdUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdUpActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdUp);

        ui_cmdTurnLeft.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdTurnLeft.setText("Left");
        ui_cmdTurnLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdTurnLeftActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdTurnLeft);

        ui_cmdTurnRight.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdTurnRight.setText("Right");
        ui_cmdTurnRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdTurnRightActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdTurnRight);

        ui_cmdClose.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_cmdClose.setText("Close");
        ui_cmdClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdCloseActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_cmdClose);

        jButton1.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jButton1.setText("?");
        ui_pnlLeftNorth.add(jButton1);

        ui_chkSendCommands.setForeground(new java.awt.Color(153, 255, 255));
        ui_chkSendCommands.setText("Send Cmd");
        ui_chkSendCommands.setOpaque(false);
        ui_chkSendCommands.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_chkSendCommandsStateChanged(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_chkSendCommands);

        ui_grpAlgorithm.add(ui_rdbOrthogonal);
        ui_rdbOrthogonal.setForeground(new java.awt.Color(153, 255, 255));
        ui_rdbOrthogonal.setSelected(true);
        ui_rdbOrthogonal.setText("Ortho");
        ui_rdbOrthogonal.setOpaque(false);
        ui_rdbOrthogonal.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_rdbOrthogonalStateChanged(evt);
            }
        });
        ui_rdbOrthogonal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_rdbOrthogonalActionPerformed(evt);
            }
        });
        ui_pnlLeftNorth.add(ui_rdbOrthogonal);

        ui_grpAlgorithm.add(ui_rdbElliptical);
        ui_rdbElliptical.setForeground(new java.awt.Color(153, 255, 255));
        ui_rdbElliptical.setText("Elliptical");
        ui_rdbElliptical.setOpaque(false);
        ui_pnlLeftNorth.add(ui_rdbElliptical);

        ui_pnlLeft.add(ui_pnlLeftNorth, java.awt.BorderLayout.NORTH);

        ui_pnlLeftEast.setBackground(new java.awt.Color(64, 64, 64));
        ui_pnlLeftEast.setLayout(new javax.swing.BoxLayout(ui_pnlLeftEast, javax.swing.BoxLayout.Y_AXIS));

        jLabel1.setForeground(new java.awt.Color(153, 255, 255));
        jLabel1.setText("HEIGHT ");
        jLabel1.setAlignmentX(1.0F);
        jLabel1.setAlignmentY(0.0F);
        ui_pnlLeftEast.add(jLabel1);

        ui_sliHeight.setMaximum(700);
        ui_sliHeight.setMinimum(250);
        ui_sliHeight.setOrientation(javax.swing.JSlider.VERTICAL);
        ui_sliHeight.setPaintLabels(true);
        ui_sliHeight.setPaintTicks(true);
        ui_sliHeight.setValue(410);
        ui_sliHeight.setAlignmentX(1.0F);
        ui_sliHeight.setAlignmentY(0.0F);
        ui_sliHeight.setOpaque(false);
        ui_sliHeight.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_sliHeightStateChanged(evt);
            }
        });
        ui_pnlLeftEast.add(ui_sliHeight);

        ui_pnlLeft.add(ui_pnlLeftEast, java.awt.BorderLayout.EAST);

        jPanel1.setAlignmentX(0.0F);
        jPanel1.setAlignmentY(0.0F);
        jPanel1.setOpaque(false);
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        ui_chkAutoStep.setForeground(new java.awt.Color(153, 255, 255));
        ui_chkAutoStep.setSelected(true);
        ui_chkAutoStep.setText("Auto-step");
        ui_chkAutoStep.setOpaque(false);
        ui_chkAutoStep.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_chkAutoStepStateChanged(evt);
            }
        });
        jPanel1.add(ui_chkAutoStep);

        jPanel6.setAlignmentX(0.0F);
        jPanel6.setAlignmentY(0.0F);
        jPanel6.setOpaque(false);
        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        ui_lblPStep.setForeground(new java.awt.Color(153, 255, 255));
        ui_lblPStep.setText("p=0.50");
        ui_lblPStep.setAlignmentY(0.0F);
        jPanel6.add(ui_lblPStep);

        ui_sldP.setAlignmentX(0.0F);
        ui_sldP.setAlignmentY(0.0F);
        ui_sldP.setOpaque(false);
        ui_sldP.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_sldPStateChanged(evt);
            }
        });
        jPanel6.add(ui_sldP);

        jPanel1.add(jPanel6);

        ui_chk135Fwd.setForeground(new java.awt.Color(153, 255, 255));
        ui_chk135Fwd.setText("\"135\" forward, \"246\" backward");
        ui_chk135Fwd.setAlignmentY(0.0F);
        ui_chk135Fwd.setOpaque(false);
        ui_chk135Fwd.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_chk135FwdStateChanged(evt);
            }
        });
        ui_chk135Fwd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_chk135FwdActionPerformed(evt);
            }
        });
        jPanel1.add(ui_chk135Fwd);

        jPanel3.setAlignmentX(0.0F);
        jPanel3.setAlignmentY(0.0F);
        jPanel3.setOpaque(false);
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        jLabel2.setForeground(new java.awt.Color(153, 255, 255));
        jLabel2.setText("STEP:");
        jPanel3.add(jLabel2);

        ui_sldStep.setMaximum(400);
        ui_sldStep.setMinimum(50);
        ui_sldStep.setPaintTicks(true);
        ui_sldStep.setValue(200);
        ui_sldStep.setOpaque(false);
        ui_sldStep.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_sldStepStateChanged(evt);
            }
        });
        jPanel3.add(ui_sldStep);

        jLabel3.setForeground(new java.awt.Color(153, 255, 255));
        jLabel3.setText("CLEARANCE:");
        jPanel3.add(jLabel3);

        ui_sldClearance.setMaximum(250);
        ui_sldClearance.setMinimum(50);
        ui_sldClearance.setPaintTicks(true);
        ui_sldClearance.setValue(100);
        ui_sldClearance.setOpaque(false);
        ui_sldClearance.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ui_sldClearanceStateChanged(evt);
            }
        });
        jPanel3.add(ui_sldClearance);

        jPanel1.add(jPanel3);

        ui_pnlLeftSouth.setBackground(new java.awt.Color(64, 64, 64));
        ui_pnlLeftSouth.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        ui_pnlLeftSouth.setForeground(new java.awt.Color(153, 255, 255));
        ui_pnlLeftSouth.setAlignmentX(0.0F);
        ui_pnlLeftSouth.setAlignmentY(0.0F);
        ui_pnlLeftSouth.setLayout(new java.awt.GridLayout(2, 3, 4, 0));

        ui_lblBearing.setFont(new java.awt.Font("Arial Black", 1, 24)); // NOI18N
        ui_lblBearing.setForeground(new java.awt.Color(153, 255, 255));
        ui_lblBearing.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ui_lblBearing.setText("0º");
        ui_lblBearing.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 153)));
        ui_pnlLeftSouth.add(ui_lblBearing);

        ui_lblSpeed.setFont(new java.awt.Font("Arial Black", 1, 24)); // NOI18N
        ui_lblSpeed.setForeground(new java.awt.Color(153, 255, 255));
        ui_lblSpeed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ui_lblSpeed.setText("0%");
        ui_lblSpeed.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 153)));
        ui_pnlLeftSouth.add(ui_lblSpeed);

        ui_lblHeight.setFont(new java.awt.Font("Arial Black", 1, 24)); // NOI18N
        ui_lblHeight.setForeground(new java.awt.Color(153, 255, 255));
        ui_lblHeight.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ui_lblHeight.setText("0mm");
        ui_lblHeight.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 153)));
        ui_pnlLeftSouth.add(ui_lblHeight);

        jLabel4.setForeground(new java.awt.Color(153, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("BEARING");
        ui_pnlLeftSouth.add(jLabel4);

        jLabel5.setForeground(new java.awt.Color(153, 255, 255));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("SPEED");
        ui_pnlLeftSouth.add(jLabel5);

        jLabel6.setForeground(new java.awt.Color(153, 255, 255));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("HEIGHT");
        ui_pnlLeftSouth.add(jLabel6);

        jPanel1.add(ui_pnlLeftSouth);

        ui_pnlLeft.add(jPanel1, java.awt.BorderLayout.SOUTH);

        ui_pnlSplit.setLeftComponent(ui_pnlLeft);

        ui_pnlRight.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ui_pnlRight.setMaximumSize(new java.awt.Dimension(100, 100));
        ui_pnlRight.setOpaque(true);
        ui_pnlRight.setPreferredSize(new java.awt.Dimension(100, 100));

        ui_pnlValues.setBackground(new java.awt.Color(153, 153, 153));
        ui_pnlValues.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ui_pnlValues.setLayout(new java.awt.GridLayout(3, 2, 10, 10));

        ui_txtStatus1.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtStatus1.setColumns(10);
        ui_txtStatus1.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtStatus1.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtStatus1.setLineWrap(true);
        ui_txtStatus1.setRows(5);
        ui_txtStatus1.setText("STATUS: 1");
        ui_pnlValues.add(ui_txtStatus1);

        ui_txtStatus2.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtStatus2.setColumns(10);
        ui_txtStatus2.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtStatus2.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtStatus2.setLineWrap(true);
        ui_txtStatus2.setRows(5);
        ui_txtStatus2.setText("STATUS: 1");
        ui_pnlValues.add(ui_txtStatus2);

        ui_txtStatus3.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtStatus3.setColumns(10);
        ui_txtStatus3.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtStatus3.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtStatus3.setLineWrap(true);
        ui_txtStatus3.setRows(5);
        ui_txtStatus3.setText("STATUS: 1");
        ui_pnlValues.add(ui_txtStatus3);

        ui_txtStatus4.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtStatus4.setColumns(10);
        ui_txtStatus4.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtStatus4.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtStatus4.setLineWrap(true);
        ui_txtStatus4.setRows(5);
        ui_txtStatus4.setText("STATUS: 1");
        ui_pnlValues.add(ui_txtStatus4);

        ui_txtStatus5.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtStatus5.setColumns(10);
        ui_txtStatus5.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtStatus5.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtStatus5.setLineWrap(true);
        ui_txtStatus5.setRows(5);
        ui_txtStatus5.setText("STATUS: 1");
        ui_pnlValues.add(ui_txtStatus5);

        ui_txtStatus6.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtStatus6.setColumns(10);
        ui_txtStatus6.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtStatus6.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtStatus6.setLineWrap(true);
        ui_txtStatus6.setRows(5);
        ui_txtStatus6.setText("STATUS: 1");
        ui_pnlValues.add(ui_txtStatus6);

        jScrollPane1.setViewportView(ui_pnlValues);

        ui_pnlRight.addTab("VALUES", jScrollPane1);

        ui_pnlGraphics.setBackground(new java.awt.Color(153, 153, 153));
        ui_pnlGraphics.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        ui_pnlGraphics.setLayout(new java.awt.GridLayout(3, 2, 20, 20));
        jScrollPane2.setViewportView(ui_pnlGraphics);

        ui_pnlRight.addTab("GRAPHICS", jScrollPane2);

        ui_pnlTab3D.setBackground(new java.awt.Color(153, 153, 153));
        ui_pnlTab3D.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                ui_pnlTab3DComponentResized(evt);
            }
        });
        ui_pnlTab3D.setLayout(new java.awt.BorderLayout());

        ui_pnlProjections.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        ui_grpProjection.add(ui_tglXY);
        ui_tglXY.setText("Top");
        ui_tglXY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_tglXYActionPerformed(evt);
            }
        });
        ui_pnlProjections.add(ui_tglXY);

        ui_grpProjection.add(ui_tglXZ);
        ui_tglXZ.setText("Side");
        ui_tglXZ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_tglXZActionPerformed(evt);
            }
        });
        ui_pnlProjections.add(ui_tglXZ);

        ui_grpProjection.add(ui_tglOblique);
        ui_tglOblique.setText("Oblique");
        ui_tglOblique.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_tglObliqueActionPerformed(evt);
            }
        });
        ui_pnlProjections.add(ui_tglOblique);

        ui_grpProjection.add(ui_tglIsometric);
        ui_tglIsometric.setSelected(true);
        ui_tglIsometric.setText("Isometric");
        ui_tglIsometric.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_tglIsometricActionPerformed(evt);
            }
        });
        ui_pnlProjections.add(ui_tglIsometric);

        ui_pnlTab3D.add(ui_pnlProjections, java.awt.BorderLayout.NORTH);

        ui_pnlRight.addTab("3D", ui_pnlTab3D);

        ui_pnlTabTrace.setBackground(new java.awt.Color(153, 153, 153));
        ui_pnlTabTrace.setLayout(new java.awt.GridLayout(2, 1, 4, 4));

        jPanel4.setBackground(new java.awt.Color(153, 153, 153));
        jPanel4.setLayout(new java.awt.BorderLayout(0, 5));

        jPanel5.setBackground(new java.awt.Color(153, 153, 153));
        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.X_AXIS));

        ui_btnClearTrace.setText("Clear trace");
        ui_btnClearTrace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_btnClearTraceActionPerformed(evt);
            }
        });
        jPanel5.add(ui_btnClearTrace);

        jPanel4.add(jPanel5, java.awt.BorderLayout.NORTH);

        jScrollPane6.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane6.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        ui_txtGlobalTrace.setEditable(false);
        ui_txtGlobalTrace.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtGlobalTrace.setColumns(20);
        ui_txtGlobalTrace.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtGlobalTrace.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtGlobalTrace.setRows(5);
        ui_txtGlobalTrace.setText("???");
        ui_txtGlobalTrace.setToolTipText("");
        jScrollPane6.setViewportView(ui_txtGlobalTrace);

        jPanel4.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        ui_pnlTabTrace.add(jPanel4);

        jPanel2.setLayout(new java.awt.GridLayout(1, 3, 4, 4));

        jPanel7.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        ui_txtTrace1.setEditable(false);
        ui_txtTrace1.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtTrace1.setColumns(20);
        ui_txtTrace1.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtTrace1.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtTrace1.setLineWrap(true);
        ui_txtTrace1.setRows(5);
        ui_txtTrace1.setText("???");
        jScrollPane3.setViewportView(ui_txtTrace1);

        jPanel7.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        ui_txtSend1.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtSend1.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtSend1.setForeground(new java.awt.Color(255, 51, 51));
        ui_txtSend1.setText("Q");
        ui_txtSend1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_txtSend1ActionPerformed(evt);
            }
        });
        jPanel7.add(ui_txtSend1, java.awt.BorderLayout.PAGE_START);

        jPanel2.add(jPanel7);

        jPanel8.setLayout(new java.awt.BorderLayout());

        jScrollPane4.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane4.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        ui_txtTrace2.setEditable(false);
        ui_txtTrace2.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtTrace2.setColumns(20);
        ui_txtTrace2.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtTrace2.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtTrace2.setLineWrap(true);
        ui_txtTrace2.setRows(5);
        ui_txtTrace2.setText("???");
        jScrollPane4.setViewportView(ui_txtTrace2);

        jPanel8.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        ui_txtSend2.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtSend2.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtSend2.setForeground(new java.awt.Color(255, 51, 51));
        ui_txtSend2.setText("Q");
        ui_txtSend2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_txtSend2ActionPerformed(evt);
            }
        });
        jPanel8.add(ui_txtSend2, java.awt.BorderLayout.PAGE_START);

        jPanel2.add(jPanel8);

        jPanel9.setLayout(new java.awt.BorderLayout());

        jScrollPane5.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane5.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        ui_txtTrace3.setEditable(false);
        ui_txtTrace3.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtTrace3.setColumns(20);
        ui_txtTrace3.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtTrace3.setForeground(new java.awt.Color(0, 255, 51));
        ui_txtTrace3.setLineWrap(true);
        ui_txtTrace3.setRows(5);
        ui_txtTrace3.setText("???");
        jScrollPane5.setViewportView(ui_txtTrace3);

        jPanel9.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        ui_txtSend3.setBackground(new java.awt.Color(64, 64, 64));
        ui_txtSend3.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        ui_txtSend3.setForeground(new java.awt.Color(255, 51, 51));
        ui_txtSend3.setText("Q");
        ui_txtSend3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_txtSend3ActionPerformed(evt);
            }
        });
        jPanel9.add(ui_txtSend3, java.awt.BorderLayout.PAGE_START);

        jPanel2.add(jPanel9);

        ui_pnlTabTrace.add(jPanel2);

        ui_pnlRight.addTab("TRACE", ui_pnlTabTrace);

        ui_pnlSplit.setRightComponent(ui_pnlRight);

        getContentPane().add(ui_pnlSplit, java.awt.BorderLayout.CENTER);

        ui_pnlNorth.setBackground(new java.awt.Color(64, 64, 64));
        ui_pnlNorth.setForeground(new java.awt.Color(153, 255, 255));
        ui_pnlNorth.setLayout(new java.awt.BorderLayout());

        ui_lblLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ar/edu/unsta/robotteam/hexabot/view/logo_64.png"))); // NOI18N
        ui_lblLogo.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ui_pnlNorth.add(ui_lblLogo, java.awt.BorderLayout.WEST);

        ui_lblTitle.setFont(new java.awt.Font("Arial", 1, 36)); // NOI18N
        ui_lblTitle.setForeground(new java.awt.Color(51, 255, 255));
        ui_lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ui_lblTitle.setText("HEXABOT - Control Panel");
        ui_pnlNorth.add(ui_lblTitle, java.awt.BorderLayout.CENTER);

        jLabel7.setForeground(new java.awt.Color(204, 204, 204));
        jLabel7.setText("<html>Version<p/>2017-03-14</html>");
        ui_pnlNorth.add(jLabel7, java.awt.BorderLayout.LINE_END);

        getContentPane().add(ui_pnlNorth, java.awt.BorderLayout.NORTH);

        setSize(new java.awt.Dimension(1180, 766));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void ui_sliHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_sliHeightStateChanged

        m_hexapodo.setHeight(ui_sliHeight.getValue());
        ui_lblHeight.setText(m_hexapodo.getHeight() + "mm");

    }//GEN-LAST:event_ui_sliHeightStateChanged

    private void doStop() {
        m_hexapodo.cmdStop(null, null);
        m_joystick.toCenter();
        updateIndicators();
        repaint();
    }

    private void ui_cmdStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdStopActionPerformed
        doStop();
    }//GEN-LAST:event_ui_cmdStopActionPerformed

    private void ui_cmdSitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdSitActionPerformed
        doSit();
    }//GEN-LAST:event_ui_cmdSitActionPerformed

    private void ui_cmdTurnLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdTurnLeftActionPerformed
        m_hexapodo.cmdTurnLeft(null);
    }//GEN-LAST:event_ui_cmdTurnLeftActionPerformed

    private void ui_cmdTurnRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdTurnRightActionPerformed
        m_hexapodo.cmdTurnRight(null);
    }//GEN-LAST:event_ui_cmdTurnRightActionPerformed

    private void ui_cmdDiscoverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdDiscoverActionPerformed
        doDiscover();
    }//GEN-LAST:event_ui_cmdDiscoverActionPerformed

    private void ui_cmdCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdCloseActionPerformed
        doClose();
    }//GEN-LAST:event_ui_cmdCloseActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        doClose();
    }//GEN-LAST:event_formWindowClosed

    private void ui_cmdConfigureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdConfigureActionPerformed
        doConfigure();
    }//GEN-LAST:event_ui_cmdConfigureActionPerformed

    private void ui_btnClearTraceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_btnClearTraceActionPerformed
        ui_txtGlobalTrace.setText("");
        ui_txtTrace1.setText("");
        ui_txtTrace2.setText("");
        ui_txtTrace3.setText("");
    }//GEN-LAST:event_ui_btnClearTraceActionPerformed

    private void ui_pnlTab3DComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_ui_pnlTab3DComponentResized

        Point l_newOrigin = new Point(m_3dView.getWidth() / 2, m_3dView.
                getHeight() / 2);

        m_3dView.setOrigin(l_newOrigin);

        double l_newScale = m_3dView.getWidth() / 5000.0;
        m_3dView.setScale(l_newScale);
    }//GEN-LAST:event_ui_pnlTab3DComponentResized

    private void ui_tglXYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_tglXYActionPerformed

        new XYProjector(m_3dView);
        m_3dView.repaint();
    }//GEN-LAST:event_ui_tglXYActionPerformed

    private void ui_tglXZActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_tglXZActionPerformed

        new XZProjector(m_3dView);
        m_3dView.repaint();
    }//GEN-LAST:event_ui_tglXZActionPerformed

    private void ui_tglObliqueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_tglObliqueActionPerformed

        new ObliqueProjector(m_3dView);
        m_3dView.repaint();
    }//GEN-LAST:event_ui_tglObliqueActionPerformed

    private void ui_tglIsometricActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_tglIsometricActionPerformed

        new IsometricProjector(m_3dView);
        m_3dView.repaint();
    }//GEN-LAST:event_ui_tglIsometricActionPerformed

    private void ui_cmdUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdUpActionPerformed
        doStop();
        doUp();

    }//GEN-LAST:event_ui_cmdUpActionPerformed

    private void ui_chkSendCommandsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_chkSendCommandsStateChanged

        m_hexapodo.setSendCommands(ui_chkSendCommands.isSelected());

    }//GEN-LAST:event_ui_chkSendCommandsStateChanged

    private void ui_sldStepStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_sldStepStateChanged

        m_hexapodo.setHalfStep(ui_sldStep.getValue() / 2.0);

    }//GEN-LAST:event_ui_sldStepStateChanged

    private void ui_sldClearanceStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_sldClearanceStateChanged

        m_hexapodo.setClearance(ui_sldClearance.getValue());

    }//GEN-LAST:event_ui_sldClearanceStateChanged

    private void ui_rdbOrthogonalStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_rdbOrthogonalStateChanged

        if (ui_rdbOrthogonal.isSelected()) {
            m_hexapodo.setStepMode(Hexapodo.STEP_MODE.ORTHOGONAL);
        } else {
            m_hexapodo.setStepMode(Hexapodo.STEP_MODE.ELLIPTICAL);
        } // end if

    }//GEN-LAST:event_ui_rdbOrthogonalStateChanged

    private void ui_rdbOrthogonalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_rdbOrthogonalActionPerformed

    }//GEN-LAST:event_ui_rdbOrthogonalActionPerformed

    private void ui_chkAutoStepStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_chkAutoStepStateChanged

        m_hexapodo.setAutoStep(ui_chkAutoStep.isSelected());

    }//GEN-LAST:event_ui_chkAutoStepStateChanged

    private void ui_sldPStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_sldPStateChanged

        doChangeP();

    }//GEN-LAST:event_ui_sldPStateChanged

    private void ui_chk135FwdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ui_chk135FwdStateChanged

    }//GEN-LAST:event_ui_chk135FwdStateChanged

    private void ui_chk135FwdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_chk135FwdActionPerformed
        doChangeLegSet();
    }//GEN-LAST:event_ui_chk135FwdActionPerformed

    private void ui_txtSend1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_txtSend1ActionPerformed
        doSend(1, ui_txtSend1.getText());
    }//GEN-LAST:event_ui_txtSend1ActionPerformed

    private void ui_txtSend2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_txtSend2ActionPerformed
        doSend(2, ui_txtSend2.getText());
    }//GEN-LAST:event_ui_txtSend2ActionPerformed

    private void ui_txtSend3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_txtSend3ActionPerformed
        doSend(3, ui_txtSend3.getText());
    }//GEN-LAST:event_ui_txtSend3ActionPerformed

    private void doSend(final int p_shield, final String p_toSend) {

        new Thread() {
            @Override
            public void run() {
                try {
                    m_hexapodo.doSend(p_shield, p_toSend);
                } catch (Exception l_ex) {
                    trace(l_ex);
                }
            }
        }.start();

    }

    private void doChangeLegSet() {

        m_hexapodo.setpStep(1.0 - m_hexapodo.getpStep());
        ui_sldP.setValue(100 - ui_sldP.getValue());

        if (m_hexapodo.m_wThread.getLegsToForward().equals("135")) {
            m_hexapodo.m_wThread.setLegsToForward("246");
            m_hexapodo.m_wThread.setLegsToBackward("135");
        } else {
            m_hexapodo.m_wThread.setLegsToForward("135");
            m_hexapodo.m_wThread.setLegsToBackward("246");
        } // end if

    }

    private void doChangeP() {

        double l_p = ui_sldP.getValue() / 100.0;

        m_hexapodo.setpStep(l_p);
        String l_leyenda = String.format("p=%.2f", l_p);
        ui_lblPStep.setText(l_leyenda);
    }

    private void doUp() {
        ui_cmdUp.setEnabled(false);
        trace("UP");
        new Thread() {
            @Override
            public void run() {
                try {
                    m_hexapodo.cmdUp(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent p_e) {
                            // TODO
                        }
                    });
                } catch (Exception l_ex) {
                    trace(l_ex);
                } finally {
                    ui_cmdUp.setEnabled(true);
                }
            }
        }.start();
    }

    private void doConfigure() {
        ui_cmdConfigure.setEnabled(false);
        trace("CONFIGURE");
        new Thread() {
            @Override
            public void run() {
                try {
                    m_hexapodo.doConfigure();
                } catch (Exception l_ex) {
                    trace(l_ex);
                } finally {
                    ui_cmdConfigure.setEnabled(true);
                }
            }
        }.start();
    }

    private void doSit() {
        ui_cmdSit.setEnabled(false);
        trace("SIT");
        new Thread() {
            @Override
            public void run() {

                try {
                    m_hexapodo.cmdSit(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent p_e) {
                            // TODO
                        }
                    });
                } catch (Exception l_ex) {
                    trace(l_ex);
                } finally {
                    ui_cmdSit.setEnabled(true);
                }
            }

        }.start();
    }

    private void doClose() {
        trace("CLOSE");
        new Thread() {
            @Override
            public void run() {
                try {
                    m_hexapodo.doClose();
                } catch (Exception l_ex) {
                    trace(l_ex);
                }
            }
        }.start();
    }

    private void doDiscover() {
        ui_cmdDiscover.setEnabled(false);
        trace("DISCOVER");
        new Thread() {
            @Override
            public void run() {
                try {
                    m_hexapodo.doDiscover();
                } catch (Exception l_ex) {
                    trace(l_ex);
                } finally {
                    ui_cmdDiscover.setEnabled(true);
                }
            }
        }.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {

        // Dirección del simulador Webots, por ejemplo tcp://127.0.0.1:5000.
        // Sin argumento se usan los Arduinos por puerto serie, como antes
        final String l_direccionTcp = args.length > 0
                && ClientServerPort.isTcp(args[0]) ? args[0] : null;

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                PanelControl l_panel = new PanelControl();
                if (l_direccionTcp != null) {
                    l_panel.m_hexapodo.setDireccionTcp(l_direccionTcp);
                    l_panel.setTitle(l_panel.getTitle() + " - Webots "
                            + l_direccionTcp);
                    l_panel.trace("Simulador: " + l_direccionTcp
                            + " (marcha de coxas)");
                } // end if
                l_panel.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JButton ui_btnClearTrace;
    private javax.swing.JCheckBox ui_chk135Fwd;
    private javax.swing.JCheckBox ui_chkAutoStep;
    private javax.swing.JCheckBox ui_chkSendCommands;
    private javax.swing.JButton ui_cmdClose;
    private javax.swing.JButton ui_cmdConfigure;
    private javax.swing.JButton ui_cmdDiscover;
    private javax.swing.JButton ui_cmdSit;
    private javax.swing.JButton ui_cmdStop;
    private javax.swing.JButton ui_cmdTurnLeft;
    private javax.swing.JButton ui_cmdTurnRight;
    private javax.swing.JButton ui_cmdUp;
    private javax.swing.ButtonGroup ui_grpAlgorithm;
    private javax.swing.ButtonGroup ui_grpProjection;
    private javax.swing.JLabel ui_lblBearing;
    private javax.swing.JLabel ui_lblHeight;
    private javax.swing.JLabel ui_lblLogo;
    private javax.swing.JLabel ui_lblPStep;
    private javax.swing.JLabel ui_lblSpeed;
    private javax.swing.JLabel ui_lblTitle;
    private javax.swing.JPanel ui_pnlGraphics;
    private javax.swing.JPanel ui_pnlLeft;
    private javax.swing.JPanel ui_pnlLeftEast;
    private javax.swing.JPanel ui_pnlLeftNorth;
    private javax.swing.JPanel ui_pnlLeftSouth;
    private javax.swing.JPanel ui_pnlNorth;
    private javax.swing.JPanel ui_pnlProjections;
    private javax.swing.JTabbedPane ui_pnlRight;
    private javax.swing.JSplitPane ui_pnlSplit;
    private javax.swing.JPanel ui_pnlTab3D;
    private javax.swing.JPanel ui_pnlTabTrace;
    private javax.swing.JPanel ui_pnlValues;
    private javax.swing.JRadioButton ui_rdbElliptical;
    private javax.swing.JRadioButton ui_rdbOrthogonal;
    private javax.swing.JSlider ui_sldClearance;
    private javax.swing.JSlider ui_sldP;
    private javax.swing.JSlider ui_sldStep;
    private javax.swing.JSlider ui_sliHeight;
    private javax.swing.JToggleButton ui_tglIsometric;
    private javax.swing.JToggleButton ui_tglOblique;
    private javax.swing.JToggleButton ui_tglXY;
    private javax.swing.JToggleButton ui_tglXZ;
    private javax.swing.JTextArea ui_txtGlobalTrace;
    private javax.swing.JTextField ui_txtSend1;
    private javax.swing.JTextField ui_txtSend2;
    private javax.swing.JTextField ui_txtSend3;
    private javax.swing.JTextArea ui_txtStatus1;
    private javax.swing.JTextArea ui_txtStatus2;
    private javax.swing.JTextArea ui_txtStatus3;
    private javax.swing.JTextArea ui_txtStatus4;
    private javax.swing.JTextArea ui_txtStatus5;
    private javax.swing.JTextArea ui_txtStatus6;
    private javax.swing.JTextArea ui_txtTrace1;
    private javax.swing.JTextArea ui_txtTrace2;
    private javax.swing.JTextArea ui_txtTrace3;
    // End of variables declaration//GEN-END:variables
}
