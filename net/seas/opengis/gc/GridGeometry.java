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
package net.seas.opengis.gc;

// OpenGIS (SEAGIS) dependencies
import net.seas.opengis.pt.Matrix;
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.Dimensioned;
import net.seas.opengis.ct.MathTransform;
import net.seas.opengis.ct.MathTransformFactory;
import net.seas.opengis.pt.MismatchedDimensionException;

// Geometry
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.AffineTransform;
import net.seas.util.XAffineTransform;
import net.seas.util.XDimension2D;
import net.seas.util.XClass;

// Miscellaneous
import java.io.Serializable;


/**
 * Describes the valid range of grid coordinates and the math
 * transform to transform grid coordinates to real world coordinates.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public class GridGeometry implements Dimensioned, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 256746343396114708L;

    /**
     * The valid coordinate range of a grid coverage. The lowest
     * valid grid coordinate is zero. A grid with 512 cells can
     * have a minimum coordinate of 0 and maximum of 512, with 511
     * as the highest valid index.
     */
    private final GridRange gridRange;

    /**
     * Transformation affine convertissant les indices de pixels de l'image en coordonnées logiques (en
     * mètres ou en degrés selon le système de coordonnées de l'image). Pour convertir des indices de
     * pixels en coordonnées logiques, il suffit d'écrire:
     *
     * <pre>gridToCoordinateJAI.transform(pixels, point);</pre>
     *
     * Notez que la coordonnées obtenue sera dans le coin supérieur gauche du pixel (vers les indices
     * <var>i</var> et <var>j</var> minimums). Pour obtenir des coordonnées au centre du pixel, il faut
     * d'abord appeller <code>geoReferencing.translate(0.5, 0.5)</code> avant de faire les transformations,
     * ou encore ajouter 0.5 aux coordonnées pixels <code>pixels.x</code> et <code>pixels.y</code>.
     */
    private final AffineTransform gridToCoordinateJAI;

    /**
     * The math transform. If <code>null</code>, will be constructed
     * from <code>gridToCoordinateJAI</code> upon request.
     */
    private MathTransform gridToCoordinateSystem;

    /**
     * Construct a new grid geometry from a math transform.
     *
     * @param gridRange The valid coordinate range of a grid coverage.
     * @param gridToCoordinateSystem The math transform which allows for the transformations
     *        from grid coordinates (pixel's <em>center</em>) to real world earth coordinates.
     */
    public GridGeometry(final GridRange gridRange, final MathTransform gridToCoordinateSystem)
    {
        this.gridRange              = gridRange;
        this.gridToCoordinateSystem = gridToCoordinateSystem;
        this.gridToCoordinateJAI    = null;

        final int dimRange  = gridRange.getDimension();
        final int dimSource = gridToCoordinateSystem.getDimSource();
        final int dimTarget = gridToCoordinateSystem.getDimTarget();
        if (dimRange != dimSource)
        {
            throw new MismatchedDimensionException(dimRange, dimSource);
        }
        if (dimRange != dimTarget)
        {
            throw new MismatchedDimensionException(dimRange, dimTarget);
        }
    }

    /**
     * Construct a new grid geometry from an affine transform.
     * This constructor is provided for interoperability with
     * Java Advanced Imaging.
     *
     * @param gridRange The valid coordinate range of a grid coverage.
     * @param gridToCoordinateJAI The affine transform which allows for the transformations
     *        from grid coordinates (pixel's <em>upper left</em> corner) to real world earth
     *        coordinates.
     */
    GridGeometry(final GridRange gridRange, final AffineTransform gridToCoordinateJAI)
    {
        final int dimension = gridRange.getDimension();
        if (dimension != 2)
        {
            throw new MismatchedDimensionException(dimension, 2);
        }
        this.gridRange           = gridRange;
        this.gridToCoordinateJAI = new AffineTransform(gridToCoordinateJAI);
    }

    /**
     * Construct a new grid geometry. An affine transform will be computed automatically
     * from the specified envelope.  The <code>inverse</code> argument tells whatever or
     * not an axis should be inversed. Callers will typically set <code>inverse[1]</code>
     * to <code>true</code> in order to inverse the <var>y</var> axis.
     *
     * @param gridRange The valid coordinate range of a grid coverage.
     * @param userRange The corresponding coordinate range in user coordinate.
     *                  This rectangle must contains entirely all pixels, i.e.
     *                  the rectangle's upper left corner must coincide with
     *                  the upper left corner of the first pixel and the rectangle's
     *                  lower right corner must coincide with the lower right corner
     *                  of the last pixel.
     * @param inverse   Tells whatever or not inverse axis. A <code>null</code> value
     *                  inverse no axis.
     */
    public GridGeometry(final GridRange gridRange, final Envelope userRange, final boolean[] inverse)
    {
        this.gridRange = gridRange;
        /*
         * Check arguments validity.
         * Dimensions must match.
         */
        final int dimension = gridRange.getDimension();
        if (userRange.getDimension() != dimension)
        {
            throw new MismatchedDimensionException(gridRange, userRange);
        }
        if (inverse!=null && inverse.length!=dimension)
        {
            throw new MismatchedDimensionException(dimension, inverse.length);
        }
        /*
         * Prepare elements for the 2D sub-transform.
         * Those elements will be set during the matrix
         * setup below.
         */
        double scaleX = 1;
        double scaleY = 1;
        double transX = 0;
        double transY = 0;
        /*
         * Setup the multi-dimensional affine transform for use with OpenGIS.
         * According OpenGIS's specification, transform must map pixel center.
         * This is done by adding 0.5 to grid coordinates.
         */
        final Matrix matrix = new Matrix(dimension+1);
        matrix.set(dimension, dimension, 1);
        for (int i=0; i<dimension; i++)
        {
            double scale = userRange.getLength(i) / gridRange.getLength(i);
            double trans;
            if (inverse==null || !inverse[i])
            {
                trans = userRange.getMinimum(i);
            }
            else
            {
                scale = -scale;
                trans = userRange.getMaximum(i);
            }
            trans -= scale*gridRange.getLower(i);
            matrix.set(i, i,         scale);
            matrix.set(i, dimension, trans - 0.5*scale);
            /*
             * Keep two-dimensional components for the AffineTransform. According
             * Java Advanced Imaging specification, transforms must map upper left
             * pixel's corner. This is why we do NOT add 0.5 in the translation
             * term below.
             */
            switch (i)
            {
                case 0: scaleX=scale; transX=trans; break;
                case 1: scaleY=scale; transY=trans; break;
            }
        }
        this.gridToCoordinateSystem = MathTransformFactory.DEFAULT.createAffineTransform(matrix);
        this.gridToCoordinateJAI    = new AffineTransform(scaleX, 0, 0, scaleY, transX, transY);
    }
    
    /**
     * Construct a new two-dimensional grid geometry. A math transform will
     * be computed automatically with an inverted <var>y</var> axis (i.e.
     * <code>gridRange</code> and <code>userRange</code> are assumed to
     * have <var>y</var> axis in opposite direction).
     *
     * @param gridRange The valid coordinate range of a grid coverage.
     *                  Increasing <var>x</var> values goes right and
     *                  increasing <var>y</var> values goes <strong>down</strong>.
     * @param userRange The corresponding coordinate range in user coordinate.
     *                  Increasing <var>x</var> values goes right and
     *                  increasing <var>y</var> values goes <strong>up</strong>.
     *                  This rectangle must contains entirely all pixels, i.e.
     *                  the rectangle's upper left corner must coincide with
     *                  the upper left corner of the first pixel and the rectangle's
     *                  lower right corner must coincide with the lower right corner
     *                  of the last pixel.
     */
    public GridGeometry(final Rectangle gridRange, final Rectangle2D userRange)
    {
        final double scaleX = userRange.getWidth()  / gridRange.getWidth();
        final double scaleY = userRange.getHeight() / gridRange.getHeight();
        final double transX = userRange.getMinX()   - gridRange.x*scaleX;
        final double transY = userRange.getMaxY()   + gridRange.y*scaleY;
        this.gridRange           = new GridRange(gridRange);
        this.gridToCoordinateJAI = new AffineTransform(scaleX, 0, 0, -scaleY, transX, transY);
    }

    /**
     * Returns the number of dimensions.
     */
    public int getDimension()
    {return gridRange.getDimension();}

    /**
     * Returns the valid coordinate range of a grid coverage.
     * The lowest valid grid coordinate is zero. A grid with
     * 512 cells can have a minimum coordinate of 0 and maximum
     * of 512, with 511 as the highest valid index.
     */
    public GridRange getGridRange()
    {return gridRange;}

    /**
     * Returns the math transform which allows  for the transformations from grid
     * coordinates to real world earth coordinates.     The transform is often an
     * affine transformation. The coordinate system of the real world coordinates
     * is given by {@link net.seas.opengis.cv.Coverage#getCoordinateSystem}. If no
     * math transform is available, this method returns <code>null</code>.
     */
    public synchronized MathTransform getGridToCoordinateSystem()
    {
        if (gridToCoordinateSystem==null)
        {
            if (gridToCoordinateJAI!=null)
            {
                // AffineTransform's operations are applied in reverse order.
                // We translate the grid coordinate by (0.5,0.5) first (which
                // set the position in the pixel center),  and then apply the
                // transformation specified by gridToCoordinateJAI().
                final AffineTransform tr = new AffineTransform(gridToCoordinateJAI);
                tr.translate(0.5, 0.5);
                gridToCoordinateSystem = MathTransformFactory.DEFAULT.createAffineTransform(tr);
            }
        }
        return gridToCoordinateSystem;
    }

    /**
     * Returns the affine transform which allows for the transformations
     * from grid coordinates to real world earth coordinates. The returned affine follows
     * <A HREF="http://java.sun.com/products/java-media/jai/">Java Advanced Imaging</A>
     * convention, i.e. its convert the pixel's <em>upper left corner</em> coordinates
     * (<var>i</var>,<var>j</var>) into real world earth coordinates (<var>x</var>,<var>y</var>).
     * In contrast, {link #gridToCoordinateSystem()} contert the pixel's <em>center</em>
     * coordinates into real world earth coordinates.
     */
    final AffineTransform getGridToCoordinateJAI()
    {
        if (gridToCoordinateJAI==null)
        {
            throw new UnsupportedOperationException("Not implemented");
        }
        return gridToCoordinateJAI; // No clone for performance raisons.
    }

    /**
     * Returns a hash value for this grid geometry.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {return gridToCoordinateSystem.hashCode()*37 + gridRange.hashCode();}

    /**
     * Compares the specified object with
     * this grid geometry for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof GridGeometry)
        {
            final GridGeometry that = (GridGeometry) object;
            return XClass.equals(this.gridRange,              that.gridRange             ) &&
                   XClass.equals(this.gridToCoordinateSystem, that.gridToCoordinateSystem) &&
                   XClass.equals(this.gridToCoordinateJAI,    that.gridToCoordinateJAI   );
        }
        else return false;
    }

    /**
     * Returns a string représentation of this grid range.
     * The returned string is implementation dependent. It
     * is usually provided for debugging purposes.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(gridRange);
        buffer.append(", ");
        buffer.append(gridToCoordinateSystem);
        buffer.append(']');
        return buffer.toString();
    }
}
