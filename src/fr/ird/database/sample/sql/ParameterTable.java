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
import java.util.List;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

// Seagis
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.SeriesTable;


/**
 * Interrogation de la table &quot;Paramètres&quot;.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterTable extends ColumnTable<ParameterEntry> {
    /**
     * Requête SQL pour obtenir le code d'un paramètre environnemental.
     */
    static final String SQL_SELECT =
            "SELECT ID, nom, séries0, séries1, bande FROM "+PARAMETERS+" WHERE ID=? ORDER BY nom";

    /** Numéro de colonne. */ private static final int ID        = 1;
    /** Numéro de colonne. */ private static final int NAME      = 2;
    /** Numéro de colonne. */ private static final int SERIES    = 3;
    /** Numéro de colonne. */ private static final int SERIES2   = 4;
    /** Numéro de colonne. */ private static final int BAND      = 5;

    /**
     * La table des séries à utiliser pour construire les objets {@link SeriesEntry}.
     * Cette table ne sera pas fermée par {@link #close}, puisqu'elle n'appartient pas
     * à cet objet <code>ParameterTable</code>.
     */
    private final SeriesTable seriesTable;

    /**
     * La table des combinaisons qui ont servit à construire un paramètre.
     * Ne sera construite que la première fois où elle sera nécessaire.
     */
    private transient CombinationTable combinations;

    /**
     * Construit une table des paramètres/opérations.
     *
     * @param  connection Connection vers une base de données des échantillons.
     * @param  type Le type de la requête. Une des constantes {@link #LIST},
     *         {@link #BY_ID} ou {@link #BY_NAME}.
     * @param  seriesTable La table des séries à utiliser pour construire les objets
     *         {@link SeriesEntry}. Cette table ne sera pas fermée par {@link #close},
     *         puisqu'elle n'appartient pas à cet objet <code>ParameterTable</code>.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requête SQL.
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
     * Retourne l'instruction SQL à utiliser pour obtenir les paramètres.
     */
    protected String getQuery() {
        return preferences.get(PARAMETERS, SQL_SELECT);
    }

    /**
     * Retourne la série référencée dans la colonne spécifiée, ou <code>null</code>
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
     * Retourne un objet {@link ParameterEntry} correspondant à la ligne courante
     * de l'objet {@link ResultSet} spécifié.
     */
    protected ParameterEntry getEntry(final ResultSet results) throws SQLException {
        final ParameterEntry entry;
        entry = new ParameterEntry(results.getInt(    ID),
                                   results.getString( NAME),
                                   getSeries(results, SERIES),
                                   getSeries(results, SERIES2),
                                   results.getInt(    BAND));
        if (combinations == null) {
            combinations = new CombinationTable(statement.getConnection());
        }
        entry.initComponents(combinations.getComponents(entry));
        return entry;
    }

    /**
     * Indique si la méthode {@link #list} devrait accepter l'entré spécifiée.
     * Cette méthode cache l'entré qui porte le numéro 0, c'est-à-dire l'entré
     * désignant la série identitée.
     */
    protected boolean accept(final ParameterEntry entry) {
        if (entry.isIdentity()) {
            return false;
        }
        return super.accept(entry);
    }

    /**
     * Initialise l'entré spécifiée. Cette méthode est appelée après que toutes les
     * requêtes SQL ont été complétées. On évite ainsi des appels recursifs (notamment
     * <code>ParameterTable</code> qui interroge {@link ColumnTable}, qui interroge
     * <code>ParameterTable</code>, etc.), ce qui pourraient entraîner la création
     * de plusieurs {@link java.sql.ResultSet}s pour le même {@link java.sql.Statement}.
     *
     * @param  entry L'entré à initialiser.
     * @throws SQLException si l'initialisation a échouée.
     */
    protected void setup(final ParameterEntry entry) throws SQLException {
        final List<ParameterEntry.Component> components = entry.getComponents();
        if (components != null) {
            for (final ParameterEntry.Component component : components) {
                component.initSource(getEntry(component.getSource().getID()));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws SQLException {
        if (combinations != null) {
            combinations.close();
            combinations = null;
        }
        super.close();
    }
}
