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
import java.util.NoSuchElementException;
import java.rmi.RemoteException;
import java.rmi.Remote;

// OpenGIS
import org.opengis.cv.CV_Coverage;

// Animats
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Représentation de l'environnement dans lequel évolueront les animaux. Cet environnement peut
 * contenir un nombre arbitraire de {@linkplain Population populations}, qui contiendront chacune
 * un nombre arbitraire {@linkplain Animal d'animaux}. L'évolution d'un environnement est soumis
 * au rythme d'une {@linkplain Clock horloge}, qui imposera son rythme à tous les animaux nés
 * dans cet environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Environment extends Remote {
    /**
     * Retourne l'ensemble des populations évoluant dans cet environnement.
     * Les populations &quot;{@linkplain Population#kill mortes}&quot; ne
     * sont pas comprises dans cet ensemble.
     *
     * @return Les populations évoluant dans cet environnement.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     *
     * @see Population#getEnvironment
     */
    Set<Population> getPopulations() throws RemoteException;

    /**
     * Retourne l'ensemble des paramètres compris dans cet environnement. Les {@link Animal animaux}
     * vont généralement observer au moins quelque uns de ces paramètres à chaque pas de temps de la
     * simulation. Chaque animal ne va pas nécessairement observer tous les paramètres, et chaque
     * animal peut aussi observer des paramètres internes (par exemple la température de ses muscles)
     * qui ne font pas partie des paramètres de l'environnement retournés par cette méthode.
     *
     * @return Les paramètres compris dans cet environnement.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     *
     * @see Animal#getObservations
     */
    Set<Parameter> getParameters() throws RemoteException;

    /**
     * Retourne toute la {@linkplain CV_Coverage couverture spatiale des données} à la
     * {@linkplain Clock#getTime date courante} pour un paramètre spécifié.
     *
     * @param  parameter Le paramètre désiré.
     * @return La couverture spatiale des données pour le paramètre spécifié.
     *
     * @throws NoSuchElementException si le paramètre spécifié n'existe pas dans cet environnement.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     *
     * @see Animal#getObservations
     */
    CV_Coverage getCoverage(Parameter parameter) throws NoSuchElementException, RemoteException;

    /**
     * Retourne l'horloge de la simulation. Cet horloge tient à jour la date et heure (virtuelle)
     * courante ainsi que l'âge de la simulation. Cette horloge contrôle le rythme de l'ensemble
     * de la simulation. Bien que chaque animal peut avoir {@linkplain Animal#getClock sa propre
     * horloge}, ces horloges individuelles sont toutes synchronisées sur celle de l'environnement
     * et ne diffèrent que par l'âge de l'animal.
     *
     * @return L'horloge de la simulation.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Clock getClock() throws RemoteException;

    /**
     * Déclare un objet à informer des changements survenant dans cet environnement.
     * Ces changements surviennent à chaque fois que la simulation avance d'un pas de temps.
     *
     * @param  listener Écouteur à informer de tout changement dans cet environnement.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void addEnvironmentChangeListener(EnvironmentChangeListener listener) throws RemoteException;

    /**
     * Retire un objet qui ne souhaite plus être informé des changements survenant
     * dans cet environnement.
     *
     * @param  listener Écouteur ne désirant plus être informé des changements.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void removeEnvironmentChangeListener(EnvironmentChangeListener listener) throws RemoteException;
}
