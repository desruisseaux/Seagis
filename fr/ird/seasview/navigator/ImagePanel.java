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
package fr.ird.seasview.navigator;

// Database
import java.util.TimeZone;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Graphical user inferface
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import java.awt.Dimension;

// Undo, Redo and event
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.undo.UndoManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Clipboard
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.ClipboardOwner;

// Geotools
import org.geotools.gui.swing.StatusBar;

// Map components
import fr.ird.seasview.InternalFrame;
import fr.ird.seasview.layer.control.LayerControl;

// Miscellaneous
import fr.ird.resources.XArray;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Classe de base pour les classes qui afficheront des mosaïques d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class ImagePanel extends JSplitPane implements ComponentListener, ChangeListener, ClipboardOwner {
    /**
     * Mosaïque affichant les images sélectionnées.
     */
    protected final MosaicCanvas mosaic;

    /**
     * Liste d'éléments {@link LayerControl} qui représentent les couches que l'utilisateur
     * peut choisir et configurer.
     */
    protected final LayerControl[] layers;

    /**
     * Nombre maximal d'images pour afficher l'échelle dans chacune d'elle. Si le nombre
     * d'images est élevé, il n'y aura pas la place de reproduire l'échelle de la carte
     * dans chacune d'elle. La valeur 0 désactive complètement l'affichage de l'échelle.
     */
    protected int maxImagesForMapScale = 1;

    /**
     * Objet à utiliser pour annuler les éditions de la table. Cet objet écoutera
     * les événements {@link javax.swing.event.UndoableEditEvent} que produira la
     * table.
     */
    protected final UndoManager undoManager = new UndoManager();

    /**
     * Construit un panneau.
     *
     * @param statusBar Barre d'état qui apparaîtra dans le bas de la fenêtre.
     * @param readers   Sous-groupe de <code>threads</code>Groupe dans lequel placer
     *                  les threads qui liront des images en arrière-plan.
     * @param layers    Couches que l'utilisateur pourra placer sur les images,
     *                  ou <code>null</code> pour n'afficher que les images.
     */
    public ImagePanel(final StatusBar statusBar, final ThreadGroup readers, final LayerControl[] layers) {
        super(HORIZONTAL_SPLIT);
        this.layers = layers;
        this.mosaic = new MosaicCanvas(statusBar, readers);

        final Icon     propertyIcon = InternalFrame.getIcon("toolbarButtonGraphics/general/Properties16.gif");
        final JComponent layerPanel = LayerControl.getPanel(layers, propertyIcon);
        final JSplitPane  controler = new JSplitPane(VERTICAL_SPLIT, true);

        controler.setBottomComponent(layerPanel);
        controler.setResizeWeight(1); // La composante du haut reçoit tout l'espace.
        controler.setOneTouchExpandable(true);
        controler.setMinimumSize  (new Dimension(200, 80));
        mosaic   .setMinimumSize  (new Dimension(100,100));

        setLeftComponent     (controler);
        setResizeWeight      (0); // La composante de droite reçoit tout l'espace.
        setOneTouchExpandable(true);
        setDividerLocation   (260);

        undoManager.setLimit(20);
        addComponentListener(this);

        if (layers != null) {
            for (int i=0; i<layers.length; i++) {
                final LayerControl layer=layers[i];
                synchronized (layer) {
                    layer.addChangeListener      (this);
                    layer.addUndoableEditListener(undoManager);
                }
            }
        }
    }

    /**
     * Reset divider locations.
     */
    final void resetDividerLocation() {
        ((JSplitPane) getLeftComponent()).setDividerLocation(-1);
        setDividerLocation(-1);
    }

    /**
     * Retourne les couches sélectionnées par l'utilisateur. Le tableau
     * retourné peut avoir une longueur de 0, mais ne sera jamais nul.
     */
    public final LayerControl[] getSelectedLayers() {
        final int length = (layers!=null) ? layers.length : 0;
        final LayerControl[] selected=new LayerControl[length];
        int count=0;
        for (int i=0; i<length; i++) {
            if (layers[i].isSelected()) {
                selected[count++]=layers[i];
            }
        }
        return XArray.resize(selected, count);
    }

    /**
     * Met à jour le texte de la barre d'état.  Cette méthode est
     * appelée automatiquement par exemple lorsque l'onglet de ce
     * panneau a été sélectionné.
     */
    protected abstract void updateStatusBar();

    /**
     * Signale tous les objets intéressés qu'un changement est survenu
     * dans cette fenêtre. Les changements signalés sont les changements
     * dans le contenu de la table,  dans les images sélectionnées par
     * l'utilisateur ou dans la visibilité de la table par exemple.
     */
    protected final void fireStateChanged() {
        ChangeEvent event=null;
        final Object[] listeners=listenerList.getListenerList();
        for (int i=listeners.length; (i-=2) >= 0;) {
            if (listeners[i] == ChangeListener.class) {
                if (event == null) {
                    event = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i+1]).stateChanged(event);
            }
        }
    }

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des éventuelles
     * bases de données accédées par cette fenêtre.
     */
    protected void setTimeZone(final TimeZone timezone) {
    }

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqué sur une image d'une
     * mosaïque doit être répliqué sur les autres.
     */
    protected final void setImagesSynchronized(final boolean s) {
        mosaic.setImagesSynchronized(s);
    }

    /**
     * Spécifie si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. Spécifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    protected final void setPaintingWhileAdjusting(final boolean s) {
        mosaic.setPaintingWhileAdjusting(s);
        setContinuousLayout(s);
    }

    /**
     * Libère les ressources utilisées par cette fenêtre.
     * Cette méthode est appelée automatiquement lorsque
     * la fenêtre est fermée.
     *
     * @throws SQLException si un accès à une base de
     *         données était nécessaire et a échoué.
     */
    public final void dispose() throws RemoteException {
        mosaic.dispose();
        if (layers != null) {
            for (int i=layers.length; --i>=0;) {
                layers[i].dispose();
            }
        }
    }

    /**
     * Indique si cette fenêtre peut traiter l'opération désignée par le code
     * spécifié. Le code <code>clé</code> désigne une des constantes de
     * l'interface {@link ResourceKeys}, utilisé pour la construction des
     * menus. Par exemple le code {@link ResourceKeys#UNDO} désigne le menu
     * "annuler".
     */
    protected boolean canProcess(final int clé) {
        switch (clé) {
            case ResourceKeys.UNDO:       return undoManager.canUndo();
            case ResourceKeys.REDO:       return undoManager.canRedo();
            case ResourceKeys.RESET_VIEW: return mosaic.getImageCount()!=0;
            case ResourceKeys.DEBUG:      return true;
            default:                      return false;
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
    protected boolean process(final int clé) throws RemoteException {
        switch (clé) {
            default: return false;

            //////////////////////////
            ///  Edition - Annuler ///
            //////////////////////////
            case ResourceKeys.UNDO: {
                undoManager.undo();
                return true;
            }
            //////////////////////////
            ///  Edition - Refaire ///
            //////////////////////////
            case ResourceKeys.REDO: {
                undoManager.redo();
                return true;
            }
            //////////////////////////
            ///  Rétablir le zoom  ///
            //////////////////////////
            case ResourceKeys.RESET_VIEW: {
                mosaic.reset();
                return true;
            }
            case ResourceKeys.DEBUG: {
                fr.ird.seasview.DataBase.out.println(mosaic);
                return true;
            }
        }
    }


    /////////////////////////////////////////////////////////////////
    ////////                                                 ////////
    ////////    Implementation des "Listeners"               ////////
    ////////    Ne pas appeller ces méthodes directement!    ////////
    ////////                                                 ////////
    /////////////////////////////////////////////////////////////////
    /**
     * Invoked when the component's size changes.
     */
    public void componentResized(final ComponentEvent event) {
    }

    /**
     * Invoked when the component's position changes.
     */
    public void componentMoved(final ComponentEvent event) {
    }

    /**
     * Méthode appelée automatiquement chaque fois
     * que l'onglet de ce panneau est sélectionné.
     */
    public void componentShown(final ComponentEvent event) {
        updateStatusBar();
    }

    /**
     * Invoked when the component has been made invisible.
     */
    public void componentHidden(final ComponentEvent event) {
    }

    /**
     * Méthode appelée automatiquement lorsque la configuration
     * d'au moins une couche a changée. Cette méthode recharge
     * les images afin de prendre en compte le changement.
     */
    public final void stateChanged(final ChangeEvent event) {
        mosaic.reload((layers!=null) ? getSelectedLayers() : null);
        fireStateChanged();
    }

    /**
     * Méthode appelée automatiquement lorsque ce panneau
     * ne possède plus le contenu du presse-papier.
     */
    public void lostOwnership(final Clipboard clipboard, final Transferable contents) {
    }
}
