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
import net.seas.opengis.pt.Dimensioned;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.cs.AxisOrientation;

// Images
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import javax.media.jai.TiledImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// Geometry
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seas.util.XAffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Collections
import java.util.List;
import java.util.Vector;
import java.util.Comparator;
import javax.media.jai.PropertySource;
import javax.media.jai.PropertySourceImpl;

// Miscellaneous
import java.util.Arrays;
import java.util.Locale;
import net.seas.resources.Resources;
import net.seas.util.Version;
import net.seas.util.XArray;
import net.seas.util.XClass;


/**
 * Base class of all coverage type. {@linkplain net.seas.opengis.gc.GridCoverage Grid coverages}
 * are typically 2D while other coverages may be 3D or 4D. The dimension of grid coverage may be
 * queried in many ways:
 *
 * <ul>
 *   <li><code>getSourceCoordinateSystem().getDimension();</code></li>
 *   <li><code>getDimensionNames().length;</code></li>
 *   <li><code>getDimension();</code></li>
 * </ul>
 *
 * All those methods should returns the same number.   Note that the dimension
 * of grid coverage <strong>is not the same</strong> than the number of sample
 * dimensions  (<code>getSampleDimensions().size()</code>).   The later may be
 * better understood as the number of bands for 2D grid coverage.
 * <br><br>
 * There is no <code>getMetadataValue(...)</code> method in this implementation.
 * OpenGIS's metadata are called "Properties" in <em>Java Advanced Imaging</em>.
 * Use {@link #getProperty} instead.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public abstract class Coverage extends PropertySourceImpl implements Dimensioned
{
    /**
     * The coverage name.
     */
    private final String name;

    /**
     * The coordinate system, or <code>null</code> if there is none.
     */
    private final CoordinateSystem coordinateSystem;

    /**
     * The names of each dimension in the coverage. Typically these names are
     * “x”, “y”, “z” and “t”. Grid coverages are typically 2D (x,y) while other
     * coverages may be 3D (x,y,z) or 4D (x,y,z,t). The number of dimensions of
     * the coverage is the number of entries in this list of dimension names.
     */
    private final String[] dimensionNames;

    /**
     * Construct a new coverage with the same
     * parameters than the specified coverage.
     */
    protected Coverage(final Coverage coverage)
    {
        super(null, coverage);
        this.name             = coverage.name;
        this.coordinateSystem = coverage.coordinateSystem;
        this.dimensionNames   = coverage.dimensionNames;
    }

    /**
     * Construct a coverage with no coordinate system.
     *
     * @param name The coverage name.
     * @param dimensionNames The names of each dimension in the coverage.
     *        Typically these names are “x”, “y”, “z” and “t”. Grid coverages
     *        are typically 2D (x,y) while other coverages may be 3D (x,y,z)
     *        or 4D (x,y,z,t). The array's length will determine the number
     *        of dimensions of the coverage.
     * @param properties The set of properties for this coverage, or <code>null</code>
     *        if there is none. "Properties" in <em>Java Advanced Imaging</em> is what
     *        OpenGIS calls "Metadata". There is non <code>getMetadataValue(...)</code>
     *        method in this implementation. Use {@link #getProperty} instead.
     */
    public Coverage(final String name, final String[] dimensionNames, final PropertySource properties)
    {
        super(null, properties);
        this.name             = name;
        this.coordinateSystem = null;
        this.dimensionNames   = (String[]) dimensionNames.clone();
    }

    /**
     * Construct a coverage using the specified coordinate system.
     * The names of each dimension in the coverage will be determined
     * from the coordinate system axis infos.
     *
     * @param name The coverage name.
     * @param coordinateSystem The coordinate system. This specifies
     *        the coordinate system used when accessing a coverage or
     *        grid coverage with the “evaluate” methods.
     * @param properties The set of properties for this coverage, or <code>null</code>
     *        if there is none. "Properties" in <em>Java Advanced Imaging</em> is what
     *        OpenGIS calls "Metadata". There is non <code>getMetadataValue(...)</code>
     *        method in this implementation. Use {@link #getProperty} instead.
     */
    public Coverage(final String name, final CoordinateSystem coordinateSystem, final PropertySource properties)
    {
        super(null, properties);
        this.name             = name;
        this.coordinateSystem = coordinateSystem;
        this.dimensionNames   = new String[coordinateSystem.getDimension()];
        for (int i=0; i<dimensionNames.length; i++)
            dimensionNames[i] = coordinateSystem.getAxis(i).name;
    }

    /**
     * Returns the coverage name, localized for the supplied locale.
     * If the specified locale is not available, returns a name in an
     * arbitrary locale. The default implementation returns the name
     * specified at construction time.
     *
     * @param  locale The desired locale, or <code>null</code> for a default locale.
     * @return The coverage name in the specified locale, or in an arbitrary locale
     *         if the specified localization is not available.
     */
    public String getName(final Locale locale)
    {return name;}

    /**
     * Returns the coordinate system. This specifies the coordinate system used when
     * accessing a coverage or grid coverage with the “evaluate” methods. It is also
     * the coordinate system of the coordinates used with the math transform  {@link
     * net.seas.opengis.gc.GridGeometry#getGridToCoordinateSystem}. This coordinate
     * system is usually different than the grid coordinate system of the grid. A grid
     * coverage can be accessed (re-projected) with new coordinate system with the
     * {@link net.seas.opengis.gp.GridCoverageProcessor} component.
     * In this case, a new instance of a grid coverage is created.
     *
     * @return The coordinate system, or <code>null</code> if this coverage
     *         does not have an associated coordinate system.
     *
     * @see net.seas.opengis.gc.GridGeometry#getGridToCoordinateSystem
     */
    public CoordinateSystem getCoordinateSystem()
    {return coordinateSystem;}

    /**
     * Returns The bounding box for the coverage domain in coordinate
     * system coordinates. May be null if this coverage has no associated
     * coordinate system. The default implementation returns the coordinate
     * system envelope if there is one.
     */
    public Envelope getEnvelope()
    {
        final CoordinateSystem cs = getCoordinateSystem();
        return (cs!=null) ? cs.getDefaultEnvelope() : null;
    }

    /**
     * Returns the dimension of the grid coverage.
     */
    public int getDimension()
    {return dimensionNames.length;}

    /**
     * Returns the names of each dimension in the coverage. Typically these names
     * are “x”, “y”, “z” and “t”. Grid coverages are typically 2D (x,y) while other
     * coverages may be 3D (x,y,z) or 4D (x,y,z,t).
     *
     * @param  locale The desired locale, or <code>null</code> for the default locale.
     * @return The names of each dimension. The array's length is equals to {@link #getDimension}.
     */
    public String[] getDimensionNames(final Locale locale)
    {
        final CoordinateSystem cs = getCoordinateSystem();
        if (cs!=null)
        {
            final String[] names = new String[cs.getDimension()];
            for (int i=0; i<names.length; i++)
                names[i] = cs.getAxis(i).getName(locale);
            return names;
        }
        return (String[]) dimensionNames.clone();
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
    public boolean[] evaluate(final CoordinatePoint coord, boolean[] dest) throws PointOutsideCoverageException
    {
        final double[] result = evaluate(coord, (double[])null);
        if (dest==null)  dest = new boolean[result.length];
        for (int i=0; i<result.length; i++)
        {
            final double value = result[i];
            dest[i] = (!Double.isNaN(value) && value!=0);
        }
        return dest;
    }

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
    public int[] evaluate(final CoordinatePoint coord, int[] dest) throws PointOutsideCoverageException
    {
        final double[] result = evaluate(coord, (double[])null);
        if (dest==null)  dest = new int[result.length];
        for (int i=0; i<result.length; i++)
        {
            final double value = Math.rint(result[i]);
            dest[i] = (value < Integer.MIN_VALUE) ? Integer.MIN_VALUE :
                      (value > Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                      (int) value;
        }
        return dest;
    }

    /**
     * Return an sequence of float values for a given point in the coverage.
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
    public float[] evaluate(final CoordinatePoint coord, float[] dest) throws PointOutsideCoverageException
    {
        final double[] result = evaluate(coord, (double[])null);
        if (dest==null)  dest = new float[result.length];
        for (int i=0; i<result.length; i++)
        {
            dest[i] = (float)result[i];
        }
        return dest;
    }

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
     * Returns 2D view of this grid coverage as a renderable image.
     * This method allows interoperability with Java2D.
     *
     * @param  xAxis Dimension to use for <var>x</var> axis.
     * @param  yAxis Dimension to use for <var>y</var> axis.
     * @return A 2D view of this grid coverage as a renderable image.
     */
    public RenderableImage getRenderableImage(final int xAxis, final int yAxis)
    {return new Renderable(xAxis, yAxis);}

    /**
     * Base class for renderable image of a grid coverage.
     * Renderable images allow interoperability with Java2D
     * for a two-dimensional view of a coverage (which may
     * or may not be a grid coverage).
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    protected class Renderable extends PropertySourceImpl implements RenderableImage
    {
        /**
         * The two dimensional view of the coverage's envelope.
         */
        private final Rectangle2D bounds;

        /**
         * Dimension to use for <var>x</var> axis.
         */
        protected final int xAxis;

        /**
         * Dimension to use for <var>y</var> axis.
         */
        protected final int yAxis;

        /**
         * Construct a renderable image.
         *
         * @param  xAxis Dimension to use for <var>x</var> axis.
         * @param  yAxis Dimension to use for <var>y</var> axis.
         */
        public Renderable(final int xAxis, final int yAxis)
        {
            super(null, Coverage.this);
            this.xAxis = xAxis;
            this.yAxis = yAxis;
            final Envelope envelope = getEnvelope();
            bounds = new Rectangle2D.Double(envelope.getMinimum(xAxis),
                                            envelope.getMinimum(yAxis),
                                            envelope.getLength (xAxis),
                                            envelope.getLength (yAxis));
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
        {return (float)bounds.getWidth();}

        /**
         * Gets the height in coverage coordinate space.
         *
         * @see Coverage#getEnvelope
         * @see Coverage#getCoordinateSystem
         */
        public float getHeight()
        {return (float)bounds.getHeight();}

        /**
         * Gets the minimum X coordinate of the rendering-independent image data.
         *
         * @see Coverage#getEnvelope
         * @see Coverage#getCoordinateSystem
         */
        public float getMinX()
        {return (float)bounds.getX();}

        /**
         * Gets the minimum Y coordinate of the rendering-independent image data.
         *
         * @see Coverage#getEnvelope
         * @see Coverage#getCoordinateSystem
         */
        public float getMinY()
        {return (float)bounds.getY();}

        /**
         * Returnd a rendered image with a default width and height in pixels.
         *
         * @return A rendered image containing the rendered data
         */
        public RenderedImage createDefaultRendering()
        {return createScaledRendering(512, 0, null);}

        /**
         * Creates a rendered image with width <code>width</code> and height
         * <code>height</code> in pixels. If <code>width</code> is 0, it will
         * be computed automatically from <code>height</code>. Conversely, if
         * <code>height</code> is 0, il will be computed automatically from
         * <code>width</code>. <code>width</code> and <code>height</code>
         * can not be both zero.
         *
         * @param  width  The width of rendered image in pixels, or 0.
         * @param  height The height of rendered image in pixels, or 0.
         * @param  hints  Rendering hints, or <code>null</code>.
         * @return A rendered image containing the rendered data
         */
        public RenderedImage createScaledRendering(int width, int height, final RenderingHints hints)
        {
            final double boundsWidth  = bounds.getWidth();
            final double boundsHeight = bounds.getHeight();
            if (!(width>0)) // Use '!' in order to catch NaN
            {
                if (!(height>0))
                {
                    throw new IllegalArgumentException(Resources.format(Clé.UNSPECIFIED_IMAGE_SIZE));
                }
                width = (int)Math.round(height * (boundsWidth/boundsHeight));
            }
            else if (!(height>0))
            {
                height = (int)Math.round(width * (boundsHeight/boundsWidth));
            }
            return createRendering(new RenderContext(getTransform(new Rectangle(0,0,width,height)), hints));
        }

        /**
         * Creates a rendered image using a given render context.
         *
         * @param  context The render context to use to produce the rendering.
         * @return A rendered image containing the rendered data
         */
        public RenderedImage createRendering(final RenderContext context)
        {
            final List<SampleDimension> catg = getSampleDimensions();
            final AffineTransform  transform = context.getTransform();
            final Shape                 area = context.getAreaOfInterest();
            final Rectangle2D        srcRect = (area!=null) ? area.getBounds2D() : bounds;
            final Rectangle          dstRect = (Rectangle) XAffineTransform.transform(transform, srcRect, new Rectangle());
            final ColorModel      colorModel = catg.get(0).getCategoryList().getColorModel(true, catg.size());
            final SampleModel    sampleModel = colorModel.createCompatibleSampleModel(512, 512);
            final TiledImage           image = new TiledImage(dstRect.x, dstRect.y, dstRect.width, dstRect.height, 0, 0, sampleModel, colorModel);
            final CoordinatePoint coordinate = new CoordinatePoint(getDimension());
            final Point2D.Double     point2D = new Point2D.Double();

            final int xmin = dstRect.x;
            final int ymin = dstRect.y;
            final int xmax = dstRect.x + dstRect.width;
            final int ymax = dstRect.y + dstRect.height;
            final int numBands = image.getNumBands();
            final double[] samples=new double[numBands];
            final double[] padNaNs=new double[numBands];
            Arrays.fill(padNaNs, Double.NaN);

            final WritableRectIter iterator = RectIterFactory.createWritable(image, dstRect);
            try
            {
                for (int y=ymin; y<ymax; y++)
                {
                    for (int x=xmin; x<xmax; x++)
                    {
                        point2D.x = x;
                        point2D.y = y;
                        transform.inverseTransform(point2D, point2D);
                        if (area==null || area.contains(point2D))
                        {
                            coordinate.ord[xAxis] = point2D.x;
                            coordinate.ord[yAxis] = point2D.y;
                            iterator.setPixel(evaluate(coordinate, samples));
                        }
                        else iterator.setPixel(padNaNs);
                        iterator.nextPixel();
                    }
                    assert(iterator.finishedPixels());
                    iterator.startPixels();
                    iterator.nextLine();
                }
                assert(iterator.finishedLines());
            }
            catch (NoninvertibleTransformException exception)
            {
                final IllegalArgumentException e = new IllegalArgumentException("RenderContext");
                if (Version.MINOR>=4) e.initCause(exception);
                throw e;
            }
            return image;
        }

        /**
         * Returns an affine transform that maps the coverage envelope
         * to the specified destination rectangle.  This transform may
         * swap axis in order to normalize them (i.e. make them appear
         * in the (x,y) order).
         *
         * @param destination The two-dimensional destination rectangle.
         */
        private AffineTransform getTransform(final Rectangle2D destination)
        {
            final Matrix matrix;
            final Envelope srcEnvelope = new Envelope(bounds);
            final Envelope dstEnvelope = new Envelope(destination);
            final CoordinateSystem  cs = getCoordinateSystem();
            if (cs!=null)
            {
                final AxisOrientation[] axis = new AxisOrientation[]
                {
                    cs.getAxis(xAxis).orientation,
                    cs.getAxis(yAxis).orientation
                };
                final AxisOrientation[] normalized = (AxisOrientation[]) axis.clone();
                Arrays.sort(normalized); // TODO: is it really a good idea?
                normalized[0] = normalized[0].absolute();
                normalized[1] = normalized[1].absolute().inverse(); // Image's Y axis is downward.
                matrix = Matrix.createAffineTransform(srcEnvelope, axis, dstEnvelope, normalized);
            }
            else
            {
                matrix = Matrix.createAffineTransform(srcEnvelope, dstEnvelope);
            }
            return matrix.toAffineTransform2D();
        }
    }

    /**
     * Returns a view of an image in which all pixels have been transformed into
     * floating-point values with the {@link CategoryList#toValue} method.  The
     * resulting image usually represents some geophysics parameter in "real
     * world" scientific and engineering units (e.g. temperature in °C).
     *
     * @param image      Image to convert. This image usually store pixel values as integers.
     * @param categories The list of categories to use for transforming pixel values into
     *                   geophysics parameters. This array's length must matches the number
     *                   of bands in <code>image</code>.
     * @return           The converted image. This image store geophysics values as floating-point
     *                   numbers. This method returns <code>null</code> if <code>image</code> was null.
     *
     * @see #toThematic
     */
    protected static RenderedImage toNumeric(final RenderedImage image, final CategoryList[] categories)
    {return NumericImage.getInstance(image, categories);}

    /**
     * Returns a view of an image in which all geophysics values have been transformed
     * into indexed pixel with the {@link CategoryList#toIndex} method.  The resulting
     * image is more suitable for rendering than the geophysics image (since Java2D do
     * a better job with integer pixels than floating-point pixels).
     *
     * @param image      Image to convert. This image usually represents some geophysics
     *                   parameter in "real world" scientific and engineering units (e.g.
     *                   temperature in °C).
     * @param categories The list of categories to use for transforming floating-point values
     *                   into pixel index. This array's length must matches the number of bands
     *                   in <code>image</code>.
     * @return           The converted image. This image store pixel index as integer.
     *                   This method returns <code>null</code> if <code>image</code> was null.
     *
     * @see #toNumeric
     */
    protected static RenderedImage toThematic(final RenderedImage image, final CategoryList[] categories)
    {return ThematicImage.getInstance(image, categories);}

    /**
     * Returns a string représentation of this coverage.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(name);
        buffer.append(": ");
        buffer.append(getEnvelope());
        buffer.append(']');
        return buffer.toString();
    }
}
