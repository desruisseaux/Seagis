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
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;


/**
 * Définit un objet qui écoutera les {@linkplain PopulationChangeEvent changements}
 * survenant dans une {@link fr.ird.animat.Population population}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface PopulationChangeListener extends EventListener, Remote {
    /**
     * Appelée quand une population a changée.
     *
     * @param  event L'événement décrivant le changement dans une population.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void populationChanged(PopulationChangeEvent event) throws RemoteException;
}
