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

// J2SE et JAI
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Point2D;
import javax.media.jai.GraphicsJAI;

// Ensembles
import java.util.Map;
import java.util.HashMap;

// Composantes cartographiques
import net.seas.awt.geom.Arrow2D;
import net.seas.map.layer.MarkLayer;

// Animats
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;


/**
 * Couche représentant une population sur une carte.
 *
 * @version 1.0
 * @author Martin Desruisseaus
 */
final class PopulationLayer extends MarkLayer implements PopulationChangeListener
{
    /**
     * Forme géométrique représentant les animaux.
     */
    private static final Shape DEFAULT_SHAPE = new Arrow2D(0, -4, 10, 8);

    /**
     * Liste des animaux formant cette population.
     */
    private Animal[] animals;

    /**
     * Couleurs et symboles des espèces.
     */
    private final Map<Species,Species.Icon> icons = new HashMap<Species,Species.Icon>();

    /**
     * Construit une couche pour la population spécifiée.
     */
    public PopulationLayer(final Population population)
    {
        refresh(population);
        population.addPopulationChangeListener(this);
    }

    /**
     * Remet à jour cette composante pour la population spécifiée.
     */
    private void refresh(final Population population)
    {animals = (Animal[]) population.toArray();}

    /**
     * Retourne le nombre d'animaux mémorisées dans cette couche.
     *
     * @see #getPosition
     * @see #getAmplitude
     * @see #getDirection
     */
    public int getCount()
    {return animals.length;}

    /**
     * Retourne les coordonnées (<var>x</var>,<var>y</var>) de l'animal
     * désignée par l'index spécifié. Cette méthode est autorisée à retourner
     * <code>null</code> si la position d'une marque n'est pas connue.
     *
     * @see #getGeographicShape
     *
     * @throws IndexOutOfBoundsException Si l'index spécifié n'est pas
     *        dans la plage <code>[0..{@link #getCount}-1]</code>.
     */
    public Point2D getPosition(final int index) throws IndexOutOfBoundsException
    {return animals[index].getLocation();}

    /**
     * Retourne la direction à la position d'un
     * animal, en radians arithmétiques.
     */
    public double getDirection(final int index)
    {return Math.toRadians(90-animals[index].getDirection());}

    /**
     * Retourne la forme géométrique servant de modèle au traçage d'un animal.
     * Cette forme peut varier d'un animal à l'autre. Cette méthode retourne
     * une flèche dont la queue se trouve à l'origine (0,0).
     */
    public Shape getMarkShape(final int index)
    {return DEFAULT_SHAPE;}

    /**
     * Dessine la forme géométrique spécifiée. Cette méthode dessine l'animal
     * en choisissant une couleur en fonction de son espèce.
     *
     * @param graphics Graphique à utiliser pour tracer l'animal.
     * @param shape    Forme géométrique représentant l'animal à tracer.
     * @param index    Index de l'animal à tracer.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index)
    {
        Species species = animals[index].getSpecies();
        Species.Icon icon = icons.get(species);
        if (icon == null)
        {
            icon = species.getIcon();
            icons.put(species, icon);
        }
        graphics.setColor(icon.getColor());
        graphics.fill(shape);
    }

    /**
     * Appelée quand une population a changée.
     */
    public void populationChanged(final PopulationChangeEvent event)
    {
        refresh(event.getSource());
        repaint();
    }
}
