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
import org.geotools.gc.GridCoverage;
import org.geotools.cv.SampleDimension;
import org.geotools.io.coverage.GridCoverageReader;
import org.geotools.io.coverage.ExoreferencedGridCoverageReader;

// Input/output
import java.io.File;
import java.io.IOException;

// Images
import javax.imageio.ImageIO;
import javax.imageio.IIOException;
import java.awt.image.RenderedImage;
import javax.imageio.spi.IIORegistry;

// Resources
import java.util.Locale;


/**
 * Ajoute le code nécessaire au décodage des images
 * fournit par la station des îles Canaries.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class GridCoverageExchange extends org.geotools.gc.GridCoverageExchange
{
    /**
     * Register a set of service providers
     * the first time this class is loaded.
     */
    static
    {
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new fr.ird.io.image.Canarias_RAW());
        // Note: previous SPIs (Aviso_ASC, etc.) will be discarted, since
        //       IIORegistry register only singletons for each leaf class.
    }

    /**
     * The parser to use for SST images from the Canarias islands station.
     */
    private static final int SST = 0;

    /**
     * The parser to use for chlorophylle images from the Canarias islands station.
     */
    private static final int CHLORO = 1;

    /**
     * Grid coverage readers. This array lenght must be long enough
     * to contains all index {@link #SST}, {@link #CHLORO}, etc.
     */
    private final GridCoverageReader[] readers = new GridCoverageReader[2];

    /**
     * The properties parsers to use for images.
     */
    private final AbstractProperties[] parsers = new AbstractProperties[readers.length];

    /**
     * The bands to be used for constructing {@link GridCoverage} objects.
     */
    private final SampleDimension[] bands;

    /**
     * Properties read during the last call to {@link #createFromName}.
     */
    private transient AbstractProperties lastProperties;

    /**
     * Construct a <code>GridCoverageExchange</code> object.
     *
     * @param bands The bands to be used for constructing {@link GridCoverage}s.
     */
    public GridCoverageExchange(final SampleDimension[] bands)
    {this.bands = bands;}

    /**
     * Create a new {@link GridCoverage} from a grid coverage file.
     *
     * @param  name File name (including path) from which to create a grid coverage.
     * @return The grid coverage.
     * @throws IOException if an error occurs during reading.
     * @throws IIOException if a grid coverage can't be create from the specified name.
     */
    public synchronized GridCoverage createFromName(final String name) throws IOException
    {
        IOException error = null;
        lastProperties    = null;
        Object input      = new File(name);
        for (int i=0; i<readers.length; i++)
        {
            AbstractProperties parser = parsers[i];
            GridCoverageReader reader = readers[i];
            if (reader==null)
            {
                switch (i)
                {
                    case SST:    parser=new ErdasProperties (bands); break;
                    case CHLORO: parser=new SimpleProperties(bands); break;
                    default:     throw new AssertionError(i);
                }
                reader = new ExoreferencedGridCoverageReader("RAW-Canarias", "raw", parser);
                reader.setLocale(getLocale());
                readers[i] = reader;
                parsers[i] = parser;
            }
            try
            {
                reader.setInput(input, true);
                lastProperties = parser;
                return reader.getGridCoverage(0);
            }
            catch (IOException exception)
            {
                if (error==null)
                    error=exception;
            }
        }
        if (error!=null) throw error;
        return super.createFromName(name);
    }

    /**
     * Returns a string representation of properties for the last
     * grid coverage read. If no grid coverage has been read, then
     * this method returns <code>null</code>.
     */
    public synchronized String getLastProperties()
    {return (lastProperties!=null) ? lastProperties.toString() : null;}

    /**
     * Gets the output filename, or <code>null</code> if none.
     */
    public synchronized String getOutputFilename()
    {return (lastProperties!=null) ? lastProperties.getOutputFilename() : null;}

    /**
     * Sets the current {@link Locale} of this <code>GridCoverageExchange</code>
     * to the given value. A value of <code>null</code> removes any previous
     * setting, and indicates that the reader should localize as it sees fit.
     */
    public synchronized void setLocale(final Locale locale)
    {
        super.setLocale(locale);
        for (int i=0; i<readers.length; i++)
            if (readers[i]!=null)
                readers[i].setLocale(locale);
    }
}
