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
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.MathTransform2D;
import net.seagis.ct.TransformException;
import net.seagis.ct.CoordinateTransformation;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.ct.CannotCreateTransformException;
import net.seagis.resources.OpenGIS;

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
import net.seagis.resources.XAffineTransform;
import net.seagis.resources.XDimension2D;

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

// Logging
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Collections
import java.util.Arrays;
import java.util.Comparator;

// Miscellaneous
import net.seas.util.XArray;
import net.seas.resources.ResourceKeys;
import net.seas.resources.Resources;
import net.seas.awt.ExceptionMonitor;
import net.seagis.resources.Utilities;


/**
 * A <i>Swing</i> component for displaying geographic informations.  A newly constructed
 * <code>MapPanel</code> is initially empty. To make something appears, user must create
 * a {@link Layer} object and add it to this <code>MapPanel</code> with the {@link #addLayer}
 * méthod. The layer content depends of the layer subclass. It may be
 *
 * a set of isobaths forming a bathymetry ({@link net.seas.map.layer.IsolineLayer}),
 * a remote sensing image ({@link net.seas.map.layer.GridCoverageLayer}),
 * a set of arbitrary shapes marking locations ({@link net.seas.map.layer.MarkLayer}),
 * a map scale ({@link net.seas.map.layer.MapScaleLayer}), etc.
 * <br><br>
 * Since this class extends {@link ZoomPane},  the user can use mouse and keyboard
 * to zoom, translate and rotate around the map (Remind: <code>MapPanel</code> has
 * no scrollbar. To display scrollbars, use {@link #createScrollPane}).
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
        {return Float.compare(layer1.getZOrder(), layer2.getZOrder());}
    };

    /**
     * Système de coordonnées utilisé pour l'affichage à l'écran. Les données des différentes
     * couches devront être converties selon ce système de coordonnées avant d'être affichées.
     * La transformation la plus courante utilisée à cette fin peut être conservée dans le
     * champ <code>commonestTransform</code> à des fins de performances.
     */
    private final CoordinateSystem coordinateSystem;

    /**
     * Transformation de coordonnées (généralement une projection cartographique) utilisée
     * le plus souvent pour convertir les coordonnées des couches en coordonnées d'affichage.
     * Ce champ n'est conservé qu'à des fins de performances. Si ce champ est nul, ça signifie
     * qu'il a besoin d'être reconstruit.
     */
    private transient CoordinateTransformation commonestTransform;

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
     * Indique si le prochain traçage ({@link #paintComponent(Graphics2D)})
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
        public void componentResized(final ComponentEvent      event) {MapPanel.this.zoomChanged (null );}
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
                    changeArea((Rectangle2D) event.getOldValue(), (Rectangle2D) event.getNewValue(), ((Layer) event.getSource()).getCoordinateSystem(), "Layer", "setPreferredArea");
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
     * @param coordinateSystem Système de coordonnées utilisé pour l'affichage de
     *        toutes les couches. Ce système de coordonnées doit avoir 2 dimensions,
     *        ou être un sytème de la classe {@link CompoundCoordinateSystem} dont
     *        le système de tête (<code>headCS</code>) a deux dimensions.
     *
     * @throws IllegalArgumentException si <code>coordinateSystem</code> ne peut
     *         pas être ramené à un système de coordonnées à deux dimensions.
     */
    public MapPanel(final CoordinateSystem coordinateSystem)
    {
        super(TRANSLATE_X | TRANSLATE_Y | UNIFORM_SCALE | DEFAULT_ZOOM | ROTATE | RESET);
        this.coordinateSystem = Layer.getCoordinateSystem2D(coordinateSystem);
        addZoomChangeListener(listeners);
        addComponentListener (listeners);
        addMouseListener     (listeners);
        setResetPolicy       (true);
    }

    /**
     * Retourne le système de coordonnées utilisé pour l'affichage de toutes les couches.
     * Toutefois, les <em>données</em> des couches ne seront pas nécessairement exprimées
     * selon ce système de coordonnées. Les conversions nécessaires seront faites au vol
     * lors de l'affichage.
     *
     * @return Le système de coordonnées de l'affichage. Ce système
     *         aura toujours exactement deux dimensions.
     */
    public CoordinateSystem getCoordinateSystem()
    {return coordinateSystem;}

    /**
     * Retourne une transformation permettant de convertir les coordonnées exprimées selon
     * le système <code>source</code> vers le système d'affichage {@link #coordinateSystem}.
     */
    private MathTransform2D getMathTransform2D(final CoordinateSystem source, final String sourceClassName, final String sourceMethodName) throws CannotCreateTransformException
    {
        CoordinateTransformation transformation = commonestTransform;
        if (transformation==null || !transformation.getSourceCS().equivalents(source))
        {
            transformation = Contour.createFromCoordinateSystems(source, coordinateSystem, sourceClassName, sourceMethodName);
        }
        return (MathTransform2D) transformation.getMathTransform();
    }

    /**
     * Retourne la transformation la plus couramment utilisée pour convertir les coordonnées
     * des couches en coordonnées d'affichage. Cette transformation sera conservée dans une
     * cache interne pour améliorer les performances.
     */
    final CoordinateTransformation getCommonestTransformation(final String sourceClassName, final String sourceMethodName) throws CannotCreateTransformException
    {
        if (commonestTransform==null)
        {
            int n=0;
            final CoordinateSystem[] cs=new CoordinateSystem[layerCount];
            final int[]           count=new int             [layerCount];
            /*
             * Compte le nombre d'occurences de
             * chaque systèmes de coordonnées.
             */
      scan: for (int i=0; i<layerCount; i++)
            {
                final CoordinateSystem sys=layers[i].getCoordinateSystem();
                for (int j=0; j<n; j++)
                {
                    if (sys.equivalents(cs[j]))
                    {
                        count[n]++;
                        continue scan;
                    }
                }
                cs[n++] = sys;
            }
            /*
             * Recherche dans la liste le
             * système le plus souvent utilisé.
             */
            CoordinateSystem sourceCS = coordinateSystem;
            int maxCount = 0;
            while (--n>=0)
            {
                if (count[n] >= maxCount)
                {
                    maxCount = n;
                    sourceCS = cs[n];
                }
            }
            commonestTransform = Contour.createFromCoordinateSystems(sourceCS, coordinateSystem, sourceClassName, sourceMethodName);
        }
        return commonestTransform;
    }

    /**
     * Returns a bounding box that completely encloses all layer's preferred area.
     * This bounding box should be representative of the geographic area to drawn.
     * User wanting to set the default rendering to a different area should use
     * <code>get/setPreferredArea</code>. Coordinates are expressed in this
     * <code>MapPanel</code>'s coordinate system (see {@link #getCoordinateSystem}).
     *
     * @return The enclosing area computed from available data, or <code>null</code>
     *         if this area can't be computed.
     */
    public Rectangle2D getArea()
    {
        final Rectangle2D area=this.area;
        return (area!=null) ? (Rectangle2D) area.clone() : null;
    }

    /**
     * Set the geographic area. This is method is invoked as a result of internal
     * computation. User should not call this method himself; he/she should calls
     * <code>get/setPreferredArea</code> instead.
     */
    private void setArea(final Rectangle2D newArea)
    {
        final Rectangle2D oldArea=this.area;
        if (!Utilities.equals(oldArea, newArea))
        {
            this.area=newArea;
            firePropertyChange("area", oldArea, newArea);
            if (super.getToolTipText()==null)
            {
                if (oldArea==null)
                {
                    if (newArea!=null)
                    {
                        ToolTipManager.sharedInstance().registerComponent(this);
                    }
                }
                else if (newArea==null)
                {
                    ToolTipManager.sharedInstance().unregisterComponent(this);
                }
            }
            fireZoomChanged(new AffineTransform()); // Update scrollbars
            log("net.seas.map", "MapPanel", "setArea", newArea);
        }
    }

    /**
     * Recalcule inconditionnelement la valeur du champ {@link #area}.
     * Sa valeur sera calculée à partir des informations retournées par
     * toutes les couches de cette carte.
     */
    private void computeArea(final String sourceClassName, final String sourceMethodName)
    {
        Rectangle2D         newArea = null;
        CoordinateSystem lastSystem = null;
        MathTransform2D   transform = null;
        for (int i=layerCount; --i>=0;)
        {
            final Layer  layer=layers[i];
            Rectangle2D bounds=layer.getPreferredArea();
            if (bounds!=null)
            {
                final CoordinateSystem system=layer.getCoordinateSystem();
                try
                {
                    if (lastSystem==null || !lastSystem.equivalents(system))
                    {
                        transform  = getMathTransform2D(system, sourceClassName, sourceMethodName);
                        lastSystem = system;
                    }
                    bounds = OpenGIS.transform(transform, bounds, null);
                    if (newArea==null) newArea=bounds;
                    else newArea.add(bounds);
                }
                catch (TransformException exception)
                {
                    handleException(sourceClassName, sourceMethodName, exception);
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
    private void changeArea(Rectangle2D oldSubArea, Rectangle2D newSubArea, final CoordinateSystem system,
                            final String sourceClassName, final String sourceMethodName)
    {
        try
        {
            final MathTransform2D transform = getMathTransform2D(system, sourceClassName, sourceMethodName);
            oldSubArea = OpenGIS.transform(transform, oldSubArea, null);
            newSubArea = OpenGIS.transform(transform, newSubArea, null);
        }
        catch (TransformException exception)
        {
            handleException(sourceClassName, sourceMethodName, exception);
            computeArea(sourceClassName, sourceMethodName);
            return;
        }
        final Rectangle2D expandedArea = Layer.changeArea(area, oldSubArea, newSubArea);
        if (expandedArea!=null)
        {
            setArea(expandedArea);
        }
        else computeArea(sourceClassName, sourceMethodName);
    }

    /**
     * Add a new layer to this map. A <code>MapPanel</code> do not draw anything as
     * long as at least one layer hasn't be added.  A {@link Layer} can be anything
     * like an isobath, a remote sensing image, city locations, map scale, etc. The
     * drawing order (relative to other layers) is determined by the {@link Layer#getZOrder}
     * property. A {@link Layer} object can be added to only one <code>MapPanel</code> object.
     *
     * @param  layer Layer to add to this <code>MapPanel</code>. This method call
     *         will be ignored if <code>layer</code> has already been added to this
     *         <code>MapPanel</code>.
     * @throws IllegalArgumentException If <code>layer</code> has already been added
     *         to an other <code>MapPanel</code>.
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
                throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_MAPPANEL_NOT_OWNER));
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
            changeArea(null, layer.getPreferredArea(), layer.getCoordinateSystem(), "MapPanel", "addLayer");
            layer.addPropertyChangeListener(listeners);
            commonestTransform = null;
            stroke             = null;
        }
        if (layerCount==1)
        {
            reset();
        }
        repaint(); // Must be invoked last
    }

    /**
     * Remove a layer from this <code>MapPanel</code>. Note that if you
     * just want to temporarily hide à layer, it is more efficient to
     * invoke {@link Layer#setVisible}.
     *
     * @param  layer The layer to remove. This method call will be ignored
     *         if <code>layer</code> has already been removed from this
     *         <code>MapPanel</code>.
     * @throws IllegalArgumentException If <code>layer</code> is owned by
     *         an other <code>MapPanel</code> that <code>this</code>.
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
                throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_MAPPANEL_NOT_OWNER));
            }
            repaint(); // Must be invoked first
            layer.removePropertyChangeListener(listeners);
            final CoordinateSystem layerCS = layer.getCoordinateSystem();
            final Rectangle2D    layerArea = layer.getPreferredArea();
            layer.setVisible(false);
            layer.clearCache();
            layer.mapPanel = null;
            /*
             * Retire cette couche de la liste {@link #layers}. On recherchera
             * toutes les occurences de cette couche, même si en principe elle ne
             * devrait apparaître qu'une et une seule fois.
             */
            for (int i=layerCount; --i>=0;)
            {
                final Layer scan=layers[i];
                if (scan==layer)
                {
                    System.arraycopy(layers, i+1, layers, i, (--layerCount)-i);
                    layers[layerCount]=null;
                }
            }
            changeArea(layerArea, null, layerCS, "MapPanel", "removeLayer");
            commonestTransform = null;
            stroke             = null;
        }
    }

    /**
     * Remove all layers from this <code>MapPanel</code>.
     */
    public synchronized void removeAllLayers()
    {
        repaint(); // Must be invoked first
        while (--layerCount>=0)
        {
            final Layer layer=layers[layerCount];
            synchronized (layer)
            {
                layer.removePropertyChangeListener(listeners);
                layer.setVisible(false);
                layer.clearCache();
                layer.mapPanel=null;
            }
            layers[layerCount]=null;
        }
        commonestTransform = null;
        stroke             = null;
        setArea(null);
    }

    /**
     * Returns all registered layers. The returned array is sorted in increasing
     * z-order (as returned by {@link Layer#getZOrder}): element at index 0
     * contains to first layer to drawn.
     *
     * @return The sorted array of layers. May have a 0 length, but will never
     *         be <code>null</code>. Change to this array, will not affect this
     *         <code>MapPanel</code>.
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
     * Returns the number of layers in this map panel.
     */
    public synchronized int getLayerCount()
    {return layerCount;}

    /**
     * Check if there is at least one registered layer.
     * Used internally by {@link MouseCoordinateFormat}.
     */
    final boolean hasLayers()
    {return layerCount!=0;}

    /**
     * Procède au classement immédiat des
     * couches, si ce n'était pas déjà fait.
     */
    private void sortLayers()
    {
        if (!layerSorted && layers!=null)
        {
            layers = XArray.resize(layers, layerCount);
            Arrays.sort(layers, COMPARATOR);
            layerSorted=true;
        }
    }

    /**
     * Registers the default text to display in a tool tip. The text displays
     * when the cursor lingers over the component and no layer has proposed a
     * tool tip (i.e. {@link Layer#getToolTipText} returned <code>null</code>
     * for all registered layers).
     *
     * @param tooltip The default tooltip, or <code>null</code> if none.
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
     * Returns the string to be used as the tooltip for a given mouse event.
     * This method invokes {@link Layer#getToolTipText} for some registered
     * layers in decreasing z-order, until one is found that returns a non-null
     * string. If no layer has a tool tip for this event, then the tooltip
     * string that has been set with {@link #setToolTipText} is returned.
     *
     * @param  event The mouse event.
     * @return The tool tip text, or <code>null</code> if there
     *         is no tool tip for this location.
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
     * Returns the popup menu to be used for a given mouse event. This method invokes
     * {@link Layer#getPopupMenu} for some registered layers in decreasing z-order,
     * until one is found that returns a non-null menu. If no layer has a popup menu
     * for this event, then this method returns {@link #getDefaultPopupMenu}.
     *
     * @param  event The mouse event.
     * @return The popup menu for this event, or <code>null</code> if there is none.
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
     * Returns a default popup menu for the given mouse event. This method
     * is invoked when no layers proposed a popup menu for this event. The
     * default implementation returns a menu with navigation options.
     */
    protected JPopupMenu getDefaultPopupMenu(final GeoMouseEvent event)
    {return super.getPopupMenu(event);}

    /**
     * Invoked when user clicked on this <code>MapPanel</code>. The default
     * implementation invokes {@link Layer#mouseClicked} for some layers in
     * decreasing z-order until one consume the event (with {@link MouseEvent#consume}).
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
     * Processes mouse events occurring on this component. This method
     * wrap the <code>MouseEvent</code> into a <code>GeoMouseEvent</code>
     * and pass it to any registered <code>MouseListener</code> objects.
     */
    protected void processMouseEvent(final MouseEvent event)
    {super.processMouseEvent(new GeoMouseEvent(event, this));}

    /**
     * Processes mouse motion events occurring on this component. This method
     * wrap the <code>MouseEvent</code> into a <code>GeoMouseEvent</code>
     * and pass it to any registered <code>MouseMotionListener</code> objects.
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
     * Returns the preferred pixel size for a close zoom. The default implementation
     * invoke {@link Layer#getPreferredPixelSize()} for each layer and returns the
     * finest resolution (transformed in this <code>MapPanel</code>'s coordinate
     * system).
     */
    protected Dimension2D getPreferredPixelSize()
    {
        double minWidth  = Double.POSITIVE_INFINITY;
        double minHeight = Double.POSITIVE_INFINITY;
        for (int i=layerCount; --i>=0;)
        {
            final Layer layer = layers[i];
            final Dimension2D size=layer.getPreferredPixelSize();
            if (size!=null) try
            {
                double width  = size.getWidth();
                double height = size.getHeight();
                final MathTransform2D transform = getMathTransform2D(layer.getCoordinateSystem(), "MapPanel", "getPreferredPixelSize");
                if (!transform.isIdentity())
                {
                    Rectangle2D area = layer.getPreferredArea();
                    if (area==null) area = new Rectangle2D.Double();
                    area.setRect(area.getCenterX()-0.5*width, area.getCenterY()-0.5*height, width, height);
                    area   = OpenGIS.transform(transform, area, area);
                    width  = area.getWidth();
                    height = area.getHeight();
                }
                if (width  < minWidth ) minWidth =width;
                if (height < minHeight) minHeight=height;
            }
            catch (TransformException exception)
            {
                handleException("MapPanel", "getPreferredPixelSize", exception);
                // Not a big deal. Continue...
            }
        }
        if (!Double.isInfinite(minWidth) && !Double.isInfinite(minHeight))
        {
            return new XDimension2D.Double(minWidth, minHeight);
        }
        else return super.getPreferredPixelSize();
    }

    /**
     * Returns the default size for this component.  This is the size
     * returned by {@link #getPreferredSize} if no preferred size has
     * been explicitly set.
     */
    protected Dimension getDefaultSize()
    {return new Dimension(512,512);}

    /**
     * Paint this <code>MapLayer</code> and all visible
     * layers it contains. Zoom are taken in account.
     *
     * @param graph The graphics context.
     */
    protected void paintComponent(final Graphics2D graph)
    {
        sortLayers();
        if (stroke==null)
        {
            Dimension2D s = getPreferredPixelSize();
            double t; t=Math.sqrt((t=s.getWidth())*t + (t=s.getHeight())*t);
            stroke=new BasicStroke((float)t);
        }
        final Layer[]       layers = this.layers;
        final GraphicsJAI graphics = GraphicsJAI.createGraphicsJAI(graph, this);
        final Rectangle clipBounds = graphics.getClipBounds();
        final RenderingContext context;
        try
        {
            context = new RenderingContext(getCommonestTransformation("MapPanel", "paintComponent"), new AffineTransform(zoom), graphics.getTransform(), getZoomableBounds(null), isPrinting);
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
                Utilities.unexpectedException("net.seas.map", "MapPanel", "paintComponent", exception);
            }
        }
    }

    /**
     * Paint this magnifier, if presents.
     *
     * @param graph The graphics context.
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
     * Print this <code>MapLayer</code> and all visible
     * layers it contains. Zoom are taken in account in
     * the same way than the {@link #paintComponent(Graphics2D)}
     * method.
     *
     * @param graph The graphics context.
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
     *
     * @param event Le changement de zoom, ou <code>null</code> s'il
     *        n'est pas connu. Dans ce dernier cas, toutes les couches
     *        seront redessinées lors du prochain traçage.
     */
    private void zoomChanged(final ZoomChangeEvent event)
    {
        AffineTransform change = (event!=null) ? event.getChange() : null;
        if (change!=null) try
        {
            if (change.isIdentity()) return;
            // NOTE: 'change' is a transformation in LOGICAL coordinates.
            //       But 'Layer.zoomChanged(...)' expect a transformation
            //       in PIXEL coordinates. Compute the matrix now...
            final AffineTransform matrix = zoom.createInverse();
            matrix.preConcatenate(change);
            matrix.preConcatenate(zoom);
            change = matrix;
        }
        catch (NoninvertibleTransformException exception)
        {
            // Should not happen.
            Utilities.unexpectedException("net.seas.map", "MapPanel", "zoomChanged", exception);
            change = null;
        }
        for (int i=layerCount; --i>=0;)
        {
            /*
             * Remind: 'Layer' is about to use the affine transform change
             *         for updating its bounding shape in pixel coordinates.
             */
            layers[i].zoomChanged(change);
        }
    }

    /**
     * Retourne la transformation inverse du
     * point spécifié. Le point sera modifié.
     */
    final Point2D inverseTransform(final Point2D point)
    {
        try
        {
            return zoom.inverseTransform(point, point);
        }
        catch (NoninvertibleTransformException exception)
        {
            // This method is actually invoked by GeoMouseEvent only.
            Utilities.unexpectedException("net.seas.map", "GeoMouseEvent", "getVisualCoordinate", exception);
            return null;
        }
    }

    /**
     * Méthode appelée lorsqu'une exception {@link TransformException} non-gérée
     * s'est produite. Cette méthode peut être appelée pendant le traçage de la carte
     * où les mouvements de la souris. Elle ne devrait donc pas utiliser une boîte de
     * dialogue pour reporter l'erreur, et retourner rapidement pour éviter de bloquer
     * la queue des événements de <i>Swing</i>.
     */
    protected void handleException(final String className, final String methodName, final TransformException exception)
    {Utilities.unexpectedException("net.seas.map", className, methodName, exception);}

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
