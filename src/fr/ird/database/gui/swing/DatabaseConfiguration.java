package fr.ird.database.gui.swing;

// J2SE.
import java.io.File;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;

// Geotools
import org.geotools.resources.SwingUtilities;

// Seagis.
import fr.ird.database.coverage.sql.CoverageDataBase;
import fr.ird.database.sample.sql.SampleDataBase;

/**
 * Interface de configuration des fichiers définissant les bases de données à 
 * utilisées. 
 *
 * Il est nécessaire de définir les fichiers de configuration à utiliser pour 
 * se connecter et interroger les Images et Observations.
 *
 * @author Remi Eve
 */
public class DatabaseConfiguration extends JPanel {
        
    /**
     * Editeurs de requêtes SQL. Par convention, le premier éditeur de
     * cette liste devrait être pour la base de données d'images (parce
     * que <code>ControlPanel</code> va y ajouter un champ "Répertoire").
     * L'ordre des autres éléments n'a pas d'importance.
     */
    private final SQLEditor[] editors = {
        CoverageDataBase.getSQLEditor(),
        SampleDataBase.getSQLEditor()
    };
    
    private final int BD_IMG = 0,
                      BD_OBS = 1;
        
    private final JLabel labels[] = new JLabel[2];    

    private final JTextField textFields[] = new JTextField[2];
        
    private final JButton     bOk = new JButton(),
                          bCancel = new JButton();    
    
    /** 
     * Constructeur. 
     */
    public DatabaseConfiguration() {
        setLayout(null);

        labels[BD_IMG] = new JLabel();
        labels[BD_IMG].setText("Base de donn\u00e9es Observations :");
        labels[BD_IMG].setBounds(0, 40, 200, 15);

        labels[BD_OBS] = new JLabel();
        labels[BD_OBS].setText("Base de donn\u00e9es Images :");
        labels[BD_OBS].setBounds(30, 10, 160, 15);
                
        textFields[BD_IMG] = new JTextField();
        textFields[BD_OBS] = new JTextField();        
        textFields[BD_IMG].setBounds(200, 10, 240, 21);
        textFields[BD_OBS].setBounds(200, 40, 240, 21);

        add(labels[BD_IMG]);        
        add(labels[BD_OBS]);
        add(textFields[BD_IMG]);
        add(textFields[BD_OBS]);        
        setBounds(10, 60, 370, 110);
        
        final Dimension size=getPreferredSize();
        size.width=450;
        size.height=80;
        setPreferredSize(size);
        
        textFields[BD_IMG].setText(CoverageDataBase.getDefaultFileOfConfiguration().toString());
        textFields[BD_OBS].setText(SampleDataBase.getDefaultFileOfConfiguration().toString());                    
    }
    
    /**
     * Affiche la boîte de dialogue.
     */
    public boolean showDialog(final Component owner) {
        if (SwingUtilities.showOptionDialog(owner, this, "Fichiers de configuration des bases de données")) {
            // Initialise les TextFields.
            CoverageDataBase.setDefaultFileOfConfiguration(new File(textFields[BD_IMG].getText()));
            SampleDataBase.setDefaultFileOfConfiguration(new File(textFields[BD_OBS].getText()));
            return true;
        }
        return false;
    }    
    
    /**
     * Main.
     */
    public static void main(final String[] args) {
        (new DatabaseConfiguration()).showDialog(null);
    }
}