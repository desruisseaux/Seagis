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

// J2SE / JAI
import java.io.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.Locale;
import java.util.Iterator;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.geom.AffineTransform;
import javax.media.jai.util.Range;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;

// SEAGIS
import fr.ird.io.text.ParseHeader;
import fr.ird.io.text.ParsePalette;
import fr.ird.io.map.IsolineReader;
import fr.ird.io.map.GEBCOReader;
import fr.ird.n1b.util.StatisticGrid;

// GEOTOOLS
import org.geotools.cv.Category;
import org.geotools.cs.AxisInfo;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.Envelope;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cv.SampleDimension;
import org.geotools.ct.MathTransform1D;
import org.geotools.ct.MathTransform2D;
import org.geotools.cs.AxisOrientation;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransformFactory;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.renderer.geom.Isoline;
import org.geotools.util.ProgressListener;
import org.geotools.renderer.j2d.Renderer;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedIsoline;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.renderer.j2d.Hints;

/**
 * Fournie des méthodes et variables statiques utiles.
 *
 * @author  Remi Eve
 * @version $Id$
 */
public final class Utilities 
{   
    /** Chemin des palettes S.S.T. et Matrice. */    
    private static final String PATH_MATRIX = "application-data/configuration/PaletteMatrix.txt",
                                PATH_SST    = "application-data/configuration/PaletteSST.txt";

    /** Résolution des images S.S.T. en degré. */
    public static final double RESOLUTION = 0.01;
    
    /** Système de coordonnées des images S.S.T. */
    public static final CoordinateSystem WGS84 = getCoordinateSystem();
    
    /** Sample de l'image S.S.T. décrivant les valeurs physiques. */
    public static final SampleDimension[] SAMPLE_SST_GEOPHYSIC = getPalette(PATH_SST, 
                                                                            true);
    
    /** Sample de l'image S.S.T. décrivant les valeurs indexées. */
    public static final SampleDimension[] SAMPLE_SST_INDEXED = getPalette(PATH_SST, 
                                                                          false);    
    
    /** Sample de l'image Matrice décrivant les valeurs indexées. */
    public static final SampleDimension[] SAMPLE_MATRIX_INDEXED = getPalette(PATH_MATRIX, 
                                                                             false);    

    /** Sample de l'image Matrice décrivant les valeurs physiques. */ 
    public static final SampleDimension[] SAMPLE_MATRIX_GEOPHYSIC = getPalette(PATH_MATRIX,
                                                                               true);    

    /** Identifiant des différentes catégories constituant la palette SST. */
    public static final String LAND_BACKGROUND = "Terre",
                               LAND_CONTOUR    = "Trait de côte",
                               CLOUD           = "Nuage",
                               TEMPERATURE     = "Température de surface",
                               NO_DATA         = "Absence de données",
                               MATRIX          = "Matrice jour nuit";
    
    /**
     * Retourne le <CODE>SampleDimension</CODE> définissant les catégories de la matrice.
     *
     * @param path         Chemin de la palette.
     * @param geophysic    <i>true</i> retourne les valeurs physiques, <i>false</i> retourne 
     *                     les valeurs indexées.
     * @return le <CODE>SampleDimension</CODE> définissant les catégories de la matrice.
     */
    private static SampleDimension[] getPalette(final String  path,  
                                                final boolean geophysic) 
    {
        try 
        {          
            final ParameterList parameter = ParsePalette.getInputDefaultParameterList();
            parameter.setParameter(ParsePalette.FILE, 
                new File(ParsePalette.class.getClassLoader().getResource(path).getPath()));            
            final ParameterList parameterOut = ParsePalette.parse(parameter);
            final SampleDimension[] sample = (SampleDimension[])parameterOut.getObjectParameter(ParsePalette.SAMPLE_DIMENSION);
            for (int i=0 ; i<sample.length ; i++)
                sample[i] = sample[i].geophysics(geophysic);
            return sample;
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }   

    /**
     * Retourne le système de coordonnées dans lequel seront projetées les images SST.
     * Ce système est le systeme WGS84 dont l'axe des ordonnèes va en direction du SUD.
     * @return le système de coordonnées dans lequel seront projetées les images SST.
     */
    private static CoordinateSystem getCoordinateSystem() 
    {
        return new GeographicCoordinateSystem("WGS84", 
                                              GeographicCoordinateSystem.WGS84.getUnits(0),
                                              GeographicCoordinateSystem.WGS84.getHorizontalDatum(),
                                              GeographicCoordinateSystem.WGS84.getPrimeMeridian(),
                                              GeographicCoordinateSystem.WGS84.getAxis(0),
                                              new AxisInfo("Latitude", AxisOrientation.SOUTH));  
    }
    
    /**
     * Dans la chaîne de traitement S.S.T., lors du calcul des <i>SST par passage(SST)</i>, 
     * des <i>SST journalières (SSTSup)</i> ou des <i>synthèses SST (SSTSynthese)</i>, une 
     * image est générée ainsi qu'un fichier <i>header</i> contenant des informations de 
     * localisation géographique de l'image. Cette méthode permet à partir du nom de l'image 
     * S.S.T. de générer un <CODE>GridCoverage</CODE> correctement localiser.
     *
     * @param file      L'image.
     * @param name      Nom du <CODE>GridCoverage</CODE>.
     * @param cs        Le systeme de coordonnées.
     * @param sample    Le sample définissant la palette de couleur ainsi que les données 
     *                  de l'image.
     *
     * @return un <CODE>GridCoverage</CODE> de l'image.
     */
    public static GridCoverage getGridCoverage(final File              file,
                                               final String            name,
                                               final CoordinateSystem  cs,
                                               final SampleDimension[] sample,
                                               final Map configuration) throws IOException
    {        
        // Lecture en tuile.
        ParameterBlock block = (new ParameterBlock()).add(file);
        block = block.add(0);    // undefine
        block = block.add(new Boolean(true)); // metadata
        block = block.add(new Boolean(true)); // thumbnails
        block = block.add(new Boolean(true)); // verify input
        block = block.add(null); // listener
        block = block.add(null); // locale
        block = block.add(null); // ReadParam
        block = block.add(null); // null            
        ImageIO.setUseCache(true);
        final RenderedImage image = JAI.create("ImageRead", block); 

        final int width  = image.getWidth(),
                  height = image.getHeight();

        /* Pour chaque image, il est aussi necessaire de charger le fichier d'information 
           associé. Ce fichier contient des informations permettant de connaitre la 
           situation géographique de la zone représentée par l'image. */
        final StringBuffer buffer = new StringBuffer();
        buffer.append(file.getParentFile().getPath() + File.separatorChar + file.getName());        
        final String header  = buffer.substring(0, buffer.length()-3).toString() + "hdr";                                
        
        // Extraction des informations du fichier <i>header</i>.
        final ParameterList paramHeaderIn  = ParseHeader.getInputDefaultParameterList();
        paramHeaderIn.setParameter(ParseHeader.FILE, new File(header));
        final ParameterList paramHeaderOut = ParseHeader.parse(paramHeaderIn);
        final Point2D     ORIGINE = (Point2D)paramHeaderOut.getObjectParameter(ParseHeader.ORIGINE);
        final double[] RESOLUTION_ = (double[])paramHeaderOut.getObjectParameter(ParseHeader.RESOLUTION);                                                

        if (RESOLUTION_[0]!=RESOLUTION || RESOLUTION_[1]!=RESOLUTION)
            throw new IllegalArgumentException("Resolution incorrect.");
        
        /* Calcul la transformation permettant de passer de l'image au système géographique  
           en fonction de la résolution. */        
        final AffineTransform at        = new AffineTransform(RESOLUTION_[0], 0, 
                                                              0, RESOLUTION_[1], 
                                                              0, 0);
        final MathTransform2D transform = MathTransformFactory.getDefault().createAffineTransform(at);

        // Translation du coin haut gauche de l'image de coordonnee (0,0) a l'origine.
        final CoordinatePoint ptOrigine = new CoordinatePoint(ORIGINE.getX(), ORIGINE.getY());
        try 
        {                
            transform.inverse().transform(ptOrigine, ptOrigine);                
        }
        catch (TransformException r)
        {
            throw new IllegalArgumentException(r.getMessage());
        }
        final float transX = (float)(ptOrigine.getOrdinate(0)),
                    transY = (float)(ptOrigine.getOrdinate(1)*-1);
        ParameterBlock block1 = new ParameterBlock();
        block1 = block1.addSource(image);
        block1 = block1.add(transX);
        block1 = block1.add(transY);        

        // Construction du gridCoverage.
        final double[] minCP = {ptOrigine.getOrdinate(0),         
                                ptOrigine.getOrdinate(1) - height},
                       maxCP = {ptOrigine.getOrdinate(0) + width, 
                                ptOrigine.getOrdinate(1)};
        final Envelope envelope = new Envelope(minCP, maxCP);        
        return (new GridCoverage(name, 
                                 JAI.create("Translate", block1).createInstance(), 
                                 cs,                                     
                                 envelope,
                                 sample,
                                 (GridCoverage[])null,
                                 (Map)null));         
    }    
    
    /**
     * Retourne la catégorie nommée <CODE>name</CODE> appartenant au <CODE>sample</CODE>.
     *
     * @param sample  Un tableau de SampleDimension.
     * @param name    Nom de la catégorie désirée.
     * @return la catégorie nommée <CODE>name</CODE> appartenant au <CODE>sample</CODE>. 
     *         si aucune catégorie n'est trouvée, <i>null</i> est retournée.
     */
    public static Category getCategory(final SampleDimension[] sample,
                                       final String            name) 
    {
        if (sample == null)
            throw new IllegalArgumentException("Sample is null.");
        for (int i=0 ; i<sample.length ; i++)
            if (sample[i] == null)
                throw new IllegalArgumentException("Categorie of index " + i + " is null.");
        if (name == null)
            throw new IllegalArgumentException("Name of categorie is null.");
        
        for (int i=0 ; i<sample.length ; i++)
        {
            final Iterator iterator = sample[i].getCategories().iterator();
            while (iterator.hasNext())
            {
                final Category category = (Category)iterator.next();
                if (category.getName(Locale.FRENCH).equals(name))
                    return category;
            }
        }        
        return null;
    }
    
    /**
     * Genere une image du gridCoverage.
     *
     * @param coverage    Le gridCoverage source.
     * @param format      Format de l'image "png", "gif", ...
     * @apram filename    Filename du fichier de sortie.
     */
/*    public static final void writeImage(final GridCoverage coverage, 
                                        final String format, 
                                        final String filename) throws IOException
    {
        if (format == "raw")
            writeFloatingImageRaw(coverage, filename);
        else
        ImageIO.write(coverage.geophysics(false).getRenderedImage(), format, new File(filename));        
    }*/

    /**
     * Genere une image du gridCoverage au format floating raw.
     *
     * @param coverage    Le gridCoverage source.
     * @apram filename    Filename du fichier de sortie.
     */
    /*private static final void writeFloatingImageRaw(final GridCoverage coverage, 
                                                    final String filename) throws IOException
    {
        final Raster raster = coverage.geophysics(true).getRenderedImage().getData();
        final int width  = raster.getWidth(),
                  height = raster.getHeight();
        
        final FileImageOutputStream out = new FileImageOutputStream(new File(filename));
        final float[] array = new float[width];
        for (int h=0 ; h<height ; h++)
        {
            for (int w=0 ; w<width ; w++)
                array[w] = (float)raster.getSampleDouble(w, h, 0);            
            out.writeFloats(array, 0, array.length);                
        }    
        out.close();
    }*/
    
    /**
     * Sérialise l'isoline <CODE>in</CODE> dans le fichier <CODE>out</CODE> en réalisant 
     * éventuellement l'assemblage.
     *
     * @param in        Fichier contenant le trait de côte au format GEBCO.
     * @param out       Fichier sérialisé.
     * @param assemble  <i>true</i> pour que le trait de côte soit rempli, <i>false</i>
                       sinon.
     * @param listener  Un listener ou null.
     */
    public static void serializeIsolineGebco(final File             in, 
                                             final File             out, 
                                             final boolean          assemble, 
                                             final ProgressListener listener)
    {
        try
        {           
            Isoline isoline = null;
            if (in.exists())
            {
                final IsolineReader isolineReader = new GEBCOReader();
                isolineReader.setInput(in);                                         
                Isoline isoline_0    = new Isoline(isolineReader.read(0)),
                        isoline_1500 = new Isoline(isolineReader.read(1500));

                if (assemble)
                {
                    final Isoline[] array = {isoline_0, isoline_1500};
                    final float[]   bary  = {0};
                    Isoline.assemble(array, listener);
                    isoline = array[0];
                }
                else
                {
                    isoline = isoline_0;
                }
                final ObjectOutputStream out_ = new ObjectOutputStream(new FileOutputStream(out));
                out_.writeObject(isoline); 
                out_.close();                
            }
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (TransformException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }     
    
    /**
     * Ecrit l'image <CODE>source</CODE> sur le disque en passant par un fichier temporaire.
     *
     * @param source    L'image.
     * @param target    Le fichier de destination.
     * @param tmp       Le fichier temporaire.
     */
    public static void writeImage(final RenderedImage source,
                                  final File          target,
                                  final File          tmp) throws IOException                                  
    {   
        // Extraction du format de l'image.
        String format = target.getName().substring(target.getName().indexOf('.') + 1);
        while (format.indexOf('.') != -1)
            format = format.substring(format.indexOf('.') + 1);        
        
        ImageIO.write(source, format, tmp);                      
        if (target.exists())
        {
            if (!target.delete())
            {
                throw new IOException("Impossible d'ecraser le fichier \"" + 
                                      target + 
                                      "\" par le fichier \"" + 
                                      tmp + "\"");
            }
        }
        if (!tmp.renameTo(target))
        {
            throw new IOException("Impossible de renommer le fichier \"" + 
                                  tmp + 
                                  "\" en \"" + 
                                  target + "\"");
        }
    }    
    
    /**
     * Affiche l'image à des fins de tests.
     *
     * @param image  Image à afficher.
     */
    public static void show(final RenderedImage image)
    {
        final Frame frame = new Frame("Test");
        frame.add(new javax.media.jai.widget.ScrollingImagePanel(image, 400, 400));
        frame.pack();
        frame.show();
    }
    
    /**
     * Génère un fichier texte contenant les statistiques.
     *
     * @param stat  Les statistiques.
     * @param file  Fichier de destination.
     */
    public static void writeStat(final StatisticGrid  grid,
                                 final File           file) throws IOException
    {
        final BufferedWriter fStatTxt = new BufferedWriter(new FileWriter(file));                                                                                        
        for (int i=0 ; i<grid.getHeight() ; i++)
        {
            final double latitude = grid.getLatitude(i);
            fStatTxt.write((double)((Math.round(latitude*100.0))/100.0)                 + "\t\t");
            fStatTxt.write((double)((int)(grid.getAvgTempOfDay(i)*100.0))/100.0         + "\t\t");
            fStatTxt.write((double)((int)(grid.getEcartTypeTempOfDay(i)*100.0))/100.0   + "\t\t"); 
            fStatTxt.write(grid.getCountDay(i)                                          + "\t\t"); 
            fStatTxt.write((double)((int)(grid.getAvgTempOfNight(i)*100.0))/100.0       + "\t\t"); 
            fStatTxt.write((double)((int)(grid.getEcartTypeTempOfNight(i)*100.0))/100.0 + "\t\t"); 
            fStatTxt.write(grid.getCountNight(i)                                        + "\n");
        }   
        fStatTxt.close();        
    }
    
    /**
     * Copie d'un fichier.
     *
     * @param src   Fichier source.
     * @param tgt   Fichier destination.
     */
    public static void copy(final File src, final File tgt) throws IOException
    {
        final DataInputStream   in = new DataInputStream(new FileInputStream(src));
        final DataOutputStream out = new DataOutputStream(new FileOutputStream(tgt));
        while (in.available() > 0)
            out.write(in.read());
        in.close();
        out.close();
    }    
    
    /**
     * Retourne un <CODE>GridCoverage</CODE> auquel a été superposé l'isoline.
     *
     * @param source      L'image source.
     * @param isoline     L'isoline contenant le trait de côte.
     * @param colBg       La couleur de fond du masque.
     * @param colContour  La couleur du trait de côte.
     * @return un <CODE>GridCoverage</CODE> auquel a été superposé l'isoline.
     */
    public static GridCoverage addLayer(final GridCoverage      source, 
                                        final Isoline           isoline,
                                        final Color             colBg,
                                        final Color             colContour)
    {        
        // Pas de trait de la côte disponible. 
        if (isoline == null)
            throw new IllegalArgumentException("Isoline is null.");

        final RenderedImage imgSource = source.getRenderedImage();
        final Point2D origine    = new Point2D.Double(imgSource.getMinX(), imgSource.getMinY());
        final AffineTransform at = new AffineTransform(1/RESOLUTION, 
                                                       0, 
                                                       0,            
                                                       1/RESOLUTION, 
                                                       imgSource.getMinX()*-1, 
                                                       imgSource.getMinY()*-1);        
        final Rectangle bound    = new Rectangle(0, 
                                                 0, 
                                                 imgSource.getWidth(), 
                                                 imgSource.getHeight());            
        BufferedImage image      = PlanarImage.wrapRenderedImage(imgSource).getAsBufferedImage();
        final Graphics2D graphic = image.createGraphics();                       
        graphic.setClip((int)bound.getX(), 
                        (int)bound.getY(), 
                        (int)bound.getWidth(), 
                        (int)bound.getHeight());        
            
        try 
        {
            final RenderedIsoline layerLine = new RenderedIsoline(isoline);
            layerLine.setContour(colContour);
            layerLine.setForeground(colBg);
            layerLine.setBackground(colBg);

            final Renderer renderer = new Renderer(null);                
            renderer.setCoordinateSystem(source.getCoordinateSystem());            
            renderer.addLayer(layerLine);
            renderer.paint(graphic, at, bound);            
            final float transX = (float)(origine.getX()),
                        transY = (float)(origine.getY());
            ParameterBlock block = (new ParameterBlock()).addSource(image).add(transX).add(transY);
            
            return new GridCoverage(source.getName(Locale.FRENCH),
                                    JAI.create("Translate", block).createInstance(),
                                    source.getCoordinateSystem(),
                                    source.getEnvelope(),
                                    source.getSampleDimensions(),
                                    null,
                                    null);
        }
        catch (TransformException e)
        {
            System.err.println(e.getMessage());
            return source;
        }                 
     }       
    
    /**
     * Retourne un <CODE>Isoline</CODE>. 
     *
     * @param name  Nom du fichier sérializé contenant l'<CODE>Isoline</CODE>.
     * @return un <CODE>Isoline</CODE> contenant l'isoline. Retourne <i>null</i> si 
     *         l'<CODE>Isoline</CODE> n'a pas été trouvé ou si un problème est survenu 
     *         en cours d'extraction.
     */
    public static Isoline loadSerializedIsoline(final String name)
    {
        final File file = new File(name);
        if (file.exists())
        {
            try 
            {
                final ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                final Isoline isoline      = (Isoline) in.readObject();                                                 
                in.close();                
                return isoline;
            }
            catch (FileNotFoundException e)                    
            {
                System.err.println("Impossible d'ouvrir le fichier sérialisé.");                       
                return null;
            }
            catch (IOException e)                    
            {
                System.err.println("Erreur d'ouverture du fichier sérialisé.");                       
                return null;
            }
            catch (ClassNotFoundException e)
            {                    
                System.err.println("Impossible de désérialiser le fichier.");
                return null;
            }
        }    
        System.err.println("Impossible de trouver le fichier \"" + name + "\".");
        return null;
    }    
}