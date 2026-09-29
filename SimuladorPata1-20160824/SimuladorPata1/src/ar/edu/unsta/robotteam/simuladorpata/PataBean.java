package ar.edu.unsta.robotteam.simuladorpata;

import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**

 @author gustavo
 */
public final class PataBean {

    public static final String PROP_AB = "AB";

    public static final String PROP_CD = "CD";
    public static final String PROP_CONFIGFILE = "configFile";

    public static final String PROP_DE = "DE";

    public static final String PROP_EF = "EF";
    public static final String PROP_MAXAB = "MAXAB";
    public static final String PROP_MAXCD = "MAXCD";
    public static final String PROP_MINAB = "MINAB";
    public static final String PROP_MINCD = "MINCD";

    public static final String PROP_OA = "OA";
    public static final String PROP_OB = "OB";

    public static final String PROP_OC = "OC";

    public static final String PROP_OE = "OE";
    public static final String PROP_PESO = "PESO";

    /**
     Cálculo por Teorema del Coseno del ángulo interno de un triángulo,
     conociendo sus tres lados

     @param p_ady1 longitud del primer lado adyascente
     @param p_ady2 longitud del segundo lado adyascente
     @param p_op longitud del opuesto
     @return ángulo interno entre los lados adyascentes
     */
    public static double anguloInterno(double p_ady1, double p_ady2, double p_op) {
        double l_arg = (p_ady1 * p_ady1 + p_ady2 * p_ady2 - p_op * p_op) / (2 * p_ady1 * p_ady2);
        return Math.acos(l_arg);
    }
    private double m_AB;
    private double m_CD;
    private double m_DE;
    private double m_EF;
    private double m_OA;
    private double m_OB;
    private double m_OC;
    private double m_OE;
    private File m_configFile;
    private double m_peso;
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private transient final VetoableChangeSupport vetoableChangeSupport = new VetoableChangeSupport(this);

    public PataBean() throws Exception {
        init();
    }

    /**
     Add PropertyChangeListener.

     @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     Add VetoableChangeListener.

     @param listener
     */
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        vetoableChangeSupport.addVetoableChangeListener(listener);
    }

    public Point.Double getA() {
        // A está verticalmente sobre O
        Point.Double l_p = new Point.Double(0.0, getOA());

        return l_p;
    }

    /**
     Get the value of AB

     @return the value of AB
     */
    public double getAB() {
        return m_AB;
    }

    /**
     Set the value of AB

     @param p_AB new value of AB
     @throws java.beans.PropertyVetoException
     */
    public void setAB(double p_AB) throws PropertyVetoException {
        double oldAB = this.m_AB;
        vetoableChangeSupport.fireVetoableChange(PROP_AB, oldAB, p_AB);
        this.m_AB = p_AB;
        propertyChangeSupport.firePropertyChange(PROP_AB, oldAB, p_AB);
    }

    public double getAOB() {
        return anguloInterno(getOA(), getOB(), getAB());
    }

    public Point.Double getB() {

        double l_AOB = getAOB();
        Point.Double l_p = new Point.Double(Math.sin(l_AOB) * getOB(),
                Math.cos(l_AOB) * getOB());
        return l_p;
    }

    public Point.Double getC() {
        double l_AOB = getAOB();
        Point.Double l_p = new Point.Double(Math.sin(l_AOB) * getOC(),
                Math.cos(l_AOB) * getOC());
        return l_p;
    }

    /**
     Get the value of CD

     @return the value of CD
     */
    public double getCD() {
        return m_CD;
    }

    /**
     Set the value of CD

     @param p_CD new value of CD
     @throws java.beans.PropertyVetoException
     */
    public void setCD(double p_CD) throws PropertyVetoException {
        double oldCD = this.m_CD;
        vetoableChangeSupport.fireVetoableChange(PROP_CD, oldCD, p_CD);
        this.m_CD = p_CD;
        propertyChangeSupport.firePropertyChange(PROP_CD, oldCD, p_CD);
    }

    public double getCE() {
        // Diferencia de longitud, porque están alieados
        return getOE() - getOC();
    }

    public double getCED() {
        // Cálculo por Teorema del Coseno
        double l_a = getCE(); // 1º adyascente
        double l_b = getDE(); // 2º adyascente
        double l_c = getCD(); // opuesto
        double l_arg = (l_a * l_a + l_b * l_b - l_c * l_c) / (2 * l_a * l_b);
        return Math.acos(l_arg);
    }

    /**
     Get the value of configFile

     @return the value of configFile
     */
    public File getConfigFile() {
        return m_configFile;
    }

    /**
     Set the value of configFile

     @param p_configFile new value of configFile
     */
    public void setConfigFile(File p_configFile) {
        File oldConfigFile = this.m_configFile;
        this.m_configFile = p_configFile;
        propertyChangeSupport.firePropertyChange(PROP_CONFIGFILE, oldConfigFile, p_configFile);
    }

    public Point.Double getD() {
        double l_OGF = getOGF();
        Point.Double l_E = getE();
        Point.Double l_p = new Point.Double(Math.sin(l_OGF) * getDE() + l_E.getX(),
                -Math.cos(l_OGF) * getDE() + l_E.getY());
        return l_p;
    }

    /**
     Get the value of DE

     @return the value of DE
     */
    public double getDE() {
        return m_DE;
    }

    /**
     Set the value of DE

     @param p_DE new value of DE
     @throws java.beans.PropertyVetoException
     */
    public void setDE(double p_DE) throws PropertyVetoException {
        double oldDE = this.m_DE;
        vetoableChangeSupport.fireVetoableChange(PROP_DE, oldDE, p_DE);
        this.m_DE = p_DE;
        propertyChangeSupport.firePropertyChange(PROP_DE, oldDE, p_DE);
        propertyChangeSupport.firePropertyChange(PROP_MINCD, oldDE, p_DE);
        propertyChangeSupport.firePropertyChange(PROP_MAXCD, oldDE, p_DE);
    }

    public Point.Double getE() {
        double l_AOB = getAOB();
        Point.Double l_p = new Point.Double(Math.sin(l_AOB) * getOE(),
                Math.cos(l_AOB) * getOE());
        return l_p;

    }

    /**
     Get the value of EF

     @return the value of EF
     */
    public double getEF() {
        return m_EF;
    }

    /**
     Set the value of EF

     @param p_EF new value of EF
     @throws java.beans.PropertyVetoException
     */
    public void setEF(double p_EF) throws PropertyVetoException {
        double oldEF = this.m_EF;
        vetoableChangeSupport.fireVetoableChange(PROP_EF, oldEF, p_EF);
        this.m_EF = p_EF;
        propertyChangeSupport.firePropertyChange(PROP_EF, oldEF, p_EF);
    }

    public Point.Double getF() {
        double l_OGF = getOGF();
        Point.Double l_E = getE();
        Point.Double l_p = new Point.Double(Math.sin(l_OGF) * getEF() + l_E.getX(),
                -Math.cos(l_OGF) * getEF() + l_E.getY());
        return l_p;
    }

    /**
     Fuerza en AB

     @return
     */
    public double getFuerzaAB() {
        double l_f2 = getTO() / getOB() * 10; // Proyección del torque hacia B en cm
        double l_OBA = getOBA();
        if (l_OBA == 0.0 || Math.abs(l_OBA) == Math.PI) {
            return 0.0;
        } // end if
        double l_cos = Math.cos(l_OBA - Math.PI / 2);
        double l_fab = l_f2 / l_cos;

        return l_fab;

    }

    public double getMaxAB() {
        return getOA() + getOB();
    }

    public double getMaxCD() {
        return getCE() + getDE();
    }

    public double getMinAB() {
        return getOA() - getOB();
    }

    public double getMinCD() {
        return getCE() - getDE();
    }

    /**
     Get the value of OA

     @return the value of OA
     */
    public double getOA() {
        return m_OA;
    }

    public double getOBA() {
        return anguloInterno(getOB(), getAB(), getOA());
    }

    /**
     Set the value of OA

     @param p_OA new value of OA
     */
    public void setOA(double p_OA) throws PropertyVetoException {
        double oldOA = this.m_OA;
        vetoableChangeSupport.fireVetoableChange(PROP_OA, oldOA, p_OA);
        this.m_OA = p_OA;
        propertyChangeSupport.firePropertyChange(PROP_OA, oldOA, p_OA);
        propertyChangeSupport.firePropertyChange(PROP_MINAB, oldOA, p_OA);
        propertyChangeSupport.firePropertyChange(PROP_MAXAB, oldOA, p_OA);
    }

    /**
     Get the value of OB

     @return the value of OB
     */
    public double getOB() {
        return m_OB;
    }

    /**
     Set the value of OB

     @param p_OB new value of OB
     @throws java.beans.PropertyVetoException
     */
    public void setOB(double p_OB) throws PropertyVetoException {
        double oldOB = this.m_OB;
        vetoableChangeSupport.fireVetoableChange(PROP_OB, oldOB, p_OB);
        this.m_OB = p_OB;
        propertyChangeSupport.firePropertyChange(PROP_OB, oldOB, p_OB);
        propertyChangeSupport.firePropertyChange(PROP_MINAB, oldOB, p_OB);
        propertyChangeSupport.firePropertyChange(PROP_MAXAB, oldOB, p_OB);
    }

    /**
     Get the value of OC

     @return the value of OC
     */
    public double getOC() {
        return m_OC;
    }

    /**
     Set the value of OC

     @param p_OC new value of OC
     @throws java.beans.PropertyVetoException
     */
    public void setOC(double p_OC) throws PropertyVetoException {
        double oldOC = this.m_OC;
        vetoableChangeSupport.fireVetoableChange(PROP_OC, oldOC, p_OC);
        this.m_OC = p_OC;
        propertyChangeSupport.firePropertyChange(PROP_OC, oldOC, p_OC);
        propertyChangeSupport.firePropertyChange(PROP_MINCD, oldOC, p_OC);
        propertyChangeSupport.firePropertyChange(PROP_MAXCD, oldOC, p_OC);

    }

    /**
     Get the value of OE

     @return the value of OE
     */
    public double getOE() {
        return m_OE;
    }

    /**
     Set the value of OE

     @param p_OE new value of OE
     @throws java.beans.PropertyVetoException
     */
    public void setOE(double p_OE) throws PropertyVetoException {
        double oldOE = this.m_OE;
        vetoableChangeSupport.fireVetoableChange(PROP_OE, oldOE, p_OE);
        this.m_OE = p_OE;
        propertyChangeSupport.firePropertyChange(PROP_OE, oldOE, p_OE);
        propertyChangeSupport.firePropertyChange(PROP_MINCD, oldOE, p_OE);
        propertyChangeSupport.firePropertyChange(PROP_MAXCD, oldOE, p_OE);
    }

    public double getOGF() {
        // Diferencia entre los ángulos
        return getCED() - getAOB();
    }

    /**
     Get the value of P

     @return the value of P
     */
    public double getPeso() {
        return m_peso;
    }

    /**
     Set the value of P

     @param p_peso new value of P
     @throws java.beans.PropertyVetoException
     */
    public void setPeso(double p_peso) throws PropertyVetoException {
        double oldP = this.m_peso;
        vetoableChangeSupport.fireVetoableChange(PROP_PESO, oldP, p_peso);
        this.m_peso = p_peso;
        propertyChangeSupport.firePropertyChange(PROP_PESO, oldP, p_peso);
    }

    /**
     Torque en O (origen), expresado en kg x cm

     @return
     */
    public double getTO() {

        Point.Double l_f = getF();

        if (l_f.getX() == 0.0) {
            // Vertical
            return 0.0;
        } // end if

        double l_of = Math.hypot(l_f.getX(), l_f.getY()); // hipotenusa
        double l_coseno = l_f.getX() / l_of; // coseno = cateto adyascente / hipotenusa
        double l_torque = l_coseno * getPeso() * (l_of / 10.0); // convierte a kg x cm

        return l_torque;
    }

    public void init() throws Exception {
        // Estructura
        setDE(50.0);
        setEF(500.0);
        setOA(300.0);
        setOB(200.0);
        setOC(100.0);
        setOE(500.0);
        setPeso(10.0);

        // Actuadores
        setAB((getMaxAB() + getMinAB()) / 2);
        setCD((getMaxCD() + getMinCD()) / 2);

        setConfigFile(null);
    }

    public void loadConfig() throws Exception {
        Properties l_props = new Properties();
        l_props.load(new FileReader(getConfigFile()));
        init();
        setDE(Double.parseDouble(l_props.getProperty(PROP_DE)));
        setEF(Double.parseDouble(l_props.getProperty(PROP_EF)));
        setOA(Double.parseDouble(l_props.getProperty(PROP_OA)));
        setOB(Double.parseDouble(l_props.getProperty(PROP_OB)));
        setOC(Double.parseDouble(l_props.getProperty(PROP_OC)));
        setOE(Double.parseDouble(l_props.getProperty(PROP_OE)));
        setPeso(Double.parseDouble(l_props.getProperty(PROP_PESO)));

        setAB(Double.parseDouble(l_props.getProperty(PROP_AB)));
        setCD(Double.parseDouble(l_props.getProperty(PROP_CD)));

    }

    /**
     Remove PropertyChangeListener.

     @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     Remove VetoableChangeListener.

     @param listener
     */
    public void removeVetoableChangeListener(VetoableChangeListener listener) {
        vetoableChangeSupport.removeVetoableChangeListener(listener);
    }

    public void saveConfig() throws Exception {
        Properties l_props = new Properties();

        l_props.setProperty(PROP_AB, "" + m_AB);
        l_props.setProperty(PROP_CD, "" + m_CD);
        l_props.setProperty(PROP_DE, "" + m_DE);
        l_props.setProperty(PROP_EF, "" + m_EF);
        l_props.setProperty(PROP_OA, "" + m_OA);
        l_props.setProperty(PROP_OB, "" + m_OB);
        l_props.setProperty(PROP_OC, "" + m_OB);
        l_props.setProperty(PROP_OE, "" + m_OE);
        l_props.setProperty(PROP_PESO, "" + m_peso);

        l_props.store(new FileWriter(getConfigFile()), null);
    }

    public PataBean cloneBean() throws Exception {
        PataBean l_toReturn = new PataBean();

        l_toReturn.setDE(getDE());
        l_toReturn.setEF(getEF());
        l_toReturn.setOA(getOA());
        l_toReturn.setOB(getOB());
        l_toReturn.setOC(getOC());
        l_toReturn.setOE(getOE());
        l_toReturn.setPeso(getPeso());

        l_toReturn.setAB(getAB());
        l_toReturn.setCD(getCD());

        return l_toReturn;
    }

}
