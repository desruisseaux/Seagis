/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.gc;

// Input/output
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOException;


/**
 * Support for creation of grid coverages from persistent formats as well
 * as exporting a grid coverage to a persistent formats.
 *
 * NOTE: This class is not yet implemented.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
abstract class GridCoverageExchange
{
    /**
     * Default constructor.
     */
    public GridCoverageExchange()
    {}

    /**
     * Create a new {@link GridCoverage} from a grid coverage file.
     * This method is meant to allow implementations to create a
     * {@link GridCoverage} from any file format.
     *
     * @param  name File name (including path) from which to create a grid coverage.
     *         This file name can be any valid file name within the underlying operating
     *         system of the server or a valid string, such as a URL which specifics
     *         a grid coverage.
     * @return The grid coverage.
     * @throws IOException if an error occurs during reading.
     * @throws IIOException if a grid coverage can't be create from the specified name.
     */
    public abstract GridCoverage createFromName(final String name) throws IOException;

    /**
     * Export a grid coverage to a persistent file format.
     *
     * @param gridCoverage Source grid coverage.
     * @param fileFormat String which indicates exported file format.
     *        The file format types are implementation specific.
     *        Sample file formats include:
     *        <ul>
     *          <li>"GeoTIFF"  - GeoTIFF</li>
     *          <li>"PIX"      - PCI Geomatics PIX</li>
     *          <li>"HDF-EOS"  - NASA HDF-EOS</li>
     *          <li>"NITF"     - National Image Transfer Format</li>
     *          <li>"STDS-DEM" - Standard Transfer Data Standard</li>
     *        </ul>
     *
     * @param fileName File name to store grid coverage. This file name can be any
     *        valid file name within the underlying operating system of the server.
     *
     * @throws IOException if an input/output operation (including RMI) failed.
     * @throws IIOException if the file format is not compatiable with the grid
     *         coverage.
     */
    public abstract void exportTo(final GridCoverage gridCoverage, final String fileFormat, final File fileName) throws IOException;
}
