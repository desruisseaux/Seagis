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
import java.awt.Rectangle;
import java.util.Map;

// JAI
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// SEAGIS
import fr.ird.io.text.ParseSatellite;

/**
 * Calcul la température de brillance. La température est obtenue depuis la radiance issues 
 * des canaux thermiques du capteur AVHRR. Cette classe est opérationnelle pour les données 
 * issues du capteur thermique AVHRR des satellites NOAA A-J (pour les satellites NOAA A-J, 
 * les canaux  thermiques sont les canaux 3, 4 and 5).<BR><BR>
 *
 * La formule est extraite de <a href="http://www2.ncdc.noaa.gov/docs/podug/html/c3/sec3-3.htm">
 * NOAA Polar Orbiter Data User's Guide (Section 3.3)</a>.<BR><BR>
 *
 * Extrait de la documentation : <BR>
 * <I>The conversion to brightness temperature from radiance is performed using the 
 * inverse of Planck's radiation equation: <BR><BR>
 *
 * <CENTER>T = (c2*v) / ln(1 + (c1*v*v*v/E))</CENTER><BR><BR>
 *
 * T is the temperature (K) for the radiance value E, <BR>
 * v is the central wave number of the channel (cm-1) <BR>
 * C1 and C2 are constants (C1 = 1.1910659 x 10-5 mW/(m2-sr-cm-4) and C2 = 1.438833 cm-K).
 * </I><BR><BR>
 *
 * Note that the temperatures obtained by this procedure are not corrected for atmospheric 
 * attenuation, etc.
 *
 * @author  Remi EVE
 * @version $Id$
 */
final class TemperatureAJ extends Temperature
{    
    /**
     * Constante definissant les parametres accessibles.
     */
    public static final String WAVE_LENGTH = "CENTRAL WAVE LENGHT";
    
    /** 
     * Constantes de radiation utilisees pour le calcul de la temperature de brillance. 
     */
    private static final double CONSTANT1 = 1.1910659E-5, // (mW/(m2-sr-cm-4))
                                CONSTANT2 = 1.438833;     // (cm-K)        
    
    /**
     * Central wave numbers.
     */
    private double wave;
    
    /** 
     * Construit une instance de TemperatureAJ.
     *
     * @param image          Une image contenant des donnees de radiance.
     * @param layout         Definition du type de l'image de sortie.
     * @param parameters     Parametres du calcul.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    protected TemperatureAJ(final RenderedImage         image, 
                            final ImageLayout           layout, 
                            final ParameterList         parameters, 
                            final Map                   configuration) 
    {
        super(image, layout, configuration);                
        wave = parameters.getDoubleParameter(WAVE_LENGTH);
    }    
    
    /**
     * Calcul la température. 
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
                iSource.startPixels();            
                iTarget.startPixels();                
                int col = (int)destRect.getX();
                while (!iTarget.finishedPixels())
                {
                    final double temperature = compute(iSource.getSampleDouble(), wave);
                    //assert !Double.isNaN(temperature);
                    iTarget.setSample(temperature);
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
      * Convertit une donnee de radiance issue d'un canal thermique AVHRR en temperature(K) 
      * brillance.       
      *
      * Note : The temperature compute isn't corrected for atmospheric attenuation, etc.
      *
      * @param  input radiance a convertir.
      * @param  centralWaveNumber The central Wave Number value.
      * @return la temperature de brillance.
      */
     public static double compute(final double input, final double centralWaveNumber) 
     {                                    
         final double centralWaveNumber_3 = centralWaveNumber*centralWaveNumber*centralWaveNumber;
         return ((CONSTANT2*centralWaveNumber)/Math.log(1.0+(CONSTANT1*centralWaveNumber_3)/input));
     }    
     
    /**
     * Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     * @return Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     */
    public static ParameterList getInputParameterList() 
    {        
        final String descriptor       = "TEMPERATURE";
        final String[] paramNames     = {WAVE_LENGTH};
        final Class[]  paramClasses   = {Double.class};
        final Object[]  paramDefaults = {null};
        final ParameterList parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                                               paramNames,
                                                                                               paramClasses,
                                                                                               paramDefaults,
                                                                                               null));        
        return parameters;    
    }                 
}