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

// OpenGIS dependencies
import org.opengis.pt.PT_Envelope;
import org.opengis.pt.PT_CoordinatePoint;


/**
 * Provide static methods for interoperability with
 * <code>org.opengis.pt</code> package. All static
 * methods accept null argument.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Adapters
{
    /**
     * Do not allow creation of
     * instance of this class.
     */
    private Adapters()
    {}

    /**
     * Returns an OpenGIS structure for a coordinate point.
     * Changes to the returned structure will not affect the original point.
     */
    public static PT_CoordinatePoint export(final CoordinatePoint point)
    {
        if (point==null) return null;
        final PT_CoordinatePoint pt = new PT_CoordinatePoint();
        pt.ord = (double[]) point.ord.clone();
        return pt;
    }

    /**
     * Returns an OpenGIS structure for an envelope.
     * Changes to the returned structure will not affect the original envelope.
     */
    public static PT_Envelope export(final Envelope envelope)
    {
        if (envelope==null) return null;
        final int  dimension = envelope.getDimension();
        final PT_Envelope ep = new PT_Envelope();
        ep.minCP = new PT_CoordinatePoint();
        ep.maxCP = new PT_CoordinatePoint();
        ep.minCP.ord = new double[dimension];
        ep.maxCP.ord = new double[dimension];
        for (int i=0; i<dimension; i++)
        {
            ep.minCP.ord[i] = envelope.getMinimum(i);
            ep.maxCP.ord[i] = envelope.getMaximum(i);
        }
        return ep;
    }

    /**
     * Returns a coordinate point from an OpenGIS's structure.
     * Changes to the returned point will not affect the original structure.
     */
    public static CoordinatePoint wrap(final PT_CoordinatePoint point)
    {return (point!=null) ? new CoordinatePoint(point.ord) : null;}

    /**
     * Returns an envelope from an OpenGIS's structure.
     * Changes to the returned envelope will not affect the original structure.
     */
    public static Envelope wrap(final PT_Envelope envelope)
    {return (envelope!=null) ? new Envelope(envelope.minCP.ord, envelope.maxCP.ord) : null;}
}
