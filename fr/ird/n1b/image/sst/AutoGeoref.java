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
import java.util.Calendar;
import java.util.GregorianCalendar;
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
 * G�or�f�rence automatiquement un fichier N1B. Pour cela, les bulletins CLS contenant
 * les positions des satellites sont utilis�s. Un bulletin est disponible par satellite 
 * et par jour.<BR><BR>
 *
 * Lorsque un fichier est g�or�ferenc�, la correction moyenne appliqu�e sur l'ensemble 
 * de l'image est logg�es dans un fichier.<BR><BR>
 *
 * Lorsque qu'une erreur survient (fichier alt�r�, correction CLS moyenne anormale, ...), 
 * le fichier source est renommer avec l'extension <i>.err</i> pour indiquer qu'il n'a pas 
 * �t� trait� ou qu'il contient des anomaliles. 
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class AutoGeoref 
{        
    /** R�pertoire contenant les bulletin de correction CLS. */
    private final String CLS_PATH;   

    /** Fichier de log. */
    private final File LOG_FILE;   

    /** Correction maximale admise en degr�. */
    private final double MAX_CORRECTION_ALLOWED;
    
    /**
     * Construit un objet de type Georeference.
     */
    public AutoGeoref() throws IOException
    {
        // Extraction des parametres de configuration de la SST. 
        final ParameterList param = ParseSST.parse(ParseSST.getInputDefaultParameterList());        
        CLS_PATH    = (String)param.getObjectParameter(ParseSST.CLS_PATH);                
        LOG_FILE    = new File((String)param.getObjectParameter(ParseSST.LOG_CLS_CORRECTION));                                        
        MAX_CORRECTION_ALLOWED = param.getDoubleParameter(ParseSST.MAX_CORRECTION_ALLOWED);
    }
    
    /**
     * Log dans un fichier les informations sur la correction du fichier en mode 
     * automatique.
     *
     * @param fileLog       Fichier de log.
     * @param fileN1B       Fichier N1B.
     * @param satellite     Le satellite.
     * @param start         Date de d�but d'acauisition.
     * @param offset        Offset moyen en d�gr� appliqu�. Si offset est <i>null</i>, le 
     *                      g�oreferencement n'a pas fonctionn� (bulletin non disponible, 
     *                      erreur dans le fichier, ...).
     */
     private void log(final File        fileLog,
                      final File        fileN1B,
                      final Satellite   satellite,
                      final Point2D     offset)
     {
        RandomAccessFile out = null;
        try
        {
            out = new RandomAccessFile(fileLog, "rw");        
        }
        catch (IOException e)
        {
            System.err.println("\"" + LOG_FILE + "\" ne peut pas �tre ouvert." );
            return;
        }
        
        try
        {
            // Positionne en fin de fichier. 
            while(out.readLine()!=null);

            // Log la Date d'acquisition du fichier. 
            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            final DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            final StringBuffer buffer = new StringBuffer("");
            format.format(calendar.getTime(), buffer, new FieldPosition(0));
            out.writeBytes("\n" + buffer.toString() + " UTC\t\t");

            // Log le satellite. 
            out.writeBytes("N" + satellite.getID() + "\t\t");

            // Log l'offset utilisateur. 
            if (offset != null)
            {
                out.writeBytes("CORRECTION_CLS_MOYENNE=" + 
                                (double)((int)(offset.getX()*1000.0))/1000.0 + "�," + 
                                (double)((int)(offset.getY()*1000.0))/1000.0 + "�" + "\t\t");
            }
            else
            {
                out.writeBytes("CORRECTION_CLS_MOYENNE=NO_BULLETIN\t\t");
            }

            // Log le nom du fichier cr��.
            out.writeBytes("FILE=\"" + fileN1B.getPath() + "\"");                
            out.close();            
        }
        catch (IOException e)
        {
            System.err.println("Erreur d'�criture dans le fichier \"" + fileLog.getPath() + "\"." );
            return;            
        }        
     }
    
    /**
     * G�or�f�rence en automatique un fichier N1B � partir des donn�es fournies 
     * par les bulletin CLS.
     *
     * @param source    Fichier source.
     * @param target    Fichier destination.
     */
    public void compute(final File fileSrc, File fileTgt) throws IOException
    {
        // Controle des param�tres de la fonction.
        if (fileSrc == null)
            throw new IllegalArgumentException("Source est null.");
        if (!fileSrc.exists())
            throw new IllegalArgumentException("Source est introuvable.");        
        if (!fileSrc.isFile())
            throw new IllegalArgumentException("Source n'est pas un fichier.");
        if (!fileSrc.canRead())
            throw new IllegalArgumentException("Source ne peut pas etre lu.");
                        
        // Informations relative au fichier N1B source.
        ImageReaderN1B reader          = (ImageReaderN1B)ImageReaderN1B.get(new FileImageInputStream(fileSrc));                
        final LocalizationGridN1B grid = reader.getGridLocalization();
        final Metadata metadata        = (Metadata)reader.getImageMetadata(0);
        final Date start               = metadata.getStartTime();
        final Satellite satellite      = Satellite.get(metadata.getSpacecraft());                                            
               
        // Information sur la correction CLS.
        Point2D offset   = null;    // Correction applique en �
        boolean hasError = false;   // "true" si une erreur survient
        try 
        {
            final Bulletin bulletin = new Bulletin(satellite);        
            bulletin.load(new File(CLS_PATH), metadata.getStartTime());
            offset = grid.applyCorrection(bulletin);
            
            /* V�rification de la correction : si la limite acceptable de correction 
               est d�pass�e, le fichier est consid�r� comme d�faillant. */
            if (Math.abs(offset.getX())>MAX_CORRECTION_ALLOWED ||
                Math.abs(offset.getY())>MAX_CORRECTION_ALLOWED)
                hasError = true;
        }
        catch (FileNotFoundException e) 
        {
            System.err.println("Aucun bulletin de correction disponible pour ce fichier.");
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

        // Une erreur a �t� detect�e.
        if (hasError)
        {
            final File fileErr = new File(fileSrc.getPath() + ".err");
            if (fileErr.exists())
            {
                if (!fileErr.delete())
                    System.err.println("Impossible de supprimer le fichier \"" + 
                                       fileErr.getPath() + "\"");
            }
            if (!fileSrc.renameTo(fileErr))
            {
                System.err.println("Impossible de renomer le fichier \"" + 
                                   fileSrc.getPath() + 
                                   "\" en \" " + 
                                   fileErr + "\"");            
            }
            System.exit(-1);
        }
        
        // Enregistrement des modifications dans le fichier de destination.
        File tmpFile              = null;
        FileImageOutputStream out = null;
        try 
        {
            tmpFile = new File(fileTgt.getPath() + ".tmp");
            out = new FileImageOutputStream(tmpFile);            
        }
        catch (IOException e) 
        {
            System.err.println("Impossible de creer le fichier temporaire \"" + 
                               tmpFile.getName() + "\".");
            System.exit(-1);
        }

        FileImageInputStream in = null;
        ImageWriterN1B writer   = null;
        try
        {
            in     = new FileImageInputStream(fileSrc);
            writer = ImageWriterN1B.get(in);                
        }
        catch (IOException e)
        {
            System.err.println("Probl�me de lecture du fichier \"" + 
                               fileSrc.getName() + "\".");
            System.exit(-1);
        }

        // Param�tre de l'enregistrement du .
        final ParameterListImpl parameters = ImageWriterN1B.getDefaultParameters();
        parameters.setParameter("GRID_LOCALIZATION", grid);
        parameters.setParameter("Y_MIN", (double)0.0);        
        parameters.setParameter("Y_MAX", (double)(grid.getSize().getHeight()-1));        

        // Copie du fichier. 
        try 
        {
            writer.write(out, parameters);
            out.close();
            in.close();                            
        }
        catch (IOException e)        
        {
            System.err.println("Erreur lors de l'ecriture du fichier.");
            System.exit(-1);
        }
        
        if (fileTgt.exists()) 
        {
            if (!fileTgt.delete())
            {
                System.err.println("Impossible de supprimer le fichier \"" + 
                                   fileTgt.getName() + "\".");
                System.exit(-1);
            }
        } 
        if (!tmpFile.renameTo(fileTgt))
        {
            System.err.println("Impossible de renomer le fichier \"" + 
                               tmpFile.getName() + "\" en \"" + 
                               fileTgt.getName() + "\".");
            System.exit(-1);
        } 
        log(LOG_FILE, fileSrc, satellite, offset);
    }
    
    /**
     * G�or�f�rence un fichier N1B en mode automatique � l'aide des bulletin de correction 
     * CLS. Le r�sultat de la correction est logg� dans un fichier d�finie dans le fichier 
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
            System.exit(-1);
        }
    }
}