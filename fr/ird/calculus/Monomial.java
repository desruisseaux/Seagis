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

// J2SE Dependencies
import java.util.Arrays;

// Dependencies
import fr.ird.resources.XArray;


/**
 * On summand in a {@link Polynomial polynomial}. This is one or more
 * {@linkplain Variable variables} raised to some power and multiplied
 * together. This include a coefficient.
 *
 * @author  Martin Desruisseaux
 */
final class Monomial extends Element {
    /**
     * An empty array of factors for constructing constants,
     */
    private static final Factor[] EMPTY_ARRAY = new Factor[0];

    /**
     * The coefficient.
     */
    final double coefficient;

    /**
     * The factors. <strong>This array is read-only</strong>.
     * It is read by {@link Polynomial}, but must stay unchanged.
     */
    final Factor[] factors;

    /**
     * Creates a new monomial with the given coefficient.
     * Example: <code>6</code>.
     *
     * @param The coefficient.
     */
    public Monomial(final double coefficient) {
        this(coefficient, EMPTY_ARRAY);
    }
    
    /**
     * Creates a new monomial with a coefficient of 1.
     * Example: <code>xy²</code>.
     *
     * @param The factors. Must be non-null.
     */
    public Monomial(final Factor[] factors) {
        this(1, factors);
    }

    /**
     * Creates a new monomial.
     * Example: <code>6xy²</code>.
     *
     * @param The coefficient.
     * @param The factors. Must be non-null.
     */
    public Monomial(final double coefficient, final Factor[] factors) {
        this.coefficient = coefficient;
        this.factors     = factors;
        assert checkNullElements(factors, false);
        assert compatibleFactors(this) : this;
    }

    /**
     * Multiply this monomial by a constant.
     * Example: <code>6&times;3xy² = 18xy²</code>.
     *
     * @param  constant The constant to multiply by.
     * @return The result of the multiplication.
     */
    public Monomial multiply(double constant) {
        if (constant == 1) {
            return this;
        }
        return new Monomial(coefficient*constant, factors);
    }

    /**
     * Multiply this monomial by the given one.
     * Example: <code>3x&times;2yx² = 6xyx² = 6x³y</code>.
     * The later simplification is performed only if {@link #simplify} is invoked.
     *
     * @param  term The monomial to multiply by this one.
     * @return The result of the multiplication.
     */
    public Monomial multiply(final Monomial term) {
        final Factor[] f = new Factor[this.factors.length + term.factors.length];
        System.arraycopy(this.factors, 0, f, 0,              this.factors.length);
        System.arraycopy(term.factors, 0, f, factors.length, term.factors.length);
        return new Monomial(coefficient*term.coefficient, f);
    }

    /**
     * Raise this monomial to a power.
     * Example: <code>(2y)² = 4y²</code>.
     *
     * @param  power The power.
     * @return The monomial raised to the given power.
     */
    public Monomial power(final int power) {
        if (power == 1) {
            return this;
        }
        final Factor[] f = new Factor[factors.length];
        for (int i=0; i<f.length; i++) {
            f[i] = factors[i].power(power);
        }
        return new Monomial(Math.pow(coefficient, power), f);
    }

    /**
     * Returns the partial derivative, or <code>null</code> if the result is zero.
     * Example: <code>d/dx 2yx² = 4yx</code>.
     */
    public Monomial derivate(final Variable variable) {
        int power =  0;
        int last  = -1; // Index of the LAST occurence only.
        for (int i=0; i<factors.length; i++) {
            if (variable.equals(factors[i].variable)) {
                power += factors[i].power;
                last   = i;
            }
        }
        if (last >= 0) {
            final Factor[] f = factors.clone();
            f[last] = new Factor(f[last].variable, f[last].power-1);
            return new Monomial(coefficient*power, f);
        }
        return null;
    }

    /**
     * Simplify this monomial. Each {@linkplain Variable variable} will
     * appears only once with its associated power.
     *
     * @return The simplified monomial.
     */
    public Monomial simplify() {
        int length = 0;
        Factor[] simplified = factors.clone();
        for (int i=0; i<simplified.length; i++) {
            Factor factor = simplified[i];
            if (factor != null) {
                final int power = factor.powerOf(simplified, i);
                if (power != 0) {
                    if (power != factor.power) {
                        factor = new Factor(factor.variable, power);
                    }
                    simplified[length++] = factor;
                }
            }
        }
        simplified = resize(simplified, length);
        final Monomial m;
        if (Arrays.equals(factors, simplified)) {
            m = this;
        } else {
            m = new Monomial(coefficient, simplified);
        }
        assert compatibleFactors(m) : m;
        return m;
    }

    /**
     * Verify that it is legal to add this monomial with the specified one.
     *
     * @param  that The monomial to check for compatibility.
     * @return <code>true</code> if the factors of both polynomials are compatible.
     */
    final boolean compatibleFactors(final Monomial that) {
        if (that == null) {
            return false;
        }
        final Factor[] f1 = this.factors.clone();
        final Factor[] f2 = that.factors.clone();
        for (int i=0; i<f1.length; i++) {
            final Factor f = f1[i];
            if (f != null) {
                if (f.powerOf(f1,i) != f.powerOf(f2,0)) {
                    return false;
                }
            }
        }
        return checkNullElements(f1, true) && checkNullElements(f2, true);
    }

    /**
     * Factorize the given variable. The given variable will be removed from
     * this monomial and the result stored in <em>one</em> element of the
     * returned array, at an index equals to the variable power. For example
     * if the variable <var>x</var> is factorized in <code>3x²y</code>, then
     * the monomial <code>3y</code> will be stored at index 2 in the returned
     * array, All others elements will be <code>null</code>.
     */
    public Monomial[] factor(final Variable variable) {
        int power = 0;
        int length = 0;
        Factor[] rest = new Factor[factors.length];
        for (int i=0; i<factors.length; i++) {
            final Factor f = factors[i];
            if (variable.equals(f.variable)) {
                power += f.power;
            } else {
                rest[length++] = f;
            }
        }
        rest = resize(rest, length);
        if (power >= 0) {
            final Monomial[] m = new Monomial[power+1];
            if (length == factors.length) {
                assert Arrays.equals(rest, factors);
                m[power] = this;
            } else {
                m[power] = new Monomial(coefficient, rest);
            }
            return m;
        }
        throw new ArithmeticException("Not yet implemented");
    }

    /**
     * Resize the specified array to the given length.
     */
    private static Factor[] resize(final Factor[] factors, final int length) {
        return (length!=0) ? XArray.resize(factors, length) : EMPTY_ARRAY;
    }

    /**
     * Constructs a string representation for this monomial.
     */
    void toString(final StringBuilder buffer) {
        if (coefficient != 1) {
            if (coefficient == -1) {
                buffer.append('-');
            } else {
                if (coefficient == (int)coefficient) {
                    buffer.append((int)coefficient);
                } else {
                    buffer.append(coefficient);
                }
            }
        }
        for (int i=0; i<factors.length; i++) {
            buffer.append(factors[i]);
        }
    }
}
