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

// Divers
import java.text.Format;
import java.util.Locale;
import javax.units.Unit;
import java.awt.RenderingHints;
import java.beans.PropertyChangeListener;


/**
 * An axis's graduation. A <code>Graduation</code> object encompass minimal
 * and maximal values for an axis in arbitrary units, and allow access to
 * tick locations and labels through a {@link TickIterator} object.
 *
 * Different implementations may compute tick locations in different ways.
 * For example a graduation for dates is handled in a different way than a
 * graduation for numbers.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Graduation
{
    /**
     * Rendering hint for the axis length, in pixels or points (1/72 of inch).
     * Values for this key should be {@link Number} objects. This hint is used
     * together with {@link #VISUAL_TICK_SPACING} during {@link TickIterator}
     * creation in order to compute a tick increment value.
     */
    public static final RenderingHints.Key VISUAL_AXIS_LENGTH = new RenderingHintKey(0);

    /**
     * Rendering hint for the preferred spacing between ticks, in pixels or points
     * (1/72 of inch). Values for this key should be {@link Number} objects. This hint
     * is used together with {@link #VISUAL_AXIS_LENGTH} during {@link TickIterator}
     * creation in order to compute a tick increment value. The tick spacing really
     * used may be slightly different, since {@link TickIterator} may choose a rounded
     * value.
     */
    public static final RenderingHints.Key VISUAL_TICK_SPACING = new RenderingHintKey(1);

    /**
     * Returns the minimal value for this graduation.
     * @return The minimal value in {@link #getUnit} units.
     *
     * @see #getMaximum
     * @see #getRange
     */
    public abstract double getMinimum();

    /**
     * Returns the maximal value for this graduation.
     * @return The maximal value in {@link #getUnit} units.
     *
     * @see #getMinimum
     * @see #getRange
     */
    public abstract double getMaximum();

    /**
     * Returns the graduation's range. This is equivalents to computing
     * <code>{@link #getMaximum}-{@link #getMinimum}</code>. However,
     * some implementation may optimize this computation in order to
     * avoid rounding errors.
     */
    public abstract double getRange();

    /**
     * Returns the axis label. This label should not include units or
     * timezone, since this part is provided by {@link #getUnitLabel}.
     */
    public abstract String getAxisLabel(); // Eviter une collision de nom avec 'TickIterator.getLabel()'.

    /**
     * Returns a string representation of axis's units or timezone, or
     * <code>null</code> if there is none. If non-null, this string is
     * the part usually written after the axis label, as in
     * <code>"label (units)"</code>.
     */
    public abstract String getUnitLabel();

    /**
     * Returns the graduation's units,
     * or <code>null</code> if unknow.
     */
    public abstract Unit getUnit();

    /**
     * Returns the locale to use for formatting labels.
     */
    public abstract Locale getLocale();

    /**
     * Returns the format to use for formatting labels. The format
     * really used by {@link TickIterator#getLabel} may not be the
     * same. For example, some iterators may adjust automatically
     * the number of fraction digits.
     */
    public abstract Format getFormat();

    /**
     * Returns an iterator object that iterates along the graduation ticks
     * and provides access to the graduation values. If an optional {@link
     * RenderingHints} is specified, tick locations are adjusted according
     * values for {@link #VISUAL_AXIS_LENGTH} and {@link #VISUAL_TICK_SPACING}
     * keys.
     *
     * @param  hints Rendering hints for the axis, or <code>null</code> for
     *         the default hints.
     * @param  reuse An iterator to reuse if possible, or <code>null</code>
     *         to create a new one. A non-null object may help to reduce the
     *         number of object garbage-collected when rendering the axis.
     * @return A iterator to use for iterating through the graduation. This
     *         iterator may or may not be the <code>reuse</code> object.
     */
    public abstract TickIterator getTickIterator(RenderingHints hints, TickIterator reuse);

    /**
     * Adds a {@link PropertyChangeListener} to the listener list.
     * The listener is registered for all properties, such as "label"
     * and "locale".
     */
    public abstract void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a {@link PropertyChangeListener} from the listener list.
     */
    public abstract void removePropertyChangeListener(PropertyChangeListener listener);
}
