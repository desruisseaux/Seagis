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
//import javax.vecmath.GMatrix;                // For JavaDoc
//import javax.media.j3d.Transform3D;          // For JavaDoc
import javax.media.jai.PerspectiveTransform; // For JavaDoc
import java.awt.geom.AffineTransform;

// Miscellaneous
import java.util.Arrays;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.FieldPosition;
//import javax.vecmath.SingularMatrixException;

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
 * will leverage their work or interoperate with those other matrix types.
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
    private static final long serialVersionUID = -5179250712400337217L;

    /**
     * Elements of the matrix. Column indice vary fastest.
     */
    private final double[] elt;

    /**
     * the number of rows.
     */
    private final int rows;

    /**
     * the number of columns.
     */
    private final int columns;

    /**
     * Construct a square matrix of size
     * <code>size</code>&nbsp;&times;&nbsp;<code>size</code>.
     */
    public Matrix(final int size)
    {this(size,size);}

    /**
     * Construct a matrix of size
     * <code>rows</code>&nbsp;&times;&nbsp;<code>columns</code>.
     */
    public Matrix(final int rows, final int columns)
    {
        if (rows < 0)
        {
            throw new NegativeArraySizeException(String.valueOf(rows));
        }
        if (columns < 0)
        {
            throw new NegativeArraySizeException(String.valueOf(columns));
        }
        this.rows    = rows;
        this.columns = columns;
        this.elt     = new double[rows*columns];
    }

    /**
     * Construct a square matrix from
     * the specified affine transform.
     */
    public Matrix(final AffineTransform transform)
    {
        rows = columns = 3;
        elt  = new double[]
        {
            transform.getScaleX(), transform.getShearX(), transform.getTranslateX(),
            transform.getShearY(), transform.getScaleY(), transform.getTranslateY(),
                                0,                     0,                         1
        };
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isAffine();
------- END OF JDK 1.4 DEPENDENCIES ---*/
    }

    /**
     * Constructs a new matrix from a two-dimensional array of doubles.
     *
     * @throws IllegalArgumentException if the specified matrix is not regular
     *         (i.e. if all rows doesn't have the same length).
     */
    public Matrix(final double[][] matrix) throws IllegalArgumentException
    {
        rows    = matrix.length;
        columns = (rows!=0) ? matrix[0].length : 0;
        elt     = new double[rows*columns];
        for (int j=0; j<rows; j++)
        {
            if (matrix[j].length!=columns)
            {
                throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_MATRIX_NOT_REGULAR));
            }
            System.arraycopy(matrix[j], 0, elt, j*columns, columns);
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
        rows = columns = dimension+1;
        elt  = new double[rows*columns];
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
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isAffine();
------- END OF JDK 1.4 DEPENDENCIES ---*/
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
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert matrix.isAffine();
------- END OF JDK 1.4 DEPENDENCIES ---*/
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
     * Returns the number of rows in this matrix.
     */
    public int getNumRows()
    {return rows;}

    /**
     * Returns the number of columns in this matrix.
     */
    public int getNumColumns()
    {return columns;}

    /**
     * Returns a matrix element.
     *
     * @param  j The 0-based row number.
     * @param  i The 0-based column number.
     * @return The matrix element.
     */
    public double get(final int j, final int i)
    {
        if (j<0 || j>=rows)    throw new IndexOutOfBoundsException(String.valueOf(j));
        if (i<0 || i>=columns) throw new IndexOutOfBoundsException(String.valueOf(i));
        return elt[j*columns+i];
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
        if (j<0 || j>=rows)    throw new IndexOutOfBoundsException(String.valueOf(j));
        if (i<0 || i>=columns) throw new IndexOutOfBoundsException(String.valueOf(i));
        elt[j*columns+i] = value;
    }

    /**
     * Copies a sub-matrix derived from this matrix into the target matrix.
     * The upper left of the sub-matrix is located at (<code>rowSource</code>, <code>colSource</code>);
     * The sub-matrix is copied into the the target matrix starting at (<code>rowDest</code>, <code>colDest</code>).
     *
     * @param rowSource The top-most row of the sub-matrix
     * @param colSource The left-most column of the sub-matrix
     * @param numRow    The number of rows in the sub-matrix
     * @param numCol    The number of columns in the sub-matrix
     * @param rowDest   The top-most row of the position of the copied sub-matrix within the target matrix
     * @param colDest   The left-most column of the position of the copied sub-matrix within the target matrix
     * @param target    The matrix into which the sub-matrix will be copied
     */
    public void copySubMatrix(int rowSource, final int colSource,
                              int numRow,    final int numCol,
                              int rowDest,   final int colDest, final Matrix target)
    {
        if (numRow<0 || numCol<0 || rowSource<0 || colSource<0 || rowDest<0 || colDest<0 ||
            rowSource + numRow >   this.rows || colSource + numCol >   this.columns ||
            rowDest   + numRow > target.rows || colDest   + numCol > target.columns)
        {
            throw new IllegalArgumentException();
        }
        rowSource = rowSource * this.columns + colSource;
        rowDest   = rowDest * target.columns + colDest;
        while (--numRow >= 0)
        {
            System.arraycopy(elt, rowSource, target.elt, rowDest, numCol);
            rowSource += this.columns;
            rowDest += target.columns;
        }
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
        final double[][] matrix = new double[rows][];
        for (int j=0; j<rows; j++)
        {
            matrix[j] = new double[columns];
            System.arraycopy(elt, j*columns, matrix[j], 0, columns);
        }
        return matrix;
    }

    /**
     * Returns <code>true</code> if this matrix is an affine transform.
     * A transform is affine if the matrix is square and last row contains
     * only zeros, except in the last column which contains 1.
     */
    public boolean isAffine()
    {
        if (rows != columns)
            return false;

        final int dimension=rows-1;
        int index = dimension*rows;
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
        if (rows != columns)
            return false;

        int index=0;
        for (int j=0; j<rows; j++)
            for (int i=0; i<columns; i++)
                if (elt[index++] != (i==j ? 1 : 0))
                    return false;
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isAffine();
------- END OF JDK 1.4 DEPENDENCIES ---*/
        return true;
    }

    /**
     * Returns a new matrix that is the product
     * of this matrix by the specified matrix.
     */
    public Matrix multiply(final Matrix matrix)
    {
        if (columns != matrix.rows)
        {
            throw new IllegalArgumentException();
        }
        int index0 = 0;
        final Matrix dest = new Matrix(rows, matrix.columns);
        for (int j=0; j<rows; j++)
        {
            for (int i=0; i<matrix.columns; i++)
            {
                int index1 = j*columns;
                int index2 = i;
                double sum = 0;
                for (int k=0; k<columns; k++)
                {
                    sum    += elt[index1++] * matrix.elt[index2];
                    index2 += matrix.columns;
                }
                dest.elt[index0++] = sum;
            }
        }
        return dest;
    }

    /**
     * Transforms an array of floating point coordinates by this matrix. Point coordinates
     * must have a dimension equals to <code>{@link #getNumColumns}-1</code>. For example,
     * for square matrix of size 4&times;4, coordinate points are three-dimensional and
     * stored in the arrays starting at the specified offset (<code>srcOff</code>) in the order
     * <code>[x<sub>0</sub>, y<sub>0</sub>, z<sub>0</sub>,
     *        x<sub>1</sub>, y<sub>1</sub>, z<sub>1</sub>...,
     *        x<sub>n</sub>, y<sub>n</sub>, z<sub>n</sub>]</code>.
     *
     * The transformed points <code>(x',y',z')</code> are computed as below
     * (note that this computation is similar to {@link PerspectiveTransform}):
     *
     * <blockquote><pre>
     * [ u ]     [ m<sub>00</sub>  m<sub>01</sub>  m<sub>02</sub>  m<sub>03</sub> ] [ x ]
     * [ v ]  =  [ m<sub>10</sub>  m<sub>11</sub>  m<sub>12</sub>  m<sub>13</sub> ] [ y ]
     * [ w ]     [ m<sub>20</sub>  m<sub>21</sub>  m<sub>22</sub>  m<sub>23</sub> ] [ z ]
     * [ t ]     [ m<sub>30</sub>  m<sub>31</sub>  m<sub>32</sub>  m<sub>33</sub> ] [ 1 ]
     *
     *   x' = u/t
     *   y' = v/t
     *   y' = w/t
     * </pre></blockquote>
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored
     *               in the destination array. The source and destination array sections can
     *               be overlaps.
     * @param numPts The number of points to be transformed
     */
    public void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts)
    {
        final int  inputDimension = columns-1; // The last ordinate will be assumed equals to 1.
        final int outputDimension = rows-1;
        final double[]     buffer = new double[rows];
        if (srcPts==dstPts)
        {
            // We are going to write in the source array. Checks if
            // source and destination sections are going to clash.
            final int upperSrc = srcOff + numPts*inputDimension;
            if (upperSrc > dstOff)
            {
                if (inputDimension >= outputDimension ? dstOff > srcOff :
                              dstOff + numPts*outputDimension > upperSrc)
                {
                    // If source overlaps destination, then the easiest workaround is
                    // to copy source data. This is not the most efficient however...
                    srcPts = new float[numPts*inputDimension];
                    System.arraycopy(dstPts, srcOff, srcPts, 0, srcPts.length);
                    srcOff = 0;
                }
            }
        }
        while (--numPts>=0)
        {
            int mix=0;
            for (int j=0; j<rows; j++)
            {
                double sum=elt[mix + inputDimension];
                for (int i=0; i<inputDimension; i++)
                {
                    sum += srcPts[srcOff+i]*elt[mix++];
                }
                buffer[j] = sum;
                mix++;
            }
            final double w = buffer[outputDimension];
            for (int j=0; j<outputDimension; j++)
            {
                // 'w' is equals to 1 if the transform is affine.
                dstPts[dstOff++] = (float) (buffer[j]/w);
            }
            srcOff += inputDimension;
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix. Point coordinates
     * must have a dimension equals to <code>{@link #getNumColumns}-1</code>. For example,
     * for square matrix of size 4&times;4, coordinate points are three-dimensional and
     * stored in the arrays starting at the specified offset (<code>srcOff</code>) in the order
     * <code>[x<sub>0</sub>, y<sub>0</sub>, z<sub>0</sub>,
     *        x<sub>1</sub>, y<sub>1</sub>, z<sub>1</sub>...,
     *        x<sub>n</sub>, y<sub>n</sub>, z<sub>n</sub>]</code>.
     *
     * The transformed points <code>(x',y',z')</code> are computed as below
     * (note that this computation is similar to {@link PerspectiveTransform}):
     *
     * <blockquote><pre>
     * [ u ]     [ m<sub>00</sub>  m<sub>01</sub>  m<sub>02</sub>  m<sub>03</sub> ] [ x ]
     * [ v ]  =  [ m<sub>10</sub>  m<sub>11</sub>  m<sub>12</sub>  m<sub>13</sub> ] [ y ]
     * [ w ]     [ m<sub>20</sub>  m<sub>21</sub>  m<sub>22</sub>  m<sub>23</sub> ] [ z ]
     * [ t ]     [ m<sub>30</sub>  m<sub>31</sub>  m<sub>32</sub>  m<sub>33</sub> ] [ 1 ]
     *
     *   x' = u/t
     *   y' = v/t
     *   y' = w/t
     * </pre></blockquote>
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored
     *               in the destination array. The source and destination array sections can
     *               be overlaps.
     * @param numPts The number of points to be transformed
     */
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
    {
        final int  inputDimension = columns-1; // The last ordinate will be assumed equals to 1.
        final int outputDimension = rows-1;
        final double[]     buffer = new double[rows];
        if (srcPts==dstPts)
        {
            // We are going to write in the source array. Checks if
            // source and destination sections are going to clash.
            final int upperSrc = srcOff + numPts*inputDimension;
            if (upperSrc > dstOff)
            {
                if (inputDimension >= outputDimension ? dstOff > srcOff :
                              dstOff + numPts*outputDimension > upperSrc)
                {
                    // If source overlaps destination, then the easiest workaround is
                    // to copy source data. This is not the most efficient however...
                    srcPts = new double[numPts*inputDimension];
                    System.arraycopy(dstPts, srcOff, srcPts, 0, srcPts.length);
                    srcOff = 0;
                }
            }
        }
        while (--numPts>=0)
        {
            int mix=0;
            for (int j=0; j<rows; j++)
            {
                double sum=elt[mix + inputDimension];
                for (int i=0; i<inputDimension; i++)
                {
                    sum += srcPts[srcOff+i]*elt[mix++];
                }
                buffer[j] = sum;
                mix++;
            }
            final double w = buffer[outputDimension];
            for (int j=0; j<outputDimension; j++)
            {
                // 'w' is equals to 1 if the transform is affine.
                dstPts[dstOff++] = buffer[j]/w;
            }
            srcOff += inputDimension;
        }
    }

    /**
     * Inverts this matrix in place.
     * <strong>Note: this method is not yet implemented</strong>.
     */
    public void invert()
    {
        if (!isIdentity())
        {
            throw new RuntimeException("Not yet implemented");
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
        int check;
        if ((check=rows)!=3 || (check=columns)!=3)
        {
            throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_NOT_TWO_DIMENSIONAL_$1, new Integer(check-1)));
        }
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert elt.length==9;
------- END OF JDK 1.4 DEPENDENCIES ---*/
        if (elt[6]==0 && elt[7]==0 && elt[8]==1)
        {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            assert isAffine();
------- END OF JDK 1.4 DEPENDENCIES ---*/
            return new AffineTransform(elt[0], elt[3], elt[1], elt[4], elt[2], elt[5]);
        }
        throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_NOT_AN_AFFINE_TRANSFORM));
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
        final Matrix copy = new Matrix(rows, columns);
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
        for (int j=0; j<rows; j++)
        {
            for (int i=0; i<columns; i++)
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
