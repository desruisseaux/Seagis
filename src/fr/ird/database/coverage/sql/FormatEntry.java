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
 */
package fr.ird.database.coverage.sql;

// Images
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

// Image I/O
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
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
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;

// Other J2SE dependencies
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import java.util.EventListener;
import java.util.IdentityHashMap;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.awt.Dimension;

// Java Advanced Imaging
import javax.media.jai.JAI;
import javax.media.jai.util.Range;
import com.sun.media.imageio.stream.RawImageInputStream;

// Geotools dependencies
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.tree.MutableTreeNode;
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;
import org.geotools.io.image.RawBinaryImageReadParam;

// Seagis dependencies
import fr.ird.resources.XArray;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Information sur un format. Un objet <code>FormatEntry</code> correspond ŕ un
 * enregistrement de la base de données de formats d'images. Ces informations sont
 * retournées par la méthode {@link FormatTable#getEntry}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class FormatEntry implements fr.ird.database.coverage.FormatEntry, Serializable {
    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
    private static final long serialVersionUID = -3074567810624000603L;

    /**
     * <code>true</code> pour utiliser l'opération "ImageRead" de JAI, ou <code>false</code>
     * pour utiliser directement l'objet {@link ImageReader}.
     */
    private static final boolean USE_IMAGE_READ_OPERATION = false;

    /**
     * Images en cours de lecture. Les clés sont les objets {@link CoverageEntry} en attente
     * d'ętre lus, tandis que les valeurs sont {@link Boolean#TRUE} si la lecture est en
     * cours, ou {@link Boolean#FALSE} si elle est en attente.
     */
    private final Map<CoverageEntry,Boolean> enqueued =
            Collections.synchronizedMap(new IdentityHashMap<CoverageEntry,Boolean>());

    /**
     * Nom du format lisant les images.
     */
    private final String name;

    /**
     * Nom MIME du format lisant les images.
     */
    private final String mimeType;

    /**
     * Extension (sans le point) des noms de fichier des images ŕ lire.
     */
    final String extension;

    /**
     * Liste des bandes appartenant ŕ ce format. Les éléments
     * de ce tableau doivent correspondre dans l'ordre aux bandes
     * <code>[0,1,2...]</code> de l'image.
     */
    private final SampleDimension[] bands;

    /**
     * <code>true</code> si les données lues représenteront
     * déjŕ les valeurs du paramčtre géophysique.
     */
    public final boolean geophysics;

    /**
     * Objet ŕ utiliser pour lire des images de ce format. Cet objet ne sera
     * créé que lors du premier appel de {@link #read},  puis réutilisé pour
     * tous les appels subséquents.
     */
    private transient ImageReader reader;

    /**
     * Construit une entrée représentant un format.
     *
     * @param name       Nom du format.
     * @param mimeType   Nom MIME du format (par exemple "image/png").
     * @param extension  Extension (sans le point) des noms de fichier
     *                   (par exemple "png").
     * @param geophysics <code>true</code> si les données lues représenteront déjŕ les
     *                   valeurs du paramčtre géophysique.
     * @param bands      Listes des bandes apparaissant dans ce format.
     */
    protected FormatEntry(final String  name,
                          final String  mimeType,
                          final String  extension,
                          final boolean geophysics,
                          final SampleDimension[] bands)
    {
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
     * Retourne le nom de cette entrée.
     */
    public String getName() {
        return name;
    }

    /**
     * Retourne la description de cette entrée.
     */
    public String getRemarks() {
        return null;
    }

    /**
     * La langue ŕ utiliser pour le décodeur d'image, ou <code>null</code> pour la langue
     * par défaut.
     */
    private static Locale getLocale() {
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
     * décoder les valeurs des paramčtres géophysiques des images lues par cet
     * objet. Cette méthode peut retourner plusieurs objets {@link SampleDimension},
     * un par bande. De façon optionnelle, on peut spécifier ŕ cette méthode les
     * paramčtres {@link ImageReadParam} qui ont servit ŕ lire une image (c'est-ŕ-dire
     * les męmes paramčtres que ceux qui avaient été donnés ŕ {@link #read}).
     * Cette méthode ne retournera alors que les listes de catégories pertinents
     * pour les bandes lues.
     *
     * @param param    Paramčtres qui ont servit ŕ lire l'image, ou
     *                 <code>null</code> pour les paramčtres par défaut.
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
         * aux bandes sources demandées. Ces objets seront placés
         * aux index des bandes de destination spécifiées.
         */
        for (int j=0; j<bandCount; j++) {
            final int srcBand = (srcBands!=null) ? srcBands[j] : j;
            final int dstBand = (dstBands!=null) ? dstBands[j] : j;
            selectedBands[dstBand] = bands[srcBand];
        }
        return selectedBands;
    }

    /**
     * Retourne l'objet ŕ utiliser pour lire des images. Le lecteur retourné ne lira
     * que des images du format MIME spécifié au constructeur. Les méthodes qui appelent
     * <code>getImageReader</code> <u>doivent</u> appeler cette méthode et utiliser l'objet
     * {@link ImageReader} retourné ŕ l'intérieur d'un bloc synchronisé sur cet objet
     * <code>FormatEntry</code> (c'est-ŕ-dire <code>this</code>).
     *
     * @return Le lecteur ŕ utiliser pour lire les images de ce format.
     *         Cette méthode ne retourne jamais <code>null</code>.
     * @throws IIOException s'il n'y a pas d'objet {@link ImageReader}
     *         pour ce format.
     */
    private ImageReader getImageReader() throws IIOException {
        assert Thread.holdsLock(this);
        if (reader != null) {
            return reader;
        }
        Iterator<ImageReader> readers;
        if (mimeType.length() != 0) {
            readers = ImageIO.getImageReadersByMIMEType(mimeType);
            if (readers.hasNext()) {
                return reader = readers.next();
            }
        }
        readers = ImageIO.getImageReadersByFormatName(extension);
        if (readers.hasNext()) {
            return reader = readers.next();
        }
        throw new IIOException(Resources.format(ResourceKeys.ERROR_NO_IMAGE_DECODER_$1, mimeType));
    }

    /**
     * Retourne un bloc de paramčtres par défaut pour le format courant.
     * Cette méthode n'est appelée que par {@link GridCoverageEntry#getGridCoverage}.
     * Note: cette méthode <strong>doit</strong> ętre appelée ŕ partir d'un bloc
     * synchronisé sur <code>this</code>.
     *
     * @return Un bloc de paramčtres par défaut.
     *         Cette méthode ne retourne jamais <code>null</code>.
     * @throws IIOException s'il n'y a pas d'objet {@link ImageReader}
     *         pour ce format.
     */
    final ImageReadParam getDefaultReadParam() throws IIOException {
        assert Thread.holdsLock(this);
        return getImageReader().getDefaultReadParam();
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
     * Convertit l'objet <code>input</code> spécifié en un des types spécifiés dans le
     * tableau <code>inputTypes</code>. Si la conversion ne peut pas ętre effectuée,
     * alors cette méthode retourne <code>null</code>.
     */
    private static Object getInput(final Object file, final Class[] inputTypes) {
        if (contains(inputTypes, file.getClass())) {
            return file;
        }
        if (contains(inputTypes, File.class)) try {
            if (file instanceof URI) {
                return new File((URI) file);
            }
            if (file instanceof URL) {
                return new File(((URL) file).toURI());
            }
        } catch (Exception exception) {
            // Ignore... Le code suivant sera un "fallback" raisonable.
        }
        if (contains(inputTypes, URL.class)) try {
            if (file instanceof File) {
                return ((File) file).toURL();
            }
            if (file instanceof URI) {
                return ((URI) file).toURL();
            }
        } catch (MalformedURLException exception) {
            // Ignore... Le code suivant sera un "fallback" raisonable.
        }
        if (contains(inputTypes, URI.class)) try {
            if (file instanceof File) {
                return ((File) file).toURI();
            }
            if (file instanceof URL) {
                return ((URL) file).toURI();
            }
        } catch (URISyntaxException exception) {
            // Ignore... Le code suivant sera un "fallback" raisonable.
        }
        return null;
    }

    /**
     * Procčde ŕ la lecture d'une image. Il est possible que l'image
     * soit lue non pas localement, mais plutôt ŕ travers un réseau.
     * Cette méthode n'est appelée que par {@link GridCoverageEntry#getGridCoverage}.
     * <br><br>
     * Note 1: cette méthode <strong>doit</strong> ętre appelée ŕ partir d'un bloc
     * synchronisé sur <code>this</code>.
     * <br><br>
     * Note 2: La méthode {@link #setReading} <strong>doit</strong> ętre appelée
     *         avant et aprčs cette méthode dans un bloc <code>try...finally</code>.
     *
     *
     * @param  file Fichier ŕ lire. Habituellement un objet {@link File}, {@link URL} ou {@link URI}.
     * @param  imageIndex Index (ŕ partir de 0) de l'image ŕ lire.
     * @param  param Bloc de paramčtre ŕ utiliser pour la lecture.
     * @param  listenerList Objets ŕ informer des progrčs de la lecture ainsi que des éventuels
     *         avertissements, ou <code>null</code> s'il n'y en a pas. Les objets qui ne sont
     *         pas de la classe {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}
     *         ne seront pas pris en compte. Cette méthode s'engage ŕ ne pas modifier l'objet
     *         {@link EventListenerList} donné.
     * @param  expected Dimension prévue de l'image.
     * @param  source Objet {@link CoverageEntry} qui a demandé la lecture de l'image.
     *         Cette information sera utilisée par {@link #abort} pour vérifier si
     *         un l'objet {@link CoverageEntry} qui demande l'annulation est celui qui
     *         est en train de lire l'image.
     * @return Image lue, ou <code>null</code> si la lecture de l'image a été annulée.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    final RenderedImage read(final Object            file,
                             final int               imageIndex,
                             final ImageReadParam    param,
                             final EventListenerList listenerList,
                             final Dimension         expected,
                             final CoverageEntry     source) throws IOException
    {
        assert Thread.holdsLock(this);
        RenderedImage    image       = null;
        ImageInputStream inputStream = null;
        Object           inputObject;
        /*
         * Obtient l'objet ŕ utiliser comme source. Autant que possible,  on
         * essaira de donner un objet de type 'File' ou 'URL', ce qui permet
         * au décodeur d'utiliser la connection la plus appropriée pour eux.
         */
        final ImageReader reader = getImageReader();
        final ImageReaderSpi spi = reader.getOriginatingProvider();
        final Class[] inputTypes = (spi!=null) ? spi.getInputTypes() : ImageReaderSpi.STANDARD_INPUT_TYPE;
        inputObject = getInput(file, inputTypes);
        if (inputObject == null) {
            inputObject = inputStream = ImageIO.createImageInputStream(file);
            if (inputObject == null) {
                throw new FileNotFoundException(Resources.format(
                        ResourceKeys.ERROR_FILE_NOT_FOUND_$1, getPath(file)));
            }
        }
        /*
         * Si l'image ŕ lire est au format "RAW", définit la taille de l'image.  C'est
         * nécessaire puisque le format binaire RAW ne contient aucune information sur
         * la taille des images qu'elle contient.
         */
        if (inputStream!=null && contains(inputTypes, RawImageInputStream.class)) {
            final SampleDimension[] bands = getSampleDimensions(param);
            final ColorModel  cm = bands[0].getColorModel(0, bands.length);
            final SampleModel sm = cm.createCompatibleSampleModel(expected.width, expected.height);
            inputObject = inputStream = new RawImageInputStream(inputStream,
                                                                new ImageTypeSpecifier(cm, sm),
                                                                new long[]{0},
                                                                new Dimension[]{expected});
        }
        // Patch temporaire, en attendant que les décodeurs spéciaux (e.g. "image/raw-msla")
        // soient adaptés ŕ l'architecture du décodeur RAW de Sun.
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
        }
        /*
         * Configure maintenant le décodeur et lance la lecture de l'image.
         * Cette étape existe en deux versions: avec utilisation de l'opération
         * "ImageRead", ou lecture directe ŕ partir du ImageReader.
         */
        if (USE_IMAGE_READ_OPERATION) {
            /*
             * Utilisation de l'opération "ImageRead": cette approche retarde la lecture des
             * tuiles ŕ un moment indéterminé aprčs l'appel de cette méthode. Elle a l'avantage
             * de contrôler la mémoire consommée grâce au TileCache de JAI, Mais elle rend plus
             * difficile la gestion des exceptions et l'annulation de la lecture avec 'abort()',
             * ce qui rend caduc la queue 'enqueued'.
             */
            EventListener[] listeners = null;
            if (listenerList != null) {
                int   count = 0;
                final Object[] list = listenerList.getListenerList();
                listeners = new EventListener[list.length/2];
           add: for (int i=1; i<list.length; i+=2) {
                    final EventListener candidate = (EventListener)list[i];
                    for (int j=count; --j>=0;) {
                        if (listeners[j] == candidate) {
                            continue add;
                        }
                    }
                    listeners[count++] = candidate;
                }
                listeners = XArray.resize(listeners, count);
            }
            image = JAI.create("ImageRead", new ParameterBlock()
                .add(inputObject)     // Objet ŕ utiliser en entré
                .add(imageIndex)      // Index de l'image ŕ lire
                .add(Boolean.FALSE)   // Pas de lecture des méta-données
                .add(Boolean.FALSE)   // Pas de lecture des "thumbnails"
                .add(Boolean.TRUE)    // Vérifier la validité de "input"
                .add(listeners)       // Liste des "listener"
                .add(getLocale())     // Langue du décodeur
                .add(param)           // Les paramčtres
                .add(reader));        // L'objet ŕ utiliser pour la lecture.
            this.reader = null;       // N'utilise que un ImageReader par opération.
        } else try {
            /*
             * Utilisation direct du 'ImageReader': cette approche lit l'image immédiatement,
             * ce qui facilite la gestion des exceptions, de l'anulation de la lecture avec
             * 'abort()' les synchronisations.
             */
            if (listenerList != null) {
                final Object[] list = listenerList.getListenerList();
                for (int i=0; i<list.length; i+=2) {
                    if (list[i] == IIOReadWarningListener.class) {
                        reader.addIIOReadWarningListener((IIOReadWarningListener) list[i+1]);
                    }
                    if (list[i] == IIOReadProgressListener.class) {
                        reader.addIIOReadProgressListener((IIOReadProgressListener) list[i+1]);
                    }
                }
            }
            reader.setLocale(getLocale());
            reader.setInput(inputObject, true, true);
            if (!(param instanceof RawBinaryImageReadParam)) {
                checkSize(reader.getWidth(imageIndex), reader.getHeight(imageIndex), expected, file);
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
     * Cette méthode peut ętre appelée ŕ partir de n'importe quel thread.
     *
     * @param source Objet qui appelle cette méthode.
     */
    final void abort(final CoverageEntry source) throws RemoteException {
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
            String name = source.getName();
            final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE,
                        ResourceKeys.ABORT_IMAGE_READING_$2, name,
                        new Integer(active.booleanValue() ? 1 : 0));
            record.setSourceClassName("CoverageEntry");
            record.setSourceMethodName("abort");
            CoverageDataBase.LOGGER.log(record);
        }
    }

    /**
     * Vérifie que la taille de l'image a bien la taille qui était déclarée
     * dans la base de données. Cette vérification sert uniquement ŕ tenter
     * d'intercepter d'éventuelles erreurs qui se serait glissées dans la
     * base de données et/ou la copie d'images sur le disque.
     *
     * @param  imageWidth   Largeur de l'image.
     * @param  imageHeight  Hauteur de l'image.
     * @param  expected     Largeur et hauteur attendues.
     * @param  file         Nom du fichier de l'image ŕ lire.
     * @throws IIOException si l'image n'a pas la largeur et hauteur attendue.
     */
    private static void checkSize(final int imageWidth, final int imageHeight,
                                  final Dimension expected, final Object file)
        throws IIOException
    {
        if (expected.width!=imageWidth || expected.height!=imageHeight) {
            throw new IIOException(Resources.format(ResourceKeys.ERROR_IMAGE_SIZE_MISMATCH_$5, getPath(file),
                                   new Integer(    imageWidth), new Integer(    imageHeight),
                                   new Integer(expected.width), new Integer(expected.height)));
        }
    }

    /**
     * Returns the path component of the specified input file.
     * The specified object is usually a {@link File}, {@link URL} or {@link URI} object.
     */
    private static String getPath(final Object file) {
        if (file instanceof File) {
            return ((File) file).getPath();
        } else if (file instanceof URL) {
            return ((URL) file).getPath();
        } else if (file instanceof URI) {
            return ((URI) file).getPath();
        } else {
            return file.toString();
        }
    }

    /**
     * Retourne un code représentant cette entrée.
     */
    public int hashCode() {
        return (int)serialVersionUID ^ name.hashCode();
    }

    /**
     * Indique si cette entrée est identique ŕ l'entrée spécifiée.
     */
    public boolean equals(final Object o) {
        if (o instanceof FormatEntry) {
            final FormatEntry that = (FormatEntry) o;
            return Utilities.equals(this.name,       that.name     )  &&
                   Utilities.equals(this.mimeType,   that.mimeType )  &&
                   Utilities.equals(this.extension,  that.extension)  &&
                      Arrays.equals(this.bands,      that.bands    )  &&
                                    this.geophysics==that.geophysics;
        }
        return false;
    }

    /**
     * Retourne les catégories de ce format sous forme d'une arborescence.
     * La racine sera le nom du format, et les branches représenteront
     * les différentes bandes avec leurs catégories.
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
     * Retourne une chaîne de caractčres représentant cette entrée.
     */
    final StringBuilder toString(final StringBuilder buffer) {
        buffer.append(name);
        buffer.append(" (");
        buffer.append(mimeType);
        buffer.append(')');
        return buffer;
    }

    /**
     * Retourne une chaîne de caractčres représentant cette entrée.
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder(40);
        buffer.append(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer = toString(buffer);
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Noeud apparaissant dans l'arborescence des formats
     * et de leurs bandes.  Ce noeud redéfinit la méthode
     * {@link #toString} pour retourner une chaîne adaptée
     * plutôt que <code>{@link #getUserObject}.toString()</code>.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class TreeNode extends DefaultMutableTreeNode {
        /**
         * Le texte ŕ retourner par {@link #toString}.
         */
        private final String text;

        /**
         * Construit un noeud pour l'entrée spécifiée.
         */
        public TreeNode(final FormatEntry entry) {
            super(entry);
            text = String.valueOf(entry.toString(new StringBuilder()));
        }

        /**
         * Construit un noeud pour la liste spécifiée. Ce constructeur ne
         * balaie pas les catégories contenues dans la liste spécifiée.
         */
        public TreeNode(final SampleDimension band, final Locale locale) {
            super(band);
            text = band.getDescription(locale);
        }

        /**
         * Construit un noeud pour la catégorie spécifiée.
         */
        public TreeNode(final Category category, final Locale locale) {
            super(category, false);
            final StringBuilder buffer = new StringBuilder();
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
        private static void append(final StringBuilder buffer, final Comparable value) {
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
