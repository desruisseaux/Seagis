/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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

// J2SE
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Une simulation qui peut être exécutée sur une machine distante. Chaque simulation contient
 * un et un seul {@linkplain Environment environnement}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Simulation extends Remote {
    /**
     * Groupe de threads utilisés pour la simulation. Les implémentations qui construisent
     * de nouveau thread (par exemple lors de l'appel de la méthode {@link #start}) devraient
     * placer leurs threads dans ce groupe.
     */
    ThreadGroup THREAD_GROUP = new ThreadGroup("Animat simulation");

    /**
     * Retourne le nom de cette simulation.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    String getName() throws RemoteException;

    /**
     * Lance la simulation dans un {@linkplain Thread thread} de basse priorité. Si une
     * simulation est déjà en cours, alors cette méthode ne fait rien. Dans tous les cas,
     * cette méthode retourne immédiatement.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void start() throws RemoteException;

    /**
     * Arrête la simulation. Cette méthode peut être utilisée pour prendre une pause.
     * La simulation peut être {@linkplain #start redémarrée} de nouveau après avoir
     * été arrêtée.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void stop() throws RemoteException;

    /**
     * Retourne l'environnement de la simulation.
     *
     * @return L'environnement de la simulation.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Environment getEnvironment() throws RemoteException;

    /**
     * Retourne une propriété de la simulation. La liste des propriétés peut contenir des
     * informations supplémentaires qui ne figure pas parmis les autres méthodes de cette
     * interface.
     *
     * @param  name Le nom de la propriété.
     * @return La valeur de la propriété spécifiée, ou <code>null</code> si aucune
     *         propriété n'est définie pour le nom spécifiée.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    String getProperty(String name) throws RemoteException;
}
