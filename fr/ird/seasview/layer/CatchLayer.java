/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le D�veloppement
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
package fr.ird.seasview.layer;

// Map components
import org.geotools.ct.TransformException;
import org.geotools.renderer.j2d.MarkIterator;
import org.geotools.renderer.j2d.RenderedMarks;
import org.geotools.renderer.j2d.GeoMouseEvent;

// Data bases
import java.sql.SQLException;
import fr.ird.animat.Species;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;

// Collections
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Collection;

// Geometry
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.AffineTransform;

// Graphics
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.RenderedImage;
import java.awt.font.GlyphVector;

// Miscellaneous
import org.geotools.units.Unit;
import java.text.NumberFormat;
import java.text.FieldPosition;
import javax.media.jai.util.Range;


/**
 * Base class for layers showing fishery positions. Default implementation show the
 * geographic extent of each catch (as returned by {@link EntryCatch#getShape}) using
 * the dominant species's color.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CatchLayer extends RenderedMarks {
    /**
     * Connection to the catch table.
     */
    private final CatchTable catchTable;

    /**
     * List of {@link CatchEntry} to display on this layer.
     * This list is changed each time {@link #setTimeRange}
     * is invoked.
     */
    private List<CatchEntry> catchs;

    /**
     * The icon used for each species. This is different than calling
     * {@link Species#getIcon}, since the later create a new icon on
     * every call. Here, we want to reuse the sames icons and keeps
     * modifications performed on them (e.g. changing the color).
     */
    private final Map<Species,Species.Icon> icons;

    /**
     * The display type for marks. May be one of the following constants:
     * {@link #POSITIONS_ONLY}, {@link #GEAR_COVERAGES} or {@link #CATCH_AMOUNTS}.
     */
    private int markType;

    /**
     * The mark type for displaying only catch positions. Catchs are drawn
     * using an ellipse or any other shape returned by {@link #getMarkShape}.
     */
    public static final int POSITIONS_ONLY = 0;

    /**
     * The mark type for displaying the geographic coverage of fishering gears.
     * For example, longlines may be displayed as straight or curved lines.
     */
    public static final int GEAR_COVERAGES = 1;

    /**
     * The mark type for a graph of catch amount by species. For
     * example, the graph may be a pie showing catch by species.
     */
    public static final int CATCH_AMOUNTS = 2;



    /////////////////////////////////////////////////////////////////////
    ////                                                             ////
    ////    Specific to GEAR_COVERAGES mark type                     ////
    ////    (except 'colors' which is shared with POSITIONS_ONLY)    ////
    ////                                                             ////
    /////////////////////////////////////////////////////////////////////
    /**
     * Colors to use for each catch in the {@link #catchs} list.
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
     * <code>true</code> if a {@link CatchEntry#getShape}
     * returned a non-null object for at least one entry.
     */
    private boolean hasShape;



    /////////////////////////////////////////////////////////////////////
    ////                                                             ////
    ////    Specific to CATCH_AMOUNTS mark type                      ////
    ////                                                             ////
    /////////////////////////////////////////////////////////////////////
    /**
     * Default width for {@link #circle}.
     */
    private static final float DEFAULT_WIDTH = 10f;

    /**
     * Forme g�om�trique repr�sentant un cercle complet. Cette forme est
     * utilis�e lorsqu'on doit tracer un graphique des captures par esp�ces
     * mais qu'il n'y a qu'une seule esp�ce d�clar�e dans une capture.
     */
    private Ellipse2D circle;

    /**
     * Typical "amplitude" for catchs. This is computed once for ever at
     * construction time, in order to avoid change in circle size each
     * time the time range is changed.
     */
    private final double typicalAmplitude;

    /**
     * Arc repr�sentant une portion d'un cercle. Cette forme g�om�trique est
     * utilis�e lorsqu'on a des captures de plusieurs esp�ces pour une m�me
     * p�che. Les coordonn�es de cet arc seront modifi� � chaque appel de la
     * m�thode {@link #getShape}.
     */
    private transient Arc2D arc;



    /////////////////////////////////////////////////////////////////////
    ////                                                             ////
    ////    For tooltip texts                                        ////
    ////                                                             ////
    /////////////////////////////////////////////////////////////////////
    /**
     * Format � utiliser pour �crire les valeurs des captures. Ce
     * format sera construit la premi�re fois o� il sera demand�.
     */
    private transient NumberFormat format;

    /**
     * Buffer temporaire � utiliser pour les �critures de nombres.
     */
    private transient StringBuffer buffer;

    /**
     * Cochonerie dont on n'a ch@&#�& rien � c�lisser.
     */
    private transient FieldPosition dummy;

    /**
     * Construct a new layer with the same {@link CatchTable} and
     * the same icons than the specified layer.
     *
     * @param  layer Layer to take data and icons from.
     * @throws SQLException If a SQL query failed.
     */
    public CatchLayer(final CatchLayer layer) throws SQLException {
        try {
            setCoordinateSystem(layer.catchTable.getCoordinateSystem());
        } catch (TransformException exception) {
            // PATCH: not really a SQL exception...
            final SQLException e = new SQLException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        this.catchTable       = layer.catchTable;
        this.catchs           = layer.catchs;
        this.colors           = layer.colors;
        this.icons            = layer.icons;
        this.markType         = layer.markType;
        this.typicalAmplitude = layer.typicalAmplitude;
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
    public CatchLayer(final CatchTable catchTable) throws SQLException {
        try {
            setCoordinateSystem(catchTable.getCoordinateSystem());
        } catch (TransformException exception) {
            // PATCH: not really a SQL exception...
            final SQLException e = new SQLException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        this.catchTable  = catchTable;
        this.catchs      = catchTable.getEntries();
        this.icons       = new HashMap<Species,Species.Icon>();
        markType         = CATCH_AMOUNTS; // Needed for "amplitude" calculations.
        typicalAmplitude = super.getTypicalAmplitude();
        markType         = POSITIONS_ONLY;
        validate();
    }

    /**
     * Query the underlying {@link CatchTable} for a new set of catchs to display.
     * This is a convenience method for {@link #setTimeRange(Date,Date)}.
     *
     * @param  timeRange the time range for catchs to display.
     * @throws SQLException If a SQL query failed.
     */
    public void setTimeRange(final Range timeRange) throws SQLException {
        setTimeRange((Date) timeRange.getMinValue(), (Date) timeRange.getMaxValue());
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
    public void setTimeRange(final Date startTime, final Date endTime) throws SQLException {
        synchronized (catchTable) {
            catchTable.setTimeRange(startTime, endTime);
            catchs = catchTable.getEntries();
        }
        validate();
        repaint();
    }

    /**
     * Validate {@link #colors} after new catch entries have been read.
     */
    private void validate() {
        hasShape = false;
        colors   = new Color[catchs.size()];
        useFill  = new boolean[colors.length];
        Rectangle2D geographicArea=null;
        for (int i=0; i<colors.length; i++) {
            final CatchEntry capture = catchs.get(i);
            final Shape        shape;
            switch (markType) {
                case CATCH_AMOUNTS : // fall through
                case POSITIONS_ONLY: shape=null;               break;
                case GEAR_COVERAGES: shape=capture.getShape(); break;
                default: throw new IllegalStateException();
            }
            final Species species = capture.getDominantSpecies();
            colors [i] = (species!=null) ? getIcon(species).getColor() : Color.black;
            useFill[i] = (shape==null);
            /*
             * Expand the bounding box by the
             * catch's geographic extent.
             */
            if (!useFill[i]) {
                hasShape = true;
                final Rectangle2D bounds = shape.getBounds2D();
                if (geographicArea==null) geographicArea=bounds;
                else geographicArea.add(bounds);
            } else {
                // The geographic extent for this catch is unknow.
                // Just expand the bounding box by the coordinate point.
                final Point2D point = capture.getCoordinate();
                if (point != null) {
                    if (geographicArea == null) {
                        geographicArea = new Rectangle2D.Double(point.getX(), point.getY(), 0, 0);
                    } else {
                        geographicArea.add(point);
                    }
                }
            }
        }
        setPreferredArea(geographicArea);
    }

    /**
     * Returns the icon for the specified species. This method keep trace
     * of created icon, in such a way that change to the icon's color are
     * saved.
     */
    private Species.Icon getIcon(final Species species) {
        synchronized (icons) {
            Species.Icon icon = icons.get(species);
            if (icon == null) {
                icon = species.getIcon();
                icons.put(species, icon);
            }
            return icon;
        }
    }

    /**
     * Returns a typical number for {@link #getAmplitude}.
     * This number is fixed at construction time only.
     */
    public double getTypicalAmplitude() {
        switch (markType) {
            case GEAR_COVERAGES: // fall through
            case POSITIONS_ONLY: return 1;
            case CATCH_AMOUNTS : return typicalAmplitude;
            default: throw new IllegalStateException();
        }
    }

    /**
     * Returns an iterator for iterating through catch marks.
     */
    public MarkIterator getMarkIterator() {
        return new Iterator();
    }

    /**
     * Define appareance for a set of species.
     *
     * @param icons A set of icons to use for species.
     */
    public void defineIcons(final Species.Icon[] icons) {
        for (int i=0; i<icons.length; i++) {
            final Species.Icon icon = icons[i];
            this.icons.put(icon.getSpecies(), icon);
        }
    }

    /**
     * Sets the mark type for catch positions. Mark may be only a circle showing
     * positions, a shape showing the gear coverage, or a plot showing catch amounts
     * by species.
     *
     * @param type One of the following constants:
     *             {@link #POSITIONS_ONLY},
     *             {@link #GEAR_COVERAGES} or
     *             {@link #CATCH_AMOUNTS}.
     */
    public void setMarkType(final int type) {
        if (type>=POSITIONS_ONLY && type<=CATCH_AMOUNTS) {
            if (markType != type) {
                this.markType = type;
                validate();
                repaint();
                listeners.firePropertyChange("markType", new Integer(markType), new Integer(type));
            }
        } else {
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    /**
     * Returns the mark type for catch positions.
     */
    public int getMarkType() {
        return markType;
    }

    /**
     * An iterator for iterating through catch data.
     */
    private final class Iterator extends MarkIterator {
        /**
         * The upper limit (exclusive) for {@link #index}.
         */
        private final int count;

        /**
         * The index of current mark.
         */
        int index = -1;

        /**
         * Construct an iterator.
         */
        public Iterator() {
            count = catchs.size();
        }
        
        /**
         * Returns the current iterator index.
         */
        public int getIteratorPosition() {
            return index;
        }
        
        /**
         * Moves the iterator to the specified index.
         */
        public void setIteratorPosition(final int n) {
            index = n;
        }
        
        /**
         * Moves the iterator a relative number of marks.
         */
        public boolean next() {
            index++;
            return index < count;
        }

        /**
         * Returns <code>true</code> if a catch is to be displayed.
         */
        public boolean visible() {
            switch (markType) {
                case CATCH_AMOUNTS : // fall through
                case POSITIONS_ONLY: return super.visible();
                case GEAR_COVERAGES: return !(hasShape && useFill[index]);
                default: throw new IllegalStateException();
            }
        }

        /**
         * Returns a catch "amplitude". If catchs are to be displayed as
         * a plot of the catch amount by species, then this method returns
         * a number proportional to the square root of catch amount. This
         * ensure that a plot with twice the catchs will appears as a circle
         * with twice the superficy.
         */
        public double amplitude() {
            switch (markType) {
                case GEAR_COVERAGES: // fall through
                case POSITIONS_ONLY: return super.amplitude();
                case CATCH_AMOUNTS : return Math.sqrt(catchs.get(index).getCatch());
                default: throw new IllegalStateException();
            }
        }

        /**
         * Returns a coordinates for a catch. If the catch cover a wide area (for
         * example a longline), the returned coordinate may be some middle point.
         */
        public Point2D position() {
            return catchs.get(index).getCoordinate();
        }

        /**
         * Returns the geographic extent for a catch, if there is one. If may be for example
         * a {@link Line2D} object where (x1,y1) and (x2,y2) are equal to starting and ending
         * points (in geographic coordinate) for a longline. If there is no known geographic
         * extent for the specified catch, or if the geographic extent is not to be displayed,
         * then this method returns <code>null</code>.
         */
        public Shape geographicArea() {
            switch (markType) {
                case CATCH_AMOUNTS : // fall through
                case POSITIONS_ONLY: return super.geographicArea();
                case GEAR_COVERAGES: return catchs.get(index).getShape();
                default: throw new IllegalStateException();
            }
        }

        /**
         * Returns a shape model for the mark, or <code>null</code> if there
         * is none. The shape use pixels coordinates and is centered at (0,0).
         *
         * @param  index The catch index.
         * @return A model for the mark. For efficienty raisons, this method
         *         may reuse and modify a previously returned mark model. You
         *         must copy the returned shape if you want to protect it from
         *         changes.
         */
        public Shape markShape() {
            switch (markType) {
                case GEAR_COVERAGES: // fall through
                case POSITIONS_ONLY: return super.markShape();
                case CATCH_AMOUNTS: {
                    final CatchEntry   capture = catchs.get(index);
                    final Set<Species> species = capture.getSpecies();
                    final int            count = species.size();
                    switch (count) {
                        case 0: return null;
                        case 1: {
                            if (circle == null) {
                                circle = new Ellipse2D.Float(-0.5f*DEFAULT_WIDTH,
                                                             -0.5f*DEFAULT_WIDTH,
                                                             DEFAULT_WIDTH, DEFAULT_WIDTH);
                            }
                            return circle;
                        }
                    }
                    /*
                     * At this stage, we know that we have to draw a plot
                     * of catch amount for at least two species.   Create
                     * necessary objects if they were not already created.
                     */
                    if (circle == null) {
                        circle = new Ellipse2D.Float(-0.5f*DEFAULT_WIDTH, -0.5f*DEFAULT_WIDTH, DEFAULT_WIDTH, DEFAULT_WIDTH);
                        arc    = new Arc2D.Float(circle.getBounds2D(), 0, 360, Arc2D.PIE);
                    }
                    final GeneralPath path = new GeneralPath();
                    final double scale = 360.0/capture.getCatch();
                    if (!(scale>0 && scale<Double.POSITIVE_INFINITY)) {
                        return null;
                    }
                    double angleStart = 0;
                    path.reset();
                    /*
                     * Construct the pie.
                     */
                    for (final java.util.Iterator<Species> it=species.iterator(); it.hasNext();) {
                        final double angleExtent = scale * capture.getCatch(it.next());
                        arc.setAngleStart (angleStart );
                        arc.setAngleExtent(angleExtent);
                        angleStart += angleExtent;
                        path.append(arc, false);
                    }
                    return path;
                }
                default: throw new IllegalStateException();
            }
        }

        /**
         * Draw a mark for a catch position.
         */
        protected void paint(final Graphics2D      graphics,
                             final Shape           geographicArea,
                             final Shape           markShape,
                             final RenderedImage   markIcon,
                             final AffineTransform iconXY,
                             final GlyphVector     label,
                             final Point2D.Float   labelXY)
        {
            switch (markType) {
                case POSITIONS_ONLY: // fall through
                case GEAR_COVERAGES: {
                    graphics.setColor(colors[index]);
                    if (useFill[index]) {
                        graphics.fill(markShape);
                        graphics.setColor(colors[index].darker());
                    }
                    graphics.draw(markShape);
                    break;
                }
                case CATCH_AMOUNTS: {
                    final CatchEntry   capture = catchs.get(index);
                    final Set<Species> species = capture.getSpecies();
                    final int            count = species.size();
                    if (count == 1) {
                        graphics.setColor(colors[index]);
                        graphics.fill(markShape);
                    } else {
                        final ShapeBroker broker = new ShapeBroker(markShape);
                        for (final java.util.Iterator<Species> it=species.iterator(); it.hasNext();) {
                            graphics.setColor(getIcon(it.next()).getColor());
                            graphics.fill(broker);
                            if (broker.finished()) {
                                break;
                            }
                        }
                    }
                    graphics.setColor(Color.black);
                    graphics.draw(markShape);
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        /**
         * Returns a string representation for the catch at the specified index.
         * This string will be displayed as a tool tip when the mouse cursor is
         * over the catch.
         */
        protected String getToolTipText(final GeoMouseEvent event) {
            if (format == null) {
                format = NumberFormat.getNumberInstance();
                buffer = new StringBuffer();
                dummy  = new FieldPosition(0);
            }
            buffer.setLength(0);
            final CatchEntry c=catchs.get(index);
            format.format(c.getCatch(), buffer, dummy);
            final Unit unit = c.getUnit();
            if (unit != null) {
                buffer.append(' ');
                buffer.append(unit);
            }
            return buffer.toString();
        }
    }
}
