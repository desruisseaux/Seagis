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
 * It�rateur balayant les donn�es d'un tableau {@link CompressedArray}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class CompressedIterator extends PointIterator
{
    /**
     * D�calages servant � calculer les
     * valeurs <var>x</var>,<var>y</var>
     * � retourner.
     */
    private int dx,dy;

    /**
     * Valeurs du premier point �
     * partir d'o� seront fait les
     * calculs.
     */
    private final float x0, y0;

    /**
     * Constantes servant � transformer lin�airement les
     * valeurs {@link #array} vers des <code>float</code>.
     */
    private final float scaleX, scaleY;

    /**
     * Tableau de donn�es � balayer.
     */
    private final byte[] array;

    /**
     * Index de la prochaine
     * donn�e � retourner.
     */
    private int index;

    /**
     * Index suivant celui de la
     * derni�re donn�e � balayer.
     */
    private final int upper;

    /**
     * Construit un it�rateur qui
     * balaiera des donn�es compress�es.
     */
    public CompressedIterator(final CompressedArray data, int pointIndex)
    {
        this.scaleX = data.scaleX;
        this.scaleY = data.scaleY;
        this.array  = data.array;
        this.x0     = data.x0;
        this.y0     = data.y0;
        this.index  = data.lower();
        this.upper  = data.upper();
        if (pointIndex >= 0)
        {
            while (--pointIndex>=0)
            {
                dx += array[index++];
                dy += array[index++];
            }
        }
        else throw new IndexOutOfBoundsException(String.valueOf(pointIndex));
    }

    /**
     * Indique si les m�thodes {@link #next}
     * peuvent retourner d'autres donn�es.
     */
    public boolean hasNext()
    {return index<upper;}

    /**
     * Retourne la valeur de la longitude courante. Avant d'appeller
     * une seconde fois cette m�thode, il faudra <g>obligatoirement</g>
     * avoir appel� {@link #nextY}.
     */
    public float nextX()
    {
        assert((index & 1)==0);
        dx += array[index++];
        return x0 + scaleX*dx;
    }

    /**
     * Retourne la valeur de la latitude courante, puis avance au point
     * suivant. Chaque appel de cette m�thode doit <g>obligatoirement</g>
     * avoir �t� pr�c�d�e d'un appel � la m�thode {@link #nextX}.
     */
    public float nextY()
    {
        assert((index & 1)==1);
        dy += array[index++];
        return y0 + scaleY*dy;
    }
}
