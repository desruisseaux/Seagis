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

// J2SE / JAI.
import java.awt.geom.Point2D;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;

// SEAGIS.
import fr.ird.io.text.ParseSST;
import fr.ird.n1b.io.ImageWriterN1B;
import fr.ird.n1b.io.Bulletin;
import fr.ird.n1b.io.Metadata;
import fr.ird.n1b.io.Satellite;
import fr.ird.n1b.io.ImageReaderN1B;
import fr.ird.n1b.io.LocalizationGridN1B;

// Geotools.
import org.geotools.ct.TransformException;


/**
 * Géoréférence automatiquement un fichier N1B. Pour cela, les bulletins CLS contenant
 * les positions des satellites sont utilisés. Un bulletin est disponible par satellite 
 * et par jour.<BR><BR>
 *
 * Lorsque un fichier est géoréferencé, la correction moyenne appliquée sur l'ensemble 
 * de l'image est loggées dans un fichier définie dans le fichier de configuration
 * "application-data/configurationSST.txt".<BR><BR>
 *
 * Lorsque qu'une erreur survient (fichier altéré, correction CLS moyenne anormale, ...), 
 * le fichier source est renommer avec l'extension ".err" pour indiquer qu'il n'a pas été 
 * traité. 
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class AutoGeoref 
{        
    /** Repertoire contenant les bulletin de correction CLS. */
    private final String CLS_PATH;   

    /** Fichier de log. */
    private final String LOG_FILE;   

    /** Correction maximale admise en degré. */
    private final double MAX_CORRECTION_ALLOWED;
    
    /**
     * Construit un objet de type Georeference.
     */
    public AutoGeoref() throws IOException
    {
        // Extraction des parametres de configuration de la SST. 
        final ParameterList param = ParseSST.parse(ParseSST.getInputDefaultParameterList());        
        CLS_PATH    = (String)param.getObjectParameter(ParseSST.CLS_PATH);                
        LOG_FILE    = (String)param.getObjectParameter(ParseSST.LOG_CLS_CORRECTION);                                        
        MAX_CORRECTION_ALLOWED = param.getDoubleParameter(ParseSST.MAX_CORRECTION_ALLOWED);
    }
    
    /**
     * Log dans un fichier les informations sur la correction du fichier en mode automatique.
     *
     * @param name          Nom du fichier de log.
     * @param file          Fichier géoréférencé.
     * @param satellite     Le satellite.
     * @param start         Date de début d'acauisition.
     * @param offset        Offset moyen en dégré appliqué. Si offset est <i>null</i>, le 
     *                      géoreferencement n'a pas fonctionné (bulletin non disponible, 
     *                      erreur dans le fichier, ...).
     */
     private void log(final String      name,
                      final Satellite   satellite,
                      final File        file,
                      final Date        start,
                      final Point2D     offset)
     {
        RandomAccessFile out = null;
        try
        {
            // Log des informations. 
            out = new RandomAccessFile(name, "rw");        
        }
        catch (IOException e)
        {
            System.err.println("\"" + LOG_FILE + "\" ne peut pas être ouvert." );
            return;
        }
        
        try
        {
            // Positionne en fin de fichier. 
            while(out.readLine()!=null);

            // Log la Date d'acquisition du fichier. 
            final DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            final StringBuffer buffer = new StringBuffer("");
            format.format(start, buffer, new FieldPosition(0));
            out.writeBytes("\n" + buffer.toString() + " UTC\t\t");

            // Log le satellite. 
            out.writeBytes("N" + satellite.getID() + "\t\t");

            // Log l'offset utilisateur. 
            if (offset != null)
            {
                out.writeBytes("CORRECTION_CLS_MOYENNE=" + 
                                (double)((int)(offset.getX()*1000.0))/1000.0 + "°," + 
                                (double)((int)(offset.getY()*1000.0))/1000.0 + "°" + "\t\t");
            }
            else
            {
                out.writeBytes("CORRECTION_CLS_MOYENNE=NO_BULLETIN\t\t");
            }

            // Log le nom du fichier créé.
            out.writeBytes("FILE=\"" + file.getPath() + "\"");                
            out.close();            
        }
        catch (IOException e)
        {
            System.err.println("Erreur d'écriture dans le fichier \"" + name + "\"." );
            return;            
        }        
     }
    
    /**
     * Géoréférence en automatique un fichier N1B à partir des données fournies par les 
     * bulletin CLS.
     *
     * @param source    Fichier source.
     * @param target    Fichier destination.
     */
    public void compute(final File source, File target) throws IOException
    {
        // Controle des paramètres de la fonction.
        if (source == null)
            throw new IllegalArgumentException("File source is null");
        if (!source.exists())
            throw new IllegalArgumentException("Source doesn't exist : " + source.getPath());        
        if (!source.isFile())
            throw new IllegalArgumentException("Source is not a file : " + source.getPath());
        if (!source.canRead())
            throw new IllegalArgumentException("Source can't be read : " + source.getPath());
                        
        // Ouverture du fichier N1B.
        ImageReaderN1B reader          = (ImageReaderN1B)ImageReaderN1B.get(new FileImageInputStream(source));                
        final LocalizationGridN1B grid = reader.getGridLocalization();
        final Metadata metadata        = (Metadata)reader.getImageMetadata(0);
        final Date start               = metadata.getStartTime();
        final Satellite satellite      = Satellite.get(metadata.getSpacecraft());                                            
        final int width  = reader.getWidth(0),
                  height = reader.getHeight(0);
               
        // Correction CLS.
        Point2D offset = null;
        boolean hasError = false;
        try 
        {
            final Bulletin bulletin = new Bulletin(satellite);        
            bulletin.load(new File(CLS_PATH), metadata.getStartTime());
            offset = grid.applyCorrection(bulletin);
            
            // La limite acceptable de correction est dépassée, le fichier est 
            // considéré comme défaillant. 
            if (Math.abs(offset.getX())>MAX_CORRECTION_ALLOWED ||
                Math.abs(offset.getY())>MAX_CORRECTION_ALLOWED)
            {
                hasError = true;
            }
        }
        catch (FileNotFoundException e) 
        {
        }        
        catch (Exception e) 
        {
            hasError = true;
        }
                
        // Liberation de ressources. 
        reader.dispose();
        reader = null;
        System.gc();
        System.runFinalization();                    

        if (hasError)
        {
            target = new File(source.getPath() + ".err");
            source.renameTo(target);        
            return;
        }
        
        // Enregistrement des modifications.
        File tmpFile              = null;
        FileImageOutputStream out = null;
        try 
        {
            tmpFile = new File(target.getPath() + ".tmp");
            out = new FileImageOutputStream(tmpFile);            
        }
        catch (IOException e) 
        {
            System.err.println("Impossible de creer le fichier temporaire \"" + tmpFile.getName() + "\".");
            return;
        }

        // Creation du writeN1B. 
        FileImageInputStream in = null;
        ImageWriterN1B writer   = null;
        try
        {
            in     = new FileImageInputStream(source);
            writer = ImageWriterN1B.get(in);                
        }
        catch (IOException e)
        {
            System.err.println("Problème de lecture du fichier \"" + source.getName() + "\".");
            return;            
        }

        final ParameterListImpl parameters = ImageWriterN1B.getDefaultParameters();
        parameters.setParameter("GRID_LOCALIZATION", grid);
        parameters.setParameter("Y_MIN", (double)0.0);        
        parameters.setParameter("Y_MAX", (double)(grid.getSize().getHeight()-1));        

        // Ecriture du fichier. 
        try 
        {
            writer.write(out, parameters);
            out.close();
            in.close();                            
        }
        catch (IOException e)        
        {
            System.err.println("Erreur lors de l'ecriture du fichier.");
            return;            
        }
        
        if (target.exists()) 
        {
            // Sauvegarde du fichier sous le meme nom. 
            if (!target.delete())
            {
                System.err.println("Impossible de supprimer le fichier \"" + 
                                   target.getName() + "\".");
                System.exit(-1);
            }
        } 
        if (!tmpFile.renameTo(target))
        {
            System.err.println("Impossible de renomer le fichier \"" + 
                               tmpFile.getName() + "\" en \"" + 
                               target.getName() + "\".");
            System.exit(-1);
        } 
        log(LOG_FILE, satellite, source, start, offset);
    }
    
    /**
     * Géoréférence un fichier N1B en mode automatique à l'aide des bulletin de correction 
     * CLS. Le résultat de la correction est loggé dans un fichier définie dans le fichier 
     * de configuration "application-data/configurationSST.txt".<BR><BR>
     *
     * @param args[0]   Nom du fichier source.
     * @param args[1]   Nom du fichier destination.
     */
    public static void main(String[] args)
    {
        final int count = args.length;
        if (count !=2 ) 
        {
            System.err.println("Format  : AutoGeoref SOURCE DESTINATION");
            System.err.println("SOURCE      --> Image source");
            System.err.println("DESTINATION --> Image destination");            
            System.exit(-1);
        }        
        final File source = new File(args[0]),
                   target = new File(args[1]);        
        try
        {
            final AutoGeoref georef = new AutoGeoref();
            georef.compute(source, target);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
    }
}