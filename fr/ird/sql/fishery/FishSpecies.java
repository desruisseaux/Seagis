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
package fr.ird.sql.fishery;

// Graphisme
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

// Divers
import java.util.Set;
import java.util.Locale;
import java.util.HashSet;
import fr.ird.animat.Species;
import javax.vecmath.MismatchedSizeException;


/**
 * Repr�sentation d'une esp�ce animale. Chaque objet {@link Animal} devra appartenir � une esp�ce.
 * Bien que son nom sugg�re que <code>Species</code> se r�f�re � la classification des esp�ces, ce
 * n'est pas n�cessairement le cas. On peut aussi utiliser plusieurs objets <code>Species</code>
 * pour repr�senter des groupes d'individus qui appartiennent � la m�me esp�ce animale, mais qui
 * sont de tailles diff�rentes (par exemple les juv�niles versus les adultes).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class FishSpecies implements Species
{
    /**
     * Langues dans lesquelles un nom d'esp�ce est disponible.
     */
    private final Locale[] locales;

    /**
     * Nom de cette esp�ce dans une des langues de {@link #locales}.
     */
    private final String[] names;

    /**
     * Couleur par d�faut � utiliser pour le tra�age.
     */
    private final Color defaultColor;

    /**
     * Couleur � utiliser pour le tra�age.
     */
    private Color color;

    /**
     * Petit icone repr�sentant cette esp�ce. Cet ic�ne sera typiquement plac�
     * devant l'�tiquette dans les listes d�roulantes des bo�tes de dialogue.
     */
    private Icon icon;

    /**
     * Construit une nouvelle esp�ce. Le nom de cette esp�ce peut �tre exprim�
     * selon plusieurs langues. A chaque nom ({@link String}) est associ� une
     * langue ({@link Locale}). Par convention, un objet <code>Locale</code>
     * nul signifie que le nom correspondant n'est pas exprim� selon une langue
     * en particulier, mais repr�sente plut�t un code ou un num�ro d'identification.
     * Il peut s'agir en particulier des codes � trois lettres de la FAO.
     *
     * @param locales Langues des noms de cette esp�ces. Ce tableau doit avoir
     *        la m�me longueur que l'argument <code>names</code>. <strong>NOTE:
     *        Ce tableau n'est pas clon�</strong>.  Evitez donc de le modifier
     *        apr�s la construction.
     * @param names  Nom de cette esp�ce selon chacune des langues �num�r�es dans
     *        l'argument <code>locales</code>. <strong>NOTE: Ce tableau n'est pas
     *        clon�</strong>. Evitez donc de le modifier apr�s la construction.
     * @param color Couleur par d�faut � utiliser pour le tra�age.
     *
     * @throws IllegalArgumentException Si un des �l�ments du tableau
     *         <code>locales</code> appara�t plus d'une fois.
     */
    public FishSpecies(final Locale[] locales, final String[] names, final Color color) throws IllegalArgumentException
    {
        this.locales = locales;
        this.names   = names;
        if (locales.length!=names.length)
            throw new MismatchedSizeException();

        final Set<Locale> set = new HashSet<Locale>();
        for (int i=0; i<locales.length; i++)
        {
            if (!set.add(locales[i])) // Accepte null
                throw new IllegalArgumentException();
            if (names[i]==null)
                throw new NullPointerException();
        }
        this.color = defaultColor = color;
    }

    /**
     * Retourne les langues dans lesquelles peuvent
     * �tre exprim�es le nom de cette esp�ce.
     */
    public Locale[] getLocales()
    {return (Locale[]) locales.clone();}

    /**
     * Retourne le nom de cette esp�ce dans la langue sp�cifi�e. Si aucun
     * nom n'est disponible dans cette langue, retourne <code>null</code>.
     */
    public String getName(final Locale locale)
    {
        if (locale==null)
        {
            for (int i=0; i<locales.length; i++)
                if (locale==locales[i])
                    return names[i];
            return null;
        }

        for (int i=0; i<locales.length; i++)
            if (locale.equals(locales[i]))
                return names[i];

        final String language=locale.getLanguage();
        if (language.length()!=0)
        {
            for (int i=0; i<locales.length; i++)
                if (locales[i]!=null)
                    if (language.equals(locales[i].getLanguage()))
                        return names[i];
        }
        return null;
    }

    /**
     * Retourne le nom de cette esp�ce. Le nom sera retourn�
     * de pr�f�rence dans la langue par d�faut du syst�me.
     */
    public String getName()
    {
        final String name = getName(Locale.getDefault());
        if (name!=null) return name;
        for (int i=0; i<names.length; i++)
            if (locales[i]!=null)
                return names[i];
        return names.length!=0 ? names[0] : null;
    }

    /**
     * Retourne un petit icone repr�sentant cette esp�ce. Cet ic�ne sera typiquement
     * plac� devant l'�tiquette dans les listes d�roulantes des bo�tes de dialogue.
     *
     * @param color Couleur pr�f�r�e de l'ic�ne, ou <code>null</code> pour
     *        utiliser une couleur par d�faut.
     */
    public synchronized Icon getIcon(Color color)
    {
        if (color==null) color=defaultColor;
        if (icon==null || !color.equals(this.color))
        {
            final int             width = 20;
            final int            height = 10;
            final int[]      components = new int[]{color.getRed(), color.getGreen(), color.getBlue()};
            final BufferedImage   image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final WritableRaster raster = image.getWritableTile(0,0);
            for (int y=0; y<height; y++)
                for (int x=0; x<width; x++)
                    raster.setPixel(x,y,components);
            image.releaseWritableTile(0,0);
            icon = new ImageIcon(image);
            this.color = color;
        }
        return icon;
    }

    /**
     * Retourne le nom de cette esp�ce. Le nom sera retourn�
     * de pr�f�rence dans la langue par d�faut du syst�me.
     */
    public String toString()
    {return getName();}
}
