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
import fr.ird.resources.XArray;


/**
 * A sum of powers in one or more {@linkplain Variable variables}
 * multiplied by coefficients.
 *
 * @author  Martin Desruisseaux
 */
public final class Polynomial extends Element {
    /**
     * The monomials. <strong>This array is read-only</strong>.
     */
    final Monomial[] monomials;

    /**
     * Creates a new polynomial.
     */
    private Polynomial(final Monomial[] monomials) {
        this.monomials = monomials;
        assert checkNullElements(monomials, false);
    }

    /**
     * Constructs a polynomial made of a single monomial.
     */
    private Polynomial(final Monomial monomial) {
        this(new Monomial[] {monomial});
    }

    /**
     * Constructs a polynomial made of a single constant.
     */
    public Polynomial(final double constant) {
        this(new Monomial(constant));
    }

    /**
     * Constructs a polynomial made of a single variable.
     *
     * @param variable The variable,
     */
    public Polynomial(final Variable variable) {
        this(1, variable, 1);
    }

    /**
     * Constructs a polynomial.
     *
     * @param coefficient The coefficient,
     * @param variable    The variable,
     * @param power       The power for the variable.
     */
    public Polynomial(final double coefficient, final Variable variable, final int power) {
        this(new Monomial(coefficient, new Factor[] {new Factor(variable, power)}));
    }

    /**
     * Returns the monomials at the given index.
     */
    final Polynomial getMonomial(final int index) {
        return new Polynomial(monomials[index]);
    }

    /**
     * Add the given polynomial to this one.
     *
     * @param  term The polynomial to add to this one.
     * @return The result of the addition.
     */
    public Polynomial add(final Polynomial term) {
        final Monomial[] m = new Monomial[this.monomials.length + term.monomials.length];
        System.arraycopy(this.monomials, 0, m, 0,                 this.monomials.length);
        System.arraycopy(term.monomials, 0, m, monomials.length,  term.monomials.length);
        return new Polynomial(m);
    }

    /**
     * Substract the given polynomial from this one.
     *
     * @param  term The polynomial to substract from this one.
     * @return The result of the substraction.
     */
    public Polynomial substract(final Polynomial term) {
        final Polynomial s = add(term);
        for (int i=monomials.length; i<s.monomials.length; i++) {
            s.monomials[i] = s.monomials[i].multiply(-1);
        }
        return s;
    }

    /**
     * Multiply this polynomial by a constant.
     *
     * @param  constant The constant to multiply by.
     * @return The result of the multiplication.
     */
    public Polynomial multiply(double constant) {
        if (constant == 1) {
            return this;
        }
        final Monomial[] m = new Monomial[monomials.length];
        for (int i=0; i<m.length; i++) {
            m[i] = monomials[i].multiply(constant);
        }
        return new Polynomial(m);
    }

    /**
     * Multiply this polynomial by the given one.
     *
     * @param  term The polynomial to multiply to this one.
     * @return The result of the multiplication.
     */
    public Polynomial multiply(final Polynomial term) {
        int c = 0;
        final Monomial[] m = new Monomial[monomials.length * term.monomials.length];
        for (int i=0; i<monomials.length; i++) {
            for (int j=0; j<term.monomials.length; j++) {
                m[c++] = monomials[i].multiply(term.monomials[j]);
            }
        }
        assert c == m.length : c;
        return new Polynomial(m);
    }

    /**
     * Multiply this polynomial by the given one.
     * Note: current implementation work only if the divider is a monomial.
     *
     * @param  term The divider.
     * @return The result of the division.
     */
    public Polynomial divide(final Polynomial divider) {
        return multiply(divider.simplify().power(-1));
    }

    /**
     * Raise this polynomial to a power.
     *
     * @param  power The power.
     * @return The polynomial raised to the given power.
     */
    public Polynomial power(int power) {
        switch (monomials.length) {
            case 0: return this;
            case 1: return new Polynomial(monomials[0].power(power));
        }
        if (power >= 1) {
            Polynomial p = this;
            while (--power != 0) {
                p = multiply(p);
            }
            return p;
        }
        throw new ArithmeticException("Not yet implemented");
    }

    /**
     * Returns the partial derivative.
     */
    public Polynomial derivate(final Variable variable) {
        Monomial[] d = new Monomial[monomials.length];
        int length = 0;
        for (int i=0; i<d.length; i++) {
            d[length] = monomials[i].derivate(variable);
            if (d[length] != null) {
                length++;
            }
        }
        d = XArray.resize(d, length);
        return new Polynomial(d);
    }

    /**
     * Simplify this polynomial.
     *
     * @return The simplified polynomial.
     */
    public Polynomial simplify() {
        Monomial[] simplified = new Monomial[monomials.length];
        for (int i=0; i<simplified.length; i++) {
            simplified[i] = monomials[i].simplify();
        }
        int length = 0;
        for (int i=0; i<simplified.length; i++) {
            Monomial m = simplified[i];
            if (m != null) {
                double coefficient = m.coefficient;
                for (int j=i; ++j<simplified.length;) {
                    if (m.compatibleFactors(simplified[j])) {
                        coefficient += simplified[j].coefficient;
                        simplified[j] = null;
                    }
                }
                if (coefficient != 0) {
                    if (coefficient != m.coefficient) {
                        m = new Monomial(coefficient, m.factors);
                    }
                    simplified[length++] = m;
                }
            }
        }
        simplified = XArray.resize(simplified, length);
        return new Polynomial(simplified);
    }

    /**
     * Factorize the given variable. The given variable will be removed from
     * this polynomial and the result stored elements of the returned array,
     * at an index equals to the variable power. For example if the variable
     * <var>x</var> is factorized in <code>4 + 2xy² + 3x²y</code>, then the
     * returned array will contains:
     * <pre>
     *   [0]  =  4
     *   [1]  =  2y²
     *   [2]  =  3y
     * </pre>
     */
    public Polynomial[] factor(final Variable variable) {
        Polynomial[] polynoms = new Polynomial[4];
        for (int i=0; i<monomials.length; i++) {
            final Monomial[] factors = monomials[i].factor(variable);
            if (factors.length > polynoms.length) {
                polynoms = XArray.resize(polynoms, factors.length);
            }
            for (int p=0; p<factors.length; p++) {
                final Monomial m = factors[p];
                if (m != null) {
                    final Polynomial pm = new Polynomial(m);
                    if (polynoms[p] == null) {
                        polynoms[p] = pm;
                    } else {
                        polynoms[p] = polynoms[p].add(pm);
                    }
                }
            }
        }
        int length = polynoms.length;
        while (length != 0) {
            if (polynoms[length-1] != null) {
                break;
            }
            length--;
        }
        polynoms = XArray.resize(polynoms, length);
        return polynoms;
    }

    /**
     * Substitute the specified variable by the given polynomial.
     */
    public Polynomial substitute(final Variable variable, final Polynomial polynomial) {
        final Polynomial[] parts = factor(variable);
        Polynomial subst  = polynomial;
        Polynomial result = parts[0];
        for (int i=1; i<parts.length; i++) {
            if (i != 1) {
                subst = subst.multiply(polynomial);
            }
            result = result.add(parts[i].multiply(subst));
        }
        return result;
    }

    /**
     * Constructs a string representation for this monomial.
     */
    void toString(final StringBuilder buffer) {
        for (int i=0; i<monomials.length; i++) {
            if (i != 0) {
                buffer.append('+');
            }
            final int pos = buffer.length();
            buffer.append(monomials[i]);
            if (pos!=0 && pos<buffer.length() && buffer.charAt(pos)=='-' && buffer.charAt(pos-1)=='+') {
                buffer.deleteCharAt(pos-1);
            }
        }
    }
}
