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

// Image
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ComponentSampleModelJAI;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.BandedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;

// Divers
import java.util.Arrays;
import java.awt.Rectangle;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;


/**
 * Image dont les valeurs des pixels correspond aux valeurs r�elles d'un param�tre g�ophysique.
 * Ces valeurs sont calcul�es en convertissant les valeurs <code>byte</code> d'une autre image
 * en valeur r�elles exprim�es selon les unit�s de {@link IndexedThemeMapper#getUnits}. Les donn�es
 * manquantes seront exprim�es avec diff�rentes valeurs <code>NaN</code>.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class NumericImage extends ImageAdapter
{
    /**
     * Convertit une image de valeurs de pixels en image de nombres r�els.
     *
     * @param image Image de valeurs de pixels. Les pixels de cette image
     *              doivent correspondre aux th�mes de <code>categories</code>.
     * @return Image de nombres r�els. Toutes les valeurs de cette image seront
     *         exprim�es selon les unit�s {@link CategoryList#getUnit}. Les pixels
     *         qui ne correspondent pas au param�tre g�ophysique auront une valeur <code>NaN</code>.
     */
    public static RenderedImage getInstance(final RenderedImage image, final CategoryList[] categories)
    {
        if (image==null) return null;
        if (image instanceof ImageAdapter)
        {
            final ImageAdapter adapter = (ImageAdapter) image;
            if (Arrays.equals(adapter.categories, categories))
                return adapter.getNumeric();
        }
        return new NumericImage(image, categories);
    }

    /**
     * Construit une image de nombres r�elles � partir des
     * valeurs <code>byte</code> de l'image index�e sp�cifi�e.
     *
     * @param image      Image contenant les valeurs index�es.
     * @param categories Ensemble des cat�gories qui donnent une signification aux pixels de l'image.
     */
    private NumericImage(final RenderedImage image, final CategoryList[] categories)
    {super(image, getLayout(image), categories);}

    /**
     * Returns the destination image layout.
     *
     * @param  image The source image.
     * @return Layout for the destination image.
     */
    private static ImageLayout getLayout(final RenderedImage image)
    {
        final int width    = image.getWidth();
        final int height   = image.getHeight();
        final int numBands = image.getSampleModel().getNumBands();
        // Construit un mod�le {@link SampleModel}
        // capable de m�moriser des nombres r�els.
        final SampleModel sampleModel;
        if (false)
        {
            sampleModel = new BandedSampleModel(DataBuffer.TYPE_FLOAT, width, height, numBands);
        }
        else
        {
            final int[] bankIndices = new int[numBands];
            final int[] bandOffsets = new int[numBands];
            for (int i=0; i<bankIndices.length; i++) bankIndices[i]=i;
            sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_FLOAT, width, height, 1, width, bankIndices, bandOffsets);
            // Java Advanced Imaging ne connait pas les nouveaux DataBufferFloat du JDK 1.4.
            // On est oblig� d'utiliser la classe adapt�e de JAI.
        }
        ImageLayout layout = new ImageLayout(image);
        if (image.getNumXTiles()==1 && image.getNumYTiles()==1)
        {
            layout = layout.unsetTileLayout(); // Lets JAI choose a default tile size.
        }
        return layout.setSampleModel(sampleModel).setColorModel(createColorModel(sampleModel));
        // TODO: we should choose a better color model than PlanarImage.createColorModel(...).
    }

    /**
     * Retourne l'image qui contient les donn�es sous forme de nombres r�els.
     * Cette image sera <code>this</code>, puisqu'elle repr�sente d�j� un
     * d�codage des valeurs de pixels.
     */
    public PlanarImage getNumeric()
    {return this;}

    /**
     * Retourne l'image qui contient les donn�es sous forme de valeurs de th�mes.
     * Cette image sera l'image source de <code>this</code>.
     */
    public PlanarImage getThematic()
    {return getSourceImage(0);}

    /**
     * Effectue le calcul d'une tuile de l'image. L'image source doit contenir
     * des index correspondant aux th�mes {@link IndexedTheme},  tandis que la
     * tuile de destination aura les valeurs r�elles correspondantes.
     *
     * @param sources  Un tableau de longueur 1 contenant la source.
     * @param dest     La tuile dans laquelle �crire les pixels.
     * @param destRect La r�gion de <code>dest</code> dans laquelle �crire.
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
                    final float sample=iterator.getSampleFloat();
                    category = categories.getDecoder(sample, category);
                    if (category==null) category = blank;
                    dest.setSample(x,y,band, category.toValue(sample));
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
