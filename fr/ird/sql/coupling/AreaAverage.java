/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.sql.coupling;

// OpenGIS dependencies
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.cv.PointOutsideCoverageException;

// G�om�trie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seas.util.XAffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Java Advanced Imaging et divers
import java.awt.image.RenderedImage;
import net.seas.awt.ExceptionMonitor;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;


/**
 * Une fonction calculant la valeur moyenne
 * des pixels dans une r�gion g�ographique.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class AreaAverage implements AreaEvaluator
{
    /**
     * Construit un �valuateur par d�faut.
     */
    public AreaAverage()
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
    final Rectangle getBounds(final Rectangle2D areaBounds, final AffineTransform transform, final RenderedImage data)
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
            ExceptionMonitor.unexpectedException("fr.ird.sql", "AreaEvaluator", "evaluate", exception);
            // Returns an empty bounds.
        }
        return bounds;
    }

    /**
     * Evalue la fonction pour une zone g�ographique de la couverture sp�cifi�e.
     * Cette fonction est �valu�e pour chaque bande de la couverture (ou image).
     *
     * @param coverage La couverture sur laquelle appliquer la fonction.
     * @param area La r�gion g�ographique sur laquelle �valuer la fonction.
     *        Les coordonn�es de cette r�gion doivent �tre exprim�es selon
     *        le syst�me de coordonn�es de <code>coverage</code>.
     */
    public double[] evaluate(final GridCoverage coverage, final Shape area)
    {
        final RenderedImage        data = coverage.getRenderedImage(true);
        final AffineTransform transform = coverage.getGridGeometry().getAffineTransform2D();
        final Point2D.Double coordinate = new Point2D.Double();
        final Rectangle2D    areaBounds = area.getBounds();
        final Rectangle          bounds = getBounds(areaBounds, transform, data);
        final int[]               count = new int[data.getSampleModel().getNumBands()];
        final double[]              sum = new double[count.length];
        double[] values = null;
        if (!bounds.isEmpty())
        {
            final RectIter iterator = RectIterFactory.create(data, bounds);
            for (int y=bounds.y; !iterator.finishedLines(); y++)
            {
                for (int x=bounds.x; !iterator.finishedPixels(); x++)
                {
                    assert(bounds.contains(x,y));
                    coordinate.x = x;
                    coordinate.y = y;
                    // TODO: que faire si 'area' intercepte le pixel mais pas le centre?
                    //       'Shape.intersects' risque de ne pas �tre assez pr�cis.
                    if (area.contains(transform.transform(coordinate, coordinate)))
                    {
                        values = iterator.getPixel(values);
                        for (int i=0; i<values.length; i++)
                        {
                            final double z = values[i];
                            if (!Double.isNaN(z))
                            {
                                sum[i] += z;
                                count[i]++;
                            }
                        }
                    }
                    iterator.nextPixel();
                }
                iterator.startPixels();
                iterator.nextLine();
            }
        }
        /*
         * Compute average. If count[i]==0, fallback
         * on pixel value at the shape center.
         */
        boolean fallback=false;
        for (int i=0; i<sum.length; i++)
        {
            if (Double.isNaN(sum[i] /= count[i]))
            {
                if (!fallback)
                {
                    coordinate.x = areaBounds.getCenterX();
                    coordinate.y = areaBounds.getCenterY();
                    values = coverage.evaluate(coordinate, values);
                    fallback = true;
                }
                sum[i] = values[i];
            }
        }
        return sum;
    }
}
