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

// Animats
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.Environment;
import fr.ird.sql.fishery.CatchEntry;


/**
 * Une population de thon. Il peut y avoir des thons de plusieurs esp�ces.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Population extends fr.ird.animat.Population {
    /**
     * Distance maximale que peuvent parcourir les animaux de
     * cette population � chaque instruction {@link #move}.
     */
    private final double maximumDistance;

    /**
     * Construit une population.
     *
     * @param maximumDistance Distance maximale que peuvent parcourir les
     *        animaux de cette population � chaque instruction {@link #move}.
     */
    public Population(final double maximumDistance) {
        this.maximumDistance = maximumDistance;
    }

    /**
     * Ajoute un animal pour chaque esp�ce � chacune des positions de p�che sp�cifi�es.
     */
    public void addAnimals(final Collection<CatchEntry> entries) {
        final int oldSize = getAnimals().size();
        for (final Iterator<CatchEntry> it=entries.iterator(); it.hasNext();) {
            final CatchEntry   entry   = it.next();
            final Point2D      coord   = entry.getCoordinate();
            final Set<Species> species = entry.getSpecies();
            for (final Iterator<Species> its=species.iterator(); its.hasNext();) {
                final Tuna tuna = new Tuna(its.next());
                tuna.setLocation(coord);
                addAnimal(tuna);
            }
        }
        if (oldSize != getAnimals().size()) {
            firePopulationChanged();
        }
    }

    /**
     * Fait �voluer une population en fonction de son environnement.
     * Cette m�thode va typiquement d�placer les {@linkplain Animal animaux}
     * en appellant des m�thodes telles que {@link Animal#moveToward}. Des
     * individus peuvent aussi na�tre ou mourrir.
     *
     * @param  duration Dur�e de l'�volution, en nombre de jours.
     *         Cette dur�e est habituellement �gale �
     *         <code>{@link #getEnvironment()}.{@link Environment#getTimeStep()
     *         getTimeStep()}.{@link TimeStep#getDuration getDuration()}</code>.
     */
    public void evoluate(final float duration) {
        // TODO
        firePopulationChanged();
    }
}
