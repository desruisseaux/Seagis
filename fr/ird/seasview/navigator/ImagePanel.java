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
 * Classe de base pour les classes qui afficheront des mosa�ques d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class ImagePanel extends JSplitPane implements ComponentListener, ChangeListener, ClipboardOwner {
    /**
     * Mosa�que affichant les images s�lectionn�es.
     */
    protected final MosaicCanvas mosaic;

    /**
     * Liste d'�l�ments {@link LayerControl} qui repr�sentent les couches que l'utilisateur
     * peut choisir et configurer.
     */
    protected final LayerControl[] layers;

    /**
     * Nombre maximal d'images pour afficher l'�chelle dans chacune d'elle. Si le nombre
     * d'images est �lev�, il n'y aura pas la place de reproduire l'�chelle de la carte
     * dans chacune d'elle. La valeur 0 d�sactive compl�tement l'affichage de l'�chelle.
     */
    protected int maxImagesForMapScale = 1;

    /**
     * Objet � utiliser pour annuler les �ditions de la table. Cet objet �coutera
     * les �v�nements {@link javax.swing.event.UndoableEditEvent} que produira la
     * table.
     */
    protected final UndoManager undoManager = new UndoManager();

    /**
     * Construit un panneau.
     *
     * @param statusBar Barre d'�tat qui appara�tra dans le bas de la fen�tre.
     * @param readers   Sous-groupe de <code>threads</code>Groupe dans lequel placer
     *                  les threads qui liront des images en arri�re-plan.
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
        controler.setResizeWeight(1); // La composante du haut re�oit tout l'espace.
        controler.setOneTouchExpandable(true);
        controler.setMinimumSize  (new Dimension(200, 80));
        mosaic   .setMinimumSize  (new Dimension(100,100));

        setLeftComponent     (controler);
        setResizeWeight      (0); // La composante de droite re�oit tout l'espace.
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
     * Retourne les couches s�lectionn�es par l'utilisateur. Le tableau
     * retourn� peut avoir une longueur de 0, mais ne sera jamais nul.
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
     * Met � jour le texte de la barre d'�tat.  Cette m�thode est
     * appel�e automatiquement par exemple lorsque l'onglet de ce
     * panneau a �t� s�lectionn�.
     */
    protected abstract void updateStatusBar();

    /**
     * Signale tous les objets int�ress�s qu'un changement est survenu
     * dans cette fen�tre. Les changements signal�s sont les changements
     * dans le contenu de la table,  dans les images s�lectionn�es par
     * l'utilisateur ou dans la visibilit� de la table par exemple.
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
     * Cette modification n'affecte pas le fuseau horaire des �ventuelles
     * bases de donn�es acc�d�es par cette fen�tre.
     */
    protected void setTimeZone(final TimeZone timezone) {
    }

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqu� sur une image d'une
     * mosa�que doit �tre r�pliqu� sur les autres.
     */
    protected final void setImagesSynchronized(final boolean s) {
        mosaic.setImagesSynchronized(s);
    }

    /**
     * Sp�cifie si les cartes doivent �tre redessin�es
     * durant les glissements des ascenceurs. Sp�cifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    protected final void setPaintingWhileAdjusting(final boolean s) {
        mosaic.setPaintingWhileAdjusting(s);
        setContinuousLayout(s);
    }

    /**
     * Lib�re les ressources utilis�es par cette fen�tre.
     * Cette m�thode est appel�e automatiquement lorsque
     * la fen�tre est ferm�e.
     *
     * @throws SQLException si un acc�s � une base de
     *         donn�es �tait n�cessaire et a �chou�.
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
     * Indique si cette fen�tre peut traiter l'op�ration d�sign�e par le code
     * sp�cifi�. Le code <code>cl�</code> d�signe une des constantes de
     * l'interface {@link ResourceKeys}, utilis� pour la construction des
     * menus. Par exemple le code {@link ResourceKeys#UNDO} d�signe le menu
     * "annuler".
     */
    protected boolean canProcess(final int cl�) {
        switch (cl�) {
            case ResourceKeys.UNDO:       return undoManager.canUndo();
            case ResourceKeys.REDO:       return undoManager.canRedo();
            case ResourceKeys.RESET_VIEW: return mosaic.getImageCount()!=0;
            case ResourceKeys.DEBUG:      return true;
            default:                      return false;
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
    protected boolean process(final int cl�) throws RemoteException {
        switch (cl�) {
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
            ///  R�tablir le zoom  ///
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
    ////////    Ne pas appeller ces m�thodes directement!    ////////
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
     * M�thode appel�e automatiquement chaque fois
     * que l'onglet de ce panneau est s�lectionn�.
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
     * M�thode appel�e automatiquement lorsque la configuration
     * d'au moins une couche a chang�e. Cette m�thode recharge
     * les images afin de prendre en compte le changement.
     */
    public final void stateChanged(final ChangeEvent event) {
        mosaic.reload((layers!=null) ? getSelectedLayers() : null);
        fireStateChanged();
    }

    /**
     * M�thode appel�e automatiquement lorsque ce panneau
     * ne poss�de plus le contenu du presse-papier.
     */
    public void lostOwnership(final Clipboard clipboard, final Transferable contents) {
    }
}
