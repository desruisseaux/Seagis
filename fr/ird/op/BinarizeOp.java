/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.op;

// JAI.
import javax.media.jai.OpImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;
import javax.media.jai.PlanarImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterFactory;

// J2SE.
import java.util.Map;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

/**
 * Binarize une image de type <CODE>RenderedImage</CODE>. <BR><BR>
 *
 * Cette opérateur requiert le pixel (x, y) de chaque source pour calculer le pixel
 * (x, y) de l'image de destination. Chaque image source produit une image binaire dans 
 * laquelle les pixels ont pour valeur 0 ou 1.<BR> La valeur des pixels de sortie est 
 * déterminée par deux valeurs désignées par "low" and "high". Tout pixel (x, y) de 
 * l'image source supérieur à "low" et "inférieur" à "high" produira un pixel (x, y)
 * valant 1 dans l'image de destination. Les autres pixels auront pour valeur 0.<BR><BR>
 *
 * <I><BR>
 * pixel = 0;
 * Si ((x, y) > low et (x, y) < high) alors pixel = 1;
 * </I>
 *
 * @author  Remi eve
 * @version $Id$
 */
public class BinarizeOp extends PointOpImage 
{
    /** Low and Hight value. */
    private final double[] low,
                           high;
    
    /** 
     * Constructeur.
     *
     * @param radiance      Une image.
     * @param layout        Type de l'image de sortie.
     * @param low           Low value.
     * @param high          High value.
     * @param configuration Configuration du comportement du JAI.
     */
    private BinarizeOp(final RenderedImage  source, 
                       final ImageLayout    layout, 
                       final double[]       low,
                       final double[]       high,
                       final Map            configuration) 
    {
        super(source, layout, configuration, false);                
        permitInPlaceOperation();
        this.low  = low;
        this.high = high;
    }
    
    /**
     * Retourne une image binaire de type <CODE>OpImage</CODE>. 
     *
     * @param radiance      Une image.
     * @param low           Low value.
     * @param high          High value.
     * @param configuration Configuration du comportement du JAI.
     */
    public static OpImage get(final RenderedImage  source,                                    
                                    final double[] low,
                                    final double[] high,
                                    final Map      configuration) 
    {
        if (source == null)
            throw new IllegalArgumentException("Source is null.");
        if (low == null)
            throw new IllegalArgumentException("Low is null.");
        if (high == null)
            throw new IllegalArgumentException("High is null.");
        
        // Définition de l'image de sortie.      
        final int width  = source.getWidth(),
                  height = source.getHeight();
        final ImageLayout layout = new ImageLayout(source);
        final int band           = source.getSampleModel().getNumBands();                
        
        // Nombre de bits doit être une puissance de 2.
        int numberOfBits = band * 2;        
        int p2 = 2;
        while (p2 < numberOfBits)
            p2*=2;
        numberOfBits = p2;        
        final SampleModel sample = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                                   source.getWidth(),
                                                                   source.getHeight(),
                                                                   numberOfBits);                               
        final byte[] r = {0,(byte)255},
                     g = {0,(byte)255},
                     b = {0,(byte)255};
        final ColorModel color = new java.awt.image.IndexColorModel(1, 2, r, g, b);
        layout.setSampleModel(sample);
        layout.setColorModel(color);
        
        return new BinarizeOp(source, layout, low, high, configuration);
    }    
    
    /**
     * Binarize les images sources.
     *
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    public void computeRect(final PlanarImage[] sources, 
                            final WritableRaster dest, 
                            Rectangle destRect) 
    {
        final RectIter iSrc = RectIterFactory.create(sources[0],destRect);
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);
        destRect = destRect.intersection(sources[0].getBounds());        
        
        iSrc.startBands();
        iTarget.startBands(); 
        int band = 0;
        while (!iTarget.finishedBands())
        {
            final double low  = this.low[band],
                         high = this.high[band];
            iSrc.startLines();
            iTarget.startLines();        
            while (!iTarget.finishedLines())
            {
                iSrc.startPixels();            
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {
                    final double pixelSrc = iSrc.getSampleDouble();
                    final double pixelDst = (pixelSrc > low && pixelSrc < high) ? 1 : 0;                    
                    iTarget.setSample(pixelDst);
                    iSrc.nextPixel();
                    iTarget.nextPixel();
                }        
                iSrc.nextLine();
                iTarget.nextLine();
            }
            iSrc.nextBand();
            iTarget.nextBand();            
            band ++;
        }
    }    
}