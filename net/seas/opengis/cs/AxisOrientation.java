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
 * Orientation of axis. Some coordinate systems use non-standard orientations.
 * For example, the first axis in South African grids usually points West,
 * instead of East. This information is obviously relevant for algorithms
 * converting South African grid coordinates into Lat/Long.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_AxisOrientationEnum
 */
public final class AxisOrientation implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 6962768641608234346L;

    /**
     * Unknown or unspecified axis orientation.
     * This can be used for local or fitted coordinate systems. 
     */
    public static final AxisOrientation OTHER = new AxisOrientation(0, Clé.OTHER);

    /**
     * Increasing ordinates values go North. This is usually
     * used for Grid Y coordinates and Latitude.
     */
    public static final AxisOrientation NORTH = new AxisOrientation(1, Clé.NORTH);

    /**
     * Increasing ordinates values go South.
     * This is rarely used.
     */
    public static final AxisOrientation SOUTH = new AxisOrientation(2, Clé.SOUTH);

    /**
     * Increasing ordinates values go East.
     * This is rarely used.
     */
    public static final AxisOrientation EAST = new AxisOrientation(3, Clé.EAST);

    /**
     * Increasing ordinates values go West.
     * This is usually used for Grid X coordinates and Longitude.
     */
    public static final AxisOrientation WEST = new AxisOrientation(4, Clé.WEST);

    /**
     * Increasing ordinates values go up.
     * This is used for vertical coordinate systems.
     */
    public static final AxisOrientation UP = new AxisOrientation(5, Clé.UP);

    /**
     * Increasing ordinates values go down.
     * This is used for vertical coordinate systems.
     */
    public static final AxisOrientation DOWN = new AxisOrientation(6, Clé.DOWN);

    /**
     * Axis orientations by value. Used to
     * canonicalize after deserialization.
     */
    private static final AxisOrientation[] ENUMS = {OTHER,NORTH,SOUTH,EAST,WEST,UP,DOWN};

    /**
     * Resource key, used for building localized name. This key doesn't need to
     * be serialized, since {@link #readResolve} canonicalize enums according their
     * {@link #value}. Furthermore, its value is implementation-dependent (which is
     * an other raison why it should not be serialized).
     */
    private transient final int clé;

    /**
     * The enum value. This field is public for compatibility
     * with {@link org.opengis.cs.CS_AxisOrientationEnum}.
     */
    public final int value;

    /**
     * Construct a new enum with the specified value.
     */
    private AxisOrientation(final int value, final int clé)
    {
        this.value = value;
        this.clé   = clé;
    }

    /**
     * Return the enum for the specified value. This method is provided for
     * compatibility with {@link org.opengis.cs.CS_AxisOrientationEnum}.
     *
     * @param  value The enum value.
     * @return The enum for the specified value.
     * @throws NoSuchElementException if there is no enum for the specified value.
     */
    public static AxisOrientation getEnum(final int value) throws NoSuchElementException
    {
        if (value>=0 && value<ENUMS.length) return ENUMS[value];
        throw new NoSuchElementException(String.valueOf(value));
    }

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
     * Returns a hash value for this enum.
     */
    public int hashCode()
    {return value;}

    /**
     * Compares the specified object with
     * this enum for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof AxisOrientation)
        {
            final AxisOrientation that = (AxisOrientation) object;
            return this.value == that.value;
        }
        else return false;
    }

    /**
     * Returns a string représentation of this enum.
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
     * Use a single instance of {@link AxisOrientation} after deserialization.
     * It allow client code to test <code>enum1==enum2</code> instead of
     * <code>enum1.equals(enum2)</code>.
     *
     * @return A single instance of this enum.
     * @throws ObjectStreamException is deserialization failed.
     */
    private Object readResolve() throws ObjectStreamException
    {
        if (value>=0 && value<ENUMS.length) return ENUMS[value]; // Canonicalize
        else return ENUMS[0]; // Collapse unknow value to a single canonical one
    }
}
