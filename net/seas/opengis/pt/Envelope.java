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
import java.io.Serializable;
import net.seas.util.XClass;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import net.seas.resources.Resources;


/**
 * A box defined by two positions. The two positions must have the same dimension.
 * Each of the ordinate values in the minimum point must be less than or equal
 * to the corresponding ordinate value in the maximum point.  Please note that
 * these two points may be outside the valid domain of their coordinate system.
 * (Of course the points and envelope do not explicitly reference a coordinate
 * system, but their implicit coordinate system is defined by their context.)
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.pt.PT_Envelope
 */
public final class Envelope implements Cloneable, Serializable
{
    /**
     * Serial number for compatibility with different versions.
     * TODO: compute serialver
     */
    //private static final long serialVersionUID = ?;

    /**
     * Point containing minimum ordinate values.
     */
    private final CoordinatePoint minCP;

    /**
     * Point containing maximum ordinate values.
     */
    private final CoordinatePoint maxCP;

    /**
     * Check if two points have the same dimension.
     *
     * @param  P1 First point to check.
     * @param  P2 Second point to check.
     * @throws IllegalArgumentException if the two points don't have the same dimension.
     */
    private static void checkDimension(final CoordinatePoint P1, final CoordinatePoint P2) throws IllegalArgumentException
    {
        if (P1.ord.length != P2.ord.length)
            throw new IllegalArgumentException(Resources.format(Clé.MISMATCHED_DIMENSION));
    }

    /**
     * Check if ordinate values in the minimum point are less than or
     * equal to the corresponding ordinate value in the maximum point.
     *
     * @param  minCP Point containing minimum ordinate values.
     * @param  maxCP Point containing maximum ordinate values.
     * @throws IllegalArgumentException if the two positions don't have the same dimension.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    private static void checkCoherence(final CoordinatePoint minCP, final CoordinatePoint maxCP) throws IllegalArgumentException
    {
        checkDimension(minCP, maxCP);
        for (int i=0; i<minCP.ord.length; i++)
            if (!(minCP.ord[i] <= maxCP.ord[i])) // Use '!' in order to catch 'NaN'.
                throw new IllegalArgumentException(Resources.format(Clé.BAD_ORDINATE¤1, new Integer(i+1)));
    }

    /**
     * Construct a box defined by two positions.
     *
     * @param  minCP Point containing minimum ordinate values.
     * @param  maxCP Point containing maximum ordinate values.
     * @throws IllegalArgumentException if the two positions don't have the same dimension.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    public Envelope(final CoordinatePoint minCP, final CoordinatePoint maxCP) throws IllegalArgumentException
    {
        checkCoherence(minCP, maxCP);
        this.minCP = minCP.clone();
        this.maxCP = maxCP.clone();
    }

    /**
     * Construct a box defined by a {@link Rectangle2D}.
     */
    public Envelope(final Rectangle2D rect)
    {
        minCP = new CoordinatePoint(rect.getMinX(), rect.getMinY());
        maxCP = new CoordinatePoint(rect.getMaxX(), rect.getMaxY());
        checkCoherence(minCP, maxCP);
    }

    /**
     * Construct a box located a the specified point. The box has an
     * initial width of 0 (i.e. <code>minCP==maxCP</code>). However,
     * it may be expanded with calls to {@link #add}.
     *
     * @param CP The box location.
     */
    public Envelope(final CoordinatePoint CP)
    {
        minCP = CP.clone();
        maxCP = CP.clone();
        checkCoherence(minCP, maxCP);
    }

    /**
     * Adds a point to this envelope. The resulting envelope is the smallest
     * envelope that contains both the original envelope and the specified point.
     * After adding a point, a call to {@link #contains} with the added point as
     * an argument will return <code>true</code>, except if one of the point's
     * ordinates was {@link Double#NaN} (in which case the corresponding ordinate
     * have been ignored).
     *
     * @param  point The point to add.
     * @throws IllegalArgumentException if the point doesn't have the expected dimension.
     */
    public void add(final CoordinatePoint point) throws IllegalArgumentException
    {
        checkDimension(point, minCP);
        for (int i=0; i<point.ord.length; i++)
        {
            final double ord = point.ord[i];
            if (ord < minCP.ord[i]) minCP.ord[i]=ord;
            if (ord > maxCP.ord[i]) maxCP.ord[i]=ord;
        }
    }

    /**
     * Tests if a specified coordinate is inside the boundary of this envelope.
     *
     * @param  point The point to text.
     * @return <code>true</code> if the specified coordinates are inside the boundary
     *         of this envelope; <code>false</code> otherwise.
     * @throws IllegalArgumentException if the point doesn't have the expected dimension.
     */
    public boolean contains(final CoordinatePoint point) throws IllegalArgumentException
    {
        checkDimension(point, minCP);
        for (int i=0; i<point.ord.length; i++)
        {
            final double ord = point.ord[i];
            if (!(ord >= minCP.ord[i])) return false;
            if (!(ord <= maxCP.ord[i])) return false;
            // Use '!' in order to take 'NaN' in acount.
        }
        return true;
    }

    /**
     * Returns the minimal ordinate along
     * the specified dimension.
     */
    public double getMinimum(final int dimension)
    {return minCP.ord[dimension];}

    /**
     * Returns the maximal ordinate along
     * the specified dimension.
     */
    public double getMaximum(final int dimension)
    {return maxCP.ord[dimension];}

    /**
     * Returns the center ordinate along
     * the specified dimension.
     */
    public double getCenter(final int dimension)
    {return 0.5*(minCP.ord[dimension] + maxCP.ord[dimension]);}

    /**
     * Returns a string representation of this envelope.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(minCP);
        buffer.append(", ");
        buffer.append(maxCP);
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Returns a deep copy of this envelope.
     */
    public Envelope clone()
    {return new Envelope(minCP, maxCP);}

    /**
     * Compares the specified object with
     * this envelope for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof Envelope)
        {
            final Envelope that = (Envelope) object;
            return XClass.equals(this.minCP, that.minCP) &&
                   XClass.equals(this.maxCP, that.maxCP);
        }
        return false;
    }

    /**
     * Returns a hash value for this envelope.
     */
    public int hashCode()
    {
        int code = 0;
        if (minCP!=null) code ^= minCP.hashCode();
        if (maxCP!=null) code ^= maxCP.hashCode();
        return code;
    }
}
