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

// Coordinates
import net.seas.opengis.cs.Info;
import net.seas.opengis.cs.CoordinateSystem;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XClass;


/**
 * Describes a coordinate transformation. This service class transforms a coordinate point
 * position between two different coordinate reference systems. A coordinate transformation
 * interface establishes an association between a source and a target coordinate reference
 * system, and provides operations for transforming coordinates in the source coordinate
 * reference system to coordinates in the target coordinate reference system. These coordinate
 * systems can be ground or image coordinates. In general mathematics, "transformation" is the
 * general term for mappings between coordinate systems (see tensor analysis).
 * <br><br>
 * For a ground coordinate point, if the transformation depends only on mathematically derived
 * parameters (as in a cartographic projection), then this is an ISO conversion. If the transformation
 * depends on empirically derived parameters (as in datum transformations), then this is an ISO
 * transformation.
 * <br><br>
 * The <code>CoordinateTransformation</code> class only describes a coordinate
 * transformation, it does not actually perform the transform operation on points.
 * To transform points you must use a {@link MathTransform}. The math transform will
 * transform positions in the source coordinate system into positions in the target
 * coordinate system.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_CoordinateSystem
 */
public abstract class CoordinateTransformation extends Info
{
    /**
     * Serial number for interoperability with different versions.
     */
    //private static final long serialVersionUID = ?;

    /**
     * Construct a coordinate transformation.
     *
     * @param name The coordinate transform name.
     */
    protected CoordinateTransformation(final String name)
    {super(name);}

    /**
     * Human readable description of domain in source coordinate system.
     */
    public String getAreaOfUse()
    {return null;}

    /**
     * Gets the semantic type of transform.
     * For example, a datum transformation
     * or a coordinate conversion.
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
     * Gets the math transform.
     */
    public abstract MathTransform getMathTransform();

    /**
     * Returns a hash value for this
     * coordinate transformation.
     */
    public int hashCode()
    {
        int code = 0;
        CoordinateSystem cs;
        if ((cs=getSourceCS()) != null) code ^= cs.hashCode();
        if ((cs=getTargetCS()) != null) code ^= cs.hashCode();
        return code;
    }

    /**
     * Compares the specified object with this
     * coordinate transformation for equality.
     */
    public boolean equals(final Object object)
    {return (object instanceof CoordinateTransformation) && equals((CoordinateTransformation)object);}

    /**
     * Compares the specified object with this
     * coordinate transformation for equality.
     */
    final boolean equals(final CoordinateTransformation that)
    {
        return super.equals(that) &&
               XClass.equals(this.getAreaOfUse(),     that.getAreaOfUse()    ) &&
               XClass.equals(this.getTransformType(), that.getTransformType()) &&
               XClass.equals(this.getSourceCS(),      that.getSourceCS()     ) &&
               XClass.equals(this.getTargetCS(),      that.getTargetCS()     );
               // We do NOT check MathTransform, since creating MathTransform
               // may be a costly operation. MathTransform should be completly
               // determined by the above parameters, i.e. if all parameters
               // are equal, created MathTransform should be equal too.
    }
}
