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

// Utilitaires
import java.util.Set;
import java.util.Map;
import java.util.Date;
import java.util.Random;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import javax.swing.event.EventListenerList;

// Journal des événements
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Géométrie
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

// Entrés/sorties
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;

// Remote Method Invocation (RMI)
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;

// Geotools
import org.geotools.cv.Coverage;
import org.geotools.cs.Ellipsoid;
import org.geotools.pt.CoordinatePoint;

// Seagis
import fr.ird.util.XArray;
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
public class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * Générateur de nombres aléatoires. Ces nombres peuvent être utilisés par exemple lorsque
     * aucune donnée n'est disponible mais qu'on ne veut pas laisser l'animal immobile.
     * L'implémentation par défaut de {@link #move} utilise ces nombres aléatoires.
     */
    protected static final Random random = new Random();

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
     * @param  species L'espèce de cet animal.
     * @param  population La population à laquelle appartient cet animal.
     * @param  position Position initiale de l'animal, en degrés de longitudes et de latitudes.
     * @throws RemoteException si l'exportation de cet animal a échoué.
     */
    protected Animal(final Species species, final Population population, final Point2D position)
            throws RemoteException
    {
        this.population = population;
        this.species    = species;
        this.clock      = population.getEnvironment().getClock().getNewClock();
        this.path       = new Path(position);
        final int port  = getRMIPort();
        if (port >= 0) {
            export(port);
        }
        population.animals.add(this);
        population.firePopulationChanged(this, true);
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
    public final Population getPopulation() {
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
        /*
         * Pas de synchronisation!  On va plutôt copier la référence vers 'observations' afin de
         * se protéger de tout changement fait dans un autre thread. On copie aussi la référence
         * vers 'species', mais ce cas n'est pas critique. Même si un changement survenait entre
         * la copie des deux références, ça n'aura pas d'impact parce que les propriétés de Species
         * que nous allons interroger ne peuvent pas avoir changées. Le seul cas problématique
         * serait lorsque la date demandée correspond au pas de temps en cours...
         */
        final Species      species = this.species;
        final float[] observations = this.observations;
        if (observations == null) {
            return null;
        }
        final int step, currentStep;
        if (time != null) {
            step = clock.computeStepSequenceNumber(time);
            if (step < 0) {
                return null;
            }
            currentStep = clock.getStepSequenceNumber();
        } else {
            step = currentStep = clock.getStepSequenceNumber();
        }
        /*
         * Vérifie si une synchronisation est nécessaire. Par prudence, on ne fera une
         * synchronisation que si l'utilisateur a demandé les observations pour le pas
         * de temps courant.  Ces observations peuvent être en cours de lecture,  d'où
         * la nécessité de se synchroniser.
         */
        if (step == currentStep) {
            final Object lock = getTreeLock();
            if (!Thread.holdsLock(lock)) {
                synchronized (lock) {
                    return getObservations(time);
                }
            }
        }
        /*
         * Copie les données demandées vers un tableau temporaire, qui sera lui-même enveloppé
         * dans un objet Map. Un traitement spécial est nécessaire pour les observations du
         * paramètre HEADING, étant donné que la position est mémorisée séparément du reste
         * (dans l'objet 'path').
         */
        int srcOffset;
        int dstOffset = 0;
        int length = species.getRecordLength();
        final float[] data = new float[length];
        if (species.headingIndex >= 0) {
            length   -= Parameter.HEADING.getNumSampleDimensions();
            srcOffset = step*length;
            dstOffset = species.offsets[species.headingIndex];
            System.arraycopy(observations, srcOffset, data, 0, dstOffset);
            data[dstOffset] = (float)path.getHeading(step);
            srcOffset += dstOffset;
            dstOffset++;
            path.getLocation(step, data, dstOffset);
            dstOffset  = species.offsets[species.headingIndex+1];
        } else {
            srcOffset = step*length;
        }
        assert length == species.getReducedRecordLength() : length;
        assert srcOffset + (data.length-dstOffset) == (step+1)*length : step;
        System.arraycopy(observations, srcOffset, data, dstOffset, data.length-dstOffset);
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
     * <br><br>
     * L'implémentation par défaut fait subir un déplacement aléatoire à l'animal.
     *
     * @param duration Durée du déplacement, en nombre de jours. Cette valeur est généralement
     *        la même que celle qui a été spécifiée à {@link Population#evoluate}.
     */
    protected void move(float duration) {
        path.rotate(10*random.nextGaussian());
        path.moveForward(30 * Math.min(1+random.nextGaussian(), 2) * duration);
    }

    /**
     * Mémorise des observations sur l'environnement actuel de l'animal. Cette méthode est
     * appelée automatiquement au moment de la création de l'animal ainsi qu'après chaque
     * {@linkplain Environment#nextTimeStep pas de temps} de l'environnement.
     *
     * L'implémentation par défaut puisera les données dans les
     * {@linkplain Environment#getCoverage(Parameter) couvertures} de chaque
     * {@linkplain Species#getObservedParameters paramètre intéressant cette espèce}.
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
            final CoordinatePoint   coord = new CoordinatePoint(path);
            final Shape    perceptionArea = getPerceptionArea(null);
            final Parameter[]  parameters = species.parameters;
            final int       reducedLength = species.getReducedRecordLength();
            final int                step = clock.getStepSequenceNumber();
            float[]               samples = new float[3];
            if (observations == null) {
                observations = new float[8*reducedLength];
            }
            int offset = reducedLength*step;
            if (offset >= observations.length) {
                observations = XArray.resize(observations,
                                             offset + Math.min(offset, reducedLength*1024));
            }
            for (int i=0; i<parameters.length; i++) {
                final Parameter parameter = parameters[i];
                if (i != species.headingIndex) {
                    final int length = parameter.getNumSampleDimensions();
                    samples = parameter.evaluate(this, coord, perceptionArea, samples);
                    System.arraycopy(samples, 0, observations, offset, length);
                    offset += length;
                } else {
                    path.setPointCount(step+1);
                }
            }
            assert offset == (step+1) * reducedLength;
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
                fireAnimalChanged(AnimalChangeEvent.POPULATION_CHANGED);
                oldPopulation.firePopulationChanged(this, false);
                population.firePopulationChanged(this, true);
            }
        }
    }

    /**
     * Change l'espèce de cet animal.
     *
     * @param species La nouvelle espèce de cette animal.
     */
    public void metamorphose(final Species species) {
        synchronized (getTreeLock()) {
            if (!Arrays.equals(this.species.parameters, species.parameters)) {
                throw new IllegalArgumentException("La nouvelle espèce doit observer les mêmes paramètres.");
            }
            if (species != this.species) {
                this.species = species;
                fireAnimalChanged(AnimalChangeEvent.SPECIES_CHANGED);
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
                population.firePopulationChanged(this, false);
            } finally {
                population = null;
            }
            fireAnimalChanged(AnimalChangeEvent.KILLED);
        }
    }




    ////////////////////////////////////////////////////////
    ////////                                        ////////
    ////////    E V E N T   L I S T E N E R S       ////////
    ////////                                        ////////
    ////////////////////////////////////////////////////////

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
     * Retourne le nombre d'objets intéressés à être informés des changements apportés à l'état
     * de cet animal. Cette information peut-être utilisée pour ce faire une idée du trafic qu'il
     * pourrait y avoir sur le réseau lorsque la simulation est exécutée sur une machine distante.
     */
    final int getListenerCount() {
        final EventListenerList listenerList = this.listenerList;
        return (listenerList!=null) ? listenerList.getListenerCount() : 0;
    }

    /**
     * A appeler à chaque fois que l'état de l'animal change.
     *
     * Cette méthode est habituellement appelée à l'intérieur d'un block synchronisé sur
     * {@link #getTreeLock()}. L'appel de {@link AnimalChangeListener#animalChanged} sera
     * mise en attente jusqu'à ce que le verrou sur <code>getTreeLock()</code> soit relâché.
     *
     * @param type Le {@linkplain AnimalChangeEvent#getType type de changement} qui est survenu.
     */
    protected void fireAnimalChanged(final int type) {
        final AnimalChangeEvent event = new AnimalChangeEvent(this, type);
        final Runnable run = new Runnable() {
            public void run() {
                assert Thread.holdsLock(getTreeLock());
                final EventListenerList listenerList = Animal.this.listenerList;
                if (listenerList != null) {
                    final Object[] listeners = listenerList.getListenerList();
                    for (int i=listeners.length; (i-=2)>=0;) {
                        if (listeners[i] == AnimalChangeListener.class) try {
                            ((AnimalChangeListener)listeners[i+1]).animalChanged(event);
                        } catch (RemoteException exception) {
                            Environment.listenerException("Animal", "fireAnimalChanged", exception);
                        }
                    }
                }
            }
        };
        final Population population = getPopulation();
        if (population != null) {
            final Environment environment = population.getEnvironment();
            if (environment != null) {
                environment.queue.invokeLater(run);
                return;
            }
        }
        run.run();
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des accès à cet animal.
     */
    protected final Object getTreeLock() {
        final Population population = this.population;
        return (population!=null) ? population.getTreeLock() : this;
    }




    //////////////////////////////////////////////////////////////////////////
    ////////                                                          ////////
    ////////    R E M O T E   M E T H O D   I N V O C A T I O N       ////////
    ////////                                                          ////////
    //////////////////////////////////////////////////////////////////////////

    /**
     * Retourne le numéro de port utilisé lorsque cet animal a été exporté,
     * or <code>-1</code> s'il n'a pas encore été exporté.
     */
    final int getRMIPort() {
        final Population population = this.population;
        return (population!=null) ? population.getRMIPort() : -1;
    }

    /**
     * Exporte cet animal de façon à ce qu'il puisse accepter les appels de machines distantes.
     *
     * @param  port Numéro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si cet animal n'a pas pu être exporté.
     */
    final void export(final int port) throws RemoteException {
        UnicastRemoteObject.exportObject(this, port);
        getSpecies().export(port);
    }

    /**
     * Annule l'exportation de cet animal. Si l'animal était déjà en train d'exécuter une méthode,
     * alors <code>unexport(...)</code> attendra un maximum d'une seconde avant de forcer l'arrêt
     * de l'exécution.
     */
    final void unexport() {
        unexport("Animal", this);
        getSpecies().unexport();
    }

    /**
     * Annule l'exportation d'un objet. Si l'objet était déjà en train d'exécuter une méthode,
     * alors <code>unexport(...)</code> attendra un maximum d'une seconde avant de forcer
     * l'arrêt de l'exécution.
     *
     * @param classname Le nom de la classe annulant l'exportation. Utilisé en cas d'erreur
     *                  pour formatter un message.
     * @param remote    L'objet dont on veut annuler l'exportation.
     */
    static final void unexport(final String classname, final Remote remote) {
        boolean force = false;
        do {
            for (int i=0; i<4; i++) {
                try {
                    if (UnicastRemoteObject.unexportObject(remote, force)) {
                        if (force) {
                            warning(classname, "La déconnexion a due être forcée", null);
                        }
                        return;
                    }
                } catch (NoSuchObjectException exception) {
                    warning(classname, "L'objet était déjà déconnecté", exception);
                    return;
                }
                try {
                    Thread.currentThread().sleep(250);
                } catch (InterruptedException exception) {
                    // Retourne au travail...
                }
            }
        } while ((force = !force) == true);
        warning(classname, "La déconnexion a échouée", null);
    }

    /**
     * Ajoute un avertissement dans le journal des événements.
     *
     * @param classname Le nom de la classe dans lequel l'avertissement est survenue.
     * @param message   Le message d'avertissement.
     * @param error     L'exception survenue, ou <code>null</code> s'il n'y en a pas.
     */
    static void warning(final String classname, final String message, final Exception error) {
        final LogRecord record = new LogRecord(Level.WARNING, message);
        record.setSourceClassName(classname);
        record.setSourceMethodName("unexport");
        if (error != null) {
            record.setThrown(error);
        }
        Logger.getLogger("fr.ird.animat").log(record);
    }

    /**
     * Enregistre cet objet. Cette méthode va d'abord éliminer la mémoire réservée
     * en trop afin de réduire la quantité d'informations qui seront enregistrées.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        synchronized (getTreeLock()) {
            observations = XArray.resize(observations,
                           species.getReducedRecordLength()*clock.getStepSequenceNumber());
            out.defaultWriteObject();
        }
    }
}
