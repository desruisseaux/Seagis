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
package net.seas.opengis.cv;

// Image
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;

// Divers
import java.util.Arrays;
import java.awt.Rectangle;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;


/**
 * Image dont les valeurs des pixels correspond aux thèmes d'un objet {@link IndexedThemeMapper}.  Les
 * valeurs des pixels sont calculées en convertissant les valeurs <code>float</code> d'une autre image
 * en valeur de pixels. Les valeurs <code>float</code> de l'image source doivent être des valeurs du
 * paramètre géophysiques exprimées selon les unités de {@link IndexedThemeMapper#getUnits}. Les données
 * manquantes peuvent être exprimées avec différentes valeurs <code>NaN</code> telle que {@link Theme#LAND}
 * ou {@link Theme#CLOUD}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ThematicImage extends ImageAdapter
{
    /**
     * Convertit une image de nombres réels en valeurs de pixels.
     *
     * @param image Image de nombres réels. Toutes les valeurs de cette image
     *              doivent être exprimées selon les unités {@link CategoryList#getUnit}.
     *              Les pixels qui ne correspondent pas au paramètre géophysique peuvent
     *              avoir une des valeurs <code>NaN</code>.
     * @return Image de valeurs de pixels. Les pixels de cette image
     *         correspondront aux thèmes de <code>categories</code>.
     */
    public static RenderedImage toThematic(final RenderedImage image, final CategoryList[] categories)
    {
        if (image==null) return null;
        if (image instanceof ImageAdapter)
        {
            final ImageAdapter adapter = (ImageAdapter) image;
            if (Arrays.equals(adapter.categories, categories))
                return adapter.getThematic();
        }
        return new ThematicImage(image, categories);
    }

    /**
     * Construit une image indexée à partir des valeurs <code>float</code>
     * de l'image spécifiée.
     *
     * @param image      Image contenant les valeurs du paramètres géophysique.
     * @param categories Ensemble des catégories qui donnent une signification aux pixels de l'image.
     */
    private ThematicImage(final RenderedImage image, final CategoryList[] categories)
    {super(image, getLayout(image, categories), categories);}

    /**
     * Returns the destination image layout.
     *
     * @param  image The source image.
     * @param  categories Category lists.
     * @return Layout for the destination image.
     */
    private static ImageLayout getLayout(final RenderedImage image, final CategoryList[] categories)
    {
        final IndexColorModel colors;
        switch (categories.length)
        {
            default: throw new UnsupportedOperationException(String.valueOf(categories.length));
            case 1:  colors = categories[0].getIndexColorModel(); break;
            // TODO: support 2, 3, 4... bands
        }
        ImageLayout layout = new ImageLayout(image);
        if (image.getNumXTiles()==1 && image.getNumYTiles()==1)
        {
            layout = layout.unsetTileLayout(); // Lets JAI choose a default tile size.
        }
        return layout.setSampleModel(colors.createCompatibleSampleModel(image.getWidth(), image.getHeight())).setColorModel(colors);
    }

    /**
     * Retourne l'image qui contient les données sous forme de nombres réels.
     * Il s'agira de l'image source de <code>this</code>.
     */
    public PlanarImage getNumeric()
    {return getSourceImage(0);}

    /**
     * Retourne l'image qui contient les données sous forme de valeurs de thèmes.
     * Cette image sera <code>this</code>, qui représente déjà un encodage des pixels.
     */
    public PlanarImage getThematic()
    {return this;}

    /**
     * Effectue le calcul d'une tuile de l'image. L'image source doit contenir
     * des valeurs géophysiques, tandis que tandis que la tuile de destination
     * aura les valeurs de pixels correspondantes.
     *
     * @param sources  Un tableau de longueur 1 contenant la source.
     * @param dest     La tuile dans laquelle écrire les pixels.
     * @param destRect La région de <code>dest</code> dans laquelle écrire.
     */
    protected void computeRect(final PlanarImage[] sources, final WritableRaster dest, final Rectangle destRect)
    {
        final RectIter iterator = RectIterFactory.create(sources[0], destRect);
        final int xmin = destRect.x;
        final int ymin = destRect.y;
        final int xmax = destRect.width  + xmin;
        final int ymax = destRect.height + ymin;
        for (int band=0; band<categories.length; band++)
        {
            final CategoryList categories = this.categories[band];
            final Category blank = categories.getBlank();
            Category category = blank;
            for (int y=ymin; y<ymax; y++)
            {
                for (int x=xmin; x<xmax; x++)
                {
                    final double value=iterator.getSampleDouble();
                    category = categories.getEncoder(value, category);
                    if (category==null) category = blank;
                    dest.setSample(x,y,band, category.toIndex(value));
                    iterator.nextPixel();
                }
                assert(iterator.finishedPixels());
                iterator.startPixels();
                iterator.nextLine();
            }
            assert(iterator.finishedLines());
            iterator.startLines();
            iterator.nextBand();
        }
        assert(iterator.finishedBands());
    }
}
