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
     * Creates a compound coordinate system.
     *
     * @param name Name to give new object.
     * @param head Coordinate system to use for earlier ordinates.
     * @param tail Coordinate system to use for later ordinates.
     */
    public CompoundCoordinateSystem createCompoundCoordinateSystem(final String name, final CoordinateSystem head, final CoordinateSystem tail)
    {return (CompoundCoordinateSystem) pool.intern(new CompoundCoordinateSystem(name, head, tail));}

    /**
     * Creates an ellipsoid from radius values.
     *
     * @param name Name to give new object.
     * @param semiMajorAxis Equatorial radius in supplied linear units.
     * @param semiMinorAxis Polar radius in supplied linear units.
     * @param linearUnit Linear units of ellipsoid axes.
     */
    public Ellipsoid createEllipsoid(final String name, final double semiMajorAxis, final double semiMinorAxis, final Unit linearUnit)
    {return (Ellipsoid) pool.intern(new Ellipsoid(name, semiMajorAxis, semiMinorAxis, linearUnit));}

    /**
     * Creates an ellipsoid from an major radius, and inverse flattening.
     *
     * @param name Name to give new object.
     * @param semiMajorAxis Equatorial radius in supplied linear units.
     * @param inverseFlattening Eccentricity of ellipsoid.
     * @param linearUnit Linear units of major axis.
     */
    public Ellipsoid createFlattenedSphere(final String name, final double semiMajorAxis, final double inverseFlattening, final Unit linearUnit)
    {return (Ellipsoid) pool.intern(Ellipsoid.createFlattenedSphere(name, semiMajorAxis, inverseFlattening, linearUnit));}

    /**
     * Creates a prime meridian, relative to Greenwich.
     *
     * @param name Name to give new object.
     * @param angularUnit Angular units of longitude.
     * @param longitude Longitude of prime meridian in supplied angular units East of Greenwich.
     */
    public PrimeMeridian createPrimeMeridian(final String name, final Unit angularUnit, final double longitude)
    {return (PrimeMeridian) pool.intern(new PrimeMeridian(name, angularUnit, longitude));}

    /**
     * Creates horizontal datum from ellipsoid and Bursa-Wolf parameters. Since this
     * method contains a set of Bursa-Wolf parameters, the created datum will always
     * have a relationship to WGS84. If you wish to create a horizontal datum that
     * has no relationship with WGS84, then you can either specify
     * {@link DatumType.Horizontal#OTHER} as the horizontalDatumType,
     * or create it via WKT.
     *
     * @param name Name to give new object.
     * @param horizontalDatumType Type of horizontal datum to create.
     * @param ellipsoid Ellipsoid to use in new horizontal datum.
     * @param toWGS84 Suggested approximate conversion from new datum to WGS84.
     */
    public HorizontalDatum createHorizontalDatum(final String name, final DatumType.Horizontal horizontalDatumType, final Ellipsoid ellipsoid, final WGS84ConversionInfo toWGS84)
    {return (HorizontalDatum) pool.intern(new HorizontalDatum(name, horizontalDatumType, ellipsoid, toWGS84));}

    /**
     * Creates horizontal datum from an ellipsoid. The datum
     * type will be {@link DatumType.Horizontal#OTHER}.
     *
     * @param name Name to give new object.
     * @param ellipsoid Ellipsoid to use in new horizontal datum.
     */
    public HorizontalDatum createHorizontalDatum(final String name, final Ellipsoid ellipsoid)
    {return createHorizontalDatum(name, DatumType.Horizontal.OTHER, ellipsoid, null);}

    /**
     * Creates a vertical datum from an enumerated type value.
     *
     * @param name Name to give new object.
     * @param verticalDatumType Type of vertical datum to create.
     */
    public VerticalDatum createVerticalDatum(final String name, final DatumType.Vertical verticalDatumType)
    {return (VerticalDatum) pool.intern(new VerticalDatum(name, verticalDatumType));}

    /**
     * Creates a vertical coordinate system from a datum and linear units.
     *
     * @param name Name to give new object.
     * @param verticalDatum Datum to use for new coordinate system.
     * @param verticalUnit Units to use for new coordinate system.
     * @param axis Axis to use for new coordinate system.
     */
    public VerticalCoordinateSystem createVerticalCoordinateSystem(final String name, final VerticalDatum verticalDatum, final Unit verticalUnit, final AxisInfo axis)
    {return (VerticalCoordinateSystem) pool.intern(new VerticalCoordinateSystem(name, verticalDatum, verticalUnit, axis));}
}
