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
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import javax.swing.JComponent;
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
 * Composante affichant une carte repr�sentant la position des
 * animaux dans leur environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Viewer extends JComponent {
    /**
     * La carte � afficher. Le syst�me de coordonn�es
     * sera un syst�me g�ographique selon l'ellipso�de
     * WGS84.
     */
    private final MapPane map = new MapPane();

    /**
     * La barre d'�tat. Elle contiendra entre autres les coordonn�es
     * g�ographiques point�es par la souris.
     */
    private final StatusBar status = new StatusBar(map);

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
     * Objet ayant la charge d'�couter les changements survenant dans {@link EnvironmentLayer}.
     */
    private final Listener listener;

    /**
     * Construit un afficheur.
     *
     * @param simulation La simulation � afficher.
     */
    public Viewer(final Simulation simulation) throws RemoteException {
        listener = new Listener();
        this.simulation = simulation;
        final Environment environment = simulation.getEnvironment();
        /*
         * Ajoute l'�chelle de la carte et l'environnement.
         */
        map.setPaintingWhileAdjusting(true);
        environmentLayer = new EnvironmentLayer(environment);
        environmentLayer.addPropertyChangeListener(listener);
        final Renderer renderer = map.getRenderer();
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
         * Construit l'interface.
         */
        setLayout(new BorderLayout());
        final JPanel mapPane = new JPanel(new BorderLayout());
        mapPane.add(map.createScrollPane(),  BorderLayout.CENTER);
        mapPane.add(environmentLayer.colors, BorderLayout.SOUTH );
        add(mapPane, BorderLayout.CENTER);
        add(status,  BorderLayout.SOUTH );
    }

    /**
     * Classe ayant la charge d'�couter les changements survenant dans
     * {@link EnvironmentLayer}.
     */
    private final class Listener implements PropertyChangeListener {
        /**
         * Appel�e quand une propri�t� de {@link EnvironmentLayer} a chang�e.
         */
        public void propertyChange(final PropertyChangeEvent event) {
            try {
                final String property = event.getPropertyName();
                if (property.equalsIgnoreCase("population")) {
                    final Renderer renderer = map.getRenderer();
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
                PopulationLayer.failed("Viewer", "propertyChange", exception);
            }
        }
    }
}
