/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 * D�finit un objet qui �coutera les {@linkplain AnimalChangeEvent changements}
 * survenant dans l'�tat d'un {@link fr.ird.animat.Animal animal}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface AnimalChangeListener extends EventListener, Remote {
    /**
     * Appel�e quand un animal a chang�.
     *
     * @param  event L'�v�nement d�crivant le changement dans l'�tat d'un animal.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void animalChanged(AnimalChangeEvent event) throws RemoteException;
}
