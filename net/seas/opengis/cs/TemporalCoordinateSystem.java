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

// Miscellaneous
import java.util.Map;
import javax.units.Unit;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import net.seas.opengis.pt.Envelope;


/**
 * <FONT COLOR="#FF6633">A one-dimensional coordinate system suitable for time measurements.</FONT>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class TemporalCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    //private static final long serialVersionUID = ?; // TODO

    /**
     * The temporal datum.
     */
    private final TemporalDatum datum;

    /**
     * Units used along the time axis.
     */
    private final Unit unit;

    /**
     * Axis details for time dimension within coordinate system.
     */
    private final AxisInfo axis;

    /**
     * Creates a temporal coordinate system from a datum and time units.
     *
     * @param name  Name to give new object.
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param axis  Axis to use for new coordinate system.
     */
    public TemporalCoordinateSystem(final String name, final TemporalDatum datum, final Unit unit, final AxisInfo axis)
    {
        super(name);
        this.datum = datum;
        this.unit  = unit;
        this.axis  = axis;
        ensureNonNull("datum", datum);
        ensureNonNull("unit",  unit );
        ensureNonNull("axis",  axis );
        ensureTimeUnit(unit);
        checkAxis(datum.getDatumType());
    }

    /**
     * Creates a temporal coordinate system from a datum and time units.
     *
     * @param properties The set of properties.
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param axis  Axis to use for new coordinate system.
     */
    TemporalCoordinateSystem(final Map<String,Object> properties, final TemporalDatum datum, final Unit unit, final AxisInfo axis)
    {
        super(properties);
        this.datum = datum;
        this.unit  = unit;
        this.axis  = axis;
        // Accept null values.
    }

    /**
     * Returns the dimension of this coordinate system, which is 1.
     */
    public final int getDimension()
    {return 1;}

    /**
     * Override {@link CoordinateSystem#getDatum()}.
     */
    final Datum getDatum()
    {return getTemporalDatum();}

    /**
     * Gets the temporal datum, which indicates the measurement method.
     */
    public TemporalDatum getTemporalDatum()
    {return datum;}

    /**
     * Gets axis details for temporal dimension within coordinate system.
     * A temporal coordinate system have only one axis, always at index 0.
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
     * Gets units for dimension within coordinate system.
     * A temporal coordinate system have only one unit,
     * always at index 0.
     *
     * @param dimension Must be 0.
     */
    public Unit getUnits(final int dimension)
    {
        final int maxDim = getDimension();
        if (dimension>=0 && dimension<maxDim) return unit;
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final TemporalCoordinateSystem that = (TemporalCoordinateSystem) object;
            return XClass.equals(this.datum, that.datum) &&
                   XClass.equals(this.unit , that.unit ) &&
                   XClass.equals(this.axis , that.axis );
        }
        return false;
    }
}
