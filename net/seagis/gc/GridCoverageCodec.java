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
import net.seagis.cs.GeographicCoordinateSystem;

// Images
import javax.imageio.IIOException;

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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.media.jai.util.CaselessStringKey;

// Miscellaneous
import javax.units.Unit;
import java.awt.geom.Point2D;
import net.seagis.resources.Utilities;


/**
 * Base class for encoding and decoding of Grid Coverage objects.
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
     * @param in The stream to read until EOF.
     *           The stream will not be closed.
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
     * @throws IIOException if the line is badly formatted, or if the
     *         line contains a property already stored.
     */
    protected boolean parseHeaderLine(final String line) throws IIOException
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
     *
     * @param  key   The key for the property to add.
     * @param  value The value for the property to add.
     * @throws IIOException if a different value already
     *         exists for the specified key.
     */
    public void addProperty(String key, String value) throws IIOException
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
            throw new IIOException("Duplicated property"); // TODO
        }
        properties.put(caselessKey, value);
    }

    /**
     * Returns the property for the specified key.
     * Keys are case-insensitive.
     *
     * @param  key The key of the desired property.
     * @return Value for the specified key (never <code>null</code>).
     * @throws IIOException if no value exists for the specified key.
     */
    public String getProperty(final String key) throws IIOException
    {
        if (properties!=null)
        {
            final String value = (String) properties.get(new CaselessStringKey(key.trim()));
            if (value!=null)
            {
                return value;
            }
        }
        throw new IIOException("Property not defined"); // TODO
    }

    /**
     * Returns the first word of the property for
     * the specified key. Keys are case-insensitive.
     *
     * @param  key The key of the desired property.
     * @return Value for the specified key (never <code>null</code>).
     * @throws IIOException if no value exists for the specified key.
     */
    private String getPropertyWord(final String key) throws IIOException
    {
        final String value = getProperty(key);
        final int index = value.indexOf(' ');
        return (index>=0) ? value.substring(0, index) : value;
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
     * Returns the units. This method fetchs the property value for the key
     * <code>key</code> and convert the resulting string into an {@link Unit}
     * object. If <code>key</code> is null, then some implementation-dependent
     * default key is used.
     */
    public Unit getUnits(String key) throws IIOException
    {
        if (key==null) key="Units";
        final String value = getProperty(key);
        if (contains(value, new String[]{"meter","meters","metre","metres","m"}))
        {
            return Unit.METRE;
        }
        else if (contains(value, new String[]{"degree","degrees","deg","°"}))
        {
            return Unit.DEGREE;
        }
        else
        {
            throw new IIOException("Unknow unit: "+value); // TODO
        }
    }

    /**
     * Returns the datum. This method fetchs the property value for the key
     * <code>key</code> and convert the resulting string into a {@link Datum}
     * object. If <code>key</code> is null, then some implementation-dependent
     * default key is used.
     */
    public HorizontalDatum getDatum(String key) throws IIOException
    {
        if (key==null) key="Datum";
        final String value = getProperty(key);
        /*
         * TODO: parse 'value' when CoordinateSystemAuthorityFactory
         *       will be implemented.
         */
        return HorizontalDatum.WGS84;
    }
    

    /**
     * Returns the ellipsoid. This method fetchs the property value for the key
     * <code>key</code> and convert the resulting string into an {@link Ellipsoid}
     * object. If <code>key</code> is null, then some implementation-dependent
     * default key is used.
     */
    public Ellipsoid getEllipsoid(String key) throws IIOException
    {
        if (key==null) key="Ellipsoid";
        final String value = getProperty(key);
        /*
         * TODO: parse 'value' when CoordinateSystemAuthorityFactory
         *       will be implemented.
         */
        return Ellipsoid.WGS84;
    }

    /**
     * Returns the central longitude and latitude. This method fetchs the property
     * values for the keys (<code>xKey</code>,<code>yKey</code>) and convert the
     * resulting string into a {@link Point2D} object. If some keys are null, then
     * some implementation-dependent default keys are used.
     */
    public Point2D getCenter(String xKey, String yKey) throws IIOException
    {
        if (xKey==null) xKey="Lon center";
        if (yKey==null) yKey="Lat center";
        final double x = Double.parseDouble(getProperty(xKey));
        final double y = Double.parseDouble(getProperty(yKey));
        return new Point2D.Double(x,y);
    }

    /**
     * Returns the projection. This method fetchs the property value for the key
     * <code>key</code> and convert the resulting string into a {@link Projection}
     * object. If <code>key</code> is null, then some implementation-dependent
     * default key is used.
     */
    public Projection getProjection(String key) throws IIOException
    {
        if (key==null) key="Projection Name";
        final String value = getProperty(key);
        String classname = value;
        if (classname.equalsIgnoreCase("Mercator"))
        {
            classname = "Mercator_1SP";
        }
        /*
         * TODO: take "False easting" and "False northing" in acount.
         *       Assuming 0 for now.
         */
        return factory.createProjection(value, classname, getEllipsoid(null), getCenter(null,null));
    }

    /**
     * Returns the coordinate system.
     */
    public CoordinateSystem getCoordinateSystem() throws IIOException
    {
        final Unit            units = getUnits(null);
        final HorizontalDatum datum = getDatum(null);
        final Projection projection = getProjection(null);
        final GeographicCoordinateSystem gcs = factory.createGeographicCoordinateSystem("Geographic CS", datum);
        return factory.createProjectedCoordinateSystem("Projected CS", gcs, projection, units, AxisInfo.X, AxisInfo.Y);
    }

    /**
     * Returns the grid range. This method fetchs the property values for the keys
     * (<code>xKey</code>,<code>yKey</code>) and convert the resulting string into
     * a {@link GridRange} object. If some keys are null, then some
     * implementation-dependent default keys are used.
     */
    public GridRange getGridRange(String xKey, String yKey) throws IIOException
    {
        if (xKey==null) xKey="x_size";
        if (yKey==null) yKey="y_size";
        final int x = Integer.parseInt(getProperty(xKey));
        final int y = Integer.parseInt(getProperty(yKey));
        return new GridRange(new int[2], new int[] {x,y});
    }

    /**
     * Returns the envelope. This method fetchs the property values for the keys
     * (<code>xKey</code>,<code>yKey</code>) and convert the resulting string into
     * a {@link Envelope} object. If some keys are null, then some
     * implementation-dependent default keys are used.
     */
    public Envelope getEnvelope(String xKey, String yKey, String xRes, String yRes) throws IIOException
    {
        if (xKey==null) xKey="ULX";
        if (yKey==null) yKey="ULY";
        if (xRes==null) xRes="Resolution";
        if (yRes==null) yRes="Resolution";
        final double  x = Double.parseDouble(getPropertyWord(xKey));
        final double  y = Double.parseDouble(getPropertyWord(yKey));
        final double rx = Double.parseDouble(getPropertyWord(xRes));
        final double ry = Double.parseDouble(getPropertyWord(yRes));
        final GridRange range = getGridRange(null,null);
        return new Envelope(new double[]{x,y}, new double[]
        {
            x + rx*range.getLength(0),
            y + ry*range.getLength(1)
        });
    }

    /**
     * Returns the grid geometry.
     */
    public GridGeometry getGridGeometry() throws IIOException
    {
        final GridRange   range = getGridRange(null,null);
        final boolean[] inverse = new boolean[range.getDimension()];
        inverse[1] = true; // Inverse Y axis.
        return new GridGeometry(range, getEnvelope(null,null,null,null), inverse);
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
