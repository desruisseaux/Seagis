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

// OpenGIS dependencies (SEAS)
import net.seas.opengis.cs.Info;
import net.seas.opengis.pt.ConvexHull;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cs.CoordinateSystem;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XClass;


/**
 * A coordinate transform delegating the transform to an other math transform.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_CoordinateTransformation
 */
final class CoordinateTransformProxy extends CoordinateTransform
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -7130168630676058705L;

    /**
     * The transform type.
     */
    private final TransformType type;

    /**
     * The source coordinate system.
     */
    private final CoordinateSystem sourceCS;

    /**
     * The destination coordinate system.
     */
    private final CoordinateSystem targetCS;

    /**
     * The underlying math transform.
     */
    private final MathTransform transform;

    /**
     * The inverse transform. This field
     * will be computed only when needed.
     */
    private transient CoordinateTransformProxy inverse;

    /**
     * Construct a coordinate transformation.
     *
     * @param transform The math transform.
     * @param type      The transform type.
     * @param sourceCS  The source coordinate system.
     * @param targetCS  The destination coordinate system.
     */
    public CoordinateTransformProxy(final MathTransform transform, final TransformType type, final CoordinateSystem sourceCS, final CoordinateSystem targetCS)
    {
        super(getName(sourceCS, targetCS, null));
        ensureNonNull("type",      type);
        ensureNonNull("transform", transform);
        this.type      = type;
        this.sourceCS  = sourceCS;
        this.targetCS  = targetCS;
        this.transform = transform.getMathTransform();
        if (transform.getDimSource() != sourceCS.getDimension())
        {
            throw new IllegalArgumentException("sourceCS");
        }
        if (transform.getDimTarget() != targetCS.getDimension())
        {
            throw new IllegalArgumentException("targetCS");
        }
    }

    /**
     * Construct a default transformation name.
     *
     * @param sourceCS  The source coordinate system.
     * @param targetCS  The destination coordinate system.
     * @param locale    The locale, or <code>null</code> for
     *                  the default locale.
     */
    private static String getName(final CoordinateSystem sourceCS, final CoordinateSystem targetCS, final Locale locale)
    {
        ensureNonNull("sourceCS", sourceCS);
        ensureNonNull("targetCS", targetCS);
        return sourceCS.getName(locale)+"\u00A0\u21E8\u00A0"+targetCS.getName(locale);
    }

    /**
     * Gets the name of this coordinate transformation.
     *
     * @param locale The desired locale, or <code>null</code> for the default locale.
     */
    public String getName(final Locale locale)
    {return (locale!=null) ? getName(sourceCS, targetCS, locale) : super.getName(locale);}

    /**
     * Gets the semantic type of transform.
     * For example, a datum transformation
     * or a coordinate conversion.
     */
    public TransformType getTransformType()
    {return type;}

    /**
     * Gets the source coordinate system.
     */
    public CoordinateSystem getSourceCS()
    {return sourceCS;}

    /**
     * Gets the target coordinate system.
     */
    public CoordinateSystem getTargetCS()
    {return targetCS;}

    /**
     * Gets flags classifying domain points within a convex hull.
     */
    public DomainFlags getDomainFlags(final ConvexHull hull)
    {return transform.getDomainFlags(hull);}

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     */
    public CoordinatePoint transform(final CoordinatePoint ptSrc, CoordinatePoint ptDst) throws TransformException
    {return transform.transform(ptSrc, ptDst);}

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     */
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
    {return transform.transform(ptSrc, ptDst);}
    
    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException
    {transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts) throws TransformException
    {transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Transform the specified shape.
     */
    public Shape createTransformedShape(final Shape shape) throws TransformException
    {return transform.createTransformedShape(shape);}

    /**
     * Creates the inverse transform of this object.
     */
    public synchronized MathTransform inverse() throws NoninvertibleTransformException
    {
        if (inverse==null)
        {
            inverse = new CoordinateTransformProxy(transform.inverse(), type, targetCS, sourceCS);
            inverse.inverse = this;
        }
        return inverse;
    }
    
    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity()
    {return transform.isIdentity();}

    /**
     * Gets the math transform.
     */
    public MathTransform getMathTransform()
    {return transform;}
}
