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
 *
 *       This class contains formulas from Ken Turkiwski's Open Source Repository
 *       (http://www.worldserver.com/turk/opensource). Ken's work is fully aknowledge here.
 */
package fr.ird.science.math;

// Geotools dependencies
import org.geotools.resources.XMath;


/**
 * Compute the root of a polynomial equation.
 *
 * @version $Id$
 * @author Ken Turkiwski
 * @author Martin Desruisseaux
 */
public class Polynom {
    /**
     * The array when no real roots can be computed.
     */
    private static final double[] NO_REAL_ROOT = new double[0];

    /**
     * Do not allows creation of instance of this class.
     */
    private Polynom() {
    }

    /**
     * Find the roots of a polynomial equation. More specifically,
     * this method solve the following equation:
     *
     * <blockquote><code>
     * c[0] +
     * c[1]*<var>x</var> +
     * c[2]*<var>x</var><sup>2</sup> +
     * c[3]*<var>x</var><sup>3</sup> +
     * ... +
     * c[n]*<var>x</var><sup>n</sup> == 0
     * </code></blockquote>
     *
     * where <var>n</var> is the array length minus 1.
     *
     * @param  c The coefficients for the polynomial equation.
     * @return The roots. This array may have any length up to <code>n-1</code>.
     */
    public static double[] findRoots(final double[] c) {
        int n = c.length;
        while (n!=0 && c[--n]==0);
        switch (n) {
            case 0: return NO_REAL_ROOT;
            case 1: return new double[] {-c[0]/c[1]};
            case 2: return quadraticRoots(c[0], c[1], c[2]);
            case 3: return cubicRoots(c[0], c[1], c[2], c[3]);
            default: throw new UnsupportedOperationException(String.valueOf(n));
        }
    }

    /**
     * Find the roots of a quadratic equation.
     * More specifically, this method solves the following equation:
     *
     * <blockquote><code>
     * c0 +
     * c1*<var>x</var> +
     * c2*<var>x</var><sup>2</sup> == 0
     * </code></blockquote>
     *
     * @return The roots. The length may be 1 or 2.
     */
    private static double[] quadraticRoots(double c0, double c1, double c2) {
        double d = c1*c1 - 4*c2*c0;
        if (d > 0) {
            // Two real, distinct roots
            d = Math.sqrt(d);
            if (c1 < 0) {
                d = -d;
            }
            final double q = 0.5*(d - c1);
            return new double[] {
                q/c2,
                (q!=0) ? c0/q : -0.5*(d + c1)/c2
            };
        } else if (d == 0) {
            // One real double root
            return new double[] {
                -c1 / (2*c2)
            };
        } else {
            // Two complex conjugate roots
            return NO_REAL_ROOT;
        }
    }

    /**
     * Find the roots of a cubic equation.
     * More specifically, this method solves the following equation:
     *
     * <blockquote><code>
     * c0 +
     * c1*<var>x</var> +
     * c2*<var>x</var><sup>2</sup> +
     * c3*<var>x</var><sup>3</sup> == 0
     * </code></blockquote>
     *
     * @return The roots. The length may be 1 or 3.
     */
    private static double[] cubicRoots(double c0, double c1, double c2, double c3) {
        c2 /= c3;
        c1 /= c3;
        c0 /= c3;
        final double Q  = (c2*c2 - 3*c1) / 9;
        final double R = (2*c2*c2*c2 - 9*c2*c1 + 27*c0) / 54;
        final double Qcubed = Q*Q*Q;
        final double d = Qcubed - R*R;

        c2 /= 3;
        if (d >= 0) {
            final double theta = Math.acos(R / Math.sqrt(Qcubed)) / 3;
            final double scale = -2 * Math.sqrt(Q);
            final double[] roots = new double[] {
                scale * Math.cos(theta              ) - c2,
                scale * Math.cos(theta + Math.PI*2/3) - c2,
                scale * Math.cos(theta + Math.PI*4/3) - c2
            };
            assert Math.abs(roots[0]*roots[1]*roots[2] + c0  ) < 1E-6;
            assert Math.abs(roots[0]+roots[1]+roots[2] + c2*3) < 1E-6;
            assert Math.abs(roots[0]*roots[1] +
                            roots[0]*roots[2] +
                            roots[1]*roots[2] - c1) < 1E-6;
            return roots;
        } else {
            double e = XMath.cbrt(Math.sqrt(-d) + Math.abs(R));
            if (R > 0) {
                e = -e;
            }
            return new double[] {
                (e + Q/e) - c2
            };
        }
    }

    /**
     * Display to the standard output the roots of a polynomial equation.
     * More specifically, this method solve the following equation:
     *
     * <blockquote><code>
     * c[0] +
     * c[1]*<var>x</var> +
     * c[2]*<var>x</var><sup>2</sup> +
     * c[3]*<var>x</var><sup>3</sup> +
     * ... +
     * c[n]*<var>x</var><sup>n</sup> == 0
     * </code></blockquote>
     *
     * where <var>n</var> is the array length minus 1.
     *
     * @param c The coefficients for the polynomial equation.
     */
    public static void main(final String[] c) {
        final double[] r = new double[c.length];
        for (int i=0; i<c.length; i++) {
            r[i] = Double.parseDouble(c[i]);
        }
        final double[] roots = findRoots(r);
        for (int i=0; i<roots.length; i++) {
            System.out.println(roots[i]);
        }
    }
}
