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
import java.awt.geom.Rectangle2D;
import net.seas.resources.Resources;


/**
 * A box defined by two positions. The two positions must have the
 * same dimension. Each of the ordinate values in the minimum point
 * must be less than or equal to the corresponding ordinate value
 * in the maximum point. Please note that these two points may be
 * outside the valid domain of their coordinate system. (Of course
 * the points and envelope do not explicitly reference a coordinate
 * system, but their implicit coordinate system is defined by their
 * context.)
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see Rectangle2D
 * @see org.opengis.pt.PT_Envelope
 */
public final class Envelope implements Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -3698298355829867895L;

    /**
     * Minimum and maximum ordinate values. The first half contains minimum
     * ordinates, while the last half contains maximum ordinates.
     */
    private final double[] ord;

    /**
     * Check if ordinate values in the minimum point are less than or
     * equal to the corresponding ordinate value in the maximum point.
     *
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    private void checkCoherence() throws IllegalArgumentException
    {
        final int dimension = ord.length/2;
        for (int i=0; i<dimension; i++)
            if (!(ord[i] <= ord[dimension+i])) // Use '!' in order to catch 'NaN'.
                throw new IllegalArgumentException(Resources.format(Clé.BAD_ORDINATE¤1, new Integer(i+1)));
    }

    /**
     * Construct a copy of the specified envelope.
     */
    private Envelope(final Envelope envelope)
    {ord = (double[]) envelope.ord.clone();}

    /**
     * Construct an envelope of the specified
     * dimension with infinite bounds.
     */
    public Envelope(final int dimension)
    {
        ord = new double[dimension*2];
        Arrays.fill(ord, 0, dimension,          Double.NEGATIVE_INFINITY);
        Arrays.fill(ord, dimension, ord.length, Double.POSITIVE_INFINITY);
    }

    /**
     * Construct one-dimensional envelope defined by a range of values.
     *
     * @param min The minimal value.
     * @param max The maximal value.
     */
    public Envelope(final double min, final double max)
    {
        ord = new double[] {min, max};
        checkCoherence();
    }

    /**
     * Construct two-dimensional envelope defined by a {@link Rectangle2D}.
     */
    public Envelope(final Rectangle2D rect)
    {
        ord = new double[]
        {
            rect.getMinX(), rect.getMinY(),
            rect.getMaxX(), rect.getMaxY()
        };
        checkCoherence();
    }

    /**
     * Construct a envelope defined by two positions.
     *
     * @param  minCP Point containing minimum ordinate values.
     * @param  maxCP Point containing maximum ordinate values.
     * @throws IllegalArgumentException if the two positions don't have the same dimension.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    public Envelope(final CoordinatePoint minCP, final CoordinatePoint maxCP) throws IllegalArgumentException
    {this(minCP.ord, maxCP.ord);}

    /**
     * Construct a envelope defined by two positions.
     *
     * @param  minCP Minimum ordinate values.
     * @param  maxCP Maximum ordinate values.
     * @throws IllegalArgumentException if the two positions don't have the same dimension.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    Envelope(final double[] minCP, final double[] maxCP) throws IllegalArgumentException
    {
        if (minCP.length != maxCP.length)
        {
            throw new IllegalArgumentException(Resources.format(Clé.MISMATCHED_POINT_DIMENSION¤2,
                                               new Integer(minCP.length), new Integer(maxCP.length)));
        }
        ord = new double[minCP.length + maxCP.length];
        System.arraycopy(minCP, 0, ord, 0,            minCP.length);
        System.arraycopy(maxCP, 0, ord, minCP.length, maxCP.length);
        checkCoherence();
    }

    /**
     * <FONT COLOR="#FF6633">Adds a point to this envelope. The resulting envelope
     * is the smallest envelope that contains both the original envelope and the
     * specified point. After adding a point, a call to {@link #contains} with the
     * added point as an argument will return <code>true</code>, except if one of
     * the point's ordinates was {@link Double#NaN} (in which case the corresponding
     * ordinate have been ignored).</FONT>
     *
     * @param  point The point to add.
     * @throws IllegalArgumentException if the point doesn't have the expected dimension.
     */
    public void add(final CoordinatePoint point) throws IllegalArgumentException
    {
        final int dimension = ord.length/2;
        point.ensureDimensionMatch(dimension);
        for (int i=0; i<dimension; i++)
        {
            final double value = point.ord[i];
            if (value < ord[i          ]) ord[i          ]=value;
            if (value > ord[i+dimension]) ord[i+dimension]=value;
        }
    }

    /**
     * <FONT COLOR="#FF6633">Tests if a specified coordinate is inside the boundary of this envelope.</FONT>
     *
     * @param  point The point to text.
     * @return <code>true</code> if the specified coordinates are inside the boundary
     *         of this envelope; <code>false</code> otherwise.
     * @throws IllegalArgumentException if the point doesn't have the expected dimension.
     */
    public boolean contains(final CoordinatePoint point) throws IllegalArgumentException
    {
        final int dimension = ord.length/2;
        point.ensureDimensionMatch(dimension);
        for (int i=0; i<dimension; i++)
        {
            final double value = point.ord[i];
            if (!(value >= ord[i          ])) return false;
            if (!(value <= ord[i+dimension])) return false;
            // Use '!' in order to take 'NaN' in acount.
        }
        return true;
    }

    /**
     * <FONT COLOR="#FF6633">Returns the number of dimensions.</FONT>
     */
    public int getDimension()
    {return ord.length/2;}

    /**
     * <FONT COLOR="#FF6633">Returns the minimal ordinate along
     * the specified dimension.</FONT>
     */
    public double getMinimum(final int dimension)
    {
        if (dimension<ord.length) return ord[dimension];
        throw new ArrayIndexOutOfBoundsException(dimension);
    }

    /**
     * <FONT COLOR="#FF6633">Returns the maximal ordinate along
     * the specified dimension.</FONT>
     */
    public double getMaximum(final int dimension)
    {
        if (dimension>=0) return ord[dimension+ord.length/2];
        throw new ArrayIndexOutOfBoundsException(dimension);
    }

    /**
     * <FONT COLOR="#FF6633">Returns the center ordinate along
     * the specified dimension.</FONT>
     */
    public double getCenter(final int dimension)
    {return 0.5*(ord[dimension] + ord[dimension+ord.length/2]);}

    /**
     * Returns a hash value for this envelope.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {return CoordinatePoint.hashCode(ord);}

    /**
     * Compares the specified object with
     * this envelope for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof Envelope)
        {
            final Envelope that = (Envelope) object;
            return Arrays.equals(this.ord, that.ord);
        }
        return false;
    }

    /**
     * Returns a deep copy of this envelope.
     */
    public Envelope clone()
    {return new Envelope(this);}

    /**
     * Returns a string representation of this envelope.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes.
     */
    public String toString()
    {return CoordinatePoint.toString(this, ord);}
}
