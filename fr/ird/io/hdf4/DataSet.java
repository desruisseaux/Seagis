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
package fr.ird.io.hdf4;

// Images
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;

// Miscellaneous
import org.geotools.resources.Utilities;


/**
 * A set of data from an HDF file. <code>DataSet</code>
 * are constructed by {@link Parser#getDataSet}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class DataSet {
    /**
     * The default index color model. Will
     * be constructed only if needed.
     */
    private static IndexColorModel defaultColors;

    /**
     * Name for this data set.
     */
    private final String name;

    /**
     * The matrix dimension.
     */
    private final int dimension;

    /**
     * Dimensions for the underlying matrix.
     * @see #sizeEquals
     */
    private final int size0, size1, size2, size3, size4, size5;

    /**
     * Conversion factors. Integers will be converted in real
     * values with <code>integer * scale - offset</code>.
     */
    private final double scale, offset;

    /**
     * Object to use for checking data quality,
     * or <code>null</code> if none.
     */
    private final QualityCheck qualityCheck;

    /**
     * Construct a data set.
     *
     * @param name   The data set name.
     * @param size   The matrix size.
     * @param length The number of elements (for checking purpose).
     * @param scale  The scale factor.
     * @param offset The offset constant.
     * @param qualityCheck Object to use for checking data quality, or <code>null</code> if none.
     */
    DataSet(final String name, final int[] size, final int length,
            final double scale, final double offset, final QualityCheck qualityCheck)
    {
        int size0=1,size1=1,size2=1,size3=1,size4=1,size5=1;
        dimension = size.length;
        switch (dimension) {
            default: throw new IllegalArgumentException(dimension+"D array not implemented");
            case 6:  size5 = size[5]; // fall through
            case 5:  size4 = size[4]; // fall through
            case 4:  size3 = size[3]; // fall through
            case 3:  size2 = size[2]; // fall through
            case 2:  size1 = size[1]; // fall through
            case 1:  size0 = size[0]; // fall through
            case 0:                   // fall through
        }
        this.name         = name;
        this.size0        = size0;
        this.size1        = size1;
        this.size2        = size2;
        this.size3        = size3;
        this.size4        = size4;
        this.size5        = size5;
        this.scale        = scale;
        this.offset       = offset;
        this.qualityCheck = qualityCheck;
        if (getNumElements() != length) {
            throw new IllegalArgumentException();
        }
        if (qualityCheck!=null && !qualityCheck.sizeEquals(this)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the matrix dimension.
     */
    public final int getDimension() {
        return dimension;
    }

    /**
     * Returns the number of elements in this data set.
     */
    public final int getNumElements() {
        return size0*size1*size2*size3*size4*size5;
    }

    /**
     * Returns the number of elements
     * in the specified dimension.
     */
    public final int getLength(final int dim) {
        switch (dim) {
            case 0:  return size0;
            case 1:  return size1;
            case 2:  return size2;
            case 3:  return size3;
            case 4:  return size4;
            case 5:  return size5;
            default: throw new IllegalArgumentException(String.valueOf(dim));
        }
    }

    /**
     * Returns the number of line in this data set.
     * This is equivalent to <code>getLength(0)</code>.
     */
    public final int getRowCount() {
        return size0;
    }

    /**
     * Returns the number of line in this data set.
     * This is equivalent to <code>getLength(1)</code>.
     */
    public final int getColumnCount() {
        return size1;
    }

    /**
     * Returns the integer value at the specified index.
     */
    abstract int getInteger(final int index);

    /**
     * Returns the value at the specified index.
     */
    public final double getFlat(final int index) {
        return (qualityCheck==null || qualityCheck.acceptIndex(index)) ?
                getInteger(index)*scale-offset : Double.NaN;
    }

    /**
     * Returns the value at the specified row and column.
     * This method should be invoked only for 1 dimensional
     * matrix.  For performance raison, this is not checked
     * except if assertions are enabled.
     */
    public final double get(final int index) {
        assert dimension == 1            : dimension;
        assert  index>=0 &&  index<size0 : index;
        return getFlat(index);
    }

    /**
     * Returns the value at the specified row and column.
     * This method should be invoked only for 2 dimensional
     * matrix.  For performance raison, this is not checked
     * except if assertions are enabled.
     */
    public final double get(final int row, final int column) {
        assert dimension == 2            : dimension;
        assert    row>=0 &&    row<size0 : row;
        assert column>=0 && column<size1 : column;
        return getFlat(row*size1 + column);
    }

    /**
     * Returns the value at the specified index.
     * This method should be invoked only for 3 dimensional
     * matrix.  For performance raison, this is not checked
     * except if assertions are enabled.
     */
    public final double get(final int row, final int column, final int plan) {
        assert dimension == 3            : dimension;
        assert    row>=0 &&    row<size0 : row;
        assert column>=0 && column<size1 : column;
        assert plan  >=0 &&   plan<size2 : plan;
        return getFlat((row*size1 + column)*size2 + plan);
    }

    /**
     * Returns the value at the specified index.
     * This method should be invoked only for 4 dimensional
     * matrix.  For performance raison, this is not checked
     * except if assertions are enabled.
     */
    public final double get(final int i1, final int i2, final int i3, final int i4) {
        assert dimension == 4    : dimension;
        assert i1>=0 && i1<size0 : i1;
        assert i2>=0 && i2<size1 : i2;
        assert i3>=0 && i3<size2 : i3;
        assert i4>=0 && i4<size3 : i4;
        return getFlat(((i1*size1 + i2)*size2 + i3)*size3 + i4);
    }

    /**
     * Returns the value at the specified index.
     * This method should be invoked only for 5 dimensional
     * matrix.  For performance raison, this is not checked
     * except if assertions are enabled.
     */
    public final double get(final int i1, final int i2, final int i3, final int i4, final int i5) {
        assert dimension == 5    : dimension;
        assert i1>=0 && i1<size0 : i1;
        assert i2>=0 && i2<size1 : i2;
        assert i3>=0 && i3<size2 : i3;
        assert i4>=0 && i4<size3 : i4;
        assert i5>=0 && i5<size4 : i5;
        return getFlat((((i1*size1 + i2)*size2 + i3)*size3 + i4)*size4 + i5);
    }

    /**
     * Returns the value at the specified index.
     * This method should be invoked only for 6 dimensional
     * matrix.  For performance raison, this is not checked
     * except if assertions are enabled.
     */
    public final double get(final int i1, final int i2, final int i3, final int i4, final int i5, final int i6) {
        assert dimension == 6    : dimension;
        assert i1>=0 && i1<size0 : i1;
        assert i2>=0 && i2<size1 : i2;
        assert i3>=0 && i3<size2 : i3;
        assert i4>=0 && i4<size3 : i4;
        assert i5>=0 && i5<size4 : i5;
        assert i6>=0 && i6<size5 : i6;
        return getFlat(((((i1*size1 + i2)*size2 + i3)*size3 + i4)*size4 + i5)*size5 + i6);
    }

    /**
     * Make sure this <code>DataSet</code> has at least 2 dimensions.
     */
    private final void ensure2D() {
        if (dimension < 2) {
            throw new IllegalStateException("Not a 2D matrix");
        }
    }

    /**
     * Retourne les données de la plage <code>[minimum...maximum]</code>
     * sous forme d'une image. L'image utilisera la palette de couleurs
     * spécifiée. L'index 0 sera utilisée pour les données manquantes,
     * et les autres (jusqu'à <code>colors.getMapSize()</code>) pour les
     * valeurs.
     *
     * @param colors  Table des couleurs à utiliser.
     * @param minimum Valeur minimale des données à représenter (inclusivement).
     * @param maximum Valeur maximale des données à représenter (inclusivement).
     */
    public BufferedImage getImage(final IndexColorModel colors, double minimum, final double maximum) {
        ensure2D();
        final int     minPixelValue = 1;
        final int     maxPixelValue = colors.getMapSize() - 1;
        final double          scale = (maxPixelValue-minPixelValue)/(maximum-minimum);
        final BufferedImage   image = new BufferedImage(size1, size0, BufferedImage.TYPE_BYTE_INDEXED, colors);
        final WritableRaster raster = image.getRaster();
        minimum -= minPixelValue/scale;
        int index = getNumElements();
        final int step = index / (size0*size1);
        for (int y=size0; --y>=0;) {
            for (int x=size1; --x>=0;) {
                final int    pixel;
                final double value = (getFlat(index-=step)-minimum)*scale;
                     if (value < minPixelValue) pixel=minPixelValue;
                else if (value > maxPixelValue) pixel=maxPixelValue;
                else pixel=(int)Math.rint(value); // Les NaN prendront la valeur 0.
                raster.setSample(x, y, 0, pixel);
            }
        }
        assert index==0;
        return image;
    }

    /**
     * Retourne les données sous forme d'une image. L'image utilisera la palette
     * de couleurs spécifiée. L'index 0 sera utilisée pour les données manquantes,
     * et les autres (jusqu'à <code>colors.getMapSize()</code>) pour les valeurs.
     *
     * @param colors Table des couleurs à utiliser.
     */
    public BufferedImage getImage(final IndexColorModel colors) {
        ensure2D();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int  index = getNumElements();
        final int step = index / (size0*size1);
        while ((index-=step) >= 0) {
            final double value = getFlat(index);
            if (value > max) max=value;
            if (value < min) min=value;
        }
        return getImage(colors, min, max);
    }

    /**
     * Retourne les données sous forme d'une image. L'image utilisera une palette
     * de couleurs par défaut. L'index 0 sera utilisée pour les données manquantes,
     * et les autres (jusqu'à <code>colors.getMapSize()</code>) pour les valeurs.
     */
    public BufferedImage getImage() {
        synchronized (DataSet.class) {
            if (defaultColors == null) {
                final byte[] RGB = new byte[256];
                for (int i=0; i<RGB.length; i++) {
                    RGB[i] = (byte) i;
                }
                defaultColors = new IndexColorModel(8, RGB.length, RGB, RGB, RGB, 0);
            }
            return getImage(defaultColors);
        }
    }

    /**
     * Indique si deux objets {@link DataSet} ont les mêmes dimensions.
     */
    public boolean sizeEquals(final DataSet that) {
        return this.size0==that.size0 &&
               this.size1==that.size1 &&
               this.size2==that.size2 &&
               this.size3==that.size3 &&
               this.size4==that.size4 &&
               this.size5==that.size5;
    }

    /**
     * Retourne une représentation
     * de cet ensemble de données.
     */
    public String toString() {
        final StringBuffer buffer=new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(name);
        buffer.append("\", ");
        buffer.append(size0);
        buffer.append('\u00D7');
        buffer.append(size1);
        buffer.append(", scale=");
        buffer.append(scale);
        buffer.append(", offset=");
        buffer.append(offset);
        buffer.append(']');

        double min  = Double.POSITIVE_INFINITY;
        double max  = Double.NEGATIVE_INFINITY;
        double sum  = 0;
        double sum2 = 0;
        int    n    = 0;
        for (int i=getNumElements(); --i>=0;) {
            final double value = getFlat(i);
            if (!Double.isNaN(value)) {
                if (value > max) max=value;
                if (value < min) min=value;
                sum  += value;
                sum2 += value*value;
                n++;
            }
        }
        if (n!=0) {
            buffer.append("\n  Minimum = "); buffer.append(min);
            buffer.append("\n  Maximum = "); buffer.append(max);
            buffer.append("\n  RMS     = "); buffer.append(Math.sqrt(sum2/n));
            buffer.append("\n  Mean    = "); buffer.append(sum/n);
            buffer.append("\n  Std-dev = "); buffer.append(Math.sqrt((sum2 - sum*sum/n)/n));
        }
        buffer.append('\n');
        return buffer.toString();
    }

    /**
     * Implémentation de {@link DataSet} pour les entiers 8-bits signés.
     */
    static final class Byte extends DataSet {
        private final byte[] data;

        Byte(final String name, final int[] size, final byte[] data,
             final double scale, final double offset, final QualityCheck qualityCheck)
        {
            super(name, size, data.length, scale, offset, qualityCheck); this.data=data;
        }

        protected int getInteger(final int index) {
            return data[index];
        }
    }

    /**
     * Implémentation de {@link DataSet} pour les entiers 16-bits signés.
     */
    static final class Short extends DataSet {
        private final short[] data;

        Short(final String name, final int[] size, final short[] data,
              final double scale, final double offset, final QualityCheck qualityCheck)
        {
            super(name, size, data.length, scale, offset, qualityCheck); this.data=data;
        }

        protected int getInteger(final int index) {
            return data[index];
        }
    }

    /**
     * Implémentation de {@link DataSet} pour les entiers 16-bits non-signés.
     */
    static final class UShort extends DataSet {
        private final short[] data;

        UShort(final String name, final int[] size, final short[] data,
               final double scale, final double offset, final QualityCheck qualityCheck)
        {
            super(name, size, data.length, scale, offset, qualityCheck); this.data=data;
        }

        protected int getInteger(final int index) {
            return data[index] & 0xFFFF;
        }
    }
}
