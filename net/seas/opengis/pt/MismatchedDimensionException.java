/*
 * OpenGIS implementation in Java
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.pt;

// Miscellaneous
import net.seas.resources.Resources;


/**
 * Indicates that an operation cannot be completed properly because
 * of a mismatch in the dimensions of object attributes.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class MismatchedDimensionException extends RuntimeException
{
    /**
     * Creates new exception without detail message.
     */
    public MismatchedDimensionException()
    {}

    /**
     * Constructs an exception with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public MismatchedDimensionException(final String msg)
    {super(msg);}

    /**
     * Construct an exception with a detail message stating that
     * two objects don't have the same number of dimensions.
     *
     * @param dim1 Number of dimensions for the first object.
     * @param dim2 Number of dimensions for the second object.
     *        It shoud be different than <code>dim1</code>,
     *        otherwise there is no dimension mismatch!
     */
    public MismatchedDimensionException(final int dim1, final int dim2)
    {this(Resources.format(Clé.MISMATCHED_OBJECT_DIMENSION¤2, new Integer(dim1), new Integer(dim2)));}
}
