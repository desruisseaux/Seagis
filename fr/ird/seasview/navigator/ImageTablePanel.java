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
package fr.ird.seasview.navigator;

// Geotools dependencies
import org.geotools.gc.GridCoverage;

// Database
import java.sql.SQLException;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.SeriesEntry;

// Map components
import fr.ird.seasview.layer.control.LayerControl;

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
import fr.ird.seasview.InternalFrame;
import fr.ird.awt.ImageTableModel;
import fr.ird.awt.ExportChooser;
import fr.ird.awt.StatusBar;
import fr.ird.awt.ColorRamp;

// Models and events
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
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

// Miscellaneous
import java.io.File;
import java.util.Date;
import java.util.TimeZone;
import fr.ird.util.XArray;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Paneau affichant une table des images disponibles, ainsi qu'une vue de ces
 * images.  L'utilisateur pourra choisir une ou plusieurs images de son choix
 * dans la table, et configurer les couches � superposer sur ces images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageTablePanel extends ImagePanel
{
    /**
     * Nombre maximal d'images � afficher. En imposant un maximum,
     * on �vite que l'utilisateur demande par m�garde l'affichage
     * de centaines d'images. Sans une limite, une aussi grosse
     * demande fait trop ramer l'ordinateur.
     */
    private static final int MAX_IMAGES = 64;

    /**
     * Entr�es des images s�lectionn�es. Lorsque la s�lection
     * change, la nouvelle s�lection sera compar�e � l'ancienne
     * pour ne lire ou supprimer que les images qui ont chang�es.
     */
    private ImageEntry[] selection=new ImageEntry[0];

    /**
     * Table des images sp�cifi�e au constructeur.   Cette table contient
     * des objets {@link ImageEntry}. Les donn�es de chaque entr�e seront
     * pr�sent�es sur plusieurs colonnes (nom du fichier, date, dur�e...).
     */
    private final ImageTableModel table;

    /**
     * Composante visuelle qui affiche la table. Cet objet offre des m�thodes
     * commodes pour obtenir les index de toutes les images s�lectionn�es.
     */
    private final JTable tableView;

    /**
     * Echelle de couleurs.
     */
    private final ColorRamp colorRamp=new ColorRamp();

    /**
     * Dernier r�pertoire � avoir �t� choisit
     * pour les exportations d'images.
     */
    private transient File lastDirectory;

    /**
     * Groupe de threads dans lequel placer les diff�rentes
     * t�ches que nous allons lancer en arri�re-plan.
     */
    private final ThreadGroup workers;

    /**
     * Objet �coutant les �v�nements d'int�ret pour ce panneau.
     */
    private final Listener listeners=new Listener();

    /**
     * Construit une repr�sentation visuelle d'une table d'images.
     * Cette repr�sentation contiendra la table, les images et un
     * contr�leur permettant de configurer les couches.
     *
     * @param table     Table d'images � afficher.
     * @param renderer  Objet � utiliser pour dessiner les cellules de la table.
     * @param statusBar Barre d'�tat qui appara�tra dans le bas de la fen�tre.
     * @param readers   Sous-groupe de <code>threads</code>Groupe dans lequel placer
     *                  les threads qui liront des images en arri�re-plan.
     * @param layers    Couches que l'utilisateur pourra placer sur les images,
     *                  ou <code>null</code> pour n'afficher que les images.
     */
    public ImageTablePanel(final ImageTableModel table, final TableCellRenderer renderer,
                           final StatusBar statusBar,   final ThreadGroup readers, final LayerControl[] layers)
    {
        super(statusBar, readers, layers);
        this.table     = table;
        this.workers   = readers.getParent();
        this.tableView = new JTable(table);

        final ListSelectionModel selection = tableView.getSelectionModel();
        final JComponent       tableScroll = new JScrollPane(tableView);
        final JSplitPane         controler = (JSplitPane) getLeftComponent();
        final JPanel                images = new JPanel(new BorderLayout());

        images.add(mosaic,    BorderLayout.CENTER);
        images.add(colorRamp, BorderLayout.SOUTH );

        this       .setRightComponent(images);
        controler  .setTopComponent(tableScroll);
        tableScroll.setMinimumSize(new Dimension(260,100));
        tableView  .setColumnSelectionAllowed(false);
        tableView  .setDefaultRenderer       (String.class, renderer);
        tableView  .setDefaultRenderer       (  Date.class, renderer);
        selection  .addListSelectionListener (listeners);
        table      .addTableModelListener    (listeners);
        table      .addUndoableEditListener  (undoManager);

        updateStatusBar();
    }

    /**
     * Met � jour le texte de la barre d'�tat.
     */
    protected final void updateStatusBar()
    {
        if (isShowing()) // Si c'est un autre panneau qui est visible,
        {                // alors la barre d'�tat ne nous appartient pas.
            mosaic.statusBar.setText(Resources.format(ResourceKeys.IMAGES_COUNT_$2,
                                     new Integer(table.getRowCount()),
                                     new Integer(tableView.getSelectedRowCount())));
            fireStateChanged();
        }
    }

    /**
     * Indique si cette fen�tre peut traiter l'op�ration d�sign�e par le code sp�cifi�.
     * Le code <code>cl�</code> d�signe une des constantes de l'interface {@link ResourceKeys},
     * utilis� pour la construction des menus. Par exemple le code {@link ResourceKeys#EXPORT}
     * d�signe le menu "exporter".
     */
    protected boolean canProcess(final int cl�)
    {
        switch (cl�)
        {
            case ResourceKeys.COPY:             // fall through
            case ResourceKeys.DELETE:           // fall through
            case ResourceKeys.INVERT_SELECTION: return tableView.getSelectedRowCount()!=0;
            case ResourceKeys.SELECT_ALL:       // fall through
            case ResourceKeys.EXPORT:           return table.getRowCount()!=0;
            default:                            return super.canProcess(cl�);
        }
    }

    /**
     * Ex�cute une action. Le code <code>cl�</code> de l'action est le m�me
     * que celui qui avait �t� pr�alablement transmis � {@link #canProcess}.
     * Si le code de l'action n'est pas reconnue, une exception sera lanc�e.
     *
     * @return <code>true</code> si l'op�ration a �t� faite.
     * @throws SQLException si une interrogation de la base de donn�es �tait
     *         n�cessaire et a �chou�e.
     */
    protected boolean process(final int cl�) throws SQLException
    {
        switch (cl�)
        {
            default: return super.process(cl�);

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
                getToolkit().getSystemClipboard().setContents(content, this);
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
            ///  Edition - S�lectionner tout ///
            ////////////////////////////////////
            case ResourceKeys.SELECT_ALL:
            {
                tableView.selectAll();
                return true;
            }
            ////////////////////////////////////////
            ///  Edition - Inverser la s�lection ///
            ////////////////////////////////////////
            case ResourceKeys.INVERT_SELECTION:
            {
                final int[] selected=tableView.getSelectedRows();
                tableView.clearSelection();
                inverseSelect(selected);
                return true;
            }
        }
    }

    /**
     * S�lectionne toutes les lignes sauf les lignes <code>rows</code>. Cette m�thode
     * peut servir par exemple � s�lectionner les nouvelles images apr�s avoir annul�
     * une supression.
     *
     * @param rows Num�ro de lignes des images dont on ne veut pas changer la s�lection.
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
     * M�thode appel�e automatiquement lorsque l'utilisateur s�lectionne
     * ou d�s�lectionne des images. Cette m�thode doit �tre appel�e dans
     * le thread de <i>Swing</i>. Elle va toutefois d�marrer les lectures
     * des images dans des threads en arri�re-plan.
     *
     * @param selection Images s�lectionn�es.
     */
    private void imageSelected(final ImageEntry[] selection)
    {
        synchronized (getTreeLock())
        {
            final LayerControl[] selectedLayers = (layers!=null) ? getSelectedLayers() : null;
            final ImageCanvas[] images=new ImageCanvas[selection.length];
            /*
             * Recherche les images qui existaient d�j�. Ces images
             * seront plac�es directement dans 'images', sans faire
             * de nouvelles lectures inutiles.
             */
            int i=this.selection.length;
            final Map<ImageEntry,ImageCanvas> old=new HashMap<ImageEntry,ImageCanvas>(Math.max(2*i, 11));
            while (--i>=0)
            {
                old.put(this.selection[i], mosaic.getImage(i));
            }
            for (i=selection.length; --i>=0;)
            {
                images[i] = old.remove(selection[i]);
            }
            /*
             * Proc�de maintenant � la lecture des images qui n'�taient
             * pas d�j� en m�moire. On tentera de r�utiliser les objets
             * 'ImageCanvas' qui �taient en trop.
             */
            this.selection=selection;
            final Iterator<ImageCanvas> it=old.values().iterator();
            for (i=0; i<selection.length; i++)
            {
                if (images[i]==null)
                {
                    final ImageEntry entry = selection[i];
                    final ImageCanvas panel;
                    if (it.hasNext())
                    {
                        panel = it.next();
                    }
                    else
                    {
                        panel = new ImageCanvas();
                        panel.addImageChangeListener(listeners);
                    }
                    images[i]=panel;
                    panel.setImage(entry, selectedLayers, mosaic.statusBar.getIIOReadProgressListener(entry.getName()));
                }
            }
            /*
             * D�truit tous les 'imagePanel' en trop, puis affiche
             * les nouvelles images. On synchronisera au passage
             * le zoom des fen�tres et la visibilit� des barres de
             * d�filements.
             */
            while (it.hasNext())
            {
                final ImageCanvas panel = it.next();
                panel.removeImageChangeListener(listeners);
                panel.dispose();
            }
            mosaic.setImages(images);
            mosaic.setScrollBarsVisible(images.length<2);
            fireStateChanged();
        }
    }

    /**
     * M�thode appel�e automatiquement lorsque les images ont chang�es.
     * Cette m�thode n'est appel�e que dans le thread de <i>Swing</i>.
     * L'impl�mentation par d�faut met � jour l'�chelle des couleurs.
     *
     * @param images Liste des images pr�sentement affich�es.
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
     * Cette copie pourra �tre enregistr�e en binaire
     * (<i>serialized</i>).
     */
    public ImageTableModel getModel()
    {return new ImageTableModel(table);}

    /**
     * Retourne les entr�s des images s�lectionn�es par l'utilisateur.
     * Le tableau retourn� peut avoir une longueur de 0, mais ne sera
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
     * Remplace tous les enregistrements courants par ceux de la liste
     * sp�cifi�e. Cette m�thode peut �tre appel�e de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     */
    public void setEntries(final List<ImageEntry> entries)
    {this.table.setEntries(entries);}

    /**
     * Remplace tous les enregistrements courants par ceux de la table <code>table</code>.
     * La s�rie de <code>table</code> (telle que retourn�e par {@link ImageTable#getSeries})
     * deviendra la s�rie courante, celle que retourne {@link #getSeries}. Cette m�thode
     * peut �tre appel�e de n'importe quel thread (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param  table Table dans laquelle puiser la liste des images.
     * @throws SQLException si l'interrogation de la table a �chou�e.
     */
    public void setEntries(final ImageTable table) throws SQLException
    {this.table.setEntries(table);}

    /**
     * Retourne la s�rie d'images repr�sent�e par cette table. Si la s�rie n'est
     * pas connue ou si cette table contient des images de plusieurs s�ries
     * diff�rentes, alors cette m�thode peut retourner <code>null</code>.
     */
    public SeriesEntry getSeries()
    {return table.getSeries();}

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des �ventuelles
     * bases de donn�es acc�d�es par cette fen�tre.
     */
    protected void setTimeZone(final TimeZone timezone)
    {
        super.setTimeZone(timezone);
        table.setTimeZone(timezone);
    }

    /**
     * Ajoute un objet � la liste des objets int�ress�s � �tre inform�s des
     * changements. Les "changements" peuvent couvrir une assez grande gamme:
     * changement du contenu de la table ou de la s�lection de l'utilisateur,
     * apparation de la fen�tre alors qu'elle �tait auparavant invisible, etc.
     */
    public void addChangeListener(final ChangeListener listener)
    {listenerList.add(ChangeListener.class, listener);}

    /**
     * Retire un objet de la liste des objets int�ress�s � �tre inform�s des
     * changements. Les "changements" peuvent couvrir une assez grande gamme:
     * changement du contenu de la table ou de la s�lection de l'utilisateur,
     * apparation de la fen�tre alors qu'elle �tait auparavant invisible, etc.
     */
    public void removeChangeListener(final ChangeListener listener)
    {listenerList.remove(ChangeListener.class, listener);}

    /**
     * Classe de l'objet qui �coutera les �v�nements d'int�ret pour ce panneau.
     * On s'int�resse aux changements des donn�es dans la table d'images, aux
     * changements de la s�lection de l'utilisateur ainsi qu'aux changements
     * de la visibilit� du panneau.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Listener implements TableModelListener, ListSelectionListener, ImageChangeListener
    {
        /**
         * M�thode appel�e automatiquement chaque
         * fois que le contenu du tableau change.
         */
        public void tableChanged(final TableModelEvent event)
        {updateStatusBar();}

        /**
         * M�thode appel�e automatiquement chaque fois que change
         * la s�lection de l'utilisateur dans le tableau.
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
         * M�thode appel�e automatiquement chaque fois qu'une des images affich�es a chang�e.
         * Cette m�thode n'est appel�e qu'apr�s que le chargement ait �t� compl�t�e.
         */
        public void imageChanged(final ImageChangeEvent event)
        {
            ImageTablePanel.this.imageChanged(mosaic.getGridCoverages());
        }
    }
}
