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

/**
 * An equation.
 *
 * @author Martin Desruisseaux
 */
public final class Equation {
    /**
     * The left hand side.
     */
    private final Polynomial left;

    /**
     * The right hand side, or <code>null</code> for 0.
     */
    private final Polynomial right;

    /**
     * Constructs an equation of the form <code>polynomial = 0</code>.
     */
    public Equation(final Polynomial polynomial) {
        this(polynomial, null);
    }

    /**
     * Constructs an equation of the form <code>x = polynomial</code>.
     */
    public Equation(final Variable variable, final Polynomial value) {
        this(new Polynomial(variable), value);
    }

    /**
     * Constructs an equation with the specified left and right hand sides.
     */
    public Equation(final Polynomial left, final Polynomial right) {
        this.left  = left;
        this.right = right;
    }

    /**
     * Returns the variable on the left hand side of the equation.
     * This variable is often the result of a call to {@link #solve}.
     *
     * @throws IllegalStateException if the left hand side is more complex than just
     *         a variable.
     */
    public Variable getVariable() throws IllegalStateException {
        if (left != null) {
            final Monomial[] m = left.monomials;
            if (m.length==1 && m[0].coefficient==1) {
                final Factor[] f = m[0].factors;
                if (f.length == 1) {
                    return f[0].variable;
                }
            }
        }
        throw new IllegalStateException();
    }

    /**
     * Returns the value on the right hand side of the equation.
     */
    public Polynomial getValue() {
        return right;
    }

    /**
     * Multiply both size of the equation by the given factor.
     */
    public Equation multiply(final double factor) {
        final Polynomial p1 = (left !=null) ?  left.multiply(factor) : null;
        final Polynomial p2 = (right!=null) ? right.multiply(factor) : null;
        if (p1==left && p2==right) {
            return this;
        }
        return new Equation(p1, p2);
    }

    /**
     * Solve the equation for the givan variable.
     */
    public Equation solve(final Variable variable) {
        Polynomial polynomial = left;
        if (right != null) {
            polynomial = polynomial.substract(right);
        }
        Polynomial[] parts = polynomial.factor(variable);
        switch (parts.length) {
            case 2: {
                if (parts[1] != null) {
                    polynomial = parts[0].multiply(-1).divide(parts[1]);
                    break;
                }
                // fall through
            }
            case 1:  // Fall through
            case 0:  return null;
            default: throw new ArithmeticException("Not yet implemented");
        }
        return new Equation(new Polynomial(variable), polynomial);
    }

    /**
     * Substitute the specified variable by the given polynomial
     * on both side of the equation. The specified equation must
     * be in the form <code>x = polynomial</code>.
     */
    public Equation substitute(final Equation equation) {
        final Variable variable = equation.getVariable();
        final Polynomial  value = equation.getValue();
        final Polynomial p1 = (left !=null) ?  left.substitute(variable, value) : null;
        final Polynomial p2 = (right!=null) ? right.substitute(variable, value) : null;
        if (p1==left && p2==right) {
            return this;
        }
        return new Equation(p1, p2);
    }

    /**
     * Simplify the polynomial on each hand side of this equation.
     */
    public Equation simplify() {
        final Polynomial p1 = (left !=null) ?  left.simplify() : null;
        final Polynomial p2 = (right!=null) ? right.simplify() : null;
        if (p1==left && p2==right) {
            return this;
        }
        return new Equation(p1, p2);
    }

    /**
     * Returns a string representation of this equation.
     */
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        if (left != null) {
            left.toString(buffer);
        } else {
            buffer.append('0');
        }
        buffer.append(" = ");
        if (right != null) {
            right.toString(buffer);
        } else {
            buffer.append('0');
        }
        return buffer.toString();
    }
}
