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
package fr.ird.sql.image;

// Image I/O
import java.io.IOException;
import java.awt.image.DataBuffer;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import net.seas.image.io.RawBinaryImageReader;
import net.seas.image.io.TextRecordImageReader;

// Miscellaneous
import java.util.Locale;
import java.nio.charset.Charset;


/**
 * Register a set of custom image reader service providers.
 * Those image format appear in some database's entries.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Codecs
{
    /**
     * Register service providers.
     */
    public static void register()
    {
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new CLS());
        registry.registerServiceProvider(new Bathy());
        registry.registerServiceProvider(new SSTCanarias());
    }

    /**
     * Service provider interface (SPI) for {@link TextRecordImageReader}
     * using CLS charset and locale. This SPI is named "CLS" and has MIME
     * type "text/x-grid-CLS". Pad value is set to 9999.
     */
    private static final class CLS extends TextRecordImageReader.Spi
    {
        public CLS()
        {
            super("CLS", "text/x-grid-CLS");
            vendorName = "Institut de Recherche pour le Développement";
            version    = "1.0";
            locale     = Locale.US;
            charset    = Charset.forName("ISO-8859-1"); // ISO Latin Alphabet No. 1 (ISO-LATIN-1)
            padValue   = 9999;
        }

        public String getDescription(final Locale locale)
        {return "Décodeur d'images SLA de CLS";}
    }

    /**
     * Service provider interface (SPI) for {@link TextRecordImageReader}
     * using CLS charset and locale. This SPI is named "bathy" and has MIME
     * type "text/x-grid-bathy". Pad value is set to 9999.
     */
    private static final class Bathy extends TextRecordImageReader.Spi
    {
        public Bathy()
        {
            super("bathy", "text/x-grid-bathy");
            vendorName    = "Institut de Recherche pour le Développement";
            version       = "1.0";
            locale        = Locale.US;
            charset       = Charset.forName("ISO-8859-1"); // ISO Latin Alphabet No. 1 (ISO-LATIN-1)
            gridTolerance = 1E-3f;
        }

        public String getDescription(final Locale locale)
        {return "Décodeur d'images bathymétrique de Baudry et Sandwell";}
    }

    /**
     * Service provider interface (SPI) for {@link RawBinaryImageReader}
     * using Canarias pad values. This SPI is named "SST-Canarias" and
     * has MIME type "image/raw-canarias".
     */
    private static final class SSTCanarias extends RawBinaryImageReader.Spi
    {
        public SSTCanarias()
        {
            super("SST-Canarias", "image/raw-canarias");
            vendorName = "Institut de Recherche pour le Développement";
            version    = "1.0";
            padValue   = 0;
            dataType   = DataBuffer.TYPE_FLOAT;
            pluginClassName = "fr.ird.image.io.SSTCanariasImageReaderSpi$Decoder";
        }

        public String getDescription(final Locale locale)
        {return "Décodeur d'images SST de la station des Canaries";}

        public ImageReader createReaderInstance() throws IOException
        {return new Reader(this);}
    }

    /**
     * The image reader for {@link SSTCanarias}.
     */
    private static final class Reader extends RawBinaryImageReader
    {
        public Reader(final ImageReaderSpi provider)
        {super(provider);}

        protected double transform(final double value)
        {
            if (value<=00) return Double.NaN;
            if (value>=99) return Double.NaN;
            return value;
        }
    }
}
