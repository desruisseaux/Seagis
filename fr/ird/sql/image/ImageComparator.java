/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
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
 * Compare deux entr�es  {@link ImageEntry}  en fonction d'un crit�re arbitraire.
 * Ce comparateur sert � classer un tableau d'images  en fonction de leur int�r�t
 * par rapport � ce qui avait �t� demand�.   L'impl�mentation par d�faut favorise
 * les images dont la plage de temps couvre le mieux la plage demand�e (les dates
 * de d�but et de fin),  et n'examinera la couverture spatiale que si deux images
 * ont une couverture temporelle �quivalente. Cette politique est appropri�e lorsque
 * les images couvrent � peu pr�s la m�me r�gion, et que les dates de ces images est
 * le principal facteur qui varie. Les crit�res de comparaison utilis�s sont:
 *
 * <ul>
 *  <li>Pour chaque image, la quantit� [<i>temps � l'int�rieur de la plage de temps
 *      demand�e</i>]-[<i>temps � l'ext�rieur de la plage de temps demand�</i>] sera
 *      calcul�e. Si une des image � une quantit� plus grande, elle sera choisie.</li>
 *  <li>Sinon, si une image se trouve mieux centr�e sur la plage de temps demand�e, cette
 *      image sera choisie.</li>
 *  <li>Sinon, pour chaque image, l'intersection entre la r�gion de l'image et la r�gion
 *      demand�e sera obtenue, et la superficie de cette intersection calcul�e. Si une
 *      des images obtient une valeur plus grande, cette image sera choisie.</li>
 *  <li>Sinon, la superficie moyenne des pixels des images seront calcul�es. Si une image
 *      a des pixels d'une meilleure r�solution (couvrant une surface plus petite), cette
 *      image sera choisie.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ImageComparator implements Comparator<ImageEntry> {
    /**
     * Object � utiliser pour construire des transformations de coordonn�es.
     */
    private final CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();

    /**
     * Syst�me de coordonn�es dans lequel faire les comparaisons. Il s'agit
     * du syst�me de coordonn�es de la table {@link ImageTable} d'origine.
     */
    private final CoordinateSystem coordinateSystem;

    /**
     * Transformation permettant de passer du syst�me de coordonn�es
     * d'un objet {@link ImageEntry} vers le syst�me de coordonn�es
     * de {@link ImageTable} (c'est-�-dire {@link #coordinateSystem}).
     * Cette transformation est conserv�e dans une cache interne afin
     * d'�viter de construire cet objet trop fr�quement.
     */
    private transient CoordinateTransformation transformation;

    /**
     * Ellispoide � utiliser pour calculer les distances orthodromiques.
     */
    private final Ellipsoid ellipsoid;

    /**
     * Coordonn�es spatio-temporelles demand�es. Ils s'agit des coordonn�es
     * qui avait �t� sp�cifi�e � la table {@link ImageTable}. Cette envelope
     * est exprim�e selon le syst�me de coordonn�es {@link #coordinateSystem}.
     * Cette enveloppe n'est pas clon�e. Elle ne doit donc pas �tre modifi�e.
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
     * Construit un comparateur pour les images de la table sp�cifi�e.
     *
     * @param table Table qui a produit les images qui seront � comparer.
     */
    public ImageComparator(final ImageTable table) {
        this(table.getCoordinateSystem(), table.getEnvelope());
    }

    /**
     * Construit un comparateur avec les plages spatio-temporelles sp�cifi�es.
     *
     * @param cs Syst�me de coordonn�es dans lequel comparer les images.
     * @param envelope Coordonn�es spatio-temporelle de la r�gion qui avait �t� demand�e.
     *        Ces coordonn�es doivent �tre exprim�es selon le syst�me de coordonn�es
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
     * Retourne une estimation de la superficie occup�e par
     * la composante horizontale de l'envelope sp�cifi�e.
     */
    private double getArea(final Envelope envelope) {
        if (xDim<0 || yDim<0) return Double.NaN;
        return getArea(envelope.getMinimum(xDim), envelope.getMinimum(yDim),
                       envelope.getMaximum(xDim), envelope.getMaximum(yDim));
    }

    /**
     * Retourne une estimation de la superficie occup�e par
     * un rectangle d�limit�e par les coordonn�es sp�cifi�es.
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
     * Compare deux objets {@link ImageEntry}. Les classes d�riv�es peuvent
     * red�finir cette m�thode pour d�finir un autre crit�re de comparaison
     * que les crit�res par d�faut.
     *
     * @return +1 si l'image <code>entry1</code> repr�sente le plus grand int�r�t,
     *         -1 si l'image <code>entry2</code> repr�sente le plus grand int�r�t, ou
     *          0 si les deux images repr�sentent le m�me int�r�t.
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
     * Retourne un objet {@link Evaluator} pour l'image sp�cifi�e. Cette m�thode est
     * habituellement appel�e au d�but de {@link #compare},  afin d'obtenir une aide
     * pour comparer les images. Si cette m�thode n'a pas pu construire un
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
     * Evalue la qualit� de la couverture d'une image par rapport � ce qui a �t�
     * demand�e. En g�n�ral, deux instances de cette classe seront construites �
     * l'int�rieur de la m�thode {@link ImageComparator#compare}.   Les m�thodes
     * de <code>Evaluator</code> seront ensuite appel�es  (dans un ordre choisit
     * par {@link ImageComparator#compare}) afin de d�terminer laquelle des deux
     * images correspond le mieux � ce que l'utilisateur a demand�.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    protected class Evaluator {
        /**
         * Coordonn�es spatio-temporelle d'une image. Il s'agit des coordonn�es de l'objet
         * {@link ImageEntry} en cours de comparaison.   Ces coordonn�es doivent avoir �t�
         * transform�es selon le syst�me de coordonn�es {@link #coordinateSystem}.
         */
        private final Envelope source;

        /**
         * Largeur et hauteur de l'image en nombre de pixels, dans l'exe
         * Est-Ouest (<code>width</code>) ou Nord-Sud (<code>height</code>).
         */
        private final int width, height;

        /**
         * Construit un �valuateur pour l'image sp�cifi�e.
         *
         * @param  entry L'image qui sera a �valuer.
         * @throws TransformException si une transformation �tait
         *         n�cessaire et n'a pas pu �tre effectu�e.
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
         * et la plage de temps qui avait �t� demand�e.  Une valeur de 0 indique que la plage de
         * l'image correspond exactement � la plage demand�e.  Une valeur sup�rieure � 0 indique
         * que l'image ne couvre pas toute la plage demand�e,   o� qu'elle couvre aussi du temps
         * en dehors de la plage demand�e.
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
         * Retourne une mesure de l'�cart entre la date de l'image et la date demand�e.
         * Une valeur de 0 indique que l'image est exactement centr�e sur la plage de
         * dates demand�e. Une valeur sup�rieure � 0 indique que le centre de l'image
         * est d�cal�e.
         */
        public double timeOffset() {
            if (tDim<0) {
                return Double.NaN;
            }
            return Math.abs(source.getCenter(tDim)-target.getCenter(tDim));
        }

        /**
         * Retourne une mesure de la correspondance entre la r�gion g�ographique couverte
         * par l'image et la r�gion qui avait �t� demand�e. Une valeur de 0 indique que
         * l'image couvre au moins la totalit� de la r�gion demand�e, tandis qu'une valeur
         * sup�rieure � 0 indique que certaines r�gions ne sont pas couvertes.
         */
        public double uncoveredArea() {
            if (xDim<0 || yDim<0) return Double.NaN;
            return area - getArea(Math.max(source.getMinimum(xDim), target.getMinimum(xDim)),
                                  Math.max(source.getMinimum(yDim), target.getMinimum(yDim)),
                                  Math.min(source.getMaximum(xDim), target.getMaximum(xDim)),
                                  Math.min(source.getMaximum(yDim), target.getMaximum(yDim)));
        }

        /**
         * Retourne une estimation de la superficie occup�e par les pixels.
         * Une valeur de 0 signifierait qu'une image � une pr�cision infinie...
         */
        public double resolution() {
            final int num = width*height;
            return (num>0) ? getArea(source)/num : Double.NaN;
        }
    }
}
