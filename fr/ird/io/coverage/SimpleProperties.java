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

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
import net.seagis.cs.Ellipsoid;
import net.seagis.cs.HorizontalDatum;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.CompoundCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.ct.CoordinateTransformationFactory;
import net.seagis.ct.TransformException;
import net.seagis.ct.MathTransform;

import net.seagis.gc.GridRange;
import net.seagis.cv.CategoryList;
import net.seagis.resources.OpenGIS;
import net.seagis.io.coverage.PropertyParser;

// Miscellaneous
import javax.units.Unit;
import java.util.NoSuchElementException;
import java.awt.image.RasterFormatException;


/**
 * Codec for simple text header. Used for chlorophylle-a
 * images in RAW binary files from the Canarias station.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class SimpleProperties extends AbstractProperties
{
    /**
     * List of keys used in an ERDAS file.
     */
    private static final String[] KEYS=
    {
        "Projection",       "Projection",         // e.g.: "projection = Mercator simple"
        "Latitude center",  "latitude_of_origin", // e.g.: "latitude center = 0.0000000"
        "Longitude center", "central_meridian",   // e.g.: "longitude center = 2.5000000"
        "Limit North",      "ULY",                // e.g.: "Limit North = 45.000000"
        "Limit South",      "BRY",                // e.g.: "Limit South = 34.000000"
        "Limit West",       "ULX",                // e.g.: "Limit West = -7.0000000"
        "Limit East",       "BRX",                // e.g.: "Limit East = 12.000000"
        "X size",           "Image width",        // e.g.: "X size = 1200"
        "Y size",           "Image height"        // e.g.: "Y size = 700"
    };

    /**
     * Construct a <code>ErdasProperties</code> object.
     *
     * @param categories The category lists to be
     *        returned by {@link #getCategoryList}.
     */
    public SimpleProperties(final CategoryList[] categories)
    {super(categories);}

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
        if (key==null)
        {
            return super.get(key, defaultValue);
        }
        // Looks for synonyms
        for (int i=KEYS.length+1; (i-=2)>=0;)
        {
            if (KEYS[i].equalsIgnoreCase(key))
            {
                key = KEYS[i-1];
                break;
            }
        }
        Object value = super.get(key, defaultValue);
        if (key.equalsIgnoreCase("Projection"))
        {
            // TODO: perform a better check.
            if (!value.toString().equalsIgnoreCase("Mercator isotropic"))
                value = "Mercator_1SP";
        }
        return value;
    }

    /**
     * Returns the units.
     */
    public Unit getUnits()
    {return get("Projection").toString().equalsIgnoreCase("Mercator isotropic") ? Unit.DEGREE : Unit.METRE;}

    /**
     * Returns the datum.
     */
    public HorizontalDatum getDatum()
    {return HorizontalDatum.WGS84;}

    /**
     * Returns the ellipsoid.
     */
    public Ellipsoid getEllipsoid()
    {return Ellipsoid.WGS84;}

    /**
     * Returns the envelope.
     *
     * @throws NoSuchElementException if a required value is missing.
     */
    public synchronized Envelope getEnvelope() throws NoSuchElementException
    {
        final GridRange range = getGridRange();
        final int   dimension = range.getDimension();
        final double[]    min = new double[dimension];
        final double[]    max = new double[dimension];
        min[0] = getAsDouble("ULX");
        min[1] = getAsDouble("BRY");
        max[0] = getAsDouble("BRX");
        max[1] = getAsDouble("ULY");
        Envelope envelope = setTimeRange(new Envelope(min, max));
        if (Unit.METRE.canConvert(getUnits())) try
        {
            final CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();

            MathTransform transform;
            final CoordinateSystem sourceCS = GeographicCoordinateSystem.WGS84;
            final CoordinateSystem targetCS = ((CompoundCoordinateSystem) getCoordinateSystem()).getHeadCS();
            transform = factory.createFromCoordinateSystems(sourceCS, targetCS).getMathTransform();
            transform = factory.getMathTransformFactory().createPassThroughTransform(0, transform, 1);
            envelope  = OpenGIS.transform(transform, envelope);
        }
        catch (TransformException exception)
        {
            NoSuchElementException e = new NoSuchElementException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        return envelope;
    }

    /**
     * Gets the output filename <strong>without</strong> extension.
     */
    protected String getOutputFilename()
    {return "CHL"+super.getOutputFilename();}
}
