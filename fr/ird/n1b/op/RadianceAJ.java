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
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// SEAGIS
import fr.ird.util.CoefficientGrid;
import fr.ird.io.text.ParseSatellite;
import fr.ird.n1b.io.Satellite;

/**
 * Cette classe calcul la radiance. La radiance est obtenue depuis 
 * les valeurs brutes (rawData) issues des canaux thermiques du capteur AVHRR. Cette 
 * classe est operationnelle pour les valeurs issues du capteur thermique AVHRR des 
 * satellites NOAA A-J (Pour les satellites NOAA A-J, les canaux  thermiques sont les 
 * canaux 3, 4 and 5).<BR><BR>
 *
 * La formule de calcul est extraite de 
 * <a href="http://www2.ncdc.noaa.gov/docs/podug/html/c3/sec3-3.htm">
 * NOAA Polar Orbiter Data User's Guide (Section 3.3)</a>.<BR><BR>
 * 
 * Extrait de la documentation (section : 3.1 Thermal Channel Calibration) :<BR><BR>
 *
 * <I>The scaled thermal channel slope values are in units of mW/(m2-sr-cm-1) per count 
 * and the intercept is in mW/(m2-sr-cm-1).<BR><BR>
 *
 * The radiance measured by the sensor (Channel i) is computed as a linear function of 
 * the input data values as follows: <BR><BR>
 *
 * <CENTER>Ei = Si*C + Ii </CENTER><BR><BR>
 *
 * Ei is the radiance value in mW/(m2-sr-cm-1)<BR>
 * C is the input data value (ranging from 0 to 1023 counts)<BR>
 * Si and Ii are respectively the scaled slope and intercept values.<BR></I>
 *
 *
 * @author  Remi EVE
 * @version $Id$
 */
final class RadianceAJ extends Radiance
{   
    /**
     * Constante definissant les parametres accessibles.
     */
    public static final String SLOPE_INTERCEPT_COEFFICIENT = "SLOPE INTERCEPT COEFFICIENTS";
    
    /**
     * Grille contenant les coefficients de calibration.
     */
    private final CoefficientGrid grid;    
        
    /** 
     * Construit un GridCoverage contenant la radiance.
     *
     * @param raw            Une image contenant des donnees brutes issues d'un canal 
     *                       thermique.
     * @param layout         Definition du type de l'image de sortie.
     * @param parameters     Parametres du calcul.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    public RadianceAJ(final RenderedImage       raw, 
                      final ImageLayout         layout, 
                      final ParameterList       parameters, 
                      final Map                 configuration) 
    {
        super(raw, layout, configuration);  
        grid = (CoefficientGrid)parameters.getObjectParameter(SLOPE_INTERCEPT_COEFFICIENT);
    }    
        
    /**
     * Computes a rectangle of output, given PlanarImage sources.  This method should be overridden 
     * by OpImage subclasses that do not require cobbled sources; typically they will instantiate 
     * iterators to perform source access, but they may access sources directly (via the 
     * SampleModel/DataBuffer interfaces) if they wish.
     *
     * Since the subclasses of OpImage may choose between the cobbling and non-cobbling versions of
     * computeRect, it is not possible to leave this method abstract in OpImage. Instead, a default
     * implementation is provided that throws a RuntimeException.
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
                int col = (int)destRect.getX();
                final double[] coeff = grid.getRecord(row);                
                iSource.startPixels();            
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {
                    final double radiance = Math.max(compute(iSource.getSampleDouble(), coeff[0], coeff[1]), 0);
                    iTarget.setSample(radiance);
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
        final String descriptor       = "RADIANCE";
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