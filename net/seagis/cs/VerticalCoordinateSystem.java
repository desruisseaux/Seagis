/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
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
import org.opengis.cs.CS_VerticalDatum;
import org.opengis.cs.CS_VerticalCoordinateSystem;

// Miscellaneous
import java.util.Map;
import javax.units.Unit;
import java.rmi.RemoteException;
import net.seagis.pt.Envelope;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


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
    private static final long serialVersionUID = -8629573233560414552L;

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
     * Creates a vertical coordinate system from a datum. Units
     * will be metres and values will be increasing upward.
     *
     * @param name  Name to give new object.
     * @param datum Datum to use for new coordinate system.
     */
    public VerticalCoordinateSystem(final String name, final VerticalDatum datum)
    {this(name, datum, Unit.METRE, AxisInfo.ALTITUDE);}

    /**
     * Creates a vertical coordinate system from a datum and linear units.
     *
     * @param name  Name to give new object.
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param axis  Axis to use for new coordinate system.
     *
     * @see org.opengis.cs.CS_CoordinateSystemFactory#createVerticalCoordinateSystem
     */
    public VerticalCoordinateSystem(final String name, final VerticalDatum datum, final Unit unit, final AxisInfo axis)
    {
        super(name);
        this.datum = datum;
        this.unit  = unit;
        this.axis  = axis;
        ensureNonNull("datum", datum);
        ensureNonNull("unit",  unit );
        ensureNonNull("axis",  axis );
        ensureLinearUnit(unit);
        checkAxis(datum.getDatumType());
    }

    /**
     * Creates a vertical coordinate system from a datum and linear units.
     *
     * @param properties The set of properties (see {@link Info}).
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param axis  Axis to use for new coordinate system.
     */
    VerticalCoordinateSystem(final Map properties, final VerticalDatum datum, final Unit unit, final AxisInfo axis)
    {
        super(properties);
        this.datum = datum;
        this.unit  = unit;
        this.axis  = axis;
        // Accept null values.
    }

    /**
     * Returns the dimension of this coordinate system, which is 1.
     *
     * @see org.opengis.cs.CS_VerticalCoordinateSystem#getDimension()
     */
    public final int getDimension()
    {return 1;}

    /**
     * Override {@link CoordinateSystem#getDatum()}.
     */
    final Datum getDatum()
    {return getVerticalDatum();}

    /**
     * Gets the vertical datum, which indicates the measurement method.
     *
     * @see org.opengis.cs.CS_VerticalCoordinateSystem#getVerticalDatum()
     */
    public VerticalDatum getVerticalDatum()
    {return datum;}

    /**
     * Gets axis details for vertical dimension within coordinate system.
     * A vertical coordinate system have only one axis, always at index 0.
     *
     * @param dimension Zero based index of axis.
     *
     * @see org.opengis.cs.CS_VerticalCoordinateSystem#getAxis(int)
     */
    public AxisInfo getAxis(final int dimension)
    {
        final int maxDim = getDimension();
        if (dimension>=0 && dimension<maxDim) return axis;
        throw new IndexOutOfBoundsException(Resources.format(ResourceKeys.ERROR_INDEX_OUT_OF_BOUNDS_$1, new Integer(dimension)));
    }

    /**
     * Gets units for dimension within coordinate system.
     * A vertical coordinate system have only one unit,
     * always at index 0.
     *
     * @param dimension Must be 0.
     *
     * @see org.opengis.cs.CS_VerticalCoordinateSystem#getUnits(int)
     * @see org.opengis.cs.CS_VerticalCoordinateSystem#getVerticalUnit()
     */
    public Unit getUnits(final int dimension)
    {
        final int maxDim = getDimension();
        if (dimension>=0 && dimension<maxDim) return unit;
        throw new IndexOutOfBoundsException(Resources.format(ResourceKeys.ERROR_INDEX_OUT_OF_BOUNDS_$1, new Integer(dimension)));
    }

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
            final VerticalCoordinateSystem that = (VerticalCoordinateSystem) cs;
            return Utilities.equals(this.datum, that.datum) &&
                   Utilities.equals(this.unit , that.unit ) &&
                   Utilities.equals(this.axis , that.axis );
        }
        return false;
    }

    /**
     * Fill the part inside "[...]".
     * Used for formatting Well Know Text (WKT).
     */
    String addString(final StringBuffer buffer)
    {
        buffer.append(", ");
        buffer.append(datum);
        buffer.append(", ");
        addUnit(buffer, unit);
        buffer.append(", ");
        buffer.append(axis);
        return "VERT_CS";
    }

    /**
     * Returns an OpenGIS interface for this vertical coordinate
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
     * Wrap a {@link VerticalCoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends CoordinateSystem.Export implements CS_VerticalCoordinateSystem
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Gets the vertical datum, which indicates the measurement method.
         */
        public CS_VerticalDatum getVerticalDatum() throws RemoteException
        {return adapters.export(VerticalCoordinateSystem.this.getVerticalDatum());}

        /**
         * Gets the units used along the vertical axis.
         */
        public CS_LinearUnit getVerticalUnit() throws RemoteException
        {return (CS_LinearUnit) adapters.export(VerticalCoordinateSystem.this.getUnits());}
    }
}