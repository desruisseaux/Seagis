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
package fr.ird.animat;

// Interface utilisateur
import javax.swing.JComponent;

// Cartes
import fr.ird.map.MapPanel;
import fr.ird.map.layer.GridCoverageLayer;

// Geotools dependencies
import org.geotools.cs.GeographicCoordinateSystem;


/**
 * Composante affichant une carte représentant la position des
 * animaux dans leur environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Viewer
{
    /**
     * La carte à afficher. Le système de coordonnées
     * sera un système géographique selon l'ellipsoïde
     * WGS84.
     */
    private final MapPanel map = new MapPanel();

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
     * @param environment L'environemment à afficher.
     * @param population  La population à afficher.
     */
    public Viewer(final Environment environment, final Population population)
    {
        this.environment = new EnvironmentLayer(environment);
        this.population  = new  PopulationLayer(population );
        map.setPaintingWhileAdjusting(true);
        map.addLayer(this.environment);
        map.addLayer(this.population );
    }

    /**
     * Retourne la composante visuelle dans laquelle seront
     * affichées les animaux.
     */
    public JComponent getView()
    {
        return map.createScrollPane();
    }
}
