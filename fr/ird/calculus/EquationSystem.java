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
import javax.vecmath.GMatrix;


/**
 * A system of many equations with the same number of unknows.
 *
 * @author  Martin Desruisseaux
 */
public final class EquationSystem {
    /**
     * The set of equations.
     */
    private final Equation[] equations;

    /**
     * Construct a new equation systems.
     *
     * @param equations The set of equations.
     */
    public EquationSystem(final Equation[] equations) {
        this.equations = equations.clone();
    }

    /**
     * Solve the equation system for the given set of variables.
     *
     * @param  variables The variables to solve.
     * @return The solved equations.
     */
    public Equation[] solve(final Variable[] variables) {
        Matrix matrix    = new Matrix(equations.length, variables.length);
        Matrix constants = new Matrix(equations.length, 1);
        for (int j=0; j<equations.length; j++) {
            Polynomial p = equations[j].getPolynomial();
            for (int i=0; i<variables.length; i++) {
                final Polynomial[] f = p.factor(variables[i]);
                final Polynomial value;
                switch (f.length) {
                    default: throw new ArithmeticException("Not yet implemented");
                    case  2: value = f[1]; break;
                    case  1: value = null; break;
                }
                matrix.set(j, i, value);
                p = f[0];
            }
            constants.set(j, 0, p);
        }
        /*
         * TODO: Matrix inversion (below) is implemented for pure numerical matrix only.
         */
        final GMatrix nm = matrix.toNumericalMatrix();
        nm.invert();
        matrix = new Matrix(nm);
        constants = matrix.multiply(constants);
        final Equation[] result = new Equation[variables.length];
        for (int j=0; j<result.length; j++) {
            result[j] = new Equation(variables[j], constants.get(j,0));
        }
        return result;
    }

    /**
     * Returns a string representation of this equation system.
     */
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        final String lineSeparator = System.getProperty("line.separator", "\n");
        for (int i=0; i<equations.length; i++) {
            buffer.append(equations[i]);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
