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
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.geom.AffineTransform;
import java.awt.Rectangle;
import java.util.Map;

// Geotools
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransformFactory;
import org.geotools.pt.CoordinatePoint;

// JAI
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterFactory;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// SEAGIS
import fr.ird.n1b.io.LocalizationGridN1B;
import fr.ird.n1b.io.ImageReaderN1B;
import fr.ird.science.astro.SatelliteRelativeAngle;

/**
 * Calcul une image contenant l'angle de visé du satellite/capteur pour chacun des pixels 
 * lors de leur acquisition. Pour calculer l'angle de visé par rapport à une <i>position 
 * géographique</i> et une <i>date</i>, on utilise la classe <CODE>SatelliteRelativeAngle</CODE>.
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SensorAngle extends SourcelessOpImage
{
    /** 
     * Un objet capable de calculer l'angle de visé du capteur/satellite lors de 
     * l'acquisition des pixels.
     */
    private final SatelliteRelativeAngle compute;
    
    /** La transformée du système de coordonnée de l'image vers le système géographique. */
    private final MathTransform transform;

    /** La grille de localisation des pixels. */
    private final LocalizationGridN1B grid;
    
    /** 
     * Construit un objet SensorAngle.
     *
     * @param layout          Un ImageLayout décrivant l'image de destination.
     * @param sampleModel     Le sampleModel de sortie.
     * @param minX            MinX.
     * @param minY            MinY.
     * @param width           width.
     * @param height          Height.
     * @param grid            La grille de localisation des pixels.
     * @param transform       La transformée du système de l'image vers le système géographique.
     * @param configuration   Configuration du traitement réalisé par JAI.
     * @return une instance de SensorAngle. 
     */
    private SensorAngle(final ImageLayout               layout, 
                        final SampleModel               model, 
                        final int                       minX, 
                        final int                       minY, 
                        final int                       width, 
                        final int                       height, 
                        final LocalizationGridN1B       grid, 
                        final MathTransform             transform, 
                        final Map                       configuration) 
    {        
        super(layout, configuration, model, minX, minY, width, height);                
        this.grid      = grid;
        this.transform = transform;             
        this.compute   = new SatelliteRelativeAngle();
    }            
    
    /**
     * Retourne une image contenant l'angle de visé du capteur/satellite pour chacun des 
     * pixels de l'image N1B. <CODE>bound</CODE> définie la zone de l'image à calculer.
     *
     * @param grid              La grille de localisation des pixels.
     * @param transform         Transformation du système de coordonnées de l'image vers 
     *                          le système géographique.
     * @param configuration     Configuration du traitement réalisé par JAI.
     * @return une image contenant l'angle de visé du capteur/satellite pour chacun des 
     *         pixels de l'image N1B. <CODE>bound</CODE> définie la zone de l'image à calculer.
     */
    public static RenderedImage get(final LocalizationGridN1B    grid,
                                    final MathTransform          transform,
                                    final Rectangle              bound,
                                    final Map                    configuration)
    {
        if (grid == null)
            throw new IllegalArgumentException("Grid is null.");        
        if (bound == null)
            throw new IllegalArgumentException("Bound is null.");        
        
        // Définition du type de l'image de sortie. 
        final ImageLayout layout = new ImageLayout((int)bound.getX(),
                                                   (int)bound.getY(),
                                                   (int)bound.getWidth(),
                                                   (int)bound.getHeight());
        final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, 
                                                                              (int)bound.getWidth(), 
                                                                              (int)bound.getHeight(),
                                                                              1);
        layout.setSampleModel(sampleModel);
        layout.setColorModel(PlanarImage.getDefaultColorModel(DataBuffer.TYPE_FLOAT,1));                        
        return new SensorAngle(layout, 
                               sampleModel, 
                               (int)bound.getX(),
                               (int)bound.getY(),
                               (int)bound.getWidth(),
                               (int)bound.getHeight(), 
                               grid,
                               transform,
                               configuration);
    } 
    
    /**
     * Calcul.
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
                            final Rectangle destRect) 
    {        
        try
        {
            final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);        
            // coordonnées X du nadir de l'image dans le système de coordonnées de l'image.            
            // 0-1023 | 1023-2047
            final double xNadir = ImageReaderN1B.NB_PIXEL_LINE/2-0.5;     

            iTarget.startBands();                
            while (!iTarget.finishedBands())
            {
                int row = (int)destRect.getY();
                iTarget.startLines();
                while (!iTarget.finishedLines())
                {
                    // Position du pixel au nadir dans le système géographique.
                    final CoordinatePoint sat = new CoordinatePoint(xNadir, row);                
                    transform.transform(sat, sat);
                    final double altitude = grid.getAltitude(row)*1000.0;

                    int col = (int)destRect.getX();            
                    iTarget.startPixels();                     
                    while (!iTarget.finishedPixels())
                    {
                        // Coordonnées du pixel dans le système géographique.
                        final CoordinatePoint pt = new CoordinatePoint(col, row);                
                        transform.transform(pt, pt);                                                              
                        try
                        {
                            compute.compute(sat, altitude, pt);
                            final double angle = compute.getSensorAngle();
                            iTarget.setSample(angle);                                               
                        }
                        catch (Exception e)
                        {
                            throw new IllegalArgumentException(e.getMessage());
                        }
                        iTarget.nextPixel();                    
                        col ++;                        
                    }        
                    iTarget.nextLine();
                    row++;
                }
                iTarget.nextBand();
            }
        }
        catch (TransformException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }        
}