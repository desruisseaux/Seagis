/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.coverage.SeriesEntry;


/**
 * Un paramètre environnemental.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface ParameterEntry extends Entry {
    /**
     * Retourne la série d'image à utiliser pour ce paramètre.
     *
     * @param n 0 pour la série principale, ou 1 pour la série de rechange à utiliser si
     *          jamais la série principale n'est pas disponible.
     * @return  La série d'images, ou <code>null</code> si aucune.
     *          Ce nom ne sera jamais nul pour <code>n=0</code>.
     */
    public abstract SeriesEntry getSeries(int n);

    /**
     * Retourne le numéro de la bande à utiliser dans les images.
     */
    public abstract int getBand();
}
