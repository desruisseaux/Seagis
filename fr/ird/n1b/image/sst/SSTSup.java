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
import fr.ird.io.text.ParseStat;
import fr.ird.io.text.ParseHeader;
import fr.ird.io.text.ParseHeader;
import fr.ird.n1b.util.StatisticGrid;
import fr.ird.n1b.image.sst.Utilities;
import fr.ird.n1b.io.Bulletin;
import fr.ird.n1b.io.Metadata;
import fr.ird.n1b.io.Satellite;
import fr.ird.n1b.io.FilenameFilterSST;

// J2SE / JAI.
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.FieldPosition;
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
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import javax.media.jai.JAI;
import javax.imageio.ImageIO;
import javax.media.jai.TileCache;
import javax.media.jai.util.Range;
import javax.media.jai.PlanarImage;
import javax.imageio.ImageReadParam;
import javax.media.jai.ParameterList;
import java.awt.image.renderable.ParameterBlock;
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
 * Calcul le SUP sur des images S.S.T. appartenant à une même journèe d'acquisition. 
 * Elle génère une image contenant le SUP des images S.S.T. ainsi qu'un fichier 
 * <i>header</i> contenant des informations sur l'image et le traitement réalisé.<BR><BR>
 *
 * L'objectif de cette image S.S.T. est de couvrir une zone plus importante par l'association 
 * des différents passages et aussi d'avoir un maximum d'informations pertinentes sur 
 * l'image produite (c'est à dire essayer lorsque cela est possible de remplacer les pixels 
 * nuageux par des pixels température par exemple).
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SSTSup
{
    /** Système de coordonnée des images S.S.T.. */  
    private final CoordinateSystem WGS84 = Utilities.WGS84;

    /** Sample de l'image S.S.T. décrivant les valeurs physiques. */ 
    private final SampleDimension[] SAMPLE_SST_GEOPHYSIC = Utilities.SAMPLE_SST_GEOPHYSIC;
    
    /** Sample de l'image S.S.T. décrivant les valeurs sous forme indexés. */ 
    private final SampleDimension[] SAMPLE_SST_INDEXED = Utilities.SAMPLE_SST_INDEXED;
    
    /** Résolution de l'image de sortie. */
    private final double RESOLUTION = Utilities.RESOLUTION; 

    /** Taille en Mo du cache JAI. */
    private final long JAI_MEMORY_CACHE;
    
    /** Taille en largeur d'une tuile de l'image. */
    private final int TILE_W;
        
    /** Taille en hauteur d'une tuile de l'image. */    
    private final int TILE_H;
    
    /** Heure de début du jour J. Le Sup sera réalisé sur des fichiers appartenant au jour J. */
    private final long START_TIME_SST_DAY;   

    /* Zone de couverture de l'image générée. */
    private final Rectangle2D SST_ONE_DAY_AREA;
    
    /** 
     * Indique si la zone definie ci-dessus doit etre la taille de la zone de sortie 
     * (SST_ONE_DAY_LIMITED_AREA vaut alors true) ou si la taille de la zone de sortie doit etre
     * la plus vaste possible.
     */
    private final boolean SST_ONE_DAY_LIMITED_AREA;
    
    /**
     * Retourne la liste des fichiers participant aux traitements. Ces fichiers 
     * sont désignés par leur nom.
     *
     * @param directory     Répertoire contenant les fichiers à traiter.
     * @param start         Date de début de l'acquisition.
     * @param end           Date de fin de l'acquisition.
     * @return un tableau contenant la liste des fichiers à traiter.
     */
    private static File[] getFileToProcess(final File directory,
                                           final Date start,
                                           final Date end)
    {
        final FilenameFilterSST filter = new FilenameFilterSST(start, end);
        final File[] array = directory.listFiles(filter);
        return array;
    }
    
    /**
     * Retourne le nom du fichier généré.<BR>
     * Format : sstsupYYYYJJJJ.png  <BR>
     * <i>Exemple : "sstsup2003017.png"</i><BR>
     *
     * @param directory     Répertoire de destination.
     * @param start         Date de début de l'acquisition.
     * @return le nom du fichier à généré.
     */
    private static String getOutputName(final String directory,
                                        final Date   start) 
    {
        /////////////////////////////////
        // Nom des fichiers de sortie. //
        // format  : SST_DAY_YYYYMMDD  //
        // exemple : SST_DAY_20030116  //
        /////////////////////////////////
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(start);
        StringBuffer buffer = new StringBuffer(directory + File.separatorChar + "sstsup");
        final int year  = calendar.get(Calendar.YEAR),
                  month = calendar.get(Calendar.MONTH) + 1,
                  day   = calendar.get(Calendar.DAY_OF_MONTH);        
        final DateFormat dateFormat  = new SimpleDateFormat("yyyyDDD",   Locale.FRANCE);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.format(start, buffer, new FieldPosition(0));        
        return (buffer.toString() + ".png");
    }
    
    /**
     * Retourne les statistiques associées à une image. Ces statistiques sont stockées dans 
     * un fichier.
     *
     * @param name  Nom du fichier de statistiques.
     * @return Ces statistiques sont stockées dans un fichier.
     */
    private StatisticGrid loadStatistics(final File file) throws IOException
    {
        if (file.exists())
        {
            final ParameterList parameter = ParseStat.getInputDefaultParameterList();
            parameter.setParameter(ParseStat.FILE, file);
            return (StatisticGrid)ParseStat.parse(parameter).getObjectParameter(ParseStat.STAT);
        }    
        System.err.println("Impossible de trouver le fichier \"" + file.getPath() + "\".");
        return null;        
    }
    
    /**
     * Construit un objet SSTSup.
     */
    public SSTSup() throws IOException
    {   
        // Extraction des parametres de configuration de la SST. 
        final ParameterList param = ParseSST.parse(ParseSST.getInputDefaultParameterList());
        JAI_MEMORY_CACHE   = param.getIntParameter(ParseSST.JAI_MEMORY_CACHE);
        TILE_W             = param.getIntParameter(ParseSST.JAI_TILE_WIDTH);
        TILE_H             = param.getIntParameter(ParseSST.JAI_TILE_HEIGHT);                
        START_TIME_SST_DAY = param.getLongParameter(ParseSST.START_TIME_SST_DAY);
        SST_ONE_DAY_LIMITED_AREA = param.getBooleanParameter(ParseSST.SST_ONE_DAY_LIMITED_AREA);
        SST_ONE_DAY_AREA         = (Rectangle2D)param.getObjectParameter(ParseSST.SST_ONE_DAY_AREA);

        // Configuration de JAI.
        final JAI jai = JAI.getDefaultInstance();
        final TileCache tileCache = jai.getTileCache();
        tileCache.setMemoryCapacity(JAI_MEMORY_CACHE*1024*1024);
        jai.setDefaultTileSize(new Dimension(TILE_W, TILE_H));            
    }
    
    /**
     * Calcul le SUP des S.S.T. sur une journée depuis un ensemble de S.S.T.. 
     *
     * @param sources         Fichiers sources.
     * @param taregt          Fichier de destination.
     */
    public void compute(final File[] sources,
                        final File   target) throws IOException
    {
        if (target == null)
            throw new IllegalArgumentException("Target file is null.");
        if (target.isDirectory())
            throw new IllegalArgumentException("Target is a directory.");
        
        // Paramètre du JAI. 
        final Map configuration   = new HashMap();
        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        configuration.put(JAI.KEY_TILE_CACHE, tileCache);        
        tileCache.flush();

        // Instant de debut des calculs. 
        final long START_COMPUTATION = System.currentTimeMillis();
        
        // Nom des fichiers créés.
        final String name       = target.getPath().substring(0, target.getPath().length()-4);
        final File fImage       = new File(name + ".png"),       // Image générée
                   fHeader      = new File(name + ".hdr"),       // Header
                   fStatTxt     = new File(name + ".msk.sta"),   // Statistiques
                   fHeaderInter = new File(name + "_inter.hdr"), // Header intermediaire
                   fImageInter  = new File(name + "_inter.png"), // Image générée intermediaire
                   fImageTemp   = new File(name + ".tmp");       // fichier temporaire
                        
        // Extraction des images participant au SUP.
        final int length   = sources.length;        
        if (length == 0)
        {
            final BufferedWriter info = new BufferedWriter(new FileWriter(fHeader));                                                                
            info.write("# Aucun fichier a traiter.");
            info.close();
            return;
        }

        // Zone couverte par l'image générée.
        /* Si bound vaut <i>null</i>, l'image couvrira une zone égale à l'union des images 
           participant au SUP. */
        Rectangle bound = null;
        if (SST_ONE_DAY_LIMITED_AREA == true) 
        {
            bound = new Rectangle((int)(SST_ONE_DAY_AREA.getX()/RESOLUTION), 
                                  (int)(-1*SST_ONE_DAY_AREA.getY()/RESOLUTION),
                                  (int)(SST_ONE_DAY_AREA.getWidth()/RESOLUTION),
                                  (int)(SST_ONE_DAY_AREA.getHeight()/RESOLUTION));
        }       
                        
        // Fusion des fichiers de statistique.
        boolean isFirstStat = true;
        StatisticGrid stat = null;
        for (int i=0 ; i<length ; i++)
        {
            final File fStat = new File(sources[i].getPath().substring(0, sources[i].getPath().indexOf('.')) + 
                                        ".msk.sta");
            
            // On passe les fichiers inexistant ou interdit en lecture.
            if (!fStat.exists() || !fStat.canRead())
                continue;
            
            final StatisticGrid stat_ = loadStatistics(fStat);            
            if (isFirstStat == true)
                stat = stat_;
            else
                stat = stat.union(stat_);            
            isFirstStat = false;
        }
        
        // Ecriture du fichier de statistique.
        if (stat != null)
            Utilities.writeStat(stat, fStatTxt);        
        
        // Traitement de l'ensemble des images SST des passages. 
        // Les images sont traitées deux par deux pour économiser de la mémoire. 
        boolean isFirstImage = true;
        for (int i=0 ; i<length ; i++)
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

            // SUP des GridCoverages. 
            GridCoverage sstSUP = fr.ird.n1b.op.SSTSup.get(arrayCoverage, bound, configuration);       
            
            // Ecriture dans un fichier temporaire du SUP. 
            final RenderedImage image = sstSUP.geophysics(false).getRenderedImage();
            final String format = fImageInter.getName().substring(fImageInter.getName().indexOf('.') + 1);
            ImageIO.write(image, format, fImageTemp);                                  

            // Libération des ressources mémoires. 
            for (int j=0 ; j<arrayCoverage.length ; j++)
                arrayCoverage[j] = null;
            sstSUP = null;
            tileCache.flush();            
            isFirstImage = false;            
            
            // Renome le fichier temporaire du SUP en fichier Intermediaire.
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
                                      fImageTemp  + 
                                      "\" en \""  + 
                                      fImageInter + "\"");
            }
            
            // Création du fichier d'information intermédiaire.
            final BufferedWriter info = new BufferedWriter(new FileWriter(fHeaderInter));                                                                
            info.write("ORIGINE        \t" + image.getMinX()*RESOLUTION + "\t" + 
                                          -1*image.getMinY()*RESOLUTION + "\n");
            info.write("RESOLUTION     \t" + RESOLUTION + "\t" + RESOLUTION + "\n\n\n\n");
            info.write("# RESUME DU TRAITEMENT SST DAY\n");            
            for (int j=0 ; j<length; j++)
                info.write("PROCESSING FILE \t" + "\"" + sources[j].getPath() + "\"\n");                        
            info.write("PROCESSING     \t" + ((System.currentTimeMillis() 
                                            - START_COMPUTATION)/1000.0) + " secondes.\n\n");                        
            info.close();                        
        }
                
        // Renomme les derniers fichiers intermédiaire en fichiers finaux.
        fImageInter.renameTo(fImage);
        fHeaderInter.renameTo(fHeader);        
    }
    
    
    /**
     * Ce programme lance la synthèse SUP d'images SST. <BR><BR>
     * 
     * Deux traitements peuvent être lancés : 
     * <UL>
     *  <LI>Un traitement semi-automatisé permettant de calculer le SUP sur des images SST
     *      acquise le jour J. Dans ce traitement, les fichiers participants au traitement
     *      sont déterminés par le programme en fonction de la date de début d'acquisition
     *      des données.</LI>
     *  <LI>Un traitement plus génréique permettant de lancer le traitement SUP sur des
     *      fichiers donnés en paramètres.</LI>
     * </UL><BR><BR>
     *
     * @param args[0]    La chaîne "-auto".
     * @param args[1]    Répertoire source des SST par passage.
     * @param args[2]    Répertoire de destination du fichier SST.
     * @param args[3]    La date de la synthèse à calculer (YYYYJJJ).
     *
     * Les paramètres du traitement semi-automatisé sont : 
     * <UL>
     *  <LI>Le répertoire contenant les fichiers SST.<BR>
     *      <i>Exemple : "C:/Partages/SST/Test/SST"</i></LI>
     *  <LI>Le répertoire de destination de la synthèse SUP.<BR>
     *      <i>Exemple : "C:/Partages/SST/Test/SST_1_DAY"</i></LI>
     *  <LI>La date julienne de la synthese à calculer (La date est de la forme YYYYJJJ).
     *  <BR><i>Exemple : 2003017</i></LI>
     * </UL><BR><BR>
     * 
     * <i>Exemple : ComputeSSTDay "C:/Partages/SST/Test/SST"  
     *                            "C:/Partages/SST/Test/SST_1_DAY"  
     *                            2003017</i><BR><BR>
     *
     * Les paramètres du traitement générique :
     * <UL>
     *  <LI>Le nom du fichier de destination</LI>
     *      <i>Exemple : "C:/Partages/SST/DESTINATION/SST_SUP.png"</i></LI>
     *  <LI>Le nom du premier fichier participant aux traitement.</LI>
     *      <i>Exemple : "C:/Partages/SST/SOURCE/SST_1.png"</i></LI>
     *  <LI>Le nom du second fichier participant aux traitement.</LI>
     *      <i>Exemple : "C:/Partages/SST/SOURCE/SST_2.png"</i></LI>
     *  <LI>etc.....</LI>
     * </UL>
     *           
     * @param args[0]    Nom du fichier de destination.
     * @param args[1]    Nom du premier fichier SST participant au traitement.
     * @param args[2]    Nom du second fichier SST participant au traitement.
     * @param args[3]    Nom du troisième fichier SST participant au traitement.
     */
    public static void main(final String[] args) 
    {
        final int count = args.length;
        
        if (count == 0)
        {
            System.err.println("SSTSup -auto DIRECTORY_SRC DIRECTORY_TGT YYYYJJJ");                    
            System.err.println("SSTSup FILE_TGT FILE_SRC1 FILE_SRC2 FILESRC3 ...");                    
            System.exit(-1);
        }
        try 
        {
            final SSTSup sst = new SSTSup();        
            boolean processSemiAuto = false;        
            if (args[0].trim().equals("-auto"))
            {
                // Lancement du traitement semiAutomatique.
                if (count != 4)
                {
                    System.err.println("SSTSup -auto DIRECTORY_SRC DIRECTORY_TGT YYYYJJJ");                    
                    System.err.println("Exemple : SSTSup -auto \"c:\\SST\\SOURCE\" " + 
                                       "\"c:\\SST\\TARGET\" 2003017");
                    System.exit(-1);
                }
                final File directorySrc = new File(args[1]);
                final String julianDate = args[3].trim();            
                final int year          = Integer.parseInt(julianDate.substring(0, 4)),
                          dayOfYear     = Integer.parseInt(julianDate.substring(4, 7));
                final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                calendar.clear();            
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);            

                // Période d'intérêt. Toutes les fichiers N1B ayant commencé leur acquisition
                // dans la période délimité par "start" et "end" participeront au traitement.
                final Date start = new Date(calendar.getTimeInMillis() + 
                                            sst.START_TIME_SST_DAY),
                           end   = new Date(calendar.getTimeInMillis() + 
                                            sst.START_TIME_SST_DAY     + 
                                            24*3600*1000);            

                // Nom du fichier de sortie.
                final File target = new File(getOutputName(args[2], start));

                // Fichier à traiter.
                final File[] file    = getFileToProcess(directorySrc, start, end);            

                // Lancement du traitement.
                sst.compute(file, target);
            }
            else
            {
                // Lancement du traitement manuel.
                if (count == 1)
                {
                    System.err.println("SSTSup FILE_TGT FILE_SRC1 FILE_SRC2 FILESRC3 ...");                    
                    System.err.println("Exemple : SSTSup \"c:\\SST\\TARGET\\SST.png" + 
                                       "\"c:\\SST\\SOURCE\\SST1.png" + 
                                       "\"c:\\SST\\SOURCE\\SST1.png 2003017");
                    System.exit(-1);
                }               
                final File target = new File(args[0]);
                final File[] file = new File[args.length-1];
                for (int i=0; i<(args.length-1) ; i++)
                {
                    file[i] = new File(args[i+1]);
                }
                
                // Lancement du traitement.
                sst.compute(file, target);
            }
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
    }
}