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
 */
package fr.ird.database;

// Base de données
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Base interface for tables in an observation database. <code>Table</code> objects
 * are produced by various methods in {@link DataBase} sub-interfaces. Each
 * <code>Table</code> can in turn produces {@link Entry} objects.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Table extends Remote {
    /**
     * Returns the database object where this table come from.
     *
     * @throws RemoteException if a problem occured while fetching this information.
     */
    public abstract DataBase getDataBase() throws RemoteException;

    /**
     * Release any resources used by this table.
     *
     * @throws RemoteException if a problem occured while releasing the resources.
     */
    public abstract void close() throws RemoteException;
}
