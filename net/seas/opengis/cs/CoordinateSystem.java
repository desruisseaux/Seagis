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
package net.seas.opengis.cs;

// Units and coordinates
import javax.units.Unit;
import net.seas.opengis.pt.Envelope;


/**
 * Base interface for all coordinate systems.
 * A coordinate system is a mathematical space, where the elements of
 * the space are called positions.  Each position is described by a list
 * of numbers.  The length of the list corresponds to the dimension of
 * the coordinate system.  So in a 2D coordinate system each position is
 * described by a list containing 2 numbers.
 * <br><br>
 * However, in a coordinate system, not all lists of numbers correspond
 * to a position - some lists may be outside the domain of the coordinate
 * system.  For example, in a 2D Lat/Lon coordinate system, the list (91,91)
 * does not correspond to a position.
 * <br><br>
 * Some coordinate systems also have a mapping from the mathematical space
 * into locations in the real world.  So in a Lat/Lon coordinate system, the
 * mathematical position (lat, long) corresponds to a location on the surface
 * of the Earth.  This mapping from the mathematical space into real-world
 * locations is called a Datum.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_CoordinateSystem
 */
public abstract class CoordinateSystem extends NamedObject
{
    /**
     * Construct a coordinate system.
     */
    protected CoordinateSystem()
    {}

    /**
     * Dimension of the coordinate system.
     */
    public abstract int getDimension();

    /**
     * Gets axis details for dimension within coordinate system.
     * Each dimension in the coordinate system has a corresponding axis.
     *
     * @param dimension Zero based index of axis.
     */
    public abstract AxisInfo getAxis(int dimension);

    /**
     * Gets units for dimension within coordinate system.
     * Each dimension in the coordinate system has corresponding units.
     *
     * @param dimension Zero based index of axis.
     */
    public abstract Unit getUnits(int dimension);

    /**
     * Gets default envelope of coordinate system.
     * Coordinate systems which are bounded should return the minimum bounding
     * box of their domain.  Unbounded coordinate systems should return a box
     * which is as large as is likely to be used.  For example, a (lon,lat)
     * geographic coordinate system in degrees should return a box from
     * (-180,-90) to (180,90), and a geocentric coordinate system could return
     * a box from (-r,-r,-r) to (+r,+r,+r) where r is the approximate radius
     * of the Earth.
     */
    public abstract Envelope getDefaultEnvelope();
}
