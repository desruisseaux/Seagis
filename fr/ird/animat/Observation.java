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
 * Observations correspondant � un {@linkplain Parameter param�tre}. Un objet
 * <code>Observation</code> comprend g�n�ralement une valeur et la position �
 * laquelle cette observation a �t� faite. Un ensemble de ces observations
 * est effectu� � chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see Animal#getObservations
 * @see Environment#getParameters
 */
public interface Observation {
    /**
     * Retourne l'animal qui a fait cette observation.
     */
    Animal getAnimal();

    /**
     * Retourne le param�tre observ�.
     */
    Parameter getParameter();

    /**
     * Retourne la valeur de l'observation. Si aucune observation n'est disponible
     * (par exemple parce que l'animal se trouvait sous un nuage), alors cette m�thode
     * retourne {@link Float#NaN NaN}.
     *
     * @return La valeur de l'observation, ou {@link Float#NaN NaN}.
     */
    float getValue();

    /**
     * Retourne une position repr�sentative de l'observation. Si une telle position
     * n'est pas disponible, alors cette m�thode retourne <code>null</code>.
     *
     * @return La position de l'observation en degr�s de longitudes et de latitudes,
     *         ou <code>null</code>.
     */
    Point2D getLocation();
}
