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
package net.seas.opengis.gc;

// Coordinate systems
import net.seas.opengis.cs.CoordinateSystem;


/**
 * Support for creation of grid coverages from persistent formats
 * as well as exporting a grid coverage to a persistent formats.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.gc.GC_GridCoverageExchange
 */
public abstract class GridCoverageExchange
{
    /**
     * Default constructor.
     */
    public GridCoverageExchange()
    {}

    /**
     * Create a new coverage with a different coordinate reference system.
     *
     * @param  gridCoverage Source grid coverage.
     * @param  coordsys Coordinate system of the new grid coverage.
     * @return The new grid coverage.
     */
    public abstract GridCoverage move(final GridCoverage gridCoverage, final CoordinateSystem coordsys);
}
