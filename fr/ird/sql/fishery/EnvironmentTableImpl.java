/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
import java.sql.SQLWarning;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Divers
import java.util.Set;
import java.util.Date;
import java.util.Arrays;
import javax.media.jai.util.Range;

// Resources
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Implémentation de la table interrogeant ou modifiant
 * un paramètre de la base de données d'environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentTableImpl extends Table implements EnvironmentTable {
    /**
     * Requête SQL pour obtenir la table des données environnementales.
     */
    static final String SQL_SELECT=
                    "SELECT [?] FROM "+ENVIRONMENTS+" "+
                    "WHERE ID=? AND position=? AND temps=? AND paramètre=?";

    /**
     * Instruction SQL pour mettre à jour une donnée environnementale.
     * Note: La valeur est le premier paramètre, et tous les autres sont décalés de 1.
     */
    static final String SQL_UPDATE=
                    "UPDATE "+ENVIRONMENTS+" SET [?]=? "+
                    "WHERE ID=? AND position=? AND temps=? AND paramètre=?";

    /**
     * Instruction SQL pour ajouter une donnée environnementale.
     */
    static final String SQL_INSERT=
                    "INSERT INTO "+ENVIRONMENTS+" (ID,position,temps,paramètre,[?]) "+
                    "VALUES(?,?,?,?,?)";

    /** Numéro d'argument. */ private static final int ARG_ID        = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION  = 2;
    /** Numéro d'argument. */ private static final int ARG_TIMELAG   = 3;
    /** Numéro d'argument. */ private static final int ARG_PARAMETER = 4;
    /** Numéro d'argument. */ private static final int ARG_VALUE     = 5;

    /**
     * Position sur la ligne de pêche.
     */
    private int position;

    /**
     * Décalage de temps, en jours.
     */
    private int timeLag;

    /**
     * Numéro du paramètre.
     */
    private int parameter;

    /**
     * Nom de la colonne dans laquelle lire ou écrire les valeurs (exemple "value" ou "sobel").
     */
    private final String column;

    /**
     * Instruction à utiliser pour les mises à jour.
     * Cette instruction ne sera construite que la
     * première fois où elle sera nécessaire.
     */
    private transient PreparedStatement update;

    /**
     * Instruction à utiliser pour les insertions.
     * Cette instruction ne sera construite que la
     * première fois où elle sera nécessaire.
     */
    private transient PreparedStatement insert;

    /**
     * Construit une table pour le paramètre spécifié.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  parameter Le paramètre à mettre à jour (exemple: "SST").
     * @param  column La colonne dans laquelle lire ou écrire les valeurs (exemple "value" ou "sobel").
     * @throws SQLException si <code>EnvironmentTable</code> n'a pas pu construire sa requête SQL.
     */
    protected EnvironmentTableImpl(final Connection connection,
                                   final String     parameter,
                                   final String     column) throws SQLException
    {
        super(connection.prepareStatement(replaceQuestionMark(preferences.get(ENVIRONMENTS, SQL_SELECT), column)));
        this.column = column;
        setParameter(parameter);
        setPosition(CENTER);
        setTimeLag(0);
    }

    /**
     * Retourne la liste des paramètres environnementaux disponibles. Les paramètres
     * environnementaux sont représentés par des noms courts tels que "CHL" ou "SST".
     *
     * @return L'ensemble des paramètres environnementaux disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public Set<String> getAvailableParameters() throws SQLException {
        return ParameterTable.list(statement.getConnection(), PARAMETERS);
    }

    /**
     * Retourne la liste des opérations disponibles. Les opérations sont appliquées sur
     * des paramètres environnementaux. Par exemple les opérations "valeur" et "sobel3"
     * correspondent à la valeur d'un paramètre environnemental et son gradient calculé
     * par l'opérateur de Sobel, respectivement.
     *
     * @return L'ensemble des opérations disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public Set<String> getAvailableOperations() throws SQLException {
        return ParameterTable.list(statement.getConnection(), OPERATIONS);
    }

    /**
     * Définit le paramètre examinée par cette table. Le paramètre doit être un nom
     * de la table "Paramètres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Le paramètre à définir (exemple: "SST").
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void setParameter(final String parameter) throws SQLException {
        final ParameterTable table = new ParameterTable(statement.getConnection(),
                                         ParameterTable.PARAMETER_BY_NAME);
        final int code = table.getParameterID(parameter);
        table.close();

        statement.setInt(ARG_PARAMETER, code);
        this.parameter = code;
        if (update != null) {
            update.setInt(ARG_PARAMETER+1, code);
        }
        if (insert != null) {
            insert.setInt(ARG_PARAMETER, code);
        }
    }

    /**
     * Définit la position relative sur la ligne de pêche où l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void setPosition(final int position) throws SQLException {
        if (position>=START_POINT && position<=END_POINT) {
            statement.setInt(ARG_POSITION, position);
            this.position = position;
            if (update != null) {
                update.setInt(ARG_POSITION+1, position);
            }
            if (insert != null) {
                insert.setInt(ARG_POSITION, position);
            }
        }
        else throw new IllegalArgumentException(String.valueOf(position));
    }

    /**
     * Définit le décalage de temps (en jours). La valeur par défaut est 0.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void setTimeLag(final int timeLag) throws SQLException {
        statement.setInt(ARG_TIMELAG, timeLag);
        this.timeLag = timeLag;
        if (update != null) {
            update.setInt(ARG_TIMELAG+1, timeLag);
        }
        if (insert != null) {
            insert.setInt(ARG_TIMELAG, timeLag);
        }
    }

    /**
     * Retourne le paramètre correspondant à une capture. Cette méthode retourne la valeur
     * de la colonne <code>column</code> (spécifiée lors de la construction) à la ligne qui
     * répond aux critères suivants:
     * <ul>
     *   <li>La capture est l'argument <code>capture</code> spécifié à cette méthode.</li>
     *   <li>Le nom du paramètre ("SST", "CHL", etc.) est celui qui a été spécifié lors du
     *       dernier appel de {@link #setParameter}.</li>
     *   <li>La position ({@link #START_POINT}, {@link #CENTER}, {@link #END_POINT}, etc.)
     *       est celle qui a été spécifiée lors du dernier appel de {@link #setPosition}.</li>
     *   <li>L'écart de temps être la pêche et la mesure environnementale est celui qui a
     *       été spécifié lors du dernier appel de {@link #setTimeLag}.</li>
     * </ul>
     *
     * S'il y a plusieurs valeurs pour la capture spécifié (ce qui ne devrait pas se produire),
     * alors cette méthode retourne la valeur moyenne.
     *
     * @param  capture La capture.
     * @param  value Valeur du paramètre.
     * @throws SQLException si un problème est survenu lors de l'accès à la base de données.
     */
    public synchronized float get(final CatchEntry capture) throws SQLException {
        statement.setInt(ARG_ID, capture.getID());
        int n = 0;
        double sum = 0;
        final ResultSet result=statement.executeQuery();
        while (result.next()) {
            final double value = result.getDouble(1);
            if (!result.wasNull() && !Double.isNaN(value)) {
                sum += value;
                n++;
            }
        }
        return (float)(sum/n);
    }

    /**
     * Met à jour le paramètre correspondant à une capture. Cette méthode met à jour la colonne
     * <code>column</code> (spécifiée lors de la construction) de la ligne qui répond aux mêmes
     * critères que pour la méthode {@link #get}, à quelques exceptions près:
     * <ul>
     *   <li>Si la capture a été prise à un seul point (c'est-à-dire si {@link CatchEntry#getShape}
     *       retourne <code>null</code>), alors cette méthode met à jour la ligne correspondant à
     *       la position {@link #CENTER}, quelle que soit la position spécifiée lors du dernier
     *       appel de {@link #setPosition}.</li>
     * </ul>
     *
     * @param  capture La capture.
     * @param  value La valeur du paramètre. Si cette valeur est <code>NaN</code>,
     *         alors cette méthode ne fait rien. L'ancien paramètre environnemental
     *         sera conservé.
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public void set(final CatchEntry capture, final float value) throws SQLException {
        set(capture, value, null);
    }

    /**
     * Met à jour le paramètre correspondant à une capture. Cette méthode est similaire à
     * {@link #set(CatchEntry, float)}, excepté que l'écart de temps sera calculée à partir
     * de la date spécifiée. Ce décalage sera utilisé à la place de la dernière valeur spécifiée
     * à {@link #setTimeLag}.
     *
     * @param  capture La capture.
     * @param  value La valeur du paramètre.
     * @param  time La date à laquelle a été évaluée la valeur <code>value</code>.
     *         Si cet argument est non-nul, alors l'écart de temps entre cette date
     *         et la date de la capture sera calculée et utilisé à la place de la valeur
     *         spécifiée lors du dernier appel de {@link #setTimeLag}.
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public synchronized void set(final CatchEntry capture, final float value, final Date valueTime) throws SQLException
    {
        if (!Float.isNaN(value)) {
            int timeLag = this.timeLag;
            final Date catchTime = capture.getTime();
            if (catchTime!=null && valueTime!=null) {
                // Les dates de la base de données ne contiennent pas d'heure.
                // La date du "21/01/1999 00:00" peut très bien signifier que
                // la ligne a été mouillée à 18h00. Dans ce cas, une image datée
                // du "20/01/01/1999 23:00" est effectivement une image de la veille.
                timeLag = (int)Math.floor((valueTime.getTime()-catchTime.getTime()) / (24.0*60*60*1000));
            }
            int position = this.position;
            if (capture instanceof AbstractCatchEntry) {
                position = ((AbstractCatchEntry) capture).clampPosition(position);
            }
            if (update==null) {
                update = statement.getConnection().prepareStatement(replaceQuestionMark(
                         preferences.get(ENVIRONMENTS+".UPDATE", SQL_UPDATE), column));
                update.setInt(1+ARG_PARAMETER, parameter);
            }
            update.setInt   (1+ARG_ID,        capture.getID());
            update.setInt   (1+ARG_POSITION,  position);
            update.setInt   (1+ARG_TIMELAG,   timeLag);
            update.setDouble(1,               value); // Note: Should be 'float', but Access doesn't like.
            int n=update.executeUpdate();
            if (n == 0) {
                if (insert==null) {
                    insert = statement.getConnection().prepareStatement(replaceQuestionMark(
                             preferences.get(ENVIRONMENTS+".INSERT", SQL_INSERT), column));
                    insert.setInt(ARG_PARAMETER, parameter);
                }
                insert.setInt   (ARG_ID,        capture.getID());
                insert.setInt   (ARG_POSITION,  position);
                insert.setInt   (ARG_TIMELAG,   timeLag);
                insert.setDouble(ARG_VALUE,     value); // Note: Should be 'float', but Access doesn't like.
                n=insert.executeUpdate();
            }
            if (n != 1) {
                throw new SQLWarning(Resources.format(ResourceKeys.ERROR_UNEXPECTED_UPDATE_$1, new Integer(n)));
            }
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        if (update != null) {
            update.close();
            update=null;
        }
        if (insert != null) {
            insert.close();
            insert=null;
        }
        super.close();
    }
}
