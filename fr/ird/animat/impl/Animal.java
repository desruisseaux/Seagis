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
import java.util.Map;
import java.util.Date;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import java.io.Serializable;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.rmi.server.RemoteObject;
import javax.swing.event.EventListenerList;

// Geotools
import org.geotools.cs.Ellipsoid;
import org.geotools.cv.Coverage;

// Seagis
import fr.ird.util.ArraySet;
import fr.ird.animat.event.AnimalChangeEvent;
import fr.ird.animat.event.AnimalChangeListener;


/**
 * Implémentation par défaut d'un animal. La {@linkplain Population population} et
 * {@linkplain Species l'espèce} initiales d'un animal doivent être spécifiées dès sa naissance
 * (c'est-à-dire dès la création d'un objet <code>Animal</code>) mais peuvent être changées par
 * la suite, soit par la {@linkplain #migrate migration} vers une autre population, ou soit par
 * la {@linkplain #metamorphose métamorphose} en une autre &quot;espèce&quot;.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * La population à laquelle appartient cet animal, ou <code>null</code> si l'animal est mort.
     *
     * @see #getPopulation
     * @see #migrate
     */
    private Population population;

    /**
     * Espèce à laquelle appartient cet animal.
     *
     * @see #getSpecies
     * @see #metamorphose
     */
    private Species species;

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
     * Les observations effectuées par cet animal depuis sa naissance. Cet objet doit
     * être séparé de <code>path</code> afin de ne pas être transmis inutilement via
     * le réseau lors de l'appel de {@link #getPath}.
     */
    private float[] observations;

    /**
     * Liste des objets intéressés à être informés des changements apportés à
     * cet animal. Ne sera construit que la première fois où il sera nécessaire.
     */
    private EventListenerList listenerList;

    /**
     * Construit un animal à la position initiale spécifiée.
     *
     * @param species L'espèce de cet animal.
     * @param population La population à laquelle appartient cet animal.
     * @param position Position initiale de l'animal, en degrés de longitudes et de latitudes.
     */
    public Animal(final Species species, final Population population, final Point2D position) {
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
     *
     * @see #migrate
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * Retourne l'espèce à laquelle appartient cet animal.
     *
     * @return L'espèce à laquelle appartient cet animal.
     *
     * @see #metamorphose
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
    public Map<fr.ird.animat.Parameter,fr.ird.animat.Observation> getObservations(Date time) {
        final int step = clock.computeStepSequenceNumber(time);
        if (step < 0) {
            return null;
        }
        int offset = 0;
        int length = species.getRecordLength();
        final float[] data = new float[length];
//        if (species.containsHeading) {
//            path.getLocation(step, data);
//            length -= (offset=2);
//        }
        System.arraycopy(observations, step*length, data, offset, length);
        return new Observations(species.parameters, data);
    }

    /**
     * Retourne la région jusqu'où s'étend la perception de cet animal. L'implémentation par
     * défaut retourne un cercle d'un rayon de 10 kilomètres centré autour de la position de
     * l'animal.
     *
     * @param  time La date pour laquelle on veut la région perçue,
     *         ou <code>null</code> pour la région actuelle.
     * @param  La région perçue, ou <code>null</code> si la date
     *         spécifiée n'est pas pendant la durée de vie de cet animal.
     */
    public Shape getPerceptionArea(final Date time) {
        final int step = clock.computeStepSequenceNumber(time);
        if (step < 0) {
            return null;
        }
        final RectangularShape area = species.getPerceptionArea();
        path.relativeToGeographic(area, step);
        return area;
    }

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
     * Mémorise des observations sur l'environnement actuel de l'animal.
     *
     * @throws IllegalStateException si cet animal est mort.
     */
    protected void observe() throws IllegalStateException {
        synchronized (getTreeLock()) {
            final Population population = getPopulation();
            if (population == null) {
                throw new IllegalStateException("L'animal est mort.");
            }
            final Environment environment = population.getEnvironment();
            float[] samples = new float[3];
            final Parameter[] parameters = species.parameters;
            for (int i=0; i<parameters.length; i++) {
                final Parameter parameter = parameters[i];
                final Coverage coverage = environment.getCoverage(parameter);
                samples = coverage.evaluate(null, samples); // TODO
            }
            final int timeStep = clock.getStepSequenceNumber();
        }
    }

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
     * @param  population La nouvelle population de cet animal.
     * @throws IllegalStateException si cet animal était mort.
     *
     * @see #getPopulation
     * @see Population#getAnimals
     */
    public void migrate(final Population population) throws IllegalStateException {
        synchronized (getTreeLock()) {
            final Population oldPopulation = this.population;
            if (oldPopulation == null) {
                throw new IllegalStateException("L'animal est mort.");
            }
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
     * 
     */
    public void metamorphose(final Species species) {
        synchronized (getTreeLock()) {
            if (!Arrays.equals(this.species.parameters, species.parameters)) {
                throw new IllegalArgumentException("La nouvelle espèce doit observer les mêmes paramètres.");
            }
            this.species = species;
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
     * Déclare un objet à informer des changements survenant dans l'état de cet animal.
     */
    public void addAnimalChangeListener(final AnimalChangeListener listener) {
        synchronized (getTreeLock()) {
            if (listenerList == null) {
                listenerList = new EventListenerList();
            }
            listenerList.add(AnimalChangeListener.class, listener);
        }
    }

    /**
     * Retire un objet qui ne souhaite plus être informé des changements survenant
     * dans l'état de cet animal.
     */
    public void removeAnimalChangeListener(final AnimalChangeListener listener) {
        synchronized (getTreeLock()) {
            if (listenerList != null) {
                listenerList.remove(AnimalChangeListener.class, listener);
                if (listenerList.getListenerCount() == 0) {
                    listenerList = null;
                }
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
