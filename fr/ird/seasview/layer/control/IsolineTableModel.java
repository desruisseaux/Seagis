/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.seasview.layer.control;

// User interface
import java.awt.Color;
import javax.swing.table.AbstractTableModel;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

// Geotools
import org.geotools.resources.XArray;

// Resources
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * The table model for isoline. The first column contains
 * all available values in increasing order. The second
 * columns contains the visible/invisible state, and the
 * last one contains the color.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class IsolineTableModel extends AbstractTableModel {
    /**
     * Column index for values.
     */
    private static final int VALUE = 0;

    /**
     * Column index for visible states.
     */
    private static final int VISIBLE = 1;

    /**
     * Column index for colors.
     */
    private static final int COLOR = 2;

    /**
     * A row in the table.
     */
    private static final class Entry implements Comparable, Cloneable {
        /** The isoline value. */ public float   value;
        /** The visible state. */ public boolean visible;
        /** The isoline color. */ public Color   color = Color.YELLOW;

        /**
         * Compares this entry with the specified entry for order.
         */
        public int compareTo(final Object other) {
            return Float.compare(value, ((Entry)other).value);
        }

        /**
         * Returns a clone of this entry.
         */
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException exception) {
                // Should not happen, since we are cloneable.
                throw new AssertionError(exception);
            }
        }
    }

    /**
     * The available values, in increasing order.
     * TODO: Can't be generic for now because of
     *       a runtime bug with code generated by
     *       the generic compiler.
     */
    private final List entries = new ArrayList();

    /**
     * <code>true</code> if the value sign should be inversed.
     * This is useful for a table of depths instead of bathymetry.
     */
    private final boolean inverse;

    /**
     * Construct an initially empty table model.
     * Isoline can be added using the {@link #add} method.
     *
     * @param inverse True if the value sign should be inversed.
     *        This is useful for a table of depth instead of bathymetry.
     */
    public IsolineTableModel(final boolean inverse) {
        this.inverse = inverse;
    }

    /**
     * Add an array of values to this table.
     * Values already present will be ignored.
     */
    public void add(final float[] values) {
        Entry entry = null;
        for (int i=0; i<values.length; i++) {
            if (entry == null) {
                entry = new Entry();
            }
            entry.value = values[i];
            if (inverse) {
                entry.value = -entry.value;
            }
            final int insertAt = Collections.binarySearch(entries, entry);
            if (insertAt < 0) {
                // The entry is not already present.
                // Add it at the insertion point.
                entries.add(~insertAt, entry);
                entry = null;
            }
        }
    }

    /**
     * Remove an array of values from this table.
     */
    public void remove(final float[] values) {
        final Entry entry = new Entry();
        for (int i=0; i<values.length; i++) {
            entry.value = values[i];
            if (inverse) {
                entry.value = -entry.value;
            }
            final int removeAt = Collections.binarySearch(entries, entry);
            if (removeAt >= 0) {
                entries.remove(removeAt);
            }
        }
    }

    /**
     * Returns an array of values with the
     * visible state set to <code>true</code>.
     */
    public float[] getSelectedValues() {
        int count = 0;
        final float[] values = new float[entries.size()];
        for (int i=0; i<values.length; i++) {
            final Entry entry = (Entry) entries.get(i);
            if (entry.visible) {
                values[count] = entry.value;
                if (inverse) values[count] = -values[count];
                count++;
            }
        }
        return XArray.resize(values, count);
    }

    /**
     * Define the set of selected values. All
     * other values will be left unselected.
     */
    public void setSelectedValues(float[] values) {
        values = (float[]) values.clone();
        Arrays.sort(values);
        for (final Iterator it=entries.iterator(); it.hasNext();) {
            final Entry entry = (Entry) it.next();
            entry.visible = (Arrays.binarySearch(values, entry.value) >= 0);
            if (!entry.visible && entry.value==0) {
                // Special case for 0, since -0 != +0 for binarySearch.
                entry.visible = (Arrays.binarySearch(values, -entry.value) >= 0);
            }
        }
    }

    /**
     * Returns the name of the column
     * at <code>column</code> index.
     */
    public String getColumnName(final int column) {
        int key;
        switch (column) {
            case VALUE:   key = inverse ? ResourceKeys.DEPTH : ResourceKeys.ALTITUDE; break;
            case VISIBLE: key = ResourceKeys.VISIBLE;  break;
            case COLOR:   key = ResourceKeys.COLOR;    break;
            default:      return null;
        }
        return Resources.format(key);
    }

    /**
     * Returns the most specific superclass
     * for all the cell values in the column.
     */
    public Class getColumnClass(final int column) {
        switch (column) {
            case VALUE:   return Float.class;
            case VISIBLE: return Boolean.class;
            case COLOR:   return Color.class;
            default:       return null;
        }
    }

    /**
     * Returns the number of columns in the model.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
    public int getColumnCount() {
        return 3;
    }

    /**
     * Returns the number of rows in the model.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    public int getRowCount() {
        return entries.size();
    }

    /**
     * Returns the value for the cell at <code>column</code>
     * and <code>row</code>.
     *
     * @param  row    the row whose value is to be queried
     * @param  column the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    public Object getValueAt(final int row, final int column) {
        final Entry entry = (Entry) entries.get(row);
        switch (column) {
            case VALUE:   return new Float(entry.value);
            case VISIBLE: return entry.visible ? Boolean.TRUE : Boolean.FALSE;
            case COLOR:   return entry.color;
            default:      return null;
        }
    }

    /**
     * Sets the value in the cell at <code>column</code>
     * and <code>row</code> index to <code>value</code>.
     */
    public void setValueAt(final Object value, final int row, final int column) {
        final Entry entry = (Entry) entries.get(row);
        switch (column) {
            case VISIBLE: entry.visible = ((Boolean)value).booleanValue(); break;
        }
    }

    /**
     * Returns <code>true</code> if the cell at
     * <code>row</code> and <code>column</code> is editable.
     */
    public boolean isCellEditable(final int row, final int column) {
        switch (column) {
            default:      return false;
            case VISIBLE: return true;
            case COLOR:   return true;
        }
    }

    /**
     * Returns the current table content as an opaque object.
     * This object can be specified to {@link #reset} in order
     * to restore the table content to the its state before
     * <code>mark()</code> was invoked. This method is used
     * for undoing or redoing action.
     */
    final Object mark() {
        final Entry[] array = (Entry[]) entries.toArray(new Entry[entries.size()]);
        for (int i=0; i<array.length; i++) {
            array[i] = (Entry) array[i].clone();
        }
        return array;
    }

    /**
     * Restore the table content to the state at the time
     * {@link #mark} was invoked. This method is used for
     * undoing or redoing action.
     *
     * @param mark The opaque object returned by {@link #mark}.
     */
    final void reset(final Object mark) {
        final Entry[] list = (Entry[]) mark;
        entries.clear();
        for (int i=0; i<list.length; i++) {
            entries.add(list[i]);
        }
    }
}
