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
package fr.ird.animat;

// J2SE dependencies
import java.awt.geom.Point2D;


/**
 * Une observation faite par un {@link Animal}. En g�n�ral, chaque animal
 * fera plusieurs observations de son environnement pendant ses d�placements.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Observation {
    /**
     * Retourne le nom du param�tre observ�.
     */
    public abstract String getName();

    /**
     * Retourne la valeur du param�tre observ�.
     */
    public abstract double getValue();

    /**
     * Retourne la position de la valeur �valu�e, ou <code>null</code>
     * si la valeur n'a pas �t� �valu�e a une position en particulier.
     */
    public abstract Point2D getLocation();
}
