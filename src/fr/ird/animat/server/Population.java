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
package fr.ird.animat.server;

// Utilitaires
import java.util.Set;
import java.util.Locale;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.EventListener;
import javax.swing.event.EventListenerList;

// G�om�trie
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Remote Method Invocation (RMI)
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteObject;
import java.rmi.RemoteException;

// Animats
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;


/**
 * Impl�mentation par d�faut d'une population d'animaux. Chaque population peut avoir sa
 * dynamique propre, et chaque {@linkplain Animal animal} dans une population peut avoir
 * un comportement diff�rent. Une population �volue � chaque appel de {@link #evoluate}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Population extends RemoteObject implements fr.ird.animat.Population {
    /**
     * L'environnement dans lequel �volue cette population.
     * Peut �tre <code>null</code> si la population est "morte".
     */
    private Environment environment;

    /**
     * Ensemble des animaux de cette population. Cet ensemble est acc�d� par
     * les m�thodes {@link Animal#migrate} et {@link Animal#kill} seulement.
     */
    final Set<Animal> animals = new LinkedHashSet<Animal>();

    /**
     * Liste immutable des animaux de cette population.
     */
    private final Set<Animal> immutableAnimals = Collections.unmodifiableSet(animals);

    /**
     * Les limites de la distribution geographique de cette population,
     * ou <code>null</code> si elle n'a pas encore �t� calcul�e.
     */
    private transient Rectangle2D bounds;

    /**
     * Liste des objets int�ress�s � �tre inform�s
     * des changements apport�s � cette population.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Construit une population initialement vide.
     *
     * @param environment Environnement Environnement de la population.
     * @throws RemoteException si l'exportation de la population a �chou�.
     */
    protected Population(final Environment environment) throws RemoteException {
        this.environment = environment;
        environment.populations.add(this);
        environment.fireEnvironmentChanged(this, true);
        final int port = getRMIPort();
        if (port >= 0) {
            export(port);
        }
    }

    /**
     * Retourne l'environnement dans lequel �volue cette population.
     * Si cette population n'existe plus (c'est-�-dire si {@link #kill}
     * a �t� appel�e), alors cette m�thode retourne <code>null</code>.
     */
    public final Environment getEnvironment() {
        return environment;
    }

    /**
     * Ajoute un nouvel animal dans cette population. L'impl�mentation par d�fault retourne
     * <code>new Animal(species, this, position)</code>. Le constructeur de {@link Animal}
     * se charge d'ajouter automatiquement le nouvel animal � cette population.
     *
     * @param  species L'esp�ce de cet animal.
     * @param  position Position initiale de l'animal, en degr�s de longitudes et de latitudes.
     * @return L'animal cr��.
     * @throws IllegalStateException si cette population est morte.
     * @throws RemoteException si l'exportation du nouvel animal a �chou�.
     */
    public Animal newAnimal(fr.ird.animat.Species species, final Point2D position)
            throws IllegalStateException, RemoteException
    {
        synchronized (getTreeLock()) {
            if (environment == null) {
                throw new IllegalStateException("Cette population est morte.");
            }
            return new Animal(Species.wrap(species), this, position);
        }
    }

    /**
     * Retourne l'ensemble des animaux que contient cette population.
     */
    public Set<? extends Animal> getAnimals() {
        return immutableAnimals;
    }
    
    /**
     * Retourne les limites de la r�gion g�ographique dans laquelle on retrouve
     * des animaux de cette population. Les coordonn�es de la r�gion retourn�e
     * sont en degr�s de longitudes et de latitudes.
     *
     * @return Les limites de la distribution geographique de cette population.
     */
    public Shape getSpatialBounds() {
        synchronized (getTreeLock()) {
            if (bounds == null) {
                for (final Animal animal : animals) {
                    final Rectangle2D b = animal.path.getBounds2D();
                    if (bounds == null) {
                        bounds = b;
                    } else {
                        bounds.add(b);
                    }
                }
                if (bounds == null) {
                    return null;
                }
            }
            return (Shape) bounds.clone();
        }
    }

    /**
     * Tue tout les animaux de cette population et fait dispara�tre
     * la population de l'{@link Environment environnement}.
     */
    public void kill() {
        synchronized (getTreeLock()) {
            /*
             * On ne peut pas utiliser Iterator, parce que les appels
             * de Animals.kill() vont modifier l'ensemble.
             */
            final Animal[] pop = (Animal[]) animals.toArray(new Animal[animals.size()]);
            for (int i=0; i<pop.length; i++) {
                pop[i].kill();
            }
            assert animals.isEmpty() : animals.size();
            animals.clear(); // Par pr�caution.
            if (environment != null) try {
                environment.populations.remove(this);
                environment.fireEnvironmentChanged(this, false);
            } finally {
                environment = null;
            }
            /*
             * Retire tous les 'listeners'.
             */
            final Object[] listeners = listenerList.getListenerList();
            for (int i=listeners.length; (i-=2)>=0;) {
                listenerList.remove((Class)         listeners[i  ],
                                    (EventListener) listeners[i+1]);
            }
            assert listenerList.getListenerCount() == 0;
        }
    }

    /**
     * Fait �voluer une population en fonction de son environnement. Cette m�thode va
     * typiquement d�placer les {@linkplain Animal animaux} en appellant des m�thodes
     * telles que {@link Path#moveToward}. Des individus peuvent aussi na�tre ou mourrir.
     * L'impl�mentation par d�faut appelle {@link Animal#move} pour chaque animal de cette
     * population.
     *
     * @param  duration Dur�e de l'�volution, en nombre de jours. Cette dur�e est habituellement
     *         �gale � <code>{@link #getEnvironment()}.{@link Environment#getStepSequenceNumber()
     *         getStepSequenceNumber()}.{@link TimeStep#getStepDuration getStepDuration()}</code>.
     */
    public void evoluate(final float duration) {
        synchronized (getTreeLock()) {
            for (final Animal animal : animals) {
                animal.move(duration);
            }
            bounds = null;
        }
    }

    /**
     * Demande � chaque animal d'observer son environnement. Cette m�thode est appel�e
     * automatiquement au moment de la cr�ation de l'animal ainsi qu'apr�s chaque
     * {@linkplain Environment#nextTimeStep pas de temps} de l'environnement.
     */
    protected void observe() {
        synchronized (getTreeLock()) {
            for (final Animal animal : animals) {
                animal.observe();
            }
        }
    }




    ////////////////////////////////////////////////////////
    ////////                                        ////////
    ////////    E V E N T   L I S T E N E R S       ////////
    ////////                                        ////////
    ////////////////////////////////////////////////////////

    /**
     * D�clare un objet � informer des changements survenant dans cette
     * population. Ces changements inclus les esp�ces qui s'ajoutent ou
     * qui meurent, mais n'incluent pas les changements de positions des
     * animaux.
     */
    public void addPopulationChangeListener(PopulationChangeListener listener) {
        synchronized (listenerList) {
            listenerList.add(PopulationChangeListener.class, listener);
        }
    }

    /**
     * Retire un objet � informer des changements survenant dans cette
     * population.
     */
    public void removePopulationChangeListener(final PopulationChangeListener listener) {
        synchronized (listenerList) {
            listenerList.remove(PopulationChangeListener.class, listener);
        }
    }

    /**
     * Retourne le nombre d'objets int�ress�s � �tre inform�s des changements apport�s � la
     * population ou � l'�tat d'un de ses animaux. Cette information peut-�tre utilis�e pour
     * ce faire une id�e du trafic qu'il pourrait y avoir sur le r�seau lorsque la simulation
     * est ex�cut�e sur une machine distante.
     */
    final int getListenerCount() {
        int count = listenerList.getListenerCount();
        synchronized (getTreeLock()) {
            for (final Animal animal : animals) {
                count += animal.getListenerCount();
            }
        }
        return count;
    }

    /**
     * Pr�viens tous les objets int�ress�s qu'un animal a �t� ajout� ou supprim�.
     *
     * @param animal L'animal ajout� ou supprim�.
     * @param added <code>true</code> si l'animal a �t� ajout�, ou <code>false</code>
     *        s'il a �t� supprim�.
     */
    final void firePopulationChanged(final Animal animal, final boolean added) {
        final Set<fr.ird.animat.Animal> change = Collections.singleton((fr.ird.animat.Animal)animal);
        final PopulationChangeEvent event;
        if (added) {
            event = new PopulationChangeEvent(this, PopulationChangeEvent.ANIMALS_ADDED, change, null);
        } else {
            event = new PopulationChangeEvent(this, PopulationChangeEvent.ANIMALS_REMOVED, null, change);
        }
        firePopulationChanged(event);
    }

    /**
     * Pr�viens tous les objets int�ress�s que la population a chang�e.
     * Cette m�thode est habituellement appel�e � l'int�rieur d'un bloc synchronis� sur
     * {@link #getTreeLock()}. L'appel de {@link PopulationChangeListener#populationChanged}
     * sera mise en attente jusqu'� ce que le verrou sur <code>getTreeLock()</code> soit rel�ch�.
     *
     * @param event Un objet d�crivant le changement survenu.
     */
    protected void firePopulationChanged(final PopulationChangeEvent event) {
        final Object[] listeners;
        synchronized (listenerList) {
            listeners = listenerList.getListenerList();
        }
        final Runnable run = new Runnable() {
            public void run() {
                assert Thread.holdsLock(getTreeLock());
                for (int i=listeners.length; (i-=2)>=0;) {
                    if (listeners[i] == PopulationChangeListener.class) try {
                        ((PopulationChangeListener)listeners[i+1]).populationChanged(event);
                    } catch (RemoteException exception) {
                        Environment.listenerException("Population", "firePopulationChanged", exception);
                    }
                }
            }
        };
        final Environment environment = getEnvironment();
        if (environment != null) {
            environment.queue.invokeLater(run);
        } else {
            run.run();
        }
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des acc�s � la population.
     */
    protected final Object getTreeLock() {
        final Environment environment = this.environment;
        return (environment!=null) ? environment.getTreeLock() : this;
    }




    //////////////////////////////////////////////////////////////////////////
    ////////                                                          ////////
    ////////    R E M O T E   M E T H O D   I N V O C A T I O N       ////////
    ////////                                                          ////////
    //////////////////////////////////////////////////////////////////////////

    /**
     * Retourne le num�ro de port utilis� lorsque cette population a �t� export�e,
     * or <code>-1</code> s'il n'a pas encore �t� export�e.
     */
    final int getRMIPort() {
        final Environment environment = this.environment;
        return (environment!=null) ? environment.getRMIPort() : -1;
    }

    /**
     * Exporte cette population et tous les animaux qu'elle contient de fa�on � ce qu'ils
     * puissent accepter les appels de machines distantes.
     *
     * @param  port Num�ro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si cette population n'a pas pu �tre export�e.
     */
    final void export(final int port) throws RemoteException {
        synchronized (getTreeLock()) {
            for (final Animal animal : animals) {
                animal.export(port);
            }
            UnicastRemoteObject.exportObject(this, port);
        }
    }

    /**
     * Annule l'exportation de cette population. Si la population ou un de ses animaux �tait d�j�
     * en train d'ex�cuter une m�thode, alors <code>unexport(...)</code> attendra quelques secondes
     * avant de forcer l'arr�t de l'ex�cution.
     */
    final void unexport() {
        synchronized (getTreeLock()) {
            for (final Animal animal : animals) {
                animal.unexport();
            }
            Animal.unexport("Population", this);
        }
    }
}
