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

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.CoordinatePoint;


/**
 * Concatened transform where the transfert dimension
 * is the same than source and target dimension. This
 * fact allows some optimizations, the most important
 * one being the possibility to avoid the use of an
 * intermediate buffer.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class ConcatenedTransformDirect extends ConcatenedTransform
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -3568975979013908920L;

    /**
     * Construct a concatenated transform.
     */
    public ConcatenedTransformDirect(final MathTransformFactory provider, final MathTransform transform1, final MathTransform transform2)
    {super(provider, transform1, transform2);}

    /**
     * Check if transforms are compatibles
     * with this implementation.
     */
    protected boolean isValid()
    {
        return super.isValid() &&
               transform1.getDimSource() == transform1.getDimTarget() &&
               transform2.getDimSource() == transform2.getDimTarget();
    }

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     */
    public CoordinatePoint transform(final CoordinatePoint ptSrc, CoordinatePoint ptDst) throws TransformException
    {
//----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
//----- END OF JDK 1.4 DEPENDENCIES ---
        ptDst = transform1.transform(ptSrc, ptDst);
        return  transform2.transform(ptDst, ptDst);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
//----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
//----- END OF JDK 1.4 DEPENDENCIES ---
        transform1.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        transform2.transform(dstPts, dstOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    public void transform(final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
//----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert isValid();
//----- END OF JDK 1.4 DEPENDENCIES ---
        transform1.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        transform2.transform(dstPts, dstOff, dstPts, dstOff, numPts);
    }
}