/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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

// J2SE et JAI
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import javax.media.jai.KernelJAI;

// geotools
import org.geotools.resources.XArray;
import org.geotools.resources.SwingUtilities;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.gui.swing.CoordinateChooser;
import org.geotools.gui.swing.GradientKernelEditor;
import org.geotools.gui.swing.LoggingPanel;

// Seagis
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.awt.DisjointLists;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Interface graphique pour lancer l'exécution de {@link EnvironmentTableFiller}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class EnvironmentControlPanel extends JPanel {
    /**
     * Nom de l'opération à effectuer sur les images.
     */
    private final JComboBox operation;

    /**
     * La colonne dans laquelle écrire les données.
     */
    private final JTextField column;

    /**
     * Liste des séries à traiter.
     */
    private final DisjointLists series;

    /**
     * Liste des pas de temps à utiliser.
     */
    private final JList timeLags;

    /**
     * Coordonnées spatio-temporelles des captures à traiter.
     */
    private final CoordinateChooser coordinates;

    /**
     * Matrices à appliquer sur les données pour calculer les
     * magnitudes des grandients.
     */
    private final GradientKernelEditor kernels;

    /**
     * L'objet à utiliser pour remplir la base de données des captures.
     */
    private final EnvironmentTableFiller filler;

    /**
     * <code>true</code> si cet objet devrait fermer la connection
     * avec les bases de données lors de la fermeture.
     */
    private boolean closeOnDispose;

    /**
     * Construit l'interface en utilisant les bases de données par défaut.
     * Les bases seront automatiquement fermées lors de la destruction de
     * cet objet.
     *
     * @throws SQLException si un problème est survenu lors d'un accès à une base de données.
     */
    public EnvironmentControlPanel() throws SQLException {
        this(new EnvironmentTableFiller());
        closeOnDispose = true;
    }

    /**
     * Construit l'interface en utilisant l'objet spécifié. Ce constructeur
     * ne fermera pas les connections à la base de données de l'objet.
     *
     * @param  filler Objet à utiliser pour remplir la table des paramètres environnementaux.
     * @throws SQLException si un problème est survenu lors d'un accès à une base de données.
     */
    public EnvironmentControlPanel(final EnvironmentTableFiller filler) throws SQLException {
        super(new BorderLayout());
        this.filler = filler;
        series      = new DisjointLists();
        operation   = new JComboBox(filler.getAvailableOperations());
        column      = new JTextField();
        coordinates = new CoordinateChooser();
        timeLags    = new JList();
        kernels     = new GradientKernelEditor();

        final Resources resources = Resources.getResources(getDefaultLocale());
        series     .setToolTipText("Paramètres environnementaux à utiliser");
        operation  .setToolTipText("Opération à appliquer sur les images");
        column     .setToolTipText("Colonne de destination dans la base de données");
        coordinates.setToolTipText("Coordonnées spatio-temporelles des captures à prendre en compte");
        timeLags   .setToolTipText("Décalage de temps (en jours) par rapport à la date de chaque capture");

        final JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(520,280));
        ////////////////////////////////////////////
        ////////    Onglet Séries sources   ////////
        ////////////////////////////////////////////
        if (true) {
            series.addElements(filler.getSeries());
            series.setBorder(BorderFactory.createTitledBorder("Paramètres environnementaux"));
            final JPanel panel = new JPanel(new GridBagLayout());
            final GridBagConstraints c = new GridBagConstraints();
            c.gridheight=1; c.weightx=1; c.fill=c.BOTH;
            c.insets.top=3; c.insets.bottom=3; c.insets.right=6; c.insets.left=6;
            c.gridx=0; c.gridy=0; c.gridwidth=4; c.weighty=1; panel.add(series, c);
            c.gridy=1; c.weighty=0; c.gridwidth=1;
            c.gridx=0; c.weightx=0; c.insets.right=0; panel.add(new JLabel("Opération: "), c);
            c.gridx=2;                                panel.add(new JLabel("Colonne: "), c);
            c.gridx=1; c.weightx=1; c.insets.right=6; panel.add(operation, c);
            c.gridx=3; c.weightx=0.5;                 panel.add(column, c);
            tabs.addTab("Séries", panel);
        }
        /////////////////////////////////////////////////////////////
        ////////    Onglet Coordonnées spatio-temporelles    ////////
        /////////////////////////////////////////////////////////////
        if (true) {
            final JPanel panel = new JPanel(new BorderLayout());
            if (true) {
                // Configure la liste des décalages de temps.
                int selectedCount=0;
                final int[]         days = filler.getDaysToEvaluate();
                final List<Integer> lags = new ArrayList<Integer>();
                int[]    selectedIndices = new int[days.length];
                for (int t=5; t>=-30; t--) {
                    if (Arrays.binarySearch(days, t)>=0) {
                        selectedIndices[selectedCount++] = lags.size();
                    }
                    lags.add(new Integer(t));
                }
                selectedIndices = XArray.resize(selectedIndices, selectedCount);
                timeLags.setListData(lags.toArray());
                timeLags.setSelectedIndices(selectedIndices);
                final JPanel timePanel = new JPanel(new BorderLayout());
                timePanel.add(new JLabel("Décalages en jours"), BorderLayout.NORTH);
                timePanel.add(new JScrollPane(timeLags),        BorderLayout.CENTER);
                timePanel.setBorder(BorderFactory.createEmptyBorder(6,18,6,6));
                panel.add(timePanel, BorderLayout.CENTER);
            }
            coordinates.setSelectorVisible(CoordinateChooser.RESOLUTION, false);
            panel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            panel.add(coordinates, BorderLayout.WEST);
            tabs.addTab("Coordonnées", panel);
        }
        //////////////////////////////////////////////////
        ////////    Onglet Operateurs de Sobel    ////////
        //////////////////////////////////////////////////
        if (true) {
            kernels.addDefaultKernels();
            tabs.addTab("Gradient", kernels);
            final int tabIndex = tabs.getTabCount()-1;
            operation.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    operationSelected(tabs, tabIndex);
                }
            });
            operationSelected(tabs, tabIndex);
        }
    }

    /**
     * Retourne les jours où extraire des données, avant, pendant et après le jour de la pêche.
     * Il décalages sélectionnés par l'utilisateur.
     */
    private int[] getDaysToEvaluate() {
        final Object[] items = timeLags.getSelectedValues();
        final int[] days = new int[items.length];
        for (int i=0; i<items.length; i++) {
            days[i] = ((Number) items[i]).intValue();
        }
        return days;
    }

    /**
     * Appelée chaque fois que l'utilisateur choisit une nouvelle opération.
     */
    private void operationSelected(final JTabbedPane tabs, final int tabIndex) {
        final Operation op = (Operation) operation.getSelectedItem();
        final String  name = op.name;
        tabs.setEnabledAt(tabIndex, (name!=null) && name.equalsIgnoreCase("GradientMagnitude"));
        column.setText(op.column);
    }

    /**
     * Fait apparaître la boîte de dialogue proposant de remplir la tabke des paramètres
     * environnementaux. Si l'utilisateur clique sur "Ok", l'opération sera immédiatement
     * lancée. Cette méthode peut être appelée à partir de n'importe quel thread. La boîte
     * de dialogue sera lancée dans le thread de <cite>Swing</cite>. Si l'utilisateur clique
     * sur "ok", l'opération sera ensuite lancée dans le thread courant.
     */
    public void showDialog(final Component owner) {
        if (SwingUtilities.showOptionDialog(owner, this, "Environnement aux positions de pêches")) {
            final LoggingPanel logging = new LoggingPanel("fr.ird");
            logging.setColumnVisible(LoggingPanel.LOGGER, false);
            final Handler handler = logging.getHandler();
            Logger.getLogger("org.geotools").addHandler(handler);
            handler.setLevel(Level.FINE);
            logging.show(owner);

            final Map<String,Object> arguments = new HashMap<String,Object>();
            arguments.put("mask1", kernels.getHorizontalEditor().getKernel());
            arguments.put("mask2", kernels.getVerticalEditor().getKernel());

            filler.getSeries().retainAll(series.getSelectedElements());
            filler.setTimeRange(coordinates.getStartTime(), coordinates.getEndTime());
            filler.setGeographicArea(coordinates.getGeographicArea());
            filler.setOperation((Operation) operation.getSelectedItem(), column.getText(), arguments);
            filler.setDaysToEvaluate(getDaysToEvaluate());
            try {
                filler.run();
            } catch (SQLException exception) {
                ExceptionMonitor.show(owner, exception);
            }
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     */
    public void dispose() throws SQLException {
        if (closeOnDispose) {
            filler.close();
            closeOnDispose = false;
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Lance l'exécution de l'application.
     *
     * @throws SQLException si un problème est survenu lors d'un accès à une base de données.
     */
    public static void main(final String[] args) throws SQLException {
        final EnvironmentControlPanel panel = new EnvironmentControlPanel();
        panel.showDialog(null);
        panel.dispose();
        System.exit(0);
    }
}
