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

// Utilitaires
import java.util.Set;
import java.util.Map;
import java.util.Date;
import java.util.Random;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import javax.swing.event.EventListenerList;

// Journal des �v�nements
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// G�om�trie
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

// Entr�s/sorties
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
 * Impl�mentation par d�faut d'un animal. La {@linkplain Population population} et
 * {@linkplain Species l'esp�ce} initiales d'un animal doivent �tre sp�cifi�es d�s sa naissance
 * (c'est-�-dire d�s la cr�ation d'un objet <code>Animal</code>) mais peuvent �tre chang�es par
 * la suite, soit par la {@linkplain #migrate migration} vers une autre population, ou soit par
 * la {@linkplain #metamorphose m�tamorphose} en une autre &quot;esp�ce&quot;.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Animal extends RemoteObject implements fr.ird.animat.Animal {
    /**
     * G�n�rateur de nombres al�atoires. Ces nombres peuvent �tre utilis�s par exemple lorsque
     * aucune donn�e n'est disponible mais qu'on ne veut pas laisser l'animal immobile.
     * L'impl�mentation par d�faut de {@link #move} utilise ces nombres al�atoires.
     */
    protected static final Random random = new Random();

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
     * @param  species L'esp�ce de cet animal.
     * @param  population La population � laquelle appartient cet animal.
     * @param  position Position initiale de l'animal, en degr�s de longitudes et de latitudes.
     * @throws RemoteException si l'exportation de cet animal a �chou�.
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
     * Retourne la population � laquelle appartient cet animal.
     * Seuls les animaux vivants appartiennent � une population.
     *
     * @return La population � laquelle appartient cet animal,
     *         ou <code>null</code> si l'animal est mort.
     *
     * @see #migrate
     */
    public final Population getPopulation() {
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
        /*
         * Pas de synchronisation!  On va plut�t copier la r�f�rence vers 'observations' afin de
         * se prot�ger de tout changement fait dans un autre thread. On copie aussi la r�f�rence
         * vers 'species', mais ce cas n'est pas critique. M�me si un changement survenait entre
         * la copie des deux r�f�rences, �a n'aura pas d'impact parce que les propri�t�s de Species
         * que nous allons interroger ne peuvent pas avoir chang�es. Le seul cas probl�matique
         * serait lorsque la date demand�e correspond au pas de temps en cours...
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
         * V�rifie si une synchronisation est n�cessaire. Par prudence, on ne fera une
         * synchronisation que si l'utilisateur a demand� les observations pour le pas
         * de temps courant.  Ces observations peuvent �tre en cours de lecture,  d'o�
         * la n�cessit� de se synchroniser.
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
         * Copie les donn�es demand�es vers un tableau temporaire, qui sera lui-m�me envelopp�
         * dans un objet Map. Un traitement sp�cial est n�cessaire pour les observations du
         * param�tre HEADING, �tant donn� que la position est m�moris�e s�par�ment du reste
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
     * <br><br>
     * L'impl�mentation par d�faut fait subir un d�placement al�atoire � l'animal.
     *
     * @param duration Dur�e du d�placement, en nombre de jours. Cette valeur est g�n�ralement
     *        la m�me que celle qui a �t� sp�cifi�e � {@link Population#evoluate}.
     */
    protected void move(float duration) {
        path.rotate(10*random.nextGaussian());
        path.moveForward(30 * Math.min(1+random.nextGaussian(), 2) * duration);
    }

    /**
     * M�morise des observations sur l'environnement actuel de l'animal. Cette m�thode est
     * appel�e automatiquement au moment de la cr�ation de l'animal ainsi qu'apr�s chaque
     * {@linkplain Environment#nextTimeStep pas de temps} de l'environnement.
     *
     * L'impl�mentation par d�faut puisera les donn�es dans les
     * {@linkplain Environment#getCoverage(Parameter) couvertures} de chaque
     * {@linkplain Species#getObservedParameters param�tre int�ressant cette esp�ce}.
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
                fireAnimalChanged(AnimalChangeEvent.POPULATION_CHANGED);
                oldPopulation.firePopulationChanged(this, false);
                population.firePopulationChanged(this, true);
            }
        }
    }

    /**
     * Change l'esp�ce de cet animal.
     *
     * @param species La nouvelle esp�ce de cette animal.
     */
    public void metamorphose(final Species species) {
        synchronized (getTreeLock()) {
            if (!Arrays.equals(this.species.parameters, species.parameters)) {
                throw new IllegalArgumentException("La nouvelle esp�ce doit observer les m�mes param�tres.");
            }
            if (species != this.species) {
                this.species = species;
                fireAnimalChanged(AnimalChangeEvent.SPECIES_CHANGED);
            }
        }
    }

    /**
     * Tue l'animal. L'animal n'appartiendra plus � aucune population.
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
     * Retourne le nombre d'objets int�ress�s � �tre inform�s des changements apport�s � l'�tat
     * de cet animal. Cette information peut-�tre utilis�e pour ce faire une id�e du trafic qu'il
     * pourrait y avoir sur le r�seau lorsque la simulation est ex�cut�e sur une machine distante.
     */
    final int getListenerCount() {
        final EventListenerList listenerList = this.listenerList;
        return (listenerList!=null) ? listenerList.getListenerCount() : 0;
    }

    /**
     * A appeler � chaque fois que l'�tat de l'animal change.
     *
     * Cette m�thode est habituellement appel�e � l'int�rieur d'un block synchronis� sur
     * {@link #getTreeLock()}. L'appel de {@link AnimalChangeListener#animalChanged} sera
     * mise en attente jusqu'� ce que le verrou sur <code>getTreeLock()</code> soit rel�ch�.
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
     * Retourne l'objet sur lequel se synchroniser lors des acc�s � cet animal.
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
     * Retourne le num�ro de port utilis� lorsque cet animal a �t� export�,
     * or <code>-1</code> s'il n'a pas encore �t� export�.
     */
    final int getRMIPort() {
        final Population population = this.population;
        return (population!=null) ? population.getRMIPort() : -1;
    }

    /**
     * Exporte cet animal de fa�on � ce qu'il puisse accepter les appels de machines distantes.
     *
     * @param  port Num�ro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si cet animal n'a pas pu �tre export�.
     */
    final void export(final int port) throws RemoteException {
        UnicastRemoteObject.exportObject(this, port);
        getSpecies().export(port);
    }

    /**
     * Annule l'exportation de cet animal. Si l'animal �tait d�j� en train d'ex�cuter une m�thode,
     * alors <code>unexport(...)</code> attendra un maximum d'une seconde avant de forcer l'arr�t
     * de l'ex�cution.
     */
    final void unexport() {
        unexport("Animal", this);
        getSpecies().unexport();
    }

    /**
     * Annule l'exportation d'un objet. Si l'objet �tait d�j� en train d'ex�cuter une m�thode,
     * alors <code>unexport(...)</code> attendra un maximum d'une seconde avant de forcer
     * l'arr�t de l'ex�cution.
     *
     * @param classname Le nom de la classe annulant l'exportation. Utilis� en cas d'erreur
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
                            warning(classname, "La d�connexion a due �tre forc�e", null);
                        }
                        return;
                    }
                } catch (NoSuchObjectException exception) {
                    warning(classname, "L'objet �tait d�j� d�connect�", exception);
                    return;
                }
                try {
                    Thread.currentThread().sleep(250);
                } catch (InterruptedException exception) {
                    // Retourne au travail...
                }
            }
        } while ((force = !force) == true);
        warning(classname, "La d�connexion a �chou�e", null);
    }

    /**
     * Ajoute un avertissement dans le journal des �v�nements.
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
     * Enregistre cet objet. Cette m�thode va d'abord �liminer la m�moire r�serv�e
     * en trop afin de r�duire la quantit� d'informations qui seront enregistr�es.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        synchronized (getTreeLock()) {
            observations = XArray.resize(observations,
                           species.getReducedRecordLength()*clock.getStepSequenceNumber());
            out.defaultWriteObject();
        }
    }
}
