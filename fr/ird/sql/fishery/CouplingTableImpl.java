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

// Entrés/sorties et formattage
import java.io.Writer;
import java.io.IOException;
import java.text.NumberFormat;

// Divers
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Geotools
import org.geotools.resources.Utilities;

// Resources
import fr.ird.util.XArray;
import fr.ird.sql.DataBase;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Implémentation d'une table qui fait le lien entre les captures et les paramètres
 * environnementaux aux positions de cette capture. Cette interrogation pourrait être
 * faites dans un logiciel de base de données avec une requête SQL classique. Mais cette
 * requête est assez longue et très laborieuse à construire à la main. De plus, elle dépasse
 * souvent les capacités de Access. Cette classe découpera cette requête monstre en une série
 * de requêtes plus petites.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CouplingTableImpl extends Table implements CouplingTable {
    /**
     * Requête SQL pour obtenir la table des données environnementales.
     */
    static final String SQL_SELECT=
                    "SELECT ID FROM "+ENVIRONMENTS+" "+
                    "WHERE position=? AND temps=? AND paramètre=? ORDER BY ID";

    /** Numéro de colonne. */ private static final int ID            = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION  = 1;
    /** Numéro d'argument. */ private static final int ARG_TIMELAG   = 2;
    /** Numéro d'argument. */ private static final int ARG_PARAMETER = 3;

    /**
     * Abréviation à utiliser à la place des noms de colonnes.
     * @task TODO: Devrait apparaître dans la base de données.
     */
    private static final String[] ALIAS = {"pixel",  "\u225E",  // Mesured by
                                           "valeur", "",
                                           "sobel3", "\u2207\u2083",   // Nabla 3
                                           "sobel5", "\u2207\u2085",   // Nabla 5
                                           "sobel7", "\u2207\u2087",   // Nabla 7
                                           "sobel9", "\u2207\u2089"};  // Nabla 9

    /**
     * Position sur la ligne de pêche.
     */
    private int position;

    /**
     * Décalages de temps, en jours. La valeur par défaut
     * ne contient que le décalage de 0 jours.
     */
    private int[] timeLags = new int[] {0};

    /**
     * Numéros des paramètres d'intérêt.
     */
    private int[] parameters;

    /**
     * Noms des colonnes à utiliser. Chaque colonne correspond à une opération
     * appliquée sur les images (par exemple des interpollations ou des calculs
     * de gradients).
     */
    private final String[] operations;

    /**
     * La connection vers la base de données.
     * TODO: remplacer par <code>statement.getConnection()</code> si on utilise
     *       un jour le constructeur 'super(...)' avec une valeur non-nulle.
     */
    private final Connection connection;

    /**
     * Liste des objets {@link PreparedStatements} construits, ou <code>null</code>
     * si aucun n'a été construit pour l'instant.
     */
    private transient PreparedStatement[] statements;

    /**
     * Construit une table pour le paramètre spécifié.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  parameters Paramètres (exemple "SST" ou "EKP"). La liste des paramètres
     *         disponibles peut être obtenu avec {@link #getAvailableParameters()}.
     * @param  operations Opérations (exemple "valeur" ou "sobel"). Ces opérations
     *         correspondent à des noms de colonnes de la table "Environnement".
     * @throws SQLException si <code>CouplingTable</code> n'a pas pu construire sa requête SQL.
     */
    protected CouplingTableImpl(final Connection connection,
                                final String[]   parameters, 
                                final String[]   operations)
        throws SQLException
    {
        super(null);
        this.connection = connection;
        // Note: if we change 'operations', then 'statements' must be set to 'null'.
        this.operations = (String[]) operations.clone();
        setPosition(EnvironmentTable.CENTER);
        setParameters(parameters);
    }

    /**
     * Complète la requète SQL en ajoutant les noms de colonnes
     * ainsi que les clauses "IS NOT NULL".
     */
    private static String completeQuery(String query, final String[] columns) {
        query = completeSelect(query, columns);
        int index = indexOf(query, "ORDER");
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
        record.setSourceClassName ("CouplingTable");
        record.setSourceMethodName("<init>");
        logger.log(record);
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
        final ParameterTable table = new ParameterTable(connection, true);
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
     * Returns the alias for a column name.
     */
    private static final String getAlias(final String operation) {
        for (int i=0; i<ALIAS.length; i+=2) {
            if (ALIAS[i].equalsIgnoreCase(operation)) {
                return ALIAS[i+1];
            }
        }
        return operation + '_';
    }

    /**
     * Retourne les colonnes de titres pour cette table.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized String[] getColumnTitles() throws SQLException {
        final ParameterTable table = new ParameterTable(connection, false);
        final String[]      titles = new String[operations.length * parameters.length * timeLags.length + 1];
        final StringBuffer  buffer = new StringBuffer();
        int count = 0;
        titles[count++] = "Capture";
        for (int k=0; k<operations.length; k++) {
            buffer.setLength(0);
            buffer.append(getAlias(operations[k]));
            final int opOffset = buffer.length();
            for (int i=0; i<parameters.length; i++) {
                buffer.setLength(opOffset);
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
                    titles[count++] = buffer.toString();
                }
            }
        }
        table.close();
        assert count == titles.length;
        return titles;
    }

    /**
     * Retourne toutes les données. La première colonne du {@link ResultSet} retourné
     * contiendra le numéro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les paramètres environnementaux. Le nombre total de colonnes est
     * égal à la longueur de la liste retournée par {@link #getColumnTitles}.
     * <br><br>
     * Note: si cette méthode est appelée plusieurs fois, alors chaque nouvel
     *       appel fermera le {@link ResultSet} de l'appel précédent.
     *
     * @return Les données environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public synchronized ResultSet getData() throws SQLException {
        /*
         * Construct an array of PreparedStatement.
         * If an array already existed, reuse this array.
         */
        int i;
        final ResultSet[] results = new ResultSet[parameters.length * timeLags.length];
        if (statements == null) {
            statements = new PreparedStatement[results.length];
            i = 0;
        } else {
            for (i=results.length; i<statements.length; i++) {
                statements[i].close();
            }
            statements = XArray.resize(statements, results.length);
            // Keep current value of 'i'
        }
        final String query = completeQuery(SQL_SELECT, operations);
        while (i<results.length) {
            statements[i++] = connection.prepareStatement(query);
        }
        /*
         * Gets the ResultSet for each PreparedStatement.
         */
        i = 0;
        for (int p=0; p<parameters.length; p++) {
            for (int t=0; t<timeLags.length; t++) {
                final PreparedStatement statement = statements[i];
                statement.setInt(ARG_POSITION,  position);
                statement.setInt(ARG_PARAMETER, parameters[p]);
                statement.setInt(ARG_TIMELAG,   timeLags  [t]);
                results[i++] = statement.executeQuery();
            }
        }
        assert i == results.length;
        switch (results.length) {
            case 0:  return null;
            case 1:  return results[0];
            default: return new CouplingResultSet(results, operations.length);
        }
    }

    /**
     * Affiche les enregistrements vers le flot spécifié.
     * Cette méthode est surtout utile à des fins de vérification.
     *
     * @param out Flot de sortie.
     * @param max Nombre maximal d'enregistrements à écrire.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public synchronized void print(final Writer out, int max) throws SQLException, IOException {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final String[] titles = getColumnTitles();
        final int[] width = new int[titles.length];
        for (int i=0; i<titles.length; i++) {
            out.write(titles[i]);
            int length = titles[i].length();
            width[i] = Math.max(i==0 ? 11 : 7, length);
            out.write(Utilities.spaces(width[i]-length + 1));
        }
        out.write(lineSeparator);
        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        final ResultSet result = getData();
        while (--max>=0 && result.next()) {
            for (int i=0; i<width.length; i++) {
                final String value;
                if (i==0) {
                    value = String.valueOf(result.getInt(i+1));
                } else {
                    value = String.valueOf(format.format(result.getDouble(i+1)));
                }
                out.write(Utilities.spaces(width[i]-value.length()));
                out.write(value);
                out.write(' ');
            }
            out.write(lineSeparator);
        }
        result.close();
        out.flush();
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
        final PreparedStatement[] toClose = statements;
        if (toClose != null) {
            statements = null;
            for (int i=toClose.length; --i>=0;) {
                if (toClose[i] != null) {
                    toClose[i].close();
                }
            }
        }
        super.close();
    }
}
