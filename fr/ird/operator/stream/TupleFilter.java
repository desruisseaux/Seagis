/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.operator.stream;

// D�pendences J2SE
import java.util.Locale;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.Writer;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.ParseException;
import javax.vecmath.MismatchedSizeException;

// D�pendences Geotools
import org.geotools.io.LineFormat;
import org.geotools.resources.Arguments;


/**
 * Supprime les doublons dans une matrice de donn�es. Un seuil de tol�rance peut �tre
 * fix�, de sorte que les valeurs num�riques proches sont consid�r�s comme des doublons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class TupleFilter {
    /**
     * Valeur de tol�rance.
     */
    private final double tolerance;

    /**
     * Objet � utiliser pour lire des lignes de donn�es.
     */
    private final LineFormat format;

    /**
     * Ensemble des donn�es qui ont �t� lues jusqu'� maintenant.
     */
    private final Map<Key,Value> data = new LinkedHashMap<Key,Value>();

    /**
     * Construit un filtre avec les valeurs par d�faut.
     */
    public TupleFilter() {
        this(1);
    }

    /**
     * Construit un filtre avec la valeur de tol�rance sp�cifi�e.
     *
     * @param tolerance Valeur de tol�rance.
     */
    public TupleFilter(final double tolerance) {
        this(new LineFormat(), tolerance);
    }

    /**
     * Construit un filtre avec la valeur de tol�rance sp�cifi�e.
     *
     * @param locale Conventions locales � utiliser.
     * @param tolerance Valeur de tol�rance.
     */
    public TupleFilter(final Locale locale, final double tolerance) {
        this(new LineFormat(locale), tolerance);
    }

    /**
     * Construit un filtre avec la valeur de tol�rance sp�cifi�e.
     *
     * @param format Objet � utiliser pour lire des lignes de donn�es.
     * @param tolerance Valeur de tol�rance.
     */
    public TupleFilter(final LineFormat format, final double tolerance) {
        this.format    = format;
        this.tolerance = tolerance;
    }

    /**
     * Proc�de � la lecture de toutes les lignes en provenance du fichier sp�cifi�.
     *
     * @param  file Le fichier ) lire.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws ParseException si des nombres n'ont pas pu �tre interpr�t�s.
     */
    public void read(final File file) throws IOException, ParseException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        read(reader);
        reader.close();
    }

    /**
     * Proc�de � la lecture de toutes les lignes en provenance du flot sp�cifi�.
     * Le flot ne sera pas ferm� � la fin de la lecture.
     *
     * @param  reader Le flot � lire.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws ParseException si des nombres n'ont pas pu �tre interpr�t�s.
     */
    public void read(final BufferedReader reader) throws IOException, ParseException {
        String line;
        double[] values = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() != 0) {
                format.setLine(line);
                values = format.getValues(values);
                add(values);
            }
        }
    }

    /**
     * Ajoute des valeurs a cet ensemble.
     *
     * @param values Valeurs � ajouter.
     */
    protected void add(final double[] values) {
        final Key key = new Key(values, tolerance);
        Value value = data.get(key);
        if (value == null) {
            value = new Value(values);
            data.put(key, value);
        } else {
            value.add(values);
        }
    }

    /**
     * Ecrit toutes les donn�es vers le flot sp�cifi�.
     * Le flot ne sera pas ferm� � la fin de cette op�ration.
     *
     * @param  out Le flot vers lequel �crire les donn�es.
     * @throws IOException si une op�ration d'�criture a �chou�e.
     */
    public void write(final Writer out) throws IOException {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        for (final Value value : data.values()) {
            final double[] values = value.getValues();
            for (int i=0; i<values.length; i++) {
                if (i != 0) {
                    out.write('\t');
                }
                out.write(String.valueOf(values[i]));
            }
            out.write(lineSeparator);
        }
    }

    /**
     * Ecrit toutes les donn�es vers le fichier sp�cifi�.
     *
     * @param  file Le fichier dans lequel �crite les donn�es.
     * @throws IOException si une op�ration d'�criture a �chou�e.
     */
    public void write(final File file) throws IOException {
        final Writer out = new BufferedWriter(new FileWriter(file));
        write(out);
        out.close();
    }

    /**
     * Ex�cute cet utilitaire � partir de la ligne de commande.
     * Les arguments sont:
     *
     * <ul>
     *   <li><code>-in</code> = fichier d'entr�es.</li>
     *   <li><code>-out</code> = fichier de sorties. Si omis, les donn�es seront �crites
     *       vers le p�riph�rique de sortie standard.</li>
     *   <li><code>-tolerance</code> = diff�rence maximale tol�r�es entre deux valeurs
     *       pour les consid�rer similaires.</li>
     * </ul>
     *
     * @param args Liste des arguments transmis sur la ligne de commande.
     */
    public static void main(final String[] args) {
        final Arguments arguments = new Arguments(args);
        try {
            final String  in = arguments.getRequiredString("-in" );
            final String out = arguments.getOptionalString("-out");
            final double tol = arguments.getRequiredDouble("-tolerance");
            final TupleFilter filter = new TupleFilter(Locale.US, tol);
            filter.read(new File(in));
            if (out != null) {
                filter.write(new File(out));
            } else {
                filter.write(arguments.out);
            }
        } catch (Exception exception) {
            exception.printStackTrace(arguments.out);
        }
        arguments.out.flush();
    }

    /**
     * Cl� servant � identifier une ligne de donn�es, avec une certaine tol�rance
     * sur la pr�cision des donn�es.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Key {
        /**
         * Les valeurs enti�res des donn�es.
         */
        private final long[] index;

        /**
         * Construit une cl�.
         */
        public Key(final double[] values, final double tolerance) {
            index = new long[values.length];
            for (int i=0; i<values.length; i++) {
                index[i] = Math.round(values[i] / tolerance);
            }
        }

        /**
         * Retourne un code repr�sentant cette cl�.
         */
        public int hashCode() {
            long code = 0;
            for (int i=0; i<index.length; i++) {
                code += index[i];
            }
            return (int)code ^ (int)(code >>> 32);
        }

        /**
         * V�rifie si cette cl� est identique � l'objet sp�cifi�.
         */
        public boolean equals(final Object object) {
            if (object instanceof Key) {
                return Arrays.equals(index, ((Key)object).index);
            }
            return false;
        }
    }

    /**
     * Une entr� repr�sentant la moyenne de plusieurs lignes de donn�es qui ont �t�
     * d�termin�es suffisament proches.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Value {
        /**
         * La somme des valeurs des donn�es.
         */
        private final double[] sum;

        /**
         * Le nombre de donn�es dans la somme.
         */
        private int count;

        /**
         * Construit une entr� initialis�es avec les valeurs sp�cifi�es.
         */
        public Value(final double[] values) {
            sum = (double[]) values.clone();
            count = 1;
        }

        /**
         * Ajoute les valeurs sp�cifi�es.
         */
        public void add(final double[] values) {
            if (values.length != sum.length) {
                throw new MismatchedSizeException();
            }
            for (int i=0; i<values.length; i++) {
                sum[i] += values[i];
            }
            count++;
        }

        /**
         * Retourne les valeurs de cette entr�e.
         */
        public double[] getValues() {
            final double[] values = new double[sum.length];
            for (int i=0; i<values.length; i++) {
                values[i] = sum[i] / count;
            }
            return values;
        }
    }
}
