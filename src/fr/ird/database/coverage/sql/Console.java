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
 */
package fr.ird.database.coverage.sql;

// J2SE dependencies
import java.util.List;
import java.util.Date;
import java.io.IOException;
import java.sql.SQLException;

// Swing dependencies
import javax.swing.JTree;
import javax.swing.JTable;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.table.TableCellRenderer;

// Geotools
import org.geotools.resources.Arguments;
import org.geotools.gui.swing.tree.Trees;
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageTableModel;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Fournit des outils de lignes de commande pour l'administration
 * de la base de données d'images. Les options reconnues sont:
 * Cette classe peut être exécutée à partir de la ligne de commande:
 *
 * <blockquote><pre>
 * java fr.ird.database.coverage.sql.Console <var>options</var>
 * </pre></blockquote>
 *
 * <P>Lorsque cette classe est exécutée avec l'argument <code>-config</code>, elle
 * fait apparaître une boite de dialogue  permettant de configurer les requêtes
 * SQL utilisées par la base de données. Les requêtes modifiées seront sauvegardées
 * dans un fichier de configuration.</P>
 *
 * <P>Cette méthode peut aussi être utilisée pour afficher des informations vers le
 * périphérique de sortie standard. Les arguments valides sont:</P>
 *
 * <blockquote><pre>
 *  <b>-help</b> <i></i>         Affiche cette liste des options
 *  <b>-series</b> <i></i>       Affiche l'arborescence des séries
 *  <b>-formats</b> <i></i>      Affiche la table des formats
 *  <b>-config</b> <i></i>       Configure la base de données (interface graphique)
 *  <b>-browse</b> <i></i>       Affiche le contenu de toute la base de données (interface graphique)
 *  <b>-locale</b> <i>name</i>   Langue et conventions d'affichage (exemple: "fr_CA")
 *  <b>-encoding</b> <i>name</i> Page de code pour les sorties     (exemple: "cp850")
 *  <b>-Xout</b> <i>filename</i> Fichier de destination (le périphérique standard par défaut)
 * </pre></blockquote>
 *
 * L'argument <code>-encoding</code> est surtout utile lorsque ce programme est lancé
 * à partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la même page
 * de code que le reste du système Windows. Il est alors nécessaire de préciser la
 * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
 * caractères étendus. La page de code en cours peut être obtenu en tappant
 * <code>chcp</code> sur la ligne de commande.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Console extends Arguments {
    /**
     * Connexion vers la base de données. Cette connexion
     * ne sera établie que la première fois où elle sera
     * nécessaire.
     */
    private transient CoverageDataBase database;

    /**
     * Construit une console.
     *
     * @param args Arguments transmis sur la ligne de commande.
     */
    private Console(final String[] args) {
        super(args);
    }

    /**
     * Retourne la connexion vers la base de données.
     *
     * @throws IOException si la connexion n'a pas pu être établie.
     */
    private CoverageDataBase getDataBase() throws IOException {
        if (database == null) try {
            database = new CoverageDataBase();
        } catch (SQLException exception) {
            throw new CatalogException(exception);
        }
        return database;
    }

    /**
     * Print help instructions.
     */
    private void help() {
        out.println();
        out.println("Outils de ligne de commande pour la base de données d'images\n"+
                    "1999-2002, Institut de Recherche pour le Développement\n"+
                    "\n"+
                    "Options disponibles:\n"+
                    "  -help            Affiche cette liste des options\n"+
                    "  -series          Affiche l'arborescence des séries\n"+
                    "  -subseries       Affiche l'arborescence des sous-séries\n"+
                    "  -formats         Affiche la table des formats\n"+
                    "  -config          Configure la base de données (interface graphique)\n"+
                    "  -browse          Affiche le contenu de toute la base de données (interface graphique)\n"+
                    "  -locale <name>   Langue et conventions d'affichage (exemple: \"fr_CA\")\n"+
                    "  -encoding <name> Page de code pour les sorties     (exemple: \"cp850\")\n"+
                    "  -Xout <filename> Fichier de destination (le périphérique standard par défaut)");
    }

    /**
     * Affiche l'arborescence des séries  qui se trouvent dans la base
     * de données. Cette méthode sert à vérifier le contenu de la base
     * de données  ainsi que le bon fonctionnement de l'implémentation
     * de {@link SeriesTable}.
     */
    private void series(final int leafType) throws IOException {
        final SeriesTable series = getDataBase().getSeriesTable();
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
    private void formats() throws IOException {
        final FormatTable formatTable = getDataBase().getFormatTable();
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(
              Resources.getResources(locale).getString(ResourceKeys.FORMATS));
        final List<FormatEntry> formats;
        try {
            formats = formatTable.getEntries();
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }
        for (final FormatEntry entry : formats) {
            root.add(entry.getTree(locale));
        }
        formatTable.close();
        out.println();
        out.println(Trees.toString(new DefaultTreeModel(root)));
        out.flush();
    }

    /**
     * Affiche dans une fenêtre <i>Swing</i>
     * le contenu de toute la base de données.
     */
    private void browse() throws IOException {
        final CoverageDataBase   database = getDataBase();
        final SeriesTable     seriesTable = database.getSeriesTable();
        final TreeModel         treeModel = seriesTable.getTree(SeriesTable.CATEGORY_LEAF);
        final JComponent         treePane = new JScrollPane(new JTree(treeModel));
        final JTabbedPane      tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        final JSplitPane        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, treePane, tabbedPane);
        final TableCellRenderer  renderer = new CoverageTableModel.CellRenderer();
        final CoverageTable coverageTable = database.getCoverageTable();
        for (final SeriesEntry currentSeries : seriesTable.getEntries()) {
            coverageTable.setSeries(currentSeries);
            final JTable table = new JTable(new CoverageTableModel(coverageTable));
            table.setDefaultRenderer(String.class, renderer);
            table.setDefaultRenderer(  Date.class, renderer);
            tabbedPane.addTab(currentSeries.getName(), new JScrollPane(table));
        }
        coverageTable.close();
        seriesTable.close();

        final JFrame frame = new JFrame(Resources.format(ResourceKeys.DATABASE));
        frame.setContentPane(splitPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Exécute tout.
     */
    private void run() throws IOException {
        final boolean    config = getFlag("-config");
        final boolean   formats = getFlag("-formats");
        final boolean subseries = getFlag("-subseries");
        final boolean    series = getFlag("-series") || subseries;
        final boolean    browse = getFlag("-browse");
        final boolean      help = getFlag("-help") || (!series && !formats && !config && !browse);

        boolean exit = true;
        getRemainingArguments(0);
        if (config) {
            getDataBase().getSQLEditor().showDialog(null);
            exit = true;
        }
        if (formats && series) series(SeriesTable.CATEGORY_LEAF);
        else if (subseries)    series(SeriesTable.SUBSERIES_LEAF);
        else if (series)       series(SeriesTable.SERIES_LEAF);
        else if (formats)      formats();
        if (browse)           {browse(); exit=false;}
        if (help)              help();
        if (database != null) {
            database.close();
            database = null;
        }
        out.flush();
        if (exit) {
            System.exit(0);
        }
    }

    /**
     * Affiche l'arborescence des séries qui se trouvent dans la base
     * de données. Cette méthode sert à vérifier le contenu de la base
     * de données ainsi que le bon fonctionnement du paquet.
     */
    public static void main(final String[] args) throws IOException {
        org.geotools.util.MonolineFormatter.init("fr.ird");
        new Console(args).run();
    }
}
