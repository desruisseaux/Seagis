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
import java.util.EventObject;
import fr.ird.animat.Animal;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;


/**
 * Un �v�nement signalant que l'�tat d'un {@link Animal animal}, d'une {@link Population population}
 * ou d'un {@link Environment environnement] a chang�.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see AnimalChangeListener
 * @see PopulationChangeListener
 * @see EnvironmentChangeListener
 */
public class ChangeEvent extends EventObject {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 6123591095950499007L;

    /**
     * Le type de changement qui est survenu. Ce champ peut �tre n'importe quelle combinaison des
     * constantes �num�r�es dans {@link AnimalChangeListener}, {@link PopulationChangeListener}
     * et {@link EnvironmentChangeListener}.
     */
    final int type;

    /**
     * Construit un nouvel �v�nement.
     *
     * @param source La source.
     * @param type Le {@linkplain #getType type de changement} qui est survenu.
     */
    public ChangeEvent(final Object source, final int type) {
        super(source);
        this.type = type;
    }

    /**
     * Retourne le type de changement qui est survenu. Ce type peut �tre n'importe quelle
     * combinaison des constantes �num�r�es dans {@link AnimalChangeListener},
     * {@link PopulationChangeListener} et {@link EnvironmentChangeListener}.
     */
    public int getType() {
        return type;
    }

    /**
     * Retourne <code>true</code> si au moins un des changements sp�cifi�s est survenu.
     * Le drapeau <code>flags</code> peut contenir n'importe quelle combinaison accept�e
     * par {@link #getType}.
     */
    public boolean changeOccured(final int flags) {
        return (getType() & flags) != 0;
    }
}
