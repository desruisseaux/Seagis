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

// J2SE
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.io.IOException;
import java.awt.image.RenderedImage;

import javax.media.jai.TileCache;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.ParameterList;
import javax.imageio.ImageIO;

// SEAGIS
import fr.ird.n1b.op.MaxFilter;
import fr.ird.io.text.ParseSST;
import fr.ird.n1b.image.sst.Utilities;

// GEOTOOLS
import org.geotools.gc.GridCoverage;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.cv.SampleDimension;
import org.geotools.pt.Envelope;


/**
 * Applique un lissage des temp�ratures sur une image S.S.T.. Ce traitement utilise une 
 * fen�tre de taille variable qui glisse progressivement sur tout les pixels de l'image
 * source. Lorsque la fen�tre est d�plac�, un nouveau pixel est calcul� : le pixel est
 * �gale � la temp�rature maximale des pixels de temp�rature appartenant � la fen�tre.
 * <BR><BR>
 * A noter que si le pixel � calculer est un pixel de type terre, le pixel sera 
 * obligatoirement un pixel terre en sortie.<BR><BR>
 *
 * @author Remi EVE
 * @version $Id$
 */
final class SSTSmooth {
    /** Taille en Mo du cache JAI. */
    private final long JAI_MEMORY_CACHE;
    
    /** Taille en largeur d'une tuile de l'image. */
    private final int TILE_W;
        
    /** Taille en hauteur d'une tuile de l'image. */    
    private final int TILE_H;
    
    /** Sample de l'image SST contenant les valeurs index�es. */
    private static final SampleDimension[] SAMPLE_SST_INDEXED = Utilities.SAMPLE_SST_INDEXED;

    /** Systeme de coordonn�e des images SST. */  
    private static final CoordinateSystem WGS84 = Utilities.WGS84;

    /**
     * Retourne un GridCoverage.
     *
     * @param source        Une image source.
     * @param cs            Un syst�me de coordonn�e.
     * @param sample        Tableau de category.
     * @return un grid coverage.
     */
    private static final GridCoverage createGridCoverage (final RenderedImage      source, 
                                                          final CoordinateSystem   cs,
                                                          final SampleDimension[]  sample) 
    {        
        final Envelope envelope = new Envelope(new Rectangle(0, 
                                                             0, 
                                                             source.getWidth(), 
                                                             source.getHeight()));            
        return new GridCoverage("", 
                                source, 
                                cs,                                     
                                envelope,
                                sample,
                                (GridCoverage[])null,
                                (Map)null);         
    }
    
    /**
     * G�n�re une image S.S.T. dont les temp�ratures sont liss�e par un filtre.
     *
     * @param args[0]   Fichier SST source.
     * @param args[1]   Fichier SST destination.
     * @param args[2]   Largeur de la fen�tre.
     * @param args[3]   Hauteur de la fen�tre.
     * @param args[4]   xKey.
     * @param args[5]   yKey.
     */
    public void compute(final File  src, 
                        final File  tgt, 
                        final int   width,
                        final int   height,
                        final int   xKey,
                        final int   yKey) throws IOException {
    
        // Parametre JAI. 
        final Map configuration   = new HashMap();
        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        configuration.put(JAI.KEY_TILE_CACHE, tileCache);        
        tileCache.flush();
                            
        // Ouverture du fichier source.
        final GridCoverage gridSrc = Utilities.getGridCoverage(src,
                                                               "source",
                                                               WGS84,
                                                               SAMPLE_SST_INDEXED,
                                                               null).geophysics(true);
                                                            
        // Lissage.
        final RenderedImage image = MaxFilter.get(gridSrc.geophysics(false).getRenderedImage(),
                                                  width, 
                                                  height, 
                                                  xKey,
                                                  yKey,
                                                  configuration);
        
        // Grid Coverage cible.
        final GridCoverage gridTgt = createGridCoverage(image, 
                                                        gridSrc.getCoordinateSystem(),
                                                        SAMPLE_SST_INDEXED);
        
        // Ecriture du fichier cible.        
        final String outTgt = tgt.getPath().substring(0, tgt.getPath().length()-4);
        final String outSrc = src.getPath().substring(0, src.getPath().length()-4);
        final File tmp      = new File(outTgt + ".tmp");
        final File hdrTgt   = new File(outTgt + ".hdr");
        final File hdrSrc   = new File(outSrc + ".hdr");
        Utilities.writeImage(gridTgt.geophysics(false).getRenderedImage(), tgt, tmp);
        Utilities.copy(hdrSrc, hdrTgt);
    }
    
     /**
     * Construit un objet SmoothTemperature capable de lisser les temperatures sur une image
     * SST. 
     */
    public SSTSmooth() throws IOException
    {   
        // Extraction des parametres de configuration de la SST. 
        final ParameterList param = ParseSST.parse(ParseSST.getInputDefaultParameterList());
        JAI_MEMORY_CACHE   = param.getIntParameter(ParseSST.JAI_MEMORY_CACHE);
        TILE_W             = param.getIntParameter(ParseSST.JAI_TILE_WIDTH);
        TILE_H             = param.getIntParameter(ParseSST.JAI_TILE_HEIGHT);                

        // Configuration de JAI.
        final JAI jai = JAI.getDefaultInstance();
        final TileCache tileCache = jai.getTileCache();
        tileCache.setMemoryCapacity(JAI_MEMORY_CACHE*1024*1024);
        jai.setDefaultTileSize(new Dimension(TILE_W, TILE_H));            
    }
     
    /**
     * Lisse les temperatures d'une image SST. Pour cela, le programme utilise une fen�tre 
     * qu'il d�place sur l'image source afin de calculer la valeur des pixels de sortie.
     * La valeur du pixel de sortie est �gale � la temp�rature maximale des pixels de
     * temp�rature apparteneant � la fen�tre.<BR><BR>
     *
     * Exemple : SSTSmooth  IMG_SOURCE  IMG_DESTINATION WIDTH HEIGHT XKEY YKEY<BR><BR>
     *      
     * @param args[0]   Fichier SST source.
     * @param args[1]   Fichier SST destination.
     * @param args[2]   Largeur de la fen�tre.
     * @param args[3]   Hauteur de la fen�tre.
     * @param args[4]   xKey.
     * @param args[5]   yKey.
     */
    public static void main(final String[] args) {
        if (args.length != 6) {
            System.err.println("SSTSmooth SOURCE  DESTINATION  WIDTH  HEIGHT  XKEY  YKEY");
            System.err.println("SOURCE      -> Image source");
            System.err.println("DESTINATION -> Image destination");
            System.err.println("WIDTH       -> Largeur de la fen�tre");
            System.err.println("HEIGHT      -> Hauteur de la fen�tre");
            System.err.println("XKEY        -> Coordonn�es x du pixel calcul� dans la fen�tre");
            System.err.println("YKEY        -> Coordonn�es y du pixel calcul� dans la fen�tre");
            System.exit(-1);
        }            
        
        try {                        
            final File src = new File(args[0]),
                       tgt = new File(args[1]);
            final int width  = Integer.parseInt(args[2]),
                      height = Integer.parseInt(args[3]),
                      xKey   = Integer.parseInt(args[4]),
                      yKey   = Integer.parseInt(args[5]);
            SSTSmooth sst = new SSTSmooth();
            sst.compute(src, tgt, width, height, xKey, yKey);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
    }
}