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
 */
package net.seagis.ct;

// OpenGIS dependencies
import org.opengis.pt.PT_Matrix;
import org.opengis.pt.PT_CoordinatePoint;
import org.opengis.ct.CT_MathTransform;
import org.opengis.ct.CT_DomainFlags;

// OpenGIS dependencies (SEAGIS)
import net.seagis.ct.DomainFlags;
import net.seagis.pt.CoordinatePoint;
import net.seagis.ct.TransformException;
import net.seagis.ct.NoninvertibleTransformException;

// Miscellaneous
import java.util.Locale;
import java.util.Arrays;
import java.io.Serializable;
import java.rmi.RemoteException;
import net.seagis.resources.Utilities;


/**
 * Wrap an {@link CT_MathTransform} into a {@link MathTransform}.
 * This class is provided for compatibility with OpenGIS. It is
 * serializable if the underlying {@link CT_MathTransform} is
 * serializable too.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class MathTransformAdapter extends AbstractMathTransform implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 4497134108375420674L;

    /**
     * The OpenGIS math transform.
     */
    protected final CT_MathTransform transform;

    /**
     * Dimension of output points.
     */
    private final int dimSource;

    /**
     * Dimension of input points.
     */
    private final int dimTarget;

    /**
     * <code>true</code> if this transform does not move any points.
     */
    private final boolean isIdentity;

    /**
     * The inverse transform. This field
     * will be computed only when needed.
     */
    protected transient MathTransformAdapter inverse;

    /**
     * Construct an adapter.
     *
     * @throws RemoteException if a remote call failed.
     */
    public MathTransformAdapter(final CT_MathTransform transform) throws RemoteException
    {
        this.transform  = transform;
        this.dimSource  = transform.getDimSource();
        this.dimTarget  = transform.getDimTarget();
        this.isIdentity = transform.isIdentity();
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     *
     * @throws TransformException if the points can't be
     *         transformed, or if a remote call failed.
     */
    public final void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        try
        {
            if (srcOff==0 && dstOff==0 && srcPts==dstPts && dimSource==dimTarget && numPts*dimSource==srcPts.length)
            {
                // Special optimization without intermediate buffer.
                final double[] array = transform.transformList(srcPts);
                if (array!=dstPts)
                {
                    System.arraycopy(array, 0, dstPts, 0, array.length);
                    Arrays.fill(dstPts, array.length, dstPts.length, Double.NaN);
                }
            }
            else
            {
                // The following array way be larger than necessary, but we make
                // it large enough to give a change to 'transformList' to reuse it.
                double[] array = new double[numPts*Math.max(dimSource, dimTarget)];
                System.arraycopy(srcPts, srcOff, array, 0, numPts*dimSource);
                array = transform.transformList(array);
                System.arraycopy(array, 0, dstPts, dstOff, array.length);
                Arrays.fill(dstPts, array.length, numPts*dimTarget, Double.NaN);
            }
        }
        catch (RemoteException exception)
        {
            throw new TransformException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     *
     * @throws TransformException if the points can't be
     *         transformed, or if a remote call failed.
     */
    public final void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        try
        {
            // The following array way be larger than necessary, but we make
            // it large enough to give a change to 'transformList' to reuse it.
            double[] array = new double[numPts*Math.max(dimSource, dimTarget)];
            for (int i=numPts*dimSource; --i>=0;) array[i]=srcPts[i+srcOff];
            array = transform.transformList(array);
            for (int i=array.length; --i>=0;) dstPts[i+dstOff] = (float)array[i];
            Arrays.fill(dstPts, array.length, numPts*dimTarget, Float.NaN);
        }
        catch (RemoteException exception)
        {
            throw new TransformException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Creates the inverse transform of this object.
     *
     * @throws NoninvertibleTransformException if the inverse transform
     *         can't be created, or if a remote call failed.
     */
    public synchronized MathTransform inverse() throws NoninvertibleTransformException
    {
        if (inverse==null) try
        {
            inverse = new MathTransformAdapter(transform.inverse());
            inverse.inverse = this;
        }
        catch (RemoteException exception)
        {
            throw new NoninvertibleTransformException(exception.getLocalizedMessage(), exception);
        }
        return inverse;
    }

    /**
     * Gets the dimension of input points.
     */
    public final int getDimSource()
    {return dimSource;}

    /**
     * Gets the dimension of output points.
     */
    public final int getDimTarget()
    {return dimTarget;}

    /**
     * Tests whether this transform does not move any points.
     */
    public final boolean isIdentity()
    {return isIdentity;}

    /**
     * Returns the underlying OpenGIS interface.
     */
    final Object toOpenGIS(final Object adapters)
    {return transform;}

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public final boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            return Utilities.equals(((MathTransformAdapter) object).transform, transform);
        }
        return false;
    }
}
