/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.n1b.op;

// J2SE
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.Rectangle;
import java.util.Map;

// JAI
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

/**
 * Superpose un masque sur une image source. L'ensemble des pixels du masque ayant pour
 * valeur <CODE>valueOfMask</CODE> sont superposés à l'image source. La valeur affectée aux
 * pixels superposés est <CODE>pix</CODE>.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class SuperposeMask extends PointOpImage 
{  
    /** Valeur du masque à superposer à l'image source. */
    private final double valueOfMask;
    
    /** Valeur affectée aux pixels superposés sur l'image source. */
    private final double pix;    
    
    /** 
     * Constructeur.
     *
     * @param source            Image source.
     * @param mask              Un masque.
     * @param valueOfMask       Valeur des pixels du masque à superposer sur l'image source.
     * @param pix               Valeur affectée au pixel superposés sur l'image source.
     * @param layout            Type de l'image de sortie.
     * @param configuration     Configuration du comportement du JAI.
     */
    private SuperposeMask(final RenderedImage   source, 
                          final RenderedImage   mask, 
                          final double          valueOfMask, 
                          final double          pix, 
                          final ImageLayout     layout, 
                          final Map             configuration)      
    {
        super(source, mask, layout, configuration, false);                
        permitInPlaceOperation();
        this.valueOfMask = valueOfMask;
        this.pix = pix;
    }    
    
    /**
     * Retourne une image contenant l'image source à laquelle a été superposé un masque.
     * Le masque est définie par les pixels ayant comme valeur <CODE>valueOfMask</CODE>. Les
     * pixels superposés à l'image source auront pour valeur <CODE>pix</CODE> une fois
     * superposés.
     *
     * @param source                Image source.
     * @param mask                  Le masque.
     * @param valueOfMask           Valeur des pixels du masque à superposer sur l'image source.
     * @param pix                   Valeur affectée aux pixels superposés sur l'image source.
     * @param sample                Un sampleDimension definissant les categories associees a l'image.
     * @param bound                 Limite de l'image de sortie.
     * @param configuration         Configuration du traitement realisé par JAI.
     * @return une image contenant l'image source à laquelle a été superposé un masque.
     *         Le masque est définie par les pixels ayant comme valeur <CODE>valueOfMask</CODE>. 
     *         Les pixels superposés à l'image source auront pour valeur <CODE>pix</CODE> 
     *         une fois superposés.
     */
    public static RenderedImage get(final RenderedImage     source,  
                                    final RenderedImage     mask,  
                                    final double            valueOfMask,                                     
                                    final double            pix,
                                    final Map               configuration)
    {        
        if (source == null)
            throw new IllegalArgumentException("Source is null.");               
        if (mask == null)
            throw new IllegalArgumentException("Mask is null.");                       
        
        // Definition du type de l'image de sortie.
        Rectangle bound = null;
        final RenderedImage[] sources = {source, mask};
        int numBands = 0;
        for (int index=0 ; index<sources.length ; index++)
        {
            final int minX   = sources[index].getMinX(),
                      minY   = sources[index].getMinY(),
                      width  = sources[index].getWidth(),
                      height = sources[index].getHeight();
            final Rectangle rect = new Rectangle(minX,minY, width, height);            
            if (bound == null)
            {
                bound = rect;
                numBands = sources[index].getSampleModel().getNumBands();
            }
            else
            {
                bound = rect.intersection(bound);
                Math.min(numBands, sources[index].getSampleModel().getNumBands());
            }
        }

        final int minX   = (int)bound.getMinX(),        
                  minY   = (int)bound.getMinY(),
                  width  = (int)bound.getWidth(),
                  height = (int)bound.getHeight();        
        final ImageLayout layout = new ImageLayout(minX, minY, width, height);
        layout.setSampleModel(RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, 
                                                                    width, 
                                                                    height,
                                                                    numBands));                
        
        return new SuperposeMask(source, 
                                 mask, 
                                 valueOfMask, 
                                 pix,
                                 layout, 
                                 configuration);
    }
    
    /**
     * Calcul. 
     *
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    public void computeRect(final PlanarImage[] sources, 
                            final WritableRaster dest, 
                            Rectangle destRect) 
    {  
        final RectIter iSource         = RectIterFactory.create(sources[0],destRect);
        final RectIter iMask           = RectIterFactory.create(sources[1],destRect);
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);
        destRect = destRect.intersection(sources[0].getBounds());
        destRect = destRect.intersection(sources[1].getBounds());
        
        iSource.startBands();
        iMask.startBands();        
        iTarget.startBands();        
        while (!iTarget.finishedBands())
        {        
            iSource.startLines();
            iMask.startLines();
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                iSource.startPixels();            
                iMask.startPixels();                        
                iTarget.startPixels();      
                while (!iTarget.finishedPixels())
                {
                    if (iMask.getSampleDouble() == valueOfMask)
                        iTarget.setSample(pix);
                    else
                        iTarget.setSample(iSource.getSampleDouble());
                    iSource.nextPixel();
                    iMask.nextPixel();                
                    iTarget.nextPixel();
                }        
                iSource.nextLine();
                iMask.nextLine();            
                iTarget.nextLine();
            }
            iSource.nextBand();
            iMask.nextBand();        
            iTarget.nextBand();        
        }            
    }
}