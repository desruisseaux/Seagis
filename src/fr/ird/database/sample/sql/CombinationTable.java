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
import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;
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
     * La table des paramètres. Cette table a été spécifiée au constructeur et n'appartient
     * pas à cet objet <code>CombinationTable</code>. Elle ne doit donc pas être fermée par
     * la méthode {@link #close}.
     */
    private final ParameterTable parameters;

    /**
     * La table des positions relatives.
     * Ne sera construite que la première fois où elle sera nécessaire.
     */
    private transient RelativePositionTable positions;

    /**
     * La table des opérations.
     * Ne sera construite que la première fois où elle sera nécessaire.
     */
    private transient OperationTable operations;

    /**
     * Construit une nouvelle connexion vers la table des combinaisons.
     *
     * @param  parameters La table des paramètres.
     * @throws SQLException si la construction de cette table a échouée.
     */
    public CombinationTable(final ParameterTable parameters) throws SQLException {
        super(parameters.statement.getConnection().prepareStatement(
              preferences.get(COMBINATIONS, SQL_SELECT)));
        this.parameters = parameters;
    }

    /**
     * Retourne les composantes du paramètre spécifié. Si ce paramètre n'est pas le résultat
     * d'une combinaison de d'autres paramètres, alors cette méthode retourne <code>null</code>.
     *
     * @param  entry Le paramètre pour lequel on veut les composantes.
     * @return Les composantes du paramètre spécifié, ou <code>null</code> s'il n'y en a pas.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public synchronized List<fr.ird.database.sample.ParameterEntry.Component>
           getComponents(final ParameterEntry entry) throws SQLException
    {
        statement.setInt(TARGET_ARG, entry.getID());
        ArrayList<fr.ird.database.sample.ParameterEntry.Component> components = null;
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final int source       = results.getInt(SOURCE);
            final int position     = results.getInt(POSITION);
            final int operation    = results.getInt(OPERATION);
            final double weight    = results.getDouble(WEIGHT);
            final double logarithm = results.getDouble(LOGARITHM);
            if (positions == null) {
                positions = new RelativePositionTable(statement.getConnection(),
                                                      RelativePositionTable.BY_ID);
            }
            if (operations == null) {
                operations = new OperationTable(statement.getConnection(),
                                                OperationTable.BY_ID);
            }
            if (components == null) {
                components = new ArrayList<fr.ird.database.sample.ParameterEntry.Component>();
            }
            components.add(entry.new Component(parameters.getEntry(source),
                                               positions .getEntry(position),
                                               operations.getEntry(operation),
                                               weight, logarithm));
        }
        results.close();
        components.trimToSize();
        return components;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws SQLException {
        if (positions != null) {
            positions.close();
            positions = null;
        }
        if (operations != null) {
            operations.close();
            operations = null;
        }
        super.close();
    }
}
