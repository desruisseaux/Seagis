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
import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;
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
     * La table des param�tres. Cette table a �t� sp�cifi�e au constructeur et n'appartient
     * pas � cet objet <code>CombinationTable</code>. Elle ne doit donc pas �tre ferm�e par
     * la m�thode {@link #close}.
     */
    private final ParameterTable parameters;

    /**
     * La table des positions relatives.
     * Ne sera construite que la premi�re fois o� elle sera n�cessaire.
     */
    private transient RelativePositionTable positions;

    /**
     * La table des op�rations.
     * Ne sera construite que la premi�re fois o� elle sera n�cessaire.
     */
    private transient OperationTable operations;

    /**
     * Construit une nouvelle connexion vers la table des combinaisons.
     *
     * @param  parameters La table des param�tres.
     * @throws SQLException si la construction de cette table a �chou�e.
     */
    public CombinationTable(final ParameterTable parameters) throws SQLException {
        super(parameters.statement.getConnection().prepareStatement(
              preferences.get(COMBINATIONS, SQL_SELECT)));
        this.parameters = parameters;
    }

    /**
     * Retourne les composantes du param�tre sp�cifi�. Si ce param�tre n'est pas le r�sultat
     * d'une combinaison de d'autres param�tres, alors cette m�thode retourne <code>null</code>.
     *
     * @param  entry Le param�tre pour lequel on veut les composantes.
     * @return Les composantes du param�tre sp�cifi�, ou <code>null</code> s'il n'y en a pas.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
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
