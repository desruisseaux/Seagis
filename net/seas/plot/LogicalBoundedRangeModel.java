/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 P�ches et Oc�ans Canada
 *               2000 Institut de Recherche pour le D�veloppement
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
 *           Institut Maurice Lamontagne           Institut de Recherche pour le D�veloppement
 *           850 de la Mer, C.P. 1000              500 rue Jean-Fran�ois Breton
 *           Mont-Joli (Qu�bec)                    34093 Montpellier
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
 * interface tente de contourner une limitation emb�tante de <em>Swing</em>, �
 * savoir que son mod�le ne travaille qu'avec des entiers. Cette classe
 * <code>LogicalBoundedRangeModel</code> offre une m�thode {@link #setLogicalRange}
 * qui permet de sp�cifier les minimums et maximums de la plage de valeurs en
 * utilisant des nombres r�els plut�t que seulement des entiers. Cette plage
 * n'affecte aucunement les minimums et maximums retourn�es comme valeurs enti�res
 * par les m�thodes {@link #getMinimum} et {@link #getMaximum}. Elle n'affecte
 * que la fa�on dont se font les conversions entre les <code>int</code>
 * et les autres types de nombres. Ces conversions se font par les m�thodes
 * {@link #toInteger} et {@link #toLogical}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface LogicalBoundedRangeModel extends BoundedRangeModel
{
    /**
     * Sp�cifie la plage des valeurs non-enti�res. Cette m�thode
     * n'affecte <u>pas</u> la plage des valeur enti�res du mod�le
     * {@link BoundedRangeModel} de base. Toutefois,
     * elle affecte la fa�on dont les conversions seront effectu�es
     * par les m�thodes {@link #toLogical} et {@link #toInteger}.
     *
     * @param minimum Valeur minimale. La valeur <code>null</code>
     *        indique qu'il faut prendre une valeur par d�faut.
     *        La valeur minimale peut �tre obtenue par un appel
     *        � <code>toLogical(getMinimum())</code>.
     *
     * @param maximum Valeur maximale. La valeur <code>null</code>
     *        indique qu'il faut prendre une valeur par d�faut.
     *        La valeur maximale peut �tre obtenue par un appel
     *        � <code>toLogical(getMaximum())</code>.
     */
    public abstract void setLogicalRange(double minimum, double maximum);

    /**
     * Convertit un entier du mod�le vers un nombre plus g�n�ral. La
     * conversion d�pendra de la plage sp�cifi�e par {@link #setRange}.
     * Cette m�thode est l'inverse de {@link #toInteger}.
     */
    public abstract double toLogical(int integer);

    /**
     * Convertit une valeur r�elle en entier utilisable par le
     *  mod�le. La conversion d�pendra de la plage sp�cifi�e par
     * {@link #setLogicalRange}. Cette m�thode est l'inverse de
     * {@link #toLogical}.
     */
    public abstract int toInteger(double logical);
}
