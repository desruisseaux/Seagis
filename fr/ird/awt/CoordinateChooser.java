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
package fr.ird.awt;

// Base de données
import java.sql.SQLException;
import fr.ird.image.sql.DataBase;
import fr.ird.image.sql.SeriesEntry;
import fr.ird.image.sql.SeriesTable;

// Interface utilisateur
import java.awt.EventQueue;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;

// Arborescence
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;
import net.seas.awt.tree.TreeNode;
import net.seas.awt.tree.Trees;
import net.seas.util.SwingUtilities;

// Divers
import java.util.Date;
import net.seas.util.XArray;
import net.seas.util.XDimension2D;
import javax.media.jai.util.Range;
import fr.ird.resources.Resources;


/**
 * Boîte de dialogue invitant l'utilisateur à sélectionner une plage de dates et de coordonnées.
 * Ces informations peuvent servir à configurer une table {@link fr.ird.image.sql.ImageTable}
 * en fonction des choix de l'utilisateur.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class CoordinateChooser extends net.seas.awt.CoordinateChooser
{
    /**
     * Résolution par défaut proposée à l'utilisateur,
     * en degrés de longitude et de latitude.
     */
    private static final double DEFAULT_RESOLUTION = 6./60;

    /**
     * Arborescence des séries.
     */
    private final TreeModel treeModel;

    /**
     * Objet indiquant quelle branche de l'arborescence
     * (quelle série) a choisit l'utilisateur.
     */
    private final TreeSelectionModel treeSelection;

    /**
     * Composante qui affiche l'arborescence des séries.
     */
    private final JComponent treeView;

    /**
     * Construit un panneau qui permettra à l'utilisateur de sélectionner
     * des coordonnées géographiques, une plage de temps ainsi qu'une ou
     * plusieurs série d'images.
     *
     * @param  database Connection vers la base de données d'images. Cette
     *         connection ne sera utilisée que le temps de la construction.
     *         Aucune référence ne sera retenue après la construction.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public CoordinateChooser(final DataBase database) throws SQLException
    {this(database, database.getTimeRange());}

    /**
     * Constructeur privé.
     */
    private CoordinateChooser(final DataBase database, final Range timeRange) throws SQLException
    {
        super((Date)timeRange.getMinValue(), (Date)timeRange.getMaxValue());
        setGeographicArea(database.getGeographicArea());
        setPreferredResolution(new XDimension2D.Double(DEFAULT_RESOLUTION, DEFAULT_RESOLUTION));
        setPreferredResolution(null); // Disable after setting it to default value.

        treeModel = database.getSeriesTable().getTree(SeriesTable.SERIES_LEAF);
        final JTree tree = new JTree(treeModel);
        tree.setExpandsSelectedPaths(true);
        treeSelection = tree.getSelectionModel();
        treeSelection.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        treeView = new JScrollPane(tree);
        treeView.setBorder(BorderFactory.createCompoundBorder(
                           BorderFactory.createLoweredBevelBorder(), treeView.getBorder()));
        treeView.setPreferredSize(new Dimension(300,200));
        setAccessory(treeView);
    }

    /**
     * Retourne les séries sélectionnées par l'utilisateur. Si aucune série n'a
     * été sélectionnée, alors cette méthode retourne un tableau de longueur 0.
     */
    public SeriesEntry[] getSeries()
    {
        final TreePath paths[]=treeSelection.getSelectionPaths();
        if (paths!=null)
        {
            int count=0;
            final SeriesEntry[] series = new SeriesEntry[paths.length];
            for (int i=0; i<paths.length; i++)
            {
                final Object node=((TreeNode) paths[i].getLastPathComponent()).getUserObject();
                if (node instanceof SeriesEntry)
                {
                    series[count++] = (SeriesEntry) node;
                }
            }
            return XArray.resize(series, count);
        }
        return new SeriesEntry[0];
    }

    /**
     * Définit la série sélectionnée. Si la série spécifiée est nulle
     * ou n'apparaît pas dans l'arborescence, alors aucun changement
     * ne sera fait.
     */
    public void setSeries(final SeriesEntry series)
    {
        final TreePath[] paths=Trees.getPathsToUserObject(treeModel, series);
        if (paths.length==0) return;
        treeSelection.setSelectionPath(paths[0]);
    }

    /**
     * Spécifie si on autorise la sélection de plusieurs séries à la fois.
     * Par défaut, l'utilisateur ne peut sélectionner qu'une seule série à
     * la fois.
     */
    public void setMultiSeriesAllowed(final boolean allowed)
    {treeSelection.setSelectionMode(allowed ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : TreeSelectionModel.SINGLE_TREE_SELECTION);}

    /**
     * Indique si les séries doivent être affichées
     * à la droite du paneau servant à sélectionner
     * les coordonnées spatio-temporelles.
     */
    public void setSeriesVisible(final boolean visible)
    {setAccessory(visible ? treeView : null);}

    /**
     * Fait apparaître une boîte de dialogue demandant à l'utilisateur de choisir une ou des séries.
     * Cette méthode est habituellement équivalente à <code>showDialog(owner,title)</code>, excepté
     * qu'elle retourne les séries sélectionnées plutôt qu'un <code>boolean</code>. Si toutefois la
     * méthode <code>setSeriesVisible(false)</code> a été appelée, alors <code>showSeriesDialog</code>
     * n'affichera que le contrôle permettant de sélectionner les séries, indépendamment du reste des
     * contrôles qui servent à sélectionner les coordonnées spatio-temporelles (ces derniers peuvent
     * toujours être affichés avec <code>showDialog</code>).
     *
     * @param  owner Fenêtre dans laquelle faire apparaître la boîte de dialogue.
     * @param  title Titre de la boîte de dialogue.
     * @return Les séries sélectionnées si l'utilisateur a cliqué sur "Ok",
     *         ou <code>null</code> sinon.
     */
    public SeriesEntry[] showSeriesDialog(final Component owner, final String title)
    {
        /*
         * Make sure that this method is
         * invoked from the Sqing thread.
         */
        if (!EventQueue.isDispatchThread())
        {
            final SeriesEntry[][] series=new SeriesEntry[1][];
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {series[0]=showSeriesDialog(owner, title);}
            });
            return series[0];
        }
        /*
         * If series still attached to the coordinate chooser
         * (as the "accessory"), invoke the usual method.
         */
        if (treeView.getParent()!=null)
        {
            while (showDialog(owner, title))
            {
                final SeriesEntry[] series=getSeries();
                if (series.length!=0) return series;
                showMessageDialog(owner);
            }
            return null;
        }
        /*
         * If series was hidden, show series
         * in a separated dialog box.
         */
        while (SwingUtilities.showOptionDialog(owner, treeView, title))
        {
            final SeriesEntry[] series=getSeries();
            if (series.length!=0) return series;
            showMessageDialog(owner);
        }
        return null;
    }

    /**
     * Affiche un message indiquant qu'aucune série n'a été sélectionnée.
     */
    private void showMessageDialog(final Component owner)
    {
        final Resources resources = Resources.getResources(getLocale());
        final String    message   = resources.getString(Clé.NO_SERIES_SELECTION);
        final String    title     = resources.getString(Clé.BAD_ENTRY);
        SwingUtilities.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Fait apparaître la boîte de dialogue et termine le programme.
     * Cette méthode ne sert qu'à tester l'apparence de la boîte.
     */
    public static void main(final String[] args) throws SQLException
    {
        new CoordinateChooser(new DataBase()).showSeriesDialog(null,null);
        System.exit(0);
    }
}
