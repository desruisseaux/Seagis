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
import org.opengis.pt.PT_Matrix;
import org.opengis.pt.PT_CoordinatePoint;
import org.opengis.ct.CT_MathTransform;
import org.opengis.ct.CT_DomainFlags;

// OpenGIS (SEAS) dependencies
import net.seas.opengis.pt.ConvexHull;
import net.seas.opengis.ct.DomainFlags;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.NoninvertibleTransformException;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XClass;
import java.rmi.RemoteException;


/**
 * Wrap an {@link CT_MathTransform} into a {@link MathTransform}.
 * This class is provided for compatibility with OpenGIS.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class MathTransformAdapter extends MathTransform
{
    /**
     * The OpenGIS math transform.
     */
    private final CT_MathTransform transform;

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
    public void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        try
        {
            double[] array = new double[numPts*Math.max(dimSource, dimTarget)];
            System.arraycopy(srcPts, srcOff, array, 0, numPts*dimSource);
            array = transform.transformList(array);
            System.arraycopy(array, 0, dstPts, dstOff, numPts*dimTarget);
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
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        try
        {
            double[] array = new double[numPts*Math.max(dimSource, dimTarget)];
            for (int i=numPts*dimSource; --i>=0;) array[i]=srcPts[i+srcOff];
            array = transform.transformList(array);
            for (int i=numPts*dimTarget; --i>=0;) dstPts[i+dstOff] = (float)array[i];
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
    public MathTransform inverse() throws NoninvertibleTransformException
    {
        try
        {
            return new MathTransformAdapter(transform.inverse());
        }
        catch (RemoteException exception)
        {
            throw new NoninvertibleTransformException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Gets the dimension of input points.
     */
    public int getDimSource()
    {return dimSource;}

    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget()
    {return dimTarget;}

    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity()
    {return isIdentity;}

    /**
     * Returns the underlying OpenGIS interface.
     */
    public CT_MathTransform toOpenGIS()
    {return transform;}

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public boolean equals(final Object object)
    {return super.equals(object) && XClass.equals(((MathTransformAdapter) object).transform, transform);}
}
