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

// SEAS dependencies
import fr.ird.operator.coverage.ParameterValue;


/**
 * Repr�sentation d'un animal. Chaque animal doit appartenir � une esp�ce,
 * d�crite par un objet {@link Species}. Toutes les coordonn�es sont
 * exprim�es en degr�es de longitude et de latitude selon l'ellipso�de
 * {@link Ellipsoid#WGS84}. Les d�placements d'un animal sont exprim�es
 * en m�tres. Les directions sont exprim�es en degr�s g�ographiques
 * (c'est-�-dire par rapport au nord "vrai").
 *
 * @version $Id$
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
     * Retourne les observations de l'animal.
     *
     * @return Les observations de l'animal, ou <code>null</code>
     *         si aucune observation n'a encore �t� faite � la
     *         position actuelle de l'animal.
     */
    public ParameterValue[] getObservations();

    /**
     * Observe l'environnement de l'animal. Cette m�thode doit �tre appel�e
     * avant {@link #move}, sans quoi l'animal ne sera pas comment se d�placer.
     *
     * @param environment L'environment � observer.
     */
    public void observe(final Environment environment);

    /**
     * D�place l'animal en fonction de son environnement. La m�thode
     * {@link #observe} doit avoir d'abord �t� appel�e, sans quoi
     * aucun d�placement ne sera fait (l'animal ne sachant pas o� aller).
     */
    public void move();

    /**
     * Retourne le chemin suivit par l'animal jusqu'ici.
     */
    public Shape getPath();
}
