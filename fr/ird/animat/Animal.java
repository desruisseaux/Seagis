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

// SEAS dependencies
import fr.ird.operator.coverage.ParameterValue;


/**
 * Représentation d'un animal. Chaque animal doit appartenir à une espèce,
 * décrite par un objet {@link Species}. Toutes les coordonnées sont
 * exprimées en degrées de longitude et de latitude selon l'ellipsoïde
 * {@link Ellipsoid#WGS84}. Les déplacements d'un animal sont exprimées
 * en mètres. Les directions sont exprimées en degrés géographiques
 * (c'est-à-dire par rapport au nord "vrai").
 *
 * @version $Id$
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
     * Retourne les observations de l'animal.
     *
     * @return Les observations de l'animal, ou <code>null</code>
     *         si aucune observation n'a encore été faite à la
     *         position actuelle de l'animal.
     */
    public ParameterValue[] getObservations();

    /**
     * Observe l'environnement de l'animal. Cette méthode doit être appelée
     * avant {@link #move}, sans quoi l'animal ne sera pas comment se déplacer.
     *
     * @param environment L'environment à observer.
     */
    public void observe(final Environment environment);

    /**
     * Déplace l'animal en fonction de son environnement. La méthode
     * {@link #observe} doit avoir d'abord été appelée, sans quoi
     * aucun déplacement ne sera fait (l'animal ne sachant pas où aller).
     */
    public void move();

    /**
     * Retourne le chemin suivit par l'animal jusqu'ici.
     */
    public Shape getPath();
}
