/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2002, Institut de Recherche pour le Développement
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
package net.seagis.gp;

// Images (J2SE)
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.IndexColorModel;

// Java Advanced Imaging
import javax.media.jai.OpImage;
import javax.media.jai.NullOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.ParameterList;

// OpenGIS implementation
import net.seagis.cv.CategoryList;
import net.seagis.gc.GridCoverage;


/**
 * Operation applied only on image's colors. This operation work
 * only for source image using an {@link IndexColorModel}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class IndexColorOperation extends Operation
{
    /**
     * Construct an operation.
     *
     * @param The name of the operation.
     * @param The parameters descriptors.
     */
    public IndexColorOperation(final String name, final ParameterListDescriptor descriptor)
    {
        super(name, descriptor);
    }

    /**
     * Performs the color transformation. This method invokes the
     * {@link #transformColormap transformColormap(...)} method with
     * current RGB colormap, the source {@link CategoryList} and the
     * supplied parameters.
     *
     * @param parameters The parameters.
     */
    protected GridCoverage doOperation(final ParameterList parameters)
    {
        final GridCoverage source = (GridCoverage) parameters.getObjectParameter("Source");
        final RenderedImage image = source.getRenderedImage(false);
        final ColorModel    model = image.getColorModel();
        if (model instanceof IndexColorModel)
        {
            final int band = 0; // Always 0 in this implementation.
            final CategoryList[] categories = source.getCategoryLists();
            final IndexColorModel    colors = (IndexColorModel) model;
            final int               mapSize = colors.getMapSize();
            final byte[] R=new byte[mapSize]; colors.getReds  (R);
            final byte[] G=new byte[mapSize]; colors.getGreens(G);
            final byte[] B=new byte[mapSize]; colors.getBlues (B);
            transformColormap(R,G,B, categories[band], parameters);
            if (!compare(colors, R,G,B))
            {
                final int computeType = (image instanceof OpImage) ?
                                        ((OpImage)image).getOperationComputeType() :
                                        OpImage.OP_COMPUTE_BOUND;
                final IndexColorModel newModel = new IndexColorModel(colors.getComponentSize()[band], mapSize, R,G,B);
                final ImageLayout       layout = new ImageLayout().setColorModel(newModel);
                final RenderedImage   newImage = new NullOpImage(image, layout, null, computeType);
                return new GridCoverage(source.getName(null), newImage,
                                        source.getCoordinateSystem(),
                                        source.getEnvelope(),
                                        new CategoryList[] {categories[band]},
                                        false,
                                        new GridCoverage[] {source},
                                        null);
            }
        }
        return source;
    }

    /**
     * Transform the supplied RGB colors. This method is automatically invoked
     * by {@link #doOperation(ParameterList)}. The source {@link GridCoverage}
     * has usually only one band; consequently <code>transformColormap</code>
     * is invoked with the {@link CategoryList} for this band only. The
     * <code>R</code>, <code>G</code> and <code>B</code> arrays contains the
     * RGB values from the current source and should be overriden with new RGB
     * values for the destination image.
     *
     * @param R Red   components to transform.
     * @param G Green components to transform.
     * @param B Blue  components to transform.
     * @param categories The list of categories. This parameter is supplied
     *        for information only. It may be usefull for interpretation of
     *        colormap's index. For example, an implementation could use this
     *        information for transforming only colors at index allocated to
     *        geophysics values.
     * @param parameters The user-supplied parameters.
     */
    protected abstract void transformColormap(final byte[] R,
                                              final byte[] G,
                                              final byte[] B,
                                              final CategoryList  categories,
                                              final ParameterList parameters);

    /**
     * Check if a color model use the specified RGB components.
     *
     * @param colors  Color map to compare.
     * @param R Red   components to compare.
     * @param G Green components to compare.
     * @param B Blue  components to compare.
     */
    private static boolean compare(final IndexColorModel colors, final byte[] R, final byte[] G, final byte[] B)
    {
        final byte[] array=new byte[colors.getMapSize()];
        colors.getReds  (array); if (!compare(array, R)) return false;
        colors.getGreens(array); if (!compare(array, G)) return false;
        colors.getBlues (array); if (!compare(array, B)) return false;
        return true;
    }

    /**
     * Check if content of array <code>C1</code> is
     * identical to content of array <code>C2</code>.
     */
    private static boolean compare(final byte[] C1, final byte[] C2)
    {
        if (C1.length != C2.length)
        {
            return false;
        }
        for (int i=C1.length; --i>=0;)
        {
            if (C1[i] != C2[i])
                return false;
        }
        return true;
    }
}
