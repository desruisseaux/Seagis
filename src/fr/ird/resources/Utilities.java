/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.resources;

// J2SE dependencies
import java.io.File;
import java.util.Locale;
import java.util.Iterator;
import java.io.IOException;
import java.nio.charset.Charset;
import java.awt.image.RenderedImage;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.IIOException;
import javax.imageio.ImageWriter;
import javax.imageio.ImageIO;

// Geotools dependencies
import org.geotools.io.image.PaletteFactory;


/**
 * A set of utilities related to images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Utilities {
    /**
     * The palette factory.
     */
    private static PaletteFactory factory;

    /**
     * Do not allows instantiation of this class.
     */
    private Utilities() {
    }

    /**
     * Gets the default palette factory.
     */
    public static synchronized PaletteFactory getPaletteFactory() {
        if (factory == null) {
            factory = new PaletteFactory(
            /* parent factory */ null,
            /* class loader   */ PaletteFactory.class.getClassLoader(),
            /* root directory */ new File("application-data/colors"),
            /* extension      */ ".pal",
            /* character set  */ Charset.forName("ISO-8859-1"),
            /* locale         */ Locale.US);
        }
        return factory;
    }

    /**
     * Save an image in a file of the given name. The file format will be infered from
     * the filename extension. If no extension were provided, default to PNG.
     *
     * @param  image The image to save.
     * @param  filename The destination filename.
     * @throws IOException if an error occured during I/O.
     */
    public static void save(final RenderedImage image, String filename) throws IOException {
        int dot = filename.lastIndexOf('.');
        String extension;
        if (dot >= 0) {
            extension = filename.substring(dot+1);
        } else {
            dot = filename.length();
            extension = "png";
            filename += ".png";
        }
        while (true) {
            final Iterator it = ImageIO.getImageWritersBySuffix(extension);
            if (it!=null && it.hasNext()) {
                final ImageWriter writer = (ImageWriter) it.next();
                final ImageOutputStream output = ImageIO.createImageOutputStream(new File(filename));
                writer.setOutput(output);
                writer.write(image);
                writer.dispose();
                output.close();
                return;
            }
            if (extension.equalsIgnoreCase("png")) {
                throw new IIOException("Can't find an encoder");
            }
            extension = "png";
            filename  = filename.substring(0, dot) + ".png";
        }
    }
}
