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
package fr.ird.animat.impl;

// J2SE dependencies
import java.util.Date;
import java.util.TimeZone;
import java.io.Serializable;
import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;

// JAI dependencies
import javax.media.jai.util.Range;

// Geotools dependencies
import org.geotools.resources.Utilities;

// seagis dependencies
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import org.geotools.science.astro.SunRelativePosition;


/**
 * Implémentation par défaut de l'horloge de la simulation. En plus de la
 * {@linkplain #getTime() date du pas de temps courant}, l'horloge tient à jour un
 * {@linkplain #getStepSequenceNumber numéro séquentiel de pas de temps}. Ce numéro commence à 0
 * et est incrémenté de 1 chaque fois que la simulation passe au {@linkplain #nextTimeStep pas de
 * temps suivant}. Le pas de temps 0 correspond au pas de temps au moment ou l'horloge a été créée,
 * soit avec {@link #createClock} ou soit avec {@link #getNewClock}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Clock implements fr.ird.animat.Clock, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 5354204301194272638L;

    /**
     * Construit une nouvelle horloge.
     */
    protected Clock() {
    }

    /**
     * Construit une nouvelle horloge initialisée à la plage de temps spécifiée.
     *
     * @param  startTime Date de début (inclusive) du premier pas de temps.
     * @param  endTime   Date de fin   (exclusive) du premier pas de temps.
     * @param  timezone  Le fuseau horaire recommandé pour l'affichage des dates.
     * @throws IllegalArgumentException Si la date de fin précède la date de début.
     */
    public static Clock createClock(final Date startTime, final Date endTime, final TimeZone timezone)
            throws IllegalArgumentException
    {
        return new Default(startTime, endTime, timezone);
    }

    /**
     * Avance l'horloge au pas de temps suivant. La date de début du pas de temps suivant sera
     * égale à la date de fin du pas de temps courant. La {@linkplain #getStepDuration durée}
     * restera la même. Si plusieurs horloges sont synchronisées (ce qui est le cas de toutes les
     * horloges retournées par {@link #getNewClock}), alors toutes les horloges sont avancées en
     * même temps.
     */
    protected abstract void nextTimeStep();

    /**
     * Calcule le numéro séquentiel du pas de temps correspondant à la date spécifiée. Les
     * numéros valides sont compris de 0 à {@link #getStepSequenceNumber()} inclusivement.
     * Si la date spécifiée est antérieure à la date initiale de l'horloge ou ultérieure à
     * la date de fin du pas de temps courant, alors cette méthode signale ce fait en
     * retournant un numéro négatif.
     *
     * @param  time Date pour laquelle on veut le pas de temps, or <code>null</code> pour le
     *         pas de temps courrant.
     * @return Le numéro séquentiel du pas de temps à la date spécifiée, ou un nombre négatif
     *         si la date spécifiée est antérieure à la date initiale de l'horloge ou ultérieure
     *         à la date de fin du pas de temps courant.
     */
    protected abstract int computeStepSequenceNumber(final Date time);

    /**
     * Retourne le numéro séquentiel du pas de temps correspondant à la date spécifiée.
     * Ce numéro sera compris de 0 à {@link #getStepSequenceNumber()} inclusivement.
     *
     * @param  time Date pour laquelle on veut le pas de temps, or <code>null</code> pour le
     *         pas de temps courrant.
     * @return Le numéro séquentiel du pas de temps à la date spécifiée.
     * @throws IllegalArgumentException si la date spécifiée est antérieure à la date initiale
     *         de l'horloge ou ultérieure à la date de fin du pas de temps courant.
     */
    public final int getStepSequenceNumber(final Date time) throws IllegalArgumentException {
        final int n = computeStepSequenceNumber(time);
        if (n >= 0) {
            return n;
        }
        throw new IllegalArgumentException(Resources.format(
                  ResourceKeys.ERROR_DATE_OUTSIDE_COVERAGE_$1, time));
    }

    /**
     * Retourne le numéro séquentiel du pas de temps courant. Ce numéro commence à 0 et est
     * incrémenté de 1 à chaque fois que l'horloge avance d'un pas de temps.
     */
    public abstract int getStepSequenceNumber();

    /**
     * Retourne le temps écoulé depuis la création de cette horloge.
     * Il s'agira de l'âge de l'animal qui est soumis à cette horloge.
     *
     * @return L'âge en nombre de jours.
     */
    public final float getAge() {
        return getStepSequenceNumber() * getStepDuration();
    }

    /**
     * Retourne la date pour le numéro séquentiel spécifié.
     */
    public abstract Date getTime(final int step);

    /**
     * Retourne la date au milieu du pas de temps courant.
     * Cette date est incrémentée toutes les fois que {@link #nextTimeStep} est appelée.
     */
    public abstract Date getTime();

    /**
     * Retourne la plage de temps du pas de temps courant.
     */
    public abstract Range getTimeRange();

    /**
     * Retourne la durée du pas de temps courant, en nombre de jours.
     */
    protected abstract float getStepDuration();

    /**
     * Retourne le fuseau horaire recommandé pour l'affichage des dates.
     * Ce fuseau horaire dépend de la région géographique de la simulation.
     */
    public abstract TimeZone getTimeZone();

    /**
     * Retourne l'élévation du soleil, en degrés par rapport à l'horizon.
     * L'élévation est calculée par rapport à la position spécifiée et la
     * date du milieu du pas de temps courant.
     *
     * @param  position Position, en degrés de longitude et de latitude.
     * @return Angle d'élévation du soleil, en degrés par rapport à l'horizon.
     */
    public abstract float getSunElevation(final Point2D position);

    /**
     * Retourne une nouvelle horloge avec le même pas de temps que <code>this</code>, mais dont le
     * numéro de pas de temps courant sera 0. Les appels de {@link #nextTimeStep} sont synchronisés,
     * c'est-à-dire qu'appeller {@link #nextTimeStep} sur une horloge avancera toutes les horloges
     * de la même façon. En d'autres mots, le temps continue de s'écouler de la même façon pour tous
     * les animaux. La principale différence est que l'appel de la méthode {@link #getStepSequenceNumber()}
     * peut retourner un numéro de pas de temps plus petit. Cette méthode est habituellement
     * appelée lorsque de nouveaux animaux viennent d'être créés. La date courantes est la même
     * pour ces animaux que pour tous les autres, mais leur âge (mesuré ici par le nombre de pas
     * de temps depuis la création de l'horloge) est plus petit.
     */
    protected abstract Clock getNewClock();

    /**
     * Retourne une représentation sous forme de texte de ce pas de temps.
     */
    public String toString() {
        final NumberFormat nmbFmt = NumberFormat.getNumberInstance();
        final DateFormat   format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        format.setTimeZone(getTimeZone());
        buffer.append('[');
        format.format(getTime(), buffer, new FieldPosition(0));
        buffer.append(", ");
        nmbFmt.format(getStepDuration(), buffer, new FieldPosition(0));
        buffer.append(' ');
        buffer.append(Resources.format(ResourceKeys.DAYS));
        buffer.append(']');
        return buffer.toString();
    }




    /**
     * Horloge identique à une autre, mais ayant démaré à un pas de temps différent.
     * 
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Delayed extends Clock {
        /**
         * Numéro de série pour compatibilité entre différentes versions.
         */
        private static final long serialVersionUID = -6947587093135725216L;

        /**
         * Numéro de pas de temps initial.
         */
        private final int offset;

        /**
         * Construit une nouvelle horloge qui prendre la date actuelle
         * comme la date de son pas de temps 0.
         */
        public Delayed() {
            offset = Clock.this.getStepSequenceNumber();
        }

        /**
         * Avance l'horloge au pas de temps suivant.
         */
        protected void nextTimeStep() {
            Clock.this.nextTimeStep();
        }

        /**
         * Retourne le numéro séquentiel du pas de temps correspondant à la date spécifiée.
         */
        protected int computeStepSequenceNumber(final Date time) {
            return Clock.this.getStepSequenceNumber(time) - offset;
        }

        /**
         * Retourne le numéro séquentiel du pas de temps courant.
         */
        public int getStepSequenceNumber() {
            return Clock.this.getStepSequenceNumber() - offset;
        }
        
        /**
         * Retourne la date pour le numéro séquentiel spécifié.
         */
        public Date getTime(final int step) {
            return Clock.this.getTime(step + offset);
        }

        /**
         * Retourne la date au milieu du pas de temps courant.
         */
        public Date getTime() {
            return Clock.this.getTime();
        }

        /**
         * Retourne la plage de temps du pas de temps courant.
         */
        public Range getTimeRange() {
            return Clock.this.getTimeRange();
        }

        /**
         * Retourne le fuseau horaire recommandé pour l'affichage des dates.
         */
        public TimeZone getTimeZone() {
            return Clock.this.getTimeZone();
        }

        /**
         * Retourne la durée du pas de temps courant, en nombre de jours.
         */
        protected float getStepDuration() {
            return Clock.this.getStepDuration();
        }

        /**
         * Retourne l'élévation du soleil, en degrés par rapport à l'horizon.
         */
        public float getSunElevation(final Point2D position) {
            return Clock.this.getSunElevation(position);
        }

        /**
         * Retourne une nouvelle horloge avec le même pas de temps que <code>this</code>,
         * mais dont le numéro de pas de temps courant sera 0.
         */
        protected Clock getNewClock() {
            return Clock.this.getNewClock();
        }

        /**
         * For the implementation of <code>equals</code>.
         */
        private Clock getClock() {
            return Clock.this;
        }

        /**
         * Vérifie si cette horloge est identique à l'objet spécifié.
         */
        public boolean equals(final Object object) {
            if (object instanceof Delayed) {
                final Delayed that = (Delayed) object;
                return this.offset == that.offset && Clock.this.equals(that.getClock());
            }
            return false;
        }

        /**
         * Retourne un "hash code" pour cette horloge.
         */
        public int hashCode() {
            return Clock.this.hashCode() ^ offset;
        }
    }




    /**
     * Implémentation par défaut de l'horloge de l'application.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Default extends Clock {
        /**
         * Numéro de série pour compatibilité entre différentes versions.
         */
        private static final long serialVersionUID = 9145853389011257303L;

        /**
         * L'objet à utiliser pour calculer la hauteur relative du soleil.
         */
        private final SunRelativePosition calculator = new SunRelativePosition();

        /**
         * Le fuseau horaire recommandé pour l'affichage des dates.
         */
        private final TimeZone timezone;

        /**
         * Date de début du pas, en nombre de millisecondes écoulées depuis le
         * 1er janvier 1970 00:00 UTC. Il s'agit de la date <code>startTime</code>
         * initiale spécifiée lors de la construction de cette horloge.
         */
        private final long initialTime;

        /**
         * Date de début du pas de temps courant, en nombre de millisecondes
         * écoulées depuis le 1er janvier 1970 00:00 UTC.
         */
        private long time;

        /**
         * Durée du pas de temps, en nombre de millisecondes.
         */
        private final long duration;

        /**
         * Une horloge représentant le même pas de temps que <code>this</code>, mais dont
         * le numéro de pas de temps sera 0. Cet objet sera créé par {@link #getNewClock}
         * lorsque nécessaire et remis à <code>null</code> par {@link #nextTimeStep}.
         */
        private transient Clock delayed = this;

        /**
         * Construit une nouvelle horloge initialisée à la plage de temps spécifiée.
         *
         * @param  startTime Date de début (inclusive) du premier pas de temps.
         * @param  endTime   Date de fin   (exclusive) du premier pas de temps.
         * @param  timezone  Le fuseau horaire recommandé pour l'affichage des dates.
         * @throws IllegalArgumentException Si la date de fin précède la date de début.
         */
        public Default(final Date startTime, final Date endTime, final TimeZone timezone)
                throws IllegalArgumentException
        {
            this.timezone = timezone;
            time = initialTime = startTime.getTime();
            duration = endTime.getTime() - time;
            if (duration < 0) {
                throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_RANGE_$2,
                                                                    startTime, endTime));
            }
        }

        /**
         * Avance l'horloge au pas de temps suivant. La date de début du pas de temps suivant sera
         * égale à la date de fin du pas de temps courant. La {@linkplain #getStepDuration durée}
         * restera la même.
         */
        protected void nextTimeStep() {
            time += duration;
            delayed = null;
        }

        /**
         * Retourne le numéro séquentiel du pas de temps correspondant à la date spécifiée.
         * Ce numéro sera compris de 0 à {@link #getStepSequenceNumber()} inclusivement.
         */
        protected int computeStepSequenceNumber(final Date time) {
            if (time == null) {
                return getStepSequenceNumber();
            }
            long delta = time.getTime();
            if (delta < this.time + duration) {
                delta -= initialTime;
                if (delta >= 0) {
                    return (int) (delta/duration);
                }
            }
            return -1;
        }

        /**
         * Retourne le numéro séquentiel du pas de temps courant. Ce numéro commence à 0 et est
         * incrémenté de 1 à chaque fois que {@link #nextTimeStep} est appelée.
         */
        public int getStepSequenceNumber() {
            final long delta = time - initialTime;
            assert delta>=0 && (delta % duration)==0 : delta;
            return (int) (delta/duration);
        }
        
        /**
         * Retourne la date pour le numéro séquentiel spécifié.
         */
        public Date getTime(final int step) {
            return new Date(initialTime + step*duration);
        }

        /**
         * Retourne la date au milieu du pas de temps courant.
         */
        public Date getTime() {
            return new Date(time + duration/2);
        }

        /**
         * Retourne la plage de temps du pas de temps courant.
         */
        public Range getTimeRange() {
            return new Range(Date.class, new Date(time), true, new Date(time+duration), false);
        }

        /**
         * Retourne le fuseau horaire recommandé pour l'affichage des dates.
         */
        public TimeZone getTimeZone() {
            return timezone;
        }

        /**
         * Retourne la durée du pas de temps courant, en nombre de jours.
         */
        protected float getStepDuration() {
            return duration / (float)(24*60*60*1000);
        }

        /**
         * Retourne l'élévation du soleil, en degrés par rapport à l'horizon.
         * L'élévation est calculée par rapport à la position spécifiée et la
         * date du milieu du pas de temps courant.
         *
         * @param  position Position, en degrés de longitude et de latitude.
         * @return Angle d'élévation du soleil, en degrés par rapport à l'horizon.
         */
        public float getSunElevation(final Point2D position) {
            calculator.setCoordinate(position.getX(), position.getY());
            calculator.setDate(new Date(time + duration/2));
            return (float) calculator.getElevation();
        }

        /**
         * Retourne une nouvelle horloge avec le même pas de temps que <code>this</code>,
         * mais dont le numéro de pas de temps courant sera 0.
         */
        protected Clock getNewClock() {
            if (delayed == null) {
                delayed = new Delayed();
            }
            return delayed;
        }

        /**
         * Vérifie si cette horloge est identique à l'objet spécifié.
         */
        public boolean equals(final Object object) {
            if (object instanceof Default) {
                final Default that = (Default) object;
                return this.time     == that.time &&
                       this.duration == that.duration;
            }
            return false;
        }

        /**
         * Retourne un "hash code" pour cette horloge.
         */
        public int hashCode() {
            final long code = time ^ duration;
            return (int)(code) ^ (int)(code >>> 32);
        }
    }
}
