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

// Geotools dependencies
import org.geotools.cs.Projection;
import org.geotools.cv.CategoryList;

// Miscellaneous
import java.util.NoSuchElementException;
import java.awt.image.RasterFormatException;


/**
 * Codec for text header written with ERDAS Imagine. Used
 * for Sea Surface Temperature images in RAW binary files
 * from the Canarias station.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ErdasProperties extends AbstractProperties
{
    /**
     * List of keys used in an ERDAS file.
     */
    private static final String[] KEYS=
    {
        "Resolution",      null,                 // e.g.: "resolution = 1000.0000"
        "ULX",             null,                 // e.g.: "ULX = 217904.31  -13:00:42"
        "ULY",             null,                 // e.g.: "ULY = 5663495.1   50:00:12"
        "Projection Name", "Projection",         // e.g.: "Projection Name = Mercator"
        "Ellipsoid",       null,                 // e.g.: "Ellipsoid = Clarke 1866"
        "Datum",           null,                 // e.g.: "Datum = Clarke 1866"
        "Units",           null,                 // e.g.: "Units = meters"
        "Lat center",      "latitude_of_origin", // e.g.: "Lat center = 28.066700"
        "Lon center",      "central_meridian",   // e.g.: "Lon center = -15.216700"
        "False easting",   "false_easting",      // e.g.: "False easting = 0.00000000"
        "False northing",  "false_northing",     // e.g.: "False northing = 0.00000000"
        "x_size",          "Image width",        // e.g.: "x_size = 2459"
        "y_size",          "Image height",       // e.g.: "y_size = 2128"
        "ncol",            "Image width",        // e.g.: "ncol : 2038"
        "nlig",            "Image height"        // e.g.: "nlig : 1130"
    };

    /**
     * Construct a <code>ErdasProperties</code> object.
     *
     * @param categories The category lists to be
     *        returned by {@link #getCategoryList}.
     */
    public ErdasProperties(final CategoryList[] categories)
    {super(categories);}

    /**
     * Parse a header line. This implementation overrides the default
     * implementation in order to accept missing "=" sign, as long as
     * a key is recognized.
     *
     * @param  line The line to parse.
     * @return <code>true</code> if the line has been consumed.
     * @throws RasterFormatException if the line is badly formatted,
     *         or if the line contains a property already stored.
     */
    protected boolean parseLine(final String line) throws RasterFormatException
    {
        for (int i=KEYS.length; (i-=2)>=0;)
        {
            final String    key = KEYS[i];
            final int keyLength = key.length();
            if (line.regionMatches(true, 0, key, 0, keyLength))
            {
                final int lineLength = line.length();
                for (int scan=keyLength; scan<lineLength; scan++)
                {
                    final char c = line.charAt(scan);
                    if (!Character.isSpaceChar(c))
                    {
                        if (c=='=') scan++;
                        if (c==':') scan++;
                        if (scan==keyLength) break;
                        add(key, line.substring(scan));
                        return true;
                    }
                }
            }
        }
        return super.parseLine(line);
    }

    /**
     * Returns the property for the specified key.
     * Keys are case-insensitive.
     *
     * @param  key The key of the desired property.
     * @param  defaultValue The default value.
     * @return Value for the specified key (never <code>null</code>).
     * @throws NoSuchElementException if no value exists for the specified key.
     */
    public Object get(String key, final Object defaultValue) throws NoSuchElementException
    {
        /*
         * Looks for synonyms
         */
        for (int i=KEYS.length+1; (i-=2)>=0;)
        {
            final String cmp = KEYS[i];
            if (cmp!=null && cmp.equalsIgnoreCase(key))
            {
                if (super.get(KEYS[i-1], null)!=null)
                {
                    key = KEYS[i-1];
                    break;
                }
            }
        }
        /*
         * Special case for ULX and ULY:
         * Retains only the first word.
         */
        Object value = super.get(key, defaultValue);
        if (value!=null && key!=null)
        {
            if (key.equalsIgnoreCase("ULX") || key.equalsIgnoreCase("ULY"))
            {
                final String text = value.toString();
                final int index = text.indexOf(' ');
                if (index>=0)
                {
                    value = text.substring(0, index);
                }
            }
        }
        return value;
    }

    /**
     * Gets the output filename <strong>without</strong> extension.
     */
    protected String getOutputFilename()
    {return "SST"+super.getOutputFilename();}
}
