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
 * Impl�mentation par d�faut d'un animal. La {@linkplain Population population} et
 * {@linkplain Species l'esp�ce} initiales d'un animal doivent �tre sp�cifi�es d�s sa naissance
 * (c'est-�-dire d�s la cr�ation d'un objet <code>Animal</code>) mais peuvent �tre chang�es par
 * la suite, soit par la {@linkplain #migrate migration} vers une autre population, ou soit par
 * la {@linkplain #metamorphose m�tamorphose} en une autre &quot;esp�ce&quot;.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * La population � laquelle appartient cet animal, ou <code>null</code> si l'animal est mort.
     *
     * @see #getPopulation
     * @see #migrate
     */
    private Population population;

    /**
     * Esp�ce � laquelle appartient cet animal.
     *
     * @see #getSpecies
     * @see #metamorphose
     */
    private Species species;

    /**
     * Horloge de l'animal. Chaque animal peut avoir une horloge qui lui est propre.
     * Toutes les horloges avancent au m�me rythme, mais leur temps 0 (qui correspond
     * � la naissance de l'animal) peuvent �tre diff�rents.
     */
    private final Clock clock;

    /**
     * Le chemin suivit par l'animal depuis le d�but de la simulation. La m�thode
     * {@link #move} agira typiquement sur cet objet en utilisant des m�thodes telles
     * que {@link Path#rotate}, {@link Path#moveForward} ou {@link Path#moveToward}.
     */
    protected final Path path;

    /**
     * Les observations effectu�es par cet animal depuis sa naissance. Cet objet doit
     * �tre s�par� de <code>path</code> afin de ne pas �tre transmis inutilement via
     * le r�seau lors de l'appel de {@link #getPath}.
     */
    private float[] observations;

    /**
     * Liste des objets int�ress�s � �tre inform�s des changements apport�s �
     * cet animal. Ne sera construit que la premi�re fois o� il sera n�cessaire.
     */
    private EventListenerList listenerList;

    /**
     * Construit un animal � la position initiale sp�cifi�e.
     *
     * @param species L'esp�ce de cet animal.
     * @param population La population � laquelle appartient cet animal.
     * @param position Position initiale de l'animal, en degr�s de longitudes et de latitudes.
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
     * Retourne la population � laquelle appartient cet animal.
     * Seuls les animaux vivants appartiennent � une population.
     *
     * @return La population � laquelle appartient cet animal,
     *         ou <code>null</code> si l'animal est mort.
     *
     * @see #migrate
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * Retourne l'esp�ce � laquelle appartient cet animal.
     *
     * @return L'esp�ce � laquelle appartient cet animal.
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
     * Retourne la r�gion jusqu'o� s'�tend la perception de cet animal. L'impl�mentation par
     * d�faut retourne un cercle d'un rayon de 10 kilom�tres centr� autour de la position de
     * l'animal.
     *
     * @param  time La date pour laquelle on veut la r�gion per�ue,
     *         ou <code>null</code> pour la r�gion actuelle.
     * @param  La r�gion per�ue, ou <code>null</code> si la date
     *         sp�cifi�e n'est pas pendant la dur�e de vie de cet animal.
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
     * M�morise des observations sur l'environnement actuel de l'animal.
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
     * Fait migrer cette animal vers une nouvelle population.  Si cet animal appartient d�j� �
     * la population sp�cifi�e, rien ne sera fait. Sinon, l'animal sera retir� de son ancienne
     * population avant d'�tre ajout� � la population sp�cifi�e.
     * <br><br>
     * Un animal peut changer de population par exemple lorsqu'il passe du stade larvaire
     * vers le stade juvenile. Puisqu'il a cess� de d�river et qu'il s'est mis � nager, on
     * peut consid�rer qu'il a rejoint une nouvelle population d'individus avec une autre
     * dynamique.
     *
     * @param  population La nouvelle population de cet animal.
     * @throws IllegalStateException si cet animal �tait mort.
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
                throw new IllegalArgumentException("La nouvelle esp�ce doit observer les m�mes param�tres.");
            }
            this.species = species;
        }
    }

    /**
     * Tue l'animal. L'animal n'appartiendra plus � aucune population.
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
     * D�clare un objet � informer des changements survenant dans l'�tat de cet animal.
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
     * Retire un objet qui ne souhaite plus �tre inform� des changements survenant
     * dans l'�tat de cet animal.
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
     * Retourne l'objet sur lequel se synchroniser lors des acc�s � cet animal.
     */
    protected final Object getTreeLock() {
        final Population population = this.population;
        return (population!=null) ? population.getTreeLock() : this;
    }
}
