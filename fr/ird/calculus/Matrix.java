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

// J2SE dependencies
import java.io.StringWriter;
import java.io.IOException;

// Vecmath dependencies
import javax.vecmath.GMatrix;
import javax.vecmath.MismatchedSizeException;

// Geotools dependencies
import org.geotools.io.TableWriter;


/**
 * A matrix of {@linkplain Polynomial polynomials}.
 *
 * @author  Martin Desruisseaux
 */
public final class Matrix {
    /**
     * Number of rows in this matrix.
     */
    public final int height;

    /**
     * Number of columns in this matrix,
     */
    public final int width;

    /**
     * The matrix elements. Null elements are treated as 0.
     * This array length must be equals to {@link #height}&times;{@link #width}.
     */
    private final Polynomial[] elements;

    /**
     * Creates a new matrix of the given size.
     */
    public Matrix(final int height, final int width) {
        this.height   = height;
        this.width    = width;
        this.elements = new Polynomial[width*height];
    }

    /**
     * Construct a matrix initialized from the given numerical matrix,
     */
    public Matrix(final GMatrix matrix) {
        this(matrix.getNumRow(), matrix.getNumCol());
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                set(j, i, matrix.getElement(j, i));
            }
        }
    }

    /**
     * Returns the polynomial at the given location.
     */
    public Polynomial get(int row, int col) {
        if (row<0 || row>=height) {
            throw new IndexOutOfBoundsException(String.valueOf(row));
        }
        if (col<0 || col>=width) {
            throw new IndexOutOfBoundsException(String.valueOf(col));
        }
        return elements[row*width + col];
    }

    /**
     * Set the polynomial at the given location.
     */
    public void set(int row, int col, final Polynomial element) {
        if (row<0 || row>=height) {
            throw new IndexOutOfBoundsException(String.valueOf(row));
        }
        if (col<0 || col>=width) {
            throw new IndexOutOfBoundsException(String.valueOf(col));
        }
        elements[row*width + col] = element;
    }

    /**
     * Set the polynomial at the given location.
     */
    public void set(int row, int col, final Variable element) {
        set(row, col, new Polynomial(element));
    }

    /**
     * Set the polynomial at the given location.
     */
    public void set(int row, int col, final double element) {
        set(row, col, new Polynomial(element));
    }

    /**
     * Ensure that the specified matrix has the same size than this matrix.
     *
     * @throws MismatchedSizeException if the sizes don't match.
     */
    private void ensureSameSize(final Matrix matrix) {
        if (width != matrix.width || height != matrix.height) {
            throw new MismatchedSizeException();
        }
    }

    /**
     * Add a matrix to this one.
     */
    public Matrix add(final Matrix matrix) {
        ensureSameSize(matrix);
        final Matrix result = new Matrix(height, width);
        for (int i=0; i<elements.length; i++) {
            final Polynomial e1 =   this.elements[i];
            final Polynomial e2 = matrix.elements[i];
            result.elements[i] = (e1==null) ? e2 : (e2==null) ? e1 : e1.add(e2);
        }
        return result;
    }

    /**
     * Substract a matrix from this one.
     */
    public Matrix substract(final Matrix matrix) {
        ensureSameSize(matrix);
        final Matrix result = new Matrix(height, width);
        for (int i=0; i<elements.length; i++) {
            final Polynomial e1 =   this.elements[i];
            final Polynomial e2 = matrix.elements[i];
            result.elements[i] = (e1==null) ? e2.multiply(-1) : (e2==null) ? e1 : e1.substract(e2);
        }
        return result;
    }

    /**
     * Multiply this matrix by the specified one.
     */
    public Matrix multiply(final Matrix matrix) {
        if (width != matrix.height) {
            throw new MismatchedSizeException();
        }
        final Matrix result = new Matrix(height, matrix.width);
        for (int j=0; j<height; j++) {
            for (int i=0; i<matrix.width; i++) {
                Polynomial sum = null;
                for (int k=0; k<width; k++) {
                    final Polynomial e1 =   this.get(j, k);
                    final Polynomial e2 = matrix.get(k, i);
                    if (e1!=null && e2!=null) {
                        final Polynomial p = e1.multiply(e2);
                        sum = (sum==null) ? p : sum.add(p);
                    }
                }
                result.set(j, i, sum);
            }
        }
        return result;
    }

    /**
     * Simplify in-place all elements in the current matrix.
     */
    public void simplify() {
        for (int i=0; i<elements.length; i++) {
            elements[i] = elements[i].simplify();
        }
    }

    /**
     * Returns a string representation of this matrix.
     */
    public String toString() {
        try {
            final StringWriter writer = new StringWriter();
            final TableWriter  table  = new TableWriter(writer, 2);
            for (int j=0; j<height; j++) {
                for (int i=0; i<width; i++) {
                    final Polynomial p = get(j,i);
                    table.write(p!=null ? p.toString() : "0");
                    table.nextColumn();
                }
                table.nextLine();
            }
            table.flush();
            return writer.toString();
        } catch (IOException exception) {
            // Should never happen
            throw new AssertionError(exception);
        }
    }
}
