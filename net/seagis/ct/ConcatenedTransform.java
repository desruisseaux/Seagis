/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.ct;

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Matrix;
import net.seagis.pt.CoordinatePoint;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;

// Miscellaneous
import java.io.Serializable;


/**
 * Base class for concatened transform. Concatened transforms are
 * serializable if all their step transforms are serializables.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class ConcatenedTransform extends AbstractMathTransform implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 5772066656987558634L;

    /**
     * The math transform factory that created this concatened transform.
     * Will be used for creating the inverse transform when needed.
     */
    private final MathTransformFactory provider;

    /**
     * The first math transform.
     */
    protected final MathTransform transform1;

    /**
     * The second math transform.
     */
    protected final MathTransform transform2;

    /**
     * The inverse transform. This field
     * will be computed only when needed.
     */
    private transient MathTransform inverse;

    /**
     * Construct a concatenated transform.
     */
    public ConcatenedTransform(final MathTransformFactory provider, final MathTransform transform1, final MathTransform transform2)
    {
        this.provider   = provider;
        this.transform1 = transform1;
        this.transform2 = transform2;
        if (!isValid())
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_CANT_CONCATENATE_CS_$2,
                                               getName(transform1), getName(transform2)));
        }
    }

    /**
     * Returns a name for the specified coordinate system.
     */
    private static final String getName(final MathTransform transform)
    {
        if (transform instanceof AbstractMathTransform)
        {
            String name = ((AbstractMathTransform) transform).getName(null);
            if (name!=null && (name=name.trim()).length()!=0) return name;
        }
        return Utilities.getShortClassName(transform);
    }

    /**
     * Check if transforms are compatibles. The default
     * implementation check if transfert dimension match.
     */
    protected boolean isValid()
    {return transform1.getDimTarget() == transform2.getDimSource();}

    /**
     * Gets the dimension of input points.
     */
    public final int getDimSource()
    {return transform1.getDimSource();}

    /**
     * Gets the dimension of output points.
     */
    public final int getDimTarget()
    {return transform2.getDimTarget();}

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     */
    public CoordinatePoint transform(final CoordinatePoint ptSrc, CoordinatePoint ptDst) throws TransformException
    {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
------- END OF JDK 1.4 DEPENDENCIES ---*/
        //  Note: If we know that the transfert dimension is the same than source
        //        and target dimension, then we don't need to use an intermediate
        //        point. This optimization is done in ConcatenedTransformDirect.
        return transform2.transform(transform1.transform(ptSrc, null), ptDst);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
------- END OF JDK 1.4 DEPENDENCIES ---*/
        //  Note: If we know that the transfert dimension is the same than source
        //        and target dimension, then we don't need to use an intermediate
        //        buffer. This optimization is done in ConcatenedTransformDirect.
        final double[] tmp = new double[numPts*transform1.getDimTarget()];
        transform1.transform(srcPts, srcOff, tmp, 0, numPts);
        transform2.transform(tmp, 0, dstPts, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
------- END OF JDK 1.4 DEPENDENCIES ---*/
        //  Note: If we know that the transfert dimension is the same than source
        //        and target dimension, then we don't need to use an intermediate
        //        buffer. This optimization is done in ConcatenedTransformDirect.
        final float[] tmp = new float[numPts*transform1.getDimTarget()];
        transform1.transform(srcPts, srcOff, tmp, 0, numPts);
        transform2.transform(tmp, 0, dstPts, dstOff, numPts);
    }

    /**
     * Creates the inverse transform of this object.
     */
    public synchronized final MathTransform inverse() throws NoninvertibleTransformException
    {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
------- END OF JDK 1.4 DEPENDENCIES ---*/
        if (inverse==null)
        {
            inverse = provider.createConcatenatedTransform(transform2.inverse(), transform1.inverse());
            if (inverse instanceof ConcatenedTransform)
            {
                ((ConcatenedTransform) inverse).inverse = this;
            }
        }
        return inverse;
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point (never <code>null</code>).
     * @throws TransformException if the derivative can't be evaluated at the specified point.
     */
    public Matrix derivative(final CoordinatePoint point) throws TransformException
    {
        final Matrix matrix1 = transform1.derivative(point);
        final Matrix matrix2 = transform2.derivative(transform1.transform(point, null));
        return matrix2.multiply(matrix1);
    }

    /**
     * Tests whether this transform does not move any points.
     * Default implementation check if the two transforms are
     * identity. This a way too conservative aproach, but it
     * it doesn't hurt since ConcatenedTransform should not
     * have been created if it were to result in an identity
     * transform (this case should have been detected earlier).
     */
    public final boolean isIdentity()
    {return transform1.isIdentity() && transform2.isIdentity();}

    /**
     * Returns a hash value for this transform.
     */
    public final int hashCode()
    {return transform1.hashCode() + 37*transform2.hashCode();}

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public final boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final ConcatenedTransform that = (ConcatenedTransform) object;
            return Utilities.equals(this.transform1, that.transform1) &&
                   Utilities.equals(this.transform2, that.transform2);
        }
        return false;
    }
}
