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

// OpenGIS dependencies
import org.opengis.ct.CT_Parameter;
import org.opengis.ct.CT_DomainFlags;
import org.opengis.ct.CT_TransformType;
import org.opengis.ct.CT_MathTransform;
import org.opengis.ct.CT_MathTransformFactory;
import org.opengis.ct.CT_CoordinateTransformation;

// Parameters
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.ParameterListDescriptorImpl;

// Miscellaneous
import java.rmi.RemoteException;
import net.seas.util.XArray;


/**
 * <FONT COLOR="#FF6633">Provide methods for interoperability with
 * <code>org.opengis.ct</code> package.</FONT>  All methods accept
 * null argument. All OpenGIS objects are suitable for RMI use.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Adapters
{
    /**
     * Default adapters. Will be constructed
     * only when first requested.
     */
    private static Adapters DEFAULT;

    /**
     * The underlying adapters for the <code>net.seas.opengis.cs</code> package.
     */
    public final net.seas.opengis.cs.Adapters CS;

    /**
     * The underlying adapters for the <code>net.seas.opengis.pt</code> package.
     */
    public final net.seas.opengis.pt.Adapters PT;

    /**
     * Default constructor.
     *
     * @param CS The underlying adapters for the <code>net.seas.opengis.cs</code> package.
     */
    protected Adapters(final net.seas.opengis.cs.Adapters CS)
    {
        this.CS = CS;
        this.PT = CS.PT;
    }

    /**
     * Gets the default adapters.
     */
    public static synchronized Adapters getDefault()
    {
        if (DEFAULT==null)
        {
            DEFAULT = new Adapters(net.seas.opengis.cs.Adapters.getDefault());
        }
        return DEFAULT;
    }

    /**
     * Returns an OpenGIS interface for a math transform.
     */
    public CT_MathTransform export(final MathTransform transform)
    {return (transform!=null) ? (CT_MathTransform)transform.cachedMath(this) : null;}

    /**
     * Returns an OpenGIS interface for a math transform.
     */
    public CT_CoordinateTransformation export(final CoordinateTransform transform)
    {return (transform!=null) ? (CT_CoordinateTransformation)transform.cachedOpenGIS(this) : null;}

    /**
     * Returns an OpenGIS interface for a math transform factory.
     */
    public CT_MathTransformFactory export(final MathTransformFactory factory)
    {return (factory!=null) ? (CT_MathTransformFactory)factory.toOpenGIS(this) : null;}

    /**
     * Construct an array of OpenGIS structure from a parameter list.
     */
    public CT_Parameter[] export(final ParameterList parameters)
    {
        if (parameters==null) return null;
        final String[] names = parameters.getParameterListDescriptor().getParamNames();
        final CT_Parameter[] param = new CT_Parameter[names!=null ? names.length : 0];
        int count=0;
        for (int i=0; i<param.length; i++)
        {
            final String name = names[i];
            final Object value;
            try
            {
                value = parameters.getObjectParameter(name);
            }
            catch (IllegalStateException exception)
            {
                // No value and no default. Ignore...
                continue;
            }
            if (value instanceof Number)
            {
                param[count++] = new CT_Parameter(name, ((Number)value).doubleValue());
            }
        }
        return XArray.resize(param, count);
    }

    /**
     * Construct an OpenGIS enum from a transform type.
     */
    public CT_TransformType export(final TransformType type)
    {return (type!=null) ? new CT_TransformType(type.getValue()) : null;}

    /**
     * Construct an OpenGIS enum from a domain flag.
     */
    public CT_DomainFlags export(final DomainFlags flags)
    {return (flags!=null) ? new CT_DomainFlags(flags.getValue()) : null;}

    /**
     * Returns a math transform for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public MathTransform wrap(final CT_MathTransform transform) throws RemoteException
    {
        if (transform==null) return null;
        if (transform instanceof MathTransform.Export)
            return ((MathTransform.Export)transform).unwrap();
        return new MathTransformAdapter(transform);
    }

    /**
     * Returns a coordinate transform for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public CoordinateTransform wrap(final CT_CoordinateTransformation transform) throws RemoteException
    {
        if (transform==null) return null;
        if (transform instanceof CoordinateTransform.Export)
        {
            return (CoordinateTransform) ((CoordinateTransform.Export)transform).unwrap();
        }
        return new CoordinateTransformProxy(wrap(transform.getMathTransform()),
                                            wrap(transform.getTransformType()),
                                         CS.wrap(transform.getSourceCS()),
                                         CS.wrap(transform.getTargetCS()));
    }

    /**
     * Returns a parameter list for an array of OpenGIS structure.
     */
    public ParameterList wrap(final CT_Parameter[] parameters)
    {
        if (parameters==null) return null;
        int count=0;
        String[] paramNames   = new String[parameters.length];
        Class [] paramClasses = new Class [parameters.length];
        for (int i=0; i<parameters.length; i++)
        {
            final CT_Parameter param = parameters[i];
            if (param!=null)
            {
                paramNames  [count] = param.name;
                paramClasses[count] = Double.class;
                count++;
            }
        }
        paramNames   = XArray.resize(paramNames,   count);
        paramClasses = XArray.resize(paramClasses, count);
        final ParameterList list = new ParameterListImpl(new ParameterListDescriptorImpl(null, paramNames, paramClasses, null, null));
        for (int i=0; i<paramNames.length; i++)
        {
            list.setParameter(paramNames[i], parameters[i].value);
        }
        return list;
    }

    /**
     * Construct a transform type from an OpenGIS enum.
     */
    public TransformType wrap(final CT_TransformType type)
    {return (type!=null) ? TransformType.getEnum(type.value) : null;}

    /**
     * Construct a domain flag from an OpenGIS enum.
     */
    public DomainFlags wrap(final CT_DomainFlags flags)
    {return (flags!=null) ? DomainFlags.getEnum(flags.value) : null;}
}
