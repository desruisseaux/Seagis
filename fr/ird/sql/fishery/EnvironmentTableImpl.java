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
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;

// Entrés/sorties et divers
import java.io.Writer;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Geotools
import org.geotools.resources.Utilities;

// Resources
import fr.ird.util.XArray;
import fr.ird.sql.DataBase;
import org.geotools.util.ProgressListener;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


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
final class EnvironmentTableImpl extends Table implements EnvironmentTable {
    /**
     * Instruction SQL pour mettre à jour une donnée environnementale.
     * Note: La valeur est le premier paramètre, et tous les autres sont décalés de 1.
     */
    static final String SQL_UPDATE=
                    "UPDATE "+ENVIRONMENTS+" SET [?]=? "+
                    "WHERE ID=? AND position=? AND temps=? AND paramètre=?";

    /**
     * Instruction SQL pour ajouter une donnée environnementale.
     */
    static final String SQL_INSERT=
                    "INSERT INTO "+ENVIRONMENTS+" (ID,position,temps,paramètre,[?]) "+
                    "VALUES(?,?,?,?,?)";

    /** Numéro d'argument. */ private static final int ARG_ID        = 1;
    /** Numéro d'argument. */ private static final int ARG_POSITION  = 2;
    /** Numéro d'argument. */ private static final int ARG_TIMELAG   = 3;
    /** Numéro d'argument. */ private static final int ARG_PARAMETER = 4;
    /** Numéro d'argument. */ private static final int ARG_VALUE     = 5;

    /**
     * Liste des paramètres et des opérations à prendre en compte. Les clés sont des
     * objets  {@link EnvironmentTableStep}  représentant le paramètre ainsi que sa
     * position spatio-temporelle.
     */
    private final Map<EnvironmentTableStep, EnvironmentTableStep> parameters =
                  new LinkedHashMap<EnvironmentTableStep, EnvironmentTableStep>();

    /**
     * Table des paramètres et des opérations. Cette table est construite
     * automatiquement la première fois où elle est nécessaire.
     */
    private transient ParameterTable parameterTable;

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
     * TODO: remplacer par <code>statement.getConnection()</code> si on utilise
     *       un jour le constructeur 'super(...)' avec une valeur non-nulle.
     */
    private final Connection connection;

    /**
     * Construit une table.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @throws SQLException si <code>EnvironmentTable</code> n'a pas pu construire sa requête SQL.
     */
    protected EnvironmentTableImpl(final Connection connection) throws SQLException {
        super(null);
        this.connection = connection;
    }

    /**
     * Retourne la liste des paramètres environnementaux disponibles. Les paramètres
     * environnementaux sont représentés par des noms courts tels que "CHL" ou "SST".
     *
     * @return L'ensemble des paramètres environnementaux disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public Set<String> getAvailableParameters() throws SQLException {
        return ParameterTable.list(connection, PARAMETERS);
    }

    /**
     * Retourne la liste des opérations disponibles. Les opérations sont appliquées sur
     * des paramètres environnementaux. Par exemple les opérations "valeur" et "sobel3"
     * correspondent à la valeur d'un paramètre environnemental et son gradient calculé
     * par l'opérateur de Sobel, respectivement.
     *
     * @return L'ensemble des opérations disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public Set<String> getAvailableOperations() throws SQLException {
        return ParameterTable.list(connection, OPERATIONS);
    }

    /**
     * Ajoute un paramètre à la sélection. Le paramètre sera mesuré aux coordonnées
     * spatio-temporelles exacte de la capture.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel"). Ces opérations
     *         correspondent à des noms des colonnes de la table "Environnement".
     * @param  parameter Paramètre (exemple "SST" ou "EKP").
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public void addParameter(final String operation,
                             final String parameter) throws SQLException
    {
        addParameter(operation, parameter, CENTER, 0);
    }

    /**
     * Ajoute un paramètre à la sélection. Ce paramètre sera pris en compte
     * lors du prochain appel de la méthode {@link #getRowSet}.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel"). Ces opérations
     *         correspondent à des noms des colonnes de la table "Environnement".
     *         La liste des opérations disponibles peut être obtenu avec {@link
     *         #getAvailableOperations()}.
     * @param  parameter Paramètre (exemple "SST" ou "EKP"). La liste des paramètres
     *         disponibles peut être obtenu avec {@link #getAvailableParameters()}.
     * @param  position Position position relative sur la ligne de pêche où l'on veut
     *         les valeurs. Les principales valeurs permises sont {@link #START_POINT},
     *         {@link #CENTER} et {@link #END_POINT}.
     * @param  timeLag Décalage temporel entre la capture et le paramètre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void addParameter(final String operation,
                                          final String parameter,
                                          final int    position,
                                          final int    timeLag) throws SQLException
    {
        if (parameterTable == null) {
            parameterTable = new ParameterTable(connection, ParameterTable.PARAMETER_BY_NAME);
        }
        final int           paramID = parameterTable.getParameterID(parameter);
        EnvironmentTableStep search = new EnvironmentTableStep(paramID, position, timeLag);
        EnvironmentTableStep step   = parameters.get(search);
        if (step == null) {
            step = search;
            parameters.put(step, step);
        }
        step.addColumn(operation);
    }

    /**
     * Retire un paramètre à la sélection.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel").
     * @param  parameter Paramètre (exemple "SST" ou "EKP").
     * @param  position Position position relative sur la ligne de pêche où l'on veut les valeurs.
     * @param  timeLag Décalage temporel entre la capture et le paramètre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void removeParameter(final String operation,
                                             final String parameter,
                                             final int    position,
                                             final int    timeLag) throws SQLException
    {
        if (parameterTable == null) {
            parameterTable = new ParameterTable(connection, ParameterTable.PARAMETER_BY_NAME);
        }
        final int paramID = parameterTable.getParameterID(parameter);
        EnvironmentTableStep step = new EnvironmentTableStep(paramID, position, timeLag);
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
     * Retourne les nom des colonnes pour cette table. Ces noms de colonnes sont identiques
     * à ceux que retourne <code>getRowSet(null).getMetaData().getColumnLabel(...)</code>.
     * Cette méthode permet toutefoit d'obtenir ces noms sans passer par la coûteuse création
     * d'un objet {@link RowSet}.
     *
     * @return Les noms de colonnes.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized String[] getColumnLabels() throws SQLException {
        if (parameterTable == null) {
            parameterTable = new ParameterTable(connection, ParameterTable.OPERATION_BY_NAME);
        }
        final List<String> titles = new ArrayList<String>();
        final StringBuffer buffer = new StringBuffer();
        titles.add("Capture");

        for (final Iterator<EnvironmentTableStep> it=parameters.values().iterator(); it.hasNext();) {
            final EnvironmentTableStep step = it.next();
            int t = step.timeLag;
            buffer.setLength(0);
            buffer.append(parameterTable.getParameterName(step.parameter));
            buffer.append(t<0 ? '-' : '+');
            t = Math.abs(t);
            if (t<10) {
                buffer.append(0);
            }
            buffer.append(t);
            int prefixLength = 0;
            final String[] columns = step.getColumns();
            for (int i=0; i<columns.length; i++) {
                final String prefix = parameterTable.getOperationPrefix(columns[i]);
                buffer.replace(0, prefixLength, prefix);
                titles.add(buffer.toString());
                prefixLength = prefix.length();
            }
        }
        return titles.toArray(new String[titles.size()]);
    }

    /**
     * Retourne un itérateur qui baleyera l'ensemble des données sélectionnées. La première
     * colonne du tableau {@link RowSet} contiendra le numéro identifiant les captures (ID).
     * Toutes les colonnes suivantes contiendront les paramètres environnementaux qui auront
     * été demandé par des appels de {@link #addParameter}.  Le nombre total de colonnes est
     * égal à la longueur du tableau retournée par {@link #getColumnLabels}.
     * <br><br>
     * Note: <strong>Chaque objet <code>EnvironmentTable</code> ne mantient qu'un seul objet
     *       <code>RowSet</code> à la fois.</strong>  Si cette méthode est appelée plusieurs
     *       fois, alors chaque nouvel appel fermera le {@link RowSet} de l'appel précédent.
     *
     * @param  progress Objet à utiliser pour informer des progrès de l'initialisation, ou
     *         <code>null</code> si aucun. Cette méthode appelle {@link ProgressListener#started},
     *         mais n'appelle <strong>pas</strong> {@link ProgressListener#complete} étant donné
     *         qu'on voudra probablement continuer à l'utiliser pour informer des progrès
     *         de la lecture du {@link RowSet}.
     * @return Les données environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public synchronized RowSet getRowSet(final ProgressListener progress) throws SQLException {
        if (progress != null) {
            progress.setDescription("Initialisation");
            progress.started();
        }
        int i=0;
        final ResultSet[]   results = new ResultSet[parameters.size()];
        final Connection connection = this.connection;
        for (final Iterator<EnvironmentTableStep> it=parameters.values().iterator(); it.hasNext();) {
            EnvironmentTableStep step = it.next();
            results[i++] = step.getResultSet(connection);
            if (progress != null) {
                progress.progress((100f/results.length) * i);
            }
        }
        assert i == results.length;
        return new EnvironmentRowSet(results, getColumnLabels());
    }

    /**
     * Affiche les enregistrements vers le flot spécifié.
     * Cette méthode est surtout utile à des fins de vérification.
     *
     * @param  out Flot de sortie.
     * @param  max Nombre maximal d'enregistrements à écrire.
     * @return Le nombre d'enregistrement écrits.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public synchronized int print(final Writer out, int max) throws SQLException, IOException {
        final ResultSet       result = getRowSet(null);
        final ResultSetMetaData meta = result.getMetaData();
        final String   lineSeparator = System.getProperty("line.separator", "\n");
        final int        columnCount = meta.getColumnCount();
        final int[]            width = new int[columnCount];
        for (int i=0; i<columnCount; i++) {
            final String title = meta.getColumnLabel(i+1);
            out.write(title);
            int length = title.length();
            width[i] = Math.max(i==0 ? 11 : 7, length);
            out.write(Utilities.spaces(width[i]-length + 1));
        }
        int count = 0;
        out.write(lineSeparator);
        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
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
            count++;
        }
        result.close();
        out.flush();
        return count;
    }

    /**
     * Copie toutes les données de {@link #getRowSet} vers une table du nom
     * spécifiée. Aucune table ne doit exister sous ce nom avant l'appel de
     * cette méthode. Cette méthode construira elle-même la table nécessaire.
     *
     * @param  tableName Nom de la table à créer.
     * @param  progress Objet à utiliser pour informer des progrès, ou <code>null</code> si aucun.
     * @return Le nombre d'enregistrement copiés dans la nouvelle table.
     * @throws Si un problème est survenu lors des accès aux bases de données.
     */
    public synchronized int copyToTable(final String tableName, final ProgressListener progress)
            throws SQLException
    {
        final ResultSet       source = getRowSet(progress);
        final ResultSetMetaData meta = source.getMetaData();
        final int        columnCount = meta.getColumnCount();
        final Statement      creator;
        final ResultSet         dest;
        /*
         * Create the destination table. The table must not exists prior to this call.
         * All values (except the ID in column 0) are stored as 32 bits floating point.
         * The CREATE statement is logged for information.
         */
        if (true) {
            final StringBuffer buffer = new StringBuffer("CREATE TABLE ");
            buffer.append(tableName);
            buffer.append('(');
            for (int i=0; i<columnCount; i++) {
                if (i!=0) {
                    buffer.append(',');
                }
                buffer.append(meta.getColumnName(i+1));
                buffer.append(' ');
                buffer.append((i==0) ? "INTEGER" : "REAL");
                buffer.append(" NOT NULL");
            }
            buffer.append(')');
            final String sqlCreate = buffer.toString();
            creator = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                 ResultSet.CONCUR_UPDATABLE);
            creator.execute(sqlCreate);
            buffer.setLength(0);
            buffer.append("SELECT * FROM ");
            buffer.append(tableName);
            dest = creator.executeQuery(buffer.toString());
            if (true) {
                // Log the SQL statement.
                final LogRecord record = new LogRecord(DataBase.SQL_UPDATE, sqlCreate);
                record.setSourceClassName ("EnvironmentTable");
                record.setSourceMethodName("copyToTable");
                logger.log(record);
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
                dest.updateFloat(i, source.getFloat(i));
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
     * Définit la valeur des paramètres environnementaux pour une capture. Cette méthode
     * affecte la valeur de chacune des colonnes qui ont été ajoutées avec {@link #addPatameter}.
     *
     * @param  capture La capture pour laquelle on veut définir les valeurs des paramètres
     *         environnementaux.
     * @param  value Les valeurs des paramètres environnementaux. Ce tableau doit avoir
     *         la même longueur que le nombre de paramètres ajoutés avec la méthode
     *         {@link #addParameter} (c'est-à-dire la longueur de {@link #getColumnLabels}
     *         moins 1). Les valeurs <code>NaN</code> seront ignorées (c'est-à-dire que les
     *         valeurs déjà présentes dans la base de données ne seront pas écrasées).
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public void set(final CatchEntry capture, final float[] values) throws SQLException {
        set(capture, Integer.MIN_VALUE, null, values);
    }

    /**
     * Définit la valeur des paramètres environnementaux pour une capture. Cette méthode fonctionne
     * comme {@link #set(CatchEntry, float[]),  excepté qu'elle permet de spécifier des coordonnées
     * spatio-temporelles différentes de celles qui avaient été spécifiées avec {@link #addPatameter}.
     *
     * @param  capture La capture pour laquelle on veut définir les valeurs des paramètres
     *         environnementaux.
     * @param  position Position relative de la valeur <code>value</code>. Si la capture est
     *         représentée par un seul point (c'est-à-dire si {@link CatchEntry#getShape}
     *         retourne <code>null</code>), alors cette méthode met à jour la l'enregistrement
     *         correspondant à la position {@link #CENTER}, quelle que soit la valeur de cet
     *         argument <code>position</code>.
     * @param  time La date à laquelle a été évaluée la valeur <code>value</code>.
     *         Si cet argument est non-nul, alors l'écart de temps entre cette date
     *         et la date de la capture sera calculée et utilisé à la place de la valeur
     *         spécifiée lors des appels de {@link #addParameter}.
     * @param  value Les valeurs des paramètres environnementaux. Ce tableau doit avoir
     *         la même longueur que le nombre de paramètres ajoutés avec la méthode
     *         {@link #addParameter} (c'est-à-dire la longueur de {@link #getColumnLabels}
     *         moins 1). Les valeurs <code>NaN</code> seront ignorées (c'est-à-dire que les
     *         valeurs déjà présentes dans la base de données ne seront pas écrasées).
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public synchronized void set(final CatchEntry capture,
                                 final int        relativePosition,
                                 final Date       valueTime,
                                 final float[]    values) throws SQLException
    {
        if (values.length != parameters.size()) {
            throw new IllegalArgumentException("Trop peu de valeurs.");
        }
        int index = 0;
        for (final Iterator<EnvironmentTableStep> it=parameters.values().iterator(); it.hasNext();) {
            EnvironmentTableStep step = it.next();
            int parameter = step.parameter;
            int position  = step.position;
            int timeLag   = step.timeLag;
            final float value = values[index++];
            if (Float.isNaN(value)) {
                continue;
            }
            /*
             * Ajustement de la date:
             *     Les dates de la base de données ne contiennent pas d'heure.
             *     La date du "21/01/1999 00:00" peut très bien signifier que
             *     la ligne a été mouillée à 18h00. Dans ce cas, une image datée
             *     du "20/01/01/1999 23:00" est effectivement une image de la veille.
             */
            final Date catchTime = capture.getTime();
            if (catchTime!=null && valueTime!=null) {
                timeLag = (int)Math.floor((valueTime.getTime()-catchTime.getTime()) / (24.0*60*60*1000));
            }
            /*
             * Ajustement de la position:
             *     Les lignes de palangres couvrent une grande distance (plusieurs miles
             *     nautique). Mais les données de senneurs ne sont notés qu'en un point.
             *     Les positions de ces derniers seront toujours ramenées à CENTRE.
             */
            if (relativePosition != Integer.MIN_VALUE) {
                position = relativePosition;
            }
            if (capture instanceof AbstractCatchEntry) {
                position = ((AbstractCatchEntry) capture).clampPosition(position);
            }
            final String[] columns = step.getColumns();
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
                update.setInt   (1+ARG_ID,        capture.getID());
                update.setInt   (1+ARG_PARAMETER, parameter);
                update.setInt   (1+ARG_POSITION,  position);
                update.setInt   (1+ARG_TIMELAG,   timeLag);
                update.setDouble(1,               value); // Note: Should be 'float', but Access doesn't like.
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
                    insert.setInt   (ARG_ID,        capture.getID());
                    insert.setInt   (ARG_PARAMETER, parameter);
                    insert.setInt   (ARG_POSITION,  position);
                    insert.setInt   (ARG_TIMELAG,   timeLag);
                    insert.setDouble(ARG_VALUE,     value); // Note: Should be 'float', but Access doesn't like.
                    n = insert.executeUpdate();
                }
                if (n != 1) {
                    throw new SQLWarning(Resources.format(ResourceKeys.ERROR_UNEXPECTED_UPDATE_$1, new Integer(n)));
                }
            }
        }
        assert index == values.length;
    }

    /**
     * Oublie tous les paramètres qui ont été déclarés avec
     * {@link #addParameter(String,String,int,int)}.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void clear() throws SQLException {
        for (final Iterator<EnvironmentTableStep> it=parameters.values().iterator(); it.hasNext();) {
            it.next().close();
        }
        parameters.clear();
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
        if (insert != null) {
            insert.close();
            insert = null;
        }
        if (update != null) {
            update.close();
            update = null;
        }
        if (parameterTable != null) {
            parameterTable.close();
            parameterTable = null;
        }
        clear();
        super.close();
    }
}
