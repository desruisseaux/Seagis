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
package fr.ird.database.sample.sql;

// J2SE
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * Connexion vers la table repr�sentant un param�tre par une combinaison de d'autres param�tres.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CombinationTable extends Table {
    /**
     * La requ�te SQL servant � interroger la table.
     */
    static final String SQL_SELECT = "SELECT source, position, op�ration, poids, logarithme " +
                                       "FROM " + COMBINATIONS + " WHERE cible=?";

    /** Num�ro de colonne. */ private static final int SOURCE     = 1;
    /** Num�ro de colonne. */ private static final int POSITION   = 2;
    /** Num�ro de colonne. */ private static final int OPERATION  = 3;
    /** Num�ro de colonne. */ private static final int WEIGHT     = 4;
    /** Num�ro de colonne. */ private static final int LOGARITHM  = 5;
    /** Num�ro d'argument. */ private static final int TARGET_ARG = 1;

    /**
     * Construit une nouvelle connexion vers la table des combinaisons.
     *
     * @param  connection La connexion vers la base de donn�es.
     * @throws SQLException si la construction de cette table a �chou�e.
     */
    public CombinationTable(final Connection connection) throws SQLException {
        super(connection.prepareStatement(preferences.get(COMBINATIONS, SQL_SELECT)));
    }

    /**
     * Retourne les composantes du param�tre sp�cifi�. Si ce param�tre n'est pas le r�sultat
     * d'une combinaison de d'autres param�tres, alors cette m�thode retourne <code>null</code>.
     *
     * @param  entry Le param�tre pour lequel on veut les composantes.
     * @return Les composantes du param�tre sp�cifi�, ou <code>null</code> s'il n'y en a pas.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public void getComponents(final ParameterEntry entry) throws SQLException {
        statement.setInt(TARGET_ARG, entry.getID());
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
        }
        results.close();
    }
}
