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
package fr.ird.animat.impl;

// J2SE
import java.awt.Color;
import java.awt.Point;
import java.util.Date;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import javax.swing.JFrame;

// Geotools
import org.geotools.resources.Arguments;

// Animats
import fr.ird.animat.viewer.Viewer;


/**
 * Implémentation par défaut d'une simulation. La méthode {@link #start} lancera la simulation
 * dans un thread de basse priorité.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Simulation extends RemoteServer implements fr.ird.animat.Simulation, Runnable {
    /**
     * Le nom de cette simulation.
     */
    private final String name;

    /**
     * L'environnement de la simulation.
     */
    private final Environment environment;

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
            thread = new Thread(this, name);
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
            synchronized (environment.getTreeLock()) {
                final float duration = environment.getClock().getStepDuration();
                for (final Iterator<fr.ird.animat.Population> it=environment.getPopulations().iterator(); it.hasNext();) {
                    ((Population) it.next()).evoluate(duration);
                }
                if (!environment.nextTimeStep()) {
                    finished = true;
                    break;
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
     * Démarre une simulation bidon et affiche son résultat. Cette simulation sert uniquement
     * à vérifier le bon fonctionnement du paquet <code>fr.ird.animat.impl</code>.
     *
     * @param  Les arguments transmis sur la ligne de commande.
     * @throws RemoteException Si un méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    public static void main(String[] args) throws RemoteException {
        final Arguments arguments = new Arguments(args);
        args = arguments.getRemainingArguments(0);
        final fr.ird.animat.Simulation simulation;
        if (true) {
            final Date        startTime   = new Date();
            final Date        endTime     = new Date(startTime.getTime() + 24L*60*60*1000);
            final Clock       clock       = Clock.createClock(startTime, endTime);
            final Environment environment = new Environment(clock);
            final Population  population  = environment.newPopulation();
            final Species     species     = new Species("Animat", Color.RED);
            final Point       point       = new Point();
            for (int i=0; i<10; i++) {
                point.x = point.y = i;
                population.newAnimal(species, point);
            }
            simulation = new Simulation("Test", environment);
        } else {
        }
        if (true) {
            final Viewer viewer = new Viewer(simulation);
            final JFrame  frame = new JFrame("Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(viewer);
            frame.pack();
            frame.show();
        }
//        simulation.start();
    }
}
