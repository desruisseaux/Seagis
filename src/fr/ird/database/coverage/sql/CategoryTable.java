/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 */
package fr.ird.database.coverage.sql;

// J2SE dependencies
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.net.URL;
import java.io.IOException;
import java.rmi.RemoteException;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.text.ParseException;
import javax.media.jai.ParameterList;

// OpenGIS dependencies
import org.opengis.referencing.FactoryException;

// Geotools dependencies
import org.geotools.cv.Category;
import org.geotools.ct.MathTransform1D;
import org.geotools.ct.MathTransformFactory;
import org.geotools.util.NumberRange;

// Seagis dependencies
import fr.ird.database.DataBase;
import fr.ird.database.ConfigurationKey;
import fr.ird.database.CatalogException;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.Utilities;


/**
 * Connection vers une table des cat�gories.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class CategoryTable extends Table {
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des cat�gories.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par les
     * constantes {@link #NAME}, {@link #UPPER} et compagnie.
     */
    static final ConfigurationKey SELECT = createKey(CATEGORIES, ResourceKeys.SQL_CATEGORIES,
            "SELECT name, "    +   // [01] NAME
                   "lower, "   +   // [02] LOWER
                   "upper, "   +   // [03] UPPER
                   "c0, "      +   // [04] C0
                   "c1, "      +   // [05] C1
                   "log, "     +   // [06] LOG
                   "colors\n"  +   // [07] COLORS

            "FROM "+SCHEMA+".\""+CATEGORIES+"\" WHERE band=? ORDER BY lower");

    /** Num�ro de colonne. */ private static final int NAME     =  1;
    /** Num�ro de colonne. */ private static final int LOWER    =  2;
    /** Num�ro de colonne. */ private static final int UPPER    =  3;
    /** Num�ro de colonne. */ private static final int C0       =  4;
    /** Num�ro de colonne. */ private static final int C1       =  5;
    /** Num�ro de colonne. */ private static final int LOG      =  6;
    /** Num�ro de colonne. */ private static final int COLORS   =  7;
    /** Num�ro d'argument. */ private static final int ARG_BAND =  1;

    /**
     * Requ�te SQL pour interroger la base de donn�es.
     */
    private final PreparedStatement statement;

    /**
     * Exponential transform in base 10.
     * Will be constructed only when first needed.
     */
    private transient MathTransform1D exponential;

    /**
     * Construit une table en utilisant la connection sp�cifi�e.
     *
     * @param database The database where this table come from.
     * @param connection Connection vers une base de donn�es d'images.
     * @throws RemoteException si <code>ThemeTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected CategoryTable(final CoverageDataBase database,
                            final Connection     connection)
            throws RemoteException
    {
        super(database);
        try {
            statement = connection.prepareStatement(getProperty(SELECT));
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }
    }

    /**
     * Retourne la liste des cat�gories qui appartiennent � la bande sp�cifi�e.
     *
     * @param  band Identificateur de la bande pour lequel on veut les cat�gories.
     * @return Les cat�gories de la bande demand�e.
     * @throws SQLException si l'interrogation de la table "Categories" a �chou�.
     * @throws IllegalRecordException si une incoh�rence a �t� trouv�e dans les enregistrements.
     */
    public synchronized Category[] getCategories(final int band)
            throws SQLException, CatalogException
    {
        statement.setInt(ARG_BAND, band);
        final List<Category> categories = new ArrayList<Category>();
        final ResultSet result = statement.executeQuery();
        while (result.next()) {
            boolean isQuantifiable = true;
            final String    name = result.getString (NAME);
            final int      lower = result.getInt    (LOWER);
            final int      upper = result.getInt    (UPPER);
            final double      c0 = result.getDouble (C0); isQuantifiable &= !result.wasNull();
            final double      c1 = result.getDouble (C1); isQuantifiable &= !result.wasNull();
            final boolean    log = result.getBoolean(LOG);
            final String colorID = result.getString (COLORS);
            /*
             * Proc�de maintenant au d�codage du champ "colors". Ce champ contient
             * une cha�ne de caract�re qui indique soit le code RGB d'une couleur
             * uniforme, ou soit l'adresse URL d'une palette de couleurs.
             */
            Color[] colors = null;
            if (colorID != null) try {
                colors = decode(colorID);
            } catch (IOException exception) {
                throw new IllegalRecordException(CATEGORIES, exception);
            } catch (ParseException exception) {
                throw new IllegalRecordException(CATEGORIES, exception);
            }
            /*
             * Construit une cat�gorie correspondant �
             * l'enregistrement qui vient d'�tre lu.
             */
            Category category;
            final NumberRange range = new NumberRange(lower, upper);
            if (!isQuantifiable) {
                category = new Category(name, colors, range, (MathTransform1D)null);
            } else {
                category = new Category(name, colors, range, c1, c0);
                if (log) {
                    final MathTransformFactory factory = MathTransformFactory.getDefault();
                    if (exponential == null) try {
                        final ParameterList param = factory.getMathTransformProvider("Exponential").getParameterList();
                        param.setParameter("Dimension", 1);
                        param.setParameter("Base", 10.0); // Must be a 'double'
                        exponential = (MathTransform1D) factory.createParameterizedTransform("Exponential", param);
                    } catch (FactoryException exception) {
                        throw new CatalogException(exception);
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
     * peut �tre un code RGB d'une seule couleur (par exemple "#D2C8A0"), ou un lien URL vers
     * une palette de couleurs (par exemple "application-data/colors/SST-Nasa.pal").
     *
     * @param  colors Identificateur de la ou les couleurs d�sir�es.
     * @return Palette de couleurs demand�e.
     * @throws IOException si les couleurs n'ont pas pu �tre lues.
     * @throws ParseException si la palette de couleurs a �t� ouvert,
     *         mais qu'elle contient des caract�res qui n'ont pas pus
     *         �tre interpr�t�s.
     */
    private static Color[] decode(String colors) throws IOException, ParseException {
        /*
         * Retire les guillements au d�but et � la fin de la cha�ne, s'il y en a.
         * Cette op�ration vise � �viter des probl�mes de compatibilit�s lorsque
         * l'importation des th�mes dans la base des donn�es s'est senti oblig�e
         * de placer des guillemets partout.
         */
        if (true) {
            colors = colors.trim();
            final int length = colors.length();
            if (length>=2 && colors.charAt(0)=='"' && colors.charAt(length-1)=='"') {
                colors = colors.substring(1, length-1);
            }
        }
        /*
         * V�rifie si la cha�ne de caract�re repr�sente un code de couleurs
         * unique, comme par exemple "#D2C8A0". Si oui, ce code sera retourn�
         * dans un tableau de longueur 1.
         */
        try {
            return new Color[] {Color.decode(colors)};
        } catch (NumberFormatException exception) {
            /*
             * Le d�codage de la cha�ne a �chou�. C'est peut-�tre
             * parce qu'il s'agit d'un nom de fichier.  On ignore
             * l'erreur et on continue en essayant de d�coder l'URL.
             */
        }
        final URL url = new URL(colors);
        return Utilities.getPaletteFactory().getColors(url.getPath());
    }

    /**
     * Lib�re les ressources utilis�es par cette table.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws CatalogException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws CatalogException {
        try {
            statement.close();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }
}
