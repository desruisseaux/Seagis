/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D?veloppement
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
 *          Maison de la t?l?d?tection
 *          Institut de Recherche pour le d?veloppement
 *          500 rue Jean-Fran?ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.n1b.image.sst;

// SEAGIS.
import fr.ird.n1b.io.Bulletin;
import fr.ird.n1b.io.Metadata;
import fr.ird.n1b.io.Satellite;
import fr.ird.util.ThresoldRange;
import fr.ird.util.GridStatistics;
import fr.ird.op.PatternFilter;
import fr.ird.io.text.ParseSatellite;
import fr.ird.n1b.io.ImageReaderN1B;
import fr.ird.n1b.image.sst.Utilities;
import fr.ird.n1b.io.LocalizationGridN1B;
import fr.ird.science.astro.SatelliteRelativeAngle;
import fr.ird.io.text.Parse;
import fr.ird.io.text.ParseSST;
import fr.ird.n1b.image.Filter;
import fr.ird.n1b.image.FilterSST;
import fr.ird.n1b.image.FilterRange;
import fr.ird.n1b.image.FilterLatitudinal;
import fr.ird.n1b.op.Mask;
import fr.ird.n1b.op.Radiance;
import fr.ird.n1b.op.Temperature;
import fr.ird.n1b.op.SuperposeMask;
import fr.ird.n1b.op.SolarElevation;
import fr.ird.n1b.op.MatrixDayNight;
import fr.ird.n1b.op.SensorAngle;
import fr.ird.n1b.op.SatelliteZenithAngle;
import fr.ird.n1b.op.AngleExclusion;

// J2SE / JAI.
import java.awt.image.renderable.ParameterBlock;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.TimeZone;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.image.Raster;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import javax.media.jai.JAI;
import javax.imageio.ImageIO;
import javax.media.jai.TileCache;
import javax.media.jai.util.Range;
import javax.media.jai.PlanarImage;
import javax.imageio.ImageReadParam;
import javax.media.jai.ParameterList;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.KernelJAI;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

// GEOTOOLS.
import org.geotools.gc.GridCoverage;
import org.geotools.gc.GridGeometry;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.cs.FittedCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.Category;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.Envelope;
import org.geotools.renderer.geom.Isoline;
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransform1D;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.MathTransformFactory;
import org.geotools.cs.AxisInfo;
import org.geotools.cs.AxisOrientation;
import org.geotools.renderer.j2d.Renderer;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedIsoline;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.renderer.j2d.Hints;

/**
 * Calcul une image Sea Surface Temperature (S.S.T.) à partir d'une image N1B.<BR><BR>
 *
 * La suite de traitement de cette classe produit à partir d'un fichier N1B :
 * <UL>
 *  <LI>Image S.S.T. dans le système géographique avec une résolution de 1/100°.</LI>
 *  <LI>Fichier <i>header</i> contenant des informations de localisation de l'image.<LI>
 *  <LI>Masque contenant l'état des pixels lors de leur acquisition. Ce masque indique les 
 *      pixels acquis de jour ou de nuit.</LI>
 *  <LI>Fichier de statistique de température des pixels par latitude. <LI>
 * </UL><BR><BR>
 *
 * Suite de traitements appliqués au cours du calcul(résumé) :<BR>
 * <UL>
 *  <LI>Calcul de la température de brillance des canaux 4 et 5.</LI>
 *  <LI>Calcul de la matrice jour/nuit (identifie les pixels acquis de jour, de nuit ou 
 *      dans une période transitoire jour/nuit).</LI>
 *  <LI>Calcul de la SST.</LI>
 *  <LI>Calcul des masques nuages et superposition à la S.S.T..</LI>
 *  <LI>Exclusion des pixels trop éloignés du nadir.</LI>
 *  <LI>Projection dans le système géographique.</LI>
 *  <LI>Superposition du masque Terre.</LI> 
 * </UL><BR><BR>
 *   
 * // TODO :<BR>
 * <UL>
 *  <LI> Dans ce calcul qui fait intervenir de nombreuses images, nous travaillons avec 
 *       des images en tuiles pour minimiser la mémoire utilisée. Cependant, la classe 
 *       <CODE>ImageReader</CODE> permettant de lire les fichiers N1B extrait l'image 
 *       d'un bloc. Il serait intéressant d'utiliser la méthode 
 *       <CODE>JAI.create("ImageRead", block);</CODE> qui extrait par tuile dans un 
 *       fichier. Cela permettrait d'optimiser la mémoire. <BR>
 *       De même, il serait intéréssant d'écrire dans un fichier en utilisant le principe 
 *       des tuiles.</LI>
 * <LI>  Pour superposer le masque terre sur l'image SST, nous utilisons la méthode 
 *       <CODE>createGrphics()</CODE> Hors cette méthode fait appel au serveur X sous 
 *       linux. Il est alors impossible de calculer la SST sur un terminal  X. Il faudrait 
 *       implémenter cette partie différement pour permettre à la chaîne de fonctionner 
 *       sous terminal X.</LI>
 * </UL>
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SST 
{   
    /** Constante définissant les paramètres accessibles. */
    public static final String SLOPE_INTERCEPT_COEFFICIENT = "Slope, intercept coefficients",
                               THERMAL_COEFFICIENT         = "Thermal calibration coefficient",
                               WAVE_LENGTH                 = "Central wave length",
                               TEMPERATURE_CONSTANT        = "Constant for temperature computation (A, B, and C)";
    
    /** Catégorie de l'image S.S.T.. */ 
    private static final SampleDimension[] SAMPLE_SST_GEOPHYSIC = Utilities.SAMPLE_SST_GEOPHYSIC;

    /** Catégorie du masque générée. */ 
    private static final SampleDimension[] SAMPLE_MASK_GEOPHYSIC = Utilities.SAMPLE_MATRIX_GEOPHYSIC;
    
    /** Système de coordonnée de l'image S.S.T. générée. */
    private static final CoordinateSystem WGS84 = Utilities.WGS84;
    
    /** Résolution de l'image générée en degré. */
    private static final double RESOLUTION = Utilities.RESOLUTION; 

    /** Taille en Mo du cache JAI. */
    private final long JAI_MEMORY_CACHE;
    
    /** Taille en largeur d'une tuile de l'image. */
    private final int TILE_W;
        
    /** Taille en hauteur d'une tuile de l'image. */    
    private final int TILE_H;
    
    /** Fichier contenant le masque terre (ce fichier est un <CODE>Isoline</CODE> sérialisé). */
    private final String ISOLINE_PATH;   

    /** 
     * Angle d'exclusion des pixels en degré. Les pixels acquis par le capteur avec un angle 
     * supérieur à "SENSOR_ANGLE_EXCLUSION" seront considérés comme des données abscentes.
     */
    private final double SENSOR_EXCLUSION_ANGLE;
    
    /** Paramètres de calcul de la matrice jour/nuit. */
    private final Range TRANSITION_AUBE,
                        TRANSITION_CREPUSCULE;

    /** Paramètres nécessaires au filtrage latitudinal. */
    private final ThresoldRange[] LATITUDINAL;
    
    /** 
     * Toutes les pixels dont la température est hors de cet intervalle seront considérés 
     * comme des nuages.
     */
    private final Range FILTER_TEMPERATURE_SST;        

    /** Paramètres de calcul du masque nuage. */
    private final AffineTransform[] FILTER_SST_SLOPE_INTERCEPT;

    /** Variables simplifiant la lecture du code. */
    private static final int CANAL_4_AJ  = 3,
                             CANAL_5_AJ  = 4,
                             CANAL_4_KLM = 4,
                             CANAL_5_KLM = 5;

    /** Variables simplifiant la lecture du code. */
    final int INTERVAL_NEXT_CONTROL_POINT = ImageReaderN1B.INTERVAL_NEXT_CONTROL_POINT,
              OFFSET_FIRST_CONTROL_POINT  = ImageReaderN1B.OFFSET_FIRST_CONTROL_POINT-1;
    
    /**
     * Construit un objet SST. 
     */
    public SST() throws IOException
    {   
        // Extraction des paramètres de configuration du calcul.
        final ParameterList param = ParseSST.parse(ParseSST.getInputDefaultParameterList());        
        JAI_MEMORY_CACHE          = param.getIntParameter(ParseSST.JAI_MEMORY_CACHE);
        SENSOR_EXCLUSION_ANGLE    = param.getDoubleParameter(ParseSST.EXCLUSION_ANGLE);
        TRANSITION_AUBE           = (Range)param.getObjectParameter(ParseSST.TRANSITION_AUBE);
        TRANSITION_CREPUSCULE     = (Range)param.getObjectParameter(ParseSST.TRANSITION_CREPUSCULE);
        FILTER_TEMPERATURE_SST    = (Range)param.getObjectParameter(ParseSST.FILTER_TEMPERATURE_SST);        
        TILE_W      = param.getIntParameter(ParseSST.JAI_TILE_WIDTH);
        TILE_H      = param.getIntParameter(ParseSST.JAI_TILE_HEIGHT);                
        FILTER_SST_SLOPE_INTERCEPT = (AffineTransform[])param.getObjectParameter(ParseSST.FILTER_SST_SLOPE_INTERCEPT);                
        ISOLINE_PATH = (String)param.getObjectParameter(ParseSST.ISOLINE_PATH);                
        LATITUDINAL  = (ThresoldRange[])param.getObjectParameter(ParseSST.FILTER_LATITUDINAL);

        // Configuration de JAI 
        final JAI jai = JAI.getDefaultInstance();
        final TileCache tileCache = jai.getTileCache();
        tileCache.setMemoryCapacity(JAI_MEMORY_CACHE*1024*1024);
        jai.setDefaultTileSize(new Dimension(TILE_W, TILE_H));            
    }
    
    /**
     * Génère un fichier "texte" de données statistiques. Le fichier généré permettra dans 
     * le futur de corriger les différences de température entre les pixels acquis de nuit 
     * et ceux acquis de jour pour une latitude donnée. Ainsi, les températures calculées 
     * de nuit et de jour pourront être ajustée.<BR><BR>
     *
     * Le fichier de statistique est de la forme : <i>LAT  STAT_JOUR   STAT_NUIT</i><BR>
     * ou <i>STAT_JOUR</i> est de la forme : <i>NB_PIX_JOUR   MOYENNE     ECART_TYPE</i><BR>
     * et <i>STAT_NUIT</i> est de la forme : <i>NB_PIX_NUIT   MOYENNE     ECART_TYPE</i><BR>
     *
     * @param sst       GridCoverage contenant la S.S.T.
     * @param matrix    GridCoverage contenant la matrice.
     * @param origine   Coordonnées (x,y) dans le système géographique du pixel coin haut 
     *                  gauche de l'image.
     * @param file      Fichier de sortie texte.
     */    
    private void generateStatisticMatrix(final GridCoverage sst,
                                         final GridCoverage matrix,
                                         final Point2D      origine,
                                         final File         fileTxt) throws IOException
    {                       
        final Category catSSTTemperature = Utilities.getCategory(SAMPLE_SST_GEOPHYSIC, 
                                                                 Utilities.TEMPERATURE);
        final Range rTemperature   = catSSTTemperature.getRange();        
        final RenderedImage source = sst.getRenderedImage();
        final Rectangle     bound  = new Rectangle(source.getMinX(),  source.getMinY(),
                                                   source.getWidth(), source.getHeight());

        final RectIter iSST    = RectIterFactory.create(sst.getRenderedImage(), bound),
                       iMatrix = RectIterFactory.create(matrix.getRenderedImage(), bound);
        
        final GridStatistics stat = new GridStatistics(origine.getY() - (source.getHeight()-1)*RESOLUTION, 
                                                       origine.getY(), 
                                                       RESOLUTION);        
        
        // Parcours des fichiers SST et Matrice.
        iSST.startBands();
        iMatrix.startBands();
        iSST.startLines();
        iMatrix.startLines();
        int row = 0;
        while (!iSST.finishedLines() && !iMatrix.finishedLines())
        {
            int countDayPix       = 0,
                countNightPix     = 0;
            double sumDayPix      = 0,
                   sumNightPix    = 0,
                   ecartTypeDay   = 0,
                   ecartTypeNight = 0;
            
            // Premier parcours de la ligne, calcul de la moyenne des températures. 
            iSST.startPixels();
            iMatrix.startPixels();
            while (!iSST.finishedPixels() && !iMatrix.finishedPixels())
            {
                final Double value_sst = new Double(iSST.getSampleDouble());
                /* Si "value_sst" est une température et si la valeur a été acquise de 
                   jour ou de nuit alors elle est prise en compte dans les statistiques. */                
                if (rTemperature.contains(value_sst))
                {
                    final int value_matrix = (int)iMatrix.getSampleDouble();                    
                    switch (value_matrix)                    
                    {
                        case (int)MatrixDayNight.DAY :
                            // température de jour.
                            countDayPix ++;
                            sumDayPix += value_sst.doubleValue();
                            break;

                        case (int)MatrixDayNight.NIGHT :
                            // température de nuit.
                            countNightPix ++;
                            sumNightPix += value_sst.doubleValue();
                            break;
                    }
                }            
                iSST.nextPixel();
                iMatrix.nextPixel();
            }        
            
            // Moyenne des températures de la ligne.
            final double avgDay   = countDayPix   > 0 ? (sumDayPix/countDayPix) : 0,
                         avgNight = countNightPix > 0 ? (sumNightPix/countNightPix) : 0;
            
            // Second parcours de la ligne, calcul de l'ecart type depuis la moyenne des 
            // temperatures. 
            iSST.startPixels();
            iMatrix.startPixels();
            while (!iSST.finishedPixels() && !iMatrix.finishedPixels())
            {
                final Double value_sst = new Double(iSST.getSampleDouble());
                /* Si "value_sst" est une température et si la valeur a été acquise de 
                   jour ou de nuit alors elle est prise en compte dans les statistiques. */                
                if (rTemperature.contains(value_sst))
                {
                    final int value_matrix = (int)iMatrix.getSampleDouble();                    
                    switch (value_matrix)                    
                    {
                        case (int)MatrixDayNight.DAY :
                            ecartTypeDay += (value_sst.doubleValue()-avgDay)*
                                            (value_sst.doubleValue()-avgDay);                            
                            break;

                        case (int)MatrixDayNight.NIGHT :
                            ecartTypeNight += (value_sst.doubleValue()-avgNight)*
                                              (value_sst.doubleValue()-avgNight);                            
                            break;
                    }
                }                            
                iSST.nextPixel();
                iMatrix.nextPixel();
            }        

            // Ecart type des températures.
            ecartTypeDay   = countDayPix   > 0 ? Math.sqrt(ecartTypeDay/countDayPix) : 0;
            ecartTypeNight = countNightPix > 0 ? Math.sqrt(ecartTypeNight/countNightPix) : 0;
                         
            // Ecriture de la statistique.
            stat.setCountDay(row, countDayPix);
            stat.setCountNight(row, countNightPix);
            stat.setAvgTempOfDay(row, avgDay);            
            stat.setAvgTempOfNight(row, avgNight);            
            stat.setEcartTypeTempOfDay(row, ecartTypeDay);
            stat.setEcartTypeTempOfNight(row, ecartTypeNight);
            iSST.nextLine();            
            iMatrix.nextLine();
            row ++;
        }           
        
        // Ecriture sur disque.
        Utilities.writeStat(stat, fileTxt);
    }
    
    /**
     * Retourne un GridCoverage.
     *
     * @param source        Une image source.
     * @param cs            Un système de coordonnée.
     * @param sample        Tableau de category.
     * @return un grid coverage.
     */
    private static final GridCoverage createGridCoverage (final RenderedImage      source, 
                                                          final CoordinateSystem   cs,
                                                          final SampleDimension[]  sample) 
    {
        for (int i=0;i<sample.length ; i++)
            sample[i] = sample[i].geophysics(true);                
        final Envelope envelope = new Envelope(new Rectangle(0, 0, source.getWidth(), source.getHeight()));            
        return new GridCoverage("cs", 
                                source, 
                                cs,                                     
                                envelope,
                                sample,
                                (GridCoverage[])null,
                                (Map)null);         
    }

    /**
     * Génère le fichier <i>header</i> associé à l'image S.S.T. PNG. Ce fichier contient
     * des informations permettant de localiser l'image S.S.T. générée dans le système
     * géographique.
     *
     * @param header        Fichier header.
     * @param n1B           Fichier N1B.
     * @param reader        Lecteur du fichier N1B.
     * @param width         Largeur de l'image N1B.
     * @param height        Hauteur de l'image N1B.
     * @param origine       Coordonnées (longitude, latitude) du pixel coin haut gauche de 
     *                      l'image S.S.T..
     * @param time          Temps de calcul en seconde.
     */
    private void createHeader(final File            header,
                              final File            n1b,
                              final int             width,
                              final int             height,
                              final Date            start,
                              final Date            end,
                              final Point2D         origine,
                              final double          time) throws IOException
    {
        final DateFormat dateFormat  = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss",   Locale.FRANCE);
        final StringBuffer bStart = new StringBuffer(),
                           bEnd   = new StringBuffer();
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.format(start, bStart, new FieldPosition(0));        
        dateFormat.format(end,   bEnd, new FieldPosition(0));        
        
        final BufferedWriter info = new BufferedWriter(new FileWriter(header));                                                                
        info.write("ORIGINE        \t" + origine.getX() + "\t" + origine.getY() + "\n");
        info.write("RESOLUTION     \t" + RESOLUTION     + "\t" + RESOLUTION     + "\n\n\n\n");
        info.write("# RESUME DU TRAITEMENT\n");            
        info.write("SOURCE         \t\""  + n1b.getPath()    + "\"\n");
        info.write("START_TIME     \t"    + bStart           + "\n");
        info.write("END_TIME       \t"    + bEnd             + "\n");
        info.write("WIDTH          \t"    + width            + "\n");
        info.write("HEIGHT         \t"    + height           + "\n");        
        info.write("PROCESSING     \t"    + time             + " secondes.\n\n");            
        info.write("EXCLUSION_ANGLE     \t\t\t" + SENSOR_EXCLUSION_ANGLE + "\n");
        info.write("TRANSITION_DAY_NIGHT\t\t\t" + TRANSITION_AUBE        + TRANSITION_CREPUSCULE + "\n");            
        info.write("FILTER_TEMPERATURE_SST\t\t"   + FILTER_TEMPERATURE_SST + "\n");            
        
        for (int i=0; i<FILTER_SST_SLOPE_INTERCEPT.length ; i++)
        {
            info.write("FILTER_SST_SLOPE_INTERCEPT  \t\t" + 
                       FILTER_SST_SLOPE_INTERCEPT[i].getScaleX()     + "\t" + 
                       FILTER_SST_SLOPE_INTERCEPT[i].getTranslateX() + "\n");        
        }
        
        for (int i=0 ; i<LATITUDINAL.length ; i++)
        {
            double latitude, 
                   seuil;
            if (i==0)
            {
                latitude = ((Double)(LATITUDINAL[i].getRange().getMaxValue())).doubleValue();
                seuil    = LATITUDINAL[i].getThresold2();
                info.write("FILTRE_LATITUDINAL\t\t\t"      + latitude + "\t" + seuil + "\n");
            }                                
            latitude = ((Double)(LATITUDINAL[i].getRange().getMinValue())).doubleValue();
            seuil    = LATITUDINAL[i].getThresold1();
            info.write("FILTRE_LATITUDINAL\t\t\t"      + latitude + "\t" + seuil + "\n");
        }
        info.close();                                                        
    }        

    /**
     * Retourne le <CODE>GridCoverage</CODE> projeté dans le système géographique.
     *
     * @param source  GridCoverage à projeter dans le système géographique.
     * @return le <CODE>GridCoverage</CODE> projeté dans le système géographique.
     */
    private GridCoverage projectToGeographic(final GridCoverage source)
    {
       // On projète l'image dans le système géographique avec la résolution désirée. 
       final AffineTransform at = new AffineTransform(RESOLUTION, 0, 
                                                      0, RESOLUTION, 
                                                      0, 0);       
       final MathTransform2D tr    = MathTransformFactory.getDefault().createAffineTransform(at);
       final GridGeometry geometry = new GridGeometry(null, tr);               
       return GridCoverageProcessor.getDefault().doOperation("Resample",         source, 
                                                             "CoordinateSystem", WGS84,
                                                             "GridGeometry",     geometry);
    }
    
    /**
     * Retourne un masque des valeurs considérées comme aberrantes. 
     * 
     * @param sst               Image S.S.T. source.
     * @param configuration     Configuration du traitement réalisé par le JAI.
     * @return un masque des valeurs considérées comme aberrantes. 
     */ 
    private RenderedImage computeMaskNoData(final RenderedImage sst,
                                            final Map           configuration) 
    {
        /* Ce masque détermine les valeurs considérée comme aberrantes. Pour cela, toutes 
           les températures hors de l'intervalle seront considérées comme valeur 
           aberrantes. */
        final double min = ((Double)FILTER_TEMPERATURE_SST.getMinValue()).doubleValue(),
                     max = ((Double)FILTER_TEMPERATURE_SST.getMaxValue()).doubleValue();
        final ParameterList parameter = FilterRange.getEmptyParameterList();
        final Range kelvin = new Range(Double.class, 
                                       new Double(min), 
                                       FILTER_TEMPERATURE_SST.isMinIncluded(), 
                                       new Double(max), 
                                       FILTER_TEMPERATURE_SST.isMaxIncluded());
        
        // Configuration du filtre. 
        parameter.setParameter(FilterRange.RANGE, kelvin);        
        final Filter filter = FilterRange.get(parameter);           
        final RenderedImage[] array    = {sst};        
        return Mask.get(array, filter, configuration);                
    }
    
    /**
     * Retourne une image contenant le masque des nuages. 
     *
     * @param t4              Une image de température de brillance du canal 4.
     * @param t5              Une image de température de brillance du canal 5.
     * @param sst             Une image SST.
     * @param imgToGeo        Transformation du système de coordonnées de l'image vers le 
     *                        système géographique.
     * @param configuration   Configuration des images utilisant le JAI.
     * @return une image contenant le masque des nuages. 
     */
    private RenderedImage computeMaskCloud(final RenderedImage t4,
                                           final RenderedImage t5,
                                           final RenderedImage sst,
                                           final MathTransform imgToGeo,
                                           final Map           configuration)
    {       
        /* Calcul le masque nuage en filtrant les nuages en fonction des termpératures des 
           canaux 4 et 5, de la S.S.T.. Plusieurs filtres de ce type peuvent avoir été 
           définis dans le fichier de configuration. Pour cette raison, on calcul un seul 
           masque égale à l'union des masques définis par l'utilisateur. */
        RenderedImage maskCloud = null;                
        for (int i=0; i<FILTER_SST_SLOPE_INTERCEPT.length ; i++)
        {
            final ParameterList parameter = FilterSST.getEmptyParameterList();        
            parameter.setParameter(FilterSST.AFFINE_TRANSFORM, FILTER_SST_SLOPE_INTERCEPT[i]);        
            final Filter filter = FilterSST.get(parameter);
            final RenderedImage[] array    = {t4, t5, sst};
            final RenderedImage maskSST = Mask.get(array, filter, configuration);                                            
            if (maskCloud == null)
            {
                maskCloud = maskSST;
            }
            else
            {
                /* On ajoute les différents masques pour soustraire un seul masque nuage 
                   à l'image. */
                ParameterBlock block = (new ParameterBlock());
                block = block.addSource(maskSST);
                block = block.addSource(maskCloud);                
                maskCloud = JAI.create("Or", block).createInstance();                
            }
        }   
        
        /* Ces masques permettent d'identifier les gros nuages, mais pas forcément les 
           contours des nuages. Pour cette raison, on applique deux traitements à ces 
           masques. On élimine dans un premier temps les pixels nuages isolés (1 pixel 
           nuage entouré de pixels qui ne sont pas des nuages) et on applique une dilatation 
           du masque nuage pour éliminer les contours.*/
        if (maskCloud != null)
        {
            final float pixCloud = (float)Mask.FILTERED,
                        pixOther = (float)Mask.NOT_FILTERED;
            
            // On décrit le filtre permettant d'éliminer les pixels isolés. 
            final float[] data_pattern = {pixOther, pixOther, pixOther, 
                                          pixOther, pixCloud, pixOther,
                                          pixOther, pixOther, pixOther};
            final KernelJAI pattern = new KernelJAI(3, 3, 1, 1, data_pattern);                        

            /* Suppression des pixels isolés (il est nécéssaire de supprimer ces pixels 
               car la dilation ferait apparâitre des nuages important au endroit ou un 
               seul pixel nuage est détectée. */
            maskCloud = PatternFilter.get(maskCloud, pattern, Mask.NOT_FILTERED, configuration);
            
            /* Dilatation du masque nuage. */
            final float[] data_dilate = {0.0F, 0.0F, 0.0F, 
                                         0.0F, 0.0F, 0.0F,
                                         0.0F, 0.0F, 0.0F};
            final KernelJAI kernel_dilate = new KernelJAI(3, 3, 1, 1, data_dilate);
            ParameterBlock block = new ParameterBlock();
            block = block.addSource(maskCloud);            
            block = block.add(kernel_dilate);
            maskCloud = JAI.create("Dilate", block).createInstance();                   
        }
        
        /* Calcul du masque nuage filtrant en latitude. Les pixels trop froid par rapport 
           à leur latitude sont considérés comme des nuages. */
        final ParameterList parameter = FilterLatitudinal.getEmptyParameterList();
        parameter.setParameter(FilterLatitudinal.RANGE,     LATITUDINAL);        
        parameter.setParameter(FilterLatitudinal.TRANSFORM, imgToGeo);                
        final Filter filter = FilterLatitudinal.get(parameter);        
        final RenderedImage[] array = {sst};        
        final RenderedImage maskLat = Mask.get(array, filter, configuration);
        
        // Ajout des masques nuages "maskCloud" et "maskLat". 
        ParameterBlock block = new ParameterBlock();
        block = block.addSource(maskCloud);
        block = block.addSource(maskLat);
        return JAI.create("Or", block).createInstance();                
    }
    
    /**
     * Retourne une image contenant la température de brillance correspondant à la bande 
     * <CODE>channel</CODE>.
     *
     * @param reader         Lecteur de fichier N1B.
     * @param channel        Numéro de la bande sur laquelle calculer la température de 
     *                       brillance.
     * @param configuration  Configuration du JAI pour la gestion des tuiles lors du calcul.
     * @return une image contenant la température de brillance correspondant à la bande 
     *         <CODE>channel</CODE>.
     */
    private RenderedImage computeTemperature(final ImageReaderN1B  reader,
                                            final int              channel,
                                            final Map              configuration) 
                                            throws IOException
    {
        final ImageReadParam paramReader = new ImageReadParam();                
        final int[] bandeSrc = {channel};

        // Extraction du satellite. 
        final Metadata metadata   = (Metadata)reader.getImageMetadata(0);
        final Satellite satellite = Satellite.get(metadata.getSpacecraft());                                                    
        
        // Paramètre de la calibration du fichier N1B. 
        final ParameterList  paramCalibN1B  = reader.getCalibrationParameter(channel);        
                    
        // Paramètres de calibration du fichier de configuration. 
        final ParameterList parameterInConf = ParseSatellite.getInputDefaultParameterList();
        parameterInConf.setParameter(ParseSatellite.SATELLITE, satellite);
        parameterInConf.setParameter(ParseSatellite.CHANNEL, new Integer(channel));
        final ParameterList parameterOutConf = ParseSatellite.parse(parameterInConf);                     
        
        // Paramètres de configuration de la radiance. 
        final ParameterList parameterInRadiance = Radiance.getInputParameterList(satellite);        
        if (!satellite.isKLM())
            parameterInRadiance.setParameter(SLOPE_INTERCEPT_COEFFICIENT,
                             paramCalibN1B.getObjectParameter(SLOPE_INTERCEPT_COEFFICIENT));
        else
            parameterInRadiance.setParameter(THERMAL_COEFFICIENT,
                                    paramCalibN1B.getObjectParameter(THERMAL_COEFFICIENT));
        
        // Extraction de l'image count. 
        paramReader.setSourceBands(bandeSrc);              
        final RenderedImage count = reader.read(0, paramReader);
        
        // Calcul de la radiance.
        final RenderedImage radiance = Radiance.get(satellite, 
                                                    count, 
                                                    parameterInRadiance, 
                                                    configuration);                            
        
        // Paramètres de configuration de la radiance. 
        final ParameterList parameterInTemperature = Temperature.getInputParameterList(satellite);                
        if (!satellite.isKLM())
            parameterInTemperature.setParameter(WAVE_LENGTH,
                                        parameterOutConf.getObjectParameter(WAVE_LENGTH));
        else
            parameterInTemperature.setParameter(TEMPERATURE_CONSTANT,            
                                parameterOutConf.getObjectParameter(TEMPERATURE_CONSTANT));

        // Calcul de la température. 
        return Temperature.get(satellite, 
                               radiance, 
                               parameterInTemperature, 
                               configuration); 
    }
    
    /**
     * Retourne le nom du fichier généré sans l'extension.
     * format  : "N12YYYYJJJHHMMSS"
     *
     * @param start         Date de début de l'acquisition.
     * Retourne le nom du fichier généré sans l'extension.
     */
    private static String getOutputName(final Date      start,
                                        final Satellite satellite) 
    {
        final DateFormat dateFormat  = new SimpleDateFormat("yyyyDDDHHmmss",   Locale.FRANCE);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        final StringBuffer buffer = new StringBuffer("N" + satellite.getID());
        dateFormat.format(start, buffer, new FieldPosition(0));
        return buffer.toString();
    }
    
    /**
     * Calcul l'image S.S.T. à partir d'un fichier N1B.
     *
     * @param source        Fichier source.
     * @param directory     Répertoire de destination des fichiers.
     * @param format        Format du fichier de sortie (png, jpg, ...).
     */
    public void compute(final File   source, 
                        final File   directory, 
                        final String format) throws IOException
    {           
        // Contrôle de la validité des paramètres. 
        if (source == null)
            throw new IllegalArgumentException("File source is null.");
        if (!source.isFile())
            throw new IllegalArgumentException("Source is not a file.");                        
        if (!source.exists())
            throw new IllegalArgumentException("File doesn't exist : " + 
                                               source.getPath() + ".");            
        if (!source.canRead())
            throw new IllegalArgumentException("Can not read file : " + 
                                               source.getPath() + ".");                    
        
        if (directory == null)
            throw new IllegalArgumentException("Directory is null.");        
        if (!directory.isDirectory())
            throw new IllegalArgumentException("Directory is not a directory.");        
        if (!directory.exists())
                throw new IllegalArgumentException("Directory doesn't exist : " + 
                                                   directory.getPath() + ".");            
        if (!directory.canWrite())
            throw new IllegalArgumentException("Can not write in directory : " + 
                                               directory.getPath() + ".");
        
        // Instant de début des calculs. 
        final long START_COMPUTATION = System.currentTimeMillis();

        // Création de la configuration du JAI utilisé par les images lors des traitements. 
        final Map configuration   = new HashMap();
        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        configuration.put(JAI.KEY_TILE_CACHE, tileCache);                
        tileCache.flush();
        
        // Ouverture du fichier N1B.
        final FileImageInputStream stream = new FileImageInputStream(source);
        final ImageReaderN1B reader       = (ImageReaderN1B)ImageReaderN1B.get(stream);                
        final LocalizationGridN1B grid    = reader.getGridLocalization();
        final Metadata metadata           = (Metadata)reader.getImageMetadata(0);
        final Satellite satellite         = Satellite.get(metadata.getSpacecraft());                                            
        final int width  = reader.getWidth(0),
                  height = reader.getHeight(0);
        final Date start = metadata.getStartTime(),
                   end   = metadata.getEndTime();
                
        // Zone couverte par l'image.
        Rectangle bound = new Rectangle(0, 0, width, height);            

        // Noms des fichiers générés.
        final String name    = directory.getPath() + 
                               File.separatorChar  +
                               getOutputName(start, satellite);        
        final File fImageSST    = new File(name + ".sst." + format),    // image S.S.T.
                   fImageMatrix = new File(name + ".msk." + format),    // masque
                   fHeader      = new File(name + ".sst.hdr"),          // header
                   fStatTxt     = new File(name + ".msk.sta"),          // statistique
                   fTmp         = new File(name + ".tmp");              // temporaire
                
        // Système de coordonnées du fichier N1B. 
        final AffineTransform at1 = new AffineTransform(INTERVAL_NEXT_CONTROL_POINT, 0, 
                                                        0, 1, 
                                                        OFFSET_FIRST_CONTROL_POINT-1, 0);        
        final MathTransformFactory defaultMT = MathTransformFactory.getDefault();        
        final MathTransform gridToGeo = grid.getMathTransform();  
        final MathTransform imToGrid;
        final MathTransform imToGeo;
        try
        {
            imToGrid  = defaultMT.createAffineTransform(at1).inverse();                            
            imToGeo   = defaultMT.createConcatenatedTransform(imToGrid, gridToGeo);        
        }
        catch (org.geotools.ct.NoninvertibleTransformException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        
        // Axes du systeme N1B.
        final AxisInfo[] AXIS = {new AxisInfo("colonne", AxisOrientation.OTHER),
                                 new AxisInfo("ligne"  , AxisOrientation.OTHER)};                                           
                                 
        // Système de coordonnée de l'image.
        final FittedCoordinateSystem cs = new FittedCoordinateSystem(
                                                        "N1B",   
                                                        GeographicCoordinateSystem.WGS84, 
                                                        imToGeo, AXIS);                        
        // Informations sur les palettes couleurs.
        final Category catSSTCloud       = Utilities.getCategory(SAMPLE_SST_GEOPHYSIC, 
                                                                 Utilities.CLOUD),
                       catSSTNoData      = Utilities.getCategory(SAMPLE_SST_GEOPHYSIC, 
                                                                 Utilities.NO_DATA),
                       catSSTLandBg      = Utilities.getCategory(SAMPLE_SST_GEOPHYSIC, 
                                                                 Utilities.LAND_BACKGROUND),
                       catSSTLandContour = Utilities.getCategory(SAMPLE_SST_GEOPHYSIC, 
                                                                 Utilities.LAND_CONTOUR),
                       catMatrixCloud    = Utilities.getCategory(SAMPLE_MASK_GEOPHYSIC, 
                                                                 Utilities.CLOUD),
                       catMatrixNoData   = Utilities.getCategory(SAMPLE_MASK_GEOPHYSIC, 
                                                                 Utilities.NO_DATA),
                       catMatrixLand     = Utilities.getCategory(SAMPLE_MASK_GEOPHYSIC, 
                                                                 Utilities.LAND_BACKGROUND);                
        if (catSSTCloud == null)
            throw new IllegalArgumentException("Category \""   + 
                                               Utilities.CLOUD + 
                                               "\" is not define.");
        if (catSSTNoData == null)
            throw new IllegalArgumentException("Category \""     + 
                                               Utilities.NO_DATA + 
                                               "\" is not define.");
        if (catSSTLandBg == null)
            throw new IllegalArgumentException("Category \"" + 
                                               Utilities.LAND_BACKGROUND + 
                                               "\" is not define.");
        if (catSSTLandContour == null)
            throw new IllegalArgumentException("Category \"" + 
                                               Utilities.LAND_CONTOUR + 
                                               "\" is not define.");
        if (catMatrixCloud == null)
            throw new IllegalArgumentException("Category \"" + 
                                               Utilities.CLOUD + 
                                               "\" is not define.");
        if (catMatrixNoData == null)
            throw new IllegalArgumentException("Category \"" + 
                                               Utilities.NO_DATA + 
                                               "\" is not define.");
        if (catMatrixLand == null)
            throw new IllegalArgumentException("Category \"" + 
                                               Utilities.LAND_BACKGROUND + 
                                               "\" is not define.");        
        final double geoSSTCloud     = ((Double)catSSTCloud.getRange().getMaxValue()).doubleValue(),
                     geoSSTNoData    = ((Double)catSSTNoData.getRange().getMaxValue()).doubleValue(),
                     geoMatrixNoData = ((Double)catMatrixNoData.getRange().getMaxValue()).doubleValue(),
                     geoMatrixCloud  = ((Double)catMatrixCloud.getRange().getMaxValue()).doubleValue();                
        final Color geoSSTLandBg      = catSSTLandBg.getColors()[0],
                    geoSSTLandContour = catSSTLandContour.getColors()[0],
                    geoMatrixLand     = catMatrixLand.getColors()[0];
                
        // Calcul de la Température de brillance du canal 4.
        final int canal4 = satellite.isKLM() ? CANAL_4_KLM : CANAL_4_AJ;
        final RenderedImage t4 = computeTemperature(reader, canal4, configuration);

        // Calcul de la Température de brillance du canal 5.
        final int canal5 = satellite.isKLM() ? CANAL_5_KLM : CANAL_5_AJ;
        final RenderedImage t5 = computeTemperature(reader, canal5, configuration);
        
        // Calcul de la matrice jour / nuit. 
        final RenderedImage elevation = SolarElevation.get(grid, bound, configuration);        
        RenderedImage matrix = MatrixDayNight.get(elevation, grid, TRANSITION_AUBE, 
                                                  TRANSITION_CREPUSCULE, configuration);        

        // Calcul de l'angle zenithal du satellite. 
        final RenderedImage zenithAngle = SatelliteZenithAngle.get(grid, bound, configuration);
        
        // Calcul de la S.S.T. 
        ParameterList paramProcess = reader.getCalibrationParameter(canal5);        
        final ParameterList parameterInConf = ParseSatellite.getInputDefaultParameterList();
        parameterInConf.setParameter(ParseSatellite.SATELLITE, satellite);
        parameterInConf.setParameter(ParseSatellite.CHANNEL, new Integer(canal5));
        final ParameterList parameterOutConf = ParseSatellite.parse(parameterInConf);                             
        double[] coeffDay   = (double[])parameterOutConf.getObjectParameter(
                                                    ParseSatellite.LINEAR_SPLIT_WINDOW_DAY),
                 coeffNight = (double[])parameterOutConf.getObjectParameter(
                                                    ParseSatellite.LINEAR_SPLIT_WINDOW_NIGHT);
        RenderedImage sst = fr.ird.n1b.op.SST.get(t4, t5, zenithAngle, matrix, 
                                                  coeffDay, coeffNight,configuration);
        
        // Calcul des masques nuages.  
        final RenderedImage maskCloud = computeMaskCloud(t4, t5, sst, imToGeo, configuration);
        sst = SuperposeMask.get(sst,maskCloud, Mask.FILTERED,geoSSTCloud, configuration);                        
        matrix = SuperposeMask.get(matrix, maskCloud,Mask.FILTERED, geoMatrixCloud, configuration);        
        
        // Calcul du masque sur la SST. 
        final RenderedImage maskNoData = computeMaskNoData(sst, configuration);
        sst = SuperposeMask.get(sst, maskNoData, Mask.FILTERED, geoSSTNoData, configuration);                
        matrix = SuperposeMask.get(matrix, maskNoData, Mask.FILTERED, geoMatrixNoData, configuration);        
        
        // Exclusion des pixels trop éloignés du nadir. 
        final RenderedImage sensorAngle = SensorAngle.get(grid, bound, configuration);        
        sst = AngleExclusion.get(sst, sensorAngle,  SENSOR_EXCLUSION_ANGLE, false,
                                 geoSSTNoData, configuration);        
        matrix = AngleExclusion.get(matrix, sensorAngle, SENSOR_EXCLUSION_ANGLE, false,
                                    geoMatrixNoData, configuration);     
        
        // projection dans le système géographique. 
        GridCoverage gridSST = projectToGeographic(
                        createGridCoverage(sst, cs, SAMPLE_SST_GEOPHYSIC).geophysics(false));
        GridCoverage gridMatrix = projectToGeographic(
                        createGridCoverage(matrix, cs, SAMPLE_MASK_GEOPHYSIC).geophysics(false));

        // Superposition du masque Terre. 
        final Isoline isoline = Utilities.loadSerializedIsoline(ISOLINE_PATH);
        if (isoline != null)
        {
            gridSST = Utilities.addLayer(gridSST, isoline, geoSSTLandBg, geoSSTLandContour);                    
            gridMatrix = Utilities.addLayer(gridMatrix, isoline, geoMatrixLand, geoMatrixLand);        
        }
        sst    = gridSST.geophysics(false).getRenderedImage();        
        matrix = gridMatrix.geophysics(false).getRenderedImage();        
        
        // Coordonnées de l'origine.
        final Point2D origine = new Point2D.Double((double)((int)(sst.getMinX()*
                                                   RESOLUTION*100.0))/100.0, 
                                                   (double)((int)(sst.getMinY()*
                                                   RESOLUTION*100.0*-1))/100.0);                 
        
        // Ecriture de l'image S.S.T.
        Utilities.writeImage(sst, fImageSST, fTmp);
        
        // Ecriture de la matrice. 
        Utilities.writeImage(matrix, fImageMatrix, fTmp);

        // Création du fichier de statistiques. 
        generateStatisticMatrix(gridSST.geophysics(true), gridMatrix.geophysics(true),
                                origine, fStatTxt);
       
        // Création du fichier Header.
        final double time = ((System.currentTimeMillis() - START_COMPUTATION)/1000.0);        
        createHeader(fHeader, source, width, height, start, end, origine, time);
    }
     
    /**
     * Lancement du traitement. Ce programme calcul la S.S.T. projetée dans le système 
     * géographique de l'image N1B avec une résolution de 1/100°. Le fichier S.S.T. est
     * généré au <CODE>format</CODE> dans le repertoire <CODE>directory</CODE>. Outre
     * le fichier S.S.T., un fichier <i>header</i> est généré contenant des informations
     * de localisation de l'image dans le système géographique. <BR><BR>
     *
     * @param source        Le fichier source.
     * @param directory     Répertoire de destination des fichiers.
     * @param format        Le format du fichier de sortie (png, ...);
     */
    public static void main(String[] args)
    {        
        final int count = args.length;
        if (count < 3) 
        {
            System.err.println("SST SOURCE  REP_DESTINATION FORMAT");
            System.err.println("SOURCE                  --> Image source N1B");
            System.err.println("REPERTOIRE_DESTINATION  --> Répertoire de destination");
            System.err.println("FORMAT                  --> Format de l'image générée (PNG, ...)");
            System.exit(-1);
        }
        try 
        {            
            final SST sst        = new SST();
            final String format  = args[2];
            final File source    = new File(args[0]);
            final File directory = new File(args[1]);        
            sst.compute(source, directory, format);
        }
        catch (Exception e) 
        {
            System.err.println(e);
            System.exit(-1);
        }
    }   
}