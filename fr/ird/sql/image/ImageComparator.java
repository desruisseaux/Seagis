/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.sql.image;

// Geotools dependencies
import org.geotools.pt.Envelope;
import org.geotools.cs.AxisInfo;
import org.geotools.gc.GridRange;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.AxisOrientation;
import org.geotools.cs.CoordinateSystem;

import org.geotools.ct.MathTransform;
import org.geotools.ct.TransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.CoordinateTransformationFactory;

import org.geotools.resources.CTSUtilities;
import org.geotools.resources.Utilities;

// Divers
import org.geotools.units.Unit;
import java.util.Comparator;


/**
 * Compare deux entrées  {@link ImageEntry}  en fonction d'un critère arbitraire.
 * Ce comparateur sert à classer un tableau d'images  en fonction de leur intérêt
 * par rapport à ce qui avait été demandé.   L'implémentation par défaut favorise
 * les images dont la plage de temps couvre le mieux la plage demandée (les dates
 * de début et de fin),  et n'examinera la couverture spatiale que si deux images
 * ont une couverture temporelle équivalente. Cette politique est appropriée lorsque
 * les images couvrent à peu près la même région, et que les dates de ces images est
 * le principal facteur qui varie. Les critères de comparaison utilisés sont:
 *
 * <ul>
 *  <li>Pour chaque image, la quantité [<i>temps à l'intérieur de la plage de temps
 *      demandée</i>]-[<i>temps à l'extérieur de la plage de temps demandé</i>] sera
 *      calculée. Si une des image à une quantité plus grande, elle sera choisie.</li>
 *  <li>Sinon, si une image se trouve mieux centrée sur la plage de temps demandée, cette
 *      image sera choisie.</li>
 *  <li>Sinon, pour chaque image, l'intersection entre la région de l'image et la région
 *      demandée sera obtenue, et la superficie de cette intersection calculée. Si une
 *      des images obtient une valeur plus grande, cette image sera choisie.</li>
 *  <li>Sinon, la superficie moyenne des pixels des images seront calculées. Si une image
 *      a des pixels d'une meilleure résolution (couvrant une surface plus petite), cette
 *      image sera choisie.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ImageComparator implements Comparator<ImageEntry> {
    /**
     * Object à utiliser pour construire des transformations de coordonnées.
     */
    private final CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();

    /**
     * Système de coordonnées dans lequel faire les comparaisons. Il s'agit
     * du système de coordonnées de la table {@link ImageTable} d'origine.
     */
    private final CoordinateSystem coordinateSystem;

    /**
     * Transformation permettant de passer du système de coordonnées
     * d'un objet {@link ImageEntry} vers le système de coordonnées
     * de {@link ImageTable} (c'est-à-dire {@link #coordinateSystem}).
     * Cette transformation est conservée dans une cache interne afin
     * d'éviter de construire cet objet trop fréquement.
     */
    private transient CoordinateTransformation transformation;

    /**
     * Ellispoide à utiliser pour calculer les distances orthodromiques.
     */
    private final Ellipsoid ellipsoid;

    /**
     * Coordonnées spatio-temporelles demandées. Ils s'agit des coordonnées
     * qui avait été spécifiée à la table {@link ImageTable}. Cette envelope
     * est exprimée selon le système de coordonnées {@link #coordinateSystem}.
     * Cette enveloppe n'est pas clonée. Elle ne doit donc pas être modifiée.
     */
    private final Envelope target;

    /**
     * Une estimation de la superficie de <code>target</code>.
     * Cette estimation est assez approximative.
     */
    private final double area;

    /**
     * Dimension des axes des <var>x</var> (longitude),
     * <var>y</var> (latitude) et <var>t</var> (temps).
     */
    private final int xDim, yDim, tDim;

    /**
     * Construit un comparateur pour les images de la table spécifiée.
     *
     * @param table Table qui a produit les images qui seront à comparer.
     */
    public ImageComparator(final ImageTable table) {
        this(table.getCoordinateSystem(), table.getEnvelope());
    }

    /**
     * Construit un comparateur avec les plages spatio-temporelles spécifiées.
     *
     * @param cs Système de coordonnées dans lequel comparer les images.
     * @param envelope Coordonnées spatio-temporelle de la région qui avait été demandée.
     *        Ces coordonnées doivent être exprimées selon le système de coordonnées
     *        <code>coordinateSystem</code>.
     */
    public ImageComparator(final CoordinateSystem cs, final Envelope envelope) {
        int xDim = -1;
        int yDim = -1;
        int tDim = -1;
        for (int i=cs.getDimension(); --i>=0;) {
            final AxisOrientation orientation = cs.getAxis(i).orientation.absolute();
            if (orientation.equals(AxisOrientation.EAST  )) xDim = i;
            if (orientation.equals(AxisOrientation.NORTH )) yDim = i;
            if (orientation.equals(AxisOrientation.FUTURE)) tDim = i;
        }
        this.xDim             = xDim;
        this.yDim             = yDim;
        this.tDim             = tDim;
        this.coordinateSystem = cs;
        this.target           = envelope;
        this.ellipsoid        = CTSUtilities.getHorizontalDatum(cs).getEllipsoid();
        this.area             = getArea(target);
    }

    /**
     * Retourne une estimation de la superficie occupée par
     * la composante horizontale de l'envelope spécifiée.
     */
    private double getArea(final Envelope envelope) {
        if (xDim<0 || yDim<0) return Double.NaN;
        return getArea(envelope.getMinimum(xDim), envelope.getMinimum(yDim),
                       envelope.getMaximum(xDim), envelope.getMaximum(yDim));
    }

    /**
     * Retourne une estimation de la superficie occupée par
     * un rectangle délimitée par les coordonnées spécifiées.
     */
    private double getArea(double xmin, double ymin, double xmax, double ymax) {
        final Unit xUnit = coordinateSystem.getUnits(xDim);
        final Unit yUnit = coordinateSystem.getUnits(yDim);
        xmin = Unit.DEGREE.convert(xmin, xUnit);
        xmax = Unit.DEGREE.convert(xmax, xUnit);
        ymin = Unit.DEGREE.convert(ymin, yUnit);
        ymax = Unit.DEGREE.convert(ymax, yUnit);
        if (xmin<xmax && ymin<ymax) {
            final double centerX = 0.5*(xmin+xmax);
            final double centerY = 0.5*(ymin+ymax);
            final double   width = ellipsoid.orthodromicDistance(xmin, centerY, xmax, centerY);
            final double  height = ellipsoid.orthodromicDistance(centerX, ymin, centerX, ymax);
            return width*height;
        } else {
            return 0;
        }
    }

    /**
     * Compare deux objets {@link ImageEntry}. Les classes dérivées peuvent
     * redéfinir cette méthode pour définir un autre critère de comparaison
     * que les critères par défaut.
     *
     * @return +1 si l'image <code>entry1</code> représente le plus grand intérêt,
     *         -1 si l'image <code>entry2</code> représente le plus grand intérêt, ou
     *          0 si les deux images représentent le même intérêt.
     */
    public int compare(final ImageEntry entry1, final ImageEntry entry2) {
        final Evaluator ev1 = evaluator(entry1);
        final Evaluator ev2 = evaluator(entry2);
        if (ev1==null) return (ev2==null) ? 0 : +1;
        if (ev2==null) return                   -1;
        double t1, t2;

        t1 = ev1.uncoveredTime();
        t2 = ev2.uncoveredTime();
        if (t1 > t2) return +1;
        if (t1 < t2) return -1;

        t1 = ev1.timeOffset();
        t2 = ev2.timeOffset();
        if (t1 > t2) return +1;
        if (t1 < t2) return -1;

        t1 = ev1.uncoveredArea();
        t2 = ev2.uncoveredArea();
        if (t1 > t2) return +1;
        if (t1 < t2) return -1;

        t1 = ev1.resolution();
        t2 = ev2.resolution();
        if (t1 > t2) return +1;
        if (t1 < t2) return -1;

        return 0;
    }

    /**
     * Retourne un objet {@link Evaluator} pour l'image spécifiée. Cette méthode est
     * habituellement appelée au début de {@link #compare},  afin d'obtenir une aide
     * pour comparer les images. Si cette méthode n'a pas pu construire un
     * <code>Evaluator</code>, alors elle retourne <code>null</code>.
     */
    protected Evaluator evaluator(final ImageEntry entry) {
        try {
            return new Evaluator(entry);
        } catch (TransformException exception) {
            Utilities.unexpectedException("fr.ird.image.sql", "ImageComparator", "evaluator", exception);
            return null;
        }
    }

    /**
     * Evalue la qualité de la couverture d'une image par rapport à ce qui a été
     * demandée. En général, deux instances de cette classe seront construites à
     * l'intérieur de la méthode {@link ImageComparator#compare}.   Les méthodes
     * de <code>Evaluator</code> seront ensuite appelées  (dans un ordre choisit
     * par {@link ImageComparator#compare}) afin de déterminer laquelle des deux
     * images correspond le mieux à ce que l'utilisateur a demandé.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    protected class Evaluator {
        /**
         * Coordonnées spatio-temporelle d'une image. Il s'agit des coordonnées de l'objet
         * {@link ImageEntry} en cours de comparaison.   Ces coordonnées doivent avoir été
         * transformées selon le système de coordonnées {@link #coordinateSystem}.
         */
        private final Envelope source;

        /**
         * Largeur et hauteur de l'image en nombre de pixels, dans l'exe
         * Est-Ouest (<code>width</code>) ou Nord-Sud (<code>height</code>).
         */
        private final int width, height;

        /**
         * Construit un évaluateur pour l'image spécifiée.
         *
         * @param  entry L'image qui sera a évaluer.
         * @throws TransformException si une transformation était
         *         nécessaire et n'a pas pu être effectuée.
         */
        public Evaluator(final ImageEntry entry) throws TransformException {
            Envelope   envelope = entry.getEnvelope();
            CoordinateSystem cs = entry.getCoordinateSystem();
            GridRange     range = entry.getGridGeometry().getGridRange();
            if (!coordinateSystem.equivalents(cs)) {
                if (transformation==null || !transformation.getSourceCS().equivalents(cs)) {
                    transformation = factory.createFromCoordinateSystems(cs, coordinateSystem);
                }
                final MathTransform transform = transformation.getMathTransform();
                if (!transform.isIdentity()) {
                    envelope = CTSUtilities.transform(transform, envelope);
                }
            }
            this.source = envelope;

            int xDim = -1;
            int yDim = -1;
            for (int i=cs.getDimension(); --i>=0;) {
                final AxisOrientation orientation = cs.getAxis(i).orientation.absolute();
                if (orientation.equals(AxisOrientation.EAST  )) xDim = i;
                if (orientation.equals(AxisOrientation.NORTH )) yDim = i;
            }
            width  = (xDim>=0) ? range.getLength(xDim) : 0;
            height = (yDim>=0) ? range.getLength(yDim) : 0;
        }

        /**
         * Retourne une mesure de la correspondance entre la plage de temps couverte par l'image
         * et la plage de temps qui avait été demandée.  Une valeur de 0 indique que la plage de
         * l'image correspond exactement à la plage demandée.  Une valeur supérieure à 0 indique
         * que l'image ne couvre pas toute la plage demandée,   où qu'elle couvre aussi du temps
         * en dehors de la plage demandée.
         */
        public double uncoveredTime() {
            if (tDim<0) return Double.NaN;
            final double srcMin = source.getMinimum(tDim);
            final double srcMax = source.getMaximum(tDim);
            final double dstMin = target.getMinimum(tDim);
            final double dstMax = target.getMaximum(tDim);
            final double lower  = Math.max(srcMin, dstMin);
            final double upper  = Math.min(srcMax, dstMax);
            final double range  = Math.max(0, upper-lower); // Find intersection range.
            return ((dstMax-dstMin) - range) +  // > 0 if image do not cover all requested range.
                   ((srcMax-srcMin) - range);   // > 0 if image cover some part outside requested range.
        }

        /**
         * Retourne une mesure de l'écart entre la date de l'image et la date demandée.
         * Une valeur de 0 indique que l'image est exactement centrée sur la plage de
         * dates demandée. Une valeur supérieure à 0 indique que le centre de l'image
         * est décalée.
         */
        public double timeOffset() {
            if (tDim<0) {
                return Double.NaN;
            }
            return Math.abs(source.getCenter(tDim)-target.getCenter(tDim));
        }

        /**
         * Retourne une mesure de la correspondance entre la région géographique couverte
         * par l'image et la région qui avait été demandée. Une valeur de 0 indique que
         * l'image couvre au moins la totalité de la région demandée, tandis qu'une valeur
         * supérieure à 0 indique que certaines régions ne sont pas couvertes.
         */
        public double uncoveredArea() {
            if (xDim<0 || yDim<0) return Double.NaN;
            return area - getArea(Math.max(source.getMinimum(xDim), target.getMinimum(xDim)),
                                  Math.max(source.getMinimum(yDim), target.getMinimum(yDim)),
                                  Math.min(source.getMaximum(xDim), target.getMaximum(xDim)),
                                  Math.min(source.getMaximum(yDim), target.getMaximum(yDim)));
        }

        /**
         * Retourne une estimation de la superficie occupée par les pixels.
         * Une valeur de 0 signifierait qu'une image à une précision infinie...
         */
        public double resolution() {
            final int num = width*height;
            return (num>0) ? getArea(source)/num : Double.NaN;
        }
    }
}
