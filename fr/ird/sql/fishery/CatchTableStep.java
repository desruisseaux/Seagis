/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.sql.fishery;

// J2SE dependencies
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;


/**
 * La première étape dans la construction d'une table des paramètres environnementaux.
 * Cette étape comprend les données d'un enregistrement de la table des captures. Elles
 * seront jointes à des paramètres environnementaux {@link EnvironmentTableStep} pour
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
     * Requête SQL pour obtenir la table des captures.
     */
    private static final String SQL_SELECT = "SELECT * FROM [?]";

    /**
     * Nom de la table utilisée dans la requête.
     */
    final String table;

    /**
     * Nom de colonnes de la table.
     */
    private String[] columns;

    /**
     * Résultat de la requête, ou <code>null</code> s'ils n'ont pas encore été construit.
     */
    private transient ResultSet result;

    /**
     * Construit une étape pour la table spécifiée.
     *
     * @param  connection Connexion vers la base de données.
     * @param  table Nom de la table ou de la requête des captures à utiliser.
     * @throws SQLException si la connection à la base de données a échouée.
     */
    public CatchTableStep(final Connection connection, final String table) throws SQLException {
        super(connection.prepareStatement(replaceQuestionMark(SQL_SELECT, table)));
        this.table = table;
    }

    /**
     * Retourne les noms de toutes les colonnes de la table.
     * Cette méthode ne retourne jamais <code>null</code>.
     *
     * @return Les noms de colonnes. Pour des raisons de performances, cette méthode retourne
     *         une référence directe vers un tableau interne. <strong>Ne pas modifier</strong>.
     * @throws SQLException si la connection à la base de données a échouée.
     */
    public synchronized String[] getColumns() throws SQLException {
        if (columns == null) {
            result = getResultSet();
        }
        return columns;
    }

    /**
     * Retourne les données.
     *
     * @return Ensemble des données de pêches.
     * @throws SQLException si la connection à la base de données a échouée.
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
     * Libère les ressources utilisées par cet objet.
     *
     * @throws SQLException si un problème est survenu
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
