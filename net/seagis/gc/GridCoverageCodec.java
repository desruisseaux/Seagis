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
 */
package net.seagis.gc;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cs.AxisInfo;
import net.seagis.cs.Ellipsoid;
import net.seagis.cs.Projection;
import net.seagis.cs.HorizontalDatum;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.CoordinateSystemFactory;
import net.seagis.cs.ProjectedCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.ct.CoordinateTransformationFactory;
import net.seagis.ct.TransformException;
import net.seagis.gc.GridCoverage;
import net.seagis.gc.GridRange;
import net.seagis.pt.AngleFormat;
import net.seagis.pt.Longitude;
import net.seagis.pt.Latitude;
import net.seagis.pt.Envelope;

// Images
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import javax.media.jai.PropertySource;

// Images intput/output
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
import javax.imageio.ImageIO;
import javax.imageio.IIOException;
------- END OF JDK 1.4 DEPENDENCIES ---*/

// General input/output
import java.net.URL;
import java.io.File;
import java.io.Writer;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import net.seagis.io.TableWriter;

// Collections
import java.util.Set;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
import java.util.LinkedHashMap;
------- END OF JDK 1.4 DEPENDENCIES ---*/
import java.util.NoSuchElementException;
import javax.media.jai.util.CaselessStringKey;

// Miscellaneous
import javax.units.Unit;
import java.awt.geom.Point2D;
import net.seagis.resources.OpenGIS;
import net.seagis.resources.Utilities;


/**
 * Base class for encoding and decoding Grid Coverage objects
 * in a specific format.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class GridCoverageCodec
{
    /**
     * The format name (e.g. "PNG" or "GeoTIFF")..
     */
    protected final String formatName;

    /**
     * Construct a <code>GridCoverageCodec</code>.
     *
     * @param formatName The format name (e.g. "PNG" or "GeoTIFF").
     */
    public GridCoverageCodec(final String formatName)
    {this.formatName = formatName;}

    /**
     * Read the grid coverage from the specified file.
     */
    public abstract GridCoverage read(final File input) throws IOException;

    /**
     * Read the grid coverage from the specified file.
     */
    public abstract GridCoverage read(final URL input) throws IOException;

    /**
     * Write a grid coverage to the specified file. Default implementation write
     * the image using {@link ImageIO} API.  This implementation doesn't add any
     * meta-data (e.g. the envelope, the coordinate system, etc.). Consequently,
     * georeferencing may be lost.
     *
     * @param  gridCoverage The gridCoverage to write.
     * @param  output The destination file.
     * @throws IOException if an error occured while writing to the output stream.
     */
    public void write(final GridCoverage gridCoverage, final File output) throws IOException
    {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        ImageIO.write(gridCoverage.getRenderedImage(false), formatName, output);
------- END OF JDK 1.4 DEPENDENCIES ---*/
        throw new UnsupportedOperationException("J2SE 1.4 required!");
//----- END OF JDK 1.3 FALLBACK -------
    }

    /**
     * Write a grid coverage to the specified URL. Default implementation write
     * the image using {@link ImageIO} API. This implementation doesn't add any
     * meta-data (e.g. the envelope, the coordinate system, etc.). Consequently,
     * georeferencing may be lost.
     *
     * @param  gridCoverage The gridCoverage to write.
     * @param  output The destination URL.
     * @throws IOException if an error occured while writing to the output stream.
     */
    public void write(final GridCoverage gridCoverage, final URL output) throws IOException
    {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        final OutputStream stream = output.openConnection().getOutputStream();
        ImageIO.write(gridCoverage.getRenderedImage(false), formatName, stream);
        stream.close();
------- END OF JDK 1.4 DEPENDENCIES ---*/
        throw new UnsupportedOperationException("J2SE 1.4 required!");
//----- END OF JDK 1.3 FALLBACK -------
    }

    /**
     * Returns the grid coverage.
     */
    protected GridCoverage getGridCoverage(final PropertySource properties, final RenderedImage image)
    {
        final GridRange range = getGridRange(properties);
        if (range.getLength(0)!=image.getWidth() || range.getLength(1)!=image.getHeight())
        {
            throw new IllegalArgumentException("Unexpected image size"); // TODO
        }
        return new GridCoverage("Image", image, getCoordinateSystem(properties), getEnvelope(properties));
    }
}
