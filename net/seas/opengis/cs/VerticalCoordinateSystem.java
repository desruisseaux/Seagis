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
import javax.units.Unit;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import net.seas.opengis.pt.Envelope;


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
     * Construct a coordinate system.
     *
     * @param name The coordinate system name.
     * @param datum The vertical datum.
     * @param unit Units to use for new coordinate system.
     * @param axis Axis to use for new coordinate system.
     */
    protected VerticalCoordinateSystem(final String name, final VerticalDatum datum, final Unit unit, final AxisInfo axis)
    {
        super(name);
        ensureNonNull("datum", datum);
        ensureNonNull("unit",  unit );
        ensureNonNull("axis",  axis );
        this.datum = datum;
        this.unit  = unit;
        this.axis  = axis.clone();
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
        if (dimension>=0 && dimension<maxDim) return axis.clone();
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Gets units for dimension within coordinate system. Each
     * dimension in the coordinate system has corresponding units.
     *
     * @param dimension Zero based index of axis.
     */
    public Unit getUnits(final int dimension)
    {
        final int maxDim = getDimension();
        if (dimension>=0 && dimension<maxDim) return getVerticalUnit();
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Gets the units used along the vertical axis. This convenience method
     * is equivalents to <code>{@link #getUnits getUnits}(0)</code>, since
     * vertical coordinate systems are one-dimensional.
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
    {return (object instanceof VerticalCoordinateSystem) && equals((VerticalCoordinateSystem)object);}

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    final boolean equals(final VerticalCoordinateSystem that)
    {
        if (super.equals(that))
        {
            return XClass.equals(this.datum, that.datum);
        }
        return false;
    }
}
