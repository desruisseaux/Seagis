/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.database.gui.swing;

// User interface
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;

// Database
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.rmi.RemoteException;

// Utilities
import java.util.Set;
import java.util.prefs.Preferences;

// Geotools dependencies
import org.geotools.resources.MonolineFormatter;
import org.geotools.resources.SwingUtilities;
import org.geotools.gui.swing.ProgressWindow;
import org.geotools.gui.swing.ExceptionMonitor;

// Seagis dependencies
import fr.ird.database.CatalogException;
import fr.ird.database.sample.ParameterEntry;
import fr.ird.database.sample.OperationEntry;
import fr.ird.database.sample.EnvironmentTable;
import fr.ird.database.sample.RelativePositionEntry;
import fr.ird.database.sample.sql.SampleDataBase;


/**
 * Panneau de configuration permettant d'extraire des donn�es de la table d'environnement
 * et de les copier vers une autre table dans une autre base de donn�es. La copie se fait
 * en puisant les donn�es � partir de {@link EnvironmentTable#getRowSet}, ce qui permet
 * de placer sur une seule ligne un nombre arbitraire de param�tres.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleRowSetChooser extends JPanel {
    /**
     * Les pr�f�rences de l'utilisateur.
     */
    private static final Preferences PREFERENCES = Preferences.userRoot().node("/fr/ird/database/gui");

    /**
     * Le nom sous lequel enregistrer l'URL de la base de donn�es dans les pr�f�rences.
     */
    private static final String KEY_DATABASE = "ExtractTo";

    /**
     * Le de la requ�te � utiliser pour obtenir les donn�es de p�ches.
     */
    private static final String KEY_SAMPLETABLE = "SampleTable";
    
    /**
     * Liste des param�tres. Il s'agit de codes apparaissant dans la table d'environnement
     * tel que "SST", "EKP", etc.
     */
    private final JList parameters;

    /**
     * Liste des op�rations. Chaque op�ration correspond � un nom de colonne dans la table
     * d'environnement, tel que "valeur", "sobel3", "isotrope3", etc.
     */
    private final JList operations;

    /**
     * Positions spatio-temporelles relatives.
     * Il peut s'agir de valeurs tels que +5, 0, -5, -10, etc.
     */
    private final JList positions;

    /**
     * Indique si les valeurs nulles doivent �tre incluses dans la requ�te.
     */
    private final JCheckBox nullIncluded = new JCheckBox("Inclure les valeurs nulles");

    /**
     * Table des �chantillons � joindre.
     */
    private final JTextField sampleTable = new JTextField(PREFERENCES.get(KEY_SAMPLETABLE, ""));

    /**
     * Le nom de la table dans laquelle �crire le r�sultat.
     */
    private final JTextField table = new JTextField();

    /**
     * L'adresse URL de la connection vers la base de donn�es de destination.
     */
    private final JTextField database = new JTextField(PREFERENCES.get(KEY_DATABASE, "jdbc:odbc:SEAS-Extractions"));

    /**
     * La table des param�tres environnementaux.
     */
    private final EnvironmentTable environment;

    /**
     * Construit un paneau pour la table d'environnement sp�cifi�.
     *
     * @param  environment La table d'environnement.
     * @throws SQLException si une interrogation de la base de donn�es a �chou�e.
     */
    public SampleRowSetChooser(final EnvironmentTable environment) throws RemoteException {
        super(new BorderLayout(0,3));
        this.environment = environment;
        /*
         * Initialise les listes.
         */
        parameters = new JList(environment.getAvailableParameters().toArray());
        operations = new JList(environment.getAvailableOperations().toArray());
        positions  = new JList(environment.getAvailablePositions ().toArray());
        Utilities.displayOperationColumnName(operations);
        Utilities.selectDefaultPositions(positions);
        /*
         * Construit l'interface utilisateur.
         */
        setBorder(BorderFactory.createEmptyBorder(9,9,9,9));
        JComponent panel = new JPanel(new GridLayout(1,3));
        panel.add(new JLabel("Param�tres", JLabel.CENTER));
        panel.add(new JLabel("Op�rations", JLabel.CENTER));
        panel.add(new JLabel("Temps",      JLabel.CENTER));
        add(panel, BorderLayout.NORTH);

        panel = new JPanel(new GridLayout(1,3, 6,6));
        panel.add(new JScrollPane(parameters));
        panel.add(new JScrollPane(operations));
        panel.add(new JScrollPane(positions ));
        add(panel, BorderLayout.CENTER);

        final JComponent fields = new JPanel(new BorderLayout());
        final JLabel catchLabel = new JLabel("Joindre les captures:");
        catchLabel.setBorder(BorderFactory.createEmptyBorder(0,18,0,12));
        fields.add(nullIncluded, BorderLayout.NORTH);
        fields.add(catchLabel,   BorderLayout.WEST);
        fields.add(sampleTable,  BorderLayout.CENTER);

        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(12,0,0,0),
                        BorderFactory.createTitledBorder("Destination")));
        final GridBagConstraints c = new GridBagConstraints();
        c.fill=c.HORIZONTAL; c.gridx=0; c.insets.left=12;
        c.gridy=0; panel.add(new JLabel("Table:"), c);
        c.gridy=1; panel.add(new JLabel("Base de donn�es:"), c);
        c.weightx=1; c.gridx=1; c.insets.left=6;
        c.gridy=0; panel.add(table,    c);
        c.gridy=1; panel.add(database, c);
        fields.add(panel, BorderLayout.SOUTH);
        add(fields, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(400,300));
    }

    /**
     * Configure la table des param�tres environnementaux en fonction
     * de la s�lection courante de l'utilisateur.
     * 
     * @return Le nombre de param�tres qui seront �crit dans la table de destination.
     * @throws SQLException si la base de donn�es n'a pas pu �tre configur�e.
     */
    private int configTable() throws RemoteException {
        final String   sampleTable = this.sampleTable.getText().trim();
        final Object[]  parameters = this.parameters .getSelectedValues();
        final Object[]  operations = this.operations .getSelectedValues();
        final Object[]  positions  = this.positions  .getSelectedValues();
        final boolean nullIncluded = this.nullIncluded.isSelected();
        for (int i=0; i<parameters.length; i++) {
            for (int j=0; j<operations.length; j++) {
                for (int k=0; k<positions .length; k++) {
                    environment.addParameter((ParameterEntry)        parameters[i],
                                             (OperationEntry)        operations[j],
                                             (RelativePositionEntry) positions [k],
                                             nullIncluded);
                }
            }
        }
        PREFERENCES.put(KEY_SAMPLETABLE, sampleTable);
        environment.setSampleTable(sampleTable.length()!=0 ? sampleTable : null);
        return parameters.length * operations.length * positions.length;
    }

    /**
     * Fait appara�tre la bo�te de dialogue et lance l'exportation si l'utilisateur
     * a cliqu� sur "Ok".
     *
     * @param  owner La fen�tre parente, or <code>null</code>.
     * @throws SQLException si une interrogation de la base de donn�es a �chou�e.
     */
    public void showAndStart(final Component owner) throws RemoteException {
        try {
            while (SwingUtilities.showOptionDialog(owner, this, "Extraction de param�tres environnementaux")) {
                final String table    = this.table   .getText().trim();
                final String database = this.database.getText().trim();
                if (table.length()==0 || database.length()==0) {
                    SwingUtilities.showMessageDialog(owner,
                            "Une table et une base de donn�es de destination doivent �tre sp�cifi�es.",
                            "Param�tres incorrects", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (configTable() == 0) {
                    SwingUtilities.showMessageDialog(owner,
                            "Des param�tres, op�rations et interval de temps doivent �tre s�lectionn�s.",
                            "Param�tres incorrects", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                PREFERENCES.put(KEY_DATABASE, database);
                final Connection   connection = DriverManager.getConnection(database);
                final ProgressWindow progress = new ProgressWindow(owner);
                try {
                    environment.copyToTable(connection, table, progress);
                } finally {
                    progress.dispose();
                    connection.close();
                }
                break;
            }
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Affiche une fen�tre demandant � l'utilisateur de s�lectionner des param�tres
     * et lance l'exportation s'il clique sur "Ok".
     */
    public static void main(final String[] args) {
        MonolineFormatter.init("fr.ird");
        try {
            final SampleDataBase   database = new SampleDataBase();
            final SampleRowSetChooser panel = new SampleRowSetChooser(database.getEnvironmentTable(null));
            panel.showAndStart(null);
            database.close();
        } catch (RemoteException exception) {
            ExceptionMonitor.show(null, exception);
        }
        System.exit(0);
    }
}
