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


/**
 * Un terme dans un modèle linéaire. Un modèle linéaire peut s'écrire de la forme suivante:
 *
 * <p align="center"><var>y</var> = <var>C</var><sub>0</sub> +
 * <var>C</var><sub>1</sub>&times;<var>x</var><sub>1</sub> +
 * <var>C</var><sub>2</sub>&times;<var>x</var><sub>2</sub> +
 * <var>C</var><sub>3</sub>&times;<var>x</var><sub>3</sub> + ...</p>
 *
 * Dans ce modèle, le terme <var>C</var><sub>0</sub> est représenté par un objet
 * <code>LinearModelTerm</code>, le terme <var>C</var><sub>1</sub>&times;<var>x</var><sub>1</sub>
 * par un autre objet <code>LinearModelTerm</code>, et ainsi de suite.
 *
 * Les variables indépendantes <var>x</var><sub>1</sub>, <var>x</var><sub>2</sub>, etc. sont
 * les {@linkplain DescriptorEntry descripteurs du paysage océanique}, eux-mêmes dérivés de
 * {@linkplain ParameterEntry paramètres environnementaux}.
 *
 * La variable dépendante <var>y</var> sera stockée dans un nouveau paramètre environnemental
 * (par exemple un paramètre appelé "potentiel de pêche"). Elle pourra donc servir d'entrée à
 * un autre modèle linéaire.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see ParameterEntry#getLinearModel
 */
public interface LinearModelTerm {
    /**
     * Retourne le paramètre dans lequel seront stockées les valeurs de la variable
     * dépendante <var>y</var>. Notez que le terme <code>this</code> n'est qu'un des
     * termes composant ce paramètre. La liste complète peut être obtenue avec
     * <code>getTarget().{@linkplain ParameterEntry#getLinearModel() getLinearModel()}</code>.
     */
    public abstract ParameterEntry getTarget();

    /**
     * Retourne les descripteurs du paysage océanique composant ce terme. Par exemple, le terme
     * <code>this</code> pourrait être <var>C</var>&times;<code>SST</code>&times;<code>SLA</code>,
     * ou <var>C</var> est le {@linkplain #getCoefficient coefficient} déterminé par la régression
     * linéaire, tandis que <code>SST</code> et <code>SLA</code> sont les valeurs {@linkplain
     * DescriptorEntry#normalize normalisées} de température de surface et d'anomalie de la hauteur
     * de l'eau respectivement. Pour cet exemple, <code>getDescriptors()</code> retournerait dans
     * une liste les deux descripteurs <code>SST</code> et <code>SLA</code>.
     */
    public abstract List<DescriptorEntry> getDescriptors();

    /**
     * Retourne le coefficient <var>C</var> de ce terme. Ce coefficient a généralement
     * été obtenu par une régression linéaire multiple.
     */
    public double getCoefficient();
}
