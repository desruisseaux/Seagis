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
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Arrays;

// Entr�s/sorties
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
 * Compilateur des ressources. Un objet <code>Compiler</code> doit �tre cr�� pour
 * chaque langue � compiler.  Il prendra en entr� des r�pertoires qui contiennent
 * (par exemple) des fichiers <code>Resources_fr.properties</code> et produira en
 * sortie un fichier binaire contenant l'ensemble des ressources.
 *
 * NOTE: Cette classe ne fonctionne pas avec le JDK 1.4-beta1,
 *       � cause d'un bug du JDK. Utilisez plut�t le JDK 1.3.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Compiler implements FileFilter, Comparator<String>
{
    /**
     * Ensemble des propri�t�s lues pour la langue de cet objet <code>Compiler</code>.
     * Les cl�s et les valeurs de cet objet {@link Map} sont les cl�s et les valeurs
     * de {@link java.util.ResourceBundle}.
     */
    private final Map<String,String> allProperties = new HashMap<String,String>();

    /**
     * Copie de {@link #allProperties}, mais dans lequel certaines propri�t�s auront 
     * �t� remplac�es par des messages utilisables par {@link MessageFormat}.
     */
    private final Map<String,String> allMessages = new HashMap<String,String>();

    /**
     * Ensembles des cl�s par modules.   Les cl�s de cet objet {@link Map} sont des noms
     * complets d'interface <code>Cl�</code>, tandis que les valeurs sont l'ensemble des
     * constantes � d�clarer dans le fichier <code>Cl�.java</code>. Les valeurs num�riques
     * des constantes sont choisit automatiquement.
     */
    private final Map<String,Set<String>> keySets = new HashMap<String,Set<String>>();

    /**
     * Pr�fix des noms de fichiers � rechercher. Ca sera en g�n�ral
     * <code>resources</code>, ce qui permettra de retenir des noms
     * comme <code>resources_fr.properties</code>.
     */
    private final String prefix = "resources";

    /**
     * Suffix d�signant la langue, avec le '_' s�parateur. Ca sera par
     * exemple <code>_fr</code> pour les ressources en langue fran�aise.
     */
    private final String suffix;

    /**
     * Extension des propri�t�s, avec le point s�parateur.
     * Ca sera en general <code>.properties</code>.
     */
    private final String extension = ".properties";

    /**
     * Nom du fichier source (sans l'extension ".java") dans lequel
     * <code>Compiler</code> �crira les d�clarations des cl�s � l'aide
     * de constantes.
     */
    private final String sourceFilename = "Cl�";

    /**
     * Construit un compilateur pour les ressources dans la langue sp�cifi�e.
     *
     * @param suffix Suffix d�signant la langue, avec le '_' s�parateur.
     *        Ca sera par exemple <code>_fr</code> pour les ressources
     *        en langue fran�aise.
     */
    private Compiler(final String suffix)
    {this.suffix=suffix;}

    /**
     * Ajoute toutes les propri�t�s contenu dans le r�pertoire
     * sp�cifi� ainsi que dans tous ses sous-r�pertoire.
     *
     * @param  directory R�pertoire racine dans lequel chercher les propri�t�s.
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
             * Retient les cl�s qui ont �t� d�finits
             * pour ce r�pertoire en particulier.
             */
            final String source=new File(file.getParentFile(), sourceFilename).getPath().replace('\\', '.').replace('/', '.');
            if (keySets.containsKey(source))
            {
                warning(file, source, "Les propri�t�s ont d�j� �t� d�finits pour ce r�pertoire.");
            }
            else keySets.put(source, properties.keySet());
            /*
             * Copie toutes les propri�t�s dans l'ensemble global
             * {@link #allProperties}, en v�rifiant au passage
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
                    warning(file, key, "La cl� est d�j� utilis�e.");
                    continue;
                }
                if (value.trim().length()==0)
                {
                    warning(file, key, "La valeur ne contient que des blancs.");
                }
                allProperties.put(key, value);
                allMessages  .put(key, value);
                /*
                 * V�rifie la validit� de la propri�t� que l'on vient d'ajouter.  On
                 * v�rifiera si le nombre d'arguments d�clar� dans la cl� correspond
                 * au nombre d'arguments attendus par {@link MessageFormat}.
                 */
                int argumentCount = 0;
                int index=key.lastIndexOf('�');
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
                    // Il est corrig� dans le JDK 1.4.
                    warning(file, key, "La cl� devrait d�clarer "+expected+" argument(s).");
                    continue;
                }
            }
        }
    }

    /**
     * Transforme une cha�ne de caract�res "normal" en patron compatible avec {@link MessageFormat}.
     * Cette transformation consiste notamment � doubler les appostrophes, sauf ceux qui se trouvent
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
                 * d'un niveau. Les guillemets ne seront doubl�s que si on se trouve
                 * au niveau 0. Si l'accolade �tait entre des guillemets, il ne sera
                 * pas pris en compte car il aura �t� saut� lors du passage pr�c�dent
                 * de la boucle.
                 */
                case '{' : level++; last=i; break;
                case '}' : level--; last=i; break;
                case '\'':
                {
                    /*
                     * Si on d�tecte une accolade entre guillemets ('{' ou '}'),
                     * on ignore tout ce bloc et on continue au caract�re qui
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
                         * Si nous n'�tions pas entre des accolades,
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
     * Recherche les contantes ID qui ont d�j� �t� d�finit dans des fichiers <code>Cl�.class</code>.
     * Les constantes ID d�j� existantes seront affect�es aux cl�s correspondantes. Les nouvelles
     * cl�s qui n'avaient pas d�j� une constante ID s'en verront affecter une automatiquement.
     *
     * @return Les constantes ID affect�es � chaques cl�s.
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
                        warning(toSourceFile(classname), key, "Acc�s refus�: "+exception.getLocalizedMessage());
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
         * index aux cl�s qui correspondent aux cha�nes de caract�res les plus courtes.
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
     * Proc�de � la cr�ation du fichier de ressources, ainsi que des
     * fichiers sources <code>Cl�.java</code> dans les r�pertoires
     * appropri�s.
     *
     * @param  directory R�pertoire o� �crire les ressources compil�s.
     * @throws IOException si une erreur est survenue lors de l'�criture.
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
            out.write(" * Liste d'identificateurs pour les ressources.  Cette liste est utilis�e lors"); out.write(lineSeparator);
            out.write(" * de la compilation des classes qui utilisent les ressources, mais ne devrait"); out.write(lineSeparator);
            out.write(" * pas appara�tre dans le code compil�. L'utilisation de noms longs ne devrait"); out.write(lineSeparator);
            out.write(" * donc pas avoir d'impact n�gatif sur la taille du programme.");                 out.write(lineSeparator);
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
     * Indique si le fichier sp�cifi� est un r�pertoire ou un fichier
     * <code>Resources_??.properties</code> dans la langue attendue.
     * La langue est reconnue par le suffix sp�cifi� au constructeur
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
     * Compare la longueur des cha�nes de caract�res associ�es aux cl�s sp�cifi�es.
     * Cette m�thode est utilis�e pour classer les cha�nes en ordre croissant de
     * longueur. En pla�ant les cha�nes les plus courtes au d�but des ressources,
     * on peut accel�rer les acc�s � certaines ressources courantes.
     */
    public int compare(final String cl�1, final String cl�2)
    {
        final int lg1 = allMessages.get(cl�1).length();
        final int lg2 = allMessages.get(cl�2).length();
        if (lg1 < lg2) return -1;
        if (lg1 < lg2) return +1;
        return 0;
    }

    /**
     * Envoie vers le p�riph�rique d'erreur standard {@link System#err}
     * un message d'avertissement pour le fichier et la cl� sp�cifi�e.
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
     * @throws IOException si l'�criture a �chou�e.
     */
    private static void white(final Writer out, int count) throws IOException
    {while (--count>=0) out.write(' ');}

    /**
     * Retourne le nom du fichier source qui
     * correspond au nom de la classe sp�cifi�e.
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
            System.out.println("  [source]      est le r�pertoire source � partir d'o� chercher les fichiers \"resources*.properties\".");
            System.out.println("                Tous les sous-r�pertoires seront aussi balay�s.");
            System.out.println("  [destination] est le r�pertoire dans lequel �crire le fichier \"resources*.dat\".");
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
