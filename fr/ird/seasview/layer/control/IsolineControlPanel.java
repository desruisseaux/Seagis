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

// User interface (Swing)
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;

// User interface (AWT)
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;

// Resources
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;

// Geotools dependencies
import org.geotools.resources.SwingUtilities;


/**
 * Control panel for selecting isolines.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class IsolineControlPanel extends JPanel
{
    /**
     * The table model for available depths.
     */
    private final IsolineTableModel table = new IsolineTableModel(true);

    /**
     * Construct an initially empty control panel.
     * Isolines can with the {@link #addIsoline}
     * method.
     */
    public IsolineControlPanel()
    {
        super(new BorderLayout());
        final JTable       tableView = new JTable(table);
        final JScrollPane scrollPane = new JScrollPane(tableView);
        scrollPane.setPreferredSize(new Dimension(300,300));
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createEmptyBorder(/*top*/6, /*left*/6, /*bottom*/6, /*right*/6),
                             scrollPane.getBorder()));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Add an array of values to the table.
     * Values already present will be ignored.
     */
    public void addValues(final float[] values)
    {table.add(values);}

    /**
     * Remove an array of values from the table.
     */
    public void removeValues(final float[] values)
    {table.remove(values);}

    /**
     * Returns an array of values with the
     * visible state set to <code>true</code>.
     */
    public float[] getSelectedValues()
    {return table.getSelectedValues();}

    /**
     * Define the set of selected values. All
     * other values will be left unselected.
     */
    public void setSelectedValues(final float[] values)
    {table.setSelectedValues(values);}

    /**
     * Returns the current table content as an opaque object.
     * This object can be specified to {@link #reset} in order
     * to restore the table content to the its state before
     * <code>mark()</code> was invoked. This method is used
     * for undoing or redoing action.
     */
    final Object mark()
    {return table.mark();}

    /**
     * Restore the table content to the state at the time
     * {@link #mark} was invoked. This method is used for
     * undoing or redoing action.
     *
     * @param mark The opaque object returned by {@link #mark}.
     */
    final void reset(final Object mark)
    {table.reset(mark);}

    /**
     * Show the dialog box. If the user clicked on "Ok"
     * then this method returns <code>true</code>.
     */
    public boolean showDialog(final Component owner)
    {
        final Object mark = mark();
        if (SwingUtilities.showOptionDialog(owner, this, Resources.format(ResourceKeys.BATHYMETRY)))
        {
            return true;
        }
        reset(mark);
        return false;
    }
}
