/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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

// Divers
import java.awt.Color;
import java.util.Locale;


/**
 * Représentation d'une espèce animale. Chaque individu {@link Animal} devra
 * obligatoirement appartenir à une et une seule espèce. Le terme "espèce"
 * est ici utilisé au sens large: bien que le nom <code>Species</code>
 * suggère qu'il se réfère à la classification des espèces, ce n'est pas
 * obligatoirement le cas. Le programmeur est libre d'utiliser plusieurs
 * objets <code>Species</code> pour représenter par exemple des groupes
 * d'individus qui appartiennent à la même espèce animale, mais qui sont
 * de tailles différentes (par exemple les juvéniles versus les adultes).
 * <br><br>
 * Les objets <code>Species</code> sont immutables. En général, il
 * n'existera qu'une courte liste d'espèces qui seront partagées par
 * tous les individus {@link Animal}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Species {
    /**
     * Constante pour la langue latine.
     * Souvent utilisée pour nommer les espèces.
     *
     * @see Locale#ENGLISH
     * @see Locale#FRENCH
     * @see Locale#SPANISH
     */
    Locale LATIN = new Locale("la", "");

    /**
     * Retourne les langues dans lesquelles peuvent
     * être exprimées le nom de cette espèce.
     */
    Locale[] getLocales();

    /**
     * Retourne le nom de cette espèce dans la langue spécifiée. Si aucun
     * nom n'est disponible dans cette langue, retourne <code>null</code>.
     *
     * @param  locale Langue désirée pour le nom de l'espèce. La valeur <code>null</code>
     *         est légale. Elle signifie que la chaîne désirée est un code représentant
     *         l'espèce. Par exemple, le code de la FAO pour l'albacore (<cite>Thunnus
     *         albacares</cite>, ou <cite>Yellowfin tuna</cite> en anglais) est "YFT".
     * @return Le nom de l'espèce dans la langue spécifiée, ou <code>null</code> s'il
     *         n'y en a pas.
     */
    String getName(final Locale locale);

    /**
     * Retourne le nom de cette espèce. Le nom sera retourné de préférence dans la langue
     * par défaut du système. Si aucun nom n'est définie dans la langue par défaut, alors
     * cette méthode tentera de retourner un nom dans une autre langue. Le code de l'espèce
     * (tel que retourné par <code>getName(null)</code>) ne sera retourné qu'en dernier
     * recours.
     */
    String getName();

    /**
     * Construit un nouvel icone représentant cette espèce.
     */
    Icon getIcon();

    /**
     * Icône représentant une espèce. Un icône peut servir à positionner
     * sur une carte plusieurs individus d'une même espèce, et peut aussi
     * apparaître devant une étiquette dans les listes déroulantes.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static interface Icon extends javax.swing.Icon {
        /**
         * Retourne l'espèce associée à cet icône.
         */
        Species getSpecies();

        /**
         * Retourne la couleur de cet icône.
         */
        Color getColor();

        /**
         * Change la couleur de cet icône.
         */
        void setColor(Color color);
    }
}
