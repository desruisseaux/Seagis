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

// J2SE / JAI
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.Rectangle;
import java.util.Map;
import java.awt.geom.Rectangle2D;
import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

// SEAGIS
import fr.ird.n1b.io.Satellite;

/**
 * Calcul la Température de brillance. Cette température est obtenue depuis les valeurs 
 * de radiance issue d'un capteur AVHRR. 
 *
 * @author  Remi EVE
 * @version $Id$
 */
public abstract class Temperature extends PointOpImage 
{  
    /** 
     * Construit un Temperature.
     *
     * @param radiance       Une image contenant des données de radiance.
     * @param layout         Type de l'image de sortie.
     * @param configuration  Configuration du comportement du JAI.
     */
    protected Temperature(final RenderedImage       radiance, 
                          final ImageLayout         layout, 
                          final Map                 configuration) 
    {
        super(radiance, layout, configuration, false);                
        permitInPlaceOperation();
    }    
    
    /**
     * Retourne une image contenant la temperature de brillance.
     *
     * @param satellite      Satellite ayant réalisé l'acquisition des données.
     * @param radiance       La radiance.
     * @param parameters     Les coefficients de calibration.
     * @param configuration  Configuration du comportement du JAI.
     * @return une image de Temperature. 
     */
    public static RenderedImage get(final Satellite       satellite, 
                                    final RenderedImage   radiance, 
                                    final ParameterList   parameters,
                                    final Map             configuration) 
    {
        if (satellite == null)
            throw new IllegalArgumentException("Satellite is null.");        
        if (radiance == null)
            throw new IllegalArgumentException("Image is null.");        
        if (parameters == null)
            throw new IllegalArgumentException("Parameters is null.");        
        
        // Definition du type de l'image de sortie.
        int minX   = radiance.getMinX(),
            minY   = radiance.getMinY(),
            width  = radiance.getWidth(),
            height = radiance.getHeight();        
        final ImageLayout layout = new ImageLayout(minX, minY, width, height);
        final int numBands       = radiance.getSampleModel().getNumBands();
        layout.setSampleModel(RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, 
                                                                    width, 
                                                                    height,
                                                                    numBands));
        layout.setColorModel(PlanarImage.getDefaultColorModel(DataBuffer.TYPE_FLOAT,
                                                              numBands));              
        
        if (satellite.isKLM()) 
            return new TemperatureKLM(radiance, 
                                      layout, 
                                      parameters, 
                                      configuration);
        else
            return new TemperatureAJ(radiance, 
                                     layout, 
                                     parameters, 
                                     configuration);
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
    protected abstract void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect);        
    
    /**
     * Convert from degree to kelvin
     *
     * @param degree Temperature en degre.
     * @return temperature in kelvin.
     */
    public static double degreeToKelvin(final double degree) 
    {
        return (273.15 + degree);
    }
    
    /**
     * Convert from kelvin to degree.
     *
     * @param kelvin Temperature en kelvin.
     * @return temperature in degree.
     */
    public static double kelvinToDegree(final double kelvin) 
    {
        return (273.15 + kelvin);
    }     
    
    /**
     * Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     * @param satellite   Le satellite.
     * @return Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     */
    public static ParameterList getInputParameterList(final Satellite satellite) 
    {        
        if (satellite.isKLM())
            return TemperatureKLM.getInputParameterList();
        else
            return TemperatureAJ.getInputParameterList();
    }           
}