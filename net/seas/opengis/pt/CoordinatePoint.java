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
import net.seas.util.XClass;
import java.awt.geom.Point2D;


/**
 * A position defined by a list of numbers. The ordinate values are
 * indexed from <code>0<code> to <code>(numDim-1)</code>, where
 * <code>numDim</code> is the dimension of the coordinate system
 * the coordinate point belongs in.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.pt.PT_CoordinatePoint
 */
public final class CoordinatePoint implements Cloneable, Serializable
{
    /**
     * Serial number for compatibility with different versions.
     * TODO: compute serialver
     */
    //private static final long serialVersionUID = ?;

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
     * The <code>ord</code> will be copied.
     */
    public CoordinatePoint(final double[] ord)
    {this.ord = (double[]) ord.clone();}

    /**
     * Construct a 2D coordinate from
     * the specified ordinate.
     */
    public CoordinatePoint(final double x, final double y)
    {ord = new double[] {x,y};}

    /**
     * Construct a 3D coordinate from
     * the specified ordinate.
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
     * Returns a string representation of this coordinate.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        if (ord!=null)
        {
            for (int i=0; i<ord.length; i++)
            {
                if (i!=0) buffer.append(", ");
                buffer.append(ord[i]);
            }
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Returns a deep copy of this coordinate.
     */
    public CoordinatePoint clone()
    {return new CoordinatePoint((double[]) ord.clone());}

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
     * Returns a hash value for this coordinate.
     */
    public int hashCode()
    {
        long code=0;
        if (ord!=null)
        {
            for (int i=0; i<ord.length; i++)
                code = (code << 1) ^ Double.doubleToLongBits(ord[i]);
        }
        return (int)(code >>> 32) ^ (int)code;
    }
}
