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
 * Impl�mentation de la table interrogeant ou modifiant
 * un param�tre de la base de donn�es d'environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentTableImpl extends Table implements EnvironmentTable {
    /**
     * Requ�te SQL pour obtenir la table des donn�es environnementales.
     */
    static final String SQL_SELECT=
                    "SELECT [?] FROM "+ENVIRONMENTS+" "+
                    "WHERE ID=? AND position=? AND temps=? AND param�tre=?";

    /**
     * Instruction SQL pour mettre � jour une donn�e environnementale.
     * Note: La valeur est le premier param�tre, et tous les autres sont d�cal�s de 1.
     */
    static final String SQL_UPDATE=
                    "UPDATE "+ENVIRONMENTS+" SET [?]=? "+
                    "WHERE ID=? AND position=? AND temps=? AND param�tre=?";

    /**
     * Instruction SQL pour ajouter une donn�e environnementale.
     */
    static final String SQL_INSERT=
                    "INSERT INTO "+ENVIRONMENTS+" (ID,position,temps,param�tre,[?]) "+
                    "VALUES(?,?,?,?,?)";

    /** Num�ro d'argument. */ private static final int ARG_ID        = 1;
    /** Num�ro d'argument. */ private static final int ARG_POSITION  = 2;
    /** Num�ro d'argument. */ private static final int ARG_TIMELAG   = 3;
    /** Num�ro d'argument. */ private static final int ARG_PARAMETER = 4;
    /** Num�ro d'argument. */ private static final int ARG_VALUE     = 5;

    /**
     * Position sur la ligne de p�che.
     */
    private int position;

    /**
     * D�calage de temps, en jours.
     */
    private int timeLag;

    /**
     * Num�ro du param�tre.
     */
    private int parameter;

    /**
     * Nom de la colonne dans laquelle lire ou �crire les valeurs (exemple "value" ou "sobel").
     */
    private final String column;

    /**
     * Instruction � utiliser pour les mises � jour.
     * Cette instruction ne sera construite que la
     * premi�re fois o� elle sera n�cessaire.
     */
    private transient PreparedStatement update;

    /**
     * Instruction � utiliser pour les insertions.
     * Cette instruction ne sera construite que la
     * premi�re fois o� elle sera n�cessaire.
     */
    private transient PreparedStatement insert;

    /**
     * Construit une table pour le param�tre sp�cifi�.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  parameter Le param�tre � mettre � jour (exemple: "SST").
     * @param  column La colonne dans laquelle lire ou �crire les valeurs (exemple "value" ou "sobel").
     * @throws SQLException si <code>EnvironmentTable</code> n'a pas pu construire sa requ�te SQL.
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
     * Retourne la liste des param�tres environnementaux disponibles. Les param�tres
     * environnementaux sont repr�sent�s par des noms courts tels que "CHL" ou "SST".
     *
     * @return L'ensemble des param�tres environnementaux disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public Set<String> getAvailableParameters() throws SQLException {
        return ParameterTable.list(statement.getConnection(), PARAMETERS);
    }

    /**
     * Retourne la liste des op�rations disponibles. Les op�rations sont appliqu�es sur
     * des param�tres environnementaux. Par exemple les op�rations "valeur" et "sobel3"
     * correspondent � la valeur d'un param�tre environnemental et son gradient calcul�
     * par l'op�rateur de Sobel, respectivement.
     *
     * @return L'ensemble des op�rations disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public Set<String> getAvailableOperations() throws SQLException {
        return ParameterTable.list(statement.getConnection(), OPERATIONS);
    }

    /**
     * D�finit le param�tre examin�e par cette table. Le param�tre doit �tre un nom
     * de la table "Param�tres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Le param�tre � d�finir (exemple: "SST").
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * D�finit la position relative sur la ligne de p�che o� l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * D�finit le d�calage de temps (en jours). La valeur par d�faut est 0.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * Retourne le param�tre correspondant � une capture. Cette m�thode retourne la valeur
     * de la colonne <code>column</code> (sp�cifi�e lors de la construction) � la ligne qui
     * r�pond aux crit�res suivants:
     * <ul>
     *   <li>La capture est l'argument <code>capture</code> sp�cifi� � cette m�thode.</li>
     *   <li>Le nom du param�tre ("SST", "CHL", etc.) est celui qui a �t� sp�cifi� lors du
     *       dernier appel de {@link #setParameter}.</li>
     *   <li>La position ({@link #START_POINT}, {@link #CENTER}, {@link #END_POINT}, etc.)
     *       est celle qui a �t� sp�cifi�e lors du dernier appel de {@link #setPosition}.</li>
     *   <li>L'�cart de temps �tre la p�che et la mesure environnementale est celui qui a
     *       �t� sp�cifi� lors du dernier appel de {@link #setTimeLag}.</li>
     * </ul>
     *
     * S'il y a plusieurs valeurs pour la capture sp�cifi� (ce qui ne devrait pas se produire),
     * alors cette m�thode retourne la valeur moyenne.
     *
     * @param  capture La capture.
     * @param  value Valeur du param�tre.
     * @throws SQLException si un probl�me est survenu lors de l'acc�s � la base de donn�es.
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
     * Met � jour le param�tre correspondant � une capture. Cette m�thode met � jour la colonne
     * <code>column</code> (sp�cifi�e lors de la construction) de la ligne qui r�pond aux m�mes
     * crit�res que pour la m�thode {@link #get}, � quelques exceptions pr�s:
     * <ul>
     *   <li>Si la capture a �t� prise � un seul point (c'est-�-dire si {@link CatchEntry#getShape}
     *       retourne <code>null</code>), alors cette m�thode met � jour la ligne correspondant �
     *       la position {@link #CENTER}, quelle que soit la position sp�cifi�e lors du dernier
     *       appel de {@link #setPosition}.</li>
     * </ul>
     *
     * @param  capture La capture.
     * @param  value La valeur du param�tre. Si cette valeur est <code>NaN</code>,
     *         alors cette m�thode ne fait rien. L'ancien param�tre environnemental
     *         sera conserv�.
     * @throws SQLException si un probl�me est survenu lors de la mise � jour.
     */
    public void set(final CatchEntry capture, final float value) throws SQLException {
        set(capture, value, null);
    }

    /**
     * Met � jour le param�tre correspondant � une capture. Cette m�thode est similaire �
     * {@link #set(CatchEntry, float)}, except� que l'�cart de temps sera calcul�e � partir
     * de la date sp�cifi�e. Ce d�calage sera utilis� � la place de la derni�re valeur sp�cifi�e
     * � {@link #setTimeLag}.
     *
     * @param  capture La capture.
     * @param  value La valeur du param�tre.
     * @param  time La date � laquelle a �t� �valu�e la valeur <code>value</code>.
     *         Si cet argument est non-nul, alors l'�cart de temps entre cette date
     *         et la date de la capture sera calcul�e et utilis� � la place de la valeur
     *         sp�cifi�e lors du dernier appel de {@link #setTimeLag}.
     * @throws SQLException si un probl�me est survenu lors de la mise � jour.
     */
    public synchronized void set(final CatchEntry capture, final float value, final Date valueTime) throws SQLException
    {
        if (!Float.isNaN(value)) {
            int timeLag = this.timeLag;
            final Date catchTime = capture.getTime();
            if (catchTime!=null && valueTime!=null) {
                // Les dates de la base de donn�es ne contiennent pas d'heure.
                // La date du "21/01/1999 00:00" peut tr�s bien signifier que
                // la ligne a �t� mouill�e � 18h00. Dans ce cas, une image dat�e
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
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
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
