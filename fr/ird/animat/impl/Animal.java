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
import java.util.Iterator;
import java.io.Serializable;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.rmi.server.RemoteObject;

// Geotools
import org.geotools.cs.Ellipsoid;
import org.geotools.cv.Coverage;

// Seagis
import fr.ird.animat.Species;
import fr.ird.util.ArraySet;


/**
 * Implémentation par défaut d'un animal.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * Index (relatif au début d'un enregistrement) de la valeur d'une observation.
     */
    private static final int VALUE_OFFSET = 0;

    /**
     * Index (relatif au début d'un enregistrement) de la longitude d'une observation.
     */
    private static final int X_OFFSET = 1;

    /**
     * Index (relatif au début d'un enregistrement) de la latitude d'une observation.
     */
    private static final int Y_OFFSET = 2;

    /**
     * La longueur des enregistrements lorsqu'ils comprennent la position de l'observation.
     */
    private static final int LOCATED_LENGTH = 3;

    /**
     * La longueur des enregistrements lorsqu'ils ne comprennent que la valeur de l'observation.
     */
    private static final int SCALAR_LENGTH = 1;

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
     * Les observations effectuées par cet animal depuis sa naissance.
     */
    private float[] observations;

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
     * Retourne tous les paramètres susceptibles d'intéresser cet animal. Cet ensemble de
     * paramètres doit être immutable et constant pendant toute la durée de vie de l'animal.
     *
     * @return L'ensemble des paramètres suceptibles d'intéresser l'animal durant les pas de
     *         temps passés, pendant le pas de temps courant ou dans un pas de temps futur.
     */
    public abstract Set<fr.ird.animat.Parameter> getParameters();

    /**
     * Retourne les observations de l'animal à la date spécifiée.
     *
     * @param  time Date pour laquelle on veut les observations, ou <code>null</code> pour les
     *         dernières observations (c'est-à-dire celle qui ont été faites après le dernier
     *         déplacement).
     * @return Les observations de l'animal, ou <code>null</code> si la date spécifiée n'est pas
     *         pendant la durée de vie de cet animal.
     */
    public Set<fr.ird.animat.Observation> getObservations(Date time) {
        final Set<fr.ird.animat.Parameter> parameters = getParameters();
        final Observation[] obs = new Observation[parameters.size()];
        int i=0,offset=0;
        for (final Iterator it=parameters.iterator(); it.hasNext();) {
            final Parameter param = (Parameter) it.next();
            obs[i] = new Observation(param, offset);
            offset += param.isLocalized() ? LOCATED_LENGTH : SCALAR_LENGTH;
            i++;
        }
        final float[] data = new float[offset];
        offset *= clock.getStepSequenceNumber(time);
        System.arraycopy(observations, offset, data, 0, data.length);
        while (--i>=0) {
            obs[i].observations = data;
        }
        return new ArraySet((fr.ird.animat.Observation[]) obs);
    }

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
            float[] samples = environment.tmpSamples;
            float[] buffer  = environment.tmpObservations;
            final Set<fr.ird.animat.Parameter> parameters = getParameters();
            for (final Iterator<fr.ird.animat.Parameter> it=parameters.iterator(); it.hasNext();) {
                final Parameter parameter = (Parameter) it.next();
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
     * Retourne l'objet sur lequel se synchroniser lors des accès à cet animal.
     */
    protected final Object getTreeLock() {
        final Population population = this.population;
        return (population!=null) ? population.getTreeLock() : this;
    }

    /**
     * Une observation de l'animal. Un ensemble de ces observations est retourné par la méthode
     * {@link Animal#getObservations}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Observation implements fr.ird.animat.Observation, Serializable {
        /**
         * Numéro de série pour compatibilité entre différentes versions.
         */
//        private static final long serialVersionUID = -1934991927931117874L;

        /**
         * Le paramètre observé.
         */
        private final Parameter parameter;

        /**
         * L'ensemble des observations de cet animal pour le pas de temps examiné.
         * Ce tableau n'est qu'un extrait du tableau {@link Animal#observations},
         * qui contient la totalité des observations de l'animal. Nous n'utilisons
         * qu'un extrait afin d'accélérer les transfert via le réseau dans le cas
         * d'une utilisation avec les RMI. Toutefois, tous les objets {@link Observation}
         * d'un même pas de temps partageront une référence vers le même tableau
         * <code>observations</code>, afin de diminuer la charge sur le ramasse-miette.
         *
         * Ce champ sera ajusté par {@link Animal#getObservations} après la
         * construction de cet objet.
         */
        float[] observations;

        /**
         * Position de l'observation dans le tableau {@link #observations}.
         */
        private final int offset;

        /**
         * Construit une observation pour le paramètre spécifié.
         *
         * @param parameter    Le paramètre observé.
         * @param offset       Index du premier élément à prendre en compte dans
         *                     {@link #observation}.
         */
        public Observation(final Parameter parameter,
                           final int offset)
        {
            this.parameter = parameter;
            this.offset    = offset;
        }

        /**
         * Retourne le paramètre observé.
         */
        public Parameter getParameter() {
            return parameter;
        }
        
        /**
         * Retourne la valeur de l'observation, ou {@link Float#NaN} si elle n'est pas disponible.
         */
        public float getValue() {
            return observations[offset + VALUE_OFFSET];
        }
        
        /**
         * Retourne une position représentative de l'observation, ou <code>null</code>
         * si elle n'est pas disponible.
         */
        public Point2D getLocation() {
            if (parameter.isLocalized()) {
                final float x = observations[offset + X_OFFSET];
                final float y = observations[offset + Y_OFFSET];
                if (!Float.isNaN(x) || !Float.isNaN(y)) {
                    return new Point2D.Float(x,y);
                }
            }
            return null;
        }
    }
}
