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

// Requ�tes SQL
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

// Collections
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;

// Resources
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.NoSuchRecordException;
import fr.ird.database.Entry;


/**
 * Classe de base de {@link ParameterTable}, {@link OperationTable} et {@link RelativePositionTable}.
 * Ces trois tables sont utilis�es conjointement pour fabriquer les noms de colonnes qui appara�tront
 * dans les tableaux de donn�es environnementales. Par exemple le param�tre "SST" sur lequel on
 * applique l'op�ration "sobel3", �valu� 5 jours avant la p�che, donnera le nom de colonne
 * <code>"grSST-05"</code>.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class ColumnTable<T extends Entry> extends Table {
    /** Indique que cet objet servira � obtenir la liste des param�tres. */
    static final int LIST = 0;

    /** Indique que cet objet servira � obtenir des param�tres � partir de leur num�ro ID. */
    static final int BY_ID = 1;

    /** Indique que cet objet servira � obtenir des param�tres � partir de leur nom. */
    static final int BY_NAME = 2;

    /**
     * Le type de la requ�te courante. Une des constantes {@link #LIST}, {@link #BY_ID} ou
     * {@link #BY_NAME}.
     */
    private int type;

    /**
     * Ensemble des positions d�j� obtenus pour chaque nom ou ID.
     */
    private final Map<Object,T> pool = new HashMap<Object,T>();

    /**
     * Construit une table des param�tres/op�rations/positions.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  type Le type de la requ�te. Une des constantes {@link #LIST},
     *         {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si <code>ColumnTable</code> n'a pas pu construire sa requ�te SQL.
     */
    ColumnTable(final Connection connection, final int type) throws SQLException {
        super(null);
        this.type = type;
        this.statement = connection.prepareStatement(getQuery(type));
    }

    /**
     * Retourne le nom de la table.
     */
    protected abstract String getTableName();

    /**
     * Retourne l'instruction SQL � utiliser pour obtenir les donn�es. Cette instruction
     * contiendra typiquement une clause WHERE du genre "WHERE ID=?".
     */
    protected abstract String getQuery();

    /**
     * Retourne la requ�te SQL du type sp�cifi�.
     *
     * @param  type Le type de la requ�te. Une des constantes {@link #PARAMETER_BY_ID},
     *         {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     * @return La requ�te � utiliser pour la construction d'un objet {@link PreparedStatement}.
     * @throws SQLException si la requ�te n'a pas pu �tre construite.
     */
    private String getQuery(final int type) throws SQLException {
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
     * Remplace le "ID" de la clause WHERE par "nom". Ce code fonctionne m�me si
     * l'utilisateur a chang� les noms des colonnes "ID" et "nom". Il proc�de comme suit:
     *
     * 1) Recherche dans la requ�te les deux premi�res colonnees apr�s la clause SELECT.
     * 2) Recherche le premier nom dans la clause WHERE, et le remplace par le deuxi�me nom.
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
     * D�finit le type de requ�te SQL de cet objet. Cette m�thode affecte une nouvelle
     * valeur � {@link #statement} si la requ�te courante n'est pas d�j� du type sp�cifi�.
     *
     * @param  type Le type de la requ�te. Une des constantes {@link #LIST},
     *         {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si <code>ColumnTable</code> n'a pas pu construire sa requ�te SQL.
     */
    private void setType(final int type) throws SQLException {
        if (type != this.type) {
            final Connection connection = statement.getConnection();
            statement.close();
            statement = connection.prepareStatement(getQuery(type));
            this.type = type;
        }
    }

    /**
     * Retourne un objet {@link Entry} correspondant � la ligne courante
     * de l'objet {@link ResultSet} sp�cifi�.
     */
    protected abstract T getEntry(final ResultSet results)  throws SQLException;

    /**
     * Retourne une seule entr� pour l'objet {@link Statement} courant.
     */
    private T doGetEntry(final Object key) throws SQLException {
        T entry = pool.get(key);
        if (entry != null) {
            return entry;
        }
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final T candidate = getEntry(results);
            if (entry == null) {
                entry = candidate;
            } else if (!entry.equals(candidate)) {
                results.close();
                throw new IllegalRecordException(PARAMETERS,
                        Resources.format(ResourceKeys.ERROR_DUPLICATED_RECORD_$1, key));
            }
        }
        results.close();
        if (entry == null) {
            final String table = getTableName();
            throw new NoSuchRecordException(table, Resources.format(
                                                ResourceKeys.ERROR_KEY_NOT_FOUND_$2, table, key));
        }
        pool.put(key, entry);
        try {
            setup(entry);
        } catch (SQLException exception) {
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
     */
    public synchronized T getEntry(final String name)
            throws SQLException
    {
        if (name == null) {
            return null;
        }
        setType(BY_NAME);
        statement.setString(1, name);
        return doGetEntry(name);
    }

    /**
     * Retourne une entr� pour le num�ro ID sp�cifi�.
     */
    public synchronized T getEntry(final int ID)
            throws SQLException
    {
        setType(BY_ID);
        statement.setInt(1, ID);
        return doGetEntry(new Integer(ID));
    }

    /**
     * Retourne toutes les entr�s disponibles dans la base de donn�es.
     */
    public synchronized Set<T> list() throws SQLException {
        pool.clear();
        final Set<T> set = new LinkedHashSet<T>();
        setType(LIST);
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final T entry = getEntry(results);
            if (accept(entry)) {
                pool.put(entry.getName(), entry);
                set.add(entry);
            }
        }
        results.close();
        // TODO: The 'for (T entry : set)' syntax crash the compiler here.
        for (final java.util.Iterator<T> it=set.iterator(); it.hasNext();) {
            setup(it.next());
        }
        return set;
    }

    /**
     * Indique si la m�thode {@link #list} devrait accepter l'entr� sp�cifi�e.
     * L'impl�mentation par d�faut retourne toujours <code>true</code>.
     */
    protected boolean accept(final T entry) {
        return true;
    }

    /**
     * Initialise l'entr� sp�cifi�e. Cette m�thode est appel�e apr�s que toutes les requ�tes SQL
     * ont �t� compl�t�es. On �vite ainsi des appels recursifs qui pourraient entra�ner la cr�ation
     * de plusieurs {@link ResultSet}s pour le m�me {@link Statement}, ce que ne supportent pas
     * tous les pilotes JDBC. L'impl�mentation par d�faut ne fait rien.
     *
     * @param  entry L'entr� � initialiser.
     * @throws SQLException si l'initialisation a �chou�e.
     */
    protected void setup(final T entry) throws SQLException {
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws SQLException {
        pool.clear();
        super.close();
    }
}
