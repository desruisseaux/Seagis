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
 * A symbolic variable.
 *
 * @author  Martin Desruisseaux
 */
public final class Variable extends Element {
    /**
     * The symbol for this variable.
     */
    private final String symbol;
    
    /**
     * Creates a new symbolic variable.
     *
     * @param symbol The variable name. Must be non-null.
     */
    public Variable(final String symbol) {
        this.symbol = symbol.trim();
    }

    /**
     * Returns the variable name.
     */
    void toString(final StringBuilder buffer) {
        buffer.append(symbol);
    }

    /**
     * Returns the variable name.
     */
    public String toString() {
        return symbol;
    }
}
