package fr.ird.io.text;

// J2SE / JAI.
import java.io.File;
import java.util.Vector;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListDescriptorImpl;

/**
 * D�finie une classe abstraite permettant d'analyser des fichiers contenant des 
 * informations de configuration ou autres. En g�n�rale, ces fichiers sont de la forme :
 * "IDENTIFIANT    VALUE1  VALUE2".<BR><BR>
 * 
 * Lorsque cette classe est �tendue, il est n�cc�ssaire de sur-d�finir les m�thodes 
 * statiques <CODE>getOutputDefaultParameterList(final ParameterList parameter)</CODE> et 
 * <CODE>getInputDefaultParameterList()</CODE>.<BR><BR>
 *
 * Lorsque l'utilisateur d�sire analyser un fichier, il doit dans une premier temps 
 * obtenir la liste des param�tres par d�faut du parser. Cette liste contient les 
 * param�tres de configuration � passer en argument du parser. La clef <CODE>FILE</CODE>
 * contient le nom du fichier � analyser. Si celui-ci n'est pas renseign�, l'utilisateur
 * devra n�c�ssairement le faire.
 * 
 * @author  Remi EVE
 * @version $Id$
 */
public abstract class Parse 
{   
    /** Identifiant d�finissant le fichier � analyser. */
    protected static final String FILE = "FILE TO PARSE";
        
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
        return null;
    }
    
    /**
     * Retourne les param�tres � extraire du fichier par d�faut. 
     *
     * @param parameter  Param�tres de configuration du parser. 
     * @return les param�tres � extraire du fichier par d�faut. 
     */
    private static ParameterList getOutputDefaultParameterList(final ParameterList parameter) 
    {
        return null;
    }
    
    /**
     * Retourne la liste des param�tres de configuration du parser.
     * @return la liste des param�tres de configuration du parser.
     */
    public static ParameterList getInputDefaultParameterList() 
    {
        return null;
    }

    /**
     * Retourne le fichier � analyser.
     *
     * @param parameter     Liste des param�tres de configuration du parser.
     * @return le fichier � analyser.
     */
    protected static File extractFileToParse(final ParameterList parameter)
    {
        final File file = (File)parameter.getObjectParameter(FILE);
        if (file == null)                
            throw new IllegalArgumentException("FILE is not define.");
        return file;
    }
    
    /**
     * Extrait <CODE>count</CODE> valeurs flottantes dans le token.
     *
     * @param token     Le token � analyser.
     * @param count     Le nombre d'�l�ments � extraire.
     * @return un tableau contenant <CODE>count</CODE> valeurs extraites.
     */
    protected static Double[] parseDouble(final StringTokenizer token, final int count) 
    {
        final Double[] array = new Double[count];
        int i=0;
        while (i<count && token.hasMoreTokens())
            array[i++] = Double.valueOf(token.nextToken());
        return array;
    }

    /**
     * Extrait <CODE>count</CODE> valeurs enti�res dans le token.
     *
     * @param token     Le token � analyser.
     * @param count     Le nombre d'�l�ments � extraire.
     * retourne un tableau contenant les <CODE>count</CODE> valeurs extraites.
     */
    protected static Integer[] parseInteger(final StringTokenizer token, final int count) 
    {
        final Integer[] array = new Integer[count];
        int i=0;
        while (i<count && token.hasMoreTokens())
            array[i++] = Integer.valueOf(token.nextToken());
        return array;
    }    
    
    /**
     * Extrait <CODE>count</CODE> valeurs bool�ennes dans le token.
     *
     * @param token     Le token � analyser.
     * @param count     Le nombre d'�l�ments � extraire.
     * retourne un tableau contenant les <CODE>count</CODE> valeurs extraites.
     */
    protected static Boolean[] parseBoolean(final StringTokenizer token, final int count) 
    {
        final Boolean[] array = new Boolean[count];
        int i=0;
        while (i<count && token.hasMoreTokens())
            array[i++] = Boolean.valueOf(token.nextToken());
        return array;
    }       
    
    /**
     * Retourne une cha�ne de la forme <I>"c:/essai/..."</I>.
     *
     * @param token     Le token � analyser.
     * @return un chemin sous forme d'une cha�ne de carat�re.
     */
    protected static String parseString(final StringTokenizer token) 
    {
        final StringBuffer buffer = new StringBuffer();
        
        while (token.hasMoreElements()) 
        {
            if (buffer.length()>0)
                buffer.append(" " + token.nextToken());
            else
                buffer.append(token.nextToken());
        }                
        return (buffer.toString().substring(1,buffer.length()-1)).trim();
    }           
}