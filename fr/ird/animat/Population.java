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
 * Une population d'animaux. Chaque population appartient � un seul {@link Environment}.
 * Une population peut contenir plusieurs individus {@link Animal} de diff�rentes esp�ces,
 * et peut aussi comprendre quelques r�gles qui gouvernent les d�placements de l'ensemble
 * de ces individus. Chaque population peut avoir sa dynamique propre, et chaque {@linkplain
 * Animal animal} dans une population peut avoir un comportement diff�rent. Une population
 * �volue � chaque appel de {@link #evoluate}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Population {
    /**
     * L'environnement dans lequel �volue cette population. Peut �tre <code>null</code> si la
     * population est "morte". Ce champ est mis-�-jour par {@link Environment#addPopulation}.
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
     * Liste des objets int�ress�s � �tre inform�s
     * des changements apport�s � cette population.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Ev�nement indiquant qu'un changement est survenu dans la population.
     */
    private final PopulationChangeEvent event = new PopulationChangeEvent(this);

    /**
     * Construit une population initialement vide.
     *
     * @param  environment L'environnement dans lequel �voluera cette population.
     */
    public Population() {
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
     * Ajoute l'animal sp�cifi�e � cette population population. Si l'animal appartient d�j�
     * � cette population, rien ne sera fait. Sinon, si l'animal appartenait � une autre
     * population, alors elle sera retir�e de son ancienne population avant d'�tre ajout�
     * � celle-ci.
     * <br><br>
     * Un animal peut changer de population par exemple lorsqu'il passe du stade larvaire
     * vers le stade juvenile. Puisqu'il a cess� de d�river et qu'il s'est mis � nager, on
     * peut consid�rer qu'il a rejoint une nouvelle population d'individus avec une autre
     * dynamique.
     *
     * @param animal L'animal � ajouter.
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
     * Utilis� par {@link Animal#kill} seulement. Cette m�thode existe
     * uniquement parce que l'ensemble {@link #animals} est priv�.
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
     * Tue tout les animaux de cette population et fait dispara�tre
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
        animals.clear(); // Par pr�caution.
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
     * Fait �voluer une population en fonction de son environnement.
     * Cette m�thode va typiquement d�placer les {@linkplain Animal animaux}
     * en appellant des m�thodes telles que {@link Animal#moveToward}. Des
     * individus peuvent aussi na�tre ou mourrir.
     *
     * @param  duration Dur�e de l'�volution, en nombre de jours.
     *         Cette dur�e est habituellement �gale �
     *         <code>{@link #getEnvironment()}.{@link Environment#getTimeStep()
     *         getTimeStep()}.{@link TimeStep#getDuration getDuration()}</code>.
     */
    public abstract void evoluate(float duration);

    /**
     * D�clare un objet � informer des changements survenant dans cette
     * population. Ces changements inclus les esp�ces qui s'ajoutent ou
     * qui meurent, mais n'incluent pas les changements de positions des
     * animaux.
     */
    public void addPopulationChangeListener(PopulationChangeListener listener) {
        listenerList.add(PopulationChangeListener.class, listener);
    }

    /**
     * Retire un objet � informer des changements survenant dans cette
     * population.
     */
    public void removePopulationChangeListener(final PopulationChangeListener listener) {
        listenerList.remove(PopulationChangeListener.class, listener);
    }

    /**
     * A appeler � chaque fois que la population change.
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
