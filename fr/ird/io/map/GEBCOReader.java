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
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.BufferedReader;

// Miscellaneous
import java.util.Map;
import java.util.TreeMap;
import java.util.Locale;
import java.text.ParseException;

// Geotools depencies
import org.geotools.io.LineFormat;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.renderer.geom.GeometryCollection;

// Seagis
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Isoline reader for ASCII files from the <A HREF="http://www.pol.ac.uk/BODC/gebco.html">General
 * Bathymetric Chart of the Oceans (GEBCO)</A> digital atlas. GEBCO's files have two columns. The
 * first line give the depth (in metres) and the number of points, followed by the above mentioned
 * points in (<var>latitude</var>,<var>longitude</var>) coordinates. Then a new line with the depth
 * and number of points appears, and so on. Example:
 * <p>
 * <blockquote><pre>
 * &nbsp;     0     6
 * &nbsp; 50.2510  -63.8400
 * &nbsp; 50.2390  -63.8400
 * &nbsp; 50.2330  -63.8310
 * &nbsp; 50.2390  -63.7950
 * &nbsp; 50.2510  -63.8130
 * &nbsp; 50.2510  -63.8400
 * &nbsp;     0     5
 * &nbsp; 50.2220  -63.9920
 * &nbsp; 50.2330  -63.9650
 * &nbsp; 50.2450  -63.9650
 * &nbsp; 50.2390  -63.9920
 * &nbsp; 50.2220  -63.9920
 * </pre></blockquote>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class GEBCOReader extends IsolineReader {
    /**
     * Objet à utiliser pour lire
     * les lignes d'un fichier GEBCO.
     */
    private final LineFormat format;

    /**
     * Système de coordonnées selon lequel sont exprimés
     * les coordonnées des points dans le fichier GEBCO.
     */
    private final CoordinateSystem coordinateSystem;

    /**
     * Construct a default reader using
     * {@link Locale#UK} for number parsing.
     */
    public GEBCOReader() {
        this(Locale.UK);
    }

    /**
     * Contruct a reader using the specified
     * locale for number parsing.
     */
    public GEBCOReader(final Locale locale) {
        this(new LineFormat(locale));
    }

    /**
     * Construct a reader using
     * the specified line format.
     */
    public GEBCOReader(final LineFormat format) {
        this.format = format;
        this.coordinateSystem = GeographicCoordinateSystem.WGS84;
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
    public GeometryCollection read(final double altitude) throws IOException {
        if (Double.isNaN(altitude)) {
            return null;
        }
        final BufferedReader reader = getBufferedReader();
        try {
            final GeometryCollection[] isolines = read(reader, altitude);
            switch (isolines.length) {
                case 0:  return null;
                case 1:  return isolines[0];
                default: throw new AssertionError(isolines.length);
            }
        } catch (ParseException exception) {
            final IOException e = new IOException(Resources.format(
                                            ResourceKeys.ERROR_BAD_FILE_FORMAT_$1, getFileName()));
            e.initCause(exception);
            throw e;
        } finally {
            reader.close();
        }
    }

    /**
     * Read all isolines.
     *
     * @return Isolines An array of all isoline founds.
     * @throws IOException if an error occured during reading.
     */
    public GeometryCollection[] read() throws IOException {
        final BufferedReader reader = getBufferedReader();
        try {
            return read(reader, Double.NaN);
        } catch (ParseException exception) {
            final IOException e=new IOException(
                            Resources.format(ResourceKeys.ERROR_BAD_FILE_FORMAT_$1, getFileName()));
            e.initCause(exception);
            throw e;
        }
        finally
        {
            reader.close();
        }
    }

    /**
     * Read all isolines found in the specified buffered stream.
     * Note that the stream is not automatically closed after
     * reading. This is up to the caller to close the stream.
     *
     * @param  reader The {@link BufferedReader} to use.
     * @param  file The desired altitude, or <code>NaN</code> for all altitudes.
     * @return Isolines found in the specified file.
     * @throws IOException if an error occured during reading.
     * @throws ParseException if an error occured during parsing.
     */
    private GeometryCollection[] read(final BufferedReader reader, final double desiredZ)
            throws IOException, ParseException
    {
        final Map<Float,GeometryCollection> isolines = new TreeMap<Float,GeometryCollection>();
        final double[] record = new double[2];
        String line; while ((line=reader.readLine()) != null) {
            if ((line=line.trim()).length() == 0) {
                continue;
            }
            /*
             * Parse header and get an isobath for the specified altitude.
             * Note: we must invert the altitude sign, since GEBCO files
             *       contains depths instead of altitudes.
             */
            format.setLine(line);
            format.getValues(record);
            final int count = (int)record[1];
            if (count != record[1]) {
                throw new ParseException(Resources.format(
                                    ResourceKeys.ERROR_NOT_AN_INTEGER_$1, new Float(record[1])), 0);
            }
            /*
             * If the next segment is to be stored in memory,
             * construct a new isoline or get an existing one.
             */
            GeometryCollection isoline = null;
            if (Double.isNaN(desiredZ) || record[0]==desiredZ) {
                final Float altitude = new Float(-record[0]);
                isoline = isolines.get(altitude);
                if (isoline == null) {
                    isoline = new GeometryCollection(coordinateSystem);
                    isoline.setValue(altitude.floatValue());
                    isolines.put(altitude, isoline);
                }
            }
            /*
             * Read the expected amount of points and add the
             * new points array (polyline) to the isobath.
             */
            final int coordCount = 2*count;
            final float[] points = (isoline!=null) ? new float[coordCount] : null;
      read: for (int j=0; j<coordCount;) {
                while ((line=reader.readLine()) != null) {
                    if ((line=line.trim()).length() == 0) {
                        continue;
                    }
                    if (points != null) {
                        format.setLine(line);
                        format.getValues(record);
                        points[j++] = (float)record[1]; // Longitude
                        points[j++] = (float)record[0]; // Latitude
                    } else {
                        j+=2;
                    }
                    continue read;
                }
                throw new EOFException(Resources.format(
                                    ResourceKeys.ERROR_MISSING_LINES_$1, new Integer(count-j/2)));
            }
            if (isoline != null) {
                isoline.add(points, 0, points.length);
            }
        }
        return (GeometryCollection[])isolines.values().toArray(new GeometryCollection[isolines.size()]);
    }
}
