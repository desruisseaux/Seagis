/*
 * OpenGIS implementation in Java
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.map.array;


/**
 * Itérateur balayant les données d'un tableau {@link DefaultArray}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class DefaultIterator extends PointIterator
{
    /**
     * Tableau de données à balayer.
     */
    private final float[] array;

    /**
     * Index suivant celui de la
     * dernière donnée à balayer.
     */
    private final int upper;

    /**
     * Index de la prochaine
     * donnée à retourner.
     */
    private int index;

    /**
     * Construit un itérateur qui balaiera la
     * plage spécifiée d'un tableau de données.
     */
    public DefaultIterator(float[] array, int lower, int upper)
    {
        this.array = array;
        this.index = lower;
        this.upper = upper;
        assert((index & 1)==0);
    }

    /**
     * Indique si les méthodes {@link #next}
     * peuvent retourner d'autres données.
     */
    public boolean hasNext()
    {return index<upper;}

    /**
     * Retourne la valeur de la longitude courante. Avant d'appeller
     * une seconde fois cette méthode, il faudra <g>obligatoirement</g>
     * avoir appelé {@link #nextY}.
     */
    public float nextX()
    {
        assert((index & 1)==0);
        return array[index++];
    }

    /**
     * Retourne la valeur de la latitude courante, puis avance au point
     * suivant. Chaque appel de cette méthode doit <g>obligatoirement</g>
     * avoir été précédée d'un appel à la méthode {@link #nextX}.
     */
    public float nextY()
    {
        assert((index & 1)!=0);
        return array[index++];
    }
}
