/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.image.sql;


/**
 * Interface de base des entr�es dans la base de donn�es.
 * Une entr�e peut repr�senter une s�rie d'images ou une
 * image individuelle par exemple.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Entry
{
    /**
     * Retourne le nom de cette entr�e.
     */
    public abstract String getName();

    /**
     * Retourne un num�ro unique identifiant cette entr�e.
     */
    public abstract int getID();
}
