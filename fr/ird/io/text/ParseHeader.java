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
 * Analyse un fichier contenant des informations de localisation. Ce fichier permet de 
 * localiser un fichier ne contenant pas d'information de localisation (PNG, GIF, ...). 
 * Il contient deux identifiants : 
 * <UL>
 *  <LI>ORIGINE         LONGITUDE       LATITUDE</LI>
 *  <LI>RESOLUTION     	LONGITUDE	LATITUDE</LI>
 * </UL>
 *
 * @author R�mi EVE
 * @version $Id$
 */
public class ParseHeader extends Parse
{
    /** Identifiant � analyser. */
    public static final String  ORIGINE    = "ORIGINE",
                                RESOLUTION = "RESOLUTION";
    
    /**
     * Retourne un objet de type <CODE>ParameterList</CODE> contenant les param�tres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     *
     * @param parameter   Les param�tres de configuration du parser.
     * @return un objet de type <CODE>ParameterList</CODE> contenant les param�tres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     */
    public static ParameterList parse(final ParameterList parameter) throws IOException
    {
        final File file = extractFileToParse(parameter);
        final BufferedReader input       = new BufferedReader(new FileReader(file));
        final ParameterList parameterOut = getOutputDefaultParameterList(parameter);        

        // Parcours ligne � ligne.
        String line;
        while ((line=input.readLine())!=null)
        {
            // Ligne vide ou commentaire.
            line = line.trim();
            if (line.length() == 0 ||line.startsWith("#"))
                continue;            
            
            final StringTokenizer token = new StringTokenizer(line);            
            final String key = token.nextToken().trim();            
            if (key.equals(ORIGINE)) 
            {
                    final String[] value = new String[2];
                    for (int i=0 ; i<value.length && token.hasMoreTokens(); i++) 
                        value[i] = token.nextToken();
                    final Point2D origine = new Point2D.Double(Double.parseDouble(value[0]), 
                                                               Double.parseDouble(value[1]));
                    parameterOut.setParameter(ORIGINE, origine);                                                                     
            } else if (key.equals(RESOLUTION)) 
            {
                if (token.hasMoreTokens())
                {
                    final double[] resolution = {Double.parseDouble(token.nextToken()),
                                                 Double.parseDouble(token.nextToken())};
                    parameterOut.setParameter(RESOLUTION, resolution);                    
                }
            } 
        }
        return parameterOut;
    }
    
    /**
     * Retourne les param�tres � extraire du fichier par d�faut. 
     *
     * @param parameter  Param�tres de configuration du parser. 
     * @return les param�tres � extraire du fichier par d�faut. 
     */
    private static ParameterList getOutputDefaultParameterList(final ParameterList parameter) 
    {        
        final ParameterList parameters;
        final String descriptor       = "HEADER";
        final String[] paramNames     = {ORIGINE,
                                         RESOLUTION};                                             
        final Class[]  paramClasses   = {Point2D.class,
                                         double[].class};
        final Object[]  paramDefaults = {null,
                                         null};
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));
        return parameters;
    }
    
    /**
     * Retourne la liste des param�tres de configuration du parser.
     * @return la liste des param�tres de configuration du parser.
     */
    public static ParameterList getInputDefaultParameterList() 
    {        
        final ParameterList parameters;
        final String descriptor       = "CONFIGURATION";        
        final String[] paramNames     = {FILE};                                                                                      
        final Class[]  paramClasses   = {File.class};
        final Object[]  paramDefaults = {null};
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));
        return parameters;
    }    
}