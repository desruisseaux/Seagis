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
 * Interface de base des tables dans la base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Table extends Remote {
    /**
     * Libère les ressources utilisées par cette table.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws RemoteException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public abstract void close() throws RemoteException;
}
