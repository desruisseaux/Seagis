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
 * A named projection parameter value. The linear units of parameters' values
 * match the linear units of the containing projected coordinate system.  The
 * angular units of parameter values match the angular units of the geographic
 * coordinate system that the projected coordinate system is based on. (Notice
 * that this is different from {@link net.seas.opengis.ct.Parameter}, where the
 * units are always meters and degrees.)
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_ProjectionParameter
 */
public class ProjectionParameter implements Cloneable, Serializable
{
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -8277498873677222418L;

    /**
     * The parameter name.
     */
    private final String name;

    /**
     * The parameter value.
     */
    private double value;

    /**
     * Construct a named parameter.
     *
     * @param name  The parameter name.
     * @param value The parameter value.
     */
    public ProjectionParameter(final String name, final double value)
    {
        this.name  = name;
        this.value = value;
    }

    /**
     * Returns the parameter name.
     */
    public String getName()
    {return name;}

    /**
     * Returns the parameter value.
     */
    public double getValue()
    {return value;}

    /**
     * Set the parameter value.
     */
    public void setValue(final double value)
    {this.value = value;}

    /**
     * Returns a hash value for this parameter.
     */
    public int hashCode()
    {
        final long longCode = Double.doubleToLongBits(value);
        int code = (int)(longCode >>> 32) ^ (int)longCode;
        if (name!=null) code ^= name.hashCode();
        return code;
    }

    /**
     * Compares the specified object with
     * this parameter for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof ProjectionParameter)
        {
            final ProjectionParameter that = (ProjectionParameter) object;
            return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(that.value) &&
                   XClass.equals(this.name, that.name);
        }
        else return false;
    }

    /**
     * Returns a copy of this parameter.
     */
    public ProjectionParameter clone()
    {
        try
        {
            return (ProjectionParameter) super.clone();
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
     * Returns a string représentation of this parameter.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(name);
        buffer.append('=');
        buffer.append(value);
        buffer.append(']');
        return buffer.toString();
    }
}
