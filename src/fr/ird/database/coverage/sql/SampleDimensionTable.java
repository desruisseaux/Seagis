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
package fr.ird.database.coverage.sql;

// Base de données
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Collections
import java.util.List;
import java.util.ArrayList;

// Geotools
import org.geotools.units.Unit;
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;

// Seagis
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.database.IllegalRecordException;


/**
 * Connection vers une table des bandes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SampleDimensionTable extends Table {
    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des bandes.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par
     * les constantes {@link #NAME}, {@link #UPPER} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID      */ "ID, "      +
                                /*[02] BAND    */ "[band], "  +
                                /*[04] UNITS   */ "units\n"   +

                    "FROM "+SAMPLE_DIMENSIONS+" WHERE format=? ORDER BY [band]";
                    // "Note: "band" semble être un opérateur pour Access.

    /** Numéro de colonne. */ private static final int ID      =  1;
    /** Numéro de colonne. */ private static final int BAND    =  2;
    /** Numéro de colonne. */ private static final int UNITS   =  3;
    /** Numéro d'argument. */ private static final int ARG_ID  =  1;

    /**
     * Requète SQL faisant le lien
     * avec la base de données.
     */
    private final PreparedStatement statement;

    /**
     * Connexion vers la table des catégories. Cette connexion sera
     * nécéssaire pour chaque interrogation de la table des bandes.
     * Elle ne sera construite que lorsqu'elle sera nécessaire.
     */
    private transient CategoryTable categories;

    /**
     * Construit un objet en utilisant la connection spécifiée.
     * @param connection Connection vers une base de données d'images.
     * @throws SQLException si <code>SampleDimensionTable</code> n'a pas pu construire sa requête SQL.
     */
    protected SampleDimensionTable(final Connection connection) throws SQLException {
        statement = connection.prepareStatement(PREFERENCES.get(SAMPLE_DIMENSIONS, SQL_SELECT));
    }

    /**
     * Retourne les bandes qui se rapportent au format spécifié.
     *
     * @param  formatID Identificateur du format pour lequel on veut les bandes.
     * @return Les listes des bandes du format demandé.
     * @throws SQLException si l'interrogation de la table "Bands" a échoué.
     */
    public synchronized SampleDimension[] getSampleDimensions(final int formatID) throws SQLException {
        statement.setInt(ARG_ID, formatID);

        int                        lastBand = 0;
        final List<SampleDimension> mappers = new ArrayList<SampleDimension>();
        final ResultSet              result = statement.executeQuery();
        while (result.next()) {
            final int   bandID = result.getInt   (ID);
            final int     band = result.getInt   (BAND); // Comptées à partir de 1.
            final String units = result.getString(UNITS);
            /*
             * Obtient les unités de cette bande.
             */
            Unit unit = null;
            if (units != null) {
                unit = Unit.get(units);
                if (unit != null) {
                    unit = unit.rename(units, null);
                }
            }
            /*
             * Obtient les thèmes de cette bande.
             */
            if (categories==null) {
                categories = new CategoryTable(statement.getConnection());
            }
            final Category[] categoryArray = categories.getCategories(bandID);
            final SampleDimension mapper;
            try {
                mapper = new SampleDimension(categoryArray, unit);
            } catch (IllegalArgumentException exception) {
                throw new IllegalRecordException(CATEGORIES, exception);
                // L'erreur se trouve bien dans la table CATEGORIES, et non SAMPLE_DIMENSIONS.
            }
            /*
             * Vérifie que la bande n'avait pas déjà
             * été définie, et ajoute cette bande à
             * la liste des bandes.
             */
            if (band-1 != lastBand) {
                throw new IllegalRecordException(SAMPLE_DIMENSIONS,
                                Resources.format(ResourceKeys.ERROR_NON_CONSECUTIVE_BANDS_$2,
                                                 new Integer(lastBand), new Integer(band)));
            }
            lastBand = band;
            mappers.add((SampleDimension)POOL.canonicalize(mapper));
        }
        result.close();
        return (SampleDimension[])mappers.toArray(new SampleDimension[mappers.size()]);
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        if (categories != null) {
            categories.close();
            categories=null;
        }
        statement.close();
    }
}
