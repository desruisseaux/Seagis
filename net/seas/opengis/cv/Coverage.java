/*
 * OpenGIS implementation in Java
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.cv;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Matrix;
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.cs.AxisOrientation;

// Images
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;

// Collections
import java.util.List;
import java.util.Vector;
import javax.media.jai.PropertySource;
import javax.media.jai.PropertySourceImpl;

// Miscellaneous
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seas.resources.Resources;
import net.seas.util.XArray;


/**
 * Base class of all coverage type. {@linkplain net.seas.opengis.cv.GridCoverage Grid coverages}
 * are typically 2D while other coverages may be 3D or 4D. The number of dimensions may be queried
 * in many ways:
 *
 * <ul>
 *   <li><code>getSourceCoordinateSystem().getDimension();</code></li>
 *   <li><code>getSampleDimensions().size();</code></li>
 *   <li><code>getDimensionNames().length;</code></li>
 *   <li><code>getDimension();</code></li>
 * </ul>
 *
 * All those methods should returns the same number.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public abstract class Coverage implements PropertySource
{
    /**
     * The default constructor.
     */
    public Coverage()
    {}

    /**
     * Returns the dimension of the grid coverage.
     */
    public int getDimension()
    {return getSampleDimensions().size();}

    /**
     * Returns the coordinate system. This specifies the coordinate system used when
     * accessing a coverage or grid coverage with the “evaluate” methods. It is also
     * the coordinate system of the coordinates used with the math transform  {@link
     * net.seas.opengis.gc.GridGeometry#gridToCoordinateSystem}. This coordinate
     * system is usually different than the grid coordinate system of the grid. A grid
     * coverage can be accessed (re-projected) with new coordinate system with the
     * {@link net.seas.opengis.gp.GridCoverageProcessor} component.
     * In this case, a new instance of a grid coverage is created.
     *
     * @return The coordinate system, or <code>null</code> if this coverage
     *         does not have an associated coordinate system.
     *
     * @see net.seas.opengis.gc.GridGeometry#gridToCoordinateSystem
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Returns The bounding box for the coverage domain in coordinate
     * system coordinates. May be null if this coverage has no associated
     * coordinate system.
     */
    public abstract Envelope getEnvelope();

    /**
     * Returns the names of each dimension in the coverage. Typically these names
     * are “x”, “y”, “z” and “t”. Grid coverages are typically 2D (x,y) while other
     * coverages may be 3D (x,y,z) or 4D (x,y,z,t). The number of dimensions of the
     * coverage is the number of entries in the list of dimension names.
     */
    public String[] getDimensionNames()
    {
        final List<SampleDimension> dim = getSampleDimensions();
        final String[] names = new String[dim.size()];
        for (int i=0; i<names.length; i++)
            names[i] = dim.get(i).getName();
        return names;
    }

    /**
     * Retrieve sample dimension information for the coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension. A coverage must have at least one sample dimension.
     */
    public abstract List<SampleDimension> getSampleDimensions();

    /**
     * Return a sequence of boolean values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * nearest neighbor. The coordinate system of the point is the same as the grid
     * coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract boolean[] evaluate(CoordinatePoint coord, boolean[] dest) throws PointOutsideCoverageException;

    /**
     * Return a sequence of unsigned byte values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default
     * interpolation type used when accessing grid values for points which fall
     * between grid cells is nearest neighbor. The coordinate system of the
     * point is the same as the grid coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract short[] evaluate(CoordinatePoint coord, short[] dest) throws PointOutsideCoverageException;

    /**
     * Return a sequence of integer values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default
     * interpolation type used when accessing grid values for points which fall
     * between grid cells is nearest neighbor. The coordinate system of the
     * point is the same as the grid coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract int[] evaluate(CoordinatePoint coord, int[] dest) throws PointOutsideCoverageException;

    /**
     * Return an sequence of double values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * nearest neighbor. The coordinate system of the point is the same as the grid coverage
     * coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract double[] evaluate(CoordinatePoint coord, double[] dest) throws PointOutsideCoverageException;

    /**
     * <FONT COLOR="#FF6633">Returns this grid coverage as a renderable image.</FONT>
     * This method allows interoperability with Java2D, but will work only for
     * two-dimensional coverages.
     *
     * @return This grid coverage as a renderable image.
     * @throws IllegalStateException if this coverage is not two-dimensional.
     */
    public abstract RenderableImage getRenderableImage() throws IllegalStateException;

    /**
     * Returns an array of metadata keywords for this coverage.
     * If no properties are available, <code>null</code> will be
     * returned.
     */
    public String[] getPropertyNames()
    {return null;}

    /**
     * Returns an array of strings recognized as names that begin with the
     * supplied prefix. If no property names match, <code>null</code> will
     * be returned. The comparison is done in a case-independent manner.
     */
    public String[] getPropertyNames(final String prefix)
    {
        int count=0;
        final int length = prefix.length();
        final String[] names = getPropertyNames();
        if (names!=null)
        {
            for (int i=0; i<names.length; i++)
            {
                final String name = names[i];
                if (name!=null && name.length()>=length && prefix.equalsIgnoreCase(name.substring(0, length)))
                {
                    names[count++] = name;
                }
            }
            return (count!=0) ? XArray.resize(names, count) : null;
        }
        return names;
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned
     */
    public Class getPropertyClass(final String propertyName)
    {return null;}

    /**
     * Retrieve the metadata value for a given metadata name. If the metadata name
     * is not recognized, {@link Image#UndefinedProperty} will be returned.
     *
     * @param name Metadata keyword for which to retrieve metadata.
     */
    public Object getProperty(final String name)
    {return Image.UndefinedProperty;}

    /**
     * <FONT COLOR="#FF6633">Base class for a renderable view of grid coverage.</FONT>
     * Renderable images allow interoperability with Java2D for two-dimensional coverage
     * (which may or may not be a grid coverage).
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    protected abstract class Renderable extends PropertySourceImpl implements RenderableImage
    {
        /**
         * The envelope as a {@link Rectangle2D}.
         */
        private final Rectangle2D envelope;

        /**
         * Construct a renderable image.
         *
         * @throws IllegalStateException if the underlying coverage is not two-dimensional.
         */
        public Renderable() throws IllegalStateException
        {
            super(null, Coverage.this);
            envelope = getEnvelope().toRectangle2D();
        }

        /**
         * Returns <code>null</code> to indicate
         * that no source information is available.
         */
        public Vector getSources()
        {return null;}

        /**
         * Returns true if successive renderings with the same arguments
         * may produce different results. The default implementation returns
         * <code>false</code>.
         *
         * @see net.seas.opengis.gc.GridCoverage#isDataEditable
         */
        public boolean isDynamic()
        {return false;}

        /**
         * Gets the width in coverage coordinate space.
         *
         * @see Coverage#getEnvelope
         * @see Coverage#getCoordinateSystem
         */
        public float getWidth()
        {return (float)envelope.getWidth();}

        /**
         * Gets the height in coverage coordinate space.
         *
         * @see Coverage#getEnvelope
         * @see Coverage#getCoordinateSystem
         */
        public float getHeight()
        {return (float)envelope.getHeight();}

        /**
         * Gets the minimum X coordinate of the rendering-independent image data.
         *
         * @see Coverage#getEnvelope
         * @see Coverage#getCoordinateSystem
         */
        public float getMinX()
        {return (float)envelope.getX();}

        /**
         * Gets the minimum Y coordinate of the rendering-independent image data.
         *
         * @see Coverage#getEnvelope
         * @see Coverage#getCoordinateSystem
         */
        public float getMinY()
        {return (float)envelope.getY();}

        /**
         * Creates a rendered image with width <code>width</code>
         * and height <code>height</code> in pixels.
         *
         * @param  width  The width of rendered image in pixels, or 0.
         * @param  height The height of rendered image in pixels, or 0.
         * @param  hints  Rendering hints, or <code>null</code>.
         * @return A rendered image containing the rendered data
         */
        public RenderedImage createScaledRendering(int width, int height, final RenderingHints hints)
        {
            final double coverageWidth  = envelope.getWidth();
            final double coverageHeight = envelope.getHeight();
            if (!(width>0)) // Use '!' in order to catch NaN
            {
                if (!(height>0))
                {
                    throw new IllegalArgumentException(Resources.format(Clé.UNSPECIFIED_IMAGE_SIZE));
                }
                width = (int)Math.round(height * (coverageWidth/coverageHeight));
            }
            else if (!(height>0))
            {
                height = (int)Math.round(width * (coverageHeight/coverageWidth));
            }
            return createRendering(new RenderContext(getTransform(new Rectangle(0,0,width,height)), hints));
        }

        /**
         * Returns an affine transform that maps the coverage envelope
         * to the specified destination rectangle.
         */
        protected AffineTransform getTransform(final Rectangle2D destination)
        {
            final Matrix matrix;
            final Envelope srcEnvelope = getEnvelope();
            final Envelope dstEnvelope = new Envelope(destination);
            final CoordinateSystem  cs = getCoordinateSystem();
            if (cs!=null)
            {
                final AxisOrientation[] axis = new AxisOrientation[]
                {
                    cs.getAxis(0).orientation,
                    cs.getAxis(1).orientation
                };
                final AxisOrientation[] normalizedAxis = axis;
                // TODO: sort normalizedAxis. Don't forget the second dimension (usually Y).
                matrix = Matrix.createAffineTransform(srcEnvelope, axis, dstEnvelope, normalizedAxis);
            }
            else
            {
                matrix = Matrix.createAffineTransform(srcEnvelope, dstEnvelope);
            }
            return matrix.toAffineTransform2D();
        }
    }
}
