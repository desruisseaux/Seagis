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
package fr.ird.database.sample;

// Divers
import java.util.Date;
import java.util.Arrays;
import java.util.Collection;
import java.sql.SQLException;


/**
 * Un {@linkplain SampleEntry �chantillon} associ� � une {@linkplain RelativePositionEntry
 * position relative}. Ces paires seront class�es en ordre croissant de date, afin de r�duire
 * le nombre d'images � charger. Sans classement, de fr�quent retours en arri�re seraient
 * n�cessaires. Ces objets sont utilis�s par {@link EnvironmentColumnFiller}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SamplePosition implements Comparable<SamplePosition> {
    /**
     * L'�chantillon � �valuer.
     */
    public final SampleEntry sample;

    /**
     * La position relative � laquelle �valuer les param�tres environnementaux.
     */
    public final RelativePositionEntry position;

    /**
     * La date � laquelle �valuer les param�tres environnementaux,
     * en nombre de millisecondes �coul�es depuis le 1er janvier 1970.
     */
    final long time;

    /**
     * Construit une nouvelle t�che.
     *
     * @param sample   L'�chantillon � �valuer.
     * @param position La position relative � laquelle �valuer les param�tres environnementaux.
     * @param time La date � laquelle �valuer l'�chantillon. Cette date pourrait �tre d�duite
     *        des arguments <code>sample</code> et <code>position</code>, mais un ajustement
     *        suppl�mentaire est parfois fait pour r�duire le nombre d'interpolation temporelles
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
     * Compare cette t�che avec la t�che sp�cifi�e. Utilis�
     * pour classer les t�ches en ordre croissant de date.
     */
    public int compareTo(final SamplePosition other) {
        if (time < other.time) return -1;
        if (time > other.time) return +1;
        return 0;
    }

    /**
     * Retourne une liste des <code>SamplePosition</code> � prendre en compte.
     *
     * @param samples   Liste des captures dont on voudra les param�tres environnementaux.
     * @param positions Positions et jours o� extraire des donn�es, avant, pendant
     *                  et apr�s le jour de l'�chantillon.
     * @param coverage  L'objet qui servira (plus tard) � calculer les param�tres.
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
