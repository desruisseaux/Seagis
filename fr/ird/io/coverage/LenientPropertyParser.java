/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.coverage;

// J2SE
import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import javax.imageio.IIOException;
import java.util.logging.Logger;

// Geotools dependencies
import org.geotools.pt.Envelope;
import org.geotools.cs.Ellipsoid;
import org.geotools.cv.SampleDimension;
import org.geotools.cs.HorizontalDatum;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.io.coverage.PropertyParser;
import org.geotools.io.coverage.PropertyException;
import org.geotools.io.coverage.MissingPropertyException;
import org.geotools.units.Unit;


/**
 * A properties parser for text headers. This parser is not very rigourous.
 * It tries to adapt to the highly variable file format that we have to handle.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class LenientPropertyParser extends PropertyParser {
    /**
     * The list of alias.
     */
    private static final Object[] ALIAS = {
        UNITS,               "Units",
        DATUM,               "Datum",
        ELLIPSOID,           "Ellipsoid",
        PROJECTION,          "Projection",         "Projection Name",
        PROJECTION_NAME,     "Projection",         "Projection Name",
        LATITUDE_OF_ORIGIN,  "Latitude of origin", "Latitude center",  "Lat center", "center Lat",
        CENTRAL_MERIDIAN,    "Central meridian",   "Longitude center", "Lon center", "center Lon",
        FALSE_EASTING,       "False easting",
        FALSE_NORTHING,      "False northing",
        SEMI_MAJOR,          "Semi major",
        SEMI_MINOR,          "Semi minor",
        Y_MAXIMUM,           "Limit North",        "ULY",
        Y_MINIMUM,           "Limit South",        "BRY",
        X_MINIMUM,           "Limit West",         "ULX",
        X_MAXIMUM,           "Limit East",         "BRX",
        X_RESOLUTION,        "Resolution x",       "XResolution", "Resolution",
        Y_RESOLUTION,        "Resolution y",       "YResolution", "Resolution",
        WIDTH,               "Image width",        "X size",      "ncol",
        HEIGHT,              "Image height",       "Y size",      "nlig"
    };

    /**
     * Mapping between some commons projection names
     * and OpenGIS's projection class name.
     */
    private static final String[] PROJECTIONS= {
        "Mercator",            "Mercator_1SP",
        "Mercator isotropic",  "Mercator_1SP",
        "Geographic (Lat/Lon)", null
    };

    /**
     * Nombre de millisecondes entre le 01/01/1970 00:00 UTC et le 01/01/1950 00:00 UTC.
     * Le 1er janvier 1970 est l'epoch du Java, tandis que le 1er janvier 1950 est celui
     * de la Nasa (son jour julien "0"). La constante <code>EPOCH</code> sert à faire les
     * conversions d'un système à l'autre.
     */
    private static final long EPOCH = -631152000000L; // Pour 1958, utiliser -378691200000L;

    /**
     * The logger in case of warnings.
     */
    private static final Logger LOGGER = Logger.getLogger("fr.ird.io.coverage");

    /**
     * The time coordinate system. Epoch is January 1, 1950. Units are days.
     */
    private static final TemporalCoordinateSystem UTC = new TemporalCoordinateSystem("UTC", new Date(EPOCH));

    /**
     * Date format to use for parsing date in input filename.
     */
    private final DateFormat inputDateFormat = new SimpleDateFormat("yyMMdd", Locale.FRANCE);

    /**
     * Date format to use for formating date in output filename.
     */
    private final DateFormat outputDateFormat;

    /**
     * The sample dimensions to be returned by {@link #getSampleDimensions}.
     */
    private final SampleDimension[] bands;

    /**
     * Construct an <code>LenientPropertyParser</code> object.
     *
     * @param  bands The category lists to be returned by {@link #getSampleDimensions}.
     * @param  outputPattern The pattern for output file. This pattern must matches the
     *         {@link SimpleDateFormat} specification. Example: <code>"'SST'yyDDD"</code>.
     * @throws PropertyException if the initialization failed.
     */
    public LenientPropertyParser(final SampleDimension[] bands, final String outputPattern)
        throws PropertyException
    {
        this.bands = bands;
        outputDateFormat = new SimpleDateFormat(outputPattern, Locale.FRANCE);
        final TimeZone UTC = TimeZone.getTimeZone("UTC");
        inputDateFormat .setTimeZone(UTC);
        outputDateFormat.setTimeZone(UTC);
        Key key = null;
        for (int i=0; i<ALIAS.length; i++) {
            final Object alias = ALIAS[i];
            if (alias instanceof Key) {
                key = (Key) alias;
            } else {
                addAlias(key, alias.toString());
            }
        }
    }

    /**
     * Parse a header line. This implementation overrides the default
     * implementation in order to accept missing "=" sign, as long as
     * a key is recognized.
     *
     * @param  line The line to parse.
     * @return <code>true</code> if the line has been consumed.
     * @throws IIOException if the line is badly formatted,
     *         or if the line contains an inconsistent property.
     */
    protected boolean parseLine(final String line) throws IIOException {
        if (line.indexOf('=') < 0) {
            for (int i=ALIAS.length; --i>=0;) {
                if (ALIAS[i] instanceof String) {
                    final String    key = (String) ALIAS[i];
                    final int keyLength = key.length();
                    if (line.regionMatches(true, 0, key, 0, keyLength)) {
                        add(key, line.substring(keyLength).trim());
                        return true;
                    }
                }
            }
        }
        return super.parseLine(line);
    }
    
    /**
     * Returns the property for the specified key. This method overrides {@link PropertyParser#get}
     * in order to transform projection name into OpenGIS's projection classification.
     *
     * @param  key The key of the desired property. Keys are case insensitive and format neutral.
     * @return Value for the specified key (never <code>null</code>).
     * @throws MissingPropertyException if no value exists for the specified key.
     */
    public Object get(final Key key) throws MissingPropertyException {
        Object value = super.get(key);
        if (PROJECTION.equals(key)) {
            final String searchFor = value.toString();
            for (int i=PROJECTIONS.length; (i-=2)>=0;) {
                if (PROJECTIONS[i].equalsIgnoreCase(searchFor)) {
                    value = PROJECTIONS[i+1];
                    break;
                }
            }
        }
        /*
         * Special processing for "ULX" and "ULY" properties:
         * ignore the characters after the number.
         */
        if (value!=null && key!=null) {
            final String name = key.toString();
            if (name.equalsIgnoreCase("ULX") || name.equalsIgnoreCase("ULY")) {
                final String text = value.toString();
                final int index = text.indexOf(' ');
                if (index >= 0) {
                    value = text.substring(0, index);
                }
            }
        }
        return value;
    }

    /**
     * Returns the date for the image to be read.
     * This implementation deduce the date from the filename.
     *
     * @throws PropertyException if the date in filename can't be parsed.
     */
    private Date getDate() throws PropertyException {
        final String filename = new File(getSource()).getName();
        final int length = filename.length();

        int lower=0;
        while (lower<length && !Character.isDigit(filename.charAt(lower))) lower++;
        if (lower >= length) lower=0;

        int upper = length;
        while (upper>lower && filename.charAt(upper-1)!='.') upper--;

        final String text = filename.substring(lower, upper);
        try {
            return inputDateFormat.parse(text);
        } catch (ParseException exception) {
            throw new PropertyException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Returns the units. This implementation invokes the default implementation,
     * and default to {@link Unit#DEGREE} or {@link Unit#METRE} if the default
     * implementation failed.
     */
    public Unit getUnits() {
        try {
            return super.getUnits();
        } catch (PropertyException exception) {
            LOGGER.warning(exception.getLocalizedMessage());
            return contains(PROJECTION) ? Unit.METRE : Unit.DEGREE;
        }
    }

    /**
     * Returns the datum. This implementation invokes the default implementation,
     * and default to WGS84 if the default implementation failed.
     */
    public HorizontalDatum getDatum() {
        try {
            return super.getDatum();
        } catch (PropertyException exception) {
            LOGGER.warning(exception.getLocalizedMessage());
            return HorizontalDatum.WGS84;
        }
    }

    /**
     * Returns the ellipsoid. This implementation invokes the default implementation,
     * and default to WGS84 if the default implementation failed.
     */
    public Ellipsoid getEllipsoid() {
        try {
            return super.getEllipsoid();
        } catch (PropertyException exception) {
            LOGGER.warning(exception.getLocalizedMessage());
            return Ellipsoid.WGS84;
        }
    }

    /**
     * Returns the coordinate system. This implementation invokes
     * the default implementation and add a time axis.
     */
    public CoordinateSystem getCoordinateSystem() throws PropertyException {
        final CoordinateSystem cs = super.getCoordinateSystem();
        return new CompoundCoordinateSystem(cs.getName(null), cs, UTC);
    }

    /**
     * Returns the envelope. This implementation invokes the
     * default implementation and add a time dimension. The
     * time dimension is at index 2.
     */
    public Envelope getEnvelope() throws PropertyException {
        final Envelope envelope = super.getEnvelope();
        if (envelope.getDimension() >= 3) {
            final long   time = getDate().getTime();
            final double minT = (time-EPOCH) / (24.0*60*60*1000);
            envelope.setRange(2, minT, minT+1);
        }
        return envelope;
    }

    /**
     * Returns the sample dimensions.
     */
    public SampleDimension[] getSampleDimensions() {
        return bands;
    }

    /**
     * Gets the output filename <strong>without</strong> extension.
     */
    final String getOutputFilename() {
        try {
            return outputDateFormat.format(getDate());
        } catch (PropertyException exception) {
            // Should not happen
            IllegalStateException e = new IllegalStateException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }
}
