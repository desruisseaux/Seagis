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
 * Impl�mentation par d�faut d'une esp�ce animale.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Species implements fr.ird.animat.Species, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 1428323484284560694L;

    /**
     * Liste des param�tres par d�faut observ�s par les animaux de cette esp�ce.
     */
    private static final Parameter[] DEFAULT_PARAMETERS = new Parameter[] {
        Parameter.HEADING
    };

    /**
     * Liste des index par d�faut auxquels trouver les valeurs des observations
     * dans le tableau {@link Observations#observations} pour chaque param�tre.
     */
    private static final int[] DEFAULT_OFFSETS = new int[] {
        0, Observations.LOCATED_LENGTH
    };

    /**
     * Largeur par d�faut des ic�nes, en pixels.
     */
    private static final int WIDTH = 20;

    /**
     * Hauteur par d�faut des ic�nes, en pixels.
     */
    private static final int HEIGHT = 10;

    /**
     * Langues dans lesquelles un nom d'esp�ce est disponible.
     */
    private final Locale[] locales;

    /**
     * Nom de cette esp�ce dans une des langues de {@link #locales}.
     */
    private final String[] names;

    /**
     * Couleur par d�faut des ic�ne.
     */
    private final Color defaultColor;

    /**
     * Liste des param�tres observ�s par les animaux de cette esp�ce.
     */
    final Parameter[] parameters = DEFAULT_PARAMETERS;

    /**
     * Liste des index auxquels trouver les valeurs des observations dans le tableau
     * <code>Observations.observations</code> pour chaque param�tre.  La longueur de
     * ce tableau doit �tre �gale � la longueur du tableau <code>parameters</code>
     * plus 1.
     */
    final int[] observationOffsets = DEFAULT_OFFSETS;

    /**
     * Construit une nouvelle esp�ce. Le nom de cette esp�ce peut �tre exprim�
     * selon plusieurs langues. A chaque nom ({@link String}) est associ� une
     * langue ({@link Locale}).
     *
     * @param locales Langues des noms de cette esp�ces. Ce tableau doit avoir
     *        la m�me longueur que l'argument <code>names</code>. <strong>NOTE:
     *        Ce tableau n'est pas clon�</strong>.  Evitez donc de le modifier
     *        apr�s la construction.
     * @param names  Nom de cette esp�ce selon chacune des langues �num�r�es dans
     *        l'argument <code>locales</code>. <strong>NOTE: Ce tableau n'est pas
     *        clon�</strong>. Evitez donc de le modifier apr�s la construction.
     * @param color Couleur par d�faut � utiliser pour les ic�nes repr�sentant cette
     *        esp�ce.
     *
     * @throws IllegalArgumentException Si un des �l�ments du tableau
     *         <code>locales</code> appara�t plus d'une fois.
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
     * les animaux de cette esp�ce. Il s'agit de la longueur qu'aura le tableau de type
     * <code>float[]</code> qui contiendra les donn�es d'observations.
     */
    final int getRecordLength() {
        return observationOffsets[parameters.length];
    }

    /**
     * Retourne les langues dans lesquelles peuvent
     * �tre exprim�es le nom de cette esp�ce.
     */
    public Locale[] getLocales() {
        return (Locale[]) locales.clone();
    }

    /**
     * Retourne le nom de cette esp�ce dans la langue sp�cifi�e. Si aucun
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
     * Retourne le nom de cette esp�ce. Le nom sera retourn�
     * de pr�f�rence dans la langue par d�faut du syst�me.
     */
    public String toString() {
        return getName(null);
    }

    /**
     * Compare cette esp�ce avec l'objet sp�cifi�.
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
     * Retourne un code pour cette esp�ce.
     */
    public int hashCode() {
        int code = names.length;
        for (int i=0; i<names.length; i++) {
            code = code*37 + names[i].hashCode();
        }
        return code;
    }

    /**
     * Retourne un nouvel ic�ne repr�sentant cette esp�ce.
     */
    public Species.Icon getIcon() {
        return new Icon();
    }

    /**
     * Icone de l'esp�ce.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Icon extends ImageIcon implements fr.ird.animat.Species.Icon {
        /**
         * Couleur de l'ic�ne.
         */
        private Color color;

        /**
         * Construit un ic�ne.
         */
        public Icon() {
            super(prepare(null, defaultColor));
            this.color = defaultColor;
        }

        /**
         * Retourne l'esp�ce associ� � cet ic�ne.
         */
        public Species getSpecies() {
            return Species.this;
        }

        /**
         * Retourne la couleur de cet ic�ne.
         */
        public Color getColor() {
            return color;
        }

        /**
         * Change la couleur de cet ic�ne.
         */
        public synchronized void setColor(final Color color) {
            setImage(prepare((BufferedImage) getImage(), color));
            this.color = color;
        }

        /**
         * Retourne une cha�ne de caract�res repr�sentant cet objet.
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
