/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.metadata.IIOMetadata;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

// Miscellaneous
import net.seas.resources.Resources;


/**
 * Base class for simple image decoders. "Simple" images are usually flat binary
 * or ASCII files with no meta-data and no color information. There pixel values
 * may be floating point values instead of integers.  Such formats are of common
 * use in remote sensing.
 * <br><br>
 * This base class makes it easier to construct images from floating point values.
 * It provides default implementations for most {@link ImageReader} methods. Decoded
 * data can be stored as float values. Since <code>SimpleImageReader</code> does not
 * expect to know anything about image's color, it uses a grayscale color space scaled
 * to fit the range of values. Because displaying such an image can be very slow, users
 * who want to display image are strongly encouraged to change data type and color space
 * with <a href="http://java.sun.com/products/java-media/jai/">Java Advanced Imaging</a>
 * operators after reading.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 *
 * @see TextRecordImageReader
 * @see MatrixImageReader
 */
public abstract class SimpleImageReader extends ImageReader
{
    /**
     * Type des images les plus proches du format de l'image. Ce type
     * devrait �tre une des constantes {@link DataBuffer#TYPE_FLOAT},
     * {@link DataBuffer#TYPE_DOUBLE} ou {@link DataBuffer#TYPE_INT}.
     */
    private final int rawImageType;

    /**
     * Construit un d�codeur d'images de
     * type {@link DataBuffer#TYPE_FLOAT}.
     *
     * @param provider Le fournisseur
     *        qui a construit ce d�codeur.
     */
    protected SimpleImageReader(final ImageReaderSpi provider)
    {this(provider, DataBuffer.TYPE_FLOAT);}

    /**
     * Construit un d�codeur d'images du type sp�cifi�.
     *
     * @param provider Le fournisseur qui a construit ce d�codeur.
     * @param rawImageType Type par d�faut des images. Ce type devrait
     *        �tre une des constantes de {@link DataBuffer}, notamment
     *        {@link DataBuffer#TYPE_INT}, {@link DataBuffer#TYPE_FLOAT}
     *        ou {@link DataBuffer#TYPE_DOUBLE}.
     */
    protected SimpleImageReader(final ImageReaderSpi provider, final int rawImageType)
    {
        super(provider);
        this.rawImageType = rawImageType;
    }

    /**
     * V�rifie si l'index de l'image est dans la plage des valeurs
     * autoris�es. L'index maximal autoris� est obtenu en appellant
     * <code>{@link #getNumImages getNumImages}(false)</code>.
     *
     * @param  imageIndex Index dont on veut v�rifier la validit�.
     * @throws IndexOutOfBoundsException si l'index sp�cifi� n'est pas valide.
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    protected final void checkImageIndex(final int imageIndex) throws IndexOutOfBoundsException, IOException
    {
        final int numImages = getNumImages(false);
        if (imageIndex<minIndex || (imageIndex>=numImages && numImages>=0))
            throw new IndexOutOfBoundsException(String.valueOf(imageIndex));
    }

    /**
     * Retourne le nombre d'image dans le fichier � lire. L'impl�mentation par d�faut
     * v�rifie si une source a �t� sp�cifi�e (elle lance {@link IllegalStateException}
     * si ce n'est pas le cas), et ensuite retourne toujours 1.
     *
     * @param allowSearch si cette m�thode est autoris� � parcourir
     *                    le fichier pour obtenir cette information.
     * @return Le nombre d'images, ou -1 si cette information aurait
     *         n�cessit� un balayage du fichier et que ce n'est pas
     *         permis.
     *
     * @throws IllegalStateException si aucune source n'avait �t� sp�cifi�e � {@link #setInput}.
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public int getNumImages(final boolean allowSearch) throws IllegalStateException, IOException
    {
        if (input!=null) return 1;
        throw new IllegalStateException(Resources.format(Cl�.NO_IMAGE_INPUT));
    }

    /**
     * Retourne le nombre de bandes dans l'image � l'index sp�cifi�. L'impl�mentation
     * par d�faut v�rifie si <code>imageIndex</code> est dans les limites permises et
     * retourne ensuite 1.
     *
     * @param  imageIndex Index de l'image dont on veut conna�tre le nombre de bandes.
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public int getNumBands(final int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        return 1;
    }

    /**
     * Retourne la valeur minimale m�moris�e dans une bande de l'image.
     *
     * @param  imageIndex Index de l'image dont on veut conna�tre la valeur minimale.
     * @param  band Bande pour laquelle on veut la valeur minimale. Les num�ros de
     *         bandes commencent � 0  et sont ind�pendents des valeurs qui peuvent
     *         avoir �t� sp�cifi�es � {@link ImageReadParam#setSourceBands}.
     * @return Valeur minimale trouv�e dans l'image et la bande sp�cifi�e.
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public abstract double getMinimum(final int imageIndex, final int band) throws IOException;

    /**
     * Retourne la valeur maximale m�moris�e dans une bande de l'image.
     *
     * @param  imageIndex Index de l'image dont on veut conna�tre la valeur maximale.
     * @param  band Bande pour laquelle on veut la valeur maximale. Les num�ros de
     *         bandes commencent � 0  et sont ind�pendents des valeurs qui peuvent
     *         avoir �t� sp�cifi�es � {@link ImageReadParam#setSourceBands}.
     * @return Valeur maximale trouv�e dans l'image et la bande sp�cifi�e.
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public abstract double getMaximum(final int imageIndex, final int band) throws IOException;

    /**
     * Retourne le format le plus pr�s du format interne de l'image. L'impl�mentation par
     * d�faut sp�cifie un format ({@link BandedSampleModel}) qui m�morise chaque canal de
     * l'image dans un tableau s�par�.
     *
     * @param  imageIndex Index de l'image.
     * @return Type de l'image (ne sera jamais <code>null</code>).
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException
    {
        final int numBands = getNumBands(imageIndex);
        final int[] bankIndices = new int[numBands];
        final int[] bandOffsets = new int[numBands];
        for (int i=numBands; --i>=0;) bankIndices[i]=i;

        ColorSpace space = null;
        double   minimum = Double.NaN;
        double   maximum = Double.NaN;
        for (int band=0; band<numBands; band++)
        {
            minimum = getMinimum(imageIndex, band);
            maximum = getMaximum(imageIndex, band);
            if (minimum < maximum)
            {
                space = new ScaledColorSpace((float)minimum, (float)maximum);
                break;
            }
        }
        if (space==null) space=ColorSpace.getInstance(ColorSpace.CS_GRAY);
        return ImageTypeSpecifier.createBanded(space, bankIndices, bandOffsets, rawImageType, false, false);
    }

    /**
     * Retourne quelques types d'images qui pourront contenir les donn�es.
     * Le premier objet retourn� sera celui qui convient le mieux pour un
     * affichage � l'�cran.
     *
     * @param  imageIndex Index de l'image dont on veut les types.
     * @return It�rateur balayant les types de l'image.
     * @throws IndexOutOfBoundsException si <code>imageIndex</code> est invalide.
     * @throws IllegalStateException si aucune source n'a �t� sp�cifi�e avec {@link #setInput}.
     * @throws IIOException si l'op�ration a �chou� pour une autre raison.
     */
    public Iterator getImageTypes(final int imageIndex) throws IOException
    {return Collections.singleton(getRawImageType(imageIndex)).iterator();}

    /**
     * Retourne un objet par d�faut permettant de sp�cifier les param�tres
     * de la lecture. L'impl�mentation construit et retourne un objet
     * {@link SimpleImageReadParam}.
     */
    public ImageReadParam getDefaultReadParam()
    {return new SimpleImageReadParam();}

    /**
     * Retourne les m�ta-donn�es associ�es � une image en particulier. Etant donn�
     * que les fichiers de donn�es brutes ne contiennent g�n�ralement pas de
     * m�ta-donn�es, l'impl�mentation par d�faut retourne toujours <code>null</code>.
     *
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        return null;
    }

    /**
     * Retourne les m�ta-donn�es associ�es � l'ensemble du fichier. Etant donn�
     * que les fichiers de donn�es brutes ne contiennent g�n�ralement pas de
     * m�ta-donn�es, l'impl�mentation par d�faut retourne toujours <code>null</code>.
     *
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public IIOMetadata getStreamMetadata() throws IOException
    {return null;}

    /**
     * Retourne la longueur (en nombre d'octets) des donn�es � lire, ou <code>-1</code> si cette longueur
     * n'est pas connue.  Cette m�thode examine le type d'entr� (@link #getInput}) et appelle une m�thode
     * {@link File#length()}, {@link ImageInputStream#length()} ou {@link URLConnection#getContentLength()}
     * en fonction du type d'entr�.
     *
     * @throws IOException si une erreur est survenue.
     */
    protected long getStreamLength() throws IOException
    {
        final Object input=getInput();
        if (input instanceof ImageInputStream)
        {
            return ((ImageInputStream) input).length();
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
     * Retourne une approximation du nombre d'octets du flot occup�s par les images <code>fromImage</code>
     * inclusivement jusqu'� <code>toImage</code> exclusivement. L'impl�mentation par d�faut calcule cette
     * longueur en supposant que toutes les images se divisent la longueur totale du flot en parts �gales.
     *
     * @param fromImage Index de la premi�re image � prendre en compte.
     * @param   toImage Index suivant celui de la derni�re image � prendre en compte, ou -1 pour
     *                  prendre en compte toutes les images restantes jusqu'� la fin du flot.
     * @return Le nombre d'octets occup�s par les images sp�cifi�s, ou -1 si cette longueur n'a pas
     *         pu �tre calcul�e. Si le calcul pr�cis de cette longueur serait prohibitif, cette m�thode
     *         est autoris�e � retourner une simple approximation ou m�me � retourner la longueur totale
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
