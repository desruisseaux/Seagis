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
 * image permettant de distinguer les pixels acquis dans un état dit <i>nuit</i>, <i>jour</i>
 * ou transitoire. Cette distinction peut être nécéssaire par exemple lors du calcul S.S.T.,
 * les coefficients de la formule S.S.T. étant différent pour des pixels acquis de nuit et
 * de jour.<BR><BR>
 *
 * Les pixels de l'image produite auront pour valeurs :
 * <UL>
 *  <LI><CODE>MatrixDayNight.DAY</CODE> pour un pixel acquis de jour (DAY vaut 0).</LI>
 *  <LI><CODE>MatrixDayNight.NIGHT</CODE> pour un pixel acquis de nuit (NIGHT vaut 200).</LI>
 *  <LI>Une valeur transitoire entre DAY et NIGHT indiquant que le pixel a été acquis dans 
 *      un état plus proche du jour, ou plus proche de la nuit (les valeurs seront dans 
 *      l'intervalle ]0..200[).</LI>
 * </UL><BR><BR>
 *
 * Pour déterminer si un pixel est acquis de nuit, de jour ou dans un état transitoire, 
 * on utilise le critère d'élévation du soleil par rapport au pixel à l'instant de son 
 * acquisition.Pour cela, on considère deux périodes particulières d'une journée : 
 * <UL>
 *  <LI>L'aube.</LI>
 *  <LI>Le crépuscule.</LI>
 * </UL><BR><BR>
 *
 * Durant l'aube, le soleil va se lever et réchauffer progressivement la surface de l'eau. 
 * Durant le crépuscule, le soleil va se coucher et la surface de l'eau va se refroidir 
 * progressivement.<BR>
 * Pour prendre en compte ces deux périodes particulières d'une journée qui sont entre la 
 * nuit et le jour, il est nécéssaire de les définir en fonction de l'angle d'élévation 
 * du  soleil. <BR>
 * On considère que l'angle d'élévation peut varier entre -90° et 90°. L'état descendant 
 * ou  montant du soleil est obtenu par la classe <CODE>SolarRelativeAngle</CODE>. Cet 
 * état determine si l'on doit considérer la transition aube ou crépuscule.<BR><BR>
 *
 * Exemple : on pourrait fixer l'interval [40°..55°] pour l'aube et [-10°..40°] pour le 
 * crépuscule dans notre cas.
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
    
    /** Permet de caluler l'heure ou le soleil est le plus haut dans la journée. */
    private final SunRelativePosition solarPosition;
        
    /** 
     * Grille de localisation des pixels de l'image. Cette grille nos renseigne également
     * sur l'heure d'acquisition des pixels.
     */
    private final LocalizationGridN1B grid;
    
    /** Fonction caractérisant les passages nuit->jour et jour->nuit. */
    private final TransfertFunction fAube;
    
    /** Fonction caractérisant les passages jour->nuit et nuit->jour. */
    private final TransfertFunction fCrepuscule;

   /** 
    * Construit un MatrixDayNight.
    *
    * @param elevation         Une image contenant l'angle d'élévation du soleil pour 
    *                          chacun des pixels.
    * @param layout            Définition du type de l'image de sortie.
    * @param grid              Grille de localisation des pixels de l'image.
    * @param rAube             Intervalle définissant l'aube en fonction de l'angle 
    *                          d'élévation du soleil en degré.
    * @param rCrepuscule       Intervalle définissant le crepuscule en fonction  de l'angle 
    *                          d'élévation du soleil en degré.
    * @param configuration     Configuration du traitement réalisé par JAI.
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
        
        // Définition de la fonction caractérisant l'aube.
        double inf = ((Double)rAube.getMinValue()).doubleValue(),
               sup = ((Double)rAube.getMaxValue()).doubleValue();
        boolean infIncluded = rAube.isMinIncluded(),
                supIncluded = rAube.isMaxIncluded();
        fAube = new TransfertFunction(inf, infIncluded, sup, supIncluded);        
        
        // Définition de la fonction caractérisant le crepuscule.        
        inf = ((Double)rCrepuscule.getMinValue()).doubleValue();
        sup = ((Double)rCrepuscule.getMaxValue()).doubleValue();
        infIncluded = rCrepuscule.isMinIncluded();
        supIncluded = rCrepuscule.isMaxIncluded();
        fCrepuscule = new TransfertFunction(inf, infIncluded, sup, supIncluded);        
        
        solarPosition = new SunRelativePosition();                
        solarPosition.setTwilight(Double.NaN);
    }    
    
    /**
     * Retourne une matrice indiquant l'état de chacun des pixels lors de son acquisition. 
     * L'état d'un pixel peut être <CODE>DAY</CODE>, <CODE>NIGHT</CODE> ou transitoire 
     * entre <CODE>DAY</CODE> et <CODE>NIGHT</CODE>.
     *
     * @param elevation         Une image contenant pour chaque pixel l'angle d'élévation 
     *                          du soleil par rapport à lui lors de son acquisition.
     * @param grid              La grille de localisation des pixels de l'image.
     * @param rAube             Intervalle caractérisant l'aube en fonction de l'angle 
     *                          d'élévation du soleil.
     * @param rCrepuscule       Intervalle caractérisant le crépuscule en fonction de
     *                          l'angle d'elevation du soleil.
     * @param configuration     Configuration du traitement réalisé par JAI.
     * @return une matrice indiquant l'état de chacun des pixels lors de son acquisition. 
     * L'état d'un pixel peut être <CODE>DAY</CODE>, <CODE>NIGHT</CODE> ou transitoire 
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
        
        // Définition du type de l'image de sortie.
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
                
                // Calcul de l'heure ou le soleil est au plus haut dans la journée.
                solarPosition.setDate(new Date(timeAcquisition));        
                final long timeOfNoon = timeAcquisition - (timeAcquisition%86400000) + 
                                        solarPosition.getNoonTime();                        

                /* <CODE>isBeforeNoon</CODE> à <i>true</i> si l'heure d'acquisition de 
                 * la ligne par le satellite a eut lieu avant l'heure ou le soleil est au 
                 * plus haut dans la journée. */
                final boolean isBeforeNoon = timeAcquisition < timeOfNoon;                 

                iSource.startPixels();
                iTarget.startPixels();                            
                while (!iTarget.finishedPixels())
                {
                    final double value = iSource.getSampleDouble();
                    
                    // Calcul de l'état du pixel.
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