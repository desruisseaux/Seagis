/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.sample;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.coverage.SeriesEntry;


/**
 * Un param�tre environnemental.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getParameters
 */
public interface ParameterEntry extends Entry {
    /**
     * Retourne la s�rie d'image � utiliser pour ce param�tre.
     *
     * @param n 0 pour la s�rie principale, ou 1 pour la s�rie de rechange � utiliser si
     *          jamais la s�rie principale n'est pas disponible.
     * @return  La s�rie d'images, ou <code>null</code> si aucune.
     *          Ce nom ne sera jamais nul pour <code>n=0</code>.
     */
    public abstract SeriesEntry getSeries(int n);

    /**
     * Retourne le num�ro de la bande � utiliser dans les images.
     */
    public abstract int getBand();

    /**
     * Une des composantes d'un {@linkplain ParameterEntry param�tre}.   Un tableau d'objets
     * {@link Component} peut-�tre associ� � un objet {@link ParameterEntry} si ce param�tre
     * est le r�sultat d'une combinaison d'autres param�tres. Ces combinaisons de param�tres
     * peuvent servir par exemple � cr�er une carte de potentiel.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static interface Component extends Entry {
        /**
         * Retourne le param�tre calcul�.
         */
        public abstract ParameterEntry getTarget();

        /**
         * Retourne une des composante du {@linkplain #getTarget param�tre calcul�}.
         * Les valeurs de cette composantes seront {@link #transform transform�e},
         * puis pond�r�es par un {@linkplain #getWeight poids}.
         */
        public abstract ParameterEntry getSource();

        /**
         * Retourne la position relative � laquelle �valuer le {@linkplain #getSource param�tre
         * source}.
         */
        public abstract RelativePositionEntry getRelativePosition();

        /**
         * Retourne l'op�ration � appliquer sur le {@linkplain #getSource param�tre source}.
         */
        public abstract OperationEntry getOperation();

        /**
         * Retourne le poids du {@linkplain #getSource param�tre source}.
         */
        public double getWeight();

        /**
         * Retourne la transformation � appliquer pr�alablement sur le {@linkplain #getSource
         * param�tre source}. Lorsqu'elle n'est pas la transformation identit�e, cette
         * transformation consistera le plus souvent � prendre le logarithme de la valeur
         * afin de donner � la distribution une apparence plus gaussienne.
         */
        public double transform(final double value);
    }
}
