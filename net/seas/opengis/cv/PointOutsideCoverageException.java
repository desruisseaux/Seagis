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
package net.seas.opengis.cv;


/**
 * Throws when a <code>Coverage.evaluate</code>
 * method is invoked with a point outside coverage.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class PointOutsideCoverageException extends RuntimeException
{
    /**
     * Construct an exception with no message.
     */
    public PointOutsideCoverageException()
    {}

    /**
     * Construct an exception with the specified message.
     */
    public PointOutsideCoverageException(final String message)
    {super(message);}
}
