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
 */
package fr.ird.database.sample.sql;

// Requ�tes SQL
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
 * Classe de base des tables dont les m�thodes <code>getEntry(...)</code> retourneront un et
 * un seul enregistrement. Les enregistrements de ces tables sont identifi�s de fa�on unique
 * par un num�ro ID ou un nom, et font souvent partie d'une cl� primaire d'une autre table.
 * Cette classe <code>SingletonTable</code> sert de base aux classes suivantes:
 *
 * <ul>
 *   <li>{@link RelativePositionTable},</li>
 *   <li>{@link ParameterTable},</li>
 *   <li>{@link OperationTable} et</li>
 *   <li>{@link DescriptorTable}.</li>
 * </ul>
 *
 * La classe <code>SingletonTable</code> d�finie des m�thodes {@link #getEntry(int)},
 * {@link #getEntry(String)} et {@link #list()}. En contrepartie, les classes d�riv�es
 * doivent impl�menter les m�thodes suivantes:
 *
 * <ul>
 *   <li>{@link #getTableName},
 *               pour retourner le nom de la table;</li>
 *   <li>{@link #getQuery()},
 *               pour retourner l'instruction SQL � utiliser pour obtenir
 *               les donn�es � partir de son num�ro ID seulement.</li>
 *   <li>{@link #getQuery(int)} (facultatif),
 *               pour retourner l'instruction SQL � utiliser pour obtenir les donn�es � partir
 *               de son num�ro ID ou d'une autre information (habituellement son nom).</li>
 *   <li>{@link #createEntry},
 *               pour construire une entr�e � partir de la ligne courante;</li>
 *   <li>{@link #postCreateEntry} (facultatif),
 *               pour achever la construction d'une entr�e apr�s que le r�sultat de la requ�te
 *               SQL ait �t� ferm�. Particuli�rement utile si la phase finale peut impliquer de
 *               nouvelles requ�tes sur le m�me objet {@link #statement}.</li>
 * </ul>
 *
 * Les entr�s obtenus lors des appels pr�c�dents seront cach�s pour un acc�s plus rapide la
 * prochaine fois qu'une m�thode <code>getEntry(...)</code> est appel�e avec la m�me cl�.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class SingletonTable<T extends Entry, Timpl extends T> extends Table {
    /**
     * Enum�ration indiquant que {@link #statement} servira � obtenir la liste des param�tres.
     * C'est le type s�lectionn� lorsque la m�thode {@link #list} est appel�e.
     *
     * @see #getQuery(int)
     */
    protected static final int LIST = 1;

    /**
     * Enum�ration indiquant que {@link #statement} servira � obtenir des param�tres � partir
     * de leur num�ro ID. C'est le type s�lectionn� lorsque la m�thode {@link #getEntry(int)}
     * est appel�e.
     *
     * @see #getQuery(int)
     */
    protected static final int BY_ID = 2;

    /**
     * Enum�ration indiquant que {@link #statement} servira � obtenir des param�tres � partir
     * de leur nom. C'est le type s�lectionn� lorsque la m�thode {@link #getEntry(String)} est
     * appel�e.
     *
     * @see #getQuery(int)
     */
    protected static final int BY_NAME = 3;

    /**
     * Le type de la requ�te courante. Sa valeur sera une des
     * constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     *
     * @see #getQuery(int)
     */
    private int type;

    /**
     * Ensemble des entr�s d�j� obtenues pour chaque nom ou ID. Les cl�s doivent �tre soit
     * des objets {@link Integer}, ou soit des objets {@link String}. Aucune autre classe
     * ne devrait �tre utilis�e.
     * <br><br>
     * Note: L'utilisation d'une cache n'est pas forc�ment souhaitable. Si la base de donn�es
     *       a �t� mise-�-jour apr�s qu'une entr�e est �t� mise dans la cache, la mise-�-jour
     *       ne sera pas visible. C'est une des raisons pour lesquelles la plupart des tables
     *       n'utilisent pas de cache. Mais les sous-classes de <code>SingletonTable</code>
     *       sont particuli�res du fais qu'une m�me entr�e sera redemand�e plusieurs fois
     *       (notamment pas {@link LinearModelTable}) dans le cadre d'un usage normal de la
     *       base de donn�es. La cache sera d�truite chaque fois que la table sera ferm�e,
     *       et une nouvelle cache reconstruite par exemple � chaque construction d'une nouvelle
     *       table {@link EnvironmentTable}. Ce compromis nous semble acceptable.
     */
    final Map<Object,Timpl> pool = new HashMap<Object,Timpl>();
    
    /**
     * Construit une table initialis�e avec la requ�te sp�cifi�e.
     *
     * @param statement Une requ�te pr�-compil�e � affecter au champ {@link #statement}, ou
     *        <code>null</code> si aucune. Si cet argument est nul, alors {@link #getConnection}
     *        <strong>doit</strong> �tre red�finie.
     */
    protected SingletonTable(final SampleDataBase    database,
                             final PreparedStatement statement)
            throws RemoteException
    {
        super(database, statement);
    }

    /**
     * Construit une table qui recherchera des singletons � partir du type de cl� sp�cifi�.
     *
     * @param  connection Connection vers une base de donn�es.
     * @param  type Le type initial de la requ�te: {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     *         Il n'est pas indispensable que le type sp�cifi� correspondent bien � la prochaine
     *         m�thode qui sera appel�e ({@link #list()}, {@link #getEntry(int)} et
     *         {@link #getEntry(String)} respectivement), mais les performances seront meilleures
     *         si c'est le cas.
     * @throws SQLException si <code>SingletonTable</code> n'a pas pu construire sa requ�te SQL.
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
     * D�finit le type d'instruction SQL n�cessaire � l'ex�cution des prochaines requ�tes.
     * Cette m�thode construira un nouvel objet {@link #statement} si la requ�te courante
     * n'est pas d�j� du type sp�cifi�. Cette m�thode est appel�e automatiquement par
     * {@link #list()}, {@link #getEntry(int)} et {@link #getEntry(String)} avec en argument
     * {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME} respectivement.
     *
     * @param  type Le type de la requ�te.
     *         Une des constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si <code>SingletonTable</code> n'a pas pu construire sa requ�te SQL.
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
     * pas encore �t� d�finit.
     */
    final int getType() {
        return type;
    }

    /**
     * Retourne la connexion � la base de donn�es. Cette m�thode n'a g�n�ralement pas besoin d'�tre
     * red�finie, sauf s'il existe une possibilit� que {@link #statement} soit nul.
     *
     * @return La connexion � la base de donn�es.
     * @throws SQLException si la connexion n'a pas pu �tre obtenue.
     */
    protected Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    /**
     * Retourne le nom de la table. Ce nom est principalement utilis�
     * pour le formatage d'�ventuels messages d'erreurs.
     */
    protected abstract String getTableName();

    /**
     * Retourne la requ�te SQL � utiliser pour obtenir les donn�es. A moins que la m�thode
     * {@link #getQuery(int)} n'ait �t� red�finie, la requ�te retourn�e par <code>getQuery()</code>
     * doit obligatoirement r�pondre aux conditions suivantes:
     *
     * <ul>
     *   <li>Les deux premi�res colonnes apr�s la clause <code>SELECT</code> doivent �tre dans
     *       l'ordre le num�ro ID de l'enregistrement (habituellement la cl� primaire) et le
     *       nom.</li>
     *
     *   <li>L'instruction SQL doit contenir une clause du genre <code>WHERE ID=?</code>,
     *       ou <code>ID</code> est l'identifiant de la premi�re colonne dans la clause
     *       <code>SELECT</code>. L'utilisateur est libre d'utiliser l'identifiant de son
     *       choix; "ID" n'est pas un nom obligatoire.</li>
     *
     *   <li>Le premier argument (le premier point d'interrogation dans la clause
     *       <code>WHERE</code>) doit �tre le num�ro ID ou le nom recherch�.</li>
     *
     *   <li>Si d'autres arguments sont utilis�s, il est de la responsabilit� des classes
     *       d�riv�es de leur affecteur une valeur.</li>
     * </ul>
     *
     * Exemple: <code>SELECT ID, name, series FROM Parameters WHERE ID=?</code>
     */
    protected abstract String getQuery();

    /**
     * Retourne la requ�te SQL du type sp�cifi�. Cette m�thode peut �tre appel�e avant
     * {@link #list()}, {@link #getEntry(int)} ou {@link #getEntry(String)}. L'argument
     * <code>type</code> d�pend de laquelle des trois m�thodes cit�es sera ex�cut�e.
     * L'impl�mentation par d�faut appelle {@link #getQuery} et modifie automatiquement
     * cette requ�te si n�cessaire.
     *
     * @param  type Le type de la requ�te.
     *         Une des constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     * @return La requ�te � utiliser pour la construction d'un objet {@link PreparedStatement}.
     *         Cette requ�te sera une version modifi�e de la cha�ne retourn�e par {@link #getQuery}.
     * @throws SQLException si la requ�te n'a pas pu �tre construite.
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
     * Remplace le "ID" de la clause <code>WHERE<.code> par "nom". Ce code fonctionne m�me si
     * l'utilisateur a chang� les noms des colonnes "ID" et "nom". Il proc�de comme suit:
     *
     * <ol>
     *   <li>Recherche dans la requ�te les deux premi�res colonnes apr�s la clause SELECT.</li>
     *   <li>Recherche le premier nom dans la clause WHERE, et le remplace par le deuxi�me nom.</li>
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
        throw new SQLException("La premi�re colonne apr�s SELECT devrait appara�tre dans la clause WHERE.");
    }

    /**
     * Retourne un objet {@link Entry} correspondant � la ligne courante de l'objet
     * {@link ResultSet} sp�cifi�. Cette m�thode est appel�e automatiquement par
     * {@link #getEntry(int)}, {@link #getEntry(String)} et {@link #list()}.
     *
     * @param  results Le r�sultat de la requ�te. Seul l'enregistrement courant doit
     *         �tre pris en compte.
     * @return L'entr� pour l'enregistrement courant de <code>results</code>
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    protected abstract Timpl createEntry(final ResultSet results) throws SQLException;

    /**
     * Compl�te la construction de l'entr� sp�cifi�e. Cette m�thode est appel�e � la fin de la
     * m�thode {@link #executeQuery}, apr�s que toutes les requ�tes SQL ont �t� compl�t�es. On
     * �vite ainsi des appels recursifs qui pourraient entra�ner la cr�ation de plusieurs
     * {@link ResultSet}s pour le m�me {@link Statement}, ce que ne supportent pas tous les
     * pilotes JDBC. L'impl�mentation par d�faut ne fait rien.
     *
     * @param  entry L'entr� � initialiser.
     * @throws CatalogException si l'initialisation a �chou�e.
     */
    protected void postCreateEntry(final Timpl entry) throws CatalogException {
    }

    /**
     * Retourne une seule entr� pour l'objet {@link #statement} courant. Tous les arguments de
     * {@link #statement} doivent avoir �t� d�finis avent d'appeler cette m�thode. Cette m�thode
     * suppose que l'appellant a d�j� v�rifi� qu'aucune entr�e n'existait pr�alablement dans la
     * cache pour la cl� sp�cifi�e. La requ�te sera ex�cut�e et {@link #createEntry} appel�e.
     * Le r�sultat sera alors plac� dans la cache, et {@link #postCreateEntry} appel�e.
     *
     * @param  key Cl� identifiant l'entr�.
     * @return L'entr� pour la cl� sp�cifi�e et l'�tat courant de {@link #statement}.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
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
     * Retourne une entr� pour le nom sp�cifi�.
     *
     * @param  name Le nom de l'entr� d�sir�.
     * @return L'entr� demand�.
     * @throws RemoteException si l'interrogation de la base de donn�es a �chou�e.
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
     * Retourne une entr� pour le num�ro ID sp�cifi�.
     *
     * @param  name Le num�ro de l'entr� d�sir�.
     * @return L'entr� demand�.
     * @throws RemoteException si l'interrogation de la base de donn�es a �chou�e.
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
     * Retourne toutes les entr�s disponibles dans la base de donn�es.
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
     * Indique si la m�thode {@link #list} devrait accepter l'entr� sp�cifi�e.
     * L'impl�mentation par d�faut retourne toujours <code>true</code>.
     *
     * @param  entry Une entr� trouv�e par {@link #list()}.
     * @return <code>true</code> si l'entr� doit �tre ajout� � l'ensemble retourn� par
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
