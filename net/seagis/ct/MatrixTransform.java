/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seagis.ct;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;

// Miscellaneous
import java.io.Serializable;
import net.seagis.pt.Matrix;
import net.seagis.pt.CoordinatePoint;
import javax.media.jai.ParameterList;
//import javax.vecmath.SingularMatrixException;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;
import net.seagis.resources.XAffineTransform;


/**
 * Transforms multi-dimensional coordinate points using an {@link Matrix}.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
final class MatrixTransform extends AbstractMathTransform implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 784363941978607191L;

    /**
     * The matrix.
     */
    private final Matrix matrix;

    /**
     * Construct a transform.
     */
    protected MatrixTransform(final Matrix matrix)
    {this.matrix = (Matrix) matrix.clone();}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts)
    {matrix.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts)
    {matrix.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Gets the derivative of this transform at a point.
     * For a matrix transform, the derivative is the
     * same everywhere.
     */
    public Matrix derivative(final Point2D point)
    {return derivative((CoordinatePoint)null);}

    /**
     * Gets the derivative of this transform at a point.
     * For a matrix transform, the derivative is the
     * same everywhere.
     */
    public Matrix derivative(final CoordinatePoint point)
    {
        final int rows = matrix.getNumRows()-1;
        final int cols = matrix.getNumColumns()-1;
        final Matrix deriv = new Matrix(rows, cols);
        matrix.copySubMatrix(0,0,rows,cols,0,0,deriv);
        return deriv;
    }

    /**
     * Returns the matrix.
     */
    public Matrix getMatrix()
    {return (Matrix) matrix.clone();}

    /**
     * Gets the dimension of input points.
     */
    public int getDimSource()
    {return matrix.getNumColumns()-1;}

    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget()
    {return matrix.getNumRows()-1;}

    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity()
    {return matrix.isIdentity();}

    /**
     * Creates the inverse transform of this object.
     */
    public MathTransform inverse() throws NoninvertibleTransformException
    {
        if (isIdentity()) return this;
        final MatrixTransform inverse = new MatrixTransform(matrix);
        try
        {
            inverse.matrix.invert();
        }
        catch (RuntimeException exception)
        {
            NoninvertibleTransformException e = new NoninvertibleTransformException(exception.getLocalizedMessage());
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            throw e;
        }
        return inverse;
    }

    /**
     * Returns a hash value for this transform.
     */
    public int hashCode()
    {return matrix.hashCode();}

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final MatrixTransform that = (MatrixTransform) object;
            return Utilities.equals(this.matrix, that.matrix);
        }
        return false;
    }

    /**
     * Returns the WKT for this math transform.
     */
    public String toString()
    {return toString(matrix);}

    /**
     * Returns the WKT for an affine transform
     * using the specified matrix.
     */
    static String toString(final Matrix matrix)
    {
        final int numRow = matrix.getNumRows();
        final int numCol = matrix.getNumColumns();
        final StringBuffer buffer = new StringBuffer("PARAM_MT[\"Affine\"");
        final StringBuffer eltBuf = new StringBuffer("elt_");
        addParameter(buffer, "Num_row", numRow);
        addParameter(buffer, "Num_col", numCol);
        for (int j=0; j<numRow; j++)
        {
            for (int i=0; i<numCol; i++)
            {
                final double value = matrix.get(j,i);
                if (value != (i==j ? 1 : 0))
                {
                    eltBuf.setLength(4);
                    eltBuf.append(j);
                    eltBuf.append('_');
                    eltBuf.append(i);
                    addParameter(buffer, eltBuf.toString(), value);
                }
            }
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * The provider for {@link MatrixTransform}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static final class Provider extends MathTransformProvider
    {
        /**
         * Create a provider for affine transforms of the specified
         * dimension. Created affine transforms will have a size of
         * <code>numRow&nbsp;&times;&nbsp;numCol</code>.
         *
         * @param numRow The number of matrix's rows.
         * @param numCol The number of matrix's columns.
         */
        public Provider(final int numRow, final int numCol)
        {
            super("Affine", ResourceKeys.AFFINE_TRANSFORM, null);
            put("Num_row", numRow, POSITIVE_RANGE);
            put("Num_col", numCol, POSITIVE_RANGE);
            final StringBuffer buffer=new StringBuffer("elt_");
            for (int j=0; j<=numRow; j++)
            {
                for (int i=0; i<=numCol; i++)
                {
                    buffer.setLength(4);
                    buffer.append(j);
                    buffer.append('_');
                    buffer.append(i);
                    put(buffer.toString(), (i==j) ? 1 : 0, null);
                }
            }
        }

        /**
         * Returns a transform for the specified parameters.
         *
         * @param  parameters The parameter values in standard units.
         * @return A {@link MathTransform} object of this classification.
         */
        public MathTransform create(final ParameterList parameters)
        {return staticCreate(parameters);}

        /**
         * Static version of {@link #create}, for use by
         * {@link MathTransformFactory#createParameterizedTransform}.
         */
        public static MathTransform staticCreate(final ParameterList parameters)
        {
            final int numRow = parameters.getIntParameter("Num_row");
            final int numCol = parameters.getIntParameter("Num_col");
            final Matrix matrix = new Matrix(numRow, numCol);
            for (int i=Math.min(numRow, numCol); --i>=0;)
            {
                matrix.set(i,i,1);
            }
            final String[] names = parameters.getParameterListDescriptor().getParamNames();
            if (names!=null)
            {
                for (int i=0; i<names.length; i++)
                {
                    final String name = names[i];
                    if (name.regionMatches(true, 0, "elt_", 0, 4))
                    {
                        final int separator = name.lastIndexOf('_');
                        final int row = Integer.parseInt(name.substring(4, separator));
                        final int col = Integer.parseInt(name.substring(separator+1));
                        matrix.set(row, col, parameters.getDoubleParameter(name));
                    }
                }
            }
            if (numRow==3 && matrix.isAffine())
            {
                return new AffineTransform2D(matrix.toAffineTransform2D());
            }
            return new MatrixTransform(matrix);
        }
    }
}
