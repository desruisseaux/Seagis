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
import org.opengis.cs.CS_HorizontalDatum;
import org.opengis.cs.CS_HorizontalCoordinateSystem;

// Miscellaneous
import java.util.Map;
import javax.units.Unit;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import java.rmi.RemoteException;


/**
 * A 2D coordinate system suitable for positions on the Earth's surface.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_HorizontalCoordinateSystem
 */
public abstract class HorizontalCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -9058204611188320317L;

    /**
     * The horizontal datum.
     */
    private final HorizontalDatum datum;

    /**
     * Details of 0th ordinates.
     */
    private final AxisInfo axis0;

    /**
     * Details of 1th ordinates.
     */
    private final AxisInfo axis1;

    /**
     * Construct a coordinate system.
     *
     * @param name  The coordinate system name.
     * @param datum The horizontal datum.
     * @param axis0 Details of 0th ordinates in created coordinate system.
     * @param axis1 Details of 1st ordinates in created coordinate system.
     */
    public HorizontalCoordinateSystem(final String name, final HorizontalDatum datum, final AxisInfo axis0, final AxisInfo axis1)
    {
        super(name);
        this.datum = datum;
        this.axis0 = axis0;
        this.axis1 = axis1;
        ensureNonNull("datum", datum);
        ensureNonNull("axis0", axis0);
        ensureNonNull("axis1", axis1);
        checkAxis(datum.getDatumType());
    }

    /**
     * Construct a coordinate system.
     *
     * @param properties The set of properties (see {@link Info}).
     * @param datum The horizontal datum.
     * @param axis0 Details of 0th ordinates in created coordinate system.
     * @param axis1 Details of 1st ordinates in created coordinate system.
     */
    HorizontalCoordinateSystem(final Map<String,Object> properties, final HorizontalDatum datum, final AxisInfo axis0, final AxisInfo axis1)
    {
        super(properties);
        this.datum = datum;
        this.axis0 = axis0;
        this.axis1 = axis1;
        // Accept null values
    }

    /**
     * Returns the dimension of this coordinate system, which is 2.
     *
     * @see org.opengis.cs.CS_HorizontalCoordinateSystem#getDimension()
     */
    public final int getDimension()
    {return 2;}

    /**
     * Override {@link CoordinateSystem#getDatum()}.
     */
    final Datum getDatum()
    {return getHorizontalDatum();}

    /**
     * Returns the horizontal datum.
     *
     * @see org.opengis.cs.CS_HorizontalCoordinateSystem#getHorizontalDatum()
     */
    public HorizontalDatum getHorizontalDatum()
    {return datum;}

    /**
     * Gets axis details for dimension within coordinate system.
     *
     * @param dimension Zero based index of axis.
     *
     * @see org.opengis.cs.CS_HorizontalCoordinateSystem#getAxis(int)
     */
    public AxisInfo getAxis(final int dimension)
    {
        switch (dimension)
        {
            case 0:  return axis0;
            case 1:  return axis1;
            default: throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
        }
    }

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final HorizontalCoordinateSystem that = (HorizontalCoordinateSystem) object;
            return XClass.equals(this.datum, that.datum) &&
                   XClass.equals(this.axis0, that.axis0) &&
                   XClass.equals(this.axis1, that.axis1);
        }
        return false;
    }

    /**
     * Returns an OpenGIS interface for this horizontal coordinate
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
     * Wrap a {@link HorizontalCoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    class Export extends CoordinateSystem.Export implements CS_HorizontalCoordinateSystem
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Returns the HorizontalDatum.
         */
        public CS_HorizontalDatum getHorizontalDatum() throws RemoteException
        {return adapters.export(HorizontalCoordinateSystem.this.getHorizontalDatum());}
    }
}
