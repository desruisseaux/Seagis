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

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;
import javax.media.jai.PerspectiveTransform;

// Miscellaneous
import java.io.Serializable;
import net.seagis.resources.Geometry;
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * Transforms two-dimensional coordinate points. {@link CoordinateTransformation#getMathTransform}
 * may returns instance of this interface when source and destination coordinate systems are both two
 * dimensional. <code>MathTransform2D</code> extends {@link MathTransform} by adding some methods
 * for easier interoperability with <A HREF="http://java.sun.com/products/java-media/2D/">Java2D</A>.
 * If the transformation is affine, then <code>MathTransform</code> may be an immutable instance of
 * {@link AffineTransform}.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see AffineTransform
 * @see PerspectiveTransform
 */
public interface MathTransform2D extends MathTransform
{
    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     * If <code>ptDst</code> is <code>null</code>, a new {@link Point2D} object is allocated
     * and then the result of the transformation is stored in this object. In either case,
     * <code>ptDst</code>, which contains the transformed point, is returned for convenience.
     * If <code>ptSrc</code> and <code>ptDst</code> are the same object, the input point is
     * correctly overwritten with the transformed point.
     *
     * @param ptSrc the specified coordinate point to be transformed.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed.
     */
    public abstract Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException;

    /**
     * Transform the specified shape. This method may replace straight lines by
     * quadratic curves when applicable. It may also do the opposite (replace
     * curves by straight lines). The returned shape doesn't need to have the
     * same number of points than the original shape.
     *
     * @param  shape Shape to transform.
     * @return Transformed shape, or <code>shape</code> if
     *         this transform is the identity transform.
     * @throws TransformException if a transform failed.
     */
    public abstract Shape createTransformedShape(final Shape shape) throws TransformException;

    /**
     * Default implementation of {@link MathTransform2D}.
     *
     * @author Martin Desruisseaux
     * @version 1.0
     */
    public static abstract class Abstract extends MathTransform.Abstract implements MathTransform2D
    {
        /**
         * Construct a math transform
         */
        public Abstract()
        {}

        /**
         * Gets the dimension of input points, which is 2.
         */
        public final int getDimSource()
        {return 2;}

        /**
         * Gets the dimension of output points, which is 2.
         */
        public final int getDimTarget()
        {return 2;}

        /**
         * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
         * The default implementation invokes {@link #transform(double[],int,double[],int,int)}
         * using a temporary array of doubles.
         */
        public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
        {
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
         * Transform the specified shape. The default implementation compute
         * quadratic curves using three points for each shape's segments.
         */
        public Shape createTransformedShape(final Shape shape) throws TransformException
        {return isIdentity() ? shape : createTransformedShape(shape, null, this, null, Geometry.PARALLEL);}

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
         * @param  tr     Transformation à appliquer sur la forme géométrique.
         * @param  postTr Transformation affine à appliquer <em>après</em> avoir transformée la
         *                forme <code>shape</code>, ou <code>null</code> pour ne pas en appliquer.
         *                Cet argument sera surtout utile lors des transformations directes.
         * @param quadDir Direction des courbes quadratiques ({@link Geometry#HORIZONTAL}
         *                ou {@link Geometry#PARALLEL}).
         *
         * @return La forme géométrique transformée.
         * @throws TransformException Si une transformation a échoué.
         */
        static Shape createTransformedShape(final Shape shape, final AffineTransform preTr, final MathTransform2D tr, final AffineTransform postTr, final int quadDir) throws TransformException
        {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            assert tr.getDimSource()==2 && tr.getDimTarget()==2 : tr;
------- END OF JDK 1.4 DEPENDENCIES ---*/
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
                        tr.transform(buffer, 0, buffer, 0, 1);
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
                 * Applique la transformation sur les points qui se
                 * trouve dans le buffer, puis ajoute ces points à
                 * la forme géométrique projetée comme une courbe
                 * quadratique.
                 */
                tr.transform(buffer, 0, buffer, 0, 2);
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
    }
}
