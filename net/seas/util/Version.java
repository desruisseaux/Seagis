/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
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
package net.seas.util;


/**
 * Information about the target Java platform.
 * Constants defined in this interface are used
 * for conditional compilation.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Version
{
    /**
     * Minor version target. It should be "2" for JRE 1.2 or higher,
     * "3" for JRE 1.3 or higher, and "4" for JRE 1.4 or higher.
     * JRE 1.0 and 1.1 are not supported.
     */
    public static final int MINOR = 4;
}
