/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.operator.stream;

// Dépendences J2SE
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

// Dépendences Geotools
import org.geotools.io.LineFormat;
import org.geotools.resources.Arguments;


/**
 * Supprime les doublons dans une matrice de données. Un seuil de tolérance peut être
 * fixé, de sorte que les valeurs numériques proches sont considérés comme des doublons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class TupleFilter {
    /**
     * Valeur de tolérance.
     */
    private final double tolerance;

    /**
     * Objet à utiliser pour lire des lignes de données.
     */
    private final LineFormat format;

    /**
     * Ensemble des données qui ont été lues jusqu'à maintenant.
     */
    private final Map<Key,Value> data = new LinkedHashMap<Key,Value>();

    /**
     * Construit un filtre avec les valeurs par défaut.
     */
    public TupleFilter() {
        this(1);
    }

    /**
     * Construit un filtre avec la valeur de tolérance spécifiée.
     *
     * @param tolerance Valeur de tolérance.
     */
    public TupleFilter(final double tolerance) {
        this(new LineFormat(), tolerance);
    }

    /**
     * Construit un filtre avec la valeur de tolérance spécifiée.
     *
     * @param locale Conventions locales à utiliser.
     * @param tolerance Valeur de tolérance.
     */
    public TupleFilter(final Locale locale, final double tolerance) {
        this(new LineFormat(locale), tolerance);
    }

    /**
     * Construit un filtre avec la valeur de tolérance spécifiée.
     *
     * @param format Objet à utiliser pour lire des lignes de données.
     * @param tolerance Valeur de tolérance.
     */
    public TupleFilter(final LineFormat format, final double tolerance) {
        this.format    = format;
        this.tolerance = tolerance;
    }

    /**
     * Procède à la lecture de toutes les lignes en provenance du fichier spécifié.
     *
     * @param  file Le fichier ) lire.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws ParseException si des nombres n'ont pas pu être interprétés.
     */
    public void read(final File file) throws IOException, ParseException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        read(reader);
        reader.close();
    }

    /**
     * Procède à la lecture de toutes les lignes en provenance du flot spécifié.
     * Le flot ne sera pas fermé à la fin de la lecture.
     *
     * @param  reader Le flot à lire.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws ParseException si des nombres n'ont pas pu être interprétés.
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
     * @param values Valeurs à ajouter.
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
     * Ecrit toutes les données vers le flot spécifié.
     * Le flot ne sera pas fermé à la fin de cette opération.
     *
     * @param  out Le flot vers lequel écrire les données.
     * @throws IOException si une opération d'écriture a échouée.
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
     * Ecrit toutes les données vers le fichier spécifié.
     *
     * @param  file Le fichier dans lequel écrite les données.
     * @throws IOException si une opération d'écriture a échouée.
     */
    public void write(final File file) throws IOException {
        final Writer out = new BufferedWriter(new FileWriter(file));
        write(out);
        out.close();
    }

    /**
     * Exécute cet utilitaire à partir de la ligne de commande.
     * Les arguments sont:
     *
     * <ul>
     *   <li><code>-in</code> = fichier d'entrées.</li>
     *   <li><code>-out</code> = fichier de sorties. Si omis, les données seront écrites
     *       vers le périphérique de sortie standard.</li>
     *   <li><code>-tolerance</code> = différence maximale tolérées entre deux valeurs
     *       pour les considérer similaires.</li>
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
     * Clé servant à identifier une ligne de données, avec une certaine tolérance
     * sur la précision des données.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Key {
        /**
         * Les valeurs entières des données.
         */
        private final long[] index;

        /**
         * Construit une clé.
         */
        public Key(final double[] values, final double tolerance) {
            index = new long[values.length];
            for (int i=0; i<values.length; i++) {
                index[i] = Math.round(values[i] / tolerance);
            }
        }

        /**
         * Retourne un code représentant cette clé.
         */
        public int hashCode() {
            long code = 0;
            for (int i=0; i<index.length; i++) {
                code += index[i];
            }
            return (int)code ^ (int)(code >>> 32);
        }

        /**
         * Vérifie si cette clé est identique à l'objet spécifié.
         */
        public boolean equals(final Object object) {
            if (object instanceof Key) {
                return Arrays.equals(index, ((Key)object).index);
            }
            return false;
        }
    }

    /**
     * Une entré représentant la moyenne de plusieurs lignes de données qui ont été
     * déterminées suffisament proches.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Value {
        /**
         * La somme des valeurs des données.
         */
        private final double[] sum;

        /**
         * Le nombre de données dans la somme.
         */
        private int count;

        /**
         * Construit une entré initialisées avec les valeurs spécifiées.
         */
        public Value(final double[] values) {
            sum = (double[]) values.clone();
            count = 1;
        }

        /**
         * Ajoute les valeurs spécifiées.
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
         * Retourne les valeurs de cette entrée.
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
