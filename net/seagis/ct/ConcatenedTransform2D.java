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

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import net.seagis.resources.Geometry;


/**
 * Concatened transform where the resulting transform is two-dimensional. New
 * methods in this class are basically copy of {@link MathTransform#Abstract}
 * implementation.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ConcatenedTransform2D extends ConcatenedTransform implements MathTransform2D
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -7307709788564866500L;

    /**
     * Construct a concatenated transform.
     */
    public ConcatenedTransform2D(final MathTransformFactory provider, final MathTransform transform1, final MathTransform transform2)
    {super(provider, transform1, transform2);}

    /**
     * Check if transforms are compatibles
     * with this implementation.
     */
    protected boolean isValid()
    {return super.isValid() && getDimSource()==2 && getDimTarget()==2;}

    /**
     * Transforms the specified <code>ptSrc</code>
     * and stores the result in <code>ptDst</code>.
     */
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
    {
/* ---- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
   ---- END OF JDK 1.4 DEPENDENCIES ---- */
        // Basically a copy of MathTransform2D.Abstract implementation
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
     * Transform the specified shape.
     */
    public Shape createTransformedShape(final Shape shape) throws TransformException
    {
/* ---- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
   ---- END OF JDK 1.4 DEPENDENCIES ---- */
        // Basically a copy of MathTransform2D.Abstract implementation
        return MathTransform2D.Abstract.createTransformedShape(shape, null, this, null, Geometry.PARALLEL);
    }
}
