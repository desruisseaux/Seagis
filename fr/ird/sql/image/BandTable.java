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
import net.seas.opengis.cv.Category;
import net.seas.opengis.cv.CategoryList;

// Base de donn�es
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Collections
import java.util.List;
import java.util.ArrayList;

// Divers
import javax.units.Unit;
import fr.ird.resources.Resources;


/**
 * Connection vers une table des bandes.
 *
 * @author Martin Desruisseaux
 * @version 1.0
 */
final class BandTable extends Table
{
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des bandes.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
     * les constantes [@link #NAME}, [@link #UPPER} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID      */ BANDS+".ID, "      +
                                /*[02] BAND    */ BANDS+".band, "    +
                                /*[04] UNITS   */ BANDS+".units\n"   +
        
                    "FROM "+BANDS+" WHERE "+BANDS+".format=? ORDER BY "+BANDS+".band";
                    // "Note: "band" semble �tre un op�rateur pour Access. En utilisant le nom
                    //        complet (c'est-�-dire en sp�cifiant la table), �a r�gle le probl�me.

    /** Num�ro de colonne. */ private static final int ID      =  1;
    /** Num�ro de colonne. */ private static final int BAND    =  2;
    /** Num�ro de colonne. */ private static final int UNITS   =  3;
    /** Num�ro d'argument. */ private static final int ARG_ID  =  1;

    /**
     * Requ�te SQL faisant le lien
     * avec la base de donn�es.
     */
    private final PreparedStatement statement;

    /**
     * Connexion vers la table des cat�gories. Cette connexion sera
     * n�c�ssaire pour chaque interrogation de la table des bandes.
     * Elle ne sera construite que lorsqu'elle sera n�cessaire.
     */
    private transient CategoryTable categories;

    /**
     * Construit un objet en utilisant la connection sp�cifi�e.
     * @param connection Connection vers une base de donn�es d'images.
     * @throws SQLException si <code>BandTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected BandTable(final Connection connection) throws SQLException
    {statement = connection.prepareStatement(preferences.get(BANDS, SQL_SELECT));}

    /**
     * Retourne les listes de cat�gories qui se rapportent au format sp�cifi�.
     *
     * @param  formatID Identificateur du format pour lequel on veut les cat�gories.
     * @return Les listes de cat�gories du format demand�. Plusieurs liste
     *         peuvent �tre retourn�es si le format contient plusieurs bandes.
     * @throws SQLException si l'interrogation de la table "Bands" a �chou�.
     */
    public synchronized CategoryList[] getCategoryList(final int formatID) throws SQLException
    {
        statement.setInt(ARG_ID, formatID);

        int                     lastBand = 0;
        final List<CategoryList> mappers = new ArrayList<CategoryList>();
        final ResultSet           result = statement.executeQuery();

        while (result.next())
        {
            final int   bandID = result.getInt   (ID);
            final int     band = result.getInt   (BAND); // Compt�es � partir de 1.
            final String units = result.getString(UNITS);
            /*
             * Obtient les unit�s de cette bande.
             */
            final Unit unit = (units!=null) ? Unit.get(units) : null;
            /*
             * Obtient les th�mes de cette bande.
             */
            if (categories==null)
            {
                categories = new CategoryTable(statement.getConnection());
            }
            final Category[] categoryArray = categories.getCategories(bandID);
            final CategoryList mapper;
            try
            {
                mapper = new CategoryList(categoryArray, unit);
            }
            catch (IllegalArgumentException exception)
            {
                throw new IllegalRecordException(CATEGORIES, exception);
                // L'erreur se trouve bien dans la table CATEGORIES, et non BANDS.
            }
            /*
             * V�rifie que la bande n'avait pas d�j�
             * �t� d�finie, et ajoute cette bande �
             * la liste des bandes.
             */
            if (band-1 != lastBand)
            {
                throw new IllegalRecordException(BANDS, Resources.format(Cl�.NON_CONSECUTIVE_BANDS�2, new Integer(lastBand), new Integer(band)));
            }
            lastBand = band;
            mappers.add((CategoryList)pool.intern(mapper));
        }
        result.close();
        return mappers.toArray(new CategoryList[mappers.size()]);
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (categories!=null)
        {
            categories.close();
            categories=null;
        }
        statement.close();
    }
}