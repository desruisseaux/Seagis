package fr.ird.io.text;

// J2SE / JAI.
import java.io.File;
import java.util.Vector;
import java.io.FileReader;
import java.io.IOException;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;

/**
 * Analyse le fichier de configuration de l'application de géoréférencement des fichiers 
 * N1B. Ce fichier contient notament des informations sur les répertoires par défault des 
 * fichiers CLS, N1B et autres.<BR><BR>
 *
 * @author  Remi EVE
 * @version $Id$
 */
public class ParseGeoref extends Parse
{   
    /** Chemin par défaut du fichier de configuration. */
    private final static String DEFAULT_FILE = "application-data/configurationGeoref.txt";
        
    /** Identifiant à analyser. */
    public static final String JAI_TILE_WIDTH               = "JAI_TILE_WIDTH",
                               JAI_TILE_HEIGHT              = "JAI_TILE_HEIGHT",
                               JAI_MEMORY_CACHE             = "JAI_MEMORY_CACHE",                                   
                               DEFAULT_DIRECTORY_N1B        = "DEFAULT_DIRECTORY_N1B",                               
                               DEFAULT_DIRECTORY_SELECTION  = "DEFAULT_DIRECTORY_SELECTION",
                               DEFAULT_DIRECTORY_COASTLINE  = "DEFAULT_DIRECTORY_COASTLINE",
                               DEFAULT_DIRECTORY_GEOREF     = "DEFAULT_DIRECTORY_GEOREF",
                               DEFAULT_FILE_SELECTION       = "DEFAULT_FILE_SELECTION",
                               CLS_PATH                     = "CLS_PATH",
                               LOG_MANUAL_CORRECTION        = "LOG_MANUAL_CORRECTION";

    /**
     * Retourne un objet de type <CODE>ParameterList</CODE> contenant les paramètres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     *
     * @param parameter   Les paramètres de configuration du parser.
     * @return un objet de type <CODE>ParameterList</CODE> contenant les paramètres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     */
    public static ParameterList parse(final ParameterList parameter) throws IOException
    {
        final File file = extractFileToParse(parameter);
        ParameterList parameterOut = getOutputDefaultParameterList(parameter);
        final BufferedReader input = new BufferedReader(new FileReader(file));
        
        // Extraction des parametres ligne par ligne.
        String line;
        while ((line=input.readLine())!=null)
        {
            // Ligne vide ou commentaire.
            line = line.trim();
            if (line.length() == 0 ||line.startsWith("#"))
                continue;            
            
            // Analyse de l'identifiant.
            final StringTokenizer token = new StringTokenizer(line);            
            final String key            = token.nextToken().trim();                        
            if (key.equals(DEFAULT_DIRECTORY_COASTLINE)) 
            {
                final String directory = parseString(token);
                parameterOut.setParameter(DEFAULT_DIRECTORY_COASTLINE, directory);
            } else if (key.equals(DEFAULT_DIRECTORY_N1B)) 
            {
                final String directory = parseString(token);
                parameterOut.setParameter(DEFAULT_DIRECTORY_N1B, directory);
            } else if (key.equals(DEFAULT_DIRECTORY_GEOREF)) 
            {
                final String directory = parseString(token);
                parameterOut.setParameter(DEFAULT_DIRECTORY_GEOREF, directory);
            } else if (key.equals(DEFAULT_DIRECTORY_SELECTION)) 
            {
                final String directory = parseString(token);
                parameterOut.setParameter(DEFAULT_DIRECTORY_SELECTION, directory);
            } else if (key.equals(JAI_MEMORY_CACHE)) 
            {
                final Integer[] array = parseInteger(token, 1);
                parameterOut.setParameter(JAI_MEMORY_CACHE, array[0]);                    
            } else if (key.equals(JAI_TILE_WIDTH)) 
            {
                final Integer[] array = parseInteger(token, 1);
                parameterOut.setParameter(JAI_TILE_WIDTH, array[0]);                    
            } else if (key.equals(JAI_TILE_HEIGHT)) 
            {
                final Integer[] array = parseInteger(token, 1);
                parameterOut.setParameter(JAI_TILE_HEIGHT, array[0]);                    
            } else if (key.equals(CLS_PATH)) 
            {
                final String directory = parseString(token);
                parameterOut.setParameter(CLS_PATH, directory);
            } else if (key.equals(LOG_MANUAL_CORRECTION)) 
            {
                final String log = parseString(token);
                parameterOut.setParameter(LOG_MANUAL_CORRECTION, log);
            } else if (key.equals(DEFAULT_FILE_SELECTION)) 
            {
                final String fileSelection = parseString(token);
                parameterOut.setParameter(DEFAULT_FILE_SELECTION, fileSelection);
            } 
        }         
        return parameter;
    }
    
    /**
     * Retourne les paramètres à extraire du fichier par défaut. 
     *
     * @param parameter  Paramètres de configuration du parser. 
     * @return les paramètres à extraire du fichier par défaut. 
     */
    private static ParameterList getOutputDefaultParameterList(final ParameterList parameter) 
    {        
        final ParameterList parameters;
        final String descriptor       = "CONFIGURATION GEOREFERENCEMENT";        
        final String[] paramNames     = {DEFAULT_DIRECTORY_COASTLINE,
                                         DEFAULT_DIRECTORY_N1B,
                                         DEFAULT_DIRECTORY_SELECTION,
                                         DEFAULT_DIRECTORY_GEOREF,                                         
                                         DEFAULT_FILE_SELECTION,
                                         CLS_PATH,
                                         JAI_MEMORY_CACHE,
                                         JAI_TILE_WIDTH,
                                         JAI_TILE_HEIGHT,
                                         LOG_MANUAL_CORRECTION};                                                                                      
        final Class[]  paramClasses   = {String.class,
                                         String.class,
                                         String.class,
                                         String.class,
                                         String.class,                                         
                                         String.class,                                         
                                         Integer.class,
                                         Integer.class,
                                         Integer.class,
                                         String.class};
        final Object[]  paramDefaults = {null,
                                         null,
                                         null,
                                         null,
                                         null, 
                                         null,                                          
                                         null,
                                         null,
                                         null,
                                         null};
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));
        return parameters;
    }
        
    /**
     * Retourne la liste des paramètres de configuration du parser.
     * @return la liste des paramètres de configuration du parser.
     */
    public static ParameterList getInputDefaultParameterList() 
    {        
        final ParameterList parameters;
        final String descriptor       = "CONFIGURATION";        
        final String[] paramNames     = {FILE};                                                                                      
        final Class[]  paramClasses   = {File.class};
        final Object[]  paramDefaults = {new File(ParseGeoref.class.getClassLoader().getResource(DEFAULT_FILE).getPath())};
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));
        return parameters;
    }    
}