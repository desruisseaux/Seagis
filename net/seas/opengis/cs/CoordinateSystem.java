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

// OpenGIS dependencies
import org.opengis.cs.CS_Unit;
import org.opengis.cs.CS_AxisInfo;
import org.opengis.pt.PT_Envelope;
import org.opengis.cs.CS_CoordinateSystem;

// Units and coordinates
import java.util.Map;
import javax.units.Unit;
import net.seas.opengis.pt.Envelope;

// Miscellaneous
import net.seas.util.XClass;
import java.rmi.RemoteException;


/**
 * Base class for all coordinate systems.
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
public abstract class CoordinateSystem extends Info
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -8188440674216625687L;

    /**
     * Construct a coordinate system.
     *
     * @param name The coordinate system name.
     */
    public CoordinateSystem(final String name)
    {super(name);}

    /**
     * Construct a coordinate system.
     *
     * @param properties The set of properties.
     */
    CoordinateSystem(final Map<String,Object> properties)
    {super(properties);}

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
     * <br><br>
     * The default implementation returns an envelope with infinite bounds.
     */
    public Envelope getDefaultEnvelope()
    {return new Envelope(getDimension());}

    /**
     * Returns an OpenGIS interface for this coordinate
     * system. The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid too early class loading of OpenGIS interface.
     */
    Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link CoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    class Export extends Info.Export implements CS_CoordinateSystem
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Dimension of the coordinate system.
         */
        public int getDimension() throws RemoteException
        {return CoordinateSystem.this.getDimension();}

        /**
         * Gets axis details for dimension within coordinate system.
         */
        public CS_AxisInfo getAxis(final int dimension) throws RemoteException
        {return adapters.export(CoordinateSystem.this.getAxis(dimension));}

        /**
         * Gets units for dimension within coordinate system.
         */
        public CS_Unit getUnits(final int dimension) throws RemoteException
        {return adapters.export(CoordinateSystem.this.getUnits(dimension));}

        /**
         * Gets default envelope of coordinate system.
         */
        public PT_Envelope getDefaultEnvelope() throws RemoteException
        {return adapters.PT.export(CoordinateSystem.this.getDefaultEnvelope());}
    }
}
