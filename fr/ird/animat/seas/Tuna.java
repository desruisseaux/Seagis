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
 * Repr�sentation d'un animal "thon".
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Tuna extends Point2D implements Animal
{
    /**
     * L'objet � utiliser pour calculer les d�placements. La version actuelle
     * utilise le m�me objet pour tous, mais une version future va peut-�tre
     * donner un objet � certaines populations pour une ex�cution multi-threads.
     */
    private static final Projector projector = new Projector();

    /**
     * Esp�ce � laquelle appartient cet animal.
     */
    private final Species species;

    /**
     * Coordonn�es <var>x</var> de cet animal, en degr�s de longitude.
     */
    private float x;

    /**
     * Coordonn�es <var>y</var> de cet animal, en degr�s de latitude.
     */
    private float y;

    /**
     * Direction actuelle de cet animal, en <u>radians arithm�tique</u>.
     * La valeur par d�faut est PI/2, ce qui correspond au nord vrai.
     */
    private float direction = (float) (Math.PI/2);

    /**
     * Construit un animal appartenant � l'esp�ce sp�cifi�.
     *
     * @param species Esp�ce de l'animal.
     */
    public Tuna(final Species species)
    {
        this.species = species;
    }

    /**
     * Retourne l'esp�ce � laquelle appartient cet animal.
     *
     * @return L'esp�ce � laquelle appartient cet animal.
     */
    public Species getSpecies()
    {
        return species;
    }

    /**
     * Tourne l'animal d'un certain angle par rapport
     * � sa direction actuelle.
     *
     * @param angle Angle en degr�es, dans le sens des
     *        aiguilles d'une montre.
     */
    public void rotate(final double angle)
    {
        direction -= Math.toRadians(angle);
    }

    /**
     * Avance l'animal d'une certaine distance.
     *
     * @param distance Distance � avancer, en m�tres.
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
     * Retourne la direction de cet animal, en degr�s
     * g�ographique par rapport au nord vrai.
     */
    public double getDirection()
    {
        return 90-Math.toDegrees(direction);
    }

    /**
     * Retourne la position de cet animal,
     * en degr�s de longitude et de latitude.
     */
    public Point2D getLocation()
    {
        return new Point2D.Float(x,y);
    }

    /**
     * Retourne la coordonn�es <var>x</var> de ce thon en degr�s de longitude.
     */
    public double getX()
    {
        return x;
    }

    /**
     * Retourne la coordonn�es <var>y</var> de ce thon en degr�s de latitude.
     */
    public double getY()
    {
        return y;
    }

    /**
     * D�finit la coordonn�es <code>x,y</code> de ce thon.
     */
    public void setLocation(final double x, final double y)
    {
        this.x = (float) x;
        this.y = (float) y;
    }

    /**
     * Retourne une cha�ne de caract�re repr�sentant
     * cet animal. Cette information est utile pour
     * faciliter les d�boguages.
     */
    public String toString()
    {
        return Utilities.getShortClassName(this)+'['+x+", "+y+']';
    }
}
