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
import java.util.Iterator;
import java.awt.geom.Point2D;


/**
 * It�rateur balayant les donn�es d'un tableau {@link PointArray}. Cet it�rateur est obtenu par
 * un appel � {@link PointArray#iterator}.  Le balayage se fait g�n�ralement par des appels aux
 * aux m�thodes {@link #nextX} et {@link #nextY}, qui retournent les donn�es sous forme de nombres
 * r�els <code>float</code>. Toutefois, ces m�thodes <g>doivent toujours</g> �tre appell�es dans
 * cet ordre: {@link #nextX} d'abord, suivit de {@link #nextY}. Tout manquement � cette r�gle (par
 * exemple appeller {@link #nextX} deux fois de suite) peut produire des r�sultats erron�es.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class PointIterator implements Iterator<Point2D>, Cloneable
{
    /**
     * Constructeur par d�faut.
     */
    protected PointIterator()
    {}

    /**
     * Indique si les m�thodes {@link #next}
     * peuvent retourner d'autres donn�es.
     */
    public abstract boolean hasNext();

    /**
     * Retourne la valeur de la longitude courante. Avant d'appeller
     * une seconde fois cette m�thode, il faudra <g>obligatoirement</g>
     * avoir appel� {@link #nextY}.
     */
    public abstract float nextX();

    /**
     * Retourne la valeur de la latitude courante, puis avance au point
     * suivant. Chaque appel de cette m�thode doit <g>obligatoirement</g>
     * avoir �t� pr�c�d�e d'un appel � la m�thode {@link #nextX}.
     */
    public abstract float nextY();

    /**
     * Retourne la valeur du point courant dans un objet {@link Point2D},
     * puis avance au point suivant. Cette m�thode combine un appel de
     * {@link #nextX} suivit de {@link #nextY}.
     */
    public final Point2D next()
    {return new Point2D.Float(nextX(), nextY());}

    /**
     * Op�ration non-support�e.
     */
    public final void remove()
    {throw new UnsupportedOperationException();};

    /**
     * Retourne une copie conforme de cet it�rateur. Cette
     * copie peut �tre utile pour balayer une seconde fois
     * les m�mes donn�es � partir du point o� se trouve cet
     * it�rateur.
     */
    public final PointIterator clone()
    {
        try
        {
            return (PointIterator) super.clone();
        }
        catch (CloneNotSupportedException exception)
        {
            InternalError e=new InternalError(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }
}
