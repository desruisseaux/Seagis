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
 * Graphique des param�tres environnementaux obsev�s par un animal.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class AnimalMonitor extends JPanel implements Runnable {
    /**
     * Objet ayant la charge d'appeller {@link #complete} � interval r�gulier.  Un seul
     * <code>Timer</code> est utilis� pour tous les <code>AnimalMonitor</code>. L'appel
     * de {@link #complete} pour un animal particulier se fait � l'aide du {@link #task}
     * associ� � cet <code>AnimalMonitor</code>.
     */
    private static final Timer timer = new Timer(true);

    /**
     * Interval de temps (en millisecondes) entre deux rafraichissement du graphique.
     */
    private static final long PERIOD = 4000;

    /**
     * L'animal pour lequel on veut afficher les param�tres.
     */
    private Animal animal;

    /**
     * Les valeurs pour l'animal.
     */
    private Values values;

    /**
     * L'ensemble des valeurs qui ont �t� cr��s pour des animaux pr�c�dents.
     */
    private Map<Animal,Values> cache = new LinkedHashMap<Animal,Values>() {
        protected boolean removeEldestEntry(final Map.Entry<Animal,Values> eldest) {
            return size() > 32;
        }
    };

    /**
     * Le graphique des param�tres.
     */
    private final Plot2D plot = new Plot2D(true, false);

    /**
     * Appel� r�guli�rement pour mettre � jour le graphique.
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
     * Modifie l'animal � afficher dans le graphique.
     *
     * @param  animal L'animal pour lequel on veut afficher un graphiques des param�tres.
     * @throws RemoteException si une connexion � une machine distance �tait n�cesaire et a �chou�.
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
                // Les derni�res observations n'auront pas �t� prises en compte. Tant pis.
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
         * Le num�ro s�quentiel du prochain pas de temps � afficher.
         */
        private int nextTimeStep = 0;

        /**
         * Prend en compte les nouvelles donn�es
         *
         * @param  animal L'animal pour lequel on veut afficher un graphiques des param�tres.
         * @throws RemoteException si une connexion � une machine distance �tait n�cesaire et a �chou�.
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
         * Lib�re la m�moire r�serv�e en trop.
         */
        public synchronized void trimToSize() {
            times = XArray.resize(times, nextTimeStep);
            for (final Map.Entry<Parameter,float[]> entry : entrySet()) {
                entry.setValue(XArray.resize(entry.getValue(), nextTimeStep));
            }
        }

        /**
         * Rafraichi le contenu du graphique sp�cifi�.
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
     * Lib�re les ressources utilis�es par cet objet.
     */
    public void dispose() {
        task.cancel();
    }
}
