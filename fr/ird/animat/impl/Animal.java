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
package fr.ird.animat.impl;

// J2SE
import java.util.Set;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.rmi.server.RemoteObject;

// Geotools
import org.geotools.cs.Ellipsoid;

// Animats
import fr.ird.animat.Species;


/**
 * Impl�mentation par d�faut d'un animal.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * La population � laquelle appartient cet animal.
     */
    Population population;

    /**
     * Horloge de l'animal. Chaque animal peut avoir une horloge qui lui est propre.
     * Toutes les horloges avancent au m�me rythme, mais leur temps 0 (qui correspond
     * � la naissance de l'animal) peuvent �tre diff�rents.
     */
    protected final Clock clock;

    /**
     * Le chemin suivit par l'animal depuis le d�but de la simulation. La m�thode
     * {@link #move} agira typiquement sur cet objet en utilisant des m�thodes telles
     * que {@link Path#rotate}, {@link Path#moveForward} ou {@link Path#moveToward}.
     */
    protected final Path path;

    /**
     * Construit un animal � la position initiale sp�cifi�e.
     *
     * @param position Position initiale de l'animal.
     * @param clock {@linkplain Environment#getClock Horloge de l'environnement} auquel sera soumis
     *        l'animal. L'animal aura {@linkplain #clock sa propre horloge} synchronis�e sur celle
     *        de l'environnement, mais avec un temps 0 (qui correspond � la naissance de l'animal)
     *        diff�rent.
     */
    public Animal(final Point2D position, final Clock clock) {
        this.path  = new Path(position);
        this.clock = clock.getNewClock();
    }

    /**
     * Retourne l'esp�ce � laquelle appartient cet animal.
     *
     * @return L'esp�ce � laquelle appartient cet animal.
     */
    public abstract Species getSpecies();

    /**
     * Retourne la population � laquelle appartient cet animal.
     * Seuls les animaux vivants appartiennent � une population.
     *
     * @return La population � laquelle appartient cet animal,
     *         ou <code>null</code> si l'animal est mort.
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * Retourne l'horloge de l'animal.
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Retourne le chemin suivit par l'animal depuis le d�but
     * de la simulation jusqu'� maintenant. Les coordonn�es
     * sont exprim�es en degr�s de longitudes et de latitudes.
     */
    public Shape getPath() {
        return path;
    }

    /**
     * Retourne les observations de l'animal � la date sp�cifi�e.
     *
     * @param  time Date pour laquelle on veut les observations, ou <code>null</code> pour les
     *         derni�res observations (c'est-�-dire celle qui ont �t� faites apr�s le dernier
     *         d�placement).
     * @return Les observations de l'animal, ou <code>null</code> si la date sp�cifi�e n'est pas
     *         pendant la dur�e de vie de cet animal.
     */
    public abstract Set<fr.ird.animat.Observation> getObservations(Date time);

    /**
     * Retourne la r�gion jusqu'o� s'�tend la perception de cet
     * animal. Il peut s'agir par exemple d'un cercle centr� sur
     * la position de l'animal.
     *
     * @param  time La date pour laquelle on veut la r�gion per�ue,
     *         ou <code>null</code> pour la r�gion actuelle.
     * @param  La r�gion per�ue, ou <code>null</code> si la date
     *         sp�cifi�e n'est pas pendant la dur�e de vie de cet animal.
     */
    public abstract Shape getPerceptionArea(Date time);

    /**
     * Fait avancer l'animal pendant le laps de temps sp�cifi�. La vitesse � laquelle se
     * d�placera l'animal (et donc la distance qu'il parcourera) peuvent d�pendre de son
     * �tat ou des conditions environnementales. Le comportement de l'animal d�pendra de
     * l'impl�mentation. Il peut par exemple {@linkplain Path#rotate changer de cap}  et
     * {@linkplain Path#moveForward se d�placer vers ce cap}.  Il peut aussi {@linkplain
     * Path#moveToward se d�placer vers un certain point}, qu'il peut ne pas atteindre si
     * le laps de temps n'est pas suffisant.
     *
     * @param duration Dur�e du d�placement, en nombre de jours. Cette valeur est g�n�ralement
     *        la m�me que celle qui a �t� sp�cifi�e � {@link Population#evoluate}.
     */
    protected abstract void move(float duration);

    /**
     * Tue l'animal. L'animal n'appartiendra plus � aucune population.
     */
    public void kill() {
        synchronized (getTreeLock()) {
            if (population != null) {
                population.kill(this);
                population = null;
            }
        }
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des acc�s � cet animal.
     */
    protected final Object getTreeLock() {
        final Population population = this.population;
        return (population!=null) ? population.getTreeLock() : this;
    }
}
