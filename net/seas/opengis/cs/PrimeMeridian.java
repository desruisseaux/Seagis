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
 * A meridian used to take longitude measurements from.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_PrimeMeridian
 */
public class PrimeMeridian extends Info
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -963498800353363758L;

    /**
     * The angular units.
     */
    private final Unit unit;

    /**
     * The longitude value relative to the Greenwich Meridian.
     */
    private final double longitude;

    /**
     * Construct a prime meridian.
     *
     * @param name The prime meridian name.
     * @param unit The angular units. The longitude is expressed in this units.
     * @param longitude The longitude value relative to the Greenwich Meridian.
     */
    protected PrimeMeridian(final String name, final Unit unit, final double longitude)
    {
        super(name);
        this.unit      = unit;
        this.longitude = longitude;
        ensureNonNull("unit", unit);
    }

    /**
     * Returns the longitude value relative to the Greenwich Meridian.
     * The longitude is expressed in this objects angular units.
     */
    public double getLongitude()
    {return longitude;}

    /**
     * Returns the angular units.
     */
    public Unit getAngularUnit()
    {return unit;}

    /**
     * Returns a hash value for this prime meridian.
     */
    public int hashCode()
    {
        final long code = Double.doubleToLongBits(longitude);
        return super.hashCode() ^ (int)(code >>> 32) ^ (int)code;
    }

    /**
     * Compares the specified object with
     * this prime meridian for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final PrimeMeridian that = (PrimeMeridian) object;
            return Double.doubleToLongBits(this.longitude) == Double.doubleToLongBits(that.longitude) &&
                   XClass.equals(this.unit, that.unit);
        }
        return false;
    }

    /**
     * Returns a string representation of this prime meridian.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getName()+'='+longitude+unit+']';}
}
