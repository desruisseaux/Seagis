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
import java.lang.Math;


/**
 * Simple mathematical functions. Some of those function will
 * be removed if JavaSoft provide a standard implementation
 * or fix some issues in Bug Parade:<br>
 * <ul>
 *   <li><a href="http://developer.java.sun.com/developer/bugParade/bugs/4074599.html">Implement log10 (base 10 logarithm)</a></li>
 *   <li><a href="http://developer.java.sun.com/developer/bugParade/bugs/4358794.html">implement pow10 (power of 10) with optimization for integer powers</a>/li>
 *   <li><a href="http://developer.java.sun.com/developer/bugParade/bugs/4461243.html">Math.acos is very slow</a></li>
 * </ul>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class XMath
{
    /**
     * Logarithme naturel de 10.
     */
    private static final double LN10=2.302585092994;
    
    /**
     * Table de puissance de 10. Elle sera utilis�
     * pour un calcul rapide de 10^x.
     */
    private static final double[] POW10 =
    {
        1E+00, 1E+01, 1E+02, 1E+03, 1E+04, 1E+05, 1E+06, 1E+07, 1E+08, 1E+09, 1E+10, 1E+11,
        1E+12, 1E+13, 1E+14, 1E+15, 1E+16, 1E+17, 1E+18, 1E+19, 1E+20, 1E+21, 1E+22, 1E+23
    };

    /**
     * Interdit toute construction
     * d'instance de cette classe.
     */
    private XMath()
    {}
    
    /**
     * Renvoie l'hypoth�nuse d'un triangle droit (<code>sqrt(x�+y�)</code>).
     */
    public static double hypot(double x, double y)
    {return Math.sqrt(x*x + y*y);}

    /**
     * Renvoie le logarithme en base 10 de <var>x</var>. Note: cette m�thode
     * dispara�tra dans une version future is JavaSoft donne suite � RFE 4074599.
     */
    public static double log10(double x)
    {return Math.log(x)/LN10;}
    
    /**
     * Renvoie 10 �lev� � la puissance <var>x</var>.
     */
    public static double pow10(double x)
    {
        final int ix=(int) x;
        if (ix==x) return pow10(ix);
        else return Math.pow(10.0, x);
    }
    
    /**
     * Renvoie 10 �lev� � la puissance <var>x</var>.
     * Ce calcul sera tr�s rapide pour des petites
     * valeurs de <var>x</var>.
     */
    public static double pow10(final int x)
    {
        if (x>=0)
        {
            if (x<POW10.length) return POW10[x];
        }
        else if (x!=Integer.MIN_VALUE)
        {
            final int nx = -x;
            if (nx<POW10.length) return 1/POW10[nx];
        }
        try
        {
            /*
             * Note: La m�thode "Math.pow(10,x)" a des erreurs d'arrondissements.
             *       La m�thode "Double.parseDouble(String)" donne un r�sultat plus
             *       pr�cis. Evidemment, la performance est d�sastreuse, mais on
             *       esp�re que cette solution n'est que temporaire.
             */
            return Double.parseDouble("1E"+x);
        }
        catch (NumberFormatException exception)
        {
            return StrictMath.pow(10, x);
        }
    }

    /**
     * Retourne une approximation de l'arc cosinus.  Cette m�thode n'existe que pour contourner
     * l'HALUCINANTE LENTEUR de la fonction {@link Math#acos} standard. Cette derni�re, appel�e
     * une seule fois au milieu de 8 autres fonctions trigonom�triques, bouffe � elle seule 60%
     * du CPU!!!  A cause d'elle, les calculs de distances orthodronomiques peuvent prendre des
     * mois!  Cette fonction <code>XMath.acos(double)</code> tente de contourner le probl�me en
     * calculant une approximation de l'arc-cosinus, avec l'�quation <code>acos=sqrt(1-a�)</code>.
     * Cette approximation est valide � 1% pr�s pour des valeurs de <code>a</code> sup�rieures �
     * 0.97, ce qui correspond � des angles inf�rieures � 14� (sur la Terre, cet angle correspond
     * � des distances de 1555 kilom�tres � l'�quateur). Pour les valeurs de <var>a</var> inf�rieures
     * ou �gales � 0.97, cette m�thode utilise la fonction <code>Math.acos(double)</code> standard.
     */
    public static double acos(final double a)
    {return (a>0.97) ? Math.sqrt(1-a*a) : Math.acos(a);}

    /**
     * Renvoie le signe du nombre r�el <var>x</var>. Cette fonction
     * retourne -1 si <var>x</var> est n�gatif, 0 si <var>x</var>
     * est nul ou NaN et +1 si <var>x</var> est positif.
     */
    public static int sgn(double x)
    {
        if (x>0) return +1;
        if (x<0) return -1;
        else     return  0;
    }

    /**
     * Renvoie le signe du nombre r�el <var>x</var>. Cette fonction
     * retourne -1 si <var>x</var> est n�gatif, 0 si <var>x</var>
     * est nul ou NaN et +1 si <var>x</var> est positif.
     */
    public static int sgn(float x)
    {
        if (x>0) return +1;
        if (x<0) return -1;
        else     return  0;
    }

    /**
     * Renvoie le signe du nombre entier <var>x</var>. Cette fonction
     * retourne -1 si <var>x</var> est n�gatif, 0 si <var>x</var>
     * est nul et +1 si <var>x</var> est positif.
     */
    public static int sgn(long x)
    {
        if (x>0) return +1;
        if (x<0) return -1;
        else     return  0;
    }

    /**
     * Renvoie le signe du nombre entier <var>x</var>. Cette fonction
     * retourne -1 si <var>x</var> est n�gatif, 0 si <var>x</var>
     * est nul et +1 si <var>x</var> est positif.
     */
    public static int sgn(int x)
    {
        if (x>0) return +1;
        if (x<0) return -1;
        else     return  0;
    }

    /**
     * Renvoie le signe du nombre entier <var>x</var>. Cette fonction
     * retourne -1 si <var>x</var> est n�gatif, 0 si <var>x</var>
     * est nul et +1 si <var>x</var> est positif.
     */
    public static short sgn(short x)
    {
        if (x>0) return (short) +1;
        if (x<0) return (short) -1;
        else     return (short)  0;
    }

    /**
     * Renvoie le signe du nombre entier <var>x</var>. Cette fonction
     * retourne -1 si <var>x</var> est n�gatif, 0 si <var>x</var>
     * est nul et +1 si <var>x</var> est positif.
     */
    public static byte sgn(byte x)
    {
        if (x>0) return (byte) +1;
        if (x<0) return (byte) -1;
        else     return (byte)  0;
    }
}
