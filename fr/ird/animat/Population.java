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
package fr.ird.animat;

// J2SE standard
import java.util.Set;
import java.awt.Shape;
import java.rmi.Remote;
import java.rmi.RemoteException;

// Animats
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;


/**
 * Une population d'animaux. Chaque population appartient à un seul {@linkplain Environment
 * environnement}. Une population peut contenir plusieurs {@linkplain Animal animaux} de
 * différentes {@linkplain Species espèces}, et peut aussi comprendre quelques règles qui
 * gouvernent les déplacements de l'ensemble de ces individus. Chaque population peut avoir
 * sa dynamique propre, et chaque {@linkplain Animal animal} dans une population peut avoir
 * un comportement différent.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Population extends Remote {
    /**
     * Retourne l'environnement dans lequel évolue cette population.
     * Si cette population n'existe plus (c'est-à-dire si {@link #kill}
     * a été appelée), alors cette méthode retourne <code>null</code>.
     *
     * @param  L'environnement dans lequel évolue cette population.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Environment getEnvironment() throws RemoteException;

    /**
     * Retourne l'ensemble des animaux appartenant à cette population.
     *
     * @param  L'ensemble des animaux appartenant à cette population.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Set<Animal> getAnimals() throws RemoteException;

    /**
     * Retourne les limites de la région géographique dans laquelle on retrouve
     * des animaux de cette population. Les coordonnées de la région retournée
     * sont en degrés de longitudes et de latitudes.
     *
     * @return Les limites de la distribution geographique de cette population.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Shape getSpatialBounds() throws RemoteException;

    /**
     * Tue tout les animaux de cette population et fait disparaître
     * la population de son {@link Environment environnement}.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void kill() throws RemoteException;

    /**
     * Déclare un objet à informer des changements survenant dans cette
     * population. Ces changements inclus les espèces qui s'ajoutent ou
     * qui meurent, ainsi que les changements de positions des animaux.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void addPopulationChangeListener(PopulationChangeListener listener) throws RemoteException;

    /**
     * Retire un objet qui ne souhaite plus être informé des changements survenant
     * dans cette population.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void removePopulationChangeListener(PopulationChangeListener listener) throws RemoteException;
}
