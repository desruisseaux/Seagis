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
import org.geotools.cs.Ellipsoid;
import org.geotools.gc.GridCoverage;

// Géométrie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import org.geotools.resources.XAffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Java Advanced Imaging et divers
import java.util.Arrays;
import fr.ird.util.XArray;
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;


/**
 * Une fonction estimant le gradient dans une région géographique.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class GradientEvaluator extends AbstractEvaluator implements Evaluator {
    /**
     * Facteur pour convertir des mètres vers les unités du résultat.
     * Cette classe calcule au départ des gradients en unités du paramètre
     * par mètres. Ce facteur convertit le gradient en unités par 60 milles
     * nautiques (environ 1 degrés de latitude).
     *
     * TODO: C'est une solution temporaire. Dans une version future, il faudra
     *       faire une gestion correcte des unités.
     */
    private static final double METERS_BY_UNIT = 60*1852;

    /**
     * Rang du gradient à choisir, en percentage. Par exemple la valeur 0.8
     * signifie que le gradient retenu sera celui qui est supérieur à 80% de
     * tous les gradients.
     */
    private final double percentile;

    /**
     * Construit un évaluateur par défaut.
     */
    public GradientEvaluator() {
        this(0.8);
    }

    /**
     * Retourne le nom de cette opération.
     */
    public String getName() {
        return "Gradient";
    }

    /**
     * Construit un évaluateur par avec le rang spécifié. Par exemple la valeur
     * 0.8 signifie que le gradient retenu sera celui qui est supérieur à 80%
     * de tous les gradients.
     */
    public GradientEvaluator(final double percentile) {
        this.percentile = percentile;
    }

    /**
     * Evalue la fonction pour une zone géographique de la couverture spécifiée.
     * Cette fonction est évaluée pour chaque bande de la couverture (ou image).
     *
     * @param coverage La couverture sur laquelle appliquer la fonction.
     * @param area La région géographique sur laquelle évaluer la fonction.
     *        Les coordonnées de cette région doivent être exprimées selon
     *        le système de coordonnées de <code>coverage</code>.
     */
    public ParameterValue[] evaluate(final GridCoverage coverage, final Shape area) {
        final RenderedImage         data = coverage.getRenderedImage();
        final AffineTransform  transform = (AffineTransform) coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final Ellipsoid        ellipsoid = Ellipsoid.WGS84; // TODO: interroger le système de coordonnées!
        final Point2D.Double coordinate0 = new Point2D.Double();
        final Point2D.Double coordinate1 = new Point2D.Double();
        final Rectangle2D     areaBounds = area.getBounds2D();
        final Rectangle           bounds = getBounds(areaBounds, transform, data);
        final int[]                count = new int[data.getSampleModel().getNumBands()];
        final double[][]       gradients = new double[count.length][];
        for (int i=0; i<gradients.length; i++) {
            gradients[i] = new double[Math.max(bounds.width*bounds.height, 64)];
        }
        double[] values0 = null;
        double[] values1 = null;
        if (!bounds.isEmpty()) {
            final RectIter iterator0 = RectIterFactory.create(data, bounds);
            final RectIter iterator1 = RectIterFactory.create(data, bounds);
            for (int y0=bounds.y; !iterator0.finishedLines(); y0++) {
                for (int x0=bounds.x; !iterator0.finishedPixels(); x0++) {
                    assert(bounds.contains(x0,y0));
                    coordinate0.x = x0;
                    coordinate0.y = y0;
                    if (area.contains(transform.transform(coordinate0, coordinate0))) {
                        values0 = iterator0.getPixel(values0);
                        iterator1.startLines();
                        for (int y1=bounds.y; !iterator1.finishedLines(); y1++) {
                            for (int x1=bounds.x; !iterator1.finishedPixels(); x1++) {
                                assert(bounds.contains(x1,y1));
                                coordinate1.x = x1;
                                coordinate1.y = y1;
                                if (area.contains(transform.transform(coordinate1, coordinate1))) {
                                    values1 = iterator1.getPixel(values1);
                                    final double distance = ellipsoid.orthodromicDistance(coordinate0, coordinate1);
                                    for (int i=Math.min(values0.length, values1.length); --i>=0;) {
                                        final double gradient = Math.abs(values0[i]-values1[i])/distance;
                                        if (!Double.isNaN(gradient) && !Double.isInfinite(gradient)) {
                                            final int index = count[i]++;
                                            double[] array = gradients[i];
                                            if (index >= array.length) {
                                                gradients[i] = array = XArray.resize(array, index*2);
                                            }
                                            array[index] = gradient;
                                        }
                                    }
                                }
                                iterator1.nextPixel();
                            }
                            iterator1.startPixels();
                            iterator1.nextLine();
                        }
                    }
                    iterator0.nextPixel();
                }
                iterator0.startPixels();
                iterator0.nextLine();
            }
        }
        final ParameterValue[] result = new ParameterValue[gradients.length];
        for (int i=0; i<gradients.length; i++) {
            final double[] array = gradients[i] = XArray.resize(gradients[i], count[i]);
            final int index = Math.min((int)(percentile*array.length), array.length-1);
            if (index >= 0) {
                Arrays.sort(array);
                result[i] = new ParameterValue.Double(coverage, this);
                result[i].setValue(array[index]*METERS_BY_UNIT, null);
            }
        }
        return result;
    }
}
