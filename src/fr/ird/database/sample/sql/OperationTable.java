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
 * Interrogation de la table "Op�rations".
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class OperationTable
    extends SingletonTable<fr.ird.database.sample.OperationEntry, OperationEntry>
{
    /**
     * Requ�te SQL pour obtenir le code d'une op�ration.
     */
    static final String SQL_SELECT = configuration.get(Configuration.KEY_OPERATIONS);
    // static final String SQL_SELECT =
    //         "SELECT ID, colonne, pr�fix, op�ration, nom, remarques FROM "+OPERATIONS+" WHERE ID=? ORDER BY ID";

    /** Num�ro de colonne. */ private static final int ID        = 1;
    /** Num�ro de colonne. */ private static final int COLUMN    = 2;
    /** Num�ro de colonne. */ private static final int PREFIX    = 3;
    /** Num�ro de colonne. */ private static final int OPERATION = 4;
    /** Num�ro de colonne. */ private static final int NAME      = 5;
    /** Num�ro de colonne. */ private static final int REMARKS   = 6;

    /**
     * Construit une table des op�rations.
     *
     * @param  connection Connection vers une base de donn�es des �chantillons.
     * @param  type Le type de la requ�te.
     *         Une des constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si <code>OperationTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected OperationTable(final Connection connection, final int type) throws SQLException {
        super(connection, type);
    }

    /**
     * {@inheritDoc}
     */
    protected String getTableName() {
        return OPERATIONS;
    }

    /**
     * Retourne l'instruction SQL � utiliser pour obtenir les op�rations.
     */
    protected String getQuery() {
        return SQL_SELECT;
    }

    /**
     * Retourne une entr�e pour la ligne courante de l'objet {@link ResultSet} sp�cifi�.
     */
    protected OperationEntry createEntry(final ResultSet results) throws SQLException {
        return new OperationEntry(results.getInt   (ID),
                                  results.getString(COLUMN),
                                  results.getString(PREFIX),
                                  results.getString(OPERATION),
                                  results.getString(NAME),
                                  results.getString(REMARKS));
    }
}
