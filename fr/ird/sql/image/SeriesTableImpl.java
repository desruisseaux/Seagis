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
package fr.ird.sql.image;

// Base de données
import fr.ird.sql.Entry;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Arborescence
import net.seas.awt.tree.TreeNode;
import net.seas.awt.tree.MutableTreeNode;
import net.seas.awt.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

// Ensembles
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

// Divers et resources
import java.util.Locale;
import java.io.Serializable;
import net.seagis.cv.CategoryList;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Connection vers une table des séries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les paramètres et opérations qui forment les séries, et de placer ces informations dans une arborescence
 * avec des chemins de la forme "<code>paramètre/opération/série/groupe</code>".
 *
 * @see ImageDataBase#getSeriesTable
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class SeriesTableImpl extends Table implements SeriesTable
{
    /**
     * Requête SQL utilisée par cette classe pour obtenir une série à partir de son nom.
     */
    static final String SQL_SELECT = "SELECT ID, name FROM "+SERIES+" WHERE name LIKE ?";

    /**
     * Requête SQL utilisée par cette classe pour obtenir une série à partir de son numéro ID.
     */
    static final String SQL_SELECT_BY_ID = "SELECT ID, name FROM "+SERIES+" WHERE ID=?";

    /**
     * Requête SQL pour compter le nombre
     * d'images appartenant à une série.
     */
    static final String SQL_COUNT = "SELECT Count("+IMAGES+".ID) "+
                                    "FROM "+IMAGES+" INNER JOIN "+GROUPS+" ON "+IMAGES+".groupe="+GROUPS+".ID\n"+
                                    "WHERE ("+GROUPS+".visible="+TRUE+") AND (series=?)\nGROUP BY "+IMAGES+".ID";

    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des séries.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par
     * les constantes [@link #GROUPS_ID}, [@link #GROUPS_NAME} et compagnie.
     */
    static final String SQL_TREE = "SELECT "+  /*[01] GROUPS_ID      */     GROUPS+".ID, "        +
                                               /*[02] GROUPS_NAME    */     GROUPS+".name, "      +
                                               /*[03] SERIES_ID      */     GROUPS+".series, "    +
                                               /*[04] SERIES_NAME    */     SERIES+".name, "      +
                                               /*[05] OPERATION_ID   */     SERIES+".operation, " +
                                               /*[06] OPERATION_NAME */ OPERATIONS+".name, "      +
                                               /*[07] PARAMETER_ID   */     SERIES+".parameter, " +
                                               /*[08] PARAMETER_NAME */ PARAMETERS+".name, "      +
                                               /*[09] GROUPS_FORMAT  */     GROUPS+".format\n"    +
                                   "FROM ["  + PARAMETERS +"], " + // Note: les [  ] sont nécessaires pour Access.
                                               OPERATIONS + ", " +
                                               SERIES     + ", " +
                                               GROUPS     + "\n" +
                                   "WHERE "  + PARAMETERS + ".ID=" + SERIES     + ".parameter AND " +
                                               OPERATIONS + ".ID=" + SERIES     + ".operation AND " +
                                               SERIES     + ".ID=" + GROUPS     + ".series    AND " +
                                               GROUPS     + ".visible=" + TRUE  + "\n"              +
                                   "ORDER BY "+PARAMETERS+".name, "+OPERATIONS+".name, "+SERIES+".name, "+GROUPS+".name";


    /** Numéro de colonne. */ private static final int GROUPS_ID      = 1;
    /** Numéro de colonne. */ private static final int GROUPS_NAME    = 2;
    /** Numéro de colonne. */ private static final int SERIES_ID      = 3;
    /** Numéro de colonne. */ private static final int SERIES_NAME    = 4;
    /** Numéro de colonne. */ private static final int OPERATION_ID   = 5;
    /** Numéro de colonne. */ private static final int OPERATION_NAME = 6;
    /** Numéro de colonne. */ private static final int PARAMETER_ID   = 7;
    /** Numéro de colonne. */ private static final int PARAMETER_NAME = 8;
    /** Numéro de colonne. */ private static final int GROUPS_FORMAT  = 9;

    /** Numéro d'argument. */ private static final int ARG_ID     = 1;
    /** Numéro d'argument. */ private static final int ARG_NAME   = 1;

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
     * Requête utilisée pour compter le nombre d'images appartenant à une série.
     * Cette requête ne sera construite que lorsqu'elle sera nécessaire.
     */
    private transient PreparedStatement count;

    /**
     * Table des formats. Ce champ ne sera construit
     * que la première fois où il sera nécessaire.
     */
    private transient FormatTable formats;

    /**
     * Construit un objet en utilisant la connection spécifiée.
     *
     * @param connection Connection vers une base de données d'images.
     * @throws SQLException si <code>SeriesTable</code> n'a pas pu construire sa requête SQL.
     */
    protected SeriesTableImpl(final Connection connection) throws SQLException
    {this.connection = connection;}

    /**
     * Retourne une référence vers un enregistrement de la table des séries.
     *
     * @param  ID Numéro identifiant la série recherchée.
     * @return La série identifiée par le numéro ID, ou <code>null</code>
     *         si aucune série de ce numéro n'a été trouvée.
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     * @throws IllegalRecordException Si plusieurs séries portent le même ID.
     */
    public synchronized SeriesEntry getSeries(final int ID) throws SQLException
    {
        if (selectByID==null)
        {
            selectByID = connection.prepareStatement(preferences.get("SERIES_BY_ID", SQL_SELECT_BY_ID));
        }
        selectByID.setInt(ARG_ID, ID);
        return getSeries(selectByID);
    }

    /**
     * Retourne une référence vers un enregistrement de la table des séries.
     *
     * @param  name Nom de la série recherchée.
     * @return Une série qui porte le nom <code>name</code>, ou <code>null</code>
     *         si aucune série de ce nom n'a été trouvée.
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     * @throws IllegalRecordException Si plusieurs séries portent le même nom.
     */
    public synchronized SeriesEntry getSeries(final String name) throws SQLException
    {
        if (selectByName==null)
        {
            selectByName = connection.prepareStatement(preferences.get(SERIES, SQL_SELECT));
        }
        selectByName.setString(ARG_NAME, name);
        return getSeries(selectByName);
    }

    /**
     * Retourne une référence vers un enregistrement de la table des séries.
     */
    private static SeriesEntry getSeries(final PreparedStatement statement) throws SQLException
    {
        final ResultSet resultSet = statement.executeQuery();
        SeriesReference entry = null;
        if (resultSet.next())
        {
            final int ID      = resultSet.getInt   (1);
            final String name = resultSet.getString(2);
            entry=new SeriesReference(SERIES, name, ID);
            while (resultSet.next())
            {
                if (resultSet.getInt(1)!=ID || !name.equals(resultSet.getString(2)))
                {
                    throw new IllegalRecordException(SERIES, Resources.format(ResourceKeys.ERROR_DUPLICATE_SERIES_$1, name));
                }
            }
        }
        return entry;
    }

    /**
     * Retourne la liste des séries présentes dans la base de données.
     * Cette méthode ne retournera que les séries qui ont au moins un
     * groupe visible.
     *
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public synchronized List<SeriesEntry> getSeries() throws SQLException
    {
        final Statement    statement = connection.createStatement();
        final ResultSet    resultSet = statement.executeQuery(preferences.get("SERIES_TREE", SQL_TREE));
        final List<SeriesEntry> list = new ArrayList<SeriesEntry>();
        SeriesReference      last = null;
        while (resultSet.next())
        {
            final int                ID = resultSet.getInt   (SERIES_ID);
            final String           name = resultSet.getString(SERIES_NAME);
            final SeriesReference entry = new SeriesReference(SERIES, name, ID);
            if (!entry.equals(last)) list.add(last=entry);
        }
        resultSet.close();
        statement.close();
        return list;
    }

    /**
     * Retourne une arborescence qui pourra être affichée dans une composante {@link javax.swing.JTree}.
     * Cette arborescence contiendra les paramètres, opérations, séries et groupes trouvés dans la base
     * de données. Pour la plupart des noeuds de cette arborescence, la méthode {@link TreeNode#getUserObject}
     * retournera un objet {@link Entry} (ou {@link SeriesEntry} pour les noeuds qui représentent des
     * séries).
     *
     * @param  leafType Un des arguments {@link #SERIES_LEAF}, {@link #GROUP_LEAF} ou {@link #CATEGORY_LEAF}.
     * @return Arborescence des séries de la base de données.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public synchronized TreeModel getTree(final int leafType) throws SQLException
    {
        final Locale       locale = null;
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(preferences.get("SERIES_TREE", SQL_TREE));
        final int    SERIES_INDEX = 2; // Index des séries dans les tableaux ci-dessous.
        final int[]        colIDs = new int[]    {PARAMETER_ID,   OPERATION_ID,   SERIES_ID,   GROUPS_ID  }; // Doit être décroissant!
        final int[]      colNames = new int[]    {PARAMETER_NAME, OPERATION_NAME, SERIES_NAME, GROUPS_NAME}; // Doit être décroissant!
        final String[] tableNames = new String[] {PARAMETERS,     OPERATIONS,     SERIES,      GROUPS     };
        final String[]      names = new String[colIDs.length];
        final int[]           ids = new int   [colIDs.length];
        final int     branchCount = ids.length - (leafType>=GROUP_LEAF ? 0 : 1);
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(Resources.getResources(locale).getString(ResourceKeys.SERIES));
        /*
         * Balaye la liste de tous les groupes, et place ces groupes
         * dans une arborescence au fur et à mesure qu'ils sont trouvés.
         */
        while (resultSet.next())
        {
            /*
             * Mémorise les numéro ID et les noms de toutes les entrées
             * trouvées dans l'enregistrement courant.  Ca comprend les
             * noms et ID des groupes, séries, opérations et paramètres.
             */
            for (int i=colIDs.length; --i>=0;)
            {
                ids  [i] = resultSet.getInt   (colIDs  [i]);
                names[i] = resultSet.getString(colNames[i]);
            }
            DefaultMutableTreeNode branch=root;
      scan: for (int i=0; i<branchCount; i++)
            {
                /*
                 * Vérifie s'il existe déjà une branche pour le paramètre,
                 * opération où la série de l'enregistrement courant.  Si
                 * une de ces branches n'existe pas, elle sera créée au
                 * passage.
                 */
                final int ID=ids[i];
                for (int j=branch.getChildCount(); --j>=0;)
                {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) branch.getChildAt(j);
                    final Reference reference = (Reference) node.getUserObject();
                    if (reference.ID == ID)
                    {
                        branch=node;
                        continue scan;
                    }
                }
                final Reference ref;
                switch (i)
                {
                    case SERIES_INDEX: ref=new SeriesReference(tableNames[i], names[i], ID); break;
                    default:           ref=new       Reference(tableNames[i], names[i], ID); break;
                }
                /*
                 * Construit le noeud. Si les catégories  ont
                 * été demandées, elles seront ajoutées après
                 * le dernier noeud qui est du ressort de cet
                 * objet <code>SeriesTable</code>.
                 */
                final boolean hasMoreBranchs = i<(branchCount-1);
                final DefaultMutableTreeNode node;
                switch (leafType)
                {
                    case SERIES_LEAF: // fall through
                    case GROUP_LEAF:
                    {
                        node=new DefaultMutableTreeNode(ref, hasMoreBranchs);
                        break;
                    }
                    case CATEGORY_LEAF:
                    {
                        node=new DefaultMutableTreeNode(ref, true);
                        if (!hasMoreBranchs)
                        {
                            if (formats==null)
                            {
                                formats=new FormatTable(statement.getConnection());
                            }
                            final FormatEntryImpl format = formats.getEntry(resultSet.getInt(GROUPS_FORMAT));
                            node.add(format.getTree(locale));
                        }
                        break;
                    }
                    default: throw new IllegalArgumentException(String.valueOf(leafType));
                }
                branch.add(node);
                branch=node;
            }
        }
        resultSet.close();
        statement.close();
        return new DefaultTreeModel(root, true);
    }

    /**
     * Retourne le nombre d'images appartenant à la série spécifiée.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public synchronized int getImageCount(final SeriesEntry series) throws SQLException
    {
        if (count==null)
        {
            count = connection.prepareStatement(preferences.get("IMAGE_COUNT", SQL_COUNT));
        }
        count.setInt(1, series.getID());
        final ResultSet resultSet = count.executeQuery();
        int c=0; while (resultSet.next()) // Should have only 1 record.
        {
            c += resultSet.getInt(1);
        }
        resultSet.close();
        return c;
    }

    /**
     * Retourne le format d'un groupe d'images.
     *
     * @param  groupID Numéro ID de la table <code>Groups</code> de la base de données.
     * @return Le format utilisé par le groupe spécifié, ou <code>null</code> si aucun
     *         groupe identifié par l'ID specifié n'a été trouvé.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public synchronized FormatEntry getFormat(final int groupID) throws SQLException
    {
        if (formats==null)
        {
            formats=new FormatTable(connection);
        }
        return formats.forGroupID(groupID);
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (selectByName != null) {selectByName.close(); selectByName = null;}
        if (selectByID   != null) {selectByID  .close(); selectByID   = null;}
        if (count        != null) {count       .close(); count        = null;}
        if (formats      != null) {formats     .close(); formats      = null;}
    }


    /**
     * Référence vers un paramètre, une opération, une série ou un groupe de la base de données d'images.
     * Les références sont représentées par des numéros {@link #ID} qui apparaisent dans plusieurs tables
     * de la base de données. Par exemple chaque enregistrement de la table "series" (donc chaque série)
     * est identifié par un numéro ID unique, et chaque enregistrement de la table "groups" (donc chaque
     * groupe) est aussi identifié par un numéro ID unique. Pour sélectionner un enregistrement, il faut
     * connaître à la fois la table où l'enregistrement apparaît et son numéro ID. Une telle sélection
     * peut être faite avec l'instruction SQL suivante:
     *
     * <blockquote><pre>
     * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
     * </pre></blockquote>
     *
     * Par exemple si on recherche la série #5, alors l'instruction SQL sera:
     *
     * <blockquote><pre>
     * SELECT * FROM Series WHERE Series.ID=5
     * </pre></blockquote>
     *
     * Des objets <code>ImageSeries.Reference</code> sont contenus dans l'arborescence construite par
     * {@link SeriesTable#getTree} et peuvent être obtenus par des appels à {@link net.seas.awt.tree.TreeNode#getUserObject}.
     * La façon préférée d'obtenir des objets de cette classe est par la méthode {@link ImageDataBase#getReference(int)}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static class Reference implements Entry, Serializable
    {
        /**
         * Nom de la table qui contient l'enregistrement référencé.
         */
        public final String table;

        /**
         * Nom de la référence. Ce nom provient du champ "name" de la table {@link #table}. Ce
         * nom pourrait servir à identifier l'enregistrement référencé, mais on utilisera plutôt
         * {@link #ID} à cette fin.
         */
        public final String name;

        /**
         * Numéro de référence. Ce numéro provient du champ "ID" de la table {@link #table}.
         * Il sert à obtenir l'enregistrement référencé avec une instruction SQL comme suit:
         *
         * <blockquote><pre>
         * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
         * </pre></blockquote>
         */
        public final int ID;

        /**
         * Construit une réference vers un enregistrement de la base de données d'image.
         * L'enregistrement référencé peut être un format, un groupe, une série, un paramètre,
         * une opération, etc.
         *
         * @param table Nom de la table qui contient l'enregistrement référencé.
         * @param name  Nom de la référence. Ce nom provient du champ "name" de la table <code>table</code>.
         * @param ID    Numéro de référence. Ce numéro provient du champ "ID" de la table <code>table</code>.
         */
        protected Reference(final String table, final String name, final int ID)
        {
            this.table = table.trim();
            this.name  = name.trim();
            this.ID    = ID;
        }

        /**
         * Retourne un numéro unique identifiant cette série.
         */
        public int getID()
        {return ID;}

        /**
         * Retourne le nom de l'enregistrement référencé,
         * c'est-à-dire le champ {@link #name}.
         */
        public String getName()
        {return name;}

        /**
         * Retourne le nom de l'enregistrement référencé, comme {@link #getName}. Cette
         * méthode est appelée par {@link javax.swing.JTree} pour construire les noms
         * des noeuds dans l'arborescence.
         */
        public String toString()
        {return name;}

        /**
         * Retourne le numéro de référence,
         * c'est-à-dire le code {@link #getID}.
         */
        public int hashCode()
        {return ID;}

        /**
         * Indique si cette référence est
         * identique à l'objet spécifié.
         */
        public boolean equals(final Object o)
        {
            if (o instanceof Reference)
            {
                final Reference that = (Reference) o;
                return this.ID       ==  that.ID    &&
                       this.name .equals(that.name) &&
                       this.table.equals(that.table);
            }
            return false;
        }
    }


    /**
     * Référence vers une série. Cette classe est identique à la classe
     * {@link Reference}, mais implémente l'interface {@link SeriesEntry}
     * pour permettre aux utilisateurs externes de reconnaître qu'il s'agit
     * d'une série.
     *
     * @author Martin Desruisseaux
     * @version 1.0
     */
    private static final class SeriesReference extends Reference implements SeriesEntry
    {
        protected SeriesReference(final String table, final String name, final int ID)
        {super(table, name, ID);}
    }
}
