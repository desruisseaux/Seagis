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
import org.opengis.cs.CS_PrimeMeridian;
import org.opengis.cs.CS_GeocentricCoordinateSystem;

// Miscellaneous
import java.util.Map;
import javax.units.Unit;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import java.rmi.RemoteException;


/**
 * A 3D coordinate system, with its origin at the center of the Earth.
 * The <var>X</var> axis points towards the prime meridian.
 * The <var>Y</var> axis points East or West.
 * The <var>Z</var> axis points North or South. By default the
 * <var>Z</var> axis will point North, and the <var>Y</var> axis
 * will point East (e.g. a right handed system), but you should
 * check the axes for non-default values.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_GeocentricCoordinateSystem
 */
public abstract class GeocentricCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    //private static final long serialVersionUID = ?; // TODO: compute

    /**
     * The linear unit.
     */
    private final Unit unit;

    /**
     * The horizontal datum.
     */
    private final HorizontalDatum datum;

    /**
     * The prime meridian.
     */
    private final PrimeMeridian meridian;

    /**
     * Construct a coordinate system.
     *
     * @param name     The coordinate system name.
     * @param unit     The linear unit.
     * @param datum    The horizontal datum.
     * @param meridian The prime meridian.
     */
    public GeocentricCoordinateSystem(final String name, final Unit unit, final HorizontalDatum datum, final PrimeMeridian meridian)
    {
        super(name);
        this.unit     = unit;
        this.datum    = datum;
        this.meridian = meridian;
        ensureNonNull("unit",     unit);
        ensureNonNull("datum",    datum);
        ensureNonNull("meridian", meridian);
    }

    /**
     * Returns the dimension of this coordinate system, which is 3.
     *
     * @see org.opengis.cs.CS_GeocentricCoordinateSystem#getDimension()
     */
    public int getDimension()
    {return 3;}

    /**
     * Override {@link CoordinateSystem#getDatum()}.
     */
    final Datum getDatum()
    {return getHorizontalDatum();}

    /**
     * Returns the horizontal datum.
     * The horizontal datum is used to determine where the center of the Earth
     * is considered to be. All coordinate points will be measured from the
     * center of the Earth, and not the surface.
     *
     * @see org.opengis.cs.CS_GeocentricCoordinateSystem#getHorizontalDatum()
     */
    public HorizontalDatum getHorizontalDatum()
    {return datum;}

    /**
     * Gets units for dimension within coordinate system.
     * For a <code>GeocentricCoordinateSystem</code>, the
     * units is the same for all axis.
     *
     * @param dimension Zero based index of axis.
     *
     * @see org.opengis.cs.CS_GeocentricCoordinateSystem#getUnits(int)
     */
    public Unit getUnits(final int dimension)
    {
        if (dimension>=0 && dimension<getDimension()) return unit;
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Returns the prime meridian.
     *
     * @see org.opengis.cs.CS_GeocentricCoordinateSystem#getPrimeMeridian()
     */
    public PrimeMeridian getPrimeMeridian()
    {return meridian;}

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
        if (cs==this) return true;
        if (super.equivalents(cs))
        {
            final GeocentricCoordinateSystem that = (GeocentricCoordinateSystem) cs;
            return XClass.equals(this.unit,     that.unit)  &&
                   XClass.equals(this.datum,    that.datum) &&
                   XClass.equals(this.meridian, that.meridian);
        }
        return false;
    }
}
