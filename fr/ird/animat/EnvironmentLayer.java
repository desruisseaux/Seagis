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

// Cartes
import net.seas.map.layer.GridCoverageLayer;

// Implémentation OpenGIS
import net.seagis.gc.GridCoverage;
import net.seagis.cs.GeographicCoordinateSystem;

// Animats
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Couche représentant une image sur une carte.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class EnvironmentLayer extends GridCoverageLayer implements EnvironmentChangeListener
{
    /**
     * L'index du paramètre à afficher.
     */
    private int parameter = 0;

    /**
     * Construit une couche pour l'environnement spécifié.
     */
    public EnvironmentLayer(final Environment environment)
    {
        super(GeographicCoordinateSystem.WGS84);
        environment.addEnvironmentChangeListener(this);
        setCoverage(environment.getGridCoverage(parameter));
    }

    /**
     * Appelée quand un environnement a changé.
     */
    public void environmentChanged(final EnvironmentChangeEvent event)
    {
        final Environment environment = event.getSource();
        setCoverage(environment.getGridCoverage(parameter));
    }
}
