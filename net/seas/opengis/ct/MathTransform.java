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
import net.seas.opengis.cs.Info;
import net.seas.opengis.pt.ConvexHull;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.pt.MismatchedDimensionException;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;
import net.seas.awt.geom.Geometry;

// Remote Method Invocation
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

// References
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XClass;
import net.seas.resources.Resources;

// For JavaDoc only
import javax.media.jai.PerspectiveTransform;


/**
 * Transforms multi-dimensional coordinate points. This class transforms
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
 */
public abstract class MathTransform extends Info
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -8343162886432673368L;

    /**
     * OpenGIS object returned by {@link #cachedOpenGIS}.
     * It may be a hard or a weak reference.
     */
    private transient Object proxy;

    /**
     * OpenGIS object returned by {@link #cachedMath}.
     * It may be a hard or a weak reference.
     */
    private transient Object proxyMT;

    /**
     * Construct a math transform with the specified name.
     *
     * @param name A default name for this math transform.
     */
    public MathTransform(final String name)
    {super(name);}

    /**
     * Gets flags classifying domain points within a convex hull.
     * Conceptually, each of the (usually infinite) points inside the convex hull is
     * tested against the source domain. The flags of all these tests are then combined.
     * In practice, implementations of different transforms will use different short-cuts
     * to avoid doing an infinite number of tests.
     * <br><br>
     * Convex hull are not yet implemented in the <code>net.seas.opengis</code>
     * package. Consequently, the default implementation for this method always
     * throws a {@link UnsupportedOperationException}.
     *
     * @param  hull The convex hull.
     * @return flags classifying domain points within the convex hull.
     */
    public DomainFlags getDomainFlags(final ConvexHull hull)
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     * If <code>ptDst</code> is <code>null</code> or do not have the expected number of
     * dimensions, a new {@link CoordinatePoint} object is allocated and then the result
     * of the transformation is stored in this object. In either case, <code>ptDst</code>,
     * which contains the transformed point, is returned for convenience. If <code>ptSrc</code>
     * and <code>ptDst</code> are the same object and dimension matches, the input point is
     * correctly overwritten with the transformed point.
     *
     * @param ptSrc the specified coordinate point to be transformed.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and storing the result in <code>ptDst</code>, or a newly
     *         created point if <code>ptDst</code> was null or its dimension
     *         was not suitable.
     * @throws MismatchedDimensionException if <code>ptSrc</code>
     *         doesn't have the expected dimension.
     * @throws TransformException if the point can't be transformed.
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
        if (ptDst==null || ptDst.getDimension()!=targetDim)
        {
            ptDst = new CoordinatePoint(targetDim);
        }
        transform(ptSrc.ord, 0, ptDst.ord, 0, 1);
        return ptDst;
    }

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in
     * <code>ptDst</code>. This is an convenience method for interoperability
     * with <a href="http://java.sun.com/products/java-media/2D">Java2D</a>.
     * This method will fail if this transform doesn't map two-dimensional spaces.
     *
     * @param ptSrc the specified coordinate point to be transformed.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed,
     *         or if this transform does not map two-dimensional spaces.
     */
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
    {
        if (getDimSource()==2 && getDimTarget()==2)
        {
            final double[] ord = new double[] {ptSrc.getX(), ptSrc.getY()};
            transform(ord, 0, ord, 0, 1);
            if (ptDst!=null)
            {
                ptDst.setLocation(ord[0], ord[1]);
                return ptDst;
            }
            else return new Point2D.Double(ord[0], ord[1]);
        }
        else throw new TransformException(Resources.format(Clé.NOT_A_TRANSFORM2D));
    }

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
    public abstract void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts) throws TransformException;

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
     * Transform the specified shape. This is an convenience method for interoperability
     * with <a href="http://java.sun.com/products/java-media/2D">Java2D</a>. This method
     * will fail if this transform doesn't map two-dimensional spaces.
     * <br><br>
     * This method may replace straight lines by quadratic curves when applicable. It may
     * also do the opposite (replace curves by straight lines). The returned shape doesn't
     * need to have the same number of points than the original shape.
     *
     * @param  shape Shape to transform.
     * @return Transformed shape, or <code>shape</code> if
     *         this transform is the identity transform.
     * @throws TransformException if a transform failed, or if
     *         this transform does not map two-dimensional spaces.
     */
    public Shape transform(final Shape shape) throws TransformException
    {return isIdentity() ? shape : transform(shape, null, null, Geometry.PARALLEL);}

    /**
     * Transforme une forme géométrique. Cette méthode copie toujours les coordonnées
     * transformées dans un nouvel objet. La plupart du temps, elle produira un objet
     * {@link GeneralPath}. Elle peut aussi retourner des objets {@link Line2D} ou
     * {@link QuadCurve2D} si une telle simplification est possible.
     *
     * @param  shape  Forme géométrique à transformer.
     * @param  preTr  Transformation affine à appliquer <em>avant</em> de transformer la forme
     *                <code>shape</code>, ou <code>null</code> pour ne pas en appliquer.
     *                Cet argument sera surtout utile lors des transformations inverses.
     * @param  postTr Transformation affine à appliquer <em>après</em> avoir transformée la
     *                forme <code>shape</code>, ou <code>null</code> pour ne pas en appliquer.
     *                Cet argument sera surtout utile lors des transformations directes.
     * @param quadDir Direction des courbes quadratiques ({@link Geometry#HORIZONTAL}
     *                ou {@link Geometry#PARALLEL}).
     *
     * @return La forme géométrique transformée.
     * @throws TransformException Si une transformation a échoué.
     */
    final Shape transform(final Shape shape, final AffineTransform preTr, final AffineTransform postTr, final int quadDir) throws TransformException
    {
        if (getDimSource()!=2 || getDimTarget()!=2)
        {
            throw new TransformException(Resources.format(Clé.NOT_A_TRANSFORM2D));
        }
        final PathIterator    it = shape.getPathIterator(preTr);
        final GeneralPath   path = new GeneralPath(it.getWindingRule());
        final Point2D.Float ctrl = new Point2D.Float();
        final double[]    buffer = new double[6];

        double ax=0, ay=0;  // Coordonnées du dernier point avant la projection.
        double px=0, py=0;  // Coordonnées du dernier point après la projection.
        int indexCtrlPt=0;  // Index du point de contrôle dans 'buffer'.
        int indexLastPt=0;  // Index du dernier point dans 'buffer'.
        for (; !it.isDone(); it.next())
        {
            switch (it.currentSegment(buffer))
            {
                default:
                {
                    throw new IllegalPathStateException();
                }
                case PathIterator.SEG_CLOSE:
                {
                    /*
                     * Ferme la forme géométrique, puis continue la boucle. On utilise une
                     * instruction 'continue' plutôt que 'break' car il ne faut pas exécuter
                     * le code qui suit ce 'switch'.
                     */
                    path.closePath();
                    continue;
                }
                case PathIterator.SEG_MOVETO:
                {
                    /*
                     * Mémorise les coordonnées spécifiées (avant et après les avoir
                     * projetées), puis continue la boucle. On utilise une instruction
                     * 'continue' plutôt que 'break' car il ne faut pas exécuter le
                     * code qui suit ce 'switch'.
                     */
                    ax = buffer[0];
                    ay = buffer[1];
                    transform(buffer, 0, buffer, 0, 1);
                    path.moveTo((float) (px=buffer[0]),
                                (float) (py=buffer[1]));
                    continue;
                }
                case PathIterator.SEG_LINETO:
                {
                    /*
                     * Place dans 'buffer[2,3]' les coordonnées
                     * d'un point qui se trouve sur la droite:
                     *
                     *  x = 0.5*(x1+x2)
                     *  y = 0.5*(y1+y2)
                     *
                     * Ce point sera traité après le 'switch', d'où
                     * l'utilisation d'un 'break' plutôt que 'continue'.
                     */
                    indexLastPt = 0;
                    indexCtrlPt = 2;
                    buffer[2] = 0.5*(ax + (ax=buffer[0]));
                    buffer[3] = 0.5*(ay + (ay=buffer[1]));
                    break;
                }
                case PathIterator.SEG_QUADTO:
                {
                    /*
                     * Place dans 'buffer[0,1]' les coordonnées
                     * d'un point qui se trouve sur la courbe:
                     *
                     *  x = 0.5*(ctrlx + 0.5*(x1+x2))
                     *  y = 0.5*(ctrly + 0.5*(y1+y2))
                     *
                     * Ce point sera traité après le 'switch', d'où
                     * l'utilisation d'un 'break' plutôt que 'continue'.
                     */
                    indexLastPt = 2;
                    indexCtrlPt = 0;
                    buffer[0] = 0.5*(buffer[0] + 0.5*(ax + (ax=buffer[2])));
                    buffer[1] = 0.5*(buffer[1] + 0.5*(ay + (ay=buffer[3])));
                    break;
                }
                case PathIterator.SEG_CUBICTO:
                {
                    /*
                     * Place dans 'buffer[0,1]' les coordonnées
                     * d'un point qui se trouve sur la courbe:
                     *
                     *  x = 0.25*(1.5*(ctrlx1+ctrlx2) + 0.5*(x1+x2));
                     *  y = 0.25*(1.5*(ctrly1+ctrly2) + 0.5*(y1+y2));
                     *
                     * Ce point sera traité après le 'switch', d'où
                     * l'utilisation d'un 'break' plutôt que 'continue'.
                     *
                     * NOTE: Le point calculé est bien sur la courbe, mais n'est pas nécessairement représentatif.
                     *       Cet algorithme remplace les deux points de contrôles par un seul, ce qui se traduit
                     *       par une perte de souplesse qui peut donner de mauvais résultats si la courbe cubique
                     *       était bien tordue. Projeter une courbe cubique ne me semble pas être un problème simple,
                     *       mais heureusement ce cas devrait être assez rare. Il se produira le plus souvent si on
                     *       essaye de projeter un cercle ou une ellipse, auxquels cas l'algorithme actuel donnera
                     *       quand même des résultats tolérables.
                     */
                    indexLastPt = 4;
                    indexCtrlPt = 0;
                    buffer[0] = 0.25*(1.5*(buffer[0]+buffer[2]) + 0.5*(ax + (ax=buffer[4])));
                    buffer[1] = 0.25*(1.5*(buffer[1]+buffer[3]) + 0.5*(ay + (ay=buffer[5])));
                    break;
                }
            }
            /*
             * Applique la projection cartographique sur les
             * points qui se trouve dans le buffer, puis ajoute
             * ces points à la forme géométrique projetée comme
             * une courbe quadratique.
             */
            transform(buffer, 0, buffer, 0, 2);
            if (Geometry.parabolicControlPoint(px, py,
                                               buffer[indexCtrlPt], buffer[indexCtrlPt+1],
                                               buffer[indexLastPt], buffer[indexLastPt+1],
                                               quadDir, ctrl)!=null)
            {
                path.quadTo(ctrl.x, ctrl.y, (float) (px=buffer[indexLastPt+0]),
                                            (float) (py=buffer[indexLastPt+1]));
            }
            else path.lineTo((float) (px=buffer[indexLastPt+0]),
                             (float) (py=buffer[indexLastPt+1]));
        }
        /*
         * La projection de la forme géométrique est terminée. Applique
         * une transformation affine si c'était demandée, puis retourne
         * une version si possible simplifiée de la forme géométrique.
         */
        if (postTr!=null)
        {
            path.transform(postTr);
        }
        return Geometry.toPrimitive(path);
    }

    /**
     * Creates the inverse transform of this object. The target of the inverse transform
     * is the source of the original. The source of the inverse transform is the target
     * of the original. Using the original transform followed by the inverse's transform
     * will result in an identity map on the source coordinate space, when allowances for
     * error are made. This method may fail if the transform is not one to one. However,
     * all cartographic projections should succeed.
     * <br><br>
     * The default implementation returns <code>this</code> if this transform is an identity
     * transform, and throws a {@link NoninvertibleTransformException} otherwise. Subclasses
     * should override this method.
     *
     * @return The inverse transform.
     * @throws NoninvertibleTransformException if the transform can't be inversed.
     */
    public MathTransform inverse() throws NoninvertibleTransformException
    {
        if (isIdentity()) return this;
        throw new NoninvertibleTransformException(Resources.format(Clé.NONINVERTIBLE_TRANSFORM¤1, getName(null)));
    }

    /**
     * Gets the dimension of input points.
     */
    public abstract int getDimSource();

    /**
     * Gets the dimension of output points.
     */
    public abstract int getDimTarget();

    /**
     * Tests whether this transform does not move any points.
     *
     * @return <code>true</code> if this <code>MathTransform</code> is
     *         an identity transform; <code>false</code> otherwise.
     */
    public abstract boolean isIdentity();

    /**
     * Creates a transform by concatenating <code>this</code> with the specified
     * transform. The default implementation construct a {@link ConcatenedTransform}
     * instance. Subclasses can override this method if they know how to concatenate
     * efficiently some special transforms.
     */
    MathTransform concatenate(final MathTransform that)
    {
        if (this.isIdentity()) return that.getMathTransform();
        if (that.isIdentity()) return this.getMathTransform();
        return new ConcatenedTransform(this.getMathTransform(), that.getMathTransform());
    }

    /**
     * Gets the math transform. This method is
     * overrided by {@link CoordinateTransformProxy}.
     */
    MathTransform getMathTransform()
    {return this;}

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
    {return (object!=null && getClass().equals(object.getClass()));}

    /**
     * Returns a string représentation of this transform.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
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
     * Note 1: The returned type is a generic {@link Object} in order
     *         to avoid too early class loading of OpenGIS interface.
     *
     * Note 2: We do NOT want this method to override Info.toOpenGIS(),
     *         since the returned object do not implements CS_Info. The
     *         package-private access do the trick.
     */
    Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}

    /**
     * Returns an OpenGIS interface for this info.
     * This method first look in the cache. If no
     * interface was previously cached, then this
     * method create a new adapter  and cache the
     * result.
     *
     * @param adapters The originating {@link Adapters}.
     */
    final synchronized Object cachedOpenGIS(final Object adapters)
    {
        if (proxy!=null)
        {
            if (proxy instanceof Reference)
            {
                final Object ref = ((Reference) proxy).get();
                if (ref!=null) return ref;
            }
            else return proxy;
        }
        final Object opengis = toOpenGIS(adapters);
        proxy = new WeakReference(opengis);
        return opengis;
    }

    /**
     * Returns an OpenGIS interface for this math.
     * This method first look in the cache. If no
     * interface was previously cached, then this
     * method create a new adapter  and cache the
     * result.
     *
     * @param adapters The originating {@link Adapters}.
     */
    final synchronized Object cachedMath(final Object adapters)
    {
        if (proxyMT!=null)
        {
            if (proxyMT instanceof Reference)
            {
                final Object ref = ((Reference) proxyMT).get();
                if (ref!=null) return ref;
            }
            else return proxyMT;
        }
        final Object opengis = new Export(adapters);
        proxy = new WeakReference(opengis);
        return opengis;
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
    final class Export extends RemoteObject implements CT_MathTransform
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
        public final MathTransform unwrap()
        {return MathTransform.this;}

        /**
         * Gets flags classifying domain points within a convex hull.
         */
        public CT_DomainFlags getDomainFlags(final double[] ord) throws RemoteException
        {return adapters.export(MathTransform.this.getDomainFlags(new ConvexHull(ord)));}

        /**
         * Gets transformed convex hull.
         */
        public double[] getCodomainConvexHull(final double[] ord) throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Gets a Well-Known text representation of this object.
         */
        public String getWKT() throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Gets an XML representation of this object.
         */
        public String getXML() throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Transforms a coordinate point.
         */
        public PT_CoordinatePoint transform(final PT_CoordinatePoint cp) throws RemoteException
        {
            try
            {
                final PT_CoordinatePoint point=new PT_CoordinatePoint();
                point.ord = new double[MathTransform.this.getDimTarget()];
                MathTransform.this.transform(cp.ord, 0, point.ord, 0, 1);
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
            final int dimSource = MathTransform.this.getDimSource();
            final int dimTarget = MathTransform.this.getDimTarget();
            if ((ord.length % dimSource)!=0)
            {
                throw new IllegalArgumentException(Resources.format(Clé.ILLEGAL_ARRAY_LENGTH_FOR_DIMENSION¤1, new Integer(dimSource)));
            }
            final int     count = ord.length/dimSource;
            final double[] dest = (dimSource==dimTarget) ? ord : new double[count*dimTarget];
            try
            {
                MathTransform.this.transform(ord, 0, dest, 0, count);
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
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Creates the inverse transform of this object.
         */
        public CT_MathTransform inverse() throws RemoteException
        {
            try
            {
                return adapters.export(MathTransform.this.inverse());
            }
            catch (NoninvertibleTransformException exception)
            {
                throw new RemoteException(exception.getLocalizedMessage(), exception);
            }
        }

        /**
         * Gets the dimension of input points.
         */
        public int getDimSource() throws RemoteException
        {return MathTransform.this.getDimSource();}

        /**
         * Gets the dimension of output points.
         */
        public int getDimTarget() throws RemoteException
        {return MathTransform.this.getDimTarget();}

        /**
         * Tests whether this transform does not move any points.
         */
        public boolean isIdentity() throws RemoteException
        {return MathTransform.this.isIdentity();}
    }
}
