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
 */
package fr.ird.database.sample;

// Seagis
import fr.ird.database.Entry;


/**
 * Un descripteur du paysage oc�anique. Un descripteur est une variable ind�pendante
 * donn�e en entr� aux mod�les lin�aires. Un descripteur du paysage oc�anique comprend:
 * <ul>
 *   <li>un {@linkplain ParameterEntry param�tre environnemental};</li>
 *   <li>une {@linkplain RelativePosition position relative} � laquelle �valuer le param�tre
 *       environnemental;</li>
 *   <li>une {@linkplain OperationEntry op�ration} � appliquer (par exemple un op�rateur de
 *       Sobel pour calculer les gradients);</li>
 *   <li>une distribution th�orique, que l'on essaiera de ramener � la distribution normale.</li>
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
     * Retourne le param�tre environnemental sur lequel est bas� ce descripteur
     * du paysage oc�anique.
     */
    public abstract ParameterEntry getParameter();

    /**
     * Retourne la position relative � laquelle �valuer le {@linkplain #getParameter param�tre
     * environnemental}. Cette position est relative aux positions des �chantillons.
     */
    public abstract RelativePositionEntry getRelativePosition();

    /**
     * Retourne l'op�ration � appliquer sur le
     * {@linkplain #getParameter param�tre environnemental}.
     */
    public abstract OperationEntry getOperation();

    /**
     * Applique un changement de variable, si n�cessaire,
     * de fa�on � obtenir une distribution des valeurs plus proche de la
     * <A HREF="http://mathworld.wolfram.com/NormalDistribution.html">distribution normale</A>.
     * Le changement de variable peut consister par exemple � calculer le logarithme d'une valeur,
     * afin de transformer une
     * <A HREF="http://mathworld.wolfram.com/LogNormalDistribution.html">distribution log-normale</A>
     * en distrution normale.
     *
     * @param  value La valeur � transformer.
     * @return La valeur transform�e, ou <code>value</code> si la distribution des valeurs
     *         �chantillon�es �tait d�j� normale.
     */
    public double normalize(final double value);
}
