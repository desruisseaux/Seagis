/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
 */
package fr.ird.animat.server.tuna;

// J2SE
import java.rmi.RemoteException;
import java.awt.geom.RectangularShape;

// Seagis
import fr.ird.resources.XEllipse2D;


/**
 * Une espèce de thon.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Species extends fr.ird.animat.server.Species {
    /**
     * Valeur par défaut du rayon de perception de l'animal, en miles nautiques.
     *
     * @see #getPerceptionArea
     */
    private final double perceptionRadius;

    /**
     * Distance maximale (en miles nautiques) que ce thon peut parcourir en une journée.
     */
    final double dailyDistance;

    /**
     * Construit une espèce avec le même nom que l'espèce spécifiée mais qui s'intéressera
     * à des paramètres différents.
     *
     * @param  parent L'espèce dont on veut copier les propriétés (noms, couleur).
     * @param  La configuration de la simulation.
     * @throws RemoteException si des méthodes devaient être appelée sur une machine distance
     *         et que ces appels ont échoués.
     */
    protected Species(final fr.ird.animat.Species parent,
                      final Configuration configuration)
            throws RemoteException
    {
        super(wrap(parent), configuration.parameterArray);
        perceptionRadius = configuration.perceptionRadius;
        dailyDistance    = configuration.dailyDistance;
    }

    /**
     * Retourne la région de perception par défaut des animaux de cette espèce. Il s'agit de la
     * région dans laquelle  l'animal peut percevoir les paramètres de son environnement autour
     * de lui. Les coordonnées de cette forme doivent être en <strong>mètres</strong> et la forme
     * doit être centrée sur la position de l'animal, sans rotation.
     */
    protected RectangularShape getPerceptionArea() {
        return new XEllipse2D(-perceptionRadius,
                              -perceptionRadius,
                             2*perceptionRadius,
                             2*perceptionRadius);
    }

    /**
     * Vérifie si cette espèce est égale à l'objet spécifié.
     */
    public boolean equals(final Object object) {
        if (object instanceof Species) {
            if (super.equals(object)) {
                final Species that = (Species) object;
                return Double.doubleToLongBits(this.perceptionRadius) ==
                       Double.doubleToLongBits(that.perceptionRadius);
            }
            return false;
        }
        return super.equals(object); // Compare RMI stubs
    }
}
