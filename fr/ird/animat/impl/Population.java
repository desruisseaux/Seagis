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
import java.util.Iterator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.EventListener;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.event.EventListenerList;
import java.rmi.server.RemoteObject;
import java.rmi.RemoteException;

// Animats
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
public abstract class Population extends RemoteObject implements fr.ird.animat.Population {
    /**
     * L'environnement dans lequel �volue cette population.
     * Peut �tre <code>null</code> si la population est "morte".
     */
    private Environment environment;

    /**
     * Ensemble des animaux de cette population. Cet ensemble est acc�d� par
     * les m�thodes {@link Animal#migrate} et {@link Animal#kill} seulement.
     */
    final Set<fr.ird.animat.Animal> animals = new LinkedHashSet<fr.ird.animat.Animal>();

    /**
     * Liste immutable des animaux de cette population.
     */
    private final Set<fr.ird.animat.Animal> immutableAnimals = Collections.unmodifiableSet(animals);

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
     * Ev�nement indiquant qu'un changement est survenu dans la population.
     */
    private final PopulationChangeEvent event = new PopulationChangeEvent(this);

    /**
     * Classe � ex�cuter lorsque l'environnement a chang�.
     *
     * @see #fireEnvironmentChanged()
     */
    private final Runnable firePopulationChanged = new Runnable() {
        public void run() {
            assert Thread.holdsLock(getTreeLock());
            final Object[] listeners = listenerList.getListenerList();
            for (int i=listeners.length; (i-=2)>=0;) {
                if (listeners[i] == PopulationChangeListener.class) try {
                    ((PopulationChangeListener)listeners[i+1]).populationChanged(event);
                } catch (RemoteException exception) {
                    Environment.listenerException("Population", "firePopulationChanged", exception);
                }
            }
        }
    };

    /**
     * Construit une population initialement vide.
     *
     * @param environment Environnement Environnement de la population.
     */
    public Population(final Environment environment) {
        this.environment = environment;
        environment.fireEnvironmentChanged();
    }

    /**
     * Retourne l'environnement dans lequel �volue cette population.
     * Si cette population n'existe plus (c'est-�-dire si {@link #kill}
     * a �t� appel�e), alors cette m�thode retourne <code>null</code>.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Retourne l'ensemble des animaux que contient cette population.
     */
    public Set<fr.ird.animat.Animal> getAnimals() {
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
                for (final Iterator<fr.ird.animat.Animal> it=animals.iterator(); it.hasNext();) {
                    final Animal animal = (Animal) it.next();
                    final Rectangle2D b = animal.path.getBounds2D();
                    if (bounds == null) {
                        bounds = b;
                    } else {
                        bounds.add(b);
                    }
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
            if (environment != null) {
                environment.populations.remove(this);
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
     * Fait �voluer une population en fonction de son environnement.
     * Cette m�thode va typiquement d�placer les {@linkplain Animal animaux}
     * en appellant des m�thodes telles que {@link Animal#moveToward}. Des
     * individus peuvent aussi na�tre ou mourrir.
     *
     * @param  duration Dur�e de l'�volution, en nombre de jours.
     *         Cette dur�e est habituellement �gale �
     *         <code>{@link #getEnvironment()}.{@link Environment#getStepSequenceNumber()
     *         getStepSequenceNumber()}.{@link TimeStep#getStepDuration getStepDuration()}</code>.
     */
    public abstract void evoluate(float duration);

    /**
     * D�clare un objet � informer des changements survenant dans cette
     * population. Ces changements inclus les esp�ces qui s'ajoutent ou
     * qui meurent, mais n'incluent pas les changements de positions des
     * animaux.
     */
    public void addPopulationChangeListener(PopulationChangeListener listener) {
        synchronized (getTreeLock()) {
            listenerList.add(PopulationChangeListener.class, listener);
        }
    }

    /**
     * Retire un objet � informer des changements survenant dans cette
     * population.
     */
    public void removePopulationChangeListener(final PopulationChangeListener listener) {
        synchronized (getTreeLock()) {
            listenerList.remove(PopulationChangeListener.class, listener);
        }
    }

    /**
     * A appeler � chaque fois que la population change.
     *
     * Cette m�thode est habituellement appel�e � l'int�rieur d'un block synchronis� sur
     * {@link #getTreeLock()}. L'appel de {@link PopulationChangeListener#populationChanged}
     * sera mise en attente jusqu'� ce que le verrou sur <code>getTreeLock()</code> soit rel�ch�.
     */
    protected void firePopulationChanged() {
        if (environment != null) {
            environment.queue.invokeLater(firePopulationChanged);
        } else {
            firePopulationChanged.run();
        }
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des acc�s � la population.
     */
    protected final Object getTreeLock() {
        final Environment environment = this.environment;
        return (environment!=null) ? environment.getTreeLock() : this;
    }
}
