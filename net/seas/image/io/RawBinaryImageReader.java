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

// Input/output
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

// Images
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import javax.media.jai.DataBufferFloat;  // JAI buffer
import javax.media.jai.DataBufferDouble; // JAI buffer

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.ComponentColorModel;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;

// Miscellaneous
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

// Miscellaneous
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.media.jai.util.Range;
import net.seas.resources.Resources;


/**
 * Image reader for raw binary files. This reader can't decode a stream
 * without some extra informations (image size, data type...).
 *
 * <strong>Image size</strong> can be specified in three ways:
 *
 * <ul>
 *   <li>Call {@link RawBinaryImageReadParam#setStreamImageSize} with a non-null argument.</li>
 *   <li>Call {@link RawBinaryImageReader.Spi#createReaderInstance(Object)} with an argument of
 *       type {@link Dimension}.</li>
 *   <li>Define a <code>RawBinaryImageReader</code> or <code>RawBinaryImageReader.Spi</code>
 *       subclass setting the <code>imageSize</code> field.</li>
 * </ul>
 *
 * <strong>Data type</strong> can be specified in two ways:
 *
 * <ul>
 *   <li>Call {@link RawBinaryImageReadParam#setDataType}.<.li>
 *   <li>Define a <code>RawBinaryImageReader</code> subclass
 *       overriding {@link #getRawDataType}.</li>
 * </ul>
 *
 * <strong>Pad value</strong> can be specified in three ways:
 *
 * <ul>
 *   <li>Call {@link RawBinaryImageReadParam#setPadValue}.</li>
 *   <li>Define a <code>RawBinaryImageReader</code> subclass
 *       overriding {@link #isPadValue}.</li>
 *   <li>Define a <code>RawBinaryImageReader.Spi</code>
 *       subclass setting the <code>padValue</code> field.</li>
 * </ul>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class RawBinaryImageReader extends SimpleImageReader
{
    /**
     * Valeurs minimales et maximales des données lues dans chaque
     * bandes, ou <code>null</code> si ces valeurs n'ont pas encore
     * été déterminées.
     */
    private Range[] ranges;

    /**
     * The pad value. If a single pad value is not enough, more
     * control can be gained by overriding {@link #isPädValue}.
     */
    private final long padValue;

    /**
     * The expected image size, or <code>null</code> if unknow.   Setting this field to a
     * non-null value allow {@link #getWidth} and {@link #getHeight} to returns this size.
     * This size will be compared with {@link RawBinaryImageReadParam#getStreamImageSize}
     * at reading time, if such a parameter is specified. An {@link IIOException}
     * exception will be thrown if sizes don't match, in order to ensure consistency
     * with <code>getWidth(int)</code> and <code>getHeight(int)</code> methods.
     */
    protected Dimension imageSize;

    /**
     * Construct a new image reader.
     *
     * @param provider the {@link ImageReaderSpi} that is
     *                 invoking this constructor, or null.
     */
    public RawBinaryImageReader(final ImageReaderSpi provider)
    {
        super(provider);
        if (provider instanceof Spi)
        {
            final Spi spi = (Spi) provider;
            padValue  = Double.doubleToLongBits(spi.padValue);
            imageSize = spi.imageSize;
        }
        else
        {
            padValue  = Double.doubleToLongBits(Double.NaN);
            imageSize = null;
        }
    }

    /**
     * Efface les information qui étaient conservées en mémoire.
     */
    private void clear()
    {
        ranges = null;
    }

    /**
     * Restores the image reader to its initial state.
     */
    public void reset()
    {
        clear();
        super.reset();
    }

    /**
     * Sets the input source to use.
     */
    public void setInput(final Object input, final boolean seekForwardOnly, final boolean ignoreMetadata)
    {
        clear();
        super.setInput(input, seekForwardOnly, ignoreMetadata);
    }

    /**
     * Convenience method returning the destination band for the
     * specified source band. If the specified source band is not
     * to be read, then this method returns -1.
     */
    private static int sourceToDestBand(final int[] sourceBands, final int[] destinationBands, final int srcBand)
    {
        if (sourceBands==null)
            return (destinationBands!=null) ? destinationBands[srcBand] : srcBand;
        for (int i=0; i<sourceBands.length; i++)
            if (sourceBands[i] == srcBand)
                return (destinationBands!=null) ? destinationBands[i] : i;
        return -1;
    }

    /**
     * Returns a default color space.
     */
    private ColorSpace getColorSpace(final int imageIndex, final int[] sourceBands, final int[] destinationBands) throws IOException
    {
        // Search for the source band which is to be
        // written into the first destination band.
        int numBands=getNumBands(imageIndex);
        int firstVisibleSourceBand=0;
        if (destinationBands!=null)
        {
            numBands = destinationBands.length;
            for (int i=1; i<destinationBands.length; i++)
                if (destinationBands[i] < destinationBands[firstVisibleSourceBand])
                    firstVisibleSourceBand = i;
        }
        if (sourceBands!=null)
        {
            numBands = sourceBands.length;
            firstVisibleSourceBand = sourceBands[firstVisibleSourceBand];
        }
        return getColorSpace(imageIndex, firstVisibleSourceBand, numBands);
    }

    /**
     * Retourne quelques types d'images qui pourront contenir les données.
     * Le premier objet retourné sera celui qui convient le mieux pour un
     * affichage à l'écran.
     *
     * @param  imageIndex Index de l'image dont on veut les types.
     * @return Itérateur balayant les types de l'image.
     * @throws IndexOutOfBoundsException si <code>imageIndex</code> est invalide.
     * @throws IllegalStateException si aucune source n'a été spécifiée avec {@link #setInput}.
     * @throws IIOException si l'opération a échoué pour une autre raison.
     */
    private Iterator getImageTypes(final int imageIndex, final ColorSpace colorSpace, final SampleModel sampleModel) throws IOException
    {
        final List<ImageTypeSpecifier> list = new ArrayList<ImageTypeSpecifier>();
        for (final Iterator it=getImageTypes(imageIndex); it.hasNext();)
        {
            list.add((ImageTypeSpecifier) it.next());
        }
        // Add an image type closely matching
        // the stream image data type.
        final ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, Transparency.OPAQUE, sampleModel.getDataType());
        list.add(0, new ImageTypeSpecifier(colorModel, sampleModel));
        return list.iterator();
    }

    /**
     * Returns the data type which most closely represents the "raw"
     * internal data of the image. It should be a constant from
     * {@link DataBuffer}. Common types are {@link DataBuffer#TYPE_INT},
     * {@link DataBuffer#TYPE_FLOAT} and {@link DataBuffer#TYPE_DOUBLE}.
     *
     * @param  imageIndex The index of the image to be queried.
     * @return The data type.
     * @throws IOException If an error occurs reading the format information from the input source.
     */
    public int getRawDataType(final int imageIndex) throws IOException
    {
        if (originatingProvider instanceof Spi)
        {
            checkImageIndex(imageIndex);
            return ((Spi) originatingProvider).dataType;
        }
        else return super.getRawDataType(imageIndex);
    }

    /**
     * Returns the stream sample model (the sample model used to encode pixel into the stream).
     * The default implementation query {@link RawBinaryImageReadParam#getStreamSampleModel()}
     * if the supplied parameters is an instance of {@link RawBinaryImageReadParam}. Default
     * values are provided through the following methods: {@link #getNumBands}, {@link #getRawDataType},
     * {@link #getRawImageType} and {@link #getExpectedRange}.
     *
     * @param  imageIndex  The index of the image to be retrieved.
     * @param  param       Parameters used to control the reading process, or null.
     * @return The stream sample model.
     * @throws IOException  if an input operation failed.
     * @throws IIOException if <code>param</code> do not contains the expected information.
     */
    protected SampleModel getStreamSampleModel(final int imageIndex, final ImageReadParam param) throws IOException
    {
        /*
         * Get the expected image size. At least one
         * of the following image size must be set:
         *
         * {@link #imageSize} or
         * {@link RawBinaryImageReadParam#getStreamImageSize()}.
         *
         * If both are set, then their size must match.
         */
        Dimension streamImageSize = imageSize;
        RawBinaryImageReadParam rawParam = null;
        if (param instanceof RawBinaryImageReadParam)
        {
            rawParam = (RawBinaryImageReadParam) param;
            streamImageSize = rawParam.getStreamImageSize();
            if (streamImageSize == null)
            {
                streamImageSize = imageSize;
            }
        }
        if (streamImageSize == null)
        {
            throw new IIOException(Resources.format(Clé.UNSPECIFIED_IMAGE_SIZE));
        }
        if (imageSize!=null && !streamImageSize.equals(imageSize))
        {
            throw new IIOException(Resources.format(Clé.UNEXPECTED_IMAGE_SIZE));
        }
        /*
         * Get the stream sample model (i.e. the sample model used to encode pixel data
         * into the stream). Modify the sample model according user's parameters,
         * if parameters was specified.
         */
        SampleModel streamModel = getRawImageType(imageIndex).getSampleModel().createCompatibleSampleModel(streamImageSize.width, streamImageSize.height);
        if (rawParam!=null) streamModel = rawParam.getStreamSampleModel(streamModel);
        return streamModel;
    }

    /**
     * Returns a default <code>ImageReadParam</code>
     * object appropriate for this format.
     */
    public ImageReadParam getDefaultReadParam()
    {return new RawBinaryImageReadParam();}

    /**
     * Reads the image indexed by <code>imageIndex</code>.
     *
     * @param  imageIndex  The index of the image to be retrieved.
     * @param  param       Parameters used to control the reading process, or null.
     * @return The desired portion of the image.
     * @throws IOException if an input operation failed.
     */
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException
    {
        /*
         * Extract user's parameters.
         */
        checkImageIndex(imageIndex);
        final int[]      sourceBands;
        final int[] destinationBands;
        final int sourceXSubsampling;
        final int sourceYSubsampling;
        final int subsamplingXOffset;
        final int subsamplingYOffset;
        final int destinationXOffset;
        final int destinationYOffset;
        if (param != null)
        {
            sourceBands        = param.getSourceBands();
            destinationBands   = param.getDestinationBands();
            final Point offset = param.getDestinationOffset();
            sourceXSubsampling = param.getSourceXSubsampling();
            sourceYSubsampling = param.getSourceYSubsampling();
            subsamplingXOffset = param.getSubsamplingXOffset();
            subsamplingYOffset = param.getSubsamplingYOffset();
            destinationXOffset = offset.x;
            destinationYOffset = offset.y;
        }
        else
        {
            sourceBands        = null;
            destinationBands   = null;
            sourceXSubsampling = 1;
            sourceYSubsampling = 1;
            subsamplingXOffset = 0;
            subsamplingYOffset = 0;
            destinationXOffset = 0;
            destinationYOffset = 0;
        }
        /*
         * Get the stream model and the destination image.
         */
        final SampleModel   streamModel = getStreamSampleModel(imageIndex, param);
        final int           streamWidth = streamModel.getWidth();
        final int          streamHeight = streamModel.getHeight();
        final int           numSrcBands = streamModel.getNumBands();
        final BufferedImage       image = getDestination(param, getImageTypes(imageIndex, getColorSpace(imageIndex, sourceBands, destinationBands), streamModel), streamWidth, streamHeight);
        final SampleModel    imageModel = image.getSampleModel();
        final int           numDstBands = imageModel.getNumBands();
        checkReadParamBandSettings(param, numSrcBands, numDstBands);
        processImageStarted(imageIndex);
        /*
         * Compute region and check for possible optimization.
         */
        final Rectangle srcRegion = new Rectangle();
        final Rectangle dstRegion = new Rectangle();
        computeRegions(param, streamWidth, streamHeight, image, srcRegion, dstRegion);
        final int      dstXMin = dstRegion.x;
        final int      dstYMin = dstRegion.y;
        final int      dstXMax = dstRegion.width  + dstXMin;
        final int      dstYMax = dstRegion.height + dstYMin;
        final boolean isDirect = sourceXSubsampling==1 && sourceYSubsampling==1          &&
                                 subsamplingXOffset==0 && subsamplingYOffset==0          &&
                                 destinationXOffset==0 && destinationYOffset==0          &&
                                 srcRegion.x       ==0 && srcRegion.width ==streamWidth  &&
                                 srcRegion.y       ==0 && srcRegion.height==streamHeight &&
                                 imageModel.equals(streamModel);
        /*
         * Initialize a temporary raster. The temporary raster will hold
         * about 4 lines and use the stream's data model. Data will be
         * copied (by block of 4 lines) into the destination raster.
         */
        final ImageInputStream   input = (ImageInputStream) getInput();
        final WritableRaster dstRaster = image.getRaster();
        final WritableRaster srcRaster = isDirect ? dstRaster : WritableRaster.createWritableRaster(streamModel.createCompatibleSampleModel(streamWidth, Math.min(4, streamHeight)), null);
        final double          padValue = (param instanceof RawBinaryImageReadParam) ? ((RawBinaryImageReadParam) param).getPadValue() : Double.NaN;
        final DataBuffer        buffer = srcRaster.getDataBuffer();
        final int         bufferHeight = srcRaster.getHeight();
        final int             dataType = buffer.getDataType();
        final int               length = buffer.getSize();
        final int[]            offsets = buffer.getOffsets();
        final double[]         minimum = new double[numSrcBands];
        final double[]         maximum = new double[numSrcBands];
        Arrays.fill(minimum, Double.POSITIVE_INFINITY);
        Arrays.fill(maximum, Double.NEGATIVE_INFINITY);
        /*
         * Read all banks sequentially. Most sample models use only one bank (which doesn't mean
         * that their image have only one band).  The main exception is BandedSampleModem, which
         * use one separated bank for each band.
         */
        final long startStreamPosition  = input.getStreamPosition();
        final long expectedStreamLength = (long)length * offsets.length * (DataBuffer.getDataTypeSize(dataType)/8);
        for (int bank=0; bank<offsets.length; bank++)
        {
            /*
             * Note: It is difficult to know which bands contains a bank,
             *       since there is no API exposing the "bands <-> banks"
             *       mapping. As an heuristic rule, we assume that:
             *
             *       "Bands == banks" if the number of bands equals the
             *       number of banks (case of BandedSampleModel).
             *
             *       "One bank for all bands" if the number of banks is
             *       equals to one (case of most other SampleModel).
             */
            final int lowerSrcBand;
            final int upperSrcBand;
            if (offsets.length == numSrcBands)
            {
                lowerSrcBand = bank;
                upperSrcBand = bank+1;
            }
            else if (offsets.length == 1)
            {
                lowerSrcBand = 0;
                upperSrcBand = numSrcBands;
            }
            else
            {
                throw new IIOException("Unknow SampleModel");
            }
            /*
             * Continue reading...
             */
            final int offset = offsets[bank];
            for (int sy=0; sy<streamHeight; sy+=bufferHeight)
            {
                final int validHeight = Math.min(streamHeight-sy, bufferHeight);
                final int validLength = (int) (((long)length*validHeight) / bufferHeight);
                assert((((long)length*validLength) % bufferHeight)==0);
                switch (dataType)
                {
                    case DataBuffer.TYPE_BYTE:   input.readFully(((DataBufferByte)   buffer).getData(bank), offset, validLength); break;
                    case DataBuffer.TYPE_USHORT: input.readFully(((DataBufferUShort) buffer).getData(bank), offset, validLength); break;
                    case DataBuffer.TYPE_SHORT:  input.readFully(((DataBufferShort)  buffer).getData(bank), offset, validLength); break;
                    case DataBuffer.TYPE_INT:    input.readFully(((DataBufferInt)    buffer).getData(bank), offset, validLength); break;
                    case DataBuffer.TYPE_FLOAT:  input.readFully(((DataBufferFloat)  buffer).getData(bank), offset, validLength); break;
                    case DataBuffer.TYPE_DOUBLE: input.readFully(((DataBufferDouble) buffer).getData(bank), offset, validLength); break;
                    default: throw new IOException(Resources.format(Clé.UNSUPPORTED_DATA_TYPE));
                }
                /*
                 * Update progress.
                 */
                final long streamPosition = input.getStreamPosition();
                input.flushBefore(streamPosition);
                processImageProgress((streamPosition-startStreamPosition)*100f/expectedStreamLength);
                if (abortRequested())
                {
                    processReadAborted();
                    return image;
                }
                /*
                 * Update statistics and replace pad values.
                 */
                for (int srcBand=lowerSrcBand; srcBand<upperSrcBand; srcBand++)
                {
                    final int stop = offset+validLength;
                    for (int i=offset; i<stop; i++)
                    {
                        final double value = buffer.getElemDouble(srcBand, i);
                        if (value==padValue || !isPadValue(value))
                        {
                            if (value<minimum[srcBand]) minimum[srcBand]=value;
                            if (value>maximum[srcBand]) maximum[srcBand]=value;
                        }
                        else buffer.setElemDouble(srcBand, i, Double.NaN);
                    }
                }
                /*
                 * Copy pixel data.
                 */
                if (!isDirect)
                {
                    final int lowerY = Math.max(dstYMin, (            sy-subsamplingYOffset)/sourceYSubsampling+destinationYOffset);
                    final int upperY = Math.min(dstYMax, (validHeight+sy-subsamplingYOffset)/sourceYSubsampling+destinationYOffset);
                    for (int srcBand=lowerSrcBand; srcBand<upperSrcBand; srcBand++)
                    {
                        final int dstBand = sourceToDestBand(sourceBands, destinationBands, srcBand);
                        if (dstBand < 0) continue;
                        for (int y=lowerY; y<upperY; y++)
                        {
                            final int srcY = (y-destinationYOffset)*sourceYSubsampling + subsamplingYOffset;
                            assert(srcY>=sy && srcY<sy+validHeight);
                            for (int x=dstXMin; x<dstXMax; x++)
                            {
                                final int srcX = (x-destinationXOffset)*sourceXSubsampling + subsamplingXOffset;
                                assert(srcX>=0 && srcX<streamWidth);
                                final double value = srcRaster.getSampleDouble(srcX, srcY, srcBand);
                                dstRaster.setSample(x, y, dstBand, value);
                            }
                        }
                    }
                }
            }
        }
        /*
         * Store statistics.
         */
        processImageComplete();
        ranges = new Range[numSrcBands];
        for (int band=0; band<numSrcBands; band++)
        {
            final double min = minimum[band];
            final double max = maximum[band];
            if (min < max)
            {
                final Range range;
                switch (dataType)
                {
                    case DataBuffer.TYPE_BYTE:   // fall through (since TYPE_BYTE is unsigned, we need to use a wider type).
                    case DataBuffer.TYPE_SHORT:  range=new Range(  Short.class, new Short  ( (short)min), new Short  ( (short)max)); break;
                    case DataBuffer.TYPE_USHORT: // fall through (since TYPE_USHORT is unsigned, we need to use a wider type).
                    case DataBuffer.TYPE_INT:    range=new Range(Integer.class, new Integer(   (int)min), new Integer(   (int)max)); break;
                    case DataBuffer.TYPE_FLOAT:  range=new Range(  Float.class, new Float  ( (float)min), new Float  ( (float)max)); break;
                    case DataBuffer.TYPE_DOUBLE: range=new Range( Double.class, new Double ((double)min), new Double ((double)max)); break;
                    default: throw new IOException(Resources.format(Clé.UNSUPPORTED_DATA_TYPE));
                }
                ranges[band] = range;
            }
        }
        /*
         * Replace the color space.
         */
        if (dataType!=DataBuffer.TYPE_BYTE)
        {
            ColorModel finalColorModel = image.getColorModel();
            if (finalColorModel instanceof ComponentColorModel)
            {
                final ColorSpace oldColorSpace = finalColorModel.getColorSpace();
                final ColorSpace newColorSpace = getColorSpace(imageIndex, sourceBands, destinationBands);
                if (!oldColorSpace.equals(newColorSpace))
                {
                    final int[]                   bits = finalColorModel.getComponentSize();
                    final boolean             hasAlpha = finalColorModel.hasAlpha();
                    final boolean isAlphaPremultiplied = finalColorModel.isAlphaPremultiplied();
                    final int             transparency = finalColorModel.getTransparency();
                    final int            transfertType = finalColorModel.getTransferType();
                    finalColorModel = new ComponentColorModel(newColorSpace, bits, hasAlpha, isAlphaPremultiplied, transparency, transfertType);
                    return new BufferedImage(finalColorModel, image.getRaster(), image.isAlphaPremultiplied(), null);
                }
            }
        }
        return image;
    }

    /**
     * Returns the image's width.
     *
     * @throws IOException if an I/O error occured.
     * @throws IIOException if the image size is unknow.
     */
    public int getWidth(final int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        if (imageSize!=null) return imageSize.width;
        throw new IIOException(Resources.format(Clé.UNSPECIFIED_IMAGE_SIZE));
    }

    /**
     * Returns the image's height.
     *
     * @throws IOException if an I/O error occured.
     * @throws IIOException if the image size is unknow.
     */
    public int getHeight(final int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        if (imageSize!=null) return imageSize.height;
        throw new IIOException(Resources.format(Clé.UNSPECIFIED_IMAGE_SIZE));
    }

    /**
     * Returns the expected range of values for a band.
     *
     * @param  imageIndex The image index.
     * @param  bandIndex The band index. Valid index goes from <code>0</code> inclusive
     *         to <code>getNumBands(imageIndex)</code> exclusive. Index are independent
     *         of any {@link ImageReadParam#setSourceBands} setting.
     * @return The expected range of values.
     * @throws IOException If an error occurs reading the data information from the input source.
     */
    public Range getExpectedRange(final int imageIndex, final int bandIndex) throws IOException
    {
        checkBandIndex(imageIndex, bandIndex);
        return (ranges!=null) ? ranges[bandIndex] : null;
    }

    /**
     * Indique si la donnée spécifiée représente une donnée
     * manquante. L'implémentation par défaut compare cette
     * valeur à la valeur {@link Spi#padValue}.
     */
    protected boolean isPadValue(final double value)
    {return Double.doubleToLongBits(value)==padValue;}

    /**
     * Service provider interface (SPI) for {@link RawBinaryImageReader}s.
     * This SPI provides all necessary implementations for creating default
     * {@link RawBinaryImageReader}. Subclasses only have to set some fields
     * at construction time, e.g.:
     *
     * <blockquote><pre>
     * public final class CustomBinaryImageReaderSpi extends RawBinaryImageReader.Spi
     * {
     *     public CustomBinaryImageReaderSpi()
     *     {
     *         super("MyRAW", "image/raw-perso");
     *         {@link #vendorName vendorName} = "Institut de Recherche pour le Développement";
     *         {@link #version    version}    = "1.0";
     *         {@link #dataType   dataType}   = {@link DataBuffer#TYPE_SHORT};
     *         {@link #imageSize  imageSize}  = new {@link Dimension}(400,600);
     *         {@link #padValue   padValue}   = 9999;
     *     }
     * }
     * </pre></blockquote>
     *
     * (Note: fields <code>vendorName</code> and <code>version</code> are only informatives).
     * There is no need to override any method in this example. However, developers
     * can gain more control by creating subclasses of {@link TextRecordImageReader}
     * <strong>and</strong> <code>Spi</code> and overriding some of their methods.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static class Spi extends ImageReaderSpi
    {
        /**
         * Default list of file's extensions.
         */
        private static final String[] EXTENSIONS = new String[] {".raw",".RAW"};

        /**
         * The expected image size, or <code>null</code> if unknow. Setting this
         * field to a non-null value allow {@link RawBinaryImageReader#getWidth}
         * and {@link RawBinaryImageReader#getHeight} to returns this size. This
         * size will be compared with {@link RawBinaryImageReadParam#getStreamImageSize}
         * at reading time, if such a parameter is specified. An {@link IIOException}
         * exception will be thrown if sizes don't match, in order to ensure consistency
         * with <code>getWidth(int)</code> and <code>getHeight(int)</code> methods.
         */
        protected Dimension imageSize;

        /**
         * The pad value. This field is initially set to {@link Double#NaN}.
         * This default value is actually redundant since <code>NaN</code>
         * is always considered as a pad value, no matter the <code>padValue</code>
         * setting. Subclasses should set a new value at construction time
         * if an other pad value is needed in addition to {@link Double#NaN}.
         * If a single pad value is not enough, more control can be gained
         * by overriding {@link RawBinaryImageReader#isPädValue}.
         */
        protected double padValue = Double.NaN;

        /**
         * The default data type.  It should be a constant from {@link DataBuffer}.
         * Common types are {@link DataBuffer#TYPE_INT}, {@link DataBuffer#TYPE_FLOAT}
         * and {@link DataBuffer#TYPE_DOUBLE}. The default value is <code>TYPE_FLOAT</code>.
         */
        protected int dataType = DataBuffer.TYPE_FLOAT;

        /**
         * Construct a new SPI with name "raw" and MIME type "image/raw".
         */
        public Spi()
        {this("raw", "image/raw");}

        /**
         * Construct a new SPI for {@link RawBinaryImageReader}. This
         * constructor initialize the following fields to default
         * values:
         *
         * <ul>
         *   <li>Image format names ({@link #names}):
         *       An array of lenght 1 containing the <code>name</code> argument.
         *
         *   <li>MIME type ({@link #MIMETypes}):
         *       An array of length 1 containing the <code>mime</code> argument.
         *
         *   <li>File suffixes ({@link #suffixes}):
         *       "<code>.raw</code>"
         *       (uppercase and lowercase).</li>
         *
         *   <li>Input types ({@link #inputTypes}):
         *       {@link ImageInputStream}.</li>
         * </ul>
         *
         * @param name Format name, or <code>null</code> to let {@link #names} unset.
         * @param mime MIME type, or <code>null</code> to let {@link #MIMETypes} unset.
         */
        public Spi(final String name, final String mime)
        {
            if (name!=null) names     = new String[] {name};
            if (mime!=null) MIMETypes = new String[] {mime};
            suffixes        = EXTENSIONS;
            inputTypes      = STANDARD_INPUT_TYPE;
            pluginClassName = "net.seas.image.io.RawBinaryImageReader";
            vendorName      = "Institut de Recherche pour le Développement";
            version         = "1.0";
        }

        /**
         * Returns a brief, human-readable description of this service provider
         * and its associated implementation. The resulting string should be
         * localized for the supplied locale, if possible.
         *
         * @param  locale A Locale for which the return value should be localized.
         * @return A String containing a description of this service provider.
         */
        public String getDescription(final Locale locale)
        {return Resources.getResources(locale).getString(Clé.RAW_READER_DESCRIPTION);}

        /**
         * Returns <code>false</code>, since "raw" format can
         * potentially attempt to read nearly any stream.
         *
         * @param  source The input source to be decoded.
         * @return <code>false</code> in order to avoid being
         *         invoked in preference to a closer match.
         * @throws IOException if an I/O error occurs while reading the stream.
         */
        public boolean canDecodeInput(final Object source) throws IOException
        {return false;}

        /**
         * Returns an instance of the ImageReader implementation associated
         * with this service provider. The optional <code>extension</code>
         * argument may be one of the following classes:
         *
         * <ul>
         *   <li>{@link Dimension} for specifying the image size.</li>
         * </ul>
         *
         * @param  extension An optional extension object, which may be null.
         * @return An image reader instance.
         * @throws IOException if the attempt to instantiate the reader fails.
         */
        public ImageReader createReaderInstance(final Object extension) throws IOException
        {
            final RawBinaryImageReader reader=new RawBinaryImageReader(this);
            if (extension instanceof Dimension)
            {
                reader.imageSize = new Dimension((Dimension) extension);
            }
            return reader;
        }
    }
}
