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
package fr.ird.image.sql;

// Base de données
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

// Modèles (table et arborescence)
import net.seas.awt.tree.Trees;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.table.TableCellRenderer;
import net.seas.awt.tree.DefaultMutableTreeNode;
import fr.ird.awt.ImageTableModel;

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
import net.seas.util.Console;
import fr.ird.resources.Resources;


/**
 * Fournit des outils de lignes de commande pour l'administration
 * de la base de données d'images. Les options reconnues sont:
 *
 * <blockquote><pre>
 *  <b>-help</b> <i></i>           Affiche cette liste des options
 *  <b>-series</b> <i></i>         Affiche l'arborescence des séries
 *  <b>-groups</b> <i></i>         Affiche l'arborescence des groupes (incluant les séries)
 *  <b>-formats</b> <i></i>        Affiche la table des formats
 *  <b>-config</b> <i></i>         Configure la base de données (interface graphique)
 *  <b>-browse</b> <i></i>         Affiche le contenu de toute la base de données (interface graphique)
 *  <b>-source</b> <i>name</i>     Source des données                (exemple: "jdbc:odbc:SEAS-Images")
 *  <b>-driver</b> <i>name</i>     Pilote de la base de données      (exemple: "sun.jdbc.odbc.JdbcOdbcDriver")
 *  <b>-locale</b> <i>name</i>     Langue et conventions d'affichage (exemple: "fr_CA")
 *  <b>-encoding</b> <i>name</i>   Page de code pour les sorties     (exemple: "cp850")
 *  <b>-output</b> <i>filename</i> Fichier de destination (le périphérique standard par défaut)
 * </pre></blockquote>
 *
 * L'argument <code>-cp</code> est surtout utile lorsque cette méthode est lancée
 * à partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la même page
 * de code que le reste du système Windows. Il est alors nécessaire de préciser la
 * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
 * caractères étendus. La page de code en cours peut être obtenu en tappant
 * <code>chcp</code> sur la ligne de commande.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Main extends Console
{
    /**
     * Pilote par défaut.
     */
    static final String DRIVER = "sun.jdbc.odbc.JdbcOdbcDriver";

    /**
     * Source de données par défaut.
     */
    static final String SOURCE = "jdbc:odbc:SEAS-Images";

    /**
     * Pilote utilisée.
     */
    private final String driver;

    /**
     * Source de données utilisée.
     */
    private final String source;

    /**
     * Connexion vers la base de données. Cette connexion
     * ne sera établie que la première fois où elle sera
     * nécessaire.
     */
    private transient Connection connection;

    /**
     * <code>true</code> si le drapeau <code>-config</code>
     * a été spécifié.
     */
    final boolean config;

    /**
     * Construit une console.
     *
     * @param args Arguments transmis sur la ligne de commande.
     */
    public Main(final String[] args)
    {
        super(args);
        this.config   = hasFlag     ("-config");
        String driver = getParameter("-driver");
        String source = getParameter("-source");
        if (driver==null) driver=Table.preferences.get("Driver", DRIVER); else Table.preferences.put("Driver", driver);
        if (source==null) source=Table.preferences.get("Source", SOURCE); else Table.preferences.put("Source", source);
        this.driver = driver;
        this.source = source;
    }

    /**
     * Retourne la connexion vers la base de données.
     * @throws SQLException si la connexion n'a pas pu être établie.
     */
    private Connection getConnection() throws SQLException
    {
        if (connection==null) try
        {
            Class.forName(driver);
            connection = DriverManager.getConnection(source);
        }
        catch (ClassNotFoundException exception)
        {
            final SQLException e = new SQLException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        return connection;
    }

    /**
     * Print help instructions.
     */
    private void help()
    {
        out.println();
        out.println("Outils de ligne de commande pour la base de données d'images\n"+
                    "1999-2002, Institut de Recherche pour le Développement\n"+
                    "\n"+
                    "Options disponibles:\n"+
                    "  -help              Affiche cette liste des options\n"+
                    "  -series            Affiche l'arborescence des séries\n"+
                    "  -groups            Affiche l'arborescence des groupes (incluant les séries)\n"+
                    "  -formats           Affiche la table des formats\n"+
                    "  -config            Configure la base de données (interface graphique)\n"+
                    "  -browse            Affiche le contenu de toute la base de données (interface graphique)\n"+
                    "  -source <name>     Source des données                (exemple: \"jdbc:odbc:SEAS-Images\")\n"+
                    "  -driver <name>     Pilote de la base de données      (exemple: \"sun.jdbc.odbc.JdbcOdbcDriver\")\n"+
                    "  -locale <name>     Langue et conventions d'affichage (exemple: \"fr_CA\")\n"+
                    "  -encoding <name>   Page de code pour les sorties     (exemple: \"cp850\")\n"+
                    "  -output <filename> Fichier de destination (le périphérique standard par défaut)");
    }

    /**
     * Affiche l'arborescence des séries  qui se trouvent dans la base
     * de données. Cette méthode sert à vérifier le contenu de la base
     * de données  ainsi que le bon fonctionnement de l'implémentation
     * de {@link SeriesTable}.
     */
    private void series(final int leafType) throws SQLException
    {
        final Connection connection = getConnection();
        final SeriesTable series = new SeriesTableImpl(connection);
        final TreeModel    model = series.getTree(leafType);
        series.close();
        out.println();
        out.println(Trees.toString(model));
        out.flush();
    }

    /**
     * Affiche la liste de tous les formats trouvés dans la base de données.
     * Cette méthode sert à vérifier le contenu de la base de données, ainsi
     * que le bon fonctionnement des classes d'interrogation.
     */
    private void formats() throws SQLException
    {
        final Connection connection = getConnection();
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(Resources.getResources(locale).getString(Clé.FORMATS));
        final String        query = "SELECT ID FROM "+Table.FORMATS;
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(query);
        final FormatTable formats = new FormatTable(connection);
        while (resultSet.next())
        {
            final Integer ID=new Integer(resultSet.getInt(1));
            root.add(formats.getEntry(ID).getTree(locale));
        }
        resultSet.close();
        statement.close();
        formats  .close();
        out.println();
        out.println(Trees.toString(new DefaultTreeModel(root)));
        out.flush();
    }

    /**
     * Affiche dans une fenêtre <i>Swing</i>
     * le contenu de toute la base de données.
     */
    private void browse() throws SQLException
    {
        final Connection   connection = getConnection();
        final SeriesTable seriesTable = new SeriesTableImpl(connection);
        final TreeModel     treeModel = seriesTable.getTree(SeriesTable.CATEGORY_LEAF);
        final SeriesEntry[]    series = seriesTable.getSeries();
        seriesTable.close();

        final JComponent    treePane = new JScrollPane(new JTree(treeModel));
        final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        final JSplitPane   splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, treePane, tabbedPane);

        final TableCellRenderer renderer = new ImageTableModel.CellRenderer();
        final ImageTable      imageTable = new ImageTableImpl(connection, TimeZone.getTimeZone("UTC"));
        imageTable.setGeographicArea(new Rectangle(-180, -90, 360, 180));
        imageTable.setTimeRange(new Date(0), new Date());
        for (int i=0; i<series.length; i++)
        {
            imageTable.setSeries(series[i]);
            final JTable table = new JTable(new ImageTableModel(imageTable));
            table.setDefaultRenderer(String.class, renderer);
            table.setDefaultRenderer(  Date.class, renderer);
            tabbedPane.addTab(series[i].getName(), new JScrollPane(table));
        }
        imageTable.close();

        final JFrame frame = new JFrame(Resources.format(Clé.DATABASE));
        frame.setContentPane(splitPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    /**
     * Exécute tout.
     */
    public void run() throws SQLException
    {
        final boolean formats = hasFlag("-formats");
        final boolean  groups = hasFlag("-groups");
        final boolean  series = hasFlag("-series");
        final boolean  browse = hasFlag("-browse");
        final boolean    help = hasFlag("-help") || (!groups && !series && !formats && !config && !browse);
        try
        {
            checkRemainingArguments(0);
            if (formats && (groups || series)) series(SeriesTable.CATEGORY_LEAF);
            else if (groups)  series(SeriesTable.GROUP_LEAF);
            else if (series)  series(SeriesTable.SERIES_LEAF);
            else if (formats) formats();
            if (browse)       browse();
            if (help)         help();
        }
        catch (IllegalArgumentException exception)
        {
            out.println(exception.getLocalizedMessage());
        }
        if (connection!=null)
        {
            connection.close();
            connection = null;
        }
        out.flush();
    }

    /**
     * Affiche l'arborescence des séries qui se trouvent dans la base
     * de données. Cette méthode sert à vérifier le contenu de la base
     * de données ainsi que le bon fonctionnement de cette classe.
     */
    public static void main(final String[] args) throws SQLException
    {new Main(args).run();}
}
