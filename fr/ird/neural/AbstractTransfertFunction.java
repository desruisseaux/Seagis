/*
 * Remote sensing images: database, visualisation and simulations
 * Copyright (C) 1999 by Joseph A. Huwaldt <jhuwaldt@gte.net>.
 *               2001 Institut de Recherche pour le Développement
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
package fr.ird.neural;

// Input/output
import java.io.Serializable;


/**
 * Base class for transfert functions.
 * This base class support serialization.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class AbstractTransfertFunction implements TransfertFunction, Serializable
{
    /**
     * Serial number for compatibility with previous versions.
     */
    private static final long serialVersionUID = 371334752349159959L;

    /**
     * The transfert function name.
     */
    private final String name;

    /**
     * Construct a transfert function.
     *
     * @param name The transfert function name.
     */
    public AbstractTransfertFunction(final String name)
    {this.name = name;}

    /**
     * Returns the transfert function name.
     */
    public String toString()
    {return name;}

    // TODO: canocalize deserialized functions.
}
