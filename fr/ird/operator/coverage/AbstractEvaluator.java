/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.operator.coverage;

// Miscellaneous
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;
import org.geotools.resources.XAffineTransform;


/**
 * Implémentation de base pour {@link Evaluator}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class AbstractEvaluator implements Evaluator
{
    /**
     * Construit un évaluateur par défaut.
     */
    public AbstractEvaluator()
    {}

    /**
     * Transform a geographic bounding box into a grid bounding box.
     * The resulting bounding box will be clipped to image's bounding
     * box.
     *
     * @param areaBounds The geographic bounding box.
     * @param transform The grid to coordinate system transform. The inverse
     *        transform will be used for transforming <code>areaBounds</code>.
     * @param data The rendered image for which the bounding box is computed.
     */
    protected static Rectangle getBounds(final Rectangle2D     areaBounds,
                                         final AffineTransform transform,
                                         final RenderedImage   data)
    {
        // 'Rectangle' performs the correct rounding.
        Rectangle bounds = new Rectangle();
        try
        {
            bounds = (Rectangle)XAffineTransform.inverseTransform(transform, areaBounds, bounds);
            int xmin = data.getMinX();
            int ymin = data.getMinY();
            int xmax = data.getWidth()  + xmin;
            int ymax = data.getHeight() + ymin;
            int t;
            if ((t =bounds.x     ) > xmin) xmin = t;
            if ((t+=bounds.width ) < xmax) xmax = t;
            if ((t =bounds.y     ) > ymin) ymin = t;
            if ((t+=bounds.height) < ymax) ymax = t;
            bounds.x      = xmin;
            bounds.y      = ymin;
            bounds.width  = xmax-xmin;
            bounds.height = ymax-ymin;
        }
        catch (NoninvertibleTransformException exception)
        {
            Utilities.unexpectedException("fr.ird.operator", "Evaluator", "evaluate", exception);
            // Returns an empty bounds.
        }
        return bounds;
    }
}
