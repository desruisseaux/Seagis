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
package fr.ird.animat.gui.swing;

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
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

// Utilitaires
import java.util.Date;
import java.util.Set;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.ct.TransformException;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.resources.XAffineTransform;
import org.geotools.renderer.j2d.MarkIterator;
import org.geotools.renderer.j2d.RenderedMarks;
import org.geotools.renderer.j2d.RenderingContext;
import org.geotools.renderer.geom.Arrow2D;

// Animats
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.Parameter;
import fr.ird.animat.Population;
import fr.ird.animat.Simulation;
import fr.ird.animat.Observation;
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;


/**
 * Couche représentant une population sur une carte.
 *
 * @version $Id$
 * @author Martin Desruisseaus
 */
final class PopulationLayer extends RenderedMarks implements PropertyChangeListener, Runnable {
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
     * Couleurs cycliques à utiliser pour chaque nouvelle population.
     */
    private static final Color[] COLORS = {Color.YELLOW, Color.RED, Color.BLUE,
                                           Color.MAGENTA, Color.GREEN.darker()};

    /**
     * Couleur des trajectoires, ou <code>null</code> pour ne pas les dessiner.
     */
    private Color pathColor = Color.YELLOW;

    /**
     * Couleur du rayon de perception, ou <code>null</code> pour ne pas le dessiner.
     */
    private Color perceptionColor = new Color(255,255,255,128);

    /**
     * Liste des animaux formant cette population.
     */
    private Animal[] animals;

    /**
     * Les observations pour chaque animal. Ces observations seront obtenues avec
     * {@link Animal#getObservations} la première fois où elles seront demandées.
     */
    private transient Map<Parameter,Observation>[=] observations;

    /**
     * La date des données à afficher. Cette date sera constamment
     * mise à jour lorsque l'environnement change.
     */
    private Date date = null;

    /**
     * Coordonnées géographiques de la région couverte par la population,
     * ou <code>null</code> s'il n'y en a pas.
     */
    private Shape bounds;

    /**
     * La population dessinée par cette couche.
     */
    private final Population population;

    /**
     * Couleurs et symboles des espèces.
     */
    private final Map<Species,Species.Icon> icons = new HashMap<Species,Species.Icon>();

    /**
     * Les instructions à exécuter si jamais la machine virtuelle était interrompue
     * par l'utilisateur avec [Ctrl-C] ou quelque autre signal du genre.  Ce thread
     * va retirer les "listeners" afin de ne pas encombrer le serveur, qui lui
     * continuera à fonctionner.
     */
    private final Thread shutdownHook;

    /**
     * L'objet chargé d'écouter les modifications survenant dans la population.
     * Ces changements peuvent survenir sur une machine distante.
     */
    private final Listener listener;

    /**
     * Construit une couche pour la population spécifiée.
     *
     * @param  population Population à afficher.
     *         Elle peut provenir d'une machine distante.
     * @param  manager Objet à utiliser pour redessiner les cartes.
     */
    public PopulationLayer(final Population population) throws RemoteException {
        this.population = population;
        refresh(population);
        listener = new Listener();
        population.addPopulationChangeListener(listener);
        shutdownHook = new Thread(Simulation.THREAD_GROUP, this, "PopulationLayer shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Exécuté automatiquement lorsque la machine virtuelle est en cours de fermeture.
     */
    public void run() {
        try {
            population.removePopulationChangeListener(listener);
        } catch (RemoteException exception) {
            // Logging during shutdown may fail silently. May be better than nothing...
            EnvironmentLayer.failed("PopulationLayer", "shutdownHook", exception);
        }
    }

    /**
     * Libère les ressources utilisées par cette couche.
     */
    public void dispose() {
        synchronized (getTreeLock()) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            run();
            super.dispose();
        }
    }

    /**
     * Définit la couleur des chemins. Un jeu de couleurs cycliques sera utilisé.
     */
    public void setColor(final int n) {
        pathColor = COLORS[n % COLORS.length];
        repaint();
    }

    /**
     * Remet à jour cette composante pour la population spécifiée.
     */
    private void refresh(final Population population) throws RemoteException {
        synchronized (getTreeLock()) {
            assert population.equals(this.population) : population;
            final Collection<+Animal> col = population.getAnimals();
            animals = (Animal[])col.toArray(new Animal[col.size()]);
            observations = new Map<Parameter,Observation>[animals.length];
            bounds = population.getSpatialBounds();
            setPreferredArea((bounds!=null) ? bounds.getBounds2D() : null);
        }
    }

    /**
     * Retourne les observations pour l'animal à l'index spécifié.
     */
    private Map<Parameter,Observation> getObservations(final int index) throws RemoteException {
        Map<Parameter,Observation> obs = observations[index];
        if (obs == null) {
            obs = animals[index].getObservations(date);
            observations[index] = obs;
        }
        return obs;
    }

    /**
     * Retourne un itérateur balayant les positions des animaux.
     */
    public MarkIterator getMarkIterator() {
        return new Iterator();
    }

    /**
     * Dessine les trajectoires des animaux, puis les animaux eux-mêmes.
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
                    // TODO: il sera peut-être plus efficace d'ajouter une méthode dans 'Population'
                    //       qui retourne la totalité des animaux avec leurs observations.
                    //       Ça éviterait d'envoyer plusieurs copies des mêmes paramètres
                    //       à travers le réseau chaque fois que 'getObservations' est appelée.
                    final Map<Parameter,Observation> observations = getObservations(i);
                    if (observations != null) {
                        for (final Map.Entry<Parameter,Observation> entry : observations.entrySet()) {
                            final Parameter param = entry.getKey();
                            final Observation obs = entry.getValue();
                            Point2D location = obs.location();
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
            if (llBounds != null) {
                final AffineTransform at = context.getAffineTransform(context.mapCS, context.textCS);
                final Rectangle pxBounds = (Rectangle) XAffineTransform.transform(at, llBounds, new Rectangle());
                pxBounds.x      -= ARROW_LENGTH;
                pxBounds.y      -= ARROW_LENGTH;
                pxBounds.width  += ARROW_LENGTH*2;
                pxBounds.height += ARROW_LENGTH*2;
                context.addPaintedArea(pxBounds, context.textCS);
            }
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
     * Appelée quand une propriété de {@link EnvironmentLayer} a changée.
     */
    public void propertyChange(final PropertyChangeEvent event) {
        final String property = event.getPropertyName();
        if (property.equalsIgnoreCase("date")) {
            date = (Date) event.getNewValue();
            Arrays.fill(observations, null);
            repaint();
        }
    }

    /**
     * Appelée automatiquement lorsque l'exécution d'une méthode RMI a échouée.
     */
    private static void failed(final String method, final RemoteException exception) {
        EnvironmentLayer.failed("PopulationLayer.MarkIterator", method, exception);
    }

    /**
     * Iterateur balayant les positions des animaux.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Iterator extends MarkIterator {
        /**
         * Index de l'animal qui a retourné la dernière observation.
         */
        private int index = -1;

        /**
         * Observations de l'animal courant.
         */
        private Map<Parameter,Observation> observations;

        /**
         * Position de l'animal courant, ou <code>null</code> si aucune.
         */
        private Observation heading;

        /**
         * Construit un itérateur par défaut.
         */
        public Iterator() {
        }

        /**
         * Retourne la position courange de l'itérateur.
         */
        public int getIteratorPosition() {
            return index;
        }

        /**
         * Positionne l'itérateur sur l'animal spécifié.
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
         * Avance l'itérateur à l'animal suivant.
         */
        public boolean next() {
            while (++index < animals.length) {
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
         * Obtient des observations qui correspondent à l'animal courant.
         */
        private void update() throws RemoteException {
            observations = getObservations(index);
            heading = observations.get(fr.ird.animat.server.Parameter.HEADING);
        }

        /**
         * Retourne les coordonnées (<var>x</var>,<var>y</var>) de l'animal
         * désignée par l'index spécifié. Cette méthode est autorisée à retourner
         * <code>null</code> si la position d'une marque n'est pas connue.
         *
         * @see #geographicArea
         */
        public Point2D position() {
            if (heading != null) {
                return heading.location();
            } else {
                return null;
            }
        }

        /**
         * Retourne la direction à la position d'un animal, en radians arithmétiques.
         */
        public double direction() {
            if (heading != null) {
                double theta = heading.value();
                theta = Math.toRadians(90-theta);
                return Double.isNaN(theta) ? 0 : theta;
            } else {
                return 0;
            }
        }

        /**
         * Retourne la forme géométrique servant de modèle au traçage d'un animal.
         * Cette forme peut varier d'un animal à l'autre. Cette méthode retourne
         * une flèche dont la queue se trouve à l'origine (0,0).
         */
        public Shape markShape() {
            return DEFAULT_SHAPE;
        }

        /**
         * Dessine la forme géométrique spécifiée. Cette méthode dessine l'animal
         * en choisissant une couleur en fonction de son espèce.
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
                try {
                    icon = species.getIcon();
                } catch (RemoteException exception) {
                    EnvironmentLayer.failed("PopulationLayer", "paint", exception);
                    return;
                }
                icon.setColor(Color.red);
                icons.put(species, icon);
            }
            graphics.setColor(icon.getColor());
            graphics.fill(markShape);
        }
    }

    /**
     * Objet ayant la charge de réagir aux changements survenant dans l'environnement.
     * Ces changements peuvent se produire sur une machine distante.
     */
    private final class Listener extends UnicastRemoteObject implements PopulationChangeListener {
        /**
         * Construit un objet par défaut. L'objet sera exporté
         * immédiatement pour un éventuel usage avec les RMI.
         */
        public Listener() throws RemoteException {
        }

        /**
         * Appelée quand la population a changée.
         */
        public void populationChanged(final PopulationChangeEvent event) throws RemoteException {
            if (event.changeOccured(PopulationChangeEvent.ANIMALS_ADDED |
                                    PopulationChangeEvent.ANIMALS_REMOVED))
            {
                refresh(event.getSource());
                repaint();
            }
        }
    }
}
