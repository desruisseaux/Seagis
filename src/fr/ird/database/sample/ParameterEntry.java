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

// J2SE
import java.util.List;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.coverage.SeriesEntry;


/**
 * Un paramètre environnemental. Les paramètres sont souvent associés à une
 * {@linklplain SeriesEntry série} de la base de données d'images. Un paramètre
 * environnemental peut être par exemple la température de surface de la mer, ou
 * la concentration en chlorophylle-a.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getParameters
 */
public interface ParameterEntry extends Entry {
    /**
     * Retourne un numéro unique identifiant ce paramètre.
     */
    public abstract int getID();

    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();

    /**
     * Retourne <code>true</code> si ce paramètre est le <cite>paramètre identité</cite>.
     * Le "paramètre identité" est un paramètre artificiel représentant une image
     * dont tous les pixels auraient la valeur 1. Il est utilisé dans des expressions de
     * la forme <code>y = C0 + C1*x + C2*x² + ...</code>, ou <code>C0</code> peut s'écrire
     * <code>C0&times;identité</code>.
     */
    public abstract boolean isIdentity();

    /**
     * Retourne la série d'image à utiliser pour ce paramètre.
     *
     * @param n 0 pour la série principale, ou 1 pour la série de rechange à utiliser si
     *          jamais la série principale n'est pas disponible.
     * @return  La série d'images, ou <code>null</code> si aucune.
     *          Cette référence ne sera jamais nulle si <code>n=0</code>.
     */
    public abstract SeriesEntry getSeries(int n);

    /**
     * Retourne le numéro de la bande à utiliser dans les images.
     */
    public abstract int getBand();

    /**
     * Retourne les termes d'un modèle linéaire calculant ce paramètre, ou <code>null</code>
     * s'il n'y a pas de modèle linéaire. Un paramètre peut être le résultat d'une combinaison
     * de d'autres paramètres, par exemple sous la forme de l'équation suivante:
     *
     * <p align="center"><code>PP</code> = <var>C</var><sub>0</sub> +
     * <var>C</var><sub>1</sub>&times;<code>SST</code> +
     * <var>C</var><sub>2</sub>&times;<code>SLA</code> +
     * <var>C</var><sub>3</sub>&times;<code>SST</code>&times;<code>SLA</code> + ...</p>
     *
     * Chacun des termes à droite du signe = est décrit par un objet {@link LinearModelTerm}.
     * Ces descriptions incluent le coefficient <var>C</var><sub>n</sub>, qui résulte
     * généralement d'une régression linéaire multiple.
     *
     * @return La liste de tous les termes composant le modèle linéaire,
     *         ou <code>null</code> s'il n'y en a pas. Cette liste est immutable.
     */
    public abstract List<? extends LinearModelTerm> getLinearModel();
}
