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
package fr.ird.animat.event;

// Dependencies
import java.util.EventObject;
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
public class AnimalChangeEvent extends EventObject {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -3863761339872054925L;

    /**
     * Construit un nouvel événement.
     *
     * @param source La source.
     */
    public AnimalChangeEvent(final Animal source) {
        super(source);
    }

    /**
     * Retourne la source.
     */
    public Animal getSource() {
        return (Animal) super.getSource();
    }
}
