/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.Date;

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

// GEOTOOLS
import org.geotools.pt.Envelope;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cs.AxisInfo;
import org.geotools.cs.AxisOrientation;
import org.geotools.cs.FittedCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransformFactory;
import org.geotools.ct.TransformException;
import org.geotools.science.astro.SunRelativePosition;

/**
 * Calcul l'angle d'élévation du soleil de chacun des pixels par rapport à leur position 
 * géographique et de leur date d'acquisition par le satellite. Pour ce calcul, la classe 
 * <CODE>SunRelativePosition</CODE> est utilisée.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class SolarElevation extends SourcelessOpImage
{   
    /** 
     * Objet capable de calculer l'élévation du soleil en fonction d'une date et d'une 
     * position géographique. 
     */
    private final SunRelativePosition solarPosition;           
    
    /** Transformation du système de coordonnées de l'image au système géographique. */
    private final MathTransform transform;
    
    /** La grille de localisation. */ 
    private final LocalizationGridN1B grid;
   
    /** 
     * Construit un objet SolarElevation.
     *
     * @param layout          Un ImageLayout décrivant l'image de destination.
     * @param sampleModel     Le sampleModel de sortie.
     * @param minX            MinX.
     * @param minY            MinY.
     * @param width           width.
     * @param height          Height.
     * @param transform       Transformation du système de coordonnées de l'image vers le système 
     *                        géographique.
     * @param localization    La grille de localization des pixels.
     * @param configuration   Configuration du traitement realise par JAI.
     * @return une instance de SolarElevation. 
     */
    private SolarElevation(final ImageLayout          layout, 
                           final SampleModel          model, 
                           final int                  minX, 
                           final int                  minY, 
                           final int                  width, 
                           final int                  height, 
                           final MathTransform        transform,
                           final LocalizationGridN1B  grid, 
                           final Map                  configuration) 
    {        
        super(layout, configuration, model, minX, minY, width, height);                
        this.transform = transform;
        this.grid      = grid;
        this.solarPosition = new SunRelativePosition();
        solarPosition.setTwilight(Double.NaN);
    }        
    
    /**
     * Retourne une image contenant l'angle d'élévation du soleil par rapport à la position
     * géographique de chacun des pixels et à leur date d'acquisition. <CODE>Bound</CODE>
     * définie la zone de l'image à calculer.
     *
     * @param grid              La grille de localization des pixels.
     * @param bound             Limite de l'image de sortie.
     * @param configuration     Configuration du traitement realise par JAI.
     * @return une image contenant l'angle d'élévation du soleil par rapport à la position
     * géographique de chacun des pixels et à leur date d'acquisition. 
     */
    public static RenderedImage get(final LocalizationGridN1B grid,
                                    final Rectangle           bound,
                                    final Map                 configuration) 
    {        
        if (grid == null)
            throw new IllegalArgumentException("Grid of localization is null");
        if (bound == null)
            throw new IllegalArgumentException("Bound is null.");
        
        // Type de l'image de sortie. 
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
        final AxisInfo[] AXIS    = {new AxisInfo("colonne", AxisOrientation.OTHER),
                                    new AxisInfo("ligne"  , AxisOrientation.OTHER)};                                                                                                             
        final Envelope enveloppe =  new Envelope(new Rectangle2D.Double(bound.getMinX(),
                                                                        bound.getMinY(),
                                                                        bound.getWidth(),
                                                                        bound.getHeight()));
        
        try
        {
            final AffineTransform at = new AffineTransform(ImageReaderN1B.INTERVAL_NEXT_CONTROL_POINT, 
                                                           0, 
                                                           0, 
                                                           1, 
                                                           ImageReaderN1B.OFFSET_FIRST_CONTROL_POINT-1, 
                                                           0);        
            final MathTransform gridToGeo = grid.getMathTransform();                
            final MathTransform imToGrid  = MathTransformFactory.getDefault().createAffineTransform(at).inverse();                            
            final MathTransform imToGeo   = MathTransformFactory.getDefault().createConcatenatedTransform(imToGrid, 
                                                                                                          gridToGeo);        
            final FittedCoordinateSystem cs = new FittedCoordinateSystem("N1B",   
                                                                         GeographicCoordinateSystem.WGS84, 
                                                                         imToGeo, 
                                                                         AXIS);                        

            // Construction du gridCoverage a retourner. 
            return new SolarElevation(layout, 
                                      sampleModel, 
                                      (int)bound.getX(),
                                      (int)bound.getY(),
                                      (int)bound.getWidth(),
                                      (int)bound.getHeight(), 
                                      imToGeo,
                                      grid,           
                                      configuration);
        }
        catch (org.geotools.ct.NoninvertibleTransformException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
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
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);                
        final CoordinatePoint target   = new CoordinatePoint(0.0, 0.0);
        final CoordinatePoint source   = new CoordinatePoint(0.0, 0.0);        
        final double[] ordSrc = source.ord;
        final double[] ordTgt = target.ord;
                
        iTarget.startBands();        
        while (!iTarget.finishedBands())
        {
            int row = (int)destRect.getY();
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                // Date d'acquisition de la ligne.
                solarPosition.setDate(grid.getTime(row));
                
                int col = (int)destRect.getX();            
                iTarget.startPixels();                     
                while (!iTarget.finishedPixels())
                {
                    ordSrc[0] = col;
                    ordSrc[1] = row;
                    try 
                    {
                        // Calcul des coordonnées géographique du pixel. 
                        transform.transform(source, target);
                    }
                    catch (TransformException te)
                    {
                        throw new IllegalArgumentException(te.getMessage());
                    }                    
                    solarPosition.setCoordinate(ordTgt[0], ordTgt[1]);
                    iTarget.setSample(solarPosition.getElevation());                    
                    iTarget.nextPixel();                    
                    col ++;
                }        
                iTarget.nextLine();
                row++;
            }
            iTarget.nextBand();
        }
    }    
}