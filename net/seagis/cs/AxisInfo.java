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

// Miscellaneous
import java.util.Locale;
import java.io.Serializable;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * Details of axis. This is used to label axes,
 * and indicate the orientation.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_AxisInfo
 */
public class AxisInfo implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 6799874182734710227L;

    /**
     * Default axis info for <var>x</var> values.
     * Increasing ordinates values go East. This
     * is usually used with projected coordinate
     * systems.
     */
    public static final AxisInfo X = new AxisInfo("x", AxisOrientation.EAST);

    /**
     * Default axis info for <var>y</var> values.
     * Increasing ordinates values go North. This
     * is usually used with projected coordinate
     * systems.
     */
    public static final AxisInfo Y = new AxisInfo("y", AxisOrientation.NORTH);

    /**
     * Default axis info for longitudes.
     * Increasing ordinates values go East.
     * This is usually used with geographic
     * coordinate systems.
     */
    public static final AxisInfo LONGITUDE = new AxisInfo.Localized("Longitude", ResourceKeys.LONGITUDE, AxisOrientation.EAST);

    /**
     * Default axis info for latitudes.
     * Increasing ordinates values go North.
     * This is usually used with geographic
     * coordinate systems.
     */
    public static final AxisInfo LATITUDE = new AxisInfo.Localized("Latitude", ResourceKeys.LATITUDE, AxisOrientation.NORTH);

    /**
     * The default axis for altitude values.
     * Increasing ordinates values go up.
     */
    public static final AxisInfo ALTITUDE = new AxisInfo.Localized("Altitude", ResourceKeys.ALTITUDE, AxisOrientation.UP);

    /**
     * A default axis for time values.
     * Increasing time go toward future.
     */
    public static final AxisInfo TIME = new AxisInfo.Localized("Time", ResourceKeys.TIME, AxisOrientation.FUTURE);

    /**
     * Human readable name for axis.
     * Possible values are <code>X</code>, <code>Y</code>,
     * <code>Long</code>, <code>Lat</code> or any other
     * short string.
     *
     * @see org.opengis.cs.CS_AxisInfo#name
     */
    public final String name;

    /**
     * Enumerated value for orientation.
     *
     * @see org.opengis.cs.CS_AxisInfo#orientation
     */
    public final AxisOrientation orientation;

    /**
     * Construct an AxisInfo.
     *
     * @param name The axis name. Possible values are <code>X</code>, <code>Y</code>,
     *             <code>Long</code>, <code>Lat</code> or any other short string.
     * @param orientation The axis orientation.
     */
    public AxisInfo(final String name, final AxisOrientation orientation)
    {
        this.name        = name;
        this.orientation = orientation;
        Info.ensureNonNull("name",        name);
        Info.ensureNonNull("orientation", orientation);
    }

    /**
     * Returns the localized name of this axis.
     * The default implementation returns {@link #name}.
     *
     * @param  locale The locale, or <code>null</code> for the default locale.
     * @return The localized string.
     */
    public String getName(final Locale locale)
    {return name;}

    /**
     * Returns a hash value for this axis.
     */
    public int hashCode()
    {
        int code=36972167;
        if (orientation!=null) code = code*37 + orientation.hashCode();
        if (name       !=null) code = code*37 + name.hashCode();
        return code;
    }

    /**
     * Compares the specified object
     * with this axis for equality.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && object.getClass().equals(getClass()))
        {
            final AxisInfo that = (AxisInfo) object;
            return Utilities.equals(this.orientation, that.orientation) &&
                   Utilities.equals(this.name       , that.name);
        }
        else return false;
    }

    /**
     * Returns the Well Know Text (WKT) for this axis.
     * The WKT is part of OpenGIS's specification and
     * looks like <code>AXIS["name",NORTH]</code>.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer("AXIS[\"");
        buffer.append(name);
        buffer.append('"');
        if (orientation!=null)
        {
            buffer.append(',');
            buffer.append(orientation.getName());
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Localized {@link AxisInfo}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class Localized extends AxisInfo
    {
        /**
         * Serial number for interoperability with different versions.
         */
        private static final long serialVersionUID = 7625005531024599865L;

        /**
         * The key for localization.
         */
        private final int key;

        /**
         * Construct a localized axis info.
         */
        public Localized(final String name, final int key, final AxisOrientation orientation)
        {
            super(name, orientation);
            this.key = key;
        }

        /**
         * Returns a localized string.
         */
        public String getName(final Locale locale)
        {return Resources.getResources(locale).getString(key);}
    }
}