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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
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
import java.util.Comparator;


/**
 * Compilateur des ressources. Un objet <code>Compiler</code> doit être créé pour
 * chaque langue à compiler.  Il prendra en entré des répertoires qui contiennent
 * (par exemple) des fichiers <code>Resources_fr.properties</code> et produira en
 * sortie un fichier binaire contenant l'ensemble des ressources.
 *
 * NOTE: Cette classe ne fonctionne pas avec le JDK 1.4-beta1,
 *       à cause d'un bug du JDK. Utilisez plutôt le JDK 1.3.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Compiler implements FileFilter, Comparator<String>
{
    /**
     * Ensemble des propriétés lues pour la langue de cet objet <code>Compiler</code>.
     * Les clés et les valeurs de cet objet {@link Map} sont les clés et les valeurs
     * de {@link java.util.ResourceBundle}.
     */
    private final Map<String,String> allProperties = new HashMap<String,String>();

    /**
     * Copie de {@link #allProperties}, mais dans lequel certaines propriétés auront 
     * été remplacées par des messages utilisables par {@link MessageFormat}.
     */
    private final Map<String,String> allMessages = new HashMap<String,String>();

    /**
     * Ensembles des clés par modules.   Les clés de cet objet {@link Map} sont des noms
     * complets d'interface <code>Clé</code>, tandis que les valeurs sont l'ensemble des
     * constantes à déclarer dans le fichier <code>Clé.java</code>. Les valeurs numériques
     * des constantes sont choisit automatiquement.
     */
    private final Map<String,Set<String>> keySets = new HashMap<String,Set<String>>();

    /**
     * Préfix des noms de fichiers à rechercher. Ca sera en général
     * <code>resources</code>, ce qui permettra de retenir des noms
     * comme <code>resources_fr.properties</code>.
     */
    private final String prefix = "resources";

    /**
     * Suffix désignant la langue, avec le '_' séparateur. Ca sera par
     * exemple <code>_fr</code> pour les ressources en langue française.
     */
    private final String suffix;

    /**
     * Extension des propriétés, avec le point séparateur.
     * Ca sera en general <code>.properties</code>.
     */
    private final String extension = ".properties";

    /**
     * Nom du fichier source (sans l'extension ".java") dans lequel
     * <code>Compiler</code> écrira les déclarations des clés à l'aide
     * de constantes.
     */
    private final String sourceFilename = "Clé";

    /**
     * Construit un compilateur pour les ressources dans la langue spécifiée.
     *
     * @param suffix Suffix désignant la langue, avec le '_' séparateur.
     *        Ca sera par exemple <code>_fr</code> pour les ressources
     *        en langue française.
     */
    private Compiler(final String suffix)
    {this.suffix=suffix;}

    /**
     * Ajoute toutes les propriétés contenu dans le répertoire
     * spécifié ainsi que dans tous ses sous-répertoire.
     *
     * @param  directory Répertoire racine dans lequel chercher les propriétés.
     * @throws IOException si une erreur de lecture est survenue.
     */
    public void read(final File directory) throws IOException
    {
        final File[] files=directory.listFiles(this);
        for (int i=0; i<files.length; i++)
        {
            final File file=files[i];
            if (file.isDirectory())
            {
                read(file);
                continue;
            }
            final InputStream input=new FileInputStream(file);
            final Properties properties=new Properties();
            properties.load(input);
            input.close();
            /*
             * Retient les clés qui ont été définits
             * pour ce répertoire en particulier.
             */
            final String source=new File(file.getParentFile(), sourceFilename).getPath().replace('\\', '.').replace('/', '.');
            if (keySets.containsKey(source))
            {
                warning(file, source, "Les propriétés ont déjà été définits pour ce répertoire.");
            }
            else keySets.put(source, properties.keySet());
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
                if (value.trim().length()==0)
                {
                    warning(file, key, "La valeur ne contient que des blancs.");
                }
                allProperties.put(key, value);
                allMessages  .put(key, value);
                /*
                 * Vérifie la validité de la propriété que l'on vient d'ajouter.  On
                 * vérifiera si le nombre d'arguments déclaré dans la clé correspond
                 * au nombre d'arguments attendus par {@link MessageFormat}.
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
                if (argumentCount!=0)
                    allMessages.put(key, message.toPattern());
                final int expected=message.getFormats().length;
                if (argumentCount!=expected && expected!=10)
                {
                    // Le "!=10" est un workaround pour un bug du JDK 1.3.
                    // Il est corrigé dans le JDK 1.4.
                    warning(file, key, "La clé devrait déclarer "+expected+" argument(s).");
                    continue;
                }
            }
        }
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
     * Recherche les contantes ID qui ont déjà été définit dans des fichiers <code>Clé.class</code>.
     * Les constantes ID déjà existantes seront affectées aux clés correspondantes. Les nouvelles
     * clés qui n'avaient pas déjà une constante ID s'en verront affecter une automatiquement.
     *
     * @return Les constantes ID affectées à chaques clés.
     */
    private Map<String,Integer> getKeyIDs()
    {
        int nextID = 0;
        final Map<String,Integer> keyIDs = new HashMap<String,Integer>(2*allMessages.size());
        for (final Iterator<String> it=keySets.keySet().iterator(); it.hasNext();)
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
                    if (allMessages.containsKey(key)) try
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
        final String[] keys = allMessages.keySet().toArray(new String[allMessages.size()]);
        Arrays.sort(keys, this);
        for (int i=0; i<keys.length; i++)
        {
            final String key=keys[i];
            if (keyIDs.get(key)==null)
                keyIDs.put(key, new Integer(nextID++));
        }
        return keyIDs;
    }

    /**
     * Procède à la création du fichier de ressources, ainsi que des
     * fichiers sources <code>Clé.java</code> dans les répertoires
     * appropriés.
     *
     * @param  directory Répertoire où écrire les ressources compilés.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public void write(final File directory) throws IOException
    {
        final Map<String,Integer> keyIDs = getKeyIDs();
        final String lineSeparator = System.getProperty("line.separator", "\n");
        for (final Iterator<Map.Entry<String,Set<String>>> it=keySets.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry<String,Set<String>> entry = it.next();
            final String    classname = entry.getKey();
            final File           file = toSourceFile(classname);
            final String  packageName = classname.substring(0, classname.lastIndexOf('.'));
            final Set<String>  keySet = entry.getValue();
            final Writer          out = new BufferedWriter(new FileWriter(file));
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
            out.write("interface "); out.write(sourceFilename);                                          out.write(lineSeparator);
            out.write("{");                                                                              out.write(lineSeparator);

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
        /*
         * Enregistre le fichier binaire qui
         * contient l'ensemble des ressources.
         */
        int count = 0;
        for (final Iterator<Integer> it=keyIDs.values().iterator(); it.hasNext();)
        {
            final int ID = it.next().intValue();
            if (ID>=count) count=ID+1;
        }
        final String[] strings = new String[count];
        for (final Iterator<Map.Entry<String,String>> it=allMessages.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry<String,String> entry = it.next();
            final int ID = keyIDs.get(entry.getKey()).intValue();
            strings[ID]  = entry.getValue();
        }
        final DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(directory, prefix+suffix+".dat"))));
        out.writeInt(strings.length);
        for (int i=0; i<strings.length; i++)
        {
            out.writeUTF((strings[i]!=null) ? strings[i] : "");
        }
        out.close();
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
        final int lg1 = allMessages.get(clé1).length();
        final int lg2 = allMessages.get(clé2).length();
        if (lg1 < lg2) return -1;
        if (lg1 < lg2) return +1;
        return 0;
    }

    /**
     * Envoie vers le périphérique d'erreur standard {@link System#err}
     * un message d'avertissement pour le fichier et la clé spécifiée.
     */
    private void warning(final File file, final String key, final String message)
    {
        String filename = file.getPath();
        if (filename.endsWith(extension))
            filename = filename.substring(0, filename.length()-extension.length());
        System.err.println("ERREUR ("+filename+"): \""+key+'"');
        System.err.println(message);
        System.err.println();
    }

    /**
     * Ecrit <code>count</code> blancs vers le flot <code>out</code>.
     * @throws IOException si l'écriture a échouée.
     */
    private static void white(final Writer out, int count) throws IOException
    {while (--count>=0) out.write(' ');}

    /**
     * Retourne le nom du fichier source qui
     * correspond au nom de la classe spécifiée.
     */
    private static File toSourceFile(final String classname)
    {return new File(classname.replace('.','/')+".java");}

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
            System.out.println("  [destination] est le répertoire dans lequel écrire le fichier \"resources*.dat\".");
            System.out.println("  [langues]     est un ou plusieurs codes de langues (\"_fr\", \"_en\", etc.).");
        }
        for (int i=args.length; --i>=2;)
        {
            final Compiler compiler=new Compiler(args[i]);
            compiler.read(new File(args[0]));
            compiler.write(new File(args[1]));
        }
    }
}
