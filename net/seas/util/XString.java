/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
     * Liste de cha�nes de caract�res ne contenant que des
     * espaces blancs. Ces cha�nes auront diff�rentes longueurs
     */
    private static final String[] spacesFactory = new String[20];

    /**
     * Renvoie une cha�ne de caract�res ne contenant que des espaces. Cette
     * cha�ne pourra �tre transmise en argument � des m�thodes telles que
     * {@link java.lang.StringBuffer#insert(int,char[])} pour aligner les
     * enregistrements d'un tableau �crit avec une police non-proportionelle.
     *
     * Afin d'am�liorer la performance, cette m�thode tient une liste sp�ciale
     * de cha�nes courtes (moins de 20 caract�res), qui seront retourn�es d'un
     * appel � l'autre plut�t que de cr�er de nouvelles cha�nes � chaque fois.
     *
     * @param  length Longueur souhait�e de la cha�ne. Cette longueur peut
     *         �tre n�gative. Dans ce cas, la cha�ne retourn�e aura une
     *         longueur de 0.
     * @return Cha�ne de caract�res de longueur <code>length</code>
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
     * Retourne le nombre de ligne et de colonnes du texte sp�cifi�. Les caract�res
     * <code>'\r'</code>, <code>'\n'</code> or <code>"\r\n"</code> sont accept�s comme
     * s�parateur de ligne. La largeur retourn�e sera la longueur de la ligne la plus
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
