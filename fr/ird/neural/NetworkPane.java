/*
 * Remote sensing images: database, simulation and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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
package fr.ird.neural;

// Graphics tools
import java.awt.Font;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import org.geotools.gui.swing.ZoomPane;

// Geometry
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

// Collections
import java.util.Map;
import java.util.HashMap;


/**
 * A graphics representation of a network.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class NetworkPane extends ZoomPane
{
    /**
     * The neural network to display.
     */
    private final FeedForwardNet network;

    /**
     * Positions of nodes, in logicial coordinates.
     * Positions point to nodes center.
     */
    private final Map<Neuron,Point> nodePositions = new HashMap<Neuron,Point>();

    /**
     * The bounding box of positions.
     */
    private Rectangle bounds;

    /**
     * The maximal connection weight found in the network.
     * This is used for drawing connections line with
     * a darker color for strongest connections.
     */
    private double maxWeight;

    /**
     * The shape to use for drawing neurons. Coordinate of this
     * shape will change during drawing. However, width and height
     * will stay unchanged.
     */
    private final RectangularShape shape = new Ellipse2D.Float(0, 0, 20, 20);

    /**
     * Color for filling neurons.
     */
    private final Color neuronFill = Color.orange;

    /**
     * Color for drawing neurons contour.
     */
    private final Color neuronDraw = neuronFill.darker();

    /**
     * Color for filling the neurons bias.
     */
    private final Color biasFill = Color.yellow;

    /**
     * Space (in pixel) to put between each neurons,
     * <strong>including</strong> the neuron's radius.
     */
    private final int horizontalSpace = 40;

    /**
     * Space (in pixel) to put between each layers,
     * <strong>including</strong> the neuron's radius.
     */
    private final int verticalSpace = 120;

    /**
     * Vertical offset between the input or output neurons and
     * the neuron label. Includes the text and half the neuron
     * height.
     */
    private final int textOffset = (int)shape.getHeight()/2 + 16;

    /**
     * Construct a pane for the specified neural network.
     */
    public NetworkPane(final FeedForwardNet network)
    {
        super(TRANSLATE_X | TRANSLATE_Y | UNIFORM_SCALE | RESET);
        this.network = network;
        setBackground(Color.white);
        setForeground(new Color(0,128,48));
        setPaintingWhileAdjusting(true);
        updatePositions(network.neurons);
        updateWeightAmplitude(network.neurons);
    }

    /**
     * Find the maximal weight occuring in the network.
     *
     * @param neurons The neurons network.
     */
    private void updateWeightAmplitude(final Neuron[][] neurons)
    {
        maxWeight = 0;
        for (int j=neurons.length; --j>=0;)
        {
            final Neuron[] layer = neurons[j];
            for (int i=layer.length; --i>=0;)
            {
                final double[] weights = layer[i].weights;
                if (weights!=null)
                {
                    for (int k=weights.length; --k>=0;)
                    {
                        final double weight = Math.abs(weights[k]);
                        if (weight > maxWeight)
                            maxWeight = weight;
                    }
                }
            }
        }
        if (!(maxWeight > 0))
            maxWeight = 1;
    }

    /**
     * Computes coordinates for node's center. Olds values of {@link #nodePositions}
     * and {@link #bounds} are discarted, and new values are computed using the
     * specified neuron network.
     *
     * @param neurons The neurons network.
     */
    private void updatePositions(final Neuron[][] neurons)
    {
        nodePositions.clear();
        bounds = null;
        for (int j=neurons.length; --j>=0;)
        {
            final Neuron[] layer = neurons[j];
            final int margin = ((layer.length-1)*horizontalSpace)/-2;
            for (int i=layer.length; --i>=0;)
            {
                final Point position = new Point(margin+i*horizontalSpace, j*verticalSpace);
                nodePositions.put(layer[i], position);
                if (bounds!=null) bounds.add(position);
                else bounds = new Rectangle(position);
            }
        }
    }

    /**
     * Draw the network. This method paint all nodes (including bias) and all their
     * connections. The bias is painted in a different color. Connections color may
     * vary according their weight.
     */
    protected void paintComponent(final Graphics2D graphics)
    {
        graphics.transform(zoom);
        final Font font = graphics.getFont();
        final FontRenderContext fontContext = graphics.getFontRenderContext();

        final Neuron[][] neurons=network.neurons;
        boolean isPaintingOutputLayer=true;
        for (int j=neurons.length; --j>=0;)
        {
            final Neuron[] layer = neurons[j];
            for (int i=layer.length; --i>=0;)
            {
                final Neuron  neuron = layer[i];
                final Point position = nodePositions.get(neuron);
                /*
                 * Paint connections first. Strong connections are
                 * drawn black, weak connections are drawn white.
                 */
                final Neuron[] inputs = neuron.inputs;
                if (inputs!=null)
                {
                    final double[] weights=neuron.weights;
                    for (int k=inputs.length; --k>=0;)
                    {
                        final float c = 1-(float)(Math.abs(weights[k]) / maxWeight);
                        final Point inputPos = nodePositions.get(inputs[k]);
                        graphics.setColor(new Color(c,c,c));
                        graphics.drawLine(position.x, position.y, inputPos.x, inputPos.y);
                    }
                }
                /*
                 * Paint the neuron.
                 */
                shape.setFrame(position.x-(int)shape.getWidth()/2,
                               position.y-(int)shape.getHeight()/2,
                               shape.getWidth(), shape.getHeight());
                graphics.setColor(neuron.isBias() ? biasFill : neuronFill);
                graphics.fill(shape);
                graphics.setColor(neuronDraw);
                graphics.draw(shape);
                /*
                 * Paint labels.
                 */
                final String label=neuron.label;
                if (label!=null)
                {
                    graphics.setColor(getForeground());
                    final GlyphVector glyphs = font.createGlyphVector(fontContext, label);
                    final Rectangle2D labelBounds = glyphs.getLogicalBounds();
                    final float x = (float)(position.x - labelBounds.getCenterX());
                    if (isPaintingOutputLayer)
                    {
                        graphics.drawGlyphVector(glyphs, x, position.y+textOffset);
                    }
                    if (j==0)
                    {
                        graphics.drawGlyphVector(glyphs, x, position.y-textOffset + (float)labelBounds.getMaxY());
                    }
                }
            }
            isPaintingOutputLayer = false;
        }
    }
    
    /**
     * Returns the bounding box of the drawing area, in logical
     * coordinates. This is used internally by {@link ZoomPane}.
     */
    public Rectangle2D getArea()
    {
        if (bounds!=null)
        {
            final int width  = (int)shape.getWidth()  + textOffset;
            final int height = (int)shape.getHeight() + textOffset;
            return new Rectangle(bounds.x-width, bounds.y-height, bounds.width+2*width, bounds.height+2*height);
        }
        else return bounds;
    }

    /**
     * Réinitialise la transformation affine
     * {@link #zoom} de façon à annuler tout
     * zoom.
     */
    public void reset()
    {reset(getZoomableBounds(null), false);}
}
