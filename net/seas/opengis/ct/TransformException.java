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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.ct;


/**
 * <FONT COLOR="#FF6633">Thrown by {@link MathTransform} when a transformation failed.</FONT>
 *
 * @version 1.0
 * @author Andr� Gosselin
 * @author Martin Desruisseaux
 */
public class TransformException extends Exception
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 688199915603567723L;

    /**
     * Constructs a new exception with no detail message.
     */
    public TransformException()
    {}

    /**
     * Constructs a new exception with the specified detail message.
     */
    public TransformException(final String message)
    {super(message);}

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public TransformException(final String message, final Throwable cause)
    {super(message, cause);}
}
