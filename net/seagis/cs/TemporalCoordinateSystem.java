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

// Time
import java.util.Date;
import java.util.TimeZone;

// Miscellaneous
import java.util.Map;
import javax.units.Unit;
import net.seagis.pt.Envelope;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * A one-dimensional coordinate system suitable for time measurements.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class TemporalCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 4436983518157910233L;

    /**
     * The temporal datum.
     */
    private final TemporalDatum datum;

    /**
     * Axis details for time dimension within coordinate system.
     */
    private final AxisInfo axis;

    /**
     * Units used along the time axis.
     */
    private final Unit unit;

    /**
     * The epoch, in milliseconds since January 1, 1970, 00:00:00 UTC.
     */
    private final long epoch;

    /**
     * Creates a temporal coordinate system. Datum is UTC,
     * units are days and values are increasing toward future.
     *
     * @param name  Name  to give new object.
     * @param epoch The epoch (i.e. date of origin).
     */
    public TemporalCoordinateSystem(final String name, final Date epoch)
    {this(name, TemporalDatum.UTC, Unit.DAY, epoch, AxisInfo.TIME);}

    /**
     * Creates a temporal coordinate system from a datum and time units.
     *
     * @param name  Name  to give new object.
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param epoch The epoch (i.e. date of origin).
     * @param axis  Axis  to use for new coordinate system.
     */
    public TemporalCoordinateSystem(final String name, final TemporalDatum datum, final Unit unit, final Date epoch, final AxisInfo axis)
    {
        super(name);
        ensureNonNull("datum", datum);
        ensureNonNull("unit",  unit );
        ensureNonNull("epoch", epoch);
        ensureNonNull("axis",  axis );
        this.datum    = datum;
        this.unit     = unit;
        this.epoch    = epoch.getTime();
        this.axis     = axis;
        ensureTimeUnit(unit);
        checkAxis(datum.getDatumType());
    }

    /**
     * Creates a temporal coordinate system from a datum and time units.
     *
     * @param properties The set of properties (see {@link Info}).
     * @param datum Datum to use for new coordinate system.
     * @param unit  Units to use for new coordinate system.
     * @param epoch The epoch (i.e. date of origin).
     * @param axis  Axis  to use for new coordinate system.
     */
    TemporalCoordinateSystem(final Map properties, final TemporalDatum datum, final Unit unit, final Date epoch, final AxisInfo axis)
    {
        super(properties);
        this.datum = datum;
        this.unit  = unit;
        this.epoch = epoch.getTime();
        this.axis  = axis;
        // Accept null values.
    }

    /**
     * Returns the dimension of this coordinate system, which is 1.
     */
    public final int getDimension()
    {return 1;}

    /**
     * Override {@link CoordinateSystem#getDatum()}.
     */
    final Datum getDatum()
    {return getTemporalDatum();}

    /**
     * Gets the temporal datum, which indicates the measurement method.
     */
    public TemporalDatum getTemporalDatum()
    {return datum;}

    /**
     * Returns the epoch. The epoch is the origin of
     * the time axis, i.e. the date for value zero.
     */
    public Date getEpoch()
    {return new Date(epoch);}

    /**
     * Gets axis details for temporal dimension within coordinate system.
     * A temporal coordinate system have only one axis, always at index 0.
     *
     * @param dimension Zero based index of axis.
     */
    public AxisInfo getAxis(final int dimension)
    {
        final int maxDim = getDimension();
        if (dimension>=0 && dimension<maxDim) return axis;
        throw new IndexOutOfBoundsException(Resources.format(ResourceKeys.ERROR_INDEX_OUT_OF_BOUNDS_$1, new Integer(dimension)));
    }

    /**
     * Gets units for dimension within coordinate system.
     * A temporal coordinate system have only one unit,
     * always at index 0.
     *
     * @param dimension Must be 0.
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
            final TemporalCoordinateSystem that = (TemporalCoordinateSystem) cs;
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
        return "TEMPORAL_CS";
    }
}