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

// Requêtes SQL
import java.sql.Types;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLWarning;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import javax.sql.RowSet;

// Collections
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;

// Entrés/sorties et divers
import java.io.Writer;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Geotools
import org.geotools.resources.Utilities;
import org.geotools.util.ProgressListener;

// Resources
import fr.ird.resources.XArray;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.sample.SampleDataBase;
import fr.ird.database.sample.SampleEntry;
import fr.ird.database.sample.ParameterEntry;
import fr.ird.database.sample.OperationEntry;
import fr.ird.database.sample.RelativePositionEntry;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Implémentation d'une table qui fait le lien entre les échantillons et les paramètres
 * environnementaux aux positions de cet échantillon. Cette interrogation pourrait être
 * faites dans un logiciel de base de données avec une requête SQL classique. Mais cette
 * requête est assez longue et très laborieuse à construire à la main. De plus, elle dépasse
 * souvent les capacités de Access. Cette classe découpera cette requête monstre en une série
 * de requêtes plus petites.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentTable extends Table implements fr.ird.database.sample.EnvironmentTable {
    /**
     * Instruction SQL pour mettre à jour une donnée environnementale.
     * Note: La valeur est le premier paramètre, et tous les autres sont décalés de 1.
     */
    static final String SQL_UPDATE=
                    "UPDATE "+ENVIRONMENTS+" SET [?]=? "+
                    "WHERE ID=? AND position=? AND paramètre=?";

    /**
     * Instruction SQL pour ajouter une donnée environnementale.
     */
    static final String SQL_INSERT=
                    "INSERT INTO "+ENVIRONMENTS+" (ID,position,paramètre,[?]) "+
                    "VALUES(?,?,?,?)";

    /** Numéro d'argument. */ private static final int ARG_ID        = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION  = 2;
    /** Numéro d'argument. */ private static final int ARG_PARAMETER = 3;
    /** Numéro d'argument. */ private static final int ARG_VALUE     = 4;

    /**
     * Table des échantillons à joindre avec les paramètres environnementaux retournés par
     * {@link #getRowSet}, ou <code>null</code> si aucune. Il ne s'agit pas nécessairement
     * de la table <code>&quot;Samples&quot;</code>. Il pourrait s'agir d'une requête,
     * comme par exemple <code>&quot;Présences par espèces&quot;<code>.
     *
     * @see #setSampleTable
     */
    private SampleTableStep sampleTableStep;

    /**
     * Liste des paramètres et des opérations à prendre en compte. Les clés sont des
     * objets  {@link EnvironmentTableStep}  représentant le paramètre ainsi que sa
     * position spatio-temporelle.
     */
    private final Map<EnvironmentTableStep, EnvironmentTableStep> parameters =
                  new LinkedHashMap<EnvironmentTableStep, EnvironmentTableStep>();

    /**
     * Indique si les valeurs nulles sont permises pour chaque colonne de {@link #getRowSet}.
     * Ce tableau est calculé en même temps que les étiquettes par {@link #getColumnLabels}.
     */
    private transient boolean[] nullIncluded;

    /**
     * La table des séries. Cette table ne sera pas fermée par {@link #close},
     * puisqu'elle n'appartient pas à cet objet <code>EnvironmentTable</code>.
     */
    private final SeriesTable seriesTable;

    /**
     * Table des paramètres. Cette table est construite
     * automatiquement la première fois où elle est nécessaire.
     */
    private transient ParameterTable parameterTable;

    /**
     * Table des opérations. Cette table est construite
     * automatiquement la première fois où elle est nécessaire.
     */
    private transient OperationTable operationTable;

    /**
     * Table des positions. Cette table est construite automatiquement la
     * première fois où elle est nécessaire.
     */
    private transient RelativePositionTable positionTable;

    /**
     * Instruction à utiliser pour les mises à jour et les insertions.
     * Ces instructions ne seront construites que la première fois où
     * elle seront nécessaires.
     */
    private transient PreparedStatement update, insert;

    /**
     * Nom des colonnes utilisées lors de la dernière création des instructions
     * {@link #update} et {@link #insert}. Sert à éviter de reconstruire de
     * nouvelles instructions lorsque ce n'est pas nécessaire (c'est-à-dire
     * lorsque le nom n'a pas changé).
     */
    private transient String columnUpdate, columnInsert;

    /**
     * La connection vers la base de données.
     *
     * @task TODO: remplacer par <code>statement.getConnection()</code> si on utilise
     *             un jour le constructeur 'super(...)' avec une valeur non-nulle.
     */
    private final Connection connection;

    /**
     * Construit une table.
     *
     * @param  connection Connection vers une base de données des échantillons.
     * @param  series La table des séries. Cette table ne sera pas fermée par {@link #close},
     *         puisqu'elle n'appartient pas à cet objet <code>EnvironmentTable</code>.
     * @throws SQLException si <code>EnvironmentTable</code> n'a pas pu construire sa requête SQL.
     */
    protected EnvironmentTable(final Connection connection, final SeriesTable series) throws SQLException {
        super(null);
        this.connection  = connection;
        this.seriesTable = series;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setSampleTable(final String table) throws SQLException {
        if (!Utilities.equals(table, getSampleTable())) {
            if (sampleTableStep != null) {
                sampleTableStep.close();
                sampleTableStep = null;
            }
            final LogRecord record = Resources.getResources(null).getLogRecord(Level.CONFIG,
                                     ResourceKeys.JOIN_TABLE_$1, (table!=null) ? table : "<aucune>");
            record.setSourceClassName("EnvironmentTable");
            record.setSourceMethodName("setSampleTable");
            SampleDataBase.LOGGER.log(record);
            if (table != null) {
                sampleTableStep = new SampleTableStep(connection, table);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getSampleTable() throws SQLException {
        return sampleTableStep!=null ? sampleTableStep.table : null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<+ParameterEntry> getAvailableParameters() throws SQLException {
        ensureTableConnected(true, false, false, ColumnTable.LIST);
        return parameterTable.list();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<OperationEntry> getAvailableOperations() throws SQLException {
        ensureTableConnected(false, true, false, ColumnTable.LIST);
        return operationTable.list();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<RelativePositionEntry> getAvailablePositions() throws SQLException {
        ensureTableConnected(false, false, true, ColumnTable.LIST);
        return positionTable.list();
    }

    /**
     * Vérifie que les tables des paramètres et des positions sont construites.
     *
     * @param  type Le type de la requête. Une des constantes {@link ColumnTable#LIST},
     *         {@link ColumnTable#BY_ID} ou {@link ColumnTable#BY_NAME}.
     */
    private void ensureTableConnected(final boolean parameter,
                                      final boolean operation,
                                      final boolean position,
                                      final int type) throws SQLException
    {
        if (parameter && parameterTable == null) {
            parameterTable = new ParameterTable(connection, type, seriesTable);
        }
        if (operation && operationTable == null) {
            operationTable = new OperationTable(connection, type);
        }
        if (position && positionTable == null) {
            positionTable = new RelativePositionTable(connection, type);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addParameter(final String  parameter,
                                          final String  operation,
                                          final String  position,
                                          final boolean nullIncluded) throws SQLException
    {
        ensureTableConnected(true, true, true, ColumnTable.BY_NAME);
        addParameter(parameterTable.getEntry(parameter),
                     operationTable.getEntry(operation),
                      positionTable.getEntry(position),
                      nullIncluded);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addParameter(final ParameterEntry       parameter,
                                          final OperationEntry       operation,
                                          final RelativePositionEntry position,
                                          final boolean nullIncluded) throws SQLException
    {
        this.nullIncluded = null;
        remove(operation, new EnvironmentTableStep(parameter, position, !nullIncluded));
        EnvironmentTableStep search = new EnvironmentTableStep(parameter, position, nullIncluded);
        EnvironmentTableStep step   = parameters.get(search);
        if (step == null) {
            step = search;
            parameters.put(step, step);
        }
        step.addColumn(operation);
    }

    /**
     * Retire une opération de l'objet <code>step</code> spécifié.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    private void remove(final OperationEntry operation, EnvironmentTableStep step) throws SQLException {
        step = parameters.get(step);
        if (step != null) {
            step.removeColumn(operation);
            if (step.isEmpty()) {
                step.close();
                parameters.remove(step);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void removeParameter(final String parameter,
                                             final String operation,
                                             final String position) throws SQLException
    {
        ensureTableConnected(true, true, true, ColumnTable.BY_NAME);
        removeParameter(parameterTable.getEntry(parameter),
                        operationTable.getEntry(operation),
                         positionTable.getEntry(position));
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void removeParameter(final ParameterEntry       parameter,
                                             final OperationEntry       operation,
                                             final RelativePositionEntry position) throws SQLException
    {
        this.nullIncluded = null;
        boolean nullIncluded=false; do {
            remove(operation, new EnvironmentTableStep(parameter, position, nullIncluded));
        }  while ((nullIncluded = !nullIncluded) == true);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int getParameterCount(final ParameterEntry       parameter,
                                              final OperationEntry       operation,
                                              final RelativePositionEntry position)
    {
        if (parameter==null && operation==null && position==null) {
            return parameters.size();
        }
        int count = 0;
        for (final EnvironmentTableStep step : parameters.values()) {
            if (parameter!=null && !parameter.equals(step.parameter)) {
                continue;
            }
            if (position!=null && !position.equals(step.position)) {
                continue;
            }
            if (operation!=null && !step.hasColumn(operation)) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized String[] getColumnLabels() throws SQLException {
        final List<String>  titles = new ArrayList<String>();
        final List<Boolean> hasNul = new ArrayList<Boolean>();
        final StringBuffer  buffer = new StringBuffer();
        titles.add("sample");
        hasNul.add(Boolean.FALSE);
        if (sampleTableStep != null) {
            final String[] columns = sampleTableStep.getColumns();
            // Skip the first column, which should be the ID.
            for (int i=1; i<columns.length; i++) {
                titles.add(columns[i]);
                hasNul.add(Boolean.FALSE);
            }
        }
        for (final EnvironmentTableStep step : parameters.values()) {
            buffer.setLength(0);
            buffer.append(step.parameter.getName());
            buffer.append(step.position.getName());
            int prefixLength = 0;
            final String[] columns = step.getColumns(true);
            for (int i=0; i<columns.length; i++) {
                final String prefix = columns[i];
                buffer.replace(0, prefixLength, prefix);
                titles.add(buffer.toString());
                hasNul.add(Boolean.valueOf(step.nullIncluded));
                prefixLength = prefix.length();
            }
        }
        nullIncluded = new boolean[hasNul.size()];
        for (int i=0; i<nullIncluded.length; i++) {
            nullIncluded[i] = hasNul.get(i).booleanValue();
        }
        return (String[])titles.toArray(new String[titles.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized RowSet getRowSet(final ProgressListener progress) throws SQLException {
        if (progress != null) {
            progress.setDescription("Initialisation");
            progress.started();
        }
        int i = (sampleTableStep!=null) ? 1 : 0;
        final ResultSet[]    results = new ResultSet[parameters.size() + i];
        final boolean[] nullIncluded = new boolean[results.length];
        final Connection  connection = this.connection;
        if (sampleTableStep != null) {
            results[0] = sampleTableStep.getResultSet();
        }
        for (final EnvironmentTableStep step : parameters.values()) {
            results[i] = step.getResultSet(connection);
            nullIncluded[i++] = step.nullIncluded;
            if (progress != null) {
                progress.progress((100f/results.length) * i);
            }
        }
        assert i == results.length;
        return new EnvironmentRowSet(results, getColumnLabels(), nullIncluded);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int print(final Writer out, int max) throws SQLException, IOException {
        final ResultSet       result = getRowSet(null);
        final ResultSetMetaData meta = result.getMetaData();
        final String   lineSeparator = System.getProperty("line.separator", "\n");
        final int        columnCount = meta.getColumnCount();
        final int[]            width = new int[columnCount];
        final boolean[]       isDate = new boolean[columnCount];
        for (int i=0; i<columnCount; i++) {
            final String title = meta.getColumnLabel(i+1);
            out.write(title);
            int length = title.length();
            final int type = meta.getColumnType(i+1);
            switch (type) {
                case Types.DATE: // Fall through
                case Types.TIME: // Fall through
                case Types.TIMESTAMP: {
                    isDate[i] = true;
                    width [i] = 8;
                    break;
                }
                default: {
                    width[i] = Math.max(i==0 ? 11 : 7, length);
                    break;
                }
            }
            if (false) {
                // Ajoute le code du type entre parenthèses.
                final String code = String.valueOf(type);
                out.write('(');
                out.write(code);
                out.write(')');
                length += (code.length() + 2);
            }
            out.write(Utilities.spaces(width[i]-length + 1));
        }
        int count = 0;
        out.write(lineSeparator);
        DateFormat dateFormat = null;
        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        while (--max>=0 && result.next()) {
            for (int i=0; i<width.length; i++) {
                final String value;
                if (i==0) {
                    final int x = result.getInt(i+1);
                    value = result.wasNull() ? "" :  String.valueOf(x);
                } else if (!isDate[i]) {
                    final double x = result.getDouble(i+1);
                    value = result.wasNull() ? "" : format.format(x);
                } else {
                    final Date x=result.getDate(i+1);
                    if (!result.wasNull()) {
                        if (dateFormat == null) {
                            dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
                        }
                        value = dateFormat.format(x);
                    } else {
                        value = "";
                    }
                }
                out.write(Utilities.spaces(width[i]-value.length()));
                out.write(value);
                out.write(' ');
            }
            out.write(lineSeparator);
            count++;
        }
        result.close();
        out.flush();
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int copyToTable(final Connection     connection,
                                        final String          tableName,
                                        final ProgressListener progress)
            throws SQLException
    {
        final ResultSet       source = getRowSet(progress);
        final ResultSetMetaData meta = source.getMetaData();
        final int        columnCount = meta.getColumnCount();
        final boolean[]       isDate = new boolean[columnCount];
        final Statement      creator;
        final ResultSet         dest;
        /*
         * Create the destination table. The table must not exists prior to this call.
         * All values (except the ID in column 0) are stored as 32 bits floating point.
         * The CREATE statement is logged for information.
         */
        if (true) {
            final StringBuffer buffer = new StringBuffer("CREATE TABLE \"");
            buffer.append(tableName);
            buffer.append("\"(\"");
            for (int i=0; i<columnCount; i++) {
                if (i!=0) {
                    buffer.append(", \"");
                }
                buffer.append(meta.getColumnName(i+1));
                buffer.append("\" ");
                if (i==0) {
                    // TODO: Ce champ devrait probablement être une clé primaire...
                    buffer.append("INTEGER");
                } else {
                    switch (meta.getColumnType(i+1)) {
                        case Types.DATE: // Fall through
                        case Types.TIME: // Fall through
                        case Types.TIMESTAMP: {
                            // TODO: On aimerait déclarer que ce champ doit être indexé (avec doublons)...
                            isDate[i] = true;
                            buffer.append("TIMESTAMP");
                            break;
                        }
                        case Types.TINYINT:     // Fall through (not strictly true, but hey,
                        case Types.SMALLINT: {  // we are fighthing against Access!!
                            // We should really uses a boolean type, but Access
                            // replace 'True' by '-1' while we really wanted '1'.
                            buffer.append("SMALLINT");
                            break;
                        }
                        default: {
                            buffer.append("REAL");
                            break;
                        }
                    }
                }
                if (!nullIncluded[i] || meta.isNullable(i+1)==ResultSetMetaData.columnNoNulls) {
                    buffer.append(" NOT NULL");
                }
            }
            buffer.append(')');
            final String sqlCreate = buffer.toString();
            creator = (connection!=null ? connection : this.connection).createStatement(
                           ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            creator.execute(sqlCreate);
            buffer.setLength(0);
            buffer.append("SELECT * FROM \"");
            buffer.append(tableName);
            buffer.append('"');
            dest = creator.executeQuery(buffer.toString());
            if (true) {
                // Log the SQL statement.
                final LogRecord record = new LogRecord(SampleDataBase.SQL_UPDATE, sqlCreate);
                record.setSourceClassName ("EnvironmentTable");
                record.setSourceMethodName("copyToTable");
                SampleDataBase.LOGGER.log(record);
            }
        }
        /*
         * Copy all values to the destination table.  The progress bar is updated
         * assuming that ID has uniformely distributed random values ranging from
         * Integer.MIN_VALUE to Integer.MAX_VALUE.
         */
        final float minID = Integer.MIN_VALUE;
        final float maxID = Integer.MAX_VALUE;
        if (progress != null) {
            progress.setDescription("Copie des données");
            progress.progress(0);
        }
        int count = 0;
        while (source.next()) {
            final int ID = source.getInt(1);
            if (progress!=null && (count & 0xFF)==0) {
                progress.progress((ID - minID) / ((maxID-minID)/100));
            }
            dest.moveToInsertRow();
            dest.updateInt(1, ID);
            for (int i=2; i<=columnCount; i++) {
                if (isDate[i-1]) {
                    dest.updateTimestamp(i, source.getTimestamp(i));
                } else {
                    final float x = source.getFloat(i);
                    if (!source.wasNull()) {
                        dest.updateFloat(i, x);
                    } else {
                        dest.updateNull(i);
                    }
                }
            }
            dest.insertRow();
            count++;
        }
        dest.close();
        source.close();
        creator.close();
        if (progress != null) {
            progress.complete();
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void set(final SampleEntry           sample,
                                 final RelativePositionEntry filter,
                                 final double[]              values)
            throws SQLException
    {
        if (values.length != getParameterCount(null, null, filter)) {
            throw new IllegalArgumentException("Le nombre de valeurs ne correspond pas.");
        }
        int index = 0;
        for (final EnvironmentTableStep step : parameters.values()) {
            if (filter!=null && !filter.equals(step.position)) {
                continue;
            }
            final int parameter = step.parameter.getID();
            final int position  = step.position.getID();
            final double value  = values[index++];
            if (Double.isNaN(value)) {
                continue;
            }
            final String[] columns = step.getColumns(false);
            for (int i=0; i<columns.length; i++) {
                final String column = columns[i];
                /*
                 * Tente la requête de mise à jour. Idéalement, cet objet EnvironmentTable
                 * ne devrait servir à mettre à jour qu'une seule colonne, ce qui signifie
                 * que l'instruction PreparedStatement ne sera construit qu'une seule fois.
                 */
                if (update!=null && !column.equals(columnUpdate)) {
                    update.close();
                    update = null;
                }
                if (update == null) {
                    update = connection.prepareStatement(replaceQuestionMark(
                             preferences.get(ENVIRONMENTS+":UPDATE", SQL_UPDATE), column));
                    columnUpdate = column;
                }
                update.setInt   (1+ARG_ID,        sample.getID());
                update.setInt   (1+ARG_PARAMETER, parameter);
                update.setInt   (1+ARG_POSITION,  position);
                update.setDouble(1,               value);
                int n = update.executeUpdate();
                if (n == 0) {
                    /*
                     * Si une ligne n'existait pas déjà pour ce paramètre, insère une nouvelle
                     * ligne. Encore une fois, cet objet EnvironmentTable ne sera habituellement
                     * utilisé que pour mettre à jour une seule colonne.
                     */
                    if (insert!=null && !column.equals(columnInsert)) {
                        insert.close();
                        insert = null;
                    }
                    if (insert == null) {
                        insert = connection.prepareStatement(replaceQuestionMark(
                                 preferences.get(ENVIRONMENTS+":INSERT", SQL_INSERT), column));
                        columnInsert = column;
                    }
                    insert.setInt   (ARG_ID,        sample.getID());
                    insert.setInt   (ARG_PARAMETER, parameter);
                    insert.setInt   (ARG_POSITION,  position);
                    insert.setDouble(ARG_VALUE,     value);
                    n = insert.executeUpdate();
                }
                if (n != 1) {
                    throw new SQLWarning(Resources.format(ResourceKeys.ERROR_UNEXPECTED_UPDATE_$1,
                                                                       new Integer(n)));
                }
            }
        }
        assert index == values.length;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void clear() throws SQLException {
        for (final EnvironmentTableStep step : parameters.values()) {
            step.close();
        }
        parameters.clear();
        nullIncluded = null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws SQLException {
        if (insert != null) {
            insert.close();
            insert = null;
        }
        if (update != null) {
            update.close();
            update = null;
        }
        if (positionTable != null) {
            positionTable.close();
            positionTable = null;
        }
        if (operationTable != null) {
            operationTable.close();
            operationTable = null;
        }
        if (parameterTable != null) {
            parameterTable.close();
            parameterTable = null;
        }
        if (sampleTableStep != null) {
            sampleTableStep.close();
            sampleTableStep = null;
        }
        clear();
        super.close();
    }
}
