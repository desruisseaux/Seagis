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
 * Calcul le pourcentage d'albedo à partir des valeurs <i>count</i> des canaux du 
 * visibles du capteur AVHRR. 
 *
 * @author  Remi EVE
 * @version $Id$
 */
public abstract class Albedo extends PointOpImage 
{  
    /** 
     * Construit un objet Albedo.
     *
     * @param image           Une image contenant des donnees brutes issues d'un canal 
     *                        du visible.
     * @param layout          Definition du type de l'image de sortie.
     * @param configuration   Configuration du traitement realise par JAI.
     */
    protected Albedo(final RenderedImage        image, 
                     final ImageLayout          layout,
                     final Map                  configuration) 
    {
        super(image, layout, configuration, false);                
        permitInPlaceOperation();
    }
        
    /**
     * Retourne une image contenant l'albedo. 
     *
     * @param satellite       Le satellite ayant permit l'acquisition de ces donnees.
     * @param raw             Une image contenant les donnees brutes issues d'un canal 
     *                        du visible.
     * @param parameters      Parametres du calcul.
     * @param configuration   Configuration du traitement realise par JAI.
     * @return une image contenant l'albedo.
     */
    public static RenderedImage get(final Satellite          satellite, 
                                    final RenderedImage      raw, 
                                    final ParameterList      parameters,
                                    final Map                configuration) 
    {
        if (satellite == null)
            throw new IllegalArgumentException("Satellite is null.");        
        if (raw == null)
            throw new IllegalArgumentException("Image is null.");        
        if (parameters == null)
            throw new IllegalArgumentException("Parameters is null.");
        
        // Definition du type de l'image de sortie.
        int minX   = raw.getMinX(),
            minY   = raw.getMinY(),
            width  = raw.getWidth(),
            height = raw.getHeight();
        final ImageLayout layout = new ImageLayout(minX, minY, width, height);
        final int numBands       = raw.getSampleModel().getNumBands();
        layout.setSampleModel(RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, 
                                                                    width, 
                                                                    height,
                                                                    numBands));
        layout.setColorModel(PlanarImage.getDefaultColorModel(DataBuffer.TYPE_FLOAT,
                                                              numBands));        
        
        // Image à retourner.
        if (satellite.isKLM()) 
            return new AlbedoKLM(raw, 
                                 layout, 
                                 parameters,
                                 configuration);
        else
            return new AlbedoAJ(raw, 
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
     * Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     * @return Retourne la liste des paramètres nécessaires au calcul de l'albedo.
     */
    public static ParameterList getInputParameterList() 
    {        
        return null;
    }        
}