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

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
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
import net.seagis.pt.AngleFormat;
import net.seagis.pt.Longitude;
import net.seagis.pt.Latitude;

// Images
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;

// Input/output
import java.net.URL;
import java.io.File;
import java.io.Writer;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
 * Base class for encoding and decoding of Grid Coverage objects.
 * This class is only a first drafy; it may change in incompatible
 * way in any future version.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class GridCoverageCodec
{
    /**
     * The source file or URL, or <code>null</code>
     * if source has not been set yet.
     */
    private String source;

    /**
     * The set of properties, or <code>null</code>
     * if there is none. Properties are typically
     * included in an image's header, or in a
     * separated text file.
     */
    private Map properties;

    /**
     * The coordinate system, or <code>null</code> if it has not been constructed yet.
     * Coordinate system may be expensive to construct and requested many time; this
     * is way it should be cached the first time {@link #getCoordinateSystem} is invoked.
     */
    private transient CoordinateSystem coordinateSystem;

    /**
     * The coordinate system factory.
     */
    protected final CoordinateSystemFactory factory = CoordinateSystemFactory.getDefault();

    /**
     * Read the grid coverage from the specified file.
     */
    public abstract GridCoverage read(final File input) throws IOException;

    /**
     * Read all properties from a header file. Default implementation
     * invokes {@link #parseHeaderLine} for each line found in the file.
     *
     * @param  in The file to read until EOF.
     * @throws IOException if an error occurs during loading.
     */
    protected void loadProperties(final File header) throws IOException
    {
        source = header.getPath();
        final BufferedReader in = new BufferedReader(new FileReader(header));
        loadProperties(in);
        in.close();
    }

    /**
     * Read all properties from an URL. Default implementation
     * invokes {@link #parseHeaderLine} for each line found in
     * the file.
     *
     * @param  in The URL to read until EOF.
     * @throws IOException if an error occurs during loading.
     */
    protected void loadProperties(final URL header) throws IOException
    {
        source = header.getPath();
        final BufferedReader in = new BufferedReader(new InputStreamReader(header.openStream()));
        loadProperties(in);
        in.close();
    }

    /**
     * Read all properties from a header file. Default implementation
     * invokes {@link #parseHeaderLine} for each line found in the stream.
     *
     * @param in The stream to read until EOF. The stream will not be closed.
     * @throws IOException if an error occurs during loading.
     */
    private void loadProperties(final BufferedReader in) throws IOException
    {
        final Set  previousComments = new HashSet();
        final StringBuffer comments = new StringBuffer();
        final String  lineSeparator = System.getProperty("line.separator", "\n");
        String line; while ((line=in.readLine())!=null)
        {
            if (line.trim().length()!=0)
            {
                if (!parseHeaderLine(line))
                {
                    if (previousComments.add(line))
                    {
                        comments.append(line);
                        comments.append(lineSeparator);
                    }
                }
            }
        }
        if (comments.length()!=0)
        {
            if (properties==null)
            {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
                properties = new LinkedHashMap();
------- END OF JDK 1.4 DEPENDENCIES ---*/
                properties = new HashMap();
//----- END OF JDK 1.3 FALLBACK -------
            }
            properties.put(null, comments.toString());
        }
    }

    /**
     * Parse a header line. The default implementation take the substring
     * on the left size of the first '=' character as the key, and the
     * substring on the right size of '=' as the value.
     *
     * @param  line The line to parse.
     * @return <code>true</code> if the line has been succefuly parsed.
     * @throws RasterFormatException if the line is badly formatted,
     *         or if the line contains a property already stored.
     */
    protected boolean parseHeaderLine(final String line) throws RasterFormatException
    {
        final int index = line.indexOf('=');
        if (index>=0)
        {
            addProperty(line.substring(0, index), line.substring(index+1));
            return true;
        }
        else return false;
    }

    /**
     * Add a property for the specified key. Keys are case-insensitive.
     * Calling this method with an illegal key-value pair thrown an
     * {@link RasterFormatException} since properties are used for
     * holding raster informations.
     *
     * @param  key   The key for the property to add.
     * @param  value The value for the property to add.
     * @throws RasterFormatException if a different value
     *         already exists for the specified key.
     */
    public void addProperty(String key, String value) throws RasterFormatException
    {
        key = key.trim();
        value = value.trim();
        if (value.length()==0)
        {
            return;
        }
        if (properties==null)
        {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            properties = new LinkedHashMap();
------- END OF JDK 1.4 DEPENDENCIES ---*/
            properties = new HashMap();
//----- END OF JDK 1.3 FALLBACK -------
        }
        final CaselessStringKey caselessKey = new CaselessStringKey(key);
        final String oldValue = (String) properties.get(caselessKey);
        if (oldValue != null && !oldValue.equals(value))
        {
            throw new RasterFormatException("Duplicated property \""+key+'"'); // TODO
        }
        properties.put(caselessKey, value);
    }

    /**
     * Returns the property for the specified key.
     * Keys are case-insensitive.
     *
     * @param  key The key of the desired property.
     * @return Value for the specified key (never <code>null</code>).
     * @throws NoSuchElementException if no value exists for the specified key.
     */
    public String getProperty(final String key) throws NoSuchElementException
    {
        if (properties!=null)
        {
            final Object value = (String) properties.get(new CaselessStringKey(key.trim()));
            if (value!=null)
            {
                return value.toString();
            }
        }
        throw new NoSuchElementException("Property \""+key+"\" not defined"); // TODO
    }

    /**
     * Check if <code>toSearch</code> appears in the <code>list</code> array.
     * Search is case-insensitive. This is a temporary patch (will be removed
     * when the final API for JSR-108: Units specification will be available).
     */
    private static boolean contains(final String toSearch, final String[] list)
    {
        for (int i=list.length; --i>=0;)
            if (toSearch.equalsIgnoreCase(list[i]))
                return true;
        return false;
    }

    /**
     * Returns the units.  Default implementation fetchs the property
     * value for key <code>"Units"</code> and transform the resulting
     * string into an {@link Unit} object.
     */
    public Unit getUnits() throws NoSuchElementException
    {
        final String text = getProperty("Units");
        if (contains(text, new String[]{"meter","meters","metre","metres","m"}))
        {
            return Unit.METRE;
        }
        else if (contains(text, new String[]{"degree","degrees","deg","°"}))
        {
            return Unit.DEGREE;
        }
        else
        {
            throw new NoSuchElementException("Unknow unit: "+text); // TODO
        }
    }

    /**
     * Returns the datum.  Default implementation fetchs the property
     * value for key <code>"Datum"</code> and transform the resulting
     * string into a {@link HorizontalDatum} object.
     */
    public HorizontalDatum getDatum() throws NoSuchElementException
    {
        final String text = getProperty("Datum");
        /*
         * TODO: parse 'text' when CoordinateSystemAuthorityFactory
         *       will be implemented.
         */
        return HorizontalDatum.WGS84;
    }

    /**
     * Returns the ellipsoid.  Default implementation fetchs the property
     * value for key <code>"Ellipsoid"</code> and transform the resulting
     * string into an {@link Ellipsoid} object.
     */
    public Ellipsoid getEllipsoid() throws NoSuchElementException
    {
        final String text = getProperty("Ellipsoid");
        /*
         * TODO: parse 'text' when CoordinateSystemAuthorityFactory
         *       will be implemented.
         */
        return Ellipsoid.WGS84;
    }

    /**
     * Returns the central longitude and latitude.  Default implementation fetchs the property
     * values for keys <code>"Longitude center"</code> and <code>"Latitude center"</code>, and
     * transform the resulting strings into an {@link Point2D} object.
     */
    public Point2D getCenter() throws NoSuchElementException
    {
        final double x = Double.parseDouble(getProperty("Longitude center"));
        final double y = Double.parseDouble(getProperty("Latitude center"));
        return new Point2D.Double(x,y);
    }

    /**
     * Returns the translation. Default implementation fetchs the property values
     * for keys <code>"False easting"</code> and <code>"False northing"</code>,
     * and transform the resulting strings into an {@link Point2D} object.
     */
    public Point2D getTranslation() throws NoSuchElementException
    {
        final double x = Double.parseDouble(getProperty("False easting"));
        final double y = Double.parseDouble(getProperty("False northing"));
        return new Point2D.Double(x,y);
    }

    /**
     * Returns the projection.  Default implementation fetchs the property
     * value for key <code>"Projection"</code> and transform the resulting
     * string into a {@link Projection} object.
     */
    public Projection getProjection() throws NoSuchElementException
    {
        final String text = getProperty("Projection");
        return factory.createProjection(text, text, getEllipsoid(), getCenter(), getTranslation());
    }

    /**
     * Returns the coordinate system.
     */
    public CoordinateSystem getCoordinateSystem() throws NoSuchElementException
    {
        if (coordinateSystem==null)
        {
            final Unit            units = getUnits();
            final HorizontalDatum datum = getDatum();
            final Projection projection = getProjection();
            final GeographicCoordinateSystem gcs = factory.createGeographicCoordinateSystem("Geographic CS", datum);
            coordinateSystem = factory.createProjectedCoordinateSystem("Projected CS", gcs, projection, units, AxisInfo.X, AxisInfo.Y);
        }
        return coordinateSystem;
    }

    /**
     * Convenience method returning the envelope
     * in geographic coordinate system using WGS
     * 1984 datum.
     *
     * @throws NoSuchElementException if the operation failed.
     */
    public Envelope getGeographicEnvelope() throws NoSuchElementException
    {
        final Envelope         envelope = getEnvelope();
        final CoordinateSystem sourceCS = getCoordinateSystem();
        final CoordinateSystem targetCS = GeographicCoordinateSystem.WGS84;
        try
        {
            final CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();
            return OpenGIS.transform(factory.createFromCoordinateSystems(sourceCS, targetCS).getMathTransform(), envelope);
        }
        catch (TransformException exception)
        {
            NoSuchElementException e = new NoSuchElementException("Can't transform envelope"); // TODO
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            throw e;
        }
    }

    /**
     * Returns the envelope. Default implementation fetchs the property values
     * for keys <code>"ULX"</code>, <code>"ULY"</code> and <code>"Resolution"</code>
     * and transform the resulting strings into an {@link Envelope} object.
     */
    public Envelope getEnvelope() throws NoSuchElementException
    {
        final double x = Double.parseDouble(getProperty("ULX"));
        final double y = Double.parseDouble(getProperty("ULY"));
        final double r = Double.parseDouble(getProperty("Resolution"));
        final GridRange range = getGridRange();
        final int   dimension = range.getDimension();
        final double[]    min = new double[dimension];
        final double[]    max = new double[dimension];
        min[0] = x; min[1] = y - r*range.getLength(1);
        max[1] = y; max[0] = x + r*range.getLength(0);
        /*
         * TODO: What should we do with other dimensions?
         *       Open question...
         */
        return new Envelope(min, max);
    }

    /**
     * Returns the grid range. Default implementation fetchs the property values
     * for keys <code>"x_size"</code> and <code>"y_size"</code>,
     * and transform the resulting strings into a {@link GridRange} object.
     */
    public GridRange getGridRange() throws NoSuchElementException
    {
        final int dimension = getCoordinateSystem().getDimension();
        final int[]   lower = new int[dimension];
        final int[]   upper = new int[dimension];
        Arrays.fill(upper, 1);
        upper[0] = Integer.parseInt(getProperty("x_size"));
        upper[1] = Integer.parseInt(getProperty("y_size"));
        return new GridRange(lower, upper);
    }

    /**
     * Returns the grid coverage.
     */
    public GridCoverage getGridCoverage(final RenderedImage image)
    {
        final GridRange range = getGridRange();
        if (range.getLength(0)!=image.getWidth() || range.getLength(1)!=image.getHeight())
        {
            throw new IllegalArgumentException("Unexpected image size"); // TODO
        }
        return new GridCoverage("Image", image, getCoordinateSystem(), getEnvelope());
    }

    /**
     * List all properties to the specified stream.
     * Comments will be printed first, if present.
     *
     * @param  out Stream to write properties to.
     * @throws IOException if an error occured while listing properties.
     */
    public void listProperties(final Writer out) throws IOException
    {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final String comments = (String) properties.get(null);
        if (comments!=null)
        {
            out.write(comments);
            out.write(lineSeparator);
        }
        int maxLength = 1;
        for (final Iterator it=properties.keySet().iterator(); it.hasNext();)
        {
            final Object key = it.next();
            if (key!=null)
            {
                final int length = key.toString().length();
                if (length > maxLength) maxLength = length;
            }
        }
        for (final Iterator it=properties.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry entry = (Map.Entry) it.next();
            final Object key = entry.getKey();
            if (key!=null)
            {
                out.write(String.valueOf(key));
                out.write(Utilities.spaces(maxLength-key.toString().length()));
                out.write(" = ");
                out.write(String.valueOf(entry.getValue()));
                out.write(lineSeparator);
            }
        }
    }

    /**
     * Returns a string representation of this codec.
     */
    public String toString()
    {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final StringWriter  buffer = new StringWriter();
        buffer.write(Utilities.getShortClassName(this));
        buffer.write("[\"");
        buffer.write(source);
        buffer.write("\"]");
        buffer.write(lineSeparator);
        try
        {
            final Envelope  envelope = getGeographicEnvelope();
            final AngleFormat format = new AngleFormat("DD°MM'SS\"");
            buffer.write(format.format(new  Latitude(envelope.getMaximum(1)))); buffer.write(", ");
            buffer.write(format.format(new Longitude(envelope.getMinimum(0)))); buffer.write(" - ");
            buffer.write(format.format(new  Latitude(envelope.getMinimum(1)))); buffer.write(", ");
            buffer.write(format.format(new Longitude(envelope.getMaximum(0)))); buffer.write(lineSeparator);
        }
        catch (RuntimeException exception)
        {
            // Ignore.
        }
        buffer.write('{');
        buffer.write(lineSeparator);
        try
        {
            final TableWriter table = new TableWriter(buffer, 4);
            table.setMultiLinesCells(true);
            table.nextColumn();
            listProperties(table);
            table.flush();
        }
        catch (IOException exception)
        {
            buffer.write(exception.getLocalizedMessage());
        }
        buffer.write('}');
        buffer.write(lineSeparator);
        return buffer.toString();
    }
}
