/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.sql.fishery;

// J2SE dependencies
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;


/**
 * La premi�re �tape dans la construction d'une table des param�tres environnementaux.
 * Cette �tape comprend les donn�es d'un enregistrement de la table des captures. Elles
 * seront jointes � des param�tres environnementaux {@link EnvironmentTableStep} pour
 * former un objet {@link EnvironmentRowSet}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see CatchTableStep
 * @see EnvironmentTableStep
 * @see EnvironmentRowSet
 * @see EnvironmentTable
 */
final class CatchTableStep extends Table {
    /**
     * Requ�te SQL pour obtenir la table des captures.
     */
    private static final String SQL_SELECT = "SELECT * FROM [?]";

    /**
     * Nom de la table utilis�e dans la requ�te.
     */
    final String table;

    /**
     * Nom de colonnes de la table.
     */
    private String[] columns;

    /**
     * R�sultat de la requ�te, ou <code>null</code> s'ils n'ont pas encore �t� construit.
     */
    private transient ResultSet result;

    /**
     * Construit une �tape pour la table sp�cifi�e.
     *
     * @param  connection Connexion vers la base de donn�es.
     * @param  table Nom de la table ou de la requ�te des captures � utiliser.
     * @throws SQLException si la connection � la base de donn�es a �chou�e.
     */
    public CatchTableStep(final Connection connection, final String table) throws SQLException {
        super(connection.prepareStatement(replaceQuestionMark(SQL_SELECT, table)));
        this.table = table;
    }

    /**
     * Retourne les noms de toutes les colonnes de la table.
     * Cette m�thode ne retourne jamais <code>null</code>.
     *
     * @return Les noms de colonnes. Pour des raisons de performances, cette m�thode retourne
     *         une r�f�rence directe vers un tableau interne. <strong>Ne pas modifier</strong>.
     * @throws SQLException si la connection � la base de donn�es a �chou�e.
     */
    public synchronized String[] getColumns() throws SQLException {
        if (columns == null) {
            result = getResultSet();
        }
        return columns;
    }

    /**
     * Retourne les donn�es.
     *
     * @return Ensemble des donn�es de p�ches.
     * @throws SQLException si la connection � la base de donn�es a �chou�e.
     */
    public synchronized ResultSet getResultSet() throws SQLException {
        if (result != null) {
            final ResultSet result = this.result;
            this.result = null;
            return result;
        }
        // Ne pas modifier this.result; il doit rester nul.
        final ResultSet result = statement.executeQuery();
        final ResultSetMetaData meta = result.getMetaData();
        columns = new String[meta.getColumnCount()];
        for (int i=0; i<columns.length; i++) {
            columns[i] = meta.getColumnName(i+1);
        }
        return result;
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        columns = null;
        if (result != null) {
            result.close();
            result = null;
        }
        super.close();
    }
}
