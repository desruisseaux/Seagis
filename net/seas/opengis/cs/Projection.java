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

// Parameters
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptor;
import net.seas.opengis.ct.MathTransformProvider;
import net.seas.opengis.ct.MissingParameterException;

// Miscellaneous
import java.util.Map;
import java.util.Arrays;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import net.seas.util.XClass;
import net.seas.util.XArray;
import javax.units.Unit;


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
    private static final long serialVersionUID = 3127605870668403003L;

    /**
     * Classification string for projection (e.g. "Transverse_Mercator").
     */
    private final String classification;

    /**
     * Parameters to use for projection, in metres or degrees.
     */
    private final ParameterList parameters;

    /**
     * Convenience constructor for a projection using the WGS84 ellipsoid.
     *
     * @param name           Name to give new object.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param centre         Central meridian and latitude of origin, in degrees. If non-null,
     *                       <code>"central_meridian"</code> and <code>"latitude_of_origin"</code>
     *                       will be set according.
     */
    public Projection(final String name, final String classification, final Point2D centre)
    {this(name, classification, Ellipsoid.WGS84, centre);}

    /**
     * Convenience constructor for a projection using the specified ellipsoid.
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
    {
        super(name);
        ensureNonNull("classification", classification);
        this.classification = classification;
        parameters = new ParameterListImpl(MathTransformProvider.DEFAULT_PROJECTION_DESCRIPTOR);
        if (ellipsoid!=null)
        {
            final Unit axisUnit = ellipsoid.getAxisUnit();
            parameters.setParameter("semi_major", Unit.METRE.convert(ellipsoid.getSemiMajorAxis(), axisUnit));
            parameters.setParameter("semi_minor", Unit.METRE.convert(ellipsoid.getSemiMinorAxis(), axisUnit));
        }
        if (centre!=null)
        {
            parameters.setParameter("central_meridian",   centre.getX());
            parameters.setParameter("latitude_of_origin", centre.getY());
        }
    }

    /**
     * Creates a projection. The set of parameters (<code>parameters</code>) may be
     * queried with {@link net.seas.opengis.ct.MathTransformFactory#getParameterList}.
     *
     * @param name           Name to give new object.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param parameters     Parameters to use for projection, in metres or degrees.
     */
    public Projection(final String name, final String classification, final ParameterList parameters)
    {
        super(name);
        ensureNonNull("classification", classification);
        ensureNonNull("parameters",     parameters);
        this.classification = classification;
        this.parameters = clone(parameters);
    }

    /**
     * Creates a projection.
     *
     * @param properties     The set of properties.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param parameters     Parameters to use for projection, in metres or degrees.
     */
    Projection(final Map<String,Object> properties, final String classification, final ParameterList parameters)
    {
        super(properties);
        this.classification = classification;
        this.parameters     = parameters;
        // Accept null values.
    }

    /**
     * Returns a clone of a parameter list.
     */
    private static ParameterList clone(final ParameterList list)
    {
        if (list==null) return null;
        final ParameterListDescriptor descriptor = list.getParameterListDescriptor();
        final ParameterList copy = new ParameterListImpl(descriptor);
        final String[] names = descriptor.getParamNames();
        if (names!=null) for (int i=0; i<names.length; i++)
        {
            final String name = names[i];
            copy.setParameter(name, list.getObjectParameter(name));
        }
        return copy;
    }

    /**
     * Gets the projection classification name (e.g. "Transverse_Mercator").
     */
    public String getClassName()
    {return classification;}

    /**
     * Returns all parameters.
     */
    public ParameterList getParameters()
    {return clone(parameters);}

    /**
     * <FONT COLOR="#FF6633">Convenience method for fetching a parameter value.</FONT>
     * Search is case-insensitive and ignore leading and trailing blanks.
     *
     * @param  name Parameter to look for.
     * @return The parameter value.
     * @throws MissingParameterException if parameter <code>name</code> is not found.
     */
    public double getValue(final String name) throws MissingParameterException
    {return getValue(parameters, name, Double.NaN, true);}

    /**
     * <FONT COLOR="#FF6633">Convenience method for fetching a parameter value.</FONT>
     * Search is case-insensitive and ignore leading and trailing blanks.
     *
     * @param  name Parameter to look for.
     * @param  defaultValue Default value to return if
     *         parameter <code>name</code> is not found.
     * @return The parameter value, or <code>defaultValue</code>
     *         if the parameter <code>name</code> is not found.
     */
    public double getValue(final String name, final double defaultValue)
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
    private static double getValue(final ParameterList parameters, final String name, final double defaultValue, final boolean required) throws MissingParameterException
    {
        RuntimeException cause=null;
        if (parameters!=null)
        {
            final Object value;
            try
            {
                value = parameters.getObjectParameter(name.trim());
                if (value instanceof Number)
                    return ((Number) value).doubleValue();
            }
            catch (IllegalArgumentException exception)
            {
                // There is no parameter with the specified name.
                cause = exception;
            }
            catch (IllegalStateException exception)
            {
                // the parameter value is still NO_PARAMETER_DEFAULT
                cause = exception;
            }
        }
        if (!required) return defaultValue;
        final MissingParameterException exception = new MissingParameterException(null, name);
        if (cause!=null) exception.initCause(cause);
        throw exception;
    }

    /**
     * Returns a hash value for this projection.
     */
    public int hashCode()
    {
        int code = 45896321;
        if (classification!=null) code = code*37 + classification.hashCode();
        if (parameters    !=null) code = code*37 + parameters.hashCode();
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
                   XClass.equals(this.parameters,     that.parameters);
        }
        return false;
    }

    /**
     * Add more information inside the "[...]" part of {@link #toString}.
     * The default implementation add nothing. Subclasses will override
     * this method in order to complete string representation.
     */
    final void addString(final StringBuffer buffer)
    {
        super.addString(buffer);
        buffer.append(", ");
        buffer.append(getParameters().getParameterListDescriptor().getNumParameters());
        buffer.append(" parameters");
    }

    /**
     * Returns an OpenGIS interface for this projection.
     * The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid too early class loading of OpenGIS interface.
     */
    final Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}




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
         * The set of parameters. This array is constructed
         * only the first time it is needed.
         */
        private transient CS_ProjectionParameter[] parameters;

        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Gets number of parameters of the projection.
         */
        public int getNumParameters() throws RemoteException
        {
            final CS_ProjectionParameter[] parameters = getParameters();
            return (parameters!=null) ? parameters.length : 0;
        }

        /**
         * Gets an indexed parameter of the projection.
         */
        public CS_ProjectionParameter getParameter(final int index) throws RemoteException
        {
            final CS_ProjectionParameter[] parameters = getParameters();
            return (CS_ProjectionParameter) parameters[index].clone();
        }

        /**
         * Gets the projection classification name (e.g. 'Transverse_Mercator').
         */
        public String getClassName() throws RemoteException
        {return Projection.this.getClassName();}

        /**
         * Returns the set of parameters.
         */
        private synchronized CS_ProjectionParameter[] getParameters()
        {
            if (parameters==null)
            {
                parameters = adapters.export(Projection.this.getParameters());
            }
            return parameters;
        }
    }
}
