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
import java.util.Map;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.rmi.Remote;

// Geotools (pour JavaDoc)
import org.geotools.cs.Ellipsoid;

// Animats
import fr.ird.animat.event.AnimalChangeListener;


/**
 * Représentation d'un animal. Chaque animal doit appartenir à une {@linkplain Species espèce}
 * ainsi qu'à une {@linkplain Population population}. Le terme &quot;animal&quot; est ici utilisé
 * au sens large. Un développeur pourrait très bien considérer la totalité d'un banc de poissons
 * comme une sorte de méga-animal.
 * <br><br>
 * Toutes les coordonnées spatiales sont exprimées en degrées de longitudes et de latitudes
 * selon l'ellipsoïde {@linkplain Ellipsoid#WGS84 WGS84}. Les déplacements sont exprimées en
 * milles nautiques, et les directions en degrés géographiques (c'est-à-dire par rapport au
 * nord &quot;vrai&quot;). Les intervalles de temps sont exprimés en nombre de jours.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Animal extends Remote {
    /**
     * Retourne l'espèce à laquelle appartient cet animal.
     *
     * @return L'espèce à laquelle appartient cet animal.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Species getSpecies() throws RemoteException;

    /**
     * Retourne la population à laquelle appartient cet animal.
     * Seuls les animaux vivants appartiennent à une population.
     *
     * @return La population à laquelle appartient cet animal, ou <code>null</code> si
     *         l'animal est {@linkplain #kill mort}.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Population getPopulation() throws RemoteException;

    /**
     * Retourne l'horloge de l'animal. Le {@linkplain Clock#getTime moment présent} de cette
     * horloge sera toujours identique à celui de {@linkplain Environment#getClock l'horloge
     * de l'environnement} (puisque le temps s'écoule de la même façon pour tous les animaux),
     * mais {@link Clock#getAge l'âge} peut être différent. Cet âge dépend de l'instant du
     * &quot;pas de temps 0&quot;, qui correspond à la naissance de l'animal.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Clock getClock() throws RemoteException;;

    /**
     * Retourne le chemin suivit par l'animal depuis le début
     * de la simulation jusqu'à maintenant. Les coordonnées
     * sont exprimées en degrés de longitudes et de latitudes.
     *
     * @return Le chemin suivit par l'animal depuis sa naissance.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Shape getPath() throws RemoteException;

    /**
     * Retourne les {@linkplain Observation observations} de l'animal à la date spécifiée. Le
     * nombre de {@linkplain Parameter paramètres} observés n'est pas nécessairement égal au
     * nombre de {@linkplain Environment#getParameters paramètres de l'environnement}, car un
     * animal peut ignorer les paramètres qui ne l'intéresse pas. A l'inverse, un animal peut
     * aussi faire quelques observations &quot;internes&quot; (par exemple la température de
     * ses muscles) qui ne font pas partie des paramètres de son environnement externe. En
     * général, {@linkplain fr.ird.animat.impl.Parameter#HEADING le cap et la position} de
     * l'animal feront partis des paramètres &quot;internes&quot; observés.
     *
     * @param  time Date pour laquelle on veut les observations, ou <code>null</code> pour les
     *         dernières observations (c'est-à-dire celle qui ont été faites après le dernier
     *         déplacement).
     * @return Les observations de l'animal, ou <code>null</code> si la date spécifiée n'est pas
     *         pendant la durée de vie de cet animal.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Map<Parameter,Observation> getObservations(Date time) throws RemoteException;

    /**
     * Retourne la région jusqu'où s'étend la perception de cet animal. Il peut s'agir par
     * exemple d'un cercle centré sur la position de l'animal.
     *
     * @param  time La date pour laquelle on veut la région perçue, ou <code>null</code>
     *         pour la région actuelle.
     * @param  La région perçue, ou <code>null</code> si la date spécifiée n'est pas pendant
     *         la durée de vie de cet animal.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Shape getPerceptionArea(Date time) throws RemoteException;

    /**
     * Tue l'animal. L'animal n'appartiendra plus à aucune population.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void kill() throws RemoteException;

    /**
     * Déclare un objet à informer des changements survenant dans l'état de cet animal.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void addAnimalChangeListener(AnimalChangeListener listener) throws RemoteException;

    /**
     * Retire un objet qui ne souhaite plus être informé des changements survenant
     * dans l'état de cet animal.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    void removeAnimalChangeListener(AnimalChangeListener listener) throws RemoteException;
}
