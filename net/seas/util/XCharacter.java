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
import java.awt.Dimension;


/**
 * Simple operations on characters.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class XCharacter
{
    /**
     * Interdit toute construction
     * d'objet de cette classe.
     */
    private XCharacter()
    {}

    /**
     * Indique si le caractère spécifié est un exposant. Les exposants sont compris principalement de \\u2070 à \\u207F.
     * Les caractères disponibles sont forme d'exposants comprennent les chiffres de 0 à 9 ainsi que quelques symboles:
     *
     * <blockquote><pre>
     * \u2070 \u00B9 \u00B2 \u00B3 \u2074 \u2075 \u2076 \u2077 \u2078 \u2079 \u207A \u207B \u207C \u207D \u207E \u207F
     * </pre></blockquote>
     */
    public static boolean isSuperScript(final char c)
    {
        switch (c)
        {
            /*1*/case '\u2071':
            /*2*/case '\u2072':
            /*3*/case '\u2073': return false;
            /*1*/case '\u00B9':
            /*2*/case '\u00B2':
            /*3*/case '\u00B3': return true;
        }
        return (c>='\u2070' && c<='\u207F');
    }

    /**
     * Indique si le caractère spécifié est un indice. Les indices sont compris principalement de \\u2080 à \\u208E.
     * Les caractères disponibles sont forme d'exposants comprennent les chiffres de 0 à 9 ainsi que quelques symboles:
     *
     * <blockquote><pre>
     * \u2080 \u2081 \u2082 \u2083 \u2084 \u2085 \u2086 \u2087 \u2088 \u2089 \u208A \u208B \u208C \u208D \u208E
     * </pre></blockquote>
     */
    public static boolean isSubScript(final char c)
    {
        return (c>='\u2080' && c<='\u208E');
    }

    /**
     * Convertit le caractère spécifié en exposant. Les caractères qui peuvent être convertit sont:
     *
     * <blockquote><pre>
     * 0 1 2 3 4 5 6 7 8 9 + - = ( ) n
     * </pre></blockquote>
     */
    public static char toSuperScript(final char c)
    {
        switch (c)
        {
            case '1': return '\u00B9';
            case '2': return '\u00B2';
            case '3': return '\u00B3';
            case '+': return '\u207A';
            case '-': return '\u207B';
            case '=': return '\u207C';
            case '(': return '\u207D';
            case ')': return '\u207E';
            case 'n': return '\u207F';
        }
        if (c>='0' && c<='9') return (char) (c+('\u2070'-'0'));
        return c;
    }

    /**
     * Convertit le caractère spécifié en indice. Les caractères qui peuvent être convertit sont:
     *
     * <blockquote><pre>
     * 0 1 2 3 4 5 6 7 8 9 + - = ( ) n
     * </pre></blockquote>
     */
    public static char toSubScript(final char c)
    {
        switch (c)
        {
            case '+': return '\u208A';
            case '-': return '\u208B';
            case '=': return '\u208C';
            case '(': return '\u208D';
            case ')': return '\u208E';
        }
        if (c>='0' && c<='9') return (char) (c+('\u2080'-'0'));
        return c;
    }

    /**
     * Convertit l'indice ou l'exposant spécifié en caractère normal.
     */
    public static char toNormalScript(final char c)
    {
        switch (c)
        {
            case '\u00B9': return '1';
            case '\u00B2': return '2';
            case '\u00B3': return '3';
            case '\u2071': return c;
            case '\u2072': return c;
            case '\u2073': return c;
            case '\u207A': return '+';
            case '\u207B': return '-';
            case '\u207C': return '=';
            case '\u207D': return '(';
            case '\u207E': return ')';
            case '\u207F': return 'n';
            case '\u208A': return '+';
            case '\u208B': return '-';
            case '\u208C': return '=';
            case '\u208D': return '(';
            case '\u208E': return ')';
        }
        if (c>='\u2070' && c<='\u2079') return (char) (c-('\u2070'-'0'));
        if (c>='\u2080' && c<='\u2089') return (char) (c-('\u2080'-'0'));
        return c;
    }

    /**
     * Retourne le nombre de ligne et de colonnes du texte spécifié. Les caractères
     * <code>'\r'</code>, <code>'\n'</code> or <code>"\r\n"</code> sont acceptés comme
     * séparateur de ligne. La largeur retournée sera la longueur de la ligne la plus
     * longue.
     */
    public static Dimension getSize(final CharSequence text)
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
