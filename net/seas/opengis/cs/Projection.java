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
import java.util.Arrays;
import net.seas.util.XClass;
import net.seas.opengis.ct.MissingParameterException;


/**
 * A projection from geographic coordinates to projected coordinates.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_Projection
 */
public class Projection extends Info
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -7116072094430367096L;

    /**
     * Classification string for projection (e.g. "Transverse_Mercator").
     */
    private final String classification;

    /**
     * Parameters to use for projection, in metres or degrees.
     */
    private final ProjectionParameter[] parameters;

    /**
     * Construct a new projection.
     *
     * @param name           Name to give new object.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param parameters     Parameters to use for projection, in metres or degrees.
     */
    protected Projection(final String name, final String classification, final ProjectionParameter[] parameters)
    {
        super(name);
        ensureNonNull("classification", classification);
        ensureNonNull("parameters",     parameters);
        this.classification = classification;
        this.parameters     = (ProjectionParameter[]) parameters.clone();
        for (int i=0; i<this.parameters.length; i++)
        {
            ensureNonNull("parameters", this.parameters, i);
            this.parameters[i] = this.parameters[i].clone();
        }
    }

    /**
     * Gets the projection classification name (e.g. "Transverse_Mercator").
     */
    public String getClassName()
    {return classification;}

    /**
     * Gets number of parameters of the projection.
     */
    public int getNumParameters()
    {return parameters.length;}

    /**
     * Gets an indexed parameter of the projection.
     *
     * @param index Zero based index of parameter to fetch.
     */
    public ProjectionParameter getParameter(final int index)
    {return parameters[index].clone();}

    /**
     * Convenience method for fetching a parameter value.
     * Search is case-insensitive and ignore leading and
     * trailing blanks.
     *
     * @param  name Parameter to look for.
     * @return The parameter value.
     * @throws MissingParameterException if parameter <code>name</code> is not found.
     */
    public double getValue(final String name) throws MissingParameterException
    {return getValue(name, Double.NaN, true);}

    /**
     * Convenience method for fetching a parameter value.
     * Search is case-insensitive and ignore leading and
     * trailing blanks.
     *
     * @param  name Parameter to look for.
     * @param  defaultValue Default value to return if
     *         parameter <code>name</code> is not found.
     * @return The parameter value, or <code>defaultValue</code>
     *         if the parameter <code>name</code> is not found.
     */
    public double getValue(final String name, final double defaultValue)
    {return getValue(name, defaultValue, false);}

    /**
     * Convenience method for fetching a parameter value.
     * Search is case-insensitive and ignore leading and
     * trailing blanks.
     *
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
    private double getValue(String name, final double defaultValue, final boolean required) throws MissingParameterException
    {
        name = name.trim();
        for (int i=0; i<parameters.length; i++)
        {
            ProjectionParameter parameter = parameters[i];
            if (name.equalsIgnoreCase(parameter.name))
            {
                return parameter.value;
            }
        }
        if (!required) return defaultValue;
        throw new MissingParameterException(null, name);
    }

    /**
     * Returns a hash value for this projection.
     */
    public int hashCode()
    {
        int code = classification.hashCode();
        for (int i=0; i<parameters.length; i++)
            code ^= parameters[i].hashCode();
        return code;
    }

    /**
     * Compares the specified object with
     * this projection for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final Projection that = (Projection) object;
            return XClass.equals(this.classification, that.classification) &&
                   Arrays.equals(this.parameters,     that.parameters);
        }
        return false;
    }

    /**
     * Returns a string representation of this projection.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getClassName()+']';}
}
