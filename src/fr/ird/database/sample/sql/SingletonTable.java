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
 */
package fr.ird.database.sample.sql;

// Requêtes SQL
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.rmi.RemoteException;

// Collections
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.CatalogException;
import fr.ird.database.NoSuchRecordException;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.sample.SampleDataBase;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Classe de base des tables dont les méthodes <code>getEntry(...)</code> retourneront un et
 * un seul enregistrement. Les enregistrements de ces tables sont identifiés de façon unique
 * par un numéro ID ou un nom, et font souvent partie d'une clé primaire d'une autre table.
 * Cette classe <code>SingletonTable</code> sert de base aux classes suivantes:
 *
 * <ul>
 *   <li>{@link RelativePositionTable},</li>
 *   <li>{@link ParameterTable},</li>
 *   <li>{@link OperationTable} et</li>
 *   <li>{@link DescriptorTable}.</li>
 * </ul>
 *
 * La classe <code>SingletonTable</code> définie des méthodes {@link #getEntry(int)},
 * {@link #getEntry(String)} et {@link #list()}. En contrepartie, les classes dérivées
 * doivent implémenter les méthodes suivantes:
 *
 * <ul>
 *   <li>{@link #getTableName},
 *               pour retourner le nom de la table;</li>
 *   <li>{@link #getQuery()},
 *               pour retourner l'instruction SQL à utiliser pour obtenir
 *               les données à partir de son numéro ID seulement.</li>
 *   <li>{@link #getQuery(int)} (facultatif),
 *               pour retourner l'instruction SQL à utiliser pour obtenir les données à partir
 *               de son numéro ID ou d'une autre information (habituellement son nom).</li>
 *   <li>{@link #createEntry},
 *               pour construire une entrée à partir de la ligne courante;</li>
 *   <li>{@link #postCreateEntry} (facultatif),
 *               pour achever la construction d'une entrée après que le résultat de la requête
 *               SQL ait été fermé. Particulièrement utile si la phase finale peut impliquer de
 *               nouvelles requêtes sur le même objet {@link #statement}.</li>
 * </ul>
 *
 * Les entrés obtenus lors des appels précédents seront cachés pour un accès plus rapide la
 * prochaine fois qu'une méthode <code>getEntry(...)</code> est appelée avec la même clé.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class SingletonTable<T extends Entry, Timpl extends T> extends Table {
    /**
     * Enumération indiquant que {@link #statement} servira à obtenir la liste des paramètres.
     * C'est le type sélectionné lorsque la méthode {@link #list} est appelée.
     *
     * @see #getQuery(int)
     */
    protected static final int LIST = 1;

    /**
     * Enumération indiquant que {@link #statement} servira à obtenir des paramètres à partir
     * de leur numéro ID. C'est le type sélectionné lorsque la méthode {@link #getEntry(int)}
     * est appelée.
     *
     * @see #getQuery(int)
     */
    protected static final int BY_ID = 2;

    /**
     * Enumération indiquant que {@link #statement} servira à obtenir des paramètres à partir
     * de leur nom. C'est le type sélectionné lorsque la méthode {@link #getEntry(String)} est
     * appelée.
     *
     * @see #getQuery(int)
     */
    protected static final int BY_NAME = 3;

    /**
     * Le type de la requête courante. Sa valeur sera une des
     * constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     *
     * @see #getQuery(int)
     */
    private int type;

    /**
     * Ensemble des entrés déjà obtenues pour chaque nom ou ID. Les clés doivent être soit
     * des objets {@link Integer}, ou soit des objets {@link String}. Aucune autre classe
     * ne devrait être utilisée.
     * <br><br>
     * Note: L'utilisation d'une cache n'est pas forcément souhaitable. Si la base de données
     *       a été mise-à-jour après qu'une entrée est été mise dans la cache, la mise-à-jour
     *       ne sera pas visible. C'est une des raisons pour lesquelles la plupart des tables
     *       n'utilisent pas de cache. Mais les sous-classes de <code>SingletonTable</code>
     *       sont particulières du fais qu'une même entrée sera redemandée plusieurs fois
     *       (notamment pas {@link LinearModelTable}) dans le cadre d'un usage normal de la
     *       base de données. La cache sera détruite chaque fois que la table sera fermée,
     *       et une nouvelle cache reconstruite par exemple à chaque construction d'une nouvelle
     *       table {@link EnvironmentTable}. Ce compromis nous semble acceptable.
     */
    final Map<Object,Timpl> pool = new HashMap<Object,Timpl>();
    
    /**
     * Construit une table initialisée avec la requête spécifiée.
     *
     * @param statement Une requête pré-compilée à affecter au champ {@link #statement}, ou
     *        <code>null</code> si aucune. Si cet argument est nul, alors {@link #getConnection}
     *        <strong>doit</strong> être redéfinie.
     */
    protected SingletonTable(final SampleDataBase    database,
                             final PreparedStatement statement)
            throws RemoteException
    {
        super(database, statement);
    }

    /**
     * Construit une table qui recherchera des singletons à partir du type de clé spécifié.
     *
     * @param  connection Connection vers une base de données.
     * @param  type Le type initial de la requête: {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     *         Il n'est pas indispensable que le type spécifié correspondent bien à la prochaine
     *         méthode qui sera appelée ({@link #list()}, {@link #getEntry(int)} et
     *         {@link #getEntry(String)} respectivement), mais les performances seront meilleures
     *         si c'est le cas.
     * @throws SQLException si <code>SingletonTable</code> n'a pas pu construire sa requête SQL.
     */
    protected SingletonTable(final SampleDataBase database,
                             final Connection     connection,
                             final int            type)
            throws RemoteException, SQLException
    {
        super(database, null);
        this.statement = connection.prepareStatement(getQuery(type));
    }

    /**
     * Définit le type d'instruction SQL nécessaire à l'exécution des prochaines requêtes.
     * Cette méthode construira un nouvel objet {@link #statement} si la requête courante
     * n'est pas déjà du type spécifié. Cette méthode est appelée automatiquement par
     * {@link #list()}, {@link #getEntry(int)} et {@link #getEntry(String)} avec en argument
     * {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME} respectivement.
     *
     * @param  type Le type de la requête.
     *         Une des constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si <code>SingletonTable</code> n'a pas pu construire sa requête SQL.
     */
    final void setType(final int type) throws SQLException {
        assert Thread.holdsLock(this);
        if (type != this.type) {
            final Connection connection = getConnection();
            if (statement != null) {
                statement.close();
            }
            statement = connection.prepareStatement(getQuery(type));
            this.type = type;
        }
    }

    /**
     * Retourne le type de l'instruction SQL courante. Sera une des constantes
     * {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}, ou 0 si le type n'a
     * pas encore été définit.
     */
    final int getType() {
        return type;
    }

    /**
     * Retourne la connexion à la base de données. Cette méthode n'a généralement pas besoin d'être
     * redéfinie, sauf s'il existe une possibilité que {@link #statement} soit nul.
     *
     * @return La connexion à la base de données.
     * @throws SQLException si la connexion n'a pas pu être obtenue.
     */
    protected Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    /**
     * Retourne le nom de la table. Ce nom est principalement utilisé
     * pour le formatage d'éventuels messages d'erreurs.
     */
    protected abstract String getTableName();

    /**
     * Retourne la requête SQL à utiliser pour obtenir les données. A moins que la méthode
     * {@link #getQuery(int)} n'ait été redéfinie, la requête retournée par <code>getQuery()</code>
     * doit obligatoirement répondre aux conditions suivantes:
     *
     * <ul>
     *   <li>Les deux premières colonnes après la clause <code>SELECT</code> doivent être dans
     *       l'ordre le numéro ID de l'enregistrement (habituellement la clé primaire) et le
     *       nom.</li>
     *
     *   <li>L'instruction SQL doit contenir une clause du genre <code>WHERE ID=?</code>,
     *       ou <code>ID</code> est l'identifiant de la première colonne dans la clause
     *       <code>SELECT</code>. L'utilisateur est libre d'utiliser l'identifiant de son
     *       choix; "ID" n'est pas un nom obligatoire.</li>
     *
     *   <li>Le premier argument (le premier point d'interrogation dans la clause
     *       <code>WHERE</code>) doit être le numéro ID ou le nom recherché.</li>
     *
     *   <li>Si d'autres arguments sont utilisés, il est de la responsabilité des classes
     *       dérivées de leur affecteur une valeur.</li>
     * </ul>
     *
     * Exemple: <code>SELECT ID, name, series FROM Parameters WHERE ID=?</code>
     */
    protected abstract String getQuery();

    /**
     * Retourne la requête SQL du type spécifié. Cette méthode peut être appelée avant
     * {@link #list()}, {@link #getEntry(int)} ou {@link #getEntry(String)}. L'argument
     * <code>type</code> dépend de laquelle des trois méthodes citées sera exécutée.
     * L'implémentation par défaut appelle {@link #getQuery} et modifie automatiquement
     * cette requête si nécessaire.
     *
     * @param  type Le type de la requête.
     *         Une des constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     * @return La requête à utiliser pour la construction d'un objet {@link PreparedStatement}.
     *         Cette requête sera une version modifiée de la chaîne retournée par {@link #getQuery}.
     * @throws SQLException si la requête n'a pas pu être construite.
     */
    protected String getQuery(final int type) throws SQLException {
        String query = getQuery();
        switch (type) {
            default: {
                throw new IllegalArgumentException(String.valueOf(type));
            }
            case LIST: {
                final int lower = indexOfWord(query, "WHERE");
                final int upper = indexOfWord(query, "ORDER");
                if (lower >= 0) {
                    final String old = query;
                    query = query.substring(0, lower);
                    if (upper >= 0) {
                        query += old.substring(upper);
                    }
                }
                break;
            }
            case BY_ID: {
                break;
            }
            case BY_NAME:
                query = IDtoName(query);
        }
        return query.trim();
    }

    /**
     * Remplace le "ID" de la clause <code>WHERE<.code> par "nom". Ce code fonctionne même si
     * l'utilisateur a changé les noms des colonnes "ID" et "nom". Il procède comme suit:
     *
     * <ol>
     *   <li>Recherche dans la requête les deux premières colonnes après la clause SELECT.</li>
     *   <li>Recherche le premier nom dans la clause WHERE, et le remplace par le deuxième nom.</li>
     * </ol>
     */
    private static String IDtoName(final String query) throws SQLException {
        String  id       = null;
        String  name     = null;
        int     step     = 0;
        int     lower    = 0;
        boolean scanword = true;
        final int length = query.length();
        for (int index=0; index<length; index++) {
            /*
             * Search for words delimited by spaces, coma and/or symbol '='.
             * Spaces, coma and symbol '=' are ignored.
             */
            final char c = query.charAt(index);
            if ((c!=',' && c!='=' && !Character.isSpaceChar(c)) == scanword) {
                continue;
            }
            if (scanword = !scanword) {
                lower = index;
                continue;
            }
            /*
             * A word has been found. Each time a new word is found, we perform the
             * following steps:
             *
             *    1) Make sure we have skipped "SELECT"
             *    2) Save the two next word as column names for "ID" and "name"
             *    3) Make sure we have skipped "WHERE"
             *    4) Change the first column name in the WHERE clause.
             */
            final String word = query.substring(lower, index);
            switch (step) {
                case 0: if (!word.equalsIgnoreCase("SELECT")) continue; break;
                case 1: id   = word; break;
                case 2: name = word; break;
                case 3: if (!word.equalsIgnoreCase("WHERE")) continue; break;
                case 4: if (!word.equalsIgnoreCase(id)) continue;
                        final StringBuffer buffer = new StringBuffer(query.substring(0, lower));
                        buffer.append(name);
                        buffer.append(query.substring(index));
                        return buffer.toString();
            }
            step++;
        }
        throw new SQLException("La première colonne après SELECT devrait apparaître dans la clause WHERE.");
    }

    /**
     * Retourne un objet {@link Entry} correspondant à la ligne courante de l'objet
     * {@link ResultSet} spécifié. Cette méthode est appelée automatiquement par
     * {@link #getEntry(int)}, {@link #getEntry(String)} et {@link #list()}.
     *
     * @param  results Le résultat de la requête. Seul l'enregistrement courant doit
     *         être pris en compte.
     * @return L'entré pour l'enregistrement courant de <code>results</code>
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    protected abstract Timpl createEntry(final ResultSet results) throws SQLException;

    /**
     * Complète la construction de l'entré spécifiée. Cette méthode est appelée à la fin de la
     * méthode {@link #executeQuery}, après que toutes les requêtes SQL ont été complétées. On
     * évite ainsi des appels recursifs qui pourraient entraîner la création de plusieurs
     * {@link ResultSet}s pour le même {@link Statement}, ce que ne supportent pas tous les
     * pilotes JDBC. L'implémentation par défaut ne fait rien.
     *
     * @param  entry L'entré à initialiser.
     * @throws CatalogException si l'initialisation a échouée.
     */
    protected void postCreateEntry(final Timpl entry) throws CatalogException {
    }

    /**
     * Retourne une seule entré pour l'objet {@link #statement} courant. Tous les arguments de
     * {@link #statement} doivent avoir été définis avent d'appeler cette méthode. Cette méthode
     * suppose que l'appellant a déjà vérifié qu'aucune entrée n'existait préalablement dans la
     * cache pour la clé spécifiée. La requête sera exécutée et {@link #createEntry} appelée.
     * Le résultat sera alors placé dans la cache, et {@link #postCreateEntry} appelée.
     *
     * @param  key Clé identifiant l'entré.
     * @return L'entré pour la clé spécifiée et l'état courant de {@link #statement}.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    private Timpl executeQuery(final Object key) throws SQLException, CatalogException {
        assert Thread.holdsLock(this);
        assert !pool.containsKey(key);
        Timpl entry = null;
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final Timpl candidate = createEntry(results);
            if (entry == null) {
                entry = candidate;
            } else if (!entry.equals(candidate)) {
                results.close();
                throw new IllegalRecordException(getTableName(),
                        Resources.format(ResourceKeys.ERROR_DUPLICATED_RECORD_$1, key));
            }
        }
        results.close();
        if (entry == null) {
            final String table = getTableName();
            throw new NoSuchRecordException(table, Resources.format(
                                            ResourceKeys.ERROR_KEY_NOT_FOUND_$2, table, key));
        }
        final Timpl check = pool.put(key, entry);
        assert check==null : check;
        try {
            postCreateEntry(entry);
        } catch (CatalogException exception) {
            pool.remove(key);
            throw exception;
        } catch (RuntimeException exception) {
            pool.remove(key);
            throw exception;
        }
        return entry;
    }

    /**
     * Retourne une entré pour le nom spécifié.
     *
     * @param  name Le nom de l'entré désiré.
     * @return L'entré demandé.
     * @throws RemoteException si l'interrogation de la base de données a échouée.
     */
    public synchronized final Timpl getEntry(final String name) throws RemoteException {
        if (name == null) {
            return null;
        }
        Timpl entry = pool.get(name);
        if (entry != null) {
            return entry;
        }
        try {
            setType(BY_NAME);
            statement.setString(1, name);
            return executeQuery(name);
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Retourne une entré pour le numéro ID spécifié.
     *
     * @param  name Le numéro de l'entré désiré.
     * @return L'entré demandé.
     * @throws RemoteException si l'interrogation de la base de données a échouée.
     */
    public synchronized final Timpl getEntry(final int ID) throws RemoteException {
        final Integer key = new Integer(ID);
        Timpl entry = pool.get(key);
        if (entry != null) {
            return entry;
        }
        try {
            setType(BY_ID);
            statement.setInt(1, ID);
            return executeQuery(key);        
        } catch (SQLException e) {
            throw new CatalogException(e);
        }        
    }

    /**
     * Retourne toutes les entrés disponibles dans la base de données.
     */
    public synchronized final Set<T> list() throws RemoteException {
        pool.clear(); // Make sure to take in account latest database updates.
        final Set<T> set = new LinkedHashSet<T>();
        try {
            setType(LIST);
            final ResultSet results = statement.executeQuery();
            while (results.next()) {
                final Timpl entry = createEntry(results);
                if (accept(entry)) {
                    Timpl old = pool.put(entry.getName(), entry);
                    assert old==null : old;
                    set.add(entry);
                }
            }
            results.close();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
        for (final T entry : set) {
            postCreateEntry((Timpl) entry);
        }
        return set;
    }

    /**
     * Indique si la méthode {@link #list} devrait accepter l'entré spécifiée.
     * L'implémentation par défaut retourne toujours <code>true</code>.
     *
     * @param  entry Une entré trouvée par {@link #list()}.
     * @return <code>true</code> si l'entré doit être ajouté à l'ensemble retourné par
     *         {@link #list()}.
     */
    protected boolean accept(final Timpl entry) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws RemoteException {
        pool.clear();
        super.close();
    }
}
