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
import java.io.Serializable;
import net.seas.util.XClass;


/**
 * A set of quantities from which other quantities are calculated.
 * It may be a textual description and/or a set of parameters describing the
 * relationship of a coordinate system to some predefined physical locations
 * (such as center of mass) and physical directions (such as axis of spin).
 * It can be defined as a set of real points on the earth that have coordinates.
 * For example a datum can be thought of as a set of parameters defining completely
 * the origin and orientation of a coordinate system with respect to the earth.
 * The definition of the datum may also include the temporal behavior (such
 * as the rate of change of the orientation of the coordinate axes).
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_Datum
 */
public class Datum extends Info implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 8521658677671362662L;

    /**
     * The datum name.
     */
    private final String name;

    /**
     * The datum type.
     */
    private final DatumType type;

    /**
     * Construct a new datum with the
     * specified name and datum type.
     *
     * @param name The datum name.
     * @param type The datum type.
     */
    protected Datum(final String name, final DatumType type)
    {
        this.name = name;
        this.type = type;
        ensureNonNull("name", name);
        ensureNonNull("type", type);
    }

    /**
     * Returns this datum name.
     */
    public String getName()
    {return name;}

    /**
     * Gets the type of the datum as an enumerated code.
     */
    public DatumType getDatumType()
    {return type;}

    /**
     * Returns a hash value for this datum.
     */
    public int hashCode()
    {
        int code = super.hashCode();
        final DatumType type = getDatumType();
        if (type!=null) code ^= type.hashCode();
        return code;
    }

    /**
     * Compares the specified object
     * with this datum for equality.
     */
    public boolean equals(final Object object)
    {return (object instanceof Datum) && equals((Datum)object);}

    /**
     * Compares the specified object
     * with this datum for equality.
     */
    final boolean equals(final Datum that)
    {
        return super.equals(that) &&
               XClass.equals(this.getDatumType(), that.getDatumType());
    }

    /**
     * Returns a string representation of this datum.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(getName());
        final DatumType type = getDatumType();
        if (type!=null)
        {
            buffer.append(", ");
            buffer.append(type.getName(null));
        }
        buffer.append(']');
        return buffer.toString();
    }
}
