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
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Map;

// Geotools
import org.geotools.cv.SampleDimension;
import org.geotools.cv.Category;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;

// JAI
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.util.Range;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

// SEAGIS
import fr.ird.n1b.image.sst.Utilities;

/**
 * Calcul le <i>SUP</i> de plusieurs images Sea Surface Temperature (S.S.T.). L'objectif de ce 
 * traitement est à partir de plusieurs images S.S.T. de construire une seule image 
 * contenant les informations les plus pertinentes issues des différentes S.S.T. disponibles.
 * <BR><BR>
 * 
 * Si l'on considère le pixel de coordonnées géographiques (x, y), il est tout à fait 
 * envisagable que plusieurs des images S.S.T. sources possèdes chacune une information 
 * pour ce pixel (nuage, terre, température, ...). Lors du <i>SUP</i>, le pixel de 
 * coordonnée (x, y) de l'image générée ne gardera que l'information la plus pertinente 
 * parmis toutes celles disponibles. <BR>
 * Il faut cependant noter que si nous avons le choix entre plusieurs valeurs de température, 
 * alors nous garderons la plus grande (le SUP des températures).<BR><BR>
 * 
 * Le tableau ci-dessous définie l'image générée en fonction de l'état des pixels sources : 
 * <BR><BR><IMG SRC="doc-files/sstsup.png"><BR><BR>
 *
 * Note : Dans ce traitement nous travaillons sur les images indxéées plutôt que sur les 
 * valeurs physiques (température) pour différencier plus facilement les différentes 
 * categories d'informations (CLOUD, NO_DATA, LAND, ...).
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SSTSup extends SourcelessOpImage
{  
    /** Systeme de coordonnées attendu pour les images sources. */
    private static final CoordinateSystem WGS84 = Utilities.WGS84;
    
    /** SampleDimension attendu dans les images sources. */
    private static final SampleDimension[] SAMPLE_SST_INDEXED = Utilities.SAMPLE_SST_INDEXED;
    
    /** Nom des catégories. */
    private static final String LAND_BACKGROUND = Utilities.LAND_BACKGROUND,
                                LAND_CONTOUR    = Utilities.LAND_CONTOUR,
                                CLOUD           = Utilities.CLOUD,
                                TEMPERATURE     = Utilities.TEMPERATURE,
                                NO_DATA         = Utilities.NO_DATA;        
    
    /** Tableau contenant les images sources. */
    private final GridCoverage[] gridCoverage;

    /** Intervalle contenant les valeurs indexées de la categorie NO_DATA. */
    private final Range rNoData;    
    
    /** Intervalle contenant les valeurs indexées de la categorie CLOUD. */
    private final Range rCloud;    

    /** Intervalle contenant les valeurs indexées de la categorie LAND. */
    private final Range rLand;    

    /** Intervalle contenant les valeurs indexées de la categorie TEMPERATURE. */
    private final Range rTemperature;    
    
    /** 
     * Construit un objet SSTSup.
     *
     * @param layout          Un ImageLayout décrivant l'image de destination.
     * @param sampleModel     Le sampleModel de sortie.
     * @param minX            MinX.
     * @param minY            MinY.
     * @param width           width.
     * @param height          Height.
     * @param gridCoverage    Les images source.
     * @param configuration   Configuration du traitement réalise par JAI.
     */
    private SSTSup(final ImageLayout        layout, 
                   final SampleModel        model, 
                   final int                minX,              
                   final int                minY, 
                   final int                width,             
                   final int                height, 
                   final GridCoverage[]     gridCoverage, 
                   final Map                configuration) 
    {        
        super(layout, configuration, model, minX, minY, width, height);                
        this.gridCoverage = gridCoverage;

        /* Extraction des catégories et des intervalles de valeur indéxés pour chacune 
           de ces catégories.*/
        final Category catCloud       = Utilities.getCategory(SAMPLE_SST_INDEXED, CLOUD),
                       catNoData      = Utilities.getCategory(SAMPLE_SST_INDEXED, NO_DATA),
                       catLandBg      = Utilities.getCategory(SAMPLE_SST_INDEXED, LAND_BACKGROUND),
                       catLandContour = Utilities.getCategory(SAMPLE_SST_INDEXED, LAND_CONTOUR),
                       catTemperature = Utilities.getCategory(SAMPLE_SST_INDEXED, TEMPERATURE);        
        
        if (catCloud == null)
            throw new IllegalArgumentException("Category \"" + 
                                               CLOUD + 
                                               "\" is not define.");
        if (catNoData == null)
            throw new IllegalArgumentException("Category \"" + 
                                               NO_DATA + 
                                               "\" is not define.");
        if (catLandBg == null)
            throw new IllegalArgumentException("Category \"" + 
                                               LAND_BACKGROUND + 
                                               "\" is not define.");
        if (catLandContour == null)
            throw new IllegalArgumentException("Category \"" + 
                                               LAND_CONTOUR + 
                                               "\" is not define.");
        if (catTemperature == null)
            throw new IllegalArgumentException("Category \"" + 
                                                TEMPERATURE + 
                                                "\" is not define.");        
        rNoData = catNoData.getRange();
        rCloud  = catCloud.getRange();
        rLand   = catLandBg.getRange().union(catLandContour.getRange());
        rTemperature = catTemperature.getRange();
    }            
    
    /**
     * Retourne un <CODE>GridCoverage</CODE> contenant l'image résulatnt du <i>SUP</i> des images 
     * Sea Surafce Temperature (S.S.T.) sources. Le système de coordonnées attendue ainsi
     * que les catégories sont défnies dans la classe <CODE>Utilities</CODE>.
     *
     * @param source            Les gridCoverages contenant les images sources. 
     * @param bound             Taille de l'image de sortie. Si <CODE>bound</CODE> vaut 
     *                          <i>null</i>, l'image générée couvre une zone égale à l'union 
     *                          des zones couvertes par les images sources. Sinon la zone
     *                          couverte est <CODE>bound</CODE>.
     * @param configuration     Configuration du traitement réalisé par JAI.
     * @return un <CODE>GridCoverage</CODE> contenant l'image résulatnt du <i>SUP</i> des images 
     *         Sea Surafce Temperature (S.S.T.) sources. Le système de coordonnées attendue ainsi
     *         que les catégories sont défnies dans la classe <CODE>Utilities</CODE>.
     */
    public static GridCoverage get(final GridCoverage[]     source,                                   
                                   Rectangle                bound,                                   
                                   final Map                configuration)
    {
        if (source == null)
            throw new IllegalArgumentException("Source is null.");          
        for (int i=0 ; i<source.length ; i++)                        
            if (source[i] == null)                
                throw new IllegalArgumentException("Source at index " + i + " is null.");        
                        
        /* Vérification du système de coordonnées des sources. */
        for (int i=1 ; i<source.length ; i++)                        
            if (!source[i].getCoordinateSystem().equals(WGS84))
                throw new IllegalArgumentException("The set of source is not in the " + 
                                                   "same coordinate System as expected.");                                                           
                
        /* Détermine la couverture du GridCoverage généré. */
        int xmin = 0, 
            xmax = 0, 
            ymin = 0, 
            ymax = 0;
        
        if (bound == null)
        {
            // Couverture la plus grande possible.
            for (int i= 0 ; i<source.length ; i++)
            {
                final Rectangle2D envelope = source[i].getEnvelope().toRectangle2D();
                final RenderedImage image = source[i].getRenderedImage();            
                if (i == 0)
                {                
                    bound = new Rectangle(image.getMinX(),
                                          image.getMinY(),
                                          image.getWidth(),
                                          image.getHeight());
                    xmin = (int)envelope.getMinX();
                    xmax = (int)envelope.getMaxX();
                    ymin = (int)envelope.getMinY();
                    ymax = (int)envelope.getMaxY();
                }
                else
                {
                    final Rectangle bound_ = new Rectangle(image.getMinX(),
                                                           image.getMinY(),
                                                           image.getWidth(),
                                                           image.getHeight());
                    bound = bound.union(bound_);                
                    xmin = (int)Math.min(envelope.getMinX(), xmin);
                    xmax = (int)Math.max(envelope.getMaxX(), xmax);
                    ymin = (int)Math.min(envelope.getMinY(), ymin);
                    ymax = (int)Math.max(envelope.getMaxY(), ymax);
                }
            }
        }
        else
        {
            // On couvre seulement la zone <CODE>bound</CODE>.
            xmin = (int)bound.getMinX();
            ymin = -1*(int)(bound.getMinY() + bound.getHeight());            
            xmax = xmin + (int)(bound.getWidth());
            ymax = -1*(int)(bound.getMinY());
        }
        final ImageLayout layout = new ImageLayout((int)bound.getX(),
                                                   (int)bound.getY(),
                                                   (int)(bound.getWidth()),
                                                   (int)(bound.getHeight()));
        final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_BYTE, 
                                                                              (int)bound.getWidth(), 
                                                                              (int)bound.getHeight(),
                                                                              1);
        layout.setSampleModel(sampleModel);
        layout.setColorModel(source[0].getRenderedImage().getColorModel());
        final RenderedImage synthese = new SSTSup(layout, 
                                                  sampleModel, 
                                                  (int)bound.getX(),
                                                  (int)bound.getY(),
                                                  (int)bound.getWidth(),
                                                  (int)bound.getHeight(), 
                                                  source,
                                                  configuration);

        final double[] minCP = {xmin, ymin},
                       maxCP = {xmax, ymax};
        final Envelope envelope = new Envelope(minCP, maxCP);  
        return new GridCoverage("Sup", 
                                synthese,         
                                WGS84,                                     
                                envelope,
                                SAMPLE_SST_INDEXED,
                                (GridCoverage[])null,
                                (Map)null);             
    } 
    
    /**
     * compute. 
     *
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    protected void computeRect(final PlanarImage[]   sources, 
                               final WritableRaster  dest, 
                               final Rectangle       destRect)     
    {            
        // Par defaut, l'image générée a pour valeur NoData. 
        final double pNoData = ((Integer)rNoData.getMinValue()).intValue();
        WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);

        iTarget.startBands();        
        while (!iTarget.finishedBands())
        {
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                iTarget.startPixels();                
                while (!iTarget.finishedPixels())
                {
                    iTarget.setSample(pNoData);   
                    iTarget.nextPixel();
                }        
                iTarget.nextLine();
            }
            iTarget.nextBand();            
        }
    
        /* On verifie les intersections entre les images sources et la zone en cours de 
           calcul. Si une intersection existe, alors on travail sur la zone d'intersection 
           pour réaliser le SUP. */
        for (int i=0 ; i<gridCoverage.length ; i++)
        {
            final RenderedImage imageSrc = gridCoverage[i].getRenderedImage();
            final Rectangle srcRect      = new Rectangle(imageSrc.getMinX(),
                                                         imageSrc.getMinY(),
                                                         imageSrc.getWidth(),
                                                         imageSrc.getHeight());                                                      
            /* Calcul de l'intersection de la zone en cours de calcul avec le gridCoverage 
               en cours. */
            if (!destRect.intersects(srcRect))
                continue;                
            
            /* Sup de la région commune. */            
            final Rectangle destRect_ = destRect.intersection(srcRect);            
            iTarget             = RectIterFactory.createWritable(dest ,destRect_);
            final RectIter iSrc = RectIterFactory.create(imageSrc,destRect_);            
            
            iTarget.startBands();        
            iSrc.startBands();        
            while (!iTarget.finishedBands())
            {
                iTarget.startLines();
                iSrc.startLines();
                while (!iTarget.finishedLines())
                {
                    iTarget.startPixels();                
                    iSrc.startPixels();                 
                    while (!iTarget.finishedPixels())
                    {   
                        if (!iSrc.finishedPixels() && !iSrc.finishedLines())
                        {
                            final Integer src = new Integer(iSrc.getSample()),
                                          tgt = new Integer(iTarget.getSample()); 

                            /* Si le pixel de l'image source est un pixel NO_DATA, il ne 
                               contient aucune information, on passe au pixel suivant. */
                            if (!rNoData.contains(src))
                            {
                                /* Si le pixel destination est LAND on passe au pixel 
                                   suivant. */
                                if (!rLand.contains(tgt))
                                {
                                    if (rNoData.contains(tgt))
                                    {
                                        /* Le pixel destination est un pixel NO_DATA, le 
                                           pixel source est toujours affectee. */ 
                                        iTarget.setSample(src.intValue());                                    
                                    } else if (rLand.contains(src))
                                    {
                                        /* Le pixel source est un pixel LAND, le pixel 
                                           source est toujours affectée. */ 
                                        iTarget.setSample(src.intValue());
                                    } else if (rTemperature.contains(src))
                                    {
                                        if (rCloud.contains(tgt))
                                        {
                                            iTarget.setSample(src.intValue());
                                        } else if (rTemperature.contains(tgt))
                                        {
                                            iTarget.setSample(Math.max(src.intValue(), 
                                                                       tgt.intValue()));                                        
                                        }                                    
                                    }
                                }                            
                            }
                        }
                        iSrc.nextPixel();        
                        iTarget.nextPixel();
                    }        
                    iTarget.nextLine();
                    iSrc.nextLine();        
                }
                iTarget.nextBand();
                iSrc.nextBand();        
            }
        }
    }    
}