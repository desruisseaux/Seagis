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
package fr.ird.animat.gui.swing;

// J2SE
import java.util.Map;
import java.util.Date;
import java.util.Timer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.LinkedHashMap;
import java.rmi.RemoteException;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JPanel;

// Geotools
import org.geotools.axis.Axis2D;
import org.geotools.axis.AbstractGraduation;
import org.geotools.util.NumberRange;
import org.geotools.gui.swing.Plot2D;
import org.geotools.resources.XArray;
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.animat.Clock;
import fr.ird.animat.Animal;
import fr.ird.animat.Parameter;
import fr.ird.animat.Observation;


/**
 * Graphique des paramètres environnementaux obsevés par un animal.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class AnimalMonitor extends JPanel implements Runnable {
    /**
     * Objet ayant la charge d'appeller {@link #complete} à interval régulier.  Un seul
     * <code>Timer</code> est utilisé pour tous les <code>AnimalMonitor</code>. L'appel
     * de {@link #complete} pour un animal particulier se fait à l'aide du {@link #task}
     * associé à cet <code>AnimalMonitor</code>.
     */
    private static final Timer timer = new Timer(true);

    /**
     * Interval de temps (en millisecondes) entre deux rafraichissement du graphique.
     */
    private static final long PERIOD = 4000;

    /**
     * L'animal pour lequel on veut afficher les paramètres.
     */
    private Animal animal;

    /**
     * Les valeurs pour l'animal.
     */
    private Values values;

    /**
     * L'ensemble des valeurs qui ont été créés pour des animaux précédents.
     */
    private Map<Animal,Values> cache = new LinkedHashMap<Animal,Values>() {
        protected boolean removeEldestEntry(final Map.Entry<Animal,Values> eldest) {
            return size() > 32;
        }
    };

    /**
     * Le graphique des paramètres.
     */
    private final Plot2D plot = new Plot2D(true, false);

    /**
     * Appelé régulièrement pour mettre à jour le graphique.
     */
    private final TimerTask task = new TimerTask() {
        public void run() {
            complete();
        }
    };

    /**
     * Construit un nouveau graphique initiallement vide.
     */
    public AnimalMonitor() {
        super(new BorderLayout());
        add(plot.createScrollPane(), BorderLayout.CENTER);
        timer.schedule(task, 1000, PERIOD);
    }

    /**
     * Modifie l'animal à afficher dans le graphique.
     *
     * @param  animal L'animal pour lequel on veut afficher un graphiques des paramètres.
     * @throws RemoteException si une connexion à une machine distance était nécesaire et a échoué.
     */
    public void setAnimal(final Animal animal) throws RemoteException {
        synchronized (cache) {
            if (animal == null) {
                this.values = null;
                this.animal = null;
                plot.clear(false);
                return;
            }
            Values values = cache.get(animal);
            if (values == null) {
                values = new Values();
                cache.put(animal, values);
            }
            if (values != this.values) {
                if (this.values != null) {
                    this.values.trimToSize();
                }
                this.values = values;
                this.animal = animal;
                values.refresh(plot);
            }
        }
    }

    /**
     * Complete this graph with new data.
     */
    private void complete() {
        synchronized (cache) {
            if (values!=null) try {
                values.complete(animal);
            } catch (RemoteException exception) {
                // Les dernières observations n'auront pas été prises en compte. Tant pis.
                Utilities.unexpectedException("fr.ird.animat.viewer", "AnimalMonitor", "complete", exception);
            }
            EventQueue.invokeLater(this);
        }
    }

    /**
     * Update the graphics with current data. This method should be executed from Swing thread.
     */
    public void run() {
        synchronized (cache) {
            if (values != null) {
                values.refresh(plot);
            }
        }
    }

    /**
     * Les valeurs pour un animal.
     */
    private static final class Values extends HashMap<Parameter,float[]> {
        /**
         * Dates et heures des observations.
         */
        private long[] times = new long[16];

        /**
         * Le numéro séquentiel du prochain pas de temps à afficher.
         */
        private int nextTimeStep = 0;

        /**
         * Prend en compte les nouvelles données
         *
         * @param  animal L'animal pour lequel on veut afficher un graphiques des paramètres.
         * @throws RemoteException si une connexion à une machine distance était nécesaire et a échoué.
         */
        public synchronized void complete(final Animal animal) throws RemoteException {
            final Clock clock = animal.getClock();
            final int lastStep = clock.getStepSequenceNumber();
            while (nextTimeStep <= lastStep) {
                final Date date = clock.getTime(nextTimeStep);
                int capacity = times.length;
                if (nextTimeStep >= capacity) {
                    capacity = Math.max(lastStep+1, nextTimeStep+Math.min(nextTimeStep, 1024));
                    times = XArray.resize(times, capacity);
                    for (final Map.Entry<Parameter,float[]> entry : entrySet()) {
                        entry.setValue(XArray.resize(entry.getValue(), capacity));
                    }
                }
                times[nextTimeStep] = date.getTime();
                final Map<Parameter,Observation> observations = animal.getObservations(date);
                for (final Map.Entry<Parameter,Observation> entry : observations.entrySet()) {
                    final Parameter parameter = entry.getKey();
                    float[] values = get(parameter);
                    if (values == null) {
                        values = new float[capacity];
                        Arrays.fill(values, 0, nextTimeStep+1, Float.NaN);
                        put(parameter, values);
                    }
                    final Observation observation = entry.getValue();
                    values[nextTimeStep] = observation.value();
                }            
                nextTimeStep++;
            }
        }

        /**
         * Libère la mémoire réservée en trop.
         */
        public synchronized void trimToSize() {
            times = XArray.resize(times, nextTimeStep);
            for (final Map.Entry<Parameter,float[]> entry : entrySet()) {
                entry.setValue(XArray.resize(entry.getValue(), nextTimeStep));
            }
        }

        /**
         * Rafraichi le contenu du graphique spécifié.
         */
        public synchronized void refresh(final Plot2D plot) {
            plot.clear(false);
            for (final Map.Entry<Parameter,float[]> entry : entrySet()) {
                try {
                    final Parameter  parameter = entry.getKey();
                    final float[]       values = entry.getValue();
                    final String          name = parameter.getName();
                    final Plot2D.Series series = plot.addSeries(name, times, values, 0, nextTimeStep);
                    final NumberRange    range = parameter.getRange();
                    if (range != null) {
                        final AbstractGraduation graduation = (AbstractGraduation)plot.getAxis(series)[1].getGraduation();
                        final double min = range.getMinimum();
                        final double max = range.getMaximum();
                        if (graduation.getMinimum() > min) {
                            graduation.setMinimum(min);
                        }
                        if (graduation.getMaximum() < max) {
                            graduation.setMaximum(max);
                        }
                    }
                } catch (RemoteException exception) {
                    // Can't add a series. Continue anyway (we will try to add the other ones).
                    Utilities.unexpectedException("fr.ird.animat.viewer", "AnimalMonitor", "run", exception);
                }
            }
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     */
    public void dispose() {
        task.cancel();
    }
}
