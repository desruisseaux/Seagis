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
 * Repr�sentation d'un animal. Chaque animal doit appartenir � une {@linkplain Species esp�ce}
 * ainsi qu'� une {@linkplain Population population}. Le terme &quot;animal&quot; est ici utilis�
 * au sens large. Un d�veloppeur pourrait tr�s bien consid�rer la totalit� d'un banc de poissons
 * comme une sorte de m�ga-animal.
 * <br><br>
 * Toutes les coordonn�es spatiales sont exprim�es en degr�es de longitudes et de latitudes
 * selon l'ellipso�de {@linkplain Ellipsoid#WGS84 WGS84}. Les d�placements sont exprim�es en
 * milles nautiques, et les directions en degr�s g�ographiques (c'est-�-dire par rapport au
 * nord &quot;vrai&quot;). Les intervalles de temps sont exprim�s en nombre de jours.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Animal extends Remote {
    /**
     * Retourne l'esp�ce � laquelle appartient cet animal.
     *
     * @return L'esp�ce � laquelle appartient cet animal.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Species getSpecies() throws RemoteException;

    /**
     * Retourne la population � laquelle appartient cet animal.
     * Seuls les animaux vivants appartiennent � une population.
     *
     * @return La population � laquelle appartient cet animal, ou <code>null</code> si
     *         l'animal est {@linkplain #kill mort}.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Population getPopulation() throws RemoteException;

    /**
     * Retourne l'horloge de l'animal. Le {@linkplain Clock#getTime moment pr�sent} de cette
     * horloge sera toujours identique � celui de {@linkplain Environment#getClock l'horloge
     * de l'environnement} (puisque le temps s'�coule de la m�me fa�on pour tous les animaux),
     * mais {@link Clock#getAge l'�ge} peut �tre diff�rent. Cet �ge d�pend de l'instant du
     * &quot;pas de temps 0&quot;, qui correspond � la naissance de l'animal.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Clock getClock() throws RemoteException;;

    /**
     * Retourne le chemin suivit par l'animal depuis le d�but
     * de la simulation jusqu'� maintenant. Les coordonn�es
     * sont exprim�es en degr�s de longitudes et de latitudes.
     *
     * @return Le chemin suivit par l'animal depuis sa naissance.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Shape getPath() throws RemoteException;

    /**
     * Retourne les {@linkplain Observation observations} de l'animal � la date sp�cifi�e. Le
     * nombre de {@linkplain Parameter param�tres} observ�s n'est pas n�cessairement �gal au
     * nombre de {@linkplain Environment#getParameters param�tres de l'environnement}, car un
     * animal peut ignorer les param�tres qui ne l'int�resse pas. A l'inverse, un animal peut
     * aussi faire quelques observations &quot;internes&quot; (par exemple la temp�rature de
     * ses muscles) qui ne font pas partie des param�tres de son environnement externe. En
     * g�n�ral, {@linkplain fr.ird.animat.impl.Parameter#HEADING le cap et la position} de
     * l'animal feront partis des param�tres &quot;internes&quot; observ�s.
     *
     * @param  time Date pour laquelle on veut les observations, ou <code>null</code> pour les
     *         derni�res observations (c'est-�-dire celle qui ont �t� faites apr�s le dernier
     *         d�placement).
     * @return Les observations de l'animal, ou <code>null</code> si la date sp�cifi�e n'est pas
     *         pendant la dur�e de vie de cet animal.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Map<Parameter,Observation> getObservations(Date time) throws RemoteException;

    /**
     * Retourne la r�gion jusqu'o� s'�tend la perception de cet animal. Il peut s'agir par
     * exemple d'un cercle centr� sur la position de l'animal.
     *
     * @param  time La date pour laquelle on veut la r�gion per�ue, ou <code>null</code>
     *         pour la r�gion actuelle.
     * @param  La r�gion per�ue, ou <code>null</code> si la date sp�cifi�e n'est pas pendant
     *         la dur�e de vie de cet animal.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Shape getPerceptionArea(Date time) throws RemoteException;

    /**
     * Tue l'animal. L'animal n'appartiendra plus � aucune population.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void kill() throws RemoteException;

    /**
     * D�clare un objet � informer des changements survenant dans l'�tat de cet animal.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void addAnimalChangeListener(AnimalChangeListener listener) throws RemoteException;

    /**
     * Retire un objet qui ne souhaite plus �tre inform� des changements survenant
     * dans l'�tat de cet animal.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void removeAnimalChangeListener(AnimalChangeListener listener) throws RemoteException;
}
