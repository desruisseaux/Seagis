/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample;

// J2SE
import java.util.List;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.coverage.SeriesEntry;


/**
 * Un paramètre environnemental.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getParameters
 */
public interface ParameterEntry extends Entry {
    /**
     * Retourne la série d'image à utiliser pour ce paramètre.
     *
     * @param n 0 pour la série principale, ou 1 pour la série de rechange à utiliser si
     *          jamais la série principale n'est pas disponible.
     * @return  La série d'images, ou <code>null</code> si aucune.
     *          Ce nom ne sera jamais nul pour <code>n=0</code>.
     */
    public abstract SeriesEntry getSeries(int n);

    /**
     * Retourne le numéro de la bande à utiliser dans les images.
     */
    public abstract int getBand();

    /**
     * Retourne les composantes consituant ce paramètre, ou <code>null</code> s'il n'y en a pas.
     * Un paramètre peut être le résultat d'une combinaison (par forcément linéaire) de d'autres
     * paramètres. L'interface {@link Component} spécifie un de ces paramètre ainsi que son poids
     * relatif.
     *
     * @return La liste de tous les paramètres composant celui-ci, ou <code>null</code> s'il n'y
     *         en a pas.
     */
    public abstract List<Component> getComponents();

    /**
     * Une des composantes d'un {@linkplain ParameterEntry paramètre}.   Un tableau d'objets
     * {@link Component} peut-être associé à un objet {@link ParameterEntry} si ce paramètre
     * est le résultat d'une combinaison d'autres paramètres. Ces combinaisons de paramètres
     * peuvent servir par exemple à créer une carte de potentiel.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static interface Component extends Entry {
        /**
         * Retourne le paramètre calculé.
         */
        public abstract ParameterEntry getTarget();

        /**
         * Retourne une des composante du {@linkplain #getTarget paramètre calculé}.
         * Les valeurs de cette composantes seront {@link #transform transformée},
         * puis pondérées par un {@linkplain #getWeight poids}.
         */
        public abstract ParameterEntry getSource();

        /**
         * Retourne la position relative à laquelle évaluer le {@linkplain #getSource paramètre
         * source}.
         */
        public abstract RelativePositionEntry getRelativePosition();

        /**
         * Retourne l'opération à appliquer sur le {@linkplain #getSource paramètre source}.
         */
        public abstract OperationEntry getOperation();

        /**
         * Retourne le poids du {@linkplain #getSource paramètre source}.
         */
        public double getWeight();

        /**
         * Retourne la transformation à appliquer préalablement sur le {@linkplain #getSource
         * paramètre source}. Lorsqu'elle n'est pas la transformation identitée, cette
         * transformation consistera le plus souvent à prendre le logarithme de la valeur
         * afin de donner à la distribution une apparence plus gaussienne.
         */
        public double transform(final double value);
    }
}
