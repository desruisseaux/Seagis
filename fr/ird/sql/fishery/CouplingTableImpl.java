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

// Requ�tes SQL
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Entr�s/sorties et formattage
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
 * Impl�mentation d'une table qui fait le lien entre les captures et les param�tres
 * environnementaux aux positions de cette capture. Cette interrogation pourrait �tre
 * faites dans un logiciel de base de donn�es avec une requ�te SQL classique. Mais cette
 * requ�te est assez longue et tr�s laborieuse � construire � la main. De plus, elle d�passe
 * souvent les capacit�s de Access. Cette classe d�coupera cette requ�te monstre en une s�rie
 * de requ�tes plus petites.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CouplingTableImpl extends Table implements CouplingTable {
    /**
     * Requ�te SQL pour obtenir la table des donn�es environnementales.
     */
    static final String SQL_SELECT=
                    "SELECT ID FROM "+ENVIRONMENTS+" "+
                    "WHERE position=? AND temps=? AND param�tre=? ORDER BY ID";

    /** Num�ro de colonne. */ private static final int ID            = 1;
    /** Num�ro d'argument. */ private static final int ARG_POSITION  = 1;
    /** Num�ro d'argument. */ private static final int ARG_TIMELAG   = 2;
    /** Num�ro d'argument. */ private static final int ARG_PARAMETER = 3;

    /**
     * Abr�viation � utiliser � la place des noms de colonnes.
     * @task TODO: Devrait appara�tre dans la base de donn�es.
     */
    private static final String[] ALIAS = {"pixel",  "\u225E",  // Mesured by
                                           "valeur", "",
                                           "sobel3", "\u2207\u2083",   // Nabla 3
                                           "sobel5", "\u2207\u2085",   // Nabla 5
                                           "sobel7", "\u2207\u2087",   // Nabla 7
                                           "sobel9", "\u2207\u2089"};  // Nabla 9

    /**
     * Position sur la ligne de p�che.
     */
    private int position;

    /**
     * D�calages de temps, en jours. La valeur par d�faut
     * ne contient que le d�calage de 0 jours.
     */
    private int[] timeLags = new int[] {0};

    /**
     * Num�ros des param�tres d'int�r�t.
     */
    private int[] parameters;

    /**
     * Noms des colonnes � utiliser. Chaque colonne correspond � une op�ration
     * appliqu�e sur les images (par exemple des interpollations ou des calculs
     * de gradients).
     */
    private final String[] operations;

    /**
     * La connection vers la base de donn�es.
     * TODO: remplacer par <code>statement.getConnection()</code> si on utilise
     *       un jour le constructeur 'super(...)' avec une valeur non-nulle.
     */
    private final Connection connection;

    /**
     * Liste des objets {@link PreparedStatements} construits, ou <code>null</code>
     * si aucun n'a �t� construit pour l'instant.
     */
    private transient PreparedStatement[] statements;

    /**
     * Construit une table pour le param�tre sp�cifi�.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  parameters Param�tres (exemple "SST" ou "EKP"). La liste des param�tres
     *         disponibles peut �tre obtenu avec {@link #getAvailableParameters()}.
     * @param  operations Op�rations (exemple "valeur" ou "sobel"). Ces op�rations
     *         correspondent � des noms de colonnes de la table "Environnement".
     * @throws SQLException si <code>CouplingTable</code> n'a pas pu construire sa requ�te SQL.
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
     * Compl�te la requ�te SQL en ajoutant les noms de colonnes
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
     * D�finit la position relative sur la ligne de p�che o� l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    private void setPosition(final int position) throws SQLException {
        if (position>=EnvironmentTable.START_POINT && position<=EnvironmentTable.END_POINT) {
            this.position = position;
        }
        else throw new IllegalArgumentException(String.valueOf(position));
    }

    /**
     * D�finit les param�tres examin�es par cette table. Les param�tres doivent �tre des
     * noms de la table "Param�tres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Les param�tres � d�finir (exemple: "SST").
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * D�finit les d�calages de temps (en jours).
     *
     * @parm   timeLags D�calages de temps en jours.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * Retourne toutes les donn�es. La premi�re colonne du {@link ResultSet} retourn�
     * contiendra le num�ro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les param�tres environnementaux. Le nombre total de colonnes est
     * �gal � la longueur de la liste retourn�e par {@link #getColumnTitles}.
     * <br><br>
     * Note: si cette m�thode est appel�e plusieurs fois, alors chaque nouvel
     *       appel fermera le {@link ResultSet} de l'appel pr�c�dent.
     *
     * @return Les donn�es environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
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
     * Affiche les enregistrements vers le flot sp�cifi�.
     * Cette m�thode est surtout utile � des fins de v�rification.
     *
     * @param out Flot de sortie.
     * @param max Nombre maximal d'enregistrements � �crire.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     * @throws IOException si une erreur est survenue lors de l'�criture.
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
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
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
