/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 * Interface graphique pour lancer l'ex�cution de {@link EnvironmentTableFiller}.
 * Cette interface pr�sente trois colonnes dans lesquelles l'utilisateur peut s�lectionner
 * les s�ries temporelles, les d�calages spatio-temporels et une op�ration � appliquer sur
 * les donn�es.  Une case � cocher, &quot;Autoriser les interpolations&quot;, activera les
 * op�rations suivantes:
 *
 * <ul>
 *   <li>Remplissage de quelques donn�es manquantes avec l'op�ration &quot;NodataFilter&quot;.</li>
 *   <li>Interpolation bicubique ou bilin�are (si possible) avec l'op�ration &quot;Interpolate&quot;.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class EnvironmentSamplingChooser extends JPanel {
    /**
     * Nom de l'op�ration � effectuer sur les images.
     */
    private final JComboBox operation;

    /**
     * La colonne dans laquelle �crire les donn�es.
     */
    private final JTextField column;

    /**
     * Liste des s�ries � traiter.
     */
    private final DisjointLists series;

    /**
     * Liste des positions spatio-temporelles relatives � utiliser.
     */
    private final JList positions;

    /**
     * Indique si les interpolations spatio-temporelles sont permises.
     */
    private final JCheckBox interpolationAllowed;

    /**
     * Coordonn�es spatio-temporelles des captures � traiter.
     */
    private final CoordinateChooser coordinates;

    /**
     * Matrices � appliquer sur les donn�es pour calculer les
     * magnitudes des grandients.
     */
    private final GradientKernelEditor kernels;

    /**
     * L'objet � utiliser pour remplir la base de donn�es des captures.
     */
    private final EnvironmentTableFiller filler;

    /**
     * <code>true</code> si cet objet devrait fermer la connection
     * avec les bases de donn�es lors de la fermeture.
     */
    private boolean closeOnDispose;

    /**
     * Construit l'interface en utilisant les bases de donn�es par d�faut.
     * Les bases seront automatiquement ferm�es lors de la destruction de
     * cet objet.
     *
     * @throws SQLException si un probl�me est survenu lors d'un acc�s � une base de donn�es.
     */
    public EnvironmentSamplingChooser() throws SQLException {
        this(new EnvironmentTableFiller());
        closeOnDispose = true;
    }

    /**
     * Construit l'interface en utilisant l'objet sp�cifi�. Ce constructeur
     * ne fermera pas les connections � la base de donn�es de l'objet.
     *
     * @param  filler Objet � utiliser pour remplir la table des param�tres environnementaux.
     * @throws SQLException si un probl�me est survenu lors d'un acc�s � une base de donn�es.
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
        interpolationAllowed = new JCheckBox("Autoriser les interpollations", true);
        series     .setToolTipText("Param�tres environnementaux � utiliser");
        operation  .setToolTipText("Op�ration � appliquer sur les images");
        column     .setToolTipText("Colonne de destination dans la base de donn�es");
        coordinates.setToolTipText("Coordonn�es spatio-temporelles des captures � prendre en compte");
        positions  .setToolTipText("D�calage de temps (en jours) par rapport � la date de chaque capture");

        final JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(520,280));
        ////////////////////////////////////////////
        ////////    Onglet S�ries sources   ////////
        ////////////////////////////////////////////
        if (true) {
            series.addElements(filler.getSeries());
            series.setBorder(BorderFactory.createTitledBorder("Param�tres environnementaux"));
            final JPanel panel = new JPanel(new GridBagLayout());
            final GridBagConstraints c = new GridBagConstraints();
            c.gridheight=1; c.weightx=1; c.fill=c.BOTH;
            c.insets.top=3; c.insets.right=6; c.insets.left=6;
            c.gridx=0; c.gridy=0; c.gridwidth=4; c.weighty=1; panel.add(series, c);
            c.gridy=1; c.weighty=0; c.gridwidth=1;
            c.gridx=0; c.weightx=0; c.insets.right=0; panel.add(new JLabel("Op�ration: "), c);
            c.gridx=2;                                panel.add(new JLabel("Colonne: "), c);
            c.gridx=1; c.weightx=1; c.insets.right=6; panel.add(operation, c);
            c.gridx=3; c.weightx=0.5;                 panel.add(column, c);
            c.gridy=2; c.insets.top=0; c.insets.bottom=3;
            c.gridx=1; c.weightx=1; c.gridwidth=3;    panel.add(interpolationAllowed, c);
            tabs.addTab("S�ries", panel);
        }
        /////////////////////////////////////////////////////////////
        ////////    Onglet Coordonn�es spatio-temporelles    ////////
        /////////////////////////////////////////////////////////////
        if (true) {
            final JPanel panel = new JPanel(new BorderLayout());
            if (true) {
                Utilities.selectDefaultPositions(positions);
                final JPanel timePanel = new JPanel(new BorderLayout());
                timePanel.add(new JLabel("D�calages en jours"), BorderLayout.NORTH);
                timePanel.add(new JScrollPane(positions),       BorderLayout.CENTER);
                timePanel.setBorder(BorderFactory.createEmptyBorder(6,18,6,6));
                panel.add(timePanel, BorderLayout.CENTER);
            }
            coordinates.setSelectorVisible(CoordinateChooser.RESOLUTION, false);
            panel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            panel.add(coordinates, BorderLayout.WEST);
            tabs.addTab("Coordonn�es", panel);
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
     * Retourne les jours o� extraire des donn�es, avant, pendant et apr�s le jour de la p�che.
     * Les d�calages sont s�lectionn�s par l'utilisateur.
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
     * Appel�e chaque fois que l'utilisateur choisit une nouvelle op�ration.
     */
    private void operationSelected(final JTabbedPane tabs, final int tabIndex) {
        final OperationEntry op = (OperationEntry) operation.getSelectedItem();
        final String  name = op.getProcessorOperation();
        tabs.setEnabledAt(tabIndex, (name!=null) && name.equalsIgnoreCase("GradientMagnitude"));
        column.setText(op.getColumn());
    }

    /**
     * Fait appara�tre la bo�te de dialogue proposant de remplir la table des param�tres
     * environnementaux. Si l'utilisateur clique sur "Ok", l'op�ration sera imm�diatement
     * lanc�e. Cette m�thode peut �tre appel�e � partir de n'importe quel thread. La bo�te
     * de dialogue sera lanc�e dans le thread de <cite>Swing</cite>. Si l'utilisateur clique
     * sur "ok", l'op�ration sera ensuite lanc�e dans le thread courant.
     */
    public void showDialog(final Component owner) {
        if (SwingUtilities.showOptionDialog(owner, this, "Environnement aux positions de p�ches")) {
            final LoggingPanel logging = new LoggingPanel("fr.ird");
            logging.setColumnVisible(LoggingPanel.LOGGER, false);
            final Handler handler = logging.getHandler();
            Logger.getLogger("org.geotools").addHandler(handler);
            handler.setLevel(Level.FINE);
            logging.show(owner);

            final String                column = this.column.getText();
            final boolean          interpolate = interpolationAllowed.isSelected();
            final OperationEntry     operation = (OperationEntry) this.operation.getSelectedItem();
            final Map<String,Object> arguments = new HashMap<String,Object>();
            arguments.put("mask1", kernels.getHorizontalEditor().getKernel());
            arguments.put("mask2", kernels.getVerticalEditor().getKernel());
            GridCoverageProcessor.initialize();

            filler.setInterpolationAllowed(interpolate);
            filler.getSeries().retainAll(series.getSelectedElements());
            filler.getRelativePositions().retainAll(Arrays.asList(positions.getSelectedValues()));
            filler.getOperations().clear();
            filler.getOperations().add(new OperationEntry.Proxy(operation) {
                /** Retourne le nom de l'op�ration � appliquer. */
                public String getProcessorOperation() {
                    String name = super.getProcessorOperation();
                    if (!interpolate) {
                        return name;
                    }
                    if (name==null || (name=name.trim()).length()==0) {
                        return GridCoverageProcessor.NODATA_FILTER;
                    }
                    return GridCoverageProcessor.NODATA_FILTER + '-' + name;
                }

                /** Retourne le nom de la colonne dans laquelle �crire le r�sultat. */
                public String getColumn() {
                    return column;
                }

                /** Retourne la valeur d'un param�tre de l'op�ration. */
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
     * Lib�re les ressources utilis�es par cet objet.
     */
    public void dispose() throws SQLException {
        if (closeOnDispose) {
            filler.close();
            closeOnDispose = false;
        }
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Lance l'ex�cution de l'application.
     *
     * @throws SQLException si un probl�me est survenu lors d'un acc�s � une base de donn�es.
     */
    public static void main(final String[] args) throws SQLException {
        MonolineFormatter.init("fr.ird");
        final EnvironmentSamplingChooser panel = new EnvironmentSamplingChooser();
        panel.showDialog(null);
        panel.dispose();
        System.exit(0);
    }
}
