/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 P�ches et Oc�ans Canada
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
 * Contact: Observatoire du Saint-Laurent
 *          Institut Maurice Lamontagne
 *          850 de la Mer, C.P. 1000
 *          Mont-Joli (Qu�bec)
 *          G5H 3Z4
 *          Canada
 *
 *          mailto:osl@osl.gc.ca
 */
package net.seas.map.array;


/**
 * Classe enveloppant une portion seulement d'un tableau <code>float[]</code>.
 * Des instances de cette classes sont retourn�es par {@link DefaultArray#subarray}.
 * L'impl�mentation par d�faut de cette classe est imutable. Toutefois, certaines
 * classes d�riv�es (notamment {@link DynamicArray}) ne le seront pas forc�ment.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class SubArray extends DefaultArray
{
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     * TODO: serialver
     */
    // private static final long serialVersionUID = -903700333079078644L;

    /**
     * Plage des donn�es valides
     * du tableau {@link #array}.
     */
    protected int lower, upper;

    /**
     * Enveloppe une partie d'un tableau de <code>float[]</code>.
     *
     * @param  array Tableau de coordonn�es (<var>x</var>,<var>y</var>).
     * @param  lower Index de la premi�re coordonn�es <var>x</var> �
     *         prendre en compte dans le tableau <code>array</code>.
     * @param  upper Index suivant celui de la derni�re coordonn�e <var>y</var> �
     *         prendre en compte dans le tableau <code>array</code>. La diff�rence
     *         <code>upper-lower</code> doit obligatoirement �tre paire.
     */
    public SubArray(final float[] array, final int lower, final int upper)
    {
        super(array);
        this.lower = lower;
        this.upper = upper;
        checkRange(array, lower, upper);
    }

    /**
     * Retourne l'index de la
     * premi�re coordonn�e valide.
     */
    protected final int lower()
    {return lower;}

    /**
     * Retourne l'index suivant celui
     * de la derni�re coordonn�e valide.
     */
    protected final int upper()
    {return upper;}
}
