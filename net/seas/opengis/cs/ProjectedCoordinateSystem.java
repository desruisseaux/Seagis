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
import org.opengis.cs.CS_Projection;
import org.opengis.cs.CS_ProjectedCoordinateSystem;
import org.opengis.cs.CS_GeographicCoordinateSystem;

// Coordinates
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;
import net.seas.opengis.pt.CoordinatePoint;

// Miscellaneous
import java.util.Map;
import javax.units.Unit;
import java.awt.geom.Point2D;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import java.rmi.RemoteException;


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
     * Default axis info for longitude.
     */
    private static final AxisInfo EAST = new AxisInfo("x", AxisOrientation.EAST);

    /**
     * Default axis info for latitude.
     */
    private static final AxisInfo NORTH = new AxisInfo("y", AxisOrientation.NORTH);

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
     * Creates a projected coordinate system using WGS84 datum.
     * Projected coordinates will be in meters, <var>x</var> values
     * increasing east and <var>y</var> values increasing north.
     *
     * @param  name Name to give new object.
     * @param  projection Projection from geographic to projected coordinate system.
     */
    public ProjectedCoordinateSystem(final String name, final Projection projection)
    {this(name, GeographicCoordinateSystem.WGS84, projection);}

    /**
     * Creates a projected coordinate system using the specified geographic
     * system. Projected coordinates will be in meters, <var>x</var> values
     * increasing east and <var>y</var> values increasing north.
     *
     * @param  name Name to give new object.
     * @param  gcs Geographic coordinate system to base projection on.
     * @param  projection Projection from geographic to projected coordinate system.
     */
    public ProjectedCoordinateSystem(final String name, final GeographicCoordinateSystem gcs, final Projection projection)
    {this(name, gcs, projection, Unit.METRE, EAST, NORTH);}

    /**
     * Creates a projected coordinate system using a projection object.
     *
     * @param  name Name to give new object.
     * @param  gcs Geographic coordinate system to base projection on.
     * @param  projection Projection from geographic to projected coordinate system.
     * @param  unit Linear units of created PCS.
     * @param  axis0 Details of 0th ordinates in created PCS coordinates.
     * @param  axis1 Details of 1st ordinates in created PCS coordinates.
     */
    public ProjectedCoordinateSystem(final String name, final GeographicCoordinateSystem gcs, final Projection projection, final Unit unit, final AxisInfo axis0, final AxisInfo axis1)
    {
        super(name, gcs.getHorizontalDatum(), axis0, axis1);
        ensureNonNull("gcs",        gcs);
        ensureNonNull("projection", projection);
        ensureNonNull("unit",       unit);
        ensureLinearUnit(unit);
        this.gcs        = gcs;
        this.projection = projection;
        this.unit       = unit;
    }

    /**
     * Creates a projected coordinate system using a projection object.
     *
     * @param  properties The set of properties.
     * @param  gcs Geographic coordinate system to base projection on.
     * @param  projection Projection from geographic to projected coordinate system.
     * @param  unit Linear units of created PCS.
     * @param  axis0 Details of 0th ordinates in created PCS coordinates.
     * @param  axis1 Details of 1st ordinates in created PCS coordinates.
     */
    ProjectedCoordinateSystem(final Map<String,Object> properties, final GeographicCoordinateSystem gcs, final Projection projection, final Unit unit, final AxisInfo axis0, final AxisInfo axis1)
    {
        super(properties, gcs.getHorizontalDatum(), axis0, axis1);
        this.gcs        = gcs;
        this.projection = projection;
        this.unit       = unit;
        // Accept null values.
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
     * Gets linear unit. This convenience is equivalent to
     * <code>{@link #getUnits getUnits}(0)</code> or
     * <code>{@link #getUnits getUnits}(1)</code>.
     */
    public Unit getLinearUnit()
    {return unit;}

    /**
     * Gets units for dimension within coordinate system.
     * This angular unit is the same for all axis.
     *
     * @param dimension Zero based index of axis.
     */
    public Unit getUnits(final int dimension)
    {
        if (dimension>=0 && dimension<getDimension()) return getLinearUnit();
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
                   XClass.equals(this.unit,       that.unit);
        }
        return false;
    }

    /**
     * Returns an OpenGIS interface for this projected coordinate
     * system. The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid too early class loading of OpenGIS interface.
     */
    final Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link ProjectedCoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends HorizontalCoordinateSystem.Export implements CS_ProjectedCoordinateSystem
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Returns the GeographicCoordinateSystem.
         */
        public CS_GeographicCoordinateSystem getGeographicCoordinateSystem() throws RemoteException
        {return adapters.export(ProjectedCoordinateSystem.this.getGeographicCoordinateSystem());}

        /**
         * Returns the LinearUnits.
         */
        public CS_LinearUnit getLinearUnit() throws RemoteException
        {return (CS_LinearUnit) adapters.export(ProjectedCoordinateSystem.this.getLinearUnit());}

        /**
         * Gets the projection.
         */
        public CS_Projection getProjection() throws RemoteException
        {return adapters.export(ProjectedCoordinateSystem.this.getProjection());}
    }
}
