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
import net.seagis.pt.Matrix;

// Miscellaneous
import java.io.Serializable;
import net.seagis.resources.Utilities;
import net.seagis.resources.XAffineTransform;


/**
 * Transforms multi-dimensional coordinate points using an {@link Matrix}.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
final class MatrixTransform extends MathTransform.Abstract implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 784363941978607191L;

    /**
     * The matrix.
     */
    private final Matrix matrix;

    /**
     * Construct a transform.
     */
    protected MatrixTransform(final Matrix matrix)
    {this.matrix = (Matrix) matrix.clone();}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts)
    {matrix.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts)
    {matrix.transform(srcPts, srcOff, dstPts, dstOff, numPts);}

    /**
     * Returns the matrix.
     */
    public Matrix getMatrix()
    {return (Matrix) matrix.clone();}

    /**
     * Gets the dimension of input points.
     */
    public int getDimSource()
    {return matrix.getSize()-1;}

    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget()
    {return matrix.getSize()-1;}

    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity()
    {return matrix.isIdentity();}

    /**
     * Returns a hash value for this transform.
     */
    public int hashCode()
    {return matrix.hashCode();}

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final MatrixTransform that = (MatrixTransform) object;
            return Utilities.equals(this.matrix, that.matrix);
        }
        return false;
    }
}
