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
package fr.ird.animat;

// Divers
import java.awt.Color;
import javax.swing.Icon;
import java.util.Locale;


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
public interface Species
{
    /**
     * Constante pour la langue latine.
     *
     * @see Locale#ENGLISH
     * @see Locale#FRENCH
     * @see Locale#SPANISH
     */
    public static final Locale LATIN = new Locale("la", "");

    /**
     * Retourne les langues dans lesquelles peuvent
     * �tre exprim�es le nom de cette esp�ce.
     */
    public abstract Locale[] getLocales();

    /**
     * Retourne le nom de cette esp�ce dans la langue sp�cifi�e. Si aucun
     * nom n'est disponible dans cette langue, retourne <code>null</code>.
     */
    public abstract String getName(final Locale locale);

    /**
     * Retourne le nom de cette esp�ce. Le nom sera retourn�
     * de pr�f�rence dans la langue par d�faut du syst�me.
     */
    public abstract String getName();

    /**
     * Retourne un petit icone repr�sentant cette esp�ce. Cet ic�ne sera typiquement
     * plac� devant l'�tiquette dans les listes d�roulantes des bo�tes de dialogue.
     *
     * @param color Couleur pr�f�r�e de l'ic�ne, ou <code>null</code> pour
     *        utiliser une couleur par d�faut.
     */
    public abstract Icon getIcon(final Color color);
}
