/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.sql.image;

// J2SE
import java.util.Locale;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.Serializable;
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
import fr.ird.sql.Entry;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Connection vers une table des s�ries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les param�tres et op�rations qui forment les s�ries, et de placer ces informations dans une arborescence
 * avec des chemins de la forme "<code>param�tre/op�ration/s�rie/groupe</code>".
 *
 * @see ImageDataBase#getSeriesTable
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SeriesTableImpl extends Table implements SeriesTable {
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir une s�rie � partir de son nom.
     */
    static final String SQL_SELECT = "SELECT ID, name, description, period FROM "+SERIES+" WHERE name LIKE ?";

    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir une s�rie � partir de son num�ro ID.
     */
    static final String SQL_SELECT_BY_ID = "SELECT ID, name, description, period FROM "+SERIES+" WHERE ID=?";

    /**
     * Requ�te SQL pour compter le nombre
     * d'images appartenant � une s�rie.
     */
    static final String SQL_COUNT = "SELECT Count("+IMAGES+".ID) "+
                                    "FROM "+IMAGES+" INNER JOIN "+GROUPS+" ON "+IMAGES+".groupe="+GROUPS+".ID\n"+
                                    "WHERE ("+GROUPS+".visible="+TRUE+") AND (series=?)\nGROUP BY "+IMAGES+".ID";

    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des s�ries.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
     * les constantes [@link #GROUPS_ID}, [@link #GROUPS_NAME} et compagnie.
     */
    static final String SQL_TREE =
           "SELECT "+  /*[01] GROUPS_ID         */     GROUPS+".ID, "          +
                       /*[02] GROUPS_NAME       */     GROUPS+".name, "        +
                       /*[03] GROUPS_REMARKS    */     GROUPS+".description, " +
                       /*[04] SERIES_ID         */     GROUPS+".series, "      +
                       /*[05] SERIES_NAME       */     SERIES+".name, "        +
                       /*[06] SERIES_REMARKS    */     SERIES+".description, " +
                       /*[07] OPERATION_ID      */     SERIES+".operation, "   +
                       /*[08] OPERATION_NAME    */ OPERATIONS+".name, "        +
                       /*[09] OPERATION_REMARKS */ OPERATIONS+".description, " +
                       /*[10] PARAMETER_ID      */     SERIES+".parameter, "   +
                       /*[11] PARAMETER_NAME    */ PARAMETERS+".name, "        +
                       /*[12] PARAMETER_REMARKS */ PARAMETERS+".description, " +
                       /*[13] GROUPS_FORMAT     */     GROUPS+".format,"       +
                       /*[14] PERIOD            */     SERIES+".period\n"      +
           "FROM ["  + PARAMETERS +"], " + // Note: les [  ] sont n�cessaires pour Access.
                       OPERATIONS + ", " +
                       SERIES     + ", " +
                       GROUPS     + "\n" +
           "WHERE "  + PARAMETERS + ".ID=" + SERIES     + ".parameter AND " +
                       OPERATIONS + ".ID=" + SERIES     + ".operation AND " +
                       SERIES     + ".ID=" + GROUPS     + ".series    AND " +
                       GROUPS     + ".visible=" + TRUE  + "\n"              +
           "ORDER BY "+PARAMETERS+".name, "+OPERATIONS+".name, "+SERIES+".name, "+GROUPS+".name";


    /** Num�ro de colonne. */ private static final int GROUPS_ID         =  1;
    /** Num�ro de colonne. */ private static final int GROUPS_NAME       =  2;
    /** Num�ro de colonne. */ private static final int GROUPS_REMARKS    =  3;
    /** Num�ro de colonne. */ private static final int SERIES_ID         =  4;
    /** Num�ro de colonne. */ private static final int SERIES_NAME       =  5;
    /** Num�ro de colonne. */ private static final int SERIES_REMARKS    =  6;
    /** Num�ro de colonne. */ private static final int OPERATION_ID      =  7;
    /** Num�ro de colonne. */ private static final int OPERATION_NAME    =  8;
    /** Num�ro de colonne. */ private static final int OPERATION_REMARKS =  9;
    /** Num�ro de colonne. */ private static final int PARAMETER_ID      = 10;
    /** Num�ro de colonne. */ private static final int PARAMETER_NAME    = 11;
    /** Num�ro de colonne. */ private static final int PARAMETER_REMARKS = 12;
    /** Num�ro de colonne. */ private static final int GROUPS_FORMAT     = 13;
    /** Num�ro de colonne. */ private static final int PERIOD            = 14;

    /** Num�ro d'argument. */ private static final int ARG_ID     = 1;
    /** Num�ro d'argument. */ private static final int ARG_NAME   = 1;

    /**
     * Repr�sente une branche de l'arborescence. Cette
     * classe interne est utilis�e par {@link #getTree}.
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
     * Liste des branches � inclure dans l'arborescence, dans l'ordre.
     * Cette liste est utilis�e par {@link #getTree} pour construire
     * l'arborescence.
     */
    private static final Branch[] TREE_STRUCTURE = new Branch[] {
        new Branch(PARAMETERS, PARAMETER_ID, PARAMETER_NAME, PARAMETER_REMARKS),
        new Branch(OPERATIONS, OPERATION_ID, OPERATION_NAME, OPERATION_REMARKS),
        new Branch(SERIES,     SERIES_ID,    SERIES_NAME,    SERIES_REMARKS   ),
        new Branch(GROUPS,     GROUPS_ID,    GROUPS_NAME,    GROUPS_REMARKS   )
    };

    /**
     * Connection avec la base de donn�es.
     */
    private final Connection connection;

    /**
     * Requ�te SQL retournant une s�rie � partir de son nom.
     */
    private transient PreparedStatement selectByName;

    /**
     * Requ�te SQL retournant une s�rie � partir de son num�ro ID.
     */
    private transient PreparedStatement selectByID;

    /**
     * Requ�te utilis�e pour compter le nombre d'images appartenant � une s�rie.
     * Cette requ�te ne sera construite que lorsqu'elle sera n�cessaire.
     */
    private transient PreparedStatement count;

    /**
     * Table des formats. Ce champ ne sera construit
     * que la premi�re fois o� il sera n�cessaire.
     */
    private transient FormatTable formats;

    /**
     * Construit un objet en utilisant la connection sp�cifi�e.
     *
     * @param connection Connection vers une base de donn�es d'images.
     * @throws SQLException si <code>SeriesTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected SeriesTableImpl(final Connection connection) throws SQLException {
        this.connection = connection;
    }

    /**
     * Retourne une r�f�rence vers un enregistrement de la table des s�ries.
     *
     * @param  ID Num�ro identifiant la s�rie recherch�e.
     * @return La s�rie identifi�e par le num�ro ID, ou <code>null</code>
     *         si aucune s�rie de ce num�ro n'a �t� trouv�e.
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     * @throws IllegalRecordException Si plusieurs s�ries portent le m�me ID.
     */
    public synchronized SeriesEntry getSeries(final int ID) throws SQLException {
        if (selectByID == null) {
            selectByID = connection.prepareStatement(preferences.get(SERIES+":ID", SQL_SELECT_BY_ID));
        }
        selectByID.setInt(ARG_ID, ID);
        return getSeries(selectByID);
    }

    /**
     * Retourne une r�f�rence vers un enregistrement de la table des s�ries.
     *
     * @param  name Nom de la s�rie recherch�e.
     * @return Une s�rie qui porte le nom <code>name</code>, ou <code>null</code>
     *         si aucune s�rie de ce nom n'a �t� trouv�e.
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     * @throws IllegalRecordException Si plusieurs s�ries portent le m�me nom.
     */
    public synchronized SeriesEntry getSeries(final String name) throws SQLException {
        if (selectByName == null) {
            selectByName = connection.prepareStatement(preferences.get(SERIES, SQL_SELECT));
        }
        selectByName.setString(ARG_NAME, name);
        return getSeries(selectByName);
    }

    /**
     * Retourne une r�f�rence vers un enregistrement de la table des s�ries.
     */
    private static SeriesEntry getSeries(final PreparedStatement statement) throws SQLException {
        final ResultSet resultSet = statement.executeQuery();
        SeriesReference entry = null;
        if (resultSet.next()) {
            final int ID         = resultSet.getInt   (1);
            final String name    = resultSet.getString(2);
            final String remarks = resultSet.getString(3);
            final double period  = resultSet.getDouble(4);
            entry = new SeriesReference(SERIES, name, ID, remarks,
                       resultSet.wasNull() ? Double.NaN : period);
            while (resultSet.next()) {
                if (resultSet.getInt(1)!=ID || !name.equals(resultSet.getString(2))) {
                    throw new IllegalRecordException(SERIES, Resources.format(
                                ResourceKeys.ERROR_DUPLICATED_SERIES_$1, name));
                }
            }
        }
        return entry;
    }

    /**
     * Retourne la liste des s�ries pr�sentes dans la base de donn�es.
     * Cette m�thode ne retournera que les s�ries qui ont au moins un
     * groupe visible.
     *
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public synchronized List<SeriesEntry> getSeries() throws SQLException {
        final Statement    statement = connection.createStatement();
        final ResultSet    resultSet = statement.executeQuery(preferences.get(SERIES+":TREE", SQL_TREE));
        final List<SeriesEntry> list = new ArrayList<SeriesEntry>();
        SeriesReference      last = null;
        while (resultSet.next()) {
            final int                ID = resultSet.getInt   (SERIES_ID);
            final String           name = resultSet.getString(SERIES_NAME);
            final String        remarks = resultSet.getString(SERIES_REMARKS);
            final double         period = resultSet.getDouble(PERIOD);
            final SeriesReference entry = new SeriesReference(SERIES, name, ID, remarks,
                                              resultSet.wasNull() ? Double.NaN : period);
            if (!entry.equals(last)) {
                list.add(last=entry);
            }
        }
        resultSet.close();
        statement.close();
        return list;
    }

    /**
     * Retourne une arborescence qui pourra �tre affich�e dans une composante {@link javax.swing.JTree}.
     * Cette arborescence contiendra les param�tres, op�rations, s�ries et groupes trouv�s dans la base
     * de donn�es. Pour la plupart des noeuds de cette arborescence, la m�thode {@link TreeNode#getUserObject}
     * retournera un objet {@link Entry} (ou {@link SeriesEntry} pour les noeuds qui repr�sentent des
     * s�ries).
     *
     * @param  leafType Un des arguments {@link #SERIES_LEAF}, {@link #GROUP_LEAF} ou {@link #CATEGORY_LEAF}.
     * @return Arborescence des s�ries de la base de donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public synchronized TreeModel getTree(final int leafType) throws SQLException {
        final Locale       locale = null;
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(preferences.get(SERIES+":TREE", SQL_TREE));
        final int     branchCount = TREE_STRUCTURE.length - (leafType>=GROUP_LEAF ? 0 : 1);
        final int[]           ids = new int   [branchCount];
        final String[]      names = new String[branchCount];
        final String[]    remarks = new String[branchCount];
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(Resources.getResources(locale).getString(ResourceKeys.SERIES));
        /*
         * Balaye la liste de tous les groupes, et place ces groupes
         * dans une arborescence au fur et � mesure qu'ils sont trouv�s.
         */
        while (resultSet.next()) {
            /*
             * M�morise les num�ro ID et les noms de toutes les entr�es
             * trouv�es dans l'enregistrement courant.  Ca comprend les
             * noms et ID des groupes, s�ries, op�rations et param�tres.
             */
            for (int i=branchCount; --i>=0;) {
                final Branch branch = TREE_STRUCTURE[i];
                ids    [i] = resultSet.getInt   (branch.ID     );
                names  [i] = resultSet.getString(branch.name   );
                remarks[i] = resultSet.getString(branch.remarks);
            }
            double period = resultSet.getDouble(PERIOD);
            if (resultSet.wasNull()) {
                period = Double.NaN;
            }
            DefaultMutableTreeNode branch=root;
      scan: for (int i=0; i<branchCount; i++) {
                /*
                 * V�rifie s'il existe d�j� une branche pour le param�tre,
                 * op�ration o� la s�rie de l'enregistrement courant.  Si
                 * une de ces branches n'existe pas, elle sera cr��e au
                 * passage.
                 */
                final int ID=ids[i];
                for (int j=branch.getChildCount(); --j>=0;) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) branch.getChildAt(j);
                    final Reference reference = (Reference) node.getUserObject();
                    if (reference.ID == ID) {
                        branch=node;
                        continue scan;
                    }
                }
                final String tableName = TREE_STRUCTURE[i].table;
                final Reference ref;
                if (tableName == SERIES) {
                    ref = new SeriesReference(tableName, names[i], ID, remarks[i], period);
                } else {
                    ref=new Reference(tableName, names[i], ID, remarks[i]);
                }
                /*
                 * Construit le noeud. Si les cat�gories  ont
                 * �t� demand�es, elles seront ajout�es apr�s
                 * le dernier noeud qui est du ressort de cet
                 * objet <code>SeriesTable</code>.
                 */
                final boolean hasMoreBranchs = i<(branchCount-1);
                final DefaultMutableTreeNode node;
                switch (leafType) {
                    case SERIES_LEAF: // fall through
                    case GROUP_LEAF: {
                        node=new DefaultMutableTreeNode(ref, hasMoreBranchs);
                        break;
                    }
                    case CATEGORY_LEAF: {
                        node=new DefaultMutableTreeNode(ref, true);
                        if (!hasMoreBranchs) {
                            if (formats == null) {
                                formats = new FormatTable(statement.getConnection());
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
     * Retourne le nombre d'images appartenant � la s�rie sp�cifi�e.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public synchronized int getImageCount(final SeriesEntry series) throws SQLException {
        if (count == null) {
            count = connection.prepareStatement(preferences.get(IMAGES+":COUNT", SQL_COUNT));
        }
        count.setInt(1, series.getID());
        final ResultSet resultSet = count.executeQuery();
        int c=0; while (resultSet.next()) { // Should have only 1 record.
            c += resultSet.getInt(1);
        }
        resultSet.close();
        return c;
    }

    /**
     * Retourne le format d'un groupe d'images.
     *
     * @param  groupID Num�ro ID de la table <code>Groups</code> de la base de donn�es.
     * @return Le format utilis� par le groupe sp�cifi�, ou <code>null</code> si aucun
     *         groupe identifi� par l'ID specifi� n'a �t� trouv�.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public synchronized FormatEntry getFormat(final int groupID) throws SQLException {
        if (formats == null) {
            formats=new FormatTable(connection);
        }
        return formats.forGroupID(groupID);
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
        if (selectByName != null) {selectByName.close(); selectByName = null;}
        if (selectByID   != null) {selectByID  .close(); selectByID   = null;}
        if (count        != null) {count       .close(); count        = null;}
        if (formats      != null) {formats     .close(); formats      = null;}
    }


    /**
     * R�f�rence vers un param�tre, une op�ration, une s�rie ou un groupe de la base de donn�es d'images.
     * Les r�f�rences sont repr�sent�es par des num�ros {@link #ID} qui apparaisent dans plusieurs tables
     * de la base de donn�es. Par exemple chaque enregistrement de la table "series" (donc chaque s�rie)
     * est identifi� par un num�ro ID unique, et chaque enregistrement de la table "groups" (donc chaque
     * groupe) est aussi identifi� par un num�ro ID unique. Pour s�lectionner un enregistrement, il faut
     * conna�tre � la fois la table o� l'enregistrement appara�t et son num�ro ID. Une telle s�lection
     * peut �tre faite avec l'instruction SQL suivante:
     *
     * <blockquote><pre>
     * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
     * </pre></blockquote>
     *
     * Par exemple si on recherche la s�rie #5, alors l'instruction SQL sera:
     *
     * <blockquote><pre>
     * SELECT * FROM Series WHERE Series.ID=5
     * </pre></blockquote>
     *
     * Des objets <code>ImageSeries.Reference</code> sont contenus dans l'arborescence
     * construite par {@link SeriesTable#getTree} et peuvent �tre obtenus par des appels
     * � {@link org.geotools.gui.swing.tree.TreeNode#getUserObject}.
     * La fa�on pr�f�r�e d'obtenir des objets de cette classe est par la m�thode
     * {@link ImageDataBase#getReference(int)}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static class Reference implements Entry, Serializable {
        /**
         * Nom de la table qui contient l'enregistrement r�f�renc�.
         */
        public final String table;

        /**
         * Nom de la r�f�rence. Ce nom provient du champ "name" de la table {@link #table}. Ce
         * nom pourrait servir � identifier l'enregistrement r�f�renc�, mais on utilisera plut�t
         * {@link #ID} � cette fin.
         */
        public final String name;

        /**
         * Remarques s'appliquant � cette entr�e.
         */
        public final String remarks;

        /**
         * Num�ro de r�f�rence. Ce num�ro provient du champ "ID" de la table {@link #table}.
         * Il sert � obtenir l'enregistrement r�f�renc� avec une instruction SQL comme suit:
         *
         * <blockquote><pre>
         * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
         * </pre></blockquote>
         */
        public final int ID;

        /**
         * Construit une r�ference vers un enregistrement de la base de donn�es d'image.
         * L'enregistrement r�f�renc� peut �tre un format, un groupe, une s�rie, un param�tre,
         * une op�ration, etc.
         *
         * @param table   Nom de la table qui contient l'enregistrement r�f�renc�.
         * @param name    Nom de la r�f�rence. Ce nom provient du champ "name" de la table <code>table</code>.
         * @param ID      Num�ro de r�f�rence. Ce num�ro provient du champ "ID" de la table <code>table</code>.
         * @param remarks Remarques s'appliquant � cette r�f�rences.
         */
        protected Reference(final String table, final String name, final int ID, final String remarks) {
            this.table   = table.trim();
            this.name    = name.trim();
            this.remarks = remarks; // May be null
            this.ID      = ID;
        }

        /**
         * Retourne un num�ro unique identifiant cette s�rie.
         */
        public int getID() {
            return ID;
        }

        /**
         * Retourne le nom de l'enregistrement r�f�renc�,
         * c'est-�-dire le champ {@link #name}.
         */
        public String getName() {
            return name;
        }

        /**
         * Retourne des remarques s'appliquant � cette entr�e,
         * ou <code>null</code> s'il n'y en a pas. Ces remarques
         * sont souvent une cha�ne descriptives qui peuvent �tre
         * affich�es comme "tooltip text".
         */
        public String getRemarks() {
            return remarks;
        }

        /**
         * Retourne le nom de l'enregistrement r�f�renc�, comme {@link #getName}. Cette
         * m�thode est appel�e par {@link javax.swing.JTree} pour construire les noms
         * des noeuds dans l'arborescence.
         */
        public String toString() {
            return name;
        }

        /**
         * Retourne le num�ro de r�f�rence,
         * c'est-�-dire le code {@link #getID}.
         */
        public int hashCode() {
            return ID;
        }

        /**
         * Indique si cette r�f�rence est
         * identique � l'objet sp�cifi�.
         */
        public boolean equals(final Object o) {
            if (o instanceof Reference) {
                final Reference that = (Reference) o;
                return this.ID       ==  that.ID    &&
                       this.name .equals(that.name) &&
                       this.table.equals(that.table);
            }
            return false;
        }
    }


    /**
     * R�f�rence vers une s�rie. Cette classe est identique � la classe
     * {@link Reference}, mais impl�mente l'interface {@link SeriesEntry}
     * pour permettre aux utilisateurs externes de reconna�tre qu'il s'agit
     * d'une s�rie.
     *
     * @author Martin Desruisseaux
     * @version $Id$
     */
    private static final class SeriesReference extends Reference implements SeriesEntry {
        /**
         * La p�riode "normale" des images de cette s�rie (en nombre
         * de jours), ou {@link Double#NaN} si elle est inconnue.
         */
        private final double period;

        /**
         * Construit une nouvelle r�f�rence.
         */
        protected SeriesReference(final String table, final String name, final int ID,
                                  final String remarks, final double period)
        {
            super(table, name, ID, remarks);
            this.period = period;
        }

        /**
         * Retourne la p�riode "normale" des images de cette s�rie (en nombre
         * de jours), ou {@link Double#NaN} si elle est inconnue.
         */
        public double getPeriod() {
            return period;
        }
    }
}
