/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contacts: Observatoire du Saint-Laurent         Michel Petit
 *           Institut Maurice Lamontagne           Institut de Recherche pour le Développement
 *           850 de la Mer, C.P. 1000              500 rue Jean-François Breton
 *           Mont-Joli (Québec)                    34093 Montpellier
 *           G5H 3Z4                               France
 *           Canada
 *
 *           mailto:osl@osl.gc.ca                  mailto:Michel.Petit@teledetection.fr
 */
package net.seas.plot;

// Model
import javax.swing.BoundedRangeModel;


/**
 * Extension de l'interface {@link BoundedRangeModel} de <em>Swing</em>. Cette
 * interface tente de contourner une limitation embêtante de <em>Swing</em>, à
 * savoir que son modèle ne travaille qu'avec des entiers. Cette classe
 * <code>LogicalBoundedRangeModel</code> offre une méthode {@link #setLogicalRange}
 * qui permet de spécifier les minimums et maximums de la plage de valeurs en
 * utilisant des nombres réels plutôt que seulement des entiers. Cette plage
 * n'affecte aucunement les minimums et maximums retournées comme valeurs entières
 * par les méthodes {@link #getMinimum} et {@link #getMaximum}. Elle n'affecte
 * que la façon dont se font les conversions entre les <code>int</code>
 * et les autres types de nombres. Ces conversions se font par les méthodes
 * {@link #toInteger} et {@link #toLogical}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface LogicalBoundedRangeModel extends BoundedRangeModel
{
    /**
     * Spécifie la plage des valeurs non-entières. Cette méthode
     * n'affecte <u>pas</u> la plage des valeur entières du modèle
     * {@link BoundedRangeModel} de base. Toutefois,
     * elle affecte la façon dont les conversions seront effectuées
     * par les méthodes {@link #toLogical} et {@link #toInteger}.
     *
     * @param minimum Valeur minimale. La valeur <code>null</code>
     *        indique qu'il faut prendre une valeur par défaut.
     *        La valeur minimale peut être obtenue par un appel
     *        à <code>toLogical(getMinimum())</code>.
     *
     * @param maximum Valeur maximale. La valeur <code>null</code>
     *        indique qu'il faut prendre une valeur par défaut.
     *        La valeur maximale peut être obtenue par un appel
     *        à <code>toLogical(getMaximum())</code>.
     */
    public abstract void setLogicalRange(double minimum, double maximum);

    /**
     * Convertit un entier du modèle vers un nombre plus général. La
     * conversion dépendra de la plage spécifiée par {@link #setRange}.
     * Cette méthode est l'inverse de {@link #toInteger}.
     */
    public abstract double toLogical(int integer);

    /**
     * Convertit une valeur réelle en entier utilisable par le
     *  modèle. La conversion dépendra de la plage spécifiée par
     * {@link #setLogicalRange}. Cette méthode est l'inverse de
     * {@link #toLogical}.
     */
    public abstract int toInteger(double logical);
}
