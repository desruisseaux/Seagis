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
package fr.ird.animat.gui.swing;

// J2SE dependencies
import java.util.List;
import java.util.Date;
import java.util.TimeZone;
import java.util.LinkedList;
import java.util.Collection;
import java.awt.Color;
import java.awt.EventQueue;
import java.rmi.RemoteException;

// Geotools & Seagis dependencies
import org.geotools.resources.Utilities;
import fr.ird.database.sample.SampleEntry;
import fr.ird.animat.server.SampleSource;
import fr.ird.animat.Species;


/**
 * Layer showing fishery positions.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SampleLayer extends fr.ird.database.gui.map.SampleLayer {
    /**
     * Couleur par défaut des positions de pêches.
     */
    private static final Color COLOR = Color.ORANGE;

    /**
     * Connexion vers les données des échantillons.
     */
    private final SampleSource samples;

    /**
     * Nombre de millisecondes à ajouter à l'heure UTC pour obtenir l'heure locale.
     */
    private int timezoneOffset;

    /**
     * La date des données affichées.
     */
    private transient Date time;

    /**
     * Thread ayant la charge de mettre à jour les positions de pêches.
     */
    private transient Thread updater;

    /**
     * Construit une nouvelle couche.
     */
    public SampleLayer(final SampleSource samples) throws RemoteException {
        this.samples = samples;
        for (final Species sp : samples.getSpecies()) {
            setColor(sp, COLOR);
        }
        setColor(null, COLOR);
    }

    /**
     * Définit le fuseau horaire de la simulation. Cette information est utilisée pour
     * déterminer quand le jour a changé.
     */
    public void setTimeZone(final TimeZone timezone) {
        timezoneOffset = timezone.getRawOffset();
    }

    /**
     * Retourne le jour de la date spécifiée, en nombre de jours
     * depuis le 1 janvier 1970 en heure locale.
     */
    private int getDay(final Date time) {
        if (time == null) {
            return Integer.MIN_VALUE;
        }
        return (int)((time.getTime() + timezoneOffset) / (24*60*60*1000));
    }

    /**
     * Remet à jour la liste des captures. Cette méthode n'effectuera la mise à jour que si la
     * date spécifiée est à une journée différente de celle des données déjà en mémoire, afin
     * d'éviter d'interroger la base de données trop souvent.
     */
    public synchronized void refresh(final Date time) {
        if (getDay(time) == getDay(this.time)) {
            return;
        }
        this.time = time;
        if (updater == null) {
            updater = new Thread(new Runnable() {
                public void run() {
                    try {
                        refresh();
                    } finally {
                        updater = null;
                    }
                }
            });
            updater.setDaemon(true);
            updater.setPriority(Thread.NORM_PRIORITY - 2);
            updater.start();
        }
        notifyAll();
    }

    /**
     * Procède à la mise à jour des positions de pêches. Cette méthode est habituellement
     * appelée dans un thread autre que celui de Swing.
     */
    private void refresh() {
        while (isVisible()) {
            try {
                final Collection<SampleEntry> catchs = (time!=null) ? samples.getSamples() : null;
                synchronized (this) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            setSamples(catchs);
                        }
                    });
                    wait();
                }
            } catch (RemoteException exception) {
                Utilities.unexpectedException("fr.ird.animat.viewer", "SampleLayer", "refresh", exception);
            } catch (InterruptedException exception) {
                // Quelqu'un ne veut pas nous laisser dormir.
                // Retourne au travail.
            }
        }
        updater = null;
    }
}
