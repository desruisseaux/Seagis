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
package net.seas.opengis.ct;

// Miscellaneous
import net.seas.util.XClass;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;


/**
 * Semantic type of transform used in coordinate transformation.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_TransformType
 */
public class TransformType implements Serializable
{
    /**
     * Serial number for compatibility with different versions.
     */
    // private static final long serialVersionUID = ?; // TODO

    /**
     * Unknown or unspecified type of transform.
     */
    public static final TransformType OTHER = new TransformType(0);

    /**
     * Transform depends only on defined parameters.
     * For example, a cartographic projection.
     */
    public static final TransformType CONVERSION = new TransformType(1);

    /**
     * Transform depends only on empirically derived parameters.
     * For example a datum transformation.
     */
    public static final TransformType TRANSFORMATION = new TransformType(2);

    /**
     * Transform depends on both defined and empirical parameters.
     */
    public static final TransformType CONVERSION_AND_TRANSFORMATION = new TransformType(3);

    /**
     * Transform types by value. Used to
     * canonicalize after deserialization.
     */
    private static final TransformType[] ENUMS =
    {
        OTHER,
        CONVERSION,
        TRANSFORMATION,
        CONVERSION_AND_TRANSFORMATION
    };

    /**
     * Enum names. TODO: localize!
     */
    private static final String[] NAMES =
    {
        "Other",
        "Conversion",
        "Transformation",
        "Conversion and transformation"
    };

    /**
     * The enum value. This field is public for compatibility
     * with {@link org.opengis.ct.CT_TransformType}.
     */
    public final int value;

    /**
     * Construct a new enum value.
     */
    private TransformType(final int value)
    {this.value = value;}

    /**
     * Return the enum for the specified value. This method is provided
     * for compatibility with {@link org.opengis.ct.CT_TransformType}.
     *
     * @param  value The enum value.
     * @return The enum for the specified value.
     * @throws NoSuchElementException if there is no enum for the specified value.
     */
    public static TransformType getEnum(final int value) throws NoSuchElementException
    {
        if (value>=1 && value<ENUMS.length) return ENUMS[value];
        throw new NoSuchElementException(String.valueOf(value));
    }

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
        if (object!=null && getClass().equals(object.getClass()))
        {
            return ((TransformType) object).value == value;
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
        if (value>=0 && value<NAMES.length)
            buffer.append(NAMES[value]);
        else
            buffer.append(value);
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Use a single instance of {@link TransformType} after deserialization.
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
