/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2004 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 */
package fr.ird.calculus;

// Dependencies
import org.geotools.resources.Utilities;


/**
 * A {@linkplain Variable variable} raised to a power.
 *
 * @author  Martin Desruisseaux
 */
final class Factor extends Element {
    /**
     * The variable.
     */
    final Variable variable;

    /**
     * The power.
     */
    final int power;

    /**
     * Creates a new factor.
     */
    public Factor(final Variable variable, final int power) {
        this.variable = variable;
        this.power    = power;
        assert variable != null;
    }

    /**
     * Raise this factor to a power.
     */
    public Factor power(final int p) {
        if (power == 1) {
            return this;
        }
        return new Factor(variable, power * p);
    }

    /**
     * Returns the sum of the power of all factors in the given array
     * which use the same variable than this factor. This method is
     * for internal use by {@link Monomial#compatibleFactors}.
     *
     * @param factors The factor array to check. This array will be overwritted.
     *                Never specify the internal array of a {@link Monomial} object.
     * @param from    The first index to consider in the <code>factors</code> array
     *                (usually 0).
     */
    final int powerOf(final Factor[] factors, int from) {
        int sum = 0;
        for (; from<factors.length; from++) {
            final Factor factor = factors[from];
            if (factor!=null && variable.equals(factor.variable)) {
                sum += factor.power;
                factors[from] = null;
            }
        }
        return sum;
    }

    /**
     * Constructs a string representation for this factor.
     */
    void toString(final StringBuilder buffer) {
        buffer.append(variable);
        if (power != 1) {
            final String p = String.valueOf(power);
            final int length = p.length();
            for (int i=0; i<length; i++) {
                buffer.append(Utilities.toSuperScript(p.charAt(i)));
            }
        }
    }
}
