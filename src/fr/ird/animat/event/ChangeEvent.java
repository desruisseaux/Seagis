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
import java.util.EventObject;
import fr.ird.animat.Animal;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;


/**
 * Un événement signalant que l'état d'un {@link Animal animal}, d'une {@link Population population}
 * ou d'un {@link Environment environnement] a changé.
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
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 6123591095950499007L;

    /**
     * Le type de changement qui est survenu. Ce champ peut être n'importe quelle combinaison des
     * constantes énumérées dans {@link AnimalChangeListener}, {@link PopulationChangeListener}
     * et {@link EnvironmentChangeListener}.
     */
    final int type;

    /**
     * Construit un nouvel événement.
     *
     * @param source La source.
     * @param type Le {@linkplain #getType type de changement} qui est survenu.
     */
    public ChangeEvent(final Object source, final int type) {
        super(source);
        this.type = type;
    }

    /**
     * Retourne le type de changement qui est survenu. Ce type peut être n'importe quelle
     * combinaison des constantes énumérées dans {@link AnimalChangeListener},
     * {@link PopulationChangeListener} et {@link EnvironmentChangeListener}.
     */
    public int getType() {
        return type;
    }

    /**
     * Retourne <code>true</code> si au moins un des changements spécifiés est survenu.
     * Le drapeau <code>flags</code> peut contenir n'importe quelle combinaison acceptée
     * par {@link #getType}.
     */
    public boolean changeOccured(final int flags) {
        return (getType() & flags) != 0;
    }
}
