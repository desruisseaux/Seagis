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
package fr.ird.sql.fishery;

// Requêtes SQL
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
 * Interrogation de la table "Paramètres" et de la table "Opérations". Ces deux tables
 * sont utilisées conjointement  pour  fabriquer les noms de colonnes qui apparaîtront
 * dans les tableaux de données environnementales.  Par exemple le paramètre "SST" sur
 * lequel on applique l'opération "sobel3", évalué 5 jours avant la pêche,  donnera le
 * nom de colonne <code>"grSST-05"</code>.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterTable extends Table {
    /**
     * Requête SQL pour obtenir le code d'un paramètre environnemental
     * ou d'une opération.
     */
    static final String SQL_LIST =
                    "SELECT nom FROM [?] ORDER BY nom";

    /**
     * Requête SQL pour obtenir le code d'un paramètre environnemental.
     */
    static final String SQL_SELECT =
                    "SELECT ID, nom FROM "+PARAMETERS+" WHERE ID=?";
    /**
     * Requête SQL pour obtenir le code d'une opération.
     */
    static final String SQL_SELECT_OPERATION =
                    "SELECT nom, préfix, opération FROM "+OPERATIONS+" WHERE nom=?";

    /** Numéro de colonne.                 */ private static final int KEY       = 1;
    /** Numéro de colonne de "Paramètres". */ private static final int NAME      = 2;
    /** Numéro de colonne de "Opérations". */ private static final int PREFIX    = 2;
    /** Numéro de colonne de "Opérations". */ private static final int OPERATION = 3;
    /** Numéro d'argument.                 */ private static final int ARG_KEY   = 1;

    /** Indique que cet objet servira à obtenir des paramètres à partir de leur numéro ID. */
    static final int PARAMETER_BY_ID = 0;

    /** Indique que cet objet servira à obtenir des paramètres à partir de leur nom. */
    static final int PARAMETER_BY_NAME = 1;

    /** Indique que cet objet servira à obtenir des opérations à partir de leur nom. */
    static final int OPERATION_BY_NAME = 2;

    /**
     * Le type de la requête courante. Une des constantes {@link #PARAMETER_BY_ID},
     * {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     */
    private int type;

    /**
     * Construit une table des paramètres/opérations.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  type Le type de la requête. Une des constantes {@link #PARAMETER_BY_ID},
     *         {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requête SQL.
     */
    ParameterTable(final Connection connection, final int type) throws SQLException {
        super(connection.prepareStatement(getQuery(type)));
        this.type = type;
    }

    /**
     * Retourne la requête SQL du type spécifié.
     *
     * @param  type Le type de la requête. Une des constantes {@link #PARAMETER_BY_ID},
     *         {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     * @return La requête à utiliser pour la construction d'un objet {@link PreparedStatement}.
     * @throws SQLException si la requête n'a pas pu être construite.
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
         * Remplace le "ID" de la clause WHERE par "nom". Ce code fonctionne même si
         * l'utilisateur a changé les noms des colonnes "ID" et "nom". Il procède comme suit:
         *
         * 1) Recherche dans la requête les deux premières colonnees après la clause SELECT.
         * 2) Recherche le premier nom dans la clause WHERE, et le remplacer par le deuxième nom.
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
        throw new SQLException("La première colonne après SELECT devrait apparaître dans la clause WHERE.");
    }

    /**
     * Définit le type de requête SQL de cet objet. Cette méthode affecte une nouvelle
     * valeur à {@link #statement} si la requête courante n'est pas déjà du type spécifié.
     *
     * @param  type Le type de la requête. Une des constantes {@link #PARAMETER_BY_ID},
     *         {@link #PARAMETER_BY_NAME} ou {@link #OPERATION_BY_NAME}.
     * @throws SQLException si <code>ParameterTable</code> n'a pas pu construire sa requête SQL.
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
     * Retourne la liste des paramètres ou des opérations disponibles.
     *
     * @param  connection La connection à utiliser.
     * @param  table La table à interroger. Devrait être une des constantes
     *         {@link #PARAMETERS} ou {@link #OPERATIONS}.
     * @throws SQLException si l'accès à la base de données a échoué.
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
     * Retourne la valeur d'une colonne sous forme d'un objet Java. Cette méthode
     * examine toutes les lignes de la requête, et vérifie que tous les objets de
     * la colonne spécifiée sont identique.  En général, il n'y aura qu'une seule
     * ligne. L'objet retourné sera typiquement de la classe {@link String} ou
     * {@link Number}.
     *
     * @param  statement La requête à exécuter. Cette requête doit
     *         être déjà configurée, prête à être exécutée.
     * @param  column Le numéro de colonne dans laquelle extraire la valeur.
     * @param  key Un nom de paramètre qui a servit à configurer la requête
     *         <code>statement</code>. Cette information sert uniquement à
     *         formatter un message d'erreur si cette méthode a échoué.
     * @return La valeur trouvée à la colonne spécifiée.
     * @throws SQLException si l'accès à la base de données a échoué, si
     *         aucune valeur n'a été trouvée ou si plusieurs valeurs différentes
     *         ont été trouvées.
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
     * Retourne le numéro ID d'un paramètre à partir de son nom.
     *
     * @param  parameter Le nom du paramètre.
     * @return Le numéro ID du paramètre spécifié.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized int getParameterID(final String parameter) throws SQLException {
        setType(PARAMETER_BY_NAME);
        statement.setString(ARG_KEY, parameter);
        return ((Number) getObject(statement, KEY, parameter)).intValue();
    }

    /**
     * Retourne le nom identifiant un paramètre.
     *
     * @param  parameter Numéro ID du paramètre.
     * @return Le nom du paramètre spécifié.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized String getParameterName(final int parameter) throws SQLException {
        setType(PARAMETER_BY_ID);
        statement.setInt(ARG_KEY, parameter);
        return getObject(statement, NAME, new Integer(parameter)).toString();
    }

    /**
     * Retourne le prefix d'une opération.
     *
     * @param  name Le nom court idenfifiant l'opération.
     * @return Le prefix de l'opération.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized String getOperationPrefix(final String name) throws SQLException {
        setType(OPERATION_BY_NAME);
        statement.setString(ARG_KEY, name);
        return getObject(statement, PREFIX, name).toString();
    }
}
