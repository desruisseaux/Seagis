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
package fr.ird.database.sample.sql;

// J2SE
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * Connexion vers la table représentant un paramètre par une combinaison de d'autres paramètres.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CombinationTable extends Table {
    /**
     * La requête SQL servant à interroger la table.
     */
    static final String SQL_SELECT = "SELECT source, position, opération, poids, logarithme " +
                                       "FROM " + COMBINATIONS + " WHERE cible=?";

    /** Numéro de colonne. */ private static final int SOURCE     = 1;
    /** Numéro de colonne. */ private static final int POSITION   = 2;
    /** Numéro de colonne. */ private static final int OPERATION  = 3;
    /** Numéro de colonne. */ private static final int WEIGHT     = 4;
    /** Numéro de colonne. */ private static final int LOGARITHM  = 5;
    /** Numéro d'argument. */ private static final int TARGET_ARG = 1;

    /**
     * Construit une nouvelle connexion vers la table des combinaisons.
     *
     * @param  connection La connexion vers la base de données.
     * @throws SQLException si la construction de cette table a échouée.
     */
    public CombinationTable(final Connection connection) throws SQLException {
        super(connection.prepareStatement(preferences.get(COMBINATIONS, SQL_SELECT)));
    }

    /**
     * Retourne les composantes du paramètre spécifié. Si ce paramètre n'est pas le résultat
     * d'une combinaison de d'autres paramètres, alors cette méthode retourne <code>null</code>.
     *
     * @param  entry Le paramètre pour lequel on veut les composantes.
     * @return Les composantes du paramètre spécifié, ou <code>null</code> s'il n'y en a pas.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public void getComponents(final ParameterEntry entry) throws SQLException {
        statement.setInt(TARGET_ARG, entry.getID());
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
        }
        results.close();
    }
}
