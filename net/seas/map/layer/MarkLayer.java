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
package net.seas.map.layer;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.util.OpenGIS;

// Geometry
import java.awt.Shape;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import net.seas.util.XAffineTransform;

// Graphics
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.BasicStroke;
import javax.swing.JPopupMenu;
import javax.media.jai.GraphicsJAI;

// Miscellaneous
import javax.units.Unit;
import net.seas.map.Layer;
import net.seas.map.GeoMouseEvent;
import net.seas.map.MapPaintContext;
import net.seas.util.XArray;
import net.seas.util.XMath;


/**
 * Représentation graphique d'un ensemble marques apparaissant sur une carte. Ces marques peuvent
 * être par exemple des points représentant les positions de stations, ou des flèches de courants.
 * L'implémentation par défaut de cette classe ne fait que dessiner des points à certaines positions
 * Les classes dérivées peuvent implémenter un dessin plus évolué, par exemple une flèche de courant
 * ou une ellipse de marée. La façon de mémorisées les données est laissée à la discrétion des classes
 * dérivées. Toute classe concrète devra implémenter au moins les deux méthodes suivantes, qui servent
 * à obtenir les coordonnées des stations:
 *
 * <ul>
 *   <li>{@link #getCount}</li>
 *   <li>{@link #getPosition}</li>
 * </ul>
 *
 * Si, à la position de chaque marque, on souhaite dessiner une figure orientable dans l'espace (par
 * exemple une flèche de courant ou une ellipse de marée), la classe dérivée pourra redéfinir une ou
 * plusieurs des méthodes ci-dessous. Redéfinir ces méthodes permet par exemple de dessiner des flèches
 * dont la forme exacte (par exemple une, deux ou trois têtes) et la couleur varie avec l'amplitude, la
 * direction ou d'autres critères à votre choix.
 *
 * <ul>
 *   <li>{@link #getTypicalAmplitude}</li>
 *   <li>{@link #getAmplitude}</li>
 *   <li>{@link #getDirection}</li>
 *   <li>{@link #getShape}</li>
 *   <li>{@link #paint(GraphicsJAI, Shape, int)}</li>
 * </ul>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class MarkLayer extends Layer
{
    /**
     * Forme géométrique à utiliser par défaut lorsqu'aucune autre forme n'a
     * été spécifiée. La position centrale de la station doit correspondre à
     * la coordonnée (0,0) de cette forme. La dimension de cette forme est
     * exprimée en pixels. La forme par défaut sera un cercle centré à
     * (0,0) et d'un diamètre de 10 pixels.
     */
    static final Shape DEFAULT_SHAPE = new Ellipse2D.Float(-5, -5, 10, 10);

    /**
     * Couleur des marques. La couleur par défaut sera orangée.
     */
    static final Color DEFAULT_COLOR=new Color(234, 192, 0);

    /**
     * Projection cartographique utilisée la
     * dernière fois pour obtenir les données.
     */
    private transient CoordinateTransform lastProjection;

    /**
     * Transformation affine utilisée la dernière fois.
     * Cette information est utilisée pour savoir si on
     * peut réutiliser {@link #transformedShapes}.
     */
    private transient AffineTransform lastTransform;

    /**
     * Formes géométriques transformées utilisées la dernière fois.
     * Ces formes seront réutilisées autant que possible plutôt que
     * d'être constamment recalculées.
     */
    private transient Shape[] transformedShapes;

    /**
     * Boîte englobant toutes les coordonnées des formes apparaissant
     * dans {@link #transformedShapes}. Les coordonnées de cette boîte
     * seront en pixels.
     */
    private transient Rectangle boundingBox;

    /**
     * Amplitude typique calculée par {@link #getTypicalAmplitude}.
     * Cette information est conservée dans une cache interne pour
     * des accès plus rapides.
     */
    private transient double typicalAmplitude;

    /**
     * Point utilisé temporairement lors
     * des mouvements de la souris.
     */
    private transient Point2D point;

    /**
     * Construit un ensemble de marques. Les coordonnées de ces
     * marques seront exprimées selon le système de coordonnées
     * par défaut (WGS 1984).
     */
    protected MarkLayer()
    {super();}

    /**
     * Construit un ensemble de marques. Les coordonnées de ces
     * marques seront exprimées selon le système de coordonnées
     * spécifié.
     */
    protected MarkLayer(final CoordinateSystem coordinateSystem)
    {super(coordinateSystem);}

    /**
     * Retourne le nombre de marques mémorisées dans cette couche.
     * Les données de chacune de ces marques pourront être accédées
     * à l'aides des différentes méthodes <code>get*</code> de cette
     * classe.
     *
     * @see #getPosition
     * @see #getAmplitude
     * @see #getDirection
     */
    public abstract int getCount();

    /**
     * Indique si la marque pointée par l'index spécifié est visible. L'implémentation par
     * défaut retourne toujours <code>true</code>. Les classes dérivées peuvent redéfinir
     * cette méthode si elles veulent que certaines marques ne soient pas visible sur la
     * carte. Les classes dérivées ne sont pas tenues de retourner toujours la même valeur
     * pour un index donné. Par exemple deux appels consécutifs à <code>isVisible(23)</code>
     * pourraient retourner <code>true</code> la première fois et <code>false</code> la
     * seconde, ce qui indiquerait que la station #23 apparaissait d'abord comme "allumée",
     * puis comme "éteinte" sur la carte.
     */
    public boolean isVisible(int index)
    {return true;}

    /**
     * Retourne les coordonnées (<var>x</var>,<var>y</var>) de la marque désignée par l'index
     * spécifié. Les coordonnées doivent être exprimées selon le système de coordonnées spécifié
     * lors de la construction (WGS 1984 par défaut). Cette méthode est autorisée à retourner
     * <code>null</code> si la position d'une marque n'est pas connue. Dans ce cas, la marque
     * ne sera pas tracée.
     *
     * @throws IndexOutOfBoundsException Si l'index spécifié n'est pas
     *         dans la plage <code>[0..{@link #getCount}-1]</code>.
     */
    public abstract Point2D getPosition(int index) throws IndexOutOfBoundsException;

    /**
     * Retourne les unités de l'amplitude, ou <code>null</code> si ces unités ne sont pas connues.
     * L'implémentation par défaut retourne toujours <code>null</code>. Les unités de la direction,
     * pour leur part, seront toujours en radians.
     */
    public Unit getAmplitudeUnit()
    {return null;}

    /**
     * Retourne l'amplitude typique des valeurs de cette couche. Cette information est à interpréter
     * de pair avec celles que retourne {@link #getAmplitude}. Les marques associées à des valeurs qui
     * ont une amplitude égale à l'amplitude typique paraîtront de la taille normale;  les marques qui
     * ont une amplitude deux fois plus grande que l'amplitude typique paraîtront deux fois plus grosse,
     * etc. L'implémentation par défaut retourne la valeur RMS de toutes les amplitudes retournées par
     * {@link #getAmplitude}.
     */
    public synchronized double getTypicalAmplitude()
    {
        if (!(typicalAmplitude>0))
        {
            int n=0;
            double rms=0;
            for (int i=getCount(); --i>=0;)
            {
                final double v=getAmplitude(i);
                if (!Double.isNaN(v))
                {
                    rms += v*v;
                    n++;
                }
            }
            typicalAmplitude = (n>0) ? Math.sqrt(rms/n) : 1;
        }
        return typicalAmplitude;
    }

    /**
     * Retourne l'amplitude horizontale à la position d'une marque. L'amplitude horizontale indique
     * de quelle grosseur doit apparaître la marque. Plus l'amplitude est élevée,  plus la marque
     * paraîtra grosse. Cette information est principalement utilisée pour dessiner des flèches de
     * courants ou de vents. L'implémentation par défaut retourne toujours 1.
     */
    public double getAmplitude(int index)
    {return 1;}

    /**
     * Retourne la direction à la position d'une marque, en radians arithmétiques. Cette
     * information est particulièrement utile pour le traçage de flèches de courants ou
     * de vents. L'implémentation par défaut retourne toujours 0.
     */
    public double getDirection(int index)
    {return 0;}

    /**
     * Retourne la forme géométrique servant de modèle au traçage des marques. Cette forme peut varier
     * d'une marque à l'autre, ou être la même pour toutes les marques. Cette forme doit être ancrée à
     * l'origine (0,0) et ses coordonnées doivent être exprimées en points (1/72 de pouces). Par exemple
     * pour dessiner des flèches de courants, la forme modèle devrait être une flèche toujours orientée
     * vers l'axe des <var>x</var> positif (le 0° arithmétique), avoir sa base centrée à (0,0) et être
     * de dimension raisonable (par exemple 16x4 pixels). La méthode {@link #paint(GraphicsJAI, MapPaintContext)}
     * prendra automatiquement en charge les rotations et translations pour ajuster le modèle aux différentes
     * marques. L'implémentation par défaut retourne toujours un cercle centré à (0,0) et d'un diamètre de 10
     * points.
     */
    public Shape getShape(int index)
    {return DEFAULT_SHAPE;}

    /**
     * Dessine la forme géométrique spécifiée. Cette méthode est appellée automatiquement par la méthode
     * {@link #paint(GraphicsJAI, MapPaintContext)}. Les classes dérivées peuvent la redéfinir si elles
     * veulent modifier la façon dont les stations sont dessinées. Cette méthode reçoit en argument une
     * forme géométrique <code>shape</code> à dessiner dans <code>graphics</code>. Les rotations,
     * translations et facteurs d'échelles nécessaires pour bien représenter la marque auront déjà été
     * pris en compte. Le graphique <code>graphics</code> a déja reçu la transformation affine appropriée.
     * L'implémentation par défaut ne fait qu'appeller le code suivant:
     *
     * <blockquote><pre>
     * graphics.setColor(<var>defaultColor</var>);
     * graphics.fill(shape);
     * </pre></blockquote>
     *
     * @param graphics Graphique à utiliser pour tracer la marque. L'espace de coordonnées
     *                 de ce graphique sera les pixels en les points (1/72 de pouce).
     * @param shape    Forme géométrique représentant la marque à tracer.
     * @param index    Index de la marque à tracer.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index)
    {
        graphics.setColor(DEFAULT_COLOR);
        graphics.fill(shape);
    }

    /**
     * Retourne les indices qui correspondent aux coordonnées spécifiées.
     * Ces indices seront utilisées par {@link #isVisible(int,Rectangle)}
     * pour vérifier si un point est dans la partie visible. Cette méthode
     * sera définie par {@link GridMarkLayer}.
     *
     * @param visibleArea Coordonnées logiques de la région visible à l'écran.
     */
    Rectangle getUserClip(final Rectangle2D visibleArea)
    {return null;}

    /**
     * Indique si la station à l'index spécifié est visible
     * dans le clip spécifié. Le rectangle <code>clip</code>
     * doit avoir été obtenu par {@link #getUserClip}. Cette
     * méthode sera définie par {@link GridMarkLayer}.
     */
    boolean isVisible(final int index, final Rectangle clip)
    {return true;}

    /**
     * Fait en sorte que {@link #transformedShapes} soit non-nul et ait
     * exactement la longueur nécessaire pour contenir toutes les formes
     * géométriques des stations. Si un nouveau tableau a du être créé,
     * cette méthode retourne <code>true</code>. Si l'ancien tableau n'a
     * pas été modifié parce qu'il convenait déjà, alors cette méthode
     * retourne <code>false</code>.
     */
    private boolean validateShapesArray(final int shapesCount)
    {
        if (transformedShapes==null || transformedShapes.length!=shapesCount)
        {
            transformedShapes = new Shape[shapesCount];
            return true;
        }
        return false;
    }

    /**
     * Procède au traçage des marques de cette couche. Les classes dérivées ne
     * devraient pas avoir besoin de redéfinir cette méthode. Pour modifier la
     * façon de dessiner les marques, redéfinissez plutôt une des méthodes
     * énumérées plus haut.
     *
     * @throws TransformException si une projection cartographique était
     *         nécessaire et a échouée.
     *
     * @see #getShape
     * @see #paint(GraphicsJAI, Shape, int)
     */
    protected synchronized Shape paint(final GraphicsJAI graphics, final MapPaintContext context) throws TransformException
    {
        final AffineTransform fromPoints = context.getAffineTransform(MapPaintContext.FROM_POINT_TO_PIXEL);
        final AffineTransform fromWorld  = context.getAffineTransform(MapPaintContext.FROM_WORLD_TO_POINT);
        final Rectangle   zoomableBounds = context.getZoomableBounds();
        final int             count      = getCount();
        if (count!=0)
        {
            /*
             * Vérifie si la transformation affine est la même que la dernière fois.   Si ce n'est pas le cas, alors
             * on va recréer une liste de toutes les formes géométriques transformées. Cette liste servira à la fois
             * à tracer les flèches et, plus tard,  à déterminer si le curseur de la souris traîne sur l'une d'entre
             * elles. Certains éléments peuvent être nuls s'ils n'apparaissent pas dans la zone de traçage.
             */
            final CoordinateTransform projection=context.getCoordinateTransform(this);
            if (validateShapesArray(count) || !equals(projection, lastProjection) || !fromWorld.equals(lastTransform))
            {
                boundingBox    = null;
                lastProjection = projection;
                lastTransform  = fromWorld;
                Rectangle userClip;
                try
                {
                    Rectangle2D visibleArea = XAffineTransform.inverseTransform(fromWorld, zoomableBounds, null);
                    visibleArea = OpenGIS.transform(projection.inverse(), visibleArea, visibleArea);
                    userClip = getUserClip(visibleArea);
                }
                catch (NoninvertibleTransformException exception)
                {
                    userClip = null;
                }
                catch (TransformException exception)
                {
                    userClip = null;
                }
                /*
                 * On veut utiliser une transformation affine identité (donc en utilisant
                 * une échelle basée sur les pixels plutôt que les coordonnées utilisateur),
                 * mais en utilisant la même rotation que celle qui a cours dans la matrice
                 * <code>fromWorld</code>. On peut y arriver en utilisant l'identité ci-dessous:
                 *
                 *    [ m00  m01 ]     m00² + m01²  == constante sous rotation
                 *    [ m10  m11 ]     m10² + m11²  == constante sous rotation
                 */
                double scale;
                final double[] matrix=new double[6];
                fromWorld.getMatrix(matrix);
                scale = XMath.hypot(matrix[0], matrix[2]);
                matrix[0] /= scale;
                matrix[2] /= scale;
                scale = XMath.hypot(matrix[1], matrix[3]);
                matrix[1] /= scale;
                matrix[3] /= scale;
                /*
                 * Initialise quelques variables qui
                 * serviront dans le reste de ce bloc...
                 */
                final double typicalScale = getTypicalAmplitude();
                final AffineTransform tr  = new AffineTransform();
                double[] array            = new double[32];
                double[] buffer           = new double[32];
                int   [] X                = new int   [16];
                int   [] Y                = new int   [16];
                int      pointIndex       = 0;
                int      shapeIndex       = 0;
                Shape    lastShape        = null;
                boolean  shapeIsPolygon   = false;
                /*
                 * Balaie les données de chaques marques. Pour chacune d'elles,
                 * on définiera une transformation affine qui prendra en compte
                 * les translations et rotations de la marque. Cette transformation
                 * servira à transformer les coordonnées de la marque "modèle" en
                 * coordonnées pixels propres à chaque marque.
                 */
                for (int i=0; i<count; i++)
                {
                    Point2D point;
                    final Shape shape;
                    if (!isVisible(i, userClip) || (point=getPosition(i))==null || (shape=getShape(i))==null)
                    {
                        transformedShapes[shapeIndex++]=null;
                        continue;
                    }
                    point=projection.transform(point, point);
                    matrix[4] = point.getX();
                    matrix[5] = point.getY();
                    fromWorld.transform(matrix, 4, matrix, 4, 1);
                    tr.setTransform(matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
                    scale = getAmplitude(i)/typicalScale;
                    tr.scale(scale,scale);
                    tr.rotate(getDirection(i));
                    /*
                     * Maintenant que la transformation affine a été définie,
                     * on créera les formes géométriques transformées des marques.
                     * Ces formes transformées resteront mémorisées dans une cache
                     * interne afin d'éviter d'avoir à les recalculer la prochaine
                     * fois.
                     */
                    if (shape!=lastShape)
                    {
                        lastShape      = shape;
                        shapeIsPolygon = false;
                        final PathIterator pit=shape.getPathIterator(null);
                        if (!pit.isDone() && pit.currentSegment(array)==PathIterator.SEG_MOVETO)
                        {
                            pointIndex=2;
                            for (pit.next(); !pit.isDone(); pit.next())
                            {
                                switch (pit.currentSegment(buffer))
                                {
                                    case PathIterator.SEG_LINETO:
                                    {
                                        if (pointIndex >= array.length)
                                            array=XArray.resize(array, 2*pointIndex);
                                        System.arraycopy(buffer, 0, array, pointIndex, 2);
                                        pointIndex += 2;
                                        continue;
                                    }
                                    case PathIterator.SEG_CLOSE:
                                    {
                                        pit.next();
                                        shapeIsPolygon = pit.isDone();
                                    }
                                }
                                break;
                            }
                        }
                    }
                    /*
                     * Les coordonnées de la forme géométrique ayant été obtenue,
                     * créé une forme géométrique transformée (c'est-à-dire dont
                     * les coordonnées seront exprimées en pixels au lieu d'être
                     * en mètres).
                     */
                    final Shape transformedShape;
                    if (!shapeIsPolygon)
                    {
                        // La méthode 'createTransformedShape' créé généralement un objet
                        // 'GeneralPath', qui peut convenir mais qui est quand même un peu
                        // lourd. Si possible, on va plutôt utiliser le code du bloc suivant,
                        // qui créera un objet 'Polygon'.
                        transformedShape = tr.createTransformedShape(shape);
                    }
                    else
                    {
                        if (pointIndex > buffer.length)
                            buffer = XArray.resize(buffer, pointIndex);
                        final int length = pointIndex >> 1;
                        tr.transform(array, 0, buffer, 0, length);
                        if (length > X.length) X=XArray.resize(X, length);
                        if (length > Y.length) Y=XArray.resize(Y, length);
                        for (int j=0; j<length; j++)
                        {
                            final int k = (j << 1);
                            X[j] = (int) Math.round(buffer[k+0]);
                            Y[j] = (int) Math.round(buffer[k+1]);
                        }
                        transformedShape = new Polygon(X,Y,length);
                    }
                    /*
                     * Construit un rectangle qui englobera toutes
                     * les marques. Ce rectangle sera utilisé par
                     * {@link MapPanel} pour détecter quand la souris
                     * traîne dans la région...
                     */
                    transformedShapes[shapeIndex++] = (transformedShape.intersects(zoomableBounds)) ? transformedShape : null;
                    final Rectangle bounds=transformedShape.getBounds();
                    if (boundingBox==null) boundingBox=bounds;
                    else boundingBox.add(bounds);
                }
            }
            /*
             * Procède maintenant au traçage de
             * toutes les marques de la couche.
             */
            final AffineTransform graphicsTr = graphics.getTransform();
            final Stroke          oldStroke  = graphics.getStroke();
            final Paint           oldPaint   = graphics.getPaint();
            try
            {
                graphics.setTransform(fromPoints);
                graphics.setStroke(new BasicStroke(0));
                final Rectangle clip=graphics.getClipBounds();
                for (int i=0; i<transformedShapes.length; i++)
                {
                    if (isVisible(i))
                    {
                        final Shape shape=transformedShapes[i];
                        if (shape!=null && (clip==null || shape.intersects(clip)))
                        {
                            paint(graphics, shape, i);
                        }
                    }
                }
            }
            finally
            {
                graphics.setTransform(graphicsTr);
                graphics.setStroke(oldStroke);
                graphics.setPaint(oldPaint);
            }
        }
        return boundingBox;
    }

    /**
     * Indique que cette couche a besoin d'être redéssinée. Cette méthode
     * <code>repaint()</code> peut être appelée à partir de n'importe quel
     * thread (pas nécessairement celui de <i>Swing</i>).
     */
    public void repaint()
    {
        clearCache();
        super.repaint();
    }

    /**
     * Déclare que la station spécifiée a besoin d'être redessinée.
     * Cette méthode peut être utilisée pour faire apparaître ou
     * disparaître une station, après que sa visibilité (telle que
     * retournée par {@link #isVisible}) ait changée.
     *
     * Si un nombre restreint de stations sont à redessiner, cette
     * méthode sera efficace car elle provoquera le retraçage d'une
     * portion relativement petite de la carte. Si toutes les stations
     * sont à redessiner, il peut être plus efficace d'appeller {@link
     * #repaint()}.
     */
    protected synchronized void repaint(final int index)
    {
        if (transformedShapes!=null)
        {
            final Shape shape=transformedShapes[index];
            if (shape!=null)
            {
                repaint(shape.getBounds());
                return;
            }
        }
        repaint();
    }

    /**
     * Méthode appelée automatiquement pour construire une chaîne de caractères représentant la valeur
     * pointée par la souris. En général (mais pas obligatoirement), lorsque cette méthode est appelée,
     * le buffer <code>toAppendTo</code> contiendra déjà une chaîne de caractères représentant les
     * coordonnées pointées par la souris. Cette méthode est appelée pour donner une chance à cette
     * couche d'ajouter d'autres informations propre à la marque <code>index</code>.
     *
     * <p>L'implémentation par défaut de cette méthode retourne toujours <code>false</code> sans rien
     * faire.</p>
     *
     * @param  index Index de la marque sur laquelle se trouve le curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette méthode a ajouté des informations dans <code>toAppendTo</code>.
     *         Dans ce cas, les couches en-dessous de <code>this</code> ne seront pas interrogées.
     */
    protected boolean getLabel(int index, StringBuffer toAppendTo)
    {return false;}

    /**
     * Retourne le texte à afficher dans une bulle lorsque le curseur de la souris traîne
     * sur une marque. L'implémentation par défaut retourne toujours <code>null</code>.
     *
     * @param  index Index de la marque sur laquelle traîne le curseur.
     * @return Le texte à afficher lorsque la souris traîne sur cette station.
     *         Ce texte peut être nul pour signifier qu'il ne faut pas en écrire.
     */
    protected String getToolTipText(int index)
    {return null;}

    /**
     * Méthode appellée automatiquement chaque fois qu'il a été déterminé qu'un menu contextuel devrait
     * être affiché. Sur Windows et Solaris, cette méthode est appelée lorsque l'utilisateur a appuyé
     * sur le bouton droit de la souris. Si la marque <code>index</code> désire faire apparaître un menu,
     * elle devrait retourner le menu en question. Si non, elle devrait retourner <code>null</code>.
     * L'implémentation par défaut retourne toujours <code>null</code>.
     *
     * @param  index Index de la marque pour laquelle l'utilisateur a demandé un menu contextuel.
     * @return Menu contextuel à faire apparaître, ou <code>null</code>
     *         si cette marque ne propose pas de menu contextuel.
     */
    protected JPopupMenu getPopupMenu(int index)
    {return null;}

    /**
     * Méthode appellée chaque fois que le bouton de la souris a été cliqué
     * sur cette couche. L'implémentation par défaut ne fait rien.
     */
    protected void mouseClicked(int index)
    {}

    /**
     * Méthode appelée automatiquement pour construire une chaîne de caractères représentant la valeur
     * pointée par la souris. Cette méthode identifie sur quelle station pointait la souris et appelle
     * la méthode {@link #getLabel(int,StringBuffer)}.
     *
     * @param  event Coordonnées du curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette méthode a ajouté des informations dans <code>toAppendTo</code>.
     *         Dans ce cas, les couches en-dessous de <code>this</code> ne seront pas interrogées.
     */
    protected final synchronized boolean getLabel(final GeoMouseEvent event, final StringBuffer toAppendTo)
    {
        final Shape[] transformedShapes=this.transformedShapes;
        if (transformedShapes!=null)
        {
            Shape shape;
            final Point2D point = this.point = event.getPoint2D(this.point);
            for (int i=transformedShapes.length; --i>=0;)
                if (isVisible(i) && (shape=transformedShapes[i])!=null)
                    if (shape.contains(point))
                        if (getLabel(i, toAppendTo))
                            return true;
        }
        return super.getLabel(event, toAppendTo);
    }

    /**
     * Retourne le texte à afficher dans une bulle lorsque le curseur
     * de la souris traîne sur la carte. L'implémentation par défaut
     * identifie la marque sur laquelle traîne le curseur et appelle
     * {@link #getToolTipText(int)}.
     *
     * @param  event Coordonnées du curseur de la souris.
     * @return Le texte à afficher lorsque la souris traîne sur cet élément.
     *         Ce texte peut être nul pour signifier qu'il ne faut pas en écrire.
     */
    protected final synchronized String getToolTipText(final GeoMouseEvent event)
    {
        final Shape[] transformedShapes=this.transformedShapes;
        if (transformedShapes!=null)
        {
            Shape shape;
            final Point2D point = this.point = event.getPoint2D(this.point);
            for (int i=transformedShapes.length; --i>=0;)
            {
                if (isVisible(i) && (shape=transformedShapes[i])!=null)
                {
                    if (shape.contains(point))
                    {
                        final String text=getToolTipText(i);
                        if (text!=null) return text;
                    }
                }
            }
        }
        return super.getToolTipText(event);
    }

    /**
     * Méthode appellée chaque fois que le bouton de la souris a été cliqué
     * sur cette couche. L'implémentation par défaut identifie la ou les
     * marques sur lesquelles pointe le curseur de la souris et appelle
     * {@link #mouseClicked(int)}.
     */
    protected final synchronized void mouseClicked(final GeoMouseEvent event)
    {
        final Shape[] transformedShapes=this.transformedShapes;
        if (transformedShapes!=null)
        {
            Shape shape;
            final Point2D point = this.point = event.getPoint2D(this.point);
            for (int i=transformedShapes.length; --i>=0;)
                if (isVisible(i) && (shape=transformedShapes[i])!=null)
                    if (shape.contains(point))
                        mouseClicked(i);
        }
        super.mouseClicked(event);
    }

    /**
     * Méthode appellée automatiquement chaque fois qu'il a été déterminé qu'un menu contextuel devrait
     * être affiché. L'implémentation par défaut identifie la marque sur laquelle pointe le curseur et
     * appelle {@link #getPopupMenu(int)}.
     *
     * @param  event Coordonnées du curseur de la souris.
     * @return Menu contextuel à faire apparaître, ou <code>null</code>
     *         si cette couche ne propose pas de menu contextuel.
     */
    protected final synchronized JPopupMenu getPopupMenu(final GeoMouseEvent event)
    {
        final Shape[] transformedShapes=this.transformedShapes;
        if (transformedShapes!=null)
        {
            Shape shape;
            final Point2D point = this.point = event.getPoint2D(this.point);
            for (int i=transformedShapes.length; --i>=0;)
            {
                if (isVisible(i) && (shape=transformedShapes[i])!=null)
                {
                    if (shape.contains(point))
                    {
                        final JPopupMenu menu=getPopupMenu(i);
                        if (menu!=null) return menu;
                    }
                }
            }
        }
        return super.getPopupMenu(event);
    }

    /**
     * Efface des informations qui avaient été conservées dans une mémoire cache.  Cette méthode est
     * automatiquement appelée lorsqu'il a été déterminé que cette couche ne sera plus affichée avant
     * un certain temps.
     */
    protected synchronized void clearCache()
    {
        lastTransform     = null;
        lastProjection    = null;
        transformedShapes = null;
        boundingBox       = null;
        typicalAmplitude  = Double.NaN;
    }

    /**
     * Indique si les deux objets spécifiés sont égaux.
     * Cette méthode accepte des <code>null</code>.
     */
    static boolean equals(final Object a, final Object b)
    {return (a==b || (a!=null && a.equals(b)));}
}
