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
package fr.ird.database.gui.swing;

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

// Divers
import java.util.Date;
import java.sql.SQLException;
import javax.media.jai.util.Range;
import java.rmi.RemoteException;

// Arborescence
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;

// Geotools
import org.geotools.resources.geometry.XDimension2D;
import org.geotools.resources.SwingUtilities;
import org.geotools.gui.swing.tree.TreeNode;
import org.geotools.gui.swing.tree.Trees;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.XArray;


/**
 * Bo�te de dialogue invitant l'utilisateur � s�lectionner une plage de dates et de coordonn�es.
 * Ces informations peuvent servir � configurer une table
 * {@link fr.ird.database.coverage.CoverageTable} en fonction des choix de l'utilisateur.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CoordinateChooser extends org.geotools.gui.swing.CoordinateChooser {
    /**
     * R�solution par d�faut propos�e � l'utilisateur,
     * en degr�s de longitude et de latitude.
     */
    private static final double DEFAULT_RESOLUTION = 6./60;

    /**
     * Arborescence des s�ries.
     */
    private final TreeModel treeModel;

    /**
     * Objet indiquant quelle branche de l'arborescence
     * (quelle s�rie) a choisit l'utilisateur.
     */
    private final TreeSelectionModel treeSelection;

    /**
     * Composante qui affiche l'arborescence des s�ries.
     */
    private final JComponent treeView;

    /**
     * Construit un panneau qui permettra � l'utilisateur de s�lectionner
     * des coordonn�es g�ographiques, une plage de temps ainsi qu'une ou
     * plusieurs s�rie d'images.
     *
     * @param  database Connection vers la base de donn�es d'images. Cette
     *         connection ne sera utilis�e que le temps de la construction.
     *         Aucune r�f�rence ne sera retenue apr�s la construction.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public CoordinateChooser(final CoverageDataBase database) throws RemoteException {
        this(database, database.getTimeRange());
    }

    /**
     * Constructeur priv�.
     */
    private CoordinateChooser(final CoverageDataBase database, final Range timeRange) throws RemoteException {
        super((Date)timeRange.getMinValue(), (Date)timeRange.getMaxValue());
        setGeographicArea((java.awt.geom.Rectangle2D)database.getGeographicArea());
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
     * Retourne les s�ries s�lectionn�es par l'utilisateur. Si aucune s�rie n'a
     * �t� s�lectionn�e, alors cette m�thode retourne un tableau de longueur 0.
     */
    public SeriesEntry[] getSeries() {
        final TreePath paths[] = treeSelection.getSelectionPaths();
        if (paths != null) {
            int count=0;
            final SeriesEntry[] series = new SeriesEntry[paths.length];
            for (int i=0; i<paths.length; i++) {
                final Object node = ((TreeNode) paths[i].getLastPathComponent()).getUserObject();
                if (node instanceof SeriesEntry) {
                    series[count++] = (SeriesEntry) node;
                }
            }
            return XArray.resize(series, count);
        }
        return new SeriesEntry[0];
    }

    /**
     * D�finit la s�rie s�lectionn�e. Si la s�rie sp�cifi�e est nulle
     * ou n'appara�t pas dans l'arborescence, alors aucun changement
     * ne sera fait.
     */
    public void setSeries(final SeriesEntry series) {
        final TreePath[] paths = Trees.getPathsToUserObject(treeModel, series);
        if (paths.length==0) {
            return;
        }
        treeSelection.setSelectionPath(paths[0]);
    }

    /**
     * Sp�cifie si on autorise la s�lection de plusieurs s�ries � la fois.
     * Par d�faut, l'utilisateur ne peut s�lectionner qu'une seule s�rie �
     * la fois.
     */
    public void setMultiSeriesAllowed(final boolean allowed) {
        treeSelection.setSelectionMode(allowed ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
                                               : TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    /**
     * Indique si les s�ries doivent �tre affich�es
     * � la droite du paneau servant � s�lectionner
     * les coordonn�es spatio-temporelles.
     */
    public void setSeriesVisible(final boolean visible) {
        setAccessory(visible ? treeView : null);
    }

    /**
     * Fait appara�tre une bo�te de dialogue demandant � l'utilisateur de choisir une ou des s�ries.
     * Cette m�thode est habituellement �quivalente � <code>showDialog(owner,title)</code>, except�
     * qu'elle retourne les s�ries s�lectionn�es plut�t qu'un <code>boolean</code>. Si toutefois la
     * m�thode <code>setSeriesVisible(false)</code> a �t� appel�e, alors <code>showSeriesDialog</code>
     * n'affichera que le contr�le permettant de s�lectionner les s�ries, ind�pendamment du reste des
     * contr�les qui servent � s�lectionner les coordonn�es spatio-temporelles (ces derniers peuvent
     * toujours �tre affich�s avec <code>showDialog</code>).
     *
     * @param  owner Fen�tre dans laquelle faire appara�tre la bo�te de dialogue.
     * @param  title Titre de la bo�te de dialogue.
     * @return Les s�ries s�lectionn�es si l'utilisateur a cliqu� sur "Ok",
     *         ou <code>null</code> sinon.
     */
    public SeriesEntry[] showSeriesDialog(final Component owner, final String title) {
        /*
         * Make sure that this method is
         * invoked from the Sqing thread.
         */
        if (!EventQueue.isDispatchThread()) {
            final SeriesEntry[][] series = new SeriesEntry[1][];
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    series[0]=showSeriesDialog(owner, title);
                }
            });
            return series[0];
        }
        /*
         * If series still attached to the coordinate chooser
         * (as the "accessory"), invoke the usual method.
         */
        if (getAccessory() == treeView) {
            while (showDialog(owner, title)) {
                final SeriesEntry[] series = getSeries();
                if (series.length!=0) {
                    return series;
                }
                showMessageDialog(owner);
            }
            return null;
        }
        /*
         * If series was hidden, show series
         * in a separated dialog box.
         */
        while (SwingUtilities.showOptionDialog(owner, treeView, title)) {
            final SeriesEntry[] series = getSeries();
            if (series.length!=0) {
                return series;
            }
            showMessageDialog(owner);
        }
        return null;
    }

    /**
     * Affiche un message indiquant qu'aucune s�rie n'a �t� s�lectionn�e.
     */
    private void showMessageDialog(final Component owner) {
        final Resources resources = Resources.getResources(getLocale());
        final String    message   = resources.getString(ResourceKeys.ERROR_NO_SERIES_SELECTION);
        final String    title     = resources.getString(ResourceKeys.ERROR_BAD_ENTRY);
        SwingUtilities.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
