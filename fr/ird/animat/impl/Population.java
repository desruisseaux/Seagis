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
import java.util.Locale;
import java.util.Iterator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.EventListener;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.event.EventListenerList;
import java.rmi.server.RemoteObject;
import java.rmi.RemoteException;

// Animats
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;


/**
 * Implémentation par défaut d'une population d'animaux. Chaque population peut avoir sa
 * dynamique propre, et chaque {@linkplain Animal animal} dans une population peut avoir
 * un comportement différent. Une population évolue à chaque appel de {@link #evoluate}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Population extends RemoteObject implements fr.ird.animat.Population {
    /**
     * L'environnement dans lequel évolue cette population.
     * Peut être <code>null</code> si la population est "morte".
     */
    private Environment environment;

    /**
     * Ensemble des animaux de cette population. Cet ensemble est accédé par
     * les méthodes {@link Animal#migrate} et {@link Animal#kill} seulement.
     */
    final Set<fr.ird.animat.Animal> animals = new LinkedHashSet<fr.ird.animat.Animal>();

    /**
     * Liste immutable des animaux de cette population.
     */
    private final Set<fr.ird.animat.Animal> immutableAnimals = Collections.unmodifiableSet(animals);

    /**
     * Les limites de la distribution geographique de cette population,
     * ou <code>null</code> si elle n'a pas encore été calculée.
     */
    private transient Rectangle2D bounds;

    /**
     * Liste des objets intéressés à être informés
     * des changements apportés à cette population.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Construit une population initialement vide.
     *
     * @param environment Environnement Environnement de la population.
     */
    protected Population(final Environment environment) {
        this.environment = environment;
        environment.populations.add(this);
        environment.fireEnvironmentChanged(this, true);
    }

    /**
     * Retourne l'environnement dans lequel évolue cette population.
     * Si cette population n'existe plus (c'est-à-dire si {@link #kill}
     * a été appelée), alors cette méthode retourne <code>null</code>.
     */
    public final Environment getEnvironment() {
        return environment;
    }

    /**
     * Ajoute un nouvel animal dans cette population. L'animal sera de l'espèce spécifiée
     * et apparaîtra à la position initiale spécifiée.
     *
     * @param  species L'espèce de cet animal.
     * @param  position Position initiale de l'animal, en degrés de longitudes et de latitudes.
     * @return L'animal créé.
     */
    public Animal newAnimal(fr.ird.animat.Species species, final Point2D position) {
        if (!(species instanceof Species)) {
            final Locale[] locales = species.getLocales();
            final String[] names = new String[locales.length];
            for (int i=0; i<locales.length; i++) {
                names[i] = species.getName(locales[i]);
            }
            species = new Species(locales, names, species.getIcon().getColor());
        }
        // Le constructeur de 'Animal' ajoute automatiquement l'animal à cette population.
        return new Animal((Species)species, this, position);
    }

    /**
     * Retourne l'ensemble des animaux que contient cette population.
     */
    public Set<fr.ird.animat.Animal> getAnimals() {
        return immutableAnimals;
    }
    
    /**
     * Retourne les limites de la région géographique dans laquelle on retrouve
     * des animaux de cette population. Les coordonnées de la région retournée
     * sont en degrés de longitudes et de latitudes.
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
     * Tue tout les animaux de cette population et fait disparaître
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
            animals.clear(); // Par précaution.
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
     * Fait évoluer une population en fonction de son environnement. Cette méthode va
     * typiquement déplacer les {@linkplain Animal animaux} en appellant des méthodes
     * telles que {@link Path#moveToward}. Des individus peuvent aussi naître ou mourrir.
     * L'implémentation par défaut appelle {@link Animal#move} pour chaque animal de cette
     * population.
     *
     * @param  duration Durée de l'évolution, en nombre de jours. Cette durée est habituellement
     *         égale à <code>{@link #getEnvironment()}.{@link Environment#getStepSequenceNumber()
     *         getStepSequenceNumber()}.{@link TimeStep#getStepDuration getStepDuration()}</code>.
     */
    public void evoluate(final float duration) {
        synchronized (getTreeLock()) {
            for (final Iterator<fr.ird.animat.Animal> it=animals.iterator(); it.hasNext();) {
                ((Animal) it.next()).move(duration);
            }
        }
    }

    /**
     * Déclare un objet à informer des changements survenant dans cette
     * population. Ces changements inclus les espèces qui s'ajoutent ou
     * qui meurent, mais n'incluent pas les changements de positions des
     * animaux.
     */
    public void addPopulationChangeListener(PopulationChangeListener listener) {
        synchronized (getTreeLock()) {
            listenerList.add(PopulationChangeListener.class, listener);
        }
    }

    /**
     * Retire un objet à informer des changements survenant dans cette
     * population.
     */
    public void removePopulationChangeListener(final PopulationChangeListener listener) {
        synchronized (getTreeLock()) {
            listenerList.remove(PopulationChangeListener.class, listener);
        }
    }

    /**
     * A appeler à chaque fois que la population change.
     * Cette méthode est habituellement appelée à l'intérieur d'un block synchronisé sur
     * {@link #getTreeLock()}. L'appel de {@link PopulationChangeListener#populationChanged}
     * sera mise en attente jusqu'à ce que le verrou sur <code>getTreeLock()</code> soit relâché.
     *
     * @param type Le type de changement qui est survenu. Cet argument peut être une des
     *        constantes énumérées dans {@link PopulationChangeEvent}.
     */
    protected void firePopulationChanged(final int type) {
        firePopulationChanged(new PopulationChangeEvent(this, type, null, null));
    }

    /**
     * Préviens tous les objets intéressés qu'un animal a été ajouté ou supprimé.
     *
     * @param animal L'animal ajouté ou supprimé.
     * @param added <code>true</code> si l'animal a été ajouté, ou <code>false</code>
     *        s'il a été supprimé.
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
     * Préviens tous les objets intéressés que la population a changée.
     *
     * @param event Un objet décrivant le changement survenu.
     */
    private void firePopulationChanged(final PopulationChangeEvent event) {
        final Runnable run = new Runnable() {
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
        final Environment environment = getEnvironment();
        if (environment != null) {
            environment.queue.invokeLater(run);
        } else {
            run.run();
        }
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des accès à la population.
     */
    protected final Object getTreeLock() {
        final Environment environment = this.environment;
        return (environment!=null) ? environment.getTreeLock() : this;
    }
}
