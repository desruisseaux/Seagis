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
import net.seas.util.Version;


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
     * May be <code>null</code> if there is no fallback.
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
     * Construct a new interpolator from a standard interpolation.
     *
     * @param coverage The coverage to interpolate.
     * @param type The interpolation type. One of
     *        {@link Interpolation#INTERP_BILINEAR},
     *        {@link Interpolation#INTERP_BICUBIC} or
     *        {@link Interpolation#INTERP_BICUBIC_2}.
     */
    public static Interpolator create(final GridCoverage coverage, final int type)
    {
        final Interpolator fallback;
        switch (type)
        {
            case Interpolation.INTERP_NEAREST  : throw new IllegalArgumentException();
            case Interpolation.INTERP_BICUBIC  : // fall through
            case Interpolation.INTERP_BICUBIC_2: fallback=create(coverage, Interpolation.INTERP_BILINEAR); break;
            case Interpolation.INTERP_BILINEAR : // fall through
            default:                             fallback=null; break;
        }
        return new Interpolator(coverage, Interpolation.getInstance(type), fallback);
    }

    /**
     * Construct a new interpolator for the specified interpolation.
     *
     * @param coverage The coverage to interpolate.
     * @param interpolation The interpolation to use.
     * @param fallback The fallback interpolator. This interpolator will be
     *                 used if the current one failed to interpolate a value.
     *                 May be <code>null</code> if there is no fallback.
     */
    public Interpolator(final GridCoverage coverage, final Interpolation interpolation, final Interpolator fallback)
    {
        super(coverage);
        this.interpolation = interpolation;
        this.fallback      = fallback;
        try
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

        this.xmin = data.getMinX()        + left;
        this.ymin = data.getMinY()        + top;
        this.xmax = data.getWidth() +xmin - right;
        this.ymax = data.getHeight()+ymin - bottom;
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
        dest = super.evaluate(coord, dest);
        final Point2D pixel = toGrid.transform(coord, null);
        final double x = pixel.getX();
        final double y = pixel.getY();
        if (!Double.isNaN(x) && !Double.isNaN(y))
        {
            interpolate(x, y, dest, 0, data.getNumBands());
            if (false) throw new PointOutsideCoverageException(coord); // TODO
        }
        return dest;
    }

    /**
     * Interpolate at the specified position.
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
            if (fallback!=null)
            {
                return fallback.interpolate(x, y, dest, band, bandUp);
            }
            else return false;
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
                if (fallback!=null)
                {
                    fallback.interpolate(x, y, dest, band, band+1);
                    continue;
                }
                continue; // TODO: if (fallbackAllowed)
            }
            dest[band] = value;
        }
        return true;
    }

    /**
     * The operation.
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
         * Construct an operation.
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
         * Apply a process operation to a grid coverage. This method
         * is invoked by {@link GridCoverageProcessor}.
         *
         * @param  parameters List of name value pairs for the parameters required for the operation.
         * @return The result as a grid coverage.
         */
        protected GridCoverage doOperation(final ParameterList parameters)
        {
            final GridCoverage   source = (GridCoverage)parameters.getObjectParameter("Source");
            final Object           type =               parameters.getObjectParameter("Type"  );
            Interpolation interpolation = null;
            if (type instanceof Interpolation)
            {
                interpolation = (Interpolation) type;
            }
            else
            {
                final String name=type.toString();
                for (int i=0; i<NAMES.length; i++)
                    if (NAMES[i].equalsIgnoreCase(name))
                        interpolation = Interpolation.getInstance(TYPES[i]);
            }
            if (interpolation!=null)
            {
                throw new IllegalArgumentException(String.valueOf(type));
            }
            // TODO: fallback, nearest neighbor...
            return new Interpolator(source, interpolation, null);
        }
    }
}
