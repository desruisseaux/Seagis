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
import fr.ird.animat.Environment;


/**
 * Un événement signalant que l'{@linkplain Environment environnement} a changé.
 * Un environnement peut changer suite à un changement de date, ainsi que suite à
 * l'ajout ou la supression de populations. Toutefois, cela n'inclu pas les ajouts
 * ou suppressions d'animaux au sein d'une population; ces derniers changements étant
 * plutôt signalés par {@link PopulationChangeEvent}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see EnvironmentChangeListener
 */
public class EnvironmentChangeEvent extends EventObject {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 756503959183321278L;

    /**
     * Construit un nouvel événement.
     *
     * @param source  La source.
     * @param parameters Paramètres qui ont changés, ou <code>null</code> si aucun.
     */
    public EnvironmentChangeEvent(final Environment source) {
        super(source);
    }

    /**
     * Retourne la source.
     */
    public Environment getSource() {
        return (Environment) super.getSource();
    }
}
