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
import net.seas.opengis.pt.ConvexHull;
import net.seas.opengis.pt.CoordinatePoint;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;

// Miscellaneous
import net.seas.util.XClass;
import net.seas.resources.Resources;


/**
 * Concatened transform.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ConcatenedTransform extends MathTransform
{
    /**
     * The first math transform.
     */
    private final MathTransform transform1;

    /**
     * The second math transform.
     */
    private final MathTransform transform2;

    /**
     * Default constructor.
     */
    public ConcatenedTransform(final MathTransform transform1, final MathTransform transform2)
    {
        super("ConcatenedTransform");
        this.transform1 = transform1;
        this.transform2 = transform2;
        if (transform1.getDimTarget() != transform2.getDimSource())
        {
            throw new IllegalArgumentException(Resources.format(Clé.COORDINATE_SYSTEM_DIMENSION_MISMATCH¤2, transform1.getName(null), transform2.getName(null)));
        }
    }

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     */
    public CoordinatePoint transform(final CoordinatePoint ptSrc, final CoordinatePoint ptDst) throws TransformException
    {return transform2.transform(transform1.transform(ptSrc, null), ptDst);}

    /**
     * Transforms the specified <code>ptSrc</code>
     * and stores the result in <code>ptDst</code>.
     */
    public Point2D transform(final Point2D ptSrc, Point2D ptDst) throws TransformException
    {
        ptDst = transform1.transform(ptSrc, ptDst);
        return  transform2.transform(ptDst, ptDst);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        final double[] tmp = new double[numPts*transform1.getDimTarget()];
        transform1.transform(srcPts, srcOff, tmp, 0, numPts);
        transform2.transform(tmp, 0, dstPts, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        final float[] tmp = new float[numPts*transform1.getDimTarget()];
        transform1.transform(srcPts, srcOff, tmp, 0, numPts);
        transform2.transform(tmp, 0, dstPts, dstOff, numPts);
    }

    /**
     * Transform the specified shape.
     */
    public Shape createTransformedShape(final Shape shape) throws TransformException
    {return transform2.createTransformedShape(transform1.createTransformedShape(shape));}

    /**
     * Creates the inverse transform of this object.
     */
    public MathTransform inverse() throws NoninvertibleTransformException
    {return new ConcatenedTransform(transform2.inverse(), transform1.inverse());}

    /**
     * Gets the dimension of input points.
     */
    public int getDimSource()
    {return transform1.getDimSource();}

    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget()
    {return transform2.getDimTarget();}

    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity()
    {return transform1.isIdentity() && transform2.isIdentity();}

    /**
     * Returns a hash value for this transform.
     */
    public int hashCode()
    {return transform1.hashCode() + 37*transform2.hashCode();}

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final ConcatenedTransform that = (ConcatenedTransform) object;
            return XClass.equals(this.transform1, that.transform1) &&
                   XClass.equals(this.transform2, that.transform2);
        }
        return false;
    }

    /**
     * Creates a transform by concatenating <code>this</code>
     * with the specified transform.
     */
    protected MathTransform concatenate(MathTransform transform)
    {
        // TODO: How to detect if "transform.concatenate(transform1)" would be more efficient?
        transform = transform2.concatenate(transform);
        if (transform.equals(transform2)) return this;
        return new ConcatenedTransform(transform1, transform);
    }
}
