/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
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
import net.seagis.pt.Matrix;
import net.seagis.pt.CoordinatePoint;
import net.seagis.pt.MismatchedDimensionException;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;

// Resources
import java.util.Locale;
import net.seagis.resources.Geometry;
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * Provides default implementations for most of the methods in the
 * {@link MathTransform} interface. This provides a convenient base
 * class from which other transform classes can be easily derived.
 * In addition, <code>AbstractMathTransform</code> provides implementation
 * for the methods in the {@link MathTransform2D} interfaces, but is
 * <strong>not</strong> declared to implements {@link MathTransform2D}.
 * This is up to subclasses to add the <code>implements MathTransform2D</code>
 * declaration if the the transform is know to maps two-dimensional coordinate
 * systems.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class AbstractMathTransform implements MathTransform
{
    /**
     * Construct a math transform.
     */
    public AbstractMathTransform()
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
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     * The default implementation invokes {@link #transform(double[],int,double[],int,int)}
     * using a temporary array of doubles.
     *
     * @param ptSrc the specified coordinate point to be transformed.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws MismatchedDimensionException if this transform
     *         doesn't map two-dimensional coordinate systems.
     * @throws TransformException if the point can't be transformed.
     *
     * @see MathTransform2D#transform(Point2D,Point2D)
     */
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
    {
        if (getDimSource()!=2 || getDimTarget()!=2)
        {
            throw new MismatchedDimensionException();
        }
        final double[] ord = new double[] {ptSrc.getX(), ptSrc.getY()};
        this.transform(ord, 0, ord, 0, 1);
        if (ptDst!=null)
        {
            ptDst.setLocation(ord[0], ord[1]);
            return ptDst;
        }
        else return new Point2D.Double(ord[0], ord[1]);
    }

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
     * Transform the specified shape. The default implementation compute
     * quadratic curves using three points for each shape's segments.
     *
     * @param  shape Shape to transform.
     * @return Transformed shape, or <code>shape</code> if
     *         this transform is the identity transform.
     * @throws IllegalStateException if this transform doesn't map 2D coordinate systems.
     * @throws TransformException if a transform failed.
     *
     * @see MathTransform2D#createTransformedShape(Shape)
     */
    public Shape createTransformedShape(final Shape shape) throws TransformException
    {return isIdentity() ? shape : createTransformedShape(shape, null, null, Geometry.PARALLEL);}

    /**
     * Transforme une forme g�om�trique. Cette m�thode copie toujours les coordonn�es
     * transform�es dans un nouvel objet. La plupart du temps, elle produira un objet
     * {@link GeneralPath}. Elle peut aussi retourner des objets {@link Line2D} ou
     * {@link QuadCurve2D} si une telle simplification est possible.
     *
     * @param  shape  Forme g�om�trique � transformer.
     * @param  preTr  Transformation affine � appliquer <em>avant</em> de transformer la forme
     *                <code>shape</code>, ou <code>null</code> pour ne pas en appliquer.
     *                Cet argument sera surtout utile lors des transformations inverses.
     * @param  postTr Transformation affine � appliquer <em>apr�s</em> avoir transform�e la
     *                forme <code>shape</code>, ou <code>null</code> pour ne pas en appliquer.
     *                Cet argument sera surtout utile lors des transformations directes.
     * @param quadDir Direction des courbes quadratiques ({@link Geometry#HORIZONTAL}
     *                ou {@link Geometry#PARALLEL}).
     *
     * @return La forme g�om�trique transform�e.
     * @throws MismatchedDimensionException if this transform
     *         doesn't map two-dimensional coordinate systems.
     * @throws TransformException Si une transformation a �chou�.
     */
    final Shape createTransformedShape(final Shape shape, final AffineTransform preTr, final AffineTransform postTr, final int quadDir) throws TransformException
    {
        if (getDimSource()!=2 || getDimTarget()!=2)
        {
            throw new MismatchedDimensionException();
        }
        final PathIterator    it = shape.getPathIterator(preTr);
        final GeneralPath   path = new GeneralPath(it.getWindingRule());
        final Point2D.Float ctrl = new Point2D.Float();
        final double[]    buffer = new double[6];

        double ax=0, ay=0;  // Coordonn�es du dernier point avant la projection.
        double px=0, py=0;  // Coordonn�es du dernier point apr�s la projection.
        int indexCtrlPt=0;  // Index du point de contr�le dans 'buffer'.
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
                     * Ferme la forme g�om�trique, puis continue la boucle. On utilise une
                     * instruction 'continue' plut�t que 'break' car il ne faut pas ex�cuter
                     * le code qui suit ce 'switch'.
                     */
                    path.closePath();
                    continue;
                }
                case PathIterator.SEG_MOVETO:
                {
                    /*
                     * M�morise les coordonn�es sp�cifi�es (avant et apr�s les avoir
                     * projet�es), puis continue la boucle. On utilise une instruction
                     * 'continue' plut�t que 'break' car il ne faut pas ex�cuter le
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
                     * Place dans 'buffer[2,3]' les coordonn�es
                     * d'un point qui se trouve sur la droite:
                     *
                     *  x = 0.5*(x1+x2)
                     *  y = 0.5*(y1+y2)
                     *
                     * Ce point sera trait� apr�s le 'switch', d'o�
                     * l'utilisation d'un 'break' plut�t que 'continue'.
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
                     * Place dans 'buffer[0,1]' les coordonn�es
                     * d'un point qui se trouve sur la courbe:
                     *
                     *  x = 0.5*(ctrlx + 0.5*(x1+x2))
                     *  y = 0.5*(ctrly + 0.5*(y1+y2))
                     *
                     * Ce point sera trait� apr�s le 'switch', d'o�
                     * l'utilisation d'un 'break' plut�t que 'continue'.
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
                     * Place dans 'buffer[0,1]' les coordonn�es
                     * d'un point qui se trouve sur la courbe:
                     *
                     *  x = 0.25*(1.5*(ctrlx1+ctrlx2) + 0.5*(x1+x2));
                     *  y = 0.25*(1.5*(ctrly1+ctrly2) + 0.5*(y1+y2));
                     *
                     * Ce point sera trait� apr�s le 'switch', d'o�
                     * l'utilisation d'un 'break' plut�t que 'continue'.
                     *
                     * NOTE: Le point calcul� est bien sur la courbe, mais n'est pas n�cessairement repr�sentatif.
                     *       Cet algorithme remplace les deux points de contr�les par un seul, ce qui se traduit
                     *       par une perte de souplesse qui peut donner de mauvais r�sultats si la courbe cubique
                     *       �tait bien tordue. Projeter une courbe cubique ne me semble pas �tre un probl�me simple,
                     *       mais heureusement ce cas devrait �tre assez rare. Il se produira le plus souvent si on
                     *       essaye de projeter un cercle ou une ellipse, auxquels cas l'algorithme actuel donnera
                     *       quand m�me des r�sultats tol�rables.
                     */
                    indexLastPt = 4;
                    indexCtrlPt = 0;
                    buffer[0] = 0.25*(1.5*(buffer[0]+buffer[2]) + 0.5*(ax + (ax=buffer[4])));
                    buffer[1] = 0.25*(1.5*(buffer[1]+buffer[3]) + 0.5*(ay + (ay=buffer[5])));
                    break;
                }
            }
            /*
             * Applique la transformation sur les points qui se
             * trouve dans le buffer, puis ajoute ces points �
             * la forme g�om�trique projet�e comme une courbe
             * quadratique.
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
         * La projection de la forme g�om�trique est termin�e. Applique
         * une transformation affine si c'�tait demand�e, puis retourne
         * une version si possible simplifi�e de la forme g�om�trique.
         */
        if (postTr!=null)
        {
            path.transform(postTr);
        }
        return Geometry.toPrimitive(path);
    }

    /**
     * Gets the derivative of this transform at a point. The default
     * implementation invokes {@link #derivative(CoordinatePoint)}.
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point as a 2x2 matrix.
     * @throws MismatchedDimensionException if the input dimension is not 2.
     * @throws TransformException if the derivative can't be evaluated at the specified point.
     *
     * @see MathTransform2D#derivative(Point2D)
     */
    public Matrix derivative(final Point2D point) throws TransformException
    {return derivative(new CoordinatePoint(point));}

    /**
     * Gets the derivative of this transform at a point. The default
     * implementation throws an {@link UnsupportedOperationException}
     * <strong>(note: this default implementation may change in a future
     * version)</strong>.
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point (never <code>null</code>).
     * @throws TransformException if the derivative can't be evaluated at the specified point.
     */
    public Matrix derivative(final CoordinatePoint point) throws TransformException
    {throw new UnsupportedOperationException("Matrix derivative not yet implemented");}

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
     * Returns a string repr�sentation of this transform.
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