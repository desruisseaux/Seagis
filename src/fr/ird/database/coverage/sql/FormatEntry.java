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
package fr.ird.database.coverage.sql;

// Images
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.IndexColorModel;

// Image I/O
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import javax.swing.event.EventListenerList;

// Generic I/O
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

// Other J2SE dependencies
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.awt.Dimension;
import java.awt.Rectangle;

// JAI dependencies
import javax.media.jai.util.Range;

// Geotools dependencies
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Utilities;
import org.geotools.io.image.RawBinaryImageReadParam;
import org.geotools.gui.swing.tree.MutableTreeNode;
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// Seagis dependencies
import fr.ird.resources.XArray;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Information sur un format. Un objet <code>FormatEntry</code> correspond � un
 * enregistrement de la base de donn�es de formats d'images. Ces informations sont
 * retourn�es par la m�thode {@link FormatTable#getEntry}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class FormatEntry implements fr.ird.database.coverage.FormatEntry, Serializable {
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = -3074567810624000603L;

    /**
     * Liste des r�pertoires synchronis�es. Voyez {@link #setSynchronizedDirectories}
     * pour une description plus d�taill�e. Cette liste est utilis�e par {@link #getLock}
     * pour obtenir l'objet sur lequel se synchroniser lors des lectures d'images.
     */
    static File[] synchronizedDirectories = File.listRoots();

    /**
     * Images en cours de lecture. Les cl�s sont les objets {@link CoverageEntry} en attente
     * d'�tre lus, tandis que les valeurs sont {@link Boolean#TRUE} si la lecture est en
     * cours, ou {@link Boolean#FALSE} si elle est en attente.
     */
    private final Map<CoverageEntry,Boolean> enqueued =
            Collections.synchronizedMap(new IdentityHashMap<CoverageEntry,Boolean>());

    /**
     * Num�ro unique identifiant ce format dans la base de donn�es.
     */
    private final int ID;

    /**
     * Nom du format lisant les images.
     */
    private final String name;

    /**
     * Nom MIME du format lisant les images.
     */
    private final String mimeType;

    /**
     * Extension (sans le point) des noms de fichier des images � lire.
     */
    final String extension;

    /**
     * Liste des bandes appartenant � ce format. Les �l�ments
     * de ce tableau doit correspondre dans l'ordre aux bandes
     * <code>[0,1,2...]</code> de l'image.
     */
    private final SampleDimension[] bands;

    /**
     * <code>true</code> si les donn�es lues repr�senteront
     * d�j� les valeurs du param�tre g�ophysique.
     */
    public final boolean geophysics;

    /**
     * Objet � utiliser pour lire des images de ce format. Cet objet ne sera
     * cr�� que lors du premier appel de {@link #read},  puis r�utilis� pour
     * tous les appels subs�quents.
     */
    private transient ImageReader reader;

    /**
     * Construit une entr�e repr�sentant un format.
     *
     * @param ID         Num�ro unique identifiant ce format.
     * @param name       Nom du format.
     * @param mimeType   Nom MIME du format (par exemple "image/png").
     * @param extension  Extension (sans le point) des noms de fichier
     *                   (par exemple "png").
     * @param geophysics <code>true</code> si les donn�es lues repr�senteront d�j� les
     *                   valeurs du param�tre g�ophysique.
     * @param bands      Listes des bandes apparaissant dans ce format.
     */
    protected FormatEntry(final int     ID,
                          final String  name,
                          final String  mimeType,
                          final String  extension,
                          final boolean geophysics,
                          final SampleDimension[] bands)
    {
        this.ID         = ID;
        this.name       = name.trim();
        this.mimeType   = mimeType.trim().intern();
        this.extension  = extension.trim().intern();
        this.geophysics = geophysics;
        this.bands      = bands;
        for (int i=0; i<bands.length; i++) {
            bands[i] = bands[i].geophysics(geophysics);
        }
    }

    /**
     * Retourne un num�ro unique identifiant cette entr�e.
     */
    public int getID() {
        return ID;
    }

    /**
     * Retourne le nom de cette entr�e.
     */
    public String getName() {
        return name;
    }

    /**
     * Retourne la description de cette entr�e.
     */
    public String getRemarks() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public SampleDimension[] getSampleDimensions() {
        return getSampleDimensions(null);
    }

    /**
     * Retourne les bandes {@link SampleDimension} qui permettent de
     * d�coder les valeurs des param�tres g�ophysiques des images lues par cet
     * objet. Cette m�thode peut retourner plusieurs objets {@link SampleDimension},
     * un par bande. De fa�on optionnelle, on peut sp�cifier � cette m�thode les
     * param�tres {@link ImageReadParam} qui ont servit � lire une image (c'est-�-dire
     * les m�mes param�tres que ceux qui avaient �t� donn�s � {@link #read}).
     * Cette m�thode ne retournera alors que les listes de cat�gories pertinents
     * pour les bandes lues.
     *
     * @param param    Param�tres qui ont servit � lire l'image, ou
     *                 <code>null</code> pour les param�tres par d�faut.
     */
    final SampleDimension[] getSampleDimensions(final ImageReadParam param) {
        int  bandCount = bands.length;
        int[] srcBands = null;
        int[] dstBands = null;
        if (param != null) {
            srcBands = param.getSourceBands();
            dstBands = param.getDestinationBands();
            if (srcBands!=null && srcBands.length<bandCount) bandCount=srcBands.length;
            if (dstBands!=null && dstBands.length<bandCount) bandCount=dstBands.length;
        }
        final SampleDimension[] selectedBands = new SampleDimension[bandCount];
        /*
         * Recherche les objets 'SampleDimension' qui correspondent
         * aux bandes sources demand�es. Ces objets seront plac�s
         * aux index des bandes de destination sp�cifi�es.
         */
        for (int j=0; j<bandCount; j++) {
            final int srcBand = (srcBands!=null) ? srcBands[j] : j;
            final int dstBand = (dstBands!=null) ? dstBands[j] : j;
            selectedBands[dstBand] = bands[srcBand];
        }
        return selectedBands;
    }

    /**
     * Retourne l'objet � utiliser pour lire des images. Le lecteur retourn� ne lira
     * que des images du format MIME sp�cifi� au constructeur. Les m�thodes qui appelent
     * <code>getImageReader</code> <u>doivent</u> appeler cette m�thode et utiliser l'objet
     * {@link ImageReader} retourn� � l'int�rieur d'un bloc synchronis� sur cet objet
     * <code>FormatEntry</code> (c'est-�-dire <code>this</code>).
     *
     * @return Le lecteur � utiliser pour lire les images de ce format.
     *         Cette m�thode ne retourne jamais <code>null</code>.
     * @throws IIOException s'il n'y a pas d'objet {@link ImageReader}
     *         pour ce format.
     */
    private ImageReader getImageReader() throws IIOException {
        assert Thread.holdsLock(this);
        if (reader == null) {
            final Iterator readers = ImageIO.getImageReadersByMIMEType(mimeType);
            if (!readers.hasNext()) {
                throw new IIOException(Resources.format(ResourceKeys.ERROR_NO_IMAGE_DECODER_$1, mimeType));
            }
            reader = (ImageReader) readers.next();
        }
        return reader;
    }

    /**
     * Retourne un bloc de param�tres par d�faut pour le format courant.
     * Cette m�thode n'est appel�e que par {@link GridCoverageEntry#getGridCoverage}.
     * Note: cette m�thode <strong>doit</strong> �tre appel�e � partir d'un bloc
     * synchronis� sur <code>this</code>.
     *
     * @return Un bloc de param�tres par d�faut.
     *         Cette m�thode ne retourne jamais <code>null</code>.
     * @throws IIOException s'il n'y a pas d'objet {@link ImageReader}
     *         pour ce format.
     */
    final ImageReadParam getDefaultReadParam() throws IIOException {
        assert Thread.holdsLock(this);
        return getImageReader().getDefaultReadParam();
    }

    /**
     * Retourne l'objet sur lequel se synchroniser
     * pour la lecture du fichier sp�cifi�.
     *
     * @param  file Fichier � lire.
     * @param  def  Objet par d�faut sur lequel se synchroniser.
     * @return Objet sur lequel se synchroniser.
     */
    private static Object getLock(File file, final Object def) throws IOException {
        final File[] sync = synchronizedDirectories;
        file = file.getCanonicalFile();
        while (file != null) {
            for (int i=0; i<sync.length; i++) {
                final File drive=sync[i];
                if (file.equals(drive)) {
                    return drive;
                }
            }
            file = file.getParentFile();
        }
        return def;
    }

    /**
     * Proc�de � la lecture d'une image. Il est possible que l'image
     * soit lue non pas localement, mais plut�t � travers un r�seau.
     * Cette m�thode n'est appel�e que par {@link GridCoverageEntry#getGridCoverage}.
     * <br><br>
     * Note 1: cette m�thode <strong>doit</strong> �tre appel�e � partir d'un bloc
     * synchronis� sur <code>this</code>.
     * <br><br>
     * Note 2: La m�thode {@link #setReading} <strong>doit</strong> �tre appel�e
     *         avant et apr�s cette m�thode dans un bloc <code>try...finally</code>.
     *
     *
     * @param  file Nom du fichier � lire. Ce nom peut d�signer un fichier sur un serveur distant.
     * @param  imageIndex Index (� partir de 0) de l'image � lire.
     * @param  param Bloc de param�tre � utiliser pour la lecture.
     * @param  listeners Objets � informer des progr�s de la lecture ainsi que des �ventuels
     *         avertissements, ou <code>null</code> s'il n'y en a pas. Les objets qui ne sont
     *         pas de la classe {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}
     *         ne seront pas pris en compte. Cette m�thode s'engage � ne pas modifier l'objet
     *         {@link EventListenerList} donn�.
     * @param  warningListener Objet � informer des avertissement lors de la lecture.
     * @param  expected Dimension pr�vue de l'image.
     * @param  source Objet {@link CoverageEntry} qui a demand� la lecture de l'image.
     *         Cette information sera utilis�e par {@link #abort} pour v�rifier si
     *         un l'objet {@link CoverageEntry} qui demande l'annulation est celui qui
     *         est en train de lire l'image.
     * @return Image lue, ou <code>null</code> si la lecture de l'image a �t� annul�e.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    final RenderedImage read(final File              file,
                             final int               imageIndex,
                             final ImageReadParam    param,
                             final EventListenerList listeners,
                             final Dimension         expected,
                             final CoverageEntry     source) throws IOException
    {
        assert Thread.holdsLock(this);
        RenderedImage image = null;
        /*
         * Proc�de � la lecture de l'image.  Plusieurs lectures peuvent �tre d�marr�es
         * en parall�le par diff�rents objets 'FormatEntry', mais la m�thode 'getLock'
         * peut imposer des restrictions sur les acc�s en parall�le au m�me lecteur.
         */
        synchronized (getLock(file, this)) {
            if (!enqueued.containsKey(source)) {
                // Obtenir le 'lock' peut prendre du temps si d'autres lectures sont
                // en cours. L'utilisateur peut avoir annul� l'op�ration entre-temps.
                return null;
            }
            Object           inputObject = null;
            ImageInputStream inputStream = null;
            /*
             * Obtient l'objet � utiliser comme source. Autant que possible,  on
             * essaira de donner un objet de type 'File' ou 'URL', ce qui permet
             * au d�codeur d'utiliser la connection la plus appropri�e pour eux.
             */
            final ImageReader reader = getImageReader();
            final ImageReaderSpi spi = reader.getOriginatingProvider();
            if (spi != null) {
                final Class[] types = spi.getInputTypes();
                if (contains(types, File.class)) {
                    inputObject = file;
                } else if (contains(types, URL.class)) {
                    try {
                        inputObject = file.toURL();
                    } catch (MalformedURLException exception) {
                        // Ignore...
                    }
                }
            }
            if (inputObject == null) {
                inputObject = inputStream = ImageIO.createImageInputStream(file);
            }
            if (inputObject == null) {
                throw new FileNotFoundException(Resources.format(
                        ResourceKeys.ERROR_FILE_NOT_FOUND_$1, file.getPath()));
            }
            /*
             * Configure maintenant le d�codeur et lance la lecture de l'image.
             */
            try {
                if (listeners != null) {
                    final Object[] list = listeners.getListenerList();
                    for (int i=0; i<list.length; i+=2) {
                        if (list[i]==IIOReadWarningListener .class) reader.addIIOReadWarningListener ((IIOReadWarningListener)  list[i+1]);
                        if (list[i]==IIOReadProgressListener.class) reader.addIIOReadProgressListener((IIOReadProgressListener) list[i+1]);
                    }
                }
                reader.setInput(inputObject, true, true);
                /*
                 * If we are going to read a RAW binary file, set the expected
                 * image size. This is necessary since RAW binary files don't
                 * know their image's size.
                 */
                if (param instanceof RawBinaryImageReadParam) {
                    final RawBinaryImageReadParam rawParam = (RawBinaryImageReadParam) param;
                    if (rawParam.getStreamImageSize() == null) {
                        rawParam.setStreamImageSize(expected);
                    }
                    if (geophysics && rawParam.getDestinationType() == null) {
                        final int dataType = rawParam.getStreamDataType();
                        if (dataType != DataBuffer.TYPE_FLOAT &&
                            dataType != DataBuffer.TYPE_DOUBLE)
                        {
                            rawParam.setDestinationType(DataBuffer.TYPE_FLOAT);
                        }
                    }
                } else {
                    // Don't perform this check if we are reading a RAW image,
                    // since the RAW image doesn't know its size by itself.
                    checkSize(reader.getWidth(imageIndex), reader.getHeight(imageIndex),
                              expected, file);
                }
                /*
                 * Read the file, close it in the "finally" block and returns the image.
                 * The reading will not be performed if the user aborted it before we reach
                 * this point.
                 */
                if (enqueued.put(source, Boolean.TRUE) != null) {
                    image = reader.readAsRenderedImage(imageIndex, param);
                }
            } finally {
                if (enqueued.remove(source) == null) {
                    // User aborted the reading while it was in process.
                    image = null;
                }
                reader.reset(); // Comprend "removeIIOReadProgressListener" et "setInput(null)".
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        return image;
    }

    /**
     * <strong>Most</strong> be invoked before and after {@link #read}. The thread must
     * <strong>not</strong> hold the lock on <code>this</code>. This method should be
     * invoked in a <code>try...finally</code> clause as below:
     *
     * <blockquote><pre>
     * try {
     *     format.setReading(source, true);
     *     synchronized (format) {
     *         format.read(...);
     *     }
     * } finally {
     *     format.setReading(source, false);
     * }
     * </pre></blockquote>
     */
    final void setReading(final CoverageEntry source, final boolean starting) {
        assert !Thread.holdsLock(this); // The thread must *not* hold the lock.
        if (starting) {
            if (enqueued.put(source, Boolean.FALSE) != null) {
                throw new AssertionError();
            }
        } else {
            enqueued.remove(source);
        }
    }

    /**
     * Annule la lecture de l'image en appelant {@link ImageReader#abort}.
     * Cette m�thode peut �tre appel�e � partir de n'importe quel thread.
     *
     * @param source Objet qui appelle cette m�thode.
     */
    final void abort(final CoverageEntry source) {
        assert !Thread.holdsLock(this); // The thread must *not* hold the lock.
        final Boolean active;
        synchronized (enqueued) {
            active = enqueued.remove(source);
            if (Boolean.TRUE.equals(active)) {
                if (reader != null) {
                    reader.abort();
                }
            }
        }
        if (active != null) {
            final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE,
                        ResourceKeys.ABORT_IMAGE_READING_$2, source.getName(),
                        new Integer(active.booleanValue() ? 1 : 0));
            record.setSourceClassName("CoverageEntry");
            record.setSourceMethodName("abort");
            CoverageDataBase.LOGGER.log(record);
        }
    }

    /**
     * Indique si le tableau <code>array</code> contient au moins un
     * exemplaire de la classe <code>item</code> ou d'une super-classe.
     */
    private static boolean contains(final Class[] array, final Class item) {
        for (int i=0; i<array.length; i++) {
            if (array[i].isAssignableFrom(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * V�rifie que la taille de l'image a bien la taille qui �tait d�clar�e
     * dans la base de donn�es. Cette v�rification sert uniquement � tenter
     * d'intercepter d'�ventuelles erreurs qui se serait gliss�es dans la
     * base de donn�es et/ou la copie d'images sur le disque.
     *
     * @param  imageWidth   Largeur de l'image.
     * @param  imageHeight  Hauteur de l'image.
     * @param  expected     Largeur et hauteur attendues.
     * @param  file         Nom du fichier de l'image � lire.
     * @throws IIOException si l'image n'a pas la largeur et hauteur attendue.
     */
    private static void checkSize(final int imageWidth, final int imageHeight,
                                  final Dimension expected, final File file)
        throws IIOException
    {
        if (expected.width!=imageWidth || expected.height!=imageHeight) {
            throw new IIOException(Resources.format(ResourceKeys.ERROR_IMAGE_SIZE_MISMATCH_$5, file.getName(),
                                   new Integer(    imageWidth), new Integer(    imageHeight),
                                   new Integer(expected.width), new Integer(expected.height)));
        }
    }

    /**
     * Retourne un code repr�sentant cette entr�e.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Indique si cette entr�e est identique � l'entr�e sp�cifi�e.
     */
    public boolean equals(final Object o) {
        if (o instanceof FormatEntry) {
            final FormatEntry that = (FormatEntry) o;
            return                  this.ID       == that.ID          &&
                   Utilities.equals(this.name,       that.name     )  &&
                   Utilities.equals(this.mimeType,   that.mimeType )  &&
                   Utilities.equals(this.extension,  that.extension)  &&
                      Arrays.equals(this.bands,      that.bands    )  &&
                                    this.geophysics==that.geophysics;
        }
        return false;
    }

    /**
     * Retourne les cat�gories de ce format sous forme d'une arborescence.
     * La racine sera le nom du format, et les branches repr�senteront
     * les diff�rentes bandes avec leurs cat�gories.
     */
    final MutableTreeNode getTree(final Locale locale) {
        final DefaultMutableTreeNode root = new TreeNode(this);
        for (int i=0; i<bands.length; i++) {
            final SampleDimension band = bands[i];
            final List      categories = band.getCategories();
            final int    categoryCount = categories.size();
            final DefaultMutableTreeNode node = new TreeNode(band, locale);
            for (int j=0; j<categoryCount; j++) {
                node.add(new TreeNode((Category)categories.get(j), locale));
            }
            root.add(node);
        }
        return root;
    }

    /**
     * Retourne une cha�ne de caract�res repr�sentant cette entr�e.
     */
    private StringBuffer toString(final StringBuffer buffer) {
        buffer.append(name);
        buffer.append(" (");
        buffer.append(mimeType);
        buffer.append(')');
        return buffer;
    }

    /**
     * Retourne une cha�ne de caract�res repr�sentant cette entr�e.
     */
    public String toString() {
        StringBuffer buffer=new StringBuffer(40);
        buffer.append(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer=toString(buffer);
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Noeud apparaissant dans l'arborescence des formats
     * et de leurs bandes.  Ce noeud red�finit la m�thode
     * {@link #toString} pour retourner une cha�ne adapt�e
     * plut�t que <code>{@link #getUserObject}.toString()</code>.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class TreeNode extends DefaultMutableTreeNode {
        /**
         * Le texte � retourner par {@link #toString}.
         */
        private final String text;

        /**
         * Construit un noeud pour l'entr�e sp�cifi�e.
         */
        public TreeNode(final FormatEntry entry) {
            super(entry);
            text = String.valueOf(entry.toString(new StringBuffer()));
        }

        /**
         * Construit un noeud pour la liste sp�cifi�e. Ce constructeur ne
         * balaie pas les cat�gories contenues dans la liste sp�cifi�e.
         */
        public TreeNode(final SampleDimension band, final Locale locale) {
            super(band);
            text = band.getDescription(locale);
        }

        /**
         * Construit un noeud pour la cat�gorie sp�cifi�e.
         */
        public TreeNode(final Category category, final Locale locale) {
            super(category, false);
            final StringBuffer buffer = new StringBuffer();
            final Range range = category.geophysics(false).getRange();
            buffer.append('[');  append(buffer, range.getMinValue());
            buffer.append(".."); append(buffer, range.getMaxValue()); // Inclusive
            buffer.append("] ");
            buffer.append(category.getName(locale));
            text = buffer.toString();
        }

        /**
         * Append an integer with at least 3 digits.
         */
        private static void append(final StringBuffer buffer, final Comparable value) {
            final String number = String.valueOf(value);
            for (int i=3-number.length(); --i>=0;) {
                buffer.append('0');
            }
            buffer.append(number);
        }

        /**
         * Retourne le texte de ce noeud.
         */
        public String toString() {
            return text;
        }
    }
}
