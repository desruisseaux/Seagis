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

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.Utilities;

// Géométrie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Java Advanced Imaging et divers
import java.util.Arrays;
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;


/**
 * Une fonction déterminant la position de la valeur maximale
 * dans une région géographique.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class MaximumLocator extends AbstractEvaluator implements Locator
{
    /**
     * Construit un objet par défaut.
     */
    public MaximumLocator()
    {}

    /**
     * Retourne la position du pixel maximal.
     *
     * @param  coverage La couverture sur laquelle appliquer la fonction.
     * @param  area La région géographique dans laquelle rechercher un pixel.
     *         Les coordonnées de cette région doivent être exprimées selon
     *         le système de coordonnées de <code>coverage</code>.
     * @return La position du pixel recherché selon le système de coordonnées
     *         de l'image. Cette méthode retourne une position pour chaque
     *         bande.
     */
    public Values[] locate(final GridCoverage coverage, final Shape area)
    {
        final RenderedImage        data = coverage.getRenderedImage(true);
        final AffineTransform transform = (AffineTransform) coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final Point2D.Double coordinate = new Point2D.Double();
        final Rectangle2D    areaBounds = area.getBounds2D();
        final Rectangle          bounds = getBounds(areaBounds, transform, data);
        final Point2D[]       locations = new Point2D[data.getSampleModel().getNumBands()];
        final double[]         maximums = new double[locations.length];
        Arrays.fill(maximums, Double.NEGATIVE_INFINITY);
        double[] values=null;
        if (!bounds.isEmpty())
        {
            final RectIter iterator = RectIterFactory.create(data, bounds);
            for (int y=bounds.y; !iterator.finishedLines(); y++)
            {
                for (int x=bounds.x; !iterator.finishedPixels(); x++)
                {
                    assert bounds.contains(x,y);
                    coordinate.x = x;
                    coordinate.y = y;
                    if (area.contains(transform.transform(coordinate, coordinate)))
                    {
                        values = iterator.getPixel(values);
                        for (int i=0; i<values.length; i++)
                        {
                            final double z = values[i];
                            if (z > maximums[i])
                            {
                                maximums[i] = z;
                                if (locations[i]==null)
                                {
                                    locations[i] = new Point2D.Double();
                                }
                                locations[i].setLocation(coordinate);
                            }
                        }
                    }
                    iterator.nextPixel();
                }
                iterator.startPixels();
                iterator.nextLine();
            }
        }
        final Values[] result = new Values[locations.length];
        for (int i=0; i<result.length; i++)
        {
            if (locations[i] != null)
            {
                result[i] = new Values(maximums[i], locations[i]);
            }
        }
        return result;
    }
}
