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
 */
package fr.ird.operator.coverage;

// Géométrie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;

// Java Advanced Imaging et divers
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;


/**
 * Une fonction calculant la valeur moyenne des pixels dans une région géographique. Cet
 * objet {@link Coverage} aura le même nombre de bandes que l'objet {@link GridCoverage}
 * source, la moyenne étant calculée pour chaque bande.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class AverageEvaluator extends Evaluator {
    /**
     * Construit un évaluateur pour l'image spécifiée.
     *
     * @param coverage Les données sources.
     * @param area La forme géométrique de la région à évaluer.
     */
    public AverageEvaluator(final GridCoverage coverage, final RectangularShape area) {
        super("Moyenne", 0, coverage, area);
    }

    /**
     * Calcule la moyenne dans la région géographique spécifiée.
     *
     * @param  area  Région géographique autour de laquelle évaluer la fonction.
     * @param  dest  Tableau dans lequel mémoriser le résultat, ou <code>null</code>.
     * @return Les résultats par bandes.
     */
    public double[] evaluate(final Shape area, double[] dest) {
        final RenderedImage        data = coverage.getRenderedImage();
        final AffineTransform transform = (AffineTransform) coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final Point2D.Double coordinate = new Point2D.Double();
        final Rectangle2D    areaBounds = area.getBounds2D();
        final Rectangle          bounds = getBounds(areaBounds, transform, data);
        final int[]               count = new int[data.getSampleModel().getNumBands()];
        final double[]              sum = new double[count.length];
        double[] values = null;
        if (!bounds.isEmpty()) {
            final RectIter iterator = RectIterFactory.create(data, bounds);
            for (int y=bounds.y; !iterator.finishedLines(); y++) {
                for (int x=bounds.x; !iterator.finishedPixels(); x++) {
                    assert bounds.contains(x,y);
                    coordinate.x = x;
                    coordinate.y = y;
                    // TODO: que faire si 'area' intercepte le pixel mais pas le centre?
                    //       'Shape.intersects' risque de ne pas être assez précis.
                    if (area.contains(transform.transform(coordinate, coordinate))) {
                        values = iterator.getPixel(values);
                        for (int i=0; i<values.length; i++) {
                            final double z = values[i];
                            if (!Double.isNaN(z)) {
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
        assert sum.length == bands.length;
        if (dest == null) {
            dest = new double[sum.length];
        }
        for (int i=0; i<sum.length; i++) {
            dest[i] = sum[i] / count[i];
        }
        return dest;
    }
}
