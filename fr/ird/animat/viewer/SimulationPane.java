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
package fr.ird.animat.viewer;

// J2SE dependencies
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.rmi.RemoteException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

// Geotools dependencies
import org.geotools.gui.swing.MapPane;
import org.geotools.gui.swing.StatusBar;
import org.geotools.renderer.j2d.Renderer;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedMapScale;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.resources.Utilities;

// Animats
import fr.ird.animat.Report;
import fr.ird.animat.Simulation;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;


/**
 * Composante affichant une carte repr�sentant la position des animaux dans leur environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SimulationPane extends JComponent implements PropertyChangeListener {
    /**
     * La carte � afficher. Le syst�me de coordonn�es sera
     * un syst�me g�ographique selon l'ellipso�de WGS84.
     */
    private final MapPane mapPane = new MapPane();

    /**
     * La barre d'�tat. Elle contiendra entre autres les coordonn�es
     * g�ographiques point�es par la souris.
     */
    private final StatusBar status = new StatusBar(mapPane);

    /**
     * La simulation en cours.
     */
    private final Simulation simulation;

    /**
     * La couche de l'environnement � afficher.
     */
    private final EnvironmentLayer environmentLayer;

    /**
     * Les couches repr�sentant les populations.
     */
    private final Map<Population,PopulationLayer> populationLayers;

    /**
     * Liste des couvertures utilis�es pour le pas de temps courant.
     */
    private final JList coverageNames;

    /**
     * La date du pas de temps courant.
     */
    private final JLabel stepTime = new JLabel(" ", JLabel.CENTER);

    /**
     * Pourcentage des donn�es manquantes.
     */
    private final JLabel missingData = new JLabel(" ", JLabel.CENTER);

    /**
     * Pourcentage des donn�es manquantes (cumulatif).
     */
    private final JLabel missingDataCumul = new JLabel(" ");

    /**
     * Pourcentage des points � l'ext�rieur (cumulatif).
     */
    private final JLabel outsideCumul = new JLabel(" ");

    /**
     * Affiche la quantit� de m�moire allou�e.
     */
    private final JLabel allocatedMemory = new JLabel();

    /**
     * Affiche la quantit� de m�moire utilis�e.
     */
    private final JLabel usedMemory = new JLabel();

    /**
     * Le format � utiliser pour �crire la date courante.
     */
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);

    /**
     * Le format � utiliser pour lire et �crire des nombres.
     */
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    /**
     * Le format � utiliser pour lire et �crire des pourcentages.
     */
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    /**
     * Construit un afficheur.
     *
     * @param simulation La simulation � afficher.
     */
    public SimulationPane(final Simulation simulation) throws RemoteException {
        this.simulation = simulation;
        final Environment environment = simulation.getEnvironment();
        /*
         * Ajoute l'�chelle de la carte et l'environnement.
         */
        mapPane.setPaintingWhileAdjusting(true);
        environmentLayer = new EnvironmentLayer(environment);
        environmentLayer.addPropertyChangeListener(this);
        final Renderer renderer = mapPane.getRenderer();
        renderer.addLayer(new RenderedMapScale());
        renderer.addLayer(environmentLayer);
        /*
         * Ajoute toutes les populations.
         */
        final Set<Population> populations = environment.getPopulations();
        int size = populations.size();
        size += size/2;
        populationLayers = new HashMap<Population,PopulationLayer>(size);
        for (final Iterator<Population> it=populations.iterator(); it.hasNext();) {
            final Population population = it.next();
            final PopulationLayer layer = new PopulationLayer(population);
            environmentLayer.addPropertyChangeListener(layer);
            populationLayers.put(population, layer);
            renderer.addLayer(layer);
        }
        /*
         * Construit le paneau d'information.
         */
        coverageNames = new JList(environmentLayer);
        final JPanel control = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets.right=6; c.insets.left=6; c.insets.top=6; c.insets.bottom=6;
        c.gridx=0; c.gridwidth=1; c.gridheight=1; c.weightx=1; c.fill=c.BOTH;
        c.gridy=0;
        control.add(stepTime, c);
        c.gridy++; c.insets.bottom=0;
        control.add(new JLabel("Donn�es courantes"), c);
        c.gridy++; c.weighty=1; c.insets.bottom=6;
        control.add(new JScrollPane(coverageNames), c);
        if (true) {
            c.gridy++; c.weighty=0;
            c.insets.top=0;
            control.add(missingData, c);
            c.insets.top=6;
        }
        if (true) {
            c.gridy++; c.weighty=0;
            final JPanel panel = new JPanel(new GridLayout(2,1));
            panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createTitledBorder("Cumulatif"),
                            BorderFactory.createEmptyBorder(0, 9, 3, 0)));
            panel.add(missingDataCumul);
            panel.add(outsideCumul);
            control.add(panel, c);
        }
        if (true) {
            c.gridy++;
            final JPanel panel = new JPanel(new GridLayout(2,2));
            panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createTitledBorder("M�moire"),
                            BorderFactory.createEmptyBorder(0, 9, 3, 0)));
            panel.add(new JLabel("Allou�e:"));  panel.add(allocatedMemory);
            panel.add(new JLabel("Utilis�e:")); panel.add(usedMemory);
            control.add(panel, c);
        }
        if (true) {
            c.gridy++;
            final JButton addPop = new JButton("Ajouter une population");
            addPop.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    addPopulation(event);
                }
            });
            control.add(addPop, c);
        }
        stepTime.setFont(stepTime.getFont().deriveFont(Font.BOLD));
        dateFormat.setTimeZone(environment.getClock().getTimeZone());
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        updateLabels();
        /*
         * Construit l'interface.
         */
        setLayout(new BorderLayout());
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(mapPane.createScrollPane(),  BorderLayout.CENTER);
        panel.add(environmentLayer.colors,     BorderLayout.SOUTH );
        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, control);
        split.setResizeWeight(1);
        split.setOneTouchExpandable(true);
        add(split,   BorderLayout.CENTER);
        add(status,  BorderLayout.SOUTH );
        mapPane.reset();
    }

    /**
     * Ajoute une population initialis� avec les positions de p�ches actuels.
     */
    private void addPopulation(final ActionEvent event) {
        try {
            final Population population = environmentLayer.environment.newPopulation();
            final PopulationLayer layer = new PopulationLayer(population);
            environmentLayer.addPropertyChangeListener(layer);
            layer.setColor(populationLayers.size());
            populationLayers.put(population, layer);
            mapPane.getRenderer().addLayer(layer);
        } catch (RemoteException exception) {
            EnvironmentLayer.failed("Environment", "newPopulation", exception);
            JButton source = (JButton) event.getSource();
            source.setText("Erreur");
            source.setEnabled(false);
        }
    }

    /**
     * Met � jour les �tiquettes (date courante, m�moire utilis�e, etc.).
     */
    private void updateLabels() {
        final Runtime runtime = Runtime.getRuntime();
        final long allocated = runtime.totalMemory();
        final long used = allocated-runtime.freeMemory();
        allocatedMemory.setText(numberFormat.format(allocated/(1024*1024.0))+" Mo");
        usedMemory.setText(percentFormat.format((double)used/(double)allocated));
        try {
            stepTime.setText(dateFormat.format(environmentLayer.environment.getClock().getTime()));
        } catch (RemoteException exception) {
            stepTime.setText(Utilities.getShortClassName(exception));
            EnvironmentLayer.failed("Environment", "getClock", exception);
        }
        try {
            Report report = environmentLayer.environment.getReport(false);
            final FieldPosition dummy = new FieldPosition(0);
            final StringBuffer buffer = new StringBuffer();
            percentFormat.format(report.percentMissingData(), buffer, dummy);
            buffer.append(" de donn�es manquantes");
            missingData.setText(buffer.toString());

            report = environmentLayer.environment.getReport(true);
            buffer.setLength(0);
            percentFormat.format(report.percentMissingData(), buffer, dummy);
            buffer.append(" de donn�es manquantes");
            missingDataCumul.setText(buffer.toString());

            buffer.setLength(0);
            percentFormat.format(report.percentOutsideSpatialBounds(), buffer, dummy);
            buffer.append(" � l'ext�rieur");
            outsideCumul.setText(buffer.toString());
        } catch (RemoteException exception) {
            missingData.setText(Utilities.getShortClassName(exception));
            EnvironmentLayer.failed("Environment", "getReport", exception);
        }
    }

    /**
     * Appel�e quand une propri�t� de {@link EnvironmentLayer} a chang�e.
     */
    public void propertyChange(final PropertyChangeEvent event) {
        try {
            final String property = event.getPropertyName();
            if (property.equalsIgnoreCase("date")) {
                mapPane.repaint();
            }
            else if (property.equalsIgnoreCase("population")) {
                final Renderer renderer = mapPane.getRenderer();
                Population population = (Population) event.getOldValue();
                if (population != null) {
                    final PopulationLayer layer = populationLayers.remove(population);
                    renderer.removeLayer(layer);
                    environmentLayer.removePropertyChangeListener(layer);
                    layer.dispose();
                }
                population = (Population) event.getNewValue();
                if (population != null) {
                    final PopulationLayer layer = new PopulationLayer(population);
                    environmentLayer.addPropertyChangeListener(layer);
                    populationLayers.put(population, layer);
                    renderer.addLayer(layer);
                }
            }
        } catch (RemoteException exception) {
            EnvironmentLayer.failed("SimulationPane", "propertyChange", exception);
        }
        updateLabels();
    }
}
