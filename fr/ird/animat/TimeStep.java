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
package fr.ird.animat;

// J2SE dependencies
import java.util.Date;
import java.io.Serializable;
import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;

// Geotools dependencies
import org.geotools.resources.Utilities;

// seagis dependencies
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import fr.ird.science.astro.SunRelativePosition;


/**
 * Un pas de temps dans l'exécution de la simulation. Chaque pas de temps est exprimée
 * par une date (de début) et une durée.  Pendant un pas de temps, tout les paramètres
 * de {@link Environment} sont considérés constants.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class TimeStep implements Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -9038388898678050330L;

    /**
     * L'objet à utiliser pour calculer la hauteur relative du soleil.
     */
    private static final SunRelativePosition calculator = new SunRelativePosition();

    /**
     * Date de début du pas de temps, en nombre de millisecondes
     * écoulées depuis le 1er janvier 1970 00:00 UTC.
     */
    private final long time;

    /**
     * Durée du pas de temps, en nombre de millisecondes.
     */
    private final long duration;

    /**
     * Construit un nouveau pas de temps.
     *
     * @param  startTime Date de début (inclusuve) du pas de temps.
     * @param  endTime   Date de fin   (exclusive) du pas de temps.
     * @throws IllegalArgumentException Si la date de fin précède la date de début.
     */
    public TimeStep(final Date startTime, final Date endTime) throws IllegalArgumentException {
        time = startTime.getTime();
        duration = endTime.getTime() - time;
        if (duration < 0) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_RANGE_$2,
                                                                startTime, endTime));
        }
    }

    /**
     * Construit un nouveau pas de temps avec la date et la durée spécifiées.
     */
    private TimeStep(final long time, final long duration) {
        this.time     = time;
        this.duration = duration;
    }

    /**
     * Retourne le pas de temps suivant.  La {@linkplain #getStartTime date de début} du pas
     * de temps suivant sera égale à la {@linkplain #getEndTime date de fin} du pas de temps
     * courant. La {@linkplain #getDuration durée} sera la même.
     */
    final TimeStep next() {
        return new TimeStep(time+duration, duration);
    }

    /**
     * Retourne la date de début (inclusive) de ce pas de temps.
     */
    public Date getStartTime() {
        return new Date(time);
    }

    /**
     * Retourne la date de fin (exclusive) de ce pas de temps.
     */
    public Date getEndTime() {
        return new Date(time + duration);
    }

    /**
     * Retourne la durée de ce pas de temps, en nombre de jours.
     */
    public float getDuration() {
        return duration / (float)(24*60*60*1000);
    }

    /**
     * Retourne l'élévation du soleil, en degrés par rapport à l'horizon.
     * L'élévation est calculée par rapport à la position spécifiée et la
     * date du milieu de ce pas de temps.
     *
     * @param  position Position, en degrés de longitude et de latitude.
     * @return Angle d'élévation du soleil, en degrés par rapport à l'horizon.
     */
    public float getSunElevation(final Point2D position) {
        calculator.compute(position.getX(), position.getY(), new Date(time + duration/2));
        return (float) calculator.getElevation();
    }

    /**
     * Vérifie si ce pas de temps est identique à l'objet spécifié.
     */
    public boolean equals(final Object object) {
        if (object instanceof TimeStep) {
            final TimeStep that = (TimeStep) object;
            return this.time     == that.time &&
                   this.duration == that.duration;
        }
        return false;
    }

    /**
     * Retourne un "hash code" pour ce pas de temps.
     */
    public int hashCode() {
        final long code = time ^ duration;
        return (int)(code) ^ (int)(code >>> 32);
    }

    /**
     * Retourne une représentation sous forme de texte de ce pas de temps.
     */
    public String toString() {
        final NumberFormat nmbFmt = NumberFormat.getNumberInstance();
        final DateFormat   format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append('[');
        format.format(getStartTime(), buffer, new FieldPosition(0));
        buffer.append(", ");
        nmbFmt.format(getDuration(),  buffer, new FieldPosition(0));
        buffer.append(' ');
        buffer.append(Resources.format(ResourceKeys.DAYS));
        buffer.append(']');
        return buffer.toString();
    }
}
