/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.sql;

// Bases de données
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.fishery.FisheryDataBase;

// Préférences
import java.util.prefs.Preferences;

// Entrés/sorties
import java.io.File;
import java.io.IOException;

// Interface utilisateur
import java.awt.Dimension;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

// Evénements
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// Divers
import java.util.List;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.ArrayList;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;

// Geotools dependencies
import org.geotools.resources.SwingUtilities;


/**
 * Panneau de configuration des bases de données. Ce panneau permettra par
 * exemple de spécifier le répertoire racine des images ainsi que de modifier
 * les instructions SQL utilisées pour interroger les tables d'images et de pêches.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class ControlPanel extends JPanel
{
    /**
     * Editeurs de requêtes SQL. Par convention, le premier éditeur de
     * cette liste devrait être pour la base de données d'images (parce
     * que <code>ControlPanel</code> va y ajouter un champ "Répertoire").
     * L'ordre des autres éléments n'a pas d'importance.
     */
    private final SQLEditor[] editors=
    {
          ImageDataBase.getSQLEditor(),
        FisheryDataBase.getSQLEditor()
    };

    /**
     * Clés des titres des éditeurs SQL. Ce tableau doit avoir la
     * même longueur que {@link #editors}. (Note: statique pour
     * l'instant, peut-être dynamique dans une version future).
     */
    private static final int[] titles=
    {
        ResourceKeys.IMAGES,
        ResourceKeys.FISHERIES
    };

    /**
     * Champs contenant les pilotes JDBC pour chaque base de données.
     * Ce tableau doit avoir la même longueur que <code>editors<code>.
     */
    private final JTextField[] drivers = new JTextField[editors.length];

    /**
     * Champs contenant les source des données pour chaque base de données.
     * Ce tableau doit avoir la même longueur que <code>editors<code>.
     */
    private final JTextField[] sources = new JTextField[editors.length];

    /**
     * Champs contenant les fuseaux horaires pour chaque base de données.
     * Ce tableau doit avoir la même longueur que <code>editors<code>.
     */
    private final JComboBox[] timezones = new JComboBox[editors.length];

    /**
     * Répertoire racine des images.
     */
    private final JTextField directory=new JTextField();

    /**
     * Construit un panneau de configuration. Les paramètres
     * qui apparaîtront dans ce panneau seront puisés dans les
     * préférences du système.
     */
    public ControlPanel()
    {
        super(new GridBagLayout());
        final GridBagConstraints    c = new GridBagConstraints();
        final Resources     resources = Resources.getResources(null);
        final String   sqlQueriesText = resources.getMenuLabel(ResourceKeys.SQL_QUERIES);
        final String       driverText = resources.getLabel(ResourceKeys.JDBC_DRIVER);
        final String       sourceText = resources.getLabel(ResourceKeys.DATABASE);
        final String     timezoneText = resources.getLabel(ResourceKeys.TIMEZONE);
        final String[]    timezoneIDs = TimeZone.getAvailableIDs();
        final JLabel   directoryLabel = new JLabel(resources.getLabel(ResourceKeys.ROOT_DIRECTORY));
        directory.setText(ImageDataBase.getDefaultDirectory().getPath());
        directoryLabel.setLabelFor(directory);
        Arrays.sort(timezoneIDs);
        for (int i=0; i<editors.length; i++)
        {
            this.drivers  [i]          = new JTextField(20);
            this.sources  [i]          = new JTextField(20);
            this.timezones[i]          = new JComboBox(timezoneIDs);
            final JPanel         panel = new JPanel(new GridBagLayout());
            final JButton      editSQL = new JButton(sqlQueriesText);
            final JLabel   driverLabel = new JLabel(  driverText, JLabel.RIGHT);
            final JLabel   sourceLabel = new JLabel(  sourceText, JLabel.RIGHT);
            final JLabel timeZoneLabel = new JLabel(timezoneText, JLabel.RIGHT);
            sourceLabel  .setLabelFor(sources  [i]);
            timeZoneLabel.setLabelFor(timezones[i]);
            panel.setBorder(BorderFactory.createTitledBorder(resources.getString(titles[i])));
            c.gridy=0; c.insets.top=c.insets.bottom=0; c.weightx=0; c.fill=c.NONE; // Reset from previous loop.
            c.gridx=0; c.insets.left=12; c.insets.right=3; c.anchor=c.EAST;
            c.gridy=0; if (i==0) panel.add(directoryLabel, c);
            c.gridy=1;           panel.add(   driverLabel, c);
            c.gridy=2;           panel.add(   sourceLabel, c);
            c.gridy=3;           panel.add( timeZoneLabel, c);
            c.gridx=1; c.insets.left=0; c.weightx=1; c.fill=c.BOTH; c.anchor=c.CENTER;
            c.gridy=0; if (i==0) panel.add(directory,      c);
            c.gridy=1;           panel.add(drivers  [i],   c);
            c.gridy=2;           panel.add(sources  [i],   c);
            c.gridy=3;           panel.add(timezones[i],   c);
            c.insets.top=9; c.insets.bottom=9;
            c.gridy=4;           panel.add(editSQL,        c);
            // -------------------------------
            // Add the newly constructed panel
            // -------------------------------
            c.gridx=0; c.insets.right=0; c.insets.top=6; c.fill=c.BOTH;
            if (i==0)
            {
                c.gridy=0; c.weighty=1; c.insets.left=0;
                add(SwingUtilities.getMultilineLabelFor(panel, resources.getString(ResourceKeys.EDIT_DATABASES_CONFIGURATION)), c);
            }
            c.gridy=i+1; c.weighty=0; c.insets.left=45; c.insets.bottom=9;
            add(panel, c);
            // ---------------------------------------
            // Initialize fields according preferences
            // ---------------------------------------
            final SQLEditor editor = editors[i];
            drivers  [i].setText        (editor.getProperty(DataBase.DRIVER  ));
            sources  [i].setText        (editor.getProperty(DataBase.SOURCE  ));
            timezones[i].setSelectedItem(editor.getProperty(DataBase.TIMEZONE));
            // -------------
            // Add listeners
            // -------------
            editSQL.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent event)
                {
                    final Component source = (Component) event.getSource();
                    try
                    {
                        source.setEnabled(false);
                        editor.showDialog(ControlPanel.this);
                    }
                    finally
                    {
                        source.setEnabled(true);
                    }
                }
            });
        }
        /*
         * Un ajustement de la taille de la fenêtre a été rendu nécessaire
         * parce que Swing ne calcule pas correctement la hauteur du texte
         * multilignes.
         */
        final Dimension size=getPreferredSize();
        if (size.width<370) size.width=370;
        size.height += 54;
        setPreferredSize(size);
    }

    /**
     * Fait apparaître le panneau de configuration. Si l'utilisateur clique sur "Ok",
     * alors les configurations éditées seront sauvegardées dans les préférences du
     * système.
     *
     * @param  owner Composante par-dessus laquelle faire apparaître la boîte de dialogue.
     * @return <code>true</code> si l'utilisateur à cliqué sur "Ok", ou <code>false</code> sinon.
     */
    public boolean showDialog(final Component owner)
    {
        if (SwingUtilities.showOptionDialog(owner, this, Resources.format(ResourceKeys.DATABASES)))
        {
            final File directory = new File(this.directory.getText());
            ImageDataBase.setDefaultDirectory(directory);
            for (int i=0; i<editors.length; i++)
            {
                final Preferences preferences = editors[i].preferences;
                preferences.put(DataBase.DRIVER,   drivers[i].getText());
                preferences.put(DataBase.SOURCE,   sources[i].getText());
                preferences.put(DataBase.TIMEZONE, TimeZone.getTimeZone(timezones[i].getSelectedItem().toString()).getID());
            }
            return true;
        }
        else return false;
    }

    /**
     * Fait apparaître le panneau de configuration. Si l'utilisateur clique sur "Ok",
     * alors les configurations éditées seront sauvegardées dans les préférences du
     * système.
     */
    public static void main(final String[] args)
    {
        new ControlPanel().showDialog(null);
        System.exit(0);
    }
}
