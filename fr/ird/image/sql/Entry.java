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
package fr.ird.image.sql;


/**
 * Interface de base des entrées dans la base de données.
 * Une entrée peut représenter une série d'images ou une
 * image individuelle par exemple.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Entry
{
    /**
     * Retourne le nom de cette entrée.
     */
    public abstract String getName();

    /**
     * Retourne un numéro unique identifiant cette entrée.
     */
    public abstract int getID();
}
