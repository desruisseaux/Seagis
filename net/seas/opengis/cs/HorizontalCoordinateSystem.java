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
package net.seas.opengis.cs;

// Miscellaneous
import javax.units.Unit;
import net.seas.util.XClass;


/**
 * A 2D coordinate system suitable for positions on the Earth's surface.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_HorizontalCoordinateSystem
 */
public abstract class HorizontalCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -9058204611188320317L;

    /**
     * The horizontal datum.
     */
    private final HorizontalDatum datum;

    /**
     * Construct a coordinate system.
     *
     * @param name  The coordinate system name.
     * @param datum The horizontal datum.
     */
    protected HorizontalCoordinateSystem(final String name, final HorizontalDatum datum)
    {
        super(name);
        this.datum = datum;
        ensureNonNull("datum", datum);
    }

    /**
     * Returns the dimension of this coordinate system, which is 2.
     */
    public int getDimension()
    {return 2;}

    /**
     * Returns the horizontal datum.
     */
    public HorizontalDatum getHorizontalDatum()
    {return datum;}

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final HorizontalCoordinateSystem that = (HorizontalCoordinateSystem) object;
            return XClass.equals(this.datum, that.datum);
        }
        return false;
    }
}
