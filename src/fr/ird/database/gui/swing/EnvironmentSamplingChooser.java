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
package fr.ird.database.gui.swing;

// J2SE et JAI
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import javax.media.jai.KernelJAI;

// geotools
import org.geotools.resources.XArray;
import org.geotools.resources.SwingUtilities;
import org.geotools.resources.MonolineFormatter;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.gui.swing.CoordinateChooser;
import org.geotools.gui.swing.GradientKernelEditor;
import org.geotools.gui.swing.DisjointLists;
import org.geotools.gui.swing.LoggingPanel;

// Seagis
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.sample.SampleTable;
import fr.ird.database.sample.ParameterEntry;
import fr.ird.database.sample.OperationEntry;
import fr.ird.database.sample.RelativePositionEntry;
import fr.ird.database.sample.EnvironmentTableFiller;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Interface graphique pour lancer l'exécution de {@link EnvironmentTableFiller}.
 * Cette interface présente trois colonnes dans lesquelles l'utilisateur peut sélectionner
 * les séries temporelles, les décalages spatio-temporels et une opération à appliquer sur
 * les données.  Des cases à cocher, &quot;Autoriser un filtrage des données manquantes&quot;
 * et &quot;Autoriser les interpolations&quot;, activeront les opérations suivantes:
 *
 * <ul>
 *   <li>Remplissage de quelques données manquantes avec l'opération &quot;NodataFilter&quot;.</li>
 *   <li>Interpolation bicubique ou bilinéare (si possible) avec l'opération &quot;Interpolate&quot;.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class EnvironmentSamplingChooser extends JPanel {
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
     * Liste des positions spatio-temporelles relatives à utiliser.
     */
    private final JList positions;

    /**
     * Indique si le filtrage des données manquantes est permis.
     */
    private final JCheckBox nodataFilterAllowed;

    /**
     * Indique si les interpolations spatio-temporelles sont permises.
     */
    private final JCheckBox interpolationAllowed;

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
    public EnvironmentSamplingChooser() throws SQLException {
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
    public EnvironmentSamplingChooser(final EnvironmentTableFiller filler) throws SQLException {
        super(new BorderLayout());
        this.filler = filler;
        series      = new DisjointLists();
        operation   = new JComboBox(filler.getOperations().toArray());
        column      = new JTextField();
        coordinates = new CoordinateChooser();
        positions   = new JList(filler.getRelativePositions().toArray());
        kernels     = new GradientKernelEditor();

        final Resources resources = Resources.getResources(getDefaultLocale());
        nodataFilterAllowed  = new JCheckBox("Autoriser un filtrage de données manquantes", true);
        interpolationAllowed = new JCheckBox("Autoriser les interpollations", true);
        series     .setToolTipText("Paramètres environnementaux à utiliser");
        operation  .setToolTipText("Opération à appliquer sur les images");
        column     .setToolTipText("Colonne de destination dans la base de données");
        coordinates.setToolTipText("Coordonnées spatio-temporelles des captures à prendre en compte");
        positions  .setToolTipText("Décalage de temps (en jours) par rapport à la date de chaque capture");

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
            c.insets.top=3; c.insets.right=6; c.insets.left=6;
            c.gridx=0; c.gridy=0; c.gridwidth=4; c.weighty=1; panel.add(series, c);
            c.gridy=1; c.weighty=0; c.gridwidth=1;
            c.gridx=0; c.weightx=0; c.insets.right=0; panel.add(new JLabel("Opération: "), c);
            c.gridx=2;                                panel.add(new JLabel("Colonne: "), c);
            c.gridx=1; c.weightx=1; c.insets.right=6; panel.add(operation, c);
            c.gridx=3; c.weightx=0.5;                 panel.add(column, c);
            c.gridy=2; c.insets.top=0;
            c.gridx=1; c.weightx=1; c.gridwidth=3;    panel.add(nodataFilterAllowed,  c);
            c.gridy=3; c.insets.bottom=3;             panel.add(interpolationAllowed, c);
            tabs.addTab("Séries", panel);
        }
        /////////////////////////////////////////////////////////////
        ////////    Onglet Coordonnées spatio-temporelles    ////////
        /////////////////////////////////////////////////////////////
        if (true) {
            final JPanel panel = new JPanel(new BorderLayout());
            if (true) {
                Utilities.selectDefaultPositions(positions);
                final JPanel timePanel = new JPanel(new BorderLayout());
                timePanel.add(new JLabel("Décalages en jours"), BorderLayout.NORTH);
                timePanel.add(new JScrollPane(positions),       BorderLayout.CENTER);
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
     * Les décalages sont sélectionnés par l'utilisateur.
     */
    private RelativePositionEntry[] getRelativePositions() {
        final Object[] items = positions.getSelectedValues();
        final RelativePositionEntry[] positions = new RelativePositionEntry[items.length];
        for (int i=0; i<items.length; i++) {
            positions[i] = (RelativePositionEntry) items[i];
        }
        return positions;
    }

    /**
     * Appelée chaque fois que l'utilisateur choisit une nouvelle opération.
     */
    private void operationSelected(final JTabbedPane tabs, final int tabIndex) {
        final OperationEntry op = (OperationEntry) operation.getSelectedItem();
        final String  name = op.getProcessorOperation();
        tabs.setEnabledAt(tabIndex, (name!=null) && name.equalsIgnoreCase("GradientMagnitude"));
        column.setText(op.getColumn());
    }

    /**
     * Fait apparaître la boîte de dialogue proposant de remplir la table des paramètres
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

            final String                column = this.column.getText();
            final boolean         nodataFilter = nodataFilterAllowed.isSelected();
            final OperationEntry     operation = (OperationEntry) this.operation.getSelectedItem();
            final Map<String,Object> arguments = new HashMap<String,Object>();
            arguments.put("mask1", kernels.getHorizontalEditor().getKernel());
            arguments.put("mask2", kernels.getVerticalEditor().getKernel());
            if (nodataFilter) {
                GridCoverageProcessor.initialize();
            }
            filler.setInterpolationAllowed(interpolationAllowed.isSelected());
            filler.getSeries().retainAll(series.getSelectedElements());
            filler.getRelativePositions().retainAll(Arrays.asList(positions.getSelectedValues()));
            filler.getOperations().clear();
            filler.getOperations().add(new OperationEntry.Proxy(operation) {
                /** Retourne le nom de l'opération à appliquer. */
                public String getProcessorOperation() {
                    String name = super.getProcessorOperation();
                    if (!nodataFilter) {
                        return name;
                    }
                    if (name==null || (name=name.trim()).length()==0) {
                        return GridCoverageProcessor.NODATA_FILTER;
                    }
                    return GridCoverageProcessor.NODATA_FILTER + '-' + name;
                }

                /** Retourne le nom de la colonne dans laquelle écrire le résultat. */
                public String getColumn() {
                    return column;
                }

                /** Retourne la valeur d'un paramètre de l'opération. */
                public Object getParameter(final String name) {
                    return arguments.get(name);
                }
            });
            try {
                final SampleTable samples = filler.getSampleTable();
                samples.setTimeRange(coordinates.getStartTime(), coordinates.getEndTime());
                samples.setGeographicArea(coordinates.getGeographicArea());
                filler.run();
            } catch (Exception exception) {
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
        MonolineFormatter.init("fr.ird");
        final EnvironmentSamplingChooser panel = new EnvironmentSamplingChooser();
        panel.showDialog(null);
        panel.dispose();
        System.exit(0);
    }
}
