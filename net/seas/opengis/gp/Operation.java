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
package net.seas.opengis.gp;

// Dependencies
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.gc.ParameterInfo;

// Miscellaneous
import java.util.Locale;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptor;
import net.seas.util.XClass;


/**
 * Provides descriptive information for a grid coverage processing
 * operation. The descriptive information includes such information as the
 * name of the operation, operation description, and number of source grid
 * coverages required for the operation.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public abstract class Operation
{
    /**
     * The name of the processing operation.
     */
    private final String name;

    /**
     * The parameters descriptors.
     */
    private final ParameterListDescriptor descriptor;

    /**
     * Construct an operation.
     *
     * @param The name of the processing operation.
     * @param The parameters descriptors.
     */
    public Operation(final String name, final ParameterListDescriptor descriptor)
    {
        this.name       = name;
        this.descriptor = descriptor;
    }

    /**
     * Returns the name of the processing operation.
     */
    public String getName()
    {return name;}

    /**
     * Returns the description of the processing operation.
     * If no description, returns <code>null</code>.
     * If no description is available in the specified locale,
     * a default one will be used.
     *
     * @param locale The desired locale, or <code>null</code>
     *        for the default locale.
     */
    public String getDescription(final Locale locale)
    {return null;}

    /**
     * Returns the number of source grid coverages required for the operation.
     */
    public int getNumSources()
    {
        int count=0;
        final Class[] c = descriptor.getParamClasses();
        if (c!=null)
            for (int i=0; i<c.length; i++)
                if (GridCoverage.class.isAssignableFrom(c[i]))
                    count++;
        return count;
    }

    /**
     * Returns the number of parameters for the
     * operation, including source grid coverages.
     */
    public int getNumParameters()
    {return descriptor.getNumParameters();}

    /**
     * Retrieve the parameter information for a given index.
     * This is mostly a convenience method, since informations
     * are extracted from {@link ParameterListDescriptor}.
     */
    public ParameterInfo getParameterInfo(final int index)
    {return new ParameterInfo(descriptor, index);}

    /**
     * Retrieve the parameter information for a given name.
     * Search is case-insensitive. This is mostly a convenience
     * method, since informations are extracted from
     * {@link ParameterListDescriptor}.
     */
    public ParameterInfo getParameterInfo(final String name)
    {return new ParameterInfo(descriptor, name);}

    /**
     * Returns a default parameter list for this operation.
     */
    public ParameterList getParameterList()
    {return new ParameterListImpl(descriptor);}

    /**
     * Apply a process operation to a grid coverage. This method
     * is invoked by {@link GridCoverageProcessor}.
     *
     * @param  parameters List of name value pairs for the parameters required for the operation.
     * @return The result as a grid coverage.
     */
    protected abstract GridCoverage doOperation(final ParameterList parameters);

    /**
     * Returns a hash value for this operation.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {return name.hashCode()*37 + descriptor.hashCode();}

    /**
     * Compares the specified object with
     * this operation for equality.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && object.getClass().equals(getClass()))
        {
            final Operation that = (Operation) object;
            return XClass.equals(this.name,       that.name) &&
                   XClass.equals(this.descriptor, that.descriptor);
        }
        else return false;
    }

    /**
     * Returns a string représentation of this operation.
     * The returned string is implementation dependent. It
     * is usually provided for debugging purposes.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+name+": "+descriptor.getNumParameters()+']';}
}
