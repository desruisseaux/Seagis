package fr.ird.database.gui.swing;

// J2SE.
import java.sql.SQLException;

import java.io.File;
import java.io.IOException;
//import java.io.*;

import java.text.*;

import java.util.regex.*;
import java.util.Iterator;
import java.util.Date;
import java.util.Map;
import java.util.regex.*;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;

import java.awt.image.*;
import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.SwingUtilities;
import javax.swing.border.*;
import javax.imageio.*;


// SEAGIS.
import fr.ird.database.*;
import fr.ird.database.coverage.Updater;
import fr.ird.database.coverage.sql.*;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.resources.Utilities;


// GEOTOOLS.
import org.geotools.pt.Envelope;
import org.geotools.cs.*;
import org.geotools.cv.*;
import org.geotools.gc.*;
import org.geotools.util.*;
import org.geotools.resources.*;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.gui.swing.*;


/**
 * This interface is designed for inserting images into database 'image'.
 *
 * @author Remi Eve
 * @version $Id$
 */
public class CoverageUpdater extends JFrame implements ChangeListener, ListSelectionListener {

    /**
     * Components of the interface.
     */
    private JButton bAdd,
                    bDelete,
                    bExit,
                    bImage,
                    bInsert,
                    bCopy,
                    bPaste;
    
    private JCheckBox checkAutoName;
    
    private JComboBox comboSeries;
    private org.geotools.gui.swing.CoordinateChooser coordinateChooser;
    
    private JPanel jPanel1,
                   jPanel2,
                   jPanel4,
                   panel;
    
    private JScrollPane jScrollPane1;
    
    private JLabel label1,
                   label12,
                   label13;
    
    private JList listImages;

    private JTextField textImage,
                       textName;
    
    
    ///////////////////////////////////////////////////////////////////////////
    ////////////                                                   ////////////
    ////////////                        CLASSE                     ////////////
    ////////////                                                   ////////////
    ///////////////////////////////////////////////////////////////////////////    
    /**
     * This inner class remember information that will be insert for each image.
     */
    private final class Record {
        public String file = "",
                      name = "",
                      serie = "";
        public Date   start,
                      end;                    
        public TimeZone timeZone;
        public Rectangle2D area;   
        public boolean autoname;
        
        /**
         * Constructor of Record.
         */
        public Record() {            
        }
    }
    
    /**
     * Vector of record.
     */
    private final Vector vRecord;
    
    /**
     * A map that associated to 'name of serie' (key) an 'prefix' (object).
     */
    private final Map map;
    private final String[][] NAMES = {{"Bathymétrie de Baudry (Réunion)",      ""},
                                      {"Bathymétrie de Sandwell (Réunion)",    ""},
                                      {"Bathymétrie ETOPO (Méditerranée)",     ""},
                                      {"CHL (Méditerranée)",                   "CHL"},
                                      {"CHL (Monde)",                          "S"},
                                      {"CHL-log (Monde)",                      ""},
                                      {"EKP (Monde)",                          "EKP"},
                                      {"Identité",                             ""},
                                      {"Images de tests",                      ""},
                                      {"Potentiel de pêche C1 (Réunion)",      "PP"},
                                      {"Potentiel de pêche C1-CHL (Réunion)",  ""},
                                      {"Potentiel de pêche C1-EKP (Réunion)",  ""},
                                      {"Potentiel de pêche C1-SLA (Réunion)",  ""},
                                      {"Potentiel de pêche C1-SST (Réunion)",  ""},
                                      {"Potentiel de pêche C1-SST_GAC (Réunion)", ""},
                                      {"Potentiel de pêche C3 (Réunion)",      ""},
                                      {"Potentiel de pêche C3-CHL (Réunion)",  ""},
                                      {"Potentiel de pêche C3-EKP (Réunion)",  ""},
                                      {"Potentiel de pêche C3-SLA (Réunion)",  ""},
                                      {"Potentiel de pêche C3-SST (Réunion)",  ""},
                                      {"Potentiel de pêche C3-SST_GAC (Réunion)", ""},
                                      {"SLA (Méditerranée - NRT)",             "HUV"},
                                      {"SLA (Monde - TP)",                     "TP_"},
                                      {"SLA (Monde - TP/ERS - Mercator)",      "TE_"},
                                      {"SLA (Monde - TP/ERS)",                 "TE_"},
                                      {"SLA (Réunion - NRT)",                  "HUV"},
                                      {"SLA (Réunion)",                        "HUV"}, 
                                      {"SST (Canaries - synthèse 5 jours)",    ""},
                                      {"SST (Méditerranée)",                   "SST"},
                                      {"SST (Monde)",                          ""},
                                      {"SST (Réunion - aperçu de synthèse)",   ""},  
                                      {"SST (Réunion - moyennes hebdomadaires)", ""},
                                      {"SST (Réunion - moyennes mensuelles)",  ""},
                                      {"SST (Réunion - synthèse 5 jours)",     "SYN"},
                                      {"SST (Réunion - nouvelle chaîne)",      "SYN"},                                      
                                      {"SST (Réunion - synthèse jour)",        "D"},        
                                      {"SST (Réunion - synthèse nuit)",        "N"},
                                      {"SST filtrés",                          ""}};    
    
    /**
     * Connection to DataBase.
     */
    private final CoverageDataBase dataBase;
    
    /**
     * Thread affichant la bar de progression du traitement.
     */
    protected Thread threadProgress;
    
    /**
     * Progression de la tache.
     */
    protected ImageProgress progress;

    
    ///////////////////////////////////////////////////////////////////////////
    ////////////                                                   ////////////
    ////////////                   CONSTRUCTOR                     ////////////
    ////////////                                                   ////////////
    ///////////////////////////////////////////////////////////////////////////    
    /** 
     * Constructor. 
     */
    public CoverageUpdater() throws SQLException {
        initComponents();
        
        // Création de la fenetre de progression des tâches.
        progress = new ImageProgress(this, true);
        progress.setSize(250,100);  
        final Dimension size = getToolkit().getScreenSize();
        progress.setLocation((int)(size.getWidth()  / 2 - 125), (int)(size.getHeight() / 2 - 50));
        
        // Preparation du thread lancant la barre de progression.
        threadProgress = new Thread(new  Runnable() {
            public void run() {
                progress.show();
            }
        });  
        
        vRecord = new Vector();
        dataBase = new CoverageDataBase();            
        
        // Init map.
        map = new HashMap();
        for (int i=0 ; i<NAMES.length ; i++) {
            if (NAMES[i].length == 2) {
                // KEY : serie
                // OBJECT : header            
                map.put(NAMES[i][0], NAMES[i][1]);
            }
        }
        
        // Init Series.
        final SeriesTable series = dataBase.getSeriesTable();
        final Iterator it = series.getEntries().iterator();
        
        while (it.hasNext()) {
            final Entry entry = (SeriesEntry)it.next();
            comboSeries.addItem(entry.getName());
        }
        
    }
    

    ///////////////////////////////////////////////////////////////////////////
    ////////////                                                   ////////////
    ////////////                      PRIVATE                      ////////////
    ////////////                                                   ////////////
    ///////////////////////////////////////////////////////////////////////////    
    /** 
     * Init components.
     */
    private void initComponents() {
        // Create component.
        jPanel1 = new JPanel();
        panel   = new JPanel();
        
        textImage = new JTextField();
        textName = new JTextField();
        
        bImage  = new JButton();
        bAdd    = new JButton();
        bDelete = new JButton();
        bInsert = new JButton();
        bExit   = new JButton();
        bCopy = new JButton();
        bPaste = new JButton();
        
        label1  = new JLabel();
        label12 = new JLabel();
        label13 = new JLabel();
        
        checkAutoName = new JCheckBox();
        
        jPanel4 = new JPanel();
        jPanel2 = new JPanel();
        
        comboSeries = new JComboBox();
        
        coordinateChooser = new org.geotools.gui.swing.CoordinateChooser();        
        
        jScrollPane1 = new JScrollPane();
        
        listImages = new JList();
        
        
        // Listener.
        bImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (bImage.isEnabled()) {
                    changeImage();
                }
            }
        });        
        bInsert.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (bInsert.isEnabled()) {
                    updateRecord();                    
                    insertToDataBase();
                }
            }
        });        
        textName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
        });
        textName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                updateRecord();
                updateInformation();                
            }
        });        
        textImage.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                updateRecord();
                update();
                updateInformation();                
            }
        });        
        textImage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateRecord();
                update();
                updateInformation();
            }
        });      
        //coordinateChooser.addVetoableChangeListener(new java.beans.VetoableChangeListener
        /*vetoableChange(PropertyChangeEvent evt) 
          /*This method gets called when a constrained property is changed.*/
        /*coordinateChooser.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                System.out.println("coordin lost");                
                //updateRecord();
                //updateInformation();
            }
            public void focusGain(java.awt.event.FocusEvent evt) {
                System.out.println("coor gain");
            }
        });        
        /*coordinateChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateRecord();
                updateInformation();
            }
        });        */
        bInsert.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();                
            }
        });
        bExit.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();                
            }
        });
        bAdd.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();                
            }
        });
        bDelete.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();                
            }
        });

        comboSeries.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateRecord();
                updateInformation();
            }
        });        
        comboSeries.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateRecord();
                updateInformation();
            }
        });        
        
        
        //listImages.addListSelectionListener({
        /*listImages.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGain(java.awt.event.FocusEvent evt) {
  //              updateRecord();
                updateInformation();
            }
        });        */
        listImages.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                updateInformation();
            }
        });
        
        bDelete.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (bDelete.isEnabled()) {
                    updateRecord();
                    deleteImages();
                }
            }
        });
        bAdd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (bAdd.isEnabled()) {
                    updateRecord();                
                    addImages();
                }
            }
        });
        bExit.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (bExit.isEnabled()) {
                    System.exit(1);
                }
            }
        });
        checkAutoName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                /*final int index = listImages.getSelectedIndex();
                if (index == -1) {
                    return;
                }
                final Record record = (Record)vRecord.get(index);                                
                System.out.println("index : " + index + "-->" + checkAutoName.isSelected());
                record.autoname = checkAutoName.isSelected();
                textName.setText(computeName(record.autoname, new File(record.file), record.end, record.serie));*/
                updateRecord();
                updateInformation();
            }
        });        
        
        // JFrame.
        setTitle("Insert an image to DataBase");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                System.exit(1);
            }
        });        

        jPanel1.add(panel);
        jPanel1.add(bInsert);
        jPanel2.add(jScrollPane1);
        jPanel2.add(label13);
        jPanel1.add(bExit);
        jPanel2.add(bDelete);
        jPanel1.add(jPanel2);
        jPanel2.add(bAdd);
        panel.add(textImage);
        panel.add(bImage);
        panel.add(label1);
        panel.add(label12);
        panel.add(textName);
        panel.add(checkAutoName);
        panel.add(jPanel4);
        panel.add(bCopy);
        panel.add(bPaste);
        panel.add(coordinateChooser);
        jPanel4.add(comboSeries);
        
        
        
        textImage.setToolTipText("Source image");
        textImage.setBounds(70, 20, 360, 20);
        textImage.setEnabled(false);
        
        bImage.setText("...");
        bImage.setBounds(430, 20, 20, 20);
        bImage.setEnabled(false);

        label1.setText("Image :");
        label1.setBounds(20, 20, 50, 16);

        label12.setText("Name :");
        label12.setBounds(20, 60, 50, 16);

        textName.setToolTipText("Name of image without extension");
        textName.setBounds(70, 60, 150, 20);
        textName.setEnabled(false);
        
        checkAutoName.setSelected(false);
        checkAutoName.setText("auto-name");
        checkAutoName.setToolTipText("Try to build a valid name for image from serie");
        checkAutoName.setBounds(240, 60, 86, 24);


        jPanel4.setLayout(null);
        jPanel4.setBorder(new TitledBorder("S\u00e9rie"));
        jPanel4.setBounds(10, 120, 350, 50);

        comboSeries.setBackground(new java.awt.Color(255, 255, 255));
        comboSeries.setToolTipText("Target serie");
        comboSeries.setBounds(20, 20, 320, 20);

        bCopy.setText("Copy");
        bCopy.setBounds(390, 130, 70, 20);
        bCopy.setVisible(false);
        
        bPaste.setText("Paste");
        bPaste.setBounds(390, 150, 70, 20);
        bPaste.setVisible(false);
        
        coordinateChooser.setBounds(10, 170, 490, 190);
        coordinateChooser.setSelectorVisible(CoordinateChooser.RESOLUTION, false);
        coordinateChooser.setTimeZone(TimeZone.getTimeZone("UTC"));
        coordinateChooser.setTimeRange(new Date(), new Date());
        
        panel.setBounds(10, 200, 510, 375);                
        panel.setLayout(null);
        panel.setBorder(new EtchedBorder());

        
        bInsert.setText("Insert");
        bInsert.setBounds(350, 590, 81, 26);

        bExit.setText("Exit");
        bExit.setBounds(440, 590, 81, 26);

        jPanel1.setLayout(null);

        jPanel2.setBounds(10, 10, 510, 170);
        jPanel2.setLayout(null);
        jPanel2.setBorder(new EtchedBorder());

        listImages.setBorder(new LineBorder(new Color(0, 0, 0)));
        listImages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listImages.addListSelectionListener(this);
        
        jScrollPane1.setViewportView(listImages);
        jScrollPane1.setBounds(23, 40, 390, 110);
        
        label13.setText("File to insert :");
        label13.setBounds(20, 20, 90, 16);

        bAdd.setText("Add");
        bAdd.setBounds(420, 50, 81, 26);

        bDelete.setText("Delete");
        bDelete.setEnabled(false);        
        bDelete.setBounds(420, 80, 81, 26);

        getContentPane().add(jPanel1, BorderLayout.CENTER);
        pack();
        setSize(535,645);
        setResizable(false);        
        
        
        final Dimension size = getToolkit().getScreenSize();
        setLocation((int)(size.getWidth()/2 - getSize().getWidth()/2) , 
                    (int)(size.getHeight()/2 - getSize().getHeight()/2));        
    }

    /**
     * Delete an image from 'listImages'.
     */
    private void deleteImages() {
        if (listImages.getSelectedIndex() == -1) {
            return;
        }        
        
        final int selectIndex      = listImages.getSelectedIndex();                
        final DefaultListModel model = (DefaultListModel)listImages.getModel();
        
        // Delete each selected image.
        vRecord.remove(selectIndex);
        model.remove(selectIndex);
        listImages.setModel(model);
        if (model.size()>0) {
            if (selectIndex<model.size()) {
                listImages.setSelectedIndex(selectIndex);
            } else {
                listImages.setSelectedIndex(Math.min(selectIndex, model.size()-1));
            }
        }       
        update();
    }
    
    /**
     * Change image.
     */
    private final void changeImage() {
        final int index = listImages.getSelectedIndex();
        
        if (index == -1) {
            return;
        }
        final Record record = (Record)vRecord.get(index);
        
        // Image to load.
        final JFileChooser chooser = new JFileChooser(textImage.getText());
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            record.file = (new File(chooser.getCurrentDirectory() +  File.separator +  chooser.getSelectedFile().getName())).toString();
            updateInformation();
        }
    }

    /**
     * Add an image to 'listImages'.
     */
    private void addImages() {        
        final JFileChooser chooser = new JFileChooser(textImage.getText());
        chooser.setMultiSelectionEnabled(true);
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File[] files = chooser.getSelectedFiles();
            
            // Process each file.
            for (int i=0 ; i<files.length ; i++) {
                final File file = files[i];
                final ListModel listModel = listImages.getModel();
                final DefaultListModel defaultModel;            
                if (!(listModel instanceof DefaultListModel)) {
                    defaultModel = new DefaultListModel();
                } else {
                    defaultModel = (DefaultListModel)listModel;            
                }

                defaultModel.addElement(file.toString());
                listImages.setModel(defaultModel);
                
                // Create a record.
                final Record record = new Record();
                record.file = file.toString();
                
                // extrat name without extension.
                record.serie    = (String)comboSeries.getSelectedItem();
                record.start    = coordinateChooser.getStartTime();
                record.end      = coordinateChooser.getEndTime();
                record.autoname = checkAutoName.isSelected();
                record.name     = computeName(record.autoname, file, record.end, record.serie);
                record.timeZone = coordinateChooser.getTimeZone();
                record.area     = coordinateChooser.getGeographicArea();
                computeDate(record);
                vRecord.add(record);                
                listImages.setSelectedIndex(listImages.getModel().getSize()-1);
            }
        }
        update();
    }

    /**
     * compute Start and end date.
     */
    private final void computeDate(final Record record) {
        final String name = new File(record.file).getName();
        final String[] patterns = {"SSTCA[0-9]{8}.png", //SSTCA20031021.png : SST 5 jours
                                   "SSTGY[0-9]{7}.png", //SSTGY2003296.png  : SST 5 jours
                                   "SSTRE[0-9]{8}.png"};//SSTRE20021104.png : SST 5 jours

        final Calendar start = new GregorianCalendar(TimeZone.getTimeZone("UTC")),
                       end   = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        start.clear();
        end.clear();
        start.setTime(record.start);
        end.setTime(record.end);

        boolean find;
        int year  = -1,
            month = -1,
            day   = -1,
            jday  = -1;
        long previousStep = -1,
             nextStep     = -1;
        
        if (Pattern.matches(patterns[0], name) || Pattern.matches(patterns[2], name)) {
            year  = Integer.parseInt(name.substring(5, 9));
            month = Integer.parseInt(name.substring(9, 11)) - 1;
            day   = Integer.parseInt(name.substring(11, 13));            
            
            previousStep = (2*24 - 2) *60*60*1000;
            nextStep     = (3*24 + 2) *60*60*1000;
            
            find = true;
        } else if (Pattern.matches(patterns[1], name)) {
            year  = Integer.parseInt(name.substring(5, 9));
            jday  = Integer.parseInt(name.substring(9, 12));

            previousStep = (2*24 - 2) *60*60*1000;
            nextStep     = (3*24 + 2) *60*60*1000;
            
            find = true;
        } else {
            find = false;
        }        
    
        // Fin a pattern matching with file.
        if (find) {
            start.clear();
            end.clear();            
            
            if (year != -1) {
                start.set(Calendar.YEAR, year);
                end.set(Calendar.YEAR, year);
            }
            if (month != -1) {
                start.set(Calendar.MONTH, month);
                end.set(Calendar.MONTH, month);                
            }
            if (day != -1) {
                start.set(Calendar.DAY_OF_MONTH, day);
                end.set(Calendar.DAY_OF_MONTH, day);        
            }            
            if (jday != -1) {
                start.set(Calendar.DAY_OF_YEAR, jday);
                end.set(Calendar.DAY_OF_YEAR, jday);        
            }
            
            record.start    = new Date(start.getTimeInMillis() - previousStep);
            record.end      = new Date(end.getTimeInMillis()   + nextStep);
            record.timeZone = TimeZone.getTimeZone("UTC");
            
            if (Pattern.matches(patterns[2], name)) {
                record.serie = "SST (Réunion - nouvelle chaîne)";
            } else if (Pattern.matches(patterns[1], name)) {
                record.serie = "SST (Guyane - synthèse 5 jours)";               
            } else if (Pattern.matches(patterns[0], name)) {
                record.serie = "SST (Canaries - synthèse 5 jours)";
            }
        }
    }
    
    /**
     * return an automatically computed 'name'.
     * @return an automatically computed 'name'.
     */
     private final String computeName(final boolean checkAutoName, final File file, final Date date, final String serie) {
         // Try to automatically compute 'name'.
        if (checkAutoName) {
            final String key = serie;
            if (map.containsKey(key)) {
                final String prefix = (String)map.get(key);
                if (prefix.length()>0) {
                    // Build a name : OBJECT + YY + JJJ
                    final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                    calendar.clear();
                    calendar.setTime(date);
                    final int year = calendar.get(Calendar.YEAR),
                              jd   = calendar.get(Calendar.DAY_OF_YEAR);
                    final String textYear = Integer.toString(year);
                    final String textJD = Integer.toString(jd);
                    final StringBuffer buffer = new StringBuffer(prefix + textYear.substring(2));
                    for (int i=textJD.length() ; i<3 ; i++) {
                        buffer.append("0");
                    }
                    buffer.append(textJD);
                    return buffer.toString();
                }
            } 
        } 
        
        // Compute name from 'file' (image source).
        final String name = file.getName();
        final int index1 = name.indexOf(".");
        if (index1 != -1) {
            return name.substring(0, index1);                               
        }          
        
        // Else return "".
        return "";
     }
    
    /**
     * Update list.
     */ 
    private final void update() {           
        // Update listImages from vRecord.
        final int index = listImages.getSelectedIndex();
        
        final DefaultListModel model = new DefaultListModel();
        for (int i=0 ; i<vRecord.size() ; i++) {
            model.addElement(((Record)vRecord.get(i)).file);
        }
        listImages.setModel(model);
        
        final int size = listImages.getModel().getSize();
        if (index != -1) {            
            listImages.setSelectedIndex(Math.min(size-1, index));
        } else {
            if (size > 0) {
                listImages.setSelectedIndex(0);
            }
        }

        if (model.size() == 0) {
            textName.setText("");
            textImage.setText("");
        }
        
        // Update button.
        if (model.getSize() == 0) {
            bDelete.setEnabled(false);
            bImage.setEnabled(false);
            textImage.setEnabled(false);
            textName.setEnabled(false);
            textImage.setText("");
        } else {
            bDelete.setEnabled(true);
            bImage.setEnabled(true);
            textImage.setEnabled(true);
            textName.setEnabled(true);
        }        
    };
    
    /**
     * Update visibleinformation.
     */
    private final void updateInformation() {                
        // Update area, hours, ...
        final int selectedIndex = listImages.getSelectedIndex();
        if (selectedIndex != -1) {
            coordinateChooser.removeChangeListener(CoordinateChooser.GEOGRAPHIC_AREA, this);
            coordinateChooser.removeChangeListener(CoordinateChooser.TIME_RANGE, this);            
            
            final Record record = (Record)vRecord.get(selectedIndex);            
            textName.setText(record.name);
            textImage.setText(record.file);
            coordinateChooser.removeChangeListener(CoordinateChooser.GEOGRAPHIC_AREA, this);
            coordinateChooser.removeChangeListener(CoordinateChooser.TIME_RANGE, this);
            
            coordinateChooser.setTimeZone(record.timeZone);
            coordinateChooser.setTimeRange(record.start, record.end);
            coordinateChooser.setGeographicArea(record.area);
            checkAutoName.setSelected(record.autoname);
            coordinateChooser.addChangeListener(CoordinateChooser.GEOGRAPHIC_AREA, this);
            coordinateChooser.addChangeListener(CoordinateChooser.TIME_RANGE, this);            
            comboSeries.setSelectedItem(record.serie);
        }
    }
    
    /**
     * Update record.
     */
    private final void updateRecord() {
        final int index = listImages.getSelectedIndex();
        if (index != -1) {
            final Record record = (Record)vRecord.get(index);
            record.area  = coordinateChooser.getGeographicArea();
            record.file  = textImage.getText();
            record.name  = textName.getText();
            record.autoname = checkAutoName.isSelected();
            record.serie = (String)comboSeries.getSelectedItem();
            record.end   = coordinateChooser.getEndTime();
            record.start = coordinateChooser.getStartTime();
            record.timeZone = coordinateChooser.getTimeZone();           
            if (record.autoname) {
                record.name = computeName(record.autoname, new File(record.file), record.end, record.serie);
            }
        }
    }
    
    /**
     * Print error.
     */
    private final void showExceptionMonitor(final Throwable e) {
        ExceptionMonitor.show(this, e);                
    }
    
    /**
     * Insert all record to database. When an insertion has been realised, 
     * the record is deleted from vRecord.
     */
    private final void insertToDataBase() {
        progress.setIndeterminate(true);         
        SwingUtilities.invokeLater(threadProgress);
        
        final Thread thread = new Thread(new Runnable(){            
            public void run() { 
                try {
                    while (true && vRecord.size()>0) {                                
                        final Record record = (Record)vRecord.get(0);
                        progress.setTitle("Insert '" + record.name + "' in database Image.");
                        listImages.setSelectedIndex(0);

                        // Connection to database.
                        final Updater updater = new Updater();
                        updater.insertToDataBase(new File(record.file), 
                                                     record.name,
                                                     record.serie,
                                                     record.start,
                                                     record.end, 
                                                     record.area);                
                        vRecord.remove(0);
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                update();
                            }
                        });                         
                    }
                    progress.dispose();
                }
                catch (Exception e) {            
                    progress.dispose();
                    showExceptionMonitor(e);
                }
                
        }});        
        thread.setPriority(Thread.NORM_PRIORITY - 2);
        thread.start();                    
    }
 

    ///////////////////////////////////////////////////////////////////////////
    ////////////                                                   ////////////
    ////////////                    LISTENER                       ////////////
    ////////////                                                   ////////////
    ///////////////////////////////////////////////////////////////////////////        
    /**
     * Invoked when the target of the listener has changed its state. 
     */
    public void stateChanged(final ChangeEvent e) {
        updateRecord();
        updateInformation();
    }
          
    /**
     *
     */
    public void valueChanged(final ListSelectionEvent e) {
    }    
    
    
    ///////////////////////////////////////////////////////////////////////////
    ////////////                                                   ////////////
    ////////////                        MAIN                       ////////////
    ////////////                                                   ////////////
    ///////////////////////////////////////////////////////////////////////////        
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            new CoverageUpdater().show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}