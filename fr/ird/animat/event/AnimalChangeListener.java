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
package fr.ird.animat.event;

// J2SE dependencies
import java.util.EventListener;
import java.rmi.RemoteException;
import java.rmi.Remote;


/**
 * Définit un objet qui écoutera les {@linkplain AnimalChangeEvent changements}
 * survenant dans l'état d'un {@link fr.ird.animat.Animal animal}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface AnimalChangeListener extends EventListener, Remote {
    /**
     * Appelée quand un animal a changé.
     *
     * @param  event L'événement décrivant le changement dans l'état d'un animal.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void animalChanged(AnimalChangeEvent event) throws RemoteException;
}
