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
 * Une �tape dans la construction d'une table des param�tres environnementaux.
 * Cette �tape comprend les donn�es environnementales d'un param�tre � une coordonn�es
 * spatio-temporelle donn�es. La table {@link EnvironmentRowSet} repr�sentera les
 * donn�es d'un ensemble de param�tres � diff�rentes coordonn�es spatio-temporelles.
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
     * Requ�te SQL pour obtenir la table des donn�es environnementales.
     */
    static final String SQL_SELECT=
                    "SELECT ID FROM "+ENVIRONMENTS+"\n"+
                    "WHERE position=? AND temps=? AND param�tre=? ORDER BY ID";

    /** Num�ro de colonne. */ private static final int ID            = 1;
    /** Num�ro d'argument. */ private static final int ARG_POSITION  = 1;
    /** Num�ro d'argument. */ private static final int ARG_TIMELAG   = 2;
    /** Num�ro d'argument. */ private static final int ARG_PARAMETER = 3;

    /**
     * Le code du param�tre.
     */
    final int parameter;

    /**
     * La position spatiale relativement � l'observation (la p�che).
     */
    final int position;

    /**
     * La position temporelle (en jours) relativement � l'observation (la p�che).
     */
    final int timeLag;

    /**
     * Colonnes utilis�es lors de la construction de {@link #statement}.
     * Si de nouvelles colonnes sont ajout�es, alors {@link #statement}
     * devra �tre ferm� et reconstruit.
     */
    private transient Set<String> columns;

    /**
     * M�morise les codes d'un param�tre ainsi que de sa position spatio-temporelle.
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
     * Retourne une valeur "hash code" pour ce param�tre. Seuls le param�tre et
     * les coordonn�es spatio-temporelles sont pris en compte.   Cela permet de
     * placer cet objet <code>EnvironmentTableStep</code> comme cl� dans un
     * {@link java.util.Map}.
     */
    public int hashCode() {
        return (parameter*37 + position)*37 + timeLag;
    }

    /**
     * V�rifie si cet objet <code>EnvironmentTableStep</code> est identique � l'objet sp�cifi�.
     * Seuls le param�tre et les coordonn�es spatio-temporelles sont pris en compte.
     * Cela permet de placer cet objet <code>EnvironmentTableStep</code> comme cl�
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
     * Ajoute une colonne � la liste des colonnes � lire. Chaque colonne correspond
     * � une op�ration appliqu�e sur les donn�es (par exemple "valeur" ou "sobel3").
     *
     * @param  column Colonne � ajouter.
     * @throws SQLException si l'op�ration a �chou�e.
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
     * Retire une colonne � la liste des colonnes � lire. Chaque colonne correspond
     * � une op�ration appliqu�e sur les donn�es (par exemple "valeur" ou "sobel3").
     *
     * @param  column Colonne � retirer.
     * @throws SQLException si l'op�ration a �chou�e.
     */
    public synchronized void removeColumn(final String column) throws SQLException {
        if (columns != null) {
            if (columns.remove(column)) {
                super.close();
            }
        }
    }

    /**
     * Retourne toutes les colonnes qui ont �t� d�clar�es.
     * Cette m�thode ne retourne jamais <code>null</code>.
     */
    public synchronized String[] getColumns() {
        if (columns != null) {
            return columns.toArray(new String[columns.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Retourne les valeurs environnementales en utilisant la connexion sp�cifi�e.
     *
     * @param  connection Connection � utiliser (n�cessaire parce que {@link #statement} peut �tre nul).
     * @return Ensemble des valeurs environnementales pour ce param�tre.
     * @throws SQLException si la connection � la base de donn�es a �chou�e.
     */
    public synchronized ResultSet getResultSet(final Connection connection) throws SQLException {
        if (statement == null) {
            //
            // Compl�te la requ�te SQL en ajoutant les noms de
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
     * Lib�re les ressources utilis�es par cet objet.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        columns = null;
        super.close();
    }
}
