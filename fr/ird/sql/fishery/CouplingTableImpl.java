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
import java.util.List;
import java.util.ArrayList;

// Resources
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Implémentation d'une table qui fait le lien entre les captures et les paramètres
 * environnementaux aux positions de cette capture. Cette interrogation pourrait être
 * faites dans un logiciel de base de données avec une requête SQL classique. Mais cette
 * requête est assez longue et très laborieuse à faire à la main. De plus, elle dépasse
 * souvent les capacités de Access. Cette classe découpera cette requête monstre en une série
 * de requêtes plus petites.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CouplingTableImpl extends Table {
    /**
     * Requête SQL pour obtenir la table des données environnementales.
     */
    static final String SQL_SELECT=
                    "SELECT ID FROM "+ENVIRONMENTS+" "+
                    "WHERE position=? AND temps=? AND paramètre=? SORTED BY ID";

    /** Numéro de colonne. */ private static final int ID            = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION  = 1;
    /** Numéro d'argument. */ private static final int ARG_TIMELAG   = 2;
    /** Numéro d'argument. */ private static final int ARG_PARAMETER = 3;

    /**
     * Position sur la ligne de pêche.
     */
    private int position;

    /**
     * Décalages de temps, en jours.
     */
    private int[] timeLags = new int[0];

    /**
     * Numéros des paramètres d'intérêt.
     */
    private int[] parameters = timeLags;

    /**
     * Noms des colonnes à utiliser. Chaque colonne correspond à une opération
     * appliquée sur les images (par exemple des interpollations ou des calculs
     * de gradients).
     */
    private String[] columns;

    /**
     * Construit une table pour le paramètre spécifié.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  columns Colonnes des paramètres d'intérêt.
     * @throws SQLException si <code>CouplingTable</code> n'a pas pu construire sa requête SQL.
     */
    protected CouplingTableImpl(final Connection connection,
                                final String[]   columns)
        throws SQLException
    {
        super(connection.prepareStatement(completeQuery(SQL_SELECT, columns)));
        this.columns = (String[]) columns.clone();
        setPosition(EnvironmentTable.CENTER);
    }

    /**
     * Complète la requète SQL en ajoutant les noms de colonnes
     * ainsi que les clauses "IS NOT NULL".
     */
    private static String completeQuery(String query, final String[] columns) {
        query = completeSelect(query, columns);
        query = query.toLowerCase();
        int index = indexOf(query, "SORTED");
        if (index >= 0) {
            final StringBuffer buffer = new StringBuffer(query.substring(0, index));
            buffer.append("WHERE ");
            for (int i=0; i<columns.length; i++) {
                if (i!=0) {
                    buffer.append("AND ");
                }
                buffer.append('(');
                buffer.append(columns[i]);
                buffer.append(" IS NOT NULL) ");
            }
            buffer.append(query.substring(index));
            query = buffer.toString();
        }
        return query;
    }

    /**
     * Définit la position relative sur la ligne de pêche où l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    private void setPosition(final int position) throws SQLException {
        if (position>=EnvironmentTable.START_POINT && position<=EnvironmentTable.END_POINT) {
            statement.setInt(ARG_POSITION, position);
            this.position = position;
        }
        else throw new IllegalArgumentException(String.valueOf(position));
    }

    /**
     * Définit les paramètres examinées par cette table. Les paramètres doivent être des
     * noms de la table "Paramètres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Les paramètres à définir (exemple: "SST").
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void setParameters(final String[] parameters) throws SQLException {
        final ParameterTable table = new ParameterTable(statement.getConnection(), true);
        final int[]  newParameters = new int[parameters.length];
        for (int i=0; i<parameters.length; i++) {
            newParameters[i] = table.getParameterID(parameters[i]);
        }
        table.close();
        this.parameters = newParameters;
    }

    /**
     * Définit les décalages de temps (en jours).
     *
     * @parm   timeLags Décalages de temps en jours.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void setTimeLags(final int[] timeLags) throws SQLException {
        this.timeLags = (int[])timeLags.clone();
    }

    /**
     * Retourne les colonnes de titre pour cette table.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized List<String> getColumnTitles() throws SQLException {
        final ParameterTable table = new ParameterTable(statement.getConnection(), true);
        final List<String>  titles = new ArrayList<String>(parameters.length * timeLags.length + 1);
        titles.add("Capture");
        final StringBuffer buffer = new StringBuffer();
        for (int i=0; i<parameters.length; i++) {
            buffer.setLength(0);
            buffer.append(table.getParameterName(parameters[i]));
            final int offset = buffer.length();
            for (int j=0; j<timeLags.length; j++) {
                buffer.setLength(offset);
                int t = timeLags[j];
                buffer.append(t<0 ? '-' : '+');
                t = Math.abs(t);
                if (t<10) {
                    buffer.append(0);
                }
                buffer.append(t);
                titles.add(buffer.toString());
            }
        }
        table.close();
        return titles;
    }

    /**
     * Retourne toutes les données. La première colonne du {@link ResultSet} retourné
     * contiendra le numéro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les paramètres environnementaux. Le nombre total de colonnes est
     * égal à la longueur de la liste retournée par {@link #getColumnTitles}.
     *
     * @return Les données environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public synchronized ResultSet getData() throws SQLException {
        int i=0;
        final ResultSet[] results = new ResultSet[parameters.length * timeLags.length];
        for (int p=0; p<parameters.length; p++) {
            statement.setInt(ARG_PARAMETER, p);
            for (int t=0; t<timeLags.length; t++) {
                statement.setInt(ARG_TIMELAG, t);
                results[i++] = statement.executeQuery();
            }
        }
        assert i == results.length;
        switch (results.length) {
            case 0:  return null;
            case 1:  return results[0];
            default: return new CouplingResultSet(results, columns.length);
        }
    }
}
