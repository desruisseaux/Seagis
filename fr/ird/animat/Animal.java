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
     * Avance l'animal d'une certaine distance.
     *
     * @param distance Distance à avancer, en mètres.
     */
    public abstract void move(final double distance);

    /**
     * Tourne l'animal d'un certain angle par rapport
     * à sa direction actuelle.
     *
     * @param angle Angle en degrées, dans le sens des
     *        aiguilles d'une montre.
     */
    public abstract void rotate(final double angle);

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
}
