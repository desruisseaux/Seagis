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
 * du paysage oc�anique} sont form�s de la combinaison des informations suivantes:
 *
 * <ul>
 *   <li>un  {@linkplain ParameterEntry param�tre environnemental};</li>
 *   <li>une {@linkplain RelativePositionEntry position relative};</li>
 *   <li>une {@linkplain OperationEntry op�ration} appliqu�e sur le param�tre;</li>
 *   <li>une distribution statistique des valeurs obtenues.</li>
 * </ul>
 *
 * Par exemple le param�tre "SST" sur lequel on applique l'op�ration "sobel3",
 * �valu� 5 jours avant la p�che, donnera un descripteur du paysage oc�anique
 * que l'on peut appeler <code>"grSST-05"</code>.
 *
 * Les informations n�cessaires � la construction des descripteurs sont puis�es dans trois tables:
 * {@link ParameterTable}, {@link RelativePositionTable} et {@link OperationTable}. De ces trois
 * tables, {@link ParameterTable} est particuli�re sur plusieurs aspects: elle peut contenir de
 * mani�re indirecte une ref�rence vers cet objet <code>DescriptorTable</code>; sa construction
 * n�cessite une {@linkplain SeriesTable table des s�ries d'images}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class DescriptorTable extends SingletonTable<DescriptorEntry, DescriptorEntry> {
    /**
     * Requ�te SQL pour obtenir un descripteur du paysage oc�anique.
     */
    static final String SQL_SELECT = Table.configuration.get(Configuration.KEY_DESCRIPTORS);
    // static final String SQL_SELECT =
    //         "SELECT nom, position, param�tre, op�ration, distribution, scale, offset, log " +
    //         "FROM "+DESCRIPTORS+" INNER JOIN "    +DISTRIBUTIONS +
    //          " ON "+DESCRIPTORS+".distribution = "+DISTRIBUTIONS+".ID " +
    //         "WHERE nom LIKE ?";

    /** Num�ro de colonne. */ private static final int NAME         = 1;
    /** Num�ro de colonne. */ private static final int POSITION     = 2;
    /** Num�ro de colonne. */ private static final int PARAMETER    = 3;
    /** Num�ro de colonne. */ private static final int OPERATION    = 4;
    /** Num�ro de colonne. */ private static final int DISTRIBUTION = 5;
    /** Num�ro de colonne. */ private static final int SCALE        = 6;
    /** Num�ro de colonne. */ private static final int OFFSET       = 7;
    /** Num�ro de colonne. */ private static final int LOGARITHM    = 8;
    /** Num�ro d'argument. */ private static final int NAME_ARG     = 1;

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
     * La table des param�tres. Cette table devrait �tre sp�cifi�e au moment de la
     * construction de <code>DescriptorEntry</code>.  Si ce n'est pas le cas, elle
     * peut �tre construite la premi�re fois o� elle sera n�cessaire.
     */
    private transient ParameterTable parameters;

    /**
     * Construit une nouvelle connexion vers la table des descripteurs.
     *
     * <strong>NOTE:</strong>  Ce constructeur ne saura pas quel {@link SeriesEntry}
     * donner � la table des param�tres. En cons�quence, il vaux mieux utiliser
     *
     *    <code>parameterTable.getLinearModelTable().getDescriptorTable()</code>
     *
     * si le {@link ParameterTable} est connu.
     *
     * @param  La connexion vers la base de donn�es.
     * @throws SQLException si la construction de cette table a �chou�e.
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
     * strictement r�serv� � {@link ParameterTable}</strong>. Des liens �troits existent entre
     * <code>DescriptorTable</code> et <code>ParameterTable</code>, de sorte que ce dernier peut
     * fort bien choisir de r�utiliser une table d�j� existante plut�t que de cr�er une nouvelle
     * instance de <code>DescriptorTable</code>.
     *
     * @param  parameters La table des param�tres. Notez que cette table
     *         sera ferm�e par {@link #close}.
     * @throws SQLException si la construction de cette table a �chou�e.
     */
    DescriptorTable(final ParameterTable parameters) throws RemoteException {
        super(null);
        this.parameters = parameters;
    }

    /**
     * Retourne la connexion � la base de donn�es.
     *
     * @return La connexion � la base de donn�es.
     * @throws SQLException si la connexion n'a pas pu �tre obtenue.
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
     * Retourne la requ�te SQL � utiliser pour obtenir un descripteur.
     */
    protected String getQuery() {
        return SQL_SELECT;
    }

    /**
     * Retourne la requ�te SQL du type sp�cifi�. Cette m�thode peut �tre appel�e avant
     * {@link #list()}, {@link #getEntry(int)} ou {@link #getEntry(String)}. L'argument
     * <code>type</code> d�pend de laquelle des trois m�thodes cit�es sera ex�cut�e.
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
     * Construit un objet {@link DescriptorEntry} � partir de la ligne courante du
     * {@link ResultSet} specifi�. Cette m�thode est appel�e automatiquement par
     * {@link #executeQuery}.
     *
     * @param  results Le r�sultat de la requ�te SQL.
     * @return Le descripteur du paysage oc�anique.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
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
     * Termine la construction du descripteur sp�cifi�. Cette m�thode est appel�e automatiquement
     * par {@link #getEntry(String)} apr�s qu'il ait termin� sa requ�te SQL. Elle compl�tera le
     * {@linkplain DescriptorEntry#getParameter param�tre environnemental} (dont seul le num�ro
     * ID avait �t� m�moris�) en ex�cutant les requ�tes de {@link ParameterTable}.
     *
     * @param  entry L'entr� � initialiser.
     * @throws SQLException si l'initialisation a �chou�e.
     */
//  protected void postCreateEntry(final DescriptorEntry entry) throws SQLException {
//      super.postCreateEntry(entry);
//      parameters.completeEntry(entry.parameter);
//  }
    // NOTE: M�thode d�sactiv�e car elle pouvait encore causer indirectement un appel r�cursif
    //       � LinearModelTable.getTerms(...), � cause de la succession suivante:
    //
    // linearModels.getTerms --> descriptors.getEntry --> parameters.getEntry --> linearModels.getTerms
    //                                                 ^----------+
    //       En commentant cette fonction, on brise la cha�ne ici ^. L'appel de
    //       parameters.completeEntry(...) se fera plut�t � l'int�rieur de LinearModels.getTerms.
    //       

    /**
     * Retourne la table des param�tres. Cette m�thode ne construira la table que la premi�re
     * fois o� elle sera n�cessaire. La table sera ferm�e par la m�thode {@link #close} de ce
     * <code>DescriptorTable</code>.
     *
     * @param  type Le type de requ�te: {@link #BY_NAME}, {@link #BY_ID} or {@link #LIST}.
     *         Par convention, la valeur 0 est synonyme de {@link #BY_ID}, except� que les
     *         assertions ne seront pas effectu�es afin d'�viter une boucle sans fin avec
     *         {@link ParameterTable#getLinearModelTable}.
     * @return La table des param�tres.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public synchronized ParameterTable getParameterTable(final int type) throws RemoteException {
        if (parameters == null) {
            parameters = new ParameterTable(this, type!=0 ? type : BY_ID);
        }

        assert type==0 || parameters.getLinearModelTable().getDescriptorTable() == this;
        return parameters;
    }

    /**
     * Retourne la table des op�rations. Cette m�thode ne construira la table que la premi�re
     * fois o� elle sera n�cessaire. La table sera ferm�e par la m�thode {@link #close} de ce
     * <code>DescriptorTable</code>.
     *
     * @param  type Le type de requ�te: {@link #BY_NAME}, {@link #BY_ID} or {@link #LIST}.
     * @return La table des op�rations.
     * @throws SQLException si la table n'a pas pu �tre construite.
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
     * Retourne la table des positions relatives. Cette m�thode ne construira la table que la
     * premi�re fois o� elle sera n�cessaire. La table sera ferm�e par la m�thode {@link #close}
     * de ce <code>DescriptorTable</code>.
     *
     * @param  type Le type de requ�te: {@link #BY_NAME}, {@link #BY_ID} or {@link #LIST}.
     * @return La table des positions relatives.
     * @throws SQLException si la table n'a pas pu �tre construite.
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
