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

// OpenGIS (SEAGIS) dependencies
import net.seas.opengis.cs.Parameter;

// Remote Method Invocation
import java.rmi.RemoteException;


/**
 * Provide methods for interoperability with
 * <code>org.opengis.ct</code> package.  All
 * methods accept null argument. All OpenGIS
 * objects are suitable for RMI use.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Adapters
{
    /**
     * Default adapters.
     */
    public static final Adapters DEFAULT = new Adapters(net.seas.opengis.cs.Adapters.DEFAULT);

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
     * Construct an OpenGIS structure from a named parameter.
     * Changes to the returned structure will not affect the original parameter.
     */
    public CT_Parameter export(final Parameter parameter)
    {return (parameter!=null) ? new CT_Parameter(parameter.name, parameter.value) : null;}

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
        return new CoordinateTransformProxy(wrap(transform.getTransformType()),
                                         CS.wrap(transform.getSourceCS()),
                                         CS.wrap(transform.getTargetCS()),
                                            wrap(transform.getMathTransform()));
    }

    /**
     * Returns a parameter array for an OpenGIS structure array.
     */
    final Parameter[] wrap(final CT_Parameter[] parameters)
    {
        if (parameters==null) return null;
        final Parameter[] p=new Parameter[parameters.length];
        for (int i=0; i<parameters.length; i++)
            p[i] = wrap(parameters[i]);
        return p;
    }

    /**
     * Construct a named parameter from an OpenGIS structure.
     * Changes to the returned parameter will not affect the original structure.
     */
    public Parameter wrap(final CT_Parameter parameter)
    {return (parameter!=null) ? new Parameter(parameter.name, parameter.value) : null;}

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
