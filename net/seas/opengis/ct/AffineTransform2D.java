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

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import net.seas.opengis.pt.ConvexHull;

// Miscellaneous
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.util.XAffineTransform;


/**
 * Transforms two-dimensional coordinate points using an {@link AffineTransform}.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
final class AffineTransform2D extends MathTransform implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -7260613547208966035L;

    /**
     * The affine transform.
     */
    private final AffineTransform transform;

    /**
     * Construct an affine transform.
     */
    protected AffineTransform2D(final AffineTransform transform)
    {
        super("AffineTransform2D");
        this.transform = (AffineTransform) transform.clone();
    }

    /**
     * Transforms the specified <code>ptSrc</code>
     * and stores the result in <code>ptDst</code>.
     */
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst)
    {return transform.transform(ptSrc, ptDst);}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts)
    {transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts)
    {transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Transform the specified shape.
     */
    public Shape transform(final Shape shape)
    {return isIdentity() ? shape : transform.createTransformedShape(shape);}

    /**
     * Returns a Java2D affine transform equivalents to this math transform.
     * This is a convenience method for interoperability with
     * <a href="http://java.sun.com/products/java-media/2D">Java2D</a>.
     */
    public AffineTransform toAffineTransform2D()
    {return new AffineTransform(transform);}

    /**
     * Creates the inverse transform of this object.
     */
    public MathTransform inverse() throws NoninvertibleTransformException
    {
        try
        {
            return new AffineTransform2D(transform.createInverse());
        }
        catch (java.awt.geom.NoninvertibleTransformException exception)
        {
            throw new NoninvertibleTransformException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Gets the dimension of input points.
     */
    public int getDimSource()
    {return 2;}

    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget()
    {return 2;}

    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity()
    {return transform.isIdentity();}

    /**
     * Returns a hash value for this transform.
     */
    public int hashCode()
    {return transform.hashCode();}

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final AffineTransform2D that = (AffineTransform2D) object;
            return XClass.equals(this.transform, that.transform);
        }
        return false;
    }

    /**
     * Creates a transform by concatenating <code>this</code>
     * with the specified transform.
     */
    protected MathTransform concatenate(final MathTransform that)
    {
        if (this.isIdentity()) return that.getMathTransform();
        if (that.isIdentity()) return this.getMathTransform();
        if (that instanceof AffineTransform2D)
        {
            final AffineTransform product = new AffineTransform(((AffineTransform2D) that).transform);
            product.concatenate(this.transform);
            XAffineTransform.round(product);
            return new AffineTransform2D(product);
        }
        return super.concatenate(that);
    }
}
