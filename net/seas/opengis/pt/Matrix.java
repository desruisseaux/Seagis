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
package net.seas.opengis.pt;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.AxisOrientation;

// Matrix and transforms
import javax.vecmath.GMatrix;                // For JavaDoc
import javax.media.j3d.Transform3D;          // For JavaDoc
import java.awt.geom.AffineTransform;
import javax.media.jai.PerspectiveTransform; // For JavaDoc

// Miscellaneous
import java.util.Arrays;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.FieldPosition;
import net.seas.resources.Resources;
import net.seas.util.XString;


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
    private static final long serialVersionUID = -3301877120221179009L;

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
                throw new IllegalArgumentException(Resources.format(Clé.MATRIX_NOT_SQUARE));
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
         * Arguments check. NOTE: those exceptions are catched by
         * 'net.seas.opengis.ct.CoordinateTransformationFactory'.
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
                        throw new IllegalArgumentException(Resources.format(Clé.NON_ORTHOGONAL_AXIS¤2,
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
                throw new IllegalArgumentException(Resources.format(Clé.NO_DESTINATION_FOR_AXIS¤1, srcAxis[srcIndex].getName(null)));
            }
        }
        set(dimension, dimension, 1);
    }

    /**
     * <FONT COLOR="#FF6633">Construct an affine transform changing axis order and/or orientation.</FONT>
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
     * <FONT COLOR="#FF6633">Construct an affine transform that maps
     * a source region to a destination region.</FONT> Axis order and
     * orientation are left unchanged.
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
     * <FONT COLOR="#FF6633">Construct an affine transform mapping a source region to a destination
     * region.</FONT> Axis order and/or orientation can be changed during the process.
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
     * and the column index as the second.
     */
    public double[][] getMatrix()
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
     * <FONT COLOR="#FF6633">Returns an affine transform for this matrix.</FONT>
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
            throw new IllegalStateException(Resources.format(Clé.MATRIX_NOT_AFFINE));
        }
        throw new IllegalStateException(Resources.format(Clé.NOT_TWO_DIMENSIONAL¤1, new Integer(size-1)));
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
                buffer.insert(position, XString.spaces(columnWidth-(buffer.length()-position)));
            }
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
