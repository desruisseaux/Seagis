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
package fr.ird.animat.event;

// Dependencies
import fr.ird.animat.Animal;


/**
 * Un événement signalant que l'état d'un {@link Animal animal} a changé. La raison du changement
 * peut être une migration vers une nouvelle population ou une métamorphose en une nouvelle
 * espèce, mais n'inclus généralement pas les déplacements d'animaux étant donné que ces derniers
 * surviennent typiquement à chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see AnimalChangeListener
 */
public class AnimalChangeEvent extends ChangeEvent {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -8282372966814112917L;

    /**
     * Drapeau indiquant que l'animal a changé de population. Les événements de ce type
     * sont généralement lancés en même temps qu'un événement {@link PopulationChangeEvent}
     * de la population concernée.
     */
    public static final int POPULATION_CHANGED = PopulationChangeEvent.LAST << 1;

    /**
     * Drapeau indiquant que l'animal a changé d'espèce.
     */
    public static final int SPECIES_CHANGED = POPULATION_CHANGED << 1;

    /**
     * Drapeau indiquant que l'animal a été tué.
     */
    public static final int KILLED = SPECIES_CHANGED << 1;

    /**
     * Construit un nouvel événement.
     *
     * @param source La source.
     * @param type Le {@linkplain #getType type de changement} qui est survenu.
     *        Ce type être n'importe quelle combinaison de 
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
