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
package fr.ird.animat.viewer;

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
import java.awt.image.RenderedImage;
import java.awt.font.GlyphVector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;

// Utilitaires
import java.util.Date;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.renderer.geom.Arrow2D;
import org.geotools.ct.TransformException;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.resources.XAffineTransform;
import org.geotools.renderer.j2d.MarkIterator;
import org.geotools.renderer.j2d.RenderedMarks;
import org.geotools.renderer.j2d.RenderingContext;

// Animats
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.Parameter;
import fr.ird.animat.Population;
import fr.ird.animat.Observation;
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;


/**
 * Couche repr�sentant une population sur une carte.
 *
 * @version $Id$
 * @author Martin Desruisseaus
 */
final class PopulationLayer extends RenderedMarks
                implements PopulationChangeListener, PropertyChangeListener
{
    /**
     * Longueur de la fl�che repr�sentant les thons.
     */
    private static final int ARROW_LENGTH = 20;

    /**
     * Le "rayon" des marques repr�sentant la position des observations.
     */
    private static final int MARK_RADIUS = 3;

    /**
     * Forme g�om�trique repr�sentant les animaux.
     */
    private static final Shape DEFAULT_SHAPE = new Arrow2D(0, -8, ARROW_LENGTH, 16);

    /**
     * Type de ligne pour les trajectoire. Par d�faut, on utilise
     * une ligne d'une �paisseur de 1 mille nautique.
     */
    private static final Stroke PATH_STROKE = new BasicStroke(1/60f);

    /**
     * Couleur des trajectoires, ou <code>null</code> pour ne pas les dessiner.
     */
    private Color pathColor = Color.yellow;

    /**
     * Couleur du rayon de perception, ou <code>null</code> pour ne pas le dessiner.
     */
    private Color perceptionColor = new Color(255,255,255,128);

    /**
     * Liste des animaux formant cette population.
     */
    private Animal[] animals;

    /**
     * La date des donn�es � afficher. Cette date sera constamment
     * mise � jour lorsque l'environnement change.
     */
    private Date date = new Date();

    /**
     * Coordonn�es g�ographiques de la r�gion couverte par la population,
     * ou <code>null</code> s'il n'y en a pas.
     */
    private Shape bounds;

    /**
     * Couleurs et symboles des esp�ces.
     */
    private final Map<Species,Species.Icon> icons = new HashMap<Species,Species.Icon>();

    /**
     * Construit une couche pour la population sp�cifi�e.
     *
     * @param  population Population � afficher.
     *         Elle peut provenir d'une machine distante.
     * @param  manager Objet � utiliser pour redessiner les cartes.
     */
    public PopulationLayer(final Population population) throws RemoteException {
        refresh(population);
        population.addPopulationChangeListener(this);
    }

    /**
     * Remet � jour cette composante pour la population sp�cifi�e.
     */
    private void refresh(final Population population) throws RemoteException {
        final Collection<Animal> col = population.getAnimals();
        animals = col.toArray(new Animal[col.size()]);
        bounds = population.getSpatialBounds();
    }

    /**
     * Retourne un it�rateur balayant les positions des animaux.
     */
    public MarkIterator getMarkIterator() {
        return new Iterator();
    }

    /**
     * Dessine les trajectoires des animaux, puis les animaux eux-m�mes.
     */
    protected void paint(final RenderingContext context) throws TransformException {
        final Graphics2D graphics = context.getGraphics();
        final Paint      oldPaint = graphics.getPaint();
        final Stroke    oldStroke = graphics.getStroke();
        final Rectangle      clip = graphics.getClipBounds();
        Rectangle2D      llBounds = (bounds!=null) ? bounds.getBounds2D() : null;
        try {
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
            ////    Dessine la r�gion de perception    ////
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
                    final Set<Observation> observations = animals[i].getObservations(date);
                    if (observations != null) {
                        for (final java.util.Iterator<Observation> it=observations.iterator(); it.hasNext();) {
                            final Observation obs = it.next();
                            final Parameter param = obs.getParameter();
                            Point2D location = obs.getLocation();
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
            super.paint(context);
            final AffineTransform at = context.getAffineTransform(context.mapCS, context.textCS);
            final Rectangle pxBounds = (Rectangle) XAffineTransform.transform(at, llBounds, new Rectangle());
            pxBounds.x      -= ARROW_LENGTH;
            pxBounds.y      -= ARROW_LENGTH;
            pxBounds.width  += ARROW_LENGTH*2;
            pxBounds.height += ARROW_LENGTH*2;
            context.addPaintedArea(pxBounds, context.textCS);
        } catch (RemoteException exception) {
            graphics.setColor(Color.BLACK);
            graphics.setStroke(new BasicStroke(0));
            context.setCoordinateSystem(context.textCS);
            ExceptionMonitor.paintStackTrace(graphics,
                                context.getPaintingArea(context.textCS).getBounds(), exception);
            context.setCoordinateSystem(context.mapCS);
        }
    }

    /**
     * Appel�e quand une population a chang�e.
     *
     * @throws RemoteException si une ex�cution sur une machine distante
     *         �tait n�cessaire et a �chou�.
     */
    public void populationChanged(final PopulationChangeEvent event) throws RemoteException {
        refresh(event.getSource());
        repaint();
    }

    /**
     * Appel�e quand une propri�t� de {@link EnvironmentLayer} a chang�e.
     */
    public void propertyChange(final PropertyChangeEvent event) {
        if (event.getPropertyName().equalsIgnoreCase("date")) {
            date = (Date) event.getNewValue();
            repaint();
        }
    }

    /**
     * Appel�e automatiquement par l'it�rateur lorsque l'ex�cution d'une m�thode RMI a �chou�e.
     */
    private static void failed(final String method, final RemoteException exception) {
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setSourceClassName("PopulationLayer.MarkIterator");
        record.setSourceMethodName(method);
        record.setThrown(exception);
        Logger.getLogger("fr.ird.animat.viewer").log(record);
    }

    /**
     * Iterateur balayant les positions des animaux.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Iterator extends MarkIterator {
        /**
         * Index de l'animal qui a retourn� la derni�re observation.
         */
        private int index = -1;

        /**
         * Observations de l'animal courant.
         */
        private Set<Observation> observations;

        /**
         * Position de l'animal courant, ou <code>null</code> si aucune.
         */
        private Observation heading;

        /**
         * Construit un it�rateur par d�faut.
         */
        public Iterator() {
        }

        /**
         * Retourne la position courange de l'it�rateur.
         */
        public int getIteratorPosition() {
            return index;
        }

        /**
         * Positionne l'it�rateur sur l'animal sp�cifi�.
         */
        public void setIteratorPosition(final int index) {
            this.index = index;
            try {
                update();
            } catch (RemoteException exception) {
                failed("setIteratorPosition", exception);
                observations = null;
                heading = null;
            }
        }

        /**
         * Avance l'it�rateur � l'animal suivant.
         */
        public boolean next() {
            while (++index >= animals.length) {
                try {
                    update();
                    return true;
                } catch (RemoteException exception) {
                    failed("next", exception);
                }
            }
            observations = null;
            heading = null;
            return false;
        }

        /**
         * Obtient des observations qui correspondent � l'animal courant.
         */
        private void update() throws RemoteException {
            observations = animals[index].getObservations(date);
            for (final java.util.Iterator<Observation> it=observations.iterator(); it.hasNext();) {
                final Observation candidate = it.next();
                if (fr.ird.animat.impl.Parameter.HEADING.equals(candidate)) {
                    heading = candidate;
                    break;
                }
            }
        }

        /**
         * Retourne les coordonn�es (<var>x</var>,<var>y</var>) de l'animal
         * d�sign�e par l'index sp�cifi�. Cette m�thode est autoris�e � retourner
         * <code>null</code> si la position d'une marque n'est pas connue.
         *
         * @see #geographicArea
         */
        public Point2D position() {
            return heading.getLocation();
        }

        /**
         * Retourne la direction � la position d'un animal, en radians arithm�tiques.
         */
        public double direction() {
            double theta = heading.getValue();
            theta = Math.toRadians(90-theta);
            return Double.isNaN(theta) ? 0 : theta;
        }

        /**
         * Retourne la forme g�om�trique servant de mod�le au tra�age d'un animal.
         * Cette forme peut varier d'un animal � l'autre. Cette m�thode retourne
         * une fl�che dont la queue se trouve � l'origine (0,0).
         */
        public Shape markShape() {
            return DEFAULT_SHAPE;
        }

        /**
         * Dessine la forme g�om�trique sp�cifi�e. Cette m�thode dessine l'animal
         * en choisissant une couleur en fonction de son esp�ce.
         */
        protected void paint(final Graphics2D      graphics,
                             final Shape           geographicArea,
                             final Shape           markShape,
                             final RenderedImage   markIcon,
                             final AffineTransform iconXY,
                             final GlyphVector     label,
                             final Point2D.Float   labelXY)
        {
            final Species species;
            try {
                species = animals[index].getSpecies();
            } catch (RemoteException exception) {
                failed("paint", exception);
                return;
            }
            Species.Icon icon = icons.get(species);
            if (icon == null) {
                icon = species.getIcon();
                icon.setColor(Color.red);
                icons.put(species, icon);
            }
            graphics.setColor(icon.getColor());
            graphics.fill(markShape);
        }
    }
}
