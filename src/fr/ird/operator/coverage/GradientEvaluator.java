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
import java.awt.geom.NoninvertibleTransformException;

// Java Advanced Imaging et divers
import java.util.Arrays;
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

// Geotools dependencies
import org.geotools.cs.Ellipsoid;
import org.geotools.gc.GridCoverage;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.resources.CTSUtilities;

// Sesgis dependencies
import fr.ird.resources.XArray;


/**
 * Une fonction estimant le gradient dans une r�gion g�ographique. Cet objet {@link Coverage}
 * aura le m�me nombre de bandes que l'objet {@link GridCoverage} source, le gradient �tant
 * calcul� pour chaque bande.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class GradientEvaluator extends Evaluator {
    /**
     * Facteur pour convertir des m�tres vers les unit�s du r�sultat.
     * Cette classe calcule au d�part des gradients en unit�s du param�tre
     * par m�tres. Ce facteur convertit le gradient en unit�s par 60 milles
     * nautiques (environ 1 degr�s de latitude).
     *
     * TODO: C'est une solution temporaire. Dans une version future, il faudra
     *       faire une gestion correcte des unit�s.
     */
    private static final double METERS_BY_UNIT = 60*1852;

    /**
     * Rang du gradient � choisir, en percentage. Par exemple la valeur 0.8
     * signifie que le gradient retenu sera celui qui est sup�rieur � 80% de
     * tous les gradients.
     */
    private final double percentile;

    /**
     * Construit un �valuateur pour l'image sp�cifi�e.
     *
     * @param coverage Les donn�es sources.
     * @param area La forme g�om�trique de la r�gion � �valuer.
     */
    public GradientEvaluator(final GridCoverage coverage, final RectangularShape area) {
        this(coverage, area, 0.8);
    }

    /**
     * Construit un �valuateur pour l'image sp�cifi�e avec le rang sp�cifi�.
     * Par exemple la valeur 0.8 signifie que le gradient retenu sera celui
     * qui est sup�rieur � 80% de tous les gradients.
     *
     * @param coverage Les donn�es sources.
     * @param area La forme g�om�trique de la r�gion � �valuer.
     * @param percentile Le rang percentile du gradient � retenir.
     */
    public GradientEvaluator(final GridCoverage coverage,
                             final RectangularShape area,
                             final double     percentile)
    {
        super("Gradient", 0, coverage, area);
        this.percentile = percentile;
        // TODO: il faudrait ajuster la couleur des bandes.
    }

    /**
     * Calcule le gradient dans la r�gion g�ographique sp�cifi�e.
     *
     * @param  area  R�gion g�ographique autour de laquelle �valuer la fonction.
     * @param  dest  Tableau dans lequel m�moriser le r�sultat, ou <code>null</code>.
     * @return Les r�sultats par bandes.
     */
    public double[] evaluate(final Shape area, double[] dest) {
        final RenderedImage         data = coverage.getRenderedImage();
        final AffineTransform  transform = (AffineTransform) coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final Ellipsoid        ellipsoid = CTSUtilities.getEllipsoid(coverage.getCoordinateSystem());
        final Point2D.Double coordinate0 = new Point2D.Double();
        final Point2D.Double coordinate1 = new Point2D.Double();
        final Rectangle2D     areaBounds = area.getBounds2D();
        final Rectangle           bounds = getBounds(areaBounds, transform, data);
        final int[]                count = new int[data.getSampleModel().getNumBands()];
        final double[][]       gradients = new double[count.length][];
        for (int i=0; i<gradients.length; i++) {
            gradients[i] = new double[Math.max(bounds.width*bounds.height/2, 8)];
        }
        double[] values0 = null;
        double[] values1 = null;
        if (!bounds.isEmpty()) {
            final RectIter iterator0 = RectIterFactory.create(data, bounds);
            final RectIter iterator1 = RectIterFactory.create(data, bounds);
            for (int y0=bounds.y; !iterator0.finishedLines(); y0++) {
                for (int x0=bounds.x; !iterator0.finishedPixels(); x0++) {
                    assert bounds.contains(x0,y0);
                    coordinate0.x = x0;
                    coordinate0.y = y0;
                    if (area.contains(transform.transform(coordinate0, coordinate0))) {
                        values0 = iterator0.getPixel(values0);
                        /*
                         * 'values0' is the pixel value at geographic coordinate (x0,y0).
                         * Now, we will check 'value1' at geographic coordinates (x1,y1)
                         * after (x0,y0);  no need to check points before (x0,y0) since
                         * it is already done. The starting point it (firstX,firstY).
                         */
                        int firstY = y0;
                        int firstX = x0;
                        if (++firstX >= bounds.x+bounds.width) {
                            firstX = bounds.x;
                            if (++firstY >= bounds.y+bounds.height) {
                                continue;
                            }
                        }
                        iterator1.startLines();
                        iterator1.jumpLines(firstY - bounds.y);
                        for (int y1=firstY; !iterator1.finishedLines(); y1++) {
                            iterator1.startPixels();
                            iterator1.jumpPixels(firstX - bounds.x);
                            for (int x1=firstX; !iterator1.finishedPixels(); x1++) {
                                assert bounds.contains(x1,y1);
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
                            iterator1.nextLine();
                            firstX = bounds.x;
                        }
                        firstY = bounds.y;
                    }
                    iterator0.nextPixel();
                }
                iterator0.startPixels();
                iterator0.nextLine();
            }
        }
        if (dest == null) {
            dest = new double[gradients.length];
        }
        for (int i=0; i<gradients.length; i++) {
            final double[] array = gradients[i] = XArray.resize(gradients[i], count[i]);
            final int index = Math.min((int)(percentile*array.length), array.length-1);
            if (index >= 0) {
                Arrays.sort(array);
                dest[i] = array[index] * METERS_BY_UNIT;
            } else {
                dest[i] = Double.NaN;
            }
        }
        return dest;
    }
}
