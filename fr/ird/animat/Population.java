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
 * Une population d'animaux. Chaque population appartient � un seul {@linkplain Environment
 * environnement}. Une population peut contenir plusieurs {@linkplain Animal animaux} de
 * diff�rentes {@linkplain Species esp�ces}, et peut aussi comprendre quelques r�gles qui
 * gouvernent les d�placements de l'ensemble de ces individus. Chaque population peut avoir
 * sa dynamique propre, et chaque {@linkplain Animal animal} dans une population peut avoir
 * un comportement diff�rent.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Population extends Remote {
    /**
     * Retourne l'environnement dans lequel �volue cette population.
     * Si cette population n'existe plus (c'est-�-dire si {@link #kill}
     * a �t� appel�e), alors cette m�thode retourne <code>null</code>.
     *
     * @param  L'environnement dans lequel �volue cette population.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Environment getEnvironment() throws RemoteException;

    /**
     * Retourne l'ensemble des animaux appartenant � cette population.
     *
     * @param  L'ensemble des animaux appartenant � cette population.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Set<Animal> getAnimals() throws RemoteException;

    /**
     * Retourne les limites de la r�gion g�ographique dans laquelle on retrouve
     * des animaux de cette population. Les coordonn�es de la r�gion retourn�e
     * sont en degr�s de longitudes et de latitudes.
     *
     * @return Les limites de la distribution geographique de cette population.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Shape getSpatialBounds() throws RemoteException;

    /**
     * Tue tout les animaux de cette population et fait dispara�tre
     * la population de son {@link Environment environnement}.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void kill() throws RemoteException;

    /**
     * D�clare un objet � informer des changements survenant dans cette
     * population. Ces changements inclus les esp�ces qui s'ajoutent ou
     * qui meurent, ainsi que les changements de positions des animaux.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void addPopulationChangeListener(PopulationChangeListener listener) throws RemoteException;

    /**
     * Retire un objet qui ne souhaite plus �tre inform� des changements survenant
     * dans cette population.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void removePopulationChangeListener(PopulationChangeListener listener) throws RemoteException;
}
