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

// Geometry
import java.awt.geom.Point2D;

// Divers
import net.seagis.resources.XMath;
import net.seagis.resources.Utilities;


/**
 * Représentation d'un animal "thon".
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Tuna extends Point2D implements Animal
{
    /**
     * L'objet à utiliser pour calculer les déplacements. La version actuelle
     * utilise le même objet pour tous, mais une version future va peut-être
     * donner un objet à certaines populations pour une exécution multi-threads.
     */
    private static final Projector projector = new Projector();

    /**
     * Espèce à laquelle appartient cet animal.
     */
    private final Species species;

    /**
     * Coordonnées <var>x</var> de cet animal, en degrés de longitude.
     */
    private float x;

    /**
     * Coordonnées <var>y</var> de cet animal, en degrés de latitude.
     */
    private float y;

    /**
     * Direction actuelle de cet animal, en <u>radians arithmétique</u>.
     * La valeur par défaut est PI/2, ce qui correspond au nord vrai.
     */
    private float direction = (float) (Math.PI/2);

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
     * Tourne l'animal d'un certain angle par rapport
     * à sa direction actuelle.
     *
     * @param angle Angle en degrées, dans le sens des
     *        aiguilles d'une montre.
     */
    public void rotate(final double angle)
    {
        direction -= Math.toRadians(angle);
    }

    /**
     * Avance l'animal d'une certaine distance.
     *
     * @param distance Distance à avancer, en mètres.
     */
    public void move(final double distance)
    {
        projector.setCentre(this);
        projector.x = distance * Math.cos(direction);
        projector.y = distance * Math.sin(direction);
        projector.toGeographic();
        setLocation(projector);
    }

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
     */
    public void moveToward(final double distance, final Point2D point)
    {
        projector.setCentre(this);
        projector.setLocation(point);
        projector.toProjected();
        double dx = projector.x;
        double dy = projector.y;
        double newDirection = Math.atan2(dy, dx);
        if (!java.lang.Double.isNaN(newDirection))
        {
            direction = (float) newDirection;
        }
        double fc = distance / XMath.hypot(dx, dy);
        if (!java.lang.Double.isInfinite(fc) && fc<1)
        {
            projector.x = dx*fc;
            projector.y = dy*fc;
            projector.toGeographic();
            setLocation(projector);
        }
        else
        {
            setLocation(point);
        }
    }

    /**
     * Retourne la direction de cet animal, en degrés
     * géographique par rapport au nord vrai.
     */
    public double getDirection()
    {
        return 90-Math.toDegrees(direction);
    }

    /**
     * Retourne la position de cet animal,
     * en degrés de longitude et de latitude.
     */
    public Point2D getLocation()
    {
        return new Point2D.Float(x,y);
    }

    /**
     * Retourne la coordonnées <var>x</var> de ce thon en degrés de longitude.
     */
    public double getX()
    {
        return x;
    }

    /**
     * Retourne la coordonnées <var>y</var> de ce thon en degrés de latitude.
     */
    public double getY()
    {
        return y;
    }

    /**
     * Définit la coordonnées <code>x,y</code> de ce thon.
     */
    public void setLocation(final double x, final double y)
    {
        this.x = (float) x;
        this.y = (float) y;
    }

    /**
     * Retourne une chaîne de caractère représentant
     * cet animal. Cette information est utile pour
     * faciliter les déboguages.
     */
    public String toString()
    {
        return Utilities.getShortClassName(this)+'['+x+", "+y+']';
    }
}
