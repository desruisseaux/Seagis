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
 */
package fr.ird.animat.server.tuna;

// J2SE
import java.util.Set;
import java.util.Date;
import java.util.Collection;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import javax.media.jai.util.Range;

// Animats
import fr.ird.animat.Species;
import fr.ird.animat.server.Animal;
import fr.ird.database.sample.SampleEntry;


/**
 * Une population de thons. Il peut y avoir des thons de plusieurs espèces.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Population extends fr.ird.animat.server.Population {
    /**
     * Construit une population qui contiendra initialement les thons aux positions
     * de pêches du pas de temps courant.
     *
     * @param environment Environnement Environnement de la population.
     * @throws RemoteException si la construction de la population a échouée.
     */
    protected Population(final Environment environment) throws RemoteException {
        super(environment);
        final Collection<SampleEntry> entries;
        entries = environment.getSamples();
        for (final SampleEntry entry : entries) {
            final Point2D      coord   = entry.getCoordinate();
            final Set<Species> species = entry.getSpecies();
            for (final Species sp : species) {
                newAnimal(sp, coord);
            }
        }
    }

    /**
     * Ajoute un nouvel animal dans cette population.
     *
     * @param  species L'espèce de cet animal.
     * @param  position Position initiale de l'animal, en degrés de longitudes et de latitudes.
     * @return L'animal créé.
     * @throws IllegalStateException si cette population est morte.
     * @throws RemoteException si l'exportation du nouvel animal a échoué.
     */
    public Animal newAnimal(fr.ird.animat.Species species, final Point2D position)
            throws IllegalStateException, RemoteException
    {
        synchronized (getTreeLock()) {
            final Environment environment = (Environment) getEnvironment();
            if (environment == null) {
                throw new IllegalStateException("Cette population est morte.");
            }
            return new Tuna(environment.wrap(species), this, position);
        }
    }
}
