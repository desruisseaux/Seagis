/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.map;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.opengis.cs.GeographicCoordinateSystem;
import net.seas.opengis.ct.CannotCreateTransformException;
import net.seas.util.OpenGIS;

// Geometry
import java.awt.Shape;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import net.seas.util.XDimension2D;

// Graphics
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import javax.media.jai.GraphicsJAI;

// User interface
import net.seas.awt.ZoomPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

// Events
import net.seas.awt.event.ZoomChangeListener;
import net.seas.awt.event.ZoomChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.EventQueue;

// Collections
import java.util.Arrays;
import java.util.Comparator;

// Miscellaneous
import net.seas.resources.Resources;
import net.seas.awt.ExceptionMonitor;


/**
 * Dessine une carte dans une composante <i>Swing</i>.
 * Cette carte peut comprendre le tracé d'une côte ainsi que toute sa bathymétrie. De
 * façon optionnelle, on peut aussi placer une image satellitaire sous la bathymétrie
 * tracée. N'importe quelle image convient, pourvu qu'on en connaisse les coordonnées.
 * Le tout forme un ensemble dans lequel l'usager peut naviguer à l'aide des touches
 * du clavier ou de la souris.
 *
 * <p>Notez que la classe <code>MapPanel</code> gère les barres de défilements
 * d'une manière particulière. Il ne faut donc pas placer d'objet <code>MapPanel</code> dans
 * un objet {@link javax.swing.JScrollPane}.    Pour construire et faire apparaître un objet
 * <code>MapPanel</code> avec ses barres de défilements,  il faut utiliser la méthode {@link
 * #createScrollPane}.</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class MapPanel extends ZoomPane
{
    /**
     * Objet utilisé pour comparer deux objets {@link Layer}.
     * Ce comparateur permettra de classer les {@link Layer}
     * par ordre croissant d'ordre <var>z</var>.
     */
    private static final Comparator<Layer> COMPARATOR=new Comparator<Layer>()
    {
        public int compare(final Layer layer1, final Layer layer2)
        {
            final float z1 = layer1.getZOrder();
            final float z2 = layer2.getZOrder();
            if (z1<z2) return -1;
            if (z1>z2) return +1;
            return 0;
        }
    };

    /**
     * Système de coordonnées utilisé pour l'affichage à l'écran. Les données des différentes
     * couches devront être converties selon ce système de coordonnées avant d'être affichées.
     * La transformation la plus courante utilisée à cette fin peut être conservée dans le
     * champ <code>commonestCoordinateTransform</code> à des fins de performances.
     */
    private final CoordinateSystem displayCoordinateSystem;

    /**
     * Transformation de coordonnées (généralement une projection cartographique) utilisée
     * le plus souvent pour convertir les coordonnées des couches en coordonnées d'affichage.
     * Ce champ n'est conservé qu'à des fins de performances. Si ce champ est nul, ça signifie
     * qu'il a besoin d'être reconstruit.
     */
    private transient CoordinateTransform commonestCoordinateTransform;

    /**
     * Rectangle englobant les coordonnées de l'ensemble des couches à tracer. Les coordonnées de
     * ce rectangle sont exprimées selon le système de coordonnées de l'affichage. Ce rectangle est
     * recalculé chaque fois que le système de coordonnées change. Une valeur nulle signifie qu'aucune
     * méthode <code>project({@link Layer#getPreferredArea})</code> n'a retourné de valeur non-nulle.
     */
    private transient Rectangle2D area;

    /**
     * Indique si les couches {@link #layers}
     * sont classées. La valeur <code>false</code>
     * indique qu'ils devront être reclassés.
     */
    private transient boolean layerSorted;

    /**
     * Nombre d'objets valides dans le
     * tableau <code>layers</code>.
     */
    private int layerCount;

    /**
     * Images satellitaires, bathymétrie, stations, échelles
     * ou autres couches à dessiner sur la carte.
     */
    private Layer[] layers;

    /**
     * Largeur des lignes à tracer. La valeur <code>null</code>
     * signifie que cette largeur doit être recalculée.
     */
    private Stroke stroke;

    /**
     * Indique si le prochain traçage ({@link #paintComponent(Graphics2D)}
     * sera en fait une impression. Si c'est le cas, alors il ne faudra pas
     * mettre à jour certains champs internes.
     */
    private transient boolean isPrinting;

    /**
     * Objet "listener" ayant la charge de réagir aux différents
     * événements qui intéressent cet objet <code>MapPanel</code>.
     */
    private final transient Listeners listeners=new Listeners();

    /**
     * Classe ayant la charge de réagir aux différents événements qui intéressent cet
     * objet <code>MapPanel</code>. Cette classe réagira entre autres aux changements
     * de l'ordre <var>z</var> ainsi qu'aux changements des coordonnées géographiques
     * d'une couche.
     */
    private final class Listeners extends MouseAdapter implements ComponentListener, PropertyChangeListener, ZoomChangeListener
    {
        public void zoomChanged     (final ZoomChangeEvent     event) {MapPanel.this.zoomChanged (event);}
        public void mouseClicked    (final MouseEvent          event) {MapPanel.this.mouseClicked(event);}
        public void componentResized(final ComponentEvent      event) {}
        public void componentMoved  (final ComponentEvent      event) {}
        public void componentShown  (final ComponentEvent      event) {}
        public void componentHidden (final ComponentEvent      event) {clearCache();}
        public void propertyChange  (final PropertyChangeEvent event)
        {
            if (EventQueue.isDispatchThread())
            {
                final String propertyName = event.getPropertyName();
                if (propertyName.equals("preferredArea"))
                {
                    changeArea((Rectangle2D) event.getOldValue(), (Rectangle2D) event.getNewValue(), ((Layer) event.getSource()).getCoordinateSystem());
                }
                else if (propertyName.equals("preferredPixelSize"))
                {
                    stroke=null;
                }
                else if (propertyName.equals("zOrder"))
                {
                    layerSorted=false;
                }
            }
            else EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {propertyChange(event);}
            });
        }
    }

    /**
     * Construit un panneau avec un système de coordonnées par défaut.
     * Ce système utilisera des (<var>longitude</var>,<var>latitude</var>).
     * Le panneau ne contiendra initialement aucune couche. Les couches
     * pourront être ajoutées par des appels à {@link #addLayer}.
     */
    public MapPanel()
    {this(GeographicCoordinateSystem.WGS84);}

    /**
     * Construit un panneau avec ln système de coordonnées spécifié.
     * Le panneau ne contiendra initialement aucune couche. Les couches
     * pourront être ajoutées par des appels à {@link #addLayer}.
     *
     * @param displayCoordinateSystem Système de coordonnées utilisé pour l'affichage
     *        de toutes les couches. Cet argument ne doit pas être nul.
     */
    public MapPanel(final CoordinateSystem displayCoordinateSystem)
    {
        super(TRANSLATE_X | TRANSLATE_Y | UNIFORM_SCALE | DEFAULT_ZOOM | ROTATE | RESET);
        if (displayCoordinateSystem==null) throw new NullPointerException();
        this.displayCoordinateSystem = displayCoordinateSystem;
        addZoomChangeListener(listeners);
        addComponentListener (listeners);
        addMouseListener     (listeners);
    }

    /**
     * Retourne le système de coordonnées utilisé pour l'affichage de toutes les couches.
     * Toutefois, les <em>données</em> des couches ne seront pas nécessairement exprimées
     * selon ce système de coordonnées. Les conversions nécessaires seront faites au vol
     * lors de l'affichage.
     */
    public CoordinateSystem getCoordinateSystem()
    {return displayCoordinateSystem;}

    /**
     * Retourne un rectangle englobant les coordonnées de l'ensemble des couches à tracer.
     * Les coordonnées de ce rectangle seront exprimées selon le système de coordonnées
     * retourné par {@link #getCoordinateSystem}. Si ces coordonnées sont inconnues (par
     * exemple si <code>MapPanel</code> ne contient aucune couche), alors cette méthode
     * retourne <code>null</code>.
     */
    public Rectangle2D getArea()
    {
        final Rectangle2D area=this.area;
        return (area!=null) ? (Rectangle2D) area.clone() : null;
    }

    /**
     * Modifie les coordonnées du rectangle qui englobe les coordonnées de l'ensemble
     * des couches à tracer.
     */
    private void setArea(final Rectangle2D newArea)
    {
        final Rectangle2D oldArea=this.area;
        if (oldArea!=newArea && (oldArea==null || !oldArea.equals(newArea)))
        {
            this.area=newArea;
            firePropertyChange("area", oldArea, newArea);
            if (oldArea==null && newArea!=null)
            {
                ToolTipManager.sharedInstance().registerComponent(this);
            }
            else if (newArea==null && super.getToolTipText()==null)
            {
                ToolTipManager.sharedInstance().unregisterComponent(this);
            }
            fireZoomChanged(new AffineTransform()); // Update scrollbars
        }
    }

    /**
     * Recalcule inconditionnelement la valeur du champ {@link #area}.
     * Sa valeur sera calculée à partir des informations retournées par
     * toutes les couches de cette carte.
     */
    private void computeArea()
    {
        Rectangle2D           newArea = null;
        CoordinateSystem   lastSystem = null;
        CoordinateTransform transform = null;
        for (int i=layerCount; --i>=0;)
        {
            final Layer layer=layers[i];
            Rectangle2D bounds=layer.getPreferredArea();
            if (bounds!=null)
            {
                final CoordinateSystem system=layer.getCoordinateSystem();
                try
                {
                    if (lastSystem==null || !lastSystem.equivalents(system))
                    {
                        transform  = getTransformFrom(system);
                        lastSystem = system;
                    }
                    bounds = OpenGIS.transform(transform, bounds, null);
                    if (newArea==null) newArea=bounds;
                    else newArea.add(bounds);
                }
                catch (TransformException exception)
                {
                    handleException("MapPanel", "addLayer", exception);
                }
            }
        }
        setArea(newArea);
    }

    /**
     * Remplace un rectangle par un autre dans le calcul de {@link #area}. Si ça a eu pour effet de
     * changer les coordonnées géographiques couvertes, un événement approprié sera lancé. Cette
     * méthode est plus économique que {@link #computeArea} du fait qu'elle essaie de ne pas tout
     * recalculer. Si on n'a pas pu faire l'économie d'un recalcul toutefois, alors {@link #computeArea}
     * sera appelée.
     */
    private void changeArea(Rectangle2D oldSubArea, Rectangle2D newSubArea, final CoordinateSystem system)
    {
        try
        {
            final CoordinateTransform transform = getTransformFrom(system);
            oldSubArea=OpenGIS.transform(transform, oldSubArea, null);
            newSubArea=OpenGIS.transform(transform, newSubArea, null);
        }
        catch (TransformException exception)
        {
            handleException("MapPanel", "addLayer", exception);
            computeArea();
            return;
        }
        final Rectangle2D expandedArea = Layer.changeArea(area, oldSubArea, newSubArea);
        if (expandedArea!=null)
        {
            setArea(expandedArea);
        }
        else computeArea();
    }

    /**
     * Retourne une transformation permettant de convertir les coordonnées exprimées selon le
     * système <code>source</code> vers le système d'affichage {@link #displayCoordinateSystem}.
     */
    private CoordinateTransform getTransformFrom(final CoordinateSystem source) throws CannotCreateTransformException
    {
        final CoordinateTransform transform=commonestCoordinateTransform;
        if (transform!=null && transform.getSourceCS().equivalents(source))
        {
            return transform;
        }
        else return Contour.TRANSFORMS.createFromCoordinateSystems(source, displayCoordinateSystem);
    }

    /**
     * Retourne la transformation la plus couramment utilisée pour convertir les coordonnées
     * des couches en coordonnées d'affichage. Cette transformation sera conservée dans une
     * cache interne pour améliorer les performances.
     */
    final CoordinateTransform getCommonestCoordinateTransform() throws CannotCreateTransformException
    {
        if (commonestCoordinateTransform==null)
        {
            int count=0;
            final CoordinateSystem[] systems=new CoordinateSystem[layerCount];
            final int[]          equalsCount=new int             [layerCount];
            final int[]      equivalentCount=new int             [layerCount];
            /*
             * Compte le nombre d'occurences de
             * chaque systèmes de coordonnées.
             */
            for (int i=layerCount; --i>=0;)
            {
                boolean found=false;
                final CoordinateSystem sys=layers[i].getCoordinateSystem();
                for (int j=0; j<count; j++)
                {
                    if (sys.equals     (systems[j])) {    equalsCount[j]++; found=true;}
                    if (sys.equivalents(systems[j])) {equivalentCount[j]++;}
                }
                if (!found)
                {
                    systems[count++] = sys;
                }
            }
            /*
             * Recherche dans la liste le
             * système le plus souvent utilisé.
             */
            int max=layerCount-1;
            for (int i=max; --i>=0;)
            {
                if ((equivalentCount[i]> equivalentCount[max]) ||
                    (equivalentCount[i]==equivalentCount[max] && equalsCount[i]>=equalsCount[max]))
                {
                    max=i;
                }
            }
            commonestCoordinateTransform = Contour.TRANSFORMS.createFromCoordinateSystems((max>=0) ? systems[max] : displayCoordinateSystem, displayCoordinateSystem);
        }
        return commonestCoordinateTransform;
    }

    /**
     * Ajoute une couche à la liste des couches à dessiner. Une couches peut être par
     * exemple une station ou une échelle. Une station pourra être un simple point, un
     * vecteur de courant ou une ellipse de marée (voir {@link net.seas.map.layer.MarkLayer}
     * et ses classes dérivées).
     *
     * @param  layer Composante à ajouter à la liste des couches à tracer.
     *         Rien ne sera fait si cette couche apparaissait déjà dans la liste.
     * @throws IllegalArgumentException si la couche spécifiée ne peut pas être ajoutée
     *         (par exemple si elle appartenait déjà à un autre objet {@link MapPanel}).
     *
     * @see #getLayers
     * @see #removeLayer
     */
    public synchronized void addLayer(final Layer layer) throws IllegalArgumentException
    {
        synchronized (layer)
        {
            if (layer.mapPanel == this)
            {
                return;
            }
            if (layer.mapPanel != null)
            {
                throw new IllegalArgumentException(Resources.format(Clé.MAPPANEL_NOT_OWNER));
            }
            layer.mapPanel = this;
            /*
             * Ajoute la nouvelle couche dans le tableau {@link #layers}. Le tableau
             * sera agrandit si nécessaire et on déclarera qu'il a besoin d'être reclassé.
             */
            if (layers==null)
            {
                layers = new Layer[16];
            }
            if (layerCount >= layers.length)
            {
                final Layer[] oldArrays = layers;
                layers = new Layer[Math.max(layerCount,8) << 1];
                System.arraycopy(oldArrays, 0, layers, 0, layerCount);
            }
            layers[layerCount++]=layer;
            layerSorted=false;
            layer.setVisible(true);
            changeArea(null, layer.getPreferredArea(), layer.getCoordinateSystem());
            layer.addPropertyChangeListener(listeners);

            commonestCoordinateTransform = null;
            stroke                       = null;
        }
        if (layerCount==1)
        {
            reset();
        }
        repaint(); // Must be invoked last
    }

    /**
     * Retire une couche de la liste des couches à dessiner.
     *
     * @param  layer Composante à retirer. Rien ne sera fait si
     *         cette couche avait déjà été retirée.
     * @throws IllegalArgumentException si la couche appartenait à
     *         un autre objet {@link MapPanel} que <code>this</code>.
     *
     * @see #addLayer
     * @see #getLayers
     */
    public synchronized void removeLayer(final Layer layer) throws IllegalArgumentException
    {
        synchronized (layer)
        {
            if (layer.mapPanel == null)
            {
                return;
            }
            if (layer.mapPanel != this)
            {
                throw new IllegalArgumentException(Resources.format(Clé.MAPPANEL_NOT_OWNER));
            }
            repaint(); // Must be invoked first
            /*
             * Retire cette couche de la liste {@link #layers}. On recherchera
             * toutes les occurences de cette couche, même si en principe elle ne
             * devrait apparaître qu'une et une seule fois.
             */
            layer.removePropertyChangeListener(listeners);
            layer.setVisible(false);
            layer.clearCache();
            layer.mapPanel=null;
            for (int i=layerCount; --i>=0;)
            {
                final Layer scan=layers[i];
                if (scan==layer)
                {
                    System.arraycopy(layers, i+1, layers, i, (--layerCount)-i);
                    layers[layerCount]=null;
                }
            }
            changeArea(layer.getPreferredArea(), null, layer.getCoordinateSystem());

            commonestCoordinateTransform = null;
            stroke                       = null;
        }
    }

    /**
     * Retire toutes les couches de cette carte.
     */
    public synchronized void removeAllLayers()
    {
        repaint();
        for (int i=layerCount; --i>=0;)
        {
            final Layer layer=layers[i];
            synchronized (layer)
            {
                layer.removePropertyChangeListener(listeners);
                layer.setVisible(false);
                layer.clearCache();
                layer.mapPanel=null;
            }
            layers[i]=null;
        }
        layerCount=0;
        commonestCoordinateTransform = null;
        stroke                       = null;
        setArea(null);
    }

    /**
     * Renvoie les couches à dessiner sur cette carte (stations ou échelles par
     * exemple). Les couches seront classées comme suit: la première couche
     * à dessiner apparaîtra à l'index 0, la seconde couche à dessiner apparaîtra
     * à l'index 1 et ainsi de suite. Cet ordre sera déterminé à partir de l'ordre Z
     * retourné par {@link Layer#getZOrder}. S'il n'y a aucune couche à
     * dessiner, le tableau retourné sera de longueur 0, mais ne sera jamais nul.
     *
     * @see #addLayer(Layer)
     * @see #removeLayer(Layer)
     */
    public synchronized Layer[] getLayers()
    {
        sortLayers();
        if (layers!=null)
        {
            final Layer[] array=new Layer[layerCount];
            System.arraycopy(layers, 0, array, 0, layerCount);
            return array;
        }
        else return new Layer[0];
    }

    /**
     * Retourne le nombre de couches
     * à dessiner sur cette carte.
     */
    public synchronized int getLayerCount()
    {return layerCount;}

    /**
     * Version non-synchronisée de
     * {@link #getLayerCount}.
     */
    final int getLayerCountFast()
    {return layerCount;}

    /**
     * Procède au classement immédiat des
     * couches, si ce n'était pas déjà fait.
     */
    private void sortLayers()
    {
        if (layers!=null)
        {
            final Layer[]    oldArray=layers;
            final int length=oldArray.length;
            if (length!=layerCount)
            {
                layers=new Layer[layerCount];
                System.arraycopy(oldArray, 0, layers, 0, layerCount);
            }
            if (!layerSorted)
            {
                Arrays.sort(layers, COMPARATOR);
                layerSorted=true;
            }
        }
    }

    /**
     * Défini le texte à afficher par défaut lorsque le curseur de la souris
     * traine sur la carte. Le texte <code>tooltip</code> sera affiché si
     * aucune couche {@link Layer} n'a pris proposé de texte.
     * La valeur <code>null</code> implique qu'aucun texte ne sera affiché
     * dans ce cas.
     */
    public void setToolTipText(final String tooptip)
    {
        super.setToolTipText(tooptip);
        if (tooptip==null && area!=null)
        {
            ToolTipManager.sharedInstance().registerComponent(this);
        }
    }

    /**
     * Retourne un texte a afficher lorsque le curseur de la souris traîne sur la
     * carte. L'implémentation par défaut vérifie si le curseur se trouve sur un
     * des éléments ajoutés à la carte (typiquement une des stations) et appelle
     * sa méthode {@link Layer#getToolTipText}. Cette méthode n'a normalement
     * pas besoin d'être utilisée directement. Elle est utilisé automatiquement par
     * <i>Swing</i>.
     *
     * @param  event Position de la souris.
     * @return Le texte à afficher, ou <code>null</code> s'il n'y a rien
     *         à afficher pour la position <code>event</code> de la souris.
     */
    public String getToolTipText(final MouseEvent event)
    {
        sortLayers();
        final int                  x = event.getX();
        final int                  y = event.getY();
        final Layer[]         layers = this.layers;
        final GeoMouseEvent geoEvent = (GeoMouseEvent) event;
        for (int i=layerCount; --i>=0;)
        {
            final Layer layer=layers[i];
            if (layer.contains(x,y))
            {
                final String tooltip=layer.getToolTipText(geoEvent);
                if (tooltip!=null) return tooltip;
            }
        }
        return super.getToolTipText(event);
    }

    /**
     * Méthode appelée automatiquement lorsque l'utilisateur a cliqué sur le bouton droit de
     * la souris. Cette méthode vérifie si le clic s'est fait sur une couche et appelle
     * {@link Layer#getPopupMenu} si c'est le cas. Si aucune couche ne propose de
     * menu, alors cette méthode appele {@link #getDefaultPopupMenu}.
     *
     * @param event Evénement de la souris contenant entre autre les coordonnées pointées.
     * @return Le menu contextuel, ou <code>null</code> pour ne pas faire apparaître de menu.
     */
    protected JPopupMenu getPopupMenu(final MouseEvent event)
    {
        sortLayers();
        final int                  x = event.getX();
        final int                  y = event.getY();
        final Layer[]         layers = this.layers;
        final GeoMouseEvent geoEvent = (GeoMouseEvent) event;
        for (int i=layerCount; --i>=0;)
        {
            final Layer layer=layers[i];
            if (layer.contains(x,y))
            {
                final JPopupMenu menu=layer.getPopupMenu(geoEvent);
                if (menu!=null) return menu;
            }
        }
        return getDefaultPopupMenu(geoEvent);
    }

    /**
     * Retourne le menu par défaut à faire apparaître lorsque l'utilisateur a cliqué
     * sur le bouton droit de la souris et qu'aucune couche n'a proposé de menu.
     * L'implémentation par défaut retourne un menu contextuel dans lequel figure des
     * options de navigations.
     */
    protected JPopupMenu getDefaultPopupMenu(final GeoMouseEvent event)
    {return super.getPopupMenu(event);}

    /**
     * Méthode appellée chaque fois que le bouton de la souris a été cliqué. L'implémentation par
     * défaut appele les méthodes {@link Layer#mouseClicked} de toutes les couches (en commençant
     * par celles qui apparaissent par dessus les autres) jusqu'à ce que l'une des couches appelle
     * {@link MouseEvent#consume}.
     */
    protected void mouseClicked(final MouseEvent event)
    {
        if ((event.getModifiers() & MouseEvent.BUTTON1_MASK)!=0)
        {
            sortLayers();
            final int                  x = event.getX();
            final int                  y = event.getY();
            final Layer[]         layers = this.layers;
            final GeoMouseEvent geoEvent = (GeoMouseEvent) event;
            for (int i=layerCount; --i>=0;)
            {
                final Layer layer=layers[i];
                if (layer.contains(x,y))
                {
                    layer.mouseClicked(geoEvent);
                    if (geoEvent.isConsumed()) break;
                }
            }
        }
    }

    /**
     * Construit une chaîne de caractères représentant la valeur pointée par la souris.  En général (mais pas
     * obligatoirement), lorsque cette méthode est appelée, le buffer <code>toAppendTo</code> contiendra déjà
     * une chaîne de caractères représentant les coordonnées pontées par la souris. Cette méthode est appelée
     * pour donner une chance aux couches d'ajouter d'autres informations pertinentes. Par exemple les couches
     * qui représentent une image satellitaire de température peuvent ajouter à <code>toAppendTo</code> un texte
     * du genre "12°C" (sans espaces au début).
     *
     * @param  event Coordonnées du curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette méthode a ajouté des informations dans <code>toAppendTo</code>.
     *
     * @see MouseCoordinateFormat
     */
    final boolean getLabel(final GeoMouseEvent event, final StringBuffer toAppendTo)
    {
        // On appele pas 'sortLayer' de façon systèmétique afin de gagner un peu en performance.
        // Cette méthode peut être appelée très souvent (à chaque déplacement de la souris).
        if (!layerSorted) sortLayers();
        final int          x = event.getX();
        final int          y = event.getY();
        final Layer[] layers = this.layers;
        for (int i=layerCount; --i>=0;)
        {
            final Layer layer=layers[i];
            if (layer.contains(x,y))
            {
                if (layer.getLabel(event, toAppendTo))
                    return true;
            }
        }
        return false;
    }

    /**
     * Effectue le traitement d'un évènement de la souris. Cette méthode convertie
     * l'événemenent {@link MouseEvent} en événement {@link GeoMouseEvent}, puis le
     * transmets à <code>super.processMouseEvent(MouseEvent)</code> pour distribution
     * auprès des objets intéréssés.
     */
    protected void processMouseEvent(final MouseEvent event)
    {super.processMouseEvent(new GeoMouseEvent(event, this));}

    /**
     * Effectue le traitement d'un mouvement de la souris. Cette méthode convertie
     * l'événemenent {@link MouseEvent} en événement {@link GeoMouseEvent}, puis le
     * transmets à <code>super.processMouseEvent(MouseEvent)</code> pour distribution
     * auprès des objets intéréssés.
     */
    protected void processMouseMotionEvent(final MouseEvent event)
    {super.processMouseMotionEvent(new GeoMouseEvent(event, this));}

    /**
     * Efface les données qui avaient été conservées dans une cache interne. L'appel
     * de cette méthode permettra de libérer un peu de mémoire à d'autres fins. Elle
     * devrait être appelée lorsque l'on sait qu'on n'affichera plus cette carte
     * avant un certain temps. Par exemple la méthode {@link java.applet.Applet#stop}
     * devrait appeller <code>clearCache()</code>. Notez que l'appel de cette méthode
     * ne modifie aucunement le paramétrage de cette carte. Seulement, son prochain
     * traçage sera plus lent, le temps que <code>MapPanel</code> reconstruise les
     * caches internes.
     */
    private void clearCache()
    {
        for (int i=layerCount; --i>=0;)
            layers[i].clearCache();
        System.gc();
    }

    /**
     * Retourne la dimension logique préférée
     * des pixels lors d'un zoom rapproché.
     */
    protected Dimension2D getPreferredPixelSize()
    {
        double minWidth  = Double.POSITIVE_INFINITY;
        double minHeight = Double.POSITIVE_INFINITY;
        for (int i=layerCount; --i>=0;)
        {
            final Dimension2D size=layers[i].getPreferredPixelSize();
            if (size!=null)
            {
                final double width  = size.getWidth();
                final double height = size.getHeight();
                if (width  < minWidth ) minWidth =width;
                if (height < minHeight) minHeight=height;
            }
        }
        if (!Double.isInfinite(minWidth) && !Double.isInfinite(minHeight))
        {
            return new XDimension2D.Double(minWidth, minHeight);
        }
        else return super.getPreferredPixelSize();
    }

    /*
     * Retourne les coordonnées projetées de la région visible à l'écran. Ces coordonnées
     * sont exprimées en mètres, ou un degrés si aucune projection cartographique n'a été
     * définie. Cette information peut être utilisée par certaines couches qui voudraient
     * utiliser un système de caches, pour ne faire de coûteux calculs que sur leurs parties
     * visibles.
     *
     * @param expand Facteur d'agrandissement du rectangle. La valeur 0 fera retourner un
     *        rectangle délimitant exactement la partie visible, tandis que la valeur 0.25
     *        (par exemple) fera retourner un rectangle agrandit de 25%.
     *        Lorsque <code>getArea</code> est utilisée pour délimiter une région à placer
     *        dans une cache, il est conseillé de se laisser une marge (par exemple de 25%)
     *        pour que la cache puisse être encore utilisée même après quelques translations.
     */
//  private static Rectangle2D getVisibleArea(final Rectangle2D visibleArea, final float expand)
//  {
//      final double trans  = 0.5*expand;
//      final double scale  = 1.0+expand;
//      final double width  = visibleArea.getWidth();
//      final double height = visibleArea.getHeight();
//      return new Rectangle2D.Double(visibleArea.getX()-trans*width,
//                                    visibleArea.getY()-trans*height,
//                                                       scale*width,
//                                                       scale*height);
//  }

    /**
     * Dessine la carte avec toutes ces couches visibles. Les couches
     * auront été ajoutées à la carte avec {@link #addLayer}. Les zooms
     * sont automatiquement pris en compte.
     *
     * @param graph Graphique à utiliser pour dessiner la carte.
     */
    protected void paintComponent(final Graphics2D graph)
    {
        sortLayers();
        if (stroke==null)
        {
            stroke=new BasicStroke((float) (4*getPreferredPixelSize().getHeight()));
        }
        final Layer[]          layers = this.layers;
        final GraphicsJAI    graphics = GraphicsJAI.createGraphicsJAI(graph, this);
        final Rectangle    clipBounds = graphics.getClipBounds();
        final MapPaintContext context;
        try
        {
            context = new MapPaintContext(getCommonestCoordinateTransform(), new AffineTransform(zoom), graphics.getTransform(), getZoomableBounds(null), isPrinting);
        }
        catch (CannotCreateTransformException exception)
        {
            ExceptionMonitor.paintStackTrace(graphics, getBounds(), exception);
            handleException("MapPanel", "paintComponent", exception);
            return;
        }
        graphics.transform(zoom);
        graphics.setStroke(stroke);
        /*
         * Dessine les couches en commençant par
         * celles qui ont un <var>z</var> le plus bas.
         */
        for (int i=0; i<layerCount; i++)
        {
            try
            {
                layers[i].paint(graphics, context, clipBounds);
            }
            catch (TransformException exception)
            {
                handleException("MapPanel", "paintComponent", exception);
            }
            catch (RuntimeException exception)
            {
                ExceptionMonitor.unexpectedException("net.seas.map", "MapPanel", "paintComponent", exception);
            }
        }
    }

    /**
     * Dessine la loupe, s'il y en a une.
     *
     * @param graphics Graphique à utiliser pour dessiner la loupe.
     */
    protected void paintMagnifier(final Graphics2D graphics)
    {
        try
        {
            isPrinting=true; // La loupe est soumise à des contraintes similaires à celles de l'impression.
            super.paintMagnifier(graphics);
        }
        finally
        {
            isPrinting=false;
        }
    }

    /**
     * Imprime la carte avec toutes ces couches visibles. Les couches auront été ajoutés
     * à la carte avec {@link #addLayer}. Les zooms sont automatiquement pris en compte
     * de la même façon que lors du dernier traçage à l'écran.
     *
     * @param graphics Graphique à utiliser pour dessiner la carte.
     */
    protected void printComponent(final Graphics2D graphics)
    {
        try
        {
            isPrinting=true;
            super.printComponent(graphics);
        }
        finally
        {
            isPrinting=false;
        }
    }

    /**
     * Méthode appelée automatiquement chaque fois que le zoom a changé.
     * Cette méthode met à jour les coordonnées des formes géométriques
     * déclarées dans les objets {@link Layer}.
     */
    private void zoomChanged(final ZoomChangeEvent event)
    {
        AffineTransform modifier;
        try
        {
            // Retrouve l'ancien zoom.
            modifier=event.getChange().createInverse();
            modifier.preConcatenate(zoom);
            // Trouve la transformation passant de l'ancien zoom au nouveau
            modifier=modifier.createInverse();
            modifier.preConcatenate(zoom);
        }
        catch (NoninvertibleTransformException exception)
        {
            modifier = null;
        }
        for (int i=layerCount; --i>=0;)
            layers[i].zoomChanged(modifier);
    }

    /**
     * Retourne la transformation inverse du
     * point spécifié. Le point sera modifié.
     */
    final Point2D inverseTransform(final Point2D point)
    {
        try {return zoom.inverseTransform(point, point);}
        catch (NoninvertibleTransformException exception)
        {exception.printStackTrace(); return null;}
    }

    /**
     * Méthode appelée lorsqu'une exception {@link TransformException} non-gérée
     * s'est produite. Cette méthode peut être appelée pendant le traçage de la carte
     * où les mouvements de la souris. Elle ne devrait donc pas utiliser une boîte de
     * dialogue pour reporter l'erreur, et retourner rapidement pour éviter de bloquer
     * la queue des événements de <i>Swing</i>.
     */
    protected void handleException(final String className, final String methodName, final TransformException exception)
    {ExceptionMonitor.unexpectedException("net.seas.map", className, methodName, exception);}

    /**
     * Préviens que ce paneau sera bientôt détruit. Cette méthode peut être appelée lorsque cet
     * objet <code>MapPanel</code> est sur le point de ne plus être référencé. Elle permet de
     * libérer des ressources plus rapidement que si l'on attend que le ramasse-miettes fasse
     * son travail. Après l'appel de cette méthode, on ne doit plus utiliser ni cet objet
     * <code>MapPanel</code> ni aucune des couches qu'il contenait.
     */
    public void dispose()
    {
        for (int i=layerCount; --i>=0;)
        {
            layers[i].clearCache();
            layers[i].dispose();
        }
    }
}
