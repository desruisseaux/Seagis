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
 * Impl�mentation par d�faut de l'environnement dans lequel �volueront les animaux. Cet
 * environnement peut contenir un nombre arbitraire de {@linkplain Population populations},
 * mais ne contient aucun param�tre. Pour ajouter des param�tre � cet environnement, il est
 * n�cessaire de red�finir les m�thodes suivantes:
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
     * Ensemble des populations comprises dans cet environnement. Cet ensemble est acc�d�
     * par le constructeur de {@link Population} et {@link Population#kill} seulement.
     */
    final Set<Population> populations = new LinkedHashSet<Population>();

    /**
     * Version immutable de la population, retourn�e par {@link #getPopulation}.
     */
    private final Set<Population> immutablePopulations = Collections.unmodifiableSet(populations);

    /**
     * Liste des objets int�ress�s � �tre inform�s
     * des changements apport�s � cet environnement.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * The event queue.
     */
    final EventQueue queue;

    /**
     * Horloge de la simulation. Toute la simulation, ainsi que les �v�nements mis en attente
     * dans {@link #queue}, seront synchronis�s sur cette horloge.
     */
    private final Clock clock;

    /**
     * Le num�ro de port utilis� lorsque cet environnement a �t� export�,
     * or <code>-1</code> s'il n'a pas encore �t� export�.
     */
    private int port = -1;

    /**
     * Rapport sur l'�tat actuel de la simulation. Un nouvel objet {@link Raport}
     * est cr�� � chaque nouveau pas de temps.
     */
    private Report report = new Report();

    /**
     * Rapport sur l'�tat de la simulation depuis le lancement de la simulation.
     */
    private final Report fullReport = new Report();

    /**
     * Construit un environnement par d�faut.
     *
     * @param clock Horloge de la simulation. Toute la simulation
     *              sera synchronis�e sur cette horloge.
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
     * Ajoute une nouvelle population dans cet environnement. L'impl�mentation par d�faut retourne
     * simplement <code>new Population(this)</code>. Le constructeur de {@link Population} se charge
     * d'ajouter automatiquement la nouvelle population � cet environnement.
     *
     * @return La population cr��e.
     * @throws RemoteException si l'exportation de la nouvelle population a �chou�.
     */
    public Population newPopulation() throws RemoteException {
        synchronized (getTreeLock()) {
            return new Population(this);
        }
    }

    /**
     * Retourne l'ensemble des populations �voluant dans cet environnement.
     */
    public Set<+Population> getPopulations() {
        return immutablePopulations;
    }

    /**
     * Retourne l'ensemble des param�tres compris dans cet environnement.
     * L'impl�mentation par d�faut retourne un ensemble vide.
     *
     * @see Animal#getObservations
     */
    public Set<+Parameter> getParameters() {
        return (Set<Parameter>)Collections.EMPTY_SET;
    }

    /**
     * Retourne toute la {@linkplain CV_Coverage couverture spatiale des donn�es} � la
     * {@linkplain Clock#getTime date courante} pour un param�tre sp�cifi�. L'impl�mentation
     * par d�faut appelle {@link #getCoverage(Parameter)}.
     *
     * @param  parameter Le param�tre d�sir�.
     * @return La couverture spatiale des donn�es pour le param�tre sp�cifi�, ou <code>null</code>
     *         si aucune donn�e n'est disponible � la date courante. Ce dernier cas peut se produire
     *         s'il y a des trous dans la couverture temporelle des donn�es.
     *
     * @throws NoSuchElementException si le param�tre sp�cifi� n'existe pas dans cet environnement.
     * @throws RemoteException si la couverture n'a pas pu �tre export�e.
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
     * Retourne toute la {@linkplain Coverage couverture spatiale des donn�es} � la
     * {@linkplain Clock#getTime date courante} pour un param�tre sp�cifi�.
     * L'impl�mentation par d�faut lance toujours une exception de type
     * {@link NoSuchElementException}.
     *
     * @param  parameter Le param�tre d�sir�.
     * @return La couverture spatiale des donn�es pour le param�tre sp�cifi�, or <code>null</code>
     *         si aucune donn�e n'est disponible � la date courante. Ce dernier cas peut se produire
     *         s'il y a des trous dans la couverture temporelle des donn�es.
     *
     * @throws NoSuchElementException si le param�tre sp�cifi� n'existe pas dans cet environnement.
     *
     * @see Animal#getObservations
     */
    public Coverage getCoverage(Parameter parameter) throws NoSuchElementException {
        throw new NoSuchElementException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2,
                                         "parameter", parameter));
    }

    /**
     * Retourne les noms de toutes les {@linkplain CV_Coverage couvertures spatiales des donn�es}
     * qui ont �t� utilis�es pour le pas de temps de la {@linkplain Clock#getTime date courante}.
     * L'impl�mentation par d�faut retourne toujours un ensemble vide.
     *
     * @return Les noms des couvertures spatiales utilis�es pour le pas de temps courant.
     */
    public String[] getCoverageNames() {
        return new String[0];
    }

    /**
     * Retourne un rapport sur l'�tat de la simulation. Ce rapport comprend le nombre total
     * d'animaux, le nombre de tentatives d'observations en dehors de la couverture spatiale
     * des donn�es, le pourcentage de donn�es manquantes, etc.
     *
     * @param  full <code>true</code> pour obtenir un rapport s'appliquant depuis le d�but de
     *         la simulation, ou <code>false</code> pour un rapport ne s'appliquant qu'au dernier
     *         pas de temps.
     * @return Un rapport sur l'�tat de la simulation.
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
     * Retourne le rapport courrant. Utilis� uniquement par {@link Animal#observe}, qui mettra
     * � jour le compte des donn�es manquantes.
     */
    final Report getReport() {
        return report;
    }

    /**
     * Incr�mente le compte des animaux cr��s depuis le d�but de la simulation.
     * Utilis� � des fins de statistiques seulement.
     */
    final void incAnimalCount() {
        fullReport.numAnimals++;
    }

    /**
     * Retourne l'horloge de la simulation. Pendant chaque pas de temps de la simulation,
     * les conditions suivantes sont remplies:
     * <ul>
     *   <li>Tous les param�tres de l'environnement sont consid�r�s constants pendant toute
     *       la dur�e du pas de temps.</li>
     *   <li>L'ensemble de la simulation est synchronis�e (au sens du mot-cl�
     *       <code>synchronized</code>) sur cette horloge.</li>
     * </ul>
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Avance l'horloge d'un pas de temps. Cette op�ration peut provoquer le chargement
     * de nouvelles donn�es et lancer un �v�nement {@link EnvironmentChangeEvent}.
     *
     * @return <code>true</code> si cette m�thode a pu avancer au pas de temps suivant,
     *         ou <code>false</code> s'il n'y a plus de donn�es disponibles pour les pas
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
     * Lib�re les ressources utilis�es par cet environnement. Toutes
     * les populations contenues dans cet environnement seront d�truites,
     * et les �ventuelles connections avec des bases de donn�es seront
     * ferm�es.
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
            populations.clear(); // Par pr�caution.
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
     * Lib�re les ressources utilis�es par cet environnement.
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
     * D�clare un objet � informer des changements survenant dans cet
     * environnement. Ces changements surviennent souvent suite � un
     * appel de {@link #nextTimeStep}.
     */
    public void addEnvironmentChangeListener(final EnvironmentChangeListener listener) {
        synchronized (listenerList) {
            listenerList.add(EnvironmentChangeListener.class, listener);
        }
    }

    /**
     * Retire un objet � informer des changements survenant dans cet environnement.
     */
    public void removeEnvironmentChangeListener(final EnvironmentChangeListener listener) {
        synchronized (listenerList) {
            listenerList.remove(EnvironmentChangeListener.class, listener);
        }
    }

    /**
     * Retourne le nombre d'objets int�ress�s � �tre inform�s des changements apport�s �
     * l'environnement, � une population ou � l'�tat d'un des animaux. Cette information
     * peut-�tre utilis�e pour ce faire une id�e du trafic qu'il pourrait y avoir sur le
     * r�seau lorsque la simulation est ex�cut�e sur une machine distante.
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
     * Pr�viens tous les objets int�ress�s qu'une population a �t� ajout�e ou supprim�e.
     *
     * @param population La population ajout�e ou supprim�e.
     * @param added <code>true</code> si la population a �t� ajout�e, ou <code>false</code>
     *        si elle a �t� supprim�e.
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
     * Pr�viens tous les objets int�ress�s que l'environnement a chang�.
     * Cette m�thode est habituellement appel�e � l'int�rieur d'un bloc synchronis� sur
     * {@link #getTreeLock()}. L'appel de {@link EnvironmentChangeListener#environmentChanged}
     * sera mise en attente jusqu'� ce que le verrou sur <code>getTreeLock()</code> soit rel�ch�.
     *
     * @param event Un objet d�crivant le changement survenu.
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
     * Appel�e lorsqu'une erreur est survenue sur une machine distance lors de la notification
     * d'un changement. Cette erreur ne concerne g�n�ralement pas la simulation. On se contentera
     * donc d'afficher un avertissement et de continuer.
     */
    static void listenerException(String classe, String method, RemoteException error) {
        Utilities.unexpectedException("fr.ird.animat", classe, method, error);
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des acc�s � l'environnement.
     * Par d�faut, toutes la simulation est synchronis�e sur l'horloge {@link #getClock}.
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
     * Retourne le num�ro de port utilis� lorsque cet environnement a �t� export�,
     * or <code>-1</code> s'il n'a pas encore �t� export�.
     */
    final int getRMIPort() {
        return port;
    }

    /**
     * Exporte cet environnement et toutes les populations qu'il contient de fa�on � ce qu'elles
     * puissent accepter les appels de machines distantes.
     *
     * @param  port Num�ro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si cet environnement n'a pas pu �tre export�.
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
     * �tait d�j� en train d'ex�cuter une m�thode, alors <code>unexport(...)</code> attendra
     * quelques secondes avant de forcer l'arr�t de l'ex�cution.
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
