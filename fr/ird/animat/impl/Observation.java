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
package fr.ird.animat.impl;


/**
 * Observations correspondant � un {@linkplain Parameter param�tre}. Un objet
 * <code>Observation</code> comprend g�n�ralement une valeur et la position �
 * laquelle cette observation a �t� faite. Un ensemble de ces observations
 * sont effectu�es � chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Observation {
    /**
     * Le param�tre correspondant � cette observation.
     */
    private final Parameter parameters;

    /**
     * Les donn�es 
     */

    /**
     */
    protected Observation(final Parameter parameters) {
        this.parameters = parameters;
    }
}
