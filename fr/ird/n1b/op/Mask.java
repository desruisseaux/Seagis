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
import java.util.Vector;

// JAI
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// SEAGIS
import fr.ird.image.n1b.Filter;

/**
 * Calcul un masque en fonction d'un filtre et d'une ou plusieurs images sources. Les 
 * Filtres dérivent de la classe <CODE>Filter</CODE>.<BR><BR>
 *
 * Les pixels de l'image produite auront pour valeur <CODE>Mask.NOT_FILTERED</CODE> 
 * (indiquant que ces pixels ne doivent pas être filtrés) ou <CODE>Mask.FILTERED</CODE> 
 * (indiquant les pixels à filtrer).<BR><BR>
 *
 * Pour déterminer si le pixel de coordonnée (x,y) du masque doit être filtré, un tableau 
 * est créé et passé en argument au filtre. Les informations envoyées au masque sont les 
 * suivantes : 
 * <UL>
 *  <LI>Coordonnée <i>x</i> du pixel dans l'image générée.</LI>
 *  <LI>Coordonnée <i>y</i> du pixel dans l'image générée.</LI>
 *  <LI>Valeur du pixel de coordonnées <i>(x, y)</i> dans la première image source.</LI>
 *  <LI>Valeur du pixel de coordonnées <i>(x, y)</i> dans la seconde image source.</LI>
 *  <LI>....</LI>
 * </UL><BR><BR>
 *
 * Ce tableau est envoyé au filtre qui se charge d'indiquer en fonction des informations 
 * du tableau l'état du pixel.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public class Mask extends PointOpImage 
{  
    /** Pixels à filtrer. */
    public static final double FILTERED = 255;
    
    /** Pixels à ne pas filtrer. */
    public static final double NOT_FILTERED = 0;

    /** Le filtre. */
    private final Filter filter;   
    
    /** 
     * Constructeur.
     *
     * @param source         Un vecteur d'images sources.
     * @param layout         Définition du type de l'image de sortie.
     * @param filter         Le filtre a appliquer aux images sources.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    protected Mask(final Vector         source, 
                   final ImageLayout    layout, 
                   final Filter         filter, 
                   final Map            configuration) 
    {
        super(source, layout, configuration, false);     
        permitInPlaceOperation();
        this.filter = filter;        
    }    
    
    /**
     * Retourne le masque générée en fonction du filtre. Le filtre détermine les pixels 
     * qui devront être filtré <CODE>Mask.FILTERED</CODE> et ceux qui ne devront pas 
     * l'être <CODE>NOT_FILTERED</CODE>. 
     *
     * @param sources           Un tableau d'images sources.
     * @param filter            Le filtre à appliquer.
     * @param configuration     Configuration du traitement réalisé par JAI.
     * @return le masque générée en fonction du filtre. Le filtre détermine les pixels 
     *         qui devront être filtré <CODE>Mask.FILTERED</CODE> et ceux qui ne devront pas 
     *         l'être <CODE>NOT_FILTERED</CODE>. 
     */
    public static RenderedImage get(final RenderedImage[]    sources,
                                    final Filter             filter,
                                    final Map                configuration)
    {
        if (sources == null)
            throw new IllegalArgumentException("Sources is null.");
        
        for (int index=0 ; index<sources.length ; index++)                        
            if (sources[index] == null)                
                throw new IllegalArgumentException("Image at index " + index + " is null.");        
                        
        if (filter == null)
            throw new IllegalArgumentException("Filter is null.");
                
        // Définition du type de l'image de sortie.
        Rectangle bound = null;
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
        layout.setSampleModel(RasterFactory.createBandedSampleModel(DataBuffer.TYPE_BYTE, 
                                                                    width, 
                                                                    height,
                                                                    numBands));        

        final Vector source = new Vector();
        for (int index=0 ; index<sources.length ; index++)                               
            source.add(sources[index]);          
        return new Mask(source, layout, filter, configuration);                
    }
    
    /**
     * Calcul. 
     *
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    protected void computeRect(final PlanarImage[]    sources, 
                               final WritableRaster   dest, 
                               Rectangle              destRect) 
    {    
        final int length = sources.length;
        final RectIter[] iSrc = new RectIter[length];
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);
        final double[] array = new double[length+2];

        for (int index=0 ; index<length ; index++) 
        {
            iSrc[index] = RectIterFactory.create(sources[index],destRect);
            destRect = destRect.intersection(sources[index].getBounds());
        }
        
        for (int index=0 ; index<length ; index++) 
            iSrc[index].startBands();
        iTarget.startBands();                        
        while (!iTarget.finishedBands())
        {
            int row = (int)destRect.getY();
            array[1] = row; 
            for (int index=0 ; index<length ; index++) 
                iSrc[index].startLines();
            iTarget.startLines();        
            while (!iTarget.finishedLines())
            {
                int col = (int)destRect.getX();
                array[0] = col;
                for (int index=0 ; index<length ; index++) 
                    iSrc[index].startPixels();
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {
                    for (int index=0 ; index<length ; index++) 
                        array[2+index] = iSrc[index].getSampleDouble();

                    if (filter.isFiltered(array))
                        iTarget.setSample(FILTERED);
                    else
                        iTarget.setSample(NOT_FILTERED);                    

                    for (int index=0 ; index<length ; index++) 
                        iSrc[index].nextPixel();
                    iTarget.nextPixel();
                    col ++;
                    array[0] = col;                
                }        
                for (int index=0 ; index<length ; index++) 
                    iSrc[index].nextLine();
                iTarget.nextLine();
                row++;
                array[1] = row; 
            }
            for (int index=0 ; index<length ; index++) 
                iSrc[index].nextBand();
            iTarget.nextBand();            
        }
    }
}