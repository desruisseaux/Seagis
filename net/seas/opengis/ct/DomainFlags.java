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
import java.util.Locale;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;
import javax.media.jai.EnumeratedParameter;
import net.seas.resources.Resources;
import net.seas.resources.Clé;
import net.seas.util.XArray;


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
public final class DomainFlags extends EnumeratedParameter
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 579583745178828392L;

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
        {
            String name=null;
            switch (i)
            {
                case 0: name="UNKNOW";        break;
                case 1: name="INSIDE";        break;
                case 2: name="OUTSIDE";       break;
                case 4: name="DISCONTINUOUS"; break;
            }
            ENUMS[i] = new DomainFlags(name, i);
        }
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
     * Construct a new enum value.
     */
    private DomainFlags(final String name, final int value)
    {super(name, value);}

    /**
     * <FONT COLOR="#FF6633">Return the enum for the specified value.
     * This method is provided for compatibility with
     * {@link org.opengis.ct.CT_DomainFlags}.</FONT>
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
     * <FONT COLOR="#FF6633">Returns enum's names in the specified locale.
     * For example if this enum has value "3", then <code>getNames</code>
     * returns an array of two elements: "Inside" and "Outside".</FONT>
     *
     * @param  locale The locale, or <code>null</code> for the current default locale.
     * @return Enum's names in the specified locale (never <code>null</code>).
     */
    public String[] getNames(final Locale locale)
    {
        int            count = 0;
        int             bits = getValue();
        Resources  resources = null;
        final int[]     clés = {Clé.INSIDE, Clé.OUTSIDE, Clé.DISCONTINUOUS};
        final String[] names = new String[clés.length];
        for (int i=0; i<clés.length; i++)
        {
            if ((bits & 1)!=0)
            {
                if (resources==null)
                    resources = Resources.getResources(locale);
                names[count++] = resources.getString(clés[i]);
            }
            bits >>>= 1;
        }
        return XArray.resize(names, count);
    }

    /**
     * <FONT COLOR="#FF6633">Returns a combination of two domain flags.
     * This is equivalent to <code>getEnum(this.{@link #value}&nbsp;|&nbsp;flags.value)</code>.</FONT>
     */
    public DomainFlags or(final DomainFlags flags)
    {return getEnum(getValue() | flags.getValue());}

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
        final int value=getValue();
        if (value>=0 && value<ENUMS.length) return ENUMS[value]; // Canonicalize
        else return ENUMS[0]; // Collapse unknow value to a single canonical one
    }
}
