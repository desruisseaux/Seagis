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

// OpenGIS dependencies
import org.opengis.pt.PT_Matrix;
import org.opengis.pt.PT_CoordinatePoint;
import org.opengis.ct.CT_MathTransform;
import org.opengis.ct.CT_DomainFlags;

// OpenGIS (SEAS) dependencies
//import net.seagis.pt.ConvexHull;
import net.seagis.pt.CoordinatePoint;
import net.seagis.pt.MismatchedDimensionException;

// Remote Method Invocation
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

// Miscellaneous
import java.util.Locale;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;

// For JavaDoc only
import java.awt.geom.AffineTransform;
import javax.media.jai.PerspectiveTransform;
//import javax.media.j3d.Transform3D;


/**
 * Transforms multi-dimensional coordinate points. This interface transforms
 * coordinate value for a point given in the source coordinate system to
 * coordinate value for the same point in the target coordinate system.
 * In an ISO conversion, the transformation is accurate to within the
 * limitations of the computer making the calculations. In an ISO
 * transformation, where some of the operational parameters are derived
 * from observations, the transformation is accurate to within the
 * limitations of those observations.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_MathTransform
 * @see AffineTransform
 * @see PerspectiveTransform
 * @see Transform3D
 */
public interface MathTransform
{
    /**
     * Gets the dimension of input points.
     *
     * @see org.opengis.ct.CT_MathTransform#getDimSource
     */
    public abstract int getDimSource();

    /**
     * Gets the dimension of output points.
     *
     * @see org.opengis.ct.CT_MathTransform#getDimTarget
     */
    public abstract int getDimTarget();

    /*
     * Gets flags classifying domain points within a convex hull.
     * Conceptually, each of the (usually infinite) points inside the convex hull is
     * tested against the source domain. The flags of all these tests are then combined.
     * In practice, implementations of different transforms will use different short-cuts
     * to avoid doing an infinite number of tests.
     * <br><br>
     * Convex hull are not yet implemented in the <code>net.seagis</code>
     * package. Consequently, the default implementation for this method always
     * throws a {@link UnsupportedOperationException}.
     *
     * @param  hull The convex hull.
     * @return flags classifying domain points within the convex hull.
     *
     * @see org.opengis.ct.CT_MathTransform#getDomainFlags
     */
//  public DomainFlags getDomainFlags(final ConvexHull hull)
//  {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     * If <code>ptDst</code> is <code>null</code>, a new {@link CoordinatePoint} object is
     * allocated and then the result of the transformation is stored in this object. In either
     * case, <code>ptDst</code>, which contains the transformed point, is returned for convenience.
     * If <code>ptSrc</code> and <code>ptDst</code> are the same object, the input point is correctly
     * overwritten with the transformed point.
     *
     * @param ptSrc the specified coordinate point to be transformed.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and storing the result in <code>ptDst</code>, or a newly
     *         created point if <code>ptDst</code> was null.
     * @throws MismatchedDimensionException if <code>ptSrc</code> or
     *         <code>ptDst</code> doesn't have the expected dimension.
     * @throws TransformException if the point can't be transformed.
     *
     * @see org.opengis.ct.CT_MathTransform#transform
     */
    public abstract CoordinatePoint transform(CoordinatePoint ptSrc, CoordinatePoint ptDst) throws TransformException;

    /**
     * Transforms a list of coordinate point ordinal values.
     * This method is provided for efficiently transforming many points.
     * The supplied array of ordinal values will contain packed ordinal
     * values.  For example, if the source dimension is 3, then the ordinals
     * will be packed in this order:
     *
     * (<var>x<sub>0</sub></var>,<var>y<sub>0</sub></var>,<var>z<sub>0</sub></var>,
     *  <var>x<sub>1</sub></var>,<var>y<sub>1</sub></var>,<var>z<sub>1</sub></var> ...).
     *
     * The size of the passed array must be an integer multiple of
     * {@link #getDimSource}.
     *
     * @param srcPts the array containing the source point coordinates.
     * @param srcOff the offset to the first point to be transformed
     *               in the source array.
     * @param dstPts the array into which the transformed point
     *               coordinates are returned. May be the same
     *               than <code>srcPts</code>.
     * @param dstOff the offset to the location of the first
     *               transformed point that is stored in the
     *               destination array.
     * @param numPts the number of point objects to be transformed.
     * @throws TransformException if a point can't be transformed.
     *
     * @see org.opengis.ct.CT_MathTransform#transformList
     */
    public abstract void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException;

    /**
     * Transforms a list of coordinate point ordinal values.
     * This method is provided for efficiently transforming many points.
     * The supplied array of ordinal values will contain packed ordinal
     * values.  For example, if the source dimension is 3, then the ordinals
     * will be packed in this order:
     *
     * (<var>x<sub>0</sub></var>,<var>y<sub>0</sub></var>,<var>z<sub>0</sub></var>,
     *  <var>x<sub>1</sub></var>,<var>y<sub>1</sub></var>,<var>z<sub>1</sub></var> ...).
     *
     * The size of the passed array must be an integer multiple of
     * {@link #getDimSource}.
     *
     * @param srcPts the array containing the source point coordinates.
     * @param srcOff the offset to the first point to be transformed
     *               in the source array.
     * @param dstPts the array into which the transformed point
     *               coordinates are returned. May be the same
     *               than <code>srcPts</code>.
     * @param dstOff the offset to the location of the first
     *               transformed point that is stored in the
     *               destination array.
     * @param numPts the number of point objects to be transformed.
     * @throws TransformException if a point can't be transformed.
     */
    public abstract void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException;

    /**
     * Creates the inverse transform of this object. The target of the inverse transform
     * is the source of the original. The source of the inverse transform is the target
     * of the original. Using the original transform followed by the inverse's transform
     * will result in an identity map on the source coordinate space, when allowances for
     * error are made. This method may fail if the transform is not one to one. However,
     * all cartographic projections should succeed.
     *
     * @return The inverse transform.
     * @throws NoninvertibleTransformException if the transform can't be inversed.
     *
     * @see org.opengis.ct.CT_MathTransform#inverse
     */
    public abstract MathTransform inverse() throws NoninvertibleTransformException;

    /**
     * Tests whether this transform does not move any points.
     *
     * @return <code>true</code> if this <code>MathTransform</code> is
     *         an identity transform; <code>false</code> otherwise.
     *
     * @see org.opengis.ct.CT_MathTransform#isIdentity
     */
    public abstract boolean isIdentity();

    /**
     * Default implementation of {@link MathTransform}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static abstract class Abstract implements MathTransform
    {
        /**
         * Construct a math transform.
         */
        public Abstract()
        {}

        /**
         * Returns a human readable name, if available. If no name is available in
         * the specified locale,   then this method returns a name in an arbitrary
         * locale. If no name is available in any locale, then this method returns
         * <code>null</code>. The default implementation always returns <code>null</code>.
         *
         * @param  locale The desired locale, or <code>null</code> for a default locale.
         * @return The transform name localized in the specified locale if possible, or
         *         <code>null</code> if no name is available in any locale.
         */
        protected String getName(final Locale locale)
        {return null;}

        /**
         * Transforms the specified <code>ptSrc</code> and stores the result
         * in <code>ptDst</code>. The default implementation invokes
         * {@link #transform(double[],int,double[],int,int)}.
         */
        public CoordinatePoint transform(final CoordinatePoint ptSrc, CoordinatePoint ptDst) throws TransformException
        {
            final int  pointDim = ptSrc.getDimension();
            final int sourceDim = getDimSource();
            final int targetDim = getDimTarget();
            if (pointDim != sourceDim)
            {
                throw new MismatchedDimensionException(pointDim, sourceDim);
            }
            if (ptDst==null)
            {
                ptDst = new CoordinatePoint(targetDim);
            }
            else if (ptDst.getDimension()!=targetDim)
            {
                throw new MismatchedDimensionException(ptDst.getDimension(), targetDim);
            }
            transform(ptSrc.ord, 0, ptDst.ord, 0, 1);
            return ptDst;
        }

        /**
         * Transforms a list of coordinate point ordinal values. The default implementation
         * invokes {@link #transform(double[],int,double[],int,int)} using a temporary array
         * of doubles.
         */
        public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts) throws TransformException
        {
            final int dimSource = getDimSource();
            final int dimTarget = getDimTarget();
            final double[] tmpPts = new double[numPts*Math.max(dimSource, dimTarget)];
            for (int i=numPts*dimSource; --i>=0;)
                tmpPts[i] = srcPts[srcOff+i];
            transform(tmpPts, 0, tmpPts, 0, numPts);
            for (int i=numPts*dimTarget; --i>=0;)
                dstPts[dstOff+i] = (float)tmpPts[i];
        }

        /**
         * Creates the inverse transform of this object.
         * The default implementation returns <code>this</code> if this transform is an identity
         * transform, and throws a {@link NoninvertibleTransformException} otherwise. Subclasses
         * should override this method.
         */
        public MathTransform inverse() throws NoninvertibleTransformException
        {
            if (isIdentity()) return this;
            throw new NoninvertibleTransformException(Resources.format(ResourceKeys.ERROR_NONINVERTIBLE_TRANSFORM));
        }

        /**
         * Returns a hash value for this transform.
         */
        public int hashCode()
        {return getDimSource() + 37*getDimTarget();}

        /**
         * Compares the specified object with
         * this math transform for equality.
         */
        public boolean equals(final Object object)
        {
            // Do not check 'object==this' here, since this
            // optimization is usually done in subclasses.
            return (object!=null && getClass().equals(object.getClass()));
        }

        /**
         * Returns a string représentation of this transform.
         */
        public String toString()
        {
            final StringBuffer buffer=new StringBuffer(Utilities.getShortClassName(this));
            buffer.append('[');
            buffer.append(getDimSource());
            buffer.append("D \u2192 "); // Arrow -->
            buffer.append(getDimTarget());
            buffer.append("D]");
            return buffer.toString();
        }

        /**
         * Returns an OpenGIS interface for this math transform.
         * The returned object is suitable for RMI use.
         *
         * Note: The returned type is a generic {@link Object} in order
         *       to avoid too early class loading of OpenGIS interface.
         */
        Object toOpenGIS(final Object adapters)
        {return new MathTransformExport(adapters, this);}
    }
}


/////////////////////////////////////////////////////////////////////////
////////////////                                         ////////////////
////////////////             OPENGIS ADAPTER             ////////////////
////////////////                                         ////////////////
/////////////////////////////////////////////////////////////////////////

/**
 * Wrap a {@link MathTransform} for use with OpenGIS. This wrapper is a
 * good place to check for non-implemented OpenGIS methods  (just check
 * for methods throwing {@link UnsupportedOperationException}). This
 * class is suitable for RMI use.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class MathTransformExport extends RemoteObject implements CT_MathTransform
{
    /**
     * The originating adapter.
     */
    protected final Adapters adapters;

    /**
     * The enclosed math transform.
     */
    protected final MathTransform transform;

    /**
     * Construct a remote object.
     */
    protected MathTransformExport(final Object adapters, final MathTransform transform)
    {
        this.adapters  = (Adapters)adapters;
        this.transform = transform;
    }

    /**
     * Returns the underlying transform of the specified <code>MathTransformExport</code>.
     * If <code>object</code> is not an instance of <code>MathTransformExport</code>, then
     * it is returned as is.
     */
    public static Object unwrap(final Object object)
    {return (object instanceof MathTransformExport) ? ((MathTransformExport)object).transform : object;}

    /**
     * Gets flags classifying domain points within a convex hull.
     */
    public CT_DomainFlags getDomainFlags(final double[] ord) throws RemoteException
//  {return adapters.export(MathTransform.this.getDomainFlags(new ConvexHull(ord)));}
    {throw new UnsupportedOperationException("Convex hull not yet implemented");}

    /**
     * Gets transformed convex hull.
     */
    public double[] getCodomainConvexHull(final double[] ord) throws RemoteException
    {throw new UnsupportedOperationException("Convex hull not yet implemented");}

    /**
     * Gets a Well-Known text representation of this object.
     */
    public String getWKT() throws RemoteException
    {throw new UnsupportedOperationException("WKT parsing not yet implemented");}

    /**
     * Gets an XML representation of this object.
     */
    public String getXML() throws RemoteException
    {throw new UnsupportedOperationException("XML parsing not yet implemented");}

    /**
     * Gets the dimension of input points.
     */
    public int getDimSource() throws RemoteException
    {return transform.getDimSource();}

    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget() throws RemoteException
    {return transform.getDimTarget();}

    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity() throws RemoteException
    {return transform.isIdentity();}

    /**
     * Transforms a coordinate point.
     */
    public PT_CoordinatePoint transform(final PT_CoordinatePoint cp) throws RemoteException
    {
        try
        {
            final PT_CoordinatePoint point=new PT_CoordinatePoint();
            point.ord = new double[transform.getDimTarget()];
            transform.transform(cp.ord, 0, point.ord, 0, 1);
            return point;
        }
        catch (TransformException exception)
        {
            throw new RemoteException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public double[] transformList(final double[] ord) throws RemoteException
    {
        final int dimSource = transform.getDimSource();
        final int dimTarget = transform.getDimTarget();
        if ((ord.length % dimSource)!=0)
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_ILLEGAL_ARRAY_LENGTH_FOR_DIMENSION_$1, new Integer(dimSource)));
        }
        final int     count = ord.length/dimSource;
        final double[] dest = (dimSource==dimTarget) ? ord : new double[count*dimTarget];
        try
        {
            transform.transform(ord, 0, dest, 0, count);
            return dest;
        }
        catch (TransformException exception)
        {
            throw new RemoteException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     */
    public PT_Matrix derivative(PT_CoordinatePoint cp) throws RemoteException
    {throw new UnsupportedOperationException("Matrix derivative not yet implemented");}

    /**
     * Creates the inverse transform of this object.
     */
    public CT_MathTransform inverse() throws RemoteException
    {
        try
        {
            return adapters.export(transform.inverse());
        }
        catch (NoninvertibleTransformException exception)
        {
            throw new RemoteException(exception.getLocalizedMessage(), exception);
        }
    }
}
