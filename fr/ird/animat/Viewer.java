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

// Interface utilisateur
import javax.swing.JComponent;

// Cartes
import net.seas.map.MapPanel;
import net.seas.map.layer.GridCoverageLayer;

// Impl�mentation OpenGIS
import net.seagis.cs.GeographicCoordinateSystem;


/**
 * Composante affichant une carte repr�sentant la position des
 * animaux dans leur environnement.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Viewer
{
    /**
     * La carte � afficher. Le syst�me de coordonn�es
     * sera un syst�me g�ographique selon l'ellipso�de
     * WGS84.
     */
    private final MapPanel map = new MapPanel();

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
     */
    public Viewer(final Environment environment, final Population population)
    {
        this.environment = new EnvironmentLayer(environment);
        this.population  = new  PopulationLayer(population );
        map.addLayer(this.environment);
        map.addLayer(this.population );
    }

    /**
     * Retourne la composante visuelle dans laquelle seront
     * affich�es les animaux.
     */
    public JComponent getView()
    {
        return map;
    }
}
