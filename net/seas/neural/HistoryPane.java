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
package net.seas.neural;

// Graphics tools
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import org.geotools.gui.swing.ZoomPane;

// Geometry
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;


/**
 * Panel for displaying the history of a network.
 * <strong>This is a temporary class</strong>.
 * This class may be removed when we will have
 * a more general one for ploting data.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class HistoryPane extends ZoomPane
{
    /**
     * The neural network to display.
     */
    private final FeedForwardNet network;

    /**
     * The maximum error.
     */
    private float maxError;

    /**
     * Construct a pane for the specified neural network.
     */
    public HistoryPane(final FeedForwardNet network)
    {
        super(TRANSLATE_X | TRANSLATE_Y | SCALE_X | SCALE_Y | RESET);
        this.network = network;
        updateMaxError();
        setPaintingWhileAdjusting(true);
    }

    /**
     * Update the maximum error.
     */
    private void updateMaxError()
    {
        maxError = 0;
        final float[] history = network.trainHistory;
        for (int i=history.length; --i>=0;)
        {
            final float error = history[i];
            if (error > maxError)
                maxError = error;
        }
    }

    /**
     * Draw the history.
     */
    protected void paintComponent(final Graphics2D graphics)
    {
        graphics.transform(zoom);
        graphics.setStroke(new BasicStroke(0));
        graphics.setColor(Color.black);
        graphics.draw(getArea());
        graphics.setColor(Color.blue);
        final Line2D.Float line=new Line2D.Float();
        final float[] history = network.trainHistory;
        for (int i=1; i<history.length; i++)
        {
            line.x1 = i-1;
            line.y1 = history[i-1];
            line.x2 = i;
            line.y2 = history[i];
            graphics.draw(line);
        }
    }
    
    /**
     * Returns the bounding box of the drawing area.
     */
    public Rectangle2D getArea()
    {return new Rectangle2D.Float(0, 0, network.trainHistory.length, maxError);}
}
