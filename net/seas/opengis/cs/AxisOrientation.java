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
import net.seas.util.XClass;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;


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
     * Serial number for compatibility with different versions.
     * TODO: compute serialver
     */
    //private static final long serialVersionUID = ?;

    /**
     * The enum value. This field is public for compatibility
     * with {@link org.opengis.cs.CS_AxisOrientationEnum} only.
     */
    public final int value;

    /**
     * Unknown or unspecified axis orientation.
     * This can be used for local or fitted coordinate systems. 
     */
    public static final AxisOrientation OTHER = new AxisOrientation(0);

    /**
     * Increasing ordinates values go North. This is usually
     * used for Grid Y coordinates and Latitude.
     */
    public static final AxisOrientation NORTH = new AxisOrientation(1);

    /**
     * Increasing ordinates values go South.
     * This is rarely used.
     */
    public static final AxisOrientation SOUTH = new AxisOrientation(2);

    /**
     * Increasing ordinates values go East.
     * This is rarely used.
     */
    public static final AxisOrientation EAST = new AxisOrientation(3);

    /**
     * Increasing ordinates values go West.
     * This is usually used for Grid X coordinates and Longitude.
     */
    public static final AxisOrientation WEST = new AxisOrientation(4);

    /**
     * Increasing ordinates values go up.
     * This is used for vertical coordinate systems.
     */
    public static final AxisOrientation UP = new AxisOrientation(5);

    /**
     * Increasing ordinates values go down.
     * This is used for vertical coordinate systems.
     */
    public static final AxisOrientation DOWN = new AxisOrientation(6);

    /**
     * Enum names. TODO: localize!
     */
    private static final String[] NAMES = {"Other","North","South","East","West","Up","Down"};

    /**
     * Axis orientations by value. Used to
     * canonicalize after deserialization.
     */
    private static final AxisOrientation[] ENUMS = {OTHER,NORTH,SOUTH,EAST,WEST,UP,DOWN};

    /**
     * Construct a new enum with the specified value.
     */
    private AxisOrientation(final int value)
    {this.value = value;}

    /**
     * Return the enum for the specified value. This method is provided for
     * compatibility with {@link org.opengis.cs.CS_AxisOrientationEnum} only.
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
     * Returns a string représentation of this enum.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        if (value>=0 && value<NAMES.length)
            buffer.append(NAMES[value]);
        else
            buffer.append(value);
        buffer.append(']');
        return buffer.toString();
    }

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
     * Returns a hash value for this enum.
     */
    public int hashCode()
    {return value;}

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
        else return this;
    }
}
