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
import net.seas.util.WeakHashSet;
import net.seas.opengis.ct.Parameter;


/**
 * Builds up complex objects from simpler objects or values.
 * <code>CoordinateSystemFactory</code> allows applications to make coordinate
 * systems that cannot be created by a {@link CoordinateSystemAuthorityFactory}.
 * This factory is very flexible, whereas the authority factory is easier to use.
 *
 * So {@link CoordinateSystemAuthorityFactory} can be used to make 'standard'
 * coordinate systems, and <code>CoordinateSystemFactory</code> can be used to
 * make "special" coordinate systems.
 *
 * For example, the EPSG authority has codes for USA state plane coordinate systems
 * using the NAD83 datum, but these coordinate systems always use meters.  EPSG does
 * not have codes for NAD83 state plane coordinate systems that use feet units.  This
 * factory lets an application create such a hybrid coordinate system.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_CoordinateSystemFactory
 */
public class CoordinateSystemFactory
{
    /**
     * Default coordinate system factory.
     */
    public static final CoordinateSystemFactory DEFAULT = new CoordinateSystemFactory();

    /**
     * Set of weak references to existing coordinate systems.
     * This set is used in order to return pre-existing object
     * instead of creating new one.
     */
    private final WeakHashSet pool=new WeakHashSet();

    /**
     * Default constructor.
     */
    protected CoordinateSystemFactory()
    {}

    /**
     * Creates a geographic coordinate system, which could be <var>latitude</var>/<var>longiude</var>
     * or <var>longitude</var>/<var>latitude</var>.
     *
     * @param name      Name to give new object.
     * @param unit      Angular units for created coordinate system.
     * @param datum     Horizontal datum for created coordinate system.
     * @param meridian  Prime Meridian for created coordinate system.
     * @param axis0     Details of 0th ordinates.
     * @param axis1     Details of 1st ordinates.
     */
    public GeographicCoordinateSystem createGeographicCoordinateSystem(final String name, final Unit unit, final HorizontalDatum datum, final PrimeMeridian meridian, final AxisInfo axis0, final AxisInfo axis1)
    {return (GeographicCoordinateSystem) pool.intern(new GeographicCoordinateSystem(name, unit, datum, meridian, axis0, axis1));}

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
    public ProjectedCoordinateSystem createProjectedCoordinateSystem(final String name, final GeographicCoordinateSystem gcs, final Projection projection, final Unit unit, final AxisInfo axis0, final AxisInfo axis1)
    {return (ProjectedCoordinateSystem) pool.intern(new ProjectedCoordinateSystem(name, gcs, projection, unit, axis0, axis1));}

    /**
     * Creates a vertical coordinate system from a datum and linear units.
     *
     * @param name  Name to give new object.
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param axis  Axis to use for new coordinate system.
     */
    public VerticalCoordinateSystem createVerticalCoordinateSystem(final String name, final VerticalDatum datum, final Unit unit, final AxisInfo axis)
    {return (VerticalCoordinateSystem) pool.intern(new VerticalCoordinateSystem(name, datum, unit, axis));}

    /**
     * Creates a compound coordinate system.
     *
     * @param name Name to give new object.
     * @param head Coordinate system to use for earlier ordinates.
     * @param tail Coordinate system to use for later ordinates.
     */
    public CompoundCoordinateSystem createCompoundCoordinateSystem(final String name, final CoordinateSystem head, final CoordinateSystem tail)
    {return (CompoundCoordinateSystem) pool.intern(new CompoundCoordinateSystem(name, head, tail));}

    /**
     * Creates a local coordinate system. The dimension of the local coordinate
     * system is determined by the size of the axis array.  All the axes will
     * have the same units.  If you want to make a coordinate system with mixed
     * units, then you can make a compound coordinate system from different local
     * coordinate systems.
     *
     * @param name  Name to give new object.
     * @param datum Local datum to use in created CS.
     * @param unit  Units to use for all axes in created CS.
     * @param axes  Axes to use in created CS.
     */
    public LocalCoordinateSystem createLocalCoordinateSystem(final String name, final LocalDatum datum, final Unit unit, final AxisInfo[] axes)
    {return (LocalCoordinateSystem) pool.intern(new LocalCoordinateSystem(name, datum, unit, axes));}

    /**
     * Creates an ellipsoid from radius values.
     *
     * @param name          Name to give new object.
     * @param semiMajorAxis Equatorial radius in supplied linear units.
     * @param semiMinorAxis Polar radius in supplied linear units.
     * @param unit          Linear units of ellipsoid axes.
     */
    public Ellipsoid createEllipsoid(final String name, final double semiMajorAxis, final double semiMinorAxis, final Unit unit)
    {return (Ellipsoid) pool.intern(new Ellipsoid(name, semiMajorAxis, semiMinorAxis, unit));}

    /**
     * Creates an ellipsoid from an major radius, and inverse flattening.
     *
     * @param name              Name to give new object.
     * @param semiMajorAxis     Equatorial radius in supplied linear units.
     * @param inverseFlattening Eccentricity of ellipsoid.
     * @param unit              Linear units of major axis.
     */
    public Ellipsoid createFlattenedSphere(final String name, final double semiMajorAxis, final double inverseFlattening, final Unit unit)
    {return (Ellipsoid) pool.intern(Ellipsoid.createFlattenedSphere(name, semiMajorAxis, inverseFlattening, unit));}

    /**
     * Creates a prime meridian, relative to Greenwich.
     *
     * @param name      Name to give new object.
     * @param unit      Angular units of longitude.
     * @param longitude Longitude of prime meridian in supplied angular units East of Greenwich.
     */
    public PrimeMeridian createPrimeMeridian(final String name, final Unit unit, final double longitude)
    {return (PrimeMeridian) pool.intern(new PrimeMeridian(name, unit, longitude));}

    /**
     * Creates a projection.
     *
     * @param name           Name to give new object.
     * @param classification Classification string for projection (e.g. "Transverse_Mercator").
     * @param parameters     Parameters to use for projection, in metres or degrees.
     */
    public Projection createProjection(final String name, final String classification, final Parameter[] parameters)
    {return (Projection) pool.intern(new Projection(name, classification, parameters));}

    /**
     * Creates horizontal datum from ellipsoid and Bursa-Wolf parameters. Since this
     * method contains a set of Bursa-Wolf parameters, the created datum will always
     * have a relationship to WGS84. If you wish to create a horizontal datum that
     * has no relationship with WGS84, then you can either specify
     * {@link DatumType.Horizontal#OTHER} as the horizontalDatumType,
     * or create it via WKT.
     *
     * @param name      Name to give new object.
     * @param type      Type of horizontal datum to create.
     * @param ellipsoid Ellipsoid to use in new horizontal datum.
     * @param toWGS84   Suggested approximate conversion from new datum to WGS84.
     */
    public HorizontalDatum createHorizontalDatum(final String name, final DatumType.Horizontal type, final Ellipsoid ellipsoid, final WGS84ConversionInfo toWGS84)
    {return (HorizontalDatum) pool.intern(new HorizontalDatum(name, type, ellipsoid, toWGS84));}

    /**
     * Creates horizontal datum from an ellipsoid. The datum
     * type will be {@link DatumType.Horizontal#OTHER}.
     *
     * @param name      Name to give new object.
     * @param ellipsoid Ellipsoid to use in new horizontal datum.
     */
    public HorizontalDatum createHorizontalDatum(final String name, final Ellipsoid ellipsoid)
    {return createHorizontalDatum(name, DatumType.Horizontal.OTHER, ellipsoid, null);}

    /**
     * Creates a vertical datum from an enumerated type value.
     *
     * @param name Name to give new object.
     * @param type Type of vertical datum to create.
     */
    public VerticalDatum createVerticalDatum(final String name, final DatumType.Vertical type)
    {return (VerticalDatum) pool.intern(new VerticalDatum(name, type));}

    /**
     * Creates a local datum.
     *
     * @param name Name to give new object.
     * @param localDatumType Type of local datum to create.
     */
    public LocalDatum createLocalDatum(final String name, final DatumType.Local type)
    {return (LocalDatum) pool.intern(new LocalDatum(name, type));}
}
