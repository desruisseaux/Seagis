/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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
package fr.ird.layer;

// Map components
import net.seas.map.layer.MarkLayer;

// Data bases
import java.sql.SQLException;
import fr.ird.animat.Species;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;

// Collections
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.HashMap;

// Geometry
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

// Graphics
import java.awt.Color;
import java.awt.Graphics2D;
import javax.media.jai.GraphicsJAI;


/**
 * Base class for layers showing fishery positions. Default implementation show the
 * geographic extent of each catch (as returned by {@link EntryCatch#getShape}) using
 * the dominant species's color.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class CatchPositionLayer extends MarkLayer
{
    /**
     * Connection to the catch table.
     */
    private final CatchTable catchTable;

    /**
     * List of {@link CatchEntry} to display on this layer.
     * This list is changed each time {@link #setTimeRange}
     * is invoked.
     */
    protected List<CatchEntry> catchs;

    /**
     * The icon used for each species. This is different than calling
     * {@link Species#getIcon}, since the later create a new icon on
     * every call. Here, we want to reuse the sames icons and keeps
     * modifications performed on them (e.g. changing the color).
     */
    private final Map<Species,Species.Icon> icons;

    /**
     * Colors to use for each catch if the {@link #catchs} list.
     * This is usually the color for the dominant species.
     */
    private Color[] colors;

    /**
     * Tell if {@link #paint} should use {@link Graphics2D#fill}
     * for a particular catch. If <code>false</code>, then only
     * {@link Graphics2D#draw} will be used.
     */
    private boolean[] useFill;

    /**
     * Construct a new layer with the same {@link CatchTable} and
     * the same icons than the specified layer.
     *
     * @param  layer Layer to take data and icons from.
     * @throws SQLException If a SQL query failed.
     */
    public CatchPositionLayer(final CatchPositionLayer layer) throws SQLException
    {
        super(layer.catchTable.getCoordinateSystem());
        this.catchTable = layer.catchTable;
        this.catchs     = layer.catchs;
        this.icons      = layer.icons;
        this.colors     = layer.colors;
    }

    /**
     * Construct a new layer for the specified catch table. This constructor
     * query <code>catchTable</code> for all catchs in its current time range.
     * <code>catchTable</code> will be queried again each time the time range
     * is changed through {@link #setTimeRange}.
     *
     * @param  catchTable Connection to a table containing catchs to display.
     * @throws SQLException If a SQL query failed.
     */
    public CatchPositionLayer(final CatchTable catchTable) throws SQLException
    {
        super(catchTable.getCoordinateSystem());
        this.catchTable = catchTable;
        this.catchs     = catchTable.getEntries();
        this.icons      = new HashMap<Species,Species.Icon>();
        validate();
    }

    /**
     * Query the underlying {@link CatchTable} for a new set of catchs to display.
     * This method first invokes {@link FisheryTable#setTimeRange} with the specified
     * time range, and then query for all catchs in this time range.
     *
     * @param  startTime Time of the first catch to display.
     * @param  startTime Time of the end catch to display.
     * @throws SQLException If a SQL query failed.
     */
    public void setTimeRange(final Date startTime, final Date endTime) throws SQLException
    {
        synchronized (catchTable)
        {
            catchTable.setTimeRange(startTime, endTime);
            catchs = catchTable.getEntries();
        }
        validate();
    }

    /**
     * Validate {@link #colors} after new
     * catch entries have been read.
     */
    private synchronized void validate()
    {
        colors  = new Color[catchs.size()];
        useFill = new boolean[colors.length];
        for (int i=0; i<colors.length; i++)
        {
            colors [i] = getIcon(catchs.get(i).getDominantSpecies()).getColor();
        }
    }

    /**
     * Returns the number of catch positions to display.
     */
    public int getCount()
    {return catchs.size();}

    /**
     * Returns a coordinates for a catch. If the catch cover a wide area (for
     * example a longline), the returned coordinate may be some middle point.
     */
    public Point2D getPosition(final int index)
    {return catchs.get(index).getCoordinate();}

    /**
     * Returns the geographic extent for a catch, if there is one. If may be for example
     * a {@link Line2D} object where (x1,y1) and (x2,y2) are equal to starting and ending
     * points (in geographic coordinate) for a longline. If there is no known geographic
     * extent for the specified catch, then this method returns <code>null</code>.
     */
    public Shape getGeographicShape(int index)
    {
        final Shape shape = catchs.get(index).getShape();
        useFill[index] = (shape==null);
        return shape;
    }

    /**
     * Draw a mark for a catch position.
     *
     * @param graphics The destination graphics.
     * @param shape    The shape to draw, in pixel coordinates.
     * @param index    The index of the catch to draw.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index)
    {
        graphics.setColor(colors[index]);
        if (useFill[index])
        {
            graphics.fill(shape);
            graphics.setColor(colors[index].darker());
        }
        graphics.draw(shape);
    }

    /**
     * Returns the icon for the specified species. This method keep trace
     * of created icon, in such a way that change to the icon's color are
     * saved.
     */
    private Species.Icon getIcon(final Species species)
    {
        synchronized (icons)
        {
            Species.Icon icon = icons.get(species);
            if (icon==null)
            {
                icon = species.getIcon();
                icons.put(species, icon);
            }
            return icon;
        }
    }
}
