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


/**
 * Interrogation de la table &quot;Op�rations&quot;.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class OperationTable extends ColumnTable<fr.ird.database.sample.OperationEntry> {
    /**
     * Requ�te SQL pour obtenir le code d'une op�ration.
     */
    static final String SQL_SELECT =
            "SELECT colonne, pr�fix, op�ration, nom, remarques FROM "+OPERATIONS+" WHERE colonne=? ORDER BY colonne";

    /** Num�ro de colonne. */ private static final int COLUMN    = 1;
    /** Num�ro de colonne. */ private static final int PREFIX    = 2;
    /** Num�ro de colonne. */ private static final int OPERATION = 3;
    /** Num�ro de colonne. */ private static final int NAME      = 4;
    /** Num�ro de colonne. */ private static final int REMARKS   = 5;

    /**
     * Construit une table des op�rations.
     *
     * @param  connection Connection vers une base de donn�es des �chantillons.
     * @param  type Le type de la requ�te. Une des constantes {@link #LIST},
     *         {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si <code>OperationTable</code> n'a pas pu construire sa requ�te SQL.
     */
    OperationTable(final Connection connection, final int type) throws SQLException {
        super(connection, type);
    }

    /**
     * {@inheritDoc}
     */
    protected String getTableName() {
        return OPERATIONS;
    }

    /**
     * Retourne l'instruction SQL � utiliser pour obtenir les param�tres.
     */
    protected String getQuery() {
        return preferences.get(OPERATIONS, SQL_SELECT);
    }

    /**
     * Retourne la requ�te SQL du type sp�cifi�. Cette m�thode interpr�te BY_NAME
     * comme BY_ID, �tant donn� que la table des op�rations n'a pas de num�ro ID.
     */
    String getQuery(int type) throws SQLException {
        switch (type) {
            case BY_ID:   throw new IllegalArgumentException();
            case BY_NAME: type = BY_ID; // Fall through
            default:      return super.getQuery(type);
        }
    }

    /**
     * Retourne un objet {@link OperationEntry} correspondant � la ligne courante
     * de l'objet {@link ResultSet} sp�cifi�.
     */
    protected fr.ird.database.sample.OperationEntry getEntry(final ResultSet results)
            throws SQLException
    {
        return new OperationEntry(results.getString(COLUMN),
                                  results.getString(PREFIX),
                                  results.getString(OPERATION),
                                  results.getString(NAME),
                                  results.getString(REMARKS));
    }
}
