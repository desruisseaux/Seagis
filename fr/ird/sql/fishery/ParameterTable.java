/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.sql.fishery;

// Requ�tes SQL
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Divers
import java.util.Set;
import java.util.LinkedHashSet;

// Resources
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Interrogation de la table des param�tres.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterTable extends Table {
    /**
     * Requ�te SQL pour obtenir le code d'un param�tre environnemental.
     */
    static final String SQL_LIST=
                    "SELECT name FROM "+PARAMETERS+" ORDER BY name";

    /**
     * Requ�te SQL pour obtenir le code d'un param�tre environnemental.
     */
    static final String SQL_SELECT=
                    "SELECT ID, name FROM "+PARAMETERS+" WHERE ID=?";

    /** Num�ro de colonne. */ private static final int ID     = 1;
    /** Num�ro de colonne. */ private static final int NAME   = 2;
    /** Num�ro d'argument. */ private static final int ARG_ID = 1;

    /**
     * Construit une table des param�tres.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  byName <code>true</code> si les param�tres devront �tre recherch� par nom plut�t
     **        que par leur num�ro ID.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected ParameterTable(final Connection connection, final boolean byName) throws SQLException {
        super(connection.prepareStatement(replace(preferences.get(PARAMETERS, SQL_SELECT), byName)));
    }

    /**
     * Remplace le "ID" de la clause WHERE par "name". Cette m�thode fonctionne m�me si
     * l'utilisateur a chang� les noms des colonnes "ID" et "name". Elle proc�de comme suit:
     *
     * 1) Recherche dans la requ�te les deux premi�res colonnees apr�s la clause SELECT.
     * 2) Recherche le premier nom dans la clause WHERE, et remplace le par le deuxi�me nom.
     *
     * @param  query La requ�te � traiter.
     * @param  <code>byName</code> Si <code>false</code>, alors cette m�thode ne fait rien.
     *         Cet argument n'existe que par commodit� pour le constructeur.
     * @return La requ�te modifi�e.
     * @throws SQLException si la requ�te n'a pas pu �tre trait�e.
     */
    private static String replace(String query, final boolean byName) throws SQLException {
        query = query.trim();
        if (!byName) {
            return query;
        }
        // The name for "ID" and "name" column (should be "ID"
        // and "name"... but the following code will make it sure.
        String id=null, name=null;

        int step  = 0;
        int lower = 0;
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
     * Retourne la liste des param�tres disponibles.
     *
     * @param connection La connection � utiliser.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    static String[] getAvailableParameters(final Connection connection) throws SQLException {
        final Statement      stm = connection.createStatement();
        final ResultSet   result = stm.executeQuery(SQL_LIST);
        final Set<String>  param = new LinkedHashSet<String>();
        while (result.next()) {
            final String item = result.getString(1);
            if (item != null) {
                param.add(item);
            }
        }
        result.close();
        stm.close();
        return param.toArray(new String[param.size()]);
    }

    /**
     * Retourne la liste des param�tres disponibles. Ces param�tres peuvent
     * �tre sp�cifi� en argument � la m�thode {@link #setParameter}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public String[] getAvailableParameters() throws SQLException {
        return getAvailableParameters(statement.getConnection());
    }

    /**
     * Retourne le num�ro identifiant un param�tre.
     *
     * @param  parameter Le param�tre.
     * @return Le code du param�tre sp�cifi�.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public synchronized int getParameterID(final String parameter) throws SQLException {
        statement.setString(ARG_ID, parameter);
        final ResultSet result = statement.executeQuery();
        int code=0, count=0;
        while (result.next()) {
            final int candidate = result.getInt(ID);
            if (count==0) {
                code = candidate;
            } else if (code == candidate) {
                continue;
            }
            if (++count >= 2) {
                // No needs to continue; we know we have failed.
                break;
            }
        }
        result.close();
        if (count != 1) {
            final int messageKey = (count==0) ? ResourceKeys.ERROR_NO_PARAMETER_$1 :
                                                ResourceKeys.ERROR_DUPLICATED_RECORD_$1;
            throw new SQLException(Resources.format(messageKey, parameter));
        }
        return code;
    }

    /**
     * Retourne le nom identifiant un param�tre.
     *
     * @param  parameter Le num�ro du param�tre.
     * @return Le nom du param�tre sp�cifi�.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public synchronized String getParameterName(final int parameter) throws SQLException {
        statement.setInt(ARG_ID, parameter);
        final ResultSet result = statement.executeQuery();
        String code=null;
        int count=0;
        while (result.next()) {
            final String candidate = result.getString(NAME);
            if (count==0) {
                code = candidate;
            } else if (code.equals(candidate)) {
                continue;
            }
            if (++count >= 2) {
                // No needs to continue; we know we have failed.
                break;
            }
        }
        result.close();
        if (count != 1) {
            final int messageKey = (count==0) ? ResourceKeys.ERROR_NO_PARAMETER_$1 :
                                                ResourceKeys.ERROR_DUPLICATED_RECORD_$1;
            throw new SQLException(Resources.format(messageKey, new Integer(parameter)));
        }
        return code;
    }
}
