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
package net.seas.opengis.gp;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.gc.GridCoverage;

// Geometry
import java.awt.geom.AffineTransform;
import net.seas.util.XAffineTransform;

// Java Advanced Imaging
import javax.media.jai.KernelJAI;
import javax.media.jai.ParameterBlockJAI;


/**
 * An operation for gradient magnitude.  This operation is similar
 * to the JAI's operation "GradientMagnitude", but the kernels are
 * normalized is such a way that the resulting gradients are closer
 * to "geophysics" measurements. The normalization include dividing
 * by the distance between pixels.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class GradientMagnitude extends OperationJAI
{
    /**
     * The default horizontal kernel. Below is a comparaison between
     * the horizontal Sobel mask and this <code>HORIZONTAL</code> mask:
     * <pre>
     *             SOBEL              THIS CLASS
     *        [ -1   0   1 ]        [ -1/8   0   1/8 ]
     *        [ -2   0   2 ]        [ -2/8   0   2/8 ]
     *        [ -1   0   1 ]        [ -1/8   0   1/8 ]
     * </pre>
     * RATIONAL: A horizontal gradient can be computed with (V2-V0)/D where
     *           V2 is the value in the right column, V0 is the value in the
     *           left column and D is distance between those two column, in
     *           pixel. We normalize left and right column in such a way that
     *           their sum is 1 (so we divide by 4). We divide again by 2 because
     *           the distance between center of rightermost pixels and leftermost
     *           pixels is two pixel width.
     */
    private static final KernelJAI HORIZONTAL = divide(KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL, 8);

    /**
     * The default vertical kernel. Same rational
     * than for the horizontal kernel.
     */
    private static final KernelJAI VERTICAL = divide(KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL, 8);

    /**
     * Construct a default gradient magnitude operation.
     */
    public GradientMagnitude()
    {super("GradientMagnitude");}

    /**
     * Divide a kernel by some number.
     */
    private static KernelJAI divide(KernelJAI k, final double denominator)
    {
        if (denominator!=0 && denominator!=1)
        {
            final float[] data = k.getKernelData();
            for (int i=0; i<data.length; i++) data[i] /= denominator;
            k = new KernelJAI(k.getWidth(), k.getHeight(), k.getXOrigin(), k.getYOrigin(), data);
        }
        return k;
    }

    /**
     * Divide a kernel by some number.
     *
     * @param parameters  The parameter block to look for kernel.
     * @param name        The parameter name for the kernel.
     * @param denominator The denominator.
     */
    private static void divide(final ParameterBlockJAI parameters, final String name, final double denominator)
    {
        final Object kernel = parameters.getObjectParameter(name);
        if (kernel instanceof KernelJAI)
        {
            parameters.setParameter(name, divide((KernelJAI) kernel, denominator));
        }
    }

    /**
     * Apply the operation on grid coverage. Default implementation looks for kernels
     * in the parameter list and divide kernel by the distance between pixel, in the
     * grid coverage's coordinate system.
     */
    protected GridCoverage doOperation(final GridCoverage[] sources, final ParameterBlockJAI parameters)
    {
        if (sources.length!=0)
        {
            final AffineTransform tr = sources[0].getGridGeometry().getAffineTransform2D();
            divide(parameters, "mask1", XAffineTransform.getScaleX0(tr));
            divide(parameters, "mask2", XAffineTransform.getScaleY0(tr));
        }
        return super.doOperation(sources, parameters);
    }

    // TODO: Set default parameters.
}
