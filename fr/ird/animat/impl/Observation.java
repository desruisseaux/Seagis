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
package fr.ird.animat.impl;


/**
 * Observations correspondant à un {@linkplain Parameter paramètre}. Un objet
 * <code>Observation</code> comprend généralement une valeur et la position à
 * laquelle cette observation a été faite. Un ensemble de ces observations
 * sont effectuées à chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Observation {
    /**
     * Le paramètre correspondant à cette observation.
     */
    private final Parameter parameters;

    /**
     * Les données 
     */

    /**
     */
    protected Observation(final Parameter parameters) {
        this.parameters = parameters;
    }
}
