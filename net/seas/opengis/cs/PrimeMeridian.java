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

// OpenGIS dependencies
import org.opengis.cs.CS_AngularUnit;
import org.opengis.cs.CS_PrimeMeridian;

// Miscellaneous
import javax.units.Unit;
import net.seas.util.XClass;
import java.rmi.RemoteException;


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
     * The Greenwich meridian, with angular measures in degrees.
     */
    public static final PrimeMeridian GREENWICH = new PrimeMeridian("Greenwich", Unit.DEGREE, 0);

    /**
     * The angular units.
     */
    private final Unit unit;

    /**
     * The longitude value relative to the Greenwich Meridian.
     */
    private final double longitude;

    /**
     * Creates a prime meridian, relative to Greenwich.
     *
     * @param name      Name to give new object.
     * @param unit      Angular units of longitude.
     * @param longitude Longitude of prime meridian in supplied angular units East of Greenwich.
     */
    public PrimeMeridian(final String name, final Unit unit, final double longitude)
    {
        super(name);
        this.unit      = unit;
        this.longitude = longitude;
        ensureNonNull("unit", unit);
        ensureAngularUnit(unit);
    }

    /**
     * Wrap the specified OpenGIS prime meridian.
     *
     * @param  meridian The OpenGIS prime meridian.
     * @throws RemoteException if a remote call failed.
     */
    PrimeMeridian(final CS_PrimeMeridian meridian) throws RemoteException
    {
        super(meridian);
        this.unit      = Adapters.wrap(meridian.getAngularUnit());
        this.longitude = meridian.getLongitude();
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
    {return XClass.getShortClassName(this)+'['+getName(null)+'='+longitude+unit+']';}

    /**
     * Returns an OpenGIS interface for this prime meridian.
     * The returned object is suitable for RMI use.
     */
    final CS_PrimeMeridian toOpenGIS()
    {return new Export();}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link PrimeMeridian} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends Info.Export implements CS_PrimeMeridian
    {
        /**
         * Returns the longitude value relative to the Greenwich Meridian.
         */
        public double getLongitude() throws RemoteException
        {return PrimeMeridian.this.getLongitude();}

        /**
         * Returns the AngularUnits.
         *
         * @throws RemoteException if a remote method call failed.
         */
        public CS_AngularUnit getAngularUnit() throws RemoteException
        {return (CS_AngularUnit) Adapters.export(PrimeMeridian.this.getAngularUnit());}
    }
}
