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
 * A 2D cartographic coordinate system.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_ProjectedCoordinateSystem
 */
public class ProjectedCoordinateSystem extends HorizontalCoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -4074962239062277502L;

    /**
     * The angular unit.
     */
    private final Unit unit;

    /**
     * Geographic coordinate system to base projection on.
     */
    private final GeographicCoordinateSystem gcs;

    /**
     * projection Projection from geographic to projected coordinate system.
     */
    private final Projection projection;

    /**
     * Details of 0th ordinates.
     */
    private final AxisInfo axis0;

    /**
     * Details of 1th ordinates.
     */
    private final AxisInfo axis1;

    /**
     * Creates a projected coordinate system using a projection object.
     *
     * @param  name Name to give new object.
     * @param  gcs Geographic coordinate system to base projection on.
     * @param  projection Projection from geographic to projected coordinate system.
     * @param  unit Linear units of returned PCS.
     * @param  axis0 Details of 0th ordinates in returned PCS coordinates.
     * @param  axis1 Details of 1st ordinates in returned PCS coordinates.
     */
    protected ProjectedCoordinateSystem(final String name, final GeographicCoordinateSystem gcs, final Projection projection, final Unit unit, final AxisInfo axis0, final AxisInfo axis1)
    {
        super(name, gcs.getHorizontalDatum());
        ensureNonNull("gcs",        gcs);
        ensureNonNull("projection", projection);
        ensureNonNull("unit",       unit);
        ensureNonNull("axis0",      axis0);
        ensureNonNull("axis1",      axis1);
        this.gcs        = gcs;
        this.projection = projection;
        this.unit       = unit;
        this.axis0      = axis0.clone();
        this.axis1      = axis1.clone();
    }

    /**
     * Returns the geographic coordinate system.
     */
    public GeographicCoordinateSystem getGeographicCoordinateSystem()
    {return gcs;}

    /**
     * Gets the projection.
     */
    public Projection getProjection()
    {return projection;}

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
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final ProjectedCoordinateSystem that = (ProjectedCoordinateSystem) object;
            return XClass.equals(this.gcs,        that.gcs)        &&
                   XClass.equals(this.projection, that.projection) &&
                   XClass.equals(this.unit,       that.unit)       &&
                   XClass.equals(this.axis0,      that.axis0)      &&
                   XClass.equals(this.axis1,      that.axis1);
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
