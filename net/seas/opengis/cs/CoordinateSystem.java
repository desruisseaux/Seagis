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

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.Dimensioned;

// Units and coordinates
import java.util.Map;
import javax.units.Unit;

// Miscellaneous
import net.seas.util.XClass;
import net.seas.resources.Resources;
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
public abstract class CoordinateSystem extends Info implements Dimensioned
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
     * @param properties The set of properties (see {@link Info}).
     */
    CoordinateSystem(final Map<String,Object> properties)
    {super(properties);}

    /**
     * Make sure there is no axis among the same direction
     * (e.g. two north axis, or a east and a west axis).
     * This methods may be invoked from subclasses constructors.
     *
     * @param  type The datum type, or <code>null</code> if unknow.
     * @throws IllegalArgumentException if two axis have the same direction.
     */
    final void checkAxis(final DatumType type) throws IllegalArgumentException
    {
        final int  dimension = getDimension();
        for (int i=0; i<dimension; i++)
        {
            AxisOrientation check = getAxis(i).orientation;
            if (type!=null && !type.isCompatibleOrientation(check))
            {
                throw new IllegalArgumentException(Resources.format(Clé.ILLEGAL_AXIS_ORIENTATION¤1, check.getName(null)));
            }
            check = check.absolute();
            if (!check.equals(AxisOrientation.OTHER))
            {
                for (int j=i+1; j<dimension; j++)
                {
                    if (check.equals(getAxis(j).orientation.absolute()))
                    {
                        final String nameI = getAxis(i).orientation.getName(null);
                        final String nameJ = getAxis(j).orientation.getName(null);
                        throw new IllegalArgumentException(Resources.format(Clé.NON_ORTHOGONAL_AXIS¤2, nameI, nameJ));
                    }
                }
            }
        }
    }

    /**
     * Returns the dimension of the coordinate system.
     *
     * @see org.opengis.cs.CS_CoordinateSystem#getDimension()
     */
    public abstract int getDimension();

    /**
     * Gets axis details for dimension within coordinate system.
     * Each dimension in the coordinate system has a corresponding axis.
     *
     * @param dimension Zero based index of axis.
     *
     * @see org.opengis.cs.CS_CoordinateSystem#getAxis(int)
     */
    public abstract AxisInfo getAxis(int dimension);

    /**
     * Gets units for dimension within coordinate system.
     * Each dimension in the coordinate system has corresponding units.
     *
     * @param dimension Zero based index of axis.
     *
     * @see org.opengis.cs.CS_CoordinateSystem#getUnits(int)
     */
    public abstract Unit getUnits(int dimension);

    /**
     * If all dimensions use the same units, returns this
     * units. Otherwise, returns <code>null</code>.
     */
    final Unit getUnits()
    {
        Unit units = null;
        for (int i=getDimension(); --i>=0;)
        {
            final Unit check = getUnits(i);
            if (units==null) units=check;
            else if (!units.equals(check))
                return null;
        }
        return units;
    }

    /**
     * Returns the datum.
     */
    Datum getDatum()
    {return null;}

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
     *
     * @see org.opengis.cs.CS_CoordinateSystem#getDefaultEnvelope()
     */
    public Envelope getDefaultEnvelope()
    {return new Envelope(getDimension());}

    /**
     * Returns a string representation of this info.
     * @param the source (usually <code>this</code>).
     */
    final String toString(final Object source)
    {
        final StringBuffer  buffer = new StringBuffer(super.toString(source));
        final String lineSeparator = System.getProperty("line.separator", "\r");
        final int        dimension = getDimension();
        for (int i=0; i<dimension; i++)
        {
            buffer.append(lineSeparator);
            buffer.append("    [");
            buffer.append(i);
            buffer.append("]:");
            buffer.append(getAxis(i));
        }
        buffer.append(lineSeparator);
        return buffer.toString();
    }

    /**
     * Add more information inside the "[...]" part of {@link #toString}.
     */
    void addString(final StringBuffer buffer)
    {
        super.addString(buffer);
        final Datum datum = getDatum();
        if (datum!=null)
        {
            buffer.append(", ");
            buffer.append(datum);
        }
    }

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
