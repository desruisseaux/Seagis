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

import java.io.PrintWriter;
import javax.vecmath.GMatrix;
import org.geotools.resources.Arguments;
import junit.framework.*;


/**
 *
 * @author  Martin Desruisseaux
 */
public class BasicTest extends TestCase {
    /**
     * The writer to the console
     */
    private static PrintWriter out;

    /**
     * Run the suite from the command line.
     */
    public static void main(String[] args) {
        final Arguments arguments = new Arguments(args);
        out = arguments.out;
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Returns the test suite.
     */
    public static Test suite() {
        return new TestSuite(BasicTest.class);
    }

    /**
     * Construct the test suite.
     */
    public BasicTest(final String name) {
        super(name);
    }

    /**
     * Test matrix creation using constants.
     */
    public void testConstants() {
        int height = 4;
        int width  = 3;
        GMatrix g = getMatrix(height, width);
        Matrix  m = new Matrix(g);
        assertEquals("Creation", g, m, 1E-8);

        GMatrix opG = getMatrix(height, width);
        Matrix  opM = new Matrix(opG);
        assertEquals("Creation", opG, opM, 1E-8);
        g.add(opG);
        m = m.add(opM);
        m.simplify();
        assertEquals("Addition", g, m, 1E-8);

        opG = getMatrix(height, width);
        opM = new Matrix(opG);
        assertEquals("Creation", opG, opM, 1E-8);
        g.sub(opG);
        m = m.substract(opM);
        m.simplify();
        assertEquals("Substraction", g, m, 1E-8);

        height = width = 3;
        g = getMatrix(height, width);
        m = new Matrix(g);
        assertEquals("Creation", g, m, 1E-8);
        opG = getMatrix(height, width);
        opG.transpose();
        opM = new Matrix(opG);
        assertEquals("Creation", opG, opM, 1E-8);
        g.mul(opG);
        m = m.multiply(opM);
        m.simplify();
        assertEquals("Multiplication", g, m, 1E-8);

        if (false) {
            out.println(g);
            out.println();
            out.println(m);
        }
    }

    /**
     * Tests {@link Polynomial} methods.
     */
    public void testPolynomial() {
        final Variable   x = new Variable("x");
        final Variable   y = new Variable("y");
        Polynomial px, py, p1, p2, dx, dy;
        Equation ex, ey;
        px = new Polynomial(x);         assertEquals("new",       "x",                  px.toString());
        py = new Polynomial(y);         assertEquals("new",       "y",                  py.toString());
        px = px.power(3);               assertEquals("power",     "x³",                 px.toString());
        py = py.power(2);               assertEquals("power",     "y²",                 py.toString());
        p1 = px.multiply(py);           assertEquals("multiply",  "x³y²",               p1.toString());
        p1 = p1.multiply(4);            assertEquals("multiply",  "4x³y²",              p1.toString());
        px = new Polynomial(x);         assertEquals("new",       "x",                  px.toString());
        py = new Polynomial(y);         assertEquals("new",       "y",                  py.toString());
        px = px.multiply(-3);           assertEquals("multiply",  "-3x",                px.toString());
        p2 = px.add(py);                assertEquals("add",       "-3x+y",              p2.toString());
        py = py.multiply(-1);           assertEquals("multiply",  "-y",                 py.toString());
        p2 = p2.substract(py);          assertEquals("substract", "-3x+y+y",            p2.toString());
        p2 = p2.simplify();             assertEquals("simplify",  "-3x+2y",             p2.toString());
        p2 = p2.power(2);               assertEquals("power",     "9xx-6xy-6yx+4yy",    p2.toString());
        p2 = p2.simplify();             assertEquals("simplify",  "9x²-12xy+4y²",       p2.toString());
        p1 = p1.substract(p2);          assertEquals("substract", "4x³y²-9x²+12xy-4y²", p1.toString());
        dx = p1.derivate(x).simplify(); assertEquals("derivate",  "12x²y²-18x+12y",     dx.toString());
        dy = p1.derivate(y).simplify(); assertEquals("derivate",  "8x³y+12x-8y",        dy.toString());
        dy = dy.substract(dy.getMonomial(0));
        dy = dy.simplify();             assertEquals("substract", "12x-8y",             dy.toString());
        py = py.multiply(2);            assertEquals("multiply",  "-2y",                py.toString());
        ey = new Equation(dy,py);       assertEquals("new",       "12x-8y = -2y",       ey.toString());
        ey = ey.solve(x).simplify();    assertEquals("solve",     "x = 0.5y",           ey.toString());
        ex = new Equation(dx);          assertEquals("new",       "12x²y²-18x+12y = 0", ex.toString());
        ex = ex.substitute(ey);         assertEquals("substitute","12y-9y+3y²yy = 0",   ex.toString());
    }

    /**
     * Tests a polynomial expression.
     */
    public void testEquation() {
        Matrix V  = new Matrix(3,1);
        V.set(0,0, +1);
        V.set(1,0, +1);
        V.set(2,0, +1);
        final Matrix  M0 = getSymbolicMatrix("");
        final Matrix  M1 = getSymbolicMatrix("'");
        final Matrix  M2 = getSymbolicMatrix("\"");
        final Variable S = M0.get(0,0).monomials[0].factors[0].variable;
        final Variable X = M0.get(2,1).monomials[0].factors[0].variable;
        final Variable Y = M0.get(0,2).monomials[0].factors[0].variable;
        final Variable Z = M0.get(1,0).monomials[0].factors[0].variable;
        Matrix M  = M1.multiply(M2);
        M = M.substract(M0);
        V = M.multiply(V);

        assertEquals("Width",  1, V.width);
        assertEquals("Height", 3, V.height);
        Polynomial P = new Polynomial(0);
        for (int j=0; j<3; j++) {
            P = P.add(V.get(j, 0).power(2));
        }
        P = P.simplify();

        final Polynomial ds = P.derivate(S).simplify();
        final Polynomial dx = P.derivate(X).simplify();
        final Polynomial dy = P.derivate(Y).simplify();
        final Polynomial dz = P.derivate(Z).simplify();
        final Equation   es = new Equation(ds).solve(S).simplify();
        final Equation   ex = new Equation(dx).solve(X).simplify();  // x = f(y,z)
        final Equation   ey = new Equation(dy).solve(Y).simplify();  // y = f(x,z)
        final Equation   ez = new Equation(dz).solve(Z).simplify();  // z = f(y,z)

        Equation rx, ry, rz;
        rx = ex.substitute(ey).solve(X).simplify();  // x = f(z)
        ry = ey.substitute(ex).solve(Y).simplify();  // y = f(z)
        rz = ez.substitute(ey).solve(Z).simplify();  // z = f(x)

        final EquationSystem system = new EquationSystem(new Equation[] {rx,ry,rz});
        if (false) system.solve(new Variable[] {X,Y,Z});

        if (true) {
            out.println();
            out.print(S); out.print(": "); out.println(ds);
            out.print(X); out.print(": "); out.println(dx);
            out.print(Y); out.print(": "); out.println(dy);
            out.print(Z); out.print(": "); out.println(dz);
            out.println();
            out.println(es.multiply(3));
            out.println(rx.multiply(3));
            out.println(ry.multiply(3));
            out.println(rz.multiply(3));
        }
    }

    /**
     * Tests a polynomial expression.
     */
    private Matrix getSymbolicMatrix(final String suffix) {
        final Variable S = new Variable("S"+suffix);
        final Variable X = new Variable("X"+suffix);
        final Variable Y = new Variable("Y"+suffix);
        final Variable Z = new Variable("Z"+suffix);
        final Matrix   M = new Matrix(3,3);
        M.set(0,0,S);  M.set(0,1,Z);  M.set(0,2,Y);
        M.set(1,0,Z);  M.set(1,1,S);  M.set(1,2,X);
        M.set(2,0,Y);  M.set(2,1,X);  M.set(2,2,S);
        M.set(0,1, M.get(0,1).multiply(-1));
        M.set(1,2, M.get(1,2).multiply(-1));
        M.set(2,0, M.get(2,0).multiply(-1));
        return M;
    }

    /**
     * Returns a matrix for test purpose.
     */
    private static GMatrix getMatrix(final int height, final int width) {
        final GMatrix matrix = new GMatrix(height, width);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                matrix.setElement(j, i, 200*Math.random()-100);
            }
        }
        return matrix;
    }

    /**
     * Compares two matrix for equality.
     */
    private static void assertEquals(final String operation,
                                     final GMatrix expected,
                                     final  Matrix actual,
                                     final double eps)
    {
        final int height = expected.getNumRow();
        final int width  = expected.getNumCol();
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                final Polynomial p = actual.get(j, i);
                assertEquals("Not a monomial", 1, p.monomials.length);
                assertEquals(operation, expected.getElement(j, i), p.monomials[0].coefficient, eps);
            }
        }
    }
}
