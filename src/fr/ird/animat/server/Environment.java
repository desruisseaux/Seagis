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
package fr.ird.animat.server;

// Utilitaires
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.EventListener;
import java.util.NoSuchElementException;
import javax.swing.event.EventListenerList;

// Remote Method Invocation (RMI)
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteObject;
import java.rmi.RemoteException;

// OpenGIS et Geotools
import org.geotools.cv.Coverage;
import org.geotools.gp.Adapters;
import org.opengis.cv.CV_Coverage;
import org.geotools.resources.Utilities;

// Animats
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Implémentation par défaut de l'environnement dans lequel évolueront les animaux. Cet
 * environnement peut contenir un nombre arbitraire de {@linkplain Population populations},
 * mais ne contient aucun paramètre. Pour ajouter des paramètre à cet environnement, il est
 * nécessaire de redéfinir les méthodes suivantes:
 * <ul>
 *   <li>{@link #getParameters}</li>
 *   <li>{@link #getCoverage(Parameter)}</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Environment extends RemoteObject implements fr.ird.animat.Environment {
    /**
     * Ensemble des populations comprises dans cet environnement. Cet ensemble est accédé
     * par le constructeur de {@link Population} et {@link Population#kill} seulement.
     */
    final Set<Population> populations = new LinkedHashSet<Population>();

    /**
     * Version immutable de la population, retournée par {@link #getPopulation}.
     */
    private final Set<Population> immutablePopulations = Collections.unmodifiableSet(populations);

    /**
     * Liste des objets intéressés à être informés
     * des changements apportés à cet environnement.
     */
    private final EventListenerList listenerList = new EventListenerList();

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
     * Le numéro de port utilisé lorsque cet environnement a été exporté,
     * or <code>-1</code> s'il n'a pas encore été exporté.
     */
    private int port = -1;

    /**
     * Rapport sur l'état actuel de la simulation. Un nouvel objet {@link Raport}
     * est créé à chaque nouveau pas de temps.
     */
    private Report report = new Report();

    /**
     * Rapport sur l'état de la simulation depuis le lancement de la simulation.
     */
    private final Report fullReport = new Report();

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
     * Ajoute une nouvelle population dans cet environnement. L'implémentation par défaut retourne
     * simplement <code>new Population(this)</code>. Le constructeur de {@link Population} se charge
     * d'ajouter automatiquement la nouvelle population à cet environnement.
     *
     * @return La population créée.
     * @throws RemoteException si l'exportation de la nouvelle population a échoué.
     */
    public Population newPopulation() throws RemoteException {
        synchronized (getTreeLock()) {
            return new Population(this);
        }
    }

    /**
     * Retourne l'ensemble des populations évoluant dans cet environnement.
     */
    public Set<+Population> getPopulations() {
        return immutablePopulations;
    }

    /**
     * Retourne l'ensemble des paramètres compris dans cet environnement.
     * L'implémentation par défaut retourne un ensemble vide.
     *
     * @see Animal#getObservations
     */
    public Set<+Parameter> getParameters() {
        return (Set<Parameter>)Collections.EMPTY_SET;
    }

    /**
     * Retourne toute la {@linkplain CV_Coverage couverture spatiale des données} à la
     * {@linkplain Clock#getTime date courante} pour un paramètre spécifié. L'implémentation
     * par défaut appelle {@link #getCoverage(Parameter)}.
     *
     * @param  parameter Le paramètre désiré.
     * @return La couverture spatiale des données pour le paramètre spécifié, ou <code>null</code>
     *         si aucune donnée n'est disponible à la date courante. Ce dernier cas peut se produire
     *         s'il y a des trous dans la couverture temporelle des données.
     *
     * @throws NoSuchElementException si le paramètre spécifié n'existe pas dans cet environnement.
     * @throws RemoteException si la couverture n'a pas pu être exportée.
     *
     * @see Animal#getObservations
     */
    public CV_Coverage getCoverage(fr.ird.animat.Parameter parameter)
            throws NoSuchElementException, RemoteException
    {
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
     * @return La couverture spatiale des données pour le paramètre spécifié, or <code>null</code>
     *         si aucune donnée n'est disponible à la date courante. Ce dernier cas peut se produire
     *         s'il y a des trous dans la couverture temporelle des données.
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
     * Retourne les noms de toutes les {@linkplain CV_Coverage couvertures spatiales des données}
     * qui ont été utilisées pour le pas de temps de la {@linkplain Clock#getTime date courante}.
     * L'implémentation par défaut retourne toujours un ensemble vide.
     *
     * @return Les noms des couvertures spatiales utilisées pour le pas de temps courant.
     */
    public String[] getCoverageNames() {
        return new String[0];
    }

    /**
     * Retourne un rapport sur l'état de la simulation. Ce rapport comprend le nombre total
     * d'animaux, le nombre de tentatives d'observations en dehors de la couverture spatiale
     * des données, le pourcentage de données manquantes, etc.
     *
     * @param  full <code>true</code> pour obtenir un rapport s'appliquant depuis le début de
     *         la simulation, ou <code>false</code> pour un rapport ne s'appliquant qu'au dernier
     *         pas de temps.
     * @return Un rapport sur l'état de la simulation.
     */
    public Report getReport(final boolean full) {
        synchronized (getTreeLock()) {
            if (full) {
                return fullReport;
            }
            if (report.numAnimals == 0) {
                int numAnimals = 0;
                for (final Population population : populations) {
                    numAnimals += population.getAnimals().size();
                }
                report.numAnimals = numAnimals;
            }
            return report;
        }
    }

    /**
     * Retourne le rapport courrant. Utilisé uniquement par {@link Animal#observe}, qui mettra
     * à jour le compte des données manquantes.
     */
    final Report getReport() {
        return report;
    }

    /**
     * Incrémente le compte des animaux créés depuis le début de la simulation.
     * Utilisé à des fins de statistiques seulement.
     */
    final void incAnimalCount() {
        fullReport.numAnimals++;
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
     *
     * @return <code>true</code> si cette méthode a pu avancer au pas de temps suivant,
     *         ou <code>false</code> s'il n'y a plus de données disponibles pour les pas
     *         de temps suivants.
     */
    public boolean nextTimeStep() {
        synchronized (getTreeLock()) {
            fullReport.add(report);
            report = new Report();
            clock.nextTimeStep();
            for (final Population population : populations) {
                population.observe();
            }
            fireEnvironmentChanged(new EnvironmentChangeEvent(this, EnvironmentChangeEvent.DATE_CHANGED,
                                                              clock.getTime(), null, null));
        }
        return true;
    }

    /**
     * Libère les ressources utilisées par cet environnement. Toutes
     * les populations contenues dans cet environnement seront détruites,
     * et les éventuelles connections avec des bases de données seront
     * fermées.
     */
    public void dispose() {
        synchronized (getTreeLock()) {
            if (port >= 0) {
                unexport();
            }
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
            queue.dispose();
        }
    }

    /**
     * Libère les ressources utilisées par cet environnement.
     */
    protected void finalize() {
        queue.dispose();
    }




    ////////////////////////////////////////////////////////
    ////////                                        ////////
    ////////    E V E N T   L I S T E N E R S       ////////
    ////////                                        ////////
    ////////////////////////////////////////////////////////

    /**
     * Déclare un objet à informer des changements survenant dans cet
     * environnement. Ces changements surviennent souvent suite à un
     * appel de {@link #nextTimeStep}.
     */
    public void addEnvironmentChangeListener(final EnvironmentChangeListener listener) {
        synchronized (listenerList) {
            listenerList.add(EnvironmentChangeListener.class, listener);
        }
    }

    /**
     * Retire un objet à informer des changements survenant dans cet environnement.
     */
    public void removeEnvironmentChangeListener(final EnvironmentChangeListener listener) {
        synchronized (listenerList) {
            listenerList.remove(EnvironmentChangeListener.class, listener);
        }
    }

    /**
     * Retourne le nombre d'objets intéressés à être informés des changements apportés à
     * l'environnement, à une population ou à l'état d'un des animaux. Cette information
     * peut-être utilisée pour ce faire une idée du trafic qu'il pourrait y avoir sur le
     * réseau lorsque la simulation est exécutée sur une machine distante.
     */
    final int getListenerCount() {
        int count = listenerList.getListenerCount();
        synchronized (getTreeLock()) {
            for (final Population population : populations) {
                count += population.getListenerCount();
            }
        }
        return count;
    }

    /**
     * Préviens tous les objets intéressés qu'une population a été ajoutée ou supprimée.
     *
     * @param population La population ajoutée ou supprimée.
     * @param added <code>true</code> si la population a été ajoutée, ou <code>false</code>
     *        si elle a été supprimée.
     */
    final void fireEnvironmentChanged(final Population population, final boolean added) {
        final Set<fr.ird.animat.Population> change = Collections.singleton((fr.ird.animat.Population)population);
        final EnvironmentChangeEvent event;
        if (added) {
            event = new EnvironmentChangeEvent(this, EnvironmentChangeEvent.POPULATIONS_ADDED,
                                               null, change, null);
        } else {
            event = new EnvironmentChangeEvent(this, EnvironmentChangeEvent.POPULATIONS_REMOVED,
                                               null, null, change);
        }
        fireEnvironmentChanged(event);
    }

    /**
     * Préviens tous les objets intéressés que l'environnement a changé.
     * Cette méthode est habituellement appelée à l'intérieur d'un bloc synchronisé sur
     * {@link #getTreeLock()}. L'appel de {@link EnvironmentChangeListener#environmentChanged}
     * sera mise en attente jusqu'à ce que le verrou sur <code>getTreeLock()</code> soit relâché.
     *
     * @param event Un objet décrivant le changement survenu.
     */
    protected void fireEnvironmentChanged(final EnvironmentChangeEvent event) {
        final Object[] listeners;
        synchronized (listenerList) {
            listeners = listenerList.getListenerList();
        }
        queue.invokeLater(new Runnable() {
            public void run() {
                assert Thread.holdsLock(getTreeLock());
                for (int i=listeners.length; (i-=2)>=0;) {
                    if (listeners[i] == EnvironmentChangeListener.class) try {
                        ((EnvironmentChangeListener)listeners[i+1]).environmentChanged(event);
                    } catch (RemoteException exception) {
                        listenerException("Environment", "fireEnvironmentChanged", exception);
                    }
                }
            }
        });
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




    //////////////////////////////////////////////////////////////////////////
    ////////                                                          ////////
    ////////    R E M O T E   M E T H O D   I N V O C A T I O N       ////////
    ////////                                                          ////////
    //////////////////////////////////////////////////////////////////////////

    /**
     * Retourne le numéro de port utilisé lorsque cet environnement a été exporté,
     * or <code>-1</code> s'il n'a pas encore été exporté.
     */
    final int getRMIPort() {
        return port;
    }

    /**
     * Exporte cet environnement et toutes les populations qu'il contient de façon à ce qu'elles
     * puissent accepter les appels de machines distantes.
     *
     * @param  port Numéro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si cet environnement n'a pas pu être exporté.
     */
    final void export(final int port) throws RemoteException {
        synchronized (getTreeLock()) {
            for (final Population population : populations) {
                population.export(port);
            }
            UnicastRemoteObject.exportObject(this, port);
            this.port = port;
        }
    }

    /**
     * Annule l'exportation de cet environnement. Si l'environnement ou une de ses populations
     * était déjà en train d'exécuter une méthode, alors <code>unexport(...)</code> attendra
     * quelques secondes avant de forcer l'arrêt de l'exécution.
     */
    final void unexport() {
        synchronized (getTreeLock()) {
            for (final Population population : populations) {
                population.unexport();
            }
            Animal.unexport("Environment", this);
            this.port = -1;
        }
    }
}
