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
package fr.ird.animat.seas;

// J2SE
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.sql.SQLException;

// Animats
import fr.ird.animat.Species;
import fr.ird.animat.impl.Animal;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.FisheryDataBase;


/**
 * Une population de thons. Il peut y avoir des thons de plusieurs esp�ces.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Population extends fr.ird.animat.impl.Population {
    /**
     * Construit une population qui contiendra initialement les thons aux positions
     * de p�ches du pas de temps courant.
     *
     * @param environment Environnement Environnement de la population.
     * @throws RemoteException si la construction de la population a �chou�e.
     */
    protected Population(final Environment environment) throws RemoteException {
        super(environment);
        final Collection<CatchEntry> entries;
        try {
            /*
             * Note: S'il y avait beaucoup de populations � cr�er, il vaudrait mieux obtenir une
             *       connexion vers CatchTable une fois pour toute  et la r�utiliser pour toutes
             *       les populations. Mais comme on va typiquement cr�er une seule (ou tr�s peu)
             *       population, on va plut�t lib�rer la connexion rapidement  afin de permettre
             *       � d'autres machines de se connecter sur cette base de donn�es (Access n'est
             *       pas terrible  lorsqu'il y plusieurs utilisateurs qui se connectent  en m�me
             *       temps).
             */
            final FisheryDataBase database = new FisheryDataBase();
            final Collection<String> species = environment.configuration.species;
            final CatchTable catchs = database.getCatchTable(species.toArray(new String[species.size()]));
            catchs.setTimeRange(environment.getClock().getTimeRange());
            entries = catchs.getEntries();
            catchs.close();
            database.close();
        } catch (SQLException exception) {
            throw new ServerException("�chec lors de l'obtention "+
                                      "des positions initiales des animaux", exception);
        }
        for (final Iterator<CatchEntry> it=entries.iterator(); it.hasNext();) {
            final CatchEntry   entry   = it.next();
            final Point2D      coord   = entry.getCoordinate();
            final Set<Species> species = entry.getSpecies();
            for (final Iterator<Species> its=species.iterator(); its.hasNext();) {
                newAnimal(its.next(), coord);
            }
        }
    }

    /**
     * Ajoute un nouvel animal dans cette population.
     *
     * @param  species L'esp�ce de cet animal.
     * @param  position Position initiale de l'animal, en degr�s de longitudes et de latitudes.
     * @return L'animal cr��.
     * @throws IllegalStateException si cette population est morte.
     * @throws RemoteException si l'exportation du nouvel animal a �chou�.
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
