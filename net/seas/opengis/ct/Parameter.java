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
package net.seas.opengis.ct;

// Miscellaneous
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.resources.Resources;


/**
 * A named parameter value.
 *
 * @version 1.0
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_Parameter
 */
public final class Parameter implements Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1928332240210223059L;

    /**
     * The parameter name. This name
     * is immutable and can't be null.
     */
    public final String name;

    /**
     * The parameter value. This value
     * is mutable and can be updated.
     */
    public double value;

    /**
     * Construct a named parameter.
     *
     * @param name  The parameter name.
     * @param value The parameter value.
     */
    public Parameter(final String name, final double value)
    {
        this.name  = name.trim();
        this.value = value;
    }

    /**
     * Convenience method for fetching a parameter value.
     * Search is case-insensitive and ignore leading and
     * trailing blanks.
     *
     * @param  parameters User-suplied parameters.
     * @param  name Parameter to look for.
     * @return The parameter value.
     * @throws MissingParameterException if parameter <code>name</code> is not found.
     */
    public static double getValue(final Parameter[] parameters, final String name) throws MissingParameterException
    {return getValue(parameters, name, Double.NaN, true);}

    /**
     * Convenience method for fetching a parameter value.
     * Search is case-insensitive and ignore leading and
     * trailing blanks.
     *
     * @param  parameters User-suplied parameters.
     * @param  name Parameter to look for.
     * @param  defaultValue Default value to return if
     *         parameter <code>name</code> is not found.
     * @return The parameter value, or <code>defaultValue</code>
     *         if the parameter <code>name</code> is not found.
     */
    public static double getValue(final Parameter[] parameters, final String name, final double defaultValue)
    {return getValue(parameters, name, defaultValue, false);}

    /**
     * Convenience method for fetching a parameter value.
     * Search is case-insensitive and ignore leading and
     * trailing blanks.
     *
     * @param  parameters User-suplied parameters.
     * @param  name Parameter to look for.
     * @param  defaultValue Default value to return if
     *         parameter <code>name</code> is not found.
     * @param  required <code>true</code> if the parameter is required (in which case
     *         <code>defaultValue</code> is ignored), or <code>false</code> otherwise.
     * @return The parameter value, or <code>defaultValue</code> if the parameter is
     *         not found and <code>required</code> is <code>false</code>.
     * @throws MissingParameterException if <code>required</code> is <code>true</code>
     *         and parameter <code>name</code> is not found.
     */
    private static double getValue(final Parameter[] parameters, String name, final double defaultValue, final boolean required) throws MissingParameterException
    {
        name = name.trim();
        for (int i=0; i<parameters.length; i++)
        {
            if (name.equalsIgnoreCase(parameters[i].name))
            {
                // Check for duplication
                final double value = parameters[i].value;
                while (++i<parameters.length)
                {
                    if (name.equalsIgnoreCase(parameters[i].name))
                        if (Double.doubleToLongBits(parameters[i].value) != Double.doubleToLongBits(value))
                            throw new IllegalArgumentException(Resources.format(Clé.DUPLICATE_PARAMETER¤1, name));
                }
                return value;
            }
        }
        if (!required) return defaultValue;
        throw new MissingParameterException(null, name);
    }

    /**
     * Returns a hash value for this parameter.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        final long longCode = Double.doubleToLongBits(value);
        int code = (int)(longCode >>> 32) ^ (int)longCode;
        if (name!=null) code ^= name.hashCode();
        return code;
    }

    /**
     * Returns a copy of this parameter.
     */
    public Parameter clone()
    {
        try
        {
            return (Parameter) super.clone();
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
     * this parameter for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof Parameter)
        {
            final Parameter that = (Parameter) object;
            return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(that.value) &&
                   XClass.equals(this.name, that.name);
        }
        else return false;
    }

    /**
     * Returns a string représentation of this parameter.
     * The returned string is implementation dependent. It
     * is usually provided for debugging purposes.
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
