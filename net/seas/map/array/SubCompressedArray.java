/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
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
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 3826653288829486841L;

    /**
     * Plage des donn�es valides
     * du tableau {@link #array}.
     */
    protected final int lower, upper;

    /**
     * Construit un sous-tableau � partir
     * d'un autre tableau compress�.
     *
     * @param  other Tableau source.
     * @param  lower Index de la premi�re coordonn�es <var>x</var> �
     *         prendre en compte dans le tableau <code>other</code>.
     * @param  upper Index suivant celui de la derni�re coordonn�e <var>y</var> �
     *         prendre en compte dans le tableau <code>other</code>. La diff�rence
     *         <code>upper-lower</code> doit obligatoirement �tre paire.
     */
    SubCompressedArray(final CompressedArray other, final int lower, final int upper)
    {
        super(other, lower);
        this.lower  = lower;
        this.upper  = upper;

        if (upper-lower < 2)       throw new IllegalArgumentException(Resources.format(Cl�.BAD_RANGE�2, new Integer(lower), new Integer(upper)));
        if (((upper-lower)&1) !=0) throw new IllegalArgumentException(Resources.format(Cl�.ODD_ARRAY_LENGTH�1, new Integer(upper-lower)));
        if (lower < 0)                   throw new ArrayIndexOutOfBoundsException(lower);
        if (upper >= other.array.length) throw new ArrayIndexOutOfBoundsException(upper);
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