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
import java.sql.SQLException;

// Divers
import java.util.Date;
import javax.media.jai.util.Range;


/**
 * Implémentation de la table interrogeant ou modifiant
 * un paramètre de la base de données d'environnement.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class EnvironmentTableImpl extends Table implements EnvironmentTable
{
    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des données
     * environnementales. L'ordre des colonnes est essentiel. Ces colonnes sont
     * référencées par les constantes [@link #ID}, [@link #POSITION} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] POSITION  */ ENVIRONMENTS+".position, " +
                                /*[02] PARAMETER */ ENVIRONMENTS+".[param] "   +

                    "FROM "+ENVIRONMENTS+" "+
                    "WHERE ID=? AND position=?"+
                    "ORDER BY position";

    /** Numéro de colonne. */ static final int POSITION  =  1;
    /** Numéro de colonne. */ static final int PARAMETER =  2;

    /** Numéro d'argument. */ private static final int ARG_ID       = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION = 2;

    /**
     * Nom du paramètre examiné par cette table.
     */
    private final String parameter;

    /**
     * Position sur la ligne de pêche.
     */
    private int position;

    /**
     * Instruction à utiliser pour les mises à jour.
     * Cette instruction ne sera construite que la
     * première fois où elle sera nécessaire.
     */
    private transient Statement update;

    /**
     * Construit une table pour le paramètre spécifié.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  parameter Le paramètre à mettre à jour.
     * @throws SQLException si <code>EnvironmentTable</code> n'a pas pu construire sa requête SQL.
     */
    protected EnvironmentTableImpl(final Connection connection, final String parameter) throws SQLException
    {
        super(connection.prepareStatement(replace(preferences.get(ENVIRONMENTS, SQL_SELECT), parameter)));
        this.parameter = parameter;
        setPosition(CENTER);
    }

    /**
     * Replace substring "[param]" by parameter name.
     */
    private static String replace(final String query, final String parameter)
    {
        final String PARAM = "[param]";
        final StringBuffer buffer=new StringBuffer(query);
        for (int index=-1; (index=buffer.indexOf(PARAM,index+1))>=0;)
        {
            buffer.replace(index, index+PARAM.length(), parameter);
        }
        return buffer.toString();
    }

    /**
     * Définit la position relative sur la ligne de pêche où l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public synchronized void setPosition(final int position) throws SQLException
    {
        if ((position>=START_POINT && position<=END_POINT) || position==AREA)
        {
            statement.setInt(ARG_POSITION, position);
            this.position = position;
        }
        else throw new IllegalArgumentException(String.valueOf(position));
    }

    /**
     * Retourne le paramètre correspondant à une capture. S'il
     * y a plusieurs valeurs pour la capture spécifié (ce qui
     * ne devrait pas se produire), retourne la moyenne.
     *
     * @param  capture La capture.
     * @param  value Valeur du paramètre.
     * @throws SQLException si un problème est survenu lors de l'accès à la base de données.
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
            if (!Double.isNaN(value))
            {
                sum += value;
                n++;
            }
        }
        return (float)(sum/n);
    }

    /**
     * Met à jour le paramètre correspondant à une capture.
     * Rien ne sera fait si <code>value</code> et une valeur NaN.
     *
     * @param  capture La capture.
     * @param  value La valeur du paramètre.
     * @param  valueTime La date à laquelle a été évaluée la valeur <code>value</code>.
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public synchronized void set(final CatchEntry capture, final float value, final Date valueTime) throws SQLException
    {
        if (!Float.isNaN(value))
        {
            int timeLag = 0;
            final Date catchTime = capture.getTime();
            if (catchTime!=null && valueTime!=null)
            {
                // Les dates de la base de données ne contiennent pas d'heure.
                // La date du "21/01/1999 00:00" peut très bien signifier que
                // la ligne a été mouillée à 18h00. Dans ce cas, une image datée
                // du "20/01/01/1999 23:00" est effectivement une image de la veille.
                timeLag = (int)Math.floor((valueTime.getTime()-catchTime.getTime()) / (24.0*60*60*1000));
            }
            int position = this.position;
            if (capture instanceof AbstractCatchEntry)
            {
                position = ((AbstractCatchEntry) capture).clampPosition(position);
            }
            // NOTE: This implementation is highly inefficient,
            //       but it seems to be the only one working with
            //       Access's ODBC driver (through Sun's JDBC-ODBC).
            if (update==null)
            {
                update = statement.getConnection().createStatement();
            }
            int n;
            n=update.executeUpdate("UPDATE "+ENVIRONMENTS+" SET "+parameter+"="+value+" "+
                                   "WHERE ID="+capture.getID()+" AND position="+position+" AND écart_temps="+timeLag);
            if (n!=0) return;
            n=update.executeUpdate("INSERT INTO "+ENVIRONMENTS+" (ID,position,écart_temps,"+parameter+") "+
                                   "VALUES("+capture.getID()+","+position+","+timeLag+","+value+")");
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
    public synchronized void close() throws SQLException
    {
        if (update!=null)
        {
            update.close();
            update=null;
        }
        super.close();
    }
}
