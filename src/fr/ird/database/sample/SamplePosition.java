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
package fr.ird.database.sample;

// Divers
import java.util.Date;
import java.util.Arrays;
import java.util.Collection;
import java.sql.SQLException;


/**
 * Un {@linkplain SampleEntry échantillon} associé à une {@linkplain RelativePositionEntry
 * position relative}. Ces paires seront classées en ordre croissant de date, afin de réduire
 * le nombre d'images à charger. Sans classement, de fréquent retours en arrière seraient
 * nécessaires. Ces objets sont utilisés par {@link EnvironmentColumnFiller}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SamplePosition implements Comparable<SamplePosition> {
    /**
     * L'échantillon à évaluer.
     */
    public final SampleEntry sample;

    /**
     * La position relative à laquelle évaluer les paramètres environnementaux.
     */
    public final RelativePositionEntry position;

    /**
     * La date à laquelle évaluer les paramètres environnementaux,
     * en nombre de millisecondes écoulées depuis le 1er janvier 1970.
     */
    final long time;

    /**
     * Construit une nouvelle tâche.
     *
     * @param sample   L'échantillon à évaluer.
     * @param position La position relative à laquelle évaluer les paramètres environnementaux.
     * @param time La date à laquelle évaluer l'échantillon. Cette date pourrait être déduite
     *        des arguments <code>sample</code> et <code>position</code>, mais un ajustement
     *        supplémentaire est parfois fait pour réduire le nombre d'interpolation temporelles
     *        dans les images.
     */
    private SamplePosition(final SampleEntry           sample,
                           final RelativePositionEntry position,
                           final Date                  time)
    {
        this.sample   = sample;
        this.position = position;
        this.time     = time.getTime();
    }

    /**
     * Compare cette tâche avec la tâche spécifiée. Utilisé
     * pour classer les tâches en ordre croissant de date.
     */
    public int compareTo(final SamplePosition other) {
        if (time < other.time) return -1;
        if (time > other.time) return +1;
        return 0;
    }

    /**
     * Retourne une liste des <code>SamplePosition</code> à prendre en compte.
     *
     * @param samples   Liste des captures dont on voudra les paramètres environnementaux.
     * @param positions Positions et jours où extraire des données, avant, pendant
     *                  et après le jour de l'échantillon.
     * @param coverage  L'objet qui servira (plus tard) à calculer les paramètres.
     */
    public static SamplePosition[] getInstances(final Collection<? extends SampleEntry> samples,
                                                final Collection<? extends RelativePositionEntry> positions,
                                                final SeriesCoverage3D coverage)
    {
        final SamplePosition[] tasks = new SamplePosition[samples.size() * positions.size()];
        int i=0;
        for (final RelativePositionEntry position : positions) {
            for (final SampleEntry sample : samples) {
                final Date time = position.getTime(sample);
                coverage.adjust(time);
                tasks[i++] = new SamplePosition(sample, position, time);
            }
        }
        assert i == tasks.length;
        Arrays.sort(tasks);
        return tasks;
    }
}
