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
import javax.sql.RowSet;

// Collections
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;

// Entr�s/sorties et divers
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
     * Liste des param�tres et des op�rations � prendre en compte. Les cl�s sont des
     * objets  {@link EnvironmentTableStep}  repr�sentant le param�tre ainsi que sa
     * position spatio-temporelle.
     */
    private final Map<EnvironmentTableStep, EnvironmentTableStep> parameters =
                  new LinkedHashMap<EnvironmentTableStep, EnvironmentTableStep>();

    /**
     * Table des param�tres et des op�rations. Cette table est construite
     * automatiquement la premi�re fois o� elle est n�cessaire.
     */
    private transient ParameterTable parameterTable;

    /**
     * La connection vers la base de donn�es.
     * TODO: remplacer par <code>statement.getConnection()</code> si on utilise
     *       un jour le constructeur 'super(...)' avec une valeur non-nulle.
     */
    private final Connection connection;

    /**
     * Construit une table.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @throws SQLException si <code>CouplingTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected CouplingTableImpl(final Connection connection) throws SQLException {
        super(null);
        this.connection = connection;
    }

    /**
     * Oublie tous les param�tres qui ont �t� d�clar�s avec {@link #addParameter}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public synchronized void clear() throws SQLException {
        for (final Iterator<EnvironmentTableStep> it=parameters.values().iterator(); it.hasNext();) {
            it.next().close();
        }
        parameters.clear();
    }

    /**
     * Ajoute un param�tre � la s�lection. Ce param�tre sera pris en compte
     * lors du prochain appel de la m�thode {@link #getRowSet}.
     *
     * @param  operation Op�ration (exemple "valeur" ou "sobel"). Ces op�rations
     *         correspondent � des noms des colonnes de la table "Environnement".
     *         La liste des op�rations disponibles peut �tre obtenu avec {@link
     *         #getAvailableOperations()}.
     * @param  parameter Param�tre (exemple "SST" ou "EKP"). La liste des param�tres
     *         disponibles peut �tre obtenu avec {@link #getAvailableParameters()}.
     * @param  position Position position relative sur la ligne de p�che o� l'on veut
     *         les valeurs. Les principales valeurs permises sont {@link #START_POINT},
     *         {@link #CENTER} et {@link #END_POINT}.
     * @param  timeLag D�calage temporel entre la capture et le param�tre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * Retire un param�tre � la s�lection.
     *
     * @param  operation Op�ration (exemple "valeur" ou "sobel").
     * @param  parameter Param�tre (exemple "SST" ou "EKP").
     * @param  position Position position relative sur la ligne de p�che o� l'on veut
     *         les valeurs.
     * @param  timeLag D�calage temporel entre la capture et le param�tre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * � ceux que retourne <code>getRowSet(null).getMetaData().getColumnLabel(...)</code>.
     * Cette m�thode permet toutefoit d'obtenir ces noms sans passer par la co�teuse cr�ation
     * d'un objet {@link RowSet}.
     *
     * @return Les noms de colonnes.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
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
     * Retourne toutes les donn�es. La premi�re colonne du {@link ResultSet} retourn�
     * contiendra le num�ro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les param�tres environnementaux. Le nombre total de colonnes est
     * �gal � la longueur de la liste retourn�e par {@link #getColumnTitles}.
     * <br><br>
     * Note: si cette m�thode est appel�e plusieurs fois, alors chaque nouvel
     *       appel fermera le {@link ResultSet} de l'appel pr�c�dent.
     *
     * @param  progress Objet � utiliser pour informer des progr�s de l'initialisation,
     *         ou <code>null</code> si aucun.
     * @return Les donn�es environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
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
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
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
