/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.cs;

// OpenGIS dependencies
import org.opengis.cs.CS_LinearUnit;
import org.opengis.cs.CS_PrimeMeridian;
import org.opengis.cs.CS_HorizontalDatum;
import org.opengis.cs.CS_GeocentricCoordinateSystem;

// Miscellaneous
import java.util.Map;
import java.util.Arrays;
import javax.units.Unit;
import java.rmi.RemoteException;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


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
public class GeocentricCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -6577810243397267703L;

    /**
     * The set of default axis orientation.
     * The <var>X</var> axis points towards the prime meridian.
     * The <var>Y</var> axis points East.
     * The <var>Z</var> axis points North.
     */
    private static final AxisInfo[] DEFAULT_AXIS = new AxisInfo[]
    {
        new AxisInfo("x", AxisOrientation.OTHER),
        new AxisInfo("y", AxisOrientation.EAST ),
        new AxisInfo("z", AxisOrientation.NORTH)
    };

    /**
     * The default geocentric coordinate system. Prime meridian is Greenwich,
     * horizontal datum in WGS84 and linear units are metre.
     * The <var>X</var> axis points towards the prime meridian.
     * The <var>Y</var> axis points East.
     * The <var>Z</var> axis points North.
     */
    public static final GeocentricCoordinateSystem DEFAULT = (GeocentricCoordinateSystem) pool.intern(
                        new GeocentricCoordinateSystem("WGS84", Unit.METRE, HorizontalDatum.WGS84,
                                                                PrimeMeridian.GREENWICH, DEFAULT_AXIS));

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
     * The axis infos.
     */
    private final AxisInfo[] axis;

    /**
     * Construct a geocentric coordinate system with default
     * axis. Unit are metres and prime meridian is greenwich.
     *
     * @param name  The coordinate system name.
     * @param datum The horizontal datum.
     */
    public GeocentricCoordinateSystem(final String name, final HorizontalDatum datum)
    {this(name, Unit.METRE, datum, PrimeMeridian.GREENWICH);}

    /**
     * Construct a geocentric coordinate system with default axis.
     * The <var>X</var> axis points towards the prime meridian.
     * The <var>Y</var> axis points East.
     * The <var>Z</var> axis points North.
     *
     * @param name     The coordinate system name.
     * @param unit     The linear unit.
     * @param datum    The horizontal datum.
     * @param meridian The prime meridian.
     */
    public GeocentricCoordinateSystem(final String name, final Unit unit, final HorizontalDatum datum, final PrimeMeridian meridian)
    {this(name, unit, datum, meridian, DEFAULT_AXIS);}

    /**
     * Construct a geocentric coordinate system.
     *
     * @param name     The coordinate system name.
     * @param unit     The linear unit.
     * @param datum    The horizontal datum.
     * @param meridian The prime meridian.
     * @param axis     The axis info. This is usually an array of lenght 3.
     */
    public GeocentricCoordinateSystem(final String name, final Unit unit, final HorizontalDatum datum, final PrimeMeridian meridian, final AxisInfo[] axis)
    {
        super(name);
        this.unit     = unit;
        this.datum    = datum;
        this.meridian = meridian;
        ensureNonNull("axis",     axis);
        ensureNonNull("unit",     unit);
        ensureNonNull("datum",    datum);
        ensureNonNull("meridian", meridian);
        ensureLinearUnit(unit);
        this.axis = clone(axis);
    }

    /**
     * Construct a geocentric coordinate system.
     *
     * @param properties The set of properties (see {@link Info}).
     * @param unit       The linear unit.
     * @param datum      The horizontal datum.
     * @param meridian   The prime meridian.
     * @param axis       The axis info. This is usually an array of lenght 3.
     */
    GeocentricCoordinateSystem(final Map properties, final Unit unit, final HorizontalDatum datum, final PrimeMeridian meridian, final AxisInfo[] axis)
    {
        super(properties);
        this.unit     = unit;
        this.datum    = datum;
        this.meridian = meridian;
        this.axis     = clone(axis);
    }

    /**
     * Clone the specified axis array.
     */
    private static AxisInfo[] clone(final AxisInfo[] axis)
    {return Arrays.equals(axis, DEFAULT_AXIS) ? DEFAULT_AXIS : (AxisInfo[]) axis.clone();}

    /**
     * Returns the dimension of this coordinate system, which is usually 3.
     *
     * @see org.opengis.cs.CS_GeocentricCoordinateSystem#getDimension()
     */
    public int getDimension()
    {return axis.length;}

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
        throw new IndexOutOfBoundsException(Resources.format(ResourceKeys.ERROR_INDEX_OUT_OF_BOUNDS_$1, new Integer(dimension)));
    }

    /**
     * Gets axis details for dimension within coordinate system.
     * Each dimension in the coordinate system has a corresponding axis.
     *
     * @param dimension Zero based index of axis.
     *
     * @see org.opengis.cs.CS_CoordinateSystem#getAxis(int)
     */
    public AxisInfo getAxis(final int dimension)
    {return axis[dimension];}

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
     * equivalent if the {@link net.seagis.ct.CoordinateTransformation} from
     * <code>this</code> to <code>cs</code> would be the identity transform.
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
            return Utilities.equals(this.unit,     that.unit)  &&
                   Utilities.equals(this.datum,    that.datum) &&
                   Utilities.equals(this.meridian, that.meridian);
        }
        return false;
    }

    /**
     * Fill the part inside "[...]".
     * Used for formatting Well Know Text (WKT).
     */
    String addString(final StringBuffer buffer, Object context)
    {
        buffer.append(", ");
        buffer.append(datum);
        buffer.append(", ");
        buffer.append(meridian);
        buffer.append(", ");
        addUnit(buffer, unit);
        for (int i=0; i<axis.length; i++)
        {
            buffer.append(", ");
            buffer.append(axis[i]);
        }
        return "GEOCCS";
    }

    /**
     * Returns an OpenGIS interface for this local coordinate
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
     * Wrap a {@link LocalCoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends CoordinateSystem.Export implements CS_GeocentricCoordinateSystem
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Gets the local datum.
         */
        public CS_HorizontalDatum getHorizontalDatum() throws RemoteException
        {return adapters.export(GeocentricCoordinateSystem.this.getHorizontalDatum());}
        
        /**
         * Gets the units used along all the axes.
         */
        public CS_LinearUnit getLinearUnit() throws RemoteException
        {return (CS_LinearUnit)adapters.export(GeocentricCoordinateSystem.this.getUnits());}
        
        /**
         * Returns the PrimeMeridian.
         */
        public CS_PrimeMeridian getPrimeMeridian() throws RemoteException
        {return adapters.export(GeocentricCoordinateSystem.this.getPrimeMeridian());}
    }
}
