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
import java.awt.geom.RectangularShape;
import java.awt.geom.Ellipse2D;

import java.util.Set;
import java.util.Arrays;
import java.util.Locale;
import java.util.HashSet;
import java.io.Serializable;
import javax.swing.ImageIcon;
import javax.vecmath.MismatchedSizeException;

// Divers
import org.geotools.resources.Utilities;
import fr.ird.util.XEllipse2D;
import fr.ird.util.ArraySet;


/**
 * Implémentation par défaut d'une espèce animale.   Le nom de l'espèce peut être spécifiée en
 * plusieurs langues, incluant un {@linkplain #FAO code de la FAO}. L'icône représentant cette
 * espèce sera par défaut un simple rectangle de la couleur spécifiée au constructeur.  Chaque
 * objet <code>Species</code> peut aussi déclarer la liste de tous les paramètres susceptibles
 * d'intéresser les animaux de cette espèce.  Par défaut, la liste ne comprend que {@linkplain
 * Parameter#HEADING le cap et la position} de l'animal.
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
     * Valeur par défaut du rayon de perception de l'animal, en miles nautiques.
     *
     * @see #getPerceptionArea
     */
    private static final double PERCEPTION_RADIUS = 30;

    /**
     * La langue par défaut du système.
     */
    private static final Locale[] DEFAULT_LOCALES = new Locale[] {
        Locale.getDefault()
    };

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
    private static final int[] DEFAULT_OFFSETS = getOffsets(DEFAULT_PARAMETERS);

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
    private final Color color;

    /**
     * Liste des paramètres observés par les animaux de cette espèce.
     */
    final Parameter[] parameters;

    /**
     * Liste des index auxquels trouver les valeurs des observations dans le tableau
     * <code>Observations.observations</code> pour chaque paramètre.  La longueur de
     * ce tableau doit être égale à la longueur du tableau <code>parameters</code>
     * plus 1.
     */
    final int[] offsets;

    /**
     * Les paramètres représentés comme un ensemble de type {@link Set}.
     * Cet ensemble ne sera construit que la première fois où il sera demandé.
     */
    private transient Set<fr.ird.animat.Parameter> parameterSet;

    /**
     * Index du paramètre {@link Parameter#HEADING}, ou -1 si ce paramètre ne fait pas parti
     * de l'ensemble des paramètres pris en compte.  Le paramètre <code>HEADING</code> est
     * traité d'une manière particulière, étant donné que ses informations sont mémorisées
     * dans l'objet {@link Path} plutôt que dans le tableau interne de {@link Animal}.
     */
    final int headingIndex;

    /**
     * Construit une espèce avec un nom dans la {@link Locale#getDefault langue par défaut}
     * du système.
     *
     * @param name  Le nom de l'espèce.
     * @param color La couleur de l'espèce.
     */
    public Species(final String name, final Color color) {
        this(DEFAULT_LOCALES, new String[] {name}, color);
    }

    /**
     * Construit une nouvelle espèce. Le nom de cette espèce peut être exprimé
     * selon plusieurs langues.  A chaque nom ({@link String}) est associé une
     * langue ({@link Locale}).
     *
     * @param locales Langues des noms de cette espèce.  Ce tableau doit avoir
     *        la même longueur que l'argument <code>names</code>. <strong>NOTE:
     *        Ce tableau n'est pas cloné</strong>.  Evitez donc de le modifier
     *        après la construction.
     * @param names Nom de cette espèce selon chacune des langues énumérées dans
     *        l'argument <code>locales</code>. <strong>NOTE: Ce tableau n'est pas
     *        cloné</strong>. Evitez donc de le modifier après la construction.
     * @param color Couleur par défaut à utiliser pour les icônes représentant cette espèce.
     *
     * @throws MismatchedSizeException si les tableaux <code>locales</code> et <code>names</code>
     *         n'ont pas la même longueur.
     * @throws IllegalArgumentException Si un des éléments du tableau
     *         <code>locales</code> apparaît plus d'une fois.
     */
    public Species(final Locale[] locales,
                   final String[] names,
                   final Color    color)
            throws MismatchedSizeException, IllegalArgumentException
    {
        this(locales, names, color, DEFAULT_PARAMETERS, DEFAULT_OFFSETS);
    }

    /**
     * Construit une nouvelle espèce d'animaux qui s'intéresseront aux paramètres spécifiés.
     * <strong>Note: aucun tableau n'est cloné</strong>, afin de faciliter la réutilisation
     * de tableaux déjà existants. Evitez donc de modifier un tableau après la construction
     * de cet objet <code>Species</code>.
     *
     * @param locales Langues des noms de cette espèce. Ce tableau doit avoir
     *        la même longueur que l'argument <code>names</code>.
     * @param names Nom de cette espèce selon chacune des langues énumérées dans
     *        l'argument <code>locales</code>.
     * @param color Couleur par défaut à utiliser pour les icônes représentant cette espèce.
     * @param parameters Paramètres susceptibles d'intéresser les animaux de cette espèce.
     *
     * @throws MismatchedSizeException si les tableaux <code>locales</code> et <code>names</code>
     *         n'ont pas la même longueur.
     * @throws IllegalArgumentException Si un des éléments du tableau
     *         <code>locales</code> apparaît plus d'une fois.
     */
    public Species(final Locale[]    locales,
                   final String[]    names,
                   final Color       color,
                   final Parameter[] parameters)
            throws MismatchedSizeException, IllegalArgumentException
    {
        this(locales, names, color, parameters, getOffsets(parameters));
    }

    /**
     * Construit une espèce avec des paramètres identiques à ceux de l'espèce spécifiée.
     * Ce constructeur est utile pour les sous-classes qui veulent modifier une espèce
     * existante en redéfinissant quelques methodes telle que {@link #getPerceptionArea}.
     */
    protected Species(final Species parent) {
        this(parent.locales, parent.names, parent.color, parent.parameters, parent.offsets);
    }

    /**
     * Construit une espèce avec le même nom que l'espèce spécifiée mais qui s'intéressera
     * à des paramètres différents.
     *
     * @param parent L'espèce dont on veut copier les propriétés (noms, couleur).
     * @param parameters Paramètres susceptibles d'intéresser les animaux de cette espèce.
     */
    protected Species(final Species parent, final Parameter[] parameters) {
        this(parent.locales, parent.names, parent.color, parameters, getOffsets(parameters));
    }

    /**
     * Procède à la construction d'une espèce.
     * <strong>Note: aucun tableau n'est cloné</strong>, afin de faciliter la réutilisation
     * de tableaux déjà existants. Evitez donc de modifier un tableau après la construction
     * de cet objet <code>Species</code>.
     *
     * @param locales Langues des noms de cette espèce. Ce tableau doit avoir
     *        la même longueur que l'argument <code>names</code>.
     * @param names Nom de cette espèce selon chacune des langues énumérées dans
     *        l'argument <code>locales</code>.
     * @param color Couleur par défaut à utiliser pour les icônes représentant cette espèce.
     * @param parameters Paramètres susceptibles d'intéresser les animaux de cette espèce.
     * @param offsets Liste des index auxquels trouver les valeurs des observations dans le
     *        tableau <code>Observations.observations</code> pour chaque paramètre. La longueur
     *        de ce tableau doit être égale à la longueur du tableau <code>parameters</code>
     *        plus 1.
     *
     * @throws MismatchedSizeException si les tableaux <code>locales</code> et <code>names</code>
     *         n'ont pas la même longueur.
     * @throws IllegalArgumentException Si un des éléments du tableau
     *         <code>locales</code> apparaît plus d'une fois.
     */
    private Species(final Locale[]    locales,
                    final String[]    names,
                    final Color       color,
                    final Parameter[] parameters,
                    final int[]       offsets)
            throws MismatchedSizeException, IllegalArgumentException
    {
        this.locales    = locales;
        this.names      = names;
        this.color      = color;
        this.parameters = parameters;
        this.offsets    = offsets;
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
        int headingIndex = -1;
        for (int i=0; i<parameters.length; i++) {
            if (Parameter.HEADING.equals(parameters[i])) {
                headingIndex = i;
                break;
            }
        }
        this.headingIndex = headingIndex;
        assert getObservedParameters().size() == parameters.length;
    }

    /**
     * Retourne un objet {@link fr.ird.animat.Species} arbitraire
     * sous forme d'un objet {@link Species} de cette classe.
     */
    protected static Species wrap(final fr.ird.animat.Species species) {
        if (species instanceof Species) {
            return (Species) species;
        }
        final Locale[] locales = species.getLocales();
        final String[] names = new String[locales.length];
        for (int i=0; i<locales.length; i++) {
            names[i] = species.getName(locales[i]);
        }
        return new Species(locales, names, species.getIcon().getColor());
    }

    /**
     * Retourne la liste des index auxquels trouver les valeurs des observations dans le tableau
     * <code>Observations.observations</code> pour chaque paramètre. La longueur de ce tableau
     * sera égale à la longueur du tableau <code>parameters</code> plus 1.
     *
     * @param parameters Paramètres susceptibles d'intéresser les animaux de cette espèce.
     * @return Index auxquels trouver les valeurs des observations pour chaque paramètre.
     */
    private static final int[] getOffsets(final Parameter[] parameters) {
        final int[] offsets = new int[parameters.length + 1];
        for (int i=0; i<parameters.length; i++) {
            offsets[i+1] = offsets[i] + parameters[i].getNumSampleDimensions();
        }
        return offsets;
    }

    /**
     * Retourne la longueur des enregistrements correspondant aux observations faites par
     * les animaux de cette espèce. Il s'agit de la longueur qu'aura le tableau de type
     * <code>float[]</code> qui contiendra les données d'observations.
     */
    final int getRecordLength() {
        assert offsets.length == parameters.length+1;
        return offsets[parameters.length];
    }

    /**
     * Retourne la longueur des enregistrements moins les éléments qui seront stokés dans
     * {@link Path}. Cette longeur sera utilisé dans le tableau {@link Animal#observations}.
     */
    final int getReducedRecordLength() {
        int length = getRecordLength();
        if (headingIndex >= 0) {
            length -= Parameter.HEADING.getNumSampleDimensions();
        }
        assert length >= 0 : length;
        return length;
    }

    /**
     * Retourne les langues dans lesquelles peuvent être exprimées le nom de cette espèce.
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
     * Retourne un nouvel icône représentant cette espèce.
     */
    public fr.ird.animat.Species.Icon getIcon() {
        return new Icon();
    }

    /**
     * Retourne tous les {@linkplain Parameter paramètres} susceptibles d'intéresser les
     * {@linkplain Animal animaux} de cette espèce. L'ensemble retourné est immutable.
     */
    public Set<fr.ird.animat.Parameter> getObservedParameters() {
        if (parameterSet == null) {
            // Pas besoin de synchroniser. Ce n'est pas très
            // grave si deux instances de cet objet existent.
            parameterSet = new ArraySet<fr.ird.animat.Parameter>(parameters);
        }
        return parameterSet;
    }

    /**
     * Retourne la région de perception par défaut des animaux de cette espèce. Il s'agit de la
     * région dans laquelle  l'animal peut percevoir les paramètres de son environnement autour
     * de lui. Les coordonnées de cette forme doivent être en <strong>mètres</strong> et la forme
     * doit être centrée sur la position de l'animal, sans rotation.  En d'autres mots, l'origine
     * (0,0) est définie comme étant la position de l'animal, tandis que le cap de l'animal est
     * défini comme pointant dans la direction des <var>x</var> positifs.  La rotation de cette
     * forme (pour tenir compte du cap), sa translation (pour tenir compte de la position) et sa
     * transformation en coordonnées géographiques pour une date donnée seront prises en compte
     * par la méthode {@link Animal#getPerceptionArea}.
     */
    protected RectangularShape getPerceptionArea() {
        return new XEllipse2D(-PERCEPTION_RADIUS,
                              -PERCEPTION_RADIUS,
                             2*PERCEPTION_RADIUS,
                             2*PERCEPTION_RADIUS);
    }

    /**
     * Implémentation par défaut de l'icône de l'espèce.
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
            super(prepare(null, Species.this.color));
            this.color = Species.this.color;
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
            return Arrays.equals(this.locales,    that.locales) &&
                   Arrays.equals(this.names,      that.names)   &&
                Utilities.equals(this.color,      that.color)   &&
                   Arrays.equals(this.parameters, that.parameters);
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
}
