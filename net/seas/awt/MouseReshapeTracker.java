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
package net.seas.awt;

// G�om�trie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import java.awt.geom.NoninvertibleTransformException;
import net.seagis.resources.XAffineTransform;

// Graphisme
import java.awt.Paint;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

// Interface utilisateur
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;
import javax.swing.JFormattedTextField;
import net.seas.awt.ExceptionMonitor;

// Ev�nements
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.MouseInputAdapter;

// Formats
import java.util.Date;
import java.text.Format;
import java.text.DateFormat;
import java.text.ParseException;

// Entr�s/sorties et divers
import java.io.IOException;
import java.io.ObjectInputStream;
import net.seagis.resources.XMath;
import net.seagis.resources.Utilities;
import net.seas.resources.Resources;
import net.seas.util.XArray;
import java.lang.Double;


/**
 * Contr�le la position et la taille d'une forme rectangulaire que l'utilisateur
 * peut faire bouger avec la souris. Cette classe peut s'utiliser par exemple comme suit:
 *
 * <blockquote><pre>
 * public class MyClass extends JPanel
 * {
 *     private final MouseReshapeTracker <em>slider</em>=new MouseReshapeTracker()
 *     {
 *         protected void {@link #clipChangeRequested clipChangeRequested}(double xmin, double xmax, double ymin, double ymax)
 *         {
 *             // Indique ce qu'il faut faire si l'utilisateur essaie de
 *             // d�placer le rectangle en dehors des limites permises.
 *             // Cette m�thode est facultative.
 *         }
 *
 *         protected void {@link #stateChanged stateChanged}(boolean isAdjusting)
 *         {
 *             // M�thode appell�e automatiquement chaque fois
 *             // que l'utilisateur aura chang�e la position de
 *             // la visi�re. Codez ici ce qu'il faut faire dans
 *             // ce cas.
 *         }
 *     };
 *
 *     private final AffineTransform transform=AffineTransform.getScaleInstance(10,10);
 *
 *     public MyClass()
 *     {
 *         <em>slider</em>.{@link #setFrame     setFrame}(0, 0, 1, 1);
 *         <em>slider</em>.{@link #setClip      setClip}(0, 100, 0, 1);
 *         <em>slider</em>.{@link #setTransform setTransform}(transform);
 *         addMouseMotionListener(<em>slider</em>);
 *         addMouseListener      (<em>slider</em>);
 *     }
 *
 *     public void paintComponent(Graphics graphics)
 *     {
 *         AffineTransform tr=...
 *         Graphics2D g = (Graphics2D) graphics;
 *         g.transform(transform);
 *         g.setColor(new Color(128,64,92,64));
 *         g.fill    (<em>slider</em>);
 *     }
 * }
 * </pre></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class MouseReshapeTracker extends MouseInputAdapter implements Shape
{
    /**
     * Largeur minimale que devrait
     * avoir le rectangle, en pixels.
     */
    private static final int MIN_WIDTH=12;

    /**
     * Hauteur minimale que devrait
     * avoir le rectangle, en pixels.
     */
    private static final int MIN_HEIGHT=12;

    /**
     * Distance en de�a de laquelle on consid�rera que l'utilisateur veut
     * redimensionner le rectangle plutp�t que la d�placer. Cette distance
     * est mesur�e en pixels � partir d'un des bords du rectangle.
     */
    private static final int RESIZE_POS=4;

    /**
     * Valeur minimale du rapport <code>(largeur du clip)/(largeur du rectangle)</code>.
     * Cette valeur minimale ne sera prise en compte que lorsque l'utilisateur modifie
     * la position du rectangle � l'aide de valeurs entr�es dans les champs. Ce nombre
     * doit obligatoire �tre sup�rieur ou �gal � 1.
     */
    private static final double MINSIZE_RATIO = 1.25;

    /**
     * Coordonn�e <var>x</var> minimale autoris�e pour le rectangle. La
     * valeur par d�faut est {@link java.lang.Double#NEGATIVE_INFINITY}.
     */
    private double xmin=Double.NEGATIVE_INFINITY;

    /**
     * Coordonn�e <var>y</var> minimale autoris�e pour le rectangle. La
     * valeur par d�faut est {@link java.lang.Double#NEGATIVE_INFINITY}.
     */
    private double ymin=Double.NEGATIVE_INFINITY;

    /**
     * Coordonn�e <var>x</var> maximale autoris�e pour le rectangle. La
     * valeur par d�faut est {@link java.lang.Double#POSITIVE_INFINITY}.
     */
    private double xmax=Double.POSITIVE_INFINITY;

    /**
     * Coordonn�e <var>y</var> maximale autoris�e pour le rectangle. La
     * valeur par d�faut est {@link java.lang.Double#POSITIVE_INFINITY}.
     */
    private double ymax=Double.POSITIVE_INFINITY;

    /**
     * Le rectangle � contr�ler. Les coordonn�es de ce rectangle doivent
     * �tre les coordonn�es logiques (par exemple des coordonn�es en m�tres),
     * et non les coordonn�es pixels de l'�cran. Un rectangle vide signifie
     * qu'aucune r�gion n'est pr�sentement s�lectionn�e.
     */
    private final RectangularShape logicalShape;

    /**
     * Rectangle qui sera � dessiner dans la composante. Ce rectangle
     * peut �tre diff�rent de {@link #logicalShape} et ce dernier est
     * si petit qu'il est pr�f�rable de le dessiner un peu plus large
     * que ce qu'� demand� l'utilisateur. Dans ce cas, <code>drawnShape</code>
     * servira de rectangle temporaire avec les coordonn�es �tendues.
     * Note: ce rectangle doit �tre acc�de en lecture seulement, sauf
     * par {@link #update} qui est la seule m�thode autoris�e � faire
     * sa mise � jour.
     */
    private transient RectangularShape drawnShape;

    /**
     * Transformation affine servant � passer des coordonn�es logiques
     * vers les coordonn�es pixels.  Il est garantie qu'aucune m�thode
     * sauf {@link #setTransform} ne modifieront cette transformation.
     */
    private final AffineTransform transform=new AffineTransform();

    /**
     * Derni�re coordonn�e <em>relatives</em> de la souris. Cette information est
     * exprim�e en coordonn�es logiques (d'apr�s la transformation affine inverse
     * de {@link #getTransform}). Les coordonn�es sont relative au coin
     * (<var>x</var>,<var>y</var>) du rectangle.
     */
    private transient double mouseDX, mouseDY;

    /**
     * Coordonn�es <code>x</code>, <code>y</code>, <code>width</code> et <code>height</code> d'une
     * bo�te qui englobe compl�tement {@link #rectangle}. Ces coordonn�es doivent �tre exprim�es
     * en <strong>pixels</strong>. Au besoin, la transformation affine {@link #getTransform} peut
     * �tre utilis�e pour passer des coordonn�es pixels aux coordonn�es logiques et inversement.
     */
    private transient int x, y, width, height;

    /**
     * Indique si le curseur de la souris
     * se trouve sur le rectangle.
     */
    private transient boolean mouseOverRect;

    /**
     * Point utilis� en interne pour certains calculs,
     * afin d'�viter de cr�er trop souvent plusieurs
     * objets {@link Point2D} temporaires.
     */
    private final transient Point2D.Double tmp=new Point2D.Double();

    /**
     * Indique si l'utilisateur est en train de faire glisser le rectangle.
     * Pour que ce champ prenne la valeur <code>true</code>, il faut que
     * la souris ait point� sur le rectangle au moment o� l'utilisateur a
     * appuy� sur le bouton de la souris.
     */
    private transient boolean isDraging;

    /**
     * Indique quels bords l'utilisateur est en train d'ajuster avec la souris. Ce
     * champ est souvent identique � {@link #adjustingSides}. Mais contrairement �
     * {@link #adjustingSides}, il d�signe un bord de la forme {@link #logicalShape}
     * et non un bord de la forme en pixels apparaissant � l'�cran. C'est diff�rent
     * par exemple si la transformation affine {@link #transform} contient une rotation
     * de 90�.
     */
    private transient int adjustingLogicalSides;

    /**
     * Indique quels bords l'utilisateur est en train d'ajuster avec
     * la souris. Les valeurs permises sont des combinaisons binaires
     * de {@link #NORTH}, {@link #SOUTH}, {@link #EAST} et {@link #WEST}.
     */
    private transient int adjustingSides;

    /**
     * Indique quels bords sont autoris�s � �tre ajust�es. Les valeurs
     * permises sont des combinaisons binaires de {@link #NORTH},
     * {@link #SOUTH}, {@link #EAST} et {@link #WEST}.
     */
    private int adjustableSides;

    /**
     * Indique si la forme g�om�trique
     * peut �tre d�plac�e.
     */
    private boolean moveable=true;

    /**
     * Lorsque la position du bord gauche ou droit du rectangle est
     * �dit�e manuellement, indique si la position du bord oppos�
     * doit �tre automatiquement ajust�. La valeur par d�faut est
     * <code>false</code>.
     */
    private boolean synchronizeX;

    /**
     * Lorsque la position du bord haut ou bas du rectangle est
     * �dit�e manuellement, indique si la position du bord oppos�
     * doit �tre automatiquement ajust�. La valeur par d�faut est
     * <code>false</code>.
     */
    private boolean synchronizeY;

    /** Bit d�signant le bord nord  */ private static final int NORTH = 1;
    /** Bit d�signant le bord sud   */ private static final int SOUTH = 2;
    /** Bit d�signant le bord est   */ private static final int EAST  = 4;
    /** Bit d�signant le bord ouest */ private static final int WEST  = 8;

    /**
     * Codes des curseurs corresondant � une
     * valeur de {@link adjustingSides} donn�e.
     */
    private static final int[] CURSORS=new int[]
    {
        Cursor.     MOVE_CURSOR, // 0000 =       |      |       |
        Cursor. N_RESIZE_CURSOR, // 0001 =       |      |       | NORTH
        Cursor. S_RESIZE_CURSOR, // 0010 =       |      | SOUTH |
        Cursor.  DEFAULT_CURSOR, // 0011 =       |      | SOUTH | NORTH
        Cursor. E_RESIZE_CURSOR, // 0100 =       | EAST |       |
        Cursor.NE_RESIZE_CURSOR, // 0101 =       | EAST |       | NORTH
        Cursor.SE_RESIZE_CURSOR, // 0110 =       | EAST | SOUTH |
        Cursor.  DEFAULT_CURSOR, // 0111 =       | EAST | SOUTH | NORTH
        Cursor. W_RESIZE_CURSOR, // 1000 =  WEST |      |       |
        Cursor.NW_RESIZE_CURSOR, // 1001 =  WEST |      |       | NORTH
        Cursor.SW_RESIZE_CURSOR  // 1010 =  WEST |      | SOUTH |
    };

    /**
     * Tableau servant � convertir les constantes <i>Swing</i> en
     * combinaisons des constantes {@link #NORTH}, {@link #SOUTH},
     * {@link #EAST} et {@link #WEST}. On ne peut pas utiliser
     * directement les constantes <i>Swing</i> parce qu'elles
     * ne correspondent malheureusement pas � des combinaisons
     * binaires des quatres coins cardinaux.
     */
    private static final int[] SWING_TO_CUSTOM=new int[]
    {
        SwingConstants.NORTH,      NORTH,
        SwingConstants.SOUTH,      SOUTH,
        SwingConstants.EAST,       EAST,
        SwingConstants.WEST,       WEST,
        SwingConstants.NORTH_EAST, NORTH|EAST,
        SwingConstants.SOUTH_EAST, SOUTH|EAST,
        SwingConstants.NORTH_WEST, NORTH|WEST,
        SwingConstants.SOUTH_WEST, SOUTH|WEST
    };

    /**
     * Liste des champs de texte qui repr�senteront
     * les coordonn�es des bords du rectangle.
     */
    private Control[] fields;

    /**
     * Construit un objet capable de bouger et redimmensionner
     * une forme rectangulaire en fonction des mouvements de
     * la souris. Le rectangle sera par d�faut situ�e � la
     * coordonn�e (0,0) et aura une largeur et hauteur nulles.
     */
    public MouseReshapeTracker()
    {this(new Rectangle2D.Double());}

    /**
     * Construit un objet capable de bouger et redimmensionner une
     * forme rectangulaire en fonction des mouvements de la souris.
     *
     * @param shape Forme g�om�trique rectangulaire. Il n'est pas obligatoire que cette forme
     *        soit un rectangle. Il pourrait s'agir par exemple d'un cercle.  Les coordonn�es
     *        de cette forme seront les coordonn�es initiales de la visi�re.   Il s'agit de
     *        coordonn�es logiques et non de pixels.  Notez que le constructeur retient une
     *        r�f�rence directe vers cette forme, sans faire de clone. En cons�quent, toute
     *        modification faite � la forme g�om�trique se r�percutera sur cet objet
     *        <code>MouseReshapeTracker</code> et vis-versa.
     */
    public MouseReshapeTracker(final RectangularShape shape)
    {
        this.logicalShape = shape;
        this.drawnShape   = shape;
        update();
    }

    /**
     * M�thode app�l�e automatiquement apr�s la lecture
     * de cet objet pour terminer la construction de
     * certains champs.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        drawnShape=logicalShape;
        update();
    }

    /**
     * Mes � jour les champs internes de
     * cet objet Les champs ajust�s seront:
     *
     * <ul>
     *   <li>{@link #drawnShape} pour le rectangle � dessiner.</li>
     *   <li>{@link #x}, {@link #y}, {@link #width} et {@link #height} pour les coordonn�es en pixels de {@link #drawnShape}.</li>
     * </ul>
     */
    private void update()
    {
        /*
         * Prend en compte les cas o� la transformation affine
         * contiendrait une rotation de 90� ou autre.
         */
        adjustingLogicalSides = inverseTransform(adjustingSides);
        /*
         * Obtient la forme g�om�trique � dessiner. Il s'agira normalement
         * de {@link #logicalShape}, sauf si celle-ci est si petite qu'on a
         * jug� pr�f�rable de cr�er une forme temporaire qui sera l�g�rement
         * plus grande.
         */
        tmp.x = logicalShape.getWidth();
        tmp.y = logicalShape.getHeight();
        transform.deltaTransform(tmp,tmp);
        if (Math.abs(tmp.x)<MIN_WIDTH || Math.abs(tmp.y)<MIN_HEIGHT)
        {
            if (Math.abs(tmp.x)<MIN_WIDTH ) tmp.x=(tmp.x<0) ? -MIN_WIDTH  : MIN_WIDTH;
            if (Math.abs(tmp.y)<MIN_HEIGHT) tmp.y=(tmp.y<0) ? -MIN_HEIGHT : MIN_HEIGHT;
            try
            {
                XAffineTransform.inverseDeltaTransform(transform, tmp, tmp);
                double x = logicalShape.getX();
                double y = logicalShape.getY();
                if ((adjustingLogicalSides & WEST)!=0)
                {
                    x += logicalShape.getWidth()-tmp.x;
                }
                if ((adjustingLogicalSides & NORTH)!=0)
                {
                    y += logicalShape.getHeight()-tmp.y;
                }
                if (drawnShape==logicalShape)
                {
                    drawnShape = (RectangularShape) logicalShape.clone();
                }
                drawnShape.setFrame(x, y, tmp.x, tmp.y);
            }
            catch (NoninvertibleTransformException exception)
            {
                drawnShape=logicalShape;
            }
        }
        else drawnShape=logicalShape;
        /*
         * NOTE: la condition 'drawnShape==logicalShape' indique qu'il n'a pas �t� n�cessaire
         *       de modifier la forme. La m�thode 'mouseDragged' utilisera cette information.
         *
         * Retient maintenant les coordonn�es en pixels de la nouvelle position du rectangle.
         */
        double xmin=Double.POSITIVE_INFINITY;
        double ymin=Double.POSITIVE_INFINITY;
        double xmax=Double.NEGATIVE_INFINITY;
        double ymax=Double.NEGATIVE_INFINITY;
        for (int i=0; i<4; i++)
        {
            tmp.x = (i&1)==0 ? drawnShape.getMinX() : drawnShape.getMaxX();
            tmp.y = (i&2)==0 ? drawnShape.getMinY() : drawnShape.getMaxY();
            transform.transform(tmp, tmp);
            if (tmp.x<xmin) xmin=tmp.x;
            if (tmp.x>xmax) xmax=tmp.x;
            if (tmp.y<ymin) ymin=tmp.y;
            if (tmp.y>ymax) ymax=tmp.y;
        }
        x      = (int) Math.floor(xmin)      -1;
        y      = (int) Math.floor(ymin)      -1;
        width  = (int) Math.ceil (xmax-xmin) +2;
        height = (int) Math.ceil (ymax-ymin) +2;
    }

    /**
     * Retourne la transformation de <code>adjusting</code>.
     * @param adjusting � transformer (g�n�ralement {@link #adjustingSides}).
     */
    private int inverseTransform(int adjusting)
    {
        switch (adjusting & (WEST|EAST))
        {
            case WEST: tmp.x=-1; break;
            case EAST: tmp.x=+1; break;
            default  : tmp.x= 0; break;
        }
        switch (adjusting & (NORTH|SOUTH))
        {
            case NORTH: tmp.y=-1; break;
            case SOUTH: tmp.y=+1; break;
            default   : tmp.y= 0; break;
        }
        try
        {
            XAffineTransform.inverseDeltaTransform(transform, tmp, tmp);
            final double normalize=0.25*XMath.hypot(tmp.x, tmp.y);
            tmp.x /= normalize;
            tmp.y /= normalize;
            adjusting=0;
            switch (XMath.sgn(Math.rint(tmp.x)))
            {
                case -1: adjusting |= WEST; break;
                case +1: adjusting |= EAST; break;
            }
            switch (XMath.sgn(Math.rint(tmp.y)))
            {
                case -1: adjusting |= NORTH; break;
                case +1: adjusting |= SOUTH; break;
            }
            return adjusting;
        }
        catch (NoninvertibleTransformException exception)
        {
            return adjusting;
        }
    }

    /**
     * D�clare la transformation affine servant � transformer les coordonn�es logiques en coordonn�es
     * pixels. Il s'agit de la transformation affine sp�cifi�e � {@link java.awt.Graphics2D#transform}
     * lors du dernier tra�age de <code>this</code>. Les informations contenues dans cette transformation
     * affine sont n�cessaires au fonctionnement de plusieurs m�thodes de cette classe. Il est de la
     * responsabilit� du programmeur de s'assurer que cette information soit toujours � jour. Par d�faut,
     * <code>MouseReshapeTracker</code> utilise une transformation identit�e.
     */
    public void setTransform(final AffineTransform newTransform)
    {
        if (!this.transform.equals(newTransform))
        {
            fireStateWillChange();
            this.transform.setTransform(newTransform);
            update();
            fireStateChanged();
        }
    }

    /**
     * Retourne la position et dimension du rectangle. Ces dimensions
     * peuvent �tre l�g�rement plus grande que celle que retournerait
     * {@link #getFrame} du fait que <code>getBounds2D()</code> retourne
     * les dimensions du rectangle visible � l'�cran, qui peut avoir une
     * certaine dimension minimale.
     */
    public Rectangle getBounds()
    {return drawnShape.getBounds();}

    /**
     * Retourne la position et dimension du rectangle. Ces dimensions
     * peuvent �tre l�g�rement plus grande que celle que retournerait
     * {@link #getFrame} du fait que <code>getBounds2D()</code> retourne
     * les dimensions du rectangle visible � l'�cran, qui peut avoir une
     * certaine dimension minimale.
     */
    public Rectangle2D getBounds2D()
    {return drawnShape.getBounds2D();}

    /**
     * Retourne la position et dimension du rectangle. Ces
     * informations seront exprim�es en coordonn�es logiques.
     *
     * @see #getCenterX
     * @see #getCenterY
     * @see #getMinX
     * @see #getMaxX
     * @see #getMinY
     * @see #getMaxY
     */
    public Rectangle2D getFrame()
    {return logicalShape.getFrame();}

    /**
     * D�finit une nouvelle position et dimension pour le rectangle. Les coordonn�es transmises
     * � cette m�thode doivent �tre des coordonn�es logiques plut�t que des pixels. Si la plage
     * des valeurs que peut couvrir le rectangle a �t� limit�e par un appel � {@link #setClip},
     * alors le rectangle sera d�plac� et au besoin redimensionn� pour la faire entrer dans la
     * r�gion permise.
     *
     * @return <code>true</code> si les coordonn�es du rectangle ont chang�es.
     *
     * @see #getFrame
     */
    public final boolean setFrame(final Rectangle2D frame)
    {return setFrame(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());}

    /**
     * D�finit une nouvelle position et dimension pour le rectangle. Les coordonn�es transmises
     * � cette m�thode doivent �tre des coordonn�es logiques plut�t que des pixels. Si la plage
     * des valeurs que peut couvrir le rectangle a �t� limit�e par un appel � {@link #setClip},
     * alors le rectangle sera d�plac� et au besoin redimensionn� pour la faire entrer dans la
     * r�gion permise.
     *
     * @return <code>true</code> si les coordonn�es du rectangle ont chang�es.
     *
     * @see #setX
     * @see #setY
     */
    public boolean setFrame(double x, double y, double width, double height)
    {
        final double oldX=logicalShape.getX();
        final double oldY=logicalShape.getY();
        final double oldW=logicalShape.getWidth();
        final double oldH=logicalShape.getHeight();
        if (x<xmin) x=xmin;
        if (y<ymin) y=ymin;
        if (x+width>xmax)
        {
            x=Math.max(xmin, xmax-width);
            width=xmax-x;
        }
        if (y+height>ymax)
        {
            y=Math.max(ymin, ymax-height);
            height=ymax-y;
        }
        fireStateWillChange();
        logicalShape.setFrame(x, y, width, height);
        if (oldX!=logicalShape.getX()     ||
            oldY!=logicalShape.getY()     ||
            oldW!=logicalShape.getWidth() ||
            oldH!=logicalShape.getHeight())
        {
            update();
            fireStateChanged();
            return true;
        }
        return false;
    }

    /**
     * D�finit la nouvelle plage de valeurs couverte par le rectangle selon l'axe
     * des <var>x</var>. Les valeurs couvertes le long de l'axe des <var>y</var>
     * ne seront pas chang�es. Les valeurs doivent �tre exprim�es en coordonn�es
     * logiques.
     *
     * @see #getMinX
     * @see #getMaxX
     * @see #getCenterX
     */
    public final void setX(final double min, final double max)
    {setFrame(Math.min(min,max), logicalShape.getY(), Math.abs(max-min), logicalShape.getHeight());}

    /**
     * D�finit la nouvelle plage de valeurs couverte par le rectangle selon l'axe
     * des <var>y</var>. Les valeurs couvertes le long de l'axe des <var>x</var>
     * ne seront pas chang�es. Les valeurs doivent �tre exprim�es en coordonn�es
     * logiques.
     *
     * @see #getMinY
     * @see #getMaxY
     * @see #getCenterY
     */
    public final void setY(final double min, final double max)
    {setFrame(logicalShape.getX(), Math.min(min,max), logicalShape.getWidth(), Math.abs(max-min));}

    /**
     * Retourne la coordonn�e <var>x</var> minimale du rectangle.
     * Il s'agira de la coordonn�e logique, et non de la coordonn�e
     * pixel.
     */
    public double getMinX()
    {return logicalShape.getMinX();}

    /**
     * Retourne la coordonn�e <var>y</var> minimale du rectangle.
     * Il s'agira de la coordonn�e logique, et non de la coordonn�e
     * pixel.
     */
    public double getMinY()
    {return logicalShape.getMinY();}

    /**
     * Retourne la coordonn�e <var>x</var> maximale du rectangle.
     * Il s'agira de la coordonn�e logique, et non de la coordonn�e
     * pixel.
     */
    public double getMaxX()
    {return logicalShape.getMaxX();}

    /**
     * Retourne la coordonn�e <var>y</var> maximale du rectangle.
     * Il s'agira de la coordonn�e logique, et non de la coordonn�e
     * pixel.
     */
    public double getMaxY()
    {return logicalShape.getMaxY();}

    /**
     * Retourne la largeur du rectangle. Cette largeur sera
     * exprim�e en coordonn�es logiques, et non en coordonn�es
     * pixels.
     */
    public double getWidth()
    {return logicalShape.getWidth();}

    /**
     * Retourne la hauteur du rectangle. Cette hauteur sera
     * exprim�e en coordonn�es logiques, et non en coordonn�es
     * pixels.
     */
    public double getHeight()
    {return logicalShape.getHeight();}

    /**
     * Retourne la coordonn�e <var>x</var> au centre du rectangle.
     * Il s'agira de la coordonn�e logique, et non de la coordonn�e
     * pixel.
     */
    public double getCenterX()
    {return logicalShape.getCenterX();}

    /**
     * Retourne la coordonn�e <var>y</var> au centre du rectangle.
     * Il s'agira de la coordonn�e logique, et non de la coordonn�e
     * pixel.
     */
    public double getCenterY()
    {return logicalShape.getCenterY();}

    /**
     * Indique si le rectangle est vide. Ce sera le
     * cas si sa largeur et/ou sa hauteur est nulle.
     */
    public boolean isEmpty()
    {return logicalShape.isEmpty();}

    /**
     * Indique si la forme rectangulaire contient le point sp�cifi�.
     * Ce point doit �tre exprim� en coordonn�es logiques.
     */
    public boolean contains(final Point2D point)
    {return logicalShape.contains(point);}

    /**
     * Indique si la forme rectangulaire contient le point sp�cifi�.
     * Ce point doit �tre exprim� en coordonn�es logiques.
     */
    public boolean contains(final double x, final double y)
    {return logicalShape.contains(x,y);}

    /**
     * Indique si la forme rectangulaire contient le rectangle sp�cifi�.
     * Ce rectangle doit �tre exprim� en coordonn�es logiques. Cette
     * m�thode peut retourne <code>false</code> de fa�on conservative,
     * comme l'autorise la sp�cification de {@link Shape}.
     */
    public boolean contains(final Rectangle2D rect)
    {return logicalShape.contains(rect);}

    /**
     * Indique si la forme rectangulaire contient le rectangle sp�cifi�.
     * Ce rectangle doit �tre exprim� en coordonn�es logiques. Cette
     * m�thode peut retourne <code>false</code> de fa�on conservative,
     * comme l'autorise la sp�cification de {@link Shape}.
     */
    public boolean contains(final double x, final double y, final double width, final double height)
    {return logicalShape.contains(x, y, width, height);}

    /**
     * Indique si la forme rectangulaire intercepte le rectangle sp�cifi�.
     * Ce rectangle doit �tre exprim� en coordonn�es logiques. Cette
     * m�thode peut retourne <code>true</code> de fa�on conservative,
     * comme l'autorise la sp�cification de {@link Shape}.
     */
    public boolean intersects(final Rectangle2D rect)
    {return drawnShape.intersects(rect);}

    /**
     * Indique si la forme rectangulaire intercepte le rectangle sp�cifi�.
     * Ce rectangle doit �tre exprim� en coordonn�es logiques. Cette
     * m�thode peut retourne <code>true</code> de fa�on conservative,
     * comme l'autorise la sp�cification de {@link Shape}.
     */
    public boolean intersects(final double x, final double y, final double width, final double height)
    {return drawnShape.intersects(x, y, width, height);}

    /**
     * Retourne un it�rateur balayant
     * la forme rectangulaire � dessiner.
     */
    public PathIterator getPathIterator(final AffineTransform transform)
    {return drawnShape.getPathIterator(transform);}

    /**
     * Retourne un it�rateur balayant
     * la forme rectangulaire � dessiner.
     */
    public PathIterator getPathIterator(final AffineTransform transform, final double flatness)
    {return drawnShape.getPathIterator(transform, flatness);}

    /**
     * Retourne les bornes entre lesquelles le rectangle peut se d�placer.
     * Ces limites sont sp�cifi�es en coordonn�es logiques.
     */
    public Rectangle2D getClip()
    {return new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);}

    /**
     * D�finit les bornes entre lesquelles le rectangle peut se d�placer.
     * Cette m�thode g�re correctement les infinit�s si le rectangle
     * sp�cifi� a bien red�finit ses m�thodes <code>getMaxX()</code>
     * et <code>getMaxY()</code>.
     *
     * @see #setClipMinMax
     */
    public final void setClip(final Rectangle2D rect)
    {setClipMinMax(rect.getMinX(), rect.getMaxX(), rect.getMinY(), rect.getMaxY());}

    /**
     * D�finit les bornes entre lesquelles le rectangle peut se d�placer. Cette m�thode se
     * contente d'appeller {@link #setClipMinMax setClipMinMax(...)} avec les param�tres
     * appropri�s. Elle est d�finie afin de r�duire les risques de confusion de la part des
     * programmeurs habitu�s aux conventions de <em>Java2D</em>. Si vous voulez sp�cifier
     * des valeurs infinies (pour �tendre les limites de la visi�re � toutes les valeurs
     * possibles sur certains axes), alors vous <u>devez</u> utiliser {@link #setClipMinMax
     * setClipMinMax(...)} plut�t que <code>setClip(...)</code>.
     */
    public final void setClip(final double x, final double y, final double width, final double height)
    {setClipMinMax(x, x+width, y, y+height);}

    /**
     * D�finit les bornes entre lesquelles le rectangle peut se d�placer. Notez que les arguments de
     * cette m�thode ne correspondent pas aux arguments habituels de {@link java.awt.geom.Rectangle2D}.
     * La convention de <em>Java2D</em> voulant que l'on sp�cifie un rectangle � l'aide d'un quadruplet
     * (<code>x</code>,<code>y</code>,<code>width</code>,<code>height</code>) est un mauvais choix dans
     * le contexte d'� peu pr�s toute les m�thodes de notre biblioth�que <code>net.seas</code>. En plus de
     * compliquer la plupart des calculs (pour s'en convaincre, il suffit de compter le nombre d'occurences
     * de l'expression <code>x+width</code> m�me dans les propres classes g�om�triques de <em>Java2D</em>),
     * elle est incapable de repr�senter correctement un rectangle dont une ou plusieurs coordonn�es s'�tendent
     * vers l'infinie. Une meilleure convention aurait �t� d'utiliser les valeurs minimales et maximales selon
     * <var>x</var> et <var>y</var>, ce que fait cette m�thode.
     * <br><br>
     * Les arguments de cette m�thodes d�finissent les valeurs minimales et maximales que peuvent prendre
     * les coordonn�es logiques du rectangle. Les valeurs {@link java.lang.Double#NEGATIVE_INFINITY} et
     * {@link java.lang.Double#POSITIVE_INFINITY} sont valides pour indiquer que la visi�re peut balayer
     * toutes les valeurs selon certains axes. La valeur {@link java.lang.Double#NaN} pour un argument
     * donn�e indique que l'on souhaite conserver l'ancienne valeur. Si la visi�re n'entre pas compl�tement
     * dans les nouvelles limites, elle sera d�plac�e et au besoin redimmensionn�e de fa�on a y entrer.
     */
    public void setClipMinMax(double xmin, double xmax, double ymin, double ymax)
    {
        if (xmin>xmax)
        {
            final double tmp=xmin;
            xmin=xmax; xmax=tmp;
        }
        if (ymin>ymax)
        {
            final double tmp=ymin;
            ymin=ymax; ymax=tmp;
        }
        if (!Double.isNaN(xmin)) this.xmin=xmin;
        if (!Double.isNaN(xmax)) this.xmax=xmax;
        if (!Double.isNaN(ymin)) this.ymin=ymin;
        if (!Double.isNaN(ymax)) this.ymax=ymax;
        setFrame(logicalShape.getX(), logicalShape.getY(), logicalShape.getWidth(), logicalShape.getHeight());
    }

    /**
     * M�thode appell�e automatiquement lorsqu'un changement du clip serait souhaitable.
     * Cette m�thode peut �tre appell�e par exemple lorsque l'utilisateur a �dit� manuellement
     * la position du rectangle dans un champ texte, et que la nouvelle position tombe en
     * dehors du clip actuel. Cette m�thode n'est <u>pas</u> oblig� d'accepter un changement
     * de clip. Elle peut ne rien faire, ce qui �quivaut � refuser tout changement. Elle peut
     * aussi accepter inconditionnellement n'importe quel changement en appellant toujours
     * {@link #setClipMinMax}. Enfin, elle peut prendre une solution mitoyenne en imposant
     * certaines conditions aux changements. L'impl�mentation par d�faut ne fait rien, ce
     * qui signifie qu'aucun changement automatique de clip ne sera autoris�.
     */
    protected void clipChangeRequested(double xmin, double xmax, double ymin, double ymax)
    {}

    /**
     * Indique si le rectangle peut �tre d�plac� avec la souris. Par
     * d�faut, il peut �tre d�plac� mais ne peut pas �tre redimensionn�.
     */
    public boolean isMoveable()
    {return moveable;}

    /**
     * Sp�cifie si le rectangle peut �tre d�plac� avec la souris. La valeur
     * <code>false</code> indique que le rectangle ne peut plus �tre d�plac�,
     * mais peut encore �tre redimensionn� si {@link #setAdjustable} a �t�
     * appel�e avec les param�tres appropri�s.
     */
    public void setMoveable(final boolean moveable)
    {this.moveable=moveable;}

    /**
     * Indique si la taille du rectangle peut �tre modifi� � partir du bord sp�cifi�.
     * Le bord sp�cifi� doit �tre une des constantes suivantes:
     *
     * <table border align=center cellpadding=8 bgcolor=floralwhite>
     * <tr><td>{@link SwingConstants#NORTH_WEST}</td><td>{@link SwingConstants#NORTH}</td><td>{@link SwingConstants#NORTH_EAST}</td></tr>
     * <tr><td>{@link SwingConstants#WEST      }</td><td>                            </td><td>{@link SwingConstants#EAST      }</td></tr>
     * <tr><td>{@link SwingConstants#SOUTH_WEST}</td><td>{@link SwingConstants#SOUTH}</td><td>{@link SwingConstants#SOUTH_EAST}</td></tr>
     * </table>
     *
     * Ces constantes d�signent le bord visible sur l'�cran. Par exemple <code>NORTH</code>
     * d�signe toujours le bord du haut sur l'�cran. Toutefois, �a peut correspondre � un
     * autre bord de la forme logique <code>this</code> d�pendemment de la transformation
     * affine qui avait �t� sp�cifi�e lors du dernier appel � {@link #setTransform}.  Par
     * exemple <code>AffineTransform.getScaleInstance(+1,-1)</code> a pour effet d'inverser
     * de faire appara�tre au "nord" les valeurs <var>y</var><sub>max</sub> plut�t que
     * <var>y</var><sub>min</sub>.
     */
    public boolean isAdjustable(int side)
    {
        side=convertSwingConstant(side);
        return (adjustableSides & side)==side;
    }

    /**
     * Sp�cifie si la taille du rectangle peut �tre modifi� � partir du bord sp�cifi�.
     * Le bord sp�cifi� doit �tre une des constantes suivantes:
     *
     * <table border align=center cellpadding=8 bgcolor=floralwhite>
     * <tr><td>{@link SwingConstants#NORTH_WEST}</td><td>{@link SwingConstants#NORTH}</td><td>{@link SwingConstants#NORTH_EAST}</td></tr>
     * <tr><td>{@link SwingConstants#WEST      }</td><td>                            </td><td>{@link SwingConstants#EAST      }</td></tr>
     * <tr><td>{@link SwingConstants#SOUTH_WEST}</td><td>{@link SwingConstants#SOUTH}</td><td>{@link SwingConstants#SOUTH_EAST}</td></tr>
     * </table>
     *
     * Ces constantes d�signent le bord visible sur l'�cran. Par exemple <code>NORTH</code>
     * d�signe toujours le bord du haut sur l'�cran. Toutefois, �a peut correspondre � un
     * autre bord de la forme logique <code>this</code> d�pendemment de la transformation
     * affine qui avait �t� sp�cifi�e lors du dernier appel � {@link #setTransform}.  Par
     * exemple <code>AffineTransform.getScaleInstance(+1,-1)</code> a pour effet d'inverser
     * de faire appara�tre au "nord" les valeurs <var>y</var><sub>max</sub> plut�t que
     * <var>y</var><sub>min</sub>.
     */
    public void setAdjustable(int side, final boolean adjustable)
    {
        side=convertSwingConstant(side);
        if (adjustable) adjustableSides |=  side;
        else            adjustableSides &= ~side;
    }

    /*
     * V�rifie � quel bord de la visi�re correspond la coordonn�e 'side' qui
     * a �t� sp�cifi�e. On ne peut malheureusement pas utiliser directement
     * les constantes de 'SwingConstants' parce qu'elles ne sont pas pr�vues
     * pour subir des combinaisons binaires.
     */
    private int convertSwingConstant(final int side)
    {
        for (int i=0; i<SWING_TO_CUSTOM.length; i+=2)
            if (SWING_TO_CUSTOM[i]==side)
                return SWING_TO_CUSTOM[i+1];
        throw new IllegalArgumentException(String.valueOf(side));
    }

    /**
     * M�thode appel�e automatiquement lors des d�placements de la souris.
     * L'impl�mentation par d�faut v�rifie si le curseur se trouve � l'int�rieur
     * du rectangle ou sur un de ces bords, et ajuste le symbole de la souris en
     * cons�quence.
     */
    public void mouseMoved(final MouseEvent event)
    {
        if (!isDraging)
        {
            final Component source=event.getComponent();
            if (source!=null)
            {
                int x=event.getX(); tmp.x=x;
                int y=event.getY(); tmp.y=y;
                final boolean mouseOverRect;
                try
                {
                    mouseOverRect=drawnShape.contains(transform.inverseTransform(tmp,tmp));
                }
                catch (NoninvertibleTransformException exception)
                {
                    // Ignore cette exception.
                    return;
                }
                final boolean mouseOverRectChanged = (mouseOverRect!=this.mouseOverRect);
                if (mouseOverRect)
                {
                    /*
                     * On n'utilise pas "adjustingLogicalSides" car on travail ici dans
                     * l'espace des coordonn�es pixels, et non des coordonn�es logiques.
                     */
                    final int old=adjustingSides;
                    adjustingSides=0;
                    if (Math.abs(x-=this.x)     <=RESIZE_POS) adjustingSides |= WEST;
                    if (Math.abs(y-=this.y)     <=RESIZE_POS) adjustingSides |= NORTH;
                    if (Math.abs(x- this.width) <=RESIZE_POS) adjustingSides |= EAST;
                    if (Math.abs(y- this.height)<=RESIZE_POS) adjustingSides |= SOUTH;

                    adjustingSides &= adjustableSides;
                    if (adjustingSides!=old || mouseOverRectChanged)
                    {
                        if (adjustingSides==0 && !moveable)
                        {
                            source.setCursor(null);
                        }
                        else
                        {
                            adjustingLogicalSides = inverseTransform(adjustingSides);
                            source.setCursor(Cursor.getPredefinedCursor(adjustingSides<CURSORS.length ?
                                                                        CURSORS[adjustingSides]       :
                                                                        Cursor.DEFAULT_CURSOR));
                        }
                    }
                    if (mouseOverRectChanged)
                    {
                        // Ajouter et retirer des 'listeners' marchait bien,  mais avait
                        // l'inconv�nient de changer l'ordre des 'listeners'. Ca causait
                        // probl�me lorsque cet ordre �tait important.

                        //source.addMouseListener(this);
                        this.mouseOverRect=mouseOverRect;
                    }
                }
                else
                {
                    if (mouseOverRectChanged)
                    {
                        adjustingSides=0;
                        source.setCursor(null);
                        //source.removeMouseListener(this);
                        this.mouseOverRect=mouseOverRect;
                    }
                }
            }
        }
    }

    /**
     * M�thode appel�e automatiquement lorsque l'utilisateur
     * a enfonc� le bouton de la souris � quelque part sur la
     * composante. L'impl�mentation par d�faut v�rifie si le
     * bouton a �t� enfonc� alors que le curseur de la souris
     * se trouvait � l'int�rieur du rectangle. Si oui, alors
     * cet objet suivra les glissements de la souris pour
     * d�placer ou redimensionner le rectangle.
     */
    public void mousePressed(final MouseEvent e)
    {
        if (!e.isConsumed() && (e.getModifiers() & MouseEvent.BUTTON1_MASK)!=0)
        {
            if (adjustingSides!=0 || moveable)
            {
                tmp.x=e.getX();
                tmp.y=e.getY();
                try
                {
                    if (drawnShape.contains(transform.inverseTransform(tmp,tmp)))
                    {
                        mouseDX = tmp.x-drawnShape.getX();
                        mouseDY = tmp.y-drawnShape.getY();
                        isDraging=true;
                        e.consume();
                    }
                }
                catch (NoninvertibleTransformException exception)
                {
                    // Pas besoin de g�rer cette exception.
                    // L'ignorer est correct.
                }
            }
        }
    }

    /**
     * M�thode appel�e automatiquement lors des glissements de la souris.
     * L'impl�mentation par d�faut applique sur le rectangle le d�placement
     * de la souris et pr�vient la composante d'o� provient l'�v�nement
     * qu'elle a besoin d'�tre redessin�e au moins en partie.
     */
    public void mouseDragged(final MouseEvent e)
    {
        if (isDraging)
        {
            final int adjustingLogicalSides=this.adjustingLogicalSides;
            final Component source=e.getComponent();
            if (source!=null) try
            {
                tmp.x = e.getX();
                tmp.y = e.getY();
                transform.inverseTransform(tmp,tmp);
                /*
                 * Calcule les coordonn�es (x0,y0) du coin du rectangle. Les coordonn�es
                 * (mouseDX, mouseDY) repr�sentent la position de la souris au moment o�
                 * le bouton a �t� enfonc�e et ne changent habituellement pas (sauf lors
                 * de certains ajustements).  En retranchant (mouseDX, mouseDY), on fait
                 * comme si l'utilisateur avait commenc� � faire glisser le rectangle
                 * exactement � partir du coin, alors qu'en fait il peut avoir cliqu�
                 * n'importe o�.
                 */
                double x0 = tmp.x-mouseDX;
                double y0 = tmp.y-mouseDY;
                double dx = drawnShape.getWidth();
                double dy = drawnShape.getHeight();
                final double oldWidth  = dx;
                final double oldHeight = dy;
                /*
                 * Effectue des ajustements pour les cas o�, au lieu de faire glisser
                 * le rectangle, l'utilisateur est en train de le redimensioner.
                 */
                switch (adjustingLogicalSides & (EAST|WEST))
                {
                    case WEST:
                    {
                        if (x0<xmin) x0=xmin;
                        dx += drawnShape.getX()-x0;
                        if (!(dx>0))
                        {
                            dx=drawnShape.getWidth();
                            x0=drawnShape.getX();
                        }
                        break;
                    }
                    case EAST:
                    {
                        dx += x0 - (x0=drawnShape.getX());
                        final double limit=xmax-x0;
                        if (dx>limit) dx=limit;
                        if (!(dx>0))
                        {
                            dx=drawnShape.getWidth();
                            x0=drawnShape.getX();
                        }
                        break;
                    }
                }
                switch (adjustingLogicalSides & (NORTH|SOUTH))
                {
                    case NORTH:
                    {
                        if (y0<ymin) y0=ymin;
                        dy += drawnShape.getY()-y0;
                        if (!(dy>0))
                        {
                            dy=drawnShape.getHeight();
                            y0=drawnShape.getY();
                        }
                        break;
                    }
                    case SOUTH:
                    {
                        dy += y0 - (y0=drawnShape.getY());
                        final double limit=ymax-y0;
                        if (dy>limit) dy=limit;
                        if (!(dy>0))
                        {
                            dy=drawnShape.getHeight();
                            y0=drawnShape.getY();
                        }
                        break;
                    }
                }
                /*
                 * Les coordonn�es (x0, y0, dx, dy) donne maintenant la nouvelle position
                 * et dimension du rectangle. Mais avant de faire le changement,  v�rifie
                 * si un seul bord �tait en cours d'ajustement. Si oui, on annule les
                 * changements selon l'autre bord (sinon, l'utilisateur pourrait d�placer
                 * verticalement le rectangle en m�me temps qu'il ajuste son bord droit ou
                 * gauche, ce qui n'est pas tr�s pratique...).
                 */
                if ((adjustingLogicalSides & (NORTH|SOUTH))!=0 && (adjustingLogicalSides & (EAST|WEST))==0)
                {
                    x0 = drawnShape.getX();
                    dx = drawnShape.getWidth();
                }
                if ((adjustingLogicalSides & (NORTH|SOUTH))==0 && (adjustingLogicalSides & (EAST|WEST))!=0)
                {
                    y0 = drawnShape.getY();
                    dy = drawnShape.getHeight();
                }
                /*
                 * Modifie les coordonn�es du rectangle et signale
                 * que la composante a besoin d'�tre redessin�e.
                 * Note: 'repaint' doit �tre appel�e avant et apr�s
                 *        'setFrame' parce que les coordonn�es changent.
                 */
                source.repaint(x, y, width, height);
                try
                {
                    setFrame(x0, y0, dx, dy);
                }
                catch (RuntimeException exception)
                {
                    exception.printStackTrace();
                }
                source.repaint(x, y, width, height);
                /*
                 * Ajustement pour
                 * les cas sp�ciaux.
                 */
                if ((adjustingLogicalSides & EAST )!=0) mouseDX += (drawnShape.getWidth() -oldWidth);
                if ((adjustingLogicalSides & SOUTH)!=0) mouseDY += (drawnShape.getHeight()-oldHeight);
            }
            catch (NoninvertibleTransformException exception)
            {
                // Ignore.
            }
        }
    }

    /**
     * M�thode appel�e automatiquement lorsque l'utilisateur a rel�ch� le bouton de la souris.
     * L'impl�mentation par d�faut appelle {@link #stateChanged} avec l'argument <code>false</code>,
     * afin d'informer les classes d�riv�es que les changements sont termin�s.
     */
    public void mouseReleased(final MouseEvent event)
    {
        if (isDraging && (event.getModifiers() & MouseEvent.BUTTON1_MASK)!=0)
        {
            isDraging=false;
            final Component source=event.getComponent();
            try
            {
                tmp.x=event.getX();
                tmp.y=event.getY();
                mouseOverRect=drawnShape.contains(transform.inverseTransform(tmp,tmp));
                if (!mouseOverRect && source!=null) source.setCursor(null);
                event.consume();
            }
            catch (NoninvertibleTransformException exception)
            {
                // Ignore cette exception.
            }
            try
            {
                // Il faut que 'isDraging=false'.
                fireStateChanged();
            }
            catch (RuntimeException exception)
            {
                ExceptionMonitor.show(source, exception);
            }
        }
    }

    /**
     * M�thode appel�e automatiquement <strong>avant</strong> que la position ou la dimension
     * de la visi�re n'ait chang�e.  L'appel de <code>stateWillChange</code> est g�n�ralement
     * suivit d'un appel de {@link #stateChanged}, <u>sauf</u> si le changement pr�vu n'a
     * finalement pas eu lieu. Les classes d�riv�es peuvent red�finir cette m�thode pour prendre
     * les actions n�cessaires lorsqu'un changement est sur le point d'�tre effectu�. Elles ne
     * devraient toutefois appeler aucune m�thode qui risque de modifier l'�tat de cet objet.
     * L'impl�mentation par d�faut ne fait rien.
     *
     * @param isAdjusting <code>true</code> si l'utilisateur
     *        est encore en train de modifier la position de
     *        la visi�re, <code>false</code> s'il a relach�
     *        le bouton de la souris.
     */
    protected void stateWillChange(final boolean isAdjusting)
    {}

    /**
     * M�thode appel�e automatiquement <strong>apr�s</strong> que la position ou la dimension de
     * la visi�re ait chang�e. L'appel de <code>stateChanged</code> a obligatoirement �t� pr�c�d�
     * d'un appel � {@link #stateWillChange}. Les classes d�riv�es peuvent red�finir cette m�thode
     * pour prendre les actions n�cessaires lorsqu'un changement vient d'�tre effectu�. Elles ne
     * devraient toutefois appeler aucune m�thode qui risque de modifier l'�tat de cet objet.
     * L'impl�mentation par d�faut ne fait rien.
     *
     * @param isAdjusting <code>true</code> si l'utilisateur
     *        est encore en train de modifier la position de
     *        la visi�re, <code>false</code> s'il a relach�
     *        le bouton de la souris.
     */
    protected void stateChanged(final boolean isAdjusting)
    {}

    /**
     * M�thode appel�e automatiquement avant que la
     * position ou la dimension de la visi�re a chang�e.
     */
    private void fireStateWillChange()
    {stateWillChange(isDraging);}

    /**
     * M�thode appel�e automatiquement apr�s que la
     * position ou la dimension de la visi�re a chang�e.
     */
    private void fireStateChanged()
    {
        updateEditors();
        stateChanged(isDraging);
    }

    /**
     * Remet � jour le texte des �diteurs. Chaque �diteur ajout�s
     * par la m�thode {@link #addEditor addEditor(...)} formatera
     * de nouveau son texte.  Cette m�thode peut �tre appel�e par
     * exemple apr�s avoir chang� le format utilis� par les �diteurs.
     * Il n'est pas n�cessaire d'appeler cette m�thode � chaque fois
     * que la souris bouge; c'est fait automatiquement.
     */
    public void updateEditors()
    {
        if (fields!=null)
            for (int i=0; i<fields.length; i++)
                fields[i].updateText();
    }

    /**
     * Ajoute un �diteur dans lequel l'utilisateur pourra sp�cifier
     * explicitement les coordonn�es d'un des bords du rectangle.
     * Chaque fois que l'utilisateur fait glisser le rectangle, le
     * texte apparaissant dans cet �diteur sera automatiquement mis
     * � jour. Si l'utilisateur entre explicitement une nouvelle
     * valeur dans cet �diteur, la position du rectangle sera ajust�e.
     *
     * @param format    Format � utiliser pour �crire et interpr�ter les
     *                  valeurs dans l'�diteur.
     * @param side      Bord du rectangle dont l'�diteur contr�le les coordonn�es.
     *                  Il devrait s'agir d'une des constantes suivante:
     *
     * <table border align=center cellpadding=8 bgcolor=floralwhite>
     * <tr><td>{@link SwingConstants#NORTH_WEST}</td><td>{@link SwingConstants#NORTH}</td><td>{@link SwingConstants#NORTH_EAST}</td></tr>
     * <tr><td>{@link SwingConstants#WEST      }</td><td>                            </td><td>{@link SwingConstants#EAST      }</td></tr>
     * <tr><td>{@link SwingConstants#SOUTH_WEST}</td><td>{@link SwingConstants#SOUTH}</td><td>{@link SwingConstants#SOUTH_EAST}</td></tr>
     * </table>
     *
     *                  Ces constantes d�signent le bord visible sur l'�cran. Par exemple <code>NORTH</code>
     *                  d�signe toujours le bord du haut sur l'�cran. Toutefois, �a peut correspondre � un
     *                  autre bord de la forme logique <code>this</code> d�pendemment de la transformation
     *                  affine qui avait �t� sp�cifi�e lors du dernier appel � {@link #setTransform}.  Par
     *                  exemple <code>AffineTransform.getScaleInstance(+1,-1)</code> a pour effet d'inverser
     *                  de faire appara�tre au "nord" les valeurs <var>y</var><sub>max</sub> plut�t que
     *                  <var>y</var><sub>min</sub>.
     *
     * @param toRepaint Composante � redessiner apr�s qu'un champ ait �t�
     *                  �dit�, ou <code>null</code> s'il n'y en a pas.
     *
     * @return       Un �diteur dans laquelle l'utilisateur pourra sp�cifier
     *               la position d'un des bords de la forme g�om�trique.
     * @throws       IllegalArgumentException si <code>side</code> n'�tait pas
     *               un des codes reconnus ou si <code>field</code> �tait d�j�
     *               enregistr�.
     */
    public synchronized JTextComponent addEditor(final Format format, final int side, Component toRepaint) throws IllegalArgumentException
    {
        final JFormattedTextField field = new JFormattedTextField(format);
        /**
         * "9" est la largeur par d�faut des champs de texte. Ces largeurs sont
         * exprim�es en nombre de colonnes. <i>Swing</i> ne semble pas mesurer
         * ces largeurs tr�s pr�cisement; il semble en metre plus que ce qu'on
         * lui demande. Pour cette raison, on sp�cifie une largeur plus �troite.
         */
        field.setColumns(5);
        field.setHorizontalAlignment(JTextField.RIGHT);
        Insets insets=field.getMargin();
        insets.right += 2;
        field.setMargin(insets);
        /*
         * Ajoute l'�diteur � la liste des �diteurs � contr�ler. Augmenter � chaque fois
         * la longueur du tableau 'fields' n'est pas la strat�gie la plus efficace, mais
         * elle suffira puisqu'il est peu probable qu'on ajoutera plus de 4 �diteurs.
         */
        final Control control=new Control(field, (format instanceof DateFormat), convertSwingConstant(side), toRepaint);
        if (fields==null) fields=new Control[1];
        else fields = XArray.resize(fields, fields.length+1);
        fields[fields.length-1]=control;
        return field;
    }

    /**
     * Retire un �diteur de la liste de ceux qui
     * affichait les coordonn�es de la visi�re.
     *
     * @param field �diteur � retirer.
     */
    public synchronized void removeEditor(final JTextComponent field)
    {
        if (fields!=null)
        {
            for (int i=0; i<fields.length; i++)
            {
                if (fields[i].field==field)
                {
                    fields = XArray.remove(fields, i, 1);
                    /*
                     * En principe, il n'y aura pas d'autres objets �
                     * retirer du tableau. Mais on laisse tout de m�me
                     * la boucle se poursuivre au cas o�...
                     */
                }
            }
            if (fields.length==0) fields=null;
        }       
    }

    /**
     * Lorsque la position d'un des bords du rectangle est �dit�e manuellement,
     * sp�cifie si le bord oppos� doit aussi �tre ajust�. Par d�faut, les bords
     * ne sont pas synchronis�s.
     *
     * @param axis {@link SwingConstants#HORIZONTAL} pour changer
     *             la synchronisation des bords gauche et droit, ou
     *             {@link SwingConstants#VERTICAL} pour changer la
     *             synchronisation des bords haut et bas.
     * @param state <code>true</code> pour synchroniser les bords, ou
     *              <code>false</code> pour les d�synchroniser.
     * @throws IllegalArgumentException si <code>axis</code>
     *         n'est pas un des codes valides.
     */
    public void setEditorsSynchronized(final int axis, final boolean state) throws IllegalArgumentException
    {
        switch (axis)
        {
            case SwingConstants.HORIZONTAL: synchronizeX=state; break;
            case SwingConstants.VERTICAL:   synchronizeY=state; break;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * Lorsque la position d'un des bords du rectangle est �dit�e manuellement,
     * indique si le bord oppos� sera aussi automatiquement ajust�. Par d�faut,
     * les bords ne sont pas synchronis�s.
     *
     * @param axis {@link SwingConstants#HORIZONTAL} pour interroger
     *             la synchronisation des bords gauche et droit, ou
     *             {@link SwingConstants#VERTICAL} pour interroger la
     *             synchronisation des bords haut et bas.
     * @return <code>true</code> si les bords sp�cifi�s sont
     *         synchronis�s, ou <code>false</code> sinon.
     * @throws IllegalArgumentException si <code>axis</code>
     *         n'est pas un des codes valides.
     */
    public boolean isEditorsSynchronized(final int axis) throws IllegalArgumentException
    {
        switch (axis)
        {
            case SwingConstants.HORIZONTAL: return synchronizeX;
            case SwingConstants.VERTICAL:   return synchronizeY;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * Retourne une cha�ne de caract�re
     * repr�sentant cet objet.
     */
    public String toString()
    {return Utilities.getShortClassName(this)+'['+Utilities.getShortClassName(logicalShape)+']';}

    /**
     * Synchronise un des bords du rectangle avec un champ de texte.  Chaque fois que
     * la visi�re bouge, le texte sera mis � jour. Si c'est au contraire le texte qui
     * est �dit� manuellement, la visi�re sera repositionn�e en cons�quence.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Control implements PropertyChangeListener
    {
        /**
         * Champ de texte repr�sentant la coordonn�e
         * d'un des bords de la visi�re.
         */
        public final JFormattedTextField field;

        /**
         * <code>true</code> si le champ {@link #field} formatte
         * des dates, ou <code>false</code> s'il formatte des nombres.
         */
        private final boolean isDate;

        /**
         * C�t� du rectangle � contr�ler. Ce champ d�signe le bord visible sur l'�cran. Par
         * exemple <code>NORTH</code> d�signe toujours le bord du haut sur l'�cran. Toutefois,
         * �a peut correspondre � un autre bord de la forme logique {@link MouseReshapeTracker}
         * d�pendemment de la transformation affine qui avait �t� sp�cifi�e lors du dernier appel
         * � {@link MouseReshapeTracker#setTransform}. Par exemple <code>AffineTransform.getScaleInstance(+1,-1)</code>
         * a pour effet d'inverser de faire appara�tre au "nord" les valeurs <var>y</var><sub>max</sub>
         * plut�t que <var>y</var><sub>min</sub>.
         */
        private final int side;

        /**
         * Composante � redessiner apr�s que le champ ait
         * �t� �dit�, ou <code>null</code> s'il n'y en a pas.
         */
        private final Component toRepaint;

        /**
         * Construit un objet qui contr�lera
         * un des bords du rectangle.
         *
         * @param field  Champ qui contiendra la coordonn�e du bord du rectangle.
         * @param isDate <code>true</code> si le champ {@link #field} formatte
         *        des dates, ou <code>false</code> s'il formatte des nombres.
         * @param side Bord du rectangle � contr�ler. Cet argument d�signe le bord visible sur l'�cran. Par
         *        exemple <code>NORTH</code> d�signe toujours le bord du haut sur l'�cran. Toutefois, �a peut
         *        correspondre � un autre bord de la forme logique {@link MouseReshapeTracker} d�pendemment de
         *        la transformation affine qui avait �t� sp�cifi�e lors du dernier appel � {@link MouseReshapeTracker#setTransform}.
         *        Par exemple <code>AffineTransform.getScaleInstance(+1,-1)</code> a pour effet d'inverser de faire
         *        appara�tre au "nord" les valeurs <var>y</var><sub>max</sub> plut�t que <var>y</var><sub>min</sub>.
         * @param toRepaint Composante a redessiner apr�s que
         *        le champ ait �t� �dit�, ou <code>null</code>
         *        s'il n'y en a pas.
         */
        public Control(final JFormattedTextField field, final boolean isDate, final int side, final Component toRepaint)
        {
            this.field     = field;
            this.isDate    = isDate;
            this.side      = side;
            this.toRepaint = toRepaint;
            updateText(field);
            field.addPropertyChangeListener("value", this);
        }

        /**
         * M�thode appel�e automatiquement chaque
         * fois que change la valeur dans l'�diteur.
         */
        public void propertyChange(final PropertyChangeEvent event)
        {
            final Object source=event.getSource();
            if (source instanceof JFormattedTextField)
            {
                final JFormattedTextField field = (JFormattedTextField) source;
                final Object value=field.getValue();
                if (value!=null)
                {
                    final double v=(value instanceof Date)       ?
                                   ((Date) value).getTime()      :
                                   ((Number) value).doubleValue();
                    if (!Double.isNaN(v))
                    {
                        /*
                         * Obtient les nouvelles coordonn�es du rectangle,
                         * en prenant en compte les coordonn�es chang�es par
                         * l'utilisateur ainsi que les anciennes coordonn�es
                         * qui n'ont pas chang�.
                         */
                        final int side = inverseTransform(this.side);
                        double Vxmin=(side &  WEST)==0 ? logicalShape.getMinX() : v;
                        double Vxmax=(side &  EAST)==0 ? logicalShape.getMaxX() : v;
                        double Vymin=(side & NORTH)==0 ? logicalShape.getMinY() : v;
                        double Vymax=(side & SOUTH)==0 ? logicalShape.getMaxY() : v;
                        if (synchronizeX || Vxmin>Vxmax)
                        {
                            final double dx=logicalShape.getWidth();
                            if ((side & WEST)!=0) Vxmax=Vxmin+dx;
                            if ((side & EAST)!=0) Vxmin=Vxmax-dx;
                        }
                        if (synchronizeY || Vymin>Vymax)
                        {
                            final double dy=logicalShape.getHeight();
                            if ((side & NORTH)!=0) Vymax=Vymin+dy;
                            if ((side & SOUTH)!=0) Vymin=Vymax-dy;
                        }
                        /*
                         * V�rifie si les nouvelles coordonn�es n�cessitent un
                         * ajustement du clip. Si oui, on demandera � la
                         * m�thode 'clipChangeRequested' de faire le changement.
                         * Cette m�thode 'clipChangeRequested' n'est pas oblig�
                         * d'accepter le changement. Le reste du code sera correct
                         * m�me si le clip n'a pas chang� (dans ce cas, la position
                         * du rectangle sera encore ajust�e par 'setFrame').
                         */
                        if (Vxmin<xmin)
                        {
                            final double dx=Math.max(xmax-xmin, MINSIZE_RATIO*(Vxmax-Vxmin));
                            final double margin=Vxmax+dx*((MINSIZE_RATIO-1)*0.5);
                            clipChangeRequested(margin-dx, margin, ymin, ymax);
                        }
                        else if (Vxmax>xmax)
                        {
                            final double dx=Math.max(xmax-xmin, MINSIZE_RATIO*(Vxmax-Vxmin));
                            final double margin=Vxmin-dx*((MINSIZE_RATIO-1)*0.5);
                            clipChangeRequested(margin, margin+dx, ymin, ymax);
                        }
                        if (Vymin<ymin)
                        {
                            final double dy=Math.max(ymax-ymin, MINSIZE_RATIO*(Vymax-Vymin));
                            final double margin=Vymax+dy*((MINSIZE_RATIO-1)*0.5);
                            clipChangeRequested(xmin, xmax, margin-dy, margin);
                        }
                        else if (Vymax>ymax)
                        {
                            final double dy=Math.max(ymax-ymin, MINSIZE_RATIO*(Vymax-Vymin));
                            final double margin=Vymin-dy*((MINSIZE_RATIO-1)*0.5);
                            clipChangeRequested(xmin, xmax, margin, margin+dy);
                        }
                        /*
                         * Proc�de au repositionnement du rectangle
                         * en fonction des nouvelles coordonn�es.
                         */
                        if (setFrame(Vxmin, Vymin, Vxmax-Vxmin, Vymax-Vymin))
                        {
                            if (toRepaint!=null) toRepaint.repaint();
                        }
                    }
                }
                updateText(field);
            }
        }

        /**
         * Appel�e chaque fois que la position du glissoir est ajust�e.
         * Cette m�thode ajustera la valeur affich�e dans le champ de
         * texte en fonction de la position du glissoir.
         */
        private void updateText(final JFormattedTextField field)
        {
            String text;
            if (!logicalShape.isEmpty() ||
                ((text=field.getText())!=null && text.trim().length()!=0))
            {
                double value;
                switch (inverseTransform(side))
                {
                    case NORTH: value=logicalShape.getMinY(); break;
                    case SOUTH: value=logicalShape.getMaxY(); break;
                    case  WEST: value=logicalShape.getMinX(); break;
                    case  EAST: value=logicalShape.getMaxX(); break;
                    default   : return;
                }
                field.setValue((isDate) ? (Object)new Date(Math.round(value)) : (Object)new Double(value));
            }
        }

        /**
         * Met � jour le texte apparaissant dans {@link #field}
         * en fonction de la position actuelle du rectangle.
         */
        public void updateText()
        {updateText(field);}
    }
}
