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
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.Projection;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CoordinateSystemFactory;
import org.geotools.cs.ProjectedCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.MathTransform;
import org.geotools.resources.CTSUtilities;

// Geometry and graphics
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;
import java.awt.geom.NoninvertibleTransformException;
import org.geotools.resources.XRectangle2D;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import net.seas.util.WeakHashSet;

// Weak references
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

// Input/Output
import java.io.Writer;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

// Formatting
import java.util.Locale;
import java.text.NumberFormat;
import java.text.FieldPosition;

// Miscellaneous
import net.seas.util.XArray;
import net.seas.util.Console;
import net.seas.util.Statistics;
import org.geotools.resources.XMath;
import org.geotools.resources.Utilities;
import net.seas.resources.ResourceKeys;
import net.seas.resources.Resources;


/**
 * A polyline or a polygon. A polyline is built from straight lines with no discontinuity.
 * A polygon is a closed polyline (i.e. {@link #isClosed} returns <code>true</code>).
 * Polygons way have two interior states: interior may be an elevation (for example
 * the polygon may be an island in the middle of a sea), or a depression (for example
 * a lake in the middle of a continent).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Polygon extends Contour
{
    /**
     * Num�ro de version pour compatibilit� avec des
     * bathym�tries enregistr�es sous d'anciennes versions.
     */
    private static final long serialVersionUID = -2146898555113075365L;

    /**
     * Projection � utiliser pour les calculs qui
     * exigent un syst�me de coordonn�es cart�sien.
     */
    private static final String CARTESIAN = "Stereographic";

    /**
     * Cache vers des objets d�j� cr��s. Cette cache utilise des r�f�rences faibles pour ne
     * retenir les objets que s'ils sont d�j� utilis�s ailleurs dans la machine virtuelle.
     * <strong>Tous les objets plac�s dans cette cache devraient �tre immutables.</strong>
     */
    private static final WeakHashSet<AffineTransform> pool=new WeakHashSet<AffineTransform>();

    /**
     * Transformation affine identit�. Cette transformation affine
     * sera partag�e par plusieurs objets {@link Polygon} et ne doit
     * pas �tre modifi�.
     */
    private static final AffineTransform IDENTITY = new AffineTransform();

    /**
     * Nombre approximatif de pixels entre deux points de la
     * carte qui seront trac�s. Ce nombre sert � d�terminer
     * la r�solution � utiliser pour tracer une carte.
     */
    private static final int RESOLUTION_PIXELS = 4;

    /**
     * Constant indicating that the polygon
     * defined by this object represents an
     * elevation. For the 0 meter isobath,
     * an elevation may be an island in a sea.
     *
     * @see #DEPRESSION
     * @see #getInteriorSign
     */
    public static final int ELEVATION = +1;

    /**
     * Constant indicating that the polygon
     * defined by this object represents a
     * depression. For the 0 meter isobath,
     * a depression may be a lake in a continent.
     *
     * @see #ELEVATION
     * @see #getInteriorSign
     */
    public static final int DEPRESSION = -1;

    /**
     * Constant indicating that the polyline
     * defined by this object represents neither
     * a lake or an island. Such a polyline may
     * not be closed.
     *
     * @see #DEPRESSION
     * @see #ELEVATION
     * @see #getInteriorSign
     */
    public static final int UNKNOW = 0;

    /**
     * Un des maillons de la cha�ne de segments,  ou
     * <code>null</code> s'il n'y a aucune donn�e de
     * m�moris�e.
     */
    private Segment data;

    /**
     * Transformation permettant de passer du syst�me de coordonn�es des points <code>data</code>
     * vers le syst�me de coordonn�es de ce polyligne. {@link CoordinateTransformation#getSourceCS}
     * doit obligatoirement �tre le syst�me de coordonn�es de <code>data</code>, tandis que
     * {@link CoordinateTransformation#getTargetCS} doit �tre le syst�me de coordonn�es du polyligne.
     * Lorsque ce polyligne utilise le m�me syst�me de coordonn�es que <code>data</code> (ce qui
     * est le cas la plupart du temps), alors ce champ contiendra une transformation identit�.
     * Ce champ peut �tre nul si le syst�me de coordonn�es de <code>data</code> n'est pas connu.
     */
    private CoordinateTransformation coordinateTransform;

    /**
     * Rectangle englobant compl�tement tous les points de <code>data</code>. Ce
     * rectangle est une information tr�s utile pour rep�rer plus rapidement les
     * traits qui n'ont pas besoin d'�tre redessin�s (par exemple sous l'effet d'un zoom).
     * <strong>Le rectangle {@link Rectangle2D} r�f�renc� par ce champ ne doit jamais �tre
     * modifi�</strong>, car il peut �tre partag� par plusieurs objets {@link Polygon}.
     */
    private Rectangle2D dataBounds;

    /**
     * Rectangle englobant compl�tement les coordonn�es projet�es de ce polyligne.
     * Ce champs est utilis� comme une cache pour la m�thode {@link #getBounds2D()}
     * afin de la rendre plus rapide.
     *
     * <strong>Le rectangle {@link Rectangle2D} r�f�renc� par ce champ ne doit jamais �tre
     * modifi�</strong>, car il peut �tre partag� par plusieurs objets {@link Polygon}.
     */
    private Rectangle2D bounds;

    /**
     * R�solution moyenne du polyligne. Ce champ contient la distance moyenne entre
     * deux points du polyligne,  ou 0 ou {@link Float#NaN} si cette r�solution n'a
     * pas encore �t� calcul�e.
     */
    private float resolution;

    /**
     * Indique si cette forme a �t� ferm�e. Si le polygone a �t� ferm�, alors ce
     * champ aura la valeur {@link #ELEVATION} ou {@link #DEPRESSION}. Sinon, il
     * aura la valeur {@link #UNKNOW}.
     */
    private byte interiorSign = (byte)UNKNOW;

    /**
     * D�cimation � appliquer au moment du tra�age du polyligne. La valeur 1 signifie
     * qu'aucune d�cimation n'est faite; la valeur 2 signifie qu'on ne tracera qu'un
     * point sur 2, etc. Cette information est utilis�e uniquement par les objets
     * {@link PathIterator}. Cette valeur doit �tre sup�rieure ou �gale � 1.
     */
    private byte drawingDecimation = 1;

    /**
     * R�f�rence molle vers un tableau <code>float[]</code>. Ce tableau est utilis�
     * pour conserver en m�moire des points qui ont d�j� �t� projet�s ou transform�s.
     */
    private transient Cache cache;

    /**
     * R�f�rence molle vers un tableau <code>float[]</code>
     * de coordonn�es (<var>x</var>,<var>y</var>).
     */
    private static final class Cache extends SoftReference
    {
        /**
         * Nombre d'objets {@link PathIterator} qui utilisent le tableau de points de la cache
         * {@link #cache}. Ce nombre sera incr�ment� � chaque appel de {@link #getDrawingArray}
         * et d�cr�ment� par {@link #releaseDrawingArray}.
         */
        public short lockCount;

        /**
         * Transformation affine qui avait �t� utilis�e pour
         * transformer les donn�es de {@link #cache}.
         */
        public AffineTransform transform = IDENTITY;

        /**
         * Construit une r�f�rence molle vers le tableau sp�cifi�.
         */
        public Cache(final float[] array)
        {super(array);}
    }

    /**
     * Construit un polyligne initialement vide.
     */
    private Polygon(final CoordinateTransformation coordinateTransform)
    {
        this.coordinateTransform = coordinateTransform;
        CoordinateSystem cs;
        if ((cs=coordinateTransform.getSourceCS()).getDimension() != 2 ||
            (cs=coordinateTransform.getTargetCS()).getDimension() != 2)
        {
            // TODO
            throw new IllegalArgumentException(String.valueOf(cs));
        }
    }

    /**
     * Construct an empty polyline.
     * Use {@link #append} to add points.
     *
     * @param coordinateSystem The coordinate system to use for all
     *        points in this polygon, or <code>null</code> if unknow.
     */
    public Polygon(final CoordinateSystem coordinateSystem)
    {this(getIdentityTransform(CTSUtilities.getCoordinateSystem2D(coordinateSystem)));}

    /**
     * Construct a new polyline with the same data than the specified
     * polyline. The new polyline will have a copy semantic. However,
     * implementation try to share as much internal data as possible
     * in order to reduce memory footprint.
     */
    public Polygon(final Polygon polygon)
    {
        super(polygon);
        data                = Segment.clone(polygon.data);
        coordinateTransform = polygon.coordinateTransform;
        dataBounds          = polygon.dataBounds;
        bounds              = polygon.bounds;
        resolution          = polygon.resolution;
        interiorSign        = polygon.interiorSign;
    }

    /**
     * Construct a closed polygon with the specified rectangle.
     * The new polygon will be empty if the rectangle was empty
     * of contains <code>NaN</code> values.
     *
     * @param rectangle Rectangle to copy in the new polygon.
     * @param coordinateSystem The rectangle's coordinate system,
     *        or <code>null</code> if unknow.
     */
    public Polygon(final Rectangle2D rectangle, final CoordinateSystem coordinateSystem)
    {
        this(coordinateSystem);
        if (!rectangle.isEmpty())
        {
            final float xmin = (float)rectangle.getMinX();
            final float ymin = (float)rectangle.getMinY();
            final float xmax = (float)rectangle.getMaxX();
            final float ymax = (float)rectangle.getMaxY();
            final Segment[] segments = Segment.getInstances(new float[] {xmin,ymin, xmax,ymin, xmax,ymax, xmin,ymax});
            if (segments.length==1) data=segments[0]; // length may be 0 or 2 if some points contain NaN
        }
    }

    /**
     * Construit des polylignes � partir des coordonn�es (<var>x</var>,<var>y</var>) sp�cifi�es.
     * Les valeurs <code>NaN</code> au d�but et � la fin de <code>data</code> seront ignor�es.
     * Celles qui apparaissent au milieu auront pour effet de s�parer le trait en plusieurs
     * polylignes.
     *
     * @param  data Tableau de coordonn�es (peut contenir des NaN). Ces donn�es seront copi�es,
     *         de sorte que toute modification future de <code>data</code> n'aura pas d'impact
     *         sur les polylignes cr��s.
     * @param  coordinateSystem Syst�me de coordonn�es des points de <code>data</code>.
     *         Cet argument peut �tre nul si le syst�me de coordonn�es n'est pas connu.
     * @return Tableau de polylignes. Peut avoir une longueur de 0, mais ne sera jamais nul.
     */
    public static Polygon[] getInstances(final float[] data, final CoordinateSystem coordinateSystem)
    {
        final Segment[]  segments = Segment.getInstances(data);
        final Polygon[] polylines = new Polygon[segments.length];
        final CoordinateTransformation ct = getIdentityTransform(coordinateSystem);
        for (int i=0; i<polylines.length; i++)
        {
            polylines[i]=new Polygon(ct);
            polylines[i].data = segments[i];
        }
        return polylines;
    }

    /**
     * Construit des polylignes � partir de la forme g�om�trique sp�cifi�e. Si <code>shape</code> est d�j�
     * de la classe <code>Polygon</code>, il sera retourn� dans un tableau de longueur 1. Dans les autres
     * cas, cette m�thode peut retourner un tableau de longueur de 0, mais ne retourne jamais <code>null</code>.
     *
     * @param  shape Forme g�om�trique � copier dans un ou des polylignes.
     * @param  coordinateSystem Syst�me de coordonn�es des points de <code>shape</code>.
     *         Cet argument peut �tre nul si le syst�me de coordonn�es n'est pas connu.
     * @return Tableau de polylignes. Peut avoir une longueur de 0, mais ne sera jamais nul.
     */
    public static Polygon[] getInstances(final Shape shape, CoordinateSystem coordinateSystem)
    {
        coordinateSystem = CTSUtilities.getCoordinateSystem2D(coordinateSystem);
        if (shape instanceof Polygon)
        {
            return new Polygon[] {(Polygon) shape};
        }
        final CoordinateTransformation ct = getIdentityTransform(coordinateSystem);
        final List<Polygon>     polylines = new ArrayList<Polygon>();
        final Rectangle2D          bounds = shape.getBounds2D();
        final PathIterator            pit = shape.getPathIterator(null, 0.025*Math.max(bounds.getHeight(), bounds.getWidth()));
        final float[]              buffer = new float[6];
        float[]                     array = new float[64];
        while (!pit.isDone())
        {
            if (pit.currentSegment(array)!=PathIterator.SEG_MOVETO)
            {
                throw new IllegalPathStateException();
            }
            /*
             * Une fois entr� dans ce bloc, le tableau <code>array</code> contient
             * d�j� le premier point aux index 0 (pour x) et 1 (pour y). On ajoute
             * maintenant les autres points tant qu'ils correspondent � des
             * instructions <code>LINETO</code>.
             */
            int index = 2;
            int interiorSign = UNKNOW;
      loop: for (pit.next(); !pit.isDone(); pit.next())
            {
                switch (pit.currentSegment(buffer))
                {
                    case PathIterator.SEG_LINETO:
                    {
                        if (index >= array.length)
                            array = XArray.resize(array, 2*index);
                        System.arraycopy(buffer, 0, array, index, 2);
                        index += 2;
                        break;
                    }
                    case PathIterator.SEG_MOVETO:
                    {
                        break loop;
                    }
                    case PathIterator.SEG_CLOSE:
                    {
                        interiorSign=ELEVATION;
                        pit.next();
                        break loop;
                    }
                    default:
                    {
                        throw new IllegalPathStateException();
                    }
                }
            }
            /*
             * Construit les polylignes qui correspondent �
             * la forme g�om�trique qui vient d'�tre balay�e.
             */
            final Segment[] segments = Segment.getInstances(array, 0, index);
            for (int i=0; i<segments.length; i++)
            {
                final Polygon polyline=new Polygon(ct);
                polyline.data = segments[i];
                polylines.add(polyline);
            }
        }
        return polylines.toArray(new Polygon[polylines.size()]);
    }

    /**
     * Retourne le syst�me de coordonn�es natif des points de
     * {@link #data}, ou <code>null</code> s'il n'est pas connu.
     */
    private CoordinateSystem getSourceCS()
    {
        final CoordinateTransformation coordinateTransform = this.coordinateTransform; // avoid synchronization
        return (coordinateTransform!=null) ? coordinateTransform.getSourceCS() : null;
    }

    /**
     * Returns the polyline's coordinate system, or <code>null</code> if unknow.
     */
    public CoordinateSystem getCoordinateSystem()
    {
        final CoordinateTransformation coordinateTransform = this.coordinateTransform; // avoid synchronization
        return (coordinateTransform!=null) ? coordinateTransform.getTargetCS() : null;
    }

    /**
     * Retourne la transformation qui permet de passer du syst�me de coordonn�es
     * des points {@link #data} vers le syst�me de coordonn�es sp�cifi�.   Si au
     * moins un des syst�mes de coordonn�es n'est pas connu, alors cette m�thode
     * retourne <code>null</code>.
     *
     * @throws CannotCreateTransformException Si la transformation ne peut pas �tre cr��e.
     */
    private CoordinateTransformation getCoordinateTransformation(final CoordinateSystem coordinateSystem) throws CannotCreateTransformException
    {
        final CoordinateTransformation coordinateTransform = this.coordinateTransform; // avoid synchronization
        if (coordinateSystem!=null && coordinateTransform!=null)
        {
            if (coordinateSystem.equivalents(coordinateTransform.getTargetCS()))
            {
                return coordinateTransform;
            }
            return TRANSFORMS.createFromCoordinateSystems(coordinateTransform.getSourceCS(), coordinateSystem);
        }
        return null;
    }

    /**
     * Returns a math transform for the specified transformations.
     * If no transformation is available, or if it is the identity
     * transform, then this method returns <code>null</code>. This
     * method accept null argument.
     */
    private static MathTransform2D getMathTransform2D(final CoordinateTransformation transformation)
    {
        if (transformation!=null)
        {
            final MathTransform transform = transformation.getMathTransform();
            if (!transform.isIdentity()) return (MathTransform2D) transform;
        }
        return null;
    }

    /**
     * Set the polyline's coordinate system. Calling this method is equivalents
     * to reproject all polyline's points from the old coordinate system to the
     * new one.
     *
     * @param  The new coordinate system. A <code>null</code> value reset the
     *         coordinate system given at construction time.
     * @throws TransformException If a transformation failed. In case of failure,
     *         the state of this object will stay unchanged (as if this method has
     *         never been invoked).
     */
    public synchronized void setCoordinateSystem(CoordinateSystem coordinateSystem) throws TransformException
    {
        coordinateSystem = CTSUtilities.getCoordinateSystem2D(coordinateSystem);
        if (coordinateSystem==null)
        {
            coordinateSystem = getSourceCS();
            // May still null. Its ok.
        }
        final CoordinateTransformation transformCandidate = getCoordinateTransformation(coordinateSystem);
        /*
         * Compute bounds now. The getBounds2D(...) method scan every point.
         * Concequently, if a exception must be throws, it will be thrown now.
         *
         * Note: There is no change to resolution, since it is computed
         *       using source coordinate system (which doesn't change).
         *       No change to 'dataBounds' neither.
         */
        bounds = Segment.getBounds2D(data, (MathTransform2D)transformCandidate.getMathTransform());
        /*
         * Store the new coordinate transform
         * only after projection succeded.
         */
        this.coordinateTransform = transformCandidate;
        this.resolution = Float.NaN;
        this.cache = null;
    }

    /**
     * Indique si la transformation sp�cifi�e est la transformation identit�e.
     * Une transformation nulle (<code>null</code>) est consid�r�e comme �tant
     * une transformation identit�e.
     */
    private static boolean isIdentity(final CoordinateTransformation coordinateTransform)
    {return coordinateTransform==null || coordinateTransform.getMathTransform().isIdentity();}

    /**
     * Test if this polyline is empty. An
     * empty polyline contains no point.
     */
    public synchronized boolean isEmpty()
    {return Segment.getPointCount(data)==0;}

    /**
     * Return the bounding box of this polyline, including its possible
     * borders. This method uses a cache, such that after a first calling,
     * the following calls should be fairly quick.
     *
     * @return A bounding box of this polylines. Changes to the
     *         fields of this rectangle will not affect the cache.
     */
    public synchronized Rectangle2D getBounds2D()
    {return (Rectangle2D) getCachedBounds().clone();}

    /**
     * Returns the smallest bounding box containing {@link #getBounds2D}.
     *
     * @deprecated This method is required by the {@link Shape} interface,
     *             but it doesn't provides enough precision for most cases.
     *             Use {@link #getBounds2D()} instead.
     */
    public synchronized Rectangle getBounds()
    {
        final Rectangle bounds=new Rectangle();
        bounds.setRect(getCachedBounds()); // 'setRect' effectue l'arrondissement correct.
        return bounds;
    }

    /**
     * Retourne un rectangle englobant tous les points de {@link #data}. Parce que cette m�thode
     * retourne directement le rectangle de la cache et non une copie, le rectangle retourn� ne
     * devrait jamais �tre modifi�.
     *
     * @return Un rectangle englobant toutes les points de {@link #data}.
     *         Ce rectangle peut �tre vide, mais ne sera jamais nul.
     */
    private Rectangle2D getDataBounds()
    {
        if (dataBounds==null)
        {
            dataBounds = getBounds(data, null);
            if (isIdentity(coordinateTransform))
            {
                bounds = dataBounds; // Avoid computing the same rectangle two times
            }
        }
        return dataBounds;
    }

    /**
     * Return the bounding box of this isoline. This methode returns
     * a direct reference to the internally cached bounding box. DO
     * NOT MODIFY!
     */
    final Rectangle2D getCachedBounds()
    {
        assert Thread.holdsLock(this);
        if (bounds==null)
        {
            bounds = getBounds(data, coordinateTransform);
            if (isIdentity(coordinateTransform))
            {
                dataBounds = bounds; // Avoid computing the same rectangle two times
            }
        }
        return bounds;
    }

    /**
     * Retourne un rectangle englobant tous les points projet�s dans le syst�me de coordonn�es sp�cifi�.
     * Cette m�thode tentera de retourner un des rectangles de la cache interne lorsque appropri�.
     * Parce que cette m�thode peut retourner directement le rectangle de la cache et non une copie,
     * le rectangle retourn� ne devrait jamais �tre modifi�.
     *
     * @param  Le syst�me de coordonn�es selon lequel projeter les points.
     * @return Un rectangle englobant tous les points de ce polyligne.
     *         Ce rectangle peut �tre vide, mais ne sera jamais nul.
     * @throws TransformException si une projection cartographique a �chou�e.
     */
    private Rectangle2D getCachedBounds(final CoordinateSystem coordinateSystem) throws TransformException
    {
        if (Utilities.equals(getSourceCS(),         coordinateSystem)) return getDataBounds();
        if (Utilities.equals(getCoordinateSystem(), coordinateSystem)) return getCachedBounds();
        Rectangle2D bounds=Segment.getBounds2D(data, getMathTransform2D(coordinateTransform));
        if (bounds==null) bounds=new Rectangle2D.Float();
        return bounds;
    }

    /**
     * Retourne en rectangle englobant tous les points de <code>data</code>.  Cette m�thode ne
     * devrait �tre appel�e que dans un contexte o� l'on sait que la projection cartographique
     * ne devrait jamais �chouer.
     *
     * @param  data Un des maillons de la cha�ne de tableaux de points (peut �tre nul).
     * @param  coordinateTransform Transformation � appliquer sur les points de <code>data</code>.
     * @return Un rectangle englobant toutes les points de <code>data</code>.
     *         Ce rectangle peut �tre vide, mais ne sera jamais nul.
     */
    private static Rectangle2D getBounds(final Segment data, final CoordinateTransformation coordinateTransform)
    {
        Rectangle2D bounds;
        try
        {
            bounds=Segment.getBounds2D(data, getMathTransform2D(coordinateTransform));
            if (bounds==null)
            {
                assert Segment.getPointCount(data)==0;
                bounds=new Rectangle2D.Float();
            }
        }
        catch (TransformException exception)
        {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getBounds2D", exception);
            bounds=null;
        }
        return bounds;
    }

    /**
     * Indique si la coordonn�e (<var>x</var>,<var>y</var>) sp�cifi�e est � l'int�rieur
     * de ce polygone. Le polygone doit avoir �t� ferm� avant l'appel de cette m�thode
     * (voir {@link #close}), sans quoi cette m�thode retournera toujours <code>false</code>.
     *
     * @param  x Coordonn�e <var>x</var> du point � tester.
     * @param  y Coordonn�e <var>y</var> du point � tester.
     * @param  transformation Transformation � utiliser pour convertir les points de {@link #data},
     *         ou <code>null</code> pour ne pas faire de transformation. Si une transformation
     *         non-nulle est sp�cifi�e, elle devrait avoir �t� obtenue par un appel � la m�thode
     *         <code>getCoordinateTransformation(targetCS)</code>. Tous les points du polygone seront
     *         alors projet�s selon le syst�me de coordonn�es <code>targetCS</code>. Autant que
     *         possible, il est plus efficace de ne calculer que la projection inverse du point
     *         (<var>x</var>,<var>y</var>) et de sp�cifier <code>null</code> pour cet argument.
     * @return <code>true</code> si le point est � l'int�rieur de ce polygone.
     *
     * @author Andr� Gosselin (version originale en C)
     * @author Martin Desruisseaux (adaptation pour le Java)
     */
    private boolean contains(final float x, final float y, final CoordinateTransformation transformation)
    {
        if (interiorSign==UNKNOW)
        {
            return false;
        }
        /*
         * Imaginez une ligne droite partant du point (<var>x</var>,<var>y</var>)
         * et allant jusqu'� l'infini � droite du point (c'est-�-dire vers l'axe
         * des <var>x</var> positifs). On comptera le nombre de fois que le polygone
         * intercepte cette ligne. Si ce nombre est impair, le point est � l'int�rieur
         * du polygone. La variable <code>nInt</code> fera ce comptage.
         */
        int   nInt                 = 0;
        int   intSuspended         = 0;
        int   nPointsToRecheck     = 0;
        final Point2D.Float nextPt = new Point2D.Float();
        final Segment.Iterator  it = new Segment.Iterator(data, getMathTransform2D(transformation));
        float x1                   = Float.NaN;
        float y1                   = Float.NaN;
        /*
         * Extrait un premier point. Il y aura un probl�me dans l'algorithme qui suit
         * si le premier point est sur la m�me ligne horizontale que le point � v�rifier.
         * Pour contourner le probl�me, on recherchera le premier point qui n'est pas sur
         * la m�me ligne horizontale.
         */
        while (true)
        {
            final float x0=x1;
            final float y0=y1;
            nPointsToRecheck++;
            if (it.next(nextPt)==null)
            {
                return false;
            }
            x1 = nextPt.x;
            y1 = nextPt.y;
            if (y1!=y) break;
            /*
             * V�rifie si le point tombe exactement
             * sur le segment (x0,y0)-(x1-y1). Si oui,
             * ce n'est pas la peine d'aller plus loin.
             */
            if (x0 < x1)
            {
                if (x>=x0 && x<=x1) return true;
            }
            else
            {
                if (x>=x1 && x<=x0) return true;
            }
        }
        /*
         * Balaye tous les points du polygone. Lorsque le dernier point sera
         * extrait, la variable <code>count</code> sera ajust�e de fa�on � ne
         * rebalayer que les points qui doivent �tre repass�s.
         */
        for (int count=-1; count!=0; count--)
        {
            /*
             * Obtient le point suivant. Si on a atteint la fin du polygone,
             * alors on refermera le polygone si ce n'�tait pas d�j� fait.
             * Si le polygone avait d�j� �t� referm�, alors ce sera la fin
             * de la boucle.
             */
            final float x0=x1;
            final float y0=y1;
            if (it.next(nextPt)==null)
            {
                count = nPointsToRecheck+1;
                nPointsToRecheck = 0;
                it.rewind();
                continue;
            }
            x1=nextPt.x;
            y1=nextPt.y;
            /*
             * On dispose maintenant d'un segment de droite allant des coordonn�es
             * (<var>x0</var>,<var>y0</var>) jusqu'� (<var>x1</var>,<var>y1</var>).
             * Si on s'apper�oit que le segment de droite est compl�tement au dessus
             * ou compl�tement en dessous du point (<var>x</var>,<var>y</var>), alors
             * on sait qu'il n'y a pas d'intersection � droite et on continue la boucle.
             */
            if (y0 < y1)
            {
                if (y<y0 || y>y1) continue;
            }
            else
            {
                if (y<y1 || y>y0) continue;
            }
            /*
             * On sait maintenant que notre segment passe soit � droite, ou soit � gauche
             * de notre point. On calcule maintenant la coordonn�e <var>xi</var> � laquelle
             * � lieu l'intersection (avec la droite horizontale passant par notre point).
             */
            final float dy = y1-y0;
            final float xi = x0 + (x1-x0)*(y-y0)/dy;
            if (!Float.isInfinite(xi) && !Float.isNaN(xi))
            {
                /*
                 * Si l'intersection est compl�tement � gauche du point, alors il n'y
                 * a �videmment pas d'intersection � droite et on continue la boucle.
                 * Sinon, si l'intersection se fait exactement � la coordonn�e <var>x</var>
                 * (c'est peu probable...), alors notre point est exactement sur la bordure
                 * du polygone et le traitement est termin�.
                 */
                if (x >  xi) continue;
                if (x == xi) return true;
            }
            else
            {
                /*
                 * Un traitement particulier est fait si le segment est horizontal. La valeur
                 * <var>xi</var> n'est pas valide (on peut voir �a comme si l'intersection se
                 * faisait partout sur la droite plut�t qu'en un seul point). Au lieu de faire
                 * les v�rifications avec <var>xi</var>, on les fera plut�t avec les <var>x</var>
                 * minimal et maximal du segment.
                 */
                if (x0 < x1)
                {
                    if (x >  x1) continue;
                    if (x >= x0) return true;
                }
                else
                {
                    if (x >  x0) continue;
                    if (x >= x1) return true;
                }
            }
            /*
             * On sait maintenant qu'il y a une intersection � droite. En principe, il
             * suffirait d'incr�menter 'nInt'. Toutefois, on doit faire attention au cas
             * cas o� <var>y</var> serait exactement � la hauteur d'une des extr�mit�s du
             * segment. Y a t'il intersection ou pas? �a d�pend si les prochains segments
             * continuent dans la m�me direction ou pas. On ajustera un drapeau, de sorte
             * que la d�cision d'incr�menter 'nInt' ou pas sera prise plus tard dans la
             * boucle, quand les autres segments auront �t� examin�s.
             */
            if (x0==x1 && y0==y1) continue;
            if (y==y0 || y==y1)
            {
                final int sgn=XMath.sgn(dy);
                if (sgn!=0)
                {
                    if (intSuspended!=0)
                    {
                        if (intSuspended==sgn) nInt++;
                        intSuspended=0;
                    }
                    else intSuspended=sgn;
                }
            }
            else nInt++;
        }
        /*
         * Si le nombre d'intersection � droite du point est impaire,
         * alors le point est � l'int�rieur du polygone. Sinon, il est
         * � l'ext�rieur.
         */
        return (nInt & 1)!=0;
    }

    /**
     * Indique si la coordonn�e (<var>x</var>,<var>y</var>) sp�cifi�e est � l'int�rieur
     * de ce polygone. Les coordonn�es du point doivent �tre exprim�es selon le syst�me
     * de coordonn�es du polygone, soit {@link #getCoordinateSystem()}. Le polygone doit
     * aussi avoir �t� ferm� avant l'appel de cette m�thode (voir {@link #close}), sans
     * quoi cette m�thode retournera toujours <code>false</code>.
     */
    public synchronized boolean contains(double x, double y)
    {
        // IMPLEMENTATION NOTE: The polygon's native point array ({@link #data}) and the
        // (x,y) point may use different coordinate systems. For efficiency raisons, the
        // (x,y) point is projected to the "native" polygon's coordinate system  instead
        // of projecting all polygon's points. As a result, point very close to the polygon's
        // edge may appear inside (when viewed on screen) while this method returns <code>false</code>,
        // and vis-versa. This is because some projections transform straight lines
        // into curves, but the Polygon class ignore curves and always use straight
        // lines between any two points.
        if (coordinateTransform!=null) try
        {
            final MathTransform transform = coordinateTransform.getMathTransform();
            if (!transform.isIdentity())
            {
                Point2D point = new Point2D.Double(x,y);
                point = ((MathTransform2D) transform.inverse()).transform(point, point);
                x = point.getX();
                y = point.getY();
            }
        }
        catch (TransformException exception)
        {
            // Si la projection a �chou�e, alors le point est probablement en dehors
            // du polygone (puisque tous les points du polygone sont projetables).
            return false;
        }
        /*
         * On v�rifie d'abord si le rectangle 'dataBounds' contient
         * le point, avant d'appeler la co�teuse m�thode 'contains'.
         */
        return getDataBounds().contains(x,y) && contains((float)x, (float)y, null);
    }

    /**
     * V�rifie si un point <code>pt</code> est � l'int�rieur de ce polygone. Les coordonn�es
     * du point doivent �tre exprim�es selon le syst�me de coordonn�es du polygone, soit
     * {@link #getCoordinateSystem()}. Le polygone doit aussi avoir �t� ferm� avant l'appel
     * de cette m�thode (voir {@link #close}), sans quoi cette m�thode retournera toujours
     * <code>false</code>.
     */
    public boolean contains(final Point2D pt)
    {return contains(pt.getX(), pt.getY());}

    /**
     * Test if the interior of this contour entirely contains the given rectangle.
     * The rectangle's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public synchronized boolean contains(final Rectangle2D rect)
    {return containsPolygon(new Polygon(rect, getCoordinateSystem()));}

    /**
     * Test if the interior of this polygon
     * entirely contains the given shape.
     */
    public synchronized boolean contains(final Shape shape)
    {
        if (shape instanceof Polygon)
        {
            return containsPolygon((Polygon) shape);
        }
        final Polygon[] polylines = getInstances(shape, getCoordinateSystem());
        for (int i=0; i<polylines.length; i++)
            if (!containsPolygon(polylines[i]))
                return false;
        return polylines.length!=0;
    }

    /**
     * Test if the interior of this polygon
     * entirely contains the given polygon.
     */
    private boolean containsPolygon(final Polygon shape)
    {
        /*
         * Cette m�thode retourne <code>true</code> si ce polygone contient
         * au moins un point de <code>shape</code> et qu'il n'y a aucune
         * intersection entre <code>shape</code> et <code>this</code>.
         */
        if (interiorSign!=UNKNOW) try
        {
            final CoordinateSystem coordinateSystem = getSourceCS();
            if (getDataBounds().contains(shape.getCachedBounds(coordinateSystem)))
            {
                final Point2D.Float firstPt = new Point2D.Float();
                final  Line2D.Float segment = new  Line2D.Float();
                final Segment.Iterator   it = new Segment.Iterator(shape.data, shape.getMathTransform2D(
                                                                   shape.getCoordinateTransformation(coordinateSystem)));
                if (it.next(firstPt)!=null && contains(firstPt.x, firstPt.y, null))
                {
                    segment.x2 = firstPt.x;
                    segment.y2 = firstPt.y;
                    do if (!it.next(segment))
                    {
                        if (shape.interiorSign==UNKNOW || isSingular(segment)) return true;
                        segment.x2 = firstPt.x;
                        segment.y2 = firstPt.y;
                    }
                    while (!intersects(segment));
                }
            }
        }
        catch (TransformException exception)
        {
            // Conservatly return 'false' if some points from 'shape' can't be projected into
            // {@link #data}'s coordinate system.   This behavior is compliant with the Shape
            // specification. Futhermore, those points are probably outside this polygon since
            // all polygon's points are projectable.
        }
        return false;
    }

    /**
     * Indique si les points (x1,y1) et (x2,y2)
     * de la ligne sp�cifi�e sont identiques.
     */
    private static boolean isSingular(final Line2D.Float segment)
    {
        return Float.floatToIntBits(segment.x1)==Float.floatToIntBits(segment.x2) &&
               Float.floatToIntBits(segment.y1)==Float.floatToIntBits(segment.y2);
    }

    /**
     * Determine si la ligne <code>line</code> intercepte une des lignes de
     * ce polygone. Le polygone sera automatiquement referm� si n�cessaire;
     * il n'est donc pas n�cessaire que le dernier point r�p�te le premier.
     *
     * @param  line Ligne dont on veut d�terminer si elle intercepte ce polygone.
     *         Cette ligne doit obligatoirement �tre exprim�e selon le syst�me de
     *         coordonn�es natif de {@link #array}, c'est-�-dire {@link #getSourceCS}.
     * @return <code>true</code> si la ligne <code>line</code> intercepte ce poylgone.
     */
    private boolean intersects(final Line2D line)
    {
        final Point2D.Float firstPt = new Point2D.Float();
        final  Line2D.Float segment = new  Line2D.Float();
        final Segment.Iterator   it = new Segment.Iterator(data, null); // Ok m�me si 'data' est nul.
        if (it.next(firstPt)!=null)
        {
            segment.x2 = firstPt.x;
            segment.y2 = firstPt.y;
            do if (!it.next(segment))
            {
                if (interiorSign==UNKNOW || isSingular(segment)) return false;
                segment.x2 = firstPt.x;
                segment.y2 = firstPt.y;
            }
            while (!segment.intersectsLine(line));
            return true;
        }
        return false;
    }

    /**
     * Tests if the interior of the contour intersects the interior of a specified rectangle.
     * The rectangle's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public synchronized boolean intersects(final Rectangle2D rect)
    {return intersectsPolygon(new Polygon(rect, getCoordinateSystem()));}

    /**
     * Tests if the interior of the contour intersects the interior of a specified shape.
     * The shape's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public synchronized boolean intersects(final Shape shape)
    {
        if (shape instanceof Polygon)
        {
            return intersectsPolygon((Polygon) shape);
        }
        final Polygon[] polylines = getInstances(shape, getCoordinateSystem());
        for (int i=0; i<polylines.length; i++)
            if (intersectsPolygon(polylines[i]))
                return true;
        return false;
    }

    /**
     * Test if this polygon intercepts a specified polygon.
     *
     * If this polygon is <em>closed</em> (if it is an island or a lake),
     * this method will return <code>true</code> if at least one point of
     * <code>s</code> lies inside this polylines. If this polylines is not
     * closed, then this method will return the same thing as
     * {@link #intersectsEdge}.
     */
    private boolean intersectsPolygon(final Polygon shape)
    {return intersects(shape, interiorSign==UNKNOW);}

    /**
     * Test if the edge of this polylines intercepts the edge of a
     * specified polylines.
     *
     * This should never happen with an error free bathymery map. However,
     * it could happen if the two polylines don't use the same units. For
     * example, this method may be use to test if an isoline of 15 degrees
     * celsius intercepts an isobath of 30 meters.
     *
     * @param s Polylines to test.
     * @return <code>true</code> If an intersection is found.
     */
    final boolean intersectsEdge(final Polygon shape)
    {return intersects(shape, true);}

    /**
     * Impl�mentation des m�thodes <code>intersects[Edge](Polygon)</code>.
     *
     * @param  shape Polylignes � v�rifier.
     * @param  checkEdgeOnly <code>true</code> pour ne v�rifier que
     *         les bordures, sans tenir compte de l'int�rieur de ce
     *         polylignes.
     */
    private boolean intersects(final Polygon shape, final boolean checkEdgeOnly)
    {
        try
        {
            final CoordinateSystem coordinateSystem = getSourceCS();
            if (getDataBounds().intersects(shape.getCachedBounds(coordinateSystem)))
            {
                final Point2D.Float firstPt = new Point2D.Float();
                final  Line2D.Float segment = new  Line2D.Float();
                final Segment.Iterator   it = new Segment.Iterator(shape.data, shape.getMathTransform2D(
                                                                   shape.getCoordinateTransformation(coordinateSystem)));
                if (it.next(firstPt)!=null)
                {
                    if (checkEdgeOnly || !contains(firstPt.x, firstPt.y))
                    {
                        segment.x2 = firstPt.x;
                        segment.y2 = firstPt.y;
                        do if (!it.next(segment))
                        {
                            if (interiorSign==UNKNOW || isSingular(segment)) return false;
                            segment.x2 = firstPt.x;
                            segment.y2 = firstPt.y;
                        }
                        while (!intersects(segment));
                    }
                    return true;
                }
            }
            return false;
        }
        catch (TransformException exception)
        {
            // Conservatly return 'true' if some points from 'shape' can't be projected into
            // {@link #data}'s coordinate system.  This behavior is compliant with the Shape
            // specification.
            return true;
        }
    }

    /**
     * Returns a path iterator for this polyline.
     */
    public PathIterator getPathIterator(final AffineTransform transform)
    {
        return new net.seas.map.PathIterator(this, transform);
        // Only polygons internal to Isoline have drawingDecimation!=1.
        // Consequently, public polygons never apply decimation, while
        // Isoline's polgyons may apply a decimation for faster rendering
        // when painting through the 'Isobath.paint(...)' method.
    }

    /**
     * Returns a path iterator for this polyline.
     */
    public PathIterator getPathIterator(final AffineTransform transform, final double flatness)
    {return getPathIterator(transform);}

    /**
     * Retourne un tableau de coordonn�es (<var>x</var>,<var>y</var>) transform�e.
     *
     * @param  destination Transformation affine � appliquer sur les donn�es. La valeur
     *         <code>null</code> sera interpr�t�e comme �tant la transformation identit�e.
     * @return Un tableau de points (<var>x</var>,<var>y</var>). Cette m�thode retourne une
     *         r�f�rence directe vers un tableau interne. En cons�quence, aucune modification
     *         ne doit �tre faite au tableau retourn�.
     */
    final synchronized float[] getDrawingArray(AffineTransform transform)
    {
        if (transform!=null)
        {
            transform=pool.intern(new AffineTransform(transform));
            // TODO: This line may fill 'pool' with a lot of entries
            //       (>100) when the user change zoom often (e.g. is
            //       scrolling). Should we look for an other way?
        }
        else transform=IDENTITY;
        /*
         * Tente de r�cup�rer le tableau de points qui a �t� utilis� la derni�re fois. Si la transformation
         * affine n'a pas chang� depuis la derni�re fois, alors on pourra retourner le tableau directement.
         * Sinon, on tentera de modifier les coordonn�es en prenant en compte seulement le <em>changement</em>
         * de {@link AffineTransform} depuis la derni�re fois. Mais cette derni�re �tape ne sera faite qu'�
         * la condition que le tableau ne soit pas en cours d'utilisation par un autre it�rateur (lockCount==0).
         */
        float[] array=null;
        if (cache!=null)
        {
            array = (float[]) cache.get();
            if (array!=null)
            {
                if (transform.equals(cache.transform))
                {
                    cache.lockCount++;
                    return array;
                }
                if (cache.lockCount==0) try
                {
                    final AffineTransform change = cache.transform.createInverse();
                    change.preConcatenate(transform);
                    change.transform(array, 0, array, 0, array.length/2);
                    cache.transform = transform;
                    cache.lockCount = 1;
                    return array;
                }
                catch (NoninvertibleTransformException exception)
                {
                    Utilities.unexpectedException("net.seas.map", "Polygon", "getDrawingArray", exception);
                    // Continue... On va simplement reconstruire le tableau � partir de la base.
                }
                else
                {
                    // Should be uncommon. Doesn't hurt, but may be a memory issue for big polyline.
                    LOGGER.info(Resources.format(ResourceKeys.WARNING_EXCESSIVE_MEMORY_USAGE));
                    array=null; // {@link #toArray} will allocate a new array.
                }
            }
        }
        /*
         * Reconstruit le tableau de points � partir des donn�es de bas niveau.
         * La projection cartographique sera appliqu�e par {@link #toArray}.
         */
        array=toArray(array, drawingDecimation);
        assert (array.length & 1) == 0;
        final int pointCount = array.length/2;
        transform.transform(array, 0, array, 0, pointCount);
        cache = new Cache(array);
        cache.transform = transform;
        cache.lockCount = 1;
        return array;
    }

    /**
     * Indique qu'un tableau de points ne sera plus utilis�.
     */
    final synchronized void releaseDrawingArray(final float[] array)
    {
        final Cache cache=this.cache;
        if (cache!=null && cache.get()==array)
        {
            cache.lockCount--;
            assert cache.lockCount >= 0;
        }
    }

    /**
     * Sp�cifie la d�cimation � appliquer au moment du tra�age du polyligne.
     * La valeur 1 signifie qu'aucune d�cimation n'est faite;  la valeur 2
     * signifie qu'on ne tracera qu'un point sur 2, etc. Cette information
     * n'est utilis�e que par les objets {@link PathIterator}.
     *
     * @param  decimation D�cimation � appliquer.
     * @return Nombre de points qui seront � recalculer, ou 0 si la cache
     *         a pu �tre r�utilis�e. Cette information n'est fournie qu'�
     *         des fins de statistiques.
     */
    final synchronized int setDrawingDecimation(final int decimation)
    {
        final byte newDecimation = (byte)Math.max(1, Math.min(Byte.MAX_VALUE, decimation));
        if (newDecimation != drawingDecimation)
        {
            cache = null;
            drawingDecimation = newDecimation;
            return Segment.getPointCount(data);
        }
        else return 0;
    }

    /**
     * Returns the drawing decimation.
     */
    final int getDrawingDecimation()
    {return drawingDecimation;}

    /**
     * Return the number of points in this polyline.
     */
    public synchronized int getPointCount()
    {return Segment.getPointCount(data);}

    /**
     * Returns all polyline's points. Point coordinates are stored in {@link Point2D}
     * objects using this polyline's coordinate system ({@link #getCoordinateSystem}).
     * This method returns an immutable collection: changes done to <code>Polygon</code>
     * after calling this method will not affect the collection. Despite this method has
     * a copy semantic, the collection will share many internal structures in such a way
     * that memory consumption should stay low.
     */
    public synchronized Collection<Point2D> getPoints()
    {return new Segment.Collection(Segment.clone(data), getMathTransform2D(coordinateTransform));}

    /**
     * Stores the value of the first point into the specified point object.
     *
     * @param  point Object in which to store the unprojected coordinate.
     * @return <code>point</code>, or a new {@link Point2D} if <code>point</code> was nul.
     * @throws NoSuchElementException If this polylines contains no point.
     *
     * @see #getFirstPoints(Point2D[])
     * @see #getLastPoint(Point2D)
     */
    public synchronized Point2D getFirstPoint(Point2D point) throws NoSuchElementException
    {
        point = Segment.getFirstPoint(data, point);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try
        {
            point = transform.transform(point, point);
        }
        catch (TransformException exception)
        {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getFirstPoint", exception);
        }
        return point;
    }

    /**
     * Stores the value of the last point into the specified point object.
     *
     * @param  point Object in which to store the unprojected coordinate.
     * @return <code>point</code>, or a new {@link Point2D} if <code>point</code> was nul.
     * @throws NoSuchElementException If this polylines contains no point.
     *
     * @see #getLastPoints(Point2D[])
     * @see #getFirstPoint(Point2D)
     */
    public synchronized Point2D getLastPoint(Point2D point) throws NoSuchElementException
    {
        point = Segment.getLastPoint(data, point);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try
        {
            point = transform.transform(point, point);
        }
        catch (TransformException exception)
        {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getLastPoint", exception);
        }
        return point;
    }

    /**
     * Stores the values of <code>points.length</code> first points into the specified array.
     *
     * @param points An array to fill with first polyline's points. <code>points[0]</code>
     *               will contains the first point, <code>points[1]</code> the second point,
     *               etc.
     *
     * @throws NoSuchElementException If this polylines doesn't contain enough points.
     */
    public synchronized void getFirstPoints(final Point2D[] points) throws NoSuchElementException
    {
        Segment.getFirstPoints(data, points);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try
        {
            for (int i=0; i<points.length; i++)
                points[i] = transform.transform(points[i], points[i]);
        }
        catch (TransformException exception)
        {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getFirstPoints", exception);
        }
        assert points.length==0 || Utilities.equals(getFirstPoint(null), points[0]);
    }

    /**
     * Stores the values of <code>points.length</code> last points into the specified array.
     *
     * @param points An array to fill with last polyline's points. <code>points[points.length-1]</code>
     *               will contains the last point, <code>points[points.length-2]</code> the point before
     *               the last one, etc.
     *
     * @throws NoSuchElementException If this polylines doesn't contain enough points.
     */
    public synchronized void getLastPoints(final Point2D[] points) throws NoSuchElementException
    {
        Segment.getLastPoints(data, points);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try
        {
            for (int i=0; i<points.length; i++)
                points[i] = transform.transform(points[i], points[i]);
        }
        catch (TransformException exception)
        {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getLastPoints", exception);
        }
        assert points.length==0 || Utilities.equals(getLastPoint(null), points[points.length-1]);
    }

    /**
     * Ajoute des points au d�but de ce polyligne. Ces points seront consid�r�s comme
     * faisant partie de la bordure de la carte, et non comme des points repr�sentant
     * une structure g�ographique.
     *
     * @param  border Coordonn�es � ajouter sous forme de paires de nombres (x,y).
     * @param  lower  Index du premier <var>x</var> � ajouter � la bordure.
     * @param  upper  Index suivant celui du dernier <var>y</var> � ajouter � la bordure.
     * @throws IllegalStateException si ce polygone a d�j� �t� ferm�.
     * @throws TransformException si <code>border</code> contient des points
     *         invalides pour le syst�me de coordonn�es natif de ce polyligne.
     */
    public void prependBorder(final float[] border, final int lower, final int upper) throws TransformException
    {addBorder(border, lower, upper, false);}

    /**
     * Ajoute des points � la fin de ce polyligne. Ces points seront consid�r�s comme
     * faisant partie de la bordure de la carte, et non comme des points repr�sentant
     * une structure g�ographique.
     *
     * @param  border Coordonn�es � ajouter sous forme de paires de nombres (x,y).
     * @param  lower  Index du premier <var>x</var> � ajouter � la bordure.
     * @param  upper  Index suivant celui du dernier <var>y</var> � ajouter � la bordure.
     * @throws IllegalStateException si ce polygone a d�j� �t� ferm�.
     * @throws TransformException si <code>border</code> contient des points
     *         invalides pour le syst�me de coordonn�es natif de ce polyligne.
     */
    public void appendBorder(final float[] border, final int lower, final int upper) throws TransformException
    {addBorder(border, lower, upper, true);}

    /**
     * Impl�mentation de <code>prependBorder(...)</code> et <code>prependBorder(...)</code>.
     *
     * @param append <code>true</code> pour effectuer l'op�ration <code>appendBorder</code>, ou
     *               <code>false</code> pour effectuer l'op�ration <code>prependBorder</code>.
     */
    private synchronized void addBorder(float[] border, int lower, int upper, final boolean append) throws TransformException
    {
        if (interiorSign==UNKNOW)
        {
            final MathTransform2D transform = getMathTransform2D(coordinateTransform);
            if (transform!=null)
            {
                final float[] oldBorder = border;
                border = new float[upper-lower];
                transform.inverse().transform(oldBorder, lower, border, 0, border.length);
                lower = 0;
                upper = border.length;
            }
            if (append)
                data   = Segment.appendBorder(data, border, lower, upper);
            else
                data   = Segment.prependBorder(data, border, lower, upper);
            dataBounds = null;
            bounds     = null;
            cache      = null;
            // No change to resolution, since its doesn't take border in account.
        }
        else throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_POLYGON_CLOSED));
    }

    /**
     * Ajoute � la fin de ce polyligne les donn�es du polyligne sp�cifi�.
     * Cette m�thode ne fait rien si <code>toAppend</code> est nul.
     *
     * @param  toAppend Polyligne � ajouter � la fin de <code>this</code>.
     *         Le polyligne <code>toAppend</code> ne sera pas modifi�.
     * @throws IllegalStateException    si ce polygone a d�j� �t� ferm�.
     * @throws IllegalArgumentException si le polygone <code>toAppend</code> a d�j� �t� ferm�.
     * @throws TransformException si <code>toAppend</code> contient des points
     *         invalides pour le syst�me de coordonn�es natif de ce polyligne.
     */
    public synchronized void append(final Polygon toAppend) throws TransformException
    {
        if (toAppend==null) return;
        if (!Utilities.equals(getSourceCS(), toAppend.getSourceCS()))
        {
            throw new UnsupportedOperationException(); // TODO.
        }
        if (interiorSign != UNKNOW || toAppend.interiorSign != UNKNOW)
        {
            throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_POLYGON_CLOSED));
        }
        data = Segment.append(data, Segment.clone(toAppend.data));
        if (dataBounds!=null)
        {
            if (toAppend.dataBounds!=null)
            {
                dataBounds.add(toAppend.dataBounds);
            }
            else dataBounds=null;
        }
        bounds = null;
        cache  = null;
        if (resolution > 0)
        {
            if (toAppend.resolution > 0)
            {
                final int thisCount =          getPointCount();
                final int thatCount = toAppend.getPointCount();
                resolution = (resolution*thisCount + toAppend.resolution*thatCount) / (thisCount + thatCount);
                assert resolution > 0;
            }
        }
    }

    /**
     * Reverse point order in this polyline.
     */
    public synchronized void reverse()
    {
        data=Segment.reverse(data);
        cache = null;
    }

    /**
     * Close and freeze this polygon. After closing it,
     * no more points can be added to this polygon.
     *
     * @param interiorSign Tells if this polygon is an elevation (e.g. an island in the middle of
     *        the sea) or a depression (e.g. a lake in the middle of a continent). Valid constants
     *        are {@link #ELEVATION}, {@link #DEPRESSION} or {@link #UNKNOW}.
     */
    public synchronized void close(final int interiorSign)
    {
        switch (interiorSign)
        {
            case ELEVATION:
            case DEPRESSION:
            case UNKNOW: data=Segment.freeze(data, interiorSign!=UNKNOW, false); break;
            default: throw new IllegalArgumentException(String.valueOf(interiorSign));
        }
        this.interiorSign = (byte) interiorSign;
        this.cache = null;
    }

    /**
     * Returns wether the polyline is closed or not.
     * A closed polyline is named a polygon.
     */
    public boolean isClosed()
    {return interiorSign!=UNKNOW;}

    /**
     * Indique si le polygone repr�sente une �l�vation ou une d�pression. Le nombre retourn� sera
     * la constante {@link #ELEVATION} si ce polygone repr�sente une �l�vation (par exemple une �le),
     * {@link #DEPRESSION} s'il repr�sente une depression (par exemple un lac) ou {@link #UNKNOW} (la
     * valeur par d�faut) si son type n'a pas �t� d�termin� ou si le polygone n'est pas ferm�.
     */
    public int getInteriorSign()
    {return interiorSign;}
    
    /**
     * Return a polylines with the point of this polylines from <code>lower</code>
     * inclusive to <code>upper</code> exclusive. The returned polyline will not be
     * closed, i.e. <code>{@link #getInteriorSign}=={@link #UNKNOW}</code>.  If no
     * data are available in the specified range, this method return <code>null</code>.
     */
    public synchronized Polygon subpoly(final int lower, final int upper)
    {
        final Segment sub=Segment.subpoly(data, lower, upper);
        if (sub==null) return null;
        if (Segment.equals(sub, data)) return this;
        final Polygon subPoly=new Polygon(coordinateTransform);
        subPoly.data=sub;
        assert subPoly.getPointCount() == (upper-lower);
        return subPoly;
    }

    /**
     * Returns the polyline's mean resolution. This resolution is the mean distance between
     * every pair of consecutive points in this polyline  (ignoring "extra" points used for
     * drawing a border, if there is one). This method try to returns linear units (usually
     * meters) no matter if the coordinate systems is actually a {@link ProjectedCoordinateSystem}
     * or a {@link GeographicCoordinateSystem}.
     *
     * @return The mean resolution, or {@link Float#NaN} if this polyline doesn't have any point.
     */
    public synchronized float getResolution()
    {
        if (!(resolution > 0)) try // '!' take NaN in account
        {
            final Statistics stats = Segment.getResolution(data, coordinateTransform);
            resolution = (stats!=null) ? (float)stats.mean() : Float.NaN;
        }
        catch (TransformException exception)
        {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getResolution", exception);
        }
        return resolution;
    }

    /**
     * Set the polyline's resolution. This method try to interpolate new points in such a way
     * that every point is spaced by exactly <code>resolution</code> units (usually meters)
     * from the previous one.
     *
     * @param  resolution Desired resolution, in the same units than {@link #getResolution}.
     * @throws TransformException If some coordinate transformations were needed and failed.
     *         There is no guaranteed on contour's state in case of failure.
     */
    public synchronized void setResolution(final double resolution) throws TransformException
    {
        CoordinateSystem sourceCS = getSourceCS();
        CoordinateSystem targetCS = sourceCS;
        if (sourceCS instanceof GeographicCoordinateSystem)
        {
            /*
             * L'algorithme de 'Segment.setResolution(...)' exige un syst�me de coordonn�es cart�sien.
             * Si le syst�me de coordonn�es sp�cifi� n'est pas cart�sien, on va utiliser arbitrairement
             * une projection st�r�ographique pour faire le calcul.
             */
            final GeographicCoordinateSystem geoCS = (GeographicCoordinateSystem) sourceCS;
            final Rectangle2D    bounds = getCachedBounds();
            final Point2D        center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
            final Ellipsoid   ellipsoid = geoCS.getHorizontalDatum().getEllipsoid();
            final Projection projection = new Projection("Generated", CARTESIAN, ellipsoid, center, null);
            targetCS = new ProjectedCoordinateSystem("Generated", geoCS, projection);
        }
        Segment.setResolution(data, getCoordinateTransformation(targetCS), resolution);
        clearCache(); // Clear everything in the cache.
    }

    /**
     * Compresse les donn�es de ce polyligne.    La compression peut entra�ner une l�g�re
     * baisse de la pr�cision des points. Avant d'effectuer la compression, cette m�thode
     * modifiera la r�solution du polyligne  en interpollant les points � une distance de
     * <code>dx&nbsp;+&nbsp;factor*std</code>, ou <var>dx</var> est la r�solution moyenne
     * du polyligne ({@link #getResolution})  et  <var>std</var> est la d�viation standard
     * de cette r�solution.
     *
     * @param  factor Facteur contr�lant la baisse de r�solution.  Les valeurs �lev�es
     *         d�ciment davantage de points, ce qui r�duit d'autant la consommation de
     *         m�moire. Ce facteur est g�n�ralement positif, mais il peut aussi �tre 0
     *         o� m�me l�g�rement n�gatif.
     * @return Un pourcentage estimant la baisse de r�solution. Par exemple la valeur 0.2
     *         indique que la distance moyenne entre deux points a augment� d'environ 20%.
     * @throws TransformException Si une erreur est survenue lors d'une projection cartographique.
     */
    public synchronized float compress(final float factor) throws TransformException
    {
        final Statistics stats = Segment.getResolution(data, coordinateTransform);
        if (stats!=null)
        {
            final double resolution = stats.mean();
            final double newResolution = resolution + factor*stats.standardDeviation(false);
            if (newResolution > 0)
            {
                setResolution(newResolution);
                data = Segment.freeze(data, false, true); // Apply the compression algorithm
                return (float) (1-(resolution/getResolution()));
            }
            data = Segment.freeze(data, false, false); // No compression
        }
        return 0;
    }

    /**
     * Returns a polyline approximatively equals to this polyline clipped to the specified bounds.
     * The clip is only approximative  in that the resulting polyline may extends outside the clip
     * area. However, it is garanted that the resulting polyline contains at least all the interior
     * of the clip area.
     *
     * If this method can't performs the clip, or if it believe that it doesn't worth to do a clip,
     * it returns <code>this</code>. If this polyline doesn't intersect the clip area, then this method
     * returns <code>null</code>. Otherwise, a new polyline is created and returned. The new polyline
     * will try to share as much internal data as possible with <code>this</code> in order to keep
     * memory footprint low.
     *
     * @param  clipper An object containing the clip area.
     * @return <code>null</code> if this polyline doesn't intersect the clip, <code>this</code>
     *         if no clip has been performed, or a new clipped polyline otherwise.
     */
    final Polygon getClipped(final Clipper clipper)
    {
        final Rectangle2D clip = clipper.setCoordinateSystem(getSourceCS());
        final Rectangle2D bounds = getDataBounds();
        if (clip.contains(bounds))
        {
            return this;
        }
        if (!clip.intersects(bounds))
        {
            return null;
        }
        /*
         * Selon toutes apparences, le polyligne n'est ni compl�tement � l'int�rieur
         * ni compl�tement en dehors de <code>clip</code>. Il faudra donc se resoudre
         * � faire une v�rification plus pouss�e (et plus couteuse).
         */
        final Polygon clipped=clipper.getClipped(this, new Segment.Iterator(data, null));
        if (clipped!=null)
        {
            if (Segment.equals(data, clipped.data))
            {
                return this;
            }
            if (bounds!=null)
            {
                clipped.bounds = new Rectangle2D.Float();
                Rectangle2D.intersect(bounds, clip, clipped.bounds);
            }
        }
        return clipped;
    }

    /**
     * Returns the string to be used as the tooltip for the given location.
     * If there is no such tooltip, returns <code>null</code>.
     *
     * @param  point Coordinates (usually mouse coordinates). Must be
     *         specified in this polyline's coordinate system
     *         (as returned by {@link #getCoordinateSystem}).
     * @return The tooltip text for the given location,
     *         or <code>null</code> if there is none.
     */
    public String getToolTipText(final Point2D point)
    {
        final String name=getName(null);
        return (name!=null && interiorSign!=UNKNOW && contains(point)) ? name : null;
    }

    /**
     * Return a copy of all coordinates of this polyline.
     * Coordinates are (x,y) or (longitude,latitude) pairs.
     * This method never return <code>null</code>, but may
     * return an array of length 0 if no data are available.
     *
     * @param  dest Tableau o� m�moriser les donn�es. Si ce tableau
     *         a exactement la longueur n�cessaire, il sera utilis�
     *         et retourn�. Sinon, cet argument sera ignor� et un
     *         nouveau tableau sera cr��. Cet argument peut �tre nul.
     * @param  subSampling D�cimation � effectuer. La valeur 1 n'effectue
     *         aucune d�cimation.  La valeur 2 ne retient qu'un point sur
     *         2, etc.
     * @return Tableau dans lequel furent m�moris�es les donn�es. Ce
     *         sera <code>dest</code> s'il avait exactement la longueur
     *         n�cessaire, ou un nouveau tableau sinon.
     */
    public synchronized float[] toArray(float[] dest, final int subSampling)
    {
        dest = Segment.toArray(data, dest, subSampling);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try
        {
            transform.transform(dest, 0, dest, 0, dest.length/2);
        }
        catch (TransformException exception)
        {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("toArray", exception);
        }
        return dest;
    }

    /**
     * Returns a hash value for this polyline.
     */
    public synchronized int hashCode()
    {return Segment.hashCode(data);}

    /**
     * Compare the specified object with this polyline for equality.
     */
    public synchronized boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final Polygon that = (Polygon) object;
            return                  this.interiorSign    ==   that.interiorSign         &&
                   Utilities.equals(this.coordinateTransform, that.coordinateTransform) &&
                     Segment.equals(this.data,                that.data);
        }
        else return false;
    }

    /**
     * Return a clone of this polyline. The clone has a deep copy semantic,
     * i.e. any change to the current polyline (including adding new points)
     * will not affect the clone,  and vis-versa   (any change to the clone
     * will not affect the current polyline). However, the two polylines will
     * share many internal structures in such a way that memory consumption
     * for polyline's clones should be kept low.
     */
    public synchronized Polygon clone()
    {
        final Polygon polyline = (Polygon) super.clone();
        polyline.data = Segment.clone(data); // Take an immutable view of 'data'.
        return polyline;
    }

    /**
     * Efface toutes les informations qui �taient conserv�es dans une cache interne.
     * Cette m�thode peut �tre appel�e lorsque l'on sait que ce polyligne ne sera plus
     * utilis� avant un certain temps. Elle ne cause la perte d'aucune information,
     * mais rendra la prochaine utilisation de ce polyligne plus lente (le temps que
     * les caches internes soient reconstruites,  apr�s quoi le polyligne retrouvera
     * sa vitesse normale).
     */
    final synchronized void clearCache()
    {
        cache      = null;
        bounds     = null;
        dataBounds = null;
        resolution = Float.NaN;
    }

    /**
     * M�thode appel�e lorsqu'une erreur inatendue est survenue.
     *
     * @param  method Nom de la m�thode dans laquelle est survenu l'exception.
     * @param  exception L'exception survenue.
     * @throws IllegalPathStateException syst�matiquement relanc�e.
     */
    static void unexpectedException(final String method, final TransformException exception)
    {
        Segment.unexpectedException("Polygon", method, exception);
        final IllegalPathStateException e=new IllegalPathStateException(exception.getLocalizedMessage());
        e.initCause(exception);
        throw e;
    }

    /**
     * Write all point coordinates to the specified stream.
     * This method is usefull for debugging purpose.
     *
     * @param  out The destination stream, or <code>null</code> for the standard output.
     * @param  locale Desired locale, or <code>null</code> for a default one.
     * @throws IOException If an error occured while writting to the destination stream.
     */
    public void print(final Writer out, final Locale locale) throws IOException
    {print(new String[]{getName(locale)}, new Collection<Point2D>[]{getPoints()}, out, locale);}

    /**
     * Write side-by-side all point coordinates of many polylines.
     * This method is usefull for checking the result of a coordinate
     * transformation; one could write side-by-side the original and
     * transformed polylines. Note that this method may require unicode
     * support for proper output.
     *
     * @param  polygons The set of polylines. Polylines may have different length.
     * @param  out The destination stream, or <code>null</code> for the standard output.
     * @param  locale Desired locale, or <code>null</code> for a default one.
     * @throws IOException If an error occured while writting to the destination stream.
     */
    public static void print(final Polygon[] polygons, final Writer out, final Locale locale) throws IOException
    {
        final String[]              titles = new String[polygons.length];
        final Collection<Point2D>[] arrays = new Collection<Point2D>[polygons.length];
        for (int i=0; i<polygons.length; i++)
        {
            final Polygon polygon = polygons[i];
            titles[i] = polygon.getName(locale);
            arrays[i] = polygon.getPoints();
        }
        print(titles, arrays, out, locale);
    }

    /**
     * Write side-by-side all points from arbitrary collections.
     * Note that this method may require unicode support for proper output.
     *
     * @param  titles The column's titles. Should have the same length than <code>points</code>.
     * @param  points Array of points collections. Collections may have different size.
     * @param  out The destination stream, or <code>null</code> for the standard output.
     * @param  locale Desired locale, or <code>null</code> for a default one.
     * @throws IOException If an error occured while writting to the destination stream.
     */
    public static void print(final String[] titles, final Collection<Point2D>[] points, Writer out, Locale locale) throws IOException
    {
        if (locale == null) locale = Locale.getDefault();
        if (out    == null)    out = Console.getWriter(System.out);

        final int            width = 8; // Columns width.
        final int        precision = 3; // Significant digits.
        final String     separator = "  \u2502  "; // Vertical bar.
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final NumberFormat  format = NumberFormat.getNumberInstance(locale);
        final FieldPosition  dummy = new FieldPosition(0);
        final StringBuffer  buffer = new StringBuffer();
        format.setMinimumFractionDigits(precision);
        format.setMaximumFractionDigits(precision);
        format.setGroupingUsed(false);

        final Iterator<Point2D>[] iterators = new Iterator<Point2D>[points.length];
        for (int i=0; i<points.length; i++)
        {
            if (i!=0) out.write(separator);
            int length=0;
            if (titles[i]!=null)
            {
                length=titles[i].length();
                final int spaces = Math.max(width-length/2, 0);
                out.write(Utilities.spaces(spaces));
                out.write(titles[i]);
                length += spaces;
            }
            out.write(Utilities.spaces(1+2*width-length));
            iterators[i]=points[i].iterator();
        }
        out.write(lineSeparator);
        boolean hasNext; do
        {
            hasNext=false;
            buffer.setLength(0);
            for (int i=0; i<iterators.length; i++)
            {
                if (i!=0) buffer.append(separator);
                final Iterator<Point2D> it = iterators[i];
                final boolean        hasPt = it.hasNext();
                final Point2D        point = (hasPt) ? it.next() : null;
                boolean xy=true; do
                {
                    final int start = buffer.length();
                    if (point!=null) format.format(xy ? point.getX() : point.getY(), buffer, dummy);
                    buffer.insert(start, Utilities.spaces(width-(buffer.length()-start)));
                    if (xy) buffer.append('\u00A0'); // No-break space
                }
                while (!(xy = !xy));
                hasNext |= hasPt;
            }
            if (!hasNext) break;
            buffer.append(lineSeparator);
            out.write(buffer.toString());
        }
        while (hasNext);
    }




    /**
     * This interface defines the method required by any object that
     * would like to be a renderer for polygons in an {@link Isoline}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static interface Renderer
    {
        /**
         * Draw or fill a polygon. The rendering is usually done with <code>graphics.draw(polygon)</code>
         * or <code>graphics.fill(polygon)</code>. This method may change the paint and stroke attributes
         * of <code>graphics</code> before to perform the rendering. However, it should not make any change
         * to <code>polygon</code> since this method may be invoked with arguments internal to some objects,
         * for performance raisons.
         *
         * @param graphics The graphics context.
         * @param polygon  The polygon to draw. This object should not be changed.
         */
        public abstract void drawPolygon(final Graphics2D graphics, final Polygon polygon);
    }
}
