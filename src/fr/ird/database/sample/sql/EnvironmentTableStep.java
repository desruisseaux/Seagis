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
package fr.ird.database.sample.sql;

// J2SE
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.LinkedHashSet;
import java.util.Set;

// Geotools
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.sample.SampleDataBase;
import fr.ird.database.sample.ParameterEntry;
import fr.ird.database.sample.OperationEntry;
import fr.ird.database.sample.RelativePositionEntry;


/**
 * Une étape dans la construction d'une table des paramètres environnementaux.
 * Cette étape comprend les données environnementales d'un paramètre à une coordonnées
 * spatio-temporelle donnée. La table {@link EnvironmentRowSet} représentera les
 * données d'un ensemble de paramètres à différentes coordonnées spatio-temporelles.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleTableStep
 * @see EnvironmentRowSet
 * @see EnvironmentTable
 */
final class EnvironmentTableStep extends Table {
    /**
     * Requête SQL pour obtenir la table des données environnementales.
     * Cette requête <strong>doit</strong> être classé par numéro ID.
     */
    static final String SQL_SELECT=
                    "SELECT ID FROM "+ENVIRONMENTS+" WHERE position=? AND paramètre=? ORDER BY ID";

    /** Numéro de colonne. */ private static final int ID            = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION  = 1;
    /** Numéro d'argument. */ private static final int ARG_PARAMETER = 2;

    /**
     * Le code du paramètre.
     */
    final ParameterEntry parameter;

    /**
     * La position spatio-temporelle relativement à l'échantillon.
     */
    final RelativePositionEntry position;

    /**
     * Indique si les valeurs nulles sont autorisées.
     * La valeur par défaut est <code>false</code>, ce qui indique que tous les
     * enregistrement pour lesquelles au moins un paramètre environnemental est
     * manquant seront omis.
     */
    final boolean nullIncluded;

    /**
     * Colonnes utilisées lors de la construction de {@link #statement}.
     * Si de nouvelles colonnes sont ajoutées, alors {@link #statement}
     * devra être fermé et reconstruit.
     */
    private Set<OperationEntry> columns;

    /**
     * Mémorise les codes d'un paramètre ainsi que de sa position spatio-temporelle.
     * La requête SQL sera créée plus tard, lorsque {@link #getResultSet} sera appelée.
     */
    public EnvironmentTableStep(final ParameterEntry parameter,
                                RelativePositionEntry position,
                                final boolean     nullIncluded)
    {
        super(null);
        if (position == null) {
            position = fr.ird.database.sample.sql.RelativePositionEntry.NULL;
        }
        this.parameter    = parameter;
        this.position     = position;
        this.nullIncluded = nullIncluded;
    }

    /**
     * Retourne une valeur "hash code" pour ce paramètre. Seuls le paramètre et
     * les coordonnées spatio-temporelles sont pris en compte.   Cela permet de
     * placer cet objet <code>EnvironmentTableStep</code> comme clé dans un
     * {@link java.util.Map}.
     */
    public int hashCode() {
        return parameter.hashCode()*37 + position.hashCode();
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
            return this.nullIncluded == that.nullIncluded &&
                   Utilities.equals(this.parameter, that.parameter) &&
                   Utilities.equals(this.position,  that.position);
                   // Do NOT compare columns.
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
    public synchronized void addColumn(final OperationEntry column) throws SQLException {
        if (columns == null) {
            columns = new LinkedHashSet<OperationEntry>();
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
    public synchronized void removeColumn(final OperationEntry column) throws SQLException {
        if (columns != null) {
            if (columns.remove(column)) {
                super.close();
            }
        }
    }

    /**
     * Retourne <code>true</code> si cet objet contient la colonne spécifiée.
     */
    public synchronized boolean hasColumn(final OperationEntry column) {
        return columns!=null && columns.contains(column);
    }

    /**
     * Retourne toutes les colonnes qui ont été déclarées.
     * Cette méthode ne retourne jamais <code>null</code>.
     *
     * @param prefix <code>true</code> pour retourner les préfix plutôt que les noms.
     */
    final synchronized String[] getColumns(final boolean prefix) {
        if (columns != null) {
            final String[] array = new String[columns.size()];
            int count = 0;
            for (final OperationEntry column : columns) {
                array[count++] = prefix ? column.getPrefix() : column.getColumn();
            }
            assert count == array.length;
            return array;
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
            final String[] columns = getColumns(false);
            String query = completeSelect(preferences.get(ENVIRONMENTS, SQL_SELECT), columns);
            if (!nullIncluded) {
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
            }
            final LogRecord record = new LogRecord(SampleDataBase.SQL_SELECT, query);
            record.setSourceClassName ("EnvironmentTable");
            record.setSourceMethodName("getRowSet");
            SampleDataBase.LOGGER.log(record);
            statement = connection.prepareStatement(query);
        }
        statement.setInt(ARG_PARAMETER, parameter.getID());
        statement.setInt(ARG_POSITION,  position.getID());
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
