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
package fr.ird.animat.event;

// J2SE dependencies
import java.util.EventListener;
import java.rmi.RemoteException;
import java.rmi.Remote;


/**
 * D�finit un objet qui �coutera les changements survenant dans un environnement.
 * Un environnement peut changer suite � un changement de date, ainsi que suite �
 * l'ajout ou la supression de populations. Toutefois, cela n'inclu pas les ajouts,
 * suppressions ou d�placements d'animaux au sein d'une population; ces derniers
 * changements sont plut�t observ�s par {@link PopulationChangeListener}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface EnvironmentChangeListener extends EventListener, Remote {
    /**
     * Appel�e quand un environnement a chang�.
     *
     * @param  event L'�v�nement d�crivant le changement d'environnement.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void environmentChanged(EnvironmentChangeEvent event) throws RemoteException;
}
