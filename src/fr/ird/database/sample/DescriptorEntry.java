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
 */
package fr.ird.database.sample;

// Seagis
import fr.ird.database.Entry;


/**
 * Un descripteur du paysage océanique. Un descripteur est une variable indépendante
 * donnée en entré aux modèles linéaires. Un descripteur du paysage océanique comprend:
 * <ul>
 *   <li>un {@linkplain ParameterEntry paramètre environnemental};</li>
 *   <li>une {@linkplain RelativePosition position relative} à laquelle évaluer le paramètre
 *       environnemental;</li>
 *   <li>une {@linkplain OperationEntry opération} à appliquer (par exemple un opérateur de
 *       Sobel pour calculer les gradients);</li>
 *   <li>une distribution théorique, que l'on essaiera de ramener à la distribution normale.</li>
 * </ul>
 *
 * @see LinearModelTerm#getDescriptors
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface DescriptorEntry extends Entry {
    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();

    /**
     * Retourne le paramètre environnemental sur lequel est basé ce descripteur
     * du paysage océanique.
     */
    public abstract ParameterEntry getParameter();

    /**
     * Retourne la position relative à laquelle évaluer le {@linkplain #getParameter paramètre
     * environnemental}. Cette position est relative aux positions des échantillons.
     */
    public abstract RelativePositionEntry getRelativePosition();

    /**
     * Retourne l'opération à appliquer sur le
     * {@linkplain #getParameter paramètre environnemental}.
     */
    public abstract OperationEntry getOperation();

    /**
     * Applique un changement de variable, si nécessaire,
     * de façon à obtenir une distribution des valeurs plus proche de la
     * <A HREF="http://mathworld.wolfram.com/NormalDistribution.html">distribution normale</A>.
     * Le changement de variable peut consister par exemple à calculer le logarithme d'une valeur,
     * afin de transformer une
     * <A HREF="http://mathworld.wolfram.com/LogNormalDistribution.html">distribution log-normale</A>
     * en distrution normale.
     *
     * @param  value La valeur à transformer.
     * @return La valeur transformée, ou <code>value</code> si la distribution des valeurs
     *         échantillonées était déjà normale.
     */
    public double normalize(final double value);
}
