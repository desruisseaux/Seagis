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
 * Impl�mentation par d�faut d'une esp�ce animale.   Le nom de l'esp�ce peut �tre sp�cifi�e en
 * plusieurs langues, incluant un {@linkplain #FAO code de la FAO}. L'ic�ne repr�sentant cette
 * esp�ce sera par d�faut un simple rectangle de la couleur sp�cifi�e au constructeur.  Chaque
 * objet <code>Species</code> peut aussi d�clarer la liste de tous les param�tres susceptibles
 * d'int�resser les animaux de cette esp�ce.  Par d�faut, la liste ne comprend que {@linkplain
 * Parameter#HEADING le cap et la position} de l'animal.
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
     * Valeur par d�faut du rayon de perception de l'animal, en miles nautiques.
     *
     * @see #getPerceptionArea
     */
    private static final double PERCEPTION_RADIUS = 30;

    /**
     * La langue par d�faut du syst�me.
     */
    private static final Locale[] DEFAULT_LOCALES = new Locale[] {
        Locale.getDefault()
    };

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
    private static final int[] DEFAULT_OFFSETS = getOffsets(DEFAULT_PARAMETERS);

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
    private final Color color;

    /**
     * Liste des param�tres observ�s par les animaux de cette esp�ce.
     */
    final Parameter[] parameters;

    /**
     * Liste des index auxquels trouver les valeurs des observations dans le tableau
     * <code>Observations.observations</code> pour chaque param�tre.  La longueur de
     * ce tableau doit �tre �gale � la longueur du tableau <code>parameters</code>
     * plus 1.
     */
    final int[] offsets;

    /**
     * Les param�tres repr�sent�s comme un ensemble de type {@link Set}.
     * Cet ensemble ne sera construit que la premi�re fois o� il sera demand�.
     */
    private transient Set<fr.ird.animat.Parameter> parameterSet;

    /**
     * Index du param�tre {@link Parameter#HEADING}, ou -1 si ce param�tre ne fait pas parti
     * de l'ensemble des param�tres pris en compte.  Le param�tre <code>HEADING</code> est
     * trait� d'une mani�re particuli�re, �tant donn� que ses informations sont m�moris�es
     * dans l'objet {@link Path} plut�t que dans le tableau interne de {@link Animal}.
     */
    final int headingIndex;

    /**
     * Construit une esp�ce avec un nom dans la {@link Locale#getDefault langue par d�faut}
     * du syst�me.
     *
     * @param name  Le nom de l'esp�ce.
     * @param color La couleur de l'esp�ce.
     */
    public Species(final String name, final Color color) {
        this(DEFAULT_LOCALES, new String[] {name}, color);
    }

    /**
     * Construit une nouvelle esp�ce. Le nom de cette esp�ce peut �tre exprim�
     * selon plusieurs langues.  A chaque nom ({@link String}) est associ� une
     * langue ({@link Locale}).
     *
     * @param locales Langues des noms de cette esp�ce.  Ce tableau doit avoir
     *        la m�me longueur que l'argument <code>names</code>. <strong>NOTE:
     *        Ce tableau n'est pas clon�</strong>.  Evitez donc de le modifier
     *        apr�s la construction.
     * @param names Nom de cette esp�ce selon chacune des langues �num�r�es dans
     *        l'argument <code>locales</code>. <strong>NOTE: Ce tableau n'est pas
     *        clon�</strong>. Evitez donc de le modifier apr�s la construction.
     * @param color Couleur par d�faut � utiliser pour les ic�nes repr�sentant cette esp�ce.
     *
     * @throws MismatchedSizeException si les tableaux <code>locales</code> et <code>names</code>
     *         n'ont pas la m�me longueur.
     * @throws IllegalArgumentException Si un des �l�ments du tableau
     *         <code>locales</code> appara�t plus d'une fois.
     */
    public Species(final Locale[] locales,
                   final String[] names,
                   final Color    color)
            throws MismatchedSizeException, IllegalArgumentException
    {
        this(locales, names, color, DEFAULT_PARAMETERS, DEFAULT_OFFSETS);
    }

    /**
     * Construit une nouvelle esp�ce d'animaux qui s'int�resseront aux param�tres sp�cifi�s.
     * <strong>Note: aucun tableau n'est clon�</strong>, afin de faciliter la r�utilisation
     * de tableaux d�j� existants. Evitez donc de modifier un tableau apr�s la construction
     * de cet objet <code>Species</code>.
     *
     * @param locales Langues des noms de cette esp�ce. Ce tableau doit avoir
     *        la m�me longueur que l'argument <code>names</code>.
     * @param names Nom de cette esp�ce selon chacune des langues �num�r�es dans
     *        l'argument <code>locales</code>.
     * @param color Couleur par d�faut � utiliser pour les ic�nes repr�sentant cette esp�ce.
     * @param parameters Param�tres susceptibles d'int�resser les animaux de cette esp�ce.
     *
     * @throws MismatchedSizeException si les tableaux <code>locales</code> et <code>names</code>
     *         n'ont pas la m�me longueur.
     * @throws IllegalArgumentException Si un des �l�ments du tableau
     *         <code>locales</code> appara�t plus d'une fois.
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
     * Construit une esp�ce avec des param�tres identiques � ceux de l'esp�ce sp�cifi�e.
     * Ce constructeur est utile pour les sous-classes qui veulent modifier une esp�ce
     * existante en red�finissant quelques methodes telle que {@link #getPerceptionArea}.
     */
    protected Species(final Species parent) {
        this(parent.locales, parent.names, parent.color, parent.parameters, parent.offsets);
    }

    /**
     * Construit une esp�ce avec le m�me nom que l'esp�ce sp�cifi�e mais qui s'int�ressera
     * � des param�tres diff�rents.
     *
     * @param parent L'esp�ce dont on veut copier les propri�t�s (noms, couleur).
     * @param parameters Param�tres susceptibles d'int�resser les animaux de cette esp�ce.
     */
    protected Species(final Species parent, final Parameter[] parameters) {
        this(parent.locales, parent.names, parent.color, parameters, getOffsets(parameters));
    }

    /**
     * Proc�de � la construction d'une esp�ce.
     * <strong>Note: aucun tableau n'est clon�</strong>, afin de faciliter la r�utilisation
     * de tableaux d�j� existants. Evitez donc de modifier un tableau apr�s la construction
     * de cet objet <code>Species</code>.
     *
     * @param locales Langues des noms de cette esp�ce. Ce tableau doit avoir
     *        la m�me longueur que l'argument <code>names</code>.
     * @param names Nom de cette esp�ce selon chacune des langues �num�r�es dans
     *        l'argument <code>locales</code>.
     * @param color Couleur par d�faut � utiliser pour les ic�nes repr�sentant cette esp�ce.
     * @param parameters Param�tres susceptibles d'int�resser les animaux de cette esp�ce.
     * @param offsets Liste des index auxquels trouver les valeurs des observations dans le
     *        tableau <code>Observations.observations</code> pour chaque param�tre. La longueur
     *        de ce tableau doit �tre �gale � la longueur du tableau <code>parameters</code>
     *        plus 1.
     *
     * @throws MismatchedSizeException si les tableaux <code>locales</code> et <code>names</code>
     *         n'ont pas la m�me longueur.
     * @throws IllegalArgumentException Si un des �l�ments du tableau
     *         <code>locales</code> appara�t plus d'une fois.
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
     * <code>Observations.observations</code> pour chaque param�tre. La longueur de ce tableau
     * sera �gale � la longueur du tableau <code>parameters</code> plus 1.
     *
     * @param parameters Param�tres susceptibles d'int�resser les animaux de cette esp�ce.
     * @return Index auxquels trouver les valeurs des observations pour chaque param�tre.
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
     * les animaux de cette esp�ce. Il s'agit de la longueur qu'aura le tableau de type
     * <code>float[]</code> qui contiendra les donn�es d'observations.
     */
    final int getRecordLength() {
        assert offsets.length == parameters.length+1;
        return offsets[parameters.length];
    }

    /**
     * Retourne la longueur des enregistrements moins les �l�ments qui seront stok�s dans
     * {@link Path}. Cette longeur sera utilis� dans le tableau {@link Animal#observations}.
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
     * Retourne les langues dans lesquelles peuvent �tre exprim�es le nom de cette esp�ce.
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
     * Retourne un nouvel ic�ne repr�sentant cette esp�ce.
     */
    public fr.ird.animat.Species.Icon getIcon() {
        return new Icon();
    }

    /**
     * Retourne tous les {@linkplain Parameter param�tres} susceptibles d'int�resser les
     * {@linkplain Animal animaux} de cette esp�ce. L'ensemble retourn� est immutable.
     */
    public Set<fr.ird.animat.Parameter> getObservedParameters() {
        if (parameterSet == null) {
            // Pas besoin de synchroniser. Ce n'est pas tr�s
            // grave si deux instances de cet objet existent.
            parameterSet = new ArraySet<fr.ird.animat.Parameter>(parameters);
        }
        return parameterSet;
    }

    /**
     * Retourne la r�gion de perception par d�faut des animaux de cette esp�ce. Il s'agit de la
     * r�gion dans laquelle  l'animal peut percevoir les param�tres de son environnement autour
     * de lui. Les coordonn�es de cette forme doivent �tre en <strong>m�tres</strong> et la forme
     * doit �tre centr�e sur la position de l'animal, sans rotation.  En d'autres mots, l'origine
     * (0,0) est d�finie comme �tant la position de l'animal, tandis que le cap de l'animal est
     * d�fini comme pointant dans la direction des <var>x</var> positifs.  La rotation de cette
     * forme (pour tenir compte du cap), sa translation (pour tenir compte de la position) et sa
     * transformation en coordonn�es g�ographiques pour une date donn�e seront prises en compte
     * par la m�thode {@link Animal#getPerceptionArea}.
     */
    protected RectangularShape getPerceptionArea() {
        return new XEllipse2D(-PERCEPTION_RADIUS,
                              -PERCEPTION_RADIUS,
                             2*PERCEPTION_RADIUS,
                             2*PERCEPTION_RADIUS);
    }

    /**
     * Impl�mentation par d�faut de l'ic�ne de l'esp�ce.
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
            super(prepare(null, Species.this.color));
            this.color = Species.this.color;
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
            return Arrays.equals(this.locales,    that.locales) &&
                   Arrays.equals(this.names,      that.names)   &&
                Utilities.equals(this.color,      that.color)   &&
                   Arrays.equals(this.parameters, that.parameters);
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
}
