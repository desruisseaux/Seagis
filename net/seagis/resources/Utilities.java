/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seagis.resources;

// Miscellaneous
import java.util.Arrays;


/**
 * A set of miscellaneous methods.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Utilities
{
    /**
     * Forbive object creation.
     */
    private Utilities()
    {}

    /**
     * An array of strings containing only white spaces. String length are equal
     * to their index + 1 in the <code>spacesFactory</code> array.  For example,
     * <code>spacesFactory[4]</code> contains a string of length 5.  Strings are
     * constructed only when first needed.
     */
    private static final String[] spacesFactory = new String[20];

    /**
     * Returns a string of the specified length filled with white spaces.
     * This method try to returns a pre-allocated string if possible.
     *
     * @param  length The string length. Negative values are clamp to 0.
     * @return A string of length <code>length</code> filled with with spaces.
     */
    public static String spaces(int length)
    {
        // No need to synchronize.   In the unlikely case where two threads
        // call this method in the same time and the two calls create a new
        // string,  the String.intern() call will take care to canonicalize
        // the strings.
        final int last = spacesFactory.length-1;
        if (length<0) length=0;
        if (length <= last)
        {
            if (spacesFactory[length]==null)
            {
                if (spacesFactory[last]==null)
                {
                    char[] blancs = new char[last];
                    Arrays.fill(blancs, ' ');
                    spacesFactory[last]=new String(blancs).intern();
                }
                spacesFactory[length] = spacesFactory[last].substring(0,length).intern();
            }
            return spacesFactory[length];
        }
        else
        {
            char[] blancs = new char[length];
            Arrays.fill(blancs, ' ');
            return new String(blancs);
        }
    }

    /**
     * Convenience method for testing two objects for
     * equality. One or both objects may be null.
     */
    public static boolean equals(final Object object1, final Object object2)
    {return (object1==object2) || (object1!=null && object1.equals(object2));}

    /**
     * Returns a short class name for the specified object. This method will
     * ommit the package name. For exemple, it will returns "String" instead
     * of "java.lang.String" for a {@link String} object.
     *
     * @param  object The object (may be <code>null</code>).
     * @return A short class name for the specified object.
     */
    public static String getShortClassName(final Object object)
    {
        if (object==null) return "<*>";
        String name = object.getClass().getName();
        int   lower = name.lastIndexOf('.');
        int   upper = name.length();
        return name.substring(lower+1, upper).replace('$','.');
    }
}
