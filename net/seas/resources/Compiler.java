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
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Collections;
import java.util.Arrays;

// Entrés/sorties
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;

// Divers
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.Comparator;


/**
 * Compilateur des ressources. Un objet <code>Compiler</code> doit être créé pour
 * chaque langue à compiler.  Il prendra en entré des répertoires qui contiennent
 * (par exemple) des fichiers <code>Resources_fr.properties</code> et produira en
 * sortie un fichier binaire contenant l'ensemble des ressources.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Compiler implements FileFilter, Comparator<String>
{
    /**
     * Ensemble des propriétés lues pour la langue de cet objet <code>Compiler</code>.
     * Les clés et les valeurs sont celles  qui ont été trouvées dans les fichiers de
     * resources, après avoir remplacé certaines valeurs par des messages utilisables
     * par {@link MessageFormat}.
     */
    private final Map<String,String> allProperties = new HashMap<String,String>();

    /**
     * Ensembles des clés par paquets.   Les clés de cet objet {@link Map} sont des noms
     * complets d'interface <code>Clé</code>, tandis que les valeurs sont l'ensemble des
     * constantes à déclarer dans le fichier <code>Clé.java</code>.
     */
    private final Map<String,Set<String>> keysByPackage = new HashMap<String,Set<String>>();

    /**
     * Valeur numérique a affecter à chacune des clés. Ces valeurs sont calculées
     * seulement après que tous les fichiers <code>.properties</code> ont été lus.
     */
    private Map<String,Integer> keyIDs;

    /**
     * Préfix des noms de fichiers à rechercher. Ca sera en général
     * <code>resources</code>, ce qui permettra de retenir des noms
     * comme <code>resources_fr.properties</code>.
     */
    private static final String prefix = "resources";

    /**
     * Suffix désignant la langue, avec le '_' séparateur. Ca sera par
     * exemple <code>_fr</code> pour les ressources en langue française.
     */
    private final String suffix;

    /**
     * Extension des propriétés, avec le point séparateur.
     * Ca sera en general <code>.properties</code>.
     */
    private static final String extension = ".properties";

    /**
     * Nom du fichier source (sans l'extension ".java") dans lequel
     * <code>Compiler</code> écrira les déclarations des clés à l'aide
     * de constantes.
     */
    private static final String sourceFilename = "Clé";

    /**
     * Construit un compilateur pour les ressources dans la langue spécifiée.
     *
     * @param suffix Suffix désignant la langue, avec le '_' séparateur.
     *        Ca sera par exemple <code>_fr</code> pour les ressources
     *        en langue française.
     * @throws IOException si une erreur de lecture est survenue.
     */
    private Compiler(final String suffix) throws IOException
    {this.suffix=suffix;}

    /**
     * Procède au chargement d'un fichier de propriétées, puis convertit les valeurs
     * en patron compréhensibles par {@link MessageFormat}. La conversion consistera
     * principalement à doubler les appostrophes. Cette méthode vérifiera au passage
     * si on respecte la convention voulant que les noms des étiquettes se terminent
     * par le nombre d'arguments.
     *
     * @param  file Nom du fichier de propriétés à lire.
     * @return Le fichier de propriétés.
     * @throws IOException si la lecture a échouée.
     */
    private static Properties load(final File file) throws IOException
    {
        final InputStream input=new FileInputStream(file);
        final Properties properties=new Properties();
        properties.load(input);
        input.close();

        for (final Iterator it=properties.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry entry = (Map.Entry) it.next();
            final String key      = (String) entry.getKey();
            final String value    = (String) entry.getValue();
            /*
             * Vérifie si le nombre d'arguments déclaré dans la clé correspond
             * au nombre d'arguments attendus par {@link MessageFormat}.
             */
            final MessageFormat message;
            try
            {
                message = new MessageFormat(toMessageFormatString(value));
            }
            catch (IllegalArgumentException exception)
            {
                warning(file, key, "Valeur invalide: "+exception.getLocalizedMessage());
                continue;
            }
            entry.setValue(message.toPattern());
            /*
             * Vérifie le respect de la convention. Le nom-respect entraînera
             * l'affichage d'un avertissement, mais sans plus.
             */
            int argumentCount = 0;
            int index=key.lastIndexOf('¤');
            if (index>=0) try
            {
                argumentCount = Integer.parseInt(key.substring(index+1));
            }
            catch (NumberFormatException exception)
            {
                warning(file, key, "Nombre invalide: "+exception.getLocalizedMessage());
                continue;
            }
            final int expected=message.getFormats().length;
            if (argumentCount!=expected)
            {
                warning(file, key, "La clé devrait déclarer "+expected+" argument(s).");
                continue;
            }
        }
        return properties;
    }

    /**
     * Transforme une chaîne de caractères "normal" en patron compatible avec {@link MessageFormat}.
     * Cette transformation consiste notamment à doubler les appostrophes, sauf ceux qui se trouvent
     * de part et d'autre d'une accolade.
     */
    private static String toMessageFormatString(final String text)
    {
        final StringBuffer buffer = new StringBuffer(text);
search: for (int level=0,last=-1,i=0; i<buffer.length(); i++) // La longueur du buffer va varier.
        {
            switch (buffer.charAt(i))
            {
                /*
                 * Les accolades ouvrantes et fermantes nous font monter et descendre
                 * d'un niveau. Les guillemets ne seront doublés que si on se trouve
                 * au niveau 0. Si l'accolade était entre des guillemets, il ne sera
                 * pas pris en compte car il aura été sauté lors du passage précédent
                 * de la boucle.
                 */
                case '{' : level++; last=i; break;
                case '}' : level--; last=i; break;
                case '\'':
                {
                    /*
                     * Si on détecte une accolade entre guillemets ('{' ou '}'),
                     * on ignore tout ce bloc et on continue au caractère qui
                     * suit le guillemet fermant.
                     */
                    if (i+2<buffer.length() && buffer.charAt(i+2)=='\'')
                    {
                        switch (buffer.charAt(i+1))
                        {
                            case '{': i+=2; continue search;
                            case '}': i+=2; continue search;
                        }
                    }
                    if (level<=0)
                    {
                        /*
                         * Si nous n'étions pas entre des accolades,
                         * alors il faut doubler les guillemets.
                         */
                        buffer.insert(i++, '\'');
                        continue search;
                    }
                    /*
                     * Si on se trouve entre des accolades, on ne doit normalement pas
                     * doubler les guillemets. Toutefois, le format {0,choice,...} est
                     * une exception.
                     */
                    if (last>=0 && buffer.charAt(last)=='{')
                    {
                        int scan=last;
                        do if (scan>=i) continue search;
                        while (Character.isDigit(buffer.charAt(++scan)));
                        final String choice=",choice,";
                        final int end=scan+choice.length();
                        if (end<buffer.length() && buffer.substring(scan, end).equalsIgnoreCase(choice))
                        {
                            buffer.insert(i++, '\'');
                            continue search;
                        }
                    }
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Ajoute toutes les propriétés contenu dans le répertoire
     * spécifié ainsi que dans tous ses sous-répertoire.
     *
     * @param  directory Répertoire racine dans lequel chercher les propriétés.
     * @throws IOException si une erreur de lecture est survenue.
     */
    private void readPropertyFiles(final File directory) throws IOException
    {
        final File[] files=directory.listFiles(this);
        for (int i=0; i<files.length; i++)
        {
            final File file=files[i];
            if (file.isDirectory())
            {
                readPropertyFiles(file);
                continue;
            }
            final Properties properties = load(file);
            final String      classname = new File(file.getParentFile(), sourceFilename).getPath().replace('\\', '.').replace('/', '.');
            Set<String>     packageKeys = keysByPackage.get(classname);
            if (packageKeys==null)
            {
                packageKeys = new HashSet<String>();
                keysByPackage.put(classname, packageKeys);
            }
            /*
             * Copie toutes les propriétés dans l'ensemble global
             * {@link #allProperties}, en vérifiant au passage
             * qu'il n'y a pas de doublons.
             */
            for (final Iterator it=properties.entrySet().iterator(); it.hasNext();)
            {
                final Map.Entry entry = (Map.Entry) it.next();
                final String      key = (String) entry.getKey();
                final String    value = (String) entry.getValue();
                final String oldValue = allProperties.get(key);
                if (oldValue!=null && !oldValue.equals(value))
                {
                    warning(file, key, "La clé est déjà utilisée.");
                    continue;
                }
                if (key.trim().length()==0)
                {
                    warning(file, key, "La clé ne contient que des blancs.");
                    continue;
                }
                if (value.trim().length()==0)
                {
                    warning(file, key, "La valeur ne contient que des blancs.");
                    continue;
                }
                allProperties.put(key, value);
                packageKeys.  add(key);
            }
        }
    }

    /**
     * Recherche les contantes ID qui ont déjà été définit dans des fichiers <code>Clé.class</code>.
     * Les constantes ID déjà existantes seront affectées aux clés correspondantes. Les nouvelles
     * clés qui n'avaient pas déjà une constante ID s'en verront affecter une automatiquement.
     *
     * @param others Autres objets {@link Compiler} à tenir compte pour déterminer les IDs.
     */
    private void initKeyIDs(final Compiler[] others)
    {
        /*
         * Vérifie si les autres objets {@link Compiler} contiendraient
         * des clés qui manquent dans cet objet-ci.
         */
        for (int i=0; i<others.length; i++)
        {
            if (others[i]==this) continue;
            for (final Iterator<Map.Entry<String,Set<String>>> it=others[i].keysByPackage.entrySet().iterator(); it.hasNext();)
            {
                final Map.Entry<String,Set<String>> entry = it.next();
                final String    classname = entry.getKey();
                final Set<String> thatSet = entry.getValue();
                final Set<String> thisSet = keysByPackage.get(classname);
                if (thisSet!=null) thisSet.addAll(thatSet);
                else keysByPackage.put(classname, thatSet);
                for (final Iterator<String> itk=thatSet.iterator(); itk.hasNext();)
                {
                    final String key = itk.next();
                    if (!allProperties.containsKey(key))
                    {
                        warning(null, key, "Clé définit en "+others[i].suffix+" mais pas en "+suffix);
                        allProperties.put(key, null);
                    }
                }
            }
        }
        keyIDs = new HashMap<String,Integer>(2*allProperties.size());
        for (int i=0; i<others.length; i++) others[i].keyIDs=keyIDs;
        /*
         * Procède au chargement des ID déjà existants.
         */
        int nextID = 0;
        for (final Iterator<String> it=keysByPackage.keySet().iterator(); it.hasNext();)
        {
            final String classname = it.next();
            try
            {
                final Field[] fields = Class.forName(classname).getFields();
                Field.setAccessible(fields, true);
                for (int i=fields.length; --i>=0;)
                {
                    final Field field = fields[i];
                    final String  key = field.getName();
                    if (allProperties.containsKey(key)) try
                    {
                        final int        ID = field.getInt(null);
                        final Integer oldID = keyIDs.get(key);
                        if (oldID!=null && oldID.intValue()!=ID)
                        {
                            warning(toSourceFile(classname), key, "ID incompatibles: "+oldID+" et "+ID+'.');
                        }
                        keyIDs.put(key, new Integer(ID));
                        if (ID>=nextID) nextID=ID+1;
                    }
                    catch (IllegalAccessException exception)
                    {
                        warning(toSourceFile(classname), key, "Accès refusé: "+exception.getLocalizedMessage());
                    }
                }
            }
            catch (ClassNotFoundException exception)
            {
                // Ignore.
            }
        }
        /*
         * Construit des index pour ceux qui n'en n'ont pas. On donnera les premiers
         * index aux clés qui correspondent aux chaînes de caractères les plus courtes.
         */
        final String[] keys = allProperties.keySet().toArray(new String[allProperties.size()]);
        Arrays.sort(keys, this);
        for (int i=0; i<keys.length; i++)
        {
            final String key=keys[i];
            if (keyIDs.get(key)==null)
                keyIDs.put(key, new Integer(nextID++));
        }
    }

    /**
     * Procède à la création du fichier de ressources.
     *
     * @param  directory Répertoire où écrire les ressources compilés.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    private void writeResource(final File directory) throws IOException
    {
        int count = 0;
        for (final Iterator<Integer> it=keyIDs.values().iterator(); it.hasNext();)
        {
            final int ID = it.next().intValue();
            if (ID>=count) count=ID+1;
        }
        final String[] strings = new String[count];
        for (final Iterator<Map.Entry<String,String>> it=allProperties.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry<String,String> entry = it.next();
            final int ID = keyIDs.get(entry.getKey()).intValue();
            strings[ID]  = entry.getValue();
        }
        final DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(directory, prefix+suffix+".serialized"))));
        out.writeInt(strings.length);
        for (int i=0; i<strings.length; i++)
        {
            out.writeUTF((strings[i]!=null) ? strings[i] : "");
        }
        out.close();
    }

    /**
     * Procède à la création des fichiers sources
     * <code>Clé.java</code> dans les répertoires
     * appropriés.
     *
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    private void writeJavaSources(final File destination) throws IOException
    {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final String   rootPackage = toRelative(destination).getPath().replace('\\','.').replace('/','.');
        final boolean   rootExists = keysByPackage.containsKey(rootPackage+'.'+sourceFilename);
        for (final Iterator<Map.Entry<String,Set<String>>> it=keysByPackage.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry<String,Set<String>> entry = it.next();
            final String    classname = entry.getKey();
            final File           file = toSourceFile(classname);
            final String  packageName = classname.substring(0, classname.lastIndexOf('.'));
            final Set<String>  keySet = entry.getValue();
            final Writer          out = new BufferedWriter(new FileWriter(file));
            final boolean      isRoot = packageName.equals(rootPackage);
            out.write("/*");                                                                             out.write(lineSeparator);
            out.write(" * This is an automatically generated file. DO NOT EDIT.");                       out.write(lineSeparator);
            out.write(" */");                                                                            out.write(lineSeparator);
            out.write("package "); out.write(packageName); out.write(";");                               out.write(lineSeparator);
                                                                                                         out.write(lineSeparator);
                                                                                                         out.write(lineSeparator);
            out.write("/**");                                                                            out.write(lineSeparator);
            out.write(" * Liste d'identificateurs pour les ressources.  Cette liste est utilisée lors"); out.write(lineSeparator);
            out.write(" * de la compilation des classes qui utilisent les ressources, mais ne devrait"); out.write(lineSeparator);
            out.write(" * pas apparaître dans le code compilé. L'utilisation de noms longs ne devrait"); out.write(lineSeparator);
            out.write(" * donc pas avoir d'impact négatif sur la taille du programme.");                 out.write(lineSeparator);
            out.write(" *");                                                                             out.write(lineSeparator);
            out.write(" * @see net.seas.resources.Compiler");                                            out.write(lineSeparator);
            out.write(" */");                                                                            out.write(lineSeparator);
            if (isRoot) out.write("public ");
            out.write("interface ");
            out.write(sourceFilename);
            if (!isRoot && rootExists)
            {
                out.write(" extends ");
                out.write(rootPackage);
                out.write(".");
                out.write(sourceFilename);
            }
            out.write(lineSeparator);
            out.write("{");
            out.write(lineSeparator);

            final String[] keys = keySet.toArray(new String[keySet.size()]);
            Arrays.sort(keys);
            int maxLength=0;
            for (int i=keys.length; --i>=0;)
            {
                final int length = keys[i].length();
                if (length>maxLength) maxLength=length;
            }
            for (int i=0; i<keys.length; i++)
            {
                final String key    = keys[i];
                final String textID = String.valueOf(keyIDs.get(key));
                white(out, 4);                      out.write("public static final int "); out.write(key);
                white(out, maxLength-key.length()); out.write(" = ");
                white(out, 5-textID.length());      out.write(textID); out.write(";");
                out.write(lineSeparator);
            }
            out.write("}"); out.write(lineSeparator);
            out.close();
        }
    }

    /**
     * Make a file path relative to classpath.
     */
    private static File toRelative(final File path)
    {
        String bestpath = null;
        final String  absolutePath = path.getAbsolutePath();
        final String fileSeparator = System.getProperty("file.separator", "/");
        final String[]  classpaths = Pattern.compile(System.getProperty("path.separator", ":")).split(System.getProperty("java.class.path", "."));
        for (int i=0; i<classpaths.length; i++)
        {
            String classpath = new File(classpaths[i]).getAbsolutePath();
            if (!classpath.endsWith(fileSeparator)) classpath+=fileSeparator;
            if (absolutePath.startsWith(classpath))
            {
                final String candidate = absolutePath.substring(classpath.length());
                if (bestpath==null || bestpath.length()>candidate.length())
                    bestpath = candidate;
            }
        }
        return (bestpath!=null) ? new File(bestpath) : path;
    }

    /**
     * Retourne le nom du fichier source qui
     * correspond au nom de la classe spécifiée.
     */
    private static File toSourceFile(final String classname)
    {return new File(classname.replace('.','/')+".java");}

    /**
     * Ecrit <code>count</code> blancs vers le flot <code>out</code>.
     * @throws IOException si l'écriture a échouée.
     */
    private static void white(final Writer out, int count) throws IOException
    {while (--count>=0) out.write(' ');}

    /**
     * Envoie vers le périphérique d'erreur standard {@link System#err}
     * un message d'avertissement pour le fichier et la clé spécifiée.
     */
    private static void warning(final File file, final String key, final String message)
    {
        System.err.print("ERREUR ");
        if (file!=null)
        {
            String filename = file.getPath();
            if (filename.endsWith(extension))
                filename = filename.substring(0, filename.length()-extension.length());
            System.err.print('(');
            System.err.print(filename);
            System.err.print(')');
        }
        System.err.print(": ");
        if (key!=null)
        {
            System.err.print('"');
            System.err.print(key);
            System.err.print('"');
        }
        System.err.println();
        System.err.println(message);
        System.err.println();
    }

    /**
     * Indique si le fichier spécifié est un répertoire ou un fichier
     * <code>Resources_??.properties</code> dans la langue attendue.
     * La langue est reconnue par le suffix spécifié au constructeur
     * de cet objet <code>Compiler</code>.
     */
    public boolean accept(final File pathname)
    {
        if (pathname.isDirectory()) return true;
        final String filename = pathname.getName();
        if (filename.startsWith(prefix) && filename.endsWith(extension))
        {
            final int lower = prefix.length();
            final int upper = lower+suffix.length();
            if (upper<filename.length() && filename.substring(lower, upper).equals(suffix))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Compare la longueur des chaînes de caractères associées aux clés spécifiées.
     * Cette méthode est utilisée pour classer les chaînes en ordre croissant de
     * longueur. En plaçant les chaînes les plus courtes au début des ressources,
     * on peut accelérer les accès à certaines ressources courantes.
     */
    public int compare(final String clé1, final String clé2)
    {
        final int lg1 = allProperties.get(clé1).length();
        final int lg2 = allProperties.get(clé2).length();
        if (lg1 < lg2) return -1;
        if (lg1 < lg2) return +1;
        return 0;
    }

    /**
     * Compile les ressources de l'application SEAS.
     */
    public static void main(final String[] args) throws IOException
    {
        if (args.length<3)
        {
            System.out.println("Usage: java net.seas.resources.Compiler [source] [destination] [langues]");
            System.out.println("  [source]      est le répertoire source à partir d'où chercher les fichiers \"resources*.properties\".");
            System.out.println("                Tous les sous-répertoires seront aussi balayés.");
            System.out.println("  [destination] est le répertoire dans lequel écrire le fichier \"resources*.serialized\".");
            System.out.println("  [langues]     est un ou plusieurs codes de langues (\"_fr\", \"_en\", etc.).");
        }
        final File          source = new File(args[0]);
        final File     destination = new File(args[1]);
        final Compiler[] compilers = new Compiler[args.length-2];
        for (int i=0; i<compilers.length; i++)
        {
            compilers[i]=new Compiler(args[i+2]);
            compilers[i].readPropertyFiles(source);
        }
        compilers[0].initKeyIDs(compilers);
        for (int i=0; i<compilers.length; i++)
        {
            compilers[i].writeResource(destination);
        }
        compilers[0].writeJavaSources(destination);
    }
}
