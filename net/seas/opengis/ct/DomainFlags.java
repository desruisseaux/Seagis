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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.ct;

// Miscellaneous
import java.util.Locale;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;
import net.seas.resources.Resources;
import net.seas.resources.Cl�;
import net.seas.util.XArray;
import net.seas.util.XClass;


/**
 * Flags indicating parts of domain covered by a convex hull.
 * These flags can be combined.  For example, the enum
 * <code>{@link #INSIDE}.or({@link #OUTSIDE})</code>
 * means that some parts of the convex hull are inside the domain,
 * and some parts of the convex hull are outside the domain.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_DomainFlags
 */
public final class DomainFlags implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 5585150830410796130L;

    /**
     * Domain flags by value. Used to
     * canonicalize after deserialization.
     */
    private static final DomainFlags[] ENUMS = new DomainFlags[8];

    /**
     * Initialize {@link #ENUMS} during class loading.
     * This code must be done before we initialize public fields.
     */
    static
    {
        for (int i=ENUMS.length; --i>=0;)
            ENUMS[i] = new DomainFlags(i);
    };

    /**
     * At least one point in a convex hull is inside the transform's domain.
     */
    public static final DomainFlags INSIDE = ENUMS[1];

    /**
     * At least one point in a convex hull is outside the transform's domain.
     */
    public static final DomainFlags OUTSIDE = ENUMS[2];

    /**
     * At least one point in a convex hull is not transformed continuously.
     * As an example, consider a "Longitude_Rotation" transform which adjusts
     * longitude coordinates to take account of a change in Prime Meridian.
     * If the rotation is 5 degrees east, then the point (Lat=0,Lon=175)
     * is not transformed continuously, since it is on the meridian line
     * which will be split at +180/-180 degrees.
     */
    public static final DomainFlags DISCONTINUOUS = ENUMS[4];

    /**
     * The enum value. This field is public for compatibility
     * with {@link org.opengis.ct.CT_DomainFlags}.
     */
    public final int value;

    /**
     * Construct a new enum value.
     */
    private DomainFlags(final int value)
    {this.value = value;}

    /**
     * Return the enum for the specified value. This method is provided
     * for compatibility with {@link org.opengis.ct.CT_DomainFlags}.
     *
     * @param  value The enum value.
     * @return The enum for the specified value.
     * @throws NoSuchElementException if there is no enum for the specified value.
     */
    public static DomainFlags getEnum(final int value) throws NoSuchElementException
    {
        if (value>=1 && value<ENUMS.length) return ENUMS[value];
        throw new NoSuchElementException(String.valueOf(value));
    }

    /**
     * Returns enum's names in the specified locale. For example if this
     * enum has value "3", then <code>getNames</code> returns an array
     * of two elements: "Inside" and "Outside".
     *
     * @param  locale The locale, or <code>null</code> for the current default locale.
     * @return Enum's names in the specified locale (never <code>null</code>).
     */
    public String[] getNames(final Locale locale)
    {
        int            count = 0;
        int             bits = value;
        Resources  resources = null;
        final int[]     cl�s = {Cl�.INSIDE, Cl�.OUTSIDE, Cl�.DISCONTINUOUS};
        final String[] names = new String[cl�s.length];
        for (int i=0; i<cl�s.length; i++)
        {
            if ((bits & 1)!=0)
            {
                if (resources==null)
                    resources = Resources.getResources(locale);
                names[count++] = resources.getString(cl�s[i]);
            }
            bits >>>= 1;
        }
        return XArray.resize(names, count);
    }

    /**
     * Returns a combination of two domain flags. This is equivalent
     * to <code>getEnum(this.{@link #value} | flags.value)</code>.
     */
    public DomainFlags or(final DomainFlags flags)
    {return getEnum(value | flags.value);}

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
        if (object instanceof DomainFlags)
        {
            return ((DomainFlags) object).value == value;
        }
        else return false;
    }

    /**
     * Returns a string repr�sentation of this enum.
     */
    public String toString()
    {
        final String[]      names = getNames(null);
        final StringBuffer buffer = new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        for (int i=0; i<names.length; i++)
        {
            if (i!=0) buffer.append('|');
            buffer.append(names[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Use a single instance of {@link DomainFlags} after deserialization.
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
