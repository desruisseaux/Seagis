/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.database.coverage.sql;

// J2SE
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;

// Geotools
import org.geotools.cv.SampleDimension;
import org.geotools.gui.swing.tree.TreeNode;
import org.geotools.gui.swing.tree.MutableTreeNode;
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// Seagis
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.database.IllegalRecordException;


/**
 * Connection vers une table des séries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les paramètres et opérations qui forment les séries, et de placer ces informations dans une
 * arborescence avec des chemins de la forme "<code>paramètre/opération/série</code>".
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SeriesTable extends Table implements fr.ird.database.coverage.SeriesTable {
    /**
     * Argument pour {@link #getEntry}.
     */
    private static final int FORMAT_LEAF = 6;

    /**
     * Requête SQL utilisée par cette classe pour obtenir une série à partir de son nom.
     *
     * @task TODO: Pourrait être dérivée de la requête suivante (SQL_SELECT_BY_ID).
     */
    static final String SQL_SELECT = configuration.get(Configuration.KEY_SERIES_NAME);
    // static final String SQL_SELECT =
    //    "SELECT ID, name, description, period FROM "+SERIES+" WHERE name LIKE ?";

    /**
     * Requête SQL utilisée par cette classe pour obtenir une série à partir de son numéro ID.
     */
    static final String SQL_SELECT_BY_ID = configuration.get(Configuration.KEY_SERIES_ID);    
    //static final String SQL_SELECT_BY_ID =
    //    "SELECT ID, name, description, period FROM "+SERIES+" WHERE ID=?";

    /**
     * Requête SQL utilisée par cette classe pour obtenir les sous-séries d'une série.
     */
    static final String SQL_SELECT_SUBSERIES = configuration.get(Configuration.KEY_SERIES_SUBSERIES);
    //static final String SQL_SELECT_SUBSERIES =
    //    "SELECT ID, name, description, format FROM "+SUBSERIES+" WHERE series=?";

    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des séries.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par
     * les constantes {@link #SERIES_ID}, {@link #SERIES_NAME} et compagnie.
     */
    static final String SQL_TREE = configuration.get(Configuration.KEY_SERIES_TREE);
    // static final String SQL_TREE =
    //        "SELECT "+  /*[01] SUBSERIES_ID      */  SUBSERIES+".ID, "          +
    //                    /*[02] SUBSERIES_NAME    */  SUBSERIES+".name, "        +
    //                    /*[03] SUBSERIES_REMARKS */  SUBSERIES+".description, " +
    //                    /*[04] SERIES_ID         */     SERIES+".ID, "          +
    //                    /*[05] SERIES_NAME       */     SERIES+".name, "        +
    //                    /*[06] SERIES_REMARKS    */     SERIES+".description, " +
    //                    /*[07] OPERATION_ID      */ OPERATIONS+".ID, "          +
    //                    /*[08] OPERATION_NAME    */ OPERATIONS+".name, "        +
    //                    /*[09] OPERATION_REMARKS */ OPERATIONS+".description, " +
    //                    /*[10] PARAMETER_ID      */ PARAMETERS+".ID, "          +
    //                    /*[11] PARAMETER_NAME    */ PARAMETERS+".name, "        +
    //                    /*[12] PARAMETER_REMARKS */ PARAMETERS+".description, " +
    //                    /*[13] FORMAT            */  SUBSERIES+".format,"       +
    //                    /*[14] PERIOD            */     SERIES+".period\n"      +
    //        "FROM ["  + PARAMETERS +"], " + // Note: les [  ] sont nécessaires pour Access.
    //                    OPERATIONS + ", " +
    //                    SERIES     + ", " +
    //                    SUBSERIES  + "\n" +
    //        "WHERE "  + PARAMETERS + ".ID=parameter AND " +
    //                    OPERATIONS + ".ID=operation AND " +
    //                    SERIES     + ".ID=series    AND " +
    //                                     "visible=TRUE\n" +
    //        "ORDER BY "+PARAMETERS+".name, "+OPERATIONS+".name, "+SERIES+".name";


    /** Numéro de colonne. */ private static final int SUBSERIES_ID      =  1;
    /** Numéro de colonne. */ private static final int SUBSERIES_NAME    =  2;
    /** Numéro de colonne. */ private static final int SUBSERIES_REMARKS =  3;
    /** Numéro de colonne. */ private static final int SERIES_ID         =  4;
    /** Numéro de colonne. */ private static final int SERIES_NAME       =  5;
    /** Numéro de colonne. */ private static final int SERIES_REMARKS    =  6;
    /** Numéro de colonne. */ private static final int OPERATION_ID      =  7;
    /** Numéro de colonne. */ private static final int OPERATION_NAME    =  8;
    /** Numéro de colonne. */ private static final int OPERATION_REMARKS =  9;
    /** Numéro de colonne. */ private static final int PARAMETER_ID      = 10;
    /** Numéro de colonne. */ private static final int PARAMETER_NAME    = 11;
    /** Numéro de colonne. */ private static final int PARAMETER_REMARKS = 12;
    /** Numéro de colonne. */ private static final int FORMAT            = 13;
    /** Numéro de colonne. */ private static final int PERIOD            = 14;

    /** Numéro d'argument. */ private static final int ARG_ID     = 1;
    /** Numéro d'argument. */ private static final int ARG_NAME   = 1;

    /**
     * Représente une branche de l'arborescence. Cette
     * classe interne est utilisée par {@link #getTree}.
     */
    private static final class Branch {
        /** Nom de la table.                */ final String table;
        /** Colonne du champ ID.            */ final int    ID;
        /** Colonne du champ 'name'.        */ final int    name;
        /** Colonne du champ 'description'. */ final int    remarks;

        /** Construit une branche. */
        Branch(String table, int ID, int name, int remarks) {
            this.table   = table;
            this.ID      = ID;
            this.name    = name;
            this.remarks = remarks;
        }
    }

    /**
     * Liste des branches à inclure dans l'arborescence, dans l'ordre.
     * Cette liste est utilisée par {@link #getTree} pour construire
     * l'arborescence.
     */
    private static final Branch[] TREE_STRUCTURE = new Branch[] {
        new Branch(PARAMETERS, PARAMETER_ID, PARAMETER_NAME, PARAMETER_REMARKS),
        new Branch(OPERATIONS, OPERATION_ID, OPERATION_NAME, OPERATION_REMARKS),
        new Branch(    SERIES,    SERIES_ID,    SERIES_NAME,    SERIES_REMARKS),
        new Branch(SUBSERIES,  SUBSERIES_ID, SUBSERIES_NAME, SUBSERIES_REMARKS)
    };

    /**
     * Connection avec la base de données.
     */
    private final Connection connection;

    /**
     * Requète SQL retournant une série à partir de son nom.
     */
    private transient PreparedStatement selectByName;

    /**
     * Requète SQL retournant une série à partir de son numéro ID.
     */
    private transient PreparedStatement selectByID;

    /**
     * Requête SQL retournant les sous-séries d'une série.
     */
    private transient PreparedStatement selectSubSeries;

    /**
     * Requête utilisée pour compter le nombre d'images appartenant à une série.
     * Cette requête ne sera construite que lorsqu'elle sera nécessaire.
     */
    private transient PreparedStatement count;

    /**
     * La table des formats. Ne sera créée que lorsqu'elle sera nécessaire.
     */
    private transient FormatTable formats;

    /**
     * Construit un objet en utilisant la connection spécifiée.
     *
     * @param connection Connection vers une base de données d'images.
     * @throws SQLException si <code>SeriesTable</code> n'a pas pu construire sa requête SQL.
     */
    protected SeriesTable(final Connection connection) throws SQLException {
        this.connection = connection;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized SeriesEntry getEntry(final int ID) throws SQLException {
        if (selectByID == null) {
            selectByID = connection.prepareStatement(SQL_SELECT_BY_ID);
        }
        selectByID.setInt(ARG_ID, ID);
        return getEntry(selectByID);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized SeriesEntry getEntry(final String name) throws SQLException {
        if (selectByName == null) {
            selectByName = connection.prepareStatement(SQL_SELECT);
        }
        selectByName.setString(ARG_NAME, name);
        return getEntry(selectByName);
    }

    /**
     * Retourne une référence vers un enregistrement de la table des séries.
     *
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     * @throws IllegalRecordException Si plusieurs séries portent le même nom ou même ID.
     */
    private SeriesEntry getEntry(final PreparedStatement statement) throws SQLException {
        final ResultSet resultSet = statement.executeQuery();
        SeriesEntry entry = null;
        while (resultSet.next()) {
            int    ID      = resultSet.getInt   (1);
            String name    = resultSet.getString(2);
            String remarks = resultSet.getString(3);
            double  period = resultSet.getDouble(4); if (resultSet.wasNull()) period=Double.NaN;
            final SeriesEntry candidate = new SeriesEntry(SERIES, name, ID, remarks, period);
            if (true) {
                /*
                 * Recherche les sous-séries (note: si ce bloc est retiré,
                 * alors la méthode peut être statique).
                 */
                if (selectSubSeries == null) {
                    System.out.println(SQL_SELECT_SUBSERIES);
                    selectSubSeries = connection.prepareStatement(SQL_SELECT_SUBSERIES);
                }
                selectSubSeries.setInt(1, ID);
                final List<Entry> subseries = new ArrayList<Entry>();
                final ResultSet subResults = selectSubSeries.executeQuery();
                while (subResults.next()) {
                    ID      = subResults.getInt   (1);
                    name    = subResults.getString(2);
                    remarks = subResults.getString(3);
                    subseries.add(new Entry(SUBSERIES, name, ID, remarks));
                }
                subResults.close();
                candidate.subseries = subseries.toArray(new Entry[subseries.size()]);
            }
            if (entry == null) {
                entry = candidate;
            } else if (!entry.equals(candidate)) {
                throw new IllegalRecordException(SERIES, Resources.format(
                            ResourceKeys.ERROR_DUPLICATED_SERIES_$1, candidate.getName()));
            }
        }
        resultSet.close();
        return entry;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<fr.ird.database.coverage.SeriesEntry> getEntries() throws SQLException {
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(SQL_TREE);
        final Set<fr.ird.database.coverage.SeriesEntry> set;
        set = new LinkedHashSet<fr.ird.database.coverage.SeriesEntry>();
        while (resultSet.next()) {
            int            ID = resultSet.getInt   (SERIES_ID);
            String       name = resultSet.getString(SERIES_NAME);
            String    remarks = resultSet.getString(SERIES_REMARKS);
            double     period = resultSet.getDouble(PERIOD); if (resultSet.wasNull()) period=Double.NaN;
            final SeriesEntry entry = new SeriesEntry(SERIES, name, ID, remarks, period);
            set.add(entry);
        }
        resultSet.close();
        statement.close();
        return set;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized TreeModel getTree(final int leafType) throws SQLException {
        final Locale       locale = null;
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(SQL_TREE);
        final int     branchCount = Math.min(TREE_STRUCTURE.length, leafType);
        final int[]           ids = new int   [branchCount];
        final String[]      names = new String[branchCount];
        final String[]    remarks = new String[branchCount];
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                Resources.getResources(locale).getString(ResourceKeys.SERIES));
        /*
         * Balaye la liste de tous les groupes, et place ces groupes
         * dans une arborescence au fur et à mesure qu'ils sont trouvés.
         */
        while (resultSet.next()) {
            /*
             * Mémorise les numéro ID et les noms de toutes les entrées
             * trouvées dans l'enregistrement courant.  Ca comprend les
             * noms et ID des groupes, séries, opérations et paramètres.
             */
            for (int i=branchCount; --i>=0;) {
                final Branch branch = TREE_STRUCTURE[i];
                ids    [i] = resultSet.getInt   (branch.ID     );
                names  [i] = resultSet.getString(branch.name   );
                remarks[i] = resultSet.getString(branch.remarks);
            }
            final int format = resultSet.getInt(FORMAT);
            double period = resultSet.getDouble(PERIOD);
            if (resultSet.wasNull()) {
                period = Double.NaN;
            }
            DefaultMutableTreeNode branch=root;
      scan: for (int i=0; i<branchCount; i++) {
                /*
                 * Vérifie s'il existe déjà une branche pour le paramètre,
                 * opération où la série de l'enregistrement courant.  Si
                 * une de ces branches n'existe pas, elle sera créée au
                 * passage.
                 */
                final int ID=ids[i];
                for (int j=branch.getChildCount(); --j>=0;) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) branch.getChildAt(j);
                    final Entry reference = (Entry) node.getUserObject();
                    if (reference.ID == ID) {
                        branch=node;
                        continue scan;
                    }
                }
                final String tableName = TREE_STRUCTURE[i].table;
                final Entry ref;
                if (tableName == SERIES) {
                    ref = new SeriesEntry(tableName, names[i], ID, remarks[i], period);
                } else {
                    ref = new Entry(tableName, names[i], ID, remarks[i]);
                }
                /*
                 * Construit le noeud. Si les catégories  ont
                 * été demandées, elles seront ajoutées après
                 * le dernier noeud qui est du ressort de cet
                 * objet <code>SeriesTable</code>.
                 */
                final boolean hasMoreBranchs = i<(branchCount-1);
                final DefaultMutableTreeNode node;
                switch (leafType) {
                    case SERIES_LEAF: // Fall through
                    case SUBSERIES_LEAF: {
                        node = new DefaultMutableTreeNode(ref, hasMoreBranchs);
                        break;
                    }
                    case CATEGORY_LEAF: {
                        node = new DefaultMutableTreeNode(ref, true);
                        if (!hasMoreBranchs) {
                            if (formats == null) {
                                formats = new FormatTable(connection);
                            }
                            node.add(formats.getEntry(format).getTree(locale));
                        }
                        break;
                    }
                    default: throw new IllegalArgumentException(String.valueOf(leafType));
                }
                branch.add(node);
                branch = node;
            }
        }
        resultSet.close();
        statement.close();
        return new DefaultTreeModel(root, true);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized FormatEntry getFormat(final fr.ird.database.coverage.SeriesEntry series)
            throws SQLException
    {
        if (selectSubSeries == null) {
            selectSubSeries = connection.prepareStatement(SQL_SELECT_SUBSERIES);
        }
        selectSubSeries.setInt(1, series.getID());
        final ResultSet results = selectSubSeries.executeQuery();
        FormatEntry result = null;
        while (results.next()) {
            final FormatEntry candidate;
            final int formatID = results.getInt(4);
            if (formats == null) {
                formats = new FormatTable(connection);
            }
            candidate = formats.getEntry(formatID);
            if (result == null) {
                result = candidate;
            } else if (!result.equals(candidate)) {
                throw new IllegalRecordException(SUBSERIES,
                          Resources.format(ResourceKeys.ERROR_DUPLICATED_RECORD_$1, series.getName()));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws SQLException {
        if (selectByName    != null) {selectByName   .close(); selectByName    = null;}
        if (selectByID      != null) {selectByID     .close(); selectByID      = null;}
        if (selectSubSeries != null) {selectSubSeries.close(); selectSubSeries = null;}
        if (formats         != null) {formats        .close(); formats         = null;}
    }
}
