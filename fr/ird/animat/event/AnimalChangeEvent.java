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
package fr.ird.animat.event;

// Dependencies
import java.util.EventObject;
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
public class AnimalChangeEvent extends EventObject {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -3863761339872054925L;

    /**
     * Construit un nouvel �v�nement.
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
