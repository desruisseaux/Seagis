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
import java.util.Locale;


/**
 * Provides the mechanism for {@link Graduation} objects to return the
 * values and labels of their ticks one tick at a time. This interface
 * returns tick values from some minimal value up to some maximal value,
 * using some increment value. Note that the increment value <strong>may
 * not be constant</strong>. For example, a graduation for the time axis
 * may use a slightly variable increment between differents months, since
 * all months doesn't have the same number of days.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface TickIterator
{
    /**
     * Tests if the iterator has more ticks.
     */
    public abstract boolean hasNext();

    /**
     * Tests if the current tick is a major one.
     *
     * @return <code>true</code> if current tick is a major tick,
     *         or <code>false</code> if it is a minor tick.
     */
    public abstract boolean isMajorTick();

    /**
     * Returns the value for current tick. The
     * current tick may be major or minor.
     */
    public abstract double getValue();

    /**
     * Returns the label for current tick. This method is usually invoked
     * only for major ticks, but may be invoked for minor ticks as well.
     * This method returns <code>null</code> if it can't produces a label
     * for current tick.
     */
    public abstract String getLabel();

    /**
     * Moves the iterator to the next minor or major tick.
     */
    public abstract void next();

    /**
     * Moves the iterator to the next major tick. This move
     * ignore any minor ticks between current position and
     * the next major tick.
     */
    public abstract void nextMajor();

    /**
     * Reset the iterator on its first tick.
     * All other properties are left unchanged.
     */
    public abstract void rewind();

    /**
     * Returns the locale used for formatting tick labels.
     */
    public abstract Locale getLocale();
}
