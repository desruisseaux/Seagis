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
import fr.ird.util.CoefficientGrid;
import fr.ird.io.text.ParseSatellite;
import fr.ird.n1b.io.Satellite;

/**
 * Cette classe calcul la radiance. La radiance est obtenue depuis les 
 * valeurs brutes (rawData) issues des canaux thermiques du capteur AVHRR. Cette classe 
 * est operationnelle pour les valeurs issues du capteur thermique AVHRR des satellites 
 * NOAA KLM (pour les satellites NOAA KLM, les canaux  thermiques sont les canaux 3b, 4 
 * and 5).<BR><BR>
 *
 * La formule est extraite de <a href="http://www2.ncdc.noaa.gov/docs/klm/html/c7/sec7-1.htm">
 * NOAA KLM User's Guide</a>.<BR><BR>
 * 
 * Extrait de la documentation : <BR>
 * <I>[steps to Calibrate the AVHRR thermal channels (Level 1b data users)] Starting with 
 * the NOAA-15 satellite, NESDIS now incorporates the nonlinear radiance corrections for 
 * AVHRR thermal channels 4 and 5 into the new Level 1b data stream.  Users compute the 
 * Earth scene radiance NE in units of W/(m2-sr-cm-1) from the 10-bit Earth scene count 
 * CE by the formula:<BR><BR>
 *
 *                      <CENTER>N = a0 + a1C + a2C*C</CENTER><BR><BR>
 *
 * There is a set of coefficients for each thermal channel 3B, 4, and 5 in the NOAA KLM 
 * Level 1b dataset.  The channel 3B detector responds linearly to incoming radiance so 
 * for channel 3B the coefficient a2 will always be 0.  Section 8 contains format 
 * information about how the Level 1b data are stored. The coefficient a0 for AVHRR 
 * channel 4 is specified as "IR Operational Cal Ch4 
 * Coefficient 1"; etc. <BR></I>
 * 
 * @author  Remi EVE
 * @version $Id$
 */
final class RadianceKLM extends Radiance
{   
    /**
     * Constante definissant les parametres accessibles.
     */
    public static final String THERMAL_COEFFICIENT = "THERMAL CALIBRATION COEFFICIENT";
    
    /**
     * Grille contenant les coefficients de calibration.
     */
    private final CoefficientGrid grid;    
    
    /** 
     * Construit une nouvelle instance de ImageRadianceKLM.
     *
     * @param raw            Une image contenant des donnees brutes issues d'un canal 
     *                       thermique.
     * @param layout         Definition de l'image de destination.
     * @param parameters     Parametres du calcul.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    public RadianceKLM(final RenderedImage raw, 
                       final ImageLayout layout, 
                       final ParameterList parameters, 
                       final Map configuration) 
    {
        super(raw, layout, configuration);        
        grid = (CoefficientGrid)parameters.getObjectParameter(THERMAL_COEFFICIENT);
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
                    final double radiance = Math.max(compute(iSource.getSampleDouble(), coeff[0], coeff[1], coeff[2]), 0);                    
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
     * @param a0    Le premier coefficient.
     * @param a1    Le second coefficient.
     * @param a2    Le troisieme coefficient.
     */
    private static double compute(final double input, final double a0, 
                                 final double a1,    final double a2) 
    {  
        return a0 + input*(a1 + a2*input);
    }
    
    /**
     * Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     * @return Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     */
    public static ParameterList getInputParameterList() 
    {        
        final String descriptor       = "RADIANCE";
        final String[] paramNames     = {THERMAL_COEFFICIENT};
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