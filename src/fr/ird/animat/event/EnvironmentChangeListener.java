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
package fr.ird.animat.event;

// J2SE dependencies
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;


/**
 * Définit un objet qui écoutera les {@linkplain EnvironmentChangeEvent changements}
 * survenant dans un {@linkplain fr.ird.animat.Environment environnement}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface EnvironmentChangeListener extends EventListener, Remote {
    /**
     * Appelée quand un environnement a changé.
     *
     * @param  event L'événement décrivant le changement dans l'environnement.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void environmentChanged(EnvironmentChangeEvent event) throws RemoteException;
}
