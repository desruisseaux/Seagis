/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.database.sample.sql;

// J2SE
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * Interrogation de la table "Opérations".
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class OperationTable
    extends SingletonTable<fr.ird.database.sample.OperationEntry, OperationEntry>
{
    /**
     * Requête SQL pour obtenir le code d'une opération.
     */
    static final String SQL_SELECT = configuration.get(Configuration.KEY_OPERATIONS);
    // static final String SQL_SELECT =
    //         "SELECT ID, colonne, préfix, opération, nom, remarques FROM "+OPERATIONS+" WHERE ID=? ORDER BY ID";

    /** Numéro de colonne. */ private static final int ID        = 1;
    /** Numéro de colonne. */ private static final int COLUMN    = 2;
    /** Numéro de colonne. */ private static final int PREFIX    = 3;
    /** Numéro de colonne. */ private static final int OPERATION = 4;
    /** Numéro de colonne. */ private static final int NAME      = 5;
    /** Numéro de colonne. */ private static final int REMARKS   = 6;

    /**
     * Construit une table des opérations.
     *
     * @param  connection Connection vers une base de données des échantillons.
     * @param  type Le type de la requête.
     *         Une des constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si <code>OperationTable</code> n'a pas pu construire sa requête SQL.
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
     * Retourne l'instruction SQL à utiliser pour obtenir les opérations.
     */
    protected String getQuery() {
        return SQL_SELECT;
    }

    /**
     * Retourne une entrée pour la ligne courante de l'objet {@link ResultSet} spécifié.
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
