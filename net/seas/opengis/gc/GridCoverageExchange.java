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

// Input/output
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOException;


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
     * Export a grid coverage to a persistent file format.
     *
     * @param gridCoverage Source grid coverage.
     * @param fileFormat String which indicates exported file format.
     *        The file format types are implementation specific.
     *        Sample file formats include:
     *        <ul>
     *          <li>“GeoTIFF” - GeoTIFF</li>
     *          <li>“PIX” - PCI Geomatics PIX</li>
     *          <li>“HDF-EOS” - NASA HDF-EOS</li>
     *          <li>“NITF” - National Image Transfer Format</li>
     *          <li>“STDS-DEM” - Standard Transfer Data Standard</li>
     *        </ul>
     *        Note: none of the above cited file formats are currently implemented.
     *
     * @param fileName File name to store grid coverage. This file name can be any
     *        valid file name within the underlying operating system of the server.
     *
     * @throws IOException if an input/output operation (including RMI) failed.
     * @throws IIOException if the file format is not compatiable with the grid
     *         coverage.
     */
    public abstract void exportTo(final GridCoverage gridCoverage, final String fileFormat, final File fileName) throws IOException;

    /**
     * Create a new coverage with a different coordinate reference system.
     *
     * @param  gridCoverage Source grid coverage.
     * @param  coordsys Coordinate system of the new grid coverage.
     * @return The new grid coverage.
     */
    public abstract GridCoverage move(final GridCoverage gridCoverage, final CoordinateSystem coordsys);
}
