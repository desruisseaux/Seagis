/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.database.coverage;

// Base de données
import fr.ird.database.Entry;


/**
 * Interface des entrées représentant une série d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SeriesEntry extends Entry {
    /**
     * Retourne la période &quot;normale&quot; des images de cette série
     * (en nombre de jours), ou {@link Double#NaN} si elle est inconnue.
     */
    public abstract double getPeriod();
}
