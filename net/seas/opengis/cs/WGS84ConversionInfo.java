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
import java.io.Serializable;
import net.seas.util.XClass;


/**
 * Parameters for a geographic transformation into WGS84.
 * The Bursa Wolf parameters should be applied to geocentric coordinates,
 * where the X axis points towards the Greenwich Prime Meridian, the Y axis
 * points East, and the Z axis points North.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_WGS84ConversionInfo
 */
public class WGS84ConversionInfo implements Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -7104458538855128443L;

    /** Bursa Wolf shift in meters. */
    public double dx;

    /** Bursa Wolf shift in meters. */
    public double dy;

    /** Bursa Wolf shift in meters. */
    public double dz;

    /** Bursa Wolf rotation in arc seconds. */
    public double ex;

    /** Bursa Wolf rotation in arc seconds. */
    public double ey;

    /** Bursa Wolf rotation in arc seconds. */
    public double ez;

    /** Bursa Wolf scaling in parts per million. */
    public double ppm;

    /** Human readable text describing intended region of transformation. */
    public String areaOfUse;

    /**
     * Construct a conversion info
     * with all parameters set to 0.
     */
    public WGS84ConversionInfo()
    {}

    /**
     * Returns a hash value for this object.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        final long code = (Double.doubleToLongBits(dx ) << 0) ^
                          (Double.doubleToLongBits(dy ) << 1) ^
                          (Double.doubleToLongBits(dz ) << 2) ^
                          (Double.doubleToLongBits(ex ) << 3) ^
                          (Double.doubleToLongBits(ey ) << 4) ^
                          (Double.doubleToLongBits(ez ) << 5) ^
                          (Double.doubleToLongBits(ppm) << 6);
        return (int)(code >>> 32) ^ (int)code;
    }

    /**
     * Returns a copy of this object.
     */
    public WGS84ConversionInfo clone()
    {
        try
        {
            return (WGS84ConversionInfo) super.clone();
        }
        catch (CloneNotSupportedException exception)
        {
            // Should not happen, since we are cloneable.
            final InternalError error = new InternalError(exception.getMessage());
            error.initCause(exception);
            throw error;
        }
    }

    /**
     * Compares the specified object with
     * this object for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof WGS84ConversionInfo)
        {
            final WGS84ConversionInfo that = (WGS84ConversionInfo) object;
            return Double.doubleToLongBits(this.dx)  == Double.doubleToLongBits(that.dx)  &&
                   Double.doubleToLongBits(this.dy)  == Double.doubleToLongBits(that.dy)  &&
                   Double.doubleToLongBits(this.dz)  == Double.doubleToLongBits(that.dz)  &&
                   Double.doubleToLongBits(this.ex)  == Double.doubleToLongBits(that.ex)  &&
                   Double.doubleToLongBits(this.ey)  == Double.doubleToLongBits(that.ey)  &&
                   Double.doubleToLongBits(this.ez)  == Double.doubleToLongBits(that.ez)  &&
                   Double.doubleToLongBits(this.ppm) == Double.doubleToLongBits(that.ppm) &&
                   XClass.equals(this.areaOfUse, that.areaOfUse);
        }
        else return false;
    }

    /**
     * Returns a string représentation of this object.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes only.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(areaOfUse);
        buffer.append(": shift=(");
        buffer.append(dx); buffer.append(", ");
        buffer.append(dy); buffer.append(", ");
        buffer.append(dz); buffer.append("), rotation=(");
        buffer.append(ex); buffer.append(", ");
        buffer.append(ey); buffer.append(", ");
        buffer.append(ez); buffer.append("), ppm=");
        buffer.append(ppm);
        buffer.append(']');
        return buffer.toString();
    }
}
