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
import java.util.Date;
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
final class EnvironmentTableImpl extends Table implements EnvironmentTable
{
    /**
     * Requ�te SQL pour obtenir le code d'un param�tre environnemental.
     */
    private static final String SQL_MAP_PARAMETER=
                    "SELECT ID FROM "+PARAMETERS+" WHERE name LIKE ?";

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
    /** Num�ro d'argument. */ private static final int ARG_TEMPS     = 3;
    /** Num�ro d'argument. */ private static final int ARG_PARAMETER = 4;
    /** Num�ro d'argument. */ private static final int ARG_VALUE     = 5;

    /**
     * Position sur la ligne de p�che.
     */
    private int position;

    /**
     * D�calage de temps, en jours.
     */
    private int time;

    /**
     * Num�ro du param�tre.
     */
    private int parameter;

    /**
     * Nom de l'op�ration examin� par cette table.
     */
    private final String operation;

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
     * @param  operation L'op�ration (exemple "value" ou "sobel").
     * @throws SQLException si <code>EnvironmentTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected EnvironmentTableImpl(final Connection connection,
                                   final String     parameter,
                                   final String     operation) throws SQLException
    {
        super(connection.prepareStatement(replace(preferences.get(ENVIRONMENTS, SQL_SELECT), operation)));
        this.operation = operation;
        setParameter(parameter);
        setPosition(CENTER);
        setTime(0);
    }

    /**
     * Replace substring "[?]" by an operation name.
     */
    private static String replace(final String query, final String operation)
    {
        final String PARAM = "[?]";
        final StringBuffer buffer=new StringBuffer(query);
        for (int index=-1; (index=buffer.indexOf(PARAM,index+1))>=0;)
        {
            buffer.replace(index, index+PARAM.length(), operation);
        }
        return buffer.toString();
    }

    /**
     * D�finit le param�tre examin�e par cette table.
     *
     * @param parameter Le param�tre � d�finir (exemple: "SST").
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public synchronized void setParameter(final String parameter) throws SQLException
    {
        final PreparedStatement stm = statement.getConnection().prepareStatement(SQL_MAP_PARAMETER);
        stm.setString(1, parameter);
        final ResultSet result = stm.executeQuery();
        int lastParameter=0, count=0;
        while (result.next())
        {
            final int code = result.getInt(1);
            if (count==0 || code!=lastParameter)
            {
                lastParameter = code;
                if (++count >= 2) break;
            }
        }
        result.close();
        stm.close();
        if (count!=1)
        {
            throw new SQLException(Resources.format(count==0 ?
                            ResourceKeys.ERROR_NO_PARAMETER_$1 : 
                            ResourceKeys.ERROR_DUPLICATED_RECORD_$1, parameter));
        }
        statement.setInt(ARG_PARAMETER, lastParameter);
        this.parameter = lastParameter;
        if (update != null)
        {
            update.setInt(ARG_PARAMETER+1, lastParameter);
        }
        if (insert != null)
        {
            insert.setInt(ARG_PARAMETER, lastParameter);
        }
    }

    /**
     * D�finit la position relative sur la ligne de p�che o� l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public synchronized void setPosition(final int position) throws SQLException
    {
        if ((position>=START_POINT && position<=END_POINT) || position==AREA)
        {
            statement.setInt(ARG_POSITION, position);
            this.position = position;
            if (update != null)
            {
                update.setInt(ARG_POSITION+1, position);
            }
            if (insert != null)
            {
                insert.setInt(ARG_POSITION, position);
            }
        }
        else throw new IllegalArgumentException(String.valueOf(position));
    }

    /**
     * D�finit le d�calage de temps (en jours).
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public synchronized void setTime(final int time) throws SQLException
    {
        statement.setInt(ARG_TEMPS, time);
        this.time = time;
        if (update != null)
        {
            update.setInt(ARG_TEMPS+1, time);
        }
        if (insert != null)
        {
            insert.setInt(ARG_TEMPS, time);
        }
    }

    /**
     * Retourne le param�tre correspondant � une capture. S'il
     * y a plusieurs valeurs pour la capture sp�cifi� (ce qui
     * ne devrait pas se produire), retourne la moyenne.
     *
     * @param  capture La capture.
     * @param  value Valeur du param�tre.
     * @throws SQLException si un probl�me est survenu lors de l'acc�s � la base de donn�es.
     */
    public synchronized float get(final CatchEntry capture) throws SQLException
    {
        statement.setInt(ARG_ID, capture.getID());
        int n = 0;
        double sum = 0;
        final ResultSet result=statement.executeQuery();
        while (result.next())
        {
            final double value = result.getDouble(1);
            if (!result.wasNull() && !Double.isNaN(value))
            {
                sum += value;
                n++;
            }
        }
        return (float)(sum/n);
    }

    /**
     * Met � jour le param�tre correspondant � une capture.
     * Rien ne sera fait si <code>value</code> et une valeur NaN.
     *
     * @param  capture La capture.
     * @param  value La valeur du param�tre.
     * @param  valueTime La date � laquelle a �t� �valu�e la valeur <code>value</code>.
     * @throws SQLException si un probl�me est survenu lors de la mise � jour.
     */
    public synchronized void set(final CatchEntry capture, final float value, final Date valueTime) throws SQLException
    {
        if (!Float.isNaN(value))
        {
            int timeLag = 0;
            final Date catchTime = capture.getTime();
            if (catchTime!=null && valueTime!=null)
            {
                // Les dates de la base de donn�es ne contiennent pas d'heure.
                // La date du "21/01/1999 00:00" peut tr�s bien signifier que
                // la ligne a �t� mouill�e � 18h00. Dans ce cas, une image dat�e
                // du "20/01/01/1999 23:00" est effectivement une image de la veille.
                timeLag = (int)Math.floor((valueTime.getTime()-catchTime.getTime()) / (24.0*60*60*1000));
            }
            int position = this.position;
            if (capture instanceof AbstractCatchEntry)
            {
                position = ((AbstractCatchEntry) capture).clampPosition(position);
            }
            if (update==null)
            {
                update = statement.getConnection().prepareStatement(replace(
                         preferences.get(ENVIRONMENTS+".UPDATE", SQL_UPDATE), operation));
                update.setInt(1+ARG_PARAMETER, parameter);
            }
            update.setInt   (1+ARG_ID,        capture.getID());
            update.setInt   (1+ARG_POSITION,  position);
            update.setInt   (1+ARG_TEMPS,     timeLag);
            update.setDouble(1,               value); // Note: Should be 'float', but Access doesn't like.
            int n=update.executeUpdate();
            if (n==0)
            {
                if (insert==null)
                {
                    insert = statement.getConnection().prepareStatement(replace(
                             preferences.get(ENVIRONMENTS+".INSERT", SQL_INSERT), operation));
                    insert.setInt(ARG_PARAMETER, parameter);
                }
                insert.setInt   (ARG_ID,        capture.getID());
                insert.setInt   (ARG_POSITION,  position);
                insert.setInt   (ARG_TEMPS,     timeLag);
                insert.setDouble(ARG_VALUE,     value); // Note: Should be 'float', but Access doesn't like.
                n=insert.executeUpdate();
            }
            if (n!=1)
            {
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
    public synchronized void close() throws SQLException
    {
        if (update!=null)
        {
            update.close();
            update=null;
        }
        if (insert!=null)
        {
            insert.close();
            insert=null;
        }
        super.close();
    }
}
