/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.bufr;

// Divers
import java.io.*;
import java.util.*;
import org.geotools.resources.Utilities;


/**
 * Classe chargée de compiler les tables ASCII. Les tables BUFR A, B et D seront lues et
 * mémorisées dans des objets {@link Map},  puis enregistrées en binaire dans le fichier
 * "<code>application-data/serialized/BUFR-Tables.dat</code>". Ce fichier binaire aura le
 * contenu suivant (dans l'ordre):
 *
 * <ul>
 *   <li>Un objet {@link Map} qui sera la table A. Les clés seront des objets {@link Byte}
 *       qui représentent le code inscrit dans la section 1 des fichiers BUFR. Les valeurs
 *       seront des objets {@link String}.</li>
 *   <li>Un objet {@link Map} qui combinera les tables B et D. Les clés seront des objets
 *       {@link Short} qui représentent le code FXY (Section 3) non-décompressé (c'est-à-dire
 *       sur leur deux octets originaux). Les valeurs peuvent être un objet {@link Descriptor},
 *       ou un tableau <code>short[]</code> qui contient des clés vers d'autres valeurs de cette
 *       même table.</li>
 * </ul>
 *
 * Ce fichier binaire peut être décodé en utilisant la classe {@link ObjectInputStream}
 * standard du Java.  <strong>Notez que cette classe n'est pas destinée à être utilisée
 * dans la bibliothèque. Elle ne sert qu'à compiler une fois pour toute les tables dans
 * un format binaire.</strong>
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class TableCompiler
{
    /**
     * Nom et chemin du fichier binaire qui contiendra les tables.
     */
    public static final String TABLES = "application-data/serialized/BUFR-Tables.dat";

    /**
     * Lance la compilation des tables BUFR.
     *
     * TODO: - Vérifier s'il n'y a pas des références cycliques dans la table 3.
     *       - Changer le format des tables A et B pour les mettres comme D.
     *       - Remplasser le format actuel des tables par XML?
     */
    public static void main(final String[] args) throws IOException
    {
        final ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(TABLES));
        if (true)
        {
            //////////////////////////////////////////////
            ////                                      ////
            ////  Procède à la lecture de la table A  ////
            ////                                      ////
            //////////////////////////////////////////////
            final Map<Byte,String> table = new HashMap<Byte,String>(31);
            final Properties  properties = new Properties();
            properties.load(getTableInputStream('A'));
            //
            // Copie les entrées dans un objet {@link HashMap},
            // mais en transformant les clés en {@link Byte}.
            //
            for (final Iterator it=properties.entrySet().iterator(); it.hasNext();)
            {
                final Map.Entry<String,String> entry = (Map.Entry) it.next();
                table.put(new Byte(entry.getKey()), entry.getValue());
            }
            out.writeObject(table);
        }
        if (true)
        {
            //////////////////////////////////////////////
            ////                                      ////
            ////  Procède à la lecture de la table B  ////
            ////                                      ////
            //////////////////////////////////////////////
            final Map<Short,Object> table = new HashMap<Short,Object>();
            BufferedReader in = new BufferedReader(new InputStreamReader(getTableInputStream('B')));
            String line; while ((line=in.readLine())!=null)
            {
                line=line.trim();
                final int length=line.length();
                if (length==0 || line.charAt(0)=='#') continue;
                /*
                 * Sépare la ligne en ses éléments (les colonnes sont séparées
                 * par des tabulations, puis place le résultat dans 'table'.
                 */
                String  value=null;
                String    key=null;
                String   name=null;
                String  units=null;
                int     scale=0;
                int reference=0;
                int     width=0;
                int index=0, lower=0, upper;
                do
                {
                    upper = line.indexOf('\t', lower);
                    value = (upper>=0 ? line.substring(lower, upper) : line.substring(lower)).trim();
                    lower = upper+1;
                    switch (index++)
                    {
                        case 0: key       =                  value ; break;
                        case 1: name      =                  value ; break;
                        case 2: units     =                  value ; break;
                        case 3: scale     = Integer.parseInt(value); break;
                        case 4: reference = Integer.parseInt(value); break;
                        case 5: width     = Integer.parseInt(value); break;
                    }
                }
                while (upper>=0);
                if (index<6 || table.put(new Short(getFXYKey(key)), new Descriptor(name,units,scale,reference,width))!=null)
                {
                    throw new IOException(line);
                }
            }
            in.close();
            //////////////////////////////////////////////
            ////                                      ////
            ////  Procède à la lecture de la table D  ////
            ////                                      ////
            //////////////////////////////////////////////
            in=new BufferedReader(new InputStreamReader(getTableInputStream('D')));
            while ((line=in.readLine())!=null)
            {
                line=line.trim();
                final int length=line.length();
                if (length==0 || line.charAt(0)=='#') continue;
                line=line.substring(line.lastIndexOf('"')+1).trim();

                final StringTokenizer tokens = new StringTokenizer(line, " ");
                final Short              key = new Short(getFXYKey(tokens.nextToken()));
                final int              count = Integer.parseInt   (tokens.nextToken()) ;
                final short[]         values = new short[count];
                for (int i=0; i<count; i++) values[i]=getFXYKey(tokens.nextToken());
                if (tokens.hasMoreTokens() || table.put(key, values)!=null)
                {
                    throw new IOException(line);
                }
            }
            in.close();
            out.writeObject(table);
        }
        out.close();
    }

    /**
     * Retourne la clé FXY (Section 3) correspondant à la chaîne
     * de caractère spécifiée. Des exemples de chaines valides
     * seraient "002044", "025042" ou "312201".
     */
    private static short getFXYKey(final String FXY)
    {
        if (FXY.length()==6)
        {
            final int F = Integer.parseInt(FXY.substring(0,1));
            final int X = Integer.parseInt(FXY.substring(1,3));
            final int Y = Integer.parseInt(FXY.substring(3,6));
            final int clé = (F << 14) | (X << 8) | Y;
            if (clé>=0 && clé<65536) return (short)clé;
        }
        throw new IllegalArgumentException(FXY);
    }

    /**
     * Retourne un flot ASCII vers la table BUFR spécifiée.
     *
     * @param  table Lettre de la table à ouvrir.
     * @return Flot ASCII vers la table demandée.
     * @throws FileNotFoundException si la ressource n'a pas été trouvée.
     */
    private static InputStream getTableInputStream(final char table) throws FileNotFoundException
    {
        final String name = "compilerData/bufr/Table-"+table+".txt";
        final InputStream in=TableCompiler.class.getClassLoader().getResourceAsStream(name);
        if (in==null) throw new FileNotFoundException(name);
        return in;
    }

    /**
     * Reformate la table B des fichiers BUFR.
     */
    private static void reformat(final File input, final File output) throws IOException
    {
        final String lineSeparator=System.getProperty("line.separator", "\n");
        final BufferedReader in=new BufferedReader(new FileReader(input));
        final Writer        out=new FileWriter(output);
        String line; while ((line=in.readLine())!=null)
        {
            if (line.trim().length()==0) continue;
            String nextLine=in.readLine();
            if (nextLine==null)
            {
                System.err.println("Il manque une ligne à la fin du fichier.");
                break;
            }
            line = line + Utilities.spaces(80-line.length()) + nextLine;
            final String FXY       = line.substring(  0, 6);
            final String name      = line.substring(  6, 66);
            final String units     = line.substring( 66, 84);
            final String scale     = line.substring( 84, 95);
            final String reference = line.substring( 95,110);
            final String bitCount  = line.substring(110    );

            out.write(FXY);
            out.write('\t');
            out.write(name);
            out.write('\t');
            out.write(units);
            out.write('\t');
            out.write(scale);
            out.write('\t');
            out.write(reference);
            out.write('\t');
            out.write(bitCount);
            out.write(lineSeparator);
        }
        out.close();
        in.close();
    }

    /**
     * Vérifie que les caractères spécifiés sont tous blancs.
     */
    private static void ensureWhite(final String check)
    {
        boolean nonwhite=false;
        for (int i=check.length(); --i>=0;)
            if (check.charAt(i)!=' ')
                nonwhite=true;
        if (nonwhite)
        {
            System.err.print("Caractères invalides: ");
            System.err.println(check);
        }
    }
}
