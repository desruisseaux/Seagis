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

// Coordinates
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.ct.CoordinateTransformation;

// Miscellaneous
import javax.units.Unit;
import net.seas.util.XClass;
import net.seas.resources.Resources;


/**
 * A coordinate system based on latitude and longitude. Some geographic
 * coordinate systems are <var>latitude</var>/<var>longiude</var>,  and
 * some are <var>longitude</var>/<var>latitude</var>.  You can find out
 * which this is by examining the axes. You should also check the angular
 * units, since not all geographic coordinate systems use degrees.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_GeographicCoordinateSystem
 */
public class GeographicCoordinateSystem extends HorizontalCoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -80914667908100423L;

    /**
     * The angular unit.
     */
    private final Unit unit;

    /**
     * The prime meridian.
     */
    private final PrimeMeridian meridian;

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
     * @param name      Name to give new object.
     * @param unit      Angular units for created coordinate system.
     * @param datum     Horizontal datum for created coordinate system.
     * @param meridian  Prime Meridian for created coordinate system.
     * @param axis0     Details of 0th ordinates.
     * @param axis1     Details of 1st ordinates.
     */
    protected GeographicCoordinateSystem(final String name, final Unit unit, final HorizontalDatum datum, final PrimeMeridian meridian, final AxisInfo axis0, final AxisInfo axis1)
    {
        super(name, datum);
        ensureNonNull("unit",     unit);
        ensureNonNull("meridian", meridian);
        ensureNonNull("axis0",    axis0);
        ensureNonNull("axis1",    axis1);
        this.unit     = unit;
        this.meridian = meridian;
        this.axis0    = axis0.clone();
        this.axis1    = axis1.clone();
    }

    /**
     * Gets axis details for dimension within coordinate system.
     *
     * @param dimension Zero based index of axis.
     */
    public AxisInfo getAxis(final int dimension)
    {
        switch (dimension)
        {
            case 0:  return axis0.clone();
            case 1:  return axis1.clone();
            default: throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
        }
    }

    /**
     * Gets units for dimension within coordinate system.
     * This angular unit is the same for all axis.
     *
     * @param dimension Zero based index of axis.
     */
    public Unit getUnits(final int dimension)
    {
        if (dimension>=0 && dimension<getDimension()) return unit;
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Returns the prime meridian.
     */
    public PrimeMeridian getPrimeMeridian()
    {return meridian;}

    /**
     * Gets default envelope of coordinate system.
     */
    public Envelope getDefaultEnvelope()
    {
        final int dimension = getDimension();
        final CoordinatePoint minCP = new CoordinatePoint(dimension);
        final CoordinatePoint maxCP = new CoordinatePoint(dimension);
        for (int i=0; i<dimension; i++)
        {
            double min, max;
            final Unit unit = getUnits(i);
            final AxisOrientation orientation = getAxis(i).orientation;
            if (AxisOrientation.NORTH.equals(orientation) ||
                AxisOrientation.SOUTH.equals(orientation))
            {
                min = Latitude.MIN_VALUE;
                max = Latitude.MAX_VALUE;
            }
            else if (AxisOrientation.EAST.equals(orientation) ||
                     AxisOrientation.WEST.equals(orientation))
            {
                min = Longitude.MIN_VALUE;
                max = Longitude.MAX_VALUE;
            }
            else
            {
                min = Double.NEGATIVE_INFINITY;
                max = Double.POSITIVE_INFINITY;
            }
            min = unit.convert(min, Unit.DEGREE);
            max = unit.convert(max, Unit.DEGREE);
            minCP.ord[i] = Math.min(min, max);
            maxCP.ord[i] = Math.max(min, max);
        }
        return new Envelope(minCP, maxCP);
    }

    /**
     * Gets the number of available conversions to WGS84 coordinates.
     */
    public int getNumConversionToWGS84()
    {return 0;}

    /**
     * Gets details on a conversion to WGS84.
     * Some geographic coordinate systems provide several transformations
     * into WGS84, which are designed to provide good accuracy in different
     * areas of interest.  The first conversion (with index=0) should
     * provide acceptable accuracy over the largest possible area of
     * interest.
     *
     * @param index Zero based index of conversion to fetch.
     * @throws RemoteException if a remote method call failed.
     */
    public WGS84ConversionInfo getWGS84ConversionInfo(final int index)
    {throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(index)));}

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final GeographicCoordinateSystem that = (GeographicCoordinateSystem) object;
            return XClass.equals(this.unit,     that.unit)     &&
                   XClass.equals(this.meridian, that.meridian) &&
                   XClass.equals(this.axis0,    that.axis0)    &&
                   XClass.equals(this.axis1,    that.axis1);
        }
        return false;
    }

    /**
     * Gets the transformation from this coordinate
     * system to the specified coordinate system.
     */
    CoordinateTransformation transformFrom(final CoordinateSystem system)
    {return transformTo(this);}
}
