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
 * Root for all elements (variable, polynom, etc.).
 * All elements are immutable by design.
 *
 * @author  Martin Desruisseaux
 */
public abstract class Element {
    /**
     * Creates a new element.
     */
    public Element() {
    }

    /**
     * Returns a human readable string representation of this element.
     * For a {@linkplain Variable variable}, this is the variable name.
     */
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        toString(buffer);
        return buffer.toString();
    }

    /**
     * Constructs a human readable string representation of this element.
     * For a {@linkplain Variable variable}, this is the variable name.
     *
     * @param buffer The buffer into which append the string.
     */
    abstract void toString(final StringBuilder buffer);

    /**
     * Check that the given arrays contains only non-null elements
     * (<code>nul==false</code>) or only null elements (<code>nul==true</code>).
     *
     * @param elements The array to check.
     * @param nul  <code>true</code> if all elements should be null, or
     *            <code>false</code> if all elements should be non-null.
     * @return <code>true</code> if <code>elements</code> is non-null and
     *         all elements meet the requirement given by <code>nul</code>.
     */
    static boolean checkNullElements(final Element[] elements, final boolean nul) {
        if (elements != null) {
            for (int i=0; i<elements.length; i++) {
                if ((elements[i] == null) != nul) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
