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
package fr.ird.database.gui.swing;

// Collections
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Comparator;

// Other J2SE dependencies
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.IIOException;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.swing.filechooser.FileFilter;

// Geotools dependencies
import org.geotools.resources.Utilities;


/**
 * Filtre des fichiers en fonction du type d'images désiré.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageFileFilter extends FileFilter {
    /**
     * Format d'image de ce filtre.
     */
    private final ImageReaderWriterSpi spi;

    /**
     * Nom et extensions de ce format d'image.
     */
    private final String name;

    /**
     * Liste des extensions des fichiers qu'accepte ce filtre, ou <code>null</code>
     * si les extensions ne sont pas connues. Ces extensions ne devraient pas commencer
     * par de point ('.').
     */
    private final String[] suffix;

    /**
     * Construit un filtre d'images.
     *
     * @param spi    Objet décrivant un format d'image.
     * @param name   Un des noms du format.
     * @param suffix Liste des extensions des fichiers qu'accepte ce filtre.
     *               Ces extensions ne devraient pas commencer par le point ('.').
     */
    private ImageFileFilter(final ImageReaderWriterSpi spi, final String name, final String[] suffix) {
        this.spi    = spi;
        this.suffix = suffix;
        if (suffix!=null && suffix.length>=1) {
            final String separator = System.getProperty("path.separator", ":");
            final StringBuffer buffer = new StringBuffer(name);
            buffer.append(" (");
            for (int i=0; i<suffix.length; i++) {
                if (suffix[i].startsWith(".")) {
                    suffix[i] = suffix[i].substring(1);
                }
                if (i != 0) {
                    buffer.append(separator);
                }
                buffer.append("*.");
                buffer.append(suffix[i]);
            }
            buffer.append(')');
            this.name = buffer.toString();
        } else {
            this.name = name;
        }
    }

    /**
     * Retourne une liste de filtres pour les lecture d'images. Les éléments de la liste apparaîtront dans
     * l'ordre alphabétique de leur description, en ignorant les différences entre majuscules et minuscules.
     *
     * @param locale Langue dans laquelle retourner les descriptions des filtres,
     *               ou <code>null</code> pour utiliser les conventions locales.
     */
    public static ImageFileFilter[] getReaderFilters(final Locale locale) {
        return getFilters(ImageReaderSpi.class, locale);
    }

    /**
     * Retourne une liste de filtres pour les écritures d'images. Les éléments de la liste apparaîtront dans
     * l'ordre alphabétique de leur description, en ignorant les différences entre majuscules et minuscules.
     *
     * @param locale Langue dans laquelle retourner les descriptions des filtres,
     *               ou <code>null</code> pour utiliser les conventions locales.
     */
    public static ImageFileFilter[] getWriterFilters(final Locale locale) {
        return getFilters(ImageWriterSpi.class, locale);
    }

    /**
     * Retourne une liste de filtres d'images. Les éléments de la liste apparaîtront dans l'ordre
     * alphabétique de leur description, en ignorant les différences entre majuscules et minuscules.
     *
     * @param category Catégorie des filtres désirés (lecture ou écriture).
     * @param loc Langue dans laquelle retourner les descriptions des filtres,
     *            ou <code>null</code> pour utiliser les conventions locales.
     */
    private static ImageFileFilter[] getFilters(final Class category, final Locale loc) {
        final Locale locale = (loc!=null) ? loc : Locale.getDefault();
        final List<ImageFileFilter> set = new ArrayList<ImageFileFilter>();
        for (final Iterator<ImageReaderWriterSpi> it=IIORegistry.getDefaultInstance().getServiceProviders(category, false); it.hasNext();) {
            final ImageReaderWriterSpi spi = it.next();
            final String       description = spi.getDescription(locale);
            final String[]          suffix = spi.getFileSuffixes();
            set.add(new ImageFileFilter(spi, description, suffix));
        }
        final ImageFileFilter[] array = set.toArray(new ImageFileFilter[set.size()]);
        Arrays.sort(array, new Comparator<ImageFileFilter>() {
            public int compare(final ImageFileFilter a, final ImageFileFilter b) {
                return a.name.toLowerCase(locale).compareTo(b.name.toLowerCase(locale));
            }
        });
        return array;
    }

    /**
     * Construit et retourne un objet qui lira les images dans le format de ce filtre.
     * Cette méthode ne peut ętre appelée que si ce filtre a été construit par un appel
     * ŕ {@link #getReaderFilters}.
     *
     * @return Un décodeur ŕ utiliser pour lire les images.
     * @param  IOException si le décodeur n'a pas pu ętre construit.
     */
    public ImageReader getImageReader() throws IOException {
        if (spi instanceof ImageReaderSpi) {
            return ((ImageReaderSpi) spi).createReaderInstance();
        } else {
            throw new IIOException(spi.toString());
        }
    }

    /**
     * Construit et retourne un objet qui écrira les images dans le format de ce filtre.
     * Cette méthode ne peut ętre appelée que si ce filtre a été construit par un appel
     * ŕ {@link #getWriterFilters}.
     *
     * @return Un codeur ŕ utiliser pour écrire les images.
     * @param  IOException si le codeur n'a pas pu ętre construit.
     */
    public ImageWriter getImageWriter() throws IOException {
        if (spi instanceof ImageWriterSpi) {
            return ((ImageWriterSpi) spi).createWriterInstance();
        } else {
            throw new IIOException(spi.toString());
        }
    }

    /**
     * Retourne une extension par défaut pour les noms de fichiers
     * de ce format d'image. La chaîne retournée ne commencera pas
     * par un point.
     *
     * @return L'extension, ou <code>null</code> si l'extension n'est pas connue.
     */
    public String getExtension() {
        String ext = null;
        if (suffix != null) {
            int length = -1;
            for (int i=suffix.length; --i>=0;) {
                String cmp = suffix[i];
                final int cmpl = cmp.length();
                if (cmpl > length) {
                    length = cmpl;
                    ext = cmp;
                }
            }
        }
        return ext;
    }

    /**
     * Indique si ce filtre accepte le fichier spécifié.
     */
    public boolean accept(final File file) {
        if (file != null) {
            if (suffix == null) {
                return true;
            }
            final String filename = file.getName();
            final int length = filename.length();
            if (length>0 && filename.charAt(0)!='.') {
                if (file.isDirectory()) {
                    return true;
                }
                int i = filename.lastIndexOf('.');
                if (i>0 && i<length-1) {
                    final String extension = filename.substring(i);
                    for (int j=suffix.length; --j>=0;) {
                        if (suffix[j].equalsIgnoreCase(extension)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Retourne la description de ce filtre. La description comprendra le
     * nom du format des images acceptées ainsi que leurs extensions.
     */
    public String getDescription() {
        return name;
    }

    /**
     * Retourne une chaîne de caractčres décrivant ce filtre.
     * Cette information ne sert qu'ŕ des fins de déboguage.
     */
    public String toString() {
        return Utilities.getShortClassName(this)+'['+name+']';
    }

    /**
     * Envoie vers le périphérique de sortie standard une
     * liste des filtres disponibles par défaut. La liste
     * est construites ŕ partir des encodeurs et décodeurs
     * fournit sur le systčme.
     */
    public static void main(final String[] args) {
        final ImageFileFilter[] filters = getReaderFilters(null);
        for (int i=0; i<filters.length; i++) {
            System.out.println(filters[i]);
        }
    }
}
