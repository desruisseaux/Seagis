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

// Geometry
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;

// Collections
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import net.seas.util.XArray;

// Miscellaneous
import net.seas.util.XClass;


/**
 * Une ligne de contour.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Isoline extends Contour
{
    /**
     * Num�ro de version pour compatibilit� avec des
     * bathym�tries enregistr�es sous d'anciennes versions.
     * TODO: serialver
     */
    //private static final long serialVersionUID = 2828645016315459429L;

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
     * Construit un isoligne initialement vide.
     *
     * @param coordinateSystem Syst�me de coordonn�es de l'isoligne.
     */
    public Isoline(final CoordinateSystem coordinateSystem)
    {this.coordinateSystem = coordinateSystem;}

    /**
     * Retourne le syst�me de coordonn�es de cet isoligne.
     * Cette m�thode peut retourner <code>null</code> s'il
     * n'est pas connu.
     */
    public synchronized CoordinateSystem getCoordinateSystem()
    {return coordinateSystem;}

    /**
     * Sp�cifie le syst�me de coordonn�es dans lequel retourner les points de l'isoligne.
     * Appeller cette m�thode est �quivalent � projeter tous les points du contour de
     * l'ancien syst�me de coordonn�es vers le nouveau.
     *
     * @param  coordinateSystem Syst�me de coordonn�es dans lequel exprimer les points
     *         de l'isoligne. La valeur <code>null</code> restaurera le syst�me "natif".
     * @throws TransformException si une projection cartographique a �chou�e. Dans
     *         ce cas, cet isobath sera laiss� dans son ancien syst�me de coordonn�es.
     */
    public synchronized void setCoordinateSystem(final CoordinateSystem coordinateSystem) throws TransformException
    {
        bounds = null;
        final CoordinateSystem oldCoordinateSystem = this.coordinateSystem;
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
    private Rectangle2D getCachedBounds()
    {
        if (bounds==null)
        {
            bounds=new Rectangle2D.Float();
            for (int i=polygonCount; --i>=0;)
            {
                final Rectangle2D polygonBounds=polygons[i].getBounds2D();
                if (!polygonBounds.isEmpty()) bounds.add(polygonBounds);
            }
        }
        return bounds;
    }

    /**
     * Return the bounding box of this isoline.
     */
    public synchronized Rectangle2D getBounds2D()
    {return (Rectangle2D) getCachedBounds().clone();}

    /**
     * Return the bounding box of this isoline.
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
     * Retourne le texte � afficher dans une bulle lorsque la souris
     * tra�ne � la coordonn�e sp�cifi�e. L'impl�mentation par d�faut
     * retourne le nom du polygone qui se trouve sous la souris, ou
     * <code>null</code> s'il n'y en a pas ou si son nom est inconnu.
     *
     * @param point Coordonn�es point�es par la souris. Cette coordonn�es
     *        doit �tre exprim�e selon le syst�me de coordonn�es de cet
     *        isoligne ({@link #getCoordinateSystem}).
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
     * Retourne un it�rateur balayant les coordonn�es de cet isoligne.
     * Les points seront exprim�s selon le syst�me de coordonn�es de
     * cet isoligne, soit {@link #getCoordinateSystem()}.
     */
    public PathIterator getPathIterator(final AffineTransform transform)
    {return new net.seas.map.PathIterator(getPolygons().iterator(), transform);}

    /**
     * Retourne un it�rateur balayant les coordonn�es de cet isoligne.
     * Les points seront exprim�s selon le syst�me de coordonn�es de
     * cet isoligne, soit {@link #getCoordinateSystem()}.
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
    public synchronized Collection<Polygon> getPolygons()
    {
        if (!sorted) sort();
        final List<Polygon> array = new ArrayList<Polygon>(polygonCount);
        for (int i=polygonCount; --i>=0;)
        {
            final Polygon polygon = polygons[i].clone();
            polygon.setDrawingDecimation(1);
            array.set(i, polygon);
        }
        return array;
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
     * Add a polygon to this isoline.
     *
     * @param  toAdd Polylines to add.
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
     * Add a polygon to this isoline. This method do not clone
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
     * Remove a polylines from this isobath.
     * @return <code>true</code> if the polygon has been removed.
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
     * Remote the polygon at the specified index.
     */
    private void remove(final int index)
    {
        bounds = null;
        System.arraycopy(polygons, index+1, polygons, index, polygonCount-(index+1));
        polygons[--polygonCount]=null;
    }

    /**
     * Renvoie la r�solution moyenne de cet isoligne. Cette r�solution sera la distance moyenne
     * (en m�tres) entre deux points du polyligne, mais sans prendre en compte les "points de
     * bordure" (par exemple les points qui suivent le bord d'une carte plut�t que de repr�senter
     * une structure g�ographique r�elle).
     *
     * @return La r�solution moyenne en m�tres, ou {@link Float#NaN}
     *         si cet isoligne ne contient pas de points.
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
                sumResolution += count*resolution;
                sumCount      += count;
            }
        }
        return (float) (sumResolution/sumCount);
    }

    /**
     * Modify the resolution of this isobath. This method will proceed
     * by decimating the data of this isobath such that each point will
     * be separated from the previous point by a distance equal to the
     * value specified in the argument. This could be translated by
     * important memory economy if a large resolution is not necessary.
     *
     * @param  resolution Desired resolution (in metres).
     * @throws TransformException If a map projection failed.
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
     *         o� m�me l�g�rement n�gatif.
     * @return Un pourcentage estimant la baisse de r�solution. Par exemple la valeur 0.2
     *         indique que la distance moyenne entre deux points a augment� d'environ 20%.
     * @throws TransformException Si une erreur est survenue lors d'une projection cartographique.
     */
    public synchronized float compress(final float factor) throws TransformException
    {
        polygons = XArray.resize(polygons, polygonCount);

        bounds = null;
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
     * Retourne un isoligne contenant les points de  <code>this</code>  qui apparaissent dans le clip
     * sp�cifi�. Si aucun point de cet isoligne n'apppara�t � l'int�rieur de <code>clip</code>, alors
     * cette m�thode retourne <code>null</code>.    Si tous les points de cet isoligne apparaissent �
     * l'int�rieur de <code>clip</code>, alors cette m�thode retourne <code>this</code>. Sinon, cette
     * m�thode retourne un isoligne qui contiendra seulement les points qui apparaissent � l'int�rieur
     * de <code>clip</code>. Cet isoligne partagera les m�mes donn�es que <code>this</code> autant que
     * possible, de sorte que la consommation de m�moire devrait rester raisonable.
     *
     * @param  clip Coordonn�es de la r�gion � couper.
     * @return Isoligne �ventuellement coup�.
     */
    private final Isoline getClipped(final Clipper clipper)
    {
        final Polygon[] clipPolygons = new Polygon[polygonCount];
        int         clipPolygonCount = 0;
        boolean              changed = false;

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
             final Isoline isoline = new Isoline(coordinateSystem);
             isoline.setName(getName());
             isoline.polygons = XArray.resize(clipPolygons, clipPolygonCount);
             isoline.polygonCount = clipPolygonCount;
             return isoline;
        }
        else return this;
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
            if (this.polygonCount == that.polygonCount)
            {
                final Set<Polygon> set = new HashSet<Polygon>(polygonCount*2);
                for (int i=polygonCount; --i>=0;)
                {
                    set.add(polygons[i]);
                }
                return set.containsAll(that.getPolygons());
            }
        }
        return false;
    }

    /**
     * Return a copy of this isoline.
     */
    public synchronized Isoline clone()
    {
        final Isoline isoline=(Isoline) super.clone();
        isoline.polygons=new Polygon[polygonCount];
        if (isoline.polygons.length!=0)
        {
            for (int i=isoline.polygons.length; --i>=0;)
            {
                isoline.polygons[i] = isoline.polygons[i].clone();
            }
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
}
