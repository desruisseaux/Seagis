/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.n1b.op;

// J2SE
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.geom.AffineTransform;
import java.awt.Rectangle;
import java.util.Map;

// Geotools
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransformFactory;
import org.geotools.ct.TransformException;
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
 * Calcul pour chaque pixel l'angle z�nital du satellite. Pour calculer l'angle 
 * zenithal par rapport � une position g�ographique et une date, on fait appel � la classe 
 * <a href="SatelliteRelativeZenithAngle.html">SatelliteRelativeZenithAngle</a>.
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SatelliteZenithAngle extends SourcelessOpImage
{
    /** 
     * Un object capable de calculer l'angle z�nithal du satellite en fonction d'une 
     * position g�ographique et d'une date.
     */
    private final SatelliteRelativeAngle compute;
    
    /** La transform�e du syst�me de coordonn�es de l'image vers le syst�me g�ographique. */
    private final MathTransform transform;

    /** La grille de localisation des pixels. */
    private final LocalizationGridN1B grid;
    
    /** 
     * Construit un objet SatelliteZenithAngle.
     *
     * @param layout          Un ImageLayout decrivant l'image de destination.
     * @param sampleModel     Le sampleModel de sortie.
     * @param minX            MinX.
     * @param minY            MinY.
     * @param width           width.
     * @param height          Height.
     * @param grid            La grille de localisation des pixels.
     * @param transform       La transform�e du syst�me de l'image vers le syst�me g�ographique.
     * @param configuration   Configuration du traitement realise par JAI.
     * @return une instance de SatelliteZenithAngle. 
     */
    private SatelliteZenithAngle(final ImageLayout              layout, 
                                 final SampleModel              model, 
                                 final int                      minX,  
                                 final int                      minY, 
                                 final int                      width, 
                                 final int                      height, 
                                 final LocalizationGridN1B      grid,
                                 final MathTransform            transform, 
                                 final Map                      configuration) 
    {        
        super(layout, configuration, model, minX, minY, width, height);                
        this.grid      = grid;
        this.transform = transform;
        this.compute   = new SatelliteRelativeAngle();
    }            
    
    /**
     * Retourne une image contenant l'angle z�nithal du satellite en degr� de chacun des 
     * pixels appartenant � <CODE>bound</CODE>. 
     *
     * @param grid              La grille de localisation de l'image.
     * @param transform         Transformation du syst�me de coordonn�es de l'image vers 
     *                          le syst�me g�ographique.
     * @param bound             Limite de l'image de sortie.
     * @param configuration     Configuration du traitement realise par JAI.
     * @return une image contenant l'angle d'acquisition des pixels depuis le satellite. 
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
        
        // D�finition du type de l'image de sortie. 
        final ImageLayout layout = new ImageLayout((int)bound.getX(),
                                                   (int)bound.getY(),
                                                   (int)bound.getWidth(),
                                                   (int)bound.getHeight());
        final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, 
                                                                              (int)bound.getWidth(), 
                                                                              (int)bound.getHeight(),
                                                                              1);
        layout.setSampleModel(sampleModel);
        layout.setColorModel(PlanarImage.getDefaultColorModel(DataBuffer.TYPE_FLOAT, 1));
        return new SatelliteZenithAngle(layout, 
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
            
            // Coordonnees X du nadir de l'image dans le syst�me de l'image.
            final double xNadir = ImageReaderN1B.NB_PIXEL_LINE/2-0.5;     

            iTarget.startBands();                
            while (!iTarget.finishedBands())
            {
                int row = (int)destRect.getY();
                iTarget.startLines();
                while (!iTarget.finishedLines())
                {
                    // Calcul de la position du pixel au nadir dans le syst�me g�ographique.
                    final CoordinatePoint sat = new CoordinatePoint(xNadir, row);                
                    transform.transform(sat, sat);
                    final double altitude = grid.getAltitude(row)*1000.0;
                    
                    int col = (int)destRect.getX();            
                    iTarget.startPixels();                     
                    while (!iTarget.finishedPixels())
                    {
                        // Coordonn�es du pixel dans le syst�me g�ographique.
                        final CoordinatePoint pt = new CoordinatePoint(col, row);                
                        transform.transform(pt, pt);       
                        try
                        {
                            compute.compute(sat, altitude, pt);
                        }
                        catch (Exception e)
                        {
                            System.err.println(e);
                        }
                        final double angle = compute.getZenithalAngle();
                        iTarget.setSample(angle);                   
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