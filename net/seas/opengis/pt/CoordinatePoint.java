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

// Miscellaneous
import java.util.Arrays;
import java.io.Serializable;
import java.awt.geom.Point2D;
import net.seas.util.XClass;
import net.seas.resources.Resources;


/**
 * A position defined by a list of numbers. The ordinate values are
 * indexed from <code>0</code> to <code>(numDim-1)</code>, where
 * <code>numDim</code> is the dimension of the coordinate system
 * the coordinate point belongs in.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see Point2D
 * @see org.opengis.pt.PT_CoordinatePoint
 */
public final class CoordinatePoint implements Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 8733694401519122222L;

    /**
     * The ordinates of the coordinate point.
     */
    public final double[] ord;

    /**
     * Construct a coordinate with the
     * specified number of dimensions.
     *
     * @param  numDim Number of dimensions.
     * @throws NegativeArraySizeException if <code>numDim</code> is negative.
     */
    public CoordinatePoint(final int numDim) throws NegativeArraySizeException
    {ord = new double[numDim];}

    /**
     * Construct a coordinate with the specified ordinates.
     * The <code>ord</code> array will be copied.
     */
    public CoordinatePoint(final double[] ord)
    {this.ord = (double[]) ord.clone();}

    /**
     * Construct a 2D coordinate from
     * the specified ordinates.
     */
    public CoordinatePoint(final double x, final double y)
    {ord = new double[] {x,y};}

    /**
     * Construct a 3D coordinate from
     * the specified ordinates.
     */
    public CoordinatePoint(final double x, final double y, final double z)
    {ord = new double[] {x,y,z};}

    /**
     * Construct a coordinate from
     * the specified {@link Point2D}.
     */
    public CoordinatePoint(final Point2D point)
    {this(point.getX(), point.getY());}

    /**
     * Returns the ordinate value along the specified dimension.
     * This is equivalent to <code>{@link #ord ord}[dimension]</code>.
     */
    public final double getOrdinate(final int dimension)
    {return ord[dimension];}

    /**
     * The number of ordinates of a <code>CoordinatePoint</code>.
     * This is equivalent to <code>{@link #ord ord}.length</code>.
     */
    public final int getDimension()
    {return ord.length;}

    /**
     * Convenience method for checking the point's dimension validity.
     * This method is usually call for argument checking.
     *
     * @param  expectedDimension Expected dimension for this point.
     * @throws IllegalArgumentException if this point doesn't have the expected dimension.
     */
    public final void ensureDimensionMatch(final int expectedDimension) throws IllegalArgumentException
    {
        final int dimension = getDimension();
        if (dimension != expectedDimension)
        {
            throw new IllegalArgumentException(Resources.format(Clé.MISMATCHED_DIMENSION¤2,
                                               new Integer(dimension), new Integer(expectedDimension)));
        }
    }

    /**
     * Returns a hash value for this coordinate.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {return hashCode(ord);}

    /**
     * Returns a hash value for the specified ordinates.
     */
    static int hashCode(final double[] ord)
    {
        long code=0;
        if (ord!=null)
        {
            for (int i=ord.length; --i>=0;)
                code = (code << 1) ^ Double.doubleToLongBits(ord[i]);
        }
        return (int)(code >>> 32) ^ (int)code;
    }

    /**
     * Compares the specified object with
     * this coordinate for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof CoordinatePoint)
        {
            final CoordinatePoint that = (CoordinatePoint) object;
            return Arrays.equals(this.ord, that.ord);
        }
        return false;
    }

    /**
     * Returns a deep copy of this coordinate.
     */
    public CoordinatePoint clone()
    {return new CoordinatePoint(ord);}

    /**
     * Returns a string representation of this coordinate.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes.
     */
    public String toString()
    {return toString(this, ord);}

    /**
     * Returns a string representation of an object.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes.
     */
    static String toString(final Object owner, final double[] ord)
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(owner));
        buffer.append('[');
        for (int i=0; i<ord.length; i++)
        {
            if (i!=0) buffer.append(", ");
            buffer.append(ord[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }
}
