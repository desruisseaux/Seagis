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
import java.util.Map;
import java.util.Date;

// Geotools
import org.geotools.science.astro.SunRelativePosition;

// JAI
import javax.media.jai.util.Range;
import javax.media.jai.RasterFactory;
import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// SEAGIS
import fr.ird.science.math.TransfertFunction;
import fr.ird.n1b.io.LocalizationGridN1B;


/**
 * Calcul la matrice d'acquisition des pixels par le satellite. Cette matrice est une 
 * image permettant de distinguer les pixels acquis dans un �tat dit <i>nuit</i>, <i>jour</i>
 * ou transitoire. Cette distinction peut �tre n�c�ssaire par exemple lors du calcul S.S.T.,
 * les coefficients de la formule S.S.T. �tant diff�rent pour des pixels acquis de nuit et
 * de jour.<BR><BR>
 *
 * Les pixels de l'image produite auront pour valeurs :
 * <UL>
 *  <LI><CODE>MatrixDayNight.DAY</CODE> pour un pixel acquis de jour (DAY vaut 0).</LI>
 *  <LI><CODE>MatrixDayNight.NIGHT</CODE> pour un pixel acquis de nuit (NIGHT vaut 200).</LI>
 *  <LI>Une valeur transitoire entre DAY et NIGHT indiquant que le pixel a �t� acquis dans 
 *      un �tat plus proche du jour, ou plus proche de la nuit (les valeurs seront dans 
 *      l'intervalle ]0..200[).</LI>
 * </UL><BR><BR>
 *
 * Pour d�terminer si un pixel est acquis de nuit, de jour ou dans un �tat transitoire, 
 * on utilise le crit�re d'�l�vation du soleil par rapport au pixel � l'instant de son 
 * acquisition.Pour cela, on consid�re deux p�riodes particuli�res d'une journ�e : 
 * <UL>
 *  <LI>L'aube.</LI>
 *  <LI>Le cr�puscule.</LI>
 * </UL><BR><BR>
 *
 * Durant l'aube, le soleil va se lever et r�chauffer progressivement la surface de l'eau. 
 * Durant le cr�puscule, le soleil va se coucher et la surface de l'eau va se refroidir 
 * progressivement.<BR>
 * Pour prendre en compte ces deux p�riodes particuli�res d'une journ�e qui sont entre la 
 * nuit et le jour, il est n�c�ssaire de les d�finir en fonction de l'angle d'�l�vation 
 * du  soleil. <BR>
 * On consid�re que l'angle d'�l�vation peut varier entre -90� et 90�. L'�tat descendant 
 * ou  montant du soleil est obtenu par la classe <CODE>SolarRelativeAngle</CODE>. Cet 
 * �tat determine si l'on doit consid�rer la transition aube ou cr�puscule.<BR><BR>
 *
 * Exemple : on pourrait fixer l'interval [40�..55�] pour l'aube et [-10�..40�] pour le 
 * cr�puscule dans notre cas.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class MatrixDayNight extends PointOpImage 
{      
    /** Identifie un pixel dit de <i>jour</i>. */ 
    public static final double DAY = 0;
    
    /** Identifie un pixel dit de <i>nuit</i>. */ 
    public static final double NIGHT = 200;
    
    /** Permet de caluler l'heure ou le soleil est le plus haut dans la journ�e. */
    private final SunRelativePosition solarPosition;
        
    /** 
     * Grille de localisation des pixels de l'image. Cette grille nos renseigne �galement
     * sur l'heure d'acquisition des pixels.
     */
    private final LocalizationGridN1B grid;
    
    /** Fonction caract�risant les passages nuit->jour et jour->nuit. */
    private final TransfertFunction fAube;
    
    /** Fonction caract�risant les passages jour->nuit et nuit->jour. */
    private final TransfertFunction fCrepuscule;

   /** 
    * Construit un MatrixDayNight.
    *
    * @param elevation         Une image contenant l'angle d'�l�vation du soleil pour 
    *                          chacun des pixels.
    * @param layout            D�finition du type de l'image de sortie.
    * @param grid              Grille de localisation des pixels de l'image.
    * @param rAube             Intervalle d�finissant l'aube en fonction de l'angle 
    *                          d'�l�vation du soleil en degr�.
    * @param rCrepuscule       Intervalle d�finissant le crepuscule en fonction  de l'angle 
    *                          d'�l�vation du soleil en degr�.
    * @param configuration     Configuration du traitement r�alis� par JAI.
    */
    private MatrixDayNight(final RenderedImage          elevation, 
                           final ImageLayout            layout, 
                           final LocalizationGridN1B    grid,
                           final Range                  rAube,
                           final Range                  rCrepuscule,
                           final Map                    configuration)      
    {
        super(elevation, layout, configuration, false);                
        permitInPlaceOperation();
        this.grid = grid;
        
        // D�finition de la fonction caract�risant l'aube.
        double inf = ((Double)rAube.getMinValue()).doubleValue(),
               sup = ((Double)rAube.getMaxValue()).doubleValue();
        boolean infIncluded = rAube.isMinIncluded(),
                supIncluded = rAube.isMaxIncluded();
        fAube = new TransfertFunction(inf, infIncluded, sup, supIncluded);        
        
        // D�finition de la fonction caract�risant le crepuscule.        
        inf = ((Double)rCrepuscule.getMinValue()).doubleValue();
        sup = ((Double)rCrepuscule.getMaxValue()).doubleValue();
        infIncluded = rCrepuscule.isMinIncluded();
        supIncluded = rCrepuscule.isMaxIncluded();
        fCrepuscule = new TransfertFunction(inf, infIncluded, sup, supIncluded);        
        
        solarPosition = new SunRelativePosition();                
        solarPosition.setTwilight(Double.NaN);
    }    
    
    /**
     * Retourne une matrice indiquant l'�tat de chacun des pixels lors de son acquisition. 
     * L'�tat d'un pixel peut �tre <CODE>DAY</CODE>, <CODE>NIGHT</CODE> ou transitoire 
     * entre <CODE>DAY</CODE> et <CODE>NIGHT</CODE>.
     *
     * @param elevation         Une image contenant pour chaque pixel l'angle d'�l�vation 
     *                          du soleil par rapport � lui lors de son acquisition.
     * @param grid              La grille de localisation des pixels de l'image.
     * @param rAube             Intervalle caract�risant l'aube en fonction de l'angle 
     *                          d'�l�vation du soleil.
     * @param rCrepuscule       Intervalle caract�risant le cr�puscule en fonction de
     *                          l'angle d'elevation du soleil.
     * @param configuration     Configuration du traitement r�alis� par JAI.
     * @return une matrice indiquant l'�tat de chacun des pixels lors de son acquisition. 
     * L'�tat d'un pixel peut �tre <CODE>DAY</CODE>, <CODE>NIGHT</CODE> ou transitoire 
     * entre <CODE>DAY</CODE> et <CODE>NIGHT</CODE>.
     */
    public static RenderedImage get(final RenderedImage          elevation,  
                                    final LocalizationGridN1B    grid,
                                    final Range                  rAube,
                                    final Range                  rCrepuscule,
                                    final Map                    configuration)
    {        
        if (elevation == null)
            throw new IllegalArgumentException("Elevation is null.");               
        if (grid == null)
            throw new IllegalArgumentException("Grid is null.");
        if (rAube == null)
            throw new IllegalArgumentException("Aube is null.");                       
        if (rCrepuscule == null)
            throw new IllegalArgumentException("Crepuscule is null.");                       
        
        // D�finition du type de l'image de sortie.
        int minX   = elevation.getMinX(),
            minY   = elevation.getMinY(),
            width  = elevation.getWidth(),
            height = elevation.getHeight();
        final ImageLayout layout = new ImageLayout(minX, minY, width, height);
        final int numBands       = elevation.getSampleModel().getNumBands();
        layout.setSampleModel(RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, 
                                                                    width, 
                                                                    height,
                                                                    numBands));
        layout.setColorModel(PlanarImage.getDefaultColorModel(DataBuffer.TYPE_FLOAT,
                                                              numBands));     
        
        // Construction de la matrice. 
        return new MatrixDayNight(elevation,
                                  layout, 
                                  grid,
                                  rAube,
                                  rCrepuscule,
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
        final RectIter iSource = RectIterFactory.create(sources[0],destRect);
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);        
        destRect = destRect.intersection(sources[0].getBounds());
        
        iSource.startBands();
        iTarget.startBands();        
        while (!iTarget.finishedBands())
        {        
            int row = (int)destRect.getY();
            iSource.startLines();
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                // Date d'acquisition de la ligne.
                final long timeAcquisition = grid.getTime(row).getTime();
                
                // Calcul de l'heure ou le soleil est au plus haut dans la journ�e.
                solarPosition.setDate(new Date(timeAcquisition));        
                final long timeOfNoon = timeAcquisition - (timeAcquisition%86400000) + 
                                        solarPosition.getNoonTime();                        

                /* <CODE>isBeforeNoon</CODE> � <i>true</i> si l'heure d'acquisition de 
                 * la ligne par le satellite a eut lieu avant l'heure ou le soleil est au 
                 * plus haut dans la journ�e. */
                final boolean isBeforeNoon = timeAcquisition < timeOfNoon;                 

                iSource.startPixels();
                iTarget.startPixels();                            
                while (!iTarget.finishedPixels())
                {
                    final double value = iSource.getSampleDouble();
                    
                    // Calcul de l'�tat du pixel.
                    // les fonctions <CODE>fAube</CODE> et <CODE>fCrepuscule</CODE>
                    // retourne des valeurs dans l'intervalle [0..1].
                    if (isBeforeNoon) 
                        iTarget.setSample((1.0 - fAube.compute(value))*NIGHT);
                    else 
                        iTarget.setSample((1.0 - fCrepuscule.compute(value))*NIGHT);
                    iSource.nextPixel();
                    iTarget.nextPixel();
                }        
                iSource.nextLine();
                iTarget.nextLine();
                row++;
            }
            iSource.nextBand();
            iTarget.nextBand();            
        }
    }            
}