/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.corssh;

// Exception
import java.util.ConcurrentModificationException;


/**
 * Itérateur balayant un ensemble d'enregistrements. Contrairement à {@link Parser}, cet
 * itérateur  ne fait aucune garantie sur l'ordre dans lequel les enregistrements seront
 * retournés: ils peuvent être ou ne pas être en ordre croissant de date. Les itérateurs
 * sont typiquement créés par un objet  {@link Buffer}  pour balayer les enregistrements
 * compris dans une certaine région et plage de temps. Il s'utilise comme suit:
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
 * Un itérateur ne contient généralement pas de copie des données de {@link Buffer},  mais
 * ne fait qu'accéder aux données existantes dans un certain ordre. Toutes les méthodes de
 * cette interface peuvent lancer une exception {@link ConcurrentModificationException} si
 * la source des données ({@link Buffer}) a changée depuis la création de l'itérateur.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Iterator
{
    /**
     * Avance la position de l'itérateur vers l'enregistrement suivant. Au moment
     * de sa création, l'itérateur est positionné avant le premier enregistrement.
     * Il faut donc appeller cette méthode une fois avant de lire le premier
     * enregistrement.
     *
     * @return <code>true</code> si l'opération a réussi, ou <code>false</code>
     *         s'il ne restait plus d'enregistrements a balayer.
     */
    public abstract boolean next();
    
    /**
     * Retourne la longitude à laquelle fut prise la donnée.
     * Cette longitude est exprimée en degrés entre 0 et 360.
     */
    public abstract double getX();
    
    /**
     * Retourne la latitude à laquelle fut prise la donnée.
     * Cette latitude est exprimée en degrés entre -90 et +90.
     */
    public abstract double getY();
    
    /**
     * Retourne la hauteur ou l'anomalie de hauteur de l'eau à la position de ce
     * point. La quantité calculée dépend de l'objet {@link Buffer} qui a produit
     * cet itérateur.
     *
     * Note: Dans une version future, cette méthode sera remplacée par <code>getField(int)</code>
     *       qui fonctionnera comme {@link Parser#getField(int)}. (TODO).
     */
    public abstract int getField();

    /**
     * Retourne la date de l'enregistrement en nombre de
     * millisecondes écoulées depuis le 1er janvier 1970.
     */
    public abstract long getTime();
}
