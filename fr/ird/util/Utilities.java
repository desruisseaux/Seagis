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
package fr.ird.util;

// Miscellaneous
import java.io.File;
import java.util.Locale;
import java.nio.charset.Charset;
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
}
