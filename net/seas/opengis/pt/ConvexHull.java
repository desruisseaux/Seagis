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


/**
 * A convex hull, for testing inclusion in a domain of validity.
 * A convex hull is a shape in a coordinate system, where if two positions
 * <var>A</var> and <var>B</var> are inside the shape, then all positions
 * in the straight line between <var>A</var> and <var>B</var> are also inside
 * the shape. So in 3D a cube and a sphere are both convex hulls. Other less
 * obvious examples of convex hulls are straight lines, and single points.
 * (A single point is a convex hull, because the positions A and B must both
 * be the same - i.e. the point itself. So the straight line between <var>A</var>
 * and <var>B</var> has zero length.)
 * <br><br>
 * Some examples of shapes that are NOT convex hulls are donuts, and horseshoes.
 * The simplest type of complex hull is a single point. So you can easily ask
 * whether a point is inside, or outside the domain of a math transform.
 * <br><br>
 * <strong>Note: this class has no direct OpenGIS equivalent
 *               and is not yet implemented</strong>.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public final class ConvexHull implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    //private static final long serialVersionUID = ?;

    /**
     * Construct the simpliest convex hull: a single point.
     * <strong>This method is not implemented.</strong>
     */
    public ConvexHull(final CoordinatePoint point)
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Construct a convex hull defined by an envelope.
     * <strong>This method is not implemented.</strong>
     */
    public ConvexHull(final Envelope envelope)
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Construct a convex hull from an array of ordinates.
     * This method is provided for compatibility with OpenGIS.
     * <strong>This method is not implemented.</strong>
     */
    public ConvexHull(final double[] ord)
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Returns a hash value for this convex hull.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        return super.hashCode(); // TODO
    }

    /**
     * Compares the specified object with
     * this convex hull for equality.
     */
    public boolean equals(final Object object)
    {
        return super.equals(object); // TODO
    }

    /**
     * Returns a string representation of this convex hull.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        // TODO
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Returns the array of ordinates.
     * This method is provided for compatibility with OpenGIS.
     *
     * @deprecated This method is not implemented.
     */
    public double[] toOpenGIS()
    {throw new UnsupportedOperationException("Not implemented");}
}
