/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat;

// J2SE
import java.awt.Shape;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.cs.Ellipsoid;


/**
 * Représentation d'un animal. Chaque animal doit appartenir à une espèce,
 * décrite par un objet {@link Species}. Toutes les coordonnées sont
 * exprimées en degrées de longitude et de latitude selon l'ellipsoïde
 * {@link Ellipsoid#WGS84}. Les déplacements d'un animal sont exprimées
 * en mètres. Les directions sont exprimées en degrés géographiques
 * (c'est-à-dire par rapport au nord "vrai").
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Animal
{
    /**
     * Retourne l'espèce à laquelle appartient cet animal.
     *
     * @return L'espèce à laquelle appartient cet animal.
     */
    public abstract Species getSpecies();

    /**
     * Retourne la région jusqu'où s'étend la perception de cette
     * animal. Il peut s'agir par exemple d'un cercle centré sur
     * la position de l'animal.
     *
     * @param condition 1 si les conditions environnementales sont optimales
     *        (eaux des plus transparentes), ou 0 si les conditions sont des
     *        plus mauvaises (eaux complètement brouillées).
     */
    public Shape getPerceptionArea(final double condition);

    /**
     * Retourne la position de cet animal,
     * en degrés de longitude et de latitude.
     */
    public abstract Point2D getLocation();

    /**
     * Retourne la direction de cet animal, en degrés
     * géographique par rapport au nord vrai.
     */
    public abstract double getDirection();

    /**
     * Tourne l'animal d'un certain angle par rapport
     * à sa direction actuelle.
     *
     * @param angle Angle en degrées, dans le sens des
     *        aiguilles d'une montre.
     */
    public abstract void rotate(final double angle);

    /**
     * Avance l'animal d'une certaine distance.
     *
     * @param distance Distance à avancer, en mètres.
     */
    public abstract void move(final double distance);

    /**
     * Avance l'animal d'une certaine distance dans la direction du point
     * spécifié. Cet animal peut ne pas atteindre le point si la distance
     * est trop courte. Si la distance est trop longue, alors l'animal
     * s'arrêtera à la position du point spécifié. La direction de l'animal
     * sera modifiée de façon à correspondre à la direction vers le nouveau
     * point.
     *
     * @param distance Distance à parcourir, en mètres. Une valeur négative
     *                 fera fuir l'animal.
     * @param point    Point vers lequel avancer, en mètres. Un point identique
     *                 à <code>this</code> ne fera pas bouger l'animal, quelle
     *                 que soit la valeur de <code>distance</code>.
     * @return <code>true</code> si l'animal a atteint le point spécifié, ou
     *         <code>false</code> s'il s'est déplacé en direction de ce point
     *         sans l'atteindre.
     */
    public boolean moveToward(final double distance, final Point2D point);

    /**
     * Déplace l'animal en fonction de son environnement.
     */
    public abstract void move(final Environment environment);

    /**
     * Retourne le chemin suivit par l'animal jusqu'ici.
     */
    public Shape getPath();
}
