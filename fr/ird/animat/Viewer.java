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
package fr.ird.animat;

// J2SE dependencies
import java.awt.Graphics2D;
import javax.swing.JComponent;
import java.rmi.RemoteException;

// Other dependencies
import fr.ird.map.RepaintManager;
import org.geotools.gui.swing.MapPane;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedGridCoverage;


/**
 * Composante affichant une carte repr�sentant la position des
 * animaux dans leur environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Viewer {
    /**
     * La carte � afficher. Le syst�me de coordonn�es
     * sera un syst�me g�ographique selon l'ellipso�de
     * WGS84.
     */
    private final MapPane map = new MapPane();

    /**
     * La couche de l'environnement � afficher.
     */
    private final EnvironmentLayer environment;

    /**
     * La couche repr�sentant la population.
     */
    private final PopulationLayer population;

    /**
     * Construit un afficheur.
     *
     * @param environment L'environemment � afficher.
     * @param population  La population � afficher.
     * @param lock Objet sur lequel synchronizer les tra�ages. La m�thode
     *        {@link MapPane#paintComponent(Graphics2D)} sera appel�e
     *        � l'int�rieur d'un block <code>synchronized(lock)</code>.
     *
     * @throws RemoteException si au moins une partie de l'ex�cution
     *         devait se faire sur un serveur et que cette ex�cution
     *         a �chou�.
     */
    public Viewer(final Environment environment,
                  final Population  population,
                  final Object      lock)
        throws RemoteException
    {
        final RepaintManager manager = new RepaintManager(lock);
        this.environment = new EnvironmentLayer(environment, manager);
        this.population  = new  PopulationLayer(population,  manager);
        this.environment.addPropertyChangeListener(this.population);
        map.setPaintingWhileAdjusting(true);
        map.getRenderer().addLayer(this.environment);
        map.getRenderer().addLayer(this.population );
    }

    /**
     * Retourne la composante visuelle dans laquelle seront
     * affich�es les animaux et leur environnement.
     */
    public JComponent getView() {
        return map.createScrollPane();
    }
}
