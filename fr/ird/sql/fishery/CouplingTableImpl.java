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
import javax.sql.RowSet;

// Collections
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;

// Entrés/sorties et divers
import java.io.Writer;
import java.io.IOException;
import java.text.NumberFormat;

// Geotools
import org.geotools.resources.Utilities;

// Resources
import fr.ird.util.XArray;
import fr.ird.awt.progress.Progress;
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
     * La connection vers la base de données.
     * TODO: remplacer par <code>statement.getConnection()</code> si on utilise
     *       un jour le constructeur 'super(...)' avec une valeur non-nulle.
     */
    private final Connection connection;

    /**
     * Construit une table.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @throws SQLException si <code>CouplingTable</code> n'a pas pu construire sa requête SQL.
     */
    protected CouplingTableImpl(final Connection connection) throws SQLException {
        super(null);
        this.connection = connection;
    }

    /**
     * Oublie tous les paramètres qui ont été déclarés avec {@link #addParameter}.
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
     * @param  position Position position relative sur la ligne de pêche où l'on veut
     *         les valeurs.
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
     * Retourne toutes les données. La première colonne du {@link ResultSet} retourné
     * contiendra le numéro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les paramètres environnementaux. Le nombre total de colonnes est
     * égal à la longueur de la liste retournée par {@link #getColumnTitles}.
     * <br><br>
     * Note: si cette méthode est appelée plusieurs fois, alors chaque nouvel
     *       appel fermera le {@link ResultSet} de l'appel précédent.
     *
     * @param  progress Objet à utiliser pour informer des progrès de l'initialisation,
     *         ou <code>null</code> si aucun.
     * @return Les données environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public synchronized RowSet getRowSet(final Progress progress) throws SQLException {
        if (progress != null) {
            progress.setDescription("Initialisation");
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
     * @param out Flot de sortie.
     * @param max Nombre maximal d'enregistrements à écrire.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public synchronized void print(final Writer out, int max) throws SQLException, IOException {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final String[] titles = getColumnLabels();
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
        final ResultSet result = getRowSet(null);
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
        if (parameterTable != null) {
            parameterTable.close();
            parameterTable = null;
        }
        clear();
        super.close();
    }
}
