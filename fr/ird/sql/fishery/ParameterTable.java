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

// Geotools
import org.geotools.resources.Utilities;

// Resources
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Interrogation de la table "Param�tres" et de la table "Op�rations". Ces deux tables
 * sont utilis�es conjointement  pour  fabriquer les noms de colonnes qui appara�tront
 * dans les tableaux de donn�es environnementales.  Par exemple le param�tre "SST" sur
 * lequel on applique l'op�ration "sobel3", �valu� 5 jours avant la p�che,  donnera le
 * nom de colonne <code>"grSST-05"</code>.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterTable extends Table {
    /**
     * Requ�te SQL pour obtenir le code d'un param�tre environnemental
     * ou d'une op�ration.
     */
    static final String SQL_LIST =
                    "SELECT nom FROM [?] ORDER BY nom";

    /**
     * Requ�te SQL pour obtenir le code d'un param�tre environnemental.
     */
    static final String SQL_SELECT =
                    "SELECT ID, nom FROM "+PARAMETERS+" WHERE ID=?";
    /**
     * Requ�te SQL pour obtenir le code d'une op�ration.
     */
    static final String SQL_SELECT_OPERATION =
                    "SELECT nom, pr�fix, op�ration FROM "+OPERATIONS+" WHERE nom=?";

    /** Num�ro de colonne.                 */ private static final int KEY       = 1;
    /** Num�ro de colonne de "Param�tres". */ private static final int NAME      = 2;
    /** Num�ro de colonne de "Op�rations". */ private static final int PREFIX    = 2;
    /** Num�ro de colonne de "Op�rations". */ private static final int OPERATION = 3;
    /** Num�ro d'argument.                 */ private static final int ARG_KEY   = 1;

    /** Indique que cet objet servira � obtenir des param�tres � partir de leur num�ro ID. */
    static final int PARAMETER_BY_ID = 0;

    /** Indique que cet objet servira � obtenir des param�tres � partir de leur nom. */
    static final int PARAMETER_BY_NAME = 1;

    /** Indique que cet objet servira � obtenir des op�rations � partir de leur nom. */
    static final int OPERATION_BY_NAME = 2;

    /**
     * Le type de la requ�te courante. Une des constantes {@link #PARAMETER_BY_ID},
     * {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     */
    private int type;

    /**
     * Construit une table des param�tres/op�rations.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  type Le type de la requ�te. Une des constantes {@link #PARAMETER_BY_ID},
     *         {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requ�te SQL.
     */
    ParameterTable(final Connection connection, final int type) throws SQLException {
        super(connection.prepareStatement(getQuery(type)));
        this.type = type;
    }

    /**
     * Retourne la requ�te SQL du type sp�cifi�.
     *
     * @param  type Le type de la requ�te. Une des constantes {@link #PARAMETER_BY_ID},
     *         {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     * @return La requ�te � utiliser pour la construction d'un objet {@link PreparedStatement}.
     * @throws SQLException si la requ�te n'a pas pu �tre construite.
     */
    private static String getQuery(final int type) throws SQLException {
        switch (type) {
            default:                throw new IllegalArgumentException(String.valueOf(type));
            case OPERATION_BY_NAME: return preferences.get(OPERATIONS, SQL_SELECT_OPERATION);
            case PARAMETER_BY_ID:   return preferences.get(PARAMETERS, SQL_SELECT);
            case PARAMETER_BY_NAME: break;
        }
        String query = preferences.get(PARAMETERS, SQL_SELECT);
        /*
         * Remplace le "ID" de la clause WHERE par "nom". Ce code fonctionne m�me si
         * l'utilisateur a chang� les noms des colonnes "ID" et "nom". Il proc�de comme suit:
         *
         * 1) Recherche dans la requ�te les deux premi�res colonnees apr�s la clause SELECT.
         * 2) Recherche le premier nom dans la clause WHERE, et le remplacer par le deuxi�me nom.
         */
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
     * @param  type Le type de la requ�te. Une des constantes {@link #PARAMETER_BY_ID},
     *         {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requ�te SQL.
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
     * Retourne la liste des param�tres ou des op�rations disponibles.
     *
     * @param  connection La connection � utiliser.
     * @param  table La table � interroger. Devrait �tre une des constantes
     *         {@link #PARAMETERS} ou {@link #OPERATIONS}.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    static Set<String> list(final Connection connection, final String table) throws SQLException {
        final Statement      stm = connection.createStatement();
        final ResultSet   result = stm.executeQuery(replaceQuestionMark(SQL_LIST, table));
        final Set<String>  param = new LinkedHashSet<String>();
        while (result.next()) {
            final String item = result.getString(1);
            if (item != null) {
                param.add(item);
            }
        }
        result.close();
        stm.close();
        return param;
    }

    /**
     * Retourne la valeur d'une colonne sous forme d'un objet Java. Cette m�thode
     * examine toutes les lignes de la requ�te, et v�rifie que tous les objets de
     * la colonne sp�cifi�e sont identique.  En g�n�ral, il n'y aura qu'une seule
     * ligne. L'objet retourn� sera typiquement de la classe {@link String} ou
     * {@link Number}.
     *
     * @param  statement La requ�te � ex�cuter. Cette requ�te doit
     *         �tre d�j� configur�e, pr�te � �tre ex�cut�e.
     * @param  column Le num�ro de colonne dans laquelle extraire la valeur.
     * @param  key Un nom de param�tre qui a servit � configurer la requ�te
     *         <code>statement</code>. Cette information sert uniquement �
     *         formatter un message d'erreur si cette m�thode a �chou�.
     * @return La valeur trouv�e � la colonne sp�cifi�e.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�, si
     *         aucune valeur n'a �t� trouv�e ou si plusieurs valeurs diff�rentes
     *         ont �t� trouv�es.
     */
    private static Object getObject(final PreparedStatement statement, final int column, final Object key)
        throws SQLException
    {
        ResultSet result = statement.executeQuery();
        Object     value = null;
        int        count = 0;
        while (result.next()) {
            final Object candidate = result.getObject(column);
            if (count == 0) {
                value = candidate;
            } else if (Utilities.equals(value, candidate)) {
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
            throw new SQLException(Resources.format(messageKey, key));
        }
        return value;
    }

    /**
     * Retourne le num�ro ID d'un param�tre � partir de son nom.
     *
     * @param  parameter Le nom du param�tre.
     * @return Le num�ro ID du param�tre sp�cifi�.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public synchronized int getParameterID(final String parameter) throws SQLException {
        setType(PARAMETER_BY_NAME);
        statement.setString(ARG_KEY, parameter);
        return ((Number) getObject(statement, KEY, parameter)).intValue();
    }

    /**
     * Retourne le nom identifiant un param�tre.
     *
     * @param  parameter Num�ro ID du param�tre.
     * @return Le nom du param�tre sp�cifi�.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public synchronized String getParameterName(final int parameter) throws SQLException {
        setType(PARAMETER_BY_ID);
        statement.setInt(ARG_KEY, parameter);
        return getObject(statement, NAME, new Integer(parameter)).toString();
    }

    /**
     * Retourne le prefix d'une op�ration.
     *
     * @param  name Le nom court idenfifiant l'op�ration.
     * @return Le prefix de l'op�ration.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public synchronized String getOperationPrefix(final String name) throws SQLException {
        setType(OPERATION_BY_NAME);
        statement.setString(ARG_KEY, name);
        return getObject(statement, PREFIX, name).toString();
    }
}
