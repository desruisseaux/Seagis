/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.sql.fishery;

// J2SE dependencies
import java.util.Set;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import java.io.Serializable;

// Miscellaneous
import fr.ird.animat.Species;


/**
 * Ensemble des esp�ces. Cet ensemble enveloppe un tableau
 * <code>Species[]</code> qu'on supposera sans doublons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class SpeciesSet extends AbstractSet<Species> implements Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 6555886839089616118L;

    /**
     * Esp�ces vis�s par cette p�che. En g�n�ral, on aura un seul exemplaire de
     * ce tableau qui sera partag� par plusieurs objets {@link AbstractCatch}.
     */
    protected final Species[] species;

    /**
     * Construit un ensemble d'esp�ces
     * � partir d'un autre ensemble.
     */
    public SpeciesSet(final Collection<Species> species) {
        this((Species[])species.toArray(new Species[species.size()]));
    }

    /**
     * Construit un ensemble d'esp�ces.
     *
     * @param species Esp�ce composant cet ensemble.
     *        <strong>Ce tableau ne sera pas clon�</strong>.
     *        Evitez donc de le modifier apr�s la construction.
     */
    public SpeciesSet(final Species[] species) {
        this.species = species;
    }

    /**
     * Returns the number of species in this catch.
     */
    public final int size() {
        return species.length;
    }

    /**
     * Returns an iterator over the species in this catch.
     * This iterator <strong>must</strong> returns species
     * always in the same order.
     */
    public final java.util.Iterator<Species> iterator() {
        return new Iterator(species);
    }

    /**
     * An iterator over the species. The underlying collection
     * must be immutable. This iterator does not support the
     * remove operation.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Iterator implements java.util.Iterator<Species> {
        /**
         * The species.
         */
        private final Species[] species;

        /**
         * Index of the next species to returns.
         */
        private int index=0;

        /**
         * Construct an iterator.
         */
        public Iterator(final Species[] species) {
            this.species = species;
        }

        /**
         * Returns <code>true</code> if the iteration has more elements.
         */
        public boolean hasNext() {
            return index<species.length;
        }

        /**
         * Returns the next element in the iteration.
         */
        public Species next() {
            if (index<species.length) {
                return species[index++];
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * Unsupported operation, since the underlying set is immutable.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
