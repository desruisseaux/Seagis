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
package fr.ird.animat.seas;

// Interfaces
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.Environment;
import fr.ird.operator.coverage.ParameterValue;

// Géométrie
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;


/**
 * Représentation d'un animal "thon".
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Tuna extends MobileObject implements Animal
{
    /**
     * Rayon de perception de l'animal en mètres.
     */
    private static final double PERCEPTION_RADIUS = 20000;

    /**
     * Espèce à laquelle appartient cet animal.
     */
    private final Species species;

    /**
     * Construit un animal appartenant à l'espèce spécifié.
     *
     * @param species Espèce de l'animal.
     */
    public Tuna(final Species species)
    {
        this.species = species;
    }

    /**
     * Retourne l'espèce à laquelle appartient cet animal.
     *
     * @return L'espèce à laquelle appartient cet animal.
     */
    public Species getSpecies()
    {
        return species;
    }

    /**
     * Retourne la région jusqu'où s'étend la perception de cette
     * animal. Il peut s'agir par exemple d'un cercle centré sur
     * la position de l'animal.
     *
     * @param condition 1 si les conditions environnementales sont optimales
     *        (eaux des plus transparentes), ou 0 si les conditions sont des
     *        plus mauvaises (eaux complètement brouillées).
     */
    public Shape getPerceptionArea(final double condition)
    {
        final double radius = condition*PERCEPTION_RADIUS;
        return relativeToGeographic(new Ellipse2D.Double(-radius, -radius, 2*radius, 2*radius));
    }

    /**
     * Déplace l'animal en fonction de son environnement.
     */
    public void move(final Environment environment)
    {
        final ParameterValue[] perceptions = environment.getParameters(this);
        if (perceptions!=null && perceptions.length!=0)
        {
            Point2D pos = getLocation();
            pos.setLocation(pos.getX()-0.2*(0.5+Math.random()),
                            pos.getY()-0.2*(0.5+Math.random()));
            moveToward(5*1852, pos);
//            moveToward(15*1852, perceptions[0].getLocation());
        }
    }
}
