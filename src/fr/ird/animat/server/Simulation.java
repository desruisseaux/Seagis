/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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

// Collections
import java.util.Map;
import java.util.HashMap;
import javax.media.jai.util.CaselessStringKey;

// Remote Method Invocation (RMI)
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.UnexpectedException;
import java.rmi.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.server.RemoteServer;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.net.MalformedURLException;


/**
 * Implémentation par défaut d'une simulation. La méthode {@link #start} lancera la simulation
 * dans un thread de basse priorité.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Simulation extends RemoteServer implements fr.ird.animat.Simulation, Runnable {
    /**
     * Le nom à utiliser lorsque la simulation est exportée pour une utilisation
     * sur des machines distances.
     */
    private static final String EXPORT_NAME = "fr.ird.animat.Simulation";

    /**
     * Le nom de cette simulation.
     */
    private final String name;

    /**
     * L'environnement de la simulation.
     */
    private final Environment environment;

    /**
     * Le délai entre deux pas de la simulation, en nombre de millisecondes. Un délai non-nul
     * peut être imposé afin de laisser le temps à l'utilisateur d'observer l'évolution de la
     * simulation sur une interface graphique.
     */
    protected int delay = 0;

    /**
     * Le thread de la simulation en cours d'exécution, ou <code>null</code>
     * si aucune simulation n'est en cours.
     */
    private transient Thread thread;

    /**
     * Drapeau mis à <code>true</code> lorsque la simulation doit être arrêtée.
     *
     * @see #run
     * @see #stop
     */
    private transient volatile boolean stop;

    /**
     * <code>true</code> si la simulation est terminée. Ca sera le cas lorsque
     * la méthode {@link Environment#nextTimeStep} retournera <code>false</code>.
     */
    private boolean finished;

    /**
     * Les propriétés de la simulation, ou <code>null</code> s'il n'y en a pas.
     */
    private Map<CaselessStringKey,String> properties;

    /**
     * Construit une nouvelle simulation avec le nom spécifiée.
     *
     * @param name Le nom de la simulation.
     * @param environment L'environnement de la simulation.
     */
    public Simulation(final String name, final Environment environment) {
        this.name = name;
        this.environment = environment;
        environment.queue.setName(name);
    }
    
    /**
     * Retourne le nom de cette simulation.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Retourne l'environnement de la simulation.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Retourne le nombre d'objets intéressés à être informés des changements apportés à
     * l'environnement, à une population ou à l'état d'un des animaux. Cette information
     * peut-être utilisée pour ce faire une idée du trafic qu'il pourrait y avoir sur le
     * réseau lorsque la simulation est exécutée sur une machine distante.
     */
    public int getListenerCount() {
        return environment.getListenerCount();
    }
    
    /**
     * Lance la simulation dans un thread de basse priorité. Si la simulation n'était pas déjà
     * en route, alors cette méthode construit un nouveau {@linkplain Thread thread} et le
     * démarre immédiatement.
     */
    public synchronized void start() {
        if (finished) {
            return;
        }
        stop = false;
        if (thread == null) {
            thread = new Thread(THREAD_GROUP, this, name);
            thread.setPriority(Thread.MIN_PRIORITY+1);
            thread.start();
        }
    }

    /**
     * Méthode exécutée par le {@linkplain Thread thread} lancé par la méthode {@link #start}.
     * L'implémentation par défaut appelle {@link Population#evoluate} en boucle  jusqu'à ce
     * que la méthode {@link Environment#nextTimeStep} retourne <code>false</code> ou que la
     * méthode {@link #stop} soit appelée.
     */
    public void run() {
        while (!stop) {
            long time = System.currentTimeMillis();
            synchronized (environment.getTreeLock()) {
                final float duration = environment.getClock().getStepDuration();
                for (final Population population : environment.getPopulations()) {
                    population.evoluate(duration);
                }
                if (!environment.nextTimeStep()) {
                    finished = true;
                    break;
                }
            }
            if (delay!=0) {
                time = System.currentTimeMillis() - time;
                time = delay-time;
                if (time > 0) try {
                    Thread.currentThread().sleep(time);
                } catch (InterruptedException exception) {
                    // Quelqu'un a interrompu l'attente. Retourne au travail...
                }
            }
        }
        synchronized (this) {
            thread = null;
        }
    }

    /**
     * Arrête momentanément la simulation.
     */
    public void stop() {
        stop = true;
    }

    /**
     * Exporte cette simulation, son environnement ainsi que toutes les populations et animaux
     * qu'il contient de façon à ce qu'ils puissent accepter les appels de machines distantes.
     * Une simulation exportée pourra être contrôlée sur une machine distante en utilisant la
     * référence retournée par {@link #lookup}.
     *
     * @param  port Numéro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si cette simulation n'a pas pu être exportée.
     */
    public void export(final int port) throws RemoteException {
        synchronized (environment.getTreeLock()) {
            if (environment.getRMIPort() < 0) {
                if (false) {
                    if (System.getSecurityManager() == null) {
                        System.setSecurityManager(new RMISecurityManager());
                    }
                }
                environment.export(port);
                UnicastRemoteObject.exportObject(this, port);
                try {
                    Naming.bind(EXPORT_NAME, this);
                } catch (MalformedURLException exception) {
                    // Ne devrait pas se produire, étant donné que nous
                    // savons que le nom forme une adresse URL valide.
                    throw new ExportException("Adresse URL invalide.", exception);
                } catch (AlreadyBoundException exception) {
                    // Ne devrait pas se produire, étant donné que nous avons
                    // vérifié que l'environnement n'avait pas déjà été exporté.
                    Animal.warning("Simulation", "La simulation était déjà exportée.", exception);
                }
            }
        }
    }

    /**
     * Annule l'exportation de la simulation. Si la simulation ou une de ses composantes était
     * déjà en train d'exécuter une méthode, alors <code>unexport(...)</code> attendra quelques
     * secondes avant de forcer l'arrêt de l'exécution.
     *
     * @throws RemoteException si l'exportation n'a pas pu être annulée.
     */
    public void unexport() throws RemoteException {
        synchronized (environment.getTreeLock()) {
            if (environment.getRMIPort() >= 0) {
                try {
                    Naming.unbind(EXPORT_NAME);
                } catch (MalformedURLException exception) {
                    // Ne devrait pas se produire, étant donné que nous
                    // savons que le nom forme une adresse URL valide.
                    throw new UnexpectedException("Adresse URL invalide.", exception);
                } catch (NotBoundException exception) {
                    // Ne devrait pas se produire, étant donné que nous
                    // avons vérifié que l'environnement avait été exportée.
                    Animal.warning("Simulation", "La simulation n'avait pas été exportée.", exception);
                }
                environment.unexport();
                Animal.unexport("Simulation", this);
            }
        }
    }

    /**
     * Retourne une connexion vers une simulation sur un serveur distant. La méthode {@link #export}
     * doit avoir été appelée sur le serveur avant que cette méthode <code>lookup</code> puisse être
     * appelée sur la machine cliente.
     *
     * @param  server Le nom du serveur ou son adresse IP, ou <code>null</code> pour chercher
     *         une simulation sur la machine locale.
     * @return La simulation sur un serveur distant.
     * @throws RemoteException si la connexion n'a pas pu être établie.
     */
    public static fr.ird.animat.Simulation lookup(final String server) throws RemoteException {
        String name = EXPORT_NAME;
        if (server != null) {
            name = "//"+server+'/'+name;
        }
        try {
            return (fr.ird.animat.Simulation) Naming.lookup(name);
        } catch (MalformedURLException exception) {
            // Ne devrait pas se produire, étant donné que nous avons formé une adresse
            // URL valide (à moins que l'argument 'server' contient plus qu'un simple nom).
            throw new UnknownHostException("Nom de serveur invalide.", exception);
        } catch (NotBoundException exception) {
            throw new UnknownHostException("Le serveur n'a pas été trouvé ou n'est pas prêt.", exception);
        }
    }

    /**
     * @inheritDoc
     */
    public String getProperty(final String name) {
        if (properties != null) {
            return properties.get(new CaselessStringKey(name.trim()));
        }
        return null;
    }

    /**
     * Ajoute ou remplace une propriété.
     *
     * @param name Le nom de la propriété à ajouter ou remplacer.
     * @param value La valeur de la propriété.
     */
    protected void setProperty(final String name, final String value) {
        if (properties == null) {
            properties = new HashMap<CaselessStringKey,String>();
        }
        properties.put(new CaselessStringKey(name.trim()), value);
    }
}
