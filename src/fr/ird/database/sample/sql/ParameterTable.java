/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.sample.sql;

// J2SE
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

// Seagis
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.SeriesTable;


/**
 * Interrogation de la table &quot;Param�tres&quot;.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterTable extends ColumnTable<fr.ird.database.sample.ParameterEntry> {
    /**
     * Requ�te SQL pour obtenir le code d'un param�tre environnemental.
     */
    static final String SQL_SELECT =
            "SELECT ID, nom, s�ries0, s�ries1, bande FROM "+PARAMETERS+" WHERE ID=? ORDER BY nom";

    /** Num�ro de colonne. */ private static final int ID        = 1;
    /** Num�ro de colonne. */ private static final int NAME      = 2;
    /** Num�ro de colonne. */ private static final int SERIES    = 3;
    /** Num�ro de colonne. */ private static final int SERIES2   = 4;
    /** Num�ro de colonne. */ private static final int BAND      = 5;

    /**
     * La table des s�ries � utiliser pour construire les objets {@link SeriesEntry}.
     * Cette table ne sera pas ferm�e par {@link #close}, puisqu'elle n'appartient pas
     * � cet objet <code>ParameterTable</code>.
     */
    private final SeriesTable seriesTable;

    /**
     * Construit une table des param�tres/op�rations.
     *
     * @param  connection Connection vers une base de donn�es des �chantillons.
     * @param  type Le type de la requ�te. Une des constantes {@link #LIST},
     *         {@link #BY_ID} ou {@link #BY_NAME}.
     * @param  seriesTable La table des s�ries � utiliser pour construire les objets
     *         {@link SeriesEntry}. Cette table ne sera pas ferm�e par {@link #close},
     *         puisqu'elle n'appartient pas � cet objet <code>ParameterTable</code>.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requ�te SQL.
     */
    ParameterTable(final Connection connection, final int type, final SeriesTable seriesTable)
            throws SQLException
    {
        super(connection, type);
        this.seriesTable = seriesTable;
    }

    /**
     * {@inheritDoc}
     */
    protected String getTableName() {
        return PARAMETERS;
    }

    /**
     * Retourne l'instruction SQL � utiliser pour obtenir les param�tres.
     */
    protected String getQuery() {
        return preferences.get(PARAMETERS, SQL_SELECT);
    }

    /**
     * Retourne la s�rie r�f�renc�e dans la colonne sp�cifi�e, ou <code>null</code>
     * s'il n'y en a pas.
     */
    private final SeriesEntry getSeries(final ResultSet results, final int column)
            throws SQLException
    {
        final int id = results.getInt(column);
        if (results.wasNull()) {
            return null;
        }
        if (seriesTable != null) {
            return seriesTable.getEntry(column);
        }
        return new SeriesEntry() {
            public int    getID()      {return id;}
            public String getName()    {return "Sans nom #"+id;}
            public String getRemarks() {return null;}
            public double getPeriod()  {return Double.NaN;}
        };
    }

    /**
     * Retourne un objet {@link ParameterEntry} correspondant � la ligne courante
     * de l'objet {@link ResultSet} sp�cifi�.
     */
    protected fr.ird.database.sample.ParameterEntry getEntry(final ResultSet results)
            throws SQLException
    {
        return new ParameterEntry(results.getInt(    ID),
                                  results.getString( NAME),
                                  getSeries(results, SERIES),
                                  getSeries(results, SERIES2),
                                  results.getInt(    BAND));
    }
}