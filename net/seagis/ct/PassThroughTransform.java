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

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Matrix;
import net.seagis.pt.CoordinatePoint;
import net.seagis.pt.MismatchedDimensionException;

// Miscellaneous
import java.io.Serializable;
import net.seagis.resources.Utilities;


/**
 * Transform which passes through a subset of ordinates to another transform.
 * This allows transforms to operate on a subset of ordinates.  For example,
 * if you have (<var>latitude</var>,<var>longitude</var>,<var>height</var>)
 * coordinates, then you may wish to convert the height values from feet to
 * meters without affecting the latitude and longitude values.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
final class PassThroughTransform extends AbstractMathTransform implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1673997634240223449L;

    /**
     * Index of the first affected ordinate.
     */
    private final int firstAffectedOrdinate;

    /**
     * Number of unaffected ordinates after the affected ones.
     * Always 0 when used through the strict OpenGIS API.
     */
    private final int numTrailingOrdinates;

    /**
     * The sub transform.
     */
    private final MathTransform transform;

    /**
     * The inverse transform. This field
     * will be computed only when needed.
     */
    private transient PassThroughTransform inverse;

    /**
     * Create a pass through transform.
     *
     * @param firstAffectedOrdinate Index of the first affected ordinate.
     * @param transform The sub transform.
     * @param numTrailingOrdinates Number of trailing ordinates to pass through.
     *        Affected ordinates will range from <code>firstAffectedOrdinate</code>
     *        inclusive to <code>dimTarget-numTrailingOrdinates</code> exclusive.
     */
    public PassThroughTransform(final int firstAffectedOrdinate, final MathTransform transform, final int numTrailingOrdinates)
    {
        this.firstAffectedOrdinate = firstAffectedOrdinate;
        this.numTrailingOrdinates  = numTrailingOrdinates;
        this.transform             = transform;
    }
    
    /**
     * Gets the dimension of input points.
     */
    public int getDimSource()
    {return firstAffectedOrdinate + transform.getDimSource() + numTrailingOrdinates;}
    
    /**
     * Gets the dimension of output points.
     */
    public int getDimTarget()
    {return firstAffectedOrdinate + transform.getDimTarget() + numTrailingOrdinates;}
    
    /**
     * Tests whether this transform does not move any points.
     */
    public boolean isIdentity()
    {return transform.isIdentity();}

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) throws TransformException
    {
        final int subDimSource = transform.getDimSource();
        final int subDimTarget = transform.getDimTarget();
        int srcStep = numTrailingOrdinates;
        int dstStep = numTrailingOrdinates;
        if (srcPts==dstPts && srcOff<dstOff)
        {
            final int dimSource = getDimSource();
            final int dimTarget = getDimTarget();
            srcOff += numPts * dimSource;
            dstOff += numPts * dimTarget;
            srcStep -= 2*dimSource;
            dstStep -= 2*dimTarget;
        }
        while (--numPts >= 0)
        {
            System.arraycopy   (srcPts, srcOff,                        dstPts, dstOff,              firstAffectedOrdinate);
            transform.transform(srcPts, srcOff+=firstAffectedOrdinate, dstPts, dstOff+=firstAffectedOrdinate,           1);
            System.arraycopy   (srcPts, srcOff+=subDimSource,          dstPts, dstOff+=subDimTarget, numTrailingOrdinates);
            srcOff += srcStep;
            dstOff += dstStep;
        }
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) throws TransformException
    {
        final int subDimSource = transform.getDimSource();
        final int subDimTarget = transform.getDimTarget();
        int srcStep = numTrailingOrdinates;
        int dstStep = numTrailingOrdinates;
        if (srcPts==dstPts && srcOff<dstOff)
        {
            final int dimSource = getDimSource();
            final int dimTarget = getDimTarget();
            srcOff += numPts * dimSource;
            dstOff += numPts * dimTarget;
            srcStep -= 2*dimSource;
            dstStep -= 2*dimTarget;
        }
        while (--numPts >= 0)
        {
            System.arraycopy   (srcPts, srcOff,                        dstPts, dstOff,              firstAffectedOrdinate);
            transform.transform(srcPts, srcOff+=firstAffectedOrdinate, dstPts, dstOff+=firstAffectedOrdinate,           1);
            System.arraycopy   (srcPts, srcOff+=subDimSource,          dstPts, dstOff+=subDimTarget, numTrailingOrdinates);
            srcOff += srcStep;
            dstOff += dstStep;
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     */
    public Matrix derivative(final CoordinatePoint point) throws TransformException
    {
        final int nSkipped = firstAffectedOrdinate + numTrailingOrdinates;
        final int transDim = transform.getDimSource();
        final int pointDim = point.getDimension();
        if (pointDim != transDim+nSkipped)
        {
            throw new MismatchedDimensionException(pointDim, transDim+nSkipped);
        }
        final CoordinatePoint subPoint = new CoordinatePoint(transDim);
        System.arraycopy(point.ord, firstAffectedOrdinate, subPoint.ord, 0, transDim);
        final Matrix subMatrix = transform.derivative(subPoint);
        final int     nRows = subMatrix.getNumRows();
        final int     nCols = subMatrix.getNumColumns();
        final Matrix matrix = new Matrix(nSkipped+nRows, nSkipped+nCols);

        //  Set UL part to 1:   [ 1  0             ]
        //                      [ 0  1             ]
        //                      [                  ]
        //                      [                  ]
        //                      [                  ]
        for (int j=0; j<firstAffectedOrdinate; j++)
            matrix.set(j,j,1);

        //  Set central part:   [ 1  0  0  0  0  0 ]
        //                      [ 0  1  0  0  0  0 ]
        //                      [ 0  0  ?  ?  ?  0 ]
        //                      [ 0  0  ?  ?  ?  0 ]
        //                      [                  ]
        subMatrix.copySubMatrix(0,0,nRows,nCols,firstAffectedOrdinate,firstAffectedOrdinate, matrix);

        //  Set LR part to 1:   [ 1  0  0  0  0  0 ]
        //                      [ 0  1  0  0  0  0 ]
        //                      [ 0  0  ?  ?  ?  0 ]
        //                      [ 0  0  ?  ?  ?  0 ]
        //                      [ 0  0  0  0  0  1 ]
        final int offset = nCols-nRows;
        for (int j=pointDim-numTrailingOrdinates; j<pointDim; j++)
            matrix.set(j, j+offset, 1);

        return matrix;
    }

    /**
     * Creates the inverse transform of this object.
     */
    public synchronized MathTransform inverse() throws NoninvertibleTransformException
    {
        if (inverse==null)
        {
            inverse = new PassThroughTransform(firstAffectedOrdinate, transform.inverse(), numTrailingOrdinates);
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * Compares the specified object with
     * this math transform for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true;
        if (super.equals(object))
        {
            final PassThroughTransform that = (PassThroughTransform) object;
            return this.firstAffectedOrdinate == that.firstAffectedOrdinate &&
                   this.numTrailingOrdinates  == that.numTrailingOrdinates  &&
                   Utilities.equals(this.transform, that.transform);
        }
        return false;
    }
}
