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
import java.util.Locale;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;
import javax.media.jai.EnumeratedParameter;
import net.seas.resources.Resources;
import net.seas.util.WeakHashSet;
import net.seas.resources.Clé;


/**
 * Type of the datum expressed as an enumerated value.
 * The enumeration is split into ranges which indicate the datum's type.
 * The value should be one of the predefined values, or within the range
 * for local types. This will allow OpenGIS Consortium to coordinate the
 * addition of new interoperable codes.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_DatumType
 */
public abstract class DatumType extends EnumeratedParameter
{
    /**
     * <code>serialVersionUID</code> for interoperability with previous versions.
     */
    private static final long serialVersionUID = -3231606206426977300L;

    /**
     * The pool of local datum. This pool
     * will be created only when needed.
     */
    private static WeakHashSet<DatumType> pool;

    /**
     * These datums, such as ED50, NAD27 and NAD83, have been designed
     * to support horizontal positions on the ellipsoid as opposed to positions
     * in 3-D space.  These datums were designed mainly to support a horizontal
     * component of a position in a domain of limited extent, such as a country,
     * a region or a continent.
     */
    public static final Horizontal CLASSIC = new Horizontal("CLASSIC", 1001, Clé.CLASSIC);

    /**
     * A geocentric datum is a "satellite age" modern geodetic datum
     * mainly of global extent, such as WGS84 (used in GPS), PZ90 (used in GLONASS) and
     * ITRF. These datums were designed to support both a horizontal
     * component of position and a vertical component of position (through
     * ellipsoidal heights).  The regional realizations of ITRF, such as
     * ETRF, are also included in this category.
     */
    public static final Horizontal GEOCENTRIC = new Horizontal("GEOCENTRIC", 1002, Clé.GEOCENTRIC);

    /**
     * A vertical datum for orthometric heights
     * that are measured along the plumb line.
     */
    public static final Vertical ORTHOMETRIC = new Vertical("ORTHOMETRIC", 2001, Clé.ORTHOMETRIC);

    /**
     * A vertical datum for ellipsoidal heights that are measured along the
     * normal to the ellipsoid used in the definition of horizontal datum.
     */
    public static final Vertical ELLIPSOIDAL = new Vertical("ELLIPSOIDAL", 2002, Clé.ELLIPSOIDAL);

    /**
     * The vertical datum of altitudes or heights in the atmosphere.
     * These are approximations of orthometric heights obtained with
     * the help of a barometer or a barometric altimeter. These values
     * are usually expressed in one of the following units: meters, feet,
     * millibars (used to measure pressure levels), or theta value (units
     * used to measure geopotential height).
     */
    public static final Vertical ALTITUDE_BAROMETRIC = new Vertical("ALTITUDE_BAROMETRIC", 2003, Clé.BAROMETRIC_ALTITUDE);

    /**
     * A normal height system.
     */
    public static final Vertical NORMAL = new Vertical("NORMAL", 2004, Clé.NORMAL);

    /**
     * A vertical datum of geoid model derived heights,
     * also called GPS-derived heights. These heights are approximations
     * of orthometric heights (<var>H</var>), constructed from the
     * ellipsoidal heights (<var>h</var>) by the use of the given
     * geoid undulation model (<var>N</var>) through the equation:
     * <var>H</var>=<var>h</var>-<var>N</var>.
     */
    public static final Vertical GEOID_MODEL_DERIVED = new Vertical("GEOID_MODEL_DERIVED", 2005, Clé.GEOID_MODEL_DERIVED);

    /**
     * This attribute is used to support the set of datums generated
     * for hydrographic engineering projects where depth measurements below
     * sea level are needed. It is often called a hydrographic or a marine
     * datum. Depths are measured in the direction perpendicular
     * (approximately) to the actual equipotential surfaces of the earth's
     * gravity field, using such procedures as echo-sounding.
     */
    public static final Vertical DEPTH = new Vertical("DEPTH", 2006, Clé.DEPTH);

    /**
     * <FONT COLOR="#FF6633">A temporal datum for Universal Time (UTC).</FONT>
     * UTC is based on an atomic clock, while GMT is based on astronomical observations.
     * <br><br>
     * <strong>Note: This enum is not part of OpenGIS specification. It may change
     *         in incompatible way if OpenGIS define an equivalent enum.</strong>
     */
    public static final Temporal UTC = new Temporal("UTC", 3001, Clé.UTC);

    /**
     * <FONT COLOR="#FF6633">A temporal datum for Greenwich Mean Time (GMT).</FONT>
     * GMT is based on astronomical observations, while UTC is based on an atomic clock.
     * <br><br>
     * <strong>Note: This enum is not part of OpenGIS specification. It may change
     *         in incompatible way if OpenGIS define an equivalent enum.</strong>
     */
    public static final Temporal GMT = new Temporal("GMT", 3002, Clé.GMT);

    /**
     * List of predefined enum types.
     */
    private static final DatumType[] ENUMS =
    {
        Horizontal.OTHER,
        CLASSIC,
        GEOCENTRIC,
        Vertical.OTHER,
        ORTHOMETRIC,
        ELLIPSOIDAL,
        ALTITUDE_BAROMETRIC,
        NORMAL,
        GEOID_MODEL_DERIVED,
        DEPTH,
        UTC,
        GMT
    };

    /**
     * Resource key, used for building localized name. This key doesn't need to
     * be serialized, since {@link #readResolve} canonicalize enums according their
     * {@link #value}. Furthermore, its value is implementation-dependent (which is
     * an other raison why it should not be serialized).
     */
    private transient final int clé;

    /**
     * Construct a new enum with the specified value.
     */
    private DatumType(final String name, final int value, final int clé)
    {
        super(name, value);
        this.clé = clé;
        if (!(value>=getMinimum() && value<=getMaximum()))
            throw new IllegalArgumentException(String.valueOf(value));
    }

    /**
     * <FONT COLOR="#FF6633">Return the enum for the specified value.</FONT>
     *
     * @param  value The enum value.
     * @return The enum for the specified value.
     */
    public static DatumType getEnum(final int value)
    {
        for (int i=0; i<ENUMS.length; i++)
            if (ENUMS[i].getValue()==value)
                return ENUMS[i];

        final DatumType datum;
        if (value>=Horizontal.MINIMUM && value<=Horizontal.MAXIMUM)
        {
            datum = new Horizontal("Custom", value, 0);
        }
        else if (value>=Vertical.MINIMUM && value<=Vertical.MAXIMUM)
        {
            datum = new Vertical("Custom", value, 0);
        }
        else if (value>=Temporal.MINIMUM && value<=Temporal.MAXIMUM)
        {
            datum = new Temporal("Custom", value, 0);
        }
        else if (value>=Local.MINIMUM && value<=Local.MAXIMUM)
        {
            datum = new Local("Custom", value, 0);
        }
        else
        {
            throw new IllegalArgumentException(String.valueOf(value));
        }
        synchronized (DatumType.class)
        {
            if (pool==null)
            {
                pool = new Pool();
            }
            return pool.intern(datum);
        }
    }

    /**
     * Returns <code>true</code> if the specified orientation is compatible
     * with this datum type. For example, a vertical datum is compatible only
     * with orientations UP and DOWN.
     */
    abstract boolean isCompatibleOrientation(final AxisOrientation orientation);

    /**
     * Get the minimum value.
     */
    abstract int getMinimum();

    /**
     * Get the maximum value.
     */
    abstract int getMaximum();

    /**
     * Return the type key.
     */
    abstract int getTypeKey();

    /**
     * <FONT COLOR="#FF6633">Return the type name in the specified locale.</FONT>
     * Type may be "Horizontal", "Vertical", "Temporal" or "Local".
     */
    public String getType(final Locale locale)
    {return Resources.getResources(locale).getString(getTypeKey());}

    /**
     * <FONT COLOR="#FF6633">Returns this enum's name in the specified locale.</FONT>
     * If no name is available for the specified locale, a default one will be used.
     *
     * @param  locale The locale, or <code>null</code> for the default locale.
     * @return Enum's name in the specified locale.
     */
    public String getName(final Locale locale)
    {return (clé!=0) ? Resources.getResources(locale).getString(clé) : getName();}

    /**
     * Use a single instance of {@link DatumType} after deserialization.
     * It allow client code to test <code>enum1==enum2</code> instead of
     * <code>enum1.equals(enum2)</code>.
     *
     * @return A single instance of this enum.
     * @throws ObjectStreamException is deserialization failed.
     */
    private Object readResolve() throws ObjectStreamException
    {return getEnum(getValue());}

    /**
     * <FONT COLOR="#FF6633">Horizontal datum type.</FONT>
     *
     * @version 1.00
     * @author Martin Desruisseaux
     */
    public static final class Horizontal extends DatumType
    {
        /**
         * <code>serialVersionUID</code> for interoperability with previous versions.
         */
        private static final long serialVersionUID = 7172421404798913210L;

        /**
         * Lowest possible value for horizontal datum types.
         */
        public static final int MINIMUM = 1000;

        /**
         * Highest possible value for horizontal datum types.
         */
        public static final int MAXIMUM = 1999;

        /**
         * Unspecified horizontal datum type.
         * Horizontal datums with this type should never supply
         * a conversion to WGS84 using Bursa Wolf parameters.
         */
        public static final Horizontal OTHER = new Horizontal("OTHER", 1000, Clé.OTHER);

        /**
         * Construct a new enum with the specified value.
         */
        private Horizontal(final String name, final int value, final int clé)
        {super(name, value, clé);}

        /**
         * Returns <code>true</code> if the specified orientation is compatible
         * with this datum type. Compatible orientations are NORTH, SOUTH, EAST
         * and WEST.
         */
        boolean isCompatibleOrientation(final AxisOrientation orientation)
        {
            return AxisOrientation.NORTH.equals(orientation) ||
                   AxisOrientation.SOUTH.equals(orientation) ||
                   AxisOrientation.EAST .equals(orientation) ||
                   AxisOrientation.WEST .equals(orientation);
        }

        /** Get the minimum value. */ final int getMinimum() {return MINIMUM;}
        /** Get the maximum value. */ final int getMaximum() {return MAXIMUM;}
        /** Return the type key.   */ final int getTypeKey() {return Clé.HORIZONTAL;}
    }

    /**
     * <FONT COLOR="#FF6633">Vertical datum type.</FONT>
     *
     * @version 1.00
     * @author Martin Desruisseaux
     */
    public static final class Vertical extends DatumType
    {
        /**
         * <code>serialVersionUID</code> for interoperability with previous versions.
         */
        private static final long serialVersionUID = -5713408139450202831L;

        /**
         * Lowest possible value for vertical datum types.
         */
        public static final int MINIMUM = 2000;

        /**
         * Highest possible value for vertical datum types.
         */
        public static final int MAXIMUM = 2999;

        /**
         * Unspecified vertical datum type.
         */
        public static final Vertical OTHER = new Vertical("OTHER", 2000, Clé.OTHER);

        /**
         * Construct a new enum with the specified value.
         */
        private Vertical(final String name, final int value, final int clé)
        {super(name, value, clé);}

        /**
         * Returns <code>true</code> if the specified orientation is compatible
         * with this datum type. Compatible orientations are UP and DOWN.
         */
        boolean isCompatibleOrientation(final AxisOrientation orientation)
        {
            return AxisOrientation.UP  .equals(orientation) ||
                   AxisOrientation.DOWN.equals(orientation);
        }

        /** Get the minimum value. */ final int getMinimum() {return MINIMUM;}
        /** Get the maximum value. */ final int getMaximum() {return MAXIMUM;}
        /** Return the type key.   */ final int getTypeKey() {return Clé.VERTICAL;}
    }

    /**
     * <FONT COLOR="#FF6633">Temporal datum type.</FONT>
     *
     * @version 1.00
     * @author Martin Desruisseaux
     */
    public static final class Temporal extends DatumType
    {
        /**
         * <code>serialVersionUID</code> for interoperability with previous versions.
         */
        // private static final long serialVersionUID = ?; // TODO

        /**
         * Lowest possible value for temporal datum types.
         * <br><br>
         * <strong>Note: Temporal enums are not part of OpenGIS specification. The
         *               <code>MINIMUM</code> "constant" may change in the future
         *               if OpenGIS defines an equivalent datum type.</strong>
         */
        public static final int MINIMUM = 3000;

        /**
         * Highest possible value for temporal datum types.
         * <br><br>
         * <strong>Note: Temporal enums are not part of OpenGIS specification. The
         *               <code>MAXIMUM</code> "constant" may change in the future
         *               if OpenGIS defines an equivalent datum type.</strong>
         */
        public static final int MAXIMUM = 3999;

        /**
         * Construct a new enum with the specified value.
         */
        private Temporal(final String name, final int value, final int clé)
        {super(name, value, clé);}

        /**
         * Returns <code>true</code> if the specified orientation is compatible
         * with this datum type. Compatible orientations are FUTURE and PAST.
         */
        boolean isCompatibleOrientation(final AxisOrientation orientation)
        {
            return AxisOrientation.FUTURE.equals(orientation) ||
                   AxisOrientation.PAST  .equals(orientation);
        }

        /** Get the minimum value. */ final int getMinimum() {return MINIMUM;}
        /** Get the maximum value. */ final int getMaximum() {return MAXIMUM;}
        /** Return the type key.   */ final int getTypeKey() {return Clé.TEMPORAL;}
    }

    /**
     * <FONT COLOR="#FF6633">Local datum type.</FONT>
     *
     * @version 1.00
     * @author Martin Desruisseaux
     */
    public static final class Local extends DatumType
    {
        /**
         * <code>serialVersionUID</code> for interoperability with previous versions.
         */
        private static final long serialVersionUID = -2000541174931444868L;

        /**
         * Lowest possible value for local datum types.
         */
        public static final int MINIMUM = 10000;

        /**
         * Highest possible value for local datum types.
         */
        public static final int MAXIMUM = 32767;

        /**
         * Construct a new enum with the specified value.
         */
        private Local(final String name, final int value, final int clé)
        {super(name, value, clé);}

        /**
         * Returns <code>true</code> if the specified orientation is compatible
         * with this datum type. Local datum accept all orientations.
         */
        boolean isCompatibleOrientation(final AxisOrientation orientation)
        {return true;}

        /** Get the minimum value. */ final int getMinimum() {return MINIMUM;}
        /** Get the maximum value. */ final int getMaximum() {return MAXIMUM;}
        /** Return the type key.   */ final int getTypeKey() {return Clé.LOCAL;}
    }

    /**
     * Pool of custom {@link DatumType}.
     *
     * @version 1.00
     * @author Martin Desruisseaux
     */
    private static final class Pool extends WeakHashSet<DatumType>
    {
        /**
         * Override in order to get hash code computed from the enum value.
         */
        protected int hashCode(final DatumType object)
        {return object.getValue();}

        /**
         * Override in order to compare hash code values only (not the name).
         */
        protected boolean equals(final DatumType object1, final DatumType object2)
        {return object1.getValue()==object2.getValue();}
    }
}
