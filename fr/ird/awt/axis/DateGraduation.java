/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contacts: Observatoire du Saint-Laurent         Michel Petit
 *           Institut Maurice Lamontagne           Institut de Recherche pour le Développement
 *           850 de la Mer, C.P. 1000              500 rue Jean-François Breton
 *           Mont-Joli (Québec)                    34093 Montpellier
 *           G5H 3Z4                               France
 *           Canada
 *
 *           mailto:osl@osl.gc.ca                  mailto:Michel.Petit@teledetection.fr
 */
package fr.ird.awt.axis;

// Miscellaneous
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import org.geotools.units.Unit;
import java.awt.RenderingHints;
import org.geotools.resources.Utilities;


/**
 * A graduation using dates on a linear axis.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class DateGraduation extends AbstractGraduation
{
    /**
     * The minimal value for this graduation.
     */
    private long minimum = System.currentTimeMillis();

    /**
     * The maximal value for this graduation.
     */
    private long maximum = minimum + 24*60*60*1000L;

    /**
     * The time zone for graduation labels.
     */
    private TimeZone timezone;

    /**
     * Construct a graduation with the supplied time zone.
     */
    public DateGraduation(final TimeZone timezone)
    {this.timezone = (TimeZone) timezone.clone();}

    /**
     * Set the minimum value for this graduation. If the new minimum is greater
     * than the current maximum, then the maximum will also be set to a value
     * greater than or equals to the minimum.
     *
     * @param value The new minimum in milliseconds ellapsed
     *              since January 1st, 1970 at 00:00 UTC.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     *
     * @see #setMaximum(long)
     */
    public synchronized boolean setMinimum(final long value)
    {
        long old=minimum;
        minimum = value;
        firePropertyChange("minimum", old, value);
        if (maximum<=value)
        {
            old = maximum;
            maximum = value;
            firePropertyChange("maximum", old, value);
            return true;
        }
        return value != old;
    }

    /**
     * Set the maximum value for this graduation. If the new maximum is less
     * than the current minimum, then the minimum will also be set to a value
     * less than or equals to the maximum.
     *
     * @param value The new maximum in milliseconds ellapsed
     *              since January 1st, 1970 at 00:00 UTC.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     *
     * @see #setMinimum(long)
     */
    public synchronized boolean setMaximum(final long value)
    {
        long old=maximum;
        maximum = value;
        firePropertyChange("maximum", old, value);
        if (minimum>=value)
        {
            old = minimum;
            minimum = value;
            firePropertyChange("minimum", old, value);
            return true;
        }
        return value != old;
    }

    /**
     * Set the minimum value as a real number. This
     * method invokes {@link #setMinimum(long)}.
     */
    public final boolean setMinimum(final double value)
    {
        ensureFinite("minimum", value);
        return setMinimum(Math.round(value));
    }

    /**
     * Set the maximum value as a real number. This
     * method invokes {@link #setMaximum(long)}.
     */
    public final boolean setMaximum(final double value)
    {
        ensureFinite("maximum", value);
        return setMaximum(Math.round(value));
    }

    /**
     * Returns the minimal value for this graduation. The value
     * is the number of millisecondes ellapsed since January 1st,
     * 1970 at 00:00 UTC.
     *
     * @see #setMinimum(long)
     * @see #getMaximum
     * @see #getRange
     */
    public double getMinimum()
    {return minimum;}

    /**
     * Returns the maximal value for this graduation. The value
     * is the number of millisecondes ellapsed since January 1st,
     * 1970 at 00:00 UTC.
     *
     * @see #setMaximum(long)
     * @see #getMinimum
     * @see #getRange
     */
    public double getMaximum()
    {return maximum;}

    /**
     * Returns the graduation's range. This is equivalents to computing
     * <code>{@link #getMaximum}-{@link #getMinimum}</code>, but using
     * integer arithmetic.
     */
    public synchronized double getRange()
    {return (maximum-minimum);}

    /**
     * Returns a string representation of the time zone for this graduation.
     */
    public String getUnitLabel()
    {return getTimeZone().getDisplayName();}

    /**
     * Returns the units for this graduation. For
     * a time axis, this is always milliseconds.
     */
    public Unit getUnit()
    {return Unit.MILLISECOND;}

    /**
     * Returns the timezone for this graduation.
     */
    public TimeZone getTimeZone()
    {return timezone;}

    /**
     * Sets the time zone for this graduation. This
     * affect only the way labels are displayed.
     */
    public void setTimeZone(final TimeZone timezone)
    {this.timezone = (TimeZone) timezone.clone();}

    /**
     * Returns the format to use for formatting labels. The format
     * really used by {@link TickIterator#getLabel} may not be the
     * same. For example, some iterators may choose to show or hide
     * hours, minutes and seconds.
     */
    public Format getFormat()
    {
        final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale());
        format.setTimeZone(timezone);
        return format;
    }
    
    /**
     * Returns an iterator object that iterates along the graduation ticks
     * and provides access to the graduation values. If an optional {@link
     * RenderingHints} is specified, tick locations are adjusted according
     * values for {@link #VISUAL_AXIS_LENGTH} and {@link #VISUAL_TICK_SPACING}
     * keys.
     *
     * @param  hints Rendering hints, or <code>null</code> for the default hints.
     * @param  reuse An iterator to reuse if possible, or <code>null</code>
     *         to create a new one. A non-null object may help to reduce the
     *         number of object garbage-collected when rendering the axis.
     * @return A iterator to use for iterating through the graduation. This
     *         iterator may or may not be the <code>reuse</code> object.
     */
    public synchronized TickIterator getTickIterator(final RenderingHints hints, final TickIterator reuse)
    {
        final float visualAxisLength  = getVisualAxisLength (hints);
        final float visualTickSpacing = getVisualTickSpacing(hints);
        long minimum = this.minimum;
        long maximum = this.maximum;
        if (!(minimum<maximum))
        {
            minimum = (minimum+maximum)/2 - 12*60*60*1000L;
            maximum = minimum + 24*60*60*1000L;
        }
        final DateIterator it;
        if (reuse instanceof DateIterator)
        {
            it = (DateIterator) reuse;
            it.setLocale(getLocale());
            it.setTimeZone(getTimeZone());
        }
        else
        {
            it = new DateIterator(getTimeZone(), getLocale());
        }
        it.init(minimum, maximum, visualAxisLength, visualTickSpacing);
        return it;
    }

    /**
     * Support for reporting property changes. This method can be called when a
     * property has changed. It will send the appropriate {@link PropertyChangeEvent}
     * to any registered {@link PropertyChangeListeners}.
     *
     * @param propertyName The property whose value has changed.
     * @param oldValue     The property's previous value.
     * @param newValue     The property's new value.
     */
    protected final void firePropertyChange(final String propertyName, final long oldValue, final long newValue)
    {
        if (oldValue != newValue)
            firePropertyChange(propertyName, new Date(oldValue), new Date(newValue));
    }

    /**
     * Compare this graduation with the
     * specified object for equality.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && object.getClass().equals(getClass()))
        {
            final DateGraduation that = (DateGraduation) object;
            return this.minimum == that.minimum &&
                   this.maximum == that.maximum &&
                   Utilities.equals(this.timezone, that.timezone);
        }
        return false;
    }

    /**
     * Returns a hash value for this graduation.
     */
    public int hashCode()
    {
        final long lcode = minimum + 37*maximum;
        int code = (int)lcode ^ (int)(lcode >>> 32);
        if (timezone!=null)
        {
            code=37*code + timezone.hashCode();
        }
        return code;
    }
}
