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
package fr.ird.animat.seas;

// J2SE
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractSet;
import java.awt.geom.Point2D;
import java.util.NoSuchElementException;
import javax.swing.event.EventListenerList;

// Animats
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;

// Divers
import fr.ird.util.XArray;
import fr.ird.sql.fishery.CatchEntry;


/**
 * Une population de thon. Il peut y avoir des thons
 * de plusieurs espèces.
 */
final class Population extends AbstractSet<Animal> implements fr.ird.animat.Population
{
    /**
     * Liste des thons de cette population.
     */
    private Animal[] animals = new Animal[32];

    /**
     * Liste des objets intéressés à être informés
     * des changements apportés à cette population.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Nombre d'entrées valides dans la liste {@link #animals}.
     */
    private int count = 0;

    /**
     * Retire tous les animaux de cette population.
     */
    public synchronized void clear()
    {
        Arrays.fill(animals, 0, count, null);
        count = 0;
    }

    /**
     * Retourne le nombre d'animaux dans cette population.
     */
    public synchronized int size()
    {return count;}

    /**
     * Retourne un itérateur sur les animaux de cette population.
     */
    public synchronized Iterator<Animal> iterator()
    {return new Iter();}

    /**
     * Retourne la liste des animaux sous forme de tableau.
     */
    public synchronized Animal[] toArray()
    {
        final Animal[] copy = new Animal[count];
        System.arraycopy(animals, 0, copy, 0, count);
        return copy;
    }

    /**
     * Déplace tous les animaux de cette population en fonction de leur
     * environnement. Cette méthode appelle {@link Animal#move(Environment)}
     * pour chaque animal de cette population.
     */
    public synchronized void moveAnimals(final fr.ird.animat.Environment environment)
    {
        for (int i=0; i<count; i++)
        {
            animals[i].move(environment);
        }
        firePopulationChanged(PopulationChangeEvent.ANIMAL_MOVED);
    }

    /**
     * Ajoute un thon pour chaque espèce à chacune des positions
     * de pêche spécifiées.
     */
    public synchronized void addTunas(final Collection<CatchEntry> entries)
    {
        final int oldCount = count;
        for (final Iterator<CatchEntry> it=entries.iterator(); it.hasNext();)
        {
            addTunas(it.next());
        }
        if (oldCount != count)
        {
            firePopulationChanged(PopulationChangeEvent.ANIMAL_ADDED);
        }
    }

    /**
     * Ajoute un thon pour chaque espèce à la position de pêche spécifiée.
     * Cette méthode ne lance pas d'évènements signalant l'ajout de thons.
     * Cette responsabilité est laissée à l'appelant (afin d'éviter de lancer
     * trop d'événements).
     */
    private synchronized void addTunas(final CatchEntry entry)
    {
        final int oldCount = count;
        final Point2D coord = entry.getCoordinate();
        final Set<Species> species = entry.getSpecies();
        for (final Iterator<Species> it=species.iterator(); it.hasNext();)
        {
            final Tuna tuna = new Tuna(it.next());
            tuna.setLocation(coord);
            if (count >= animals.length)
            {
                animals = XArray.resize(animals, animals.length + Math.min(animals.length, 4096));
            }
            animals[count++] = tuna;
        }
    }

    /**
     * A appeler à chaque fois que la population change.
     *
     * @param type Le type de cet événement: {@link #ANIMAL_ADDED},
     *             {@link #ANIMAL_KILLED} ou {@link #ANIMAL_MOVED}.
     */
    protected void firePopulationChanged(final int type)
    {
        PopulationChangeEvent event = null;
        final Object[] listeners = listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;)
        {
            if (listeners[i] == PopulationChangeListener.class)
            {
                if (event==null)
                {
                    event = new PopulationChangeEvent(this, type);
                }
                ((PopulationChangeListener)listeners[i+1]).populationChanged(event);
            }
        }
    }

    /**
     * Déclare un objet à informer des changements survenant dans cette
     * population. Ces changements inclus les espèces qui s'ajoutent ou
     * qui meurent, mais n'incluent pas les changements de positions des
     * animaux.
     */
    public synchronized void addPopulationChangeListener(PopulationChangeListener listener)
    {listenerList.add(PopulationChangeListener.class, listener);}

    /**
     * Retire un objet à informer des changements survenant dans cette
     * population.
     */
    public synchronized void removePopulationChangeListener(final PopulationChangeListener listener)
    {listenerList.remove(PopulationChangeListener.class, listener);}

    /**
     * Un itérateur balayant les animaux de cette population. Cet itérateur
     * n'est pas synchronisée. Ca ne causera pas de problème même si Swing
     * affiche les populations dans un Thread séparé parce que l'implémentation
     * de {@link fr.ird.animat.PopulationLayer} n'utilise pas cet itérateur.
     * Elle utilise plutôt {@link Population#toArray}, qui est synchronisée.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Iter implements Iterator<Animal>
    {
        /**
         * Next index to returns.
         */
        private int index = 0;

        /**
         * Returns <code>true</code> if the iteration has more elements.
         */
        public boolean hasNext()
        {return index < count;}

        /**
         * Returns the next element in the iteration.
         */
        public Animal next()
        {
            if (index < count) return animals[index++];
            else throw new NoSuchElementException();
        }

        /**
         * Removes from the underlying collection the last element returned
         * by the iterator.
         */
        public void remove()
        {
            if (index!=0 && index<=count)
            {
                animals = XArray.remove(animals, --index, 1);
                animals[--count] = null;
                firePopulationChanged(PopulationChangeEvent.ANIMAL_KILLED);
            }
            else
            {
                throw new IllegalStateException();
            }
        }
    }
}
