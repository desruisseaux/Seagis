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
package fr.ird.animat.viewer;

// J2SE
import java.util.Map;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.rmi.RemoteException;

// Geotools
import org.geotools.gui.swing.Plot2D;
import org.geotools.resources.XArray;

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
final class AnimalMonitor {
    /**
     * L'animal pour lequel on veut afficher les paramètres.
     */
    private final Animal animal;

    /**
     * L'horloge de l'animal, extraite une fois pour toute.
     */
    private final Clock clock;

    /**
     * Le graphique des paramètres.
     */
    private final Plot2D plot = new Plot2D(true, false);

    /**
     * Dates et heures des observations.
     */
    private long[] times = new long[16];

    /**
     * Les valeurs des paramètres observés.
     */
    private Map<Parameter,float[]> values = new HashMap<Parameter,float[]>();

    /**
     * Le numéro séquentiel du prochain pas de temps à afficher.
     */
    private int nextTimeStep = 0;

    /**
     * Construit un nouveau graphique pour l'animal spécifié.
     *
     * @param  animal L'animal pour lequel on veut afficher un graphiques des paramètres.
     * @throws RemoteException si une connexion à une machine distance était nécesaire et a échoué.
     */
    public AnimalMonitor(final Animal animal) throws RemoteException {
        this.animal = animal;
        this.clock  = animal.getClock();
    }

    /**
     * Complete this graph with new data.
     */
    private void complete() throws RemoteException {
        assert clock.equals(animal.getClock()) : clock;
        final int lastStep = clock.getStepSequenceNumber();
        while (nextTimeStep <= lastStep) {
            final Date date = clock.getTime(nextTimeStep);
            int capacity = times.length;
            if (nextTimeStep >= capacity) {
                capacity = Math.max(lastStep+1, nextTimeStep+Math.min(nextTimeStep, 1024));
                times = XArray.resize(times, capacity);
                for (final Iterator<Map.Entry<Parameter,float[]>> it=values.entrySet().iterator(); it.hasNext();) {
                    final Map.Entry<Parameter,float[]> entry = it.next();
                    entry.setValue(XArray.resize(entry.getValue(), capacity));
                }
            }
            times[nextTimeStep] = date.getTime();
            final Map<Parameter,Observation> observations = animal.getObservations(date);
            for (final Iterator<Map.Entry<Parameter,Observation>> it=observations.entrySet().iterator(); it.hasNext();) {
                final Map.Entry<Parameter,Observation> entry = it.next();
                final Parameter parameter = entry.getKey();
                float[] values = this.values.get(parameter);
                if (values == null) {
                    values = new float[capacity];
                    Arrays.fill(values, 0, nextTimeStep+1, Float.NaN);
                    this.values.put(parameter, values);
                }
                final Observation observation = entry.getValue();
                values[nextTimeStep] = observation.value();
            }            
            nextTimeStep++;
        }
        /*
         * Update the graphics.
         */
        plot.clear(false);
        for (final Iterator<Map.Entry<Parameter,float[]>> it=values.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<Parameter,float[]> entry = it.next();
            plot.addSeries(entry.getKey().getName(), times, entry.getValue(), 0, nextTimeStep);
        }
    }
}
