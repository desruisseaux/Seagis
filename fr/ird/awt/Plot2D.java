/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2002 Institut de Recherche pour le Développement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.awt;

// Base class and axis
import fr.ird.awt.axis.Axis;
import fr.ird.awt.axis.Graduation;
import fr.ird.awt.axis.AbstractGraduation;
import fr.ird.awt.series.Series;

// Graphics and geometry
import java.awt.Shape;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Components and events
import java.awt.Container;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

// Collections
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

// Geotools dependencies
import org.geotools.gui.swing.ZoomPane;


/**
 * A widget displaying two axis and an arbitrary amount of data series.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Plot2D extends ZoomPane
{
    /**
     * The set of <var>x</var> axis. There is usually
     * only one axis, but more axis are allowed.
     */
    private final List<Axis> xAxis = new ArrayList<Axis>(3);

    /**
     * The set of <var>y</var> axis. There is usually
     * only one axis, but more axis are allowed.
     */
    private final List<Axis> yAxis = new ArrayList<Axis>(3);

    /**
     * The set of series to plot.
     */
    private final List<Series> series = new ArrayList<Series>();

    /**
     * Listener class for various events.
     */
    private final class Listeners extends ComponentAdapter
    {
        public void componentResized(final ComponentEvent event)
        {
            final Container c = (Container) event.getSource();
            c.invalidate();
            c.validate(); // TODO: Trouver pourquoi Swing ne l'appelle pas tout seul.
        }
    }

    /**
     * Construct an initially empty <code>Plot2D</code> with
     * zoom capabilities on horizontal and vertical axis.
     */
    public Plot2D()
    {this(SCALE_X | SCALE_Y | TRANSLATE_X | TRANSLATE_Y);}

    /**
     * Construct an initially empty <code>Plot2D</code>
     * with the specified zoom capacities.
     *
     * @param  zoomCapacities Allowed zoom types. It can be a
     *         bitwise combinaison of the following constants:
     *         {@link #SCALE_X}, {@link #SCALE_Y}, {@link #TRANSLATE_X}, {@link #TRANSLATE_Y},
     *         {@link #ROTATE}, {@link #RESET} and {@link #DEFAULT_ZOOM}.
     * @throws IllegalArgumentException If <code>zoomCapacities</code> is invalid.
     */
    public Plot2D(final int zoomCapacities)
    {
        super(zoomCapacities);
        final Listeners listeners = new Listeners();
        addComponentListener(listeners);
    }

    /**
     * Add a new serie to the chart.
     *
     * @param series Serie to add.
     */
    public void addSeries(final Series series)
    {
        Rectangle2D bounds = null;
        /*
         * Si aucun axe n'a été définie, construit
         * et ajoute de nouveau axes maintenant.
         */
        if (xAxis.isEmpty())
        {
            if (bounds==null)
            {
                bounds = series.getPath().getBounds2D();
            }
            final Axis axis = new Axis();
            final AbstractGraduation grad = (AbstractGraduation) axis.getGraduation();
            grad.setMinimum(bounds.getMinX());
            grad.setMaximum(bounds.getMaxX());
            xAxis.add(axis);
            invalidate();
        }
        if (yAxis.isEmpty())
        {
            if (bounds==null)
            {
                bounds = series.getPath().getBounds2D();
            }
            final Axis axis = new Axis();
            final AbstractGraduation grad = (AbstractGraduation) axis.getGraduation();
            grad.setMinimum(bounds.getMinY());
            grad.setMaximum(bounds.getMaxY());
            yAxis.add(axis);
            invalidate();
        }
        this.series.add(series);
        validate();
    }

    /**
     * Returns a bounding box that contains the logical coordinates of
     * all data that may be displayed in this <code>ZoomPane</code>.
     *
     * @return A bounding box for the logical coordinates of every content
     *         that is going to be drawn on this <code>ZoomPane</code>. If
     *         this bounding box is unknow, then this method can returns
     *         <code>null</code> (but this is not recommanded).
     */
    public Rectangle2D getArea()
    {
        double xmin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        for (final Iterator<Axis> it=xAxis.iterator(); it.hasNext();)
        {
            double value;
            final Graduation grad = it.next().getGraduation();
            if ((value=grad.getMinimum()) < xmin) xmin=value;
            if ((value=grad.getMaximum()) > xmax) xmax=value;
        }
        for (final Iterator<Axis> it=yAxis.iterator(); it.hasNext();)
        {
            double value;
            final Graduation grad = it.next().getGraduation();
            if ((value=grad.getMinimum()) < ymin) ymin=value;
            if ((value=grad.getMaximum()) > ymax) ymax=value;
        }
        if (xmin<=xmax && ymin<=ymax)
        {
            return new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
        }
        return null;
    }

    /**
     * Validate this panel. This method is automatically invoked
     * when the axis needs to be layout. This occur for example
     * when new axis are added, or when the component has been
     * resized.
     */
    public void validate()
    {
        super.validate();
        int width  = getWidth();
        int height = getHeight();
        for (Iterator<Axis> it=xAxis.iterator(); it.hasNext();)
        {
            final Axis axis = it.next();
            axis.setLabelClockwise(true);
            axis.setLine(60, height-60, width-30, height-60);
            // TODO: Déclarer des constantes.
            // TODO: Positionner les 2ème, 3ème... axes en fonction
            //       de la position des axes qui précèdent.
        }
        for (Iterator<Axis> it=yAxis.iterator(); it.hasNext();)
        {
            final Axis axis = it.next();
            axis.setLabelClockwise(false);
            axis.setLine(60, height-60, 60, 30);
            // TODO: Déclarer des constantes.
            // TODO: Positionner les 2ème, 3ème... axes en fonction
            //       de la position des axes qui précèdent.
        }
    }

    /**
     * Paints the axis and all series.
     */
    protected void paintComponent(final Graphics2D graphics)
    {
        final AffineTransform oldTransform = graphics.getTransform();
        final Stroke          oldStroke    = graphics.getStroke();
        /*
         * Paint series first.
         */
// TODO graphics.transform(zoom);
        graphics.setColor(Color.blue);
        graphics.setStroke(new BasicStroke(0));
        final AffineTransform zoomTr = graphics.getTransform();
        for (Iterator<Series> it=series.iterator(); it.hasNext();)
        {
            final Axis xAxis = this.xAxis.get(0); // TODO: trouver le bon axe
            final Axis yAxis = this.yAxis.get(0); // TODO: trouver le bon axe
            final AffineTransform transform = Axis.createAffineTransform(xAxis, yAxis);
            final Series series = it.next();
            final Shape path = series.getPath();
            graphics.transform(transform);
            graphics.draw(path);
            graphics.setTransform(zoomTr);
        }
        /*
         * Paint axis last.
         */
        graphics.setStroke   (oldStroke);
        graphics.setTransform(oldTransform);
        graphics.setStroke(new BasicStroke(0));
        graphics.setColor(Color.black);
        for (final Iterator<Axis> it=xAxis.iterator(); it.hasNext();)
        {
            it.next().paint(graphics);
        }
        for (final Iterator<Axis> it=yAxis.iterator(); it.hasNext();)
        {
            it.next().paint(graphics);
        }
        graphics.transform(zoom); // Reset the zoom for the magnifier.
    }

    /**
     * Translate an axis in a perpendicular direction to its orientation.
     * The following rules applies:
     *
     * <ul>
     *   <li>If the axis is vertical, then the axis is translated horizontally
     *       by <code>tx</code> only. The <code>ty</code> argument is ignored.</li>
     *   <li>If the axis is horizontal, then the axis is translated vertically
     *       by <code>ty</code> only. The <code>tx</code> argument is ignored.</li>
     *   <li>If the axis is diagonal, then the axis is translated using the
     *       following formula (<var>theta</var> is the axis orientation relative
     *       to the horizontal):
     *       <br>
     *       <blockquote><pre>
     *          dx = x*sin(theta)
     *          dy = y*cos(theta)
     *       </pre></blockquote>
     *    </li>
     *  </ul>
     */
    private static void translatePerpendicularly(final Axis axis, final double tx, final double ty)
    {
        synchronized (axis)
        {
            final double x1 = axis.getX1();
            final double y1 = axis.getY1();
            final double x2 = axis.getX2();
            final double y2 = axis.getY2();
            double dy = (double) x2 - (double) x1; // Note: dx and dy are really
            double dx = (double) y1 - (double) y2; //       swapped. Not an error.
            double length = Math.sqrt(dx*dx + dy*dy);
            dx *= tx/length;
            dy *= ty/length;
            axis.setLine(x1+dx, y1+dy, x2+dx, y2+dy);
        }
    }
}
