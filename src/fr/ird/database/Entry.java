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
package fr.ird.database;


/**
 * Interface de base des entr�es dans la base de donn�es.
 * Une entr�e peut repr�senter une s�rie d'images ou une
 * image individuelle par exemple.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Entry {
    /**
     * Retourne un num�ro unique identifiant cette entr�e.
     */
    public abstract int getID();

    /**
     * Retourne le nom de cette entr�e. Ce nom peut �tre arbitraire.
     * Dans le cas d'une image (par exemple), il s'agira le plus souvent
     * du nom du fichier de l'image.
     */
    public abstract String getName();

    /**
     * Retourne des remarques s'appliquant � cette entr�e,
     * ou <code>null</code> s'il n'y en a pas. Ces remarques
     * sont souvent une cha�ne descriptives qui peuvent �tre
     * affich�es comme "tooltip text".
     */
    public abstract String getRemarks();
}
