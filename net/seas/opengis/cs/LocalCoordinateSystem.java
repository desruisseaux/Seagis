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
import org.opengis.cs.CS_LocalDatum;
import org.opengis.cs.CS_LocalCoordinateSystem;

// Units and coordinates
import javax.units.Unit;
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.CoordinatePoint;

// Miscellaneous
import java.util.Arrays;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import java.rmi.RemoteException;


/**
 * A local coordinate system, with uncertain relationship to the world.
 * In general, a local coordinate system cannot be related to other
 * coordinate systems. However, if two objects supporting this interface
 * have the same dimension, axes, units and datum then client code
 * is permitted to assume that the two coordinate systems are identical.
 * This allows several datasets from a common source (e.g. a CAD system)
 * to be overlaid.
 * In addition, some implementations of the Coordinate Transformation (CT)
 * package may have a mechanism for correlating local datums.
 * (E.g. from a database of transformations, which is created and
 * maintained from real-world measurements.)
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_LocalCoordinateSystem
 */
public class LocalCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 8506198734333409238L;

    /**
     * The local datum.
     */
    private final LocalDatum datum;

    /**
     * Units used along all axis.
     */
    private final Unit[] unit;

    /**
     * Axes details.
     */
    private final AxisInfo[] axes;

    /**
     * Creates a local coordinate system. The dimension of the local coordinate
     * system is determined by the size of the axis array.  All the axes will
     * have the same units.  If you want to make a coordinate system with mixed
     * units, then you can make a compound coordinate system from different local
     * coordinate systems.
     *
     * @param name  Name to give new object.
     * @param datum Local datum to use in created coordinate system.
     * @param unit  Units to use for all axes in created coordinate system.
     * @param axes  Axes to use in created coordinate system.
     */
    public LocalCoordinateSystem(final String name, final LocalDatum datum, final Unit unit, final AxisInfo[] axes)
    {
        super(name);
        ensureNonNull("datum", datum);
        ensureNonNull("unit",  unit );
        ensureNonNull("axes",  axes );
        this.datum = datum;
        this.unit  = new Unit[axes.length];
        this.axes  = (AxisInfo[])axes.clone();
        for (int i=0; i<this.axes.length; i++)
        {
            this.unit[i] = unit;
            ensureNonNull("axes", this.axes, i);
        }
    }

    /**
     * Wrap an OpenGIS coordinate system.
     *
     * @param  cs The OpenGIS coordinate system.
     * @throws RemoteException if a remote call failed.
     */
    LocalCoordinateSystem(final CS_LocalCoordinateSystem cs) throws RemoteException
    {
        super(cs);
        datum = Adapters.wrap(cs.getLocalDatum());
        axes  = new AxisInfo[cs.getDimension()];
        unit  = new Unit[axes.length];
        for (int i=0; i<axes.length; i++)
        {
            axes[i] = Adapters.wrap(cs.getAxis (i));
            unit[i] = Adapters.wrap(cs.getUnits(i));
            // Accept null value.
        }
    }

    /**
     * Gets the local datum.
     */
    public LocalDatum getLocalDatum()
    {return datum;}

    /**
     * Dimension of the coordinate system.
     */
    public int getDimension()
    {return axes.length;}

    /**
     * Gets axis details for dimension within coordinate system.
     *
     * @param dimension Zero based index of axis.
     */
    public AxisInfo getAxis(final int dimension)
    {return axes[dimension];}

    /**
     * Gets units for dimension within coordinate system.
     *
     * @param dimension Zero based index of axis.
     */
    public Unit getUnits(final int dimension)
    {return unit[dimension];}

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final LocalCoordinateSystem that = (LocalCoordinateSystem) object;
            return XClass.equals(this.datum, that.datum) &&
                   Arrays.equals(this.unit , that.unit ) &&
                   Arrays.equals(this.axes , that.axes );
        }
        return false;
    }

    /**
     * Returns an OpenGIS interface for this local coordinate
     * system. The returned object is suitable for RMI use.
     */
    final CS_LocalCoordinateSystem toOpenGIS()
    {return new Export();}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link LocalCoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends CoordinateSystem.Export implements CS_LocalCoordinateSystem
    {
        /**
         * Gets the local datum.
         */
        public CS_LocalDatum getLocalDatum() throws RemoteException
        {return Adapters.export(LocalCoordinateSystem.this.getLocalDatum());}
    }
}
