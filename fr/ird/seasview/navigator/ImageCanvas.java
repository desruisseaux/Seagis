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

// Geometry and graphics
import java.awt.Color;
import java.awt.geom.Rectangle2D;

// User interface
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

// Events and progress
import java.awt.EventQueue;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import javax.swing.event.EventListenerList;

// Miscellaneous
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.FieldPosition;
import javax.media.jai.util.Range;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;
import org.geotools.renderer.j2d.Renderer;
import org.geotools.renderer.j2d.ImageType;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedMapScale;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.gui.swing.StatusBar;
import org.geotools.gui.swing.MapPane;

// Seagis
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.gui.event.CoverageChangeEvent;
import fr.ird.database.gui.event.CoverageChangeListener;

// Main framework
import fr.ird.seasview.Task;
import fr.ird.seasview.DataBase;
import fr.ird.seasview.InternalFrame;
import fr.ird.seasview.layer.control.LayerControl;

// Resources
import fr.ird.resources.XArray;
import fr.ird.resources.experimental.ResourceKeys;
import fr.ird.resources.experimental.Resources;


/**
 * Panneau affichant une image. En plus de l'image, d'autres informations
 * peuvent être affichées comme par exemple l'échelle de couleurs.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageCanvas extends JPanel {
    /**
     * Durée de vie des threads {@link Reader}. Si un laps de temps
     * supérieur à <code>LIFETIME</code> s'est écoulé sans qu'aucune
     * image ne soit demandée, le thread mourra. Un autre thread sera
     * créé à la volée si une nouvelle image est demandée par après.
     */
    private static final int LIFETIME = 30000; // 30 secondes

    /**
     * Laps de temps à attendre avant de démarrer effectivement la lecture
     * d'une image. On attend ce laps de temps au cas où l'utilisateur
     * changerait d'idée.
     */
    private static final int TIMELAG = 300;

    /**
     * The date format to use for formatting date in image's title.
     */
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    /**
     * Texte à afficher par défaut au dessus des
     * images lorsqu'aucun titre n'a été spécifié.
     */
    private static final String UNTITLED = Resources.format(ResourceKeys.UNTITLED);

    /**
     * Composante qui contiendra
     * le titre de l'image.
     */
    private final JLabel title=new JLabel(UNTITLED);

    /**
     * Composante affichant la carte.
     */
    public final MapPane mapPanel = new MapPane(GeographicCoordinateSystem.WGS84);

    /**
     * Paneau qui contiendra la carte
     * avec les barres de défilements.
     */
    private JComponent scrollPane;

    /**
     * Processus qui est en train de faire la lecture
     * d'une image, ou <code>null</code> s'il n'y en
     * a pas encore.
     */
    private transient Reader reader;

    /**
     * Entré ayant servit à obtenir l'image
     * présentement affichée. Ce champ peut
     * être nul si l'entré n'est pas connue.
     */
    private CoverageEntry source;

    /**
     * <code>true</code> si l'échelle de la carte doit être affichée.
     */
    private boolean mapScaleVisible;

    /**
     * Construit un panneau initialement vide.  Le contenu
     * de ce panneau pourra être spécifié par des appels à
     * la méthode {@link #setImage}.
     */
    public ImageCanvas() {
        super(new BorderLayout());
        mapPanel.setBackground(Color.BLACK);
        title   .setBackground(Color.BLACK);
        title   .setForeground(Color.YELLOW);
        title   .setHorizontalAlignment(SwingConstants.CENTER);
        title   .setOpaque(true);
        setOpaque(false);
        setBorder(BorderFactory.createLoweredBevelBorder());
        add(title,    BorderLayout.NORTH );
        add(mapPanel, BorderLayout.CENTER);
        mapPanel.getRenderer().setOffscreenBuffered(Float.NEGATIVE_INFINITY,
                                                    Float.POSITIVE_INFINITY,
                                                    ImageType.VOLATILE);
    }

    /**
     * Retourne le paneau {@link MosaicCanvas} dans lequel est insérée
     * cette image, ou <code>null</code> s'il n'y en a pas.
     */
    private MosaicCanvas getMosaic() {
        for (Container parent=getParent(); parent!=null; parent=parent.getParent()) {
            if (parent instanceof MosaicCanvas) return (MosaicCanvas)parent;
        }
        return null;
    }

    /**
     * Met à jour le titre de la fenêtre en fonction de
     * l'image présentement affichée.
     */
    private void updateTitle() {
        final CoverageEntry entry = source;
        if (entry != null) {
            final Range timeRange = entry.getTimeRange();
            StringBuffer   buffer = new StringBuffer(entry.getName());
            if (timeRange != null) {
                final Date startTime = (Date) timeRange.getMinValue();
                final Date   endTime = (Date) timeRange.getMaxValue();
                if (startTime!=null || endTime!=null) {
                    long time;
                    if (startTime != null) {
                        time = startTime.getTime();
                        if (endTime != null) {
                            time = (time + endTime.getTime())/2;
                        }
                    } else {
                        time = endTime.getTime();
                    }
                    buffer.append(" (");
                    buffer = dateFormat.format(new Date(time), buffer, new FieldPosition(0));
                    buffer.append(')');
                }
            }
            title.setText(buffer.toString());
        } else {
            title.setText(UNTITLED);
        }
    }

    /**
     * Indique si l'échelle doit être visible ou pas.
     */
    public void setMapScaleVisible(final boolean visible) {
        if (mapScaleVisible != visible) {
            mapScaleVisible = visible;
            repaint();
        }
    }

    /**
     * Indique si le chargement d'une image est en cours.
     * Le fait qu'une image soit en train d'être chargée
     * n'empêche pas d'appeler {@link #setImage}. Dans ce
     * dernier cas, le chargement en cours sera annulé et
     * le chargement de la nouvelle image commencera.
     */
    public synchronized boolean isLoading() {
        return reader!=null && reader.isLoading();
    }

    /**
     * Ajoute à la liste spécifiée toutes les images
     * {@link GridCoverage} affichées dans ce paneau.
     */
    final synchronized void getImages(final List<GridCoverage> images) {
        final RenderedLayer[] visualLayers = mapPanel.getRenderer().getLayers();
        for (int i=0; i<visualLayers.length; i++) {
            if (visualLayers[i] instanceof RenderedGridCoverage) {
                final GridCoverage image=((RenderedGridCoverage) visualLayers[i]).getGridCoverage();
                if (image != null) {
                    images.add(image);
                }
            }
        }
    }

    /**
     * Spécifie l'image à faire apparaître. Une valeur nulle
     * signifie qu'aucune image ne doit être affichée. Cette
     * méthode peut être appelée à partir de n'importe quel
     * thread.
     *
     * @param image Image à afficher, ou <code>null</code>
     *              pour ne plus afficher d'image.
     */
    public void setImage(final GridCoverage image) { // NO synchronize here!
        setImage((image!=null) ? new RenderedLayer[] {new RenderedGridCoverage(image)} : null, null, image);
    }

    /**
     * Spécifie l'image à faire apparaître. Une valeur nulle
     * signifie qu'aucune image ne doit être affichée. Cette
     * méthode peut être appelée à partir de n'importe quel
     * thread.
     *
     * @param visualLayers Couches à afficher, ou <code>null</code> pour ne plus afficher d'image.
     *                     Cette liste ne doit comprendre que des objets {@link Layers}.
     * @param entry        Entré ayant servit à obtenir l'image, ou <code>null</code> s'il n'y en a pas.
     * @param image        Image affichée. Cette information n'est utilisée que pour informer les objets
     *                     {@link CoverageChangeListener} du changement.
     */
    private void setImage(final RenderedLayer[] visualLayers,
                          final CoverageEntry   entry,
                          final GridCoverage    image) // NO synchronize here!
    {
        if (!EventQueue.isDispatchThread()) {
            Task.invokeAndWait(new Runnable() {
                public void run() {
                    setImage(visualLayers, entry, image);
                }
            });
            return;
        }
        // Le code suivant sera exécuté
        // dans le thread de Swing.
        ///////////////////////////////
        synchronized (getTreeLock()) {
            this.source = null;
            /*
             * Obtient les coordonnées géographiques de la région qui était
             * couverte par les anciennes images.   On fera en sorte que la
             * nouvelle image couvre les mêmes coordonnées.
             */
            final MosaicCanvas mosaic = getMosaic();
            Rectangle2D visibleArea  = null;
            if (mosaic!=null) {
                visibleArea=mosaic.getVisibleArea();
            }
            if (visibleArea == null) {
                visibleArea = (mapPanel.getRenderer().getLayerCount()!=0) ?
                               mapPanel.getVisibleArea() : null;
            }
            /*
             * Insère la nouvelle image. Pendant le remplacement de l'image,
             * on désactivera temporairement la synchronisation des images.
             */
            final boolean oldAdjusting = (mosaic!=null) ? mosaic.isAdjusting : false;
            try {
                if (mosaic != null) {
                    mosaic.isAdjusting = true;
                }
                mapPanel.getRenderer().removeAllLayers();
                if (visualLayers != null) {
                    for (int i=0; i<visualLayers.length; i++) {
                        mapPanel.getRenderer().addLayer((RenderedLayer) visualLayers[i]);
                    }
                    if (mapScaleVisible) {
                        mapPanel.getRenderer().addLayer(new RenderedMapScale());
                    }
                    if (visibleArea != null) {
                        mapPanel.setVisibleArea(visibleArea);
                    }
                }
                this.source = entry;
            } finally {
                if (mosaic != null) {
                    mosaic.isAdjusting = oldAdjusting;
                }
            }
            updateTitle();
            fireImageChanged(source, image);
        }
    }

    /**
     * Spécifie l'image à faire apparaître. Si l'image n'était pas déjà en
     * mémoire, elle sera automatiquement lue en arrière-plan à partir de
     * son fichier. Cette méthode peut être appelée à partir de n'importe
     * quel thread.
     *
     * @param image    Entrée de la base de données qui décrit l'image à lire.
     * @param layers   Couches à placer sur l'image, ou <code>null</code> pour
     *                 ne mettre que l'image.
     * @param progress Objet à informer des progrès de la lecture,
     *                 ou <code>null</code> s'il n'y en a pas.
     */
    public synchronized void setImage(final CoverageEntry  entry,
                                      final LayerControl[] layers,
                                      final IIOReadProgressListener progress)
    {
        if (entry == null) {
            setImage((GridCoverage)null);
            return;
        }
        if (reader != null) {
            synchronized (reader) {
                if (reader.isAlive() && !reader.kill) {
                    reader.setImage(entry, layers, TIMELAG, progress);
                    return;
                }
            }
        }
        final MosaicCanvas mosaic = getMosaic();
        reader = new Reader(mosaic!=null ? mosaic.readers : null);
        reader.setImage(entry, layers, TIMELAG, progress);
        reader.start();
    }

    /**
     * Procède à un nouveau chargement de l'image en mémoire. Cette méthode ne vide
     * pas les caches internes. Si l'image à relire est encore dans la cache, la
     * cache sera utilisée.
     */
    final synchronized void reload(final LayerControl[] layers, final StatusBar statusBar) {
        setImage(source, layers, (source!=null) ?
                    statusBar.getIIOReadProgressListener(source.getName()) : null);
    }

    /**
     * Indique si les barres de défilements sont visibles.
     * Par défaut, les barres de défilements ne sont pas
     * visibles.
     */
    public boolean getScrollBarsVisible() {
        return scrollPane != null;
    }

    /**
     * Définit si les barres de défilements doivent être visibles.
     * Par défaut, les barres de défilements ne sont pas visibles.
     */
    public void setScrollBarsVisible(final boolean visible) {
        synchronized (getTreeLock()) {
            if (visible) {
                if (scrollPane == null) {
                    remove(mapPanel);
                    scrollPane = mapPanel.createScrollPane();
                    add(scrollPane, BorderLayout.CENTER);
                    validate();
                }
            } else {
                if (scrollPane != null) {
                    remove(scrollPane);
                    scrollPane = null;
                    add(mapPanel, BorderLayout.CENTER);
                    validate();
                }
            }
        }
    }

    /**
     * Spécifie si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. Spécifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    public void setPaintingWhileAdjusting(final boolean s) {
        mapPanel.setPaintingWhileAdjusting(s);
    }

    /**
     * Inscrit un objet dans la liste des objets intéressés
     * à être informés chaque fois que l'image affichée change.
     */
    public void addCoverageChangeListener(final CoverageChangeListener listener) {
        listenerList.add(CoverageChangeListener.class, listener);
    }

    /**
     * Retire un objet de la liste des objets intéressés à
     * être informés chaque fois que l'image affichée change.
     */
    public void removeCoverageChangeListener(final CoverageChangeListener listener) {
        listenerList.remove(CoverageChangeListener.class, listener);
    }

    /**
     * Préviens tous les objets {@link CoverageChangeListener}
     * que l'image affichée vient de changer.
     */
    protected void fireImageChanged(final CoverageEntry entry, final GridCoverage newImage) {
        CoverageChangeEvent event = null;
        final Object[] listeners = listenerList.getListenerList();
        for (int i=listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == CoverageChangeListener.class) {
                if (event == null) {
                    event=new CoverageChangeEvent(this, entry, newImage);
                }
                ((CoverageChangeListener)listeners[i+1]).coverageChanged(event);
            }
        }
    }

    /**
     * Libère les ressources utilisée par ce panneau. Cette methode devrait être appelée
     * lorsque la fenêtre qui contenant ce panneau est détruite. L'implémentation par
     * défaut termine le thread qui lisait des images en arrière plan.
     */
    protected synchronized void dispose() {
        if (reader != null) {
            synchronized (reader) {
                reader.kill = true;                   // must be first
                reader.setImage(null, null, 0, null); // must call 'reader.notifyAll()'.
                reader = null;
            }
        }
        setImage(null);
        mapPanel.getRenderer().dispose();
    }

    /**
     * Retourne une chaîne de caractères représentant l'image affichée
     * dans ce paneau. La chaîne retournée sera sur une seule ligne.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer.append((source!=null) ? (Object)source : (Object)mapPanel);
        buffer.append(']');
        final Renderer renderer = mapPanel.getRenderer();
        if (renderer != null) {
            final RenderedLayer[] layers = renderer.getLayers();
            if (layers != null) {
                for (int i=0; i<layers.length; i++) {
                    buffer.append('\n');
                    buffer.append(Utilities.spaces(8));
                    buffer.append(layers[i]);
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Thread ayant la charge de lire une image en arrière plan. Ce thread
     * restera continuellement en arrière plan dans l'attente d'un appel de
     * {@link #setImage}. Pour tuer ce thread, affectez la valeur <code>true</code>
     * au drapeau {@link #kill}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Reader extends Thread implements IIOReadWarningListener {
        /**
         * Image à lire. Ce champ prend la valeur <code>null</code>
         * lorsque la lecture d'une image est terminée.
         */
        private volatile CoverageEntry entry;

        /**
         * Couches à placer sur l'image, ou <code>null</code>
         * pour ne mettre que l'image.
         */
        private volatile LayerControl[] layers;

        /**
         * Nombre de millisecondes à attendre avant de commencer réellement
         * la lecture, au cas où l'utilisateur changerait d'idée.
         */
        private volatile int delay;

        /**
         * Objet à informer des progrès de la lecture,
         * ou <code>null</code> s'il n'y en a pas.
         */
        private volatile IIOReadProgressListener progress;

        /**
         * Objets à informer des progrès de la lecture ainsi
         * que des éventuels avertissements. Cette liste
         * comprendra {@link #progress} et <code>this</code>.
         */
        private final EventListenerList listeners = new EventListenerList();

        /**
         * Mettre ce drapeau à <code>true</code> pour tuer ce thread.
         */
        public transient boolean kill;

        /**
         * Construit le thread. Le thread ne démarrera pas immédiatement.
         * Il faudra appeller {@link #start} après sa construction.
         */
        public Reader(final ThreadGroup readers) {
            super(readers, UNTITLED);
            setDaemon(true);
            listeners.add(IIOReadWarningListener.class, this);
        }

        /**
         * Indique si le chargement d'une image est en cours.
         * Le fait qu'une image soit en train d'être chargée
         * n'empêche pas d'appeler {@link #setImage}. Dans ce
         * dernier cas, le chargement en cours sera annulé et
         * le chargement de la nouvelle image commencera.
         */
        public synchronized boolean isLoading() {
            return isAlive() && entry!=null;
        }

        /**
         * Définit la prochaine image à lire. Cette méthode peut être appelée même pendant
         * qu'une lecture est en cours. Dans ce cas, la lecture actuelle sera anulée et la
         * nouvelle lecture démarrera dès que possible. Cette méthode peut être appelée de
         * n'importe quel thread.
         *
         * @param entry    Image à lire. Si l'image spécifiée est la même que celle qui
         *                 est en cours de lecture, alors rien ne sera fait; on laissera
         *                 simplement la lecture actuelle poursuivre son cours.
         * @param layers   Couches à placer sur l'image, ou <code>null</code> pour
         *                 ne mettre que l'image.
         * @param delay    Nombre de millisecondes à attendre avant de commencer réellement
         *                 la lecture, au cas où l'utilisateur changerait d'idée.
         * @param progress Objet à informer des progrès de la lecture,
         *                 ou <code>null</code> s'il n'y en a pas.
         */
        public synchronized void setImage(final CoverageEntry  entry,
                                          final LayerControl[] layers,
                                          final int            delay,
                                          final IIOReadProgressListener progress)
        {
            if (this.entry!=null && !this.entry.equals(entry)) {
                this.entry.abort();
            }
            this.entry    = entry;
            this.layers   = layers;
            this.delay    = delay;
            this.progress = progress;
            notifyAll();
        }

        /**
         * Procède à la lecture de l'image, puis à son insertion dans le panneau.
         * Cette boucle tournera pendant toute la durée de vie de la fenêtre. Le
         * thread sera bloqué lorsqu'il n'y a pas d'image à lire, afin de laisser
         * le CPU aux autres.
         */
        public void run() {
            while (!kill) {
                /*
                 * Fait une copie cohérente de l'état actuel de cet objet.
                 * S'il n'y a aucune image à lire, on bloquera ce thread
                 * jusqu'à ce qu'une nouvelle image soit spécifiée.
                 */
                final CoverageEntry           entry;
                final LayerControl[]          layers;
                final int                     delay;
                final IIOReadProgressListener progress;
                synchronized (this) {
                    entry    = this.entry;
                    layers   = this.layers;
                    delay    = this.delay;
                    progress = this.progress;
                    if (entry == null) try {
                        wait(LIFETIME);
                        if (this.entry == null) {
                            kill = true;
                        }
                        continue;
                    } catch (InterruptedException exception) {
                        // Quelqu'un ne veux pas nous laisser
                        // dormir. Retourne donc au travail.
                        continue;
                    }
                }
                /*
                 * Attend un peu au cas où l'utilisateur changerait d'idée. Si le choix de
                 * l'utilisateur se confirme (entry==this.entry),  alors on démarrera la
                 * lecture réelle. Pendant la lecture, l'utilisateur peut encore changer
                 * d'idée. C'est pourquoi on testera encore (entry==this.entry) après la
                 * lecture.
                 */
                try {
                    sleep(delay);
                    if (entry.equals(this.entry)) {
                        setName(Resources.format(ResourceKeys.LOADING_$1, entry.getName()));
                        final List<RenderedLayer> visualLayers = new ArrayList<RenderedLayer>();
                        GridCoverage image = null;
                        if (layers == null) {
                            try {
                                // Note: 'progress' may be null.
                                listeners.add(IIOReadProgressListener.class, progress);
                                image = entry.getGridCoverage(listeners);
                            } finally {
                                listeners.remove(IIOReadProgressListener.class, progress);
                            }
                            if (image != null) {
                                visualLayers.add(new RenderedGridCoverage(image));
                            }
                        } else {
                            /*
                             * Si l'utilisateur a demandé à ajouter des couches,
                             * procède maintenant à la création de ces couches.
                             * Les choix des couches comprennent celle de l'image.
                             */
                            for (int i=0; i<layers.length; i++) {
                                final RenderedLayer[] newLayers;
                                try {
                                    // Note: 'progress' may be null.
                                    listeners.add(IIOReadProgressListener.class, progress);
                                    newLayers = layers[i].configLayers(null, entry, listeners);
                                } finally {
                                    listeners.remove(IIOReadProgressListener.class, progress);
                                }
                                if (newLayers != null) {
                                    for (int j=0; j<newLayers.length; j++) {
                                        final RenderedLayer layer = newLayers[j];
                                        if (layer instanceof RenderedGridCoverage) {
                                            image = ((RenderedGridCoverage)layer).getGridCoverage();
                                        }
                                        visualLayers.add(layer);
                                    }
                                }
                            }
                        }
                        if (entry.equals(this.entry)) {
                            ImageCanvas.this.setImage(visualLayers.toArray(
                                             new RenderedLayer[visualLayers.size()]), entry, image);
                        }
                    }
                } catch (Exception exception) {
                    /*
                     * En cas d'erreur, le message ne sera affichée que si on ne vient
                     * pas de démarrer une nouvelle lecture. Si on a une autre lecture
                     * sur les bras, on laisse tomber le message d'erreur.
                     */
                    synchronized (this) {
                        if (entry.equals(this.entry)) {
                            ExceptionMonitor.show(ImageCanvas.this, exception);
                        }
                    }
                }  catch (OutOfMemoryError error) {
                    /*
                     * Si on a manqué de mémoire, on n'essayera pas de lire une autre image.
                     * On arrête tout de suite même s'il y avait d'autres images dans la queue.
                     */
                    synchronized (this) {
                        this.entry    = null;
                        this.layers   = null;
                        this.delay    = 0;
                        this.progress = null;
                        this.kill     = true;
                    }
                    System.gc();
                    ExceptionMonitor.show(ImageCanvas.this, error);
                } finally {
                    /*
                     * Après la lecture (qu'il y ait eu erreur ou pas), si aucune nouvelle
                     * demande d'image n'est arrivée, on met les champs internes à 0 pour
                     * signifier qu'il n'y a plus rien à lire.
                     */
                    synchronized (this) {
                        if (entry.equals(this.entry)) {
                            this.entry    = null;
                            this.layers   = null;
                            this.delay    = 0;
                            this.progress = null;
                            setName(UNTITLED);
                        }
                    }
                }
            }
            listeners.remove(IIOReadWarningListener.class, this);
        }

        /**
         * Retourne la fenêtre interne dans laquelle apparaît l'objet {@link ImageCanvas}.
         * Cette fenêtre interne sera informé de la fin de la lecture ainsi que d'éventuels
         * messages d'avertissements. Si aucune fenêtre parente n'a été trouvée, alors cette
         * méthode retourne <code>null</code>.
         */
        private InternalFrame getInternalFrame() {
            Component parent=ImageCanvas.this;
            synchronized (getTreeLock()) {
                while ((parent=parent.getParent()) != null) {
                    if (parent instanceof InternalFrame) {
                        break;
                    }
                }
            }
            return (InternalFrame) parent;
        }
        
        /**
         * Prévient qu'une erreur non-fatale est survenue pendant le décodage de l'image.
         * Cette méthode est appelée automatiquement par {@link ImageReader} lorsqu'une
         * telle situation survient.
         *
         * @param source  L'objet {@link ImageReader} qui appelle cette méthode.
         * @param message Le message d'avertissement.
         */
        public void warningOccurred(final ImageReader source, final String message) {
            final InternalFrame parent = getInternalFrame();
            if (parent != null) {
                final CoverageEntry entry = this.entry;
                if (entry != null) {
                    parent.warning(entry.getName(), message);
                    return;
                }
            }
            DataBase.logger.warning(message);
        }
    }
}
