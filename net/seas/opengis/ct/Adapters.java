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

// Remote Method Invocation
import java.rmi.RemoteException;


/**
 * Provide static methods for interoperability with
 * <code>org.opengis.ct</code> package. All static
 * methods accept null argument.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Adapters
{
    /**
     * Do not allow creation of
     * instance of this class.
     */
    private Adapters()
    {}

    /**
     * Returns an OpenGIS interface for a math transform.
     */
    public static CT_MathTransform export(final MathTransform transform)
    {return (transform!=null) ? (CT_MathTransform)transform.toOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a math transform factory.
     */
    public static CT_MathTransformFactory export(final MathTransformFactory factory)
    {return (factory!=null) ? (CT_MathTransformFactory)factory.toOpenGIS() : null;}

    /**
     * Construct an OpenGIS structure from a named parameter.
     * Changes to the returned structure will not affect the original parameter.
     */
    public static CT_Parameter export(final Parameter parameter)
    {return (parameter!=null) ? new CT_Parameter(parameter.name, parameter.value) : null;}

    /**
     * Construct an OpenGIS enum from a transform type.
     */
    public static CT_TransformType export(final TransformType type)
    {return (type!=null) ? new CT_TransformType(type.value) : null;}

    /**
     * Construct an OpenGIS enum from a domain flag.
     */
    public static CT_DomainFlags export(final DomainFlags flags)
    {return (flags!=null) ? new CT_DomainFlags(flags.value) : null;}

    /**
     * Returns a math transform for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static MathTransform wrap(final CT_MathTransform transform) throws RemoteException
    {
        if (transform==null) return null;
        if (transform instanceof MathTransform.Export)
            return ((MathTransform.Export)transform).unwrap();
        return new MathTransformAdapter(transform);
    }

    /**
     * Construct a named parameter from an OpenGIS structure.
     * Changes to the returned parameter will not affect the original structure.
     */
    public static Parameter wrap(final CT_Parameter parameter)
    {return (parameter!=null) ? new Parameter(parameter.name, parameter.value) : null;}

    /**
     * Construct a transform type from an OpenGIS enum.
     */
    public static TransformType wrap(final CT_TransformType type)
    {return (type!=null) ? TransformType.getEnum(type.value) : null;}

    /**
     * Construct a domain flag from an OpenGIS enum.
     */
    public static DomainFlags wrap(final CT_DomainFlags flags)
    {return (flags!=null) ? DomainFlags.getEnum(flags.value) : null;}
}
