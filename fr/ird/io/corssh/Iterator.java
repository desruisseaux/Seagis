/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.corssh;

// Exception
import java.util.ConcurrentModificationException;


/**
 * It�rateur balayant un ensemble d'enregistrements. Contrairement � {@link Parser}, cet
 * it�rateur  ne fait aucune garantie sur l'ordre dans lequel les enregistrements seront
 * retourn�s: ils peuvent �tre ou ne pas �tre en ordre croissant de date. Les it�rateurs
 * sont typiquement cr��s par un objet  {@link Buffer}  pour balayer les enregistrements
 * compris dans une certaine r�gion et plage de temps. Il s'utilise comme suit:
 *
 * <blockquote><pre>
 * Iterator it = ...
 * while (it.next())
 * {
 *     double x = it.getX();
 *     double y = it.getY();
 *     // etc...
 * }
 * </pre></blockquote>
 *
 * Un it�rateur ne contient g�n�ralement pas de copie des donn�es de {@link Buffer},  mais
 * ne fait qu'acc�der aux donn�es existantes dans un certain ordre. Toutes les m�thodes de
 * cette interface peuvent lancer une exception {@link ConcurrentModificationException} si
 * la source des donn�es ({@link Buffer}) a chang�e depuis la cr�ation de l'it�rateur.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Iterator
{
    /**
     * Avance la position de l'it�rateur vers l'enregistrement suivant. Au moment
     * de sa cr�ation, l'it�rateur est positionn� avant le premier enregistrement.
     * Il faut donc appeller cette m�thode une fois avant de lire le premier
     * enregistrement.
     *
     * @return <code>true</code> si l'op�ration a r�ussi, ou <code>false</code>
     *         s'il ne restait plus d'enregistrements a balayer.
     */
    public abstract boolean next();
    
    /**
     * Retourne la longitude � laquelle fut prise la donn�e.
     * Cette longitude est exprim�e en degr�s entre 0 et 360.
     */
    public abstract double getX();
    
    /**
     * Retourne la latitude � laquelle fut prise la donn�e.
     * Cette latitude est exprim�e en degr�s entre -90 et +90.
     */
    public abstract double getY();
    
    /**
     * Retourne la hauteur ou l'anomalie de hauteur de l'eau � la position de ce
     * point. La quantit� calcul�e d�pend de l'objet {@link Buffer} qui a produit
     * cet it�rateur.
     *
     * Note: Dans une version future, cette m�thode sera remplac�e par <code>getField(int)</code>
     *       qui fonctionnera comme {@link Parser#getField(int)}. (TODO).
     */
    public abstract int getField();

    /**
     * Retourne la date de l'enregistrement en nombre de
     * millisecondes �coul�es depuis le 1er janvier 1970.
     */
    public abstract long getTime();
}
