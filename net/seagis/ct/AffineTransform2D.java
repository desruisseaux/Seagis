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

// OpenGIS (SEAS) dependencies
//import net.seagis.pt.ConvexHull;
import net.seagis.pt.CoordinatePoint;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

// Miscellaneous
import java.io.Serializable;
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;
import net.seagis.resources.XAffineTransform;


/**
 * Transforms two-dimensional coordinate points using an {@link AffineTransform}.
 *
 * @version 1.00
 * @author Martin Desruisseaux
 */
final class AffineTransform2D extends XAffineTransform implements MathTransform2D
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -5299837898367149069L;

    /**
     * The inverse transform. This field
     * will be computed only when needed.
     */
    private transient AffineTransform2D inverse;

    /**
     * Construct an affine transform.
     */
    protected AffineTransform2D(final AffineTransform transform)
    {super(transform);}

    /**
     * Throws an {@link UnsupportedOperationException} when a mutable method
     * is invoked, since <code>AffineTransform2D</code> must be immutable.
     */
    protected void checkPermission()
    {throw new UnsupportedOperationException(Resources.format(ResourceKeys.ERROR_UNMODIFIABLE_AFFINE_TRANSFORM));}

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
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     */
    public CoordinatePoint transform(final CoordinatePoint ptSrc, CoordinatePoint ptDst)
    {
        if (ptDst==null)
        {
            ptDst = new CoordinatePoint(2);
        }
        transform(ptSrc.ord, 0, ptDst.ord, 0, 1);
        return ptDst;
    }

    /**
     * Creates the inverse transform of this object.
     */
    public synchronized MathTransform inverse() throws NoninvertibleTransformException
    {
        if (inverse==null) try
        {
            if (!isIdentity())
            {
                inverse = new AffineTransform2D(createInverse());
                inverse.inverse = this;
            }
            else inverse = this;
        }
        catch (java.awt.geom.NoninvertibleTransformException exception)
        {
            throw new NoninvertibleTransformException(exception.getLocalizedMessage(), exception);
        }
        return inverse;
    }
}
