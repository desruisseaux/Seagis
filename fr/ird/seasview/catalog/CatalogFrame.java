/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 */
package fr.ird.seasview.catalog;

// Graphics, geometry and AWT
import java.awt.Font;
import java.awt.Component;
import java.awt.Container;
import java.awt.geom.Rectangle2D;

// User interface
import javax.swing.JTree;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreeModel;

// Utilities
import java.util.Date;
import java.util.TimeZone;
import java.util.Collection;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Geotools
import org.geotools.util.RangeSet;
import org.geotools.util.ProgressListener;
import org.geotools.gui.swing.ProgressWindow;
import org.geotools.gui.swing.ExceptionMonitor;

// Seagis
import fr.ird.awt.RangeBars;
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageRanges;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.seasview.Task;
import fr.ird.seasview.DataBase;
import fr.ird.seasview.InternalFrame;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Window displaying a graph of available data. The upper half will display a chart of time.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CatalogFrame extends InternalFrame {
    /**
     * The image database.
     */
    private final CoverageDataBase database;

    /**
     * The time ranges.
     */
    private final RangeBars times = new RangeBars(TimeZone.getDefault(),
                                                  RangeBars.HORIZONTAL);

    /**
     * The split pane.
     */
    private final JSplitPane pane;

    /**
     * The coordinate chooser. Global time range and geographic
     * area can be narrowed using this coordinate chooser.
     */
//  private final CoordinateChooser chooser = new CoordinateChooser();

    /**
     * The tree of all series, format and their bands.
     */
    private final TreeModel tree;

    /**
     * Construct a catalog frame.
     *
     * @param  databases The database to connect to.
     * @param  owner Composante propri�taire (pour l'affichage d'une bo�te des progr�s).
     * @throws SQLException If access to the database failed.
     */
    public CatalogFrame(final DataBase databases, final Component owner) throws RemoteException {
        super(Resources.format(ResourceKeys.IMAGES_CATALOG));
        final SeriesTable series;
        
        database = databases.getCoverageDataBase();
        series   = database.getSeriesTable();
        tree     = series.getTree(SeriesTable.CATEGORY_LEAF);
        series.close();
        times.setFont(new Font("SansSerif", Font.BOLD, 12));

        final JTree        treeView = new JTree(tree);
        final JComponent scrollView = new JScrollPane(treeView);
        scrollView.setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createEmptyBorder(/*top*/12, /*left*/12, /*bottom*/12, /*right*/12),
                             scrollView.getBorder()));
        pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, times, scrollView);
        pane.setDividerLocation(330);
        getContentPane().add(pane);
        update(owner);
    }

    /**
     * Update times ranges for each series.
     *
     * @param  owner Composante propri�taire (pour l'affichage d'une bo�te des progr�s).
     * @throws SQLException If access to the database failed.
     */
    private void update(final Component owner) {
        final ProgressWindow progress = new ProgressWindow(owner);
        progress.setTitle(Resources.format(ResourceKeys.LOOKING_INTO_DATABASE));
        progress.started();
        try {
//          final Date     startTime = chooser.getStartTime();
//          final Date       endTime = chooser.getEndTime();
//          final Rectangle2D   area = chooser.getGeographicArea();
            final SeriesTable series = database.getSeriesTable();
            final Collection<SeriesEntry> list = series.getEntries();
            final float factor = 100f/list.size();
            int index = 0;
            for (final SeriesEntry entry : list) {
                final String name = entry.getName();
                progress.setDescription(name);
                progress.progress(factor*index++);
                final CoverageTable images = database.getCoverageTable(entry);                
                final CoverageRanges gcRange = images.getRanges(false, false, true, false);
                final RangeSet  timeRanges = gcRange.t;
                images.close();
                if (!timeRanges.isEmpty()) {
                    times.setRanges(name, timeRanges);
                }
            }
            progress.complete();
            series.close();
            progress.dispose();
        } catch (RemoteException exception) {
            progress.exceptionOccurred(exception);
            progress.complete();
        }
        times.invalidate();
    }

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des �ventuelles
     * bases de donn�es acc�d�es par cette fen�tre.
     */
    protected void setTimeZone(final TimeZone timezone) {
        times.setTimeZone(timezone);
    }

    /**
     * Sp�cifie si le retra�age de la s�paration doit doit �tre continu.
     */
    protected void setPaintingWhileAdjusting(final boolean s) {
        pane.setContinuousLayout(s);
    }
}
