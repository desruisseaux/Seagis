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
package fr.ird.animat;

// Divers
import java.awt.Color;
import java.util.Locale;


/**
 * Représentation d'une espèce animale. Chaque objet {@link Animal} devra appartenir à une espèce.
 * Bien que son nom suggère que <code>Species</code> se réfère à la classification des espèces, ce
 * n'est pas nécessairement le cas. On peut aussi utiliser plusieurs objets <code>Species</code>
 * pour représenter des groupes d'individus qui appartiennent à la même espèce animale, mais qui
 * sont de tailles différentes (par exemple les juvéniles versus les adultes).
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
     * être exprimées le nom de cette espèce.
     */
    public abstract Locale[] getLocales();

    /**
     * Retourne le nom de cette espèce dans la langue spécifiée. Si aucun
     * nom n'est disponible dans cette langue, retourne <code>null</code>.
     */
    public abstract String getName(final Locale locale);

    /**
     * Retourne le nom de cette espèce. Le nom sera retourné
     * de préférence dans la langue par défaut du système.
     */
    public abstract String getName();

    /**
     * Construit un nouvel icone représentant cette espèce.
     */
    public abstract Icon getIcon();

    /**
     * Icône représentant cette espèce.  Un icône peut servir à positionner sur une
     * carte plusieurs individus d'une même espèce, et peut aussi apparaître devant
     * une étiquette dans les listes déroulantes.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static interface Icon extends javax.swing.Icon
    {
        /**
         * Retourne l'espèce associée à cet icône.
         */
        public abstract Species getSpecies();

        /**
         * Retourne la couleur de cet icône.
         */
        public abstract Color getColor();

        /**
         * Change la couleur de cet icône.
         */
        public abstract void setColor(Color color);
    }
}
