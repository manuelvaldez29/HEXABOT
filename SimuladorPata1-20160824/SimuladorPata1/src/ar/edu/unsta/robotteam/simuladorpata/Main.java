package ar.edu.unsta.robotteam.simuladorpata;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.jdesktop.beansbinding.Converter;

/**

 @author gustavo
 */
public class Main extends javax.swing.JFrame {

    private double m_model2view = 100.0 / 300.0; // pixels / mm
    private List<ResultadoPuntual> m_resultados;
    private int m_pasosActuador = 100;

    private enum TipoAnalisis {
        TO,
        FAB,
        TE,
        FCD
    }

    private TipoAnalisis m_tipoAnalisis;

    private class DIConverter extends Converter<Double, Integer> {

        @Override
        public Integer convertForward(Double p_value) {
            if (p_value == null) {
                return null;
            } //end if

            return p_value.intValue();
        }

        @Override
        public Double convertReverse(Integer p_value) {
            if (p_value == null) {
                return null;
            } // end if
            return p_value.doubleValue();
        }

    }

    private PataBean m_pata;
    private javax.swing.JPanel ui_display1;

    /**
     Get the value of pata

     @return the value of pata
     */
    public PataBean getPata() throws Exception {
        if (m_pata == null) {
            m_pata = new PataBean();
        } // end if
        return m_pata;
    }

    /**
     Set the value of pata

     @param p_pata new value of pata
     */
    public void setPata(PataBean p_pata) {
        this.m_pata = p_pata;
    }

    /**
     Creates new form Main
     */
    public Main() throws Exception {
        initComponents();
        ui_display1 = new javax.swing.JPanel() {

            private int m_grWidth;
            private int m_grHeight;
            private int m_grCenterX;
            private int m_grCenterY;

            private int m_rad = 5;

            /**
             Dibuja la pata

             @param p_g
             */
            @Override
            public void paint(Graphics p_g) {
                try {
                    super.paint(p_g);

                    Graphics2D l_gr = (Graphics2D) p_g;
                    if (l_gr == null) {
                        return;
                    } //end if
                    m_grWidth = ui_display1.getWidth();
                    m_grHeight = ui_display1.getHeight();
                    m_grCenterX = m_grWidth / 2;
                    m_grCenterY = m_grHeight / 2;

                    // Dibuja resultados de la última simulación
                    if (m_resultados != null) {
                        for (ResultadoPuntual l_res : m_resultados) {
                            dibujaResultado(l_gr, l_res);
                        } // end for
                    } // end if

                    // Dibuja fémur (OE)
                    drawModelLine(l_gr, 0, 0,
                            getPata().getE().getX(), getPata().getE().getY(),
                            Color.BLUE, 12);

                    // Dibuja tibia (EF)
                    drawModelLine(l_gr, getPata().getE().getX(), getPata().getE().getY(),
                            getPata().getF().getX(), getPata().getF().getY(),
                            Color.GREEN, 8);

                    // Dibuja primer actuador (AB)
                    drawModelLine(l_gr, getPata().getA().getX(), getPata().getA().getY(),
                            getPata().getB().getX(), getPata().getB().getY(),
                            Color.RED, 4);

                    // Dibuja segundo actuador (CD)
                    drawModelLine(l_gr, getPata().getC().getX(), getPata().getC().getY(),
                            getPata().getD().getX(), getPata().getD().getY(),
                            Color.ORANGE, 4);

                    // Dibuja ejes cartesianos
                    l_gr.setColor(Color.black);
                    l_gr.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND));
                    l_gr.drawLine(0, m_grCenterY, m_grWidth - 1, m_grCenterY);
                    l_gr.drawLine(m_grCenterX, 0, m_grCenterX, m_grHeight - 1);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                }

            }

            private void dibujaResultado(Graphics2D p_gr, ResultadoPuntual p_res) {
                int l_x = model2viewX(p_res.m_x);
                int l_y = model2viewY(p_res.m_y);
                p_gr.setColor(p_res.m_color);
                p_gr.fillOval(l_x - m_rad, l_y - m_rad, m_rad + m_rad, m_rad + m_rad);
            }

            private void drawModelLine(Graphics2D p_gr, double p_x0, double p_y0,
                    double p_x1, double p_y1, Color p_color, int p_width) {
                p_gr.setColor(p_color);
                p_gr.setStroke(new BasicStroke(p_width, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                p_gr.drawLine(model2viewX(p_x0), model2viewY(p_y0),
                        model2viewX(p_x1), model2viewY(p_y1));

            }

            private int model2viewX(double p_value) {
                return (int) (p_value * m_model2view + m_grCenterX);
            }

            private int model2viewY(double p_value) {
                return (int) (-p_value * m_model2view + m_grCenterY);
            }

        };

        PropertyChangeListener l_displayListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent p_evt) {
                String l_prop = p_evt.getPropertyName();

                if (l_prop.equals(PataBean.PROP_AB)
                        || l_prop.equals(PataBean.PROP_CD)) {
                    ui_display1.repaint();

                } // end if
            }
        };

        getPata().addPropertyChangeListener(l_displayListener);

        ui_display1.setOpaque(true);
        ui_display1.setBackground(Color.WHITE);
        jTabbedPane1.addTab("Representación", ui_display1);
    }

    /**
     This method is called from within the constructor to
     initialize the form.
     WARNING: Do NOT modify this code. The content of this method is
     always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        ui_dlgConfigFile = new javax.swing.JFileChooser();
        ui_northPanel = new javax.swing.JPanel();
        ui_legIcon = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        ui_sldAB = new javax.swing.JSlider();
        ui_sldCD = new javax.swing.JSlider();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        ui_AB = new javax.swing.JTextField();
        ui_CD = new javax.swing.JTextField();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        ui_OA = new javax.swing.JTextField();
        ui_OB = new javax.swing.JTextField();
        ui_OC = new javax.swing.JTextField();
        ui_OE = new javax.swing.JTextField();
        ui_EF = new javax.swing.JTextField();
        ui_p = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        ui_cmdAnalizaTO = new javax.swing.JButton();
        ui_cmdAnalizaFAB = new javax.swing.JButton();
        ui_chkAnimacion = new javax.swing.JCheckBox();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        ui_lblMin = new javax.swing.JLabel();
        ui_lblMax = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Simulador de Patas v1");

        ui_northPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ui_legIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphics/leg1.png"))); // NOI18N
        ui_legIcon.setOpaque(true);
        ui_northPanel.add(ui_legIcon, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 20, 130, 150));

        jLabel1.setBackground(new java.awt.Color(0, 0, 153));
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("SIMULADOR DE PATAS v1");
        jLabel1.setOpaque(true);
        ui_northPanel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 300, 40));

        jLabel2.setText("Universidad del Norte Santo Tomás de Aquino / Facultad de Ingeniería / Laboratorio de Robótica");
        ui_northPanel.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, -1));

        jLabel3.setText("Actuador AB:");
        ui_northPanel.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, -1, -1));

        jLabel4.setText("Actuador CD:");
        ui_northPanel.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 130, -1, -1));

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.AB}"), ui_sldAB, org.jdesktop.beansbinding.BeanProperty.create("value"));
        binding.setConverter(new DIConverter());
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.maxAB}"), ui_sldAB, org.jdesktop.beansbinding.BeanProperty.create("maximum"));
        binding.setConverter(new DIConverter());
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, this, org.jdesktop.beansbinding.ELProperty.create("${pata.minAB}"), ui_sldAB, org.jdesktop.beansbinding.BeanProperty.create("minimum"));
        binding.setConverter(new DIConverter());
        bindingGroup.addBinding(binding);

        ui_northPanel.add(ui_sldAB, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 100, 430, -1));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.CD}"), ui_sldCD, org.jdesktop.beansbinding.BeanProperty.create("value"));
        binding.setConverter(new DIConverter());
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, this, org.jdesktop.beansbinding.ELProperty.create("${pata.maxCD}"), ui_sldCD, org.jdesktop.beansbinding.BeanProperty.create("maximum"));
        binding.setConverter(new DIConverter());
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, this, org.jdesktop.beansbinding.ELProperty.create("${pata.minCD}"), ui_sldCD, org.jdesktop.beansbinding.BeanProperty.create("minimum"));
        binding.setConverter(new DIConverter());
        bindingGroup.addBinding(binding);

        ui_northPanel.add(ui_sldCD, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 130, 430, -1));

        jLabel17.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel17.setText("mm");
        ui_northPanel.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 100, -1, -1));

        jLabel18.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel18.setText("mm");
        ui_northPanel.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 130, -1, -1));

        ui_AB.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_AB.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.AB}"), ui_AB, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        ui_northPanel.add(ui_AB, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 100, -1, -1));

        ui_CD.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_CD.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.CD}"), ui_CD, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        ui_northPanel.add(ui_CD, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 130, -1, -1));

        getContentPane().add(ui_northPanel, java.awt.BorderLayout.NORTH);

        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel5.setText("OA:");
        jPanel2.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 30, -1, -1));

        jLabel6.setText("OB:");
        jPanel2.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 60, -1, -1));

        jLabel7.setText("OC:");
        jPanel2.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 90, -1, -1));

        jLabel8.setText("OE (fémur):");
        jPanel2.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 30, -1, -1));

        jLabel9.setText("EF (tibia):");
        jPanel2.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 60, -1, -1));

        jLabel10.setText("P (peso):");
        jPanel2.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 120, -1, -1));

        ui_OA.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_OA.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.OA}"), ui_OA, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        jPanel2.add(ui_OA, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 30, -1, -1));

        ui_OB.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_OB.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.OB}"), ui_OB, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        jPanel2.add(ui_OB, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 60, -1, -1));

        ui_OC.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_OC.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.OC}"), ui_OC, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        jPanel2.add(ui_OC, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 90, -1, -1));

        ui_OE.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_OE.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.OE}"), ui_OE, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        jPanel2.add(ui_OE, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 30, -1, -1));

        ui_EF.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_EF.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.EF}"), ui_EF, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        jPanel2.add(ui_EF, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 60, -1, -1));

        ui_p.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ui_p.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.peso}"), ui_p, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        jPanel2.add(ui_p, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 120, -1, -1));

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel11.setText("mm");
        jPanel2.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 30, -1, -1));

        jLabel12.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel12.setText("mm");
        jPanel2.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 60, -1, -1));

        jLabel13.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel13.setText("mm");
        jPanel2.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 90, -1, -1));

        jLabel14.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel14.setText("mm");
        jPanel2.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 30, -1, -1));

        jLabel15.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel15.setText("mm");
        jPanel2.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 60, -1, -1));

        jLabel16.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel16.setText("kg");
        jPanel2.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 120, -1, -1));

        jLabel19.setText("DE:");
        jPanel2.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 120, -1, -1));

        jLabel20.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel20.setText("mm");
        jPanel2.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 120, -1, -1));

        jTextField1.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField1.setPreferredSize(new java.awt.Dimension(60, 19));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${pata.DE}"), jTextField1, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);

        jPanel2.add(jTextField1, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 120, -1, -1));

        jTabbedPane1.addTab("Parámetros", jPanel2);

        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ui_cmdAnalizaTO.setText("Torque en O");
        ui_cmdAnalizaTO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdAnalizaTOActionPerformed(evt);
            }
        });
        jPanel4.add(ui_cmdAnalizaTO, new org.netbeans.lib.awtextra.AbsoluteConstraints(12, 12, -1, -1));

        ui_cmdAnalizaFAB.setText("Fuerza AB");
        ui_cmdAnalizaFAB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ui_cmdAnalizaFABActionPerformed(evt);
            }
        });
        jPanel4.add(ui_cmdAnalizaFAB, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 120, -1));

        ui_chkAnimacion.setText("Animación");
        jPanel4.add(ui_chkAnimacion, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 13, -1, 30));

        jLabel21.setText("Mínimo:");
        jPanel4.add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 20, -1, -1));

        jLabel22.setText("Máximo:");
        jPanel4.add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 50, -1, -1));

        ui_lblMin.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_lblMin.setForeground(new java.awt.Color(255, 255, 255));
        ui_lblMin.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        ui_lblMin.setText("0.0");
        ui_lblMin.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        ui_lblMin.setOpaque(true);
        ui_lblMin.setPreferredSize(new java.awt.Dimension(100, 22));
        jPanel4.add(ui_lblMin, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 20, -1, -1));

        ui_lblMax.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ui_lblMax.setForeground(new java.awt.Color(255, 255, 255));
        ui_lblMax.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        ui_lblMax.setText("0.0");
        ui_lblMax.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        ui_lblMax.setOpaque(true);
        ui_lblMax.setPreferredSize(new java.awt.Dimension(100, 22));
        jPanel4.add(ui_lblMax, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 50, -1, -1));

        jTabbedPane1.addTab("Análisis", jPanel4);

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);
        jTabbedPane1.getAccessibleContext().setAccessibleName("Análisis");

        jMenu1.setText("Archivo");

        jMenuItem5.setText("Nuevo");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem5);

        jMenuItem1.setText("Abrir...");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText("Guardar");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setText("Guardar como...");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);
        jMenu1.add(jSeparator1);

        jMenuItem4.setText("Salir");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem4);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Análisis");

        jMenuItem6.setText("Torque en O");
        jMenu2.add(jMenuItem6);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        bindingGroup.bind();

        setSize(new java.awt.Dimension(884, 565));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void ui_cmdAnalizaTOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdAnalizaTOActionPerformed
        m_tipoAnalisis = TipoAnalisis.TO;
        analiza();

    }//GEN-LAST:event_ui_cmdAnalizaTOActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        doCmdNuevo();
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        doCmdAbrir();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        doCmdGuardar();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        doCmdGuardarComo();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        doCmdSalir();
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void ui_cmdAnalizaFABActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ui_cmdAnalizaFABActionPerformed
        m_tipoAnalisis = TipoAnalisis.FAB;

        analiza();

    }//GEN-LAST:event_ui_cmdAnalizaFABActionPerformed

    private void doCmdNuevo() {
        try {
            getPata().init();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doCmdAbrir() {

    }

    private void doCmdGuardar() {
        try {
            if (getPata().getConfigFile() == null) {
                doCmdGuardarComo();
            } // end if
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doCmdGuardarComo() {
        try {
            int l_result = ui_dlgConfigFile.showSaveDialog(this);
            if (l_result == JFileChooser.CANCEL_OPTION) {
                return;
            } // end if
            File l_confFile = ui_dlgConfigFile.getSelectedFile();
            getPata().setConfigFile(l_confFile);
            getPata().saveConfig();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void doCmdSalir() {
        dispose();
    }

    /**
     Análisis
     Se ejecuta desde el EDT

     */
    private void analiza() {

        new Thread() {
            @Override
            public void run() {
                try {
                    //PataBean l_p = getPata();
                    PataBean l_p = getPata().cloneBean();

                    double l_stepAB = (l_p.getMaxAB() - l_p.getMinAB()) / m_pasosActuador;
                    double l_stepCD = (l_p.getMaxCD() - l_p.getMinCD()) / m_pasosActuador;
                    m_resultados = new ArrayList<>();

                    for (double l_ab = l_p.getMinAB() + l_stepAB; l_ab < l_p.getMaxAB(); l_ab += l_stepAB) {
                        for (double l_cd = l_p.getMinCD() + l_stepCD; l_cd < l_p.getMaxCD(); l_cd += l_stepCD) {

                            ResultadoPuntual l_res = new ResultadoPuntual();

                            try {
                                l_p.setAB(l_ab);
                                l_p.setCD(l_cd);

                                Point2D l_f = l_p.getF();
                                l_res.m_x = l_f.getX();
                                l_res.m_y = l_f.getY();

                                if (l_res.m_x < 0) {
                                    continue;
                                } // end if
                                if (l_res.m_y > 0) {
                                    continue;
                                } // end if
                                
                                switch (m_tipoAnalisis) {
                                    case TO: {
                                        l_res.m_value = l_p.getTO();
                                    }
                                    break;
                                    case FAB: {
                                        l_res.m_value = l_p.getFuerzaAB();
                                    }
                                    break;
                                }

                                l_res.m_color = Color.RED;

                                m_resultados.add(l_res);
                            } catch (PropertyVetoException ex) {
                                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        } // end for
                    } // end for

                    analizaResultados();

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ui_display1.repaint();
                        }
                    });
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }.start();

    }

    /**
     Encuentra máximos, mínimos y coloriza
     */
    private void analizaResultados() {
        if (m_resultados == null) {
            return;
        } // end if

        // Extremos
        double l_max = Double.MIN_VALUE;
        double l_min = Double.MAX_VALUE;
        for (ResultadoPuntual l_res : m_resultados) {
            double l_absValue = Math.abs(l_res.m_value);
            if (l_absValue < l_min) {
                l_min = l_absValue;
            } // end if
            if (l_absValue > l_max) {
                l_max = l_absValue;
            } // end if
        } // end for

        // Colores
        for (ResultadoPuntual l_res : m_resultados) {
            l_res.m_color = coloriza(l_min, l_max, Math.abs(l_res.m_value));
        } // end for
        ui_lblMin.setText(String.format(Locale.US, "%8.2f", l_min));
        ui_lblMax.setText(String.format(Locale.US, "%8.2f", l_max));
        ui_lblMin.setBackground(coloriza(l_min, l_max, l_min));
        ui_lblMax.setBackground(coloriza(l_min, l_max, l_max));

    }

    private Color coloriza(double p_min, double p_max, double p_valor) {
        int l_index = (int) ((p_valor - p_min) / (p_max - p_min) * 1023);
        l_index = 1023 - l_index;
        int l_red = 0;
        int l_green = 0;
        int l_blue = 0;
        if (l_index < 512) {
            if (l_index < 256) {
                // Tramo 1
                l_red = 255;
                l_green = l_index;
            } else {
                // Tramo 2
                l_red = 511 - l_index;
                l_green = 255;
            } // end if
        } else if (l_index < 768) {
            // Tramo 3
            l_green = 255;
            l_blue = l_index - 512;
        } else {
            // Tramo 4
            l_green = 1023 - l_index;
            l_blue = 255;
        } // end if // end if

        Color l_color = new Color(l_red, l_green, l_blue);
        return l_color;
    }

    /**
     @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Main().setVisible(true);
                } catch (Exception l_ex) {
                    System.out.println(l_ex.getMessage());
                    l_ex.printStackTrace();
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField ui_AB;
    private javax.swing.JTextField ui_CD;
    private javax.swing.JTextField ui_EF;
    private javax.swing.JTextField ui_OA;
    private javax.swing.JTextField ui_OB;
    private javax.swing.JTextField ui_OC;
    private javax.swing.JTextField ui_OE;
    private javax.swing.JCheckBox ui_chkAnimacion;
    private javax.swing.JButton ui_cmdAnalizaFAB;
    private javax.swing.JButton ui_cmdAnalizaTO;
    private javax.swing.JFileChooser ui_dlgConfigFile;
    private javax.swing.JLabel ui_lblMax;
    private javax.swing.JLabel ui_lblMin;
    private javax.swing.JLabel ui_legIcon;
    private javax.swing.JPanel ui_northPanel;
    private javax.swing.JTextField ui_p;
    private javax.swing.JSlider ui_sldAB;
    private javax.swing.JSlider ui_sldCD;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
