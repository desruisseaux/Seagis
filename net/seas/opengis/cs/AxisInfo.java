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
 * Details of axis. This is used to label
 * axes, and indicate the orientation.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_AxisInfo
 */
public class AxisInfo implements Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 2949229234384551980L;

    /**
     * Human readable name for axis. Possible values are
     * <code>X</code>, <code>Y</code>, <code>Long</code>,
     * <code>Lat</code> or any other short string.
     */
    public String name;

    /**
     * Enumerated value for orientation.
     */
    public AxisOrientation orientation;

    /**
     * Construct an AxisInfo.
     *
     * @param name The axis name.
     * @param orientation The axis orientation.
     */
    public AxisInfo(final String name, final AxisOrientation orientation)
    {
        this.name        = name;
        this.orientation = orientation;
    }

    /**
     * Returns a hash value for this axis.
     */
    public int hashCode()
    {
        int code=0;
        if (orientation!=null) code ^= orientation.hashCode();
        if (name       !=null) code ^= name.hashCode();
        return code;
    }

    /**
     * Compares the specified object
     * with this axis for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof AxisInfo)
        {
            final AxisInfo that = (AxisInfo) object;
            return XClass.equals(this.orientation, that.orientation) &&
                   XClass.equals(this.name       , that.name);
        }
        else return false;
    }

    /**
     * Returns a copy of this axis.
     */
    public AxisInfo clone()
    {
        try
        {
            return (AxisInfo) super.clone();
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
     * Returns a string représentation of this axis.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(name);
        if (orientation!=null)
        {
            buffer.append(',');
            buffer.append(orientation.getName(null));
        }
        buffer.append(']');
        return buffer.toString();
    }
}
