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
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;
import net.seas.resources.Resources;
import net.seas.resources.Clé;
import net.seas.util.XClass;


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
public abstract class DatumType implements Serializable
{
    /**
     * <code>serialVersionUID</code> for interoperability with previous versions.
     */
    private static final long serialVersionUID = 2123402922368772495L;

    /**
     * These datums, such as ED50, NAD27 and NAD83, have been designed to
     * support horizontal positions on the ellipsoid as opposed to positions
     * in 3-D space.  These datums were designed mainly to support a horizontal
     * component of a position in a domain of limited extent, such as a country,
     * a region or a continent.
     */
    public static final Horizontal CLASSIC = new Horizontal(1001, Clé.CLASSIC);

    /**
     * A geocentric datum is a "satellite age" modern geodetic datum mainly of
     * global extent, such as WGS84 (used in GPS), PZ90 (used in GLONASS) and
     * ITRF. These datums were designed to support both a horizontal
     * component of position and a vertical component of position (through
     * ellipsoidal heights).  The regional realizations of ITRF, such as
     * ETRF, are also included in this category.
     */
    public static final Horizontal GEOCENTRIC = new Horizontal(1002, Clé.GEOCENTRIC);

    /**
     * A vertical datum for orthometric heights that are measured along the
     * plumb line.
     */
    public static final Vertical ORTHOMETRIC = new Vertical(2001, Clé.ORTHOMETRIC);

    /**
     * A vertical datum for ellipsoidal heights that are measured along the
     * normal to the ellipsoid used in the definition of horizontal datum.
     */
    public static final Vertical ELLIPSOIDAL = new Vertical(2002, Clé.ELLIPSOIDAL);

    /**
     * The vertical datum of altitudes or heights in the atmosphere. These
     * are approximations of orthometric heights obtained with the help of
     * a barometer or a barometric altimeter.  These values are usually
     * expressed in one of the following units: meters, feet, millibars
     * (used to measure pressure levels),  or theta value (units used to
     * measure geopotential height).
     */
    public static final Vertical ALTITUDE_BAROMETRIC = new Vertical(2003, Clé.BAROMETRIC_ALTITUDE);

    /**
     * A normal height system.
     */
    public static final Vertical NORMAL = new Vertical(2004, Clé.NORMAL);

    /**
     * A vertical datum of geoid model derived heights, also called
     * GPS-derived heights. These heights are approximations of
     * orthometric heights (<var>H</var>), constructed from the
     * ellipsoidal heights (<var>h</var>) by the use of the given
     * geoid undulation model (<var>N</var>) through the equation:
     * <var>H</var>=<var>h</var>-<var>N</var>.
     */
    public static final Vertical GEOID_MODEL_DERIVED = new Vertical(2005, Clé.GEOID_MODEL_DERIVED);

    /**
     * This attribute is used to support the set of datums generated
     * for hydrographic engineering projects where depth measurements below
     * sea level are needed. It is often called a hydrographic or a marine
     * datum. Depths are measured in the direction perpendicular
     * (approximately) to the actual equipotential surfaces of the earth's
     * gravity field, using such procedures as echo-sounding.
     */
    public static final Vertical DEPTH = new Vertical(2006, Clé.DEPTH);

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
        DEPTH
    };

    /**
     * Resource key, used for building localized name. This key doesn't need to
     * be serialized, since {@link #readResolve} canonicalize enums according their
     * {@link #value}. Furthermore, its value is implementation-dependent (which is
     * an other raison why it should not be serialized).
     */
    private transient final int clé;

    /**
     * The enum value. This field is public for compatibility
     * with {@link org.opengis.cs.CS_DatumType}.
     */
    public final int value;

    /**
     * Construct a new enum with the specified value.
     */
    private DatumType(final int value, final int clé)
    {
        this.value = value;
        this.clé   = clé;
        if (!(value>=getMinimum() && value<=getMaximum()))
            throw new IllegalArgumentException(String.valueOf(value));
    }

    /**
     * Return the enum for the specified value. Current version don't allow
     * new enum definition (it will be fixed in a future version).
     *
     * @param  value The enum value.
     * @return The enum for the specified value.
     */
    public static DatumType getEnum(final int value)
    {
        for (int i=0; i<ENUMS.length; i++)
            if (ENUMS[i].value==value)
                return ENUMS[i];
        throw new NoSuchElementException(String.valueOf(value));
    }

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
     * Return the type name in the specified locale.
     * Type may be "Horizontal", "Vertical" ou "Local".
     */
    public String getType(final Locale locale)
    {return Resources.getResources(locale).getString(getTypeKey());}

    /**
     * Returns this enum's name in the specified locale. If no name
     * is available for the specified locale, a default one will be
     * used.
     *
     * @param  locale The locale, or <code>null</code> for the current default locale.
     * @return Enum's name in the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(clé);}

    /**
     * Returns the enum value.
     */
    public int hashCode()
    {return value;}

    /**
     * Compares the specified object with
     * this enum for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof DatumType)
        {
            return ((DatumType) object).value == value;
        }
        else return false;
    }

    /**
     * Returns a string représentation of this enum.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes only.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        final String name = getName(null);
        if (name!=null)
            buffer.append(name);
        else
            buffer.append(value);
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Use a single instance of {@link DatumType} after deserialization.
     * It allow client code to test <code>enum1==enum2</code> instead of
     * <code>enum1.equals(enum2)</code>.
     *
     * @return A single instance of this enum.
     * @throws ObjectStreamException is deserialization failed.
     */
    private Object readResolve() throws ObjectStreamException
    {return getEnum(value);}

    /**
     * Horizontal datum type.
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
        public static final Horizontal OTHER = new Horizontal(1000, Clé.OTHER);

        /**
         * Construct a new enum with the specified value.
         */
        private Horizontal(final int value, final int clé)
        {super(value, clé);}

        /** Get the minimum value. */ final int getMinimum() {return MINIMUM;}
        /** Get the maximum value. */ final int getMaximum() {return MAXIMUM;}
        /** Return the type key.   */ final int getTypeKey() {return Clé.HORIZONTAL;}
    }

    /**
     * Vertical datum type.
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
        public static final Vertical OTHER = new Vertical(2000, Clé.OTHER);

        /**
         * Construct a new enum with the specified value.
         */
        private Vertical(final int value, final int clé)
        {super(value, clé);}

        /** Get the minimum value. */ final int getMinimum() {return MINIMUM;}
        /** Get the maximum value. */ final int getMaximum() {return MAXIMUM;}
        /** Return the type key.   */ final int getTypeKey() {return Clé.VERTICAL;}
    }

    /**
     * Local datum type.
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
        private Local(final int value, final int clé)
        {super(value, clé);}

        /** Get the minimum value. */ final int getMinimum() {return MINIMUM;}
        /** Get the maximum value. */ final int getMaximum() {return MAXIMUM;}
        /** Return the type key.   */ final int getTypeKey() {return Clé.LOCAL;}
    }
}
