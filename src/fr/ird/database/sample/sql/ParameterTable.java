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
 * Interrogation de la table "Paramètres". Contrairement à la plupart des autres tables,
 * les appels des méthodes <code>getEntry(...)</code> de <code>ParameterTable</code> peuvent être
 * récursifs. En effet, chaque paramètre peut être associé à un modèle linéaire.
 * Or les {@linkplain LinearModelTerm termes du modèle linéaire} sont composés de
 * {@linkplain DescriptorEntry descripteurs du paysage océanique}, eux-mêmes composés
 * d'un {@linkplain ParameterEntry paramètre environnemental}. Si on n'y prend garde,
 * cette récursivité entraîne l'utilisation simultanée de plusieurs {@link ResultSet}
 * sur le même objet {@link #statement}, ce que ne supporte pas tous les pilotes JDBC.
 *
 * Pour contourner le problème, le code risquant d'entraîner une récursivité apparaît
 * dans la méthode {@link #postCreateEntry} plutôt que {@link #createEntry}. Il entraînera
 * indirectement (via {@link DescriptorTable}) l'appel de {@link #getIncompleteEntry}.
 * L'entré incomplète ainsi obtenu devra obligatoirement être complétée plus tard par
 * l'appel de {@link #completeEntry}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterTable
    extends SingletonTable<fr.ird.database.sample.ParameterEntry, ParameterEntry> {
    /**
     * Requête SQL pour obtenir un paramètre environnemental.
     */
    static final String SQL_SELECT = Table.configuration.get(Configuration.KEY_PARAMETERS);
    // static final String SQL_SELECT =
    //         "SELECT ID, nom, séries0, séries1, bande FROM "+PARAMETERS+" WHERE ID=? ORDER BY nom";

    /** Numéro de colonne. */ private static final int ID      = 1;
    /** Numéro de colonne. */ private static final int NAME    = 2;
    /** Numéro de colonne. */ private static final int SERIES0 = 3;
    /** Numéro de colonne. */ private static final int SERIES1 = 4;
    /** Numéro de colonne. */ private static final int BAND    = 5;

    /**
     * La table des séries à utiliser pour construire les objets {@link SeriesEntry}.
     * Cette table ne sera pas fermée par {@link #close}, puisqu'elle n'appartient pas
     * à cet objet <code>ParameterTable</code>.
     */
    private final SeriesTable seriesTable;

    /**
     * La table des modèles linéaires qui ont servit à construire les paramètres.
     * Ne sera construite que la première fois où elle sera nécessaire. Ce champ
     * peut aussi être initialisé lors de la construction si cette construction
     * est effectuée par {@link DescriptorTables}.
     */
    private transient LinearModelTable linearModels;

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
    protected ParameterTable(final Connection connection, final int type,
                             final SeriesTable seriesTable)
            throws RemoteException
    {
        super(connection, type);
        this.seriesTable = seriesTable;
    }

    /**
     * Construit une table des paramètres/opérations en utilisant une table des descripteurs déjà
     * existant. <strong>Ce constructeur est strictement réservé à {@link DescriptorTable}</strong>.
     * Des liens étroits existent entre <code>ParameterTable</code> et <code>DescriptorTable</code>,
     * de sorte que ce dernier peut fort bien choisir de réutiliser une table déjà existante plutôt
     * que de créer une nouvelle instance de <code>ParameterTable</code>.
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
     * Retourne l'instruction SQL à utiliser pour obtenir les paramètres.
     */
    protected String getQuery() {
        return SQL_SELECT;
    }

    /**
     * Complète la construction d'un objet {@link ParameterEntry} avec les informations de la
     * ligne courante du {@link ResultSet} spécifié. Cette méthode procède à la lecture de toutes
     * les colonnes <strong>après</strong> la colonne {@link #ID}, et affecte inconditionnelement
     * les champs correspondant de {@link ParameterEntry}. Cette méthode est appelée par
     * {@link #createEntry} pour la construction normale d'une entrée, ainsi que par
     * {@link #completeEntry(ParameterEntry)} pour compléter une entrée dont seul le
     * numéro ID avait été lu par {@link DescriptorTable#createEntry}.
     *
     * @param entry   L'entré à modifier.
     * @param results Le résultat de la requête {@link #SQL_SELECT}.
     *                La ligne courante servira à initialiser <code>entry</code>.
     * @throws Si l'interrogation de <code>results</code> a échoué.
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
     * Complète les champs du paramètre spécifié, s'ils n'étaient pas déjà complétés. Si cette
     * méthode détermine que la construction de <code>entry</code> n'était pas terminée (ce qui
     * peut être le cas lors de la création d'un {@link DescriptorEntry}), alors une requête sera
     * exécutée pour chercher les informations manquantes. Etant donné que cette méthode peut être
     * utilisée pour compléter des entrés pré-existantes, il est possible que l'entré en question
     * existe déjà (même dans sa forme incomplète) dans la cache {@link #pool}.
     *
     * @param entry L'entré à compléter.
     * @throws Si l'interrogation de <code>results</code> a échoué.
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
     * Retourne l'entré pour le numéro ID spécifié. Si une entré était déjà disponible dans la
     * cache, elle sera retournée. Sinon, une entré incomplète sera créée. Dans tous les cas,
     * la méthode {@link #completeEntry(ParameterEntry)} <strong>devra</strong> être appelée
     * plus tard afin de compléter la construction de l'entrée.
     *
     * @param  id Le numéro unique identifiant le paramètre voulu.
     * @return Le paramètre portant le numéro ID spécifié.
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
     * Retourne un objet {@link ParameterEntry} correspondant à la ligne courante de
     * l'objet {@link ResultSet} spécifié. Tous les champs de {@link ParameterEntry}
     * seront complétés, sauf le modèle linéaire qui apparaît dans une autre table.
     * Ce modèle sera construit lors de l'exécution de {@link #postCreateEntry}.
     *
     * @param  id Le numéro unique identifiant le paramètre voulu.
     * @return Le paramètre portant le numéro ID spécifié.
     * @throws SQLException si la construction du paramètre a échoué.
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
     * Complète la construction de l'entré spécifiée. Cette méthode est appelée après que toutes
     * les requêtes SQL ont été complétées. Elle obtient les informations sur le modèle linéaire
     * ayant servit à calculer le paramètre. Etant donné que ces informations sont codées dans une
     * table séparée, il n'était pas nécessaire de les lires à l'intérieur de {@link #getEntry}.
     * Il fallait au contraire différer la construction du modèle linéaire, car elle nécessitera
     * la lecture de nouveaux paramètres alors que {@link #statement} n'était pas encore libéré.
     *
     * @param  entry L'entré à initialiser.
     * @throws SQLException si l'initialisation a échouée.
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
     * Retourne la table des modèle linéaires associée à cette table des paramètres. Cette
     * table est utilisée par {@link #postCreateEntry} afin de terminer la construction des
     * objets {@link ParameterEntry}. Elle est aussi utilisée pour obtenir de manière indirecte
     * la table {@link DescriptorEntry}.
     *
     * @return La table des modèles linéaires.
     * @throws SQLException si la table n'a pas pu être obtenue.
     */
    protected synchronized LinearModelTable getLinearModelTable() throws RemoteException {
        if (linearModels == null) {
            linearModels = new LinearModelTable(new DescriptorTable(this));
        }
        assert linearModels.getDescriptorTable().getParameterTable(0) == this;
        return linearModels;
    }

    /**
     * Indique si la méthode {@link #list} devrait accepter l'entré spécifiée.
     * Cette méthode cache l'entré désignant la série identitée.
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
