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
import org.geotools.pt.Envelope;
import org.geotools.cs.HorizontalDatum;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.io.coverage.PropertyParser;
import org.geotools.cv.SampleDimension;

// Time parsing
import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.NoSuchElementException;

// Logging
import java.util.logging.Logger;


/**
 * Abstract class for codec for text headers.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class AbstractProperties extends PropertyParser
{
    /**
     * Nombre de millisecondes entre le 01/01/1970 00:00 UTC et le 01/01/1950 00:00 UTC.
     * Le 1er janvier 1970 est l'epoch du Java, tandis que le 1er janvier 1950 est celui
     * de la Nasa (son jour julier "0"). La constante <code>EPOCH</code> sert à faire les
     * conversions d'un système à l'autre.
     */
    private static final long EPOCH = -631152000000L; // Pour 1958, utiliser -378691200000L;

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
    private final DateFormat outputDateFormat = new SimpleDateFormat("yyDDD", Locale.FRANCE);

    /**
     * The category lists to be returned by {@link #getSampleDimensions}.
     */
    private final SampleDimension[] bands;

    /**
     * Construct an <code>AbstractProperties</code> object.
     *
     * @param bands The category lists to be returned by {@link #getSampleDimensions}.
     */
    protected AbstractProperties(final SampleDimension[] bands)
    {
        this.bands = bands;
        final TimeZone UTC = TimeZone.getTimeZone("UTC");
        inputDateFormat .setTimeZone(UTC);
        outputDateFormat.setTimeZone(UTC);
    }

    /**
     * Returns the date.
     *
     * @throws NumberFormatException if the date in filename can't be parsed.
     */
    private Date getDate() throws NumberFormatException
    {
        final String filename = new File(getSource()).getName();
        final int length = filename.length();

        int lower=0;
        while (lower<length && !Character.isDigit(filename.charAt(lower))) lower++;
        if (lower >= length) lower=0;

        int upper = length;
        while (upper>lower && filename.charAt(upper-1)!='.') upper--;

        final String text = filename.substring(lower, upper);
        try
        {
            return inputDateFormat.parse(text);
        }
        catch (ParseException exception)
        {
            final NumberFormatException e = new NumberFormatException(text);
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Set the time range for the specified envelope.
     *
     * @throws NumberFormatException if the date in filename can't be parsed.
     */
    final Envelope setTimeRange(final Envelope envelope) throws NumberFormatException
    {
        if (envelope.getDimension() >= 3)
        {
            final long   time = getDate().getTime();
            final double minT = (time-EPOCH) / (24.0*60*60*1000);
            envelope.setRange(2, minT, minT+1);
        }
        return envelope;
    }

    /**
     * Returns the envelope.
     *
     * @throws NoSuchElementException if a required value is missing.
     */
    public Envelope getEnvelope() throws NoSuchElementException
    {return setTimeRange(super.getEnvelope());}

    /**
     * Returns the coordinate system.
     */
    public CoordinateSystem getCoordinateSystem()
    {
        final CoordinateSystem cs = super.getCoordinateSystem();
        return new CompoundCoordinateSystem(cs.getName(null), cs, UTC);
    }

    /**
     * Returns the datum. Default to WGS 84.
     */
    public HorizontalDatum getDatum()
    {
        try
        {
            return super.getDatum();
        }
        catch (NoSuchElementException exception)
        {
            Logger.getLogger("org.geotools.gcs").warning(exception.getLocalizedMessage());
            return HorizontalDatum.WGS84;
        }
    }

    /**
     * Returns the to bands.
     */
    public SampleDimension[] getSampleDimensions()
    {return bands;}

    /**
     * Gets the output filename <strong>without</strong> extension.
     * The default implementation returns a string from the date.
     * Subclasses should overrides this method in order to add a
     * prefix.
     */
    protected String getOutputFilename()
    {return outputDateFormat.format(getDate());}
}
