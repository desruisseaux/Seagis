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
 * Impl�mentation par d�faut de l'horloge de la simulation. En plus de la
 * {@linkplain #getTime() date du pas de temps courant}, l'horloge tient � jour un
 * {@linkplain #getStepSequenceNumber num�ro s�quentiel de pas de temps}. Ce num�ro commence � 0
 * et est incr�ment� de 1 chaque fois que la simulation passe au {@linkplain #nextTimeStep pas de
 * temps suivant}. Le pas de temps 0 correspond au pas de temps au moment ou l'horloge a �t� cr��e,
 * soit avec {@link #createClock} ou soit avec {@link #getNewClock}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Clock implements fr.ird.animat.Clock, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 5354204301194272638L;

    /**
     * Construit une nouvelle horloge.
     */
    protected Clock() {
    }

    /**
     * Construit une nouvelle horloge initialis�e � la plage de temps sp�cifi�e.
     *
     * @param  startTime Date de d�but (inclusive) du premier pas de temps.
     * @param  endTime   Date de fin   (exclusive) du premier pas de temps.
     * @param  timezone  Le fuseau horaire recommand� pour l'affichage des dates.
     * @throws IllegalArgumentException Si la date de fin pr�c�de la date de d�but.
     */
    public static Clock createClock(final Date startTime, final Date endTime, final TimeZone timezone)
            throws IllegalArgumentException
    {
        return new Default(startTime, endTime, timezone);
    }

    /**
     * Avance l'horloge au pas de temps suivant. La date de d�but du pas de temps suivant sera
     * �gale � la date de fin du pas de temps courant. La {@linkplain #getStepDuration dur�e}
     * restera la m�me. Si plusieurs horloges sont synchronis�es (ce qui est le cas de toutes les
     * horloges retourn�es par {@link #getNewClock}), alors toutes les horloges sont avanc�es en
     * m�me temps.
     */
    protected abstract void nextTimeStep();

    /**
     * Calcule le num�ro s�quentiel du pas de temps correspondant � la date sp�cifi�e. Les
     * num�ros valides sont compris de 0 � {@link #getStepSequenceNumber()} inclusivement.
     * Si la date sp�cifi�e est ant�rieure � la date initiale de l'horloge ou ult�rieure �
     * la date de fin du pas de temps courant, alors cette m�thode signale ce fait en
     * retournant un num�ro n�gatif.
     *
     * @param  time Date pour laquelle on veut le pas de temps, or <code>null</code> pour le
     *         pas de temps courrant.
     * @return Le num�ro s�quentiel du pas de temps � la date sp�cifi�e, ou un nombre n�gatif
     *         si la date sp�cifi�e est ant�rieure � la date initiale de l'horloge ou ult�rieure
     *         � la date de fin du pas de temps courant.
     */
    protected abstract int computeStepSequenceNumber(final Date time);

    /**
     * Retourne le num�ro s�quentiel du pas de temps correspondant � la date sp�cifi�e.
     * Ce num�ro sera compris de 0 � {@link #getStepSequenceNumber()} inclusivement.
     *
     * @param  time Date pour laquelle on veut le pas de temps, or <code>null</code> pour le
     *         pas de temps courrant.
     * @return Le num�ro s�quentiel du pas de temps � la date sp�cifi�e.
     * @throws IllegalArgumentException si la date sp�cifi�e est ant�rieure � la date initiale
     *         de l'horloge ou ult�rieure � la date de fin du pas de temps courant.
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
     * Retourne le num�ro s�quentiel du pas de temps courant. Ce num�ro commence � 0 et est
     * incr�ment� de 1 � chaque fois que l'horloge avance d'un pas de temps.
     */
    public abstract int getStepSequenceNumber();

    /**
     * Retourne le temps �coul� depuis la cr�ation de cette horloge.
     * Il s'agira de l'�ge de l'animal qui est soumis � cette horloge.
     *
     * @return L'�ge en nombre de jours.
     */
    public final float getAge() {
        return getStepSequenceNumber() * getStepDuration();
    }

    /**
     * Retourne la date pour le num�ro s�quentiel sp�cifi�.
     */
    public abstract Date getTime(final int step);

    /**
     * Retourne la date au milieu du pas de temps courant.
     * Cette date est incr�ment�e toutes les fois que {@link #nextTimeStep} est appel�e.
     */
    public abstract Date getTime();

    /**
     * Retourne la plage de temps du pas de temps courant.
     */
    public abstract Range getTimeRange();

    /**
     * Retourne la dur�e du pas de temps courant, en nombre de jours.
     */
    protected abstract float getStepDuration();

    /**
     * Retourne le fuseau horaire recommand� pour l'affichage des dates.
     * Ce fuseau horaire d�pend de la r�gion g�ographique de la simulation.
     */
    public abstract TimeZone getTimeZone();

    /**
     * Retourne l'�l�vation du soleil, en degr�s par rapport � l'horizon.
     * L'�l�vation est calcul�e par rapport � la position sp�cifi�e et la
     * date du milieu du pas de temps courant.
     *
     * @param  position Position, en degr�s de longitude et de latitude.
     * @return Angle d'�l�vation du soleil, en degr�s par rapport � l'horizon.
     */
    public abstract float getSunElevation(final Point2D position);

    /**
     * Retourne une nouvelle horloge avec le m�me pas de temps que <code>this</code>, mais dont le
     * num�ro de pas de temps courant sera 0. Les appels de {@link #nextTimeStep} sont synchronis�s,
     * c'est-�-dire qu'appeller {@link #nextTimeStep} sur une horloge avancera toutes les horloges
     * de la m�me fa�on. En d'autres mots, le temps continue de s'�couler de la m�me fa�on pour tous
     * les animaux. La principale diff�rence est que l'appel de la m�thode {@link #getStepSequenceNumber()}
     * peut retourner un num�ro de pas de temps plus petit. Cette m�thode est habituellement
     * appel�e lorsque de nouveaux animaux viennent d'�tre cr��s. La date courantes est la m�me
     * pour ces animaux que pour tous les autres, mais leur �ge (mesur� ici par le nombre de pas
     * de temps depuis la cr�ation de l'horloge) est plus petit.
     */
    protected abstract Clock getNewClock();

    /**
     * Retourne une repr�sentation sous forme de texte de ce pas de temps.
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
     * Horloge identique � une autre, mais ayant d�mar� � un pas de temps diff�rent.
     * 
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Delayed extends Clock {
        /**
         * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
         */
        private static final long serialVersionUID = -6947587093135725216L;

        /**
         * Num�ro de pas de temps initial.
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
         * Retourne le num�ro s�quentiel du pas de temps correspondant � la date sp�cifi�e.
         */
        protected int computeStepSequenceNumber(final Date time) {
            return Clock.this.getStepSequenceNumber(time) - offset;
        }

        /**
         * Retourne le num�ro s�quentiel du pas de temps courant.
         */
        public int getStepSequenceNumber() {
            return Clock.this.getStepSequenceNumber() - offset;
        }
        
        /**
         * Retourne la date pour le num�ro s�quentiel sp�cifi�.
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
         * Retourne le fuseau horaire recommand� pour l'affichage des dates.
         */
        public TimeZone getTimeZone() {
            return Clock.this.getTimeZone();
        }

        /**
         * Retourne la dur�e du pas de temps courant, en nombre de jours.
         */
        protected float getStepDuration() {
            return Clock.this.getStepDuration();
        }

        /**
         * Retourne l'�l�vation du soleil, en degr�s par rapport � l'horizon.
         */
        public float getSunElevation(final Point2D position) {
            return Clock.this.getSunElevation(position);
        }

        /**
         * Retourne une nouvelle horloge avec le m�me pas de temps que <code>this</code>,
         * mais dont le num�ro de pas de temps courant sera 0.
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
         * V�rifie si cette horloge est identique � l'objet sp�cifi�.
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
     * Impl�mentation par d�faut de l'horloge de l'application.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Default extends Clock {
        /**
         * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
         */
        private static final long serialVersionUID = 9145853389011257303L;

        /**
         * L'objet � utiliser pour calculer la hauteur relative du soleil.
         */
        private final SunRelativePosition calculator = new SunRelativePosition();

        /**
         * Le fuseau horaire recommand� pour l'affichage des dates.
         */
        private final TimeZone timezone;

        /**
         * Date de d�but du pas, en nombre de millisecondes �coul�es depuis le
         * 1er janvier 1970 00:00 UTC. Il s'agit de la date <code>startTime</code>
         * initiale sp�cifi�e lors de la construction de cette horloge.
         */
        private final long initialTime;

        /**
         * Date de d�but du pas de temps courant, en nombre de millisecondes
         * �coul�es depuis le 1er janvier 1970 00:00 UTC.
         */
        private long time;

        /**
         * Dur�e du pas de temps, en nombre de millisecondes.
         */
        private final long duration;

        /**
         * Une horloge repr�sentant le m�me pas de temps que <code>this</code>, mais dont
         * le num�ro de pas de temps sera 0. Cet objet sera cr�� par {@link #getNewClock}
         * lorsque n�cessaire et remis � <code>null</code> par {@link #nextTimeStep}.
         */
        private transient Clock delayed = this;

        /**
         * Construit une nouvelle horloge initialis�e � la plage de temps sp�cifi�e.
         *
         * @param  startTime Date de d�but (inclusive) du premier pas de temps.
         * @param  endTime   Date de fin   (exclusive) du premier pas de temps.
         * @param  timezone  Le fuseau horaire recommand� pour l'affichage des dates.
         * @throws IllegalArgumentException Si la date de fin pr�c�de la date de d�but.
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
         * Avance l'horloge au pas de temps suivant. La date de d�but du pas de temps suivant sera
         * �gale � la date de fin du pas de temps courant. La {@linkplain #getStepDuration dur�e}
         * restera la m�me.
         */
        protected void nextTimeStep() {
            time += duration;
            delayed = null;
        }

        /**
         * Retourne le num�ro s�quentiel du pas de temps correspondant � la date sp�cifi�e.
         * Ce num�ro sera compris de 0 � {@link #getStepSequenceNumber()} inclusivement.
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
         * Retourne le num�ro s�quentiel du pas de temps courant. Ce num�ro commence � 0 et est
         * incr�ment� de 1 � chaque fois que {@link #nextTimeStep} est appel�e.
         */
        public int getStepSequenceNumber() {
            final long delta = time - initialTime;
            assert delta>=0 && (delta % duration)==0 : delta;
            return (int) (delta/duration);
        }
        
        /**
         * Retourne la date pour le num�ro s�quentiel sp�cifi�.
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
         * Retourne le fuseau horaire recommand� pour l'affichage des dates.
         */
        public TimeZone getTimeZone() {
            return timezone;
        }

        /**
         * Retourne la dur�e du pas de temps courant, en nombre de jours.
         */
        protected float getStepDuration() {
            return duration / (float)(24*60*60*1000);
        }

        /**
         * Retourne l'�l�vation du soleil, en degr�s par rapport � l'horizon.
         * L'�l�vation est calcul�e par rapport � la position sp�cifi�e et la
         * date du milieu du pas de temps courant.
         *
         * @param  position Position, en degr�s de longitude et de latitude.
         * @return Angle d'�l�vation du soleil, en degr�s par rapport � l'horizon.
         */
        public float getSunElevation(final Point2D position) {
            calculator.setCoordinate(position.getX(), position.getY());
            calculator.setDate(new Date(time + duration/2));
            return (float) calculator.getElevation();
        }

        /**
         * Retourne une nouvelle horloge avec le m�me pas de temps que <code>this</code>,
         * mais dont le num�ro de pas de temps courant sera 0.
         */
        protected Clock getNewClock() {
            if (delayed == null) {
                delayed = new Delayed();
            }
            return delayed;
        }

        /**
         * V�rifie si cette horloge est identique � l'objet sp�cifi�.
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
