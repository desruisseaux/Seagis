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


/**
 * A longitude angle. Positive longitudes are
 * East, while negative longitudes are West.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 *
 * @see Latitude
 * @see net.seas.text.AngleFormat
 */
public final class Longitude extends Angle
{
    /**
     * Serial number for interoperability with different versions.
     */
    // private static final long serialVersionUID = ?; // TODO

    /**
     * Minimum legal value for longitude (-180°).
     */
    public static final double MIN_VALUE = -180;

    /**
     * Maximum legal value for longitude (+180°).
     */
    public static final double MAX_VALUE = +180;

    /**
     * Contruct a new longitude with the specified value.
     *
     * @param theta Angle in degrees.
     */
    public Longitude(final double theta)
    {super(theta);}

    /**
     * Constructs a newly allocated <code>Longitude</code> object that
     * represents the longitude value represented by the string.   The
     * string should represents an angle in either fractional degrees
     * (e.g. 45.5°) or degrees with minutes and seconds (e.g. 45°30').
     * The hemisphere (E or W) is optional (default to East).
     *
     * @param  string A string to be converted to a <code>Longitude</code>.
     * @throws NumberFormatException if the string does not contain a parsable longitude.
     */
    public Longitude(final String source) throws NumberFormatException
    {super(source);}
}
