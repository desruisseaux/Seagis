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
package fr.ird.animat.viewer;

// J2SE dependencies
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.rmi.RemoteException;

// Geotools dependencies
import org.geotools.gui.swing.MapPane;
import org.geotools.gui.swing.StatusBar;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedMapScale;
import org.geotools.renderer.j2d.RenderedGridCoverage;

// Animats
import fr.ird.animat.Population;
import fr.ird.animat.Environment;


/**
 * Composante affichant une carte représentant la position des
 * animaux dans leur environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Viewer {
    /**
     * La carte à afficher. Le système de coordonnées
     * sera un système géographique selon l'ellipsoïde
     * WGS84.
     */
    private final MapPane map = new MapPane();

    /**
     * La barre d'état. Elle contiendra entre autres les coordonnées
     * géographiques pointées par la souris.
     */
    private final StatusBar status = new StatusBar(map);

    /**
     * La couche de l'environnement à afficher.
     */
    private final EnvironmentLayer environment;

    /**
     * La couche représentant la population.
     */
    private final PopulationLayer population;

    /**
     * Construit un afficheur.
     *
     * @param population  La population à afficher.
     */
    public Viewer(final Population population) throws RemoteException {
        this.environment = new EnvironmentLayer(population.getEnvironment());
        this.population  = new  PopulationLayer(population);
        this.environment.addPropertyChangeListener(this.population);
        map.setPaintingWhileAdjusting(true);
        map.getRenderer().addLayer(this.environment);
        map.getRenderer().addLayer(this.population );
        map.getRenderer().addLayer(new RenderedMapScale());
    }

    /**
     * Retourne la composante visuelle dans laquelle seront
     * affichées les animaux et leur environnement.
     */
    public JComponent getView() {
        final JPanel panel   = new JPanel(new BorderLayout());
        final JPanel mapPane = new JPanel(new BorderLayout());
        mapPane.add(map.createScrollPane(), BorderLayout.CENTER);
        mapPane.add(environment.colors,     BorderLayout.SOUTH );
        panel.add(mapPane, BorderLayout.CENTER);
        panel.add(status,  BorderLayout.SOUTH );
        return panel;
    }
}
