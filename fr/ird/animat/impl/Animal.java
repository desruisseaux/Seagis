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
 * Implémentation par défaut d'un animal.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * La population à laquelle appartient cet animal, ou <code>null</code>
     * si l'animal est mort.
     */
    private Population population;

    /**
     * Espèce à laquelle appartient cet animal.
     */
    private final Species species;

    /**
     * Horloge de l'animal. Chaque animal peut avoir une horloge qui lui est propre.
     * Toutes les horloges avancent au même rythme, mais leur temps 0 (qui correspond
     * à la naissance de l'animal) peuvent être différents.
     */
    private final Clock clock;

    /**
     * Le chemin suivit par l'animal depuis le début de la simulation. La méthode
     * {@link #move} agira typiquement sur cet objet en utilisant des méthodes telles
     * que {@link Path#rotate}, {@link Path#moveForward} ou {@link Path#moveToward}.
     */
    protected final Path path;

    /**
     * Les observations effectuées par cet animal depuis sa naissance.
     */
    private float[] observations;

    /**
     * Construit un animal à la position initiale spécifiée. L'animal n'appartiendra
     * initiallement à aucune population. 
     *
     * @param population La population à laquelle appartient cet animal.
     * @param species L'espèce de cet animal.
     * @param position Position initiale de l'animal, en degrés de longitudes et de latitudes.
     */
    public Animal(final Population population, final Species species, final Point2D position) {
        this.population = population;
        this.species    = species;
        this.clock      = population.getEnvironment().getClock().getNewClock();
        this.path       = new Path(position);
        population.animals.add(this);
        population.firePopulationChanged();
    }

    /**
     * Retourne la population à laquelle appartient cet animal.
     * Seuls les animaux vivants appartiennent à une population.
     *
     * @return La population à laquelle appartient cet animal,
     *         ou <code>null</code> si l'animal est mort.
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * Retourne l'espèce à laquelle appartient cet animal.
     *
     * @return L'espèce à laquelle appartient cet animal.
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Retourne l'horloge de l'animal.
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Retourne le chemin suivit par l'animal depuis le début
     * de la simulation jusqu'à maintenant. Les coordonnées
     * sont exprimées en degrés de longitudes et de latitudes.
     */
    public Shape getPath() {
        return path;
    }

    /**
     * Retourne les observations de l'animal à la date spécifiée.
     *
     * @param  time Date pour laquelle on veut les observations, ou <code>null</code> pour les
     *         dernières observations (c'est-à-dire celle qui ont été faites après le dernier
     *         déplacement).
     * @return Les observations de l'animal, ou <code>null</code> si la date spécifiée n'est pas
     *         pendant la durée de vie de cet animal.
     */
    public abstract Set<fr.ird.animat.Observation> getObservations(Date time);

    /**
     * Retourne la région jusqu'où s'étend la perception de cet
     * animal. Il peut s'agir par exemple d'un cercle centré sur
     * la position de l'animal.
     *
     * @param  time La date pour laquelle on veut la région perçue,
     *         ou <code>null</code> pour la région actuelle.
     * @param  La région perçue, ou <code>null</code> si la date
     *         spécifiée n'est pas pendant la durée de vie de cet animal.
     */
    public abstract Shape getPerceptionArea(Date time);

    /**
     * Fait avancer l'animal pendant le laps de temps spécifié. La vitesse à laquelle se
     * déplacera l'animal (et donc la distance qu'il parcourera) peuvent dépendre de son
     * état ou des conditions environnementales. Le comportement de l'animal dépendra de
     * l'implémentation. Il peut par exemple {@linkplain Path#rotate changer de cap}  et
     * {@linkplain Path#moveForward se déplacer vers ce cap}.  Il peut aussi {@linkplain
     * Path#moveToward se déplacer vers un certain point}, qu'il peut ne pas atteindre si
     * le laps de temps n'est pas suffisant.
     *
     * @param duration Durée du déplacement, en nombre de jours. Cette valeur est généralement
     *        la même que celle qui a été spécifiée à {@link Population#evoluate}.
     */
    protected abstract void move(float duration);

    /**
     * Fait migrer cette animal vers une nouvelle population.  Si cet animal appartient déjà à
     * la population spécifiée, rien ne sera fait. Sinon, l'animal sera retiré de son ancienne
     * population avant d'être ajouté à la population spécifiée.
     * <br><br>
     * Un animal peut changer de population par exemple lorsqu'il passe du stade larvaire
     * vers le stade juvenile. Puisqu'il a cessé de dériver et qu'il s'est mis à nager, on
     * peut considérer qu'il a rejoint une nouvelle population d'individus avec une autre
     * dynamique.
     *
     * @see Population#getAnimals
     * @see #kill
     */
    public void migrate(final Population population) {
        synchronized (getTreeLock()) {
            final Population oldPopulation = this.population;
            if (oldPopulation != population) {
                oldPopulation.animals.remove(this);
                this.population = population;
                population.animals.add(this);
                oldPopulation.firePopulationChanged();
                population.firePopulationChanged();
            }
        }
    }

    /**
     * Tue l'animal. L'animal n'appartiendra plus à aucune population.
     */
    public void kill() {
        synchronized (getTreeLock()) {
            if (population != null) try {
                population.animals.remove(this);
                population.firePopulationChanged();
            } finally {
                population = null;
            }
        }
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des accès à cet animal.
     */
    protected final Object getTreeLock() {
        final Population population = this.population;
        return (population!=null) ? population.getTreeLock() : this;
    }
}
