package fr.ird.io.text;

// J2SE / JAI.
import java.io.File;
import java.awt.geom.AffineTransform;
import java.util.Vector;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;

// J2SE.
import fr.ird.util.ThresoldRange;

/**
 * Analyse le fichier de configuration de l'application S.S.T. (Sea Surface Temperature).
 * Ce fichier contient les paramètres du traitement S.S.T. tels que les filtres nuages, 
 * ...
 *
 * @author  Remi EVE
 * @version $Id$
 */
public class ParseSST extends Parse
{      
    /** Chemin par défaut du fichier de configuration. */
    public final static String DEFAULT_FILE = "application-data/configurationSST.txt";
    
    /** Identifiant à analyser. */
    public static final String  JAI_MEMORY_CACHE           = "JAI_MEMORY_CACHE",
                                JAI_TILE_WIDTH             = "JAI_TILE_WIDTH",
                                JAI_TILE_HEIGHT            = "JAI_TILE_HEIGHT",
                                CLS_PATH                   = "CLS_PATH",
                                ISOLINE_PATH               = "ISOLINE_PATH",
                                TRANSITION_DAY_NIGHT       = "TRANSITION_DAY_NIGHT",
                                TRANSITION_AUBE            = "TRANSITION_AUBE",
                                TRANSITION_CREPUSCULE      = "TRANSITION_CREPUSCULE",                                
                                FILTER_TEMPERATURE_SST     = "FILTER_TEMPERATURE_SST",
                                FILTER_SST_SLOPE_INTERCEPT = "FILTER_SST_SLOPE_INTERCEPT",
                                FILTER_INTERCEPT_SST       = "FILTER_INTERCEPT_SST",
                                EXCLUSION_ANGLE            = "EXCLUSION_ANGLE",
                                FILTER_LATITUDINAL         = "FILTER_LATITUDINAL",
                                START_TIME_SST_DAY         = "START_TIME_SST_DAY",
                                SST_ONE_DAY_LIMITED_AREA   = "SST_ONE_DAY_LIMITED_AREA",
                                SST_ONE_DAY_AREA           = "SST_ONE_DAY_AREA",
                                SST_N_DAY_LIMITED_AREA     = "SST_N_DAY_LIMITED_AREA",
                                SST_N_DAY_AREA             = "SST_N_DAY_AREA",
                                LOG_CLS_CORRECTION         = "LOG_CLS_CORRECTION",
                                MAX_CORRECTION_ALLOWED     = "MAX_CORRECTION_ALLOWED";
    
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
        final File file                   = extractFileToParse(parameter);
        final ParameterList parameterList = getOutputDefaultParameterList(parameter); 
        final BufferedReader input        = new BufferedReader(new FileReader(file));
        
        // Liste des filtres latitudinaux.
        final Vector vFilterLatitudinal   = new Vector();
        
        // Liste des filtres SST.
        final Vector vFilterSST           = new Vector();
        
        // Parcours ligne à ligne.
        String line;
        while ((line=input.readLine())!=null)
        {
            // Ligne vide ou commentaire.
            line = line.trim();            
            if (line.length() == 0 ||line.startsWith("#"))
                continue;
            
            final StringTokenizer token = new StringTokenizer(line);            
            final String key            = token.nextToken().trim();                        
            if (key.equals(JAI_MEMORY_CACHE)) 
            {
                final Integer[] array = parseInteger(token, 1);
                parameterList.setParameter(JAI_MEMORY_CACHE, array[0]);                    
            } else if (key.equals(JAI_TILE_WIDTH)) 
            {
                final Integer[] array = parseInteger(token, 1);
                parameterList.setParameter(JAI_TILE_WIDTH, array[0]);                    
            } else if (key.equals(JAI_TILE_HEIGHT)) 
            {
                final Integer[] array = parseInteger(token, 1);
                parameterList.setParameter(JAI_TILE_HEIGHT, array[0]);                    
            } else if (key.equals(FILTER_SST_SLOPE_INTERCEPT)) 
            {
                final Double[] array = parseDouble(token, 2);
                final AffineTransform at = new AffineTransform(array[0].doubleValue(), 0,
                                                               0, 1,
                                                               array[1].doubleValue(), 0);                
                vFilterSST.add(at);
            } else if (key.equals(EXCLUSION_ANGLE))                 
            {
                final Double[] array = parseDouble(token, 1);
                parameterList.setParameter(EXCLUSION_ANGLE, array[0]);                    
            } else if (key.equals(FILTER_TEMPERATURE_SST)) 
            {
                final Double[] array = parseDouble(token, 2);
                final Range range    = new Range(Double.class, 
                                                 array[0], false,
                                                 array[1], false); 
                parameterList.setParameter(FILTER_TEMPERATURE_SST, range);                    
            } else if (key.equals(TRANSITION_DAY_NIGHT)) 
            {
                final Double[] array = parseDouble(token, 4);
                final Range aube     = new Range(Double.class, 
                                                 array[0], false,
                                                 array[1], false); 
                final Range crepuscule = new Range(Double.class, 
                                                   array[2], false,
                                                   array[3], false);
                parameterList.setParameter(TRANSITION_AUBE, aube);                    
                parameterList.setParameter(TRANSITION_CREPUSCULE, crepuscule);                    
            } else if (key.equals(CLS_PATH)) 
            {
                final String path_ = parseString(token);
                parameterList.setParameter(CLS_PATH, path_);
            } else if (key.equals(ISOLINE_PATH)) 
            {
                final String path_ = parseString(token);
                parameterList.setParameter(ISOLINE_PATH, path_);
            } else if (key.equals(FILTER_LATITUDINAL)) 
            {
                final Double[] array  = parseDouble(token, 2);
                final double[] array_ = {array[0].doubleValue(), array[1].doubleValue()};
                vFilterLatitudinal.add(array_);                    
            } else if (key.equals(START_TIME_SST_DAY)) 
            {
                final Integer[] array = parseInteger(token, 1);                
                // StartTime est converti en millisecond.
                final long timeSec  = array[0].intValue();
                final long timeMSec = (((long)(timeSec/100)) * 
                                      60*60 + 
                                      (timeSec%100)*60) * 1000;
                parameterList.setParameter(START_TIME_SST_DAY, timeMSec);
            } else if (key.equals(SST_ONE_DAY_LIMITED_AREA)) 
            {
                    final Boolean[] array = parseBoolean(token, 1);
                    parameterList.setParameter(SST_ONE_DAY_LIMITED_AREA, array[0]);                    
            } else if (key.equals(SST_N_DAY_LIMITED_AREA)) 
            {
                    final Boolean[] array = parseBoolean(token, 1);
                    parameterList.setParameter(SST_N_DAY_LIMITED_AREA, array[0]);                    
            } else if (key.equals(SST_ONE_DAY_AREA))                 
            {
                final Double[] array = parseDouble(token, 4);
                final Rectangle2D area = new Rectangle2D.Double(array[0].doubleValue(), 
                                                                array[1].doubleValue(),
                                                                array[2].doubleValue(),
                                                                array[3].doubleValue());                 
                parameterList.setParameter(SST_ONE_DAY_AREA, area);                    
            } else if (key.equals(SST_N_DAY_AREA)) 
            {
                final Double[] array = parseDouble(token, 4);
                final Rectangle2D area = new Rectangle2D.Double(array[0].doubleValue(), 
                                                                array[1].doubleValue(),
                                                                array[2].doubleValue(),
                                                                array[3].doubleValue());                 
                parameterList.setParameter(SST_N_DAY_AREA, area);                                    
            } else if (key.equals(LOG_CLS_CORRECTION)) 
            {
                final String log = parseString(token);
                parameterList.setParameter(LOG_CLS_CORRECTION, log);
            } else if (key.equals(MAX_CORRECTION_ALLOWED)) 
            {
                final Double[] max = parseDouble(token, 1);
                parameterList.setParameter(MAX_CORRECTION_ALLOWED, max[0].doubleValue());
            }             
        } 

        /* Fin de l'analyse du fichier. Les listes contenant les filtres sont modifier pour
           avoir les filtres selon le type définie. */
        int count = vFilterLatitudinal.size();
        if (count>0)
        {
            final ThresoldRange[] thresoldRange = new ThresoldRange[count-1];            
            for (int i=1 ; i<count ; i++)
            {                
                final double[] current  = (double[])vFilterLatitudinal.get(i-1);
                final double[] previous = (double[])vFilterLatitudinal.get(i);                
                final Range range       = new Range(Double.class, 
                                                    new Double(previous[0]), true, 
                                                    new Double(current[0]),  false);
                thresoldRange[i-1]      = new ThresoldRange(range, previous[1], current[1]);
            }
            parameterList.setParameter(FILTER_LATITUDINAL, thresoldRange);
        }        
                
        count = vFilterSST.size();
        if (count>0)
        {
            final AffineTransform[] array = new AffineTransform[count];
            for (int i=0 ; i<count ; i++)
                array[i] = (AffineTransform)vFilterSST.get(i);
            parameterList.setParameter(FILTER_SST_SLOPE_INTERCEPT, array);
        }                
        return parameterList;
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
        final String descriptor       = "CONFIGURATION";
        final String[] paramNames     = {JAI_MEMORY_CACHE,
                                         JAI_TILE_WIDTH,
                                         JAI_TILE_HEIGHT,
                                         TRANSITION_AUBE,
                                         TRANSITION_CREPUSCULE,
                                         FILTER_TEMPERATURE_SST,
                                         FILTER_SST_SLOPE_INTERCEPT,
                                         EXCLUSION_ANGLE,
                                         CLS_PATH,
                                         FILTER_LATITUDINAL,
                                         START_TIME_SST_DAY,
                                         SST_ONE_DAY_LIMITED_AREA,
                                         SST_ONE_DAY_AREA,
                                         SST_N_DAY_LIMITED_AREA,
                                         SST_N_DAY_AREA,
                                         ISOLINE_PATH,
                                         LOG_CLS_CORRECTION,
                                         MAX_CORRECTION_ALLOWED};                                             
        final Class[]  paramClasses   = {Integer.class,
                                         Integer.class,
                                         Integer.class,
                                         Range.class,
                                         Range.class,
                                         Range.class,
                                         AffineTransform[].class,
                                         Double.class,
                                         String.class,
                                         ThresoldRange[].class,
                                         Long.class,
                                         Boolean.class,
                                         Rectangle2D.class,
                                         Boolean.class,
                                         Rectangle2D.class,
                                         String.class,
                                         String.class,
                                         Double.class};
        final Object[]  paramDefaults = {null,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null,
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