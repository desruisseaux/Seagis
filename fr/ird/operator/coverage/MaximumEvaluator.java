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
package fr.ird.operator.coverage;

// G�om�trie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;

// Java Advanced Imaging et divers
import java.util.Arrays;
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.Utilities;

// Sesgis dependencies
import fr.ird.util.XArray;


/**
 * Une fonction d�terminant la position de la valeur maximale dans une r�gion g�ographique. Cet
 * objet {@link Coverage} aura le m�me nombre de bandes que l'objet {@link GridCoverage} source,
 * suivit de bandes repr�sentant les coordonn�es (<var>x</var>,<var>y</var>) des endroits o� le
 * maximum a �t� trouv�.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class MaximumEvaluator extends Evaluator {
    /**
     * Construit un �valuateur pour l'image sp�cifi�e.
     *
     * @param coverage Les donn�es sources.
     * @param area La forme g�om�trique de la r�gion � �valuer.
     */
    public MaximumEvaluator(final GridCoverage coverage, final RectangularShape area) {
        super("Maximum", coverage.getSampleDimensions().length, coverage, area);
        /*
         * En plus de la valeur, ne retient que les bandes des coordonn�es (x,y). Si
         * il y avait des bandes pour des coordonn�es (z,...), elles seront �limin�es.
         */
        bands = XArray.resize(bands, 3*coverage.getSampleDimensions().length);
    }

    /**
     * Trouve le maximum dans la r�gion g�ographique sp�cifi�e.
     *
     * @param  area  R�gion g�ographique autour de laquelle �valuer la fonction.
     * @param  dest  Tableau dans lequel m�moriser le r�sultat, ou <code>null</code>.
     * @return Les r�sultats par bandes.
     */
    public double[] evaluate(final Shape area, double[] dest) {
        if (dest == null) {
            dest = new double[bands.length];
        }
        final RenderedImage        data = coverage.getRenderedImage();
        final AffineTransform transform = (AffineTransform) coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final Point2D.Double coordinate = new Point2D.Double();
        final Rectangle2D    areaBounds = area.getBounds2D();
        final Rectangle          bounds = getBounds(areaBounds, transform, data);
        final int              numBands = data.getSampleModel().getNumBands();
        Arrays.fill(dest, 0, numBands, Double.NEGATIVE_INFINITY);
        Arrays.fill(dest, numBands, bands.length, Double.NaN);
        double[] values = null;
        if (!bounds.isEmpty()) {
            final RectIter iterator = RectIterFactory.create(data, bounds);
            for (int y=bounds.y; !iterator.finishedLines(); y++) {
                for (int x=bounds.x; !iterator.finishedPixels(); x++) {
                    assert bounds.contains(x,y);
                    coordinate.x = x;
                    coordinate.y = y;
                    if (area.contains(transform.transform(coordinate, coordinate))) {
                        values = iterator.getPixel(values);
                        for (int i=0; i<values.length; i++) {
                            final double z = values[i];
                            if (z > dest[i]) {
                                dest[i             ] =            z;
                                dest[i +   numBands] = coordinate.x;
                                dest[i + 2*numBands] = coordinate.y;
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
         * Si certains maximums n'ont pas �t� trouv�s, donne la valeur NaN.
         */
        for (int i=0; i<numBands; i++) {
            if (Double.isInfinite(dest[        i]) &&
                Double.isNaN(dest[numBands   + i]) &&
                Double.isNaN(dest[numBands*2 + i]))
            {
                dest[i] = Double.NaN;
            }
        }
        return dest;
    }
}
