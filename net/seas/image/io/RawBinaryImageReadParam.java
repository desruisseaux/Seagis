/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.image.io;

// Miscellaneous
import java.awt.Dimension;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import javax.imageio.ImageReadParam;


/**
 * A class describing how a raw binary stream is to be decoded. In the context of
 * {@link RawBinaryImageReader}, the stream may not contains enough information
 * for an optimal decoding. For example the stream may not contains image's
 * width and height. The <code>RawBinaryImageReadParam</code> gives a chance
 * to specify those missing informations.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class RawBinaryImageReadParam extends ImageReadParam
{
    /**
     * The expected image model, or <code>null</code> if unknow.
     */
    private SampleModel model;

    /**
     * The expected image size, or <code>null</code> if unknow.
     */
    private Dimension size;

    /**
     * The expected data type, or {@link DataBuffer#TYPE_UNDEFINED} if unknow.
     */
    private int dataType = DataBuffer.TYPE_UNDEFINED;

    /**
     * The pad value, or {@link Double#NaN} if there is none.
     */
    private double padValue = Double.NaN;

    /**
     * Construct a new <code>RawBinaryImageReadParam</code>
     * with default parameters.
     */
    public RawBinaryImageReadParam()
    {}

    /**
     * Specify the image size in the input stream. Setting the size to <code>null</code>
     * reset the default size, which is reader dependent. Most readers will thrown an
     * exception at reading time if the image size is unspecified.
     *
     * @param size The expected image size, or
     *             <code>null</code> if unknow.
     */
    public void setStreamImageSize(final Dimension size)
    {this.size = (size!=null) ? new Dimension(size.width, size.height) : null;}

    /**
     * Returns the image size in the input stream, or <code>null</code> if unknow.
     * Image size is specified by the last call to {@link #setImageSize} or
     * {@link #setStreamSampleModel}.
     */
    public Dimension getStreamImageSize()
    {return (size!=null) ? (Dimension) size.clone() : null;}

    /**
     * Specify the data type in input stream. Setting data type to
     * {@link DataType#TYPE_UNDEFINED} reset the default value, which
     * is reader dependent.
     *
     * @param dataType The data type, or {@link DataType#TYPE_UNDEFINED} if unknow.
     *        Know data type should be a constant from {@link DataBuffer}. Common
     *        types are {@link DataBuffer#TYPE_INT}, {@link DataBuffer#TYPE_FLOAT}
     *        and {@link DataBuffer#TYPE_DOUBLE}.
     */
    public void setStreamDataType(final int dataType)
    {
        if ((dataType>=DataBuffer.TYPE_BYTE && dataType<=DataBuffer.TYPE_DOUBLE) || dataType==DataBuffer.TYPE_UNDEFINED)
        {
            this.dataType = dataType;
        }
        else throw new IllegalArgumentException(String.valueOf(dataType));
    }

    /**
     * Returns the data type in input stream, or {@link DataBuffer#TYPE_UNDEFINED}
     * if unknow. Data type is specified by the last call to {@link #setDataType}
     * or {@link #setStreamSampleModel}.
     */
    public int getStreamDataType()
    {return dataType;}

    /**
     * Set the pad value.
     *
     * @param padValue The pad value, or {@link Double#NaN} if there is none.
     */
    public void setPadValue(final double padValue)
    {this.padValue = padValue;}

    /**
     * Returns the pad value, or {@link Double#NaN} if there is none
     */
    public double getPadValue()
    {return padValue;}

    /**
     * Set a sample model indicating the data layout in the input stream.
     * Indications comprise image size and data type, i.e. calling this
     * method with a non-null value is equivalent to calling also the
     * following methods:
     *
     * <blockquote><pre>
     * setStreamImageSize(model.getWidth(), model.getHeight());
     * setDataType(model.getDataType());
     * </pre></blockquote>
     *
     * Setting the sample model to <code>null</code> reset
     * the default model, which is reader dependent.
     */
    public void setStreamSampleModel(final SampleModel model)
    {
        this.model = model;
        if (model!=null)
        {
            size = new Dimension(model.getWidth(), model.getHeight());
            dataType = model.getDataType();
        }
    }

    /**
     * Returns a sample model indicating the data layout in the input stream.
     * The {@link SampleModel}'s width and height should matches the image
     * size in the input stream.
     *
     * @return A sample model indicating the data layout in the input stream,
     *         or <code>null</code> if unknow.
     */
    public SampleModel getStreamSampleModel()
    {return model=getStreamSampleModel(null);}

    /**
     * Returns a sample model indicating the data layout in the input stream.
     * The {@link SampleModel}'s width and height should matches the image
     * size in the input stream.
     *
     * @param  defaultSampleModel A default sample model, or <code>null</code>
     *         if there is no default. If this <code>RawBinaryImageReadParam</code>
     *         contains unspecified sample model, image size or data type, values
     *         from <code>defaultSampleModel</code> will be used.
     * @return A sample model indicating the data layout in the input stream,
     *         or <code>null</code> if unknow.
     */
    final SampleModel getStreamSampleModel(final SampleModel defaultSampleModel)
    {
        SampleModel model = this.model;
        Dimension    size = this.size;
        int      dataType = this.dataType;
        if (defaultSampleModel!=null)
        {
            if (model==null) model = defaultSampleModel;
            if (size ==null) size  = new Dimension(defaultSampleModel.getWidth(), defaultSampleModel.getHeight());
            if (dataType==DataBuffer.TYPE_UNDEFINED) dataType = defaultSampleModel.getDataType();
        }
        if (model==null || size==null || dataType==DataBuffer.TYPE_UNDEFINED)
        {
            return null;
        }
        final int width  = size.width;
        final int height = size.height;
        if (dataType != model.getDataType())
        {
            final int numBands = model.getNumBands();
            if (model instanceof ComponentSampleModel)
            {
                final ComponentSampleModel cast = (ComponentSampleModel) model;
                final int   pixelStride    = cast.getPixelStride();
                final int   scanlineStride = cast.getScanlineStride();
                final int[] bankIndices    = cast.getBankIndices();
                final int[] bandOffsets    = cast.getBandOffsets();
                if (model instanceof BandedSampleModel)
                {
                    model = new BandedSampleModel(dataType, width, height, scanlineStride, bankIndices, bandOffsets);
                }
                else if (model instanceof PixelInterleavedSampleModel)
                {
                    model = new PixelInterleavedSampleModel(dataType, width, height, pixelStride, scanlineStride, bandOffsets);
                }
                else
                {
                    model = new ComponentSampleModel(dataType, width, height, pixelStride, scanlineStride, bankIndices, bandOffsets);
                }
            }
            else if (model instanceof MultiPixelPackedSampleModel)
            {
                final MultiPixelPackedSampleModel cast = (MultiPixelPackedSampleModel) model;
                final int numberOfBits   = DataBuffer.getDataTypeSize(dataType);
                final int scanlineStride = cast.getScanlineStride();
                final int dataBitOffset  = cast.getDataBitOffset();
                model = new MultiPixelPackedSampleModel(dataType, width, height, numberOfBits, scanlineStride, dataBitOffset);
            }
            else if (model instanceof SinglePixelPackedSampleModel)
            {
                final SinglePixelPackedSampleModel cast = (SinglePixelPackedSampleModel) model;
                final int   scanlineStride = cast.getScanlineStride();
                final int[] bitMasks       = cast.getBitMasks();
                model = new SinglePixelPackedSampleModel(dataType, width, height, scanlineStride, bitMasks);
            }
            else throw new IllegalStateException(model.getClass().getName());
        }
        if (model.getWidth()!=width || model.getHeight()!=height)
        {
            model = model.createCompatibleSampleModel(width, height);
        }
        return model;
    }
}
