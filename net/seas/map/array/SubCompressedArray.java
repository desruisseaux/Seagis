/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 Pêches et Océans Canada
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
 *          Mont-Joli (Québec)
 *          G5H 3Z4
 *          Canada
 *
 *          mailto:osl@osl.gc.ca
 */
package net.seas.map.array;

// Divers
import net.seas.resources.Resources;


/**
 * Classe enveloppant une portion seulement d'un tableau {@link CompressedArray}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class SubCompressedArray extends CompressedArray
{
    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     * TODO: serialver
     */
    // private static final long serialVersionUID = -903700333079078644L;

    /**
     * Plage des données valides
     * du tableau {@link #array}.
     */
    protected final int lower, upper;

    /**
     * Construit un sous-tableau à partir
     * d'un autre tableau compressé.
     *
     * @param  other Tableau source.
     * @param  lower Index de la première coordonnées <var>x</var> à
     *         prendre en compte dans le tableau <code>other</code>.
     * @param  upper Index suivant celui de la dernière coordonnée <var>y</var> à
     *         prendre en compte dans le tableau <code>other</code>. La différence
     *         <code>upper-lower</code> doit obligatoirement être paire.
     */
    SubCompressedArray(final CompressedArray other, final int lower, final int upper)
    {
        super(other, lower);
        this.lower  = lower;
        this.upper  = upper;

        if (upper-lower < 2)       throw new IllegalArgumentException(Resources.format(Clé.BAD_RANGE¤2, new Integer(lower), new Integer(upper)));
        if (((upper-lower)&1) !=0) throw new IllegalArgumentException(Resources.format(Clé.ODD_ARRAY_LENGTH¤1, new Integer(upper-lower)));
        if (lower < 0)                   throw new ArrayIndexOutOfBoundsException(lower);
        if (upper >= other.array.length) throw new ArrayIndexOutOfBoundsException(upper);
    }

    /**
     * Retourne l'index de la
     * première coordonnée valide.
     */
    protected final int lower()
    {return lower;}

    /**
     * Retourne l'index suivant celui
     * de la dernière coordonnée valide.
     */
    protected final int upper()
    {return upper;}
}
