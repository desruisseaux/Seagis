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
package fr.ird.sql.image;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cv.Category;

// Base de donn�es
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Images et couleurs
import java.awt.Color;
import net.seas.image.io.PaletteFactory;

// Collections
import java.util.List;
import java.util.ArrayList;

// Entr�s/sorties
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


/**
 * Connection vers une table des cat�gories.
 *
 * @author Martin Desruisseaux
 * @version 1.0
 */
final class CategoryTable extends Table
{
    /**
     * Objet � utiliser pour construire les palettes de couleurs.
     */
    private static final PaletteFactory FACTORY = new PaletteFactory(
            /* parent factory */ null,
            /* class loader   */ CategoryTable.class.getClassLoader(),
            /* root directory */ new File("applicationData/colors"),
            /* extension      */ null, // TODO: Should be ".pal"
            /* character set  */ Charset.forName("ISO-8859-1"),
            /* locale         */ Locale.US);

    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des cat�gories.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par les
     * constantes [@link #NAME}, [@link #UPPER} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] NAME    */ CATEGORIES+".name, "    +
                                /*[02] LOWER   */ CATEGORIES+".lower, "   +
                                /*[03] UPPER   */ CATEGORIES+".upper, "   +
                                /*[04] C0      */ CATEGORIES+".c0, "      +
                                /*[05] C1      */ CATEGORIES+".c1, "      +
                                /*[06] LOG     */ CATEGORIES+".log, "     +
                                /*[07] COLORS  */ CATEGORIES+".colors\n"  +
        
                    "FROM "+CATEGORIES+" WHERE "+CATEGORIES+".band=? ORDER BY "+CATEGORIES+".lower";
                    // "Note: "band" semble �tre un op�rateur pour Access. En utilisant le nom
                    //        complet (c'est-�-dire en sp�cifiant la table), �a r�gle le probl�me.

    /** Num�ro de colonne. */ private static final int NAME    =  1;
    /** Num�ro de colonne. */ private static final int LOWER   =  2;
    /** Num�ro de colonne. */ private static final int UPPER   =  3;
    /** Num�ro de colonne. */ private static final int C0      =  4;
    /** Num�ro de colonne. */ private static final int C1      =  5;
    /** Num�ro de colonne. */ private static final int LOG     =  6;
    /** Num�ro de colonne. */ private static final int COLORS  =  7;
    /** Num�ro d'argument. */ private static final int ARG_ID  =  1;

    /**
     * Requ�te SQL faisant le lien
     * avec la base de donn�es.
     */
    private final PreparedStatement statement;

    /**
     * Construit une table en utilisant la connection sp�cifi�e.
     * @param connection Connection vers une base de donn�es d'images.
     * @throws SQLException si <code>ThemeTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected CategoryTable(final Connection connection) throws SQLException
    {statement = connection.prepareStatement(preferences.get(CATEGORIES, SQL_SELECT));}

    /**
     * Retourne la liste des cat�gories qui appartiennent � la bande sp�cifi�e.
     *
     * @param  bandID Identificateur de la bande pour lequel on veut les cat�gories.
     * @return Les cat�gories de la bande demand�e.
     * @throws SQLException si l'interrogation de la table "Categories" a �chou�.
     */
    public synchronized Category[] getCategories(final int bandID) throws SQLException
    {
        statement.setInt(ARG_ID, bandID);
        final List<Category> categories = new ArrayList<Category>();
        final ResultSet result = statement.executeQuery();
        while (result.next())
        {
            boolean isQuantifiable = true;
            final String    name = result.getString (NAME).intern();
            final int      lower = result.getInt    (LOWER);
            final int      upper = result.getInt    (UPPER);
            final float       c0 = result.getFloat  (C0); isQuantifiable &= !result.wasNull();
            final float       c1 = result.getFloat  (C1); isQuantifiable &= !result.wasNull();
            final boolean    log = result.getBoolean(LOG);
            final String colorID = result.getString (COLORS);
            /*
             * Proc�de maintenant au d�codage du champ "colors". Ce champ contient
             * une cha�ne de caract�re qui indique soit le code RGB d'une couleur
             * uniforme, ou soit l'adresse URL d'une palette de couleurs.
             */
            Color[] colors=null;
            if (colorID!=null) try
            {
                colors = decode(colorID);
            }
            catch (IOException exception)
            {
                throw new IllegalRecordException(CATEGORIES, exception);
            }
            catch (ParseException exception)
            {
                throw new IllegalRecordException(CATEGORIES, exception);
            }
            /*
             * Construit une cat�gorie correspondant �
             * l'enregistrement qui vient d'�tre lu.
             */
            final Category category;
            if (!isQuantifiable) category = new            Category(name, colors, lower, upper+1);
            else if (!log)       category = new            Category(name, colors, lower, upper+1, c0, c1);
            else                 category = new LogarithmicCategory(name, colors, lower, upper+1, c0, c1);
            categories.add(category);
        }
        result.close();
        return categories.toArray(new Category[categories.size()]);
    }

    /**
     * Optient une couleur uniforme ou une palette de couleur. L'argument <code>colors</code>
     * peut �tre un code RGB d'une seule couleur (par exemple "#D2C8A0"), ou un lien URL vers
     * une palette de couleurs (par exemple "applicationData/colors/SST-Nasa.pal").
     *
     * @param  colors Identificateur de la ou les couleurs d�sir�es.
     * @return Palette de couleurs demand�e.
     * @throws IOException si les couleurs n'ont pas pu �tre lues.
     * @throws ParseException si la palette de couleurs a �t� ouvert,
     *         mais qu'elle contient des caract�res qui n'ont pas pus
     *         �tre interpr�t�s.
     */
    private static Color[] decode(String colors) throws IOException, ParseException
    {
        /*
         * Retire les guillements au d�but et � la fin de la cha�ne, s'il y en a.
         * Cette op�ration vise � �viter des probl�mes de compatibilit�s lorsque
         * l'importation des th�mes dans la base des donn�es s'est senti oblig�e
         * de placer des guillemets partout (cas de MySQL sous Linux par exemple).
         */
        if (true)
        {
            colors = colors.trim();
            final int length = colors.length();
            if (length>=2 && colors.charAt(0)=='\"' && colors.charAt(length-1)=='\"')
                colors = colors.substring(1, length-1);
        }
        /*
         * V�rifie si la cha�ne de caract�re repr�sente un code de couleurs
         * unique, comme par exemple "#D2C8A0". Si oui, ce code sera retourn�
         * dans un tableau de longueur 1.
         */
        try
        {
            return new Color[] {Color.decode(colors)};
        }
        catch (NumberFormatException exception)
        {
            /*
             * Le d�codage de la cha�ne a �chou�. C'est peut-�tre
             * parce qu'il s'agit d'un nom de fichier.  On ignore
             * l'erreur et on continue en essayant de d�coder l'URL.
             */
        }
        final URL url=new URL(colors);
        return FACTORY.getColors(url.getPath());
    }

    /**
     * Lib�re les ressources utilis�es par cette table.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {statement.close();}
}
