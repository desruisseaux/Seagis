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
 */
package fr.ird.animat.event;

// Dependencies
import fr.ird.animat.Animal;


/**
 * Un �v�nement signalant que l'�tat d'un {@link Animal animal} a chang�. La raison du changement
 * peut �tre une migration vers une nouvelle population ou une m�tamorphose en une nouvelle
 * esp�ce, mais n'inclus g�n�ralement pas les d�placements d'animaux �tant donn� que ces derniers
 * surviennent typiquement � chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see AnimalChangeListener
 */
public class AnimalChangeEvent extends ChangeEvent {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -8282372966814112917L;

    /**
     * Drapeau indiquant que l'animal a chang� de population. Les �v�nements de ce type
     * sont g�n�ralement lanc�s en m�me temps qu'un �v�nement {@link PopulationChangeEvent}
     * de la population concern�e.
     */
    public static final int POPULATION_CHANGED = PopulationChangeEvent.LAST << 1;

    /**
     * Drapeau indiquant que l'animal a chang� d'esp�ce.
     */
    public static final int SPECIES_CHANGED = POPULATION_CHANGED << 1;

    /**
     * Drapeau indiquant que l'animal a �t� tu�.
     */
    public static final int KILLED = SPECIES_CHANGED << 1;

    /**
     * Construit un nouvel �v�nement.
     *
     * @param source La source.
     * @param type Le {@linkplain #getType type de changement} qui est survenu.
     *        Ce type �tre n'importe quelle combinaison de 
     *
     *        {@link #POPULATION_CHANGED},
     *        {@link #SPECIES_CHANGED} et
     *        {@link #KILLED}.
     */
    public AnimalChangeEvent(final Animal source, final int type) {
        super(source, type);
    }

    /**
     * Retourne la source.
     */
    public Animal getSource() {
        return (Animal) super.getSource();
    }
}
