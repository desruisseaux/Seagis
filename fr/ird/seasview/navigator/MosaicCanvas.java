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

// Geotools dependencies
import org.geotools.gc.GridCoverage;

// Map components
import org.geotools.gui.swing.MapPane;
import org.geotools.renderer.j2d.MouseCoordinateFormat;
import fr.ird.seasview.layer.control.LayerControl;

// User interface
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import fr.ird.seasview.InternalFrame;

// Events
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import org.geotools.gui.swing.event.ZoomChangeEvent;
import org.geotools.gui.swing.event.ZoomChangeListener;

// Logger
import java.util.logging.Level;
import java.util.logging.Logger;

// Geometry
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Collections
import java.util.List;
import java.util.ArrayList;

// Database
import fr.ird.seasview.DataBase;

// Miscellaneous
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.StatusBar;


/**
 * Panneau contenant une mosaïque d'images {@link ImageCanvas}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class MosaicCanvas extends JPanel {
    /**
     * Groupe des threads ayant la charge de lire des images en arrière-plan.
     * Ce groupe contiendra des threads de la classe {@link ImageCanvas.Reader}.
     * Ce champ est accédé par {@link ImageCanvas} lorsqu'il construit des threads.
     */
    final ThreadGroup readers;

    /**
     * Barre d'état dans laquelle écrire les coordonnées du curseur
     * de la souris, ou <code>null</code> s'il n'y en a pas. Ce champ
     * est aussi accédé par le paneau {@link ImageTablePanel} parent.
     */
    final StatusBar statusBar;

    /**
     * Le format à utiliser pour écrire les coordonnées de la souris.
     */
    private final MouseCoordinateFormat mouseFormat = new MouseCoordinateFormat();

    /**
     * Indique si les barres de défilements sont visible
     * ou pas. Par défaut, elles ne le sont pas.
     */
    private boolean scrollBarsVisible;

    /**
     * Indique si les images sont synchronisées. La valeur <code>true</code> indique
     * que tout zoom ou translation appliqué sur une image d'une mosaïque doit être
     * répliqué sur les autres.
     */
    private boolean imagesSynchronized=true;

    /**
     * Indique si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. La valeur
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    private boolean paintingWhileAdjusting;

    /**
     * Indique si le zoom de l'ensemble des images est en
     * train de se faire ajuster. Si c'est le cas, alors
     * {@link Listeners#zoomChanged} ne devra pas tenir
     * compte des événements lancés.
     */
    transient boolean isAdjusting;

    /**
     * Indique si cette mosaïque sera de dimension fixe. La
     * valeur <code>false</code> indique que les ajustements
     * automatiques seront autorisés.
     */
    private boolean fixed;

    /**
     * Objet ayant la charge de réagir aux événements tels
     * qu'un changement de la taille de la fenêtre ou un
     * changement de zoom.
     */
    private final Listeners listeners = new Listeners();

    /**
     * Classe des objets ayant la charge de réagir aux événements tels
     * qu'un changement de la taille de la fenêtre ou un changement de
     * zoom. Cette classe se chargera aussi de surveiller les mouvements
     * de la souris pour afficher les coordonnées géographiques pointées.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Listeners
            implements ZoomChangeListener, MouseListener, ComponentListener
    {
        /**
         * Méthode appelée lorsque le zoom d'une des images a changé. Cette
         * méthode applique la même transformation sur les autres images.
         */
        public void zoomChanged(final ZoomChangeEvent event) {
            synchronized (getTreeLock()) {
                updateMouseCoordinate();
                if (imagesSynchronized && !isAdjusting) try {
                    isAdjusting = true;
                    final Object          source=event.getSource();
                    final AffineTransform change=event.getChange();
                    for (int i=getComponentCount(); --i>=0;) {
                        final Component c=getComponent(i);
                        if (c instanceof ImageCanvas) {
                            final MapPane mapPanel=((ImageCanvas) c).mapPanel;
                            if (mapPanel!=source) {
                                mapPanel.transform(change);
                            }
                        }
                    }
                } finally {
                    isAdjusting = false;
                }
            }
        }

        /**
         * Met à jour l'affichage des coordonnées de la souris. Cette méthode peut être
         * appelée lorsque la fenêtre change de place où lorsqu'un zoom a été effectué.
         */
        private void updateMouseCoordinate() {
            if (statusBar != null) {
                // TODO: il faudrait obtenir les coordonnées de la souris et recalculer
                //       les coordonnées géographiques. Malheureusement, le JDK 1.3 n'a
                //       pas d'API pour obtenir la position du curseur de la souris...
                statusBar.setCoordinate(null);
            }
        }

        /**
         * Méthode appelée lorsque la taille de la mosaïque a changée.
         * Cette méthode peut modifier la disposition des images de
         * façon à conserver un aspect aussi carré que possible.
         */
        public void componentResized(final ComponentEvent event) {
            synchronized (getTreeLock()) {
                if (adjust()) {
                    doLayout();
                    // validate()/invalidate() ont besoin de deux
                    // glissements de la souris pour prendre effet...
                }
                updateMouseCoordinate();
            }
        }

        /** Invoked when the component's position changes.      */ public void componentMoved (final ComponentEvent event) {updateMouseCoordinate();}
        /** Invoked when the component has been made visible.   */ public void componentShown (final ComponentEvent event) {}
        /** Invoked when the component has been made invisible. */ public void componentHidden(final ComponentEvent event) {}
        /** Invoked when the mouse has been clicked.            */ public void mouseClicked   (final MouseEvent     event) {}
        /** Invoked when a mouse button has been pressed.       */ public void mousePressed   (final MouseEvent     event) {statusBar.mouseMoved(event);}
        /** Invoked when a mouse button has been released.      */ public void mouseReleased  (final MouseEvent     event) {statusBar.mouseMoved(event);}
        /** Invoked when the mouse enters a component.          */ public void mouseEntered   (final MouseEvent     event) {}
        /** Invoked when the mouse exits a component.           */ public void mouseExited    (final MouseEvent     event) {statusBar.setCoordinate(null);}
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Construit une mosaïque d'images. La mosaïque
     * contiendra initialement de l'espace pour une
     * seule image.
     *
     * @param statusBar Barre d'état dans laquelle écrire les coordonnées du curseur
     *                  de la souris, ou <code>null</code> s'il n'y en a pas.
     * @param readers   Groupe dans lequel placer les threads qui liront des
     *                  images en arrière plan.
     */
    public MosaicCanvas(final StatusBar statusBar, final ThreadGroup readers) {
        super(new GridLayout(), false);
        this.statusBar = statusBar;
        this.readers   = readers;
        addComponentListener(listeners);
        adjust();
    }

    /**
     * Définit la taille de la grille.
     *
     * @param rows    Nombre d'images par ligne.
     * @param columns Nombre d'images par colonne.
     */
    public void setGridSize(final int rows, final int columns) {
        final GridLayout layout = (GridLayout) getLayout();
        layout.setRows(rows);
        layout.setColumns(columns);
        this.fixed=true;
    }

    /**
     * Modifie l'agencement de la mosaïque de façon
     * à accomoder le nombre d'images qu'elle contient.
     * @return <code>true</code> si la mosaïque a changée.
     */
    private boolean adjust() {
        if (!fixed) {
            final int           count = getComponentCount();
            final GridLayout   layout = (GridLayout) getLayout();
            final Resources resources = Resources.getResources(getLocale());
            int ny=Math.max(1, (int) Math.round(Math.sqrt(count * ((double)getHeight() / (double)getWidth()))));
            int nx=(count+(ny-1))/ny;              // Arrondi à l'entier supérieur.
            while (ny>1 && (ny-1)*nx>=count) ny--; // Supprime les lignes en trop.

            DataBase.logger.log(resources.getLogRecord(Level.FINER, ResourceKeys.ADJUST_MOSAIC_$3,
                                           new Integer(count), new Integer(ny), new Integer(nx)));

            if (ny!=layout.getRows() && nx!=layout.getColumns()) {
                layout.setRows   (ny);
                layout.setColumns(nx);
                return true;
            }
            /*
             * Si le paneau ne contient aucune composantes, alors plutôt
             * que de laisser un espace vide on placera des instructions
             * pour l'utilisateur.
             */
            if (count == 0) {
                final List<JLabel> labels=new ArrayList<JLabel>();
                labels.add(new JLabel(InternalFrame.getIcon("application-data/images/Cover.png")));
                final String message=resources.getString(ResourceKeys.IMAGES_TABLE_INSTRUCTIONS);
                int lower=0, upper;
                while ((upper=message.indexOf('\n', lower)) >= lower) {
                    labels.add(new JLabel(message.substring(lower, upper)));
                    lower = upper+1;
                }
                labels.add(new JLabel(message.substring(lower)));
                /*
                 * 'labels' contient maintenant la liste des étiquettes à placer
                 * dans le paneau. Dispose maintenant ces étiquettes au centre.
                 */
                final JPanel panel = new JPanel(new GridBagLayout());
                final GridBagConstraints c = new GridBagConstraints();
                panel.setBackground(Color.black);
                panel.setForeground(Color.white);
                final int length=labels.size();
                for (int i=0; i<length; i++) {
                    c.gridx=0; c.gridy=i;
                    c.insets.bottom = (i==0) ? 21 : 0;
                    final JLabel label = labels.get(i);
                    label.setForeground(Color.white);
                    panel.add(label, c);
                }
                add(panel);
            }
        }
        return false;
    }

    /**
     * Enregistre {@link #listeners} auprès de <code>mapPanel</code>.
     */
    private void addListeners(final MapPane mapPanel) {
        mapPanel.addZoomChangeListener(listeners);
        if (statusBar!=null) {
            mapPanel.addMouseMotionListener(statusBar);
            mapPanel.addMouseListener      (listeners);
        }
        mapPanel.setPaintingWhileAdjusting(paintingWhileAdjusting);
    }

    /**
     * Retire {@link #listeners} auprès de <code>mapPanel</code>.
     */
    private void removeListeners(final MapPane mapPanel) {
        mapPanel.removeZoomChangeListener (listeners);
        mapPanel.removeMouseMotionListener(statusBar);
        mapPanel.removeMouseListener      (listeners);
    }

    /**
     * Indique si au moins une image est en cours de lecture dans la mosaïque.
     * Le fait qu'une image soit en cours de lecture n'empêche pas d'ajouter ou
     * de retirer des images de la mosaïque.
     */
    public boolean isLoading() {
        synchronized (getTreeLock()) {
            final int count=getComponentCount();
            for (int i=0; i<count; i++) {
                final Component c=getComponent(i);
                if (c instanceof ImageCanvas) {
                    if (((ImageCanvas) c).isLoading()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Change le nombre d'images présentes dans cette mosaïque.
     * Si cette methode est appelée pour augmenter le nombre
     * d'images, alors de nouvelles images vides seront créees
     * et ajoutées. Si cette méthode est appellée pour diminuer
     * le nombre d'images, alors les images en trop seront oubliées.
     * Le nombre de lignes et de colonnes sera ajusté de façon à
     * accomoder le nouveau nombre d'images.
     */
    public void setImageCount(final int count) {
        final Component[] components = getComponents();
        final ImageCanvas[] images = new ImageCanvas[count];
        for (int i=0; i<images.length; i++) {
            if (i<components.length) {
                final Component c = components[i];
                if (c instanceof ImageCanvas) {
                    images[i] = (ImageCanvas) c;
                    continue;
                }
            }
            images[i] = new ImageCanvas();
        }
        setImages(images);
    }

    /**
     * Supprime toutes les images contenu dans cette mosaïque,
     * et met à la place les images spécifiées.  Le nombre de
     * lignes et de colonnes sera ajusté de façon à accomoder
     * le nouveau nombre d'images.
     */
    public void setImages(final ImageCanvas[] images) {
        synchronized (getTreeLock()) {
            ///// WORKAROUND BEGIN (bug #4130347)
            final int  removeCount = getComponentCount()-images.length;
            final Rectangle[] rect = new Rectangle[Math.max(removeCount,0)];
            for (int i=0; i<rect.length; i++)
                rect[i] = getComponent(images.length+i).getBounds();
            ///// WORKAROUND END (bug #4130347)

            /*
             * Annule l'inscription de 'listeners' auprès
             * des images, puis supprime toutes les images.
             */
            final Rectangle2D area = getVisibleArea();
            for (int i=getComponentCount(); --i>=0;) {
                final Component c=getComponent(i);
                if (c instanceof ImageCanvas) {
                    removeListeners(((ImageCanvas) c).mapPanel);
                }
            }
            removeAll();
            /*
             * Ajoute toutes les images
             * qui ont été spécifiées.
             */
            for (int i=0; i<images.length; i++) {
                final ImageCanvas image = images[i];
                image.setBorder(BorderFactory.createLoweredBevelBorder());
                image.setScrollBarsVisible(scrollBarsVisible);
                add(image);
            }
            adjust();
            validate();
            if (area != null) {
                setVisibleArea(area);
            }
            for (int i=0; i<rect.length; i++) {
                repaint(rect[i]); // workaround for bug #4130347
            }
            for (int i=0; i<images.length; i++) {
                addListeners(images[i].mapPanel);
            }
        }
    }

    /**
     * Supprime toutes les images qui se trouvait dans ce paneau.
     * Les ressources utilisées par les images seront libérées.
     */
    public void removeAllImages() {
        synchronized (getTreeLock()) {
            final Component[] old=getComponents();
            removeAll();
            for (int i=old.length; --i>=0;) {
                if (old[i] instanceof ImageCanvas) {
                    final ImageCanvas image = (ImageCanvas) old[i];
                    removeListeners(image.mapPanel);
                    image.dispose();
                }
            }
            adjust();
            validate();
            repaint(); // workaround for bug #4130347?
        }
    }

    /**
     * Retourne l'image à l'index spécifié.
     */
    public ImageCanvas getImage(int index) {
        final int n = getComponentCount();
        for (int i=0; i<n; i++) {
            final Component c = getComponent(i);
            if (c instanceof ImageCanvas) {
                if (index == 0) {
                    return (ImageCanvas) c;
                }
                index--;
            }
        }
        return null;
    }

    /**
     * Retourne toutes les images apparaissant dans la mosaïque.
     * Le tableau retourné ne sera jamais nul, mais peut avoir
     * une longueur de 0. Il ne contiendra aucun élément nul.
     */
    public GridCoverage[] getGridCoverages() {
        synchronized (getTreeLock()) {
            final int count=getComponentCount();
            final List<GridCoverage> images=new ArrayList<GridCoverage>(count);
            for (int i=0; i<count; i++) {
                final Component c=getComponent(i);
                if (c instanceof ImageCanvas) {
                    ((ImageCanvas) c).getImages(images);
                }
            }
            return images.toArray(new GridCoverage[images.size()]);
        }
    }

    /**
     * Retourne le nombre d'images dans ce paneau.
     */
    public int getImageCount() {
        int count=0;
        for (int i=getComponentCount(); --i>=0;) {
            if (getComponent(i) instanceof ImageCanvas) {
                count++;
            }
        }
        return count;
    }

    /**
     * Procède à un nouveau chargement des images en mémoire. Cette méthode ne vide
     * pas les caches internes. Si l'image à relire est encore dans la cache, la
     * cache sera utilisée.
     */
    public void reload(final LayerControl[] layers) {
        synchronized (getTreeLock()) {
            final boolean oldAdjusting = isAdjusting;
            try {
                isAdjusting = true;
                for (int i=getComponentCount(); --i>=0;) {
                    final Component c = getComponent(i);
                    if (c instanceof ImageCanvas) {
                        ((ImageCanvas) c).reload(layers, statusBar);
                    }
                }
            } finally {
                isAdjusting = oldAdjusting;
            }
        }
    }

    /**
     * Réinitialise les zooms de toutes les
     * images à leurs valeurs par défaut.
     */
    public void reset() {
        synchronized (getTreeLock()) {
            final boolean oldAdjusting=isAdjusting;
            try {
                isAdjusting = true;
                for (int i=getComponentCount(); --i>=0;) {
                    final Component c = getComponent(i);
                    if (c instanceof ImageCanvas) {
                        ((ImageCanvas) c).mapPanel.reset();
                    }
                }
            } finally {
                isAdjusting = oldAdjusting;
            }
        }
    }

    /**
     * Retourne un rectangle qui est l'union des
     * régions visibles de toutes les images. Si
     * aucune image n'est affichée,  alors cette
     * méthode retourne <code>null</code>.
     */
    public Rectangle2D getVisibleArea() {
        synchronized (getTreeLock()) {
            Rectangle2D area = null;
            for (int i=getComponentCount(); --i>=0;) {
                final Component c = getComponent(i);
                if (c instanceof ImageCanvas) {
                    final MapPane mapPanel = ((ImageCanvas) c).mapPanel;
                    if (mapPanel.getRenderer().getLayerCount() != 0) {
                        final Rectangle2D areaI=mapPanel.getVisibleArea();
                        // TODO: tenir compte du système de coordonnées.
                        if (area == null) {
                            area = areaI;
                        } else {
                            area.add(areaI);
                        }
                    }
                }
            }
            return area;
        }
    }

    /**
     * Modifie la région visible de toute les images.
     */
    public void setVisibleArea(final Rectangle2D area) {
        synchronized (getTreeLock()) {
            final boolean oldAdjusting = isAdjusting;
            try {
                isAdjusting = true;
                for (int i=getComponentCount(); --i>=0;) {
                    final Component c=getComponent(i);
                    if (c instanceof ImageCanvas) {
                        // TODO: tenir compte du système de coordonnées.
                        ((ImageCanvas) c).mapPanel.setVisibleArea(area);
                    }
                }
            } finally {
                isAdjusting = oldAdjusting;
            }
        }
    }

    /**
     * Indique si les barres de défilements des images doivent être
     * visibles ou pas. Par défaut, les barres de défilements ne sont
     * pas visibles.
     */
    public boolean getScrollBarsVisible() {
        return scrollBarsVisible;
    }

    /**
     * Spécifie si les barres de défilements des images doivent être
     * visibles ou pas. Par défaut, les barres de défilements ne sont
     * pas visibles.
     */
    public void setScrollBarsVisible(final boolean visible) {
        synchronized (getTreeLock()) {
            for (int i=getComponentCount(); --i>=0;) {
                final Component c=getComponent(i);
                if (c instanceof ImageCanvas) {
                    ((ImageCanvas) c).setScrollBarsVisible(visible);
                }
            }
            scrollBarsVisible=visible;
        }
    }

    /**
     * Indique si les images sont synchronisées. La valeur <code>true</code> indique
     * que tout zoom ou translation appliqué sur une image d'une mosaïque doit être
     * répliqué sur les autres.
     */
    public boolean getImagesSynchronized() {
        return imagesSynchronized;
    }

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqué sur une image d'une
     * mosaïque doit être répliqué sur les autres.
     */
    public void setImagesSynchronized(final boolean s) {
        imagesSynchronized = s;
    }

    /**
     * Spécifie si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. Spécifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    protected void setPaintingWhileAdjusting(final boolean s) {
        synchronized (getTreeLock()) {
            for (int i=getComponentCount(); --i>=0;) {
                final Component c=getComponent(i);
                if (c instanceof ImageCanvas) {
                    ((ImageCanvas) c).setPaintingWhileAdjusting(s);
                }
            }
        }
        paintingWhileAdjusting=s;
    }

    /**
     * Libère les ressources utilisées par cette composante.
     * Cette méthode appelera {@link ImageCanvas#dispose()}
     * pour chacune des images contenues dans cette mosaïque.
     */
    protected void dispose() {
        synchronized (getTreeLock()) {
            for (int i=getComponentCount(); --i>=0;) {
                final Component c=getComponent(i);
                if (c instanceof ImageCanvas) {
                    ((ImageCanvas) c).dispose();
                }
            }
        }
    }

    /**
     * Retourne la liste des images apparaissant dans cette composante.
     * Cette méthode n'est utilisée qu'à des fins de déboguages.
     */
    public String toString() {
        synchronized (getTreeLock()) {
            final GridLayout layout = (GridLayout) getLayout();
            final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
            buffer.append('[');
            buffer.append(layout.getRows());
            buffer.append('\u00D7'); // Symbole de multiplication
            buffer.append(layout.getColumns());
            buffer.append(']');
            buffer.append('\n');
            for (int i=getComponentCount(); --i>=0;) {
                buffer.append(Utilities.spaces(4));
                buffer.append(getComponent(i));
                buffer.append('\n');
            }
            return buffer.toString();
        }
    }
}
