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

// Base de donn�es
import java.sql.SQLException;


/**
 * Interface de base des tables dans la base de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Table {
    /**
     * Lib�re les ressources utilis�es par cette table.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public abstract void close() throws SQLException;
}
