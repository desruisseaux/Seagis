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
package net.seas.plot.axis;

// Miscellaneous
import org.geotools.units.Unit;
import java.util.Locale;
import java.text.Format;
import java.text.NumberFormat;
import java.awt.RenderingHints;
import org.geotools.resources.Utilities;


/**
 * A graduation using numbers on a linear axis. This
 * is the default graduation used in most charts.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class NumberGraduation extends AbstractGraduation
{
    /**
     * The minimal value for this graduation.
     */
    private double minimum=0;

    /**
     * The maximal value for this graduation.
     */
    private double maximum=10;

    /**
     * The axis's units, or <code>null</code> if unknow.
     */
    private Unit unit;

    /**
     * Construct a graduation with the supplied units.
     *
     * @param units The axis's units, or <code>null</code> if unknow.
     */
    public NumberGraduation(final Unit unit)
    {this.unit=unit;}

    /**
     * Set the minimum value for this graduation. If the new minimum is greater
     * than the current maximum, then the maximum will also be set to a value
     * greater than or equals to the minimum.
     *
     * @param  value The new minimum in {@link #getUnit} units.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     * @throws IllegalArgumentException Si <code>value</code> is NaN ou infinite.
     *
     * @see #getMinimum
     * @see #setMaximum(double)
     */
    public synchronized boolean setMinimum(final double value) throws IllegalArgumentException
    {
        ensureFinite("minimum", value);
        double old = minimum;
        minimum    = value;
        firePropertyChange("minimum", old, value);
        if (maximum<=value)
        {
            old = maximum;
            maximum = value;
            firePropertyChange("maximum", old, value);
            return true;
        }
        return Double.doubleToLongBits(value) != Double.doubleToLongBits(old);
    }

    /**
     * Set the maximum value for this graduation. If the new maximum is less
     * than the current minimum, then the minimum will also be set to a value
     * less than or equals to the maximum.
     *
     * @param  value The new maximum in {@link #getUnit} units.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     * @throws IllegalArgumentException If <code>value</code> is NaN ou infinite.
     *
     * @see #getMaximum
     * @see #setMinimum(double)
     */
    public synchronized boolean setMaximum(final double value) throws IllegalArgumentException
    {
        ensureFinite("maximum", value);
        double old = maximum;
        maximum    = value;
        firePropertyChange("maximum", old, value);
        if (minimum>=value)
        {
            old = minimum;
            minimum = value;
            firePropertyChange("minimum", old, value);
            return true;
        }
        return Double.doubleToLongBits(value) != Double.doubleToLongBits(old);
    }

    /**
     * Returns the minimal value for this graduation.
     * @return The minimal value in {@link #getUnit} units.
     *
     * @see #setMinimum(double)
     * @see #getMaximum
     * @see #getRange
     */
    public double getMinimum()
    {return minimum;}

    /**
     * Returns the maximal value for this graduation.
     * @return The maximal value in {@link #getUnit} units.
     *
     * @see #setMaximum(double)
     * @see #getMinimum
     * @see #getRange
     */
    public double getMaximum()
    {return maximum;}

    /**
     * Returns the graduation's range. This is equivalents to computing
     * <code>{@link #getMaximum}-{@link #getMinimum}</code>.
     */
    public synchronized double getRange()
    {return (maximum-minimum);}

    /**
     * Returns the graduation's units,
     * or <code>null</code> if unknow.
     */
    public Unit getUnit()
    {return unit;}

    /**
     * Changes the graduation's units. This method will automatically
     * convert minimum and maximum values from the old units to the
     * new one.
     *
     * @param unit The new units, or <code>null</code> if unknow.
     *        If null, minimum and maximum values are not converted.
     */
    public synchronized void setUnit(final Unit newUnit)
    {
        double min = minimum;
        double max = maximum;
        if (unit!=null && newUnit!=null)
        {
            min = newUnit.convert(min, unit);
            max = newUnit.convert(max, unit);
        }
        setAxis(min, max, newUnit);
    }

    /**
     * Sets the graduation's minimum, maximum and units.
     * This method will fire property change events for
     * <code>"minimum"</code>, <code>"maximum"</code>
     * and <code>"unit"</code> property names.
     */
    public synchronized void setAxis(final double min, final double max, final Unit unit)
    {
        final Unit  oldUnit = this.unit;
        final double oldMin = minimum;
        final double oldMax = maximum;
        this.minimum = Math.min(min, max);
        this.maximum = Math.max(min, max);
        this.unit    = unit;
        firePropertyChange("minimum", oldMin, min);
        firePropertyChange("maximum", oldMax, max);
        firePropertyChange("unit",  oldUnit, unit);
    }

    /**
     * Returns the format to use for formatting labels. The format
     * really used by {@link TickIterator#getLabel} may not be the
     * same. For example, some iterators may adjust automatically
     * the number of fraction digits.
     */
    public Format getFormat()
    {return NumberFormat.getNumberInstance(getLocale());}
    
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
        double minimum = this.minimum;
        double maximum = this.maximum;
        if (!(minimum<maximum))
        {
            minimum = (minimum+maximum)*0.5-0.5;
            maximum = minimum+1;
        }
        final NumberIterator it = getTickIterator(reuse, getLocale());
        it.init(minimum, maximum, visualAxisLength, visualTickSpacing);
        return it;
    }

    /**
     * Construct or reuse an iterator. This method is
     * overriden by {@link LogarithmicNumberGraduation}.
     */
    NumberIterator getTickIterator(final TickIterator reuse, final Locale locale)
    {
        if (reuse!=null && reuse.getClass().equals(NumberIterator.class))
        {
            final NumberIterator it = (NumberIterator) reuse;
            it.setLocale(locale);
            return it;
        }
        else
        {
            return new NumberIterator(locale);
        }
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
    protected final void firePropertyChange(final String propertyName, final double oldValue, final double newValue)
    {
        if (oldValue != newValue)
            firePropertyChange(propertyName, new Double(oldValue), new Double(newValue));
    }

    /**
     * Compare this graduation with the
     * specified object for equality.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && object.getClass().equals(getClass()))
        {
            final NumberGraduation that = (NumberGraduation) object;
            return Double.doubleToLongBits(this.minimum) == Double.doubleToLongBits(that.minimum) &&
                   Double.doubleToLongBits(this.maximum) == Double.doubleToLongBits(that.maximum) &&
                          Utilities.equals(this.unit, that.unit);
        }
        return false;
    }

    /**
     * Returns a hash value for this graduation.
     */
    public int hashCode()
    {
        final long lcode = Double.doubleToLongBits(minimum) +
                        37*Double.doubleToLongBits(maximum);
        int code = (int)lcode ^ (int)(lcode >>> 32);
        if (unit!=null)
        {
            code=37*code + unit.hashCode();
        }
        return code;
    }
}
