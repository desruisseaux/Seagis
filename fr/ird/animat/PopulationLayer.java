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

// J2SE et JAI
import java.awt.Paint;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import javax.media.jai.GraphicsJAI;

// Ensembles
import java.util.Map;
import java.util.HashMap;

// Composantes cartographiques
import net.seas.awt.geom.Arrow2D;
import net.seas.map.layer.MarkLayer;
import net.seas.map.RenderingContext;

// Animats
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;

// Geotools dependencies
import org.geotools.ct.TransformException;
import org.geotools.resources.XAffineTransform;


/**
 * Couche repr�sentant une population sur une carte.
 *
 * @version 1.0
 * @author Martin Desruisseaus
 */
final class PopulationLayer extends MarkLayer implements PopulationChangeListener
{
    /**
     * Longueur de la fl�che.
     */
    private static final int ARROW_LENGTH = 20;

    /**
     * Forme g�om�trique repr�sentant les animaux.
     */
    private static final Shape DEFAULT_SHAPE = new Arrow2D(0, -8, ARROW_LENGTH, 16);

    /**
     * Couleur des trajectoires, ou <code>null</code>
     * pour ne pas les dessiner.
     */
    private Color pathColor = Color.yellow;

    /**
     * Couleur du rayon de perception, ou <code>null</code>
     * pour ne pas le dessiner.
     */
    private Color perceptionColor = new Color(255,255,255,128);

    /**
     * Liste des animaux formant cette population.
     */
    private Animal[] animals;

    /**
     * Coordonn�es g�ographiques de la r�gion couverte
     * par la population, ou <code>null</code> s'il n'y
     * en a pas.
     */
    private Rectangle2D bounds;

    /**
     * Couleurs et symboles des esp�ces.
     */
    private final Map<Species,Species.Icon> icons = new HashMap<Species,Species.Icon>();

    /**
     * Construit une couche pour la population sp�cifi�e.
     */
    public PopulationLayer(final Population population)
    {
        refresh(population);
        population.addPopulationChangeListener(this);
    }

    /**
     * Remet � jour cette composante pour la population sp�cifi�e.
     */
    private void refresh(final Population population)
    {
        animals = (Animal[]) population.toArray();
        bounds = null;
        for (int i=0; i<animals.length; i++)
        {
            final Rectangle2D animalBounds = animals[i].getPath().getBounds2D();
            if (animalBounds != null)
            {
                if (bounds == null)
                {
                    bounds = animalBounds;
                }
                else
                {
                    bounds.add(animalBounds);
                }
            }
        }
    }

    /**
     * Retourne le nombre d'animaux m�moris�es dans cette couche.
     *
     * @see #getPosition
     * @see #getAmplitude
     * @see #getDirection
     */
    public int getCount()
    {return animals.length;}

    /**
     * Retourne les coordonn�es (<var>x</var>,<var>y</var>) de l'animal
     * d�sign�e par l'index sp�cifi�. Cette m�thode est autoris�e � retourner
     * <code>null</code> si la position d'une marque n'est pas connue.
     *
     * @see #getGeographicShape
     *
     * @throws IndexOutOfBoundsException Si l'index sp�cifi� n'est pas
     *        dans la plage <code>[0..{@link #getCount}-1]</code>.
     */
    public Point2D getPosition(final int index) throws IndexOutOfBoundsException
    {return animals[index].getLocation();}

    /**
     * Retourne la direction � la position d'un
     * animal, en radians arithm�tiques.
     */
    public double getDirection(final int index)
    {return Math.toRadians(90-animals[index].getDirection());}

    /**
     * Retourne la forme g�om�trique servant de mod�le au tra�age d'un animal.
     * Cette forme peut varier d'un animal � l'autre. Cette m�thode retourne
     * une fl�che dont la queue se trouve � l'origine (0,0).
     */
    public Shape getMarkShape(final int index)
    {return DEFAULT_SHAPE;}

    /**
     * Dessine la forme g�om�trique sp�cifi�e. Cette m�thode dessine l'animal
     * en choisissant une couleur en fonction de son esp�ce.
     *
     * @param graphics Graphique � utiliser pour tracer l'animal.
     * @param shape    Forme g�om�trique repr�sentant l'animal � tracer.
     * @param index    Index de l'animal � tracer.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index)
    {
        Species species = animals[index].getSpecies();
        Species.Icon icon = icons.get(species);
        if (icon == null)
        {
            icon = species.getIcon();
            icon.setColor(Color.red);
            icons.put(species, icon);
        }
        graphics.setColor(icon.getColor());
        graphics.fill(shape);
    }

    /**
     * Dessine les trajectoires des animaux, puis les animaux eux-m�mes.
     */
    protected synchronized Shape paint(final GraphicsJAI graphics, final RenderingContext context) throws TransformException
    {
        final Paint oldPaint = graphics.getPaint();
        final Rectangle clip = graphics.getClipBounds();
        Rectangle2D llBounds = (bounds!=null) ? (Rectangle2D) bounds.clone() : null;
        if (pathColor != null)
        {
            graphics.setColor(pathColor);
            for (int i=0; i<animals.length; i++)
            {
                final Shape shape = animals[i].getPath();
                if (clip==null || shape.intersects(clip))
                {
                    graphics.draw(shape);
                }
            }
        }
        if (perceptionColor != null)
        {
            graphics.setColor(perceptionColor);
            for (int i=0; i<animals.length; i++)
            {
                final Shape shape = animals[i].getPerceptionArea(1);
                if (clip==null || shape.intersects(clip))
                {
                    graphics.fill(shape);
                }
                final Rectangle2D perBounds = shape.getBounds2D();
                if (perBounds != null)
                {
                    if (llBounds==null)
                    {
                        llBounds = perBounds;
                    }
                    else
                    {
                        llBounds.add(perBounds);
                    }
                }
            }
        }
        graphics.setPaint(oldPaint);
        final Shape        shape = super.paint(graphics, context);
        final AffineTransform at = context.getAffineTransform(RenderingContext.WORLD_TO_POINT);
        final Rectangle pxBounds = (Rectangle) XAffineTransform.transform(at, llBounds, new Rectangle());
        pxBounds.add(shape.getBounds());
        pxBounds.x      -= ARROW_LENGTH;
        pxBounds.y      -= ARROW_LENGTH;
        pxBounds.width  += ARROW_LENGTH*2;
        pxBounds.height += ARROW_LENGTH*2;
        return pxBounds;
    }

    /**
     * Appel�e quand une population a chang�e.
     */
    public void populationChanged(final PopulationChangeEvent event)
    {
        refresh(event.getSource());
        repaint();
    }
}
