/*
 * OpenGIS implementation in Java
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
package net.seas.opengis.cv;

// Coordinate systems
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cs.CoordinateSystem;


// Images
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

// Collections
import java.util.List;
import java.util.Vector;
import javax.media.jai.PropertySource;
import javax.media.jai.PropertySourceImpl;

// Miscellaneous
import net.seas.util.XArray;
import java.awt.geom.Rectangle2D;


/**
 * Base class of all coverage type.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public abstract class Coverage implements PropertySource
{
    /**
     * The default constructor.
     */
    public Coverage()
    {}

    /**
     * Returns the coordinate system. This specifies the coordinate
     * system used when accessing a coverage or grid coverage with the “evaluate” methods.
     * It is also the coordinate system of the coordinates used with the math
     * transform {@link net.seas.opengis.gc.GridGeometry#gridToCoordinateSystem
     * gridToCoordinateSystem()}. This coordinate system is usually different than
     * the grid coordinate system of the grid. A grid coverage can be accessed
     * (re-projected) with new coordinate system with the
     * {@link net.seas.opengis.gp.GridCoverageProcessor} component.
     * In this case, a new instance of a grid coverage is created.
     *
     * @return The coordinate system, or <code>null</code> if this coverage
     *         does not have an associated coordinate system.
     *
     * @see net.seas.opengis.gc.GridGeometry#gridToCoordinateSystem
     * @see net.seas.opengis.gp.GridCoverageProcessor#move
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Returns The bounding box for the coverage domain in coordinate
     * system coordinates. May be null if this coverage has no associated
     * coordinate system.
     */
    public abstract Envelope getEnvelope();

    /**
     * The names of each dimension in the coverage. Typically
     * these names are “x”, “y”, “z” and “t”. Grid coverages are typically 2D (x,y)
     * while other coverages may be 3D (x,y,z) or 4D (x,y,z,t). The number of
     * dimensions of the coverage is the number of entries in the list of dimension
     * names.
     */
    public abstract String[] getDimensionNames();

    /**
     * Retrieve sample dimension information for the coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension. A coverage must have at least one sample dimension.
     */
    public abstract List<SampleDimension> getSampleDimensions() throws IndexOutOfBoundsException;

    /**
     * Return a sequence of boolean values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * nearest neighbor. The coordinate system of the point is the same as the grid
     * coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract boolean[] evaluate(CoordinatePoint coord, boolean[] dest) throws PointOutsideCoverageException;

    /**
     * Return a sequence of unsigned byte values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default
     * interpolation type used when accessing grid values for points which fall
     * between grid cells is nearest neighbor. The coordinate system of the
     * point is the same as the grid coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract short[] evaluate(CoordinatePoint coord, short[] dest) throws PointOutsideCoverageException;

    /**
     * Return a sequence of integer values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default
     * interpolation type used when accessing grid values for points which fall
     * between grid cells is nearest neighbor. The coordinate system of the
     * point is the same as the grid coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract int[] evaluate(CoordinatePoint coord, int[] dest) throws PointOutsideCoverageException;

    /**
     * Return an sequence of double values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * nearest neighbor. The coordinate system of the point is the same as the grid coverage
     * coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public abstract double[] evaluate(CoordinatePoint coord, double[] dest) throws PointOutsideCoverageException;

    /**
     * TODO
     */
    public abstract RenderableImage getRenderableImage();

    /**
     * Returns the list of metadata keywords for a coverage. If no metadata
     * is available, the array will be empty (but not <code>null</code>).
     * The default implementation always returns an empty array.
     */
    public String[] getPropertyNames() // getMetadataNames()
    {return new String[0];}

    /**
     * Returns an array of strings recognized as names that begin with the
     * supplied prefix. If no property names match, an empty array will be
     * returned. The comparison is done in a case-independent manner.
     */
    public String[] getPropertyNames(final String prefix)
    {
        int count=0;
        final int length = prefix.length();
        final String[] names = getPropertyNames();
        for (int i=0; i<names.length; i++)
        {
            final String name = names[i];
            if (name.length() >= length  &&  prefix.equalsIgnoreCase(name.substring(0, length)))
            {
                names[count++] = name;
            }
        }
        return XArray.resize(names, count);
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned
     */
    public Class getPropertyClass(final String propertyName)
    {return null;}

    /**
     * Retrieve the metadata value for a given metadata name. If the metadata name
     * is not recognized, {@link Image#UndefinedProperty} will be returned.
     * The default implementation always returns <code>null</code>.
     *
     * @param name Metadata keyword for which to retrieve metadata.
     */
    public Object getProperty(final String name) // getMetadataValue(String)
    {return Image.UndefinedProperty;}

    /**
     * TODO
     */
    private abstract class Renderable extends PropertySourceImpl implements RenderableImage
    {
        /**
         * The envelope as a {@link Rectangle2D}.
         */
        final Rectangle2D envelope;

        /**
         * Default constructor.
         *
         * @throws IllegalStateException if the coverage is not two-dimensional.
         */
        public Renderable() throws IllegalStateException
        {
            super(null, Coverage.this);
            envelope = getEnvelope().toRectangle2D();
        }

        /**
         * Returns <code>null</code> to indicate
         * that no information is available.
         */
        public Vector getSources()
        {return null;}

        /**
         * Returns true if successive renderings with the same arguments
         * may produce different results. It is always safe to return true.
         */
        public boolean isDynamic()
        {return true;}

        /**
         * Gets the width in user coordinate space.
         */
        public float getWidth()
        {return (float)envelope.getWidth();}

        /**
         * Gets the height in user coordinate space.
         */
        public float getHeight()
        {return (float)envelope.getHeight();}

        /**
         * Gets the minimum X coordinate of the rendering-independent image data.
         */
        public float getMinX()
        {return (float)envelope.getX();}

        /**
         * Gets the minimum Y coordinate of the rendering-independent image data.
         */
        public float getMinY()
        {return (float)envelope.getY();}
    }
}
