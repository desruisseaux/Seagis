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

// Miscellaneous
import java.util.Locale;
import java.util.MissingResourceException;
import java.io.IOException;
import net.seas.util.Console;


/**
 * Liste de ressources qui dépendront de la langue de l'utilisateur. L'usager ne devrait
 * pas créer lui-même des instances de cette classe. Une instance statique sera créée une
 * fois pour toute lors du chargement de cette classe, et les divers resources seront mises
 * à la disposition du développeur via les méthodes statiques.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Resources extends ResourceBundle
{
    /**
     * Initialise les ressources par défaut. Ces ressources ne seront pas forcément dans
     * la langue de l'utilisateur. Il s'agit plutôt de ressources à utiliser par défaut
     * si aucune n'est disponible dans la langue de l'utilisateur. Ce constructeur est
     * réservé à un usage interne et ne devrait pas être appellé directement.
     */
    public Resources()
    {
        super(// Set 'true' in front of language to use as default.
              false ? Resources_fr.FILEPATH :
               true ? Resources_en.FILEPATH :
               null);
    }

    /**
     * Initialise les ressources en
     * utilisant le fichier spécifié.
     *
     * @param  filename Nom du fichier binaire contenant les ressources.
     */
    protected Resources(final String filepath)
    {super(filepath);}

    /**
     * Returns resources in the given locale.
     *
     * @param  local The locale, or <code>null</code> for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     */
    public static Resources getResources(Locale locale) throws MissingResourceException
    {
        if (locale==null) locale = Locale.getDefault();
        return (Resources) getBundle(Resources.class.getName(), locale);
        /*
         * We rely on cache capability of {@link java.util.ResourceBundle}.
         */
    }

    /**
     * Renvoie la ressource associée à la clé spécifiée.
     *
     * @param  key Clé désignant la ressource désirée.
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public static String format(final int key) throws MissingResourceException
    {return getResources(null).getString(key);}

    /**
     * Renvoie la ressource associée à la clé spécifiée en remplaçant
     * toutes les occurences de "{0}" par la valeur de <code>arg0</code>.
     *
     * @param  key Clé désignant la ressource désirée.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public static String format(final int key, final Object arg0) throws MissingResourceException
    {return getResources(null).getString(key, arg0);}

    /**
     * Renvoie la ressource associée à la clé spécifiée en remplaçant toutes
     * les occurences de "{0}" et "{1}" par la valeur de <code>arg0</code>
     * et <code>arg1</code> respectivement.
     *
     * @param  key Clé désignant la ressource désirée.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public static String format(final int key, final Object arg0, final Object arg1) throws MissingResourceException
    {return getResources(null).getString(key, arg0, arg1);}

    /**
     * Renvoie la ressource associée à la clé spécifiée en remplaçant
     * toutes les occurences de "{0}", "{1}" et "{2}" par la valeur de
     * <code>arg0</code>, <code>arg1</code> et <code>arg2</code> respectivement.
     *
     * @param  key Clé désignant la ressource désirée.
     * @param  arg0 Objet dont la valeur remplacera toutes les occurences de "{0}"
     * @param  arg1 Objet dont la valeur remplacera toutes les occurences de "{1}"
     * @param  arg2 Objet dont la valeur remplacera toutes les occurences de "{2}"
     * @return Ressource dans la langue de l'utilisateur.
     * @throws MissingResourceException Si aucune ressource n'est affectée à la clé spécifiée.
     */
    public static String format(final int key, final Object arg0, final Object arg1, final Object arg2) throws MissingResourceException
    {return getResources(null).getString(key, arg0, arg1, arg2);}

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
    public static String format(final int key, final Object arg0, final Object arg1, final Object arg2, final Object arg3) throws MissingResourceException
    {return getResources(null).getString(key, arg0, arg1, arg2, arg3);}

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
    public static String format(final int key, final Object arg0, final Object arg1, final Object arg2, final Object arg3, final Object arg4) throws MissingResourceException
    {return getResources(null).getString(key, arg0, arg1, arg2, arg3, arg4);}

    /**
     * Retourne la ressource associée à la clé spécifiée
     * en la terminant par les caractères ":&nbsp;".
     */
    public static String label(final int key)
    {return getResources(null).getLabel(key);}

    /**
     * Retourne la ressource associée à la clé spécifiée
     * en la terminant par les caractères "...".
     */
    public static String trailing(final int key)
    {return getResources(null).getTrailing(key);}

    /**
     * List resources to the command line. This facility is provided
     * mainly for debugging purpose. Optional command-line arguments
     * are:
     *
     * <blockquote><pre>
     *  <b>-locale</b> <i>name</i>     Locale to be used    (example: "fr_CA")
     *  <b>-encoding</b> <i>name</i>   Output encoding name (example: "cp850")
     * </pre></blockquote>
     */
    public static void main(final String[] args)
    {
        try
        {
            final Console console = new Console(args);
            console.checkRemainingArguments(0);
            getResources(console.locale).list(console.out);
            console.out.flush();
        }
        catch (IllegalArgumentException exception)
        {
            System.err.println(exception.getLocalizedMessage());
        }
        catch (IOException exception)
        {
            // Should not happen
            exception.printStackTrace();
        }
    }
}
