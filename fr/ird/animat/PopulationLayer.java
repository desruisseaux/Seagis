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
import java.awt.Paint;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.media.jai.GraphicsJAI;

// Utilitaires
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Composantes cartographiques
import fr.ird.map.RepaintManager;
import org.geotools.renderer.j2d.RenderedMarks;
import org.geotools.renderer.j2d.RenderingContext;

// Animats
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;

// Geotools dependencies
import org.geotools.renderer.geom.Arrow2D;
import org.geotools.ct.TransformException;
import org.geotools.resources.XAffineTransform;


/**
 * Couche représentant une population sur une carte.
 *
 * @version $Id$
 * @author Martin Desruisseaus
 */
final class PopulationLayer extends RenderedMarks
                implements PopulationChangeListener, PropertyChangeListener
{
    /**
     * Longueur de la flèche représentant les thons.
     */
    private static final int ARROW_LENGTH = 20;

    /**
     * Le "rayon" des marques représentant la position des observations.
     */
    private static final int MARK_RADIUS = 3;

    /**
     * Forme géométrique représentant les animaux.
     */
    private static final Shape DEFAULT_SHAPE = new Arrow2D(0, -8, ARROW_LENGTH, 16);

    /**
     * Type de ligne pour les trajectoire. Par défaut, on utilise
     * une ligne d'une épaisseur de 1 mille nautique.
     */
    private static final Stroke PATH_STROKE = new BasicStroke(1/60f);

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
     * La date des données à afficher. Cette date sera constamment
     * mise à jour lorsque l'environnement change.
     */
    private Date date = new Date();

    /**
     * Coordonnées géographiques de la région couverte
     * par la population, ou <code>null</code> s'il n'y
     * en a pas.
     */
    private Rectangle2D bounds;

    /**
     * Couleurs et symboles des espèces.
     */
    private final Map<Species,Species.Icon> icons = new HashMap<Species,Species.Icon>();

    /**
     * L'objet à utiliser pour synchroniser les retraçaces.
     */
    private final RepaintManager manager;

    /**
     * Dernière position à avoir été retournée.
     * Ce champ n'existe qu'à des fins d'optimisation.
     */
    private transient double[] lastPosition;

    /**
     * Index de l'animal qui a retourné la dernière observation.
     * Ce champ n'existe qu'à des fins d'optimisation.
     */
    private transient int indexLastPosition;

    /**
     * Construit une couche pour la population spécifiée.
     *
     * @param  population Population à afficher.
     *         Elle peut provenir d'une machine distante.
     * @param  manager Objet à utiliser pour redessiner les cartes.
     */
    public PopulationLayer(final Population population,
                           final RepaintManager manager)
    {
        this.manager = manager;
        refresh(population);
        population.addPopulationChangeListener(this);
    }

    /**
     * Remet à jour cette composante pour la population spécifiée.
     */
    private void refresh(final Population population) {
        animals = (Animal[]) population.getAnimals().toArray();
        bounds = null;
        for (int i=0; i<animals.length; i++) {
            final Rectangle2D animalBounds = animals[i].getPath().getBounds();
            if (animalBounds != null) {
                if (bounds == null) {
                    bounds = animalBounds;
                } else {
                    bounds.add(animalBounds);
                }
            }
        }
    }

    /**
     * Retourne le nombre d'animaux mémorisées dans cette couche.
     *
     * @see #getPosition
     * @see #getAmplitude
     * @see #getDirection
     */
    public int getCount() {
        return animals.length;
    }

    /**
     * Retourne l'observation qui correspond à la position à l'index spécifié.
     */
    private double[] getPositionObs(final int index) {
        if (index!=indexLastPosition || lastPosition==null) {
            final Map<Parameter,double[]> observations;
            observations = animals[index].getObservations(date);
            if (observations == null) {
                return null;
            }
            lastPosition = observations.get(Parameter.HEADING);
            indexLastPosition = index;
        }
        return lastPosition;
    }

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
    public Point2D getPosition(final int index) throws IndexOutOfBoundsException {
        return Parameter.HEADING.getLocation(getPositionObs(index));
    }

    /**
     * Retourne la direction à la position d'un animal, en radians arithmétiques.
     */
    public double getDirection(final int index) {
        double theta = Parameter.HEADING.getValue(getPositionObs(index));
        theta = Math.toRadians(90-theta);
        return Double.isNaN(theta) ? 0 : theta;
    }

    /**
     * Retourne la forme géométrique servant de modèle au traçage d'un animal.
     * Cette forme peut varier d'un animal à l'autre. Cette méthode retourne
     * une flèche dont la queue se trouve à l'origine (0,0).
     */
    public Shape getMarkShape(final int index) {
        return DEFAULT_SHAPE;
    }

    /**
     * Dessine la forme géométrique spécifiée. Cette méthode dessine l'animal
     * en choisissant une couleur en fonction de son espèce.
     *
     * @param graphics Graphique à utiliser pour tracer l'animal.
     * @param shape    Forme géométrique représentant l'animal à tracer.
     * @param index    Index de l'animal à tracer.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index) {
        Species species;
        species = animals[index].getSpecies();
        Species.Icon icon = icons.get(species);
        if (icon == null) {
            icon = species.getIcon();
            icon.setColor(Color.red);
            icons.put(species, icon);
        }
        graphics.setColor(icon.getColor());
        graphics.fill(shape);
    }

    /**
     * Dessine les trajectoires des animaux, puis les animaux eux-mêmes.
     */
    protected Shape paint(final RenderingContext context) throws TransformException {
        final Graphics2D graphics = context.getGraphics();
        final Paint      oldPaint = graphics.getPaint();
        final Stroke    oldStroke = graphics.getStroke();
        final Rectangle      clip = graphics.getClipBounds();
        Rectangle2D      llBounds = (bounds!=null) ? (Rectangle2D) bounds.clone() : null;
        //////////////////////////////////////
        ////    Dessine la trajectoire    ////
        //////////////////////////////////////
        if (pathColor != null) {
            graphics.setColor(pathColor);
            graphics.setStroke(PATH_STROKE);
            for (int i=0; i<animals.length; i++) {
                final Shape shape = animals[i].getPath();
                if (clip==null || shape.intersects(clip)) {
                    graphics.draw(shape);
                }
            }
            graphics.setStroke(oldStroke);
        }
        ///////////////////////////////////////////////
        ////    Dessine la région de perception    ////
        ///////////////////////////////////////////////
        if (perceptionColor != null) {
            graphics.setColor(perceptionColor);
            for (int i=0; i<animals.length; i++) {
                final Shape shape = animals[i].getPerceptionArea(date);
                if (clip==null || shape.intersects(clip)) {
                    graphics.fill(shape);
                }
                final Rectangle2D perBounds = shape.getBounds2D();
                if (perBounds != null) {
                    if (llBounds==null) {
                        llBounds = perBounds;
                    } else {
                        llBounds.add(perBounds);
                    }
                }
            }
        }
        //////////////////////////////////////////////////////
        ////    Dessine les positions des observations    ////
        //////////////////////////////////////////////////////
        final AffineTransform oldTr = graphics.getTransform();
        final AffineTransform  zoom = context.getAffineTransform(context.mapCS, context.textCS);
        if (true) {
            graphics.setTransform(context.getAffineTransform(context.textCS, context.deviceCS));
            graphics.setStroke(new BasicStroke(0));
            graphics.setColor(Color.black);
            for (int i=0; i<animals.length; i++) {
                final Map<Parameter,double[]> observations = animals[i].getObservations(date);
                if (observations != null) {
                    for (final Iterator<Map.Entry<Parameter,double[]>> it=observations.entrySet().iterator(); it.hasNext();) {
                        final Map.Entry<Parameter,double[]> obs = it.next();
                        final Parameter param = obs.getKey();
                        final double[] values = obs.getValue();
                        Point2D location = param.getLocation(values);
                        if (location != null) {
                            location = zoom.transform(location, location);
                            final int x = (int) Math.round(location.getX());
                            final int y = (int) Math.round(location.getY());
                            graphics.drawLine(x-MARK_RADIUS, y, x+MARK_RADIUS, y);
                            graphics.drawLine(x, y-MARK_RADIUS, x, y+MARK_RADIUS);
                        }
                    }
                }
            }
        }
        graphics.setTransform(oldTr);
        graphics.setStroke(oldStroke);
        graphics.setPaint(oldPaint);
        ///////////////////////////////////
        ////    Dessine les animaux    ////
        ///////////////////////////////////
        final Shape        shape = super.paint(graphics, context);
        final AffineTransform at = context.getAffineTransform(context.mapCS, context.textCS);
        final Rectangle pxBounds = (Rectangle) XAffineTransform.transform(at, llBounds, new Rectangle());
        pxBounds.add(shape.getBounds());
        pxBounds.x      -= ARROW_LENGTH;
        pxBounds.y      -= ARROW_LENGTH;
        pxBounds.width  += ARROW_LENGTH*2;
        pxBounds.height += ARROW_LENGTH*2;
        return pxBounds;
    }

    /**
     * Appelée quand une population a changée.
     *
     * @throws RemoteException si une exécution sur une machine distante
     *         était nécessaire et a échoué.
     */
    public void populationChanged(final PopulationChangeEvent event) {
        refresh(event.getSource());
        manager.repaint(this);
    }

    /**
     * Appelée quand une propriété de {@link EnvironmentLayer}
     * a changée.
     */
    public void propertyChange(final PropertyChangeEvent event) {
        if (event.getPropertyName().equalsIgnoreCase("date")) {
            date = (Date) event.getNewValue();
            manager.repaint(this);
        }
    }
}
