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
import fr.ird.image.sst.n1b.Utilities;

/**
 * Calcul la synthèse de plusieurs images Sea Surface Temperature (S.S.T.). L'objectif de 
 * la synthèse est de compléter les informations manquantes (COUD, NODATA) de la première 
 * image S.S.T. source à partir des images S.S.T. suivantes. <BR>
 * En effet, la première image source contient un certain nombre d'informations pertinentes 
 * de température mais elle contient aussi beaucoup de pixel moins informés comme les pixels
 * nuageux ou sans informations. Le but sera pour chaque pixel de la première image S.S.T.
 * qui n'est pas un pixel de température ou un pixel de terre à trouver un pixel contenant 
 * de l'information dans les autres images S.S.T. en prenant les images S.S.T. en respectant
 * l'ordre dans lequel elles ont été passées en argument. Le premier pixel trouvé est affecté.
 * <BR><BR>
 *
 * Le tableau ci-dessous définit l'image générée en fonction de l'état des pixels des images 
 * sources : <BR>
 * <IMG SRC="doc-files/sstsynthese.png"><BR><BR>
 *
 * Note : Dans ce traitement on travail sur les valeurs indéxées de l'image plutôt que sur 
 * les valeurs physiques (température) pour faciliter la différenciation entre les catégories. 
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SSTSynthese extends SourcelessOpImage
{  
    /** Système de coordonnées attendu dans les images sources. */
    private static final CoordinateSystem WGS84 = Utilities.WGS84;
    
    /** SampleDimension attendu dans les images sources. */
    private static final SampleDimension[] SAMPLE_SST_INDEXED = Utilities.SAMPLE_SST_INDEXED;
    
    /** Nom des catégories. */
    private static final String LAND_BACKGROUND = Utilities.LAND_BACKGROUND,
                                LAND_CONTOUR    = Utilities.LAND_CONTOUR,
                                CLOUD           = Utilities.CLOUD,
                                TEMPERATURE     = Utilities.TEMPERATURE,
                                NO_DATA         = Utilities.NO_DATA;
        
    
    /** GridCoverage des images sources. */
    private final GridCoverage[] gridCoverage;

    /** Intervalle des valeurs indexées de la categorie NO_DATA. */
    private final Range rNoData;    
    
    /** Intervalle des valeurs indexées de la categorie CLOUD. */
    private final Range rCloud;    

    /** Intervalle des valeurs indexées de la categorie LAND. */
    private final Range rLand;    

    
    /** 
     * Construit un objet SSTSynthese.
     *
     * @param layout          Un ImageLayout décrivant l'image de destination.
     * @param sampleModel     Le sampleModel de sortie.
     * @param minX            MinX.
     * @param minY            MinY.
     * @param width           width.
     * @param height          Height.
     * @param gridCoverage    Les images source.
     * @param configuration   Configuration du traitement réalisé par JAI.
     */
    private SSTSynthese(final ImageLayout        layout, 
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

        /* Extraction des catégories et des intervalles de valeur indexés pour chacune 
           des catégories. */
        final Category catCloud       = Utilities.getCategory(SAMPLE_SST_INDEXED, CLOUD),
                       catNoData      = Utilities.getCategory(SAMPLE_SST_INDEXED, NO_DATA),
                       catLandBg      = Utilities.getCategory(SAMPLE_SST_INDEXED, LAND_BACKGROUND),
                       catLandContour = Utilities.getCategory(SAMPLE_SST_INDEXED, LAND_CONTOUR);        
        
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
        rNoData = catNoData.getRange();
        rCloud  = catCloud.getRange();
        rLand   = catLandBg.getRange().union(catLandContour.getRange());
    }            
    
    /**
     * Retourne un <CODE>GridCoverage</CODE> contenant une image de synthèse des images 
     * S.S.T. sources. Cette synthèse vise a compléter la premiere image S.S.T. source 
     * (contenant des pixels nuageux et non informés) avec les S.S.T. sources suivantes.
     * Le système de coordonnées attendue ainsi que les catégories sont défnies dans la 
     * classe <CODE>Utilities</CODE>.
     *
     * @param source            Les gridCoverages contenant les images S.S.T. sources. 
     * @param bound             Taille de l'image de sortie. Si <CODE>bound</CODE> vaut 
     *                          <i>null</i>, l'image générée couvre une zone égale à l'union 
     *                          des zones couvertes par les images sources. Sinon la zone
     *                          couverte est <CODE>bound</CODE>.
     * @param configuration     Configuration du traitement réalisé par JAI.
     * @return un <CODE>GridCoverage</CODE> contenant une image de synthèse des images 
     *         S.S.T. sources. Cette synthèse vise a compléter la premiere image S.S.T. source 
     *         (contenant des pixels nuageux et non informés) avec les S.S.T. sources suivantes.
     *         Le système de coordonnées attendue ainsi que les catégories sont défnies dans la 
     *         classe <CODE>Utilities</CODE>.
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
                        
        /* Vérification du système de coordonnées des images S.S.T. sources. */
        for (int i=1 ; i<source.length ; i++)                        
            if (!source[i].getCoordinateSystem().equals(WGS84))
                throw new IllegalArgumentException("The set of source is not in the same" +
                                                   " coordinate System as expected.");                                                           
        
        /* Calcul de la couverture de l'image générée. */
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
        
        final RenderedImage synthese = new SSTSynthese(layout, 
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
        return new GridCoverage("Synthese", 
                                synthese,         
                                WGS84,                                     
                                envelope,
                                SAMPLE_SST_INDEXED,
                                (GridCoverage[])null,
                                (Map)null);             
    } 
    
    /**
     * Calcul.
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
           pour réaliser la synthèse. */
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
            
            /* Synthèse de la région commune. */                    
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
                            final Integer src = new Integer(iSrc.getSample());    

                            /* Si le pixel de l'image source est un pixel NO_DATA, il ne 
                               contient aucune information, on passe au pixel suivant. */
                             if (!rNoData.contains(src))
                             {
                                final Integer tgt = new Integer(iTarget.getSample());                           

                                if (rNoData.contains(tgt) || rCloud.contains(tgt) || rLand.contains(src))
                                {
                                    /* Le pixel de l'image destination est un pixel CLOUD ou 
                                       NO_DATA, alors l'information de source qui un pixel LAND, 
                                       NO_DATA ou TEMPERATURE est plus pertinente, on remplace 
                                       le pixel destination. */
                                    iTarget.setSample(src.intValue());                                    
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