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
package net.seas.opengis.cs;

// Miscellaneous
import java.util.NoSuchElementException;


/**
 * Creates spatial reference objects using codes.
 * The codes are maintained by an external authority.
 * A commonly used authority is EPSG, which is also
 * used in the GeoTIFF standard.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_CoordinateSystemAuthorityFactory
 */
public abstract class CoordinateSystemAuthorityFactory
{
    /**
     * Default constructor. Subclass should
     * override methods they support.
     */
    protected CoordinateSystemAuthorityFactory()
    {}

    /**
     * Returns the authority name.
     */
    public abstract String getAuthority();

    /**
     * Returns an {@link Ellipsoid} object from a code.
     *
     * @param  code Value allocated by authority.
     * @return The ellipsoid object.
     * @throws NoSuchElementException if <code>code</code> is not a known code.
     *
     * @see org.opengis.cs.CS_CoordinateSystemAuthorityFactory#createEllipsoid
     */
    public Ellipsoid createEllipsoid(final String code) throws NoSuchElementException
    {throw new NoSuchElementException(code);}
}
