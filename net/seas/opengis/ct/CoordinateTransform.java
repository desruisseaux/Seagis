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
import org.opengis.ct.CT_TransformType;
import org.opengis.ct.CT_MathTransform;
import org.opengis.ct.CT_CoordinateTransformation;
import org.opengis.cs.CS_CoordinateSystem;

// Coordinates
import net.seas.opengis.cs.Info;
import net.seas.opengis.cs.CoordinateSystem;

// Remote Method Invocation
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XClass;


/**
 * Describes a coordinate transformation. This service class transforms
 * a coordinate point position between two different coordinate reference systems. A coordinate
 * transformation interface establishes an association between a source and a target coordinate
 * reference system, and provides operations for transforming coordinates in the source coordinate
 * reference system to coordinates in the target coordinate reference system. These coordinate
 * systems can be ground or image coordinates. In general mathematics, "transformation" is the
 * general term for mappings between coordinate systems (see tensor analysis).
 * <br><br>
 * For a ground coordinate point, if the transformation depends only on mathematically
 * derived parameters (as in a cartographic projection), then this is an ISO conversion.
 * If the transformation depends on empirically derived parameters (as in datum
 * transformations), then this is an ISO transformation.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_CoordinateTransformation
 */
public abstract class CoordinateTransform extends MathTransform
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -6765195667756471802L;

    /**
     * Construct a coordinate transformation.
     *
     * @param name The coordinate transform name.
     */
    public CoordinateTransform(final String name)
    {super(name);}

    /**
     * Gets the semantic type of transform.
     * For example, a datum transformation or a coordinate conversion.
     */
    public abstract TransformType getTransformType();

    /**
     * Gets the source coordinate system.
     */
    public abstract CoordinateSystem getSourceCS();

    /**
     * Gets the target coordinate system.
     */
    public abstract CoordinateSystem getTargetCS();
    
    /**
     * Gets the dimension of input points.
     */
    public int getDimSource()
    {return getSourceCS().getDimension();}
    
    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget()
    {return getTargetCS().getDimension();}

    /**
     * Returns a hash value for this
     * coordinate transformation.
     */
    public int hashCode()
    {
        int code = 7851236;
        CoordinateSystem cs;
        if ((cs=getSourceCS()) != null) code = code*37 + cs.hashCode();
        if ((cs=getTargetCS()) != null) code = code*37 + cs.hashCode();
        return code;
    }

    /**
     * Compares the specified object with this
     * coordinate transformation for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final CoordinateTransform that = (CoordinateTransform) object;
            return XClass.equals(this.getTransformType(), that.getTransformType()) &&
                   XClass.equals(this.getSourceCS(),      that.getSourceCS()     ) &&
                   XClass.equals(this.getTargetCS(),      that.getTargetCS()     );
        }
        return false;
    }

    /**
     * Returns an OpenGIS interface for this math transform.
     * The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid too early class loading of OpenGIS interface.
     */
    Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link CoordinateTransform} for use with OpenGIS.
     * This wrapper is a good place to check for non-implemented
     * OpenGIS methods (just check for methods throwing
     * {@link UnsupportedOperationException}). This class
     * is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    final class Export extends RemoteObject implements CT_CoordinateTransformation
    {
        /**
         * The originating adapter.
         */
        protected final Adapters adapters;

        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {this.adapters = (Adapters)adapters;}

        /**
         * Returns the underlying math transform.
         */
        public final CoordinateTransform unwrap()
        {return CoordinateTransform.this;}

        /**
         * Name of transformation.
         */
        public String getName() throws RemoteException
        {return CoordinateTransform.this.getName(null);}

        /**
         * Authority which defined transformation and parameter values.
         */
        public String getAuthority() throws RemoteException
        {return CoordinateTransform.this.getAuthority(null);}

        /**
         * Code used by authority to identify transformation.
         */
        public String getAuthorityCode() throws RemoteException
        {return CoordinateTransform.this.getAuthorityCode(null);}

        /**
         * Gets the provider-supplied remarks.
         */
        public String getRemarks() throws RemoteException
        {return CoordinateTransform.this.getRemarks(null);}

        /**
         * Human readable description of domain in source coordinate system.
         */
        public String getAreaOfUse() throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Semantic type of transform.
         */
        public CT_TransformType getTransformType() throws RemoteException
        {return adapters.export(CoordinateTransform.this.getTransformType());}

        /**
         * Source coordinate system.
         */
        public CS_CoordinateSystem getSourceCS() throws RemoteException
        {return adapters.CS.export(CoordinateTransform.this.getSourceCS());}

        /**
         * Target coordinate system.
         */
        public CS_CoordinateSystem getTargetCS() throws RemoteException
        {return adapters.CS.export(CoordinateTransform.this.getTargetCS());}

        /**
         * Gets math transform.
         */
        public CT_MathTransform getMathTransform() throws RemoteException
        {return adapters.export(CoordinateTransform.this.getMathTransform());}
    }
}
