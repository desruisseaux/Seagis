/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.sql.fishery.fill;

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

// Utilities
import java.util.Set;
import java.util.prefs.Preferences;

// Geotools dependencies
import org.geotools.resources.SwingUtilities;
import org.geotools.gui.swing.ProgressWindow;
import org.geotools.gui.swing.ExceptionMonitor;

// Seagis dependencies
import fr.ird.sql.fishery.FisheryDataBase;
import fr.ird.sql.fishery.EnvironmentTable;


/**
 * Panneau de configuration permettant d'extraire des données de la table d'environnement
 * et de les copier vers une autre table dans une autre base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ExtractorControlPanel extends JPanel {
    /**
     * Les préférences de l'utilisateur.
     */
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ExtractorControlPanel.class);

    /**
     * Le nom sous lequel enregistrer l'URL de la base de données dans les préférences.
     */
    private static final String KEY_DATABASE = "ExtractTo";

    /**
     * Le de la requête à utiliser pour obtenir les données de pêches.
     */
    private static final String KEY_CATCHTABLE = "CatchTable";
    
    /**
     * Liste des paramètres. Il s'agit de codes apparaissant dans la table d'environnement
     * tel que "SST", "EKP", etc.
     */
    private final JList parameters;

    /**
     * Décalage dans le temps. Il peut s'agir de valeurs tels que +5, 0, -5, -10, etc.
     */
    private final JList timeOffset;

    /**
     * Liste des opérations. Chaque opération correspond à un nom de colonne dans la table
     * d'environnement, tel que "valeur", "sobel3", "isotrope3", etc.
     */
    private final JList operations;

    /**
     * Indique si les valeurs nulles doivent être incluses dans la requête.
     */
    private final JCheckBox nullIncluded = new JCheckBox("Inclure les valeurs nulles");

    /**
     * Table des captures à joindre.
     */
    private final JTextField catchTable = new JTextField(PREFERENCES.get(KEY_CATCHTABLE, ""));

    /**
     * Le nom de la table dans laquelle écrire le résultat.
     */
    private final JTextField table = new JTextField();

    /**
     * L'adresse URL de la connection vers la base de données de destination.
     */
    private final JTextField database = new JTextField(PREFERENCES.get(KEY_DATABASE, "jdbc:odbc:SEAS-Extractions"));

    /**
     * La table des paramètres environnementaux.
     */
    private final EnvironmentTable environment;

    /**
     * Construit un paneau pour la table d'environnement spécifié.
     *
     * @param  environment La table d'environnement.
     * @throws SQLException si une interrogation de la base de données a échouée.
     */
    public ExtractorControlPanel(final EnvironmentTable environment) throws SQLException {
        super(new BorderLayout(0,3));
        this.environment = environment;
        /*
         * Initialise les listes.
         */
        parameters = new JList(environment.getAvailableParameters().toArray());
        operations = new JList(environment.getAvailableOperations().toArray());
        timeOffset = new JList(getAvailableTimeOffsets());
        /*
         * Construit l'interface utilisateur.
         */
        setBorder(BorderFactory.createEmptyBorder(9,9,9,9));
        JComponent panel = new JPanel(new GridLayout(1,3));
        panel.add(new JLabel("Paramètres", JLabel.CENTER));
        panel.add(new JLabel("Temps",      JLabel.CENTER));
        panel.add(new JLabel("Opérations", JLabel.CENTER));
        add(panel, BorderLayout.NORTH);

        panel = new JPanel(new GridLayout(1,3, 6,6));
        panel.add(new JScrollPane(parameters));
        panel.add(new JScrollPane(timeOffset));
        panel.add(new JScrollPane(operations));
        add(panel, BorderLayout.CENTER);

        final JComponent fields = new JPanel(new BorderLayout());
        final JLabel catchLabel = new JLabel("Joindre les captures:");
        catchLabel.setBorder(BorderFactory.createEmptyBorder(0,18,0,12));
        fields.add(nullIncluded, BorderLayout.NORTH);
        fields.add(catchLabel,   BorderLayout.WEST);
        fields.add(catchTable,   BorderLayout.CENTER);

        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(12,0,0,0),
                        BorderFactory.createTitledBorder("Destination")));
        final GridBagConstraints c = new GridBagConstraints();
        c.fill=c.HORIZONTAL; c.gridx=0; c.insets.left=12;
        c.gridy=0; panel.add(new JLabel("Table:"), c);
        c.gridy=1; panel.add(new JLabel("Base de données:"), c);
        c.weightx=1; c.gridx=1; c.insets.left=6;
        c.gridy=0; panel.add(table,    c);
        c.gridy=1; panel.add(database, c);
        fields.add(panel, BorderLayout.SOUTH);
        add(fields, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(400,300));
    }

    /**
     * Retourne les décalage dans le temps disponible.
     */
    private static final Number[] getAvailableTimeOffsets() {
        int offset = 5;
        final Number[] values = new Number[36];
        for (int i=0; i<values.length; i++) {
            values[i] = new Integer(offset--);
        }
        return values;
    }

    /**
     * Configure la table des paramètres environnementaux en fonction
     * de la sélection courante de l'utilisateur.
     * 
     * @return Le nombre de paramètres qui seront écrit dans la table de destination.
     * @throws SQLException si la base de données n'a pas pu être configurée.
     */
    private int configTable() throws SQLException {
        final String    catchTable = this.catchTable.getText().trim();
        final Object[]  parameters = this.parameters.getSelectedValues();
        final Object[]  operations = this.operations.getSelectedValues();
        final Object[]  timeOffset = this.timeOffset.getSelectedValues();
        final boolean nullIncluded = this.nullIncluded.isSelected();
        for (int i=0; i<operations.length; i++) {
            for (int j=0; j<parameters.length; j++) {
                for (int k=0; k<timeOffset.length; k++) {
                    environment.addParameter(operations[i].toString(), parameters[j].toString(),
                                   EnvironmentTable.CENTER, ((Number)timeOffset[k]).intValue(),
                                   nullIncluded);
                }
            }
        }
        PREFERENCES.put(KEY_CATCHTABLE, catchTable);
        environment.setCatchTable(catchTable.length()!=0 ? catchTable : null);
        return parameters.length * operations.length * timeOffset.length;
    }

    /**
     * Fait apparaître la boîte de dialogue et lance l'exportation si l'utilisateur
     * a cliqué sur "Ok".
     *
     * @param  owner La fenêtre parente, or <code>null</code>.
     * @throws SQLException si une interrogation de la base de données a échouée.
     */
    public void showAndStart(final Component owner) throws SQLException {
        while (SwingUtilities.showOptionDialog(owner, this, "Extraction de paramètres environnementaux")) {
            final String table    = this.table   .getText().trim();
            final String database = this.database.getText().trim();
            if (table.length()==0 || database.length()==0) {
                SwingUtilities.showMessageDialog(owner,
                        "Une table et une base de données de destination doivent être spécifiées.",
                        "Paramètres incorrects", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (configTable() == 0) {
                SwingUtilities.showMessageDialog(owner,
                        "Des paramètres, opérations et interval de temps doivent être sélectionnés.",
                        "Paramètres incorrects", JOptionPane.ERROR_MESSAGE);
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
    }

    /**
     * Affiche une fenêtre demandant à l'utilisateur de sélectionner des paramètres
     * et lance l'exportation s'il clique sur "Ok".
     */
    public static void main(final String[] args) {
        try {
            final FisheryDataBase database = new FisheryDataBase();
            final ExtractorControlPanel panel = new ExtractorControlPanel(database.getEnvironmentTable());
            panel.showAndStart(null);
            database.close();
        } catch (SQLException exception) {
            ExceptionMonitor.show(null, exception);
        }
        System.exit(0);
    }
}
