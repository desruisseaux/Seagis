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
package fr.ird.sql.coupling;

// Divers
import java.util.Date;
import java.util.Arrays;
import java.sql.SQLException;

// Base de donn�es de p�ches
import fr.ird.sql.fishery.CatchEntry;


/**
 * Une capture dont la valeur devra �tre �valu�e. Ces t�ches seront class�es
 * en ordre croissant de date, afin de r�duire le nombre d'images � charger.
 * Sans classement, de fr�quent retours en arri�re seraient n�cessaire.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Task implements Comparable<Task> {
    /**
     * La dur�e d'une journ�e, en nombre de millisecondes.
     */
    private static final long DAY = 24*60*60*1000L;

    /**
     * Jours o� extraire des donn�es, avant,
     * pendant et apr�s le jour de la p�che.
     */
    private static final int[] DAYS_TO_EVALUATE = {-30, -25, -20, -15, -10, -5, 0, 5};

    /**
     * La capture � �valuer.
     */
    public final CatchEntry capture;

    /**
     * La date � laquelle �valuer la capture, en nombre de
     * millisecondes �coul�es depuis le 1er janvier 1970.
     */
    final long time;

    /**
     * Construit une nouvelle t�che.
     *
     * @param capture La capture � �valuer.
     * @param time La date � laquelle �valuer la capture.
     */
    private Task(final CatchEntry capture, final Date time) {
        this.capture = capture;
        this.time    = time.getTime();
    }

    /**
     * Compare cette t�che avec la t�che sp�cifi�e. Utilis�
     * pour classer les t�ches en ordre croissant de date.
     */
    public int compareTo(final Task other) {
        if (time < other.time) return -1;
        if (time > other.time) return +1;
        return 0;
    }

    /**
     * Retourne une liste des t�ches � effectuer.
     *
     * @param catchs Liste des captures dont on voudra les param�tres environnementaux.
     * @param coverage L'objet qui servira (plus tard) � calculer les param�tres.
     */
    public static Task[] getTasks(final CatchEntry[] catchs, final CatchCoverage coverage) {
        final Task[] tasks = new Task[catchs.length * DAYS_TO_EVALUATE.length];
        for (int i=0,t=0; t<DAYS_TO_EVALUATE.length; t++) {
            final long timeOffset = DAY*DAYS_TO_EVALUATE[t];
            for (int j=0; j<catchs.length; j++) {
                final CatchEntry capture = catchs[j];
                final Date time = coverage.getTime(capture);
                time.setTime(time.getTime() + timeOffset);
                tasks[i++] = new Task(capture, time);
            }
        }
        Arrays.sort(tasks);
        return tasks;
    }
}
