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
package fr.ird.animat.gui.swing;

// J2SE dependencies
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
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

// Animats
import fr.ird.animat.Simulation;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;


/**
 * Composante affichant une carte représentant la position des animaux dans leur environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class SimulationPane extends JComponent implements PropertyChangeListener {
    /**
     * La carte à afficher. Le système de coordonnées sera
     * un système géographique selon l'ellipsoïde WGS84.
     */
    protected final MapPane mapPane = new MapPane();

    /**
     * La barre d'état. Elle contiendra entre autres les coordonnées
     * géographiques pointées par la souris.
     */
    protected final StatusBar status = new StatusBar(mapPane);

    /**
     * La simulation en cours.
     */
    private final Simulation simulation;

    /**
     * La couche de l'environnement à afficher.
     */
    final EnvironmentLayer environmentLayer;

    /**
     * Les couches représentant les populations.
     */
    private final Map<Population,PopulationLayer> populationLayers;

    /**
     * La composante servant à séparer la carte à gauche et les contrôles à droite.
     * @see #setControlPanel
     */
    private final JSplitPane split;

    /**
     * Construit un afficheur.
     *
     * @param simulation La simulation à afficher.
     */
    public SimulationPane(final Simulation simulation) throws RemoteException {
        this.simulation = simulation;
        final Environment environment = simulation.getEnvironment();
        /*
         * Ajoute l'échelle de la carte et l'environnement.
         */
        mapPane.setPaintingWhileAdjusting(true);
        environmentLayer = new EnvironmentLayer(environment);
        final Renderer renderer = mapPane.getRenderer();
        renderer.addLayer(environmentLayer);
        renderer.addLayer(new RenderedMapScale());
        /*
         * Ajoute toutes les populations.
         */
        final Set<+Population> populations = environment.getPopulations();
        int size = populations.size();
        size += size/2;
        populationLayers = new HashMap<Population,PopulationLayer>(size);
        for (final Population population : populations) {
            final PopulationLayer layer = new PopulationLayer(population);
            environmentLayer.addPropertyChangeListener(layer);
            populationLayers.put(population, layer);
            renderer.addLayer(layer);
        }
        // Ne doit pas précéder l'ajout des populations.
        environmentLayer.addPropertyChangeListener(this);
        /*
         * Construit l'interface.
         */
        setLayout(new BorderLayout());
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(mapPane.createScrollPane(),  BorderLayout.CENTER);
        panel.add(environmentLayer.colors,     BorderLayout.SOUTH );
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(panel);
        split.setResizeWeight(1);
        split.setOneTouchExpandable(true);
        add(split,   BorderLayout.CENTER);
        add(status,  BorderLayout.SOUTH );
        mapPane.reset();
    }

    /**
     * Définit le paneau de controle à la droite de la carte.
     * Cette méthode est habituellement appelée dans le constructeur des classes dérivées.
     */
    protected final void setControlPanel(final JComponent panel) {
        split.setRightComponent(panel);
    }

    /**
     * Met à jour les étiquettes (date courante, mémoire utilisée, etc.).
     * L'implémentation par défaut ne fait rien.
     */
    protected void updateLabels() {
    }

    /**
     * Appelée quand une propriété de {@link EnvironmentLayer} a changée.
     */
    public void propertyChange(final PropertyChangeEvent event) {
        try {
            final String property = event.getPropertyName();
            if (property.equalsIgnoreCase("date")) {
                updateLabels();
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
                    layer.setColor(populationLayers.size());
                    populationLayers.put(population, layer);
                    renderer.addLayer(layer);
                }
            }
        } catch (RemoteException exception) {
            EnvironmentLayer.failed("SimulationPane", "propertyChange", exception);
        }
    }
}
