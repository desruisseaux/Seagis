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
import fr.ird.io.text.Parse;
import fr.ird.io.text.ParseSST;
import fr.ird.io.text.ParseHeader;
import fr.ird.n1b.io.Bulletin;
import fr.ird.n1b.io.Metadata;
import fr.ird.n1b.io.Satellite;
import fr.ird.n1b.io.FilenameFilterSST;

// J2SE / JAI.
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.util.TimeZone;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.image.Raster;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import javax.media.jai.JAI;
import javax.imageio.ImageIO;
import javax.media.jai.TileCache;
import javax.media.jai.util.Range;
import javax.media.jai.PlanarImage;
import javax.imageio.ImageReadParam;
import javax.media.jai.ParameterList;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;

// GEOTOOLS.
import org.geotools.gc.GridCoverage;
import org.geotools.cs.FittedCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.Category;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.Envelope;
import org.geotools.pt.CoordinatePoint;
import org.geotools.renderer.geom.Isoline;
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransform1D;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.MathTransformFactory;
import org.geotools.cs.AxisInfo;
import org.geotools.cs.AxisOrientation;

/**
 * Calcul la synthèse d'image Sea Surface Temperature (S.S.T.) sur N jour. Cette S.S.T. 
 * génère une image contenant la synthèse des images sources ainsi qu'un fichier <i>header</i>
 * contenant des informations sur le traitement réalisé.<BR><BR>
 *
 * Cette synthèse permet notament de calculer une images plus réaliste de la S.S.T. pour 
 * le jour <i>J</i> en utilisant les jour "J-1", "J-2", "J+1", "J+2" et "J". En prenant 
 * la SST du jour "J" et en complétant les pixels non renseignés avec des pixels des images 
 * S.S.T. des jours les plus proches, on obtient une image S.S.T. pour le jour "J" plus 
 * complète qui reste cohérente.<BR><BR>
 *
 * L'algorithme consiste ici à prendre les images sources dans l'ordre donnée et à essayer 
 * de compléter les pixels non renseignés de la première image avec les pixels des images 
 * sources suivantes. Lorsqu'un pixel est renseigné avec une information de type température 
 * ou terre, il ne peut plus être modifié (on traite le pixel suivant).<BR><BR>
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SSTSynthese
{    
    /** Système de coordonnée des images SST. */  
    private final CoordinateSystem WGS84 = Utilities.WGS84;
    
    /** Sample de l'image SST décrivant les valeurs physiques. */ 
    private final SampleDimension[] SAMPLE_SST_GEOPHYSIC = Utilities.SAMPLE_SST_GEOPHYSIC;

    /** Sample de l'image SST décrivant les valeurs des indexes. */ 
    private final SampleDimension[] SAMPLE_SST_INDEXED = Utilities.SAMPLE_SST_INDEXED;

    /** Résolution de l'image générée. */
    private final static double RESOLUTION = Utilities.RESOLUTION; 
    
    /** Taille en Mo du cache JAI. */
    private final long JAI_MEMORY_CACHE;
    
    /** Taille en largeur d'une tuile de l'image. */
    private final int TILE_W;
        
    /** Taille en hauteur d'une tuile de l'image. */    
    private final int TILE_H;

    /** Zone de couverture de l'image générée. */
    private final Rectangle2D SST_N_DAY_AREA;
    
    /**
     * Indique si la zone definie ci-dessus doit etre la taille de la zone de sortie 
     * (SST_N_DAY_LIMITED_AREA vaut alors true) ou si la taille de la zone de sortie doit etre
     * la plus vaste possible.
     */
    private final boolean SST_N_DAY_LIMITED_AREA;
    
    
    /**
     * Constructeur. 
     */
    public SSTSynthese() throws IOException 
    {   
        // Extraction des paramètres de configuration de la SST. 
        final ParameterList param = ParseSST.parse(ParseSST.getInputDefaultParameterList());        
        JAI_MEMORY_CACHE   = param.getIntParameter(ParseSST.JAI_MEMORY_CACHE);
        TILE_W             = param.getIntParameter(ParseSST.JAI_TILE_WIDTH);
        TILE_H             = param.getIntParameter(ParseSST.JAI_TILE_HEIGHT);                
        SST_N_DAY_LIMITED_AREA = param.getBooleanParameter(ParseSST.SST_N_DAY_LIMITED_AREA);
        SST_N_DAY_AREA         = (Rectangle2D)param.getObjectParameter(ParseSST.SST_N_DAY_AREA);

        // Configuration de JAI.
        final JAI jai = JAI.getDefaultInstance();
        final TileCache tileCache = jai.getTileCache();
        tileCache.setMemoryCapacity(JAI_MEMORY_CACHE*1024*1024);
        jai.setDefaultTileSize(new Dimension(TILE_W, TILE_H));            
    }
    
    /**
     * Calcul la synthèse d'images S.S.T.. 
     *
     * @param sources   Fichiers sources.
     * @param target    Fichier de destination.
     */
    public void compute(final File[] sources, 
                        final File   target) throws IOException
    {
        if (sources == null)
            throw new IllegalArgumentException("Source is null.");
        for (int i=0 ; i<sources.length ; i++)
            if (sources[i] == null)
                throw new IllegalArgumentException("Source at index " + i + 
                                                   "is null.");
            else if (!sources[i].exists())
                throw new IllegalArgumentException("Source at index " + i + 
                                                   "doesn't exist.");                
        if (target == null)
            throw new IllegalArgumentException("Target directory is null.");
                
        // Paramètres JAI. 
        final Map configuration = new HashMap();
        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        configuration.put(JAI.KEY_TILE_CACHE, tileCache);        
        tileCache.flush();

        // Instant de debut des calculs.         
        final long START_COMPUTATION = System.currentTimeMillis();
        
        // Zone couverte par l'image générée. 
        /* si <i>bound<i> vaut <i>null<i>, l'image générée couvrira une zone égale à 
           l'union des images participant au traitement. */
        Rectangle bound = null;
        if (SST_N_DAY_LIMITED_AREA == true) 
        {
            bound = new Rectangle((int)(SST_N_DAY_AREA.getX()/RESOLUTION), 
                                  (int)(-1*SST_N_DAY_AREA.getY()/RESOLUTION),
                                  (int)(SST_N_DAY_AREA.getWidth()/RESOLUTION),
                                  (int)(SST_N_DAY_AREA.getHeight()/RESOLUTION));
        }
        
        // Nom des fichiers créés. 
        final String name  = target.getParentFile().getPath() + File.separatorChar + 
                             target.getName().substring(0, target.getName().indexOf('.'));        
        final File fHeader      = new File(name + ".hdr"),          // header généré.
                   fHeaderInter = new File(name + "_inter.hdr"),    // header intermediaire.
                   fImageInter  = new File(name + "_inter.png"),    // Synthèse intermediaire. */        
                   fImageTemp   = new File(name + ".tmp");          // Synthèse temporaire 
        
        // Calcul de la synthèse SST.
        // Les images sont traitées deux par deux pour économiser de la mémoire. 
        boolean isFirstImage = true;        
        for (int i=0 ; i<sources.length ; i++)
        {
            final GridCoverage currentGridCoverage = Utilities.getGridCoverage(sources[i],
                                                                               "SST passage " + i,
                                                                               WGS84,
                                                                               SAMPLE_SST_INDEXED,
                                                                               configuration);
            GridCoverage[] arrayCoverage;            
            if (isFirstImage == true)
            {
                arrayCoverage    = new GridCoverage[1];
                arrayCoverage[0] = currentGridCoverage;
            }
            else
            {
                arrayCoverage    = new GridCoverage[2];
                arrayCoverage[0] = Utilities.getGridCoverage(fImageInter,
                                                             "SST passage " + i,
                                                             WGS84,
                                                             SAMPLE_SST_INDEXED,
                                                             configuration);
                arrayCoverage[1] = currentGridCoverage;
            }

            // Synthèse des GridCoverages. 
            GridCoverage sstSynthese = fr.ird.n1b.op.SSTSynthese.get(arrayCoverage,
                                                                bound, 
                                                                configuration);       
            
            // Ecriture dans un fichier temporaire de la synthèse. 
            final RenderedImage image = sstSynthese.geophysics(false).getRenderedImage();
            final String format = fImageInter.getName().substring(fImageInter.getName().indexOf('.') + 1);
            ImageIO.write(image, format, fImageTemp);                                  

            // Libération des ressources mémoires. 
            for (int j=0 ; j<arrayCoverage.length ; j++)
                arrayCoverage[j] = null;
            sstSynthese = null;
            System.gc();
            tileCache.flush();            
            isFirstImage = false;            
            
            // Renome le fichier temporaire en fichier Intermediaire.
            if (fImageInter.exists())
            {
                if (!fImageInter.delete())
                {
                    throw new IOException("Impossible d'effacer le fichier \"" + 
                                          fImageInter + "\"");
                }
            }
            if (!fImageTemp.renameTo(fImageInter))
            {
                throw new IOException("Impossible de renommer le fichier \"" + 
                                      fImageTemp  + "\" en \""  + 
                                      fImageInter + "\"");
            }
            
            // Création du fichier de localisation de l'image. 
            final BufferedWriter info = new BufferedWriter(new FileWriter(fHeaderInter));                                                                
            info.write("ORIGINE        \t" + image.getMinX()*RESOLUTION + "\t" + -1*image.getMinY()*RESOLUTION + "\n");
            info.write("RESOLUTION     \t" + RESOLUTION + "\t" + RESOLUTION + "\n\n\n\n");

            info.write("# RESUME DU TRAITEMENT SST DAY\n");            
            for (int j=0 ; j<sources.length ; j++)
                info.write("PROCESSING FILE \t" + "\"" + sources[j].getPath() + "\"\n");                        
            info.write("PROCESSING     \t" + ((System.currentTimeMillis() 
                                         - START_COMPUTATION)/1000.0) + " secondes.\n\n");                        
            info.close();                    
        }
        
        // Renomme les derniers fichiers intermédiaires en fichier finaux.
        fHeaderInter.renameTo(fHeader);                     
        if (!fImageInter.renameTo(target))
        {
            // Code nécéssaire à cause d'un bug
            target.delete();
            fImageInter.renameTo(target);
        }
    }
    
    /**
     * Ce programme calcul la synthèse des S.S.T.. 
     *
     * @param args[0]    Fichier de destination.
     * @param args[1]    Premier fichier source.
     * @param args[2]    Second fichier source.
     * @param args[3]    ....
     */
    public static void main(final String[] args) 
    {
        final int count = args.length;
        
        if (count <2) 
        {
            System.err.println("SSTSynthese TARGET IMAGE1 IMAG2 IMAGE3 ...");                    
            System.err.println("TARGET      --> Fichier de destination");
            System.err.println("IMAGE1      --> Premier fichier source");
            System.err.println("IMAGE2      --> Second fichier source");
            System.err.println(".... ");
            System.err.println("IMAGEN      --> Nième fichier source.");
            System.exit(-1);        
        }
        try 
        {
            final File[] array = new File[args.length-1];
            for (int i=1 ; i<args.length ; i++)
                array[i-1] = new File(args[i]);
            
            final SSTSynthese sst = new SSTSynthese();
            sst.compute(array, new File(args[0]));
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
    }
}