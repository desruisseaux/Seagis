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
import java.util.List;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.NoSuchRecordException;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Interrogation de la table "Param�tres". Contrairement � la plupart des autres tables,
 * les appels des m�thodes <code>getEntry(...)</code> de <code>ParameterTable</code> peuvent �tre
 * r�cursifs. En effet, chaque param�tre peut �tre associ� � un mod�le lin�aire.
 * Or les {@linkplain LinearModelTerm termes du mod�le lin�aire} sont compos�s de
 * {@linkplain DescriptorEntry descripteurs du paysage oc�anique}, eux-m�mes compos�s
 * d'un {@linkplain ParameterEntry param�tre environnemental}. Si on n'y prend garde,
 * cette r�cursivit� entra�ne l'utilisation simultan�e de plusieurs {@link ResultSet}
 * sur le m�me objet {@link #statement}, ce que ne supporte pas tous les pilotes JDBC.
 *
 * Pour contourner le probl�me, le code risquant d'entra�ner une r�cursivit� appara�t
 * dans la m�thode {@link #postCreateEntry} plut�t que {@link #createEntry}. Il entra�nera
 * indirectement (via {@link DescriptorTable}) l'appel de {@link #getIncompleteEntry}.
 * L'entr� incompl�te ainsi obtenu devra obligatoirement �tre compl�t�e plus tard par
 * l'appel de {@link #completeEntry}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterTable
    extends SingletonTable<fr.ird.database.sample.ParameterEntry, ParameterEntry> {
    /**
     * Requ�te SQL pour obtenir un param�tre environnemental.
     */
    static final String SQL_SELECT = Table.configuration.get(Configuration.KEY_PARAMETERS);
    // static final String SQL_SELECT =
    //         "SELECT ID, nom, s�ries0, s�ries1, bande FROM "+PARAMETERS+" WHERE ID=? ORDER BY nom";

    /** Num�ro de colonne. */ private static final int ID      = 1;
    /** Num�ro de colonne. */ private static final int NAME    = 2;
    /** Num�ro de colonne. */ private static final int SERIES0 = 3;
    /** Num�ro de colonne. */ private static final int SERIES1 = 4;
    /** Num�ro de colonne. */ private static final int BAND    = 5;

    /**
     * La table des s�ries � utiliser pour construire les objets {@link SeriesEntry}.
     * Cette table ne sera pas ferm�e par {@link #close}, puisqu'elle n'appartient pas
     * � cet objet <code>ParameterTable</code>.
     */
    private final SeriesTable seriesTable;

    /**
     * La table des mod�les lin�aires qui ont servit � construire les param�tres.
     * Ne sera construite que la premi�re fois o� elle sera n�cessaire. Ce champ
     * peut aussi �tre initialis� lors de la construction si cette construction
     * est effectu�e par {@link DescriptorTables}.
     */
    private transient LinearModelTable linearModels;

    /**
     * Construit une table des param�tres/op�rations.
     *
     * @param  connection Connection vers une base de donn�es des �chantillons.
     * @param  type Le type de la requ�te. Une des constantes {@link #LIST},
     *         {@link #BY_ID} ou {@link #BY_NAME}.
     * @param  seriesTable La table des s�ries � utiliser pour construire les objets
     *         {@link SeriesEntry}. Cette table ne sera pas ferm�e par {@link #close},
     *         puisqu'elle n'appartient pas � cet objet <code>ParameterTable</code>.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected ParameterTable(final Connection connection, final int type,
                             final SeriesTable seriesTable)
            throws RemoteException
    {
        super(connection, type);
        this.seriesTable = seriesTable;
    }

    /**
     * Construit une table des param�tres/op�rations en utilisant une table des descripteurs d�j�
     * existant. <strong>Ce constructeur est strictement r�serv� � {@link DescriptorTable}</strong>.
     * Des liens �troits existent entre <code>ParameterTable</code> et <code>DescriptorTable</code>,
     * de sorte que ce dernier peut fort bien choisir de r�utiliser une table d�j� existante plut�t
     * que de cr�er une nouvelle instance de <code>ParameterTable</code>.
     */
    ParameterTable(final DescriptorTable descriptors, final int type) throws RemoteException {
        this(getConnection(descriptors), type, null);
        linearModels = new LinearModelTable(descriptors);
    }

    /**
     * Retourne la connection.
     */
    private static final Connection getConnection(final DescriptorTable descriptors) throws RemoteException {
        try {
            return descriptors.getConnection();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected String getTableName() {
        return PARAMETERS;
    }

    /**
     * Retourne l'instruction SQL � utiliser pour obtenir les param�tres.
     */
    protected String getQuery() {
        return SQL_SELECT;
    }

    /**
     * Compl�te la construction d'un objet {@link ParameterEntry} avec les informations de la
     * ligne courante du {@link ResultSet} sp�cifi�. Cette m�thode proc�de � la lecture de toutes
     * les colonnes <strong>apr�s</strong> la colonne {@link #ID}, et affecte inconditionnelement
     * les champs correspondant de {@link ParameterEntry}. Cette m�thode est appel�e par
     * {@link #createEntry} pour la construction normale d'une entr�e, ainsi que par
     * {@link #completeEntry(ParameterEntry)} pour compl�ter une entr�e dont seul le
     * num�ro ID avait �t� lu par {@link DescriptorTable#createEntry}.
     *
     * @param entry   L'entr� � modifier.
     * @param results Le r�sultat de la requ�te {@link #SQL_SELECT}.
     *                La ligne courante servira � initialiser <code>entry</code>.
     * @throws Si l'interrogation de <code>results</code> a �chou�.
     *
     * @see #completeEntry(ParameterEntry)
     * @see #getIncompleteEntry(int)
     */
    private void completeEntry(final ParameterEntry entry, final ResultSet results) throws RemoteException {
        try {
            assert entry.name == null : entry.name;
            entry.name = results.getString(NAME);
            for (int column=SERIES0; column<=SERIES1; column++) {
                final SeriesEntry series;
                final int id = results.getInt(column);
                if (results.wasNull()) {
                    series = null;
                } else if (seriesTable != null) {
                    series = seriesTable.getEntry(id);
                } else {
                    series = new SeriesEntry() {
                        public int    getID()      {return id;}
                        public String getName()    {return "Sans nom #"+id;}
                        public String getRemarks() {return null;}
                        public double getPeriod()  {return Double.NaN;}
                    };
                }
                switch (column) {
                    case SERIES0: entry.series0 = series; break;
                    case SERIES1: entry.series1 = series; break;
                    default: throw new AssertionError(column);
                }
            }
            entry.band = results.getInt(BAND);
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Compl�te les champs du param�tre sp�cifi�, s'ils n'�taient pas d�j� compl�t�s. Si cette
     * m�thode d�termine que la construction de <code>entry</code> n'�tait pas termin�e (ce qui
     * peut �tre le cas lors de la cr�ation d'un {@link DescriptorEntry}), alors une requ�te sera
     * ex�cut�e pour chercher les informations manquantes. Etant donn� que cette m�thode peut �tre
     * utilis�e pour compl�ter des entr�s pr�-existantes, il est possible que l'entr� en question
     * existe d�j� (m�me dans sa forme incompl�te) dans la cache {@link #pool}.
     *
     * @param entry L'entr� � compl�ter.
     * @throws Si l'interrogation de <code>results</code> a �chou�.
     */
    final void completeEntry(final ParameterEntry entry) throws RemoteException {
        try {
            assert Thread.holdsLock(this);
            final Integer key = new Integer(entry.getID());
            if (entry.name != null) {
                assert pool.get(key) == entry;
            } else try {
                setType(BY_ID);
                statement.setInt(1, entry.getID());
                final ResultSet results = statement.executeQuery();
                if (!results.next()) {
                    results.close();
                    final String table = getTableName();
                    throw new NoSuchRecordException(table, Resources.format(
                              ResourceKeys.ERROR_KEY_NOT_FOUND_$2, table, key));
                }
                completeEntry(entry, results);
                while (results.next()) {
                    if (!entry.equals(createEntry(results))) {
                        results.close();
                        throw new IllegalRecordException(getTableName(),
                                  Resources.format(ResourceKeys.ERROR_DUPLICATED_RECORD_$1, key));
                    }
                }
                results.close();
                postCreateEntry(entry);
            } catch (SQLException exception) {
                pool.remove(key);
                throw exception;
            } catch (RuntimeException exception) {
                pool.remove(key);
                throw exception;
            }
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Retourne l'entr� pour le num�ro ID sp�cifi�. Si une entr� �tait d�j� disponible dans la
     * cache, elle sera retourn�e. Sinon, une entr� incompl�te sera cr��e. Dans tous les cas,
     * la m�thode {@link #completeEntry(ParameterEntry)} <strong>devra</strong> �tre appel�e
     * plus tard afin de compl�ter la construction de l'entr�e.
     *
     * @param  id Le num�ro unique identifiant le param�tre voulu.
     * @return Le param�tre portant le num�ro ID sp�cifi�.
     */
    final ParameterEntry getIncompleteEntry(final int id) {
        assert Thread.holdsLock(this);
        final Integer key = new Integer(id);
        ParameterEntry entry = pool.get(key);
        if (entry == null) {
            entry = new ParameterEntry(id);
            pool.put(key, entry);
        }
        return entry;
    }

    /**
     * Retourne un objet {@link ParameterEntry} correspondant � la ligne courante de
     * l'objet {@link ResultSet} sp�cifi�. Tous les champs de {@link ParameterEntry}
     * seront compl�t�s, sauf le mod�le lin�aire qui appara�t dans une autre table.
     * Ce mod�le sera construit lors de l'ex�cution de {@link #postCreateEntry}.
     *
     * @param  id Le num�ro unique identifiant le param�tre voulu.
     * @return Le param�tre portant le num�ro ID sp�cifi�.
     * @throws SQLException si la construction du param�tre a �chou�.
     */
    protected ParameterEntry createEntry(final ResultSet results) throws RemoteException {
        try {
            final ParameterEntry entry = new ParameterEntry(results.getInt(ID));
            completeEntry(entry, results);
            return entry;
        } catch (SQLException e) {
            throw new CatalogException(e);
        }                                    
    }

    /**
     * Compl�te la construction de l'entr� sp�cifi�e. Cette m�thode est appel�e apr�s que toutes
     * les requ�tes SQL ont �t� compl�t�es. Elle obtient les informations sur le mod�le lin�aire
     * ayant servit � calculer le param�tre. Etant donn� que ces informations sont cod�es dans une
     * table s�par�e, il n'�tait pas n�cessaire de les lires � l'int�rieur de {@link #getEntry}.
     * Il fallait au contraire diff�rer la construction du mod�le lin�aire, car elle n�cessitera
     * la lecture de nouveaux param�tres alors que {@link #statement} n'�tait pas encore lib�r�.
     *
     * @param  entry L'entr� � initialiser.
     * @throws SQLException si l'initialisation a �chou�e.
     */
    protected void postCreateEntry(final ParameterEntry entry) throws RemoteException {
        if (entry.linearModel == null) {
            entry.linearModel = getLinearModelTable().getTerms(entry);
        }
    }

    /**
     * Workaround for being unable to cast T to Timpl in list().
     *
     * @task TODO: try to get ride of this workaround if possible.
     */
    void _postCreateEntry(final fr.ird.database.sample.ParameterEntry entry) throws RemoteException {
        postCreateEntry((ParameterEntry) entry);
    }

    /**
     * Retourne la table des mod�le lin�aires associ�e � cette table des param�tres. Cette
     * table est utilis�e par {@link #postCreateEntry} afin de terminer la construction des
     * objets {@link ParameterEntry}. Elle est aussi utilis�e pour obtenir de mani�re indirecte
     * la table {@link DescriptorEntry}.
     *
     * @return La table des mod�les lin�aires.
     * @throws SQLException si la table n'a pas pu �tre obtenue.
     */
    protected synchronized LinearModelTable getLinearModelTable() throws RemoteException {
        if (linearModels == null) {
            linearModels = new LinearModelTable(new DescriptorTable(this));
        }
        assert linearModels.getDescriptorTable().getParameterTable(0) == this;
        return linearModels;
    }

    /**
     * Indique si la m�thode {@link #list} devrait accepter l'entr� sp�cifi�e.
     * Cette m�thode cache l'entr� d�signant la s�rie identit�e.
     */
    protected boolean accept(final ParameterEntry entry) {
        if (entry.isIdentity()) {
            return false;
        }
        return super.accept(entry);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws RemoteException {
        if (linearModels != null) {
            final Table table = linearModels;
            linearModels = null; // Set to null first to avoid never-ending loop.
            table.close();
        }
        super.close();
    }
}
