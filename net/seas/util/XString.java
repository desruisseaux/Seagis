/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
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
package net.seas.util;

// Miscellaneous
import java.util.Arrays;
import java.awt.Dimension;


/**
 * Simple operations on {@link java.lang.String} and {@link java.lang.StringBuffer} objects.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class XString
{
    /**
     * Toute construction d'objet
     * de cette classe est interdites.
     */
    private XString()
    {}

    /**
     * Liste de chaînes de caractères ne contenant que des
     * espaces blancs. Ces chaînes auront différentes longueurs
     */
    private static final String[] spacesFactory = new String[20];

    /**
     * Renvoie une chaîne de caractères ne contenant que des espaces. Cette
     * chaîne pourra être transmise en argument à des méthodes telles que
     * {@link java.lang.StringBuffer#insert(int,char[])} pour aligner les
     * enregistrements d'un tableau écrit avec une police non-proportionelle.
     *
     * Afin d'améliorer la performance, cette méthode tient une liste spéciale
     * de chaînes courtes (moins de 20 caractères), qui seront retournées d'un
     * appel à l'autre plutôt que de créer de nouvelles chaînes à chaque fois.
     *
     * @param  length Longueur souhaitée de la chaîne. Cette longueur peut
     *         être négative. Dans ce cas, la chaîne retournée aura une
     *         longueur de 0.
     * @return Chaîne de caractères de longueur <code>length</code>
     *         ne contenant que des espaces.
     */
    public static String spaces(int length)
    {
        final int last=spacesFactory.length-1;
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
     * Retourne le nombre de ligne et de colonnes du texte spécifié. Les caractères
     * <code>'\r'</code>, <code>'\n'</code> or <code>"\r\n"</code> sont acceptés comme
     * séparateur de ligne. La largeur retournée sera la longueur de la ligne la plus
     * longue.
     */
    public static Dimension getSize(final /*CharSequence*/String text) // CharSequence: J2SE 1.4
    {
        final int length = text.length();
        int height = 0;
        int width  = 0;
        int lower  = 0;
        int upper  = 0;
        while (upper < length)
        {
            final char c=text.charAt(upper++);
            if (c=='\r' || c=='\n')
            {
                final int lineWidth = upper-lower;
                if (lineWidth>width) width=lineWidth;
                if (c=='\r' && upper<length && text.charAt(upper)=='\n')
                {
                    upper++;
                }
                lower = upper;
                height++;
            }
        }
        final int lineWidth = upper-lower;
        if (lineWidth>width) width=lineWidth;
        if (lineWidth>0) height++;
        return new Dimension(width, height);
    }
}
