/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
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
import java.awt.geom.AffineTransform;
import java.util.Map;

// JAI
import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

/**
 * Exclue du calcul tous les pixels �loign�s de plus d'un certain angle du nadir. Les pixels 
 * de l'image dont l'angle est �loign�s de plus de X� du nadir se voit affect�e une valeur 
 * d�finie par l'utilisateur. Les autres pixels sont simplement copi�e de l'image source 
 * vers l'image destination. <BR><BR>
 *
 * Les angles doivent �tre lin�aire et monotone depuis le nadir de l'image.<BR><BR>
 *
 * L'int�r�t est d'�liminer des pixels ind�sirables ainsi que d'�viter de calculer des 
 * tuiles qui ne seront pas n�c�ssaires au calcul.
 *
 * @author Remi EVE
 * @version $Id$
 */
public class AngleExclusion extends PointOpImage 
{  
    /** 
     * Angle d'exclusion (les pixels �loignes de plus de <CODE>angle</CODE> du nadir 
     * se veront affect�es une valeur par defaut.
     */
    private final double angle;
    
    /**
     * <i>true</i> si les pixels acquis avec un angle �gale � l'angle d'exclusion doivent 
     * �tre �limin�s ou gard�s. <i>false</i> sinon.
     */
    private final boolean isIncluded;    
    
    /** Valeurs affect�es aux pixels trop �loign�s du nadir. */
    private final double pix;    
    
    /** 
     * Construit un objet AngleExclusion.
     *
     * @param source            Image source.
     * @param iAngle            Image contenant des angles.
     * @param angle             Angle en degr� d'exclusion des pixels.
     * @param isIncluded        <i>true</i> si les pixels acquis avec un angle de <i>angle</i> 
     *                          doivent �tre �limin�s et <i>false</i> sinon.
     * @param pix               Valeurs des pixels �limin�s.
     * @param layout            Type de l'image de sortie.
     * @param configuration     Configuration du traitement realis� par JAI.
     */
    protected AngleExclusion(final RenderedImage source, final RenderedImage iAngle, final double angle, final boolean isIncluded, final double pix, final ImageLayout layout, final Map configuration) 
    {
        super(source, iAngle, layout, configuration, false);                
        permitInPlaceOperation();
        this.angle      = angle;        
        this.pix        = pix;
        this.isIncluded = isIncluded;
    }
    
    /**
     * Retourne une image dont les pixels trop eloign�s du nadir sont remplac�s par <CODE>
     * pix</CODE>. Les autres pixels sont copi�s dans l'image de sortie.
     *
     * @param source            Image source.
     * @param iAngle            Image contenant des angles.
     * @param angle             Angle des pixels � exclure en degr� .
     * @param isIncluded        <i>true</i> si les pixels acquis avec un angle de <i>angle</i> 
     *                          doivent �tre �limin�s et <i>false</i> sinon.
     * @param pix               Valeurs affect�e aux pixels �limin�s.
     * @param configuration     Configuration du traitement realis� par JAI.
     * @return une image dont les pixels trop eloign�s du nadir sont remplac�s par <CODE>
     *         pix</CODE>. Les autres pixels sont copi�s dans l'image de sortie.
     */
    public static RenderedImage get(final RenderedImage      source, 
                                    final RenderedImage      iAngle, 
                                    final double             angle,
                                    final boolean            isIncluded,
                                    final double             pix,
                                    final Map                configuration) 
    {
        if (source == null)
            throw new IllegalArgumentException("Source is null.");        
        if (iAngle == null)
            throw new IllegalArgumentException("iAngle is null.");        
        
        // D�finition du type de l'image de sortie.
        final RenderedImage[] array = {source, iAngle};
        Rectangle bound = null;
        int numBands = 0;
        for (int index=0 ; index<array.length ; index++)
        {
            final int minX   = array[index].getMinX(),
                      minY   = array[index].getMinY(),
                      width  = array[index].getWidth(),
                      height = array[index].getHeight();
            final Rectangle rect = new Rectangle(minX,minY, width, height);            
            if (bound == null)
            {
                bound = rect;
                numBands = array[index].getSampleModel().getNumBands();
            }
            else
            {
                bound = rect.intersection(bound);
                Math.min(numBands, array[index].getSampleModel().getNumBands());
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
        
        return new AngleExclusion(source,
                                  iAngle,
                                  angle, 
                                  isIncluded,
                                  pix, 
                                  layout,
                                  configuration);
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
        final RectIter iSource = RectIterFactory.create(sources[0],destRect);       
        final RectIter iAngle = RectIterFactory.create(sources[1],destRect);       
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);                 
        
        iAngle.startBands();        
        iSource.startBands();
        iTarget.startBands();
        while (!iTarget.finishedBands())
        {        
            iAngle.startLines();
            iSource.startLines();
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                iAngle.startPixels();      
                iTarget.startPixels();
                iSource.startPixels();
                while (!iTarget.finishedPixels())
                {
                    final double a = iAngle.getSampleDouble();                
                    if ((a < angle) || (a == angle && !isIncluded))                
                        iTarget.setSample(iSource.getSampleDouble());
                    else 
                        iTarget.setSample(pix);
                    iAngle.nextPixel();
                    iTarget.nextPixel();
                    iSource.nextPixel();
                }        
                iAngle.nextLine();
                iTarget.nextLine();
                iSource.nextLine();
            }       
            iAngle.nextBand();
            iTarget.nextBand();
            iSource.nextBand();            
        }
    }    
}