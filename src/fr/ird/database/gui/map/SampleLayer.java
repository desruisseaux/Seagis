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
package fr.ird.database.gui.map;

// Collections
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.RandomAccess;

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
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.rmi.RemoteException;

// Geotools
import org.geotools.units.Unit;
import org.geotools.resources.Utilities;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.renderer.j2d.MarkIterator;
import org.geotools.renderer.j2d.RenderedMarks;
import org.geotools.renderer.j2d.GeoMouseEvent;

// Seagis
import fr.ird.animat.Species;
import fr.ird.database.sample.SampleEntry;


/**
 * Base class for layers showing sample positions. Default implementation show the
 * geographic extent of each sample (as returned by {@link SampleEntry#getShape}) using
 * the dominant species's color.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleLayer extends RenderedMarks {
    /**
     * List of {@link SampleEntry} to display on this layer.
     */
    private List<SampleEntry> samples;

    /**
     * The icon used for each species. This is different than calling
     * {@link Species#getIcon}, since the later create a new icon on
     * every call. Here, we want to reuse the sames icons and keeps
     * modifications performed on them (e.g. changing the color).
     */
    private final Map<Species,Species.Icon> icons;

    /**
     * The display type for marks. May be one of the following constants:
     * {@link #SAMPLE_POSITIONS}, {@link #SAMPLE_COVERAGES} or {@link #SAMPLE_VALUES}.
     */
    private int markType = SAMPLE_POSITIONS;

    /**
     * The mark type for displaying only catch positions. Samples are drawn
     * using an ellipse or any other shape returned by {@link #getMarkShape}.
     */
    public static final int SAMPLE_POSITIONS = 0;

    /**
     * The mark type for displaying the geographic coverage of fishering gears.
     * For example, longlines may be displayed as straight or curved lines.
     */
    public static final int SAMPLE_COVERAGES = 1;

    /**
     * The mark type for a graph of catch amount by species. For
     * example, the graph may be a pie showing catch by species.
     */
    public static final int SAMPLE_VALUES = 2;



    /////////////////////////////////////////////////////////////////////
    ////                                                             ////
    ////    Specific to SAMPLE_COVERAGES mark type                   ////
    ////    (except 'colors' which is shared with SAMPLE_POSITIONS)  ////
    ////                                                             ////
    /////////////////////////////////////////////////////////////////////
    /**
     * Couleur des coups nuls.
     */
    private Color nullColor = Color.WHITE.darker();

    /**
     * Colors to use for each sample in the {@link #samples} list.
     * This is usually the color for the dominant species.
     */
    private Color[] colors;

    /**
     * Tell if {@link #paint} should use {@link Graphics2D#fill}
     * for a particular sample. If <code>false</code>, then only
     * {@link Graphics2D#draw} will be used.
     */
    private boolean[] useFill;

    /**
     * <code>true</code> if a {@link SampleEntry#getShape}
     * returned a non-null object for at least one entry.
     */
    private boolean hasShape;



    /////////////////////////////////////////////////////////////////////
    ////                                                             ////
    ////    Specific to SAMPLE_VALUES mark type                      ////
    ////                                                             ////
    /////////////////////////////////////////////////////////////////////
    /**
     * Default width for {@link #circle}.
     */
    private static final float DEFAULT_WIDTH = 10f;

    /**
     * Forme géométrique représentant un cercle complet. Cette forme est
     * utilisée lorsqu'on doit tracer un graphique des échantillons par espèces
     * mais qu'il n'y a qu'une seule espèce déclarée dans un échantillon.
     */
    private Ellipse2D circle;

    /**
     * Typical "amplitude" for samples. This is usually computed once for ever at
     * construction time, in order to avoid change in circle size each time the
     * time range is changed.
     */
    private double typicalAmplitude = 1;

    /**
     * Arc représentant une portion d'un cercle. Cette forme géométrique est
     * utilisée lorsqu'on a des échantillons de plusieurs espèces pour une même
     * pêche. Les coordonnées de cet arc seront modifié à chaque appel de la
     * méthode {@link #getShape}.
     */
    private transient Arc2D arc;



    /////////////////////////////////////////////////////////////////////
    ////                                                             ////
    ////    For tooltip texts                                        ////
    ////                                                             ////
    /////////////////////////////////////////////////////////////////////
    /**
     * Format à utiliser pour écrire les valeurs des échantillons. Ce
     * format sera construit la première fois où il sera demandé.
     */
    private transient NumberFormat format;

    /**
     * Buffer temporaire à utiliser pour les écritures de nombres.
     */
    private transient StringBuffer buffer;

    /**
     * Cochonerie dont on n'a ch@&#²& rien à câlisser.
     */
    private transient FieldPosition dummy;

    /**
     * Construct an initially empty layer.
     */
    public SampleLayer() {
        this.samples = (List)Collections.EMPTY_LIST;
        this.icons   = new HashMap<Species,Species.Icon>();
    }

    /**
     * Construct a new layer with the same data and icons than the specified layer.
     *
     * @param  layer Layer to take data and icons from.
     */
    public SampleLayer(final SampleLayer layer) {
        this(layer, layer.getCoordinateSystem());
    }

    /**
     * Construct a new layer with the same data and icons than the specified layer.
     *
     * @param  layer Layer to take data and icons from.
     * @param  cs The layer coordinate system.
     */
    public SampleLayer(final SampleLayer layer, final CoordinateSystem cs) {
        try {
            setCoordinateSystem(cs);
        } catch (TransformException exception) {
            // Should not happen, since we don't have any data yet.
            IllegalStateException e = new IllegalStateException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        this.samples          = layer.samples;
        this.colors           = layer.colors;
        this.icons            = layer.icons;
        this.markType         = layer.markType;
        this.typicalAmplitude = layer.typicalAmplitude;
    }

    /**
     * Compute the typical amplitude. This is usually performed once at construction time,
     * before the list of entries is changed.
     */
    final void updateTypicalAmplitude() {
        final int oldMarkType = markType;
        markType = SAMPLE_VALUES; // Needed for "amplitude" calculations.
        try {
            typicalAmplitude = super.getTypicalAmplitude();
        } finally {
            markType = oldMarkType;
        }
        validate();
    }

    /**
     * Set the samples to renderer.
     *
     * @param samples The samples, or <code>null</code> if none.
     */
    public void setSamples(final Collection<SampleEntry> samples) {
        if ((samples instanceof List<SampleEntry>) && (samples instanceof RandomAccess)) {
            this.samples = (List<SampleEntry>) samples;
        } else if (samples != null) {
            this.samples = new ArrayList<SampleEntry>(samples);
        } else {
            this.samples = (List)Collections.EMPTY_LIST;
        }
        invalidate();
        validate();
        repaint();
        assert colors.length == samples.size();
        assert colors.length == useFill.length;
    }

    /**
     * Validate {@link #colors}, {@link #useFill} and {@link #hasShape}
     * after new sample entries have been specified.
     */
    private void validate() {
        hasShape = false;
        colors   = new Color[samples.size()];
        useFill  = new boolean[colors.length];
        Rectangle2D geographicArea = null;
        for (int i=0; i<colors.length; i++) {
            final SampleEntry sample = samples.get(i);
            final Shape shape;
            switch (markType) {
                case SAMPLE_VALUES    : // fall through
                case SAMPLE_POSITIONS : shape=null;              break;
                case SAMPLE_COVERAGES : shape=sample.getShape(); break;
                default: throw new IllegalStateException();
            }
            final Species species = sample.getDominantSpecies();
            colors [i] = (species!=null) ? getIcon(species).getColor() : nullColor;
            useFill[i] = (shape==null);
            /*
             * Expand the bounding box by the sample's geographic extent.
             */
            if (!useFill[i]) {
                hasShape = true;
                final Rectangle2D bounds = shape.getBounds2D();
                if (geographicArea == null) {
                    geographicArea = bounds;
                } else {
                    geographicArea.add(bounds);
                }
            } else {
                // The geographic extent for this sample is unknow.
                // Just expand the bounding box by the coordinate point.
                final Point2D point = sample.getCoordinate();
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
                try {
                    icon = species.getIcon();
                } catch (RemoteException exception) {
                    Utilities.unexpectedException("fr.ird.animat", "Species", "getIcon", exception);
                    icon = new fr.ird.animat.server.Species("(erreur)", nullColor).getIcon();
                }
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
            case SAMPLE_COVERAGES : // fall through
            case SAMPLE_POSITIONS : return 1;
            case SAMPLE_VALUES    : return typicalAmplitude;
            default: throw new IllegalStateException();
        }
    }

    /**
     * Returns an iterator for iterating through sample marks.
     */
    public MarkIterator getMarkIterator() {
        return new Iterator();
    }

    /**
     * Change la couleur utilisée pour représenter une espèce.
     *
     * @param species L'espèce pour laquelle définir les couleurs, ou <code>null</code>
     *        pour définir la couleur des coups nuls.
     * @param color La nouvelle couleur pour l'espèce spécifiée.
     */
    public void setColor(final Species species, final Color color) {
        if (species != null) {
            getIcon(species).setColor(color);
        } else {
            nullColor = color;
        }
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
     * Sets the mark type for sample positions. Mark may be only a circle showing
     * positions, a shape showing the gear coverage, or a plot showing sample amounts
     * by species.
     *
     * @param type One of the following constants:
     *             {@link #SAMPLE_POSITIONS},
     *             {@link #SAMPLE_COVERAGES} or
     *             {@link #SAMPLE_VALUES}.
     */
    public void setMarkType(final int type) {
        if (type>=SAMPLE_POSITIONS && type<=SAMPLE_VALUES) {
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
     * Returns the mark type for sample positions.
     */
    public int getMarkType() {
        return markType;
    }

    /**
     * An iterator for iterating through sample data.
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
            count = samples.size();
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
            assert count == samples.size();
            assert n>=-1 && n<count : n;
            index = n;
        }
        
        /**
         * Moves the iterator a relative number of marks.
         */
        public boolean next() {
            assert count == samples.size();
            return ++index < count;
        }

        /**
         * Returns <code>true</code> if a sample is to be displayed.
         */
        public boolean visible() {
            switch (markType) {
                case SAMPLE_VALUES    : // fall through
                case SAMPLE_POSITIONS : return super.visible();
                case SAMPLE_COVERAGES : return !(hasShape && useFill[index]);
                default: throw new IllegalStateException();
            }
        }

        /**
         * Returns a sample "amplitude". If samples are to be displayed as
         * a plot of the sample amount by species, then this method returns
         * a number proportional to the square root of sample amount. This
         * ensure that a plot with twice the samples will appears as a circle
         * with twice the superficy.
         */
        public double amplitude() {
            switch (markType) {
                case SAMPLE_COVERAGES : // fall through
                case SAMPLE_POSITIONS : return super.amplitude();
                case SAMPLE_VALUES    : return Math.sqrt(samples.get(index).getValue());
                default: throw new IllegalStateException();
            }
        }

        /**
         * Returns a coordinates for a sample. If the sample cover a wide area (for
         * example a longline), the returned coordinate may be some middle point.
         */
        public Point2D position() {
            return samples.get(index).getCoordinate();
        }

        /**
         * Returns the geographic extent for a sample, if there is one. If may be for example
         * a {@link Line2D} object where (x1,y1) and (x2,y2) are equal to starting and ending
         * points (in geographic coordinate) for a longline. If there is no known geographic
         * extent for the specified sample, or if the geographic extent is not to be displayed,
         * then this method returns <code>null</code>.
         */
        public Shape geographicArea() {
            switch (markType) {
                case SAMPLE_VALUES    : // fall through
                case SAMPLE_POSITIONS : return super.geographicArea();
                case SAMPLE_COVERAGES : return samples.get(index).getShape();
                default: throw new IllegalStateException();
            }
        }

        /**
         * Returns a shape model for the mark, or <code>null</code> if there
         * is none. The shape use pixels coordinates and is centered at (0,0).
         *
         * @param  index The sample index.
         * @return A model for the mark. For efficienty raisons, this method
         *         may reuse and modify a previously returned mark model. You
         *         must copy the returned shape if you want to protect it from
         *         changes.
         */
        public Shape markShape() {
            switch (markType) {
                case SAMPLE_COVERAGES: // fall through
                case SAMPLE_POSITIONS: return super.markShape();
                case SAMPLE_VALUES: {
                    final SampleEntry   sample = samples.get(index);
                    final Set<Species> species = sample.getSpecies();
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
                     * of sample amount for at least two species.  Create
                     * necessary objects if they were not already created.
                     */
                    if (circle == null) {
                        circle = new Ellipse2D.Float(-0.5f*DEFAULT_WIDTH, -0.5f*DEFAULT_WIDTH, DEFAULT_WIDTH, DEFAULT_WIDTH);
                        arc    = new Arc2D.Float(circle.getBounds2D(), 0, 360, Arc2D.PIE);
                    }
                    final GeneralPath path = new GeneralPath();
                    final double scale = 360.0/sample.getValue();
                    if (!(scale>0 && scale<Double.POSITIVE_INFINITY)) {
                        return null;
                    }
                    double angleStart = 0;
                    path.reset();
                    /*
                     * Construct the pie.
                     */
                    for (final Species sp : species) {
                        final double angleExtent = scale * sample.getValue(sp);
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
         * Draw a mark for a sample position.
         */
        protected void paint(final Graphics2D      graphics,
                             final Shape           geographicArea,
                             final Shape           markShape,
                             final RenderedImage   markIcon,
                             final AffineTransform iconXY,
                             final GlyphVector     label,
                             final Point2D.Float   labelXY)
        {
            assert count == samples.size();
            assert index < count : index;
            switch (markType) {
                case SAMPLE_POSITIONS: // fall through
                case SAMPLE_COVERAGES: {
                    graphics.setColor(colors[index]);
                    if (useFill[index]) {
                        graphics.fill(markShape);
                        graphics.setColor(colors[index].darker());
                    }
                    graphics.draw(markShape);
                    break;
                }
                case SAMPLE_VALUES: {
                    final SampleEntry   sample = samples.get(index);
                    final Set<Species> species = sample.getSpecies();
                    final int            count = species.size();
                    if (count == 1) {
                        graphics.setColor(colors[index]);
                        graphics.fill(markShape);
                    } else {
                        final ShapeBroker broker = new ShapeBroker(markShape);
                        for (final Species sp : species) {
                            graphics.setColor(getIcon(sp).getColor());
                            graphics.fill(broker);
                            if (broker.finished()) {
                                break;
                            }
                        }
                    }
                    graphics.setColor(Color.BLACK);
                    graphics.draw(markShape);
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        /**
         * Returns a string representation for the sample at the specified index.
         * This string will be displayed as a tool tip when the mouse cursor is
         * over the sample.
         */
        protected String getToolTipText(final GeoMouseEvent event) {
            if (format == null) {
                format = NumberFormat.getNumberInstance();
                buffer = new StringBuffer();
                dummy  = new FieldPosition(0);
            }
            buffer.setLength(0);
            final SampleEntry c=samples.get(index);
            format.format(c.getValue(), buffer, dummy);
            final Unit unit = c.getUnit();
            if (unit != null) {
                buffer.append(' ');
                buffer.append(unit);
            }
            return buffer.toString();
        }
    }
}
