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

// J2SE standard
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.EventListener;
import java.util.NoSuchElementException;
import javax.swing.event.EventListenerList;
import java.rmi.server.RemoteServer;
import java.rmi.RemoteException;

// OpenGIS et Geotools
import org.geotools.cv.Coverage;
import org.geotools.gp.Adapters;
import org.opengis.cv.CV_Coverage;
import org.geotools.resources.Utilities;

// Animats
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Implémentation par défaut de l'environnement dans lequel évolueront les animaux. Cet
 * environnement peut contenir un nombre arbitraire de {@linkplain Population populations},
 * mais ne contient aucun paramètre. Pour ajouter des paramètre à cet environnement, il est
 * nécessaire de redéfinir les méthodes suivantes:
 * <ul>
 *   <li>{@link #getParameters}</li>
 *   <li>{@link #getCoverage}</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Environment extends RemoteServer implements fr.ird.animat.Environment {
    /**
     * Ensemble des populations comprises dans cet environnement.
     */
    private final Set<fr.ird.animat.Population> populations = new LinkedHashSet<fr.ird.animat.Population>();

    /**
     * Version immutable de la population, retournée par {@link #getPopulation}.
     */
    private final Set<fr.ird.animat.Population> immutablePopulations = Collections.unmodifiableSet(populations);

    /**
     * Liste des objets intéressés à être informés
     * des changements apportés à cet environnement.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Evénement indiquant qu'un changement est survenu dans l'environnement.
     */
    private final EnvironmentChangeEvent event = new EnvironmentChangeEvent(this);

    /**
     * Classe à exécuter lorsque l'environnement a changé.
     *
     * @see #fireEnvironmentChanged()
     */
    private final Runnable fireEnvironmentChanged = new Runnable() {
        public void run() {
            assert Thread.holdsLock(getTreeLock());
            final Object[] listeners = listenerList.getListenerList();
            for (int i=listeners.length; (i-=2)>=0;) {
                if (listeners[i] == EnvironmentChangeListener.class) try {
                    ((EnvironmentChangeListener)listeners[i+1]).environmentChanged(event);
                } catch (RemoteException exception) {
                    listenerException("Environment", "fireEnvironmentChanged", exception);
                }
            }
        }
    };

    /**
     * The event queue.
     */
    final EventQueue queue;

    /**
     * Horloge de la simulation. Toute la simulation, ainsi que les événements mis en attente
     * dans {@link #queue}, seront synchronisés sur cette horloge.
     */
    private final Clock clock;

    /**
     * Construit un environnement par défaut.
     *
     * @param clock Horloge de la simulation. Toute la simulation
     *              sera synchronisée sur cette horloge.
     */
    public Environment(final Clock clock) {
        if (clock == null) {
            throw new NullPointerException(Resources.format(
                                           ResourceKeys.ERROR_BAD_ARGUMENT_$2, "clock", clock));
        }
        this.clock = clock;
        queue = new EventQueue(clock);
    }

    /**
     * Ajoute une population à cet environnement. Si la population appartient déjà à cet
     * environnement, rien ne sera fait. Sinon, si la population appartenait à un autre
     * environnement, alors elle sera retirée de son ancien environnement avant d'être
     * ajouté à celui-ci.
     *
     * @param population La population à ajouter.
     *
     * @see #getPopulations
     * @see Population#kill
     */
    public void addPopulation(final Population population) {
        synchronized (getTreeLock()) {
            final Environment oldEnvironment = population.environment;
            if (oldEnvironment != this) {
                if (oldEnvironment != null) {
                    oldEnvironment.populations.remove(this);
                    population.environment = null;
                    oldEnvironment.fireEnvironmentChanged();
                }
                populations.add(population);
                population.environment = this;
                fireEnvironmentChanged();
            }
        }
    }

    /**
     * Utilisé par {@link Population#kill} seulement. Cette méthode existe
     * uniquement parce que l'ensemble {@link #populations} est privé.
     */
    final void kill(final Population population) {
        assert Thread.holdsLock(getTreeLock());
        populations.remove(population);
    }

    /**
     * Retourne l'ensemble des populations évoluant dans cet environnement.
     */
    public Set<fr.ird.animat.Population> getPopulations() {
        return immutablePopulations;
    }

    /**
     * Retourne l'ensemble des paramètres compris dans cet environnement.
     * L'implémentation par défaut retourne un ensemble vide.
     *
     * @see Animal#getObservations
     */
    public Set<fr.ird.animat.Parameter> getParameters() {
        return Collections.EMPTY_SET;
    }

    /**
     * Retourne toute la {@linkplain CV_Coverage couverture spatiale des données} à la
     * {@linkplain Clock#getTime date courante} pour un paramètre spécifié. L'implémentation
     * par défaut appelle {@link #getCoverage(Parameter)}.
     *
     * @param  parameter Le paramètre désiré.
     * @return La couverture spatiale des données pour le paramètre spécifié.
     *
     * @throws NoSuchElementException si le paramètre spécifié n'existe pas dans cet environnement.
     *
     * @see Animal#getObservations
     */
    public CV_Coverage getCoverage(fr.ird.animat.Parameter parameter) throws NoSuchElementException {
        if (parameter instanceof Parameter) {
            return Adapters.getDefault().export(getCoverage((Parameter)parameter));
        }
        throw new NoSuchElementException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2,
                                         "parameter", parameter));
    }

    /**
     * Retourne toute la {@linkplain Coverage couverture spatiale des données} à la
     * {@linkplain Clock#getTime date courante} pour un paramètre spécifié.
     * L'implémentation par défaut lance toujours une exception de type
     * {@link NoSuchElementException}.
     *
     * @param  parameter Le paramètre désiré.
     * @return La couverture spatiale des données pour le paramètre spécifié.
     *
     * @throws NoSuchElementException si le paramètre spécifié n'existe pas dans cet environnement.
     *
     * @see Animal#getObservations
     */
    public Coverage getCoverage(Parameter parameter) throws NoSuchElementException {
        throw new NoSuchElementException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2,
                                         "parameter", parameter));
    }

    /**
     * Retourne l'horloge de la simulation. Pendant chaque pas de temps de la simulation,
     * les conditions suivantes sont remplies:
     * <ul>
     *   <li>Tous les paramètres de l'environnement sont considérés constants pendant toute
     *       la durée du pas de temps.</li>
     *   <li>L'ensemble de la simulation est synchronisée (au sens du mot-clé
     *       <code>synchronized</code>) sur cette horloge.</li>
     * </ul>
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Avance l'horloge d'un pas de temps. Cette opération peut provoquer le chargement
     * de nouvelles données et lancer un événement {@link EnvironmentChangeEvent}.
     */
    public void nextTimeStep() {
        synchronized (getTreeLock()) {
            clock.nextTimeStep();
            fireEnvironmentChanged();
        }
    }

    /**
     * Déclare un objet à informer des changements survenant dans cet
     * environnement. Ces changements surviennent souvent suite à un
     * appel de {@link #nextTimeStep}.
     */
    public void addEnvironmentChangeListener(final EnvironmentChangeListener listener) {
        synchronized (getTreeLock()) {
            listenerList.add(EnvironmentChangeListener.class, listener);
        }
    }

    /**
     * Retire un objet à informer des changements survenant dans cet environnement.
     */
    public void removeEnvironmentChangeListener(final EnvironmentChangeListener listener) {
        synchronized (getTreeLock()) {
            listenerList.remove(EnvironmentChangeListener.class, listener);
        }
    }

    /**
     * Préviens tous les objets intéressés que des données ont changés.
     * Cette méthode peut être appelée par les classes dérivées suite à
     * un chargement de nouvelles données.
     *
     * Cette méthode est habituellement appelée à l'intérieur d'un block synchronisé sur
     * {@link #getTreeLock()}. L'appel de {@link EnvironmentChangeListener#environmentChanged}
     * sera mise en attente jusqu'à ce que le verrou sur <code>getTreeLock()</code> soit relâché.
     */
    protected void fireEnvironmentChanged() {
        queue.invokeLater(fireEnvironmentChanged);
    }

    /**
     * Appelée lorsqu'une erreur est survenue sur une machine distance lors de la notification
     * d'un changement. Cette erreur ne concerne généralement pas la simulation. On se contentera
     * donc d'afficher un avertissement et de continuer.
     */
    static void listenerException(String classe, String method, RemoteException error) {
        Utilities.unexpectedException("fr.ird.animat", classe, method, error);
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des accès à l'environnement.
     * Par défaut, toutes la simulation est synchronisée sur l'horloge {@link #getClock}.
     */
    protected final Object getTreeLock() {
        return queue.lock;
    }

    /**
     * Libère les ressources utilisées par cet environnement. Toutes
     * les populations contenues dans cet environnement seront détruites,
     * et les éventuelles connections avec des bases de données seront
     * fermées.
     */
    public void dispose() {
        synchronized (getTreeLock()) {
            /*
             * On ne peut pas utiliser Iterator, parce que les appels
             * de Population.kill() vont modifier l'ensemble.
             */
            final Population[] pop = (Population[]) populations.toArray(new Population[populations.size()]);
            for (int i=0; i<pop.length; i++) {
                pop[i].kill();
            }
            assert populations.isEmpty() : populations.size();
            populations.clear(); // Par précaution.
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
}
