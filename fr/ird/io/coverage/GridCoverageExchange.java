/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.coverage;

// J2SE
import java.io.File;
import java.io.IOException;
import java.util.Locale;

// Images
import javax.imageio.ImageIO;
import javax.imageio.IIOException;
import java.awt.image.RenderedImage;
import javax.imageio.spi.IIORegistry;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.cv.SampleDimension;
import org.geotools.io.coverage.GridCoverageReader;
import org.geotools.io.coverage.ExoreferencedGridCoverageReader;


/**
 * Ajoute le code n�cessaire au d�codage des images
 * fournit par la station des �les Canaries.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class GridCoverageExchange extends org.geotools.gc.GridCoverageExchange {
    /**
     * Register a set of service providers
     * the first time this class is loaded.
     */
    static {
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
     * The list of prefix in filename for each type.
     */
    private static final String[] PREFIX = new String[2];
    static {
        PREFIX[SST]    = "SST";
        PREFIX[CHLORO] = "CHL";
    }

    /**
     * Grid coverage readers. This array lenght must be long enough
     * to contains all index {@link #SST}, {@link #CHLORO}, etc.
     */
    private final GridCoverageReader[] readers = new GridCoverageReader[PREFIX.length];

    /**
     * The properties parsers to use for images.
     */
    private final LenientPropertyParser[] parsers = new LenientPropertyParser[readers.length];

    /**
     * The bands to be used for constructing {@link GridCoverage} objects.
     */
    private final SampleDimension[] bands;

    /**
     * Properties read during the last call to {@link #createFromName}.
     */
    private transient LenientPropertyParser lastProperties;

    /**
     * Construct a <code>GridCoverageExchange</code> object.
     *
     * @param bands The bands to be used for constructing {@link GridCoverage}s.
     */
    public GridCoverageExchange(final SampleDimension[] bands) {
        this.bands = bands;
    }

    /**
     * Create a new {@link GridCoverage} from a grid coverage file.
     *
     * @param  filename File name (including path) from which to create a grid coverage.
     * @return The grid coverage.
     * @throws IOException if an error occurs during reading.
     * @throws IIOException if a grid coverage can't be create from the specified name.
     */
    public synchronized GridCoverage createFromName(final String filename) throws IOException {
        IOException error = null;
        lastProperties    = null;
        File   input      = new File(filename);
        final int type    = getType(input.getName());
        LenientPropertyParser parser = parsers[type];
        GridCoverageReader    reader = readers[type];
        if (reader == null) {
            switch (type) {
                case SST:    parser=new LenientPropertyParser(bands, "'SST'yyDDD"); break;
                case CHLORO: parser=new LenientPropertyParser(bands, "'CHL'yyDDD"); break;
                default:     throw new AssertionError(type);
            }
            reader = new ExoreferencedGridCoverageReader("RAW-Canarias", "raw", parser);
            reader.setLocale(getLocale());
            readers[type] = reader;
            parsers[type] = parser;
        }
        reader.setInput(input, true);
        lastProperties = parser;
        return reader.getGridCoverage(0);
    }

    /**
     * Returns the image type from its filename.
     */
    private static int getType(String filename) throws IIOException {
        filename = filename.toUpperCase();
        for (int i=0; i<PREFIX.length; i++) {
            if (filename.indexOf(PREFIX[i]) >= 0) {
                return i;
            }
        }
        throw new IIOException("Type de fichier inconnu: \""+filename+'"');
    }

    /**
     * Returns a string representation of properties for the last
     * grid coverage read. If no grid coverage has been read, then
     * this method returns <code>null</code>.
     */
    public synchronized String getLastProperties() {
        return (lastProperties!=null) ? lastProperties.toString() : null;
    }

    /**
     * Gets the output filename, or <code>null</code> if none.
     */
    public synchronized String getOutputFilename() {
        return (lastProperties!=null) ? lastProperties.getOutputFilename() : null;
    }

    /**
     * Sets the current {@link Locale} of this <code>GridCoverageExchange</code>
     * to the given value. A value of <code>null</code> removes any previous
     * setting, and indicates that the reader should localize as it sees fit.
     */
    public synchronized void setLocale(final Locale locale) {
        super.setLocale(locale);
        for (int i=0; i<readers.length; i++) {
            if (readers[i]!=null) {
                readers[i].setLocale(locale);
            }
        }
    }
}
