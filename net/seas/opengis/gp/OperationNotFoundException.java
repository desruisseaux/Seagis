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
package net.seas.opengis.gp;


/**
 * Thrown by {@link GridCoverageProcessor} when an operation is queried
 * but has not been found.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class OperationNotFoundException extends IllegalArgumentException
{
    /**
     * Construct an exception with no detail message.
     */
    public OperationNotFoundException()
    {}

    /**
     * Construct an exception with the specified detail message.
     */
    public OperationNotFoundException(final String message)
    {super(message);}
}
