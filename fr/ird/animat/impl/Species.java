/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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

// Divers
import java.awt.Color;
import java.util.Locale;


/**
 * Repr�sentation d'une esp�ce animale. Chaque individu {@link Animal} devra
 * obligatoirement appartenir � une et une seule esp�ce. Le terme "esp�ce"
 * est ici utilis� au sens large: bien que le nom <code>Species</code>
 * sugg�re qu'il se r�f�re � la classification des esp�ces, ce n'est pas
 * obligatoirement le cas. Le programmeur est libre d'utiliser plusieurs
 * objets <code>Species</code> pour repr�senter par exemple des groupes
 * d'individus qui appartiennent � la m�me esp�ce animale, mais qui sont
 * de tailles diff�rentes (par exemple les juv�niles versus les adultes).
 * <br><br>
 * Les objets <code>Species</code> sont immutables. En g�n�ral, il
 * n'existera qu'une courte liste d'esp�ces qui seront partag�es par
 * tous les individus {@link Animal}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Species {
    /**
     * Constante pour la langue latine.
     * Souvent utilis�e pour nommer les esp�ces.
     *
     * @see Locale#ENGLISH
     * @see Locale#FRENCH
     * @see Locale#SPANISH
     */
    Locale LATIN = new Locale("la", "");

    /**
     * Retourne les langues dans lesquelles peuvent
     * �tre exprim�es le nom de cette esp�ce.
     */
    Locale[] getLocales();

    /**
     * Retourne le nom de cette esp�ce dans la langue sp�cifi�e. Si aucun
     * nom n'est disponible dans cette langue, retourne <code>null</code>.
     *
     * @param  locale Langue d�sir�e pour le nom de l'esp�ce. La valeur <code>null</code>
     *         est l�gale. Elle signifie que la cha�ne d�sir�e est un code repr�sentant
     *         l'esp�ce. Par exemple, le code de la FAO pour l'albacore (<cite>Thunnus
     *         albacares</cite>, ou <cite>Yellowfin tuna</cite> en anglais) est "YFT".
     * @return Le nom de l'esp�ce dans la langue sp�cifi�e, ou <code>null</code> s'il
     *         n'y en a pas.
     */
    String getName(final Locale locale);

    /**
     * Retourne le nom de cette esp�ce. Le nom sera retourn� de pr�f�rence dans la langue
     * par d�faut du syst�me. Si aucun nom n'est d�finie dans la langue par d�faut, alors
     * cette m�thode tentera de retourner un nom dans une autre langue. Le code de l'esp�ce
     * (tel que retourn� par <code>getName(null)</code>) ne sera retourn� qu'en dernier
     * recours.
     */
    String getName();

    /**
     * Construit un nouvel icone repr�sentant cette esp�ce.
     */
    Icon getIcon();

    /**
     * Ic�ne repr�sentant une esp�ce. Un ic�ne peut servir � positionner
     * sur une carte plusieurs individus d'une m�me esp�ce, et peut aussi
     * appara�tre devant une �tiquette dans les listes d�roulantes.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static interface Icon extends javax.swing.Icon {
        /**
         * Retourne l'esp�ce associ�e � cet ic�ne.
         */
        Species getSpecies();

        /**
         * Retourne la couleur de cet ic�ne.
         */
        Color getColor();

        /**
         * Change la couleur de cet ic�ne.
         */
        void setColor(Color color);
    }
}
