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
import org.opengis.cs.CS_LinearUnit;
import org.opengis.cs.CS_VerticalDatum;
import org.opengis.cs.CS_VerticalCoordinateSystem;

// Miscellaneous
import java.util.Map;
import javax.units.Unit;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.ct.CoordinateTransformation;
import java.rmi.RemoteException;


/**
 * A one-dimensional coordinate system suitable for vertical measurements.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_VerticalCoordinateSystem
 */
public class VerticalCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 3670736517382316475L;

    /**
     * The vertical datum.
     */
    private final VerticalDatum datum;

    /**
     * Units used along the vertical axis.
     */
    private final Unit unit;

    /**
     * Axis details for vertical dimension within coordinate system.
     */
    private final AxisInfo axis;

    /**
     * Creates a vertical coordinate system from a datum and linear units.
     *
     * @param name  Name to give new object.
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param axis  Axis to use for new coordinate system.
     */
    public VerticalCoordinateSystem(final String name, final VerticalDatum datum, final Unit unit, final AxisInfo axis)
    {
        super(name);
        ensureNonNull("datum", datum);
        ensureNonNull("unit",  unit );
        ensureNonNull("axis",  axis );
        ensureLinearUnit(unit);
        this.datum = datum;
        this.unit  = unit;
        this.axis  = axis;
    }

    /**
     * Creates a vertical coordinate system from a datum and linear units.
     *
     * @param properties Properties to give new object.
     * @param datum      Datum to use for new coordinate system.
     * @param unit       Units to use for new coordinate system.
     * @param axis       Axis to use for new coordinate system.
     */
    VerticalCoordinateSystem(final Map<String,String> properties, final VerticalDatum datum, final Unit unit, final AxisInfo axis)
    {
        super(properties);
        ensureNonNull("datum", datum);
        ensureNonNull("unit",  unit );
        ensureNonNull("axis",  axis );
        ensureLinearUnit(unit);
        this.datum = datum;
        this.unit  = unit;
        this.axis  = axis;
    }

    /**
     * Returns the dimension of this coordinate system, which is 1.
     */
    public int getDimension()
    {return 1;}

    /**
     * Gets the vertical datum, which indicates the measurement method.
     */
    public VerticalDatum getVerticalDatum()
    {return datum;}

    /**
     * Gets axis details for vertical dimension within coordinate system.
     *
     * @param dimension Zero based index of axis.
     */
    public AxisInfo getAxis(final int dimension)
    {
        final int maxDim = getDimension();
        if (dimension>=0 && dimension<maxDim) return axis;
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Gets units for dimension within coordinate system. A vertical
     * coordinate system have only one axis, always at index 0.
     *
     * @param dimension Must be 0.
     */
    public Unit getUnits(final int dimension)
    {
        final int maxDim = getDimension();
        if (dimension>=0 && dimension<maxDim) return getVerticalUnit();
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Gets the units used along the vertical axis. This convenience
     * is equivalent to <code>{@link #getUnits getUnits}(0)</code>.
     */
    public Unit getVerticalUnit()
    {return unit;}

    /**
     * Gets default envelope for this coordinate system.
     */
    public Envelope getDefaultEnvelope()
    {return new Envelope(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);}

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final VerticalCoordinateSystem that = (VerticalCoordinateSystem) object;
            return XClass.equals(this.datum, that.datum) &&
                   XClass.equals(this.unit , that.unit ) &&
                   XClass.equals(this.axis , that.axis );
        }
        return false;
    }

    /**
     * Returns an OpenGIS interface for this vertical coordinate
     * system. The returned object is suitable for RMI use.
     */
    final CS_VerticalCoordinateSystem toOpenGIS()
    {return new Export();}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link VerticalCoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends CoordinateSystem.Export implements CS_VerticalCoordinateSystem
    {
        /**
         * Gets the vertical datum, which indicates the measurement method.
         */
        public CS_VerticalDatum getVerticalDatum() throws RemoteException
        {return Adapters.export(VerticalCoordinateSystem.this.getVerticalDatum());}

        /**
         * Gets the units used along the vertical axis.
         */
        public CS_LinearUnit getVerticalUnit() throws RemoteException
        {return (CS_LinearUnit) Adapters.export(VerticalCoordinateSystem.this.getVerticalUnit());}
    }
}
