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

// Cartes
import net.seas.map.layer.GridCoverageLayer;

// Impl�mentation OpenGIS
import net.seagis.gc.GridCoverage;
import net.seagis.cs.GeographicCoordinateSystem;

// Animats
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Couche repr�sentant une image sur une carte.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class EnvironmentLayer extends GridCoverageLayer implements EnvironmentChangeListener
{
    /**
     * L'index du param�tre � afficher.
     */
    private int parameter = 0;

    /**
     * Construit une couche pour l'environnement sp�cifi�.
     */
    public EnvironmentLayer(final Environment environment)
    {
        super(GeographicCoordinateSystem.WGS84);
        environment.addEnvironmentChangeListener(this);
        setCoverage(environment.getGridCoverage(parameter));
    }

    /**
     * Appel�e quand un environnement a chang�.
     */
    public void environmentChanged(final EnvironmentChangeEvent event)
    {
        final Environment environment = event.getSource();
        setCoverage(environment.getGridCoverage(parameter));
    }
}
