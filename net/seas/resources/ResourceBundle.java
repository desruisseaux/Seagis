/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
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

// Entrés/sorties
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;


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
     * Longueur maximale des chaînes de caractères. Les chaînes plus longues que
     * cette longueur seront coupées afin d'éviter de surcharger les messages.
     * <code>ResourceBundle</code> tentera de trouver un endroit pas trop mauvais
     * pour couper une phrase trop longue.
     */
    private static final int MAX_STRING_LENGTH = 128;

    /**
     * Tableau des valeurs.
     */
    private final String[] values;

    /**
     * Objet utiliser pour écrire une chaîne de caractères qui
     * contient des arguments. Cet objet ne sera construit que
     * la première fois où il sera nécessaire.
     */
    private transient MessageFormat format;

    /**
     * Clé de la dernière resources qui avait été demandée et
     * formatée avec {@link #format}. Si la même ressource est
     * demandée plusieurs fois de suite, on évitera d'appeler
     * la couteuse méthode {@link MessageFormat#applyPattern}.
     */
    private transient int lastKey;

    /**
     * Construit une table des ressources.
     *
     * @param  locale {@link Locale} des ressources (à titre informatif).
     * @param  filename Nom du fichier binaire contenant les ressources.
     * @throws IOException si les ressources n'ont pas pu être ouvertes.
     */
    protected ResourceBundle(final Locale locale, final String filename) throws IOException
    {
        if (filename!=null)
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
            Logger.getLogger("net.seas").log(Level.CONFIG, "Loaded resources for {0}.", new String[]{locale.getDisplayName(Locale.UK)});
        }
        else values = new String[0];
    }

    int getLoadingKey()
    {return -1;}

    /**
     * Renvoie un énumérateur qui balayera toutes
     * les clés que possède cette liste de ressources.
     */
    public final Enumeration getKeys()
    {
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
     * Renvoie la ressource associée à une clé donnée. Cette méthode est définie
     * pour répondre aux exigences de la classe {@link java.util.ResourceBundle}
     * et n'a généralement pas besoin d'être appellée directement.
     *
     * @param  key Clé désignant la ressouce désirée (ne doit pas être <code>null</code>).
     * @return La ressource demandée, ou <code>null</code> si aucune ressource n'est
     *         définie pour cette clé.
     */
    protected final synchronized Object handleGetObject(final String key)
    {
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
     * Renvoie la chaîne <code>text</code> en s'assurant qu'elle n'aura pas plus
     * de <code>maxLength</code> caractères. Si la chaîne <code>text</code> est
     * suffisament courte, elle sera retournée telle quelle. Sinon, une chaîne
     * synthèse sera produite. Par exemple la chaîne "Cette phrase donnée en exemple
     * est beaucoup trop longue" pourra devenir "Cette phrase (...) trop longue".
     * Cette méthode est utile pour écrire des phrases d'origine inconnue dans une
     * boîte de dialogue.
     *
     * @param  text Phrase à raccourcir si elle est trop longue.
     * @param  maxLength Longueur maximale de la phrase à retourner.
     * @return La phrase <code>text</code> (sans les espaces au début
     *         et à la fin) si elle a moins de <code>maxLength</code>
     *         caractère, ou une phrase synthèse sinon.
     */
    private static String summarize(String text, int maxLength)
    {
        text=text.trim();
        final int length=text.length();
        if (length<=maxLength) return text;
        /*
         * On tentera de créer une chaîne dont la moitié provient du début
         * de 'text' et l'autre moitié de la fin de 'text'. Entre les deux,
         * on placera " (...) ". On ajuste la variable 'maxLength' de façon
         * à ce qu'elle reflète maintenant la longueur maximale de ces moitiées.
         */
        maxLength = (maxLength-7) >> 1;
        if (maxLength<=0) return text;
        /*
         * La partie à ignorer ira de 'break1' jusqu'à 'break2' exclusivement.
         * On tentera de couper le texte vis-à-vis un espace. Les variables
         * 'lower' et 'upper' seront les bornes au dela desquelles on renoncera
         * à couper d'avantage de texte.
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
     * était déjà un tableau,  il peut être retourné tel quel.   S'il n'était qu'un objet, il sera
     * enveloppé dans un tableau de longueur 1. Dans tous les cas, les éléments du tableaux seront
     * vérifiés. Ceux qui correspondent à des chaînes de caractères d'une longueur supérieure à
     * {@link #MAX_STRING_LENGTH} seront coupées afin d'éviter de surcharger le message.
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
     * Retourne la ressource associée à la clé spécifiée.
     *
     * @param  keyID Clé de la ressource à utiliser.
     * @throws MissingResourceException si aucune ressource n'est associée à la clé spécifiée.
     */
    public final String getString(final int keyID) throws MissingResourceException
    {return getString(String.valueOf(keyID));}

    /**
     * Utilise la ressource désignée par la clé <code>key</code> pour écrire un objet <code>arg0</code>.
     * Un objet {@link java.text.MessageFormat} sera utilisé pour formater l'argument <code>arg0</code>.
     * Ca sera comme si la sortie avait été produite par:
     *
     * <blockquote><pre>
     *     String pattern = getString(key);
     *     Format f = new MessageFormat(pattern);
     *     return f.format(arg0);
     * </pre></blockquote>
     *
     * Cette méthode n'exige pas que l'on double les appostrophes ('') chaque fois que l'on veut
     * écrire un simple appostrophe, contrairement à la classe {@link java.text.MessageFormat}
     * standard du Java. L'argument <code>arg0</code> peut aussi être un tableau d'objets
     * (<code>Object[]</code>) au lieu d'un objet seul (<code>Object</code>). Dans ce cas, toutes
     * les occurences de "{0}", "{1}", "{2}", etc. dans la chaîne de caractères seront remplacées
     * par <code>arg0[0]</code>, <code>arg0[1]</code>, <code>arg0[2]</code>, etc. respectivement.
     *
     * @param  keyID Clé de la ressource à utiliser.
     * @param  arg0 Objet ou tableau d'objets à substituer aux "{0}", "{1}", etc.
     * @throws MissingResourceException si aucune ressource n'est associée à la clé spécifiée.
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
             * Crée un objet {@link MessageFormat} pour l'écriture des arguments. Cet objet {@link MessageFormat}
             * utilisera normalement les conventions locales de l'utilisateur. Ca permettra par exemple d'écrire
             * les dates selon les conventions du Canada Français (si on se trouve au Canada) même si les ressources
             * sont celles du français de France. Si toutefois la langue utilisée n'est pas la même, alors un utilisera
             * les conventions des ressources actuelles plutôt que celles de l'utilisateur, afin d'être cohérent avec
             * la langue du texte à afficher.
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
             * La méthode {@link MessageFormat#applyPattern} est coûteuse. On évitera
             * de l'appeller si {@link #format} contient déjà le bon pattern.
             */
            format.applyPattern(object.toString());
            lastKey = keyID;
        }
        return format.format(arguments);
    }

    /**
     * Renvoie la ressource associée à la clé spécifiée en remplaçant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Clé désignant la ressource désirée.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1});}

    /**
     * Renvoie la ressource associée à la clé spécifiée en remplaçant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Clé désignant la ressource désirée.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @param  arg2 Objet dont la valeur remplacera toutes les occurences de "{2}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1, final Object arg2) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1, arg2});}

    /**
     * Renvoie la ressource associée à la clé spécifiée en remplaçant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Clé désignant la ressource désirée.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @param  arg2 Objet dont la valeur remplacera toutes les occurences de "{2}"
     * @param  arg3 Objet dont la valeur remplacera toutes les occurences de "{3}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1, final Object arg2, final Object arg3) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1, arg2, arg3});}

    /**
     * Renvoie la ressource associée à la clé spécifiée en remplaçant
     * toutes les occurences de "{0}", "{1}", etc. par les valeurs de
     * <code>arg0</code>, <code>arg1</code>, etc.
     *
     * @param  key Clé désignant la ressource désirée.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @param  arg2 Objet dont la valeur remplacera toutes les occurences de "{2}"
     * @param  arg3 Objet dont la valeur remplacera toutes les occurences de "{3}"
     * @param  arg4 Objet dont la valeur remplacera toutes les occurences de "{4}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public final String getString(final int keyID, final Object arg0, final Object arg1, final Object arg2, final Object arg3, final Object arg4) throws MissingResourceException
    {return getString(keyID, new Object[] {arg0, arg1, arg2, arg3, arg4});}

    /**
     * Retourne la ressource associée à la clé spécifiée
     * en la terminant par les caractères ":&nbsp;".
     *
     * @param  keyID Clé de la ressource à utiliser.
     * @throws MissingResourceException si aucune ressource n'est associée à la clé spécifiée.
     */
    public final String getLabel(final int key) throws MissingResourceException
    {return getString(key)+": ";}

    /**
     * Retourne la ressource associée à la clé spécifiée
     * en la terminant par les caractères "...".
     *
     * @param  keyID Clé de la ressource à utiliser.
     * @throws MissingResourceException si aucune ressource n'est associée à la clé spécifiée.
     */
    public final String getTrailing(final int key) throws MissingResourceException
    {return getString(key)+"...";}
}
