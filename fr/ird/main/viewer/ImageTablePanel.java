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
package fr.ird.main.viewer;

// OpenGIS dependencies (SEAGIS)
import net.seagis.gc.GridCoverage;

// Database
import java.sql.SQLException;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.SeriesEntry;

// Map components
import fr.ird.layer.control.LayerControl;

// Graphical user inferface
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import fr.ird.main.InternalFrame;
import fr.ird.awt.ImageTableModel;
import fr.ird.awt.ExportChooser;
import fr.ird.awt.StatusBar;
import net.seas.awt.ColorRamp;

// Models and events
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.undo.UndoManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import fr.ird.awt.event.ImageChangeListener;
import fr.ird.awt.event.ImageChangeEvent;

// Clipboard
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.ClipboardOwner;

// Collections
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

// Miscellaneous
import java.io.File;
import java.util.Date;
import java.util.TimeZone;
import net.seas.util.XArray;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Paneau affichant une table des images disponibles, ainsi qu'une vue de ces
 * images.  L'utilisateur pourra choisir une ou plusieurs images de son choix
 * dans la table, et configurer les couches à superposer sur ces images.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ImageTablePanel extends JSplitPane
{
    /**
     * Nombre maximal d'images à afficher. En imposant un maximum,
     * on évite que l'utilisateur demande par mégarde l'affichage
     * de centaines d'images. Sans une limite, une aussi grosse
     * demande fait trop ramer l'ordinateur.
     */
    private static final int MAX_IMAGES = 64;

    /**
     * Entrées des images sélectionnées. Lorsque la sélection
     * change, la nouvelle sélection sera comparée à l'ancienne
     * pour ne lire ou supprimer que les images qui ont changées.
     */
    private ImageEntry[] selection=new ImageEntry[0];

    /**
     * Table des images spécifiée au constructeur.   Cette table contient
     * des objets {@link ImageEntry}. Les données de chaque entrée seront
     * présentées sur plusieurs colonnes (nom du fichier, date, durée...).
     */
    private final ImageTableModel table;

    /**
     * Composante visuelle qui affiche la table. Cet objet offre des méthodes
     * commodes pour obtenir les index de toutes les images sélectionnées.
     */
    private final JTable tableView;

    /**
     * Mosaïque affichant les images sélectionnées.
     */
    private final MosaicPanel mosaic;

    /**
     * Echelle de couleurs.
     */
    private final ColorRamp colorRamp=new ColorRamp();

    /**
     * Liste d'éléments {@link LayerControl} qui
     * représentent les couches que l'utilisateur
     * peut choisir et configurer.
     */
    private final LayerControl[] layers;

    /**
     * Dernier répertoire à avoir été choisit
     * pour les exportations d'images.
     */
    private transient File lastDirectory;

    /**
     * Groupe de threads dans lequel placer les différentes
     * tâches que nous allons lancer en arrière-plan.
     */
    private final ThreadGroup workers;

    /**
     * Objet à utiliser pour annuler les éditions de la table. Cet objet écoutera
     * les événements {@link javax.swing.event.UndoableEditEvent} que produira la
     * table.
     */
    private final UndoManager undoManager=new UndoManager();

    /**
     * Objet écoutant les événements d'intéret pour ce paneau.
     */
    private final Listener listeners=new Listener();

    /**
     * Construit une représentation visuelle d'une table d'images.
     * Cette représentation contiendra la table, les images et un
     * contrôleur permettant de configurer les couches.
     *
     * @param table     Table d'images à afficher.
     * @param renderer  Objet à utiliser pour dessiner les cellules de la table.
     * @param statusBar Barre d'état qui apparaîtra dans le bas de la fenêtre.
     * @param readers   Sous-groupe de <code>threads</code>Groupe dans lequel placer
     *                  les threads qui liront des images en arrière-plan.
     * @param layers    Couches que l'utilisateur pourra placer sur les images,
     *                  ou <code>null</code> pour n'afficher que les images.
     */
    public ImageTablePanel(final ImageTableModel table, final TableCellRenderer renderer,
                           final StatusBar statusBar,   final ThreadGroup readers, final LayerControl[] layers)
    {
        super(HORIZONTAL_SPLIT);
        this.table   = table;
        this.workers = readers.getParent();
        this.layers  = layers;

        mosaic    = new MosaicPanel(statusBar, readers);
        tableView = new JTable(table);
        final Icon            propertyIcon = InternalFrame.getIcon("toolbarButtonGraphics/general/Properties16.gif");
        final ListSelectionModel selection = tableView.getSelectionModel();
        final JComponent       tableScroll = new JScrollPane(tableView);
        final JComponent        layerPanel = LayerControl.getPanel(layers, propertyIcon);
        final JSplitPane         controler = new JSplitPane(VERTICAL_SPLIT, true, tableScroll, layerPanel);
        final JPanel                images = new JPanel(new BorderLayout());
        if (layers!=null)
        {
            for (int i=0; i<layers.length; i++)
            {
                final LayerControl layer=layers[i];
                synchronized (layer)
                {
                    layer.addChangeListener      (listeners);
                    layer.addUndoableEditListener(undoManager);
                }
            }
        }

        images.add(mosaic,    BorderLayout.CENTER);
        images.add(colorRamp, BorderLayout.SOUTH );

        controler.setResizeWeight(1); // La composante du haut reçoit tout l'espace.
        controler.setOneTouchExpandable(true);

        tableScroll.setMinimumSize  (new Dimension(260,100));
        controler  .setMinimumSize  (new Dimension(260, 80));
        mosaic     .setMinimumSize  (new Dimension(100,100));

        setLeftComponent     (controler);
        setRightComponent    (images);
        setResizeWeight      (0); // La composante de droite reçoit tout l'espace.
        setOneTouchExpandable(true);
        setDividerLocation   (260);
        undoManager.setLimit (20);

        tableView.setColumnSelectionAllowed(false);
        tableView.setDefaultRenderer       (String.class, renderer);
        tableView.setDefaultRenderer       (  Date.class, renderer);
        selection.addListSelectionListener (listeners);
        table    .addTableModelListener    (listeners);
        table    .addUndoableEditListener  (undoManager);
        this     .addComponentListener     (listeners);

        updateStatusBar();
    }

    /**
     * Met à jour le texte de la barre d'état.
     */
    private void updateStatusBar()
    {
        if (isShowing()) // Si c'est un autre paneau qui est visible,
        {                // alors la barre d'état ne nous appartient pas.
            mosaic.statusBar.setText(Resources.format(ResourceKeys.IMAGES_COUNT_$2,
                                     new Integer(table.getRowCount()),
                                     new Integer(tableView.getSelectedRowCount())));
            fireStateChanged();
        }
    }

    /**
     * Reset divider locations.
     */
    final void resetDividerLocation()
    {
        ((JSplitPane) getLeftComponent()).setDividerLocation(-1);
        setDividerLocation(-1);
    }

    /**
     * Indique si cette fenêtre peut traiter l'opération désignée par le code spécifié.
     * Le code <code>clé</code> désigne une des constantes de l'interface {@link ResourceKeys},
     * utilisé pour la construction des menus. Par exemple le code {@link ResourceKeys#EXPORT}
     * désigne le menu "exporter".
     */
    protected boolean canProcess(final int clé)
    {
        switch (clé)
        {
            case ResourceKeys.UNDO:             return undoManager.canUndo();
            case ResourceKeys.REDO:             return undoManager.canRedo();
            case ResourceKeys.COPY:             // fall through
            case ResourceKeys.DELETE:           // fall through
            case ResourceKeys.INVERT_SELECTION: return tableView.getSelectedRowCount()!=0;
            case ResourceKeys.SELECT_ALL:       // fall through
            case ResourceKeys.EXPORT:           return table.getRowCount()!=0;
            case ResourceKeys.RESET_VIEW:       return mosaic.getImageCount()!=0;
            case ResourceKeys.DEBUG:            return true;
            default:                            return false;
        }
    }

    /**
     * Exécute une action. Le code <code>clé</code> de l'action est le même
     * que celui qui avait été préalablement transmis à {@link #canProcess}.
     * Si le code de l'action n'est pas reconnue, une exception sera lancée.
     *
     * @return <code>true</code> si l'opération a été faite.
     * @throws SQLException si une interrogation de la base de données était
     *         nécessaire et a échouée.
     */
    protected boolean process(final int clé) throws SQLException
    {
        switch (clé)
        {
            ///////////////////////////
            ///  Fichier - Exporter ///
            ///////////////////////////
            case ResourceKeys.EXPORT:
            {
                final ExportChooser chooser=new ExportChooser(lastDirectory);
                chooser.addEntries(table.getEntries());
                chooser.showDialogAndStart(this, workers);
                lastDirectory=chooser.getDestinationDirectory();
                return true;
            }
            //////////////////////////
            ///  Edition - Annuler ///
            //////////////////////////
            case ResourceKeys.UNDO:
            {
                final int[] IDs=table.getEntryIDs();
                undoManager.undo(); // Change les index des images 'IDs'.
                if (IDs.length!=0)
                    inverseSelect(table.indexOf(IDs));
                return true;
            }
            //////////////////////////
            ///  Edition - Refaire ///
            //////////////////////////
            case ResourceKeys.REDO:
            {
                final int[] IDs=table.getEntryIDs();
                undoManager.redo(); // Change les index des images 'IDs'.
                if (IDs.length!=0)
                    inverseSelect(table.indexOf(IDs));
                return true;
            }
            /////////////////////////
            ///  Edition - Copier ///
            /////////////////////////
            case ResourceKeys.COPY:
            {
                final Transferable content=table.copy(tableView.getSelectedRows());
                getToolkit().getSystemClipboard().setContents(content, listeners);
                return true;
            }
            ////////////////////////////
            ///  Edition - Supprimer ///
            ////////////////////////////
            case ResourceKeys.DELETE:
            {
                table.remove(tableView.getSelectedRows());
                selection=new ImageEntry[0];
                mosaic.removeAllImages();
                updateStatusBar();
                return true;
            }
            ////////////////////////////////////
            ///  Edition - Sélectionner tout ///
            ////////////////////////////////////
            case ResourceKeys.SELECT_ALL:
            {
                tableView.selectAll();
                return true;
            }
            ////////////////////////////////////////
            ///  Edition - Inverser la sélection ///
            ////////////////////////////////////////
            case ResourceKeys.INVERT_SELECTION:
            {
                final int[] selected=tableView.getSelectedRows();
                tableView.clearSelection();
                inverseSelect(selected);
                return true;
            }
            //////////////////////////
            ///  Rétablir le zoom  ///
            //////////////////////////
            case ResourceKeys.RESET_VIEW:
            {
                mosaic.reset();
                return true;
            }
        }
        return false;
    }

    /**
     * Sélectionne toutes les lignes sauf les lignes <code>rows</code>. Cette méthode
     * peut servir par exemple à sélectionner les nouvelles images après avoir annulé
     * une supression.
     *
     * @param rows Numéro de lignes des images dont on ne veut pas changer la sélection.
     */
    private void inverseSelect(final int[] rows)
    {
        Arrays.sort(rows);
        final ListSelectionModel model = tableView.getSelectionModel();
        model.setValueIsAdjusting(true);

        int lower=0;
        for (int i=0; i<rows.length; i++)
        {
            final int upper = rows[i];
            if (upper > lower)
                model.addSelectionInterval(lower, upper-1);
            lower = upper+1;
        }
        final int upper = table.getRowCount();
        if (upper > lower)
            model.addSelectionInterval(lower, upper-1);

        model.setValueIsAdjusting(false);
    }

    /**
     * Méthode appelée automatiquement lorsque l'utilisateur sélectionne
     * ou désélectionne des images. Cette méthode doit être appelée dans
     * le thread de <i>Swing</i>. Elle va toutefois démarrer les lectures
     * des images dans des threads en arrière-plan.
     *
     * @param selection Images sélectionnées.
     */
    private void imageSelected(final ImageEntry[] selection)
    {
        synchronized (getTreeLock())
        {
            final LayerControl[] selectedLayers = (layers!=null) ? getSelectedLayers() : null;
            final ImagePanel[] images=new ImagePanel[selection.length];
            /*
             * Recherche les images qui existaient déjà. Ces images
             * seront placées directement dans 'images', sans faire
             * de nouvelles lectures inutiles.
             */
            int i=this.selection.length;
            final Map<ImageEntry,ImagePanel> old=new HashMap<ImageEntry,ImagePanel>(Math.max(2*i, 11));
            while (--i>=0)
            {
                old.put(this.selection[i], mosaic.getImage(i));
            }
            for (i=selection.length; --i>=0;)
            {
                images[i] = old.remove(selection[i]);
            }
            /*
             * Procède maintenant à la lecture des images qui n'étaient
             * pas déjà en mémoire. On tentera de réutiliser les objets
             * 'ImagePanel' qui étaient en trop.
             */
            this.selection=selection;
            final Iterator<ImagePanel> it=old.values().iterator();
            for (i=0; i<selection.length; i++)
            {
                if (images[i]==null)
                {
                    final ImageEntry entry = selection[i];
                    final ImagePanel panel;
                    if (it.hasNext())
                    {
                        panel = it.next();
                    }
                    else
                    {
                        panel = new ImagePanel();
                        panel.addImageChangeListener(listeners);
                    }
                    images[i]=panel;
                    panel.setImage(entry, selectedLayers, mosaic.statusBar.getIIOReadProgressListener(entry.getName()));
                }
            }
            /*
             * Détruit tous les 'imagePanel' en trop, puis affiche
             * les nouvelles images. On synchronisera au passage
             * le zoom des fenêtres et la visibilité des barres de
             * défilements.
             */
            while (it.hasNext())
            {
                final ImagePanel panel = it.next();
                panel.removeImageChangeListener(listeners);
                panel.dispose();
            }
            mosaic.setImages(images);
            mosaic.setScrollBarsVisible(images.length<2);
            fireStateChanged();
        }
    }

    /**
     * Méthode appelée automatiquement lorsque les images ont changées.
     * Cette méthode n'est appelée que dans le thread de <i>Swing</i>.
     * L'implémentation par défaut met à jour l'échelle des couleurs.
     *
     * @param images Liste des images présentement affichées.
     */
    private void imageChanged(final GridCoverage[] images)
    {
        if (images.length!=0)
        {
            colorRamp.setColorRamp(images[0]);
            for (int i=1; i<images.length; i++)
            {
                if (colorRamp.setColorRamp(images[i]))
                {
                    colorRamp.setColorRamp((GridCoverage) null);
                    break;
                }
            }
        }
    }

    /**
     * Retourne <u>une copie</u> de la table des images.
     * Cette copie pourra être enregistrée en binaire
     * (<i>serialized</i>).
     */
    public ImageTableModel getModel()
    {return new ImageTableModel(table);}

    /**
     * Retourne les couches sélectionnées par l'utilisateur. Le tableau
     * retourné peut avoir une longueur de 0, mais ne sera jamais nul.
     */
    public LayerControl[] getSelectedLayers()
    {
        final int length = (layers!=null) ? layers.length : 0;
        final LayerControl[] selected=new LayerControl[length];
        int count=0;
        for (int i=0; i<length; i++)
            if (layers[i].isSelected())
                selected[count++]=layers[i];
        return XArray.resize(selected, count);
    }

    /**
     * Retourne les entrés des images sélectionnées par l'utilisateur.
     * Le tableau retourné peut avoir une longueur de 0, mais ne sera
     * jamais <code>null</code>.
     */
    public ImageEntry[] getSelectedEntries()
    {
        final int[]             rows = tableView.getSelectedRows();
        final ImageEntry[] selection = new ImageEntry[rows.length];
        for (int i=0; i<rows.length; i++)
            selection[i]=table.getEntryAt(rows[i]);
        return selection;
    }

    /**
     * Remplace tous les enregistrements courants par ceux de la table <code>table</code>.
     * La série de <code>table</code> (telle que retournée par {@link ImageTable#getSeries})
     * deviendra la série courante, celle que retourne {@link #getSeries}. Cette méthode
     * peut être appelée de n'importe quel thread (pas nécessairement celui de <i>Swing</i>).
     *
     * @param  table Table dans laquelle puiser la liste des images.
     * @throws SQLException si l'interrogation de la table a échouée.
     */
    public void setEntries(final ImageTable table) throws SQLException
    {this.table.setEntries(table);}

    /**
     * Retourne la série d'images représentée par cette table. Si la série n'est
     * pas connue ou si cette table contient des images de plusieurs séries
     * différentes, alors cette méthode peut retourner <code>null</code>.
     */
    public SeriesEntry getSeries()
    {return table.getSeries();}

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des éventuelles
     * bases de données accédées par cette fenêtre.
     */
    protected void setTimeZone(final TimeZone timezone)
    {table.setTimeZone(timezone);}

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqué sur une image d'une
     * mosaïque doit être répliqué sur les autres.
     */
    protected void setImagesSynchronized(final boolean s)
    {mosaic.setImagesSynchronized(s);}

    /**
     * Spécifie si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. Spécifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    protected void setPaintingWhileAdjusting(final boolean s)
    {
        mosaic.setPaintingWhileAdjusting(s);
        setContinuousLayout(s);
    }

    /**
     * Ajoute un objet à la liste des objets intéressés à être informés des
     * changements. Les "changements" peuvent couvrir une assez grande gamme:
     * changement du contenu de la table ou de la sélection de l'utilisateur,
     * apparation de la fenêtre alors qu'elle était auparavant invisible, etc.
     */
    public void addChangeListener(final ChangeListener listener)
    {listenerList.add(ChangeListener.class, listener);}

    /**
     * Retire un objet de la liste des objets intéressés à être informés des
     * changements. Les "changements" peuvent couvrir une assez grande gamme:
     * changement du contenu de la table ou de la sélection de l'utilisateur,
     * apparation de la fenêtre alors qu'elle était auparavant invisible, etc.
     */
    public void removeChangeListener(final ChangeListener listener)
    {listenerList.remove(ChangeListener.class, listener);}

    /**
     * Signale tous les objets intéressés qu'un changement est survenu
     * dans cette table. Les changements signalés sont les changements
     * dans le contenu de la table,  dans les images sélectionnées par
     * l'utilisateur ou dans la visibilité de la table par exemple.
     */
    private void fireStateChanged()
    {
        ChangeEvent event=null;
        final Object[] listeners=listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;)
        {
            if (listeners[i]==ChangeListener.class)
            {
                if (event==null) event=new ChangeEvent(this);
                ((ChangeListener) listeners[i+1]).stateChanged(event);
            }
        }
    }

    /**
     * Libère les ressources utilisées par cette fenêtre.
     * Cette méthode est appelée automatiquement lorsque
     * la fenêtre est fermée.
     *
     * @throws SQLException si un accès à une base de
     *         données était nécessaire et a échoué.
     */
    public synchronized void dispose() throws SQLException
    {
        mosaic.dispose();
        for (int i=layers.length; --i>=0;)
            layers[i].dispose();
    }

    /**
     * Classe de l'objet qui écoutera les événements d'intéret pour ce paneau.
     * On s'intéresse aux changements des données dans la table d'images, aux
     * changements de la sélection de l'utilisateur ainsi qu'aux changements
     * de la visibilité du paneau.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Listener extends ComponentAdapter implements TableModelListener, ListSelectionListener, ChangeListener, ImageChangeListener, ClipboardOwner
    {
        /**
         * Méthode appelée automatiquement chaque fois
         * que l'onglet de ce paneau est sélectionné.
         */
        public void componentShown(final ComponentEvent event)
        {updateStatusBar();}

        /**
         * Méthode appelée automatiquement chaque
         * fois que le contenu du tableau change.
         */
        public void tableChanged(final TableModelEvent event)
        {updateStatusBar();}

        /**
         * Méthode appelée automatiquement chaque fois que change
         * la sélection de l'utilisateur dans le tableau.
         */
        public void valueChanged(final ListSelectionEvent event)
        {
            if (!event.getValueIsAdjusting())
            {
                ImageEntry[] entries=getSelectedEntries();
                if (entries.length>MAX_IMAGES)
                    entries = XArray.resize(entries, MAX_IMAGES);
                imageSelected(entries);
            }
            updateStatusBar();
        }

        /**
         * Méthode appelée automatiquement lorsque la configuration
         * d'au moins une couche a changée. Cette méthode recharge
         * les images afin de prendre en compte le changement.
         */
        public void stateChanged(final ChangeEvent event)
        {
            mosaic.reload((layers!=null) ? getSelectedLayers() : null);
            fireStateChanged();
        }

        /**
         * Méthode appelée automatiquement chaque fois qu'une des images affichées a changée.
         * Cette méthode n'est appelée qu'après que le chargement ait été complétée.
         */
        public void imageChanged(final ImageChangeEvent event)
        {
            synchronized (ImageTablePanel.this)
            {ImageTablePanel.this.imageChanged(mosaic.getGridCoverages());}
        }

        /**
         * Méthode appelée automatiquement lorsque cette table
         * ne possède plus le contenu du presse-papier.
         */
        public void lostOwnership(final Clipboard clipboard, final Transferable contents)
        {
        }
    }
}
