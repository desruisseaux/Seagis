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
     * Avance l'animal d'une certaine distance.
     *
     * @param distance Distance � avancer, en m�tres.
     */
    public abstract void move(final double distance);

    /**
     * Tourne l'animal d'un certain angle par rapport
     * � sa direction actuelle.
     *
     * @param angle Angle en degr�es, dans le sens des
     *        aiguilles d'une montre.
     */
    public abstract void rotate(final double angle);

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
}
