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
import org.opengis.cs.CS_Projection;
import org.opengis.cs.CS_ProjectionParameter;

// Miscellaneous
import java.util.Map;
import java.util.Arrays;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import javax.units.Unit;

import net.seas.util.XClass;
import net.seas.util.XArray;
import net.seas.opengis.ct.Parameter;
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
    private final Parameter[] parameters;

    /**
     * Creates a projection.
     *
     * @param name           Name to give new object.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param ellipsoid      Ellipsoid parameter. If non-null <code>"semi_major"</code> and
     *                       <code>"semi_minor"</code> parameters will be set according.
     * @param centre         Central meridian and latitude of origin, in degrees. If non-null,
     *                       <code>"central_meridian"</code> and <code>"latitude_of_origin"</code>
     *                       will be set according.
     */
    public Projection(final String name, final String classification, final Ellipsoid ellipsoid, final Point2D centre)
    {this(name, classification, getParameters(ellipsoid, centre));}

    /**
     * Creates a projection.
     *
     * @param name           Name to give new object.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param parameters     Parameters to use for projection, in metres or degrees.
     */
    public Projection(final String name, final String classification, final Parameter[] parameters)
    {
        super(name);
        ensureNonNull("classification", classification);
        ensureNonNull("parameters",     parameters);
        this.classification = classification;
        this.parameters     = clone(parameters);
    }

    /**
     * Creates a projection.
     *
     * @param properties     Properties to give new object.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param parameters     Parameters to use for projection, in metres or degrees.
     */
    Projection(final Map<String,String> properties, final String classification, final Parameter[] parameters)
    {
        super(properties);
        ensureNonNull("classification", classification);
        ensureNonNull("parameters",     parameters);
        this.classification = classification;
        this.parameters     = clone(parameters);
    }

    /**
     * Returns a deep clone of the specified parameters array.
     */
    private static Parameter[] clone(Parameter[] parameters)
    {
        parameters = (Parameter[]) parameters.clone();
        for (int i=0; i<parameters.length; i++)
        {
            ensureNonNull("parameters", parameters, i);
            parameters[i] = parameters[i].clone();
        }
        return parameters;
    }

    /**
     * Transform an ellipsoid and point argument into a parameters array.
     */
    private static Parameter[] getParameters(final Ellipsoid ellipsoid, final Point2D centre)
    {
        int n=0;
        final Parameter[] param = new Parameter[4];
        if (ellipsoid!=null)
        {
            final Unit axisUnit = ellipsoid.getAxisUnit();
            param[n++] = new Parameter("semi_major", Unit.METRE.convert(ellipsoid.getSemiMajorAxis(), axisUnit));
            param[n++] = new Parameter("semi_minor", Unit.METRE.convert(ellipsoid.getSemiMinorAxis(), axisUnit));
        }
        if (centre!=null)
        {
            param[n++] = new Parameter("central_meridian",   centre.getX());
            param[n++] = new Parameter("latitude_of_origin", centre.getY());
        }
        return XArray.resize(param, n);
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
    public Parameter getParameter(final int index)
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
    {return Parameter.getValue(parameters, name);}

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
    {return Parameter.getValue(parameters, name, defaultValue);}

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

    /**
     * Returns an OpenGIS interface for this projection.
     * The returned object is suitable for RMI use.
     */
    final CS_Projection toOpenGIS()
    {return new Export();}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link Projection} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends Info.Export implements CS_Projection
    {
        /**
         * Gets number of parameters of the projection.
         */
        public int getNumParameters() throws RemoteException
        {return Projection.this.getNumParameters();}

        /**
         * Gets an indexed parameter of the projection.
         */
        public CS_ProjectionParameter getParameter(final int index) throws RemoteException
        {
            final Parameter param = Projection.this.getParameter(index);
            return new CS_ProjectionParameter(param.name, param.value);
        }

        /**
         * Gets the projection classification name (e.g. 'Transverse_Mercator').
         */
        public String getClassName() throws RemoteException
        {return Projection.this.getClassName();}
    }
}
