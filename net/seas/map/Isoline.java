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
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.TransformException;
import net.seagis.cs.ProjectedCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.resources.Utilities;

// Geometry and graphics
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;

// Graphics
import java.awt.Graphics2D;

// Collections
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import net.seas.util.XArray;

// Logging and resources
import java.util.logging.Level;
import java.util.logging.LogRecord;
import net.seas.resources.Resources;
import net.seas.resources.ResourceKeys;


/**
 * An isoline built from a set of polylines.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Isoline extends Contour
{
    /**
     * Num�ro de version pour compatibilit� avec des
     * bathym�tries enregistr�es sous d'anciennes versions.
     */
    private static final long serialVersionUID = 7006680587688349800L;

    /**
     * The value for this isoline. In the case
     * of isobath, the value is the altitude.
     */
    public final float value;

    /**
     * Syst�me de coordonn�es.
     */
    private CoordinateSystem coordinateSystem;

    /**
     * Ensemble de polygones constituant cet isoligne. Les �l�ments de
     * ce tableau peuvent �tre class�s de fa�on � am�liorer la qualit�
     * de l'affichage lorsqu'ils sont dessin�s du dernier au premier.
     */
    private Polygon polygons[];

    /**
     * Nombre d'�l�ments valides dans <code>polygons</code>.
     */
    private int polygonCount;

    /**
     * Indique si les polygones contenus dans le tableau <code>polygons</code>
     * ont �t� class�s. Si ce n'est pas le cas, le classement devrait �tre fait
     * avant de dessiner les polygones.
     */
    private boolean sorted;

    /**
     * Rectangle englobant compl�tement cet isoligne. Ce rectangle est
     * calcul� une fois pour toute et conserv�e dans une cache interne
     * pour acc�l�rer certaines v�rifications.
     */
    private Rectangle2D bounds;

    /**
     * Construct an initialy empty isoline.
     *
     * @param value The value for this isoline. In the case
     *        of isobath, the value is the altitude.
     * @param coordinateSystem The coordinate system to use for all
     *        points in this isoline, or <code>null</code> if unknow.
     */
    public Isoline(final float value, final CoordinateSystem coordinateSystem)
    {
        this.value = value;
        this.coordinateSystem = coordinateSystem;
    }

    /**
     * Construct an isoline with the same data than
     * the specified isoline. The new isoline will
     * have a copy semantic.
     */
    public Isoline(final Isoline isoline)
    {
        this.value            = isoline.value;
        this.coordinateSystem = isoline.coordinateSystem;
        this.polygonCount     = isoline.polygonCount;
        this.sorted           = isoline.sorted;
        this.bounds           = isoline.bounds;
        this.polygons         = new Polygon[polygonCount];
        for (int i=0; i<polygonCount; i++)
            polygons[i] = isoline.polygons[i].clone();
    }

    /**
     * Returns the polyline's coordinate system, or <code>null</code> if unknow.
     */
    public synchronized CoordinateSystem getCoordinateSystem()
    {return coordinateSystem;}

    /**
     * Set the isoline's coordinate system. Calling this method is equivalents
     * to reproject all polyline from the old coordinate system to the new one.
     *
     * @param  The new coordinate system. A <code>null</code> value reset the
     *         coordinate system given at construction time.
     * @throws TransformException If a transformation failed. In case of failure,
     *         the state of this object will stay unchanged (as if this method has
     *         never been invoked).
     */
    public synchronized void setCoordinateSystem(final CoordinateSystem coordinateSystem) throws TransformException
    {
        bounds = null;
        final CoordinateSystem oldCoordinateSystem = this.coordinateSystem;
        if (Utilities.equals(oldCoordinateSystem, coordinateSystem)) return;
        int i=polygonCount;
        try
        {
            while (--i>=0)
            {
                polygons[i].setCoordinateSystem(coordinateSystem);
            }
            this.coordinateSystem = coordinateSystem; // Do it last.
        }
        catch (TransformException exception)
        {
            /*
             * If a map projection failed, reset
             * to the original coordinate system.
             */
            while (++i < polygonCount)
            {
                try
                {
                    polygons[i].setCoordinateSystem(oldCoordinateSystem);
                }
                catch (TransformException unexpected)
                {
                    // Should not happen, since the old coordinate system is supposed to be ok.
                    Polygon.unexpectedException("setCoordinateSystem", unexpected);
                }
            }
            throw exception;
        }
    }

    /**
     * Determines whetever the isoline is empty.
     */
    public synchronized boolean isEmpty()
    {
        for (int i=polygonCount; --i>=0;)
            if (!polygons[i].isEmpty()) return false;
        return true;
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
            for (int i=polygonCount; --i>=0;)
            {
                final Polygon polygon = polygons[i];
                if (!polygon.isEmpty())
                {
                    final Rectangle2D polygonBounds=polygon.getBounds2D();
                    if (bounds==null) bounds=polygonBounds;
                    else bounds.add(polygonBounds);
                }
            }
            if (bounds==null)
            {
                bounds = new Rectangle2D.Float();
            }
        }
        return bounds;
    }

    /**
     * Return the bounding box of this isoline, including its possible
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
        final Rectangle rect = new Rectangle();
        rect.setRect(getCachedBounds()); // Perform the appropriate rounding.
        return rect;
    }

    /**
     * Indique si le point (<var>x</var>,<var>y</var>) sp�cifi� est � l'int�rieur
     * de cet isoligne. Les coordonn�es du point doivent �tre exprim�es selon le syst�me
     * de coordonn�es de l'isoligne, soit {@link #getCoordinateSystem()}. Cette m�thode
     * recherchera le plus petit polygone qui contient le polygone sp�cifi�, et retournera
     * <code>true</code> si ce point est une �l�vation (par exemple une �le) ou
     * <code>false</code> s'il est une d�pression (par exemple un lac).
     */
    public synchronized boolean contains(final double x, final double y)
    {
        if (getCachedBounds().contains(x,y))
        {
            if (!sorted) sort();
            for (int i=0; i<polygonCount; i++)
            {
                final Polygon polygon=polygons[i];
                if (polygon.contains(x,y))
                {
                    switch (polygon.getInteriorSign())
                    {
                        case Polygon.ELEVATION:  return true;
                        case Polygon.DEPRESSION: return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Indique si le point sp�cifi� sp�cifi�e est � l'int�rieur de cet isoligne. Les coordonn�es
     * du point doivent �tre exprim�es selon le syst�me de coordonn�es de l'isoligne, soit
     * {@link #getCoordinateSystem()}. Cette m�thode recherchera le plus petit polygone qui
     * contient le point sp�cifi�, et retournera <code>true</code> si ce polygone est une �l�vation
     * (par exemple une �le) ou <code>false</code> s'il est une d�pression (par exemple un lac).
     */
    public synchronized boolean contains(final Point2D point)
    {
        if (getCachedBounds().contains(point))
        {
            if (!sorted) sort();
            for (int i=0; i<polygonCount; i++)
            {
                final Polygon polygon=polygons[i];
                if (polygon.contains(point))
                {
                    switch (polygon.getInteriorSign())
                    {
                        case Polygon.ELEVATION:  return true;
                        case Polygon.DEPRESSION: return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * V�rifie si le rectangle sp�cifi� est enti�rement compris dans cet isoligne.
     * Les coordonn�es du rectangle doivent �tre exprim�es selon le syst�me de
     * coordonn�es de l'isoligne, soit {@link #getCoordinateSystem()}. Cette
     * m�thode recherchera le plus petit polygone qui contient le rectangle sp�cifi�,
     * et retournera <code>true</code> si ce polygone est une �l�vation (par exemple
     * une �le) ou <code>false</code> s'il est une d�pression (par exemple un lac).
     */
    public synchronized boolean contains(final Rectangle2D rect)
    {
        if (getCachedBounds().contains(rect))
        {
            if (!sorted) sort();
            for (int i=0; i<polygonCount; i++)
            {
                final Polygon polygon=polygons[i];
                switch (polygon.getInteriorSign())
                {
                    case Polygon.ELEVATION:  if (polygon.contains  (rect)) return true;  break;
                    case Polygon.DEPRESSION: if (polygon.intersects(rect)) return false; break;
                }
            }
        }
        return false;
    }

    /**
     * V�rifie si la forme sp�cifi�e est enti�rement compris dans cet isoligne.
     * Les coordonn�es de la forme doivent �tre exprim�es selon le syst�me de
     * coordonn�es de l'isoligne, soit {@link #getCoordinateSystem()}. Cette
     * m�thode recherchera le plus petit polygone qui contient la forme sp�cifi�e,
     * et retournera <code>true</code> si ce polygone est une �l�vation (par exemple
     * une �le) ou <code>false</code> s'il est une d�pression (par exemple un lac).
     */
    public synchronized boolean contains(final Shape shape)
    {
        if (getCachedBounds().contains(shape.getBounds2D()))
        {
            if (!sorted) sort();
            for (int i=0; i<polygonCount; i++)
            {
                final Polygon polygon=polygons[i];
                switch (polygon.getInteriorSign())
                {
                    case Polygon.ELEVATION:  if (polygon.contains  (shape)) return true;  break;
                    case Polygon.DEPRESSION: if (polygon.intersects(shape)) return false; break;
                }
            }
        }
        return false;
    }

    /**
     * Test if the specified rectangle intercept with
     * the interior of a polylines of this isoline.
     */
    public synchronized boolean intersects(final Rectangle2D rect)
    {
        if (getCachedBounds().intersects(rect))
        {
            if (!sorted) sort();
            for (int i=0; i<polygonCount; i++)
            {
                final Polygon polygon=polygons[i];
                switch (polygon.getInteriorSign())
                {
                    case Polygon.ELEVATION:  if (polygon.intersects(rect)) return true;  break;
                    case Polygon.DEPRESSION: if (polygon.contains  (rect)) return false; break;
                }
            }
        }
        return false;
    }

    /**
     * Test if the specified shape intercept with
     * the interior of a polylines of this isoline.
     */
    public synchronized boolean intersects(final Shape shape)
    {
        if (getCachedBounds().intersects(shape.getBounds2D()))
        {
            if (!sorted) sort();
            for (int i=0; i<polygonCount; i++)
            {
                final Polygon polygon=polygons[i];
                switch (polygon.getInteriorSign())
                {
                    case Polygon.ELEVATION:  if (polygon.intersects(shape)) return true;  break;
                    case Polygon.DEPRESSION: if (polygon.contains  (shape)) return false; break;
                }
            }
        }
        return false;
    }

    /**
     * Returns the string to be used as the tooltip for the given location.
     * If there is no such tooltip, returns <code>null</code>. The default
     * implementation search for a polygon's tooltip at the given location.
     *
     * @param  point Coordinates (usually mouse coordinates). Must be
     *         specified in this isoline's coordinate system
     *         (as returned by {@link #getCoordinateSystem}).
     * @return The tooltip text for the given location,
     *         or <code>null</code> if there is none.
     */
    public synchronized String getToolTipText(final Point2D point)
    {
        if (getCachedBounds().contains(point))
        {
            if (!sorted) sort();
            for (int i=0; i<polygonCount; i++)
            {
                final String name=polygons[i].getToolTipText(point);
                if (name!=null) return name;
            }
        }
        return null;
    }

    /**
     * Trace cette isoligne dans le graphique sp�cifi�. Cette m�thode
     * est � peu pr�s �quivalente � <code>graphics.draw(this)</code>
     * ou <code>graphics.fill(this)</code>, mais peut �tre plus rapide
     * en raison de l'utilisation de caches internes.
     *
     * @param  graphics Graphiques dans lequel dessiner cet isoligne.
     * @param  resolution R�solution approximative d�sir�e � l'affichage,
     *         selon les unit�s de {@link Polygon#getResolution}. Une
     *         r�solution plus grossi�re (un nombre plus �lev�) peut
     *         rendre le tra�age plus rapide au d�triment de la qualit�.
     * @param  renderer An optional renderer for polygons,
     *         or <code>null</code> for the default rendering.
     */
    public synchronized void paint(final Graphics2D graphics, final float resolution, final Polygon.Renderer renderer)
    {
        final Shape clip = graphics.getClip();
        if (clip.intersects(getCachedBounds()))
        {
            if (!sorted) sort();
            int numPoints = 0;
            int numDecimated = 0;
            for (int i=polygonCount; --i>=0;)
            {
                final Polygon polygon = polygons[i];
                synchronized (polygon)
                {
                    if (clip.intersects(polygon.getCachedBounds()))
                    {
                        final int n=polygon.setDrawingDecimation(Math.round(resolution/polygon.getResolution()));
                        numDecimated += n/polygon.getDrawingDecimation();
                        numPoints    += n;
                        if (renderer!=null)
                        {
                            renderer.drawPolygon(graphics, polygon);
                        }
                        else
                        {
                            if (polygon.isClosed())
                                graphics.fill(polygon);
                            else
                                graphics.draw(polygon);
                        }
                    }
                }
            }
            if (numPoints != 0)
            {
                // FINER is the default level for entering, returning, or throwing an exception.
                final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINER,
                                         ResourceKeys.REBUILD_CACHE_ARRAY_$3,
                                         new Float(value),
                                         new Integer(numPoints),
                                         new Double((double)numDecimated / (double)numPoints));
                record.setSourceClassName ("Isoline");
                record.setSourceMethodName("paint");
                LOGGER.log(record);
            }
        }
    }

    /**
     * Returns a path iterator for this isoline.
     */
    public synchronized PathIterator getPathIterator(final AffineTransform transform)
    {return new net.seas.map.PathIterator(getPolygonList(true).iterator(), transform);}

    /**
     * Returns a path iterator for this isoline.
     */
    public PathIterator getPathIterator(final AffineTransform transform, final double flatness)
    {return getPathIterator(transform);}

    /**
     * Return the number of points describing this isobath.
     */
    public synchronized int getPointCount()
    {
        int total=0;
        for (int i=polygonCount; --i>=0;)
            total += polygons[i].getPointCount();
        return total;
    }

    /**
     * Returns the set of {@link Polygon} objects in this isoline. If possible,
     * the set's iterator will return continents last and small lakes within the
     * continents fist. Therefore, drawing done in a reverse order should yield
     * good results.
     * <br><br>
     * The order in which the iterator returns {@link Polygon} objects make it
     * convenient to find the smallest island or lake that a given point contains.
     * For example:
     *
     * <blockquote><pre>
     * &nbsp;public Polygon getSmallestIslandAt(double x, double y)
     * &nbsp;{
     * &nbsp;    final Iterator it=isobath.getPolygons().iterator();
     * &nbsp;    while (it.hasNext())
     * &nbsp;    {
     * &nbsp;        final Polygon �le=(Polygon) it.next();
     * &nbsp;        if (�le.contains(x,y)) return �le;
     * &nbsp;    }
     * &nbsp;    return null;
     * &nbsp;}
     * </pre></blockquote>
     */
    public synchronized Set<Polygon> getPolygons()
    {return new LinkedHashSet<Polygon>(getPolygonList(false));}

    /**
     * Returns the set of polygons as a list. This method is faster than
     * {@link #getPolygons} and is optimized for {@link #getPathIterator}.
     *
     * @param  reverse <code>true</code> for reversing order (i.e. returning
     *         big continents first, and small lakes or islands last). This
     *         reverse order is appropriate for drawing, while the "normal"
     *         order is more appropriate for searching a polygon.
     * @return The set of polygons. This set will be ordered if possible.
     */
    private List<Polygon> getPolygonList(final boolean reverse)
    {
        if (!sorted) sort();
        final List<Polygon> list = new ArrayList<Polygon>(polygonCount);
        for (int i=polygonCount; --i>=0;)
        {
            final Polygon polygon = polygons[i].clone();
            polygon.setDrawingDecimation(1);
            list.add(polygon);
        }
        if (!reverse)
        {
            // Elements was inserted in reverse order.
            // (remind: this method is optimized for getPathIterator)
            Collections.reverse(list);
        }
        return list;
    }

    /**
     * Returns the set of polygons containing the specified point.
     *
     * @param  point A coordinate expressed according {@link #getCoordinateSystem}.
     * @return The set of polygons under the specified point.
     */
    public synchronized Set<Polygon> getPolygons(final Point2D point)
    {
        if (getCachedBounds().contains(point))
        {
            if (!sorted) sort();
            final Polygon[] copy = new Polygon[polygonCount];
            System.arraycopy(polygons, 0, copy, 0, polygonCount);
            return new FilteredSet(copy, point, null, null);
        }
        return new FilteredSet(new Polygon[0], point, null, null);
        // TODO: On devrait retourner Collections.EMPTY_SET,
        //       mais �a provoque une erreur de compilation.
    }

    /**
     * Returns the set of polygons containing or intersecting the specified shape.
     *
     * @param  shape A shape with coordinates expressed according {@link #getCoordinateSystem}.
     * @param  intersects <code>false</code> to search for polygons containing <code>shape</code>,
     *         or <code>true</code> to search for polygons intercepting <code>shape</code>.
     * @return The set of polygons containing or intersecting the specified shape.
     */
    public synchronized Set<Polygon> getPolygons(final Shape shape, final boolean intersects)
    {
        if (shape.intersects(getCachedBounds()))
        {
            if (!sorted) sort();
            final Polygon[] copy = new Polygon[polygonCount];
            System.arraycopy(polygons, 0, copy, 0, polygonCount);
            if (intersects)
                return new FilteredSet(copy, null, null, shape);
            else
                return new FilteredSet(copy, null, shape, null);
        }
        return new FilteredSet(new Polygon[0], null, shape, null);
        // TODO: On devrait retourner Collections.EMPTY_SET,
        //       mais �a provoque une erreur de compilation.
    }

    /**
     * Ajoute des points � cet isobath. Les donn�es doivent �tre �crites sous forme de
     * paires (<var>x</var>,<var>y</var>) dans le syst�me de coordonn�es de cet isoligne
     * ({@link #getCoordinateSystem}). Les <code>NaN</code> seront consid�r�s comme des
     * trous; aucune ligne ne joindra les points entre deux <code>NaN</code>.
     */
    public synchronized void add(final float[] array)
    {
        final Polygon[] toAdd = Polygon.getInstances(array, coordinateSystem);
        for (int i=0; i<toAdd.length; i++)
            if (!toAdd[i].isEmpty())
                addImpl(toAdd[i]);
    }

    /**
     * Add a polyline to this isoline.
     *
     * @param  toAdd Polyline to add.
     * @throws TransformException if the specified polygon can't
     *         be transformed in this isoline's coordinate system.
     */
    public synchronized void add(Polygon toAdd) throws TransformException
    {
        if (toAdd!=null)
        {
            toAdd = toAdd.clone();
            if (coordinateSystem!=null)
            {
                toAdd.setCoordinateSystem(coordinateSystem);
            }
            else
            {
                coordinateSystem = toAdd.getCoordinateSystem();
                if (coordinateSystem!=null)
                    setCoordinateSystem(coordinateSystem);
            }
            addImpl(toAdd);
        }
    }

    /**
     * Add a polyline to this isoline. This method do not clone
     * the polygon and doesn't set the coordinate system.
     */
    private void addImpl(final Polygon toAdd)
    {
        if (polygons==null)
            polygons=new Polygon[16];
        if (polygonCount >= polygons.length)
        {
            polygons = XArray.resize(polygons, polygonCount+Math.min(polygonCount, 256));
        }
        polygons[polygonCount++] = toAdd;
        sorted = false;
        bounds = null;
    }

    /**
     * Remove a polyline from this isobath.
     * @return <code>true</code> if the polyline has been removed.
     */
    public synchronized boolean remove(final Polygon toRemove)
    {
        boolean removed=false;
        for (int i=polygonCount; --i>=0;)
        {
            if (polygons[i].equals(toRemove))
            {
                remove(i);
                removed=true;
            }
        }
        return removed;
        // No change to sorting order.
    }

    /**
     * Remote the polyline at the specified index.
     */
    private void remove(final int index)
    {
        bounds = null;
        System.arraycopy(polygons, index+1, polygons, index, polygonCount-(index+1));
        polygons[--polygonCount]=null;
    }

    /**
     * Returns the isoline's mean resolution. This resolution is the mean distance between
     * every pair of consecutive points in this isoline (ignoring "extra" points used for
     * drawing a border, if there is one). This method try to returns linear units (usually
     * meters) no matter if the coordinate systems is actually a {@link ProjectedCoordinateSystem}
     * or a {@link GeographicCoordinateSystem}.
     *
     * @return The mean resolution, or {@link Float#NaN} if this isoline doesn't have any point.
     */
    public synchronized float getResolution()
    {
        int    sumCount      = 0;
        double sumResolution = 0;
        for (int i=polygonCount; --i>=0;)
        {
            final Polygon polygon = polygons[i];
            final float resolution=polygon.getResolution();
            if (!Float.isNaN(resolution))
            {
                final int count = polygon.getPointCount();
                sumResolution += count*(double)resolution;
                sumCount      += count;
            }
        }
        return (float) (sumResolution/sumCount);
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
        bounds = null;
        for (int i=polygonCount; --i>=0;)
        {
            final Polygon polygon = polygons[i];
            polygon.setResolution(resolution);
            if (polygon.isEmpty()) remove(i);
        }
    }

    /**
     * Compresse les donn�es de cet isoligne.
     *
     * @param  factor Facteur contr�lant la baisse de r�solution.  Les valeurs �lev�es
     *         d�ciment davantage de points, ce qui r�duit d'autant la consommation de
     *         m�moire. Ce facteur est g�n�ralement positif, mais il peut aussi �tre 0
     *         ou m�me l�g�rement n�gatif.
     * @return Un pourcentage estimant la baisse de r�solution. Par exemple la valeur 0.2
     *         indique que la distance moyenne entre deux points a augment� d'environ 20%.
     * @throws TransformException Si une erreur est survenue lors d'une projection cartographique.
     */
    public synchronized float compress(final float factor) throws TransformException
    {
        polygons = XArray.resize(polygons, polygonCount);

        bounds            = null;
        int    sumCount   = 0;
        double sumPercent = 0;
        for (int i=polygonCount; --i>=0;)
        {
            final Polygon polygon = polygons[i];
            final float percent=polygon.compress(factor);
            if (polygon.isEmpty()) remove(i);
            if (!Float.isNaN(percent))
            {
                final int count = polygon.getPointCount();
                sumPercent += count*percent;
                sumCount   += count;
            }
        }
        return (float) (sumPercent/sumCount);
        // No change to sorting order.
    }

    /**
     * Returns an isoline approximatively equals to this isoline clipped to the specified bounds.
     * The clip is only approximative  in that  the resulting isoline may extends outside the clip
     * area. However, it is garanted that the resulting isoline contains at least all the interior
     * of the clip area.
     *
     * If this method can't performs the clip, or if it believe that it doesn't worth to do a clip,
     * it returns <code>this</code>. If this isoline doesn't intersect the clip area, then this method
     * returns <code>null</code>. Otherwise, a new isoline is created and returned. The new isoline
     * will try to share as much internal data as possible with <code>this</code> in order to keep
     * memory footprint low.
     *
     * @param  clipper An object containing the clip area.
     * @return <code>null</code> if this isoline doesn't intersect the clip, <code>this</code>
     *         if no clip has been performed, or a new clipped isoline otherwise.
     */
    final Isoline getClipped(final Clipper clipper)
    {
        final Rectangle2D clipRegion = clipper.setCoordinateSystem(coordinateSystem);
        final Polygon[] clipPolygons = new Polygon[polygonCount];
        int         clipPolygonCount = 0;
        boolean              changed = false;
        /*
         * Clip all polygons, discarding
         * polygons outside the clip.
         */
        for (int i=0; i<polygonCount; i++)
        {
            final Polygon toClip  = polygons[i];
            final Polygon clipped = toClip.getClipped(clipper);
            if (clipped!=null && !clipped.isEmpty())
            {
                clipPolygons[clipPolygonCount++] = clipped;
                if (!toClip.equals(clipped)) changed=true;
            }
            else changed = true;
        }
        if (changed)
        {
            /*
             * If at least one polygon has been clipped, returns a new isoline.
             * Note: we set the new bounds to the clip region. It may be bigger
             * than computed bounds, but it is needed for optimal behaviour of
             * {@link RenderingContext#clip}. Clipped isolines should not be
             * public anyways (except for very short time).
             */
             final Isoline isoline = new Isoline(value, coordinateSystem);
             isoline.polygons      = XArray.resize(clipPolygons, clipPolygonCount);
             isoline.polygonCount  = clipPolygonCount;
             isoline.bounds        = clipRegion;
             isoline.setName(super.getName(null));
             return isoline;
        }
        else return this;
    }

    /**
     * Returns a hash value for this isoline.
     */
    public synchronized int hashCode()
    {
        int code = 4782135;
        for (int i=0; i<polygonCount; i++)
        {
            // Must be insensitive to order.
            code += polygons[i].hashCode();
        }
        return code;
    }

    /**
     * Compares the specified object with
     * this isoline for equality.
     */
    public synchronized boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final Isoline that = (Isoline) object;
            if (Float.floatToIntBits(this.value) == Float.floatToIntBits(that.value) &&
                this.polygonCount == that.polygonCount)
            {
                // Compare ignoring order. Note: we don't call any synchronized
                // methods on 'that' in order to avoid dead lock.
                return getPolygons().containsAll(that.getPolygonList(true));
            }
        }
        return false;
    }

    /**
     * Return a copy of this isoline. The clone has a deep copy semantic,
     * but will shares many internal arrays with the original isoline.
     */
    public synchronized Isoline clone()
    {
        final Isoline isoline=(Isoline) super.clone();
        isoline.polygons=new Polygon[polygonCount];
        for (int i=isoline.polygons.length; --i>=0;)
        {
            isoline.polygons[i] = polygons[i].clone();
        }
        return isoline;
    }

    /**
     * Efface toutes les informations qui �taient conserv�es dans une cache interne.
     * Cette m�thode peut �tre appel�e lorsque l'on sait que cet isoligne ne sera plus
     * utilis� avant un certain temps. Elle ne cause la perte d'aucune information,
     * mais rendra les prochaines utilisations de cet isoligne plus lentes (le temps
     * que les caches internes soient reconstruites, apr�s quoi l'isoligne retrouvera
     * sa vitesse normale).
     */
    final void clearCache()
    {
        bounds = null;
        for (int i=polygonCount; --i>=0;)
            polygons[i].clearCache();
    }

    /**
     * Classe les polygones de fa�on � faire appara�tre les petites �les
     * ou les lacs en premiers, et les gros continents en derniers.
     */
    private void sort()
    {
        // TODO
    }




    /**
     * The set of polygons under a point. The check of inclusion
     * or intersection will be performed only when needed.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class FilteredSet extends AbstractSet<Polygon>
    {
        /**
         * The polygons to check. This array must be a copy of
         * {@link Isoline#polygons}. It will be changed during
         * iteration: polygons that do not obey to condition
         * will be set to <code>null</code>.
         */
        final Polygon[] polygons;

        /**
         * The point to check for inclusion, or <code>null</code> if none.
         */
        private final Point2D point;

        /**
         * The shape to check for inclusion, or <code>null</code> if none.
         */
        private final Shape contains;

        /**
         * The shape to check for intersection, or <code>null</code> if none.
         */
        private final Shape intersects;

        /**
         * Index of the next polygon to check. All polygons
         * before this index are considered valid.
         */
        private int upper;

        /**
         * Construct a filtered set.
         *
         * @param polygons The polygon array. This array <strong>must be a copy</strong>
         *                 of {@link Isoline#polygons}. It must not be the original!
         */
        public FilteredSet(final Polygon[] polygons, final Point2D point, final Shape contains, final Shape intersects)
        {
            this.polygons   = polygons;
            this.point      = point;
            this.contains   = contains;
            this.intersects = intersects;
        }

        /**
         * Returns the index of the next valid polygon starting at of after the specified
         * index. If there is no polygon left, returns a number greater than or equals to
         * <code>polygons.length</code>. This method should be invoked with increasing
         * value of <code>from</code> only (values in random order are not supported).
         */
        final int next(int from)
        {
            while (from < polygons.length)
            {
                Polygon polygon = polygons[from];
                if (polygon!=null)
                {
                    if (from >= upper)
                    {
                        // This polygon has not been
                        // checked yet for validity.
                        upper = from+1;
                        if ((     point!=null && !polygon.contains  (point   )) ||
                            (  contains!=null && !polygon.contains  (contains)) ||
                            (intersects!=null && !polygon.intersects(intersects)))
                        {
                            polygons[from] = null;
                            continue;
                        }
                        polygon = polygon.clone();
                        polygon.setDrawingDecimation(1);
                        polygons[from] = polygon;
                    }
                    break;
                }
            }
            return from;
        }
        
        /**
         * Returns the number of elements in this collection.
         */
        public int size()
        {
            int count=0;
            for (int i=next(0); i<polygons.length; i=next(i+1)) count++;
            return count;
        }

        /**
         * Returns an iterator over the elements in this collection.
         */
        public Iterator<Polygon> iterator()
        {
            return new Iterator<Polygon>()
            {
                /** Index of the next valid polygon. */
                private int index = FilteredSet.this.next(0);

                /** Check if there is more polygons. */
                public boolean hasNext()
                {return index<polygons.length;}

                /** Returns the next polygon. */
                public Polygon next()
                {
                    if (index<polygons.length)
                    {
                        final Polygon next = polygons[index];
                        index = FilteredSet.this.next(index+1);
                        return next;
                    }
                    else throw new NoSuchElementException();
                }

                /** Unsupported operation. */
                public void remove()
                {throw new UnsupportedOperationException();}
            };
        }
    }
}