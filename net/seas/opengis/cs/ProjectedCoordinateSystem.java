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
     * Creates a projected coordinate system using the specified geographic
     * system. Projected coordinates will be in meters, <var>x</var> values
     * increasing east and <var>y</var> values increasing north.
     *
     * @param  name Name to give new object.
     * @param  gcs Geographic coordinate system to base projection on.
     * @param  projection Projection from geographic to projected coordinate system.
     */
    public ProjectedCoordinateSystem(final String name, final GeographicCoordinateSystem gcs, final Projection projection)
    {this(name, gcs, projection, Unit.METRE, AxisInfo.X, AxisInfo.Y);}

    /**
     * Creates a projected coordinate system using a projection object.
     *
     * @param  name Name to give new object.
     * @param  gcs Geographic coordinate system to base projection on.
     * @param  projection Projection from geographic to projected coordinate system.
     * @param  unit Linear units of created PCS.
     * @param  axis0 Details of 0th ordinates in created PCS coordinates.
     * @param  axis1 Details of 1st ordinates in created PCS coordinates.
     *
     * @see org.opengis.cs.CS_CoordinateSystemFactory#createProjectedCoordinateSystem
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
     * @param  properties The set of properties (see {@link Info}).
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
     *
     * @see org.opengis.cs.CS_ProjectedCoordinateSystem#getGeographicCoordinateSystem()
     */
    public GeographicCoordinateSystem getGeographicCoordinateSystem()
    {return gcs;}

    /**
     * Gets the projection.
     *
     * @see org.opengis.cs.CS_ProjectedCoordinateSystem#getProjection()
     */
    public Projection getProjection()
    {return projection;}

    /**
     * Gets units for dimension within coordinate system.
     * This linear unit is the same for all axis.
     *
     * @param dimension Zero based index of axis.
     *
     * @see org.opengis.cs.CS_ProjectedCoordinateSystem#getUnits(int)
     * @see org.opengis.cs.CS_ProjectedCoordinateSystem#getLinearUnit()
     */
    public Unit getUnits(final int dimension)
    {
        if (dimension>=0 && dimension<getDimension()) return unit;
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Returns  <code>true</code> if this coordinate system is equivalents to
     * the specified coordinate system. Two coordinate systems are considered
     * equivalent if the {@link net.seas.opengis.ct.CoordinateTransform} from
     * <code>this</code> to  <code>cs</code>  would be the identity transform.
     * The default implementation compare datum, units and axis, but ignore
     * name, alias and other meta-data informations.
     *
     * @param  cs The coordinate system (may be <code>null</code>).
     * @return <code>true</code> if both coordinate systems are equivalent.
     */
    public boolean equivalents(final CoordinateSystem cs)
    {
        if (super.equivalents(cs))
        {
            final ProjectedCoordinateSystem that = (ProjectedCoordinateSystem) cs;
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
        {return (CS_LinearUnit) adapters.export(ProjectedCoordinateSystem.this.getUnits());}

        /**
         * Gets the projection.
         */
        public CS_Projection getProjection() throws RemoteException
        {return adapters.export(ProjectedCoordinateSystem.this.getProjection());}
    }
}
