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
package fr.ird.io.map;

// Input/output
import java.net.URL;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

// Geotools
import org.geotools.renderer.geom.GeometryCollection;

// Resources
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Base class for bathymetry readers.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class IsolineReader {
    /**
     * The source input, or <code>null</code> if not set.
     */
    private File file;

    /**
     * The source input, or <code>null</code> if not set.
     */
    private URL url;

    /**
     * Construct a default isoline reader.
     */
    public IsolineReader() {
    }

    /**
     * Sets the input source to use. The input source must be
     * set before any of the query or read methods are used.
     *
     * @param  file The input source, or <code>null</code> if none.
     * @throws IOException if the input source can't be set.
     */
    public void setInput(final File file) throws IOException {
        this.url  = null;
        this.file = file;
    }

    /**
     * Sets the input source to use. The input source must be
     * set before any of the query or read methods are used.
     *
     * @param  url The input source, or <code>null</code> if none.
     * @throws IOException if the input source can't be set.
     */
    public void setInput(final URL url) throws IOException {
        this.file = null;
        this.url  = url;
    }

    /**
     * Returns the filename of input source, or
     * <code>null</code> if no input source has
     * been set.
     */
    final String getFileName() {
        if (file != null) {
            return file.getName();
        }
        if (url != null) {
            return new File(url.getPath()).getName();
        }
        return null;
    }

    /**
     * Returns the input source as a {@link BufferedReader}. It is
     * the caller responsability to close this stream when finished.
     *
     * @throws IOException if an error occured while constructing the reader.
     */
    protected BufferedReader getBufferedReader() throws IOException {
        if (file != null) {
            return new BufferedReader(new FileReader(file));
        }
        if (url != null) {
            return new BufferedReader(new InputStreamReader(url.openStream()));
        }
        throw new IOException(Resources.format(ResourceKeys.ERROR_NO_INPUT_SET));
    }

    /**
     * Read the isolines for the specified altitude.
     *
     * @param  The altitude for the isoline to be read. Zero is
     *         set at the mean sea level. Depths under sea level
     *         are negative altitudes.
     * @return The isoline, or <code>null</code> if there
     *         is no isoline for the specified altitude.
     * @throws IOException if an error occured during reading.
     */
    public abstract GeometryCollection read(final double altitude) throws IOException;

    /**
     * Read all isolines.
     *
     * @return Isolines An array of all isoline founds.
     * @throws IOException if an error occured during reading.
     */
    public abstract GeometryCollection[] read() throws IOException;
}
