/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.gc;

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
import net.seagis.pt.CoordinatePoint;
import net.seagis.pt.MismatchedDimensionException;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cs.CoordinateSystem;

// OpenGIS dependencies (SEAGIS)
import net.seagis.ct.MathTransform;
import net.seagis.ct.MathTransform2D;
import net.seagis.ct.TransformException;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cv.Coverage;
import net.seagis.cv.Category;
import net.seagis.cv.CategoryList;
import net.seagis.cv.SampleDimension;
import net.seagis.cv.PointOutsideCoverageException;

// Images
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.awt.image.renderable.ParameterBlock;

// Java Advanced Imaging
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.Histogram;
import javax.media.jai.ImageMIPMap;
import javax.media.jai.PlanarImage;
import javax.media.jai.GraphicsJAI;
import javax.media.jai.util.Range;
import javax.media.jai.util.CaselessStringKey;

// Geometry
import java.awt.Point;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seagis.resources.XAffineTransform;
import net.seagis.resources.XDimension2D;

// Collections
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

// Weak references
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

// Events
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// Miscellaneous
import java.util.Date;
import java.util.Arrays;
import java.text.DateFormat;
import java.text.FieldPosition;

// Resources
import net.seagis.resources.XArray;
import net.seagis.resources.OpenGIS;
import net.seagis.resources.WeakHashSet;
import net.seagis.resources.gcs.Resources;
import net.seagis.resources.gcs.ResourceKeys;


/**
 * Basic access to grid data values. Grid coverages are backed by
 * {@link RenderedImage}. Each band in an image is represented as
 * a sample dimension.
 * <br><br>
 * Grid coverages are usually two-dimensional. However, their envelope may
 * have more than two dimensions.  For example, a remote sensing image may
 * be valid only over some time range (the time of satellite pass over the
 * observed area). Envelope for such grid coverage may have three dimensions:
 * the two usual ones (horizontal extends along <var>x</var> and <var>y</var>),
 * and a third one for start time and end time (time extends along <var>t</var>).
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public class GridCoverage extends Coverage
{
    /**
     * Tells if we should try an optimisation using pyramidal images.
     * Default value do not use this optimisation, since it doesn't
     * seems to provide the expected performance benefict.
     */
    private static final boolean USE_PYRAMID=false;

    /**
     * Decimation factor for image. A value of 0.5 means that each
     * level in the image pyramid will contains an image with half
     * the resolution of previous level. This value is used only if
     * {@link #USE_PYRAMID} is <code>true</code>.
     */
    private static final double DOWN_SAMPLER = 0.5;

    /**
     * Natural logarithm of {@link #DOWN_SAMPLER}. Used
     * only if {@link #USE_PYRAMID} is <code>true</code>.
     */
    private static final double LOG_DOWN_SAMPLER = Math.log(DOWN_SAMPLER);

    /**
     * Minimum size (in pixel) for use of pyramidal image. Images smaller
     * than this size will not use pyramidal images, since it would not
     * give many visible benefict. Used only if {@link #USE_PYRAMID} is
     * <code>true</code>.
     */
    private static final int MIN_SIZE = 256;

    /**
     * Pool of created object. Objects in this pool must be immutable.
     * Those objects will be shared among many grid coverages.
     */
    private static final WeakHashSet pool=new WeakHashSet();

    /**
     * An empty list of grid coverage.
     */
    private static final GridCoverage[] EMPTY_LIST = new GridCoverage[0];

    /**
     * Sources grid coverage.
     */
    private final GridCoverage[] sources;

    /**
     * Underlying data as an {@link RenderedImage} object.   This object contains
     * "real world" (geophysics) measurements, for example temperature in Celsius
     * degrees. Pixel are usually stored as floating point numbers.
     */
    protected final PlanarImage data;

    /**
     * A mirror of {@link #data} suitable for rendering.  This image usually store pixels as
     * integer values.  Transformations from {@link #data} to {@link #image} are computed on
     * fly using a set a linear equations (specified through {@link Category} objects). This
     * object may be equals to {@link #data} if no transformation is available or needed.
     */
    protected final PlanarImage image;

    /**
     * A list of multi-resolution images. Image at level 0 is identical to
     * {@link #image}. Other level contains the image at lower resolution
     * for faster rendering.
     */
    private final ImageMIPMap images;

    /**
     * Maximum amount of level to use for multi-resolution images.
     */
    private final int maxLevel;

    /**
     * The grid geometry.
     */
    protected final GridGeometry gridGeometry;

    /**
     * The image's envelope. This envelope must have at least two
     * dimensions. It may have more dimensions if the image have
     * some extend in other dimensions (for example a depth, or
     * a start and end time).
     */
    private final Envelope envelope;

    /**
     * List of sample dimension information for the grid coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension. A coverage must have at least one sample dimension.
     */
    private final SampleDimension[] sampleDimensions;

    /**
     * Construct a new grid coverage with the same parameter
     * than the specified coverage.
     */
    protected GridCoverage(final GridCoverage coverage)
    {
        super(coverage);
        data             = coverage.data;
        image            = coverage.image;
        images           = coverage.images;
        maxLevel         = coverage.maxLevel;
        gridGeometry     = coverage.gridGeometry;
        envelope         = coverage.envelope;
        sampleDimensions = coverage.sampleDimensions;
        sources          = new GridCoverage[] {coverage};
    }

    /**
     * Construct a grid coverage with the specified envelope.
     * Pixels will not be classified in any category.
     *
     * @param name         The grid coverage name.
     * @param image        The image.
     * @param cs           The coordinate system. This specifies the coordinate system used
     *                     when accessing a grid coverage with the "evaluate" methods.  The
     *                     number of dimensions must matches the number of dimensions for
     *                     <code>envelope</code>.
     * @param envelope     The grid coverage cordinates. This envelope must have at least two
     *                     dimensions.   The two first dimensions describe the image location
     *                     along <var>x</var> and <var>y</var> axis. The other dimensions are
     *                     optional and may be used to locate the image on a vertical axis or
     *                     on the time axis.
     *
     * @throws MismatchedDimensionException If the envelope's dimension
     *         is not the same than the coordinate system's dimension.
     */
    public GridCoverage(final String         name, final RenderedImage  image,
                        final CoordinateSystem cs, final Envelope    envelope) throws MismatchedDimensionException
    {
        this(name, image, cs, envelope, null, false, null, null);
    }

    /**
     * Construct a grid coverage with the specified envelope and category lists.
     *
     * @param name         The grid coverage name.
     * @param image        The image.
     * @param cs           The coordinate system. This specifies the coordinate system used
     *                     when accessing a grid coverage with the "evaluate" methods.  The
     *                     number of dimensions must matches the number of dimensions for
     *                     <code>envelope</code>.
     * @param envelope     The grid coverage cordinates. This envelope must have at least two
     *                     dimensions.   The two first dimensions describe the image location
     *                     along <var>x</var> and <var>y</var> axis. The other dimensions are
     *                     optional and may be used to locate the image on a vertical axis or
     *                     on the time axis.
     * @param categories   Category lists which allows for the transformation from pixel
     *                     values to real world geophysics value. This array's length must
     *                     matches the number of bands in <code>image</code>. This argument
     *                     may be <code>null</code> if there is no categories for the image.
     * @param isGeophysics <code>true</code> if pixel's values are already geophysics values, or
     *                     <code>false</code> if transformation described in <code>categories</code>
     *                     must be applied first. This argument is ignored if <code>categories</code>
     *                     is <code>null</code>.
     * @param sources      The sources for this grid coverage, or <code>null</code> if none.
     * @param properties The set of properties for this coverage, or <code>null</code>
     *        if there is none. "Properties" in <em>Java Advanced Imaging</em> is what
     *        OpenGIS calls "Metadata".  There is no <code>getMetadataValue(...)</code>
     *        method in this implementation. Use {@link #getProperty} instead. Keys may
     *        be {@link String} or {@link CaselessStringKey} objects,  while values may
     *        be any {@link Object}.
     *
     * @throws MismatchedDimensionException If the envelope's dimension
     *         is not the same than the coordinate system's dimension.
     * @param  IllegalArgumentException if the number of bands differs
     *         from the number of categories list.
     */
    public GridCoverage(final String         name,       final RenderedImage  image,
                        final CoordinateSystem cs,       final Envelope    envelope,
                        final CategoryList[] categories, final boolean isGeophysics,
                        final GridCoverage[] sources,    final Map properties) throws MismatchedDimensionException
    {
        this(name, PlanarImage.wrapRenderedImage(image), cs, (Envelope)envelope.clone(), null, categories, isGeophysics, sources, properties);
    }

    /**
     * Construct a grid coverage with the specified transform and category lists.
     *
     * @param name         The grid coverage name.
     * @param image        The image.
     * @param cs           The coordinate system. This specifies the coordinate system used
     *                     when accessing a grid coverage with the "evaluate" methods.  The
     *                     number of dimensions must matches the number of dimensions for
     *                     <code>gridToCS</code>.
     * @param gridToCS     The math transform from grid to coordinate system.
     * @param categories   Category lists which allows for the transformation from pixel
     *                     values to real world geophysics value. This array's length must
     *                     matches the number of bands in <code>image</code>. This argument
     *                     may be <code>null</code> if there is no categories for the image.
     * @param isGeophysics <code>true</code> if pixel's values are already geophysics values, or
     *                     <code>false</code> if transformation described in <code>categories</code>
     *                     must be applied first. This argument is ignored if <code>categories</code>
     *                     is <code>null</code>.
     * @param sources      The sources for this grid coverage, or <code>null</code> if none.
     * @param properties The set of properties for this coverage, or <code>null</code>
     *        if there is none. "Properties" in <em>Java Advanced Imaging</em> is what
     *        OpenGIS calls "Metadata".  There is no <code>getMetadataValue(...)</code>
     *        method in this implementation. Use {@link #getProperty} instead. Keys may
     *        be {@link String} or {@link CaselessStringKey} objects,  while values may
     *        be any {@link Object}.
     *
     * @throws MismatchedDimensionException If the transform's dimension
     *         is not the same than the coordinate system's dimension.
     * @param  IllegalArgumentException if the number of bands differs
     *         from the number of categories list.
     */
    public GridCoverage(final String         name,       final RenderedImage    image,
                        final CoordinateSystem cs,       final MathTransform gridToCS,
                        final CategoryList[] categories, final boolean   isGeophysics,
                        final GridCoverage[] sources,    final Map properties) throws MismatchedDimensionException
    {
        this(name, PlanarImage.wrapRenderedImage(image), cs, null, gridToCS, categories, isGeophysics, sources, properties);
    }

    /**
     * Construct a grid coverage. This private constructor expect both an envelope
     * (<code>envelope</code>) and a math transform (<code>transform</code>).  One
     * of those argument should be null.   The null argument will be computed from
     * the non-null argument.
     */
    private GridCoverage(final String         name,       final PlanarImage image,
                         final CoordinateSystem cs,       Envelope envelope, MathTransform transform,
                         final CategoryList[] categories, final boolean isGeophysics,
                         final GridCoverage[] sources,    final Map properties) throws MismatchedDimensionException
    {
        super(name, cs, image, properties);
        if (sources!=null)
        {
            this.sources = (GridCoverage[]) sources.clone();
        }
        else this.sources = EMPTY_LIST;

        /*------------------------------------------
         * Check category lists. The number of lists
         * must matches the number of image's bands.
         */
        final int numBands = image.getSampleModel().getNumBands();
        if (categories!=null && numBands!=categories.length)
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NUMBER_OF_BANDS_MISMATCH_$2,
                                               new Integer(numBands), new Integer(categories.length)));
        }

        /*------------------------------------------------------------
         * Checks the envelope. The envelope must be non-empty and its
         * dimension must matches the coordinate system's dimension. A
         * pool of shared envelopes will be used in order to recycle
         * existing envelopes.
         */
        if (envelope==null) try
        {
            envelope = new Envelope(cs.getDimension());
            for (int i=envelope.getDimension(); --i>=0;)
            {
                final int min, max;
                switch (i)
                {
                    case 0:  min=image.getMinX(); max=min+image.getWidth();  break;
                    case 1:  min=image.getMinY(); max=min+image.getHeight(); break;
                    default: min=0; max=1; break;
                }
                // According OpenGIS specification, GridGeometry maps pixel's center.
                // We want a bounding box for all pixels, not pixel's centers. Offset by
                // 0.5 (use -0.5 for maximum too, not +0.5, since maximum is exclusive).
                envelope.setRange(i, envelope.getMinimum(i)-0.5, envelope.getMaximum(i)-0.5);
            }
            envelope = OpenGIS.transform(transform, envelope);
        }
        catch (TransformException exception)
        {
            final IllegalArgumentException e=new IllegalArgumentException(); // TODO
//----- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
//----- END OF JDK 1.4 DEPENDENCIES ---
            throw e;
        }
        final int dimension = envelope.getDimension();
        if (envelope.isEmpty() || dimension<2)
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_EMPTY_ENVELOPE));
        }
        if (dimension != cs.getDimension())
        {
            throw new MismatchedDimensionException(cs, envelope);
        }
        this.envelope = (Envelope)pool.intern(envelope);

        /*------------------------------------------------------------------------
         * Compute the grid geometry. If the specified math transform is non-null,
         * it will be used as is. Otherwise, it will be computed from the envelope.
         * A pool of shared grid geometries will be used in order to recycle existing
         * objects.
         */
        final GridRange    gridRange = (GridRange)pool.intern(new GridRange(image, dimension));
        final GridGeometry gridGeometry;
        if (transform==null)
        {
            // Should we invert some axis? For example, the 'y' axis is often inversed
            // (since image use a downward 'y' axis). If all source grid coverages use
            // the same axis orientations, we will reuse those orientations. Otherwise,
            // we will use default orientations where only the 'y' axis is inversed.
            boolean[] inverse = null;
            if (sources!=null)
            {
                for (int i=0; i<sources.length; i++)
                {
                    final boolean[] check = XArray.resize(sources[i].gridGeometry.areAxisInverted(), dimension);
                    if (inverse!=null)
                    {
                        if (!Arrays.equals(check, inverse))
                        {
                            inverse = null;
                            break;
                        }
                    }
                    else inverse = check;
                }
            }
            if (inverse==null)
            {
                inverse = new boolean[dimension];
                inverse[1] = true; // Inverse 'y' axis only.
            }
            gridGeometry = new GridGeometry(gridRange, envelope, inverse);
        }
        else gridGeometry = new GridGeometry(gridRange, transform);
        this.gridGeometry = (GridGeometry)pool.intern(gridGeometry);

        /*-------------------------------------------------------------------------------
         * Construct sample dimensions and the image.  We keep two versions of the image.
         * One is suitable for rendering (it uses integer pixels, which are rendered much
         * faster than float value),  and the other is suitable for computation (since it
         * uses real numbers).
         */
        final SampleDimension[] dimensions = new SampleDimension[numBands];
        for (int i=0; i<numBands; i++)
        {
            dimensions[i] = new GridSampleDimension(categories!=null ? categories[i] : null);
        }
        if (categories==null)
        {
            this.image = image;
            this.data  = image;
        }
        else if (isGeophysics)
        {
            final int   band  = 0; // TODO: make available as a parameter.
            final int[] bands = new int[]{band};

            final RenderedImage reducedImage = (bands.length==numBands && isIncreasing(bands)) ?
                    image : JAI.create("BandSelect", new ParameterBlock().addSource(image).add(bands));
            final CategoryList[] reducedCat = new CategoryList[bands.length];
            for (int i=0; i<bands.length; i++) reducedCat[i]=categories[bands[i]];
    
            this.data  = image;
            this.image = PlanarImage.wrapRenderedImage(CategoryList.toIndexed(reducedImage, reducedCat));
        }
        else
        {
            this.image = image;
            this.data  = PlanarImage.wrapRenderedImage(CategoryList.toValues(image, categories));
        }
        this.images           = USE_PYRAMID ? new ImageMIPMap(image, AffineTransform.getScaleInstance(DOWN_SAMPLER, DOWN_SAMPLER), null) : null;
        this.maxLevel         = Math.max((int) (Math.log((double)MIN_SIZE/(double)Math.max(image.getWidth(), image.getHeight()))/LOG_DOWN_SAMPLER), 0);
        this.sampleDimensions = (SampleDimension[]) dimensions.clone();
    }

    /**
     * Check if all numbers in <code>bands</code> are
     * increasing from 0 to <code>bands.length-1</code>.
     */
    private static boolean isIncreasing(final int[] bands)
    {
        for (int i=0; i<bands.length; i++)
            if (bands[i]!=i) return false;
        return true;
    }

    /**
     * Returns <code>true</code> if grid data can be edited. The default
     * implementation returns <code>true</code>  if  {@link #data} is an
     * instance of {@link WritableRenderedImage}.
     */
    public boolean isDataEditable()
    {return (data instanceof WritableRenderedImage);}

    /**
     * Returns the source data for a grid coverage. If the <code>GridCoverage</code>
     * was produced from an underlying dataset, the returned list is an empty list.
     * If the <code>GridCoverage</code> was produced using
     * {@link net.seagis.gp.GridCoverageProcessor} then it should return the source
     * grid coverage of the one used as input to <code>GridCoverageProcessor</code>.
     * In general the <code>getSource()</code> method is intended to return the original
     * <code>GridCoverage</code> on which it depends. This is intended to allow applications
     * to establish what <code>GridCoverage</code>s will be affected when others are updated,
     * as well as to trace back to the "raw data".
     */
    public GridCoverage[] getSources()
    {return (GridCoverage[]) sources.clone();}

    /**
     * Returns information for the grid coverage geometry. Grid geometry
     * includes the valid range of grid coordinates and the georeferencing.
     */
    public GridGeometry getGridGeometry()
    {return gridGeometry;}

    /**
     * Returns The bounding box for the coverage domain in coordinate
     * system coordinates.
     */
    public Envelope getEnvelope()
    {return (Envelope) envelope.clone();}

    /**
     * Retrieve sample dimension information for the coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension. A coverage must have at least one sample dimension.
     */
    public SampleDimension[] getSampleDimensions()
    {return (SampleDimension[]) sampleDimensions.clone();}

    /**
     * Returns a sequence of integer values for a given point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public int[] evaluate(final CoordinatePoint coord, final int[] dest) throws PointOutsideCoverageException
    {return evaluate(new Point2D.Double(coord.ord[0], coord.ord[1]), dest);}

    /**
     * Returns a sequence of float values for a given point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public float[] evaluate(final CoordinatePoint coord, final float[] dest) throws PointOutsideCoverageException
    {return evaluate(new Point2D.Double(coord.ord[0], coord.ord[1]), dest);}

    /**
     * Returns a sequence of double values for a given point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public double[] evaluate(final CoordinatePoint coord, final double[] dest) throws PointOutsideCoverageException
    {return evaluate(new Point2D.Double(coord.ord[0], coord.ord[1]), dest);}

    /**
     * Returns a sequence of integer values for a given two-dimensional point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public int[] evaluate(final Point2D coord, final int[] dest) throws PointOutsideCoverageException
    {
        final Point2D pixel = gridGeometry.inverseTransform(coord);
        final double fx = pixel.getX();
        final double fy = pixel.getY();
        if (!Double.isNaN(fx) && !Double.isNaN(fy))
        {
            final int x = (int)Math.round(fx);
            final int y = (int)Math.round(fy);
            if (data.getBounds().contains(x,y)) // getBounds() returns a cached instance.
            {
                return data.getTile(data.XToTileX(x), data.YToTileY(y)).getPixel(x, y, dest);
            }
        }
        throw new PointOutsideCoverageException(coord);
    }

    /**
     * Returns a sequence of float values for a given two-dimensional point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public float[] evaluate(final Point2D coord, final float[] dest) throws PointOutsideCoverageException
    {
        final Point2D pixel = gridGeometry.inverseTransform(coord);
        final double fx = pixel.getX();
        final double fy = pixel.getY();
        if (!Double.isNaN(fx) && !Double.isNaN(fy))
        {
            final int x = (int)Math.round(fx);
            final int y = (int)Math.round(fy);
            if (data.getBounds().contains(x,y)) // getBounds() returns a cached instance.
            {
                return data.getTile(data.XToTileX(x), data.YToTileY(y)).getPixel(x, y, dest);
            }
        }
        throw new PointOutsideCoverageException(coord);
    }

    /**
     * Returns a sequence of double values for a given two-dimensional point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public double[] evaluate(final Point2D coord, final double[] dest) throws PointOutsideCoverageException
    {
        final Point2D pixel = gridGeometry.inverseTransform(coord);
        final double fx = pixel.getX();
        final double fy = pixel.getY();
        if (!Double.isNaN(fx) && !Double.isNaN(fy))
        {
            final int x = (int)Math.round(fx);
            final int y = (int)Math.round(fy);
            if (data.getBounds().contains(x,y)) // getBounds() returns a cached instance.
            {
                return data.getTile(data.XToTileX(x), data.YToTileY(y)).getPixel(x, y, dest);
            }
        }
        throw new PointOutsideCoverageException(coord);
    }

    /**
     * Returns a debug string for the specified coordinate.   This method produces a
     * string with pixel coordinates and pixel values for all bands (with geophysics
     * values or category name in parenthesis). Example for a 1-banded image:
     *
     * <blockquote><pre>(1171,1566)=[196 (29.6 �C)]</pre></blockquote>
     *
     * @param  coord The coordinate point where to evaluate.
     * @return A string with pixel coordinates and pixel values at the specified location,
     *         or <code>null</code> if <code>coord</code> is outside coverage.
     */
    public synchronized String getDebugString(final CoordinatePoint coord)
    {
        final Point2D pixel = gridGeometry.inverseTransform(new Point2D.Double(coord.ord[0], coord.ord[1]));
        final int         x = (int)Math.round(pixel.getX());
        final int         y = (int)Math.round(pixel.getY());
        if (data.getBounds().contains(x,y)) // getBounds() returns a cached instance.
        {
            final int    numImageBands = image.getNumBands();
            final int  numNumericBands = data.getNumBands();
            final int         numBands = Math.max(numImageBands, numNumericBands);
            final Raster   imageRaster = image.getTile(image.XToTileX(x), image.YToTileY(y));
            final Raster numericRaster = data .getTile(data .XToTileX(x), data .YToTileY(y));
            final StringBuffer  buffer = new StringBuffer();
            buffer.append('(');
            buffer.append(x);
            buffer.append(',');
            buffer.append(y);
            buffer.append(")=[");

            for (int band=0; band<numBands; band++)
            {
                if (band!=0) buffer.append(";\u00A0");
                if (band<numImageBands)
                {
                    buffer.append(imageRaster.getSample(x, y, band));
                }
                if (band<numNumericBands)
                {
                    final CategoryList categories = sampleDimensions[band].getCategoryList();
                    if (categories!=null)
                    {
                        buffer.append("\u00A0(");
                        buffer.append(categories.format(numericRaster.getSampleDouble(x, y, band), null));
                        buffer.append(')');
                    }
                }
            }
            buffer.append(']');
            return buffer.toString();
        }
        else return null;
    }

    /**
     * Return a sequence of strongly typed values for a block.
     * A value for each sample dimension will be returned. The return value is an
     * <CODE>N+1</CODE> dimensional array, with dimensions. For 2 dimensional
     * grid coverages, this array will be accessed as (sample dimension, column,
     * row). The index values will be based from 0. The indices in the returned
     * <CODE>N</CODE> dimensional array will need to be offset by grid range
     * minimum coordinates to get equivalent grid coordinates.
     */
//  public abstract DoubleMultiArray getDataBlockAsDouble(final GridRange range)
//  {
        // TODO: Waiting for multiarray package (JSR-083)!
        //       Same for setDataBlock*
//  }

    /**
     * Returns grid data as a rendered image. If <code>geophysics</code> is <code>true</code>,
     * this method returns an image's view filled with "real world" data (e.g. temperature in
     * Celsius degres as floating point values). If <code>geophysics</code> is <code>false</code>,
     * then this method returns a "classical" image with integer pixel values.  The "geophysics"
     * view is better for computation, while the "classical" view is more suitable for rendering
     * on screen.
     *
     * If this <code>GridCoverage</code> hasn't been constructed with a <code>CategoryList[]</code>
     * argument, then the <code>geophysics</code> parameter has no effect.
     */
    public RenderedImage getRenderedImage(final boolean geophysics)
    {return geophysics ? data : image;}

    /**
     * Paint this grid coverage. The caller must ensure that <code>graphics</code>
     * has an affine transform mapping "real world" coordinates in the coordinate
     * system given by {@link #getCoordinateSystem}.
     */
    public void paint(final Graphics2D graphics)
    {
        final MathTransform2D mathTransform = gridGeometry.getGridToCoordinateSystem2D();
        if (!(mathTransform instanceof AffineTransform))
        {
            throw new UnsupportedOperationException("Non-affine transformations not yet implemented"); // TODO
        }
        final AffineTransform gridToCoordinate = (AffineTransform) mathTransform;
        if (images==null)
        {
            final AffineTransform transform = new AffineTransform(gridToCoordinate);
            transform.translate(-0.5, -0.5); // Map to upper-left corner.
            graphics.drawRenderedImage(image, transform);
        }
        else
        {
            /*
             * Calcule quel "niveau" d'image serait le plus appropri�.
             * Ce calcul est fait en fonction de la r�solution requise.
             */
            AffineTransform transform=graphics.getTransform();
            transform.concatenate(gridToCoordinate);
            final int level = Math.max(0,
                              Math.min(maxLevel,
                                       (int) (Math.log(Math.max(XAffineTransform.getScaleX0(transform),
                                                                XAffineTransform.getScaleY0(transform)))/LOG_DOWN_SAMPLER)));
            /*
             * Si on utilise une r�solution inf�rieure (pour un
             * affichage plus rapide), alors il faut utiliser un
             * g�or�f�rencement ajust� en cons�quence.
             */
            transform.setTransform(gridToCoordinate);
            if (level!=0)
            {
                final double scale=Math.pow(DOWN_SAMPLER, -level);
                transform.scale(scale, scale);
            }
            transform.translate(-0.5, -0.5); // Map to upper-left corner.
            graphics.drawRenderedImage(images.getImage(level), transform);
        }
    }

    /**
     * Hints that the given area may be needed in the near future. Some implementations
     * may spawn a thread or threads to compute the tiles while others may ignore the hint.
     *
     * @param area A rectangle indicating which geographic area to prefetch.
     *             This area's coordinates must be expressed according the
     *             grid coverage's coordinate system (as given by
     *             {@link #getCoordinateSystem}).
     */
    public void prefetch(final Rectangle2D area)
    {
        final Point[] tileIndices=image.getTileIndices(gridGeometry.inverseTransform(area));
        if (tileIndices!=null)
        {
            image.prefetchTiles(tileIndices);
        }
    }
}