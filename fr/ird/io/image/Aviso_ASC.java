/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.io.image;

// Miscellaneous
import java.util.Locale;
import java.nio.charset.Charset;
import org.geotools.io.image.TextRecordImageReader;


/**
 * Service provider interface for {@link TextRecordImageReader}
 * decoding Aviso's gridded SLA files (MIME type "text/x-grid-CLS").
 * <strong>(NOTE: MIME type is to be renamed)</strong>
 *
 * Locale is US, charset is ISO-LATIN-1 and pad value is 9999.
 */
public class Aviso_ASC extends TextRecordImageReader.Spi {
    public Aviso_ASC() {
        super("Aviso", "text/x-grid-CLS"); // TODO: rename x-grid-Aviso
        vendorName = "Institut de Recherche pour le D�veloppement";
        version    = "1.0";
        locale     = Locale.US;
        charset    = Charset.forName("ISO-8859-1"); // ISO Latin Alphabet No. 1 (ISO-LATIN-1)
        padValue   = 9999;
    }

    public String getDescription(final Locale locale) {
        return "Donn�es sur une grille par Aviso";
    }
}
