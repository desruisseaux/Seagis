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
package fr.ird.main.catalog;

// Graphics and geometry
import java.awt.Font;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;

// Time
import java.util.Date;
import java.util.TimeZone;

// Collections
import java.util.Iterator;
import java.util.Collection;

// Ranges and plot
import net.seas.plot.RangeSet;
import net.seas.plot.RangeBars;

// Images database
import java.sql.SQLException;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.image.ImageDataBase;

// Dialog box
import net.seas.awt.ExceptionMonitor;
import net.seas.awt.CoordinateChooser;
import net.seas.awt.progress.Progress;
import net.seas.awt.progress.WindowProgress;

// Main framework
import fr.ird.main.Task;
import fr.ird.main.DataBase;
import fr.ird.main.InternalFrame;

// Miscellaneous
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Window displaying a graph of available data. The upper half
 * will display a chart of time.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class CatalogFrame extends InternalFrame
{
    /**
     * The image database.
     */
    private final ImageDataBase database;

    /**
     * The time ranges.
     */
    private final RangeBars times = new RangeBars(TimeZone.getDefault());

    /**
     * The coordinate chooser. Global time range and geographic
     * area can be narrowed using this coordinate chooser.
     */
//  private final CoordinateChooser chooser = new CoordinateChooser();

    /**
     * Construct a catalog frame.
     *
     * @param  database The database to connect to.
     * @param  owner Composante propriétaire (pour l'affichage d'une boîte des progrès).
     * @throws SQLException If access to the database failed.
     */
    public CatalogFrame(final DataBase database, final Component owner) throws SQLException
    {
        super(Resources.format(ResourceKeys.IMAGES_CATALOG));
        this.database = database.getImageDataBase();
        times.setFont(new Font("SansSerif", Font.BOLD, 12));
        times.setLegendVisible(true);
        final Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        pane.add(times,   BorderLayout.NORTH);
//      pane.add(chooser, BorderLayout.WEST);
        update(owner);
    }

    /**
     * Update times ranges for each series.
     *
     * @param  owner Composante propriétaire (pour l'affichage d'une boîte des progrès).
     * @throws SQLException If access to the database failed.
     */
    private void update(final Component owner)
    {
        final WindowProgress progress = new WindowProgress(owner);
        progress.setTitle(Resources.format(ResourceKeys.LOOKING_INTO_DATABASE));
        progress.started();
        try
        {
//          final Date     startTime = chooser.getStartTime();
//          final Date       endTime = chooser.getEndTime();
//          final Rectangle2D   area = chooser.getGeographicArea();
            final SeriesTable series = database.getSeriesTable();
            final Collection<SeriesEntry> list = series.getSeries();
            final float factor = 100f/list.size();
            int index = 0;
            for (final Iterator<SeriesEntry> it=list.iterator(); it.hasNext();)
            {
                final SeriesEntry entry = it.next();
                final String name = entry.getName();
                progress.setDescription(name);
                progress.progress(factor*index++);
                final ImageTable   images = database.getImageTable(entry);
                final RangeSet timeRanges = new RangeSet(Date.class);
                images.getRanges(null, null, timeRanges);
                images.close();
                if (!timeRanges.isEmpty())
                {
                    times.setRanges(name, timeRanges);
                }
            }
            progress.complete();
            series.close();
            progress.dispose();
        }
        catch (SQLException exception)
        {
            progress.exceptionOccurred(exception);
            progress.complete();
        }
    }

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des éventuelles
     * bases de données accédées par cette fenêtre.
     */
    protected void setTimeZone(final TimeZone timezone)
    {times.setTimeZone(timezone);}
}
