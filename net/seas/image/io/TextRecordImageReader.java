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

// Images
import java.awt.image.DataBuffer;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;

// Entr�s/sorties d'images
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;

// G�om�trie
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.AffineTransform;

// Ensembles
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Arrays;

// Entr�s/sorties
import java.io.IOException;
import java.io.BufferedReader;
import java.text.ParseException;
import net.seas.text.LineFormat;

// Divers
import java.util.Locale;
import net.seas.util.XMath;
import net.seas.util.XArray;
import net.seas.resources.Resources;
import java.util.logging.Logger;


/**
 * Image decoder for text files storing pixel values as records.
 * Such text files use one line (record) by pixel. Each line contains
 * at least 3 columns (in arbitrary order):
 *
 * <ul>
 *   <li>Pixel's <var>x</var> coordinate.</li>
 *   <li>Pixel's <var>y</var> coordinate.</li>
 *   <li>An arbitrary number of pixel values.</li>
 * </ul>
 *
 * For example, some Sea Level Anomaly (SLA) files contains rows of longitude
 * (degrees), latitude (degrees), SLA (cm), East/West current (cm/s) and
 * North/South current (cm/s), as below:
 *
 * <blockquote><pre>
 * 45.1250 -29.8750    -7.28     10.3483     -0.3164
 * 45.1250 -29.6250    -4.97     11.8847      3.6192
 * 45.1250 -29.3750    -2.91      3.7900      3.0858
 * 45.1250 -29.1250    -3.48     -5.1833     -5.0759
 * 45.1250 -28.8750    -4.36     -1.8129    -16.3689
 * 45.1250 -28.6250    -3.91      7.5577    -24.6801
 * </pre>(...etc...)
 * </blockquote>
 *
 * From this decoder point of view, the two first columns (longitude and latitude)
 * are pixel's logical coordinate (<var>x</var>,<var>y</var>), while the three last
 * columns are three image's bands. The whole file contains only one image (unless
 * {@link #getNumImages} has been overridden). All (<var>x</var>,<var>y</var>)
 * coordinates belong to pixel's center. This decoder will automatically translate
 * (<var>x</var>,<var>y</var>) coordinates from logical space to pixel space. The
 * {@link #getTransform} method provides a convenient {@link AffineTransform} for
 * performing coordinate transformations between pixel and logical spaces.
 * <br><br>
 * By default, <code>TextRecordImageReader</code> assume that <var>x</var> and
 * <var>y</var> coordinates appear in column #0 and 1 respectively. It also assumes
 * that numeric values are encoded using current defaults {@link java.nio.charset.Charset}
 * and {@link java.util.Locale}, and that there is no pad value. The easiest way to change
 * the default setting is to create a {@link Spi} subclass. There is no need to subclass
 * <code>TextRecordImageReader</code>, unless you want more control on the decoding process.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class TextRecordImageReader extends TextImageReader
{
    /**
     * Petit facteur de tol�rance servant � tenir
     * compte des erreurs d'arrondissement.
     */
    private static final float EPS = 1E-6f;

    /**
     * Intervalle (en nombre d'octets) entre les rapports de progr�s.
     */
    private static final int PROGRESS_INTERVAL = 4096;

    /**
     * Lorsque la lecture se fait par-dessus une image {@link BufferedReader} existante,
     * indique s'il faut effacer la r�gion dans laquelle sera plac�e l'image avant de la
     * lire. La valeur <code>false</code> permettra de conserver les anciens pixels dans
     * les r�gions ou le fichier ne d�finit pas de nouvelles valeurs.
     */
    private static final boolean CLEAR = false;

    /**
     * Num�ro de colonne des <var>x</var>, compt� � partir de 0.
     * Ce champ n'existe que pour des raisons de performances; il
     * n'est utilis� que par {@link #parseLine} pendant la lecture
     * d'une image. Dans tous les autres cas, on utilisera plut�t
     * {@link #getColumnX}.
     */
    private transient int xColumn = 0;

    /**
     * Num�ro de colonne des <var>y</var>, compt� � partir de 0.
     * Ce champ n'existe que pour des raisons de performances; il
     * n'est utilis� que par {@link #parseLine} pendant la lecture
     * d'une image. Dans tous les autres cas, on utilisera plut�t
     * {@link #getColumnY}.
     */
    private transient int yColumn = 1;

    /**
     * Valeur repr�sentant les donn�es manquantes,   ou {@link Double#NaN} s'il n'y en
     * a pas. Ce champ n'existe que pour des raisons de performances; il n'est utilis�
     * que par {@link #parseLine} pendant la lecture d'une image. Dans tous les autres
     * cas, on utilisera plut�t {@link #getPadValue}.
     */
    private transient double padValue = Double.NaN;

    /**
     * Objet � utiliser pour lire chacune des lignes de fichier. Ce champ n'existe que
     * pour des raisons de performances; il n'est utilis� que par {@link #parseLine}
     * pendant la lecture d'une image. Dans tous les autres cas, on utilisera plut�t
     * {@link #getLineFormat}.
     */
    private transient LineFormat lineFormat;

    /**
     * Donn�es des images, ou <code>null</code> si aucune lecture n'a encore �t� faite. Chaque �l�ment
     * contient les donn�es de l'image � l'index correspondant    (i.e. l'�l�ment <code>data[0]</code>
     * contient les donn�es de l'image #0,    <code>data[1]</code> contient les donn�es de l'image #1,
     * etc.).   Des �l�ments de ce tableau peuvent �tre nuls si les donn�es des images correspondantes
     * ne sont pas retenues apr�s chaque lecture (c'est-�-dire si <code>{@link #seekForwardOnly}==true</code>).
     */
    private RecordList[] data;

    /**
     * Index de la prochaine image � lire. Cet index n'est pas n�cessairement �gal � la
     * longueur du tableau {@link #data}. Il peut �tre aussi bien plus petit que plus grand.
     */
    private int nextImageIndex;

    /**
     * Nombre moyen de caract�res par donn�es (incluant les espaces et les codes
     * de fin de ligne). Cette information n'est qu'� titre indicative, mais son
     * exactitude peut aider � accelerer la lecture et rendre les rapport des
     * progr�s plus pr�cis. Elle sera automatiquement mise � jour en fonction
     * des lignes lues.
     */
    private float expectedDatumLength = 10.4f;

    /**
     * Construit un d�codeur d'images
     * de type {@link DataBuffer#TYPE_FLOAT}.
     *
     * @param provider Le fournisseur
     *        qui a construit ce d�codeur.
     */
    public TextRecordImageReader(final ImageReaderSpi provider)
    {
        super(provider);
        clear();
    }

    /**
     * Construit un d�codeur d'images.
     *
     * @param provider Le fournisseur qui a construit ce d�codeur.
     * @param rawImageType Type par d�faut des images. Ce type devrait �tre une des
     *        constantes de {@link DataBuffer}, notamment {@link DataBuffer#TYPE_INT}
     *        ou {@link DataBuffer#TYPE_FLOAT}. Le type {@link DataBuffer#TYPE_DOUBLE}
     *        est accept� mais d�conseill�, �tant donn� que l'impl�mentation actuelle
     *        ne lira les donn�es qu'avec la pr�cision des types <code>float</code>.
     */
    public TextRecordImageReader(final ImageReaderSpi provider, final int rawImageType)
    {
        super(provider, rawImageType);
        clear();
        if (rawImageType == DataBuffer.TYPE_DOUBLE)
        {
            Logger.getLogger("net.seas.image.io").warning("Type double is deprecated.");
        }
    }

    /**
     * Retourne le num�ro de colonne des <var>x</var>, compt� � partir de 0.
     * L'impl�mentation par d�faut retourne le num�ro de colonne qui avait �t�
     * sp�cifi� dans l'objet {@link Spi} qui a cr�� ce d�codeur. Les classes
     * d�riv�es peuvent red�finir cette m�thode pour d�terminer cette valeur
     * d'une fa�on plus �labor�e.
     *
     * @param  imageIndex Index de l'image � lire.
     * @throws IOException si l'op�ration n�cessitait une lecture du fichier (par exemple
     *         des informations inscrites dans un en-t�te) et que cette lecture a �chou�e.
     */
    public int getColumnX(final int imageIndex) throws IOException
    {return (originatingProvider instanceof Spi) ? ((Spi)originatingProvider).xColumn : 0;}

    /**
     * Retourne le num�ro de colonne des <var>y</var>, compt� � partir de 0.
     * L'impl�mentation par d�faut retourne le num�ro de colonne qui avait �t�
     * sp�cifi� dans l'objet {@link Spi} qui a cr�� ce d�codeur. Les classes
     * d�riv�es peuvent red�finir cette m�thode pour d�terminer cette valeur
     * d'une fa�on plus �labor�e.
     *
     * @param  imageIndex Index de l'image � lire.
     * @throws IOException si l'op�ration n�cessitait une lecture du fichier (par exemple
     *         des informations inscrites dans un en-t�te) et que cette lecture a �chou�e.
     */
    public int getColumnY(final int imageIndex) throws IOException
    {return (originatingProvider instanceof Spi) ? ((Spi)originatingProvider).yColumn : 1;}

    /**
     * Retourne le num�ro de colonne dans laquelle se trouvent les donn�es de la bande sp�cifi�e.
     * L'impl�mentation par d�faut retourne <code>band</code> + 1 ou 2 si la bande est plus grand
     * ou �gal � {@link #getColumnX} et/ou {@link #getColumnY}. Cette impl�mentation devrait
     * convenir pour des donn�es se trouvant aussi bien avant qu'apr�s les colonnes <var>x</var>
     * et <var>y</var>, m�me si ces derni�res ne sont pas cons�cutives.
     *
     * @param  imageIndex Index de l'image � lire.
     * @param  band Bande de l'image � lire.
     * @return Num�ro de colonne des donn�es de l'image.
     * @throws IOException si l'op�ration n�cessitait une lecture du fichier (par exemple
     *         des informations inscrites dans un en-t�te) et que cette lecture a �chou�e.
     */
    private int getColumn(final int imageIndex, int band) throws IOException
    {
        final int xColumn = getColumnX(imageIndex);
        final int yColumn = getColumnY(imageIndex);
        if (band >= Math.min(xColumn, yColumn)) band++;
        if (band >= Math.max(xColumn, yColumn)) band++;
        return band;
    }

    /**
     * Retourne la valeur repr�sentant les donn�es manquantes, ou {@link Double#NaN}
     * s'il n'y en a pas. Cette valeur s'appliquera � toutes les colonnes du fichier
     * sauf les colonnes des <var>x</var> et des <var>y</var>.  L'impl�mentation par
     * d�faut retourne la valeur qui avait �t� sp�cifi�e dans l'objet {@link Spi} qui
     * a cr�� ce d�codeur. Les classes d�riv�es peuvent red�finir cette m�thode pour
     * d�terminer cette valeur d'une fa�on plus �labor�e.
     *
     * @param  imageIndex Index de l'image � lire.
     * @throws IOException si l'op�ration n�cessitait une lecture du fichier (par exemple
     *         des informations inscrites dans un en-t�te) et que cette lecture a �chou�e.
     */
    public double getPadValue(final int imageIndex) throws IOException
    {return (originatingProvider instanceof Spi) ? ((Spi)originatingProvider).padValue : Double.NaN;}

    /**
     * Retourne l'objet � utiliser pour lire chaque ligne d'une image. L'impl�mentation par
     * d�faut construit un nouveal objet {@link LineFormat} en utilisant les conventions
     * locales sp�cifi�es par {@link Spi#locale}. Les classes d�riv�es peuvent red�finir
     * cette m�thode pour construire un objet {@link LineFormat} d'une fa�on plus �labor�e.
     *
     * @param  imageIndex Index de l'image � lire.
     * @throws IOException si l'op�ration n�cessitait une lecture du fichier (par exemple
     *         des informations inscrites dans un en-t�te) et que cette lecture a �chou�e.
     */
    public LineFormat getLineFormat(final int imageIndex) throws IOException
    {
        if (originatingProvider instanceof Spi)
        {
            final Locale locale = ((Spi)originatingProvider).locale;
            if (locale!=null) return new LineFormat(locale);
        }
        return new LineFormat();
    }

    /**
     * Sp�cifie le flot � utiliser en entr�. Ce flot peut �tre un objet des objets suivants (en ordre de pr�f�rence):
     * {@link java.io.File}, {@link java.net.URL} ou {@link java.io.BufferedReader}. Les flots de type
     * {@link java.io.Reader}, {@link java.io.InputStream} et {@link javax.imageio.stream.ImageInputStream}
     * sont aussi accept�s, mais moins conseill�s.
     */
    public void setInput(final Object input, final boolean seekForwardOnly, final boolean ignoreMetadata)
    {
        clear();
        super.setInput(input, seekForwardOnly, ignoreMetadata);
    }

    /**
     * Retourne le nombre de bandes dans l'image � l'index sp�cifi�.
     *
     * @param  imageIndex Index de l'image dont on veut conna�tre le nombre de bandes.
     * @throws IOException si l'op�ration a �chou�e � cause d'une erreur d'entr�s/sorties.
     */
    public int getNumBands(final int imageIndex) throws IOException
    {return getRecords(imageIndex).getColumnCount() - (getColumnX(imageIndex)==getColumnY(imageIndex) ? 1 : 2);}

    /**
     * Retourne la largeur de l'image � l'index sp�cifi�.
     *
     * @param  imageIndex Index de l'image dont on veut la largeur.
     * @return Largeur de l'image.
     * @throws IOException si la lecture de l'image a �chou�.
     */
    public int getWidth(final int imageIndex) throws IOException
    {return getRecords(imageIndex).getPointCount(getColumnX(imageIndex), EPS);}

    /**
     * Retourne la hauteur de l'image � l'index sp�cifi�.
     *
     * @param  imageIndex Index de l'image dont on veut la hauteur.
     * @return Hauteur de l'image.
     * @throws IOException si la lecture de l'image a �chou�.
     */
    public int getHeight(final int imageIndex) throws IOException
    {return getRecords(imageIndex).getPointCount(getColumnY(imageIndex), EPS);}

    /**
     * Retourne les coordonn�es logiques couvertes par l'image.
     * Les limites du rectangle retourn� correspondront aux valeurs
     * minimales et maximales des <var>x</var> et <var>y</var>.
     *
     * @param  imageIndex Index de l'image dont on veut les coordonn�es logiques.
     * @return Coordonn�es logiques couverte par l'image.
     * @throws IOException si la lecture de l'image a �chou�.
     */
    public Rectangle2D getLogicalBounds(final int imageIndex) throws IOException
    {
        final RecordList records = getRecords(imageIndex);
        final int xColumn        = getColumnX(imageIndex);
        final int yColumn        = getColumnY(imageIndex);
        final double xmin        = records.getMinimum(xColumn);
        final double ymin        = records.getMinimum(yColumn);
        final double width       = records.getMaximum(xColumn)-xmin;
        final double height      = records.getMaximum(yColumn)-ymin;
        final double dx          = width /(records.getPointCount(xColumn, EPS)-1);
        final double dy          = height/(records.getPointCount(yColumn, EPS)-1);
        return new Rectangle2D.Double(xmin-0.5*dx, ymin-0.5*dy, width+dx, height+dy);
    }

    /**
     * Returns an {@link AffineTransform} for transforming pixel coordinates
     * to logical coordinates. Pixel coordinates are usually integer values
     * with (0,0) at the image's upper-left corner, while logical coordinates
     * are floating point values at the pixel's upper-left corner. The later
     * is consistent with <a href="http://java.sun.com/products/java-media/jai/">Java
     * Advanced Imaging</a> convention. In order to get logical values at the pixel
     * center, a translation must be apply once as below:
     *
     * <blockquote><pre>
     * AffineTransform tr = getTransform(imageIndex);
     * tr.translate(0.5, 0.5);
     * </pre></blockquote>
     *
     * @param  imageIndex The 0-based image index.
     * @return A transform mapping pixel coordinates to logical coordinates.
     * @throws IOException if an I/O operation failed.
     */
    public AffineTransform getTransform(final int imageIndex) throws IOException
    {
        final Rectangle2D bounds = getLogicalBounds(imageIndex);
        final int          width = getWidth        (imageIndex);
        final int         height = getHeight       (imageIndex);
        final double  pixelWidth = bounds.getWidth ()/ (width-1);
        final double pixelHeight = bounds.getHeight()/(height-1);
        return new AffineTransform(pixelWidth, 0, 0, -pixelHeight,
                                   bounds.getMinX()-0.5*pixelWidth,
                                   bounds.getMaxY()+0.5*pixelHeight);
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
    public double getMinimum(final int imageIndex, final int band) throws IOException
    {return getRecords(imageIndex).getMinimum(getColumn(imageIndex, band));}

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
    public double getMaximum(final int imageIndex, final int band) throws IOException
    {return getRecords(imageIndex).getMaximum(getColumn(imageIndex, band));}

    /**
     * Convertit une ligne en valeurs num�riques. Cette m�thode est appel�e automatiquement
     * lors de la lecture de chaque ligne, avec en argument la ligne lue (<code>line</code>)
     * et le buffer dans lequel placer les valeurs num�riques (<code>values</code>).
     *
     * L'impl�mentation par d�faut d�code la ligne en exigeant qu'il y ait autant de nombres
     * que la longueur du tableau <code>values</code>.  Elle remplace ensuite les occurences
     * de <code>padValue</code> par {@link Double#NaN}  dans toutes les colonnes sauf celles
     * des coordonn�es <var>x</var> et <var>y</var>.
     *
     * @param line   Ligne � d�coder.
     * @param values Derni�res valeurs � avoir �t� lues, ou <code>null</code> si cette ligne
     *               est la premi�re � �tre d�cod�e. Se buffer peut �tre r�utiliser en �crasant
     *               les anciennes valeurs par les nouvelles valeurs de la ligne <code>line</code>.
     * @return Les valeurs lues, ou <code>null</code> si la fin de l'image a �t� atteinte. Le
     *         tableau retourn� sera habituellement le m�me que <code>values</code>, mais pas
     *         obligatoirement. Par convention, un tableau de longueur 0 signifie que la ligne
     *         ne contient aucune donn�e et doit �tre ignor�e.
     * @throws ParseException si une erreur est survenue lors du d�codage de la ligne.
     */
    protected double[] parseLine(final String line, double[] values) throws ParseException
    {
        if (line==null) return null;
        if (lineFormat.setLine(line)==0)
        {
            return new double[0];
        }
        values=lineFormat.getValues(values);
        for (int i=0; i<values.length; i++)
            if (i!=xColumn && i!=yColumn && values[i]==padValue)
                values[i]=Double.NaN;
        return values;
    }

    /**
     * Retourne les donn�es de l'image � l'index sp�cifi�. Si cette image avait d�j� �t� lue, ses
     * donn�es seront retourn�es imm�diatement.  Sinon, cette image sera lue ainsi que toutes les
     * images qui pr�c�dent <code>imageIndex</code> et qui n'avaient pas encore �t� lues. Que ces
     * images pr�c�dentes soient m�moris�es ou oubli�es d�pend de {@link #seekForwardOnly}.
     *
     * @param  imageIndex Index de l'image � lire.
     * @return Les donn�es de l'image. Cette m�thode ne retourne jamais <code>null</code>.
     * @throws IOException si une erreur est survenue lors de la lecture du flot,
     *         ou si des nombres n'�taient pas correctement format�s dans le flot.
     * @throws IndexOutOfBoundsException si l'index sp�cifi� est en dehors des
     *         limites permises ou si aucune image n'a �t� conserv�e � cet index.
     */
    private RecordList getRecords(final int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        if (imageIndex >= nextImageIndex)
        {
            processImageStarted(imageIndex);
            final BufferedReader reader = getReader();
            final long          origine = getStreamPosition(reader);
            final long           length = getStreamLength(nextImageIndex, imageIndex+1);
            long   nextProgressPosition = (origine>=0 && length>0) ? 0 : Long.MAX_VALUE;
            for (;nextImageIndex<=imageIndex; nextImageIndex++)
            {
                /*
                 * R�duit la consommation de m�moire des images pr�c�dentes. On ne r�duit
                 * pas celle de l'image courante,  puisque la plupart du temps le tableau
                 * sera bient�t d�truit de toute fa�on.
                 */
                if (seekForwardOnly) minIndex=nextImageIndex;
                if (nextImageIndex!=0 && data!=null)
                {
                    final RecordList records = data[nextImageIndex-1];
                    if (records!=null)
                    {
                        if (seekForwardOnly)
                            data[nextImageIndex-1]=null;
                        else records.trimToSize();
                    }
                }
                /*
                 * Proc�de � la lecture de chacune des lignes de donn�es. Que ces lignes
                 * soient m�moris�es ou pas d�pend de l'image que l'on est en train de
                 * d�coder ainsi que de la valeur de {@link #seekForwardOnly}.
                 */
                double[]    values = null;
                RecordList records = null;
                final boolean  keep = (nextImageIndex==imageIndex) || !seekForwardOnly;
                // Initialise temporary fields used by 'parseLine'.
                this.xColumn    = getColumnX   (nextImageIndex);
                this.yColumn    = getColumnY   (nextImageIndex);
                this.padValue   = getPadValue  (nextImageIndex);
                this.lineFormat = getLineFormat(nextImageIndex);
                try
                {
                    String line;
                    while ((line=reader.readLine())!=null)
                    {
                        values = parseLine(line, values);
                        if (values  ==  null) break;
                        if (values.length==0) continue;
                        if (keep)
                        {
                            if (records==null)
                            {
                                final int expectedLineCount = Math.max(8, Math.min(65536, Math.round(length / (expectedDatumLength*values.length))));
                                records = new RecordList(values.length, expectedLineCount);
                            }
                            records.add(values);
                        }
                        final long position = getStreamPosition(reader)-origine;
                        if (position >= nextProgressPosition)
                        {
                            processImageProgress(position * (100f/length));
                            nextProgressPosition = position + PROGRESS_INTERVAL;
                        }
                    }
                }
                catch (ParseException exception)
                {
                    throw new IIOException(getPositionString(exception.getLocalizedMessage()), exception);
                }
                /*
                 * Apr�s la lecture d'une image, v�rifie s'il y avait un nombre suffisant de lignes.
                 * Une exception sera lanc�e si l'image ne contenait pas au moins deux lignes. On
                 * ajustera ensuite le nombre moyens de caract�res par donn�es.
                 */
                if (records!=null)
                {
                    final int lineCount = records.getLineCount();
                    if (lineCount<2)
                    {
                        throw new IIOException(getPositionString(Resources.format(Cl�.FILE_HAS_TOO_FEW_DATA)));
                    }
                    if (data==null)
                    {
                        data = new RecordList[imageIndex+1];
                    }
                    else if (data.length <= imageIndex)
                    {
                        data = XArray.resize(data, imageIndex+1);
                    }
                    data[nextImageIndex] = records;
                    final float meanDatumLength = (getStreamPosition(reader)-origine) / (float)records.getDataCount();
                    if (meanDatumLength>0) expectedDatumLength = meanDatumLength;
                }
            }
            processImageComplete();
        }
        /*
         * Une fois les lectures termin�es, retourne les donn�es de l'image
         * demand�e. Une exception sera lanc�e si ces donn�es n'ont pas �t�
         * conserv�es.
         */
        if (data!=null && imageIndex<data.length)
        {
            final RecordList records = data[imageIndex];
            if (records!=null) return records;
        }
        throw new IndexOutOfBoundsException(String.valueOf(imageIndex));
    }

    /**
     * Proc�de � la lecture d'une image.
     *
     * @param  imageIndex Index de l'image � lire.
     * @param  param Param�tres de la lecture, ou <code>null</code> s'il n'y en a pas.
     * @return Image lue.
     * @throws IOException si l'op�ration a �chou�.
     */
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException
    {
        /*
         * Obtient quelques informations de base
         * sur l'image qui sera � d�coder.
         */
        final int        xColumn = getColumnX(imageIndex);
        final int        yColumn = getColumnY(imageIndex);
        final RecordList records = getRecords(imageIndex);
        final int          width = records.getPointCount(xColumn, EPS);
        final int         height = records.getPointCount(yColumn, EPS);
        final int       numBands = records.getColumnCount() - (xColumn==yColumn ? 1 : 2);
        /*
         * Obtient les coordonn�es en pixels de la r�gion � d�coder.  Le rectangle retourn� sera
         * toujours compris � l'int�rieur des limites de l'image. Il tient compte des param�tres
         * sp�cifi�s par l'utilisateur tel que le d�calage initial du "subsampling".
         */
        final Rectangle sourceRegion=getSourceRegion(param, width, height);
        /*
         * Obtient l'image dans laquelle �crire les donn�es. Cette image pourrait avoir
         * �t� sp�cifi�e explicitement par l'utilisateur. Dans ce cas, il faudra veiller
         * � nettoyer la r�gion de destination avant d'�crire dedans (au cas o� elle
         * contiendrait les restes d'une image plus ancienne).
         */
        final BufferedImage image=getDestination(param, getImageTypes(imageIndex), width, height);
        /*
         * V�rifie si les bandes demand�es par l'utilisateur sont compatibles avec le
         * nombre de bandes pr�sentes dans le fichier et pr�sentes dans l'image de destination.
         */
        checkReadParamBandSettings(param, numBands, image.getSampleModel().getNumBands());
        /*
         * Maintenant que l'on sait que les param�tres sont valides,
         * obtient l'objet {@link WritableRaster} dans lequel �crire.
         */
        final WritableRaster raster=image.getRaster();
        /*
         * Calcule quelques constantes qui serviront au d�codage.
         */
        final double      xmin = records.getMinimum(xColumn);
        final double      ymin = records.getMinimum(yColumn);
        final double      xmax = records.getMaximum(xColumn);
        final double      ymax = records.getMaximum(yColumn);
        final double    scaleX = (width -1)/(xmax-xmin);
        final double    scaleY = (height-1)/(ymax-ymin);
        final int   sourceXMin = sourceRegion.x;
        final int   sourceYMin = sourceRegion.y;
        final int   sourceXMax = sourceRegion.width  + sourceXMin;
        final int   sourceYMax = sourceRegion.height + sourceYMin;
        final int  rasterWidth = raster.getWidth();
        final int rasterHeigth = raster.getHeight();
        final int  columnCount = records.getColumnCount();
        final int    dataCount = records.getDataCount();
        final float[]     data = records.getData();
        /*
         * Obtient les param�tres sp�cifi�s par l'utilisateur.  Si aucun
         * param�tre n'a �t� d�fini, alors des valeurs par d�faut seront
         * utilis�es comme param�tres.
         */
        final int[] sourceBands;
        final int[] destBands;
        final int sourceXSubsampling;
        final int sourceYSubsampling;
        final int subsamplingXOffset;
        final int subsamplingYOffset;
        final int destinationXOffset;
        final int destinationYOffset;
        if (param != null)
        {
            sourceBands        = param.getSourceBands();
            destBands          = param.getDestinationBands();
            final Point offset = param.getDestinationOffset();
            sourceXSubsampling = param.getSourceXSubsampling();
            sourceYSubsampling = param.getSourceYSubsampling();
            subsamplingXOffset = param.getSubsamplingXOffset();
            subsamplingYOffset = param.getSubsamplingXOffset();
            destinationXOffset = offset.x;
            destinationYOffset = offset.y;
        }
        else
        {
            sourceBands = null;
            destBands   = null;
            sourceXSubsampling = 1;
            sourceYSubsampling = 1;
            subsamplingXOffset = 0;
            subsamplingYOffset = 0;
            destinationXOffset = 0;
            destinationYOffset = 0;
        }
        /*
         * Proc�de � la cr�ation de l'image. Si l'image a �t� sp�cifi�e explicitement par
         * l'utilisateur, alors il faut d'abord effacer toute la r�gion dans laquelle on
         * va �crire.
         */
        if (CLEAR && param!=null && param.getDestination()!=null)
        {
            final int[] sample=new int[raster.getNumBands()];
            final int maxX = Math.min(destinationXOffset +  sourceRegion.width/sourceXSubsampling, raster.getMinX()+raster.getWidth());
            final int maxY = Math.min(destinationYOffset + sourceRegion.height/sourceYSubsampling, raster.getMinY()+raster.getHeight());
            for (int y=destinationYOffset; y<maxY; y++)
                for (int x=destinationXOffset; x<maxX; x++)
                    raster.setPixel(x, y, sample);
        }
        for (int i=0; i<dataCount; i+=columnCount)
        {
            /*
             * A ce stade, nous disposons de toutes les valeurs d'une ligne du fichier ASCII.
             * On convertit maintenant la coordonn�e (x,y) logique en coordonn�e pixel. Cette
             * coordonn�e pixel se r�f�re � l'image "source";  elle ne se r�f�re pas encore �
             * l'image destination. Elle doit obligatoirement �tre enti�re. Plus loin, nous
             * tiendrons compte du "subsampling".
             */
            final double fx = (data[i+xColumn]-xmin)*scaleX; // (fx,fy) may be NaN: Use
            final double fy = (ymax-data[i+yColumn])*scaleY; // "!abs(...)<=EPS" below.
            int           x = (int)Math.round(fx); // This conversion is not the same than
            int           y = (int)Math.round(fy); // getTransform(), but it should be ok.
            if (!(Math.abs(x-fx)<=EPS)) {fireBadCoordinate(data[i+xColumn]); continue;}
            if (!(Math.abs(y-fy)<=EPS)) {fireBadCoordinate(data[i+yColumn]); continue;}
            if (x>=sourceXMin && x<sourceXMax && y>=sourceYMin && y<sourceYMax)
            {
                x -= subsamplingXOffset;
                y -= subsamplingYOffset;
                if ((x % sourceXSubsampling)==0 && (y % sourceYSubsampling)==0)
                {
                    x = x/sourceXSubsampling + (destinationXOffset-sourceXMin);
                    y = y/sourceYSubsampling + (destinationYOffset-sourceYMin);
                    if (x<rasterWidth && y<rasterHeigth)
                    {
                        for (int j=(sourceBands!=null) ? sourceBands.length : numBands; --j>=0;)
                        {
                            // TODO
                        }
                        int band = 0;
                        for (int j=0; j<columnCount; j++)
                        {
                            if (j!=xColumn && j!=yColumn)
                            {
                                // TODO: tenir compte de sourceBands.
                                raster.setSample(x, y, (destBands!=null) ? destBands[band] : band, data[i+j]);
                                band++;
                            }
                        }
                    }
                }
            }
        }
        return image;
    }

    /**
     * Pr�vient qu'une coordonn�e est mauvaise. Cette m�thode est appel�e lors de la lecture
     * s'il a �t� d�tect� qu'une coordonn�e est en dehors des limites pr�vues, ou qu'elle ne
     * correspond pas � des coordonn�es pixels enti�res.
     */
    private void fireBadCoordinate(final float coordinate)
    {processWarningOccurred(getPositionString(Resources.format(Cl�.BAD_COORDINATE�1, new Float(coordinate))));}

    /**
     * Supprime les donn�es de toutes les images
     * qui avait �t� conserv�es en m�moire.
     */
    private void clear()
    {
        data                = null;
        lineFormat          = null;
        nextImageIndex      = 0;
        expectedDatumLength = 10.4f;
        if (originatingProvider instanceof Spi)
        {
            final Spi provider = (Spi) originatingProvider;
            xColumn  = provider.xColumn;
            yColumn  = provider.yColumn;
            padValue = provider.padValue;
        }
        else
        {
            xColumn  = 0;
            yColumn  = 1;
            padValue = Double.NaN;
        }
    }

    /**
     * Replace le d�codeur dans son �tat initial.
     */
    public void reset()
    {
        clear();
        super.reset();
    }




    /**
     * Service provider interface (SPI) for {@link TextRecordImageReader}s.
     * This SPI provides all necessary implementations for creating default
     * {@link TextRecordImageReader}. Subclasses only have to set some fields
     * at construction time, e.g.:
     *
     * <blockquote><pre>
     * public final class CLSImageReaderSpi extends TextRecordImageReader.Spi
     * {
     *     public CLSImageReaderSpi()
     *     {
     *         super("CLS", "text/x-grid-CLS");
     *         {@link #vendorName vendorName} = "Institut de Recherche pour le D�veloppement";
     *         {@link #version    version}    = "1.0";
     *         {@link #locale     locale}     = Locale.US;
     *         {@link #charset    charset}    = Charset.forName("ISO-LATIN-1");
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
    public static class Spi extends TextImageReader.Spi
    {
        /**
         * Num�ro de colonne des <var>x</var>, compt� � partir de 0.
         * Par d�faut, on suppose que les <var>x</var> se trouvent
         * dans la premi�re colonne (colonne #0).
         *
         * @see TextRecordImageReader#getColumnX
         * @see TextRecordImageReader#parseLine
         */
        final int xColumn;

        /**
         * Num�ro de colonne des <var>y</var>, compt� � partir de 0.
         * Par d�faut, on suppose que les <var>y</var> se trouvent
         * dans la deuxi�me colonne (colonne #1).
         *
         * @see TextRecordImageReader#getColumnY
         * @see TextRecordImageReader#parseLine
         */
        final int yColumn;

        /**
         * Valeur par d�faut repr�sentant les donn�es manquantes, ou
         * {@link Double#NaN} s'il n'y en a pas.  Lors de la lecture
         * d'une image, toutes les occurences de cette valeur seront
         * remplac�es par {@link Double#NaN} dans toutes les colonnes
         * sauf les colonnes des <var>x</var> et des <var>y</var>.
         *
         * @see TextRecordImageReader#getPadValue
         * @see TextRecordImageReader#parseLine
         */
        protected double padValue = Double.NaN;

        /**
         * Conventions locales � utiliser pour lire les nombres.  Par exemple
         * la valeur {@link Locale#US} signifie que les nombres seront �crits
         * en utilisant le point comme s�parateur d�cimal (entre autres
         * conventions). La valeur <code>null</code> signifie qu'il faudra
         * utiliser les conventions locales par d�faut au moment ou une image
         * sera lue.
         *
         * @see TextRecordImageReader#getLineFormat
         * @see TextRecordImageReader#parseLine
         */
        protected Locale locale;

        /**
         * Construit un descripteur. Ce constructeur suppose que les <var>x</var>
         * et les <var>y</var> se trouvent dans les colonnes 0 et 1 respectivement.
         * Les autres param�tres sont initialis�s comme dans le constructeur de la
         * classe parente.
         *
         * @param name Nom de ce d�codeur, ou <code>null</code> pour ne
         *             pas initialiser le champ {@link #names names}.
         * @param mime Nom MIME de ce d�codeur, ou <code>null</code> pour ne
         *             pas initialiser le champ {@link #MIMETypes MIMETypes}.
         */
        public Spi(final String name, final String mime)
        {this(name, mime, 0, 1);}

        /**
         * Construit un descripteur.
         *
         * @param name Nom de ce d�codeur, ou <code>null</code> pour ne
         *             pas initialiser le champ {@link #names names}.
         * @param mime Nom MIME de ce d�codeur, ou <code>null</code> pour ne
         *             pas initialiser le champ {@link #MIMETypes MIMETypes}.
         * @param xColumn Num�ro de colonne des <var>x</var> (� partir de 0).
         * @param yColumn Num�ro de colonne des <var>y</var> (� partir de 0).
         */
        public Spi(final String name, final String mime, final int xColumn, final int yColumn)
        {
            super(name, mime);
            this.xColumn = xColumn;
            this.yColumn = yColumn;
            if (xColumn < 0) throw new IllegalArgumentException(Resources.format(Cl�.NEGATIVE_COLUMN�2, "x", new Integer(xColumn)));
            if (yColumn < 0) throw new IllegalArgumentException(Resources.format(Cl�.NEGATIVE_COLUMN�2, "y", new Integer(yColumn)));
            pluginClassName = "net.seas.image.io.TextRecordImageReader";
        }

        /**
         * Retourne une cha�ne de caract�re
         * donnant une description de ce d�codeur.
         *
         * @param locale Langue dans laquelle retourner la description.
         */
        public String getDescription(final Locale locale)
        {return Resources.getResources(locale).getString(Cl�.GRID_READER_DESCRIPTION);}

        /**
         * V�rifie si la ligne sp�cifi�e peut �tre d�cod�e.
         *
         * @param  line Une des premi�res lignes du flot � lire.
         * @return {@link Boolean#TRUE} si la ligne peut �tre d�cod�e, {@link Boolean#FALSE}
         *         si elle ne peut pas �tre d�cod�e ou <code>null</code> si on ne sait pas.
         *         Dans ce dernier cas, cette m�thode sera appel�e une nouvelle fois avec la
         *         ligne suivante.
         */
        protected Boolean canDecodeLine(final String line)
        {
            if (line.trim().length()!=0) try
            {
                final LineFormat reader = (locale!=null) ? new LineFormat(locale) : new LineFormat();
                if (reader.setLine(line) >= (xColumn==yColumn ? 2 : 3))
                {
                    return Boolean.TRUE;
                }
            }
            catch (ParseException exception)
            {
                return Boolean.FALSE;
            }
            return null;
        }

        /**
         * Retourne un nouveau d�codeur {@link TextRecordImageReader}.
         *
         * @param  extension Param�tres optionels, ou <code>null</code> pour
         *         utiliser les param�tres par d�faut.
         * @return Un nouveau d�codeur {@link TextRecordImageReader}.
         * @throws IIOException si la cr�ation du d�codeur a �chou�.
         */
        public ImageReader createReaderInstance(final Object extension) throws IIOException
        {return new TextRecordImageReader(this);}
    }
}