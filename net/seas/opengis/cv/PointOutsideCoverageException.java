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
package net.seas.opengis.cv;

// Miscellaneous
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.text.FieldPosition;
import net.seas.resources.Resources;
import net.seas.opengis.pt.CoordinatePoint;


/**
 * Throws when a <code>Coverage.evaluate</code>
 * method is invoked with a point outside coverage.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class PointOutsideCoverageException extends RuntimeException
{
    /**
     * Construct an exception with no message.
     */
    public PointOutsideCoverageException()
    {}

    /**
     * Construct an exception with the specified message.
     */
    public PointOutsideCoverageException(final String message)
    {super(message);}

    /**
     * Construct an exception with a message for the specified point.
     */
    public PointOutsideCoverageException(final Point2D point)
    {this(new CoordinatePoint(point));}

    /**
     * Construct an exception with a message for the specified point.
     */
    public PointOutsideCoverageException(final CoordinatePoint point)
    {super(Resources.format(Clé.POINT_OUTSIDE_COVERAGE¤1, toString(point)));}

    /**
     * Construct a string for the specified point.
     */
    private static String toString(final CoordinatePoint point)
    {
        final StringBuffer buffer = new StringBuffer();
        final FieldPosition dummy = new FieldPosition(0);
        final NumberFormat format = NumberFormat.getNumberInstance();
        final int       dimension = point.getDimension();
        for (int i=0; i<dimension; i++)
        {
            if (i!=0) buffer.append(", ");
            format.format(point.getOrdinate(i), buffer, dummy);
        }
        return buffer.toString();
    }
}
