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
package net.seas.opengis.ct;


/**
 * <FONT COLOR="#FF6633">Thrown when {@link MathTransform#inverse}
 * is invoked but the transform can't be inverted.</FONT>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class NoninvertibleTransformException extends TransformException
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -2601170123706246740L;

    /**
     * Constructs a new exception with no detail message.
     */
    public NoninvertibleTransformException()
    {}

    /**
     * Constructs a new exception with the specified detail message.
     */
    public NoninvertibleTransformException(final String message)
    {super(message);}

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public NoninvertibleTransformException(final String message, final Throwable cause)
    {super(message, cause);}
}
