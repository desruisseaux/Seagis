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

// J2SE dependencies
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.LinkedHashSet;
import java.util.Set;

// SEAS dependencies
import fr.ird.sql.DataBase;


/**
 * Une étape dans la construction d'une table des paramètres environnementaux.
 * Cette étape comprend les données environnementales d'un paramètre à une coordonnées
 * spatio-temporelle données. La table {@link EnvironmentRowSet} représentera les
 * données d'un ensemble de paramètres à différentes coordonnées spatio-temporelles.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see CatchTableStep
 * @see EnvironmentTableStep
 * @see EnvironmentRowSet
 * @see EnvironmentTable
 */
final class EnvironmentTableStep extends Table {
    /**
     * Requête SQL pour obtenir la table des données environnementales.
     */
    static final String SQL_SELECT=
                    "SELECT ID FROM "+ENVIRONMENTS+"\n"+
                    "WHERE position=? AND temps=? AND paramètre=? ORDER BY ID";

    /** Numéro de colonne. */ private static final int ID            = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION  = 1;
    /** Numéro d'argument. */ private static final int ARG_TIMELAG   = 2;
    /** Numéro d'argument. */ private static final int ARG_PARAMETER = 3;

    /**
     * Le code du paramètre.
     */
    final int parameter;

    /**
     * La position spatiale relativement à l'observation (la pêche).
     */
    final int position;

    /**
     * La position temporelle (en jours) relativement à l'observation (la pêche).
     */
    final int timeLag;

    /**
     * Colonnes utilisées lors de la construction de {@link #statement}.
     * Si de nouvelles colonnes sont ajoutées, alors {@link #statement}
     * devra être fermé et reconstruit.
     */
    private transient Set<String> columns;

    /**
     * Mémorise les codes d'un paramètre ainsi que de sa position spatio-temporelle.
     */
    public EnvironmentTableStep(final int parameter, final int position, final int timeLag) {
        super(null);
        this.parameter = parameter;
        this.position  = position;
        this.timeLag   = timeLag;
        if (!(position>=EnvironmentTable.START_POINT && position<=EnvironmentTable.END_POINT)) {
            throw new IllegalArgumentException(String.valueOf(position));
        }
    }

    /**
     * Retourne une valeur "hash code" pour ce paramètre. Seuls le paramètre et
     * les coordonnées spatio-temporelles sont pris en compte.   Cela permet de
     * placer cet objet <code>EnvironmentTableStep</code> comme clé dans un
     * {@link java.util.Map}.
     */
    public int hashCode() {
        return (parameter*37 + position)*37 + timeLag;
    }

    /**
     * Vérifie si cet objet <code>EnvironmentTableStep</code> est identique à l'objet spécifié.
     * Seuls le paramètre et les coordonnées spatio-temporelles sont pris en compte.
     * Cela permet de placer cet objet <code>EnvironmentTableStep</code> comme clé
     * dans un {@link java.util.Map}.
     */
    public boolean equals(final Object object) {
        if (object instanceof EnvironmentTableStep) {
            final EnvironmentTableStep that = (EnvironmentTableStep) object;
            return this.parameter == that.parameter &&
                   this.position  == that.position  &&
                   this.timeLag   == that.timeLag;
        }
        return false;
    }

    /**
     * Indique si cet objet ne contient aucune colonne.
     */
    public boolean isEmpty() {
        return (columns==null || columns.isEmpty());
    }

    /**
     * Ajoute une colonne à la liste des colonnes à lire. Chaque colonne correspond
     * à une opération appliquée sur les données (par exemple "valeur" ou "sobel3").
     *
     * @param  column Colonne à ajouter.
     * @throws SQLException si l'opération a échouée.
     */
    public synchronized void addColumn(final String column) throws SQLException {
        if (columns == null) {
            columns = new LinkedHashSet<String>();
        }
        if (columns.add(column)) {
            super.close();
        }
    }

    /**
     * Retire une colonne à la liste des colonnes à lire. Chaque colonne correspond
     * à une opération appliquée sur les données (par exemple "valeur" ou "sobel3").
     *
     * @param  column Colonne à retirer.
     * @throws SQLException si l'opération a échouée.
     */
    public synchronized void removeColumn(final String column) throws SQLException {
        if (columns != null) {
            if (columns.remove(column)) {
                super.close();
            }
        }
    }

    /**
     * Retourne toutes les colonnes qui ont été déclarées.
     * Cette méthode ne retourne jamais <code>null</code>.
     */
    public synchronized String[] getColumns() {
        if (columns != null) {
            return columns.toArray(new String[columns.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Retourne les valeurs environnementales en utilisant la connexion spécifiée.
     *
     * @param  connection Connection à utiliser (nécessaire parce que {@link #statement} peut être nul).
     * @return Ensemble des valeurs environnementales pour ce paramètre.
     * @throws SQLException si la connection à la base de données a échouée.
     */
    public synchronized ResultSet getResultSet(final Connection connection) throws SQLException {
        if (statement == null) {
            //
            // Complète la requète SQL en ajoutant les noms de
            // colonnes ainsi que les clauses "IS NOT NULL".
            //
            final String[] columns = getColumns();
            String query = completeSelect(preferences.get(ENVIRONMENTS, SQL_SELECT), columns);
            int index = indexOfWord(query, "ORDER");
            if (index >= 0) {
                final StringBuffer buffer = new StringBuffer(query.substring(0, index));
                for (int i=0; i<columns.length; i++) {
                    buffer.append("AND ");
                    buffer.append('(');
                    buffer.append(columns[i]);
                    buffer.append(" IS NOT NULL) ");
                }
                buffer.append(query.substring(index));
                query = buffer.toString();
            }
            final LogRecord record = new LogRecord(DataBase.SQL_SELECT, query);
            record.setSourceClassName ("EnvironmentTable");
            record.setSourceMethodName("getRowSet");
            logger.log(record);
            statement = connection.prepareStatement(query);
        }
        statement.setInt(ARG_PARAMETER, parameter);
        statement.setInt(ARG_POSITION,  position );
        statement.setInt(ARG_TIMELAG,   timeLag  );
        return statement.executeQuery();
    }

    /**
     * Libère les ressources utilisées par cet objet.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        columns = null;
        super.close();
    }
}
