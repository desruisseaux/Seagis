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
 * Impl�mentation par d�faut d'un animal.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * Index (relatif au d�but d'un enregistrement) de la valeur d'une observation.
     */
    private static final int VALUE_OFFSET = 0;

    /**
     * Index (relatif au d�but d'un enregistrement) de la longitude d'une observation.
     */
    private static final int X_OFFSET = 1;

    /**
     * Index (relatif au d�but d'un enregistrement) de la latitude d'une observation.
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
     * Les observations effectu�es par cet animal depuis sa naissance.
     */
    private float[] observations;

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
     * Retourne tous les param�tres susceptibles d'int�resser cet animal. Cet ensemble de
     * param�tres doit �tre immutable et constant pendant toute la dur�e de vie de l'animal.
     *
     * @return L'ensemble des param�tres suceptibles d'int�resser l'animal durant les pas de
     *         temps pass�s, pendant le pas de temps courant ou dans un pas de temps futur.
     */
    public abstract Set<fr.ird.animat.Parameter> getParameters();

    /**
     * Retourne les observations de l'animal � la date sp�cifi�e.
     *
     * @param  time Date pour laquelle on veut les observations, ou <code>null</code> pour les
     *         derni�res observations (c'est-�-dire celle qui ont �t� faites apr�s le dernier
     *         d�placement).
     * @return Les observations de l'animal, ou <code>null</code> si la date sp�cifi�e n'est pas
     *         pendant la dur�e de vie de cet animal.
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
     * Retourne l'objet sur lequel se synchroniser lors des acc�s � cet animal.
     */
    protected final Object getTreeLock() {
        final Population population = this.population;
        return (population!=null) ? population.getTreeLock() : this;
    }

    /**
     * Une observation de l'animal. Un ensemble de ces observations est retourn� par la m�thode
     * {@link Animal#getObservations}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Observation implements fr.ird.animat.Observation, Serializable {
        /**
         * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
         */
//        private static final long serialVersionUID = -1934991927931117874L;

        /**
         * Le param�tre observ�.
         */
        private final Parameter parameter;

        /**
         * L'ensemble des observations de cet animal pour le pas de temps examin�.
         * Ce tableau n'est qu'un extrait du tableau {@link Animal#observations},
         * qui contient la totalit� des observations de l'animal. Nous n'utilisons
         * qu'un extrait afin d'acc�l�rer les transfert via le r�seau dans le cas
         * d'une utilisation avec les RMI. Toutefois, tous les objets {@link Observation}
         * d'un m�me pas de temps partageront une r�f�rence vers le m�me tableau
         * <code>observations</code>, afin de diminuer la charge sur le ramasse-miette.
         *
         * Ce champ sera ajust� par {@link Animal#getObservations} apr�s la
         * construction de cet objet.
         */
        float[] observations;

        /**
         * Position de l'observation dans le tableau {@link #observations}.
         */
        private final int offset;

        /**
         * Construit une observation pour le param�tre sp�cifi�.
         *
         * @param parameter    Le param�tre observ�.
         * @param offset       Index du premier �l�ment � prendre en compte dans
         *                     {@link #observation}.
         */
        public Observation(final Parameter parameter,
                           final int offset)
        {
            this.parameter = parameter;
            this.offset    = offset;
        }

        /**
         * Retourne le param�tre observ�.
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
         * Retourne une position repr�sentative de l'observation, ou <code>null</code>
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
