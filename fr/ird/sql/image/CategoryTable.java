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
package fr.ird.sql.image;

// Geotools dependencies
import org.geotools.cv.Category;
import org.geotools.ct.MathTransform1D;
import org.geotools.ct.MathTransformFactory;

// Base de données
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Collections
import java.util.List;
import java.util.ArrayList;

// Entrés/sorties
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;

// Formatage
import java.util.Locale;
import java.nio.charset.Charset;
import java.text.ParseException;

// Divers
import java.awt.Color;
import fr.ird.util.Utilities;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;


/**
 * Connection vers une table des catégories.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class CategoryTable extends Table {
    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des catégories.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par les
     * constantes {@link #NAME}, {@link #UPPER} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] NAME    */ "name, "    +
                                /*[02] LOWER   */ "lower, "   +
                                /*[03] UPPER   */ "upper, "   +
                                /*[04] C0      */ "c0, "      +
                                /*[05] C1      */ "c1, "      +
                                /*[06] LOG     */ "log, "     +
                                /*[07] COLORS  */ "colors\n"  +
        
                    "FROM "+CATEGORIES+" WHERE [band]=? ORDER BY lower";
                    // "Note: "band" semble être un opérateur pour Access. En utilisant le nom
                    //        complet (c'est-à-dire en spécifiant la table), ça rêgle le problème.

    /** Numéro de colonne. */ private static final int NAME    =  1;
    /** Numéro de colonne. */ private static final int LOWER   =  2;
    /** Numéro de colonne. */ private static final int UPPER   =  3;
    /** Numéro de colonne. */ private static final int C0      =  4;
    /** Numéro de colonne. */ private static final int C1      =  5;
    /** Numéro de colonne. */ private static final int LOG     =  6;
    /** Numéro de colonne. */ private static final int COLORS  =  7;
    /** Numéro d'argument. */ private static final int ARG_ID  =  1;

    /**
     * Requète SQL faisant le lien
     * avec la base de données.
     */
    private final PreparedStatement statement;

    /**
     * Exponential transform in base 10.
     * Will be constructed only when first needed.
     */
    private transient MathTransform1D exponential;

    /**
     * Construit une table en utilisant la connection spécifiée.
     * @param connection Connection vers une base de données d'images.
     * @throws SQLException si <code>ThemeTable</code> n'a pas pu construire sa requête SQL.
     */
    protected CategoryTable(final Connection connection) throws SQLException {
        statement = connection.prepareStatement(preferences.get(CATEGORIES, SQL_SELECT));
    }

    /**
     * Retourne la liste des catégories qui appartiennent à la bande spécifiée.
     *
     * @param  bandID Identificateur de la bande pour lequel on veut les catégories.
     * @return Les catégories de la bande demandée.
     * @throws SQLException si l'interrogation de la table "Categories" a échoué.
     */
    public synchronized Category[] getCategories(final int bandID) throws SQLException {
        statement.setInt(ARG_ID, bandID);
        final List<Category> categories = new ArrayList<Category>();
        final ResultSet result = statement.executeQuery();
        while (result.next()) {
            boolean isQuantifiable = true;
            final String    name = result.getString (NAME).intern();
            final int      lower = result.getInt    (LOWER);
            final int      upper = result.getInt    (UPPER);
            final double      c0 = result.getDouble (C0); isQuantifiable &= !result.wasNull();
            final double      c1 = result.getDouble (C1); isQuantifiable &= !result.wasNull();
            final boolean    log = result.getBoolean(LOG);
            final String colorID = result.getString (COLORS);
            /*
             * Procède maintenant au décodage du champ "colors". Ce champ contient
             * une chaîne de caractère qui indique soit le code RGB d'une couleur
             * uniforme, ou soit l'adresse URL d'une palette de couleurs.
             */
            Color[] colors=null;
            if (colorID != null) try {
                colors = decode(colorID);
            } catch (IOException exception) {
                throw new IllegalRecordException(CATEGORIES, exception);
            } catch (ParseException exception) {
                throw new IllegalRecordException(CATEGORIES, exception);
            }
            /*
             * Construit une catégorie correspondant à
             * l'enregistrement qui vient d'être lu.
             */
            Category category;
            final Range range = new Range(Integer.class, new Integer(lower), new Integer(upper));
            if (!isQuantifiable) {
                category = new Category(name, colors, range, (MathTransform1D)null);
            } else {
                category = new Category(name, colors, range, c1, c0);
                if (log) {
                    final MathTransformFactory factory = MathTransformFactory.getDefault();
                    if (exponential == null) {
                        final ParameterList param = factory.getMathTransformProvider("Exponential").getParameterList();
                        param.setParameter("Dimension", 1);
                        param.setParameter("Base", 10.0); // Must be a 'double'
                        exponential = (MathTransform1D) factory.createParameterizedTransform("Exponential", param);
                    }
                    MathTransform1D tr = category.getSampleToGeophysics();
                    tr = (MathTransform1D) factory.createConcatenatedTransform(tr, exponential);
                    category = new Category(name, colors, range, tr);
                }
            }
            categories.add(category);
        }
        result.close();
        return categories.toArray(new Category[categories.size()]);
    }

    /**
     * Optient une couleur uniforme ou une palette de couleur. L'argument <code>colors</code>
     * peut être un code RGB d'une seule couleur (par exemple "#D2C8A0"), ou un lien URL vers
     * une palette de couleurs (par exemple "application-data/colors/SST-Nasa.pal").
     *
     * @param  colors Identificateur de la ou les couleurs désirées.
     * @return Palette de couleurs demandée.
     * @throws IOException si les couleurs n'ont pas pu être lues.
     * @throws ParseException si la palette de couleurs a été ouvert,
     *         mais qu'elle contient des caractères qui n'ont pas pus
     *         être interprétés.
     */
    private static Color[] decode(String colors) throws IOException, ParseException {
        /*
         * Retire les guillements au début et à la fin de la chaîne, s'il y en a.
         * Cette opération vise à éviter des problèmes de compatibilités lorsque
         * l'importation des thèmes dans la base des données s'est senti obligée
         * de placer des guillemets partout (cas de MySQL sous Linux par exemple).
         */
        if (true) {
            colors = colors.trim();
            final int length = colors.length();
            if (length>=2 && colors.charAt(0)=='\"' && colors.charAt(length-1)=='\"') {
                colors = colors.substring(1, length-1);
            }
        }
        /*
         * Vérifie si la chaîne de caractère représente un code de couleurs
         * unique, comme par exemple "#D2C8A0". Si oui, ce code sera retourné
         * dans un tableau de longueur 1.
         */
        try {
            return new Color[] {Color.decode(colors)};
        } catch (NumberFormatException exception) {
            /*
             * Le décodage de la chaîne a échoué. C'est peut-être
             * parce qu'il s'agit d'un nom de fichier.  On ignore
             * l'erreur et on continue en essayant de décoder l'URL.
             */
        }
        final URL url=new URL(colors);
        return Utilities.getPaletteFactory().getColors(url.getPath());
    }

    /**
     * Libère les ressources utilisées par cette table.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        statement.close();
    }
}
