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

// SEAGIS
import fr.ird.image.sst.n1b.Utilities;
import fr.ird.op.KernelFilter;

// J2SE && JAI
import javax.media.jai.KernelJAI;
import javax.media.jai.OpImage;
import javax.media.jai.util.Range;
import javax.media.jai.PointOpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import java.util.Map;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;

//GEOTOOLS
import org.geotools.cv.Category;

/**
 * Lisse une image SST. Le lissage est réalisé au travers d'une fenêtre de taille variable.
 * Cette fenêtre contient un pixel dit <CODE>Key</CODE> de coordonnées (xKey, yKey) dans 
 * la fenêtre définissant le pixel succeptible d'être modifié. Le pixel <CODE>Key</CODE> 
 * prendra la valeur MAX des pixels représentant une température dans notre fenêtre.<BR><BR>
 *
 * La valeur des pixels dans le Kernel n'a aucune influence sur le traitement. Le kernel
 * définie seulement la taille de la fenêtre ainsi que la position du pixel <CODE>Key</CODE>.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class MaxFilter extends KernelFilter
{  
    /** Index contenant les temperatures. */
    private final Range rTemperature;
    
    /** Index contenant les pixels terre. */
    private final Range rLand;
    
    /** Index contenant les pixels contour terre. */
    private final Range rContour;

    /**
     * Construit un MaxFilter.
     *
     * @param source         L'image SST source en valeur indexé.
     * @param kernel         Kernel.
     * @param layout         Définition du type de l'image de sortie.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    protected MaxFilter(final RenderedImage source, 
                        final KernelJAI     kernel,
                        final ImageLayout   layout, 
                        final Map           configuration) 
    {        
        super(source, kernel, null, configuration);            
        rTemperature = Utilities.getCategory(Utilities.SAMPLE_SST_INDEXED, 
                                             Utilities.TEMPERATURE).getRange();
        rLand        = Utilities.getCategory(Utilities.SAMPLE_SST_INDEXED, 
                                             Utilities.LAND_BACKGROUND).getRange();
        rContour     = Utilities.getCategory(Utilities.SAMPLE_SST_INDEXED, 
                                             Utilities.LAND_CONTOUR).getRange();
    }    
    
    /**
     * Retourne un objet de type <CODE>RenderedImage</CODE> contenant une image S.S.T.
     * dont les température ont été lissées.
     *
     * @param source         L'image S.S.T. source en valeur indéxées.
     * @param width          Largeur de la fenêtre.
     * @param height         Hauteur de la fenêtre.
     * @param xKey           Position x du pixel <i>Key</i> dans la fenêtre.
     * @param yKey           Position y du pixel <i>Key</i> dans la fenêtre.
     * @param configuration  Configuration du traitement réalisé par JAI.
     * @return un objet de type <CODE>OpImage</CODE> contenant une image S.S.T.
     * dont les températures ont été lissées. 
     */
    public static RenderedImage get(final RenderedImage source, 
                                    final int           width,
                                    final int           height,
                                    final int           xKey,
                                    final int           yKey,
                                    final Map           configuration) {
        if (source == null)
            throw new IllegalArgumentException("Source is null."); 
                           
        // Construction du type de l'image de sortie.
        final ImageLayout layout = new ImageLayout(source);
        
        // Construction du Kernel.
        final float[] data = new float[width*height];
        for (int i=0; i<data.length ; i++)
            data[i] = 0;
        final KernelJAI kernel = new KernelJAI(width, height, xKey, yKey, data);                                   
        return new MaxFilter(source, kernel, layout, configuration);
    }
        
    /**
     * Retourne la valeur à affecter au pixel <i>Key</i>. Dans cette classe, la valeur sera
     * la température maximale des pixels de type température appartenant à la fenêtre.
     * Si aucun pixel de type température n'est présent, la valeur d'origine sera 
     * retournée.
     *
     * @param dataSrc La fenêtre de l'image source.
     * @return Retourne la valeur à affecter au pixel <i>Key</i>.
     */
    protected double process(final double[] dataSrc)
    {
        final Double pixelKey = new Double(dataSrc[kernel.getYOrigin()*kernel.getWidth() + 
                                                   kernel.getXOrigin()]);
        
        // Le pixel "Key" est de type "terre" ou "contour terre".
        if (rContour.contains(pixelKey) || rLand.contains(pixelKey))
            return pixelKey.doubleValue();
        
        // Calcul la moyenne des pixels.
        /*double avg = 0;
        double num = 0;
        for (int row=0 ; row<kernel.getHeight(); row++)
        {
            for (int col=0 ; col<kernel.getWidth() ; col++)
            {
                final Double pixel = new Double(dataSrc[row*kernel.getWidth() + col]);
                if (rTemperature.contains(pixel))
                {
                    avg += pixel.doubleValue();
                    num ++;
                }
            }            
        }
        if (num == 0)
            return pixelKey.doubleValue();
        if (num == 1)
            return avg;
        avg /= num;*/
        
        
        // Le pixel "Key" est de type "nuage", "abscence de donnée" ou "température".
        double max = Double.NEGATIVE_INFINITY;        
        for (int row=0 ; row<kernel.getHeight(); row++)
        {
            for (int col=0 ; col<kernel.getWidth() ; col++)
            {
                final Double pixel = new Double(dataSrc[row*kernel.getWidth() + col]);
                if (rTemperature.contains(pixel))
                {
                    /*if (pixel.doubleValue() < (avg+10.0) && 
                        pixel.doubleValue() > (avg-10.0))*/
                    max = Math.max(max, pixel.doubleValue());
                }
            }            
        }
        
        if (max != Double.NEGATIVE_INFINITY)
            return max;
        else 
            return pixelKey.doubleValue();
    }    
}