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
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;
import net.seas.awt.geom.Geometry;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XClass;
import net.seas.resources.Resources;

// For JavaDoc only
import javax.media.jai.PerspectiveTransform;


/**
 * Transforms multi-dimensional coordinate points.      This class transforms coordinate value
 * for a point given in the source coordinate system to coordinate value for the same point in
 * the target coordinate system. In an ISO conversion, the transformation is accurate to within
 * the limitations of the computer making the calculations. In an ISO transformation, where
 * some of the operational parameters are derived from observations, the transformation is
 * accurate to within the limitations of those observations.
 * <br><br>
 * If a client application wishes to query the source and target
 * coordinate systems of a transformation, then it should keep hold
 * of the {@link CoordinateTransformation} class, and use the
 * contained math transform object whenever it wishes to perform a
 * transform.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see AffineTransform
 * @see PerspectiveTransform
 * @see org.opengis.ct.CT_MathTransform
 */
public abstract class MathTransform
{
    /**
     * Default constructor.
     */
    protected MathTransform()
    {}

    /**
     * Returns a human readable name localized for the specified locale.
     * If no name is available for the specified locale, this method may
     * returns a name in an arbitrary locale. The default implementation
     * returns the class name.
     */
    public String getName(final Locale locale)
    {return XClass.getShortClassName(this);}

    /**
     * Gets flags classifying domain points within a convex hull.
     * Conceptually, each of the (usually infinite) points inside
     * the convex hull is tested against the source domain.  The
     * flags of all these tests are then combined.  In practice,
     * implementations of different transforms will use different
     * short-cuts to avoid doing an infinite number of tests.
     * <br><br>
     * Convex hull are not yet implemented in the <code>net.seas.opengis</code>
     * package. Consequently, the default implementation for thie method always
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
     *         and storing the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed.
     */
    public CoordinatePoint transform(final CoordinatePoint ptSrc, CoordinatePoint ptDst) throws TransformException
    {
        ptSrc.ensureDimensionMatch(getDimSource());
        if (ptDst==null || ptDst.getDimension()!=getDimTarget())
        {
            ptDst = new CoordinatePoint(getDimTarget());
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
                     * NOTE: LE POINT CALCULÉ EST BIEN SUR LA COURBE, MAIS N'EST PAS NÉCESSAIREMENT REPRÉSENTATIF.
                     *       CET ALGORITHME REMPLACE LES DEUX POINTS DE CONTRÔLES PAR UN SEUL, CE QUI SE TRADUIT
                     *       PAR UNE PERTE DE SOUPLESSE QUI PEUT DONNER DE MAUVAIS RÉSULTATS SI LA COURBE CUBIQUE
                     *       ÉTAIT BIEN TORDU. PROJETER UNE COURBE CUBIQUE NE ME SEMBLE PAS ÊTRE UN PROBLÈME SIMPLE,
                     *       MAIS HEUREUSEMENT CE CAS DEVRAIT ÊTRE ASSEZ RARE. IL SE PRODUIRA LE PLUS SOUVENT SI ON
                     *       ESSAYE DE PROJETER UN CERCLE OU UNE ELLIPSE, AUXQUELS CAS L'ALGORITHME ACTUEL DONNERA
                     *       QUAND MÊME DES RÉSULTATS TOLÉRABLES.
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
     * Creates the inverse transform of this object. The target of the inverse transform is
     * the source of the original. The source of the inverse transform is the target of the
     * original. Using the original transform followed by the inverse's transform will result
     * in an identity map on the source coordinate space, when allowances for error are made.
     * This method may fail if the transform is not one to one. However, all cartographic
     * projections should succeed.
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
     * Base class for inverse {@link MathTransform}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    abstract class Inverse extends MathTransform
    {
        /**
         * Default constructor.
         */
        protected Inverse()
        {}

        /**
         * Returns a human readable name localized for the specified locale.
         */
        public final String getName(final Locale locale)
        {return Resources.format(Clé.INVERSE_OF¤1, MathTransform.this.getName(locale));}

        /**
         * Returns the inverse transform of this object.
         */
        public final MathTransform inverse()
        {return MathTransform.this;}

        /**
         * Gets the dimension of input points.
         */
        public final int getDimSource()
        {return MathTransform.this.getDimTarget();}

        /**
         * Gets the dimension of output points.
         */
        public final int getDimTarget()
        {return MathTransform.this.getDimSource();}

        /**
         * Tests whether this transform does not move any points.
         */
        public final boolean isIdentity()
        {return MathTransform.this.isIdentity();}

        /**
         * Returns a hash value for this transform.
         */
        public final int hashCode()
        {return MathTransform.this.hashCode();}

        /**
         * Compares the specified object with
         * this math transform for equality.
         */
        public final boolean equals(final Object object)
        {
            if (object==this) return true;
            if (object instanceof Inverse)
            {
                final Inverse that = (Inverse) object;
                return this.inverse().equals(that.inverse());
            }
            else return false;
        }
    }




    /**
     * Base class for registration of {@link MathTransform} object. Instance of
     * this class allow the creation of transform objects from a classification
     * name.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static abstract class Registration
    {
        /**
         * The classification name. This name do
         * not contains leading or trailing blanks.
         */
        public final String classification;

        /**
         * Resources key for a human readable name. This
         * is used for {@link #getName} implementation.
         */
        private final int nameKey;

        /**
         * Construct a new registration.
         *
         * @param classification The classification name.
         * @param nameKey Resources key for a human readable name.
         *        This is used for {@link #getName} implementation.
         */
        protected Registration(final String classification, final int nameKey)
        {
            this.classification = classification.trim();
            this.nameKey        = nameKey;
        }

        /**
         * Returns a human readable name localized for the specified locale.
         * If no name is available for the specified locale, this method may
         * returns a name in an arbitrary locale.
         */
        public final String getName(final Locale locale)
        {return Resources.getResources(locale).getString(nameKey);}

        /**
         * Returns a set of default parameters. The returns array should contains
         * one element for every parameter supported by the registered transform.
         *
         * @return A set of default parameters.
         */
        public abstract Parameter[] getDefaultParameters();

        /**
         * Returns an objet for the specified parameters.
         *
         * @param  parameters The parameter values in standard units.
         * @return A {@link MathTransform} object of this classification.
         */
        public abstract MathTransform create(final Parameter[] parameters);
    }
}
