/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.resources;

// Utilitaires
import java.util.Locale;
import java.util.Enumeration;
import java.util.ListResourceBundle;
import java.util.NoSuchElementException;
import java.util.MissingResourceException;

// Formats
import java.text.Format;
import java.text.MessageFormat;
import net.seas.util.XString;
import net.seas.util.XClass;

// Entr�s/sorties
import java.io.Writer;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Divers
import net.seas.util.Version;


/**
 * {link java.util.ResourceBundle} implementation using integer key instead of strings.
 * This class make use of {@link MessageFormat} for string formatting.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class ResourceBundle extends java.util.ResourceBundle
{
    /**
     * Longueur maximale des cha�nes de caract�res. Les cha�nes plus longues que
     * cette longueur seront coup�es afin d'�viter de surcharger les messages.
     * <code>ResourceBundle</code> tentera de trouver un endroit pas trop mauvais
     * pour couper une phrase trop longue.
     */
    private static final int MAX_STRING_LENGTH = 128;

    /**
     * Nom du fichier binaire qui contient les ressources.
     */
    private final String filename;

    /**
     * Tableau des valeurs. Ce tableau ne sera initialis�
     * que la premi�re fois o� il sera n�cessaire.  Il ne
     * faut pas l'initialiser trop t�t pour �viter que les
     * classes parentes (par exemple {@link Resources} qui
     * est l'anc�tre de {@link Resources_fr}) ne chargent
     * des ressources qu'ils n'utiliseront pas.
     */
    private String[] values;

    /**
     * Objet utiliser pour �crire une cha�ne de caract�res qui
     * contient des arguments. Cet objet ne sera construit que
     * la premi�re fois o� il sera n�cessaire.
     */
    private transient MessageFormat format;

    /**
     * Cl� de la derni�re resources qui avait �t� demand�e et
     * format�e avec {@link #format}. Si la m�me ressource est
     * demand�e plusieurs fois de suite, on �vitera d'appeler
     * la couteuse m�thode {@link MessageFormat#applyPattern}.
     */
    private transient int lastKey;

    /**
     * Construit une table des ressources.
     *
     * @param  filename Nom du fichier binaire contenant les ressources.
     */
    protected ResourceBundle(final String filename)
    {this.filename = filename;}

    /**
     * List resources to the specified stream.
     *
     * @param out   The destination stream.
     * @param lower The beginning index (inclusive).
     * @param upper The ending index (exclusive), or
     *              {@link Integer#MAX_VALUE} for all resources.
     * @throws IOException if an output operation failed.
     */
    public final synchronized void list(final Writer out) throws IOException
    {
        ensureLoaded(null);
        list(out, 0, values.length);
    }

    /**
     * List resources to the specified stream.
     *
     * @param out   The destination stream.
     * @param lower The beginning index (inclusive).
     * @param upper The ending index (exclusive).
     * @throws IOException if an output operation failed.
     */
    private void list(final Writer out, int lower, int upper) throws IOException
    {
        final String lineSeparator=System.getProperty("line.separator", "\n");
        for (int i=lower; i<upper; i++)
        {
            String value = values[i];
            if (value==null) continue;
            int indexCR=value.indexOf('\r'); if (indexCR<0) indexCR=value.length();
            int indexLF=value.indexOf('\n'); if (indexLF<0) indexLF=value.length();
            final String number = String.valueOf(i);
            out.write(XString.spaces(5-number.length()));
            out.write(number);
            out.write(":\t");
            out.write(value.substring(0, Math.min(indexCR,indexLF)));
            out.write(lineSeparator);
        }
    }

    /**
     * V�rifie que les ressources ont bien �t� charg�es. Si ce
     * n'est pas le cas, proc�de imm�diatement � leur chargement.
     *
     * @param  key Cl� de la ressource d�sir�e, ou <code>null</code> pour charger toutes les ressources.
     * @throws MissingResourceException si le chargement des ressources a �chou�.
     */
    private void ensureLoaded(final String key) throws MissingResourceException
    {
        if (values!=null)
        {
            return;
        }
        /*
         * Prepare a log record. We will wait for succesfull loading before to post this
         * record. If loading fail, the record will be changed into an error record.
         */
        final Logger    logger;
        final LogRecord record;
        if (Version.MINOR>=4)
        {
            logger = Logger.getLogger("net.seas");
            record = new LogRecord(Level.CONFIG, "Loaded resources for {0}.");
            record.setSourceClassName (getClass().getName());
            record.setSourceMethodName((key!=null) ? "getObject" : "getKeys");
        }
        try
        {
            final InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
            if (in==null) throw new FileNotFoundException(filename);
            final DataInputStream input=new DataInputStream(new BufferedInputStream(in));
            values = new String[input.readInt()];
            for (int i=0; i<values.length; i++)
            {
                values[i] = input.readUTF();
                if (values[i].length()==0)
                    values[i]=null;
            }
            input.close();
            String language = getLocale().getDisplayName(Locale.UK);
            if (language==null || language.length()==0) language="<default>";
            if (Version.MINOR>=4)
            {
                record.setParameters(new String[]{language});
                logger.log(record);
            }
        }
        catch (IOException exception)
        {
            if (Version.MINOR>=4)
            {
                record.setLevel  (Level.WARNING);
                record.setMessage(exception.getLocalizedMessage());
                record.setThrown (exception);
                logger.log(record);
            }
            final MissingResourceException error = new MissingResourceException(exception.getLocalizedMessage(), getClass().getName(), key);
            if (Version.MINOR>=4) error.initCause(exception);
            throw error;
        }
    }

    /**
     * Renvoie un �num�rateur qui balayera toutes
     * les cl�s que poss�de cette liste de ressources.
     */
    public final synchronized Enumeration getKeys()
    {
        ensureLoaded(null);
        return new Enumeration()
        {
            private int i=0;

            public boolean hasMoreElements()
            {
                while (true)
                {
                    if (i>=values.length) return false;
                    if (values[i]!=null) return true;
                    i++;
                }
            }

            public Object nextElement()
            {
                while (true)
                {
                    if (i>=values.length) throw new NoSuchElementException();
                    if (values[i]!=null) return String.valueOf(i++);
                    i++;
                }
            }
        };
    }

    /**
     * Renvoie la ressource associ�e � une cl� donn�e. Cette m�thode est d�finie
     * pour r�pondre aux exigences de la classe {@link java.util.ResourceBundle}
     * et n'a g�n�ralement pas besoin d'�tre appell�e directement.
     *
     * @param  key Cl� d�signant la ressouce d�sir�e (ne doit pas �tre <code>null</code>).
     * @return La ressource demand�e, ou <code>null</code> si aucune ressource n'est
     *         d�finie pour cette cl�.
     */
    protected final synchronized Object handleGetObject(final String key)
    {
        ensureLoaded(key);
        final int keyID;
        try
        {
            keyID=Integer.parseInt(key);
        }
        catch (NumberFormatException exception)
        {
            return null;
        }
        return (keyID>=0 && keyID<values.length) ? values[keyID] : null;
    }

    /**
     * Renvoie la cha�ne <code>text</code> en s'assurant qu'elle n'aura pas plus
     * de <code>maxLength</code> caract�res. Si la cha�ne <code>text</code> est
     * suffisament courte, elle sera retourn�e telle quelle. Sinon, une cha�ne
     * synth�se sera produite. Par exemple la cha�ne "Cette phrase donn�e en exemple
     * est beaucoup trop longue" pourra devenir "Cette phrase (...) trop longue".
     * Cette m�thode est utile pour �crire des phrases d'origine inconnue dans une
     * bo�te de dialogue.
     *
     * @param  text Phrase � raccourcir si elle est trop longue.
     * @param  maxLength Longueur maximale de la phrase � retourner.
     * @return La phrase <code>text</code> (sans les espaces au d�but
     *         et � la fin) si elle a moins de <code>maxLength</code>
     *         caract�re, ou une phrase synth�se sinon.
     */
    private static String summarize(String text, int maxLength)
    {
        text=text.trim();
        final int length=text.length();
        if (length<=maxLength) return text;
        /*
         * On tentera de cr�er une cha�ne dont la moiti� provient du d�but
         * de 'text' et l'autre moiti� de la fin de 'text'. Entre les deux,
         * on placera " (...) ". On ajuste la variable 'maxLength' de fa�on
         * � ce qu'elle refl�te maintenant la longueur maximale de ces moiti�es.
         */
        maxLength = (maxLength-7) >> 1;
        if (maxLength<=0) return text;
        /*
         * La partie � ignorer ira de 'break1' jusqu'� 'break2' exclusivement.
         * On tentera de couper le texte vis-�-vis un espace. Les variables
         * 'lower' et 'upper' seront les bornes au dela desquelles on renoncera
         * � couper d'avantage de texte.
         */
        int break1 = maxLength;
        int break2 = length-maxLength;
        for (final int lower=(maxLength>>1); break1>=lower; break1--)
        {
            if (!Character.isUnicodeIdentifierPart(text.charAt(break1)))
            {
                while (--break1>=lower && !Character.isUnicodeIdentifierPart(text.charAt(break1)));
                break;
            }
        }
        for (final int upper=length-(maxLength>>1); break2<upper; break2++)
        {
            if (!Character.isUnicodeIdentifierPart(text.charAt(break2)))
            {
                while (++break2<upper && !Character.isUnicodeIdentifierPart(text.charAt(break2)));
                break;
            }
        }
        return (text.substring(0,break1+1)+" (...) "+text.substring(break2)).trim();
    }

    /**
     * Retourne <code>arguments</code> sous forme d'un tableau d'objets. Si <code>arguments</code>
     * �tait d�j� un tableau,  il peut �tre retourn� tel quel.   S'il n'�tait qu'un objet, il sera
     * envelopp� dans un tableau de longueur 1. Dans tous les cas, les �l�ments du tableaux seront
     * v�rifi�s. Ceux qui correspondent � des cha�nes de caract�res d'une longueur sup�rieure �
     * {@link #MAX_STRING_LENGTH} seront coup�es afin d'�viter de surcharger le message.
     */
    private static Object[] toArray(final Object arguments)
    {
        Object[] array=(arguments instanceof Object[]) ? (Object[]) arguments : new Object[] {arguments};
        for (int i=0; i<array.length; i++)
        {
            if (array[i] instanceof String)
            {
                final String s0=(String) array[i];
                final String s1=summarize(s0, MAX_STRING_LENGTH);
                if (s0!=s1 && !s0.equals(s1))
                {
                    if (array==arguments)
                    {
                        array=new Object[array.length];
                        System.arraycopy(arguments, 0, array, 0, array.length);
                    }
                    array[i]=s1;
                }
            }
        }
        return array;
    }

    /**
     * Retourne la ressource associ�e � la cl� sp�cifi�e.
     *
     * @param  keyID Cl� de la ressource � utiliser.
     * @throws MissingResourceException si aucune ressource n'est associ�e � la cl� sp�cifi�e.
     */
    public final String getString(final int keyID) throws MissingResourceException
    {return getString(String.valueOf(keyID));}

    /**
     * Utilise la ressource d�sign�e par la cl� <code>key</code> pour �crire un objet <code>arg0</code>.
     * Un objet {@link java.text.MessageFormat} sera utilis� pour formater l'argument <code>arg0</code>.
     * Ca sera comme si la sortie avait �t� produite par:
     *
     * <blockquote><pre>
     *     String pattern = getString(key);
     *     Format f = new MessageFormat(pattern);
     *     return f.format(arg0);
     * </pre></blockquote>
     *
     * Cette m�thode n'exige pas que l'on double les appostrophes ('') chaque fois que l'on veut
     * �crire un simple appostrophe, contrairement � la classe {@link java.text.MessageFormat}
     * standard du Java. L'argument <code>arg0</code> peut aussi �tre un tableau d'objets
     * (<code>Object[]</code>) au lieu d'un objet seul (<code>Object</code>). Dans ce cas, toutes
     * les occurences de "{0}", "{1}", "{2}", etc. dans la cha�ne de caract�res seront remplac�es
     * par <code>arg0[0]</code>, <code>arg0[1]</code>, <code>arg0[2]</code>, etc. respectivement.
     *
     * @param  keyID Cl� de la ressource � utiliser.
     * @param  arg0 Objet ou tableau d'objets � substituer aux "{0}", "{1}", etc.
     * @throws MissingResourceException si aucune ressource n'est associ�e � la cl� sp�cifi�e.
     *
     * @see #getString(String)
     * @see #getString(String,Object,Object)
     * @see #getString(String,Object,Object,Object)
     * @see java.text.MessageFormat
     */
    public final synchronized String getString(final int keyID, final Object arg0) throws MissingResourceException
    {
        final Object      object = getObject(String.valueOf(keyID));
        final Object[] arguments = toArray(arg0);
        if (format == null)
        {
            /*
             * Cr�e un objet {@link MessageFormat} pour l'�criture des arguments. Cet objet {@link MessageFormat}
             * utilisera normalement les conventions locales de l'utilisateur. Ca permettra par exemple d'�crire
             * les dates selon les conventions du Canada Fran�ais (si on se trouve au Canada) m�me si les ressources
             * sont celles du fran�ais de France. Si toutefois la langue utilis�e n'est pas la m�me, alors un utilisera
             * les conventions des ressources actuelles plut�t que celles de l'utilisateur, afin d'�tre coh�rent avec
             * la langue du texte � afficher.
             */
            Locale locale = Locale.getDefault();
            final Locale resourceLocale = getLocale();
            if (!locale.getLanguage().equalsIgnoreCase(resourceLocale.getLanguage()))
            {
                locale = resourceLocale;
            }
            format = new MessageFormat(object.toString(), locale);
        }
        else if (keyID != lastKey)
        {
            /*
             * La m�thode {@link MessageFormat#applyPattern} est co�teuse. On �vitera
             * de l'appeller si {@link #format} contient d�j� le bon pattern.
             */
            format.applyPattern(object.toString());
            lastKey = keyID;
        }
        return format.format(arguments);
    }

    /**
     * Renvoie la ressource associ�e � la cl� sp�cifi�e en rempla�ant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Cl� d�signant la ressource d�sir�e.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affect�e � la cl� sp�cifi�e.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1});}

    /**
     * Renvoie la ressource associ�e � la cl� sp�cifi�e en rempla�ant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Cl� d�signant la ressource d�sir�e.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @param  arg2 Objet dont la valeur remplacera toutes les occurences de "{2}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affect�e � la cl� sp�cifi�e.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1, final Object arg2) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1, arg2});}

    /**
     * Renvoie la ressource associ�e � la cl� sp�cifi�e en rempla�ant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Cl� d�signant la ressource d�sir�e.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @param  arg2 Objet dont la valeur remplacera toutes les occurences de "{2}"
     * @param  arg3 Objet dont la valeur remplacera toutes les occurences de "{3}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affect�e � la cl� sp�cifi�e.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1, final Object arg2, final Object arg3) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1, arg2, arg3});}

    /**
     * Renvoie la ressource associ�e � la cl� sp�cifi�e en rempla�ant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Cl� d�signant la ressource d�sir�e.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @param  arg2 Objet dont la valeur remplacera toutes les occurences de "{2}"
     * @param  arg3 Objet dont la valeur remplacera toutes les occurences de "{3}"
     * @param  arg4 Objet dont la valeur remplacera toutes les occurences de "{4}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affect�e � la cl� sp�cifi�e.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1, final Object arg2, final Object arg3, final Object arg4) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1, arg2, arg3, arg4});}

    /**
     * Retourne la ressource associ�e � la cl� sp�cifi�e
     * en la terminant par les caract�res ":&nbsp;".
     *
     * @param  keyID Cl� de la ressource � utiliser.
     * @throws MissingResourceException si aucune ressource n'est associ�e � la cl� sp�cifi�e.
     */
    public final String getLabel(final int key) throws MissingResourceException
    {return getString(key)+": ";}

    /**
     * Retourne la ressource associ�e � la cl� sp�cifi�e
     * en la terminant par les caract�res "...".
     *
     * @param  keyID Cl� de la ressource � utiliser.
     * @throws MissingResourceException si aucune ressource n'est associ�e � la cl� sp�cifi�e.
     */
    public final String getTrailing(final int key) throws MissingResourceException
    {return getString(key)+"...";}

    /**
     * Get a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @return The log record.
     */
    public LogRecord getLogRecord(final Level level, final int key)
    {return getLogRecord(level, key);}

    /**
     * Get a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @param  arg0  The parameter for the log message, or <code>null</code>.
     * @return The log record.
     */
    public LogRecord getLogRecord(final Level level, final int key, final Object arg0)
    {
        final LogRecord record = new LogRecord(level, String.valueOf(key));
        record.setResourceBundle(this);
        if (arg0!=null) record.setParameters(toArray(arg0));
        return record;
    }

    /**
     * Get a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @param  arg0  The first parameter.
     * @param  arg1  The second parameter.
     * @return The log record.
     */
    public LogRecord getLogRecord(final Level level, final int key, final Object arg0, final Object arg1)
    {return getLogRecord(level, key, new Object[]{arg0, arg1});}

    /**
     * Returns a string representation of this object.
     */
    public synchronized String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        if (values!=null)
        {
            int count=0;
            for (int i=0; i<values.length; i++)
                if (values[i]!=null) count++;
            buffer.append(count);
        }
        buffer.append(']');
        return buffer.toString();
    }
}
