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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.pt;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cs.AxisOrientation;

// Matrix and transforms
import org.opengis.pt.PT_Matrix;             // For JavaDoc
//import javax.vecmath.GMatrix;                // For JavaDoc (TODO)
//import javax.media.j3d.Transform3D;          // For JavaDoc (TODO)
import javax.media.jai.PerspectiveTransform; // For JavaDoc
import java.awt.geom.AffineTransform;

// Miscellaneous
import java.util.Arrays;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.FieldPosition;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * A two dimensional array of numbers.
 * <br><br>
 * <strong>NOTE: THIS CLASS MAY CHANGE IN INCOMPATIBLE WAY IN FUTURE RELEASE.</strong>
 * Many alternatives exist or are underway in Java (the Java3D {@link javax.vecmath.GMatrix}
 * class, the <A HREF="http://math.nist.gov/javanumerics/jama/">Jama matrix</A>,
 * <A HREF="http://jcp.org/jsr/detail/83.jsp">JSR-83 Multiarray package</A>, etc.).
 * It is not clear at this time how this <code>Matrix</code> class
 * will interoperate with those other matrix types.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.pt.PT_Matrix
 * @see java.awt.geom.AffineTransform
 * @see javax.media.jai.PerspectiveTransform
 * @see javax.media.j3d.Transform3D
 * @see javax.vecmath.GMatrix
 */
public final class Matrix implements Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 1738552446018677791L;

    /**
     * Elements of the matrix. Column indice vary fastest.
     */
    private final double[] elt;

    /**
     * The matrix size. Since the matrix is square,
     * the number of rows and the number of columns
     * are both equals to <code>size</code>.
     */
    private final int size;

    /**
     * Construct a square matrix of size
     * <code>size</code>&nbsp;x&nbsp;<code>size</code>.
     */
    public Matrix(final int size)
    {
        if (size>=0)
        {
            this.size = size;
            this.elt  = new double[size*size];
        }
        else throw new NegativeArraySizeException(String.valueOf(size));
    }

    /**
     * Construct a square matrix from
     * the specified affine transform.
     */
    public Matrix(final AffineTransform transform)
    {
        size = 3;
        elt  = new double[]
        {
            transform.getScaleX(), transform.getShearX(), transform.getTranslateX(),
            transform.getShearY(), transform.getScaleY(), transform.getTranslateY(),
                                0,                     0,                         1
        };
    }

    /**
     * Constructs a new matrix from a two-dimensional array of doubles.
     *
     * @throws IllegalArgumentException if the specified matrix is not square.
     */
    public Matrix(final double[][] matrix) throws IllegalArgumentException
    {
        size = matrix.length;
        elt  = new double[size*size];
        for (int j=0; j<size; j++)
        {
            if (matrix[j].length!=size)
            {
                throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_MATRIX_NOT_SQUARE));
            }
            System.arraycopy(matrix[j], 0, elt, j*size, size);
        }
    }

    /**
     * Construct an affine transform mapping a source region to a destination
     * region. The regions must have the same number of dimensions, but their
     * axis order and axis orientation may be different.
     *
     * @param srcRegion The source region.
     * @param srcAxis   Axis orientation for each dimension of the source region.
     * @param dstRegion The destination region.
     * @param dstAxis   Axis orientation for each dimension of the destination region.
     * @param validRegions   <code>true</code> if source and destination regions must
     *        be taken in account. If <code>false</code>, then source and destination
     *        regions will be ignored and may be null.
     */
    private Matrix(final Envelope srcRegion, final AxisOrientation[] srcAxis,
                   final Envelope dstRegion, final AxisOrientation[] dstAxis,
                   final boolean validRegions)
    {
        /*
         * Arguments check. NOTE: those exceptions are catched
         * by 'net.seagis.ct.CoordinateTransformationFactory'.
         * If exception type change, update the factory class.
         */
        final int dimension = srcAxis.length;
        if (dstAxis.length != dimension)
        {
            throw new MismatchedDimensionException(dimension, dstAxis.length);
        }
        if (validRegions)
        {
            srcRegion.ensureDimensionMatch(dimension);
            dstRegion.ensureDimensionMatch(dimension);
        }
        this.size = dimension+1;
        this.elt  = new double[size*size];
        /*
         * Map source axis to destination axis.  If no axis is moved (for example if the user
         * want to transform (NORTH,EAST) to (SOUTH,EAST)), then source and destination index
         * will be equal.   If some axis are moved (for example if the user want to transform
         * (NORTH,EAST) to (EAST,NORTH)),  then ordinates at index <code>srcIndex</code> will
         * have to be moved at index <code>dstIndex</code>.
         */
        for (int srcIndex=0; srcIndex<dimension; srcIndex++)
        {
            boolean hasFound = false;
            final AxisOrientation srcAxe = srcAxis[srcIndex];
            final AxisOrientation search = srcAxe.absolute();
            for (int dstIndex=0; dstIndex<dimension; dstIndex++)
            {
                final AxisOrientation dstAxe = dstAxis[dstIndex];
                if (search.equals(dstAxe.absolute()))
                {
                    if (hasFound)
                    {
                        throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_COLINEAR_AXIS_$2,
                                                           srcAxe.getName(null), dstAxe.getName(null)));
                    }
                    hasFound = true;
                    /*
                     * Set the matrix elements. Some matrix elements will never
                     * be set. They will be left to zero, which is their wanted
                     * value.
                     */
                    final boolean normal = srcAxe.equals(dstAxe);
                    double scale     = (normal) ? +1 : -1;
                    double translate = 0;
                    if (validRegions)
                    {
                        translate  = (normal) ? dstRegion.getMinimum(dstIndex) : dstRegion.getMaximum(dstIndex);
                        scale     *= dstRegion.getLength (dstIndex) / srcRegion.getLength (srcIndex);
                        translate -= srcRegion.getMinimum(srcIndex)*scale;
                    }
                    set(dstIndex, srcIndex,  scale);
                    set(dstIndex, dimension, translate);
                }
            }
            if (!hasFound)
            {
                throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NO_DESTINATION_AXIS_$1, srcAxis[srcIndex].getName(null)));
            }
        }
        set(dimension, dimension, 1);
    }

    /**
     * Construct an affine transform changing axis order and/or orientation.
     * For example, the affine transform may convert (NORTH,WEST) coordinates
     * into (EAST,NORTH). Axis orientation can be inversed only. For example,
     * it is illegal to transform (NORTH,WEST) coordinates into (NORTH,DOWN).
     *
     * @param  srcAxis The set of axis orientation for source coordinate system.
     * @param  dstAxis The set of axis orientation for destination coordinate system.
     * @throws MismatchedDimensionException if <code>srcAxis</code> and <code>dstAxis</code> don't have the same length.
     * @throws IllegalArgumentException if the affine transform can't be created for some other raison.
     */
    public static Matrix createAffineTransform(final AxisOrientation[] srcAxis, final AxisOrientation[] dstAxis)
    {return new Matrix(null, srcAxis, null, dstAxis, false);}

    /**
     * Construct an affine transform that maps
     * a source region to a destination region.
     * Axis order and orientation are left unchanged.
     *
     * @param  srcRegion The source region.
     * @param  dstRegion The destination region.
     * @throws MismatchedDimensionException if regions don't have the same dimension.
     */
    public static Matrix createAffineTransform(final Envelope srcRegion, final Envelope dstRegion)
    {
        final int dimension = srcRegion.getDimension();
        dstRegion.ensureDimensionMatch(dimension);
        final Matrix matrix = new Matrix(dimension+1);
        for (int i=0; i<dimension; i++)
        {
            final double scale     = dstRegion.getLength (i) / srcRegion.getLength (i);
            final double translate = dstRegion.getMinimum(i) - srcRegion.getMinimum(i)*scale;
            matrix.set(i, i,         scale);
            matrix.set(i, dimension, translate);
        }
        matrix.set(dimension, dimension, 1);
        return matrix;
    }

    /**
     * Construct an affine transform mapping a source region to a destination
     * region. Axis order and/or orientation can be changed during the process.
     * For example, the affine transform may convert (NORTH,WEST) coordinates
     * into (EAST,NORTH). Axis orientation can be inversed only. For example,
     * it is illegal to transform (NORTH,WEST) coordinates into (NORTH,DOWN).
     *
     * @param  srcRegion The source region.
     * @param  srcAxis   Axis orientation for each dimension of the source region.
     * @param  dstRegion The destination region.
     * @param  dstAxis   Axis orientation for each dimension of the destination region.
     * @throws MismatchedDimensionException if all arguments don't have the same dimension.
     * @throws IllegalArgumentException if the affine transform can't be created for some other raison.
     */
    public static Matrix createAffineTransform(final Envelope srcRegion, final AxisOrientation[] srcAxis,
                                               final Envelope dstRegion, final AxisOrientation[] dstAxis)
    {return new Matrix(srcRegion, srcAxis, dstRegion, dstAxis, true);}

    /**
     * Returns the size of this matrix. Since the matrix is square,
     * the number of rows and the number of columns are both equals
     * to <code>size</code>.
     */
    public int getSize()
    {return size;}

    /**
     * Returns a matrix element.
     *
     * @param  j The 0-based row number.
     * @param  i The 0-based column number.
     * @return The matrix element.
     */
    public double get(final int j, final int i)
    {
        if (j<0 || j>=size) throw new IndexOutOfBoundsException(String.valueOf(j));
        if (i<0 || i>=size) throw new IndexOutOfBoundsException(String.valueOf(i));
        return elt[j*size+i];
    }

    /**
     * Set a matrix element.
     *
     * @param j The 0-based row number.
     * @param i The 0-based column number.
     * @param value The new value.
     */
    public void set(final int j, final int i, final double value)
    {
        if (j<0 || j>=size) throw new IndexOutOfBoundsException(String.valueOf(j));
        if (i<0 || i>=size) throw new IndexOutOfBoundsException(String.valueOf(i));
        elt[j*size+i] = value;
    }

    /**
     * Retrieves the specifiable values in the transformation matrix into a
     * 2-dimensional array of double precision values. The values are stored
     * into the 2-dimensional array using the row index as the first subscript
     * and the column index as the second. Values are copied; changes to the
     * returned array will not change this matrix.
     *
     * @see org.opengis.pt.PT_Matrix#elt
     */
    public double[][] getElements()
    {
        final double[][] matrix = new double[size][];
        for (int j=0; j<size; j++)
        {
            matrix[j] = new double[size];
            System.arraycopy(elt, j*size, matrix[j], 0, size);
        }
        return matrix;
    }

    /**
     * Returns <code>true</code> if this matrix is an affine transform.
     * A transform is affine if the last row contains only zeros, except
     * in the last column which contains 1.
     */
    public boolean isAffine()
    {
        final int dimension=size-1;
        int index = dimension*size;
        for (int i=0; i<=dimension; i++)
            if (elt[index++] != (i==dimension ? 1 : 0))
                return false;
        return true;
    }

    /**
     * Returns <code>true</code> if this matrix is an identity matrix.
     */
    public boolean isIdentity()
    {
        int index=0;
        for (int j=0; j<size; j++)
            for (int i=0; i<size; i++)
                if (elt[index++] != (i==j ? 1 : 0))
                    return false;
        return true;
    }

    /**
     * Returns a new matrix that is the product
     * of this matrix by the specified matrix.
     */
    public Matrix multiply(final Matrix matrix)
    {
        if (size!=matrix.size)
        {
            throw new IllegalArgumentException();
        }
        int index0 = 0;
        final Matrix dest = new Matrix(size);
        for (int j=0; j<size; j++)
        {
            for (int i=0; i<size; i++)
            {
                int index1 = j*size;
                int index2 = i;
                double sum = 0;
                for (int k=0; k<size; k++)
                {
                    sum    += elt[index1++] * matrix.elt[index2];
                    index2 += size;
                }
                dest.elt[index0++] = sum;
            }
        }
        return dest;
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     * The two coordinate array sections can be exactly the same or can
     * be overlapping sections of the same array without affecting the
     * validity of the results.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of points to be transformed
     */
    public void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts)
    {
        final int size = this.size-1; // The last ordinate will be assumed 1.
        if (srcPts==dstPts && srcOff-size<dstOff && (srcOff+numPts*size)>dstOff)
        {
            // If source overlaps destination  (taking in account the 'size' elements of
            // 'srcPts' that need to be read many times), then the easiest workaround is
            // to copy source data. This is not the most efficient however...
            srcPts = new float[numPts*size];
            System.arraycopy(dstPts, srcOff, srcPts, 0, numPts*size);
            srcOff = 0;
        }
        while (--numPts>=0)
        {
            int mix=0;
            for (int j=0; j<size; j++)
            {
                double sum=elt[mix+size];
                for (int i=0; i<size; i++)
                {
                    sum += srcPts[srcOff+i]*elt[mix++];
                }
                dstPts[dstOff++] = (float) sum;
                mix++;
            }
            srcOff += size;
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     * The two coordinate array sections can be exactly the same or can
     * be overlapping sections of the same array without affecting the
     * validity of the results.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of points to be transformed
     */
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
    {
        final int size = this.size-1; // The last ordinate will be assumed 1.
        if (srcPts==dstPts && srcOff-size<dstOff && (srcOff+numPts*size)>dstOff)
        {
            // If source overlaps destination  (taking in account the 'size' elements of
            // 'srcPts' that need to be read many times), then the easiest workaround is
            // to copy source data. This is not the most efficient however...
            srcPts = new double[numPts*size];
            System.arraycopy(dstPts, srcOff, srcPts, 0, numPts*size);
            srcOff = 0;
        }
        while (--numPts>=0)
        {
            int mix=0;
            for (int j=0; j<size; j++)
            {
                double sum=elt[mix+size];
                for (int i=0; i<size; i++)
                {
                    sum += srcPts[srcOff+i]*elt[mix++];
                }
                dstPts[dstOff++] = sum;
                mix++;
            }
            srcOff += size;
        }
    }

    /**
     * Returns an affine transform for this matrix.
     * This is a convenience method for interoperability with Java2D.
     *
     * @throws IllegalStateException if this matrix is not 3x3,
     *         or if the last row is not [0 0 1].
     */
    public AffineTransform toAffineTransform2D() throws IllegalStateException
    {
        if (elt.length == 9)
        {
            if (elt[6]==0 && elt[7]==0 && elt[8]==1)
            {
                return new AffineTransform(elt[0], elt[3], elt[1], elt[4], elt[2], elt[5]);
            }
            throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_NOT_AN_AFFINE_TRANSFORM));
        }
        throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_NOT_TWO_DIMENSIONAL_$1, new Integer(size-1)));
    }

    /**
     * Returns a hash value for this coordinate.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        long code=2563217;
        for (int i=elt.length; --i>=0;)
        {
            code = code*37 + Double.doubleToLongBits(elt[i]);
        }
        return (int)(code >>> 32) ^ (int)code;
    }

    /**
     * Compares the specified object
     * with this matrix for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof Matrix)
        {
            final Matrix that = (Matrix) object;
            return Arrays.equals(this.elt, that.elt);
        }
        return false;
    }

    /**
     * Returns a copy of this matrix.
     */
    public Object clone()
    {
        final Matrix copy = new Matrix(size);
        System.arraycopy(elt, 0, copy.elt, 0, elt.length);
        return copy;
    }

    /**
     * Returns a string representation of this matrix.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes only.
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        final int      columnWidth = 12;
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final FieldPosition  dummy = new FieldPosition(0);
        final NumberFormat  format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(6);
        format.setMaximumFractionDigits(6);
        for (int j=0; j<size; j++)
        {
            for (int i=0; i<size; i++)
            {
                final int position = buffer.length();
                buffer = format.format(get(j,i), buffer, dummy);
                buffer.insert(position, Utilities.spaces(columnWidth-(buffer.length()-position)));
            }
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
