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
 * Calcul le pourcentage d'albedo. L'albedo est obtenu depuis les valeurs brutes (rawData) 
 * issues des canaux du visible du capteur AVHRR. Cette classe est operationnelle pour les 
 * valeurs issues du capteur thermique AVHRR des satellites NOAA A-J (pour les satellites 
 * NOAA A-J, les canaux  visibles sont les canaux 1 et 2 de l'image N1B).<BR><BR>
 *
 * La formule de calcul est extraite de 
 * <a href="http://www2.ncdc.noaa.gov/docs/podug/html/c3/sec3-3.htm">
 * NOAA Polar Orbiter Data User's Guide (Section 3.3)</a>.<BR><BR>
 * 
 * Extrait de la documentation (section : 3.1 Visible Channel Calibration) :<BR><BR>
 * <i> The scaled visible channel slope values are in units of percent albedo/count for 
 * slope and in percent albedo for intercept. The percent albedo measured by the sensor 
 * channel i is computed as a linear function of the input data value as follows:<BR><BR>
 *
 * <CENTER>Ai = SiC + Ii</CENTER><BR><BR>
 *
 * where Ai is the percent albedo measured by channel i,<BR>
 * C is the input data value in counts,<BR>
 * Si and Ii are respectively, the scaled slope and intercept values. </i>
 *
 * @author  Remi EVE
 * @version $Id$
 */
final class AlbedoAJ extends Albedo
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
     * Construit une nouvelle instance de AlbedoAJ.
     *
     * @param image           Une image contenant des donnees brutes issues d'un canal 
     *                        du visible.
     * @param layout          Definition de l'image de destination.
     * @param parameters      Parametres du calcul.
     * @param configuration   Configuration du traitement realise par JAI.
     */
    public AlbedoAJ(final RenderedImage         image,      
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
                final double[] coeff = grid.getRecord(row);                
                int col = (int)destRect.getX();
                iSource.startPixels();            
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {
                    final double albedo = Math.max(compute(iSource.getSampleDouble(), 
                                                           coeff[0], 
                                                           coeff[1]), 0);
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
     * Convertit une donnee brute issue d'un canal visible AVHRR en pourcentage d'albedo. 
     *
     * @param input Valeur brute (dans l'intervalle [0..1023]) a convertir.
     * @param a0    The scaled slope value (mW/(m2-sr-cm-1).
     * @param a1    The intercept value (mW/(m2-sr-cm-1).
     */
    private static double compute(final double input, final double a0, final double a1) 
    { 
        return a0*input+a1;
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