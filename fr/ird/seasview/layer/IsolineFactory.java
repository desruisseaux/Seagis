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
package fr.ird.seasview.layer;

// Map components
import fr.ird.map.Isoline;
import fr.ird.map.io.GEBCOReader;

// Input/output
import java.net.URL;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;


/**
 * An isoline factory.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class IsolineFactory extends fr.ird.map.io.IsolineFactory
{
    /**
     * The source directory for isolines data.
     */
    private static final String SOURCE_DIRECTORY = "compilerData/map/";

    /**
     * The directory for cached data.
     */
    private static final String CACHE_DIRECTORY = "applicationData/cache/";

    /**
     * The extension for source file.
     */
    private static final String SOURCE_EXTENSION = ".asc";

    /**
     * The extension for cache file.
     */
    private static final String CACHE_EXTENSION = ".zip";

    /**
     * This factory's name.
     */
    private final String name;

    /**
     * Construct an isoline factory with the specified name.
     *
     * @param  name The source name (not including directory or file extension).
     * @throws FileNotFoundException if the resources has not been found.
     */
    public IsolineFactory(final String name) throws FileNotFoundException
    {
        super(toCacheURL(name));
        this.name = name;
    }

    /**
     * Returns the source URL for the specified name.
     *
     * @param  name The source name (not including directory or file extension).
     * @throws FileNotFoundException if the resources has not been found.
     */
    private static URL toSourceURL(final String name) throws FileNotFoundException
    {
        final String path = SOURCE_DIRECTORY+name+SOURCE_EXTENSION;
        final URL url = IsolineFactory.class.getClassLoader().getResource(path);
        if (url==null)
        {
            throw new FileNotFoundException(path);
        }
        return url;
    }

    /**
     * Returns the cache file for the specified name.
     *
     * @param  name The source name (not including directory or file extension).
     * @throws FileNotFoundException if the resources has not been found.
     */
    private static URL toCacheURL(final String name) throws FileNotFoundException
    {
        final String path = CACHE_DIRECTORY+name+CACHE_EXTENSION;
        URL url = IsolineFactory.class.getClassLoader().getResource(path);
        if (url != null)
        {
            return url;
        }
        /*
         * If the cache file is not found, try to deduce
         * its path from the source file path.
         */
        url = toSourceURL(name);
        Exception cause = null;
        final String sourcePath = url.toExternalForm();
        final int directory = sourcePath.lastIndexOf(SOURCE_DIRECTORY);
        if (directory >= 0)
        {
            final int filename = directory + SOURCE_DIRECTORY.length();
            final int extension = sourcePath.indexOf(SOURCE_EXTENSION, filename);
            if (extension >= 0)
            {
                final int query = extension+SOURCE_EXTENSION.length();
                final String cachePath = sourcePath.substring(0, directory)        +
                                         CACHE_DIRECTORY                           +
                                         sourcePath.substring(filename, extension) +
                                         CACHE_EXTENSION                           +
                                         sourcePath.substring(query);
                try
                {
                    url = new URL(cachePath);
                    return url;
                }
                catch (MalformedURLException exception)
                {
                    cause = exception;
                }
            }
        }
        FileNotFoundException e = new FileNotFoundException(path);
        if (cause!=null) e.initCause(cause);
        throw e;
    }

    /**
     * Parse an input source and read all isolines that it contains.
     *
     * @return All isolines parsed.
     * @throws IOException if the reader can't be created.
     */
    protected Isoline[] readAll() throws IOException
    {
        final GEBCOReader reader = new GEBCOReader();
        reader.setInput(toSourceURL(name));
        return reader.read();
    }
}
