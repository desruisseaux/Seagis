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
package fr.ird.animat;

// J2SE dependencies
import java.awt.geom.Point2D;


/**
 * Observations correspondant à un {@linkplain Parameter paramètre}. Un objet
 * <code>Observation</code> comprend généralement une valeur et la position à
 * laquelle cette observation a été faite. Un ensemble de ces observations
 * est effectué à chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see Animal#getObservations
 * @see Environment#getParameters
 */
public interface Observation {
    /**
     * Retourne la valeur de l'observation. Si aucune observation n'est disponible
     * (par exemple parce que l'animal se trouvait sous un nuage), alors cette méthode
     * retourne {@link Float#NaN NaN}.
     *
     * @return La valeur de l'observation, ou {@link Float#NaN NaN}.
     */
    float value();

    /**
     * Retourne une position représentative de l'observation. Si une telle position
     * n'est pas disponible, alors cette méthode retourne <code>null</code>.
     *
     * @return La position de l'observation en degrés de longitudes et de latitudes,
     *         ou <code>null</code>.
     */
    Point2D location();
}
