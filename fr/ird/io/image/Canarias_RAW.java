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
import java.io.IOException;
import java.awt.image.DataBuffer;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import org.geotools.io.image.RawBinaryImageReader;


/**
 * Service provider interface for {@link RawBinaryImageReader}
 * decoding RAW images from Canarias (MIME type "image/raw-canarias").
 *
 * Data type is <code>float</code> and pad values are 0 and all values
 * equals or greater than 99.
 */
public class Canarias_RAW extends RawBinaryImageReader.Spi
{
    public Canarias_RAW()
    {
        super("RAW-Canarias", "image/raw-canarias");
        vendorName = "Institut de Recherche pour le D�veloppement";
        version    = "1.0";
        padValue   = 0;
        dataType   = DataBuffer.TYPE_FLOAT;
        pluginClassName = "fr.ird.io.image.Canarias_RAW$Reader";
    }

    public String getDescription(final Locale locale)
    {return "Images RAW de la station des Canaries";}

    public ImageReader createReaderInstance() throws IOException
    {return new Reader(this);}

    /**
     * The image reader for {@link Canarias_RAW}.
     */
    private static final class Reader extends RawBinaryImageReader
    {
        public Reader(final ImageReaderSpi provider)
        {super(provider);}

        protected double transform(final double value)
        {
            if (value<=00.0) return LAND;
            if (value>=99.0) return NODATA;
            if (value<=0.01) return NODATA;
            // Note: Can't apply the following patch on SeaWifs images.
//            if (value>=25.5) return Double.NaN; // TODO: temporary patch.
            return value;
        }
    }

    /**
     * Copie de {@link org.geotools.cv.Category#toNaN}.
     */
    private static float toNaN(int index) throws IndexOutOfBoundsException {
        index += 0x200000;
        if (index>=0 && index<=0x3FFFFF) {
            final float value = Float.intBitsToFloat(0x7FC00000 + index);
            assert Float.isNaN(value);
            return value;
        }
        else {
            throw new IndexOutOfBoundsException(Integer.toHexString(index));
        }
    }

    private static final double LAND   = toNaN(255);
    private static final double NODATA = toNaN(  0);

    public static void main(String[] args) throws Exception {
        args = new String[] {
            "-group",   "2041402270",
            "-sources", "D:/Pouponni�re/M�diterran�e/Chlorophylle/chl_020819.txt"
        };
        fr.ird.io.coverage.Processor.main(args);
    }
}
