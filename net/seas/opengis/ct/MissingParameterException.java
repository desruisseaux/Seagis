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

// Miscellaneous
import net.seas.resources.Resources;


/**
 * Thrown when a {@link Parameter} was missing. For example, this exception may
 * be thrown when a map projection was requested but the "semi_major" parameter
 * was not specified.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class MissingParameterException extends RuntimeException
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 3175167318321053417L;

    /**
     * The missing parameter name.
     */
    private final String parameter;

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param msg the detail message, or <code>null</code> to construct
     *        a default message from the missing parameter name.
     * @param parameter The missing parameter name.
     */
    public MissingParameterException(final String msg, final String parameter)
    {
        super((msg!=null || parameter==null) ? msg : Resources.format(Clé.MISSING_PARAMETER¤1, parameter));
        this.parameter = parameter;
    }

    /**
     * Returns the missing parameter name.
     */
    public String getMissingParameterName()
    {return parameter;}
}
