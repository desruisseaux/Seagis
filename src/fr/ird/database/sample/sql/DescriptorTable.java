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
import java.util.Set;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.SeriesTable;


/**
 * Connexion vers la table des descripteurs. Les {@linkplain DescriptorEntry descripteurs
 * du paysage océanique} sont formés de la combinaison des informations suivantes:
 *
 * <ul>
 *   <li>un  {@linkplain ParameterEntry paramètre environnemental};</li>
 *   <li>une {@linkplain RelativePositionEntry position relative};</li>
 *   <li>une {@linkplain OperationEntry opération} appliquée sur le paramètre;</li>
 *   <li>une distribution statistique des valeurs obtenues.</li>
 * </ul>
 *
 * Par exemple le paramètre "SST" sur lequel on applique l'opération "sobel3",
 * évalué 5 jours avant la pêche, donnera un descripteur du paysage océanique
 * que l'on peut appeler <code>"grSST-05"</code>.
 *
 * Les informations nécessaires à la construction des descripteurs sont puisées dans trois tables:
 * {@link ParameterTable}, {@link RelativePositionTable} et {@link OperationTable}. De ces trois
 * tables, {@link ParameterTable} est particulière sur plusieurs aspects: elle peut contenir de
 * manière indirecte une reférence vers cet objet <code>DescriptorTable</code>; sa construction
 * nécessite une {@linkplain SeriesTable table des séries d'images}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class DescriptorTable extends SingletonTable<DescriptorEntry, DescriptorEntry> {
    /**
     * Requête SQL pour obtenir un descripteur du paysage océanique.
     */
    static final String SQL_SELECT = Table.configuration.get(Configuration.KEY_DESCRIPTORS);
    // static final String SQL_SELECT =
    //         "SELECT nom, position, paramètre, opération, distribution, scale, offset, log " +
    //         "FROM "+DESCRIPTORS+" INNER JOIN "    +DISTRIBUTIONS +
    //          " ON "+DESCRIPTORS+".distribution = "+DISTRIBUTIONS+".ID " +
    //         "WHERE nom LIKE ?";

    /** Numéro de colonne. */ private static final int NAME         = 1;
    /** Numéro de colonne. */ private static final int POSITION     = 2;
    /** Numéro de colonne. */ private static final int PARAMETER    = 3;
    /** Numéro de colonne. */ private static final int OPERATION    = 4;
    /** Numéro de colonne. */ private static final int DISTRIBUTION = 5;
    /** Numéro de colonne. */ private static final int SCALE        = 6;
    /** Numéro de colonne. */ private static final int OFFSET       = 7;
    /** Numéro de colonne. */ private static final int LOGARITHM    = 8;
    /** Numéro d'argument. */ private static final int NAME_ARG     = 1;

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
     * La table des paramètres. Cette table devrait être spécifiée au moment de la
     * construction de <code>DescriptorEntry</code>.  Si ce n'est pas le cas, elle
     * peut être construite la première fois où elle sera nécessaire.
     */
    private transient ParameterTable parameters;

    /**
     * Construit une nouvelle connexion vers la table des descripteurs.
     *
     * <strong>NOTE:</strong>  Ce constructeur ne saura pas quel {@link SeriesEntry}
     * donner à la table des paramètres. En conséquence, il vaux mieux utiliser
     *
     *    <code>parameterTable.getLinearModelTable().getDescriptorTable()</code>
     *
     * si le {@link ParameterTable} est connu.
     *
     * @param  La connexion vers la base de données.
     * @throws SQLException si la construction de cette table a échouée.
     */
    protected DescriptorTable(final Connection connection) throws RemoteException {
        super(getPreparedStatement(connection));
    }

    /**
     * Retourne un PreparedStatement.
     */
    private static final java.sql.PreparedStatement getPreparedStatement(final Connection connection) throws RemoteException {
        try {
            return connection.prepareStatement(SQL_SELECT);
        } catch (SQLException e) {
            throw new CatalogException(e);
        }        
    }
    
    /**
     * Construit une nouvelle connexion vers la table des descripteurs. <strong>Ce constructeur est
     * strictement réservé à {@link ParameterTable}</strong>. Des liens étroits existent entre
     * <code>DescriptorTable</code> et <code>ParameterTable</code>, de sorte que ce dernier peut
     * fort bien choisir de réutiliser une table déjà existante plutôt que de créer une nouvelle
     * instance de <code>DescriptorTable</code>.
     *
     * @param  parameters La table des paramètres. Notez que cette table
     *         sera fermée par {@link #close}.
     * @throws SQLException si la construction de cette table a échouée.
     */
    DescriptorTable(final ParameterTable parameters) throws RemoteException {
        super(null);
        this.parameters = parameters;
    }

    /**
     * Retourne la connexion à la base de données.
     *
     * @return La connexion à la base de données.
     * @throws SQLException si la connexion n'a pas pu être obtenue.
     */
    protected Connection getConnection() throws SQLException {
        if (statement == null) {
            return parameters.statement.getConnection();
        }
        return super.getConnection();
    }

    /**
     * Retourne le nom de la table.
     */
    protected String getTableName() {
        return DESCRIPTORS;
    }

    /**
     * Retourne la requête SQL à utiliser pour obtenir un descripteur.
     */
    protected String getQuery() {
        return SQL_SELECT;
    }

    /**
     * Retourne la requête SQL du type spécifié. Cette méthode peut être appelée avant
     * {@link #list()}, {@link #getEntry(int)} ou {@link #getEntry(String)}. L'argument
     * <code>type</code> dépend de laquelle des trois méthodes citées sera exécutée.
     */
    protected String getQuery(final int type) throws SQLException {
        switch (type) {
            case BY_NAME: {
                return getQuery();
            }
            case BY_ID: {
                // Should not happen.
                throw new SQLException("Unsupported operation");
            }
            default: {
                return super.getQuery(type);
            }
        }
    }

    /**
     * Construit un objet {@link DescriptorEntry} à partir de la ligne courante du
     * {@link ResultSet} specifié. Cette méthode est appelée automatiquement par
     * {@link #executeQuery}.
     *
     * @param  results Le résultat de la requête SQL.
     * @return Le descripteur du paysage océanique.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    protected DescriptorEntry createEntry(final ResultSet results) throws RemoteException {
        try {
            final String name       = results.getString (NAME);
            final int position      = results.getInt    (POSITION);
            final int parameter     = results.getInt    (PARAMETER);
            final int operation     = results.getInt    (OPERATION);
            final int distribution  = results.getInt    (DISTRIBUTION);
            final double  scale     = results.getDouble (SCALE);
            final double  offset    = results.getDouble (OFFSET);
            final boolean logarithm = results.getBoolean(LOGARITHM);
            final RelativePositionEntry positionEntry  = getPositionTable (BY_ID).getEntry(position );
            final OperationEntry        operationEntry = getOperationTable(BY_ID).getEntry(operation);
            final ParameterEntry        parameterEntry = getParameterTable(BY_ID).getIncompleteEntry(parameter);
            final DescriptorEntry       entry;
            if (!logarithm) {
                if (scale==1 && offset==0) {
                    entry = new DescriptorEntry(name, parameterEntry, positionEntry, operationEntry,
                                                distribution);
                } else {
                    entry = new DescriptorEntry.Scaled(name, parameterEntry, positionEntry, operationEntry,
                                                       distribution, scale, offset);
                }
                assert 1E-6 >= Math.abs(entry.normalize(0) - (offset)) : offset;
                assert 1E-6 >= Math.abs(entry.normalize(1) - (offset + scale));
            } else {
                entry = new DescriptorEntry.LogScaled(name, parameterEntry, positionEntry, operationEntry,
                                                      distribution, scale, offset);
            }
            return entry;
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Termine la construction du descripteur spécifié. Cette méthode est appelée automatiquement
     * par {@link #getEntry(String)} après qu'il ait terminé sa requête SQL. Elle complètera le
     * {@linkplain DescriptorEntry#getParameter paramètre environnemental} (dont seul le numéro
     * ID avait été mémorisé) en exécutant les requêtes de {@link ParameterTable}.
     *
     * @param  entry L'entré à initialiser.
     * @throws SQLException si l'initialisation a échouée.
     */
//  protected void postCreateEntry(final DescriptorEntry entry) throws SQLException {
//      super.postCreateEntry(entry);
//      parameters.completeEntry(entry.parameter);
//  }
    // NOTE: Méthode désactivée car elle pouvait encore causer indirectement un appel récursif
    //       à LinearModelTable.getTerms(...), à cause de la succession suivante:
    //
    // linearModels.getTerms --> descriptors.getEntry --> parameters.getEntry --> linearModels.getTerms
    //                                                 ^----------+
    //       En commentant cette fonction, on brise la chaîne ici ^. L'appel de
    //       parameters.completeEntry(...) se fera plutôt à l'intérieur de LinearModels.getTerms.
    //       

    /**
     * Retourne la table des paramètres. Cette méthode ne construira la table que la première
     * fois où elle sera nécessaire. La table sera fermée par la méthode {@link #close} de ce
     * <code>DescriptorTable</code>.
     *
     * @param  type Le type de requête: {@link #BY_NAME}, {@link #BY_ID} or {@link #LIST}.
     *         Par convention, la valeur 0 est synonyme de {@link #BY_ID}, excepté que les
     *         assertions ne seront pas effectuées afin d'éviter une boucle sans fin avec
     *         {@link ParameterTable#getLinearModelTable}.
     * @return La table des paramètres.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public synchronized ParameterTable getParameterTable(final int type) throws RemoteException {
        if (parameters == null) {
            parameters = new ParameterTable(this, type!=0 ? type : BY_ID);
        }

        assert type==0 || parameters.getLinearModelTable().getDescriptorTable() == this;
        return parameters;
    }

    /**
     * Retourne la table des opérations. Cette méthode ne construira la table que la première
     * fois où elle sera nécessaire. La table sera fermée par la méthode {@link #close} de ce
     * <code>DescriptorTable</code>.
     *
     * @param  type Le type de requête: {@link #BY_NAME}, {@link #BY_ID} or {@link #LIST}.
     * @return La table des opérations.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public synchronized OperationTable getOperationTable(final int type) throws RemoteException {
        if (operations == null) {
            try {
                operations = new OperationTable(getConnection(), type);
            } catch (SQLException e) {
                throw new CatalogException(e);
            }
        }
        return operations;
    }

    /**
     * Retourne la table des positions relatives. Cette méthode ne construira la table que la
     * première fois où elle sera nécessaire. La table sera fermée par la méthode {@link #close}
     * de ce <code>DescriptorTable</code>.
     *
     * @param  type Le type de requête: {@link #BY_NAME}, {@link #BY_ID} or {@link #LIST}.
     * @return La table des positions relatives.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public synchronized RelativePositionTable getPositionTable(final int type) throws RemoteException {
        if (positions == null) {
            try {
                positions = new RelativePositionTable(getConnection(), type);
            } catch (SQLException e) {
                throw new CatalogException(e);
            }
        }
        return positions;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws RemoteException {
        if (positions != null) {
            positions.close();
            positions = null;
        }
        if (operations != null) {
            operations.close();
            operations = null;
        }
        if (parameters != null) {
            parameters.close();
            parameters = null;
        }
        super.close();
    }
}
