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
 * Repr�sentation graphique d'un ensemble marques apparaissant sur une carte. Ces marques peuvent
 * �tre par exemple des points repr�sentant les positions de stations, ou des fl�ches de courants.
 * L'impl�mentation par d�faut de cette classe ne fait que dessiner des points � certaines positions
 * Les classes d�riv�es peuvent impl�menter un dessin plus �volu�, par exemple une fl�che de courant
 * ou une ellipse de mar�e. La fa�on de m�moris�es les donn�es est laiss�e � la discr�tion des classes
 * d�riv�es. Toute classe concr�te devra impl�menter au moins les deux m�thodes suivantes, qui servent
 * � obtenir les coordonn�es des stations:
 *
 * <ul>
 *   <li>{@link #getCount}</li>
 *   <li>{@link #getPosition}</li>
 * </ul>
 *
 * Si, � la position de chaque marque, on souhaite dessiner une figure orientable dans l'espace (par
 * exemple une fl�che de courant ou une ellipse de mar�e), la classe d�riv�e pourra red�finir une ou
 * plusieurs des m�thodes ci-dessous. Red�finir ces m�thodes permet par exemple de dessiner des fl�ches
 * dont la forme exacte (par exemple une, deux ou trois t�tes) et la couleur varie avec l'amplitude, la
 * direction ou d'autres crit�res � votre choix.
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
     * Forme g�om�trique � utiliser par d�faut lorsqu'aucune autre forme n'a
     * �t� sp�cifi�e. La position centrale de la station doit correspondre �
     * la coordonn�e (0,0) de cette forme. La dimension de cette forme est
     * exprim�e en pixels. La forme par d�faut sera un cercle centr� �
     * (0,0) et d'un diam�tre de 10 pixels.
     */
    static final Shape DEFAULT_SHAPE = new Ellipse2D.Float(-5, -5, 10, 10);

    /**
     * Couleur des marques. La couleur par d�faut sera orang�e.
     */
    static final Color DEFAULT_COLOR=new Color(234, 192, 0);

    /**
     * Projection cartographique utilis�e la
     * derni�re fois pour obtenir les donn�es.
     */
    private transient CoordinateTransform lastProjection;

    /**
     * Transformation affine utilis�e la derni�re fois.
     * Cette information est utilis�e pour savoir si on
     * peut r�utiliser {@link #transformedShapes}.
     */
    private transient AffineTransform lastTransform;

    /**
     * Formes g�om�triques transform�es utilis�es la derni�re fois.
     * Ces formes seront r�utilis�es autant que possible plut�t que
     * d'�tre constamment recalcul�es.
     */
    private transient Shape[] transformedShapes;

    /**
     * Bo�te englobant toutes les coordonn�es des formes apparaissant
     * dans {@link #transformedShapes}. Les coordonn�es de cette bo�te
     * seront en pixels.
     */
    private transient Rectangle boundingBox;

    /**
     * Amplitude typique calcul�e par {@link #getTypicalAmplitude}.
     * Cette information est conserv�e dans une cache interne pour
     * des acc�s plus rapides.
     */
    private transient double typicalAmplitude;

    /**
     * Point utilis� temporairement lors
     * des mouvements de la souris.
     */
    private transient Point2D point;

    /**
     * Construit un ensemble de marques. Les coordonn�es de ces
     * marques seront exprim�es selon le syst�me de coordonn�es
     * par d�faut (WGS 1984).
     */
    protected MarkLayer()
    {super();}

    /**
     * Construit un ensemble de marques. Les coordonn�es de ces
     * marques seront exprim�es selon le syst�me de coordonn�es
     * sp�cifi�.
     */
    protected MarkLayer(final CoordinateSystem coordinateSystem)
    {super(coordinateSystem);}

    /**
     * Retourne le nombre de marques m�moris�es dans cette couche.
     * Les donn�es de chacune de ces marques pourront �tre acc�d�es
     * � l'aides des diff�rentes m�thodes <code>get*</code> de cette
     * classe.
     *
     * @see #getPosition
     * @see #getAmplitude
     * @see #getDirection
     */
    public abstract int getCount();

    /**
     * Indique si la marque point�e par l'index sp�cifi� est visible. L'impl�mentation par
     * d�faut retourne toujours <code>true</code>. Les classes d�riv�es peuvent red�finir
     * cette m�thode si elles veulent que certaines marques ne soient pas visible sur la
     * carte. Les classes d�riv�es ne sont pas tenues de retourner toujours la m�me valeur
     * pour un index donn�. Par exemple deux appels cons�cutifs � <code>isVisible(23)</code>
     * pourraient retourner <code>true</code> la premi�re fois et <code>false</code> la
     * seconde, ce qui indiquerait que la station #23 apparaissait d'abord comme "allum�e",
     * puis comme "�teinte" sur la carte.
     */
    public boolean isVisible(int index)
    {return true;}

    /**
     * Retourne les coordonn�es (<var>x</var>,<var>y</var>) de la marque d�sign�e par l'index
     * sp�cifi�. Les coordonn�es doivent �tre exprim�es selon le syst�me de coordonn�es sp�cifi�
     * lors de la construction (WGS 1984 par d�faut). Cette m�thode est autoris�e � retourner
     * <code>null</code> si la position d'une marque n'est pas connue. Dans ce cas, la marque
     * ne sera pas trac�e.
     *
     * @throws IndexOutOfBoundsException Si l'index sp�cifi� n'est pas
     *         dans la plage <code>[0..{@link #getCount}-1]</code>.
     */
    public abstract Point2D getPosition(int index) throws IndexOutOfBoundsException;

    /**
     * Retourne les unit�s de l'amplitude, ou <code>null</code> si ces unit�s ne sont pas connues.
     * L'impl�mentation par d�faut retourne toujours <code>null</code>. Les unit�s de la direction,
     * pour leur part, seront toujours en radians.
     */
    public Unit getAmplitudeUnit()
    {return null;}

    /**
     * Retourne l'amplitude typique des valeurs de cette couche. Cette information est � interpr�ter
     * de pair avec celles que retourne {@link #getAmplitude}. Les marques associ�es � des valeurs qui
     * ont une amplitude �gale � l'amplitude typique para�tront de la taille normale;  les marques qui
     * ont une amplitude deux fois plus grande que l'amplitude typique para�tront deux fois plus grosse,
     * etc. L'impl�mentation par d�faut retourne la valeur RMS de toutes les amplitudes retourn�es par
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
     * Retourne l'amplitude horizontale � la position d'une marque. L'amplitude horizontale indique
     * de quelle grosseur doit appara�tre la marque. Plus l'amplitude est �lev�e,  plus la marque
     * para�tra grosse. Cette information est principalement utilis�e pour dessiner des fl�ches de
     * courants ou de vents. L'impl�mentation par d�faut retourne toujours 1.
     */
    public double getAmplitude(int index)
    {return 1;}

    /**
     * Retourne la direction � la position d'une marque, en radians arithm�tiques. Cette
     * information est particuli�rement utile pour le tra�age de fl�ches de courants ou
     * de vents. L'impl�mentation par d�faut retourne toujours 0.
     */
    public double getDirection(int index)
    {return 0;}

    /**
     * Retourne la forme g�om�trique servant de mod�le au tra�age des marques. Cette forme peut varier
     * d'une marque � l'autre, ou �tre la m�me pour toutes les marques. Cette forme doit �tre ancr�e �
     * l'origine (0,0) et ses coordonn�es doivent �tre exprim�es en points (1/72 de pouces). Par exemple
     * pour dessiner des fl�ches de courants, la forme mod�le devrait �tre une fl�che toujours orient�e
     * vers l'axe des <var>x</var> positif (le 0� arithm�tique), avoir sa base centr�e � (0,0) et �tre
     * de dimension raisonable (par exemple 16x4 pixels). La m�thode {@link #paint(GraphicsJAI, MapPaintContext)}
     * prendra automatiquement en charge les rotations et translations pour ajuster le mod�le aux diff�rentes
     * marques. L'impl�mentation par d�faut retourne toujours un cercle centr� � (0,0) et d'un diam�tre de 10
     * points.
     */
    public Shape getShape(int index)
    {return DEFAULT_SHAPE;}

    /**
     * Dessine la forme g�om�trique sp�cifi�e. Cette m�thode est appell�e automatiquement par la m�thode
     * {@link #paint(GraphicsJAI, MapPaintContext)}. Les classes d�riv�es peuvent la red�finir si elles
     * veulent modifier la fa�on dont les stations sont dessin�es. Cette m�thode re�oit en argument une
     * forme g�om�trique <code>shape</code> � dessiner dans <code>graphics</code>. Les rotations,
     * translations et facteurs d'�chelles n�cessaires pour bien repr�senter la marque auront d�j� �t�
     * pris en compte. Le graphique <code>graphics</code> a d�ja re�u la transformation affine appropri�e.
     * L'impl�mentation par d�faut ne fait qu'appeller le code suivant:
     *
     * <blockquote><pre>
     * graphics.setColor(<var>defaultColor</var>);
     * graphics.fill(shape);
     * </pre></blockquote>
     *
     * @param graphics Graphique � utiliser pour tracer la marque. L'espace de coordonn�es
     *                 de ce graphique sera les pixels en les points (1/72 de pouce).
     * @param shape    Forme g�om�trique repr�sentant la marque � tracer.
     * @param index    Index de la marque � tracer.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index)
    {
        graphics.setColor(DEFAULT_COLOR);
        graphics.fill(shape);
    }

    /**
     * Retourne les indices qui correspondent aux coordonn�es sp�cifi�es.
     * Ces indices seront utilis�es par {@link #isVisible(int,Rectangle)}
     * pour v�rifier si un point est dans la partie visible. Cette m�thode
     * sera d�finie par {@link GridMarkLayer}.
     *
     * @param visibleArea Coordonn�es logiques de la r�gion visible � l'�cran.
     */
    Rectangle getUserClip(final Rectangle2D visibleArea)
    {return null;}

    /**
     * Indique si la station � l'index sp�cifi� est visible
     * dans le clip sp�cifi�. Le rectangle <code>clip</code>
     * doit avoir �t� obtenu par {@link #getUserClip}. Cette
     * m�thode sera d�finie par {@link GridMarkLayer}.
     */
    boolean isVisible(final int index, final Rectangle clip)
    {return true;}

    /**
     * Fait en sorte que {@link #transformedShapes} soit non-nul et ait
     * exactement la longueur n�cessaire pour contenir toutes les formes
     * g�om�triques des stations. Si un nouveau tableau a du �tre cr��,
     * cette m�thode retourne <code>true</code>. Si l'ancien tableau n'a
     * pas �t� modifi� parce qu'il convenait d�j�, alors cette m�thode
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
     * Proc�de au tra�age des marques de cette couche. Les classes d�riv�es ne
     * devraient pas avoir besoin de red�finir cette m�thode. Pour modifier la
     * fa�on de dessiner les marques, red�finissez plut�t une des m�thodes
     * �num�r�es plus haut.
     *
     * @throws TransformException si une projection cartographique �tait
     *         n�cessaire et a �chou�e.
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
             * V�rifie si la transformation affine est la m�me que la derni�re fois.   Si ce n'est pas le cas, alors
             * on va recr�er une liste de toutes les formes g�om�triques transform�es. Cette liste servira � la fois
             * � tracer les fl�ches et, plus tard,  � d�terminer si le curseur de la souris tra�ne sur l'une d'entre
             * elles. Certains �l�ments peuvent �tre nuls s'ils n'apparaissent pas dans la zone de tra�age.
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
                 * On veut utiliser une transformation affine identit� (donc en utilisant
                 * une �chelle bas�e sur les pixels plut�t que les coordonn�es utilisateur),
                 * mais en utilisant la m�me rotation que celle qui a cours dans la matrice
                 * <code>fromWorld</code>. On peut y arriver en utilisant l'identit� ci-dessous:
                 *
                 *    [ m00  m01 ]     m00� + m01�  == constante sous rotation
                 *    [ m10  m11 ]     m10� + m11�  == constante sous rotation
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
                 * Balaie les donn�es de chaques marques. Pour chacune d'elles,
                 * on d�finiera une transformation affine qui prendra en compte
                 * les translations et rotations de la marque. Cette transformation
                 * servira � transformer les coordonn�es de la marque "mod�le" en
                 * coordonn�es pixels propres � chaque marque.
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
                     * Maintenant que la transformation affine a �t� d�finie,
                     * on cr�era les formes g�om�triques transform�es des marques.
                     * Ces formes transform�es resteront m�moris�es dans une cache
                     * interne afin d'�viter d'avoir � les recalculer la prochaine
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
                     * Les coordonn�es de la forme g�om�trique ayant �t� obtenue,
                     * cr�� une forme g�om�trique transform�e (c'est-�-dire dont
                     * les coordonn�es seront exprim�es en pixels au lieu d'�tre
                     * en m�tres).
                     */
                    final Shape transformedShape;
                    if (!shapeIsPolygon)
                    {
                        // La m�thode 'createTransformedShape' cr�� g�n�ralement un objet
                        // 'GeneralPath', qui peut convenir mais qui est quand m�me un peu
                        // lourd. Si possible, on va plut�t utiliser le code du bloc suivant,
                        // qui cr�era un objet 'Polygon'.
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
                     * les marques. Ce rectangle sera utilis� par
                     * {@link MapPanel} pour d�tecter quand la souris
                     * tra�ne dans la r�gion...
                     */
                    transformedShapes[shapeIndex++] = (transformedShape.intersects(zoomableBounds)) ? transformedShape : null;
                    final Rectangle bounds=transformedShape.getBounds();
                    if (boundingBox==null) boundingBox=bounds;
                    else boundingBox.add(bounds);
                }
            }
            /*
             * Proc�de maintenant au tra�age de
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
     * Indique que cette couche a besoin d'�tre red�ssin�e. Cette m�thode
     * <code>repaint()</code> peut �tre appel�e � partir de n'importe quel
     * thread (pas n�cessairement celui de <i>Swing</i>).
     */
    public void repaint()
    {
        clearCache();
        super.repaint();
    }

    /**
     * D�clare que la station sp�cifi�e a besoin d'�tre redessin�e.
     * Cette m�thode peut �tre utilis�e pour faire appara�tre ou
     * dispara�tre une station, apr�s que sa visibilit� (telle que
     * retourn�e par {@link #isVisible}) ait chang�e.
     *
     * Si un nombre restreint de stations sont � redessiner, cette
     * m�thode sera efficace car elle provoquera le retra�age d'une
     * portion relativement petite de la carte. Si toutes les stations
     * sont � redessiner, il peut �tre plus efficace d'appeller {@link
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
     * M�thode appel�e automatiquement pour construire une cha�ne de caract�res repr�sentant la valeur
     * point�e par la souris. En g�n�ral (mais pas obligatoirement), lorsque cette m�thode est appel�e,
     * le buffer <code>toAppendTo</code> contiendra d�j� une cha�ne de caract�res repr�sentant les
     * coordonn�es point�es par la souris. Cette m�thode est appel�e pour donner une chance � cette
     * couche d'ajouter d'autres informations propre � la marque <code>index</code>.
     *
     * <p>L'impl�mentation par d�faut de cette m�thode retourne toujours <code>false</code> sans rien
     * faire.</p>
     *
     * @param  index Index de la marque sur laquelle se trouve le curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette m�thode a ajout� des informations dans <code>toAppendTo</code>.
     *         Dans ce cas, les couches en-dessous de <code>this</code> ne seront pas interrog�es.
     */
    protected boolean getLabel(int index, StringBuffer toAppendTo)
    {return false;}

    /**
     * Retourne le texte � afficher dans une bulle lorsque le curseur de la souris tra�ne
     * sur une marque. L'impl�mentation par d�faut retourne toujours <code>null</code>.
     *
     * @param  index Index de la marque sur laquelle tra�ne le curseur.
     * @return Le texte � afficher lorsque la souris tra�ne sur cette station.
     *         Ce texte peut �tre nul pour signifier qu'il ne faut pas en �crire.
     */
    protected String getToolTipText(int index)
    {return null;}

    /**
     * M�thode appell�e automatiquement chaque fois qu'il a �t� d�termin� qu'un menu contextuel devrait
     * �tre affich�. Sur Windows et Solaris, cette m�thode est appel�e lorsque l'utilisateur a appuy�
     * sur le bouton droit de la souris. Si la marque <code>index</code> d�sire faire appara�tre un menu,
     * elle devrait retourner le menu en question. Si non, elle devrait retourner <code>null</code>.
     * L'impl�mentation par d�faut retourne toujours <code>null</code>.
     *
     * @param  index Index de la marque pour laquelle l'utilisateur a demand� un menu contextuel.
     * @return Menu contextuel � faire appara�tre, ou <code>null</code>
     *         si cette marque ne propose pas de menu contextuel.
     */
    protected JPopupMenu getPopupMenu(int index)
    {return null;}

    /**
     * M�thode appell�e chaque fois que le bouton de la souris a �t� cliqu�
     * sur cette couche. L'impl�mentation par d�faut ne fait rien.
     */
    protected void mouseClicked(int index)
    {}

    /**
     * M�thode appel�e automatiquement pour construire une cha�ne de caract�res repr�sentant la valeur
     * point�e par la souris. Cette m�thode identifie sur quelle station pointait la souris et appelle
     * la m�thode {@link #getLabel(int,StringBuffer)}.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette m�thode a ajout� des informations dans <code>toAppendTo</code>.
     *         Dans ce cas, les couches en-dessous de <code>this</code> ne seront pas interrog�es.
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
     * Retourne le texte � afficher dans une bulle lorsque le curseur
     * de la souris tra�ne sur la carte. L'impl�mentation par d�faut
     * identifie la marque sur laquelle tra�ne le curseur et appelle
     * {@link #getToolTipText(int)}.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @return Le texte � afficher lorsque la souris tra�ne sur cet �l�ment.
     *         Ce texte peut �tre nul pour signifier qu'il ne faut pas en �crire.
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
     * M�thode appell�e chaque fois que le bouton de la souris a �t� cliqu�
     * sur cette couche. L'impl�mentation par d�faut identifie la ou les
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
     * M�thode appell�e automatiquement chaque fois qu'il a �t� d�termin� qu'un menu contextuel devrait
     * �tre affich�. L'impl�mentation par d�faut identifie la marque sur laquelle pointe le curseur et
     * appelle {@link #getPopupMenu(int)}.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @return Menu contextuel � faire appara�tre, ou <code>null</code>
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
     * Efface des informations qui avaient �t� conserv�es dans une m�moire cache.  Cette m�thode est
     * automatiquement appel�e lorsqu'il a �t� d�termin� que cette couche ne sera plus affich�e avant
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
     * Indique si les deux objets sp�cifi�s sont �gaux.
     * Cette m�thode accepte des <code>null</code>.
     */
    static boolean equals(final Object a, final Object b)
    {return (a==b || (a!=null && a.equals(b)));}
}
