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
package fr.ird.animat.impl;

// J2SE
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import java.util.Set;
import java.util.Arrays;
import java.util.Locale;
import java.util.HashSet;
import java.io.Serializable;
import javax.swing.ImageIcon;
import javax.vecmath.MismatchedSizeException;

// Divers
import org.geotools.resources.Utilities;


/**
 * Implémentation par défaut d'une espèce animale.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Species implements fr.ird.animat.Species, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 1428323484284560694L;

    /**
     * Liste des paramètres par défaut observés par les animaux de cette espèce.
     */
    private static final Parameter[] DEFAULT_PARAMETERS = new Parameter[] {
        Parameter.HEADING
    };

    /**
     * Liste des index par défaut auxquels trouver les valeurs des observations
     * dans le tableau {@link Observations#observations} pour chaque paramètre.
     */
    private static final int[] DEFAULT_OFFSETS = new int[] {
        0, Observations.LOCATED_LENGTH
    };

    /**
     * Largeur par défaut des icônes, en pixels.
     */
    private static final int WIDTH = 20;

    /**
     * Hauteur par défaut des icônes, en pixels.
     */
    private static final int HEIGHT = 10;

    /**
     * Langues dans lesquelles un nom d'espèce est disponible.
     */
    private final Locale[] locales;

    /**
     * Nom de cette espèce dans une des langues de {@link #locales}.
     */
    private final String[] names;

    /**
     * Couleur par défaut des icône.
     */
    private final Color defaultColor;

    /**
     * Liste des paramètres observés par les animaux de cette espèce.
     */
    final Parameter[] parameters = DEFAULT_PARAMETERS;

    /**
     * Liste des index auxquels trouver les valeurs des observations dans le tableau
     * <code>Observations.observations</code> pour chaque paramètre.  La longueur de
     * ce tableau doit être égale à la longueur du tableau <code>parameters</code>
     * plus 1.
     */
    final int[] observationOffsets = DEFAULT_OFFSETS;

    /**
     * Construit une nouvelle espèce. Le nom de cette espèce peut être exprimé
     * selon plusieurs langues. A chaque nom ({@link String}) est associé une
     * langue ({@link Locale}).
     *
     * @param locales Langues des noms de cette espèces. Ce tableau doit avoir
     *        la même longueur que l'argument <code>names</code>. <strong>NOTE:
     *        Ce tableau n'est pas cloné</strong>.  Evitez donc de le modifier
     *        après la construction.
     * @param names  Nom de cette espèce selon chacune des langues énumérées dans
     *        l'argument <code>locales</code>. <strong>NOTE: Ce tableau n'est pas
     *        cloné</strong>. Evitez donc de le modifier après la construction.
     * @param color Couleur par défaut à utiliser pour les icônes représentant cette
     *        espèce.
     *
     * @throws IllegalArgumentException Si un des éléments du tableau
     *         <code>locales</code> apparaît plus d'une fois.
     */
    public Species(final Locale[] locales,
                   final String[] names,
                   final Color    color)
            throws IllegalArgumentException
    {
        this.locales = locales;
        this.names   = names;
        if (locales.length != names.length) {
            throw new MismatchedSizeException();
        }
        final Set<Locale> set = new HashSet<Locale>();
        for (int i=0; i<locales.length; i++) {
            if (!set.add(locales[i])) {// Accepte null
                throw new IllegalArgumentException();
            }
            if (names[i] == null) {
                throw new NullPointerException();
            }
        }
        defaultColor = color;
    }

    /**
     * Retourne la longueur des enregistrements correspondant aux observations faites par
     * les animaux de cette espèce. Il s'agit de la longueur qu'aura le tableau de type
     * <code>float[]</code> qui contiendra les données d'observations.
     */
    final int getRecordLength() {
        return observationOffsets[parameters.length];
    }

    /**
     * Retourne les langues dans lesquelles peuvent
     * être exprimées le nom de cette espèce.
     */
    public Locale[] getLocales() {
        return (Locale[]) locales.clone();
    }

    /**
     * Retourne le nom de cette espèce dans la langue spécifiée. Si aucun
     * nom n'est disponible dans cette langue, retourne <code>null</code>.
     */
    public String getName(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
            if (locale != null) {
                final String name = getName(locale);
                if (name != null) {
                    return name;
                }
                for (int i=0; i<names.length; i++) {
                    if (locales[i] != null) {
                        return names[i];
                    }
                }
                return names.length!=0 ? names[0] : null;
            }
        }
        for (int i=0; i<locales.length; i++) {
            if (Utilities.equals(locale, locales[i])) {
                return names[i];
            }
        }
        if (locale!=null && locale!=FAO) {
            final String language = locale.getLanguage();
            if (language.length() != 0) {
                for (int i=0; i<locales.length; i++) {
                    if (locales[i]!=null) {
                        if (language.equals(locales[i].getLanguage())) {
                            return names[i];
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retourne le nom de cette espèce. Le nom sera retourné
     * de préférence dans la langue par défaut du système.
     */
    public String toString() {
        return getName(null);
    }

    /**
     * Compare cette espèce avec l'objet spécifié.
     */
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object!=null && object.getClass().equals(getClass())) {
            final Species that = (Species) object;
            return Arrays.equals(locales, locales) &&
                   Arrays.equals(names,   names)   &&
                   Utilities.equals(defaultColor, defaultColor);
        }
        return false;
    }

    /**
     * Retourne un code pour cette espèce.
     */
    public int hashCode() {
        int code = names.length;
        for (int i=0; i<names.length; i++) {
            code = code*37 + names[i].hashCode();
        }
        return code;
    }

    /**
     * Retourne un nouvel icône représentant cette espèce.
     */
    public Species.Icon getIcon() {
        return new Icon();
    }

    /**
     * Icone de l'espèce.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Icon extends ImageIcon implements fr.ird.animat.Species.Icon {
        /**
         * Couleur de l'icône.
         */
        private Color color;

        /**
         * Construit un icône.
         */
        public Icon() {
            super(prepare(null, defaultColor));
            this.color = defaultColor;
        }

        /**
         * Retourne l'espèce associé à cet icône.
         */
        public Species getSpecies() {
            return Species.this;
        }

        /**
         * Retourne la couleur de cet icône.
         */
        public Color getColor() {
            return color;
        }

        /**
         * Change la couleur de cet icône.
         */
        public synchronized void setColor(final Color color) {
            setImage(prepare((BufferedImage) getImage(), color));
            this.color = color;
        }

        /**
         * Retourne une chaîne de caractères représentant cet objet.
         */
        public String toString() {
            return Species.this.toString()+'['+color+']';
        }
    }

    /**
     * Write the icon in the specified image with the specified color.
     *
     * @param  image The image to write to, or <code>null</code> to create a new imagE.
     * @param  color The color to use for icon.
     * @return <code>image</code>, or a new image if <code>image</code> was null.
     */
    private static BufferedImage prepare(BufferedImage image, final Color color) {
        if (image==null) {
            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        }
        final int[] components = new int[] {
            color.getRed(),
            color.getGreen(),
            color.getBlue()
        };
        final WritableRaster raster = image.getWritableTile(0,0);
        for (int y=raster.getHeight(); --y>=0;) {
            for (int x=raster.getWidth(); --x>=0;) {
                raster.setPixel(x,y,components);
            }
        }
        image.releaseWritableTile(0,0);
        return image;
    }
}
