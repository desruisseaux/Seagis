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
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.net.URLConnection;
import java.net.URL;

// Images
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentColorModel;
import javax.media.jai.ComponentSampleModelJAI;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

// Miscellaneous
import javax.media.jai.util.Range;
import net.seas.resources.Resources;
import net.seas.awt.ExceptionMonitor;


/**
 * Base class for simple image decoders. "Simple" images are usually flat binary
 * or ASCII files with no meta-data and no color information. There pixel values
 * may be floating point values instead of integers.  Such formats are of common
 * use in remote sensing.
 * <br><br>
 * This base class makes it easier to construct images from floating point values.
 * It provides default implementations for most {@link ImageReader} methods. Since
 * <code>SimpleImageReader</code> does not expect to know anything about image's
 * color, it uses a grayscale color space scaled to fit the range of values.
 * Displaying such an image may be very slow. Consequently, users who want
 * to display image are encouraged to change data type and color space with
 * <a href="http://java.sun.com/products/java-media/jai/">Java Advanced Imaging</a>
 * operators after reading.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 *
 * @see RawBinaryImageReader
 * @see TextRecordImageReader
 * @see TextMatrixImageReader
 */
public abstract class SimpleImageReader extends ImageReader
{
    /**
     * The stream position when {@link #setInput} is invoked.
     */
    private long streamOrigin;

    /**
     * Construct a new image reader.
     *
     * @param provider the {@link ImageReaderSpi} that is
     *                 invoking this constructor, or null.
     */
    protected SimpleImageReader(final ImageReaderSpi provider)
    {super(provider);}

    /**
     * Sets the input source to use. If <code>input</code> is <code>null</code>,
     * any currently set input source will be removed.
     *
     * @param input           The input object to use for future decoding.
     * @param seekForwardOnly If true, images and metadata may only be read
     *                        in ascending order from this input source.
     * @param ignoreMetadata  If true, metadata may be ignored during reads.
     */
    public void setInput(final Object input, final boolean seekForwardOnly, final boolean ignoreMetadata)
    {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        if (input instanceof ImageInputStream) try
        {
            streamOrigin = ((ImageInputStream) input).getStreamPosition();
        }
        catch (IOException exception)
        {
            streamOrigin = 0;
            ExceptionMonitor.unexpectedException("net.seas.image.io", "SimpleImageReader", "setInput", exception);
        }
    }

    /**
     * Vérifie si l'index de l'image est dans la plage des valeurs
     * autorisées. L'index maximal autorisé est obtenu en appelant
     * <code>{@link #getNumImages getNumImages}(false)</code>.
     *
     * @param  imageIndex Index dont on veut vérifier la validité.
     * @throws IndexOutOfBoundsException si l'index spécifié n'est pas valide.
     * @throws IOException si l'opération a échouée à cause d'une erreur d'entrés/sorties.
     */
    final void checkImageIndex(final int imageIndex) throws IOException, IndexOutOfBoundsException
    {
        final int numImages = getNumImages(false);
        if (imageIndex<minIndex || (imageIndex>=numImages && numImages>=0))
            throw new IndexOutOfBoundsException(String.valueOf(imageIndex));
    }

    /**
     * Vérifie si l'index de la bande est dans la plage des valeurs
     * autorisées. L'index maximal autorisé est obtenu en appelant
     * {@link #getNumBands}. L'index de l'image sera aussi vérifié.
     *
     * @param  imageIndex Index de l'image dont on veut vérifier la validité.
     * @param  bandIndex  Index de la bande dont on veut vérifier la validité.
     * @throws IndexOutOfBoundsException si l'index spécifié n'est pas valide.
     * @throws IOException si l'opération a échouée à cause d'une erreur d'entrés/sorties.
     */
    final void checkBandIndex(final int imageIndex, final int bandIndex) throws IOException, IndexOutOfBoundsException
    {
        // Call 'getNumBands' first in order to call 'checkImageIndex'.
        if (bandIndex>=getNumBands(imageIndex) || bandIndex<0)
            throw new IndexOutOfBoundsException(String.valueOf(bandIndex));
    }

    /**
     * Returns the number of images available from the current input source.
     * Default implementation returns 1.
     *
     * @param  allowSearch If true, the number of images will be returned
     *         even if a search is required.
     * @return The number of images, or -1 if <code>allowSearch</code>
     *         is false and a search would be required.
     *
     * @throws IllegalStateException if the input source has not been set.
     * @throws IOException if an error occurs reading the information from the input source.
     */
    public int getNumImages(final boolean allowSearch) throws IllegalStateException, IOException
    {
        if (input!=null) return 1;
        throw new IllegalStateException(Resources.format(Clé.NO_IMAGE_INPUT));
    }

    /**
     * Returns the number of bands available for the specified image.
     * Default implementation returns 1.
     *
     * @param  imageIndex  The image index.
     * @throws IOException if an error occurs reading the information from the input source.
     */
    public int getNumBands(final int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        return 1;
    }

    /**
     * Retourne les méta-données associées à une image en particulier. Etant donné
     * que les fichiers de données brutes ne contiennent généralement pas de
     * méta-données, l'implémentation par défaut retourne toujours <code>null</code>.
     *
     * @throws IOException si l'opération a échouée à cause d'une erreur d'entrés/sorties.
     */
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        return null;
    }

    /**
     * Retourne les méta-données associées à l'ensemble du fichier. Etant donné
     * que les fichiers de données brutes ne contiennent généralement pas de
     * méta-données, l'implémentation par défaut retourne toujours <code>null</code>.
     *
     * @throws IOException si l'opération a échouée à cause d'une erreur d'entrés/sorties.
     */
    public IIOMetadata getStreamMetadata() throws IOException
    {return null;}

    /**
     * Returns a collection of {@link ImageTypeSpecifier} containing possible image
     * types to which the given image may be decoded. The default implementation
     * returns a singleton containing {@link #getRawImageType}.
     *
     * @param  imageIndex The index of the image to be retrieved.
     * @return A set of suggested image types for decoding the current given image.
     * @throws IOException If an error occurs reading the format information from the input source.
     */
    public Iterator getImageTypes(final int imageIndex) throws IOException
    {return Collections.singleton(getRawImageType(imageIndex)).iterator();}

    /**
     * Returns an image type specifier indicating the {@link SampleModel} and {@link ColorModel}
     * which most closely represents the "raw" internal format of the image. The default
     * implementation returns an image type specifier for a {@link BandedSampleModel} of
     * data type {@link #getRawDataType}.
     *
     * @param  imageIndex The index of the image to be queried.
     * @return The image type (never <code>null</code>).
     * @throws IOException If an error occurs reading the format information from the input source.
     */
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException
    {
        final int dataType = getRawDataType(imageIndex);
        final int numBands = getNumBands(imageIndex);
        final int[] bankIndices = new int[numBands];
        final int[] bandOffsets = new int[numBands];
        for (int i=numBands; --i>=0;) bankIndices[i]=i;
        final ColorSpace colorSpace = getColorSpace(imageIndex, 0, numBands);
        
        if (true)
        {
            /*
             * Note: We should use ImageTypeSpecifier.createBanded(...) instead.
             *       Unfortunatly, there is two problems with 'createBanded(...)':
             *
             *    1) As of JDK 1.4-beta2, 'createBanded' don't accept TYPE_FLOAT and TYPE_DOUBLE.
             *       See source code for 'ImageTypeSpecifier.createComponentCM(...)'.
             *    2) As of JAI 1.1, operators don't accept Java2D's DataBufferFloat and
             *       DataBufferDouble. They require JAI's DataBuffer instead.
             */
            final ColorModel cm = new ComponentColorModel(colorSpace, null, false, false, Transparency.OPAQUE, dataType);
            return new ImageTypeSpecifier(cm, new ComponentSampleModelJAI(dataType, 1, 1, 1, 1, bankIndices, bandOffsets));
        }
        return ImageTypeSpecifier.createBanded(colorSpace, bankIndices, bandOffsets, dataType, false, false);
    }

    /**
     * Returns the data type which most closely represents the "raw"
     * internal data of the image. It should be a constant from
     * {@link DataBuffer}. Common types are {@link DataBuffer#TYPE_INT},
     * {@link DataBuffer#TYPE_FLOAT} and {@link DataBuffer#TYPE_DOUBLE}.
     * The default implementation returns <code>TYPE_FLOAT</code>.
     *
     * @param  imageIndex The index of the image to be queried.
     * @return The data type (<code>TYPE_FLOAT</code> by default).
     * @throws IOException If an error occurs reading the format information from the input source.
     */
    public int getRawDataType(final int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        return DataBuffer.TYPE_FLOAT;
    }

    /**
     * Returns the expected range of values for a band. Implementation
     * may read image data, or just returns some raisonable range.
     *
     * @param  imageIndex The image index.
     * @param  bandIndex The band index. Valid index goes from <code>0</code> inclusive
     *         to <code>getNumBands(imageIndex)</code> exclusive. Index are independent
     *         of any {@link ImageReadParam#setSourceBands} setting.
     * @return The expected range of values, or <code>null</code> if unknow.
     * @throws IOException If an error occurs reading the data information from the input source.
     */
    public abstract Range getExpectedRange(final int imageIndex, final int bandIndex) throws IOException;

    /**
     * Returns a default color space. Default implementation returns a
     * grayscale color space scaled to fit {@link #getExpectedRange}.
     *
     * @param  imageIndex The image index.
     * @param  bandIndex  The band index.
     * @param  numBands   The number of bands.
     * @return A default color space scaled to fit data.
     * @throws IOException if an input operation failed.
     */
    final ColorSpace getColorSpace(final int imageIndex, final int bandIndex, final int numBands) throws IOException
    {
        final int dataType = getRawDataType(imageIndex);
        if (dataType!=DataBuffer.TYPE_BYTE)
        {
            final Range range = getExpectedRange(imageIndex, bandIndex);
            if (range!=null && Number.class.isAssignableFrom(range.getElementClass()))
            {
                final Number minimum = (Number) range.getMinValue();
                final Number maximum = (Number) range.getMaxValue();
                if (minimum!=null && maximum!=null)
                {
                    final float minValue = minimum.floatValue();
                    final float maxValue = maximum.floatValue();
                    if (minValue<maxValue && !Float.isInfinite(minValue) && !Float.isInfinite(maxValue))
                    {
                        return new ScaledColorSpace(numBands, minValue, maxValue);
                    }
                }
            }
        }
        return ColorSpace.getInstance(ColorSpace.CS_GRAY);
    }

    /**
     * Retourne la longueur (en nombre d'octets) des données à lire, ou <code>-1</code> si cette longueur
     * n'est pas connue.  Cette méthode examine le type d'entré (@link #getInput}) et appelle une méthode
     * {@link File#length()}, {@link ImageInputStream#length()} ou {@link URLConnection#getContentLength()}
     * en fonction du type d'entré.
     *
     * @throws IOException si une erreur est survenue.
     */
    protected long getStreamLength() throws IOException
    {
        final Object input=getInput();
        if (input instanceof ImageInputStream)
        {
            long length = ((ImageInputStream) input).length();
            if (length>=0) length -= streamOrigin;
            return length;
        }
        if (input instanceof File)
        {
            return ((File) input).length();
        }
        if (input instanceof URL)
        {
            return ((URL) input).openConnection().getContentLength();
        }
        if (input instanceof URLConnection)
        {
            return ((URLConnection) input).getContentLength();
        }
        return -1;
    }

    /**
     * Retourne une approximation du nombre d'octets du flot occupés par les images <code>fromImage</code>
     * inclusivement jusqu'à <code>toImage</code> exclusivement. L'implémentation par défaut calcule cette
     * longueur en supposant que toutes les images se divisent la longueur totale du flot en parts égales.
     *
     * @param fromImage Index de la première image à prendre en compte.
     * @param   toImage Index suivant celui de la dernière image à prendre en compte, ou -1 pour
     *                  prendre en compte toutes les images restantes jusqu'à la fin du flot.
     * @return Le nombre d'octets occupés par les images spécifiés, ou -1 si cette longueur n'a pas
     *         pu être calculée. Si le calcul précis de cette longueur serait prohibitif, cette méthode
     *         est autorisée à retourner une simple approximation ou même à retourner la longueur totale
     *         du flot.
     * @throws IOException si une erreur est survenue lors de la lecture du flot.
     */
    protected long getStreamLength(final int fromImage, int toImage) throws IOException
    {
        long length = getStreamLength();
        if (length > 0)
        {
            final int numImages = getNumImages(false);
            if (numImages > 0)
            {
                if (toImage == -1) toImage=numImages;
                if (fromImage<0 || fromImage>numImages) throw new IndexOutOfBoundsException(String.valueOf(fromImage));
                if (  toImage<0 ||   toImage>numImages) throw new IndexOutOfBoundsException(String.valueOf(  toImage));
                if (fromImage > toImage)                throw new IllegalArgumentException();
                return length * (toImage-fromImage) / numImages;
            }
        }
        return length;
    }
}
