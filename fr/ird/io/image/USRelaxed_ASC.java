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
package fr.ird.io.image;

// Miscellaneous
import java.util.Locale;
import java.nio.charset.Charset;
import org.geotools.io.image.TextRecordImageReader;


/**
 * Service provider interface for {@link TextRecordImageReader}
 * decoding gridded data (MIME type "text/x-grid-bathy").
 * <strong>(NOTE: MIME type is to be renamed)</strong>
 *
 * Locale is US and charset is ISO-LATIN-1. There is no pad value.
 */
public class USRelaxed_ASC extends TextRecordImageReader.Spi
{
    public USRelaxed_ASC()
    {
        super("bathy", "text/x-grid-bathy"); // TODO: rename x-grid-usr
        vendorName    = "Institut de Recherche pour le Développement";
        version       = "1.0";
        locale        = Locale.US;
        charset       = Charset.forName("ISO-8859-1"); // ISO Latin Alphabet No. 1 (ISO-LATIN-1)
        gridTolerance = 1E-3f;
    }

    public String getDescription(final Locale locale)
    {return "Images sur une grille à peu près régulière";}
}
