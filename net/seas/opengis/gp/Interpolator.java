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
package net.seas.opengis.gp;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.cv.PointOutsideCoverageException;

// Images
import java.awt.image.Raster;
import javax.media.jai.PlanarImage;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

// Parameters
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.ParameterListDescriptorImpl;

// Geometry
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Miscellaneous
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Array;
import net.seas.util.Version;
import net.seas.resources.Resources;


/**
 * A grid coverage using an {@link Interpolation} for evaluating points.
 * This interpolator <strong>do not work</strong>  for nearest-neighbor
 * interpolation (use the standard {@link GridCoverage} class for that).
 * It should work for other kinds of interpolation however.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Interpolator extends GridCoverage
{
    /**
     * Affine transform from "real world" coordinates to grid coordinates.
     * This transform maps coordinates to pixel <em>centers</em>.
     */
    private final AffineTransform toGrid;

    /**
     * The interpolation method.
     */
    private final Interpolation interpolation;

    /**
     * Second interpolation method to use if this one failed.
     * May be <code>null</code> if there is no fallback.  By
     * convention, <code>this</code> means that interpolation
     * should fallback on <code>super.evaluate(...)</code>
     * (i.e. nearest neighbor).
     */
    private final Interpolator fallback;

    /**
     * Image bounds. Bounds have been reduced
     * by {@link Interpolation}'s padding.
     */
    private final int xmin, ymin, xmax, ymax;

    /**
     * Interpolation padding.
     */
    private final int right, bottom;

    /**
     * Arrays to use for passing arguments to interpolation.
     * This array will be constructed only when first needed.
     */
    private transient double[][] doubles;

    /**
     * Arrays to use for passing arguments to interpolation.
     * This array will be constructed only when first needed.
     */
    private transient float[][] floats;

    /**
     * Arrays to use for passing arguments to interpolation.
     * This array will be constructed only when first needed.
     */
    private transient int[][] ints;

    /**
     * Construct a new interpolator.
     *
     * @param  coverage The coverage to interpolate.
     * @param  interpolations The interpolations to use and its fallback (if any).
     */
    public static GridCoverage create(GridCoverage coverage, final Interpolation[] interpolations)
    {
        if (coverage instanceof Interpolator)
        {
            coverage = coverage.getSources().get(0);
        }
        if (interpolations.length==0 || (interpolations[0] instanceof InterpolationNearest))
        {
            return coverage;
        }
        return new Interpolator(coverage, interpolations, 0);
    }

    /**
     * Construct a new interpolator for the specified interpolation.
     *
     * @param  coverage The coverage to interpolate.
     * @param  interpolations The interpolations to use and its fallback
     *         (if any). This array must have at least 1 element.
     * @param  index The index of interpolation to use in the <code>interpolations</code> array.
     */
    private Interpolator(final GridCoverage coverage, final Interpolation[] interpolations, final int index)
    {
        super(coverage);
        this.interpolation = interpolations[index];
        if (index+1 < interpolations.length)
        {
            if (interpolations[index+1] instanceof InterpolationNearest)
            {
                // By convention, 'fallback==this' is for 'super.evaluate(...)'
                // (i.e. "NearestNeighbor").
                this.fallback = this;
            }
            else this.fallback = new Interpolator(coverage, interpolations, index+1);
        }
        else this.fallback = null;
        /*
         * Compute the affine transform from "real world" coordinates  to grid coordinates.
         * This transform maps coordinates to pixel <em>centers</em>. If this transform has
         * already be created during fallback construction, reuse the fallback's instance
         * instead of creating a new identical one.
         */
        if (fallback!=null && fallback!=this)
        {
            this.toGrid = fallback.toGrid;
        }
        else try
        {
            final AffineTransform transform = gridGeometry.getGridToCoordinateSystem2D();
            // Note: If we want nearest-neighbor interpolation,
            //       we need to add the following line:
            //
            //       transform.translate(-0.5, -0.5);
            //
            //       This is because we need to cancel the last 'translate(0.5, 0.5)' that appear in
            //       'getGridToCoordinateSystem2D()' (we must remember that OpenGIS's transform maps
            //       pixel CENTER, while JAI transforms maps pixel UPPER LEFT corner).   For exemple
            //       the  (12.4, 18.9)  coordinates still lies on the [12,9] pixel.  Since the JAI's
            //       nearest-neighbor interpolation use 'Math.floor' operation instead of 'Math.round',
            //       we must follow this convention.
            //
            //       For other kinds of interpolation, we want to maps pixel values to pixel center.
            //       For example, coordinate (12.5, 18.5) (in floating-point coordinates) lies at the
            //       center of pixel [12,18] (in integer coordinates);  the evaluated value should be
            //       the exact pixel's value. On the other hand, coordinate (12.5, 19) (in floating-
            //       point coordinates) lies exactly at the edge between pixels [12,19] and [12,20];
            //       the evaluated value should be a mid-value between those two pixels. If we want
            //       center of mass located at pixel centers, we must keep the (0.5, 0.5) translation
            //       provided by 'getGridToCoordinateSystem2D()' for interpolation other than nearest-
            //       neighbor.
            toGrid = transform.createInverse();
        }
        catch (NoninvertibleTransformException exception)
        {
            final IllegalArgumentException e = new IllegalArgumentException();
            if (Version.MINOR>=4) e.initCause(exception);
            throw e;
        }

        final int left   = interpolation.getLeftPadding();
        final int right  = interpolation.getRightPadding();
        final int top    = interpolation.getTopPadding();
        final int bottom = interpolation.getBottomPadding();

        this.right  = right;
        this.bottom = bottom;

        final int x = data.getMinX();
        final int y = data.getMinY();

        this.xmin = x + left;
        this.ymin = y + top;
        this.xmax = x + data.getWidth()  - right;
        this.ymax = y + data.getHeight() - bottom;
    }

    /**
     * Returns interpolations. The first array's element is the
     * interpolation for this grid coverage. Other elements (if
     * any) are fallbacks.
     */
    public Interpolation[] getInterpolations()
    {
        final List<Interpolation> interp = new ArrayList<Interpolation>();
        Interpolator scan = this;
        do
        {
            interp.add(interpolation);
            if (scan.fallback==scan)
            {
                interp.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                break;
            }
            scan = scan.fallback;
        }
        while (scan!=null);
        return interp.toArray(new Interpolation[interp.size()]);
    }

    /**
     * Return an sequence of integer values for a given two-dimensional point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public int[] evaluate(final Point2D coord, int[] dest) throws PointOutsideCoverageException
    {
        if (fallback!=null)
        {
            dest = super.evaluate(coord, dest);
        }
        final Point2D pixel = toGrid.transform(coord, null);
        final double x = pixel.getX();
        final double y = pixel.getY();
        if (!Double.isNaN(x) && !Double.isNaN(y))
        {
            if (interpolate(x, y, dest, 0, data.getNumBands()))
            {
                return dest;
            }
        }
        throw new PointOutsideCoverageException(coord);
    }

    /**
     * Return an sequence of float values for a given two-dimensional point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public float[] evaluate(final Point2D coord, float[] dest) throws PointOutsideCoverageException
    {
        if (fallback!=null)
        {
            dest = super.evaluate(coord, dest);
        }
        final Point2D pixel = toGrid.transform(coord, null);
        final double x = pixel.getX();
        final double y = pixel.getY();
        if (!Double.isNaN(x) && !Double.isNaN(y))
        {
            if (interpolate(x, y, dest, 0, data.getNumBands()))
            {
                return dest;
            }
        }
        throw new PointOutsideCoverageException(coord);
    }

    /**
     * Return an sequence of double values for a given two-dimensional point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public double[] evaluate(final Point2D coord, double[] dest) throws PointOutsideCoverageException
    {
        if (fallback!=null)
        {
            dest = super.evaluate(coord, dest);
        }
        final Point2D pixel = toGrid.transform(coord, null);
        final double x = pixel.getX();
        final double y = pixel.getY();
        if (!Double.isNaN(x) && !Double.isNaN(y))
        {
            if (interpolate(x, y, dest, 0, data.getNumBands()))
            {
                return dest;
            }
        }
        throw new PointOutsideCoverageException(coord);
    }

    /**
     * Interpolate at the specified position. If <code>fallback!=null</code>,
     * then <code>dest</code> <strong>must</strong> have been initialized with
     * <code>super.evaluate(...)</code> prior to invoking this method.
     *
     * @param x      The x position in pixel's coordinates.
     * @param y      The y position in pixel's coordinates.
     * @param dest   The destination array, or null.
     * @param band   The first band's index to interpolate.
     * @param bandUp The last band's index+1 to interpolate.
     * @return <code>false</code> if point is outside grid coverage.
     */
    private boolean interpolate(final double x, final double y, int[] dest, int band, final int bandUp)
    {
        final double x0 = Math.floor(x);
        final double y0 = Math.floor(y);
        int ix = (int)x0;
        int iy = (int)y0;
        if (!(ix>=xmin && ix<xmax && iy>=ymin && iy<ymax))
        {
            if (fallback==null) return false;
            if (fallback==this) return true; // super.evaluate(...) succeed prior to this method call.
            return fallback.interpolate(x, y, dest, band, bandUp);
        }
        /*
         * Create buffers, if not already created.
         */
        int[][] samples = ints;
        if (samples==null)
        {
            final int rowCount = interpolation.getHeight();
            final int colCount = interpolation.getWidth();
            ints = samples = new int[rowCount][];
            for (int i=0; i<rowCount; i++)
                samples[i] = new int[colCount];
        }
        /*
         * Interpolate all bands. TODO: Would it be more efficient to use RectIter?
         * We are going to read very few points, and we don't know how costly is
         * RectIterFactory.create(...).
         */
        ix += right;
        iy += bottom;
        if (dest==null)
            dest=new int[bandUp];
        for (; band<bandUp; band++)
        {
            for (int sy=iy,j=samples.length; --j>=0; --sy)
            {
                final int[] row=samples[j];
                final int ty=data.YToTileY(sy);
                for (int sx=ix,i=row.length; --i>=0; --sx)
                {
                    final int tx=data.XToTileX(sx);
                    row[i] = data.getTile(tx, ty).getSample(sx, sy, band);
                }
            }
            final int xfrac = (int) ((x-x0) * (1 << interpolation.getSubsampleBitsH()));
            final int yfrac = (int) ((y-y0) * (1 << interpolation.getSubsampleBitsV()));
            dest[band] = interpolation.interpolate(samples, xfrac, yfrac);
        }
        return true;
    }

    /**
     * Interpolate at the specified position. If <code>fallback!=null</code>,
     * then <code>dest</code> <strong>must</strong> have been initialized with
     * <code>super.evaluate(...)</code> prior to invoking this method.
     *
     * @param x      The x position in pixel's coordinates.
     * @param y      The y position in pixel's coordinates.
     * @param dest   The destination array, or null.
     * @param band   The first band's index to interpolate.
     * @param bandUp The last band's index+1 to interpolate.
     * @return <code>false</code> if point is outside grid coverage.
     */
    private boolean interpolate(final double x, final double y, float[] dest, int band, final int bandUp)
    {
        final double x0 = Math.floor(x);
        final double y0 = Math.floor(y);
        int ix = (int)x0;
        int iy = (int)y0;
        if (!(ix>=xmin && ix<xmax && iy>=ymin && iy<ymax))
        {
            if (fallback==null) return false;
            if (fallback==this) return true; // super.evaluate(...) succeed prior to this method call.
            return fallback.interpolate(x, y, dest, band, bandUp);
        }
        /*
         * Create buffers, if not already created.
         */
        float[][] samples = floats;
        if (samples==null)
        {
            final int rowCount = interpolation.getHeight();
            final int colCount = interpolation.getWidth();
            floats = samples = new float[rowCount][];
            for (int i=0; i<rowCount; i++)
                samples[i] = new float[colCount];
        }
        /*
         * Interpolate all bands. TODO: Would it be more efficient to use RectIter?
         * We are going to read very few points, and we don't know how costly is
         * RectIterFactory.create(...).
         */
        ix += right;
        iy += bottom;
        if (dest==null)
            dest=new float[bandUp];
        for (; band<bandUp; band++)
        {
            for (int sy=iy,j=samples.length; --j>=0; --sy)
            {
                final float[] row=samples[j];
                final int ty=data.YToTileY(sy);
                for (int sx=ix,i=row.length; --i>=0; --sx)
                {
                    final int tx=data.XToTileX(sx);
                    row[i] = data.getTile(tx, ty).getSampleFloat(sx, sy, band);
                }
            }
            final float value=interpolation.interpolate(samples, (float)(x-x0), (float)(y-y0));
            if (Float.isNaN(value))
            {
                if (fallback==this) continue; // 'dest' was set by 'super.evaluate(...)'.
                if (fallback!=null)
                {
                    fallback.interpolate(x, y, dest, band, band+1);
                    continue;
                }
                // If no fallback was specified, then 'dest' is not required to
                // have been initialized. It may contains random value.  Set it
                // to the NaN value...
            }
            dest[band] = value;
        }
        return true;
    }

    /**
     * Interpolate at the specified position. If <code>fallback!=null</code>,
     * then <code>dest</code> <strong>must</strong> have been initialized with
     * <code>super.evaluate(...)</code> prior to invoking this method.
     *
     * @param x      The x position in pixel's coordinates.
     * @param y      The y position in pixel's coordinates.
     * @param dest   The destination array, or null.
     * @param band   The first band's index to interpolate.
     * @param bandUp The last band's index+1 to interpolate.
     * @return <code>false</code> if point is outside grid coverage.
     */
    private boolean interpolate(final double x, final double y, double[] dest, int band, final int bandUp)
    {
        final double x0 = Math.floor(x);
        final double y0 = Math.floor(y);
        int ix = (int)x0;
        int iy = (int)y0;
        if (!(ix>=xmin && ix<xmax && iy>=ymin && iy<ymax))
        {
            if (fallback==null) return false;
            if (fallback==this) return true; // super.evaluate(...) succeed prior to this method call.
            return fallback.interpolate(x, y, dest, band, bandUp);
        }
        /*
         * Create buffers, if not already created.
         */
        double[][] samples = doubles;
        if (samples==null)
        {
            final int rowCount = interpolation.getHeight();
            final int colCount = interpolation.getWidth();
            doubles = samples = new double[rowCount][];
            for (int i=0; i<rowCount; i++)
                samples[i] = new double[colCount];
        }
        /*
         * Interpolate all bands. TODO: Would it be more efficient to use RectIter?
         * We are going to read very few points, and we don't know how costly is
         * RectIterFactory.create(...).
         */
        ix += right;
        iy += bottom;
        if (dest==null)
            dest=new double[bandUp];
        for (; band<bandUp; band++)
        {
            for (int sy=iy,j=samples.length; --j>=0; --sy)
            {
                final double[] row=samples[j];
                final int ty=data.YToTileY(sy);
                for (int sx=ix,i=row.length; --i>=0; --sx)
                {
                    final int tx=data.XToTileX(sx);
                    row[i] = data.getTile(tx, ty).getSampleDouble(sx, sy, band);
                }
            }
            final double value=interpolation.interpolate(samples, (float)(x-x0), (float)(y-y0));
            if (Double.isNaN(value))
            {
                if (fallback==this) continue; // 'dest' was set by 'super.evaluate(...)'.
                if (fallback!=null)
                {
                    fallback.interpolate(x, y, dest, band, band+1);
                    continue;
                }
                // If no fallback was specified, then 'dest' is not required to
                // have been initialized. It may contains random value.  Set it
                // to the NaN value...
            }
            dest[band] = value;
        }
        return true;
    }




    /**
     * The "Interpolate" operation. This operation specifies the interpolation type
     * to be used to interpolate values for points which fall between grid cells.
     * The default value is nearest neighbor. The new interpolation type operates
     * on all sample dimensions. See package description for more details.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static final class Operation extends net.seas.opengis.gp.Operation
    {
        /**
         * List of valid names. Note: the "Optimal" type is not
         * implemented because currently not provided by JAI.
         */
        private static final String[] NAMES=
        {
            "NearestNeighbor",
            "Bilinear",
            "Bicubic"
        };

        /**
         * Interpolation types (provided by Java
         * Advanced Imaging) for {@link #NAMES}.
         */
        private static final int[] TYPES=
        {
            Interpolation.INTERP_NEAREST,
            Interpolation.INTERP_BILINEAR,
            Interpolation.INTERP_BICUBIC
        };

        /**
         * Construct an "Interpolate" operation.
         */
        public Operation()
        {
            super("Interpolate", new ParameterListDescriptorImpl(
                  null,         // the object to be reflected upon for enumerated values.
                  new String[]  // the names of each parameter.
                  {
                      "Source",
                      "Type"
                  },
                  new Class[]   // the class of each parameter.
                  {
                      GridCoverage.class,
                      Object.class
                  },
                  new Object[] // The default values for each parameter.
                  {
                      ParameterListDescriptor.NO_PARAMETER_DEFAULT,
                      NAMES[0] // "NearestNeighbor"
                  },
                  null // Defines the valid values for each parameter.
            ));
        }

        /**
         * Cast the specified object to an {@link Interpolation object}.
         *
         * @param  type The interpolation type as an {@link Interpolation} or a {@link CharSequence} object.
         * @return The interpolation object for the specified type.
         * @throws IllegalArgumentException if the specified interpolation type is not a know one.
         */
        private static Interpolation cast(final Object type)
        {
            if (type instanceof Interpolation)
            {
                return (Interpolation) type;
            }
            else if ((Version.MINOR>=4) ? (type instanceof CharSequence) : (type instanceof String))
            {
                final String name=type.toString();
                for (int i=0; i<NAMES.length; i++)
                    if (NAMES[i].equalsIgnoreCase(name))
                        return Interpolation.getInstance(TYPES[i]);
            }
            throw new IllegalArgumentException(Resources.format(Clé.UNKNOW_INTERPOLATION¤1, type));
        }

        /**
         * Apply an interpolation to a grid coverage. This method is invoked
         * by {@link GridCoverageProcessor} for the "Interpolate" operation.
         */
        protected GridCoverage doOperation(final ParameterList parameters)
        {
            final GridCoverage   source = (GridCoverage)parameters.getObjectParameter("Source");
            final Object           type =               parameters.getObjectParameter("Type"  );
            final Interpolation[] interpolations;
            if (type.getClass().isArray())
            {
                interpolations = new Interpolation[Array.getLength(type)];
                for (int i=0; i<interpolations.length; i++)
                    interpolations[i] = cast(Array.get(type, i));
            }
            else
            {
                interpolations = new Interpolation[] {cast(type)};
            }
            return create(source, interpolations);
        }
    }
}
