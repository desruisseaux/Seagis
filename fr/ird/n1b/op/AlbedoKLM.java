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
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

// JAI
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// SEAGIS
import fr.ird.util.CoefficientGrid;
import fr.ird.io.text.ParseSatellite;

/**
 * Calcul le pourcentage d'albedo. L'albedo est obtenu depuis les valeurs brutes calculées 
 * à partir des canaux thermiques du capteur AVHRR. Cette classe est opérationnelle pour 
 * les valeurs issues du capteur thermique AVHRR des satellites NOAA KLM (pour les 
 * satellites NOAA KLM, les canaux  thermiques sont les canaux 3b, 4 and 5). <BR><BR>
 *
 * La formule est extraite de 
 * <a href="http://www2.ncdc.noaa.gov/docs/klm/html/c7/sec7-1.htm"> NOAA KLM User's 
 * Guide</a>.<BR><BR>
 * 
 * Extrait de la documentation : <BR>
 * <i>It is the usual practice in NOAA to give the pre-launch calibration results in the 
 * form of a simple linear regression relationship between the measured AVHRR signal, C10, 
 * expressed in ten-bit counts, and the albedo, A, of the integrating sphere source at 
 * different levels of illumination. Thus,<BR><BR>
 *
 * <CENTER>A = SC10 + I </CENTER><BR><BR>
 * 
 * where S is the slope (percent albedo/count), <BR>
 * I is the intercept (percent albedo) listed in the Level 1b data under the heading 
 * "pre-launch.".<BR><BR>
 *
 * It should therefore be noted that the use of these slope and intercept values with the 
 * measured AVHRR signal will yield the albedo in percent under the assumption that the 
 * pre-launch calibration is valid in orbit.<i><BR><BR>
 *
 * NOAA recommande d'utiliser les recommandation extraite de 
 * <a href="http://www2.ncdc.noaa.gov/docs/klm/html/d/app-d2.htm">Appendix D.2 NOAA-16 (L) 
 * </a> 
 * pour le calcul de la reflectance : <BR>
 * NOAA recommends using the following equations for NOAA AVHRR reflectance channels : 
 * <BR><BR>
 *  
 * <CENTER>A = SLOPE_1 * C10 - INTERCEPT_1,  if (C10<=INTERSECTION_POINT) </CENTER>
 * <CENTER>A = SLOPE_2 * C10 - INTERCEPT_2,  if (C10>INTERSECTION_POINT) </CENTER><BR><BR>
 * 
 * SLOPE_1, SLOPE_2, INTERCEPT_1, INTERCEPT_2 and INTERSECTION are in data record.
 * 
 * @author  Remi EVE
 * @version $Id$
 */
final class AlbedoKLM extends Albedo
{   
    /**
     * Constante definissant les parametres accessibles.
     */
    public static final String SLOPE_INTERCEPT_COEFFICIENT = "SLOPE INTERCEPT COEFFICIENTS";
    /**
     * Grille contenant les coefficients de calibration utile lors de la conversion.
     */
    private final CoefficientGrid grid;    
    
    /** 
     * Construit une nouvelle instance de AlbedoKLM.
     *
     * @param image           Une image contenant des donnees brutes issues d'un canal 
     *                        thermique.
     * @param layout          Definition de l'image de destination.
     * @param parameters      Parametres du calcul.
     * @param configuration   Configuration du traitement realise par JAI.
     */
    public AlbedoKLM(final RenderedImage         image, 
                     final ImageLayout           layout, 
                     final ParameterList         parameters,                     
                     final Map                   configuration) 
    {
        super(image, layout, configuration);        
        grid = (CoefficientGrid)parameters.getObjectParameter(SLOPE_INTERCEPT_COEFFICIENT);
    }    
        
    /**
     * Calcul l'albedo. 
     *
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    protected void computeRect(final PlanarImage[] sources, 
                            final WritableRaster dest, 
                            Rectangle destRect) 
    {        
        final RectIter iSource         = RectIterFactory.create(sources[0],destRect);
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
                int col = (int)destRect.getX();
                final double[] coeff = grid.getRecord(row);                
                iSource.startPixels();            
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {
                    final double albedo = Math.max(compute(iSource.getSampleDouble(), 
                                                           coeff[0], 
                                                           coeff[1],
                                                           coeff[2],
                                                           coeff[3],
                                                           coeff[4]),0);
                    iTarget.setSample(albedo);
                    iSource.nextPixel();
                    iTarget.nextPixel();
                    col ++;
                }        
                iSource.nextLine();
                iTarget.nextLine();
                row++;
            }
            iSource.nextBand();
            iTarget.nextBand();
        }
    }       
    
    /**
     * Convertit une donnee brute issue d'un canal thermique AVHRR en radiance. 
     *
     * @param input Valeur brute (dans l'intervalle [0..1023]) a convertir.
     * @param slope1        The scaled slope value (mW/(m2-sr-cm-1).
     * @param intercept1    The intercept value (mW/(m2-sr-cm-1).
     * @param slope2        The scaled slope value (mW/(m2-sr-cm-1).
     * @param intercept2    The intercept value (mW/(m2-sr-cm-1).
     * @param intersection  The intersection value.
     */
    private static double compute(final double input, 
                                 final double slope1, final double intercept1, 
                                 final double slope2, final double intercept2, 
                                 final double intersection) 
    {          
        if (input <=  intersection)
            return slope1*input + intercept1;
        else
            return slope2*input + intercept2;            
    }
    
    /**
     * Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     * @return Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     */
    public static ParameterList getInputParameterList() 
    {        
        final String descriptor       = "ALBEDO";
        final String[] paramNames     = {SLOPE_INTERCEPT_COEFFICIENT};
        final Class[]  paramClasses   = {CoefficientGrid.class};
        final Object[]  paramDefaults = {null};
        final ParameterList parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                                               paramNames,
                                                                                               paramClasses,
                                                                                               paramDefaults,
                                                                                               null));        
        return parameters;    
    }                
}    