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


/**
 * Base class of all coverage type.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public abstract class Coverage
{
    /**
     * The default constructor.
     */
    public Coverage()
    {}

    /**
     * Returns the coordinate system. This specifies the coordinate system used
     * when accessing a coverage or grid coverage with the “evaluate” methods.
     * It is also the coordinate system of the coordinates used with the
     * math transform. This coordinate system is usually different than
     * the grid coordinate system of the grid. A grid coverage can be accessed
     * (re-projected) with new coordinate system with the
     * {@link net.seas.opengis.gp.GridCoverageProcessor} component. In this case,
     * a new instance of a grid coverage is created.
     *
     * @return The coordinate system, or <code>null</code> if this coverage
     *         does not have an associated coordinate system.
     *
     * @see net.seas.opengis.gc.GridGeometry#gridToCoordinateSystem
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Returns The bounding box for the coverage domain in coordinate system
     * coordinates. May be null if this coverage has no associated coordinate
     * system.
     */
    public abstract Envelope getEnvelope();

    /**
     * The names of each dimension in the coverage. Typically these
     * names are “x”, “y”, “z” and “t”. Grid coverages are typically
     * 2D (x, y) while other coverages may be 3D (x, y, z) or 4D
     * (x, y, z, t). The number of dimensions of the coverage is the
     * number of entries in the list of dimension names.
     */
    public abstract String[] getDimensionNames();

    /**
     * The number of sample dimensions in the coverage.
     * For grid coverages, a sample dimension is a band.
     */
    public abstract int getNumSampleDimensions();

    /**
     * Retrieve sample dimension information for the coverage. For a grid coverage,
     * a sample dimension is a band. The sample dimension information include such
     * things as description, data type of the value (bit, byte, integer…), the no
     * data values, minimum and maximum values and a color table if one is associated
     * with the dimension. A coverage must have at least one sample dimension.
     */
    public abstract SampleDimension getSampleDimension(int index) throws IndexOutOfBoundsException;

    /**
     * Return a sequence of boolean values for a given point in the coverage. A value
     * for each sample dimension is included in the sequence. The default interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * nearest neighbor. The coordinate system of the point is the same as the grid
     * coverage coordinate system.
     */
    public abstract boolean[] evaluateAsBoolean(CoordinatePoint coord) throws PointOutsideCoverageException;

    /**
     * Return a sequence of unsigned byte values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default
     * interpolation type used when accessing grid values for points which fall
     * between grid cells is nearest neighbor. The coordinate system of the
     * point is the same as the grid coverage coordinate system.
     */
    public abstract short[] evaluateAsByte(CoordinatePoint coord) throws PointOutsideCoverageException;

    /**
     * Return a sequence of integer values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The default
     * interpolation type used when accessing grid values for points which fall
     * between grid cells is nearest neighbor. The coordinate system of the
     * point is the same as the grid coverage coordinate system.
     */
    public abstract int[] evaluateAsInteger(CoordinatePoint coord) throws PointOutsideCoverageException;

    /**
     * Return an sequence of double values for a given point in the coverage. A value
     * for each sample dimension is included in the sequence. The default interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * nearest neighbor. The coordinate system of the point is the same as the grid coverage
     * coordinate system.
     */
    public abstract double[] evaluateAsDouble(CoordinatePoint coord) throws PointOutsideCoverageException;
}
