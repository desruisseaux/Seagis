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
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.util.Vector;
import java.util.Map;

// JAI
import javax.media.jai.RasterFactory;
import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

/**
 * Calcul la S.S.T. (Sea Surface Temperature) d'une image. Ce calcul nécéssite de préciser :
 * <UL>
 *  <LI>Les coefficients A1, A2, A3, A4 de la formule de jour.</LI>
 *  <LI>Les coefficients A1, A2, A3, A4 de la formule de nuit.</LI>
 *  <LI>L'angle de visé du satellite/capteur lors de l'acquisition de chacun des pixels.</LI>
 *  <LI>La température de brillance de chacun des pixels pour les canaux 4 et 5.</LI>
 *  <LI>L'état d'acquisition des pixels (acquisition de nuit, de jour ou entre jour/nuit).</LI>
 * </UL><BR><BR>
 *
 * La formule de calcul S.S.T. est une équation linéaire dite <i>Linear split window</i> : 
 *<BR><BR>
 *
 * <CENTER><i>S.S.T. (°C) = A1*T4 + A2*T5 + A3*(T4-T5)*(SEC(Z)-1) + A4</CENTER><BR><BR>
 *
 * Avec T4 la température de brillance du canal 4, <BR>
 *      T5 la température de brillance du canal 5, <BR>
 *      A1, A2, A3, A4 les coefficients (ils different pour les pixels acquis de nuit 
 *      ou de jour), <BR>
 *      Z l'angle d'acquisition du pixel (ou encore nommé angle de visé du satellite),<BR>
 *      SEC la sécante de l'angle : SEC(Z) = 1/cos(angle).</i><BR><BR>
 *
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class SST extends PointOpImage
{  
    /**
     * Coefficient des formules jour et nuit du calcul S.S.T..
     */
    private final double[] coeffDay,
                           coeffNight;
        
    /**
     * Construit un SST.
     *
     * @param source         Un vecteur d'images.
     * @param layout         Définition du type de l'image de sortie.
     * @param coeffDay       Les coefficients a0, a1, a2, a3 nécéssaires au calcul de jour.
     * @param coeffNight     Les coefficients a0, a1, a2, a3 nécéssaires au calcul de nuit.
     * @param grid           La grille de localization de l'ensemble des points.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    private SST(final Vector      source, 
                final ImageLayout layout, 
                final double[]    coeffDay, 
                final double[]    coeffNight, 
                final Map         configuration)
    {        
        super(source, layout, configuration, false);                        
        permitInPlaceOperation();
        this.coeffDay   = coeffDay;        
        this.coeffNight = coeffNight;                
    }
    
    /**
     * Retourne une image contenant la S.S.T. (température) de chacun des pixels en degré 
     * celcius.
     *
     * @param tb4               Une image contenant la température de brillance du canal 4
     *                          de chacun des pixels en Kelvin.
     * @param tb5               Une image contenant la température de brillance du canal 5
     *                          de chacun des pixels en Kelvin.
     * @param angle             Une image contenant l'angle de visé du satellite/capteur 
     *                          lors de l'acquisition des pixels en degré.
     * @param matrix            Une image contenant l'état d'acquisition de chacun des 
     *                          pixels (acquisition de jour, de nuit, transitoire jour/nuit).
     * @param coeffDay          Les coefficient a0, a1, a2, a3 de la formule de calcul de 
     *                          la SST de jour.
     * @param coeffNight        Les coefficient a0, a1, a2, a3 de la formule de calcul de 
     *                          la SST de nuit.
     * @param configuration     Configuration du traitement réalisé par JAI.
     * @return une image contenant la S.S.T. (température) de cahcun des pixels en degré 
     * celcius.
     */
    public static RenderedImage get(final RenderedImage      tb4, 
                                    final RenderedImage      tb5, 
                                    final RenderedImage      angle, 
                                    final RenderedImage      matrix,                                    
                                    final double[]           coeffDay,
                                    final double[]           coeffNight,                                    
                                    final Map                configuration)
    {
        if (tb4 == null)
            throw new IllegalArgumentException("TB4 is null."); 
        if (tb5 == null)
            throw new IllegalArgumentException("TB5 is null."); 
        if (angle == null)
            throw new IllegalArgumentException("Angle is null.");        
        if (matrix == null)
            throw new IllegalArgumentException("Matrix is null.");        
        if (coeffDay == null || coeffNight == null)
            throw new IllegalArgumentException("Coefficients are null."); 
        if (coeffDay.length < 4 || coeffNight.length < 4)
            throw new IllegalArgumentException("Number of coefficients is not valid.");        
        
        // Définition du type de l'image de sortie.
        final RenderedImage[] array = {matrix, tb4, tb5, angle};
        Rectangle bound = null;
        int numBands    = 0;
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

        // Construction du gridCoverage a retourner. 
        final Vector source = new Vector();
        source.add(tb4);
        source.add(tb5);
        source.add(angle);
        source.add(matrix);
        return new SST(source, 
                       layout, 
                       coeffDay, 
                       coeffNight,
                       configuration);
    }

    /**
     * Calcul la SST.
     * 
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    public void computeRect(final PlanarImage[] sources, 
                            final WritableRaster dest, 
                            Rectangle destRect) 
    {  
        final RectIter iTB4   = RectIterFactory.create(sources[0],destRect);
        final RectIter iTB5   = RectIterFactory.create(sources[1],destRect);
        final RectIter iAngle = RectIterFactory.create(sources[2],destRect);
        final RectIter iMatrix= RectIterFactory.create(sources[3],destRect);
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);
        destRect = destRect.intersection(sources[0].getBounds());
        destRect = destRect.intersection(sources[1].getBounds());
        destRect = destRect.intersection(sources[2].getBounds());
        destRect = destRect.intersection(sources[3].getBounds());        
        
        iTB4.startBands();
        iTB5.startBands();
        iAngle.startBands();
        iMatrix.startBands();        
        iTarget.startBands();                
        while (!iTarget.finishedBands())
        {        
            iTB4.startLines();
            iTB5.startLines();
            iAngle.startLines();
            iMatrix.startLines();            
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                iTB4.startPixels();            
                iTB5.startPixels();            
                iAngle.startPixels();                       
                iMatrix.startPixels();                       
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {                                                           
                    final double matrix = iMatrix.getSampleDouble(),
                                 tb4    = iTB4.getSampleDouble(),
                                 tb5    = iTB5.getSampleDouble(),
                                 angle  = iAngle.getSampleDouble();

                    // Calcul S.S.T.
                    switch ((int)matrix)
                    {
                        case (int)MatrixDayNight.DAY :          
                            // Pixel acquis de jour : formule de jour.
                            final double sstDay = computeSST(coeffDay, angle, tb4, tb5);                                                                        
                            iTarget.setSample(sstDay);
                            break;
                            
                        case (int)MatrixDayNight.NIGHT :                       
                            // Pixel acquis de nuit : formule de nuit.
                            final double sstNight = computeSST(coeffNight, angle, tb4, tb5);
                            iTarget.setSample(sstNight);
                            break;
                            
                        default :
                            // Pixel acquis entre jour et nuit : formule de jour + formule de nuit.                            
                            final double  sstDay_   = computeSST(coeffDay,   angle, tb4, tb5);
                            final double  sstNight_ = computeSST(coeffNight, angle, tb4, tb5);                                                                
                            iTarget.setSample(sstDay_ + (sstNight_ - sstDay_) * 
                                              matrix/MatrixDayNight.NIGHT);  
                            break;
                    }       
                    
                    iTB4.nextPixel();
                    iTB5.nextPixel();                    
                    iAngle.nextPixel();                                       
                    iMatrix.nextPixel();                                       
                    iTarget.nextPixel();
                }        
                iTB4.nextLine();
                iTB5.nextLine();
                iAngle.nextLine();                       
                iMatrix.nextLine();                       
                iTarget.nextLine();
            }
            iTB4.nextBand();
            iTB5.nextBand();
            iAngle.nextBand();                       
            iMatrix.nextBand();                       
            iTarget.nextBand();
        }
    } 
    
    /**
     * Calcul de la S.S.T. d'un pixel.
     *
     * @param coeff         Tableau de coefficients de la SST. 
     * @param angle         Angle de visé du satellite lors de l'acquisition du pixel.
     * @param tb4           Température de brillance du canal 4.
     * @param tb5           Température de brillance du canal 5.
     * @return La SST.
     */ 
    private static double computeSST(final double[] coeff, 
                                     final double   angle,
                                     final double   tb4, 
                                     final double   tb5) 
    {
        return (coeff[0] * tb4 + coeff[1] * tb5 + coeff[2] * (tb4 - tb5) * 
                (computeSecante(angle) - 1.0) + coeff[3]);
    }

    /**
     * Calcul de la sécante de l'angle. 
     *
     * @param angle  Angle de visé du satellite lors de l'acquisition du pixel en degré.
     * @return la sécante de l'angle.
     */ 
    private static double computeSecante(final double angle) 
    {        
        return (1.0/Math.cos(Math.toRadians(angle)));
    }
}