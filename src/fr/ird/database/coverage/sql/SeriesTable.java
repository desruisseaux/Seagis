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
 */
package fr.ird.database.coverage.sql;

// J2SE dependencies
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.io.Serializable;
import java.rmi.RemoteException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;

// Geotools dependencies
import org.geotools.gui.swing.tree.TreeNode;
import org.geotools.gui.swing.tree.MutableTreeNode;
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// Seagis dependencies
import fr.ird.database.ConfigurationKey;
import fr.ird.database.CatalogException;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Connection vers une table des s�ries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les param�tres et op�rations qui forment les s�ries, et de placer ces informations dans une
 * arborescence avec des chemins de la forme "<code>param�tre/op�ration/s�rie</code>".
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
     * Requ�te SQL utilis�e par cette classe pour obtenir une s�rie � partir de son nom.
     */
    static final ConfigurationKey SELECT = createKey(SERIES, ResourceKeys.SQL_SERIES_BY_NAME,
            "SELECT name, description, period FROM "+SCHEMA+".\""+SERIES+"\" WHERE name=?");

    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir les sous-s�ries d'une s�rie.
     */
    static final ConfigurationKey SELECT_SUBSERIES = createKey(SERIES+":SubSeries", ResourceKeys.SQL_SERIES_BY_SUBSERIES,
        "SELECT identifier, format FROM "+SCHEMA+".\""+SUBSERIES+"\" WHERE series=?");

    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des s�ries.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
     * les constantes {@link #SERIES_ID}, {@link #SERIES_NAME} et compagnie.
     */
    static final ConfigurationKey SELECT_TREE = createKey(SERIES+":Tree", ResourceKeys.SQL_SERIES_TREE,
            "SELECT \"" +   SUBSERIES  + "\".identifier, "    +   // [01] SUBSERIES_ID
                    '"' +      SERIES  + "\".name, "          +   // [02] SERIES_NAME
                    '"' +      SERIES  + "\".description, "   +   // [03] SERIES_REMARKS
                    '"' +  PROCEDURES  + "\".name, "          +   // [04] PROCEDURE_NAME
                    '"' +  PROCEDURES  + "\".description, "   +   // [05] PROCEDURE_REMARKS
                    '"' + PHENOMENONS  + "\".name, "          +   // [06] PHENOMENON_NAME
                    '"' + PHENOMENONS  + "\".description, "   +   // [07] PHENOMENON_REMARKS
                    '"' +   SUBSERIES  + "\".format, "        +   // [08] FORMAT
                    '"' +      SERIES  + "\".period\n"        +   // [09] PERIOD
              "FROM "   + SCHEMA+".\"" + PHENOMENONS + "\", " +
                          SCHEMA+".\"" + PROCEDURES  + "\", " +
                          SCHEMA+".\"" + SERIES      + "\", " +
                          SCHEMA+".\"" + SUBSERIES   + "\"\n" +
             "WHERE \"" + PHENOMENONS  + "\".name=phenomenon "+
               "AND \"" + PROCEDURES   + "\".name=procedure " +
               "AND \"" + SERIES       + "\".name=series "    +
               "AND "   +                "visible=TRUE\n"     +
          "ORDER BY \"" + PHENOMENONS  + "\".name, "          +
                    '"' + PROCEDURES   + "\".name, "          +
                    '"' + SERIES       + "\".name");


    /** Num�ro de colonne. */ private static final int SUBSERIES_ID       =  1;
    /** Num�ro de colonne. */ private static final int SERIES_NAME        =  2;
    /** Num�ro de colonne. */ private static final int SERIES_REMARKS     =  3;
    /** Num�ro de colonne. */ private static final int PROCEDURE_NAME     =  4;
    /** Num�ro de colonne. */ private static final int PROCEDURE_REMARKS  =  5;
    /** Num�ro de colonne. */ private static final int PHENOMENON_NAME    =  6;
    /** Num�ro de colonne. */ private static final int PHENOMENON_REMARKS =  7;
    /** Num�ro de colonne. */ private static final int FORMAT             =  8;
    /** Num�ro de colonne. */ private static final int PERIOD             =  9;
    /** Num�ro d'argument. */ private static final int ARG_NAME           =  1;

    /**
     * Repr�sente une branche de l'arborescence. Cette
     * classe interne est utilis�e par {@link #getTree}.
     */
    private static final class Branch implements Serializable {
        /** Nom de la table.                */ final String table;
        /** Colonne du champ ID ou 'name'.  */ final int identifier;
        /** Colonne du champ 'description'. */ final int remarks;

        /** Construit une branche. */
        Branch(final String table, final int identifier, final int remarks) {
            this.table      = table;
            this.identifier = identifier;
            this.remarks    = remarks;
        }
    }

    /**
     * Liste des branches � inclure dans l'arborescence, dans l'ordre.
     * Cette liste est utilis�e par {@link #getTree} pour construire
     * l'arborescence.
     */
    private static final Branch[] TREE_STRUCTURE = new Branch[] {
        new Branch(PHENOMENONS, PHENOMENON_NAME, PHENOMENON_REMARKS),
        new Branch( PROCEDURES,  PROCEDURE_NAME,  PROCEDURE_REMARKS),
        new Branch(     SERIES,     SERIES_NAME,     SERIES_REMARKS),
        new Branch(  SUBSERIES,  SUBSERIES_ID,                    0)
    };

    /**
     * Connection avec la base de donn�es.
     */
    private final Connection connection;

    /**
     * Requ�te SQL retournant une s�rie � partir de son nom.
     */
    private transient PreparedStatement select;

    /**
     * Requ�te SQL retournant les sous-s�ries d'une s�rie.
     */
    private transient PreparedStatement subseries;

    /**
     * La table des formats. Ne sera cr��e que lorsqu'elle sera n�cessaire.
     */
    private transient FormatTable formats;

    /**
     * Construit un objet en utilisant la connection sp�cifi�e.
     *
     * @param database The database where this table come from.
     * @param connection Connection vers une base de donn�es d'images.
     * @throws SQLException si <code>SeriesTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected SeriesTable(final CoverageDataBase database,
                          final Connection     connection)
            throws RemoteException
    {
        super(database);
        this.connection = connection;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized SeriesEntry getEntry(final String name) throws RemoteException {
        try {
            if (select == null) {
                select = connection.prepareStatement(getProperty(SELECT));
            }
            select.setString(ARG_NAME, name);
            return getEntry(select);
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }        
    }

    /**
     * Retourne une r�f�rence vers un enregistrement de la table des s�ries.
     *
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     * @throws IllegalRecordException Si plusieurs s�ries portent le m�me nom ou m�me ID.
     */
    private SeriesEntry getEntry(final PreparedStatement statement)
            throws SQLException, RemoteException
    {
        final ResultSet resultSet = statement.executeQuery();
        SeriesEntry entry = null;
        while (resultSet.next()) {
            String name    = resultSet.getString(1);
            String remarks = resultSet.getString(2);
            double  period = resultSet.getDouble(3);
            if (resultSet.wasNull()) period=Double.NaN;
            final SeriesEntry candidate = new SeriesEntry(SERIES, name, remarks, period);
            if (true) {
                /*
                 * Recherche les sous-s�ries (note: si ce bloc est retir�,
                 * alors la m�thode peut �tre statique).
                 */
                if (subseries == null) {
                    subseries = connection.prepareStatement(getProperty(SELECT_SUBSERIES));
                }
                subseries.setString(1, name);
                final List<Entry> subseries = new ArrayList<Entry>();
                final ResultSet subResults = this.subseries.executeQuery();
                while (subResults.next()) {
                    subseries.add(new Entry(SUBSERIES, subResults.getString(1), null));
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
    public synchronized Set<fr.ird.database.coverage.SeriesEntry> getEntries() throws RemoteException {
        try {
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery(getProperty(SELECT_TREE));
            final Set<fr.ird.database.coverage.SeriesEntry> set =
                  new LinkedHashSet<fr.ird.database.coverage.SeriesEntry>();
            while (resultSet.next()) {
                String    name = resultSet.getString(SERIES_NAME);
                String remarks = resultSet.getString(SERIES_REMARKS);
                double  period = resultSet.getDouble(PERIOD);
                if (resultSet.wasNull()) period=Double.NaN;
                final SeriesEntry entry = new SeriesEntry(SERIES, name, remarks, period);
                set.add(entry);
            }
            resultSet.close();
            statement.close();
            return set;
        } catch (SQLException e) {
            throw new CatalogException(e);
        }        
    }

    /**
     * {@inheritDoc}
     */
    public synchronized TreeModel getTree(final int leafType) throws RemoteException {
        try {
            final Locale        locale = Locale.getDefault();
            final Statement  statement = connection.createStatement();
            final ResultSet  resultSet = statement.executeQuery(getProperty(SELECT_TREE));
            final int      branchCount = Math.min(TREE_STRUCTURE.length, leafType);
            final String[] identifiers = new String[branchCount];
            final String[]     remarks = new String[branchCount];
            final DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                    Resources.getResources(locale).getString(ResourceKeys.SERIES));
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
                    int c;
                    final Branch branch = TREE_STRUCTURE[i];
                    identifiers[i] = ((c=branch.identifier)!=0) ? resultSet.getString(c) : null;
                    remarks[i]     = ((c=branch.remarks   )!=0) ? resultSet.getString(c) : null;
                }
                final String format = resultSet.getString(FORMAT);
                double period = resultSet.getDouble(PERIOD);
                if (resultSet.wasNull()) {
                    period = Double.NaN;
                }
                DefaultMutableTreeNode branch = root;
          scan: for (int i=0; i<branchCount; i++) {
                    /*
                     * V�rifie s'il existe d�j� une branche pour le param�tre,
                     * op�ration o� la s�rie de l'enregistrement courant.  Si
                     * une de ces branches n'existe pas, elle sera cr��e au
                     * passage.
                     */
                    final String identifier = identifiers[i];
                    for (int j=branch.getChildCount(); --j>=0;) {
                        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) branch.getChildAt(j);
                        final Entry reference = (Entry) node.getUserObject();
                        if (identifier.equals(reference.identifier)) {
                            branch = node;
                            continue scan;
                        }
                    }
                    final String tableName = TREE_STRUCTURE[i].table;
                    final Entry ref;
                    if (tableName == SERIES) {
                        ref = new SeriesEntry(tableName, identifier, remarks[i], period);
                    } else {
                        ref = new Entry(tableName, identifier, remarks[i]);
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
                        case SERIES_LEAF: // Fall through
                        case SUBSERIES_LEAF: {
                            node = new EntryNode(ref, hasMoreBranchs);
                            break;
                        }
                        case CATEGORY_LEAF: {
                            node = new EntryNode(ref, true);
                            if (!hasMoreBranchs) {
                                if (formats == null) {
                                    formats = new FormatTable(database, connection);
                                }
                                node.add(formats.getEntry(format).getTree(locale));
                            }
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException(String.valueOf(leafType));
                        }
                    }
                    branch.add(node);                    
                    branch = node;
                }
            }
            resultSet.close();
            statement.close();
            return new DefaultTreeModel(root, true);
        } catch (SQLException e) {
            throw new CatalogException(e);
        }        
    }

    /**
     * {@inheritDoc}
     */
    public synchronized FormatEntry getFormat(final fr.ird.database.coverage.SeriesEntry series)
            throws RemoteException
    {
        try {
            if (subseries == null) {
                subseries = connection.prepareStatement(getProperty(SELECT_SUBSERIES));
            }
            subseries.setString(1, series.getName());
            final ResultSet results = subseries.executeQuery();
            FormatEntry result = null;
            while (results.next()) {
                final FormatEntry candidate;
                final String format = results.getString(2);
                if (formats == null) {
                    formats = new FormatTable(database, connection);
                }
                candidate = formats.getEntry(format);
                if (result == null) {
                    result = candidate;
                } else if (!result.equals(candidate)) {
                    throw new IllegalRecordException(SUBSERIES,
                              Resources.format(ResourceKeys.ERROR_DUPLICATED_RECORD_$1, series.getName()));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new CatalogException(e);
        }            
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws RemoteException {
        try {
            if (select    != null) {select   .close(); select    = null;}
            if (subseries != null) {subseries.close(); subseries = null;}
            if (formats   != null) {formats  .close(); formats   = null;}
        } catch (SQLException e) {
            throw new CatalogException(e);
        }            
    }
}
