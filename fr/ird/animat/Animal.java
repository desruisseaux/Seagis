/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat;

// J2SE
import java.awt.Shape;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.cs.Ellipsoid;


/**
 * Repr�sentation d'un animal. Chaque animal doit appartenir � une esp�ce,
 * d�crite par un objet {@link Species}. Toutes les coordonn�es sont
 * exprim�es en degr�es de longitude et de latitude selon l'ellipso�de
 * {@link Ellipsoid#WGS84}. Les d�placements d'un animal sont exprim�es
 * en m�tres. Les directions sont exprim�es en degr�s g�ographiques
 * (c'est-�-dire par rapport au nord "vrai").
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Animal
{
    /**
     * Retourne l'esp�ce � laquelle appartient cet animal.
     *
     * @return L'esp�ce � laquelle appartient cet animal.
     */
    public abstract Species getSpecies();

    /**
     * Retourne la r�gion jusqu'o� s'�tend la perception de cette
     * animal. Il peut s'agir par exemple d'un cercle centr� sur
     * la position de l'animal.
     *
     * @param condition 1 si les conditions environnementales sont optimales
     *        (eaux des plus transparentes), ou 0 si les conditions sont des
     *        plus mauvaises (eaux compl�tement brouill�es).
     */
    public Shape getPerceptionArea(final double condition);

    /**
     * Retourne la position de cet animal,
     * en degr�s de longitude et de latitude.
     */
    public abstract Point2D getLocation();

    /**
     * Retourne la direction de cet animal, en degr�s
     * g�ographique par rapport au nord vrai.
     */
    public abstract double getDirection();

    /**
     * Tourne l'animal d'un certain angle par rapport
     * � sa direction actuelle.
     *
     * @param angle Angle en degr�es, dans le sens des
     *        aiguilles d'une montre.
     */
    public abstract void rotate(final double angle);

    /**
     * Avance l'animal d'une certaine distance.
     *
     * @param distance Distance � avancer, en m�tres.
     */
    public abstract void move(final double distance);

    /**
     * Avance l'animal d'une certaine distance dans la direction du point
     * sp�cifi�. Cet animal peut ne pas atteindre le point si la distance
     * est trop courte. Si la distance est trop longue, alors l'animal
     * s'arr�tera � la position du point sp�cifi�. La direction de l'animal
     * sera modifi�e de fa�on � correspondre � la direction vers le nouveau
     * point.
     *
     * @param distance Distance � parcourir, en m�tres. Une valeur n�gative
     *                 fera fuir l'animal.
     * @param point    Point vers lequel avancer, en m�tres. Un point identique
     *                 � <code>this</code> ne fera pas bouger l'animal, quelle
     *                 que soit la valeur de <code>distance</code>.
     * @return <code>true</code> si l'animal a atteint le point sp�cifi�, ou
     *         <code>false</code> s'il s'est d�plac� en direction de ce point
     *         sans l'atteindre.
     */
    public boolean moveToward(final double distance, final Point2D point);

    /**
     * D�place l'animal en fonction de son environnement.
     */
    public abstract void move(final Environment environment);

    /**
     * Retourne le chemin suivit par l'animal jusqu'ici.
     */
    public Shape getPath();
}
