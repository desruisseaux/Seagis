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
import net.seagis.cs.Ellipsoid;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.ProjectedCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.ct.CoordinateTransformationFactory;
import net.seagis.ct.CoordinateTransformation;
import net.seagis.ct.TransformException;
import net.seagis.ct.CannotCreateTransformException;
import net.seagis.resources.Utilities;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Formatting
import java.text.Format;
import java.text.NumberFormat;
import java.text.FieldPosition;
import net.seagis.pt.Latitude;
import net.seagis.pt.Longitude;
import net.seagis.pt.AngleFormat;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Miscellaneous
import java.util.Locale;
import java.io.Serializable;
import net.seas.resources.Resources;


/**
 * A contour line. A contour line may be a single polygon ({@link Polygon})
 * or a set of polygons at the same altitude value ({@link Isoline}). This
 * class implements the {@link Shape} interface  for interoperability with
 * <A HREF="http://java.sun.com/products/java-media/2D/">Java2D</A>.   But
 * it provides also some more capabilities. For example, <code>contains</code>
 * and <code>intersects</code> methods accepts arbitrary shapes instead of
 * rectangles only. <code>Contour</code> objects can have arbitrary two-dimensional
 * coordinate systems, which can be changed dynamically (i.e. contours can
 * be reprojected). Futhermore, contours can compress and share their data
 * in order to reduce memory footprint.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class Contour implements Shape, Cloneable, Serializable
{
    /**
     * Numéro de version pour compatibilité avec des
     * bathymétries enregistrées sous d'anciennes versions.
     */
    private static final long serialVersionUID = 7579026572945453650L;

    /**
     * Objet à utiliser pour fabriquer des transformations.
     */
    static final CoordinateTransformationFactory TRANSFORMS = CoordinateTransformationFactory.getDefault();

    /**
     * The logger for map operations.
     */
    static final Logger LOGGER = Logger.getLogger("net.seas.map");
    static
    {
        net.seas.util.InterlineFormatter.init(LOGGER);
    }

    /**
     * Nom de ce contour.   Il s'agit en général d'un nom géographique, par exemple
     * "Île d'Anticosti" ou "Lac Supérieur". Ce champs peut être nul si ce polygone
     * ne porte pas de nom.
     */
    private String name;

    /**
     * Construct an empty contour.
     */
    public Contour()
    {}

    /**
     * Construct a contour with the same
     * data than the specified contour.
     */
    public Contour(final Contour contour)
    {this.name = contour.name;}

    /**
     * Set a default name for this contour. For example, a polygon
     * may have the name of a lake or an island. This name may be
     * <code>null</code> if this contour is unnamed.
     */
    public void setName(final String name)
    {this.name = name;}

    /**
     * Returns the localized name for this contour. The default
     * implementation ignore the locale and returns the last name
     * set by {@link #setName}.
     *
     * @param  locale The desired locale. If no name is available
     *         for this locale, a default locale will be used.
     * @return The contour's name, localized if possible.
     */
    public String getName(final Locale locale)
    {return name;}

    /**
     * Convenience method returning the {@link GeographicCoordinateSystem}'s ellipsoid.
     * If the coordinate system is not geographic (for example if it is an instance of
     * {@link ProjectedCoordinateSystem}), then this method returns <code>null</code>.
     * Use this method to determine how to compute distances. If <code>getEllipsoid()</code>
     * returns a non-null value, one can use {@link Ellipsoid#orthodromicDistance}.
     * Otherwise, one can use the Pythagoras's formula (assuming that the coordinate
     * system is cartesian).
     */
    public Ellipsoid getEllipsoid()
    {return Segment.getEllipsoid(getCoordinateSystem());}

    /**
     * Returns the contour's coordinate system,
     * or <code>null</code> if unknow.
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Set the contour's coordinate system. Calling this method is equivalents
     * to reproject all contour's points from the old coordinate system to the
     * new one.
     *
     * @param  The new coordinate system. A <code>null</code> value way reset
     *         some default coordinate system (usually the one that best fits
     *         some internal data).
     * @throws TransformException If a transformation failed. In case of failure,
     *         the state of this object will stay unchanged (as if this method has
     *         never been invoked).
     */
    public abstract void setCoordinateSystem(final CoordinateSystem coordinateSystem) throws TransformException;

    /**
     * Determines whetever this contour is empty.
     */
    public abstract boolean isEmpty();

    /**
     * Return the bounding box of this isoline. This methode returns
     * a direct reference to the internally cached bounding box. DO
     * NOT MODIFY!
     */
    Rectangle2D getCachedBounds()
    {return getBounds2D();} // To be overriden by subclasses.

    /**
     * Test if the interior of this contour entirely contains the given rectangle.
     * The rectangle's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public boolean contains(final double x, final double y, final double width, final double height)
    {return contains(new Rectangle2D.Double(x, y, width, height));}

    /**
     * Test if the interior of this contour entirely contains the given shape.
     * The shape's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public abstract boolean contains(final Shape shape);

    /**
     * Tests if the interior of the contour intersects the interior of a specified rectangle.
     * The rectangle's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public boolean intersects(final double x, final double y, final double width, final double height)
    {return intersects(new Rectangle2D.Double(x, y, width, height));}

    /**
     * Tests if the interior of the contour intersects the interior of a specified shape.
     * The shape's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public abstract boolean intersects(final Shape shape);

    /**
     * Returns the string to be used as the tooltip for the given location.
     * If there is no such tooltip, returns <code>null</code>. This method
     * is usually invoked as result of mouse events. Default implementation
     * returns the name that has been set with {@link #setName} if the
     * specified coordinates is contained inside this contour, or
     * <code>null</code> otherwise.
     *
     * @param  point Coordinates (usually mouse coordinates). Must be
     *         specified in this contour's coordinate system (as returned
     *         by {@link #getCoordinateSystem}).
     * @return The tooltip text for the given location, or <code>null</code>
     *         if there is none.
     */
    public String getToolTipText(final Point2D point)
    {return (name!=null && contains(point)) ? name : null;}

    /**
     * Return the number of points in this contour.
     */
    public abstract int getPointCount();

    /**
     * Returns the contour's mean resolution. This resolution is the mean distance between
     * every pair of consecutive points in this contour  (ignoring "extra" points used for
     * drawing a border, if there is one). Resolution's units are the same than {@link #getCoordinateSystem}'s
     * units, <strong>except</strong> if the later use angular units (this is the case for
     * {@link GeographicCoordinateSystem}). In the later case, this method will compute
     * orthodromic distances using the coordinate system's {@link Ellipsoid} (as returned
     * by {@link #getEllipsoid}). In other words, this method try to returns linear units
     * (usually meters) no matter if the coordinate systems is actually a
     * {@link ProjectedCoordinateSystem} or a {@link GeographicCoordinateSystem}.
     *
     * @return The mean resolution, or {@link Float#NaN} if this contour doesn't have any point.
     *         If {@link #getEllipsoid} returns a non-null value, the resolution is expressed in
     *         ellipsoid's axis units. Otherwise, resolution is expressed in coordinate system
     *         units.
     */
    public abstract float getResolution();

    /**
     * Set the contour's resolution. This method try to interpolate new points in such a way
     * that every point is spaced by exactly <code>resolution</code> units (usually meters)
     * from the previous one. Calling this method with a lower resolution may help to reduce
     * memory footprint if a high resolution is not needed (note that {@link Isoline#compress}
     * provides an alternative way to reduce memory footprint).
     * <br><br>
     * This method is irreversible. Invoking <code>setResolution</code> with a finner
     * resolution will increase memory consumption with no real resolution improvement.
     *
     * @param  resolution Desired resolution, in the same units than {@link #getResolution}.
     * @throws TransformException If some coordinate transformations were needed and failed.
     *         There is no guaranteed on contour's state in case of failure.
     */
    public abstract void setResolution(final double resolution) throws TransformException;

    /**
     * Returns a contour approximatively equals to this contour clipped to the specified bounds.
     * The clip is only approximative  in that  the resulting contour may extends outside the clip
     * area. However, it is garanted that the resulting contour contains at least all the interior
     * of the clip area.
     *
     * If this method can't performs the clip, or if it believe that it doesn't worth to do a clip,
     * it returns <code>this</code>. If this contour doesn't intersect the clip area, then this method
     * returns <code>null</code>. Otherwise, a new contour is created and returned. The new contour
     * will try to share as much internal data as possible with <code>this</code> in order to keep
     * memory footprint low.
     *
     * @param  clipper An object containing the clip area.
     * @return <code>null</code> if this contour doesn't intersect the clip, <code>this</code>
     *         if no clip has been performed, or a new clipped contour otherwise.
     */
    Contour getClipped(final Clipper clipper)
    {return this;}

    /**
     * Return a string representation of this contour for debugging purpose.
     * The returned string will look like
     * "<code>Polygon["Île Quelconque", 44°30'N-51°59'N  70°59'W-54°59'W (56 pts)]</code>".
     */
    public String toString()
    {
        final Format format;
        final Rectangle2D bounds=getBounds2D();
        Object minX,minY,maxX,maxY;
        if (getCoordinateSystem() instanceof GeographicCoordinateSystem)
        {
            minX=new Longitude(bounds.getMinX());
            minY=new Latitude (bounds.getMinY());
            maxX=new Longitude(bounds.getMaxX());
            maxY=new Latitude (bounds.getMaxY());
            format = new AngleFormat();
        }
        else
        {
            minX=new Float((float) bounds.getMinX());
            minY=new Float((float) bounds.getMinY());
            maxX=new Float((float) bounds.getMaxX());
            maxY=new Float((float) bounds.getMaxY());
            format=NumberFormat.getNumberInstance();
        }
        final String         name=getName(null);
        final FieldPosition dummy=new FieldPosition(0);
        final StringBuffer buffer=new StringBuffer(Utilities.getShortClassName(this));
        buffer.append('[');
        if (name!=null)
        {
            buffer.append('"');
            buffer.append(name);
            buffer.append("\", ");
        }
        format.format(minY, buffer, dummy).append('-' );
        format.format(maxY, buffer, dummy).append("  ");
        format.format(minX, buffer, dummy).append('-' );
        format.format(maxX, buffer, dummy).append(" (");
        buffer.append(getPointCount()); buffer.append(" pts)]");
        return buffer.toString();
    }

    /**
     * Returns an hash code for this contour. Subclasses should
     * overrides this method to provide a more appropriate value.
     */
    public int hashCode()
    {return (name!=null) ? name.hashCode() : 0;}

    /**
     * Compare the specified object with this contour for equality.
     * Default implementation tests if the two objects are instances
     * of the same class and compare their name. Subclasses should
     * overrides this method for checking contour's points.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && object.getClass().equals(getClass()))
        {
            return Utilities.equals(name, ((Contour) object).name);
        }
        else return false;
    }

    /**
     * Return a clone of this contour. The returned contour will have
     * a deep copy semantic. However, subclasses should overrides this
     * method in such a way that both contours will share as much internal
     * arrays as possible, even if they use differents coordinate systems.
     */
    public Contour clone()
    {
        try
        {
            return (Contour) super.clone();
        }
        catch (CloneNotSupportedException exception)
        {
            // Should never happen, since we are cloneable.
            InternalError e=new InternalError(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Retourne une transformation identitée pour le système de coordonnées
     * spécifié, ou <code>null</code> si <code>coordinateSystem</code> est nul.
     */
    static CoordinateTransformation getIdentityTransform(final CoordinateSystem coordinateSystem)
    {
        if (coordinateSystem!=null) try
        {
            return TRANSFORMS.createFromCoordinateSystems(coordinateSystem, coordinateSystem);
        }
        catch (CannotCreateTransformException exception)
        {
            // Should not happen; we are just asking for an identity transform!
            Utilities.unexpectedException("net.seas.map", "Contour", "getIdentityTransform", exception);
        }
        return null;
    }

    /**
     * Construct a transform from two coordinate systems.
     */
    static CoordinateTransformation createFromCoordinateSystems(final CoordinateSystem sourceCS,
                                                                final CoordinateSystem targetCS,
                                                                final String sourceClassName,
                                                                final String sourceMethodName) throws CannotCreateTransformException
    {
        final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE,
                                 Clé.INITIALIZING_TRANSFORMATION¤2, sourceCS, targetCS);
        record.setSourceClassName (sourceClassName);
        record.setSourceMethodName(sourceMethodName);
        LOGGER.log(record);
        return TRANSFORMS.createFromCoordinateSystems(sourceCS, targetCS);
    }
}
