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

// OpenGIS dependencies (SEAGIS)
import net.seagis.cv.Category;
import net.seagis.cv.CategoryList;
import net.seagis.resources.Utilities;
import net.seagis.io.image.RawBinaryImageReadParam;

// Images
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.IndexColorModel;

// Décodeurs
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import javax.swing.event.EventListenerList;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

// Géométrie
import java.awt.Dimension;
import java.awt.Rectangle;

// Arborescence
import net.seas.awt.tree.MutableTreeNode;
import net.seas.awt.tree.DefaultMutableTreeNode;

// Divers
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import net.seas.util.XArray;
import javax.media.jai.util.Range;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Information sur un format. Un objet <code>FormatEntryImpl</code> correspond à un
 * enregistrement de la base de données de formats d'images. Ces informations sont
 * retournées par la méthode {@link FormatTable#getEntry}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class FormatEntryImpl implements FormatEntry, Serializable
{
    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
    private static final long serialVersionUID = -3074567810624000603L;

    /**
     * Liste des répertoires synchronisées. Voyez {@link #setSynchronizedDirectories}
     * pour une description plus détaillée. Cette liste est utilisée par {@link #getLock}
     * pour obtenir l'objet sur lequel se synchroniser lors des lectures d'images.
     */
    static File[] synchronizedDirectories=File.listRoots();

    /**
     * Numéro unique identifiant ce format
     * dans la base de données.
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
     * Extension (sans le point) des noms de fichier des images à lire.
     */
    final String extension;

    /**
     * Liste des catégories appartenant à ce format. Les éléments
     * de ce tableau doit correspondre dans l'ordre aux bandes
     * <code>[0,1,2...]</code> de l'image.
     */
    private final CategoryList[] categories;

    /**
     * <code>true</code> si les données lues représenteront déjà les
     * valeurs du paramètre géophysique,  ou <code>false</code> s'il
     * faut d'abord appliquer {@link CategoryList#toValue}.
     */
    public final boolean geophysics;

    /**
     * Objet à utiliser pour lire des images de ce format. Cet objet ne sera
     * créé que lors du premier appel de {@link #read},  puis réutilisé pour
     * tous les appels subséquents.
     */
    private transient ImageReader reader;

    /**
     * Objet {@link ImageEntry} qui est en train d'utiliser ce format pour lire une image.
     * Cette information est utilisée pour vérifier si un objet  {@link ImageEntry}  a le
     * droit d'annuler une lecture avec {@link #abort}.
     */
    private transient ImageEntry owner;

    /**
     * Drapeau mis à <code>true</code> si une lecture
     * de l'image était en cours et a été annulée.
     */
    private transient boolean aborted;

    /**
     * Construit une entrée représentant un format.
     *
     * @param ID         Numéro unique identifiant ce format.
     * @param name       Nom du format.
     * @param mimeType   Nom MIME du format (par exemple "image/png").
     * @param extension  Extension (sans le point) des noms de fichier
     *                   (par exemple "png").
     * @param geophysics <code>true</code> si les données lues représenteront déjà les
     *                   valeurs du paramètre géophysique,  ou <code>false</code> s'il
     *                   faut d'abord appliquer {@link CategoryList#toValue}.
     * @param categories Listes des catégories apparaissant dans ce format.
     *                   Il peut y avoir plusieurs ensembles de catégories
     *                   qui utilisent différentes bandes de l'image.
     */
    protected FormatEntryImpl(final int ID, final String name, final String mimeType, final String extension, final boolean geophysics, final CategoryList[] categories)
    {
        this.ID         = ID;
        this.name       = name.trim();
        this.mimeType   = mimeType.trim().intern();
        this.extension  = extension.trim().intern();
        this.geophysics = geophysics;
        this.categories = categories;
    }

    /**
     * Retourne le nom de cette entrée.
     */
    public String getName()
    {return name;}

    /**
     * Retourne un numéro unique identifiant cette entrée.
     */
    public int getID()
    {return ID;}

    /**
     * Retourne les listes de catégories {@link CategoryList} qui permettent
     * de décoder les valeurs des paramètres géophysiques. Cette méthode peut
     * retourner plusieurs objets {@link CategoryList}, un par bande.
     *
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public CategoryList[] getCategoryLists()
    {return getCategoryLists(null);}

    /**
     * Retourne les listes de catégories {@link CategoryList} qui permettent de
     * décoder les valeurs des paramètres géophysiques  des images lues par cet
     * objet. Cette méthode peut retourner plusieurs objets {@link CategoryList},
     * un par bande. De façon optionnelle, on peut spécifier à cette méthode les
     * paramètres {@link ImageReadParam} qui ont servit à lire une image (c'est-à-dire
     * les mêmes paramètres que ceux qui avaient été donnés à {@link #read}).
     * Cette méthode ne retournera alors que les listes de catégories pertinents
     * pour les bandes lues.
     *
     * @param param    Paramètres qui ont servit à lire l'image, ou
     *                 <code>null</code> pour les paramètres par défaut.
     */
    final CategoryList[] getCategoryLists(final ImageReadParam param)
    {
        int  bandCount = categories.length;
        int[] srcBands = null;
        int[] dstBands = null;
        if (param!=null)
        {
            srcBands = param.getSourceBands();
            dstBands = param.getDestinationBands();
            if (srcBands!=null && srcBands.length<bandCount) bandCount=srcBands.length;
            if (dstBands!=null && dstBands.length<bandCount) bandCount=dstBands.length;
        }
        final CategoryList[] categoryLists = new CategoryList[bandCount];
        /*
         * Recherche les objets 'CategoryList'  qui correspondent
         * aux bandes sources demandées. Ces objets seront placés
         * aux index des bandes de destination spécifiées.
         */
        for (int j=0; j<bandCount; j++)
        {
            final int srcBand = (srcBands!=null) ? srcBands[j] : j;
            final int dstBand = (dstBands!=null) ? dstBands[j] : j;
            categoryLists[dstBand] = categories[srcBand];
        }
        return categoryLists;
    }

    /**
     * Retourne l'objet à utiliser pour lire des images. Le lecteur retourné ne lira
     * que des images du format MIME spécifié au constructeur. Les méthodes qui appelent
     * <code>getImageReader</code> <u>doivent</u> appeler cette méthode et utiliser l'objet
     * {@link ImageReader} retourné à l'intérieur d'un bloc synchronisé sur cet objet
     * <code>FormatEntryImpl</code> (c'est-à-dire <code>this</code>).
     *
     * @return Le lecteur à utiliser pour lire les images de ce format.
     *         Cette méthode ne retourne jamais <code>null</code>.
     * @throws IIOException s'il n'y a pas d'objet {@link ImageReader}
     *         pour ce format.
     */
    private ImageReader getImageReader() throws IIOException
    {
        assert Thread.holdsLock(this);
        if (reader==null)
        {
            final Iterator readers=ImageIO.getImageReadersByMIMEType(mimeType);
            if (!readers.hasNext()) throw new IIOException(Resources.format(ResourceKeys.ERROR_NO_IMAGE_DECODER_$1, mimeType));
            reader = (ImageReader) readers.next();
        }
        return reader;
    }

    /**
     * Retourne un bloc de paramètres par défaut pour le format courant.
     *
     * @return Un bloc de paramètres par défaut.
     *         Cette méthode ne retourne jamais <code>null</code>.
     * @throws IIOException s'il n'y a pas d'objet {@link ImageReader}
     *         pour ce format.
     */
    final synchronized ImageReadParam getDefaultReadParam() throws IIOException
    {return getImageReader().getDefaultReadParam();}

    /**
     * Retourne l'objet sur lequel se synchroniser
     * pour la lecture du fichier spécifié.
     *
     * @param  file Fichier à lire.
     * @param  def  Objet par défaut sur lequel se synchroniser.
     * @return Objet sur lequel se synchroniser.
     */
    private static Object getLock(File file, final Object def) throws IOException
    {
        final File[] sync=synchronizedDirectories;
        file = file.getCanonicalFile();
        while (file!=null)
        {
            for (int i=0; i<sync.length; i++)
            {
                final File drive=sync[i];
                if (file.equals(drive)) return drive;
            }
            file = file.getParentFile();
        }
        return def;
    }

    /**
     * Procède à la lecture d'une image. Il est possible que l'image
     * soit lue non pas localement, mais plutôt à travers un réseau.
     *
     * @param  file Nom du fichier à lire. Ce nom peut désigner un
     *         fichier sur un serveur distant. Le nom du serveur
     *         aura été spécifié par {@link ImageTable#setServer}.
     * @param  imageIndex Index (à partir de 0) de l'image à lire.
     * @param  param Bloc de paramètre à utiliser pour la lecture.
     * @param  listeners Objets à informer des progrès de la lecture ainsi que des éventuels
     *         avertissements, ou <code>null</code> s'il n'y en a pas. Les objets qui ne sont
     *         pas de la classe {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}
     *         ne seront pas pris en compte. Cette méthode s'engage à ne pas modifier l'objet
     *         {@link EventListenerList} donné.
     * @param  warningListener Objet à informer des avertissement lors de la lecture.
     * @param  expected Dimension prévue de l'image.
     * @param  source Objet {@link ImageEntry} qui a demandé la lecture de l'image.
     *         Cette information sera utilisée par {@link #abort} pour vérifier si
     *         un l'objet {@link ImageEntry} qui demande l'annulation est celui qui
     *         est en train de lire l'image.
     * @return Image lue, ou <code>null</code> si la lecture de l'image a été annulée.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    synchronized RenderedImage read(final File              file,
                                    final int               imageIndex,
                                    final ImageReadParam    param,
                                    final EventListenerList listeners,
                                    final Dimension         expected,
                                    final ImageEntry        source) throws IOException
    {
        /*
         * Procède à la lecture de l'image.  Plusieurs lectures peuvent être démarrées
         * en parallèle par différents objets 'FormatEntryImpl', mais la méthode 'getLock'
         * peut imposer des restrictions sur les accès en parallèle au même lecteur.
         */
        synchronized (getLock(file, this))
        {
            Object           inputObject = null;
            ImageInputStream inputStream = null;
            /*
             * Obtient l'objet à utiliser comme source. Autant que possible,  on
             * essaira de donner un objet de type 'File' ou 'URL', ce qui permet
             * au décodeur d'utiliser la connection la plus appropriée pour eux.
             */
            final ImageReader reader = getImageReader();
            final ImageReaderSpi spi = reader.getOriginatingProvider();
            if (spi!=null)
            {
                final Class[] types = spi.getInputTypes();
                if (contains(types, File.class))
                {
                    inputObject = file;
                }
                else if (contains(types, URL.class)) try
                {
                    inputObject = file.toURL();
                }
                catch (MalformedURLException exception)
                {
                    // Ignore...
                }
            }
            if (inputObject==null)
            {
                inputObject = inputStream = ImageIO.createImageInputStream(file);
            }
            if (inputObject==null)
            {
                throw new FileNotFoundException(Resources.format(ResourceKeys.ERROR_FILE_NOT_FOUND_$1, file.getPath()));
            }
            /*
             * Configure maintenant le décodeur
             * et lance la lecture de l'image.
             */
            try
            {
                synchronized (getAbortLock())
                {
                    owner = source;
                    aborted = false;
                }
                if (listeners!=null)
                {
                    final Object[] list = listeners.getListenerList();
                    for (int i=0; i<list.length; i+=2)
                    {
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
                if (param instanceof RawBinaryImageReadParam)
                {
                    final RawBinaryImageReadParam rawParam = (RawBinaryImageReadParam) param;
                    if (rawParam.getStreamImageSize()==null)
                    {
                        rawParam.setStreamImageSize(expected);
                    }
                }
                else
                {
                    // Don't perform this check if we are reading a RAW image,
                    // since the RAW image doesn't know its size by itself.
                    checkSize(reader.getWidth(imageIndex), reader.getHeight(imageIndex), expected, file);
                }
                /*
                 * Read the file, close it in the
                 * "finally" block and returns.
                 */
                final RenderedImage image = reader.readAsRenderedImage(imageIndex, param);
                return !aborted ? image : null;
            }
            finally
            {
                synchronized (getAbortLock())
                {
                    owner = null;
                }
                reader.reset(); // Comprend "removeIIOReadProgressListener" et "setInput(null)".
                if (inputStream!=null) inputStream.close();
            }
        }
    }

    /**
     * Annule la lecture de l'image en appelant {@link ImageReader#abort}.
     * Cette méthode peut être appelée à partir de n'importe quel thread.
     *
     * @param source Objet qui appelle cette méthode. Cette information est
     *        utilisée pour vérifier si  l'appelant  est le propriétaire de
     *        l'image en cours de lecture.    Si ce n'est pas le cas, alors
     *        cette méthode <code>abort</code> ne fera rien.
     */
    final void abort(final ImageEntry source)
    {
        synchronized (getAbortLock())
        {
            if (owner==source)
            {
                aborted = true;
                final ImageReader reader=this.reader;
                if (reader!=null) reader.abort();
            }
        }
    }

    /**
     * Retourne un objet sur lequel se synchroniser pour les demandes d'annulation de
     * la lecture. On ne pourra pas se synchroniser sur <code>this</code>  puisque la
     * méthode <code>read(...)</code> est synchronisée sur <code>this</code>  pendant
     * toute la durée de la lecture de l'image. N'importe quel autre objet déclaré
     * <code>final</code> fait l'affaire.
     */
    private Object getAbortLock()
    {return mimeType;}

    /**
     * Indique si le tableau <code>array</code> contient au moins un
     * exemplaire de la classe <code>item</code> ou d'une super-classe.
     */
    private static boolean contains(final Class[] array, final Class item)
    {
        for (int i=0; i<array.length; i++)
            if (array[i].isAssignableFrom(item))
                return true;
        return false;
    }

    /**
     * Vérifie que la taille de l'image a bien la taille qui était déclarée
     * dans la base de données. Cette vérification sert uniquement à tenter
     * d'intercepter d'éventuelles erreurs qui se serait glissées dans la
     * base de données et/ou la copie d'images sur le disque.
     *
     * @param  imageWidth   Largeur de l'image.
     * @param  imageHeight  Hauteur de l'image.
     * @param  expected     Largeur et hauteur attendues.
     * @param  file         Nom du fichier de l'image à lire.
     * @throws IIOException si l'image n'a pas la largeur et hauteur attendue.
     */
    private static void checkSize(final int imageWidth, final int imageHeight, final Dimension expected, final File file) throws IIOException
    {
        if (expected.width!=imageWidth || expected.height!=imageHeight)
        {
            throw new IIOException(Resources.format(ResourceKeys.ERROR_IMAGE_SIZE_MISMATCH_$5, file.getName(),
                                   new Integer(    imageWidth), new Integer(    imageHeight),
                                   new Integer(expected.width), new Integer(expected.height)));
        }
    }

    /**
     * Retourne un code représentant cette entrée.
     */
    public int hashCode()
    {return ID;}

    /**
     * Indique si cette entrée est identique à l'entrée spécifiée.
     */
    public boolean equals(final Object o)
    {
        if (o instanceof FormatEntryImpl)
        {
            final FormatEntryImpl that = (FormatEntryImpl) o;
            return                  this.ID       == that.ID          &&
                   Utilities.equals(this.name,       that.name     )  &&
                   Utilities.equals(this.mimeType,   that.mimeType )  &&
                   Utilities.equals(this.extension,  that.extension)  &&
                      Arrays.equals(this.categories, that.categories) &&
                                    this.geophysics==that.geophysics;
        }
        return false;
    }

    /**
     * Retourne les catégories de ce format sous forme d'une arborescence.
     * La racine sera le nom du format, et les branches représenteront
     * les différentes bandes avec leurs catégories.
     */
    final MutableTreeNode getTree(final Locale locale)
    {
        final DefaultMutableTreeNode root = new TreeNode(this);
        for (int i=0; i<categories.length; i++)
        {
            final CategoryList categories = this.categories[i];
            final int       categoryCount = categories.size();
            final DefaultMutableTreeNode node = new TreeNode(categories, i, locale);
            for (int j=0; j<categoryCount; j++)
            {
                node.add(new TreeNode(categories.get(j), locale));
            }
            root.add(node);
        }
        return root;
    }

    /**
     * Retourne une chaîne de caractères représentant cette entrée.
     */
    private StringBuffer toString(final StringBuffer buffer)
    {
        buffer.append(name);
        buffer.append(" (");
        buffer.append(mimeType);
        buffer.append(')');
        return buffer;
    }

    /**
     * Retourne une chaîne de caractères représentant cette entrée.
     */
    public String toString()
    {
        StringBuffer buffer=new StringBuffer(40);
        buffer.append(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer=toString(buffer);
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Noeud apparaissant dans l'arborescence des formats
     * et de leurs bandes.  Ce noeud redéfinit la méthode
     * {@link #toString} pour retourner une chaîne adaptée
     * plutôt que <code>{@link #getUserObject}.toString()</code>.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class TreeNode extends DefaultMutableTreeNode
    {
        /**
         * Le texte à retourner par {@link #toString}.
         */
        private final String text;

        /**
         * Construit un noeud pour l'entrée spécifiée.
         */
        public TreeNode(final FormatEntryImpl entry)
        {
            super(entry);
            text = String.valueOf(entry.toString(new StringBuffer()));
        }

        /**
         * Construit un noeud pour la liste spécifiée. Ce constructeur ne
         * balaie pas les catégories contenues dans la liste spécifiée.
         */
        public TreeNode(final CategoryList categories, final int band, final Locale locale)
        {
            super(categories);
            text = categories.getName(locale);
        }

        /**
         * Construit un noeud pour la catégorie spécifiée.
         */
        public TreeNode(final Category category, final Locale locale)
        {
            super(category, false);
            final StringBuffer buffer = new StringBuffer();
            buffer.append('[');  append(buffer, category.lower);
            buffer.append(".."); append(buffer, category.upper-1); // Inclusive
            buffer.append("] ");
            buffer.append(category.getName(locale));
            text = buffer.toString();
        }

        /**
         * Append an integer with at least 3 digits.
         */
        private static void append(final StringBuffer buffer, final int value)
        {
            final String number = String.valueOf(value);
            for (int i=3-number.length(); --i>=0;)
                buffer.append('0');
            buffer.append(number);
        }

        /**
         * Retourne le texte de ce noeud.
         */
        public String toString()
        {return text;}
    }
}
