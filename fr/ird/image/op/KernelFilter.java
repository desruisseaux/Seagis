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
package fr.ird.image.op;

// JAI
import javax.media.jai.KernelJAI;
import javax.media.jai.PointOpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// J2SE
import java.util.Map;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;

/**
 * Applique un traitement sur une image source en fonction d'une fenêtre. Pour cela,
 * une fenêtre est définie par l'objet <CODE>KernelJAI</CODE> contenant un pixel dit 
 * <i>Key</i> de coordonnées (xKey, yKey) dans la fenêtre, ce pixel est succeptible d'être 
 * modifié en fonction de la valeur des pixels de la fenêtre. <BR><BR>
 * 
 * Cette classe abstraite nécessite d'être étendue pour être utiliser. La méthode 
 * <CODE>process(data)</CODE> retourne la valeur à affecter au pixel <i>Key</i> en fonction de 
 * la valeur des pixels de la fenêtre.<BR><BR> 
 *
 * Par défaut, les pixels de l'image de destination reçoivent comme valeur les pixels de 
 * l'image source.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public abstract class KernelFilter extends PointOpImage
{  
    /* Le kernel. */
    protected final KernelJAI kernel;

    /**
     * Construit un KernelFilter.
     *
     * @param source         L'image source.
     * @param kernel         Le kernel.
     * @param layout         Definition du type de l'image de sortie.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    protected KernelFilter(final RenderedImage source, 
                           final KernelJAI     kernel, 
                           final ImageLayout   layout, 
                           final Map           configuration) 
    {        
        super(source, layout, configuration, false);                        
        permitInPlaceOperation();
        this.kernel  = kernel;
    }
    
    /**
     * Compute. 
     *
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    public void computeRect(final PlanarImage[] sources, 
                            final WritableRaster dest, 
                            Rectangle destRect) 
    {  
        // Par defaut, l'image crée se voit affectée la valeur de l'image source a 
        // tous ses pixels. 
        WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);
        RectIter iSource         = RectIterFactory.create(sources[0],destRect);        
        iTarget.startBands();        
        iSource.startBands();        
        iTarget.startLines();
        iSource.startLines();        
        while (!iTarget.finishedLines())
        {
            iTarget.startPixels();                
            iSource.startPixels();
            while (!iTarget.finishedPixels())
            {                
                iTarget.setSample(iSource.getSampleDouble());   
                iTarget.nextPixel();
                iSource.nextPixel();
            }        
            iTarget.nextLine();
            iSource.nextLine();
        }     
        
        
        final int xKey = kernel.getXOrigin(),
                  yKey = kernel.getYOrigin();
        
        // Limite de l'image source en prenant en compte la hauteur et largeur du kernel. 
        final int x = sources[0].getMinX() + xKey,
                  y = sources[0].getMinY() + yKey,                 
                  width  = sources[0].getWidth()  - (kernel.getHeight() - 2*xKey),
                  height = sources[0].getHeight() - (kernel.getHeight() - 2*xKey);                
        
        // Calcul de la zone couverte par la "key" du noyau de l'image de sortie. 
        destRect = destRect.intersection(new Rectangle(x, y, width, height));

        // Fenetre du kernel. 
        final Rectangle window = new Rectangle(0, 0, (int)kernel.getWidth(), (int)kernel.getHeight());
        double[] data = new double[kernel.getWidth() * kernel.getHeight()];
                
        iSource = RectIterFactory.create(sources[0],destRect);
        iTarget = RectIterFactory.createWritable(dest ,destRect);
        iSource.startBands();
        iTarget.startBands();
        while (!iTarget.finishedBands())
        {        
            window.setLocation((int)window.getX(), (int)destRect.getY() - yKey);
            iSource.startLines();
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                window.setLocation((int)destRect.getX() - xKey, (int)window.getY());
                iSource.startPixels();            
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {
                    // On test le kernel. 
                    data = sources[0].getData(window).getPixels((int)window.getX(), 
                                                                (int)window.getY(), 
                                                                (int)window.getWidth(),
                                                                (int)window.getHeight(),                                                            
                                                                data);
                    iTarget.setSample(process(data));
                    iSource.nextPixel();
                    iTarget.nextPixel();
                    window.setLocation((int)window.getX() + 1, (int)window.getY());                
                }        
                iSource.nextLine();
                iTarget.nextLine();
                window.setLocation((int)window.getX(), (int)window.getY() + 1);                
            }
            iSource.nextBand();
            iTarget.nextBand();
        }        
    }     
    
    /**
     * Retourne la valeur à affecter au pixel <i>Key</i>.
     *
     * @param dataSrc La fenêtre de l'image source.
     * @return Retourne la valeur à affecter au pixel <i>Key</i>.
     */
    protected abstract double process(final double[] dataSrc);
}