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
 * peuvent �tre affich�es comme par exemple l'�chelle de couleurs.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageCanvas extends JPanel {
    /**
     * Dur�e de vie des threads {@link Reader}. Si un laps de temps
     * sup�rieur � <code>LIFETIME</code> s'est �coul� sans qu'aucune
     * image ne soit demand�e, le thread mourra. Un autre thread sera
     * cr�� � la vol�e si une nouvelle image est demand�e par apr�s.
     */
    private static final int LIFETIME = 30000; // 30 secondes

    /**
     * Laps de temps � attendre avant de d�marrer effectivement la lecture
     * d'une image. On attend ce laps de temps au cas o� l'utilisateur
     * changerait d'id�e.
     */
    private static final int TIMELAG = 300;

    /**
     * The date format to use for formatting date in image's title.
     */
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    /**
     * Texte � afficher par d�faut au dessus des
     * images lorsqu'aucun titre n'a �t� sp�cifi�.
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
     * avec les barres de d�filements.
     */
    private JComponent scrollPane;

    /**
     * Processus qui est en train de faire la lecture
     * d'une image, ou <code>null</code> s'il n'y en
     * a pas encore.
     */
    private transient Reader reader;

    /**
     * Entr� ayant servit � obtenir l'image
     * pr�sentement affich�e. Ce champ peut
     * �tre nul si l'entr� n'est pas connue.
     */
    private CoverageEntry source;

    /**
     * <code>true</code> si l'�chelle de la carte doit �tre affich�e.
     */
    private boolean mapScaleVisible;

    /**
     * Construit un panneau initialement vide.  Le contenu
     * de ce panneau pourra �tre sp�cifi� par des appels �
     * la m�thode {@link #setImage}.
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
     * Retourne le paneau {@link MosaicCanvas} dans lequel est ins�r�e
     * cette image, ou <code>null</code> s'il n'y en a pas.
     */
    private MosaicCanvas getMosaic() {
        for (Container parent=getParent(); parent!=null; parent=parent.getParent()) {
            if (parent instanceof MosaicCanvas) return (MosaicCanvas)parent;
        }
        return null;
    }

    /**
     * Met � jour le titre de la fen�tre en fonction de
     * l'image pr�sentement affich�e.
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
     * Indique si l'�chelle doit �tre visible ou pas.
     */
    public void setMapScaleVisible(final boolean visible) {
        if (mapScaleVisible != visible) {
            mapScaleVisible = visible;
            repaint();
        }
    }

    /**
     * Indique si le chargement d'une image est en cours.
     * Le fait qu'une image soit en train d'�tre charg�e
     * n'emp�che pas d'appeler {@link #setImage}. Dans ce
     * dernier cas, le chargement en cours sera annul� et
     * le chargement de la nouvelle image commencera.
     */
    public synchronized boolean isLoading() {
        return reader!=null && reader.isLoading();
    }

    /**
     * Ajoute � la liste sp�cifi�e toutes les images
     * {@link GridCoverage} affich�es dans ce paneau.
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
     * Sp�cifie l'image � faire appara�tre. Une valeur nulle
     * signifie qu'aucune image ne doit �tre affich�e. Cette
     * m�thode peut �tre appel�e � partir de n'importe quel
     * thread.
     *
     * @param image Image � afficher, ou <code>null</code>
     *              pour ne plus afficher d'image.
     */
    public void setImage(final GridCoverage image) { // NO synchronize here!
        setImage((image!=null) ? new RenderedLayer[] {new RenderedGridCoverage(image)} : null, null, image);
    }

    /**
     * Sp�cifie l'image � faire appara�tre. Une valeur nulle
     * signifie qu'aucune image ne doit �tre affich�e. Cette
     * m�thode peut �tre appel�e � partir de n'importe quel
     * thread.
     *
     * @param visualLayers Couches � afficher, ou <code>null</code> pour ne plus afficher d'image.
     *                     Cette liste ne doit comprendre que des objets {@link Layers}.
     * @param entry        Entr� ayant servit � obtenir l'image, ou <code>null</code> s'il n'y en a pas.
     * @param image        Image affich�e. Cette information n'est utilis�e que pour informer les objets
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
        // Le code suivant sera ex�cut�
        // dans le thread de Swing.
        ///////////////////////////////
        synchronized (getTreeLock()) {
            this.source = null;
            /*
             * Obtient les coordonn�es g�ographiques de la r�gion qui �tait
             * couverte par les anciennes images.   On fera en sorte que la
             * nouvelle image couvre les m�mes coordonn�es.
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
             * Ins�re la nouvelle image. Pendant le remplacement de l'image,
             * on d�sactivera temporairement la synchronisation des images.
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
     * Sp�cifie l'image � faire appara�tre. Si l'image n'�tait pas d�j� en
     * m�moire, elle sera automatiquement lue en arri�re-plan � partir de
     * son fichier. Cette m�thode peut �tre appel�e � partir de n'importe
     * quel thread.
     *
     * @param image    Entr�e de la base de donn�es qui d�crit l'image � lire.
     * @param layers   Couches � placer sur l'image, ou <code>null</code> pour
     *                 ne mettre que l'image.
     * @param progress Objet � informer des progr�s de la lecture,
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
     * Proc�de � un nouveau chargement de l'image en m�moire. Cette m�thode ne vide
     * pas les caches internes. Si l'image � relire est encore dans la cache, la
     * cache sera utilis�e.
     */
    final synchronized void reload(final LayerControl[] layers, final StatusBar statusBar) {
        setImage(source, layers, (source!=null) ?
                    statusBar.getIIOReadProgressListener(source.getName()) : null);
    }

    /**
     * Indique si les barres de d�filements sont visibles.
     * Par d�faut, les barres de d�filements ne sont pas
     * visibles.
     */
    public boolean getScrollBarsVisible() {
        return scrollPane != null;
    }

    /**
     * D�finit si les barres de d�filements doivent �tre visibles.
     * Par d�faut, les barres de d�filements ne sont pas visibles.
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
     * Sp�cifie si les cartes doivent �tre redessin�es
     * durant les glissements des ascenceurs. Sp�cifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    public void setPaintingWhileAdjusting(final boolean s) {
        mapPanel.setPaintingWhileAdjusting(s);
    }

    /**
     * Inscrit un objet dans la liste des objets int�ress�s
     * � �tre inform�s chaque fois que l'image affich�e change.
     */
    public void addCoverageChangeListener(final CoverageChangeListener listener) {
        listenerList.add(CoverageChangeListener.class, listener);
    }

    /**
     * Retire un objet de la liste des objets int�ress�s �
     * �tre inform�s chaque fois que l'image affich�e change.
     */
    public void removeCoverageChangeListener(final CoverageChangeListener listener) {
        listenerList.remove(CoverageChangeListener.class, listener);
    }

    /**
     * Pr�viens tous les objets {@link CoverageChangeListener}
     * que l'image affich�e vient de changer.
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
     * Lib�re les ressources utilis�e par ce panneau. Cette methode devrait �tre appel�e
     * lorsque la fen�tre qui contenant ce panneau est d�truite. L'impl�mentation par
     * d�faut termine le thread qui lisait des images en arri�re plan.
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
     * Retourne une cha�ne de caract�res repr�sentant l'image affich�e
     * dans ce paneau. La cha�ne retourn�e sera sur une seule ligne.
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
     * Thread ayant la charge de lire une image en arri�re plan. Ce thread
     * restera continuellement en arri�re plan dans l'attente d'un appel de
     * {@link #setImage}. Pour tuer ce thread, affectez la valeur <code>true</code>
     * au drapeau {@link #kill}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Reader extends Thread implements IIOReadWarningListener {
        /**
         * Image � lire. Ce champ prend la valeur <code>null</code>
         * lorsque la lecture d'une image est termin�e.
         */
        private volatile CoverageEntry entry;

        /**
         * Couches � placer sur l'image, ou <code>null</code>
         * pour ne mettre que l'image.
         */
        private volatile LayerControl[] layers;

        /**
         * Nombre de millisecondes � attendre avant de commencer r�ellement
         * la lecture, au cas o� l'utilisateur changerait d'id�e.
         */
        private volatile int delay;

        /**
         * Objet � informer des progr�s de la lecture,
         * ou <code>null</code> s'il n'y en a pas.
         */
        private volatile IIOReadProgressListener progress;

        /**
         * Objets � informer des progr�s de la lecture ainsi
         * que des �ventuels avertissements. Cette liste
         * comprendra {@link #progress} et <code>this</code>.
         */
        private final EventListenerList listeners = new EventListenerList();

        /**
         * Mettre ce drapeau � <code>true</code> pour tuer ce thread.
         */
        public transient boolean kill;

        /**
         * Construit le thread. Le thread ne d�marrera pas imm�diatement.
         * Il faudra appeller {@link #start} apr�s sa construction.
         */
        public Reader(final ThreadGroup readers) {
            super(readers, UNTITLED);
            setDaemon(true);
            listeners.add(IIOReadWarningListener.class, this);
        }

        /**
         * Indique si le chargement d'une image est en cours.
         * Le fait qu'une image soit en train d'�tre charg�e
         * n'emp�che pas d'appeler {@link #setImage}. Dans ce
         * dernier cas, le chargement en cours sera annul� et
         * le chargement de la nouvelle image commencera.
         */
        public synchronized boolean isLoading() {
            return isAlive() && entry!=null;
        }

        /**
         * D�finit la prochaine image � lire. Cette m�thode peut �tre appel�e m�me pendant
         * qu'une lecture est en cours. Dans ce cas, la lecture actuelle sera anul�e et la
         * nouvelle lecture d�marrera d�s que possible. Cette m�thode peut �tre appel�e de
         * n'importe quel thread.
         *
         * @param entry    Image � lire. Si l'image sp�cifi�e est la m�me que celle qui
         *                 est en cours de lecture, alors rien ne sera fait; on laissera
         *                 simplement la lecture actuelle poursuivre son cours.
         * @param layers   Couches � placer sur l'image, ou <code>null</code> pour
         *                 ne mettre que l'image.
         * @param delay    Nombre de millisecondes � attendre avant de commencer r�ellement
         *                 la lecture, au cas o� l'utilisateur changerait d'id�e.
         * @param progress Objet � informer des progr�s de la lecture,
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
         * Proc�de � la lecture de l'image, puis � son insertion dans le panneau.
         * Cette boucle tournera pendant toute la dur�e de vie de la fen�tre. Le
         * thread sera bloqu� lorsqu'il n'y a pas d'image � lire, afin de laisser
         * le CPU aux autres.
         */
        public void run() {
            while (!kill) {
                /*
                 * Fait une copie coh�rente de l'�tat actuel de cet objet.
                 * S'il n'y a aucune image � lire, on bloquera ce thread
                 * jusqu'� ce qu'une nouvelle image soit sp�cifi�e.
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
                 * Attend un peu au cas o� l'utilisateur changerait d'id�e. Si le choix de
                 * l'utilisateur se confirme (entry==this.entry),  alors on d�marrera la
                 * lecture r�elle. Pendant la lecture, l'utilisateur peut encore changer
                 * d'id�e. C'est pourquoi on testera encore (entry==this.entry) apr�s la
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
                             * Si l'utilisateur a demand� � ajouter des couches,
                             * proc�de maintenant � la cr�ation de ces couches.
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
                     * En cas d'erreur, le message ne sera affich�e que si on ne vient
                     * pas de d�marrer une nouvelle lecture. Si on a une autre lecture
                     * sur les bras, on laisse tomber le message d'erreur.
                     */
                    synchronized (this) {
                        if (entry.equals(this.entry)) {
                            ExceptionMonitor.show(ImageCanvas.this, exception);
                        }
                    }
                }  catch (OutOfMemoryError error) {
                    /*
                     * Si on a manqu� de m�moire, on n'essayera pas de lire une autre image.
                     * On arr�te tout de suite m�me s'il y avait d'autres images dans la queue.
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
                     * Apr�s la lecture (qu'il y ait eu erreur ou pas), si aucune nouvelle
                     * demande d'image n'est arriv�e, on met les champs internes � 0 pour
                     * signifier qu'il n'y a plus rien � lire.
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
         * Retourne la fen�tre interne dans laquelle appara�t l'objet {@link ImageCanvas}.
         * Cette fen�tre interne sera inform� de la fin de la lecture ainsi que d'�ventuels
         * messages d'avertissements. Si aucune fen�tre parente n'a �t� trouv�e, alors cette
         * m�thode retourne <code>null</code>.
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
         * Pr�vient qu'une erreur non-fatale est survenue pendant le d�codage de l'image.
         * Cette m�thode est appel�e automatiquement par {@link ImageReader} lorsqu'une
         * telle situation survient.
         *
         * @param source  L'objet {@link ImageReader} qui appelle cette m�thode.
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
