/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.database.coverage.sql;

// Geotools dependencies
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// Seagis dependencies
import fr.ird.database.Entry;


/**
 * Overrides {@link #toString} for usage through RMI. The {@linkplain Entry entry}
 * string representation is fetch at construction time, which happen on the server
 * side. The name is then serialized, which protect the tree node from returning
 * the RMI stub string representation on the client side. 
 *
 * @version $Id$
 * @author Remi Eve
 * @author Martin Desruisseaus
 */
final class EntryNode extends DefaultMutableTreeNode {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 375054311152079630L;

    /**
     * The default entry name.
     */
    private final String name;

    /**
     * Creates a tree node with no parent, no children, initialized with
     * the specified user object, and that allows children only if
     * specified.
     *
     * @param userObject an Object provided by the user that constitutes
     *        the node's data
     * @param allowsChildren if true, the node is allowed to have child
     *        nodes -- otherwise, it is always a leaf node
     */
    public EntryNode(Entry userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
        name = userObject.toString();
    }

    /**
     * Returns the entry name.
     */
    public String toString() {
        return name;
    }
}
