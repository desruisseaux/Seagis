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
package fr.ird.animat;

// J2SE
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.EventListener;
import javax.swing.event.EventListenerList;

// Animats
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;


/**
 * Une population d'animaux. Chaque population appartient à un seul {@link Environment}.
 * Une population peut contenir plusieurs individus {@link Animal} de différentes espèces,
 * et peut aussi comprendre quelques règles qui gouvernent les déplacements de l'ensemble
 * de ces individus. Chaque population peut avoir sa dynamique propre, et chaque {@linkplain
 * Animal animal} dans une population peut avoir un comportement différent. Une population
 * évolue à chaque appel de {@link #evoluate}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Population {
    /**
     * L'environnement dans lequel évolue cette population. Peut être <code>null</code> si la
     * population est "morte". Ce champ est mis-à-jour par {@link Environment#addPopulation}.
     */
    Environment environment;

    /**
     * Ensemble des animaux de cette population.
     */
    private final Set<Animal> animals = new LinkedHashSet<Animal>();

    /**
     * Liste immutable des animaux de cette population.
     */
    private final Set<Animal> immutableAnimals = Collections.unmodifiableSet(animals);

    /**
     * Liste des objets intéressés à être informés
     * des changements apportés à cette population.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Evénement indiquant qu'un changement est survenu dans la population.
     */
    private final PopulationChangeEvent event = new PopulationChangeEvent(this);

    /**
     * Construit une population initialement vide.
     *
     * @param  environment L'environnement dans lequel évoluera cette population.
     */
    public Population() {
    }

    /**
     * Retourne l'environnement dans lequel évolue cette population.
     * Si cette population n'existe plus (c'est-à-dire si {@link #kill}
     * a été appelée), alors cette méthode retourne <code>null</code>.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Ajoute l'animal spécifiée à cette population population. Si l'animal appartient déjà
     * à cette population, rien ne sera fait. Sinon, si l'animal appartenait à une autre
     * population, alors elle sera retirée de son ancienne population avant d'être ajouté
     * à celle-ci.
     * <br><br>
     * Un animal peut changer de population par exemple lorsqu'il passe du stade larvaire
     * vers le stade juvenile. Puisqu'il a cessé de dériver et qu'il s'est mis à nager, on
     * peut considérer qu'il a rejoint une nouvelle population d'individus avec une autre
     * dynamique.
     *
     * @param animal L'animal à ajouter.
     *
     * @see #getAnimals
     * @see Animal#kill
     */
    public void addAnimal(final Animal animal) {
        final Population oldPopulation = animal.population;
        if (oldPopulation != this) {
            if (oldPopulation != null) {
                oldPopulation.animals.remove(this);
                animal.population = null;
                oldPopulation.firePopulationChanged();
            }
            animals.add(animal);
            animal.population = this;
            firePopulationChanged();
        }
    }

    /**
     * Utilisé par {@link Animal#kill} seulement. Cette méthode existe
     * uniquement parce que l'ensemble {@link #animals} est privé.
     */
    final void kill(final Animal animal) {
        animals.remove(animal);
    }

    /**
     * Retourne l'ensemble des animaux que contient cette population.
     */
    public Set<Animal> getAnimals() {
        return immutableAnimals;
    }

    /**
     * Tue tout les animaux de cette population et fait disparaître
     * la population de l'{@link Environment environnement}.
     */
    public void kill() {
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
        if (environment != null) {
            environment.kill(this);
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

    /**
     * Fait évoluer une population en fonction de son environnement.
     * Cette méthode va typiquement déplacer les {@linkplain Animal animaux}
     * en appellant des méthodes telles que {@link Animal#moveToward}. Des
     * individus peuvent aussi naître ou mourrir.
     *
     * @param  duration Durée de l'évolution, en nombre de jours.
     *         Cette durée est habituellement égale à
     *         <code>{@link #getEnvironment()}.{@link Environment#getTimeStep()
     *         getTimeStep()}.{@link TimeStep#getDuration getDuration()}</code>.
     */
    public abstract void evoluate(float duration);

    /**
     * Déclare un objet à informer des changements survenant dans cette
     * population. Ces changements inclus les espèces qui s'ajoutent ou
     * qui meurent, mais n'incluent pas les changements de positions des
     * animaux.
     */
    public void addPopulationChangeListener(PopulationChangeListener listener) {
        listenerList.add(PopulationChangeListener.class, listener);
    }

    /**
     * Retire un objet à informer des changements survenant dans cette
     * population.
     */
    public void removePopulationChangeListener(final PopulationChangeListener listener) {
        listenerList.remove(PopulationChangeListener.class, listener);
    }

    /**
     * A appeler à chaque fois que la population change.
     */
    protected void firePopulationChanged() {
        final Object[] listeners = listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;) {
            if (listeners[i] == PopulationChangeListener.class) {
                ((PopulationChangeListener)listeners[i+1]).populationChanged(event);
            }
        }
    }
}
