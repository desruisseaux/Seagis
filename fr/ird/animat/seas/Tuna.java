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

// J2SE standard
import java.util.Arrays;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

// Geotools dependencies
import org.geotools.resources.XArray;

// Interfaces
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.Environment;
import fr.ird.operator.coverage.ParameterValue;


/**
 * Représentation d'un animal "thon". En plus d'être mobile,
 * cet animal est attentif aux signaux de son environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Tuna extends MobileObject implements Animal
{
    /**
     * Rayon de perception de l'animal en mètres.
     */
    private static final double PERCEPTION_RADIUS = 20000;

    /**
     * Index relatif de la coordonnées <var>x</var> dans le tableau {@link #parameters}.
     */
    private static final int LONGITUDE = 0;

    /**
     * Index relatif de la coordonnées <var>y</var> dans le tableau {@link #parameters}.
     */
    private static final int LATITUDE = 1;

    /**
     * Index relatif de la valeur <var>value</var> dans le tableau {@link #parameters}.
     */
    private static final int VALUE = 2;

    /**
     * Nombre d'éléments dans chaque enregistrement <code>parameters</code>.
     */
    private static final int RECORD_LENGTH = 3;

    /**
     * Les valeurs des paramètres mesurés. Chaque pas de temps peux mesurer
     * plusieurs paramètres, et chaque paramètres contient trois éléments:
     * la position (<var>x</var>,<var>y</var>) en coordonnées géographiques
     * ainsi que la valeur <var>value</var> du paramètre.
     */
    private float[] parameters = new float[8];

    /**
     * Longueur valide du tableau {@link #parameters}. Le nombre d'éléments est cette
     * longueur divisée par <code>{@link #paramCount}*{@link #RECORD_LENGTH}</code>.
     */
    private int validLength;

    /**
     * Nombre de paramètres.
     */
    private static final int paramCount = 1;

    /**
     * Espèce à laquelle appartient cet animal.
     */
    private final Species species;

    /**
     * Construit un animal appartenant à l'espèce spécifié. L'animal n'a pas de
     * position initiale. Appellez {@link #setLocation} après la construction de
     * cet animal pour lui affecter une position.
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
     * Déplace l'animal en fonction de son environnement.
     */
    public void move(final Environment environment)
    {
        observe(environment);
        final ParameterValue param = getObservation(0);
        if (param!=null)
        {
            final Point2D location = param.getLocation();
            if (location != null)
            {
                moveToward(5*1852, location);
            }
        }
    }

    /**
     * Prend note des paramètres environnementaux autour de cet animal.
     *
     * @param environment L'environnement de l'animal.
     */
    public void observe(final Environment environment)
    {
        final ParameterValue[] perceptions = environment.getParameters(this);
        final int requiredLength = getPointCount()*(paramCount*RECORD_LENGTH);
        if (requiredLength > parameters.length)
        {
            parameters = XArray.resize(parameters, requiredLength + Math.min(requiredLength, 4096));
            Arrays.fill(parameters, validLength, parameters.length, Float.NaN);
        }
        if (requiredLength != 0) // 'false' if initial position is not set.
        {
            validLength = requiredLength - (paramCount*RECORD_LENGTH);
            for (int i=0; i<paramCount; i++)
            {
                Point2D pos = getLocation();
                final ParameterValue value = perceptions[i];
                parameters[validLength + LONGITUDE] = (float) (pos.getX()-0.2*(0.5+Math.random()));
                parameters[validLength +  LATITUDE] = (float) (pos.getY()-0.2*(0.5+Math.random()));
                parameters[validLength +     VALUE] = (float) value.getValue();
                validLength += RECORD_LENGTH;
            }
        }
        assert validLength == requiredLength;
    }

    /**
     * Retourne une observation de l'animal.
     *
     * @param  index L'index du paramètre, de 0 jusqu'à
     *         <code>{@link #getNumParameters()}-1</code>.
     * @return L'observation de l'animal, ou <code>null</code>
     *         si aucune observation n'a encore été faite à la
     *         position actuelle de l'animal.
     */
    public ParameterValue getObservation(final int index)
    {
        if (index<0 || index>=paramCount)
        {
            throw new IndexOutOfBoundsException("Not a valid parameter index: "+index);
        }
        int pos = getObservationCount();
        if (pos!=0 && pos==getPointCount())
        {
            pos = (pos-1) * (paramCount*RECORD_LENGTH);
            pos += index*RECORD_LENGTH;
            final float x = parameters[pos + LONGITUDE];
            final float y = parameters[pos +  LATITUDE];
            final float z = parameters[pos +     VALUE];
            final ParameterValue.Float param = new ParameterValue.Float("param #1"); // TODO
            param.setValue(z,x,y);
            return param;
        }
        return null;
    }

    /**
     * Retourne le nombre d'observations. Ce nombre devrait être égal au nombre
     * de points, sauf si l'utilisateur fait déplacer l'animal sans appeller la
     * méthode {@link #observe}.
     */
    private int getObservationCount()
    {
        assert (validLength % (paramCount*RECORD_LENGTH)) == 0;
        final int count = validLength / (paramCount*RECORD_LENGTH);
        assert count <= getPointCount() : count;
        return count;
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
}
