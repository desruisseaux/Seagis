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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.cv;

// Image et g�om�trie
import java.awt.Rectangle;
import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;


/**
 * Classe de base des images qui repr�senteront leurs donn�es sous forme
 * de nombre r�els ou sous forme d'index de th�mes {@link IndexedTheme}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class ImageAdapter extends PointOpImage
{
    /**
     * Ensemble des cat�gories qui donnent une
     * signification aux pixels de l'image. La
     * longueur de ce tableau doit correspondre
     * au nombre de bandes de l'image source.
     */
    protected final CategoryList[] categories;

    /**
     * Construit une image qui puisera ses donn�es dans l'image sp�cifi�e.
     *
     * @param image      Image source.
     * @param layout     Disposition de l'image de destination.
     * @param categories Ensemble des cat�gories qui donnent une signification aux pixels de l'image.
     */
    protected ImageAdapter(final RenderedImage image, final ImageLayout layout, final CategoryList[] categories)
    {
        super(image, layout, JAI.getDefaultInstance().getRenderingHints(), false);
        this.categories = categories;
        final int numBands = image.getSampleModel().getNumBands();
        if (categories.length!=numBands)
        {
            throw new IllegalArgumentException(String.valueOf(categories.length)+"!="+numBands);
        }
        permitInPlaceOperation();
    }

    /**
     * Retourne l'image qui contient les donn�es sous forme de nombres r�els.
     * Cette image sera <code>this</code> ou l'image source de <code>this</code>.
     */
    public abstract PlanarImage getNumeric();

    /**
     * Retourne l'image qui contient les donn�es sous forme de valeurs de th�mes.
     * Cette image sera <code>this</code> ou l'image source de <code>this</code>.
     */
    public abstract PlanarImage getThematic();

    /**
     * Effectue le calcul d'une tuile de l'image.
     *
     * @param sources  Un tableau de longueur 1 contenant la source.
     * @param dest     La tuile dans laquelle �crire les pixels.
     * @param destRect La r�gion de <code>dest</code> dans laquelle �crire.
     */
    protected abstract void computeRect(final PlanarImage[] sources, final WritableRaster dest, final Rectangle destRect);
}
