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
package fr.ird.database.coverage.sql;

// Base de donn�es
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.rmi.RemoteException;

// Mod�les (table et arborescence)
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.table.TableCellRenderer;

// Interface utilisateur
import javax.swing.JTree;
import javax.swing.JTable;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

// Utilitaires
import java.util.Date;
import java.util.TimeZone;
import java.awt.Rectangle;

// Geotools
import org.geotools.resources.Arguments;
import org.geotools.gui.swing.tree.Trees;
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageTableModel;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Fournit des outils de lignes de commande pour l'administration
 * de la base de donn�es d'images. Les options reconnues sont:
 *
 * <blockquote><pre>
 *  <b>-help</b> <i></i>         Affiche cette liste des options
 *  <b>-series</b> <i></i>       Affiche l'arborescence des s�ries
 *  <b>-formats</b> <i></i>      Affiche la table des formats
 *  <b>-config</b> <i></i>       Configure la base de donn�es (interface graphique)
 *  <b>-browse</b> <i></i>       Affiche le contenu de toute la base de donn�es (interface graphique)
 *  <b>-source</b> <i>name</i>   Source des donn�es                (exemple: "jdbc:odbc:SEAS-Images")
 *  <b>-driver</b> <i>name</i>   Pilote de la base de donn�es      (exemple: "sun.jdbc.odbc.JdbcOdbcDriver")
 *  <b>-locale</b> <i>name</i>   Langue et conventions d'affichage (exemple: "fr_CA")
 *  <b>-encoding</b> <i>name</i> Page de code pour les sorties     (exemple: "cp850")
 *  <b>-Xout</b> <i>filename</i> Fichier de destination (le p�riph�rique standard par d�faut)
 * </pre></blockquote>
 *
 * L'argument <code>-cp</code> est surtout utile lorsque cette m�thode est lanc�e
 * � partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la m�me page
 * de code que le reste du syst�me Windows. Il est alors n�cessaire de pr�ciser la
 * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
 * caract�res �tendus. La page de code en cours peut �tre obtenu en tappant
 * <code>chcp</code> sur la ligne de commande.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Main extends Arguments {
    /**
     * Pilote utilis�e.
     */
    private final String driver;

    /**
     * Source de donn�es utilis�e.
     */
    private final String source;

    /**
     * Connexion vers la base de donn�es. Cette connexion
     * ne sera �tablie que la premi�re fois o� elle sera
     * n�cessaire.
     */
    private transient Connection connection;

    /**
     * <code>true</code> si le drapeau <code>-config</code>
     * a �t� sp�cifi�.
     */
    final boolean config;

    /**
     * Construit une console.
     *
     * @param args Arguments transmis sur la ligne de commande.
     */
    public Main(final String[] args) {
        super(args);
        this.config   = getFlag          ("-config");
        String driver = getOptionalString("-driver");
        String source = getOptionalString("-source");
        if (driver == null) {
            Table.configuration.get(Configuration.KEY_DRIVER);
        }
        if (source == null) {
            Table.configuration.get(Configuration.KEY_SOURCE);
        }
        this.driver = driver;
        this.source = source;
    }

    /**
     * Retourne la connexion vers la base de donn�es.
     * @throws SQLException si la connexion n'a pas pu �tre �tablie.
     */
    private Connection getConnection() throws SQLException {
        if (connection==null) try {
            Class.forName(driver);
            connection = DriverManager.getConnection(source);
        } catch (ClassNotFoundException exception) {
            final SQLException e = new SQLException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        return connection;
    }

    /**
     * Print help instructions.
     */
    private void help() {
        out.println();
        out.println("Outils de ligne de commande pour la base de donn�es d'images\n"+
                    "1999-2002, Institut de Recherche pour le D�veloppement\n"+
                    "\n"+
                    "Options disponibles:\n"+
                    "  -help            Affiche cette liste des options\n"+
                    "  -series          Affiche l'arborescence des s�ries\n"+
                    "  -subseries       Affiche l'arborescence des sous-s�ries\n"+
                    "  -formats         Affiche la table des formats\n"+
                    "  -config          Configure la base de donn�es (interface graphique)\n"+
                    "  -browse          Affiche le contenu de toute la base de donn�es (interface graphique)\n"+
                    "  -source <name>   Source des donn�es                (exemple: \"jdbc:odbc:SEAS-Images\")\n"+
                    "  -driver <name>   Pilote de la base de donn�es      (exemple: \"sun.jdbc.odbc.JdbcOdbcDriver\")\n"+
                    "  -locale <name>   Langue et conventions d'affichage (exemple: \"fr_CA\")\n"+
                    "  -encoding <name> Page de code pour les sorties     (exemple: \"cp850\")\n"+
                    "  -Xout <filename> Fichier de destination (le p�riph�rique standard par d�faut)");
    }

    /**
     * Affiche l'arborescence des s�ries  qui se trouvent dans la base
     * de donn�es. Cette m�thode sert � v�rifier le contenu de la base
     * de donn�es  ainsi que le bon fonctionnement de l'impl�mentation
     * de {@link SeriesTable}.
     */
    private void series(final int leafType) throws RemoteException {
        try {
            final Connection connection = getConnection();
            final SeriesTable series = new SeriesTable(connection);
            final TreeModel    model = series.getTree(leafType);
            series.close();
            out.println();
            out.println(Trees.toString(model));
            out.flush();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Affiche la liste de tous les formats trouv�s dans la base de donn�es.
     * Cette m�thode sert � v�rifier le contenu de la base de donn�es, ainsi
     * que le bon fonctionnement des classes d'interrogation.
     */
    private void formats() throws RemoteException {
        try {
            final Connection connection = getConnection();
            final DefaultMutableTreeNode root = new DefaultMutableTreeNode(Resources.getResources(locale).getString(ResourceKeys.FORMATS));
            final String        query = "SELECT ID FROM "+Table.FORMATS;
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery(query);
            final FormatTable formats = new FormatTable(connection);
            while (resultSet.next()) {
                final Integer ID=new Integer(resultSet.getInt(1));
                root.add(formats.getEntry(ID).getTree(locale));
            }
            resultSet.close();
            statement.close();
            formats  .close();
            out.println();
            out.println(Trees.toString(new DefaultTreeModel(root)));
            out.flush();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Affiche dans une fen�tre <i>Swing</i>
     * le contenu de toute la base de donn�es.
     */
    private void browse() throws RemoteException {
        try {
            final Connection    connection = getConnection();
            final SeriesTable  seriesTable = new SeriesTable(connection);
            final TreeModel      treeModel = seriesTable.getTree(SeriesTable.CATEGORY_LEAF);

            final JComponent    treePane = new JScrollPane(new JTree(treeModel));
            final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
            final JSplitPane   splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, treePane, tabbedPane);

            final TableCellRenderer renderer = new CoverageTableModel.CellRenderer();
            final CoverageTable   imageTable = new GridCoverageTable(connection,
                                TimeZone.getTimeZone(Table.getPreference(CoverageDataBase.TIMEZONE)));

            imageTable.setGeographicArea(new Rectangle(-180, -90, 360, 180));
            imageTable.setTimeRange(new Date(0), new Date());
            for (final fr.ird.database.coverage.SeriesEntry currentSeries : seriesTable.getEntries()) {
                imageTable.setSeries(currentSeries);
                final JTable table = new JTable(new CoverageTableModel(imageTable));
                table.setDefaultRenderer(String.class, renderer);
                table.setDefaultRenderer(  Date.class, renderer);
                tabbedPane.addTab(currentSeries.getName(), new JScrollPane(table));
            }
            imageTable .close();
            seriesTable.close();

            final JFrame frame = new JFrame(Resources.format(ResourceKeys.DATABASE));
            frame.setContentPane(splitPane);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.show();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }            
    }

    /**
     * Ex�cute tout.
     */
    public void run() throws RemoteException {
        try {
            final boolean   formats = getFlag("-formats");
            final boolean subseries = getFlag("-subseries");
            final boolean    series = getFlag("-series") || subseries;
            final boolean    browse = getFlag("-browse");
            final boolean      help = getFlag("-help") || (!series && !formats && !config && !browse);

            getRemainingArguments(0);
            if (formats && series) series(SeriesTable.CATEGORY_LEAF);
            else if (subseries) series(SeriesTable.SUBSERIES_LEAF);
            else if (series)  series(SeriesTable.SERIES_LEAF);
            else if (formats) formats();
            if (browse)       browse();
            if (help)         help();

            if (connection!=null) {
                connection.close();
                connection = null;
            }
            out.flush();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }            
    }

    /**
     * Affiche l'arborescence des s�ries qui se trouvent dans la base
     * de donn�es. Cette m�thode sert � v�rifier le contenu de la base
     * de donn�es ainsi que le bon fonctionnement de cette classe.
     */
    public static void main(final String[] args) throws RemoteException {
        org.geotools.resources.MonolineFormatter.init("fr.ird");
        new Main(args).run();
    }
}
