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
 * Calcul la température de brillance. Elle est obtenue depuis les valeurs de radiance 
 * issues des canaux thermiques du capteur AVHRR. Cette classe est opérationnelle pour les 
 * données issues du capteur thermique AVHRR des satellites NOAA KLM (pour les satellites 
 * NOAA KLM, les canaux thermiques sont les canaux 3b, 4 and 5 de l'image N1B).<BR><BR>
 *
 * Cette formule est extraite de la documentation 
 * <a href="http://www2.ncdc.noaa.gov/docs/klm/html/c7/sec7-1.htm">
 * NOAA KLM User's Guide(Section 7.1)</a>.<BR><BR>
 *
 * Extrait de la documentation : <BR>
 * <I>Two formula are used for converting radiance value to temperature 
 * brightness<BR><BR>
 *
 * <CENTER>T' = (c2*v) / (ln(1+(c1*v*v*v)/N))</CENTER><BR><BR>
 *
 * <CENTER>T = (T' - A)/B</CENTER></I><BR><BR>
 *
 *
 * @author  Remi EVE
 * @version $Id$
 */
final class TemperatureKLM extends Temperature
{    
    /**
     * Constante definissant les parametres accessibles.
     */
    public static final String CENTRAL_WAVE_LENGHT      = "WAVE",
                               RADIANCE_TO_TEMPERATURE  = "RADIANCE TO TEMPERATURE COEFFICIENT";
    
    /** 
     * Constantes de radiation utilisees pour le calcul de la temperature de brillance. 
     */
    private static final double CONSTANT1 = 1.1910427E-5,  // (mW/(m2-sr-cm-4))
                                CONSTANT2 = 1.4387752;     // (cm-K)        
    
    /**
     * Constante de radiation.
     */
    private double a;

    /**
     * Constante de radiation.
     */
    private double b;
    
    /**
     * Constante de radiation.
     */
    private double vc;

    /** 
     * Construit une instance de TemperatureKLM.
     *
     * @param image          Une image contenant des donnees de radiance.
     * @param layout         Definition du type de l'image de sortie.
     * @param parameters     Parametres du calcul.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    protected TemperatureKLM(final RenderedImage image, 
                             final ImageLayout layout, 
                             final ParameterList parameters, 
                             final Map configuration) 
    {
        super(image, layout, configuration);                
        final double[] array = (double[])parameters.getObjectParameter(RADIANCE_TO_TEMPERATURE);
        a = array[0];
        b = array[1];
        vc = parameters.getDoubleParameter(CENTRAL_WAVE_LENGHT);
    }    
    
    /**
     * Calcul la temperature.
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
                    final double temperature = compute(iSource.getSampleDouble(), vc, a,  b);

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
      * @param  input             Radiance a convertir.
      * @param  vc                The central Wave Number value.
      * @param  a                 La constante A.
      * @param  b                 Le constante B.
      * @return la temperature de brillance.
      */
     public static double compute(final double input,     final double vc, 
                                  final double a        , final double b) 
     {                                                   
         final double vc_3 = vc*vc*vc;
         final double temperature_ = ((CONSTANT2*vc) / Math.log(1.0+CONSTANT1*vc_3/input));
         return (temperature_ - a)/b;         
     }    
     
    /**
     * Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     * @return Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     */
    public static ParameterList getInputParameterList() 
    {        
        final String descriptor       = "TEMPERATURE";
        final String[] paramNames     = {RADIANCE_TO_TEMPERATURE,
                                         CENTRAL_WAVE_LENGHT};
        final Class[]  paramClasses   = {double[].class,
                                         Double.class};
        final Object[]  paramDefaults = {null,
                                         null};
        final ParameterList parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                                               paramNames,
                                                                                               paramClasses,
                                                                                               paramDefaults,
                                                                                               null));        
        return parameters;    
    }                      
}