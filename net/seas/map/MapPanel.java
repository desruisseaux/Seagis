/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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
 * Cette carte peut comprendre le trac� d'une c�te ainsi que toute sa bathym�trie. De
 * fa�on optionnelle, on peut aussi placer une image satellitaire sous la bathym�trie
 * trac�e. N'importe quelle image convient, pourvu qu'on en connaisse les coordonn�es.
 * Le tout forme un ensemble dans lequel l'usager peut naviguer � l'aide des touches
 * du clavier ou de la souris.
 *
 * <p>Notez que la classe <code>MapPanel</code> g�re les barres de d�filements
 * d'une mani�re particuli�re. Il ne faut donc pas placer d'objet <code>MapPanel</code> dans
 * un objet {@link javax.swing.JScrollPane}.    Pour construire et faire appara�tre un objet
 * <code>MapPanel</code> avec ses barres de d�filements,  il faut utiliser la m�thode {@link
 * #createScrollPane}.</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class MapPanel extends ZoomPane
{
    /**
     * Objet utilis� pour comparer deux objets {@link Layer}.
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
     * Syst�me de coordonn�es utilis� pour l'affichage � l'�cran. Les donn�es des diff�rentes
     * couches devront �tre converties selon ce syst�me de coordonn�es avant d'�tre affich�es.
     * La transformation la plus courante utilis�e � cette fin peut �tre conserv�e dans le
     * champ <code>commonestCoordinateTransform</code> � des fins de performances.
     */
    private final CoordinateSystem displayCoordinateSystem;

    /**
     * Transformation de coordonn�es (g�n�ralement une projection cartographique) utilis�e
     * le plus souvent pour convertir les coordonn�es des couches en coordonn�es d'affichage.
     * Ce champ n'est conserv� qu'� des fins de performances. Si ce champ est nul, �a signifie
     * qu'il a besoin d'�tre reconstruit.
     */
    private transient CoordinateTransform commonestCoordinateTransform;

    /**
     * Rectangle englobant les coordonn�es de l'ensemble des couches � tracer. Les coordonn�es de
     * ce rectangle sont exprim�es selon le syst�me de coordonn�es de l'affichage. Ce rectangle est
     * recalcul� chaque fois que le syst�me de coordonn�es change. Une valeur nulle signifie qu'aucune
     * m�thode <code>project({@link Layer#getPreferredArea})</code> n'a retourn� de valeur non-nulle.
     */
    private transient Rectangle2D area;

    /**
     * Indique si les couches {@link #layers}
     * sont class�es. La valeur <code>false</code>
     * indique qu'ils devront �tre reclass�s.
     */
    private transient boolean layerSorted;

    /**
     * Nombre d'objets valides dans le
     * tableau <code>layers</code>.
     */
    private int layerCount;

    /**
     * Images satellitaires, bathym�trie, stations, �chelles
     * ou autres couches � dessiner sur la carte.
     */
    private Layer[] layers;

    /**
     * Largeur des lignes � tracer. La valeur <code>null</code>
     * signifie que cette largeur doit �tre recalcul�e.
     */
    private Stroke stroke;

    /**
     * Indique si le prochain tra�age ({@link #paintComponent(Graphics2D)}
     * sera en fait une impression. Si c'est le cas, alors il ne faudra pas
     * mettre � jour certains champs internes.
     */
    private transient boolean isPrinting;

    /**
     * Objet "listener" ayant la charge de r�agir aux diff�rents
     * �v�nements qui int�ressent cet objet <code>MapPanel</code>.
     */
    private final transient Listeners listeners=new Listeners();

    /**
     * Classe ayant la charge de r�agir aux diff�rents �v�nements qui int�ressent cet
     * objet <code>MapPanel</code>. Cette classe r�agira entre autres aux changements
     * de l'ordre <var>z</var> ainsi qu'aux changements des coordonn�es g�ographiques
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
     * Construit un panneau avec un syst�me de coordonn�es par d�faut.
     * Ce syst�me utilisera des (<var>longitude</var>,<var>latitude</var>).
     * Le panneau ne contiendra initialement aucune couche. Les couches
     * pourront �tre ajout�es par des appels � {@link #addLayer}.
     */
    public MapPanel()
    {this(GeographicCoordinateSystem.WGS84);}

    /**
     * Construit un panneau avec ln syst�me de coordonn�es sp�cifi�.
     * Le panneau ne contiendra initialement aucune couche. Les couches
     * pourront �tre ajout�es par des appels � {@link #addLayer}.
     *
     * @param displayCoordinateSystem Syst�me de coordonn�es utilis� pour l'affichage
     *        de toutes les couches. Cet argument ne doit pas �tre nul.
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
     * Retourne le syst�me de coordonn�es utilis� pour l'affichage de toutes les couches.
     * Toutefois, les <em>donn�es</em> des couches ne seront pas n�cessairement exprim�es
     * selon ce syst�me de coordonn�es. Les conversions n�cessaires seront faites au vol
     * lors de l'affichage.
     */
    public CoordinateSystem getCoordinateSystem()
    {return displayCoordinateSystem;}

    /**
     * Retourne un rectangle englobant les coordonn�es de l'ensemble des couches � tracer.
     * Les coordonn�es de ce rectangle seront exprim�es selon le syst�me de coordonn�es
     * retourn� par {@link #getCoordinateSystem}. Si ces coordonn�es sont inconnues (par
     * exemple si <code>MapPanel</code> ne contient aucune couche), alors cette m�thode
     * retourne <code>null</code>.
     */
    public Rectangle2D getArea()
    {
        final Rectangle2D area=this.area;
        return (area!=null) ? (Rectangle2D) area.clone() : null;
    }

    /**
     * Modifie les coordonn�es du rectangle qui englobe les coordonn�es de l'ensemble
     * des couches � tracer.
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
     * Sa valeur sera calcul�e � partir des informations retourn�es par
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
     * Remplace un rectangle par un autre dans le calcul de {@link #area}. Si �a a eu pour effet de
     * changer les coordonn�es g�ographiques couvertes, un �v�nement appropri� sera lanc�. Cette
     * m�thode est plus �conomique que {@link #computeArea} du fait qu'elle essaie de ne pas tout
     * recalculer. Si on n'a pas pu faire l'�conomie d'un recalcul toutefois, alors {@link #computeArea}
     * sera appel�e.
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
     * Retourne une transformation permettant de convertir les coordonn�es exprim�es selon le
     * syst�me <code>source</code> vers le syst�me d'affichage {@link #displayCoordinateSystem}.
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
     * Retourne la transformation la plus couramment utilis�e pour convertir les coordonn�es
     * des couches en coordonn�es d'affichage. Cette transformation sera conserv�e dans une
     * cache interne pour am�liorer les performances.
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
             * chaque syst�mes de coordonn�es.
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
             * syst�me le plus souvent utilis�.
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
     * Ajoute une couche � la liste des couches � dessiner. Une couches peut �tre par
     * exemple une station ou une �chelle. Une station pourra �tre un simple point, un
     * vecteur de courant ou une ellipse de mar�e (voir {@link net.seas.map.layer.MarkLayer}
     * et ses classes d�riv�es).
     *
     * @param  layer Composante � ajouter � la liste des couches � tracer.
     *         Rien ne sera fait si cette couche apparaissait d�j� dans la liste.
     * @throws IllegalArgumentException si la couche sp�cifi�e ne peut pas �tre ajout�e
     *         (par exemple si elle appartenait d�j� � un autre objet {@link MapPanel}).
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
                throw new IllegalArgumentException(Resources.format(Cl�.MAPPANEL_NOT_OWNER));
            }
            layer.mapPanel = this;
            /*
             * Ajoute la nouvelle couche dans le tableau {@link #layers}. Le tableau
             * sera agrandit si n�cessaire et on d�clarera qu'il a besoin d'�tre reclass�.
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
     * Retire une couche de la liste des couches � dessiner.
     *
     * @param  layer Composante � retirer. Rien ne sera fait si
     *         cette couche avait d�j� �t� retir�e.
     * @throws IllegalArgumentException si la couche appartenait �
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
                throw new IllegalArgumentException(Resources.format(Cl�.MAPPANEL_NOT_OWNER));
            }
            repaint(); // Must be invoked first
            /*
             * Retire cette couche de la liste {@link #layers}. On recherchera
             * toutes les occurences de cette couche, m�me si en principe elle ne
             * devrait appara�tre qu'une et une seule fois.
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
     * Renvoie les couches � dessiner sur cette carte (stations ou �chelles par
     * exemple). Les couches seront class�es comme suit: la premi�re couche
     * � dessiner appara�tra � l'index 0, la seconde couche � dessiner appara�tra
     * � l'index 1 et ainsi de suite. Cet ordre sera d�termin� � partir de l'ordre Z
     * retourn� par {@link Layer#getZOrder}. S'il n'y a aucune couche �
     * dessiner, le tableau retourn� sera de longueur 0, mais ne sera jamais nul.
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
     * � dessiner sur cette carte.
     */
    public synchronized int getLayerCount()
    {return layerCount;}

    /**
     * Version non-synchronis�e de
     * {@link #getLayerCount}.
     */
    final int getLayerCountFast()
    {return layerCount;}

    /**
     * Proc�de au classement imm�diat des
     * couches, si ce n'�tait pas d�j� fait.
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
     * D�fini le texte � afficher par d�faut lorsque le curseur de la souris
     * traine sur la carte. Le texte <code>tooltip</code> sera affich� si
     * aucune couche {@link Layer} n'a pris propos� de texte.
     * La valeur <code>null</code> implique qu'aucun texte ne sera affich�
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
     * Retourne un texte a afficher lorsque le curseur de la souris tra�ne sur la
     * carte. L'impl�mentation par d�faut v�rifie si le curseur se trouve sur un
     * des �l�ments ajout�s � la carte (typiquement une des stations) et appelle
     * sa m�thode {@link Layer#getToolTipText}. Cette m�thode n'a normalement
     * pas besoin d'�tre utilis�e directement. Elle est utilis� automatiquement par
     * <i>Swing</i>.
     *
     * @param  event Position de la souris.
     * @return Le texte � afficher, ou <code>null</code> s'il n'y a rien
     *         � afficher pour la position <code>event</code> de la souris.
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
     * M�thode appel�e automatiquement lorsque l'utilisateur a cliqu� sur le bouton droit de
     * la souris. Cette m�thode v�rifie si le clic s'est fait sur une couche et appelle
     * {@link Layer#getPopupMenu} si c'est le cas. Si aucune couche ne propose de
     * menu, alors cette m�thode appele {@link #getDefaultPopupMenu}.
     *
     * @param event Ev�nement de la souris contenant entre autre les coordonn�es point�es.
     * @return Le menu contextuel, ou <code>null</code> pour ne pas faire appara�tre de menu.
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
     * Retourne le menu par d�faut � faire appara�tre lorsque l'utilisateur a cliqu�
     * sur le bouton droit de la souris et qu'aucune couche n'a propos� de menu.
     * L'impl�mentation par d�faut retourne un menu contextuel dans lequel figure des
     * options de navigations.
     */
    protected JPopupMenu getDefaultPopupMenu(final GeoMouseEvent event)
    {return super.getPopupMenu(event);}

    /**
     * M�thode appell�e chaque fois que le bouton de la souris a �t� cliqu�. L'impl�mentation par
     * d�faut appele les m�thodes {@link Layer#mouseClicked} de toutes les couches (en commen�ant
     * par celles qui apparaissent par dessus les autres) jusqu'� ce que l'une des couches appelle
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
     * Construit une cha�ne de caract�res repr�sentant la valeur point�e par la souris.  En g�n�ral (mais pas
     * obligatoirement), lorsque cette m�thode est appel�e, le buffer <code>toAppendTo</code> contiendra d�j�
     * une cha�ne de caract�res repr�sentant les coordonn�es pont�es par la souris. Cette m�thode est appel�e
     * pour donner une chance aux couches d'ajouter d'autres informations pertinentes. Par exemple les couches
     * qui repr�sentent une image satellitaire de temp�rature peuvent ajouter � <code>toAppendTo</code> un texte
     * du genre "12�C" (sans espaces au d�but).
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette m�thode a ajout� des informations dans <code>toAppendTo</code>.
     *
     * @see MouseCoordinateFormat
     */
    final boolean getLabel(final GeoMouseEvent event, final StringBuffer toAppendTo)
    {
        // On appele pas 'sortLayer' de fa�on syst�m�tique afin de gagner un peu en performance.
        // Cette m�thode peut �tre appel�e tr�s souvent (� chaque d�placement de la souris).
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
     * Effectue le traitement d'un �v�nement de la souris. Cette m�thode convertie
     * l'�v�nemenent {@link MouseEvent} en �v�nement {@link GeoMouseEvent}, puis le
     * transmets � <code>super.processMouseEvent(MouseEvent)</code> pour distribution
     * aupr�s des objets int�r�ss�s.
     */
    protected void processMouseEvent(final MouseEvent event)
    {super.processMouseEvent(new GeoMouseEvent(event, this));}

    /**
     * Effectue le traitement d'un mouvement de la souris. Cette m�thode convertie
     * l'�v�nemenent {@link MouseEvent} en �v�nement {@link GeoMouseEvent}, puis le
     * transmets � <code>super.processMouseEvent(MouseEvent)</code> pour distribution
     * aupr�s des objets int�r�ss�s.
     */
    protected void processMouseMotionEvent(final MouseEvent event)
    {super.processMouseMotionEvent(new GeoMouseEvent(event, this));}

    /**
     * Efface les donn�es qui avaient �t� conserv�es dans une cache interne. L'appel
     * de cette m�thode permettra de lib�rer un peu de m�moire � d'autres fins. Elle
     * devrait �tre appel�e lorsque l'on sait qu'on n'affichera plus cette carte
     * avant un certain temps. Par exemple la m�thode {@link java.applet.Applet#stop}
     * devrait appeller <code>clearCache()</code>. Notez que l'appel de cette m�thode
     * ne modifie aucunement le param�trage de cette carte. Seulement, son prochain
     * tra�age sera plus lent, le temps que <code>MapPanel</code> reconstruise les
     * caches internes.
     */
    private void clearCache()
    {
        for (int i=layerCount; --i>=0;)
            layers[i].clearCache();
        System.gc();
    }

    /**
     * Retourne la dimension logique pr�f�r�e
     * des pixels lors d'un zoom rapproch�.
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
     * Retourne les coordonn�es projet�es de la r�gion visible � l'�cran. Ces coordonn�es
     * sont exprim�es en m�tres, ou un degr�s si aucune projection cartographique n'a �t�
     * d�finie. Cette information peut �tre utilis�e par certaines couches qui voudraient
     * utiliser un syst�me de caches, pour ne faire de co�teux calculs que sur leurs parties
     * visibles.
     *
     * @param expand Facteur d'agrandissement du rectangle. La valeur 0 fera retourner un
     *        rectangle d�limitant exactement la partie visible, tandis que la valeur 0.25
     *        (par exemple) fera retourner un rectangle agrandit de 25%.
     *        Lorsque <code>getArea</code> est utilis�e pour d�limiter une r�gion � placer
     *        dans une cache, il est conseill� de se laisser une marge (par exemple de 25%)
     *        pour que la cache puisse �tre encore utilis�e m�me apr�s quelques translations.
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
     * auront �t� ajout�es � la carte avec {@link #addLayer}. Les zooms
     * sont automatiquement pris en compte.
     *
     * @param graph Graphique � utiliser pour dessiner la carte.
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
         * Dessine les couches en commen�ant par
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
     * @param graphics Graphique � utiliser pour dessiner la loupe.
     */
    protected void paintMagnifier(final Graphics2D graphics)
    {
        try
        {
            isPrinting=true; // La loupe est soumise � des contraintes similaires � celles de l'impression.
            super.paintMagnifier(graphics);
        }
        finally
        {
            isPrinting=false;
        }
    }

    /**
     * Imprime la carte avec toutes ces couches visibles. Les couches auront �t� ajout�s
     * � la carte avec {@link #addLayer}. Les zooms sont automatiquement pris en compte
     * de la m�me fa�on que lors du dernier tra�age � l'�cran.
     *
     * @param graphics Graphique � utiliser pour dessiner la carte.
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
     * M�thode appel�e automatiquement chaque fois que le zoom a chang�.
     * Cette m�thode met � jour les coordonn�es des formes g�om�triques
     * d�clar�es dans les objets {@link Layer}.
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
     * point sp�cifi�. Le point sera modifi�.
     */
    final Point2D inverseTransform(final Point2D point)
    {
        try {return zoom.inverseTransform(point, point);}
        catch (NoninvertibleTransformException exception)
        {exception.printStackTrace(); return null;}
    }

    /**
     * M�thode appel�e lorsqu'une exception {@link TransformException} non-g�r�e
     * s'est produite. Cette m�thode peut �tre appel�e pendant le tra�age de la carte
     * o� les mouvements de la souris. Elle ne devrait donc pas utiliser une bo�te de
     * dialogue pour reporter l'erreur, et retourner rapidement pour �viter de bloquer
     * la queue des �v�nements de <i>Swing</i>.
     */
    protected void handleException(final String className, final String methodName, final TransformException exception)
    {ExceptionMonitor.unexpectedException("net.seas.map", className, methodName, exception);}

    /**
     * Pr�viens que ce paneau sera bient�t d�truit. Cette m�thode peut �tre appel�e lorsque cet
     * objet <code>MapPanel</code> est sur le point de ne plus �tre r�f�renc�. Elle permet de
     * lib�rer des ressources plus rapidement que si l'on attend que le ramasse-miettes fasse
     * son travail. Apr�s l'appel de cette m�thode, on ne doit plus utiliser ni cet objet
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
