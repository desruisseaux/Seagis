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
package fr.ird.image.work;

// Images
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.RandomIterFactory;

// G�om�trie
import java.awt.Shape;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Entr�s/sorties et formattage
import java.io.File;
import java.io.Writer;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

// Resources
import org.geotools.gc.GridCoverage;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XMath;

// Seagis
import fr.ird.sql.image.ImageEntry;


/**
 * Variance et semi-variance d'une image.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class SemiVariance extends Result {
    /**
     * Num�ro de s�rie (pour compatibilit� entre diff�rentes versions).
     */
    private static final long serialVersionUID = -7829114806884220976L;

    /**
     * Nombre de valeurs qui ont �t� utilis�s pour
     * le calcul de la moyenne et l'�cart-type.
     */
    private long count;

    /**
     * Nombre de valeurs qui ont �t� utilis�s pour le
     * calcul de chaque �l�ment du semi-variogramme.
     */
    private final long[] count1D, count2D;

    /**
     * Somme des valeurs des pixels. Cette somme servira
     * � calculer la moyenne et l'�cart type.
     */
    private double sum;

    /**
     * Somme des valeurs au carr�s des pixels. Cette somme
     * servira � calculer la valeur RMS et l'�cart type.
     */
    private double sumSq;

    /**
     * Somme des carr�s des diff�rences de valeurs entre deux pixels.
     * Ces donn�es divis�es par le nombre de points constitueront les
     * semi-variances de l'image.
     */
    private final double[] sumSq1D, sumSq2D;

    /**
     * La distance entre les deux pixels qui ont servit � calculer la
     * semi-variance. Ces donn�es divis�es par le nombre de valeurs
     * constitueront les ordonn�es du semi-variogramme.
     */
    private final double[] distance1D;

    /**
     * La distance entre les deux pixels qui ont servit � calculer la
     * semi-variance, d�compos�e selon la longitude et la latitude.
     */
    private final double[] distance2Dx, distance2Dy;

    /**
     * Intervalle (en m�tres) � utiliser pour la distance, et
     * rayon en m�tres autour duquel calculer la semi-variance.
     */
    private final double interval, radius;

    /**
     * Nombre de ligne � avoir �t� pris en compte dans l'image. Ce nombre
     * doit �tre �gal � la hauteur de l'image. Si ce n'est pas le cas, �a
     * signifie que le calcul n'avait pas �t� termin�.
     */
    private int lineCount;

    /**
     * Construit un objet {@link SemiVariance} initialement vide.
     *
     * @param interval Intervalle (en m�tres) entre chaque points du semi-variogramme.
     * @param radius   Rayon (en m�tres) autour duquel calculer la semi-variance.
     */
    public SemiVariance(final double interval, final double radius) {
        this.interval    = interval;
        this.radius      = radius;
        this.count1D     = new long  [(int)Math.ceil(radius/interval)];
        this.count2D     = new long  [count1D.length*count1D.length];
        this.sumSq1D     = new double[count1D.length];
        this.sumSq2D     = new double[count2D.length];
        this.distance1D  = new double[count1D.length];
        this.distance2Dx = new double[count1D.length];
        this.distance2Dy = new double[count1D.length];
    }

    /**
     * Enregistre les r�sultats des statistiques dans un fichier texte.
     * Ce fichier pourra �tre ouvert avec un tableau comme Excel.
     *
     * @param  out Flot vers o� �crire les r�sultats.
     * @throws IOException si l'�criture a �chou�e.
     */
    public void write(final Writer out) throws IOException {
        final int    metres2output = 1000; // Facteur par lequel diviser les m�tres.
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final NumberFormat  format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(12);
        format.setGroupingUsed(false);
        if (format instanceof DecimalFormat) {
            final DecimalFormat        decimal = (DecimalFormat) format;
            final DecimalFormatSymbols symbols = decimal.getDecimalFormatSymbols();
            symbols.setNaN("#N/A");
            decimal.setDecimalFormatSymbols(symbols);
        }
        /*
         * Ecrit d'abord des statistiques g�n�rales.
         */
        out.write("Nb. lignes:\t"); out.write(format.format(            lineCount));  out.write(lineSeparator);
        out.write("Compte:\t");     out.write(format.format(                count));  out.write(lineSeparator);
        out.write("Moyenne:\t");    out.write(format.format(          sum  /count));  out.write(lineSeparator);
        out.write("RMS:\t");        out.write(format.format(Math.sqrt(sumSq/count))); out.write(lineSeparator);
        out.write("Ecart type:\t"); out.write(format.format(Math.sqrt((sumSq-sum*sum/count)/(count-1))));
        out.write(lineSeparator);
        out.write(lineSeparator);
        /*
         * Ecrit la semi-variance 1D.
         */
        if (count1D != null) {
            out.write("Distance (km)\tSemi-variance\tCompte");
            out.write(lineSeparator);
            for (int i=0; i<count1D.length; i++) {
                final long c = count1D[i];
                out.write(format.format((distance1D[i]/c) / metres2output));
                out.write('\t');
                out.write(format.format(Math.sqrt(sumSq1D[i]/c)));
                out.write('\t');
                out.write(format.format(c));
                out.write(lineSeparator);
            }
            out.write(lineSeparator);
        }
        /*
         * Ecrit la semi-variance 2D.
         */
        if (count2D != null) {
            out.write("Semi-variance 2D");
            out.write(lineSeparator);
            /*
             * Ecrit d'abord les distance en longitudes.
             */
            for (int i=0; i<distance2Dx.length; i++) {
                long c=0;
                for (int j=i; j<count2D.length; j+=distance2Dx.length) {
                    c += count2D[j];
                }
                out.write('\t');
                out.write(format.format((distance2Dx[i]/c)/metres2output));
            }
            out.write(lineSeparator);
            /*
             * Ecrit les distance en latitudes et les semi-variances.
             */
            for (int j=0; j<distance2Dy.length; j++) {
                long c=0;
                final int lower = j*distance2Dx.length;
                final int upper = lower + distance2Dx.length;
                for (int i=lower; i<upper; i++) c+=count2D[i];
                out.write(format.format((distance2Dy[j]/c)/metres2output));
                for (int i=lower; i<upper; i++) {
                    out.write('\t');
                    out.write(format.format(Math.sqrt(sumSq2D[i]/count2D[i])));
                }
                out.write(lineSeparator);
            }
            out.write(lineSeparator);
        }
    }

    /**
     * Calcule la semi-variance d'une s�rie d'images.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Worker extends SimpleWorker {
        /**
         * Intervalle (en m�tres) � utiliser pour la distance, et
         * rayon en m�tres autour duquel calculer la semi-variance.
         */
        private final double interval, radius;

        /**
         * Coordonn�es de la r�gion g�ographique � prendre en compte, ou
         * <code>null</code> pour utiliser toute l'image. Ces coordonn�es
         * sont g�n�ralement exprim�es en degr�s de longitude et de latitude.
         */
        private static final Shape area = null;

        /**
         * Num�ro de la bande � d�coder dans les images.
         */
        private static final int band = 0;

        /**
         * Construit un objet par d�faut.
         *
         * @param interval Intervalle (en m�tres) entre chaque points du semi-variogramme.
         * @param radius   Rayon (en m�tres) autour duquel calculer la semi-variance.
         */
        public Worker(final double interval, final double radius) {
            super("Semi-variance");
            this.interval = interval;
            this.radius   = radius;
        }

        /**
         * Calcule les statistiques de l'image sp�cifi�e, et ajoute les
         * statistiques obtenues � celles qui ont �t� calcul�es pr�c�demment.
         */
        protected Result run(final ImageEntry imageEntry, Result lastResult) throws IOException {
            final SemiVariance result;
            if (lastResult instanceof SemiVariance) {
                result = (SemiVariance) lastResult;
                if (!result.contains(imageEntry) || result.interval!=interval || result.radius!=radius) {
                    throw new IllegalArgumentException("R�sultat incompatible.");
                }
                if (result.lineCount == imageEntry.getGridGeometry().getGridRange().getLength(1)) {
                    return null; // Le r�sultat est d�j� complet.
                }
            } else {
                result = new SemiVariance(interval, radius);
                result.add(imageEntry);
            }
            final GridCoverage coverage = getGridCoverage(imageEntry).geophysics(true);
            /*
             * Les calculs seront faits dans des tableaux locaux,  qu'on ajoutera
             * plus tard aux tableaux de la classe. En proc�dant ainsi, on r�duit
             * les risques d'erreurs d'arrondissements et on rend cette m�thode
             * utilisable dans un environnement multi-threads.
             *
             * ATTENTIONS AUX DEBORDEMENTS: Pour compter le nombre de pixels,  le
             * type 'int' d�borde pour les images de 2000 x 2000 avec une fen�tre
             * de plus de 680 pixels.
             */
            double sum   = 0;
            double sumSq = 0;
            int    count = 0;
            final long[]      count1D  = new long  [result.   count1D .length];
            final long[]      count2D  = new long  [result.   count2D .length];
            final double[]    sumSq1D  = new double[result.   sumSq1D .length];
            final double[]    sumSq2D  = new double[result.   sumSq2D .length];
            final double[] distance1D  = new double[result.distance1D .length];
            final double[] distance2Dx = new double[result.distance2Dx.length];
            final double[] distance2Dy = new double[result.distance2Dy.length];

            final RenderedImage image = coverage.getRenderedImage();
            final Rectangle    bounds = getBounds(coverage, area);
            final int          width  = bounds.width;
            final int          height = bounds.height;
            final int          xmin   = bounds.x;
            final int          ymin   = bounds.y;
            final int          xmax   = width  + xmin;
            final int          ymax   = height + ymin;
            final float[]      data   = image.getData(bounds).getSamples(xmin, ymin, width, height, band, (float[])null);
            final float progressScale = 100f/(ymax-ymin);
            final RandomIter iterator = RandomIterFactory.create(image, bounds);
            // Je n'arrive pas � utiliser 'RectIter' sans me prendre un 'ArrayIndexOutOfBoundsException'
            // sur la gueule! 'RandomIter' en revanche semble fonctionner correctement, mais sera plus lent.

            final Point2D.Double    coordinate = new Point2D.Double();
            final CoordinateSystem coordSystem;
            try {
                coordSystem = CTSUtilities.getCoordinateSystem2D(coverage.getCoordinateSystem());
            } catch (TransformException exception) {
                throw new IllegalArgumentException(exception.getLocalizedMessage());
            }
            if (coordSystem instanceof GeographicCoordinateSystem) {
                /*
                 * Calcul des semi-variances dans le cas o� le syst�me de coordonn�es est un
                 * ellipso�de. Cette impl�mentation utilise un code sp�cial qui calculera
                 * les distances (au moins de fa�on approximative) plus rapidement que si
                 * on appelle {@link CoordinateSystem#distance} � r�p�tition.
                 */
                final int                 xCenter = (xmin+xmax)/2;
                final Ellipsoid         ellipsoid = ((GeographicCoordinateSystem) coordSystem).getHorizontalDatum().getEllipsoid();
                final double         searchRadius = interval*count1D.length; // En m�tres
                final double  semiMajorAxisLength = ellipsoid.getSemiMajorAxis();
                final double  semiMinorAxisLength = ellipsoid.getSemiMinorAxis();
                final AffineTransform referencing = AffineTransform.getScaleInstance(Math.PI/180, Math.PI/180); // Degr�s --> radians.
                referencing.concatenate((AffineTransform)coverage.getGridGeometry().getGridToCoordinateSystem2D());
                if (referencing.getShearX()!=0 || referencing.getShearY()!=0) {
                    throw new UnsupportedOperationException(String.valueOf(referencing)); // TODO
                }
                double[] precomputedDistance1D = new double[0]; // Will be expanded when necessary
                int   [] precomputedIndex1D    = new int   [0]; // Will be expanded when necessary
                int   [] precomputedIndex2D    = precomputedIndex1D;
                /*
                 * D�marre la boucle pour examiner tous les pixels de l'image. On balayera d'abord
                 * les colonnes, et ensuite les lignes. Au d�but de chaque ligne, on calculera une
                 * fois pour toutes les distances entres les pixels. On profite du fait que ce calcul
                 * d�pend de la latitude o� on se trouve, mais pas de la longitude.
                 */
                for (int y=ymin+result.lineCount; y<ymax; y++) {
                    progress(progressScale*(y-ymin));
                          int searchWidth;  // To be computed below
                    final int searchHeight; // To be computed below
                    if (true) {
                        /*
                         * Calcule les coordonn�es g�ographique du point centr�
                         * horizontalement et � la ligne <var>y</var> verticalement.
                         */
                        coordinate.x = xCenter;
                        coordinate.y = y;
                        referencing.transform(coordinate, coordinate);
                        final double coordX  = coordinate.x;
                        final double coordY  = coordinate.y;
                        final double sin_y   = Math.sin(coordY);
                        final double cos_y   = Math.cos(coordY);
                        final double inverseApparentRadius = XMath.hypot(sin_y/semiMajorAxisLength, cos_y/semiMinorAxisLength);
                        /*
                         * Calcule la largeur et la hauteur de la r�gion qu'il faudra lire,
                         * en pixels. Cette largeur et hauteur devrait �tre constante tant
                         * que la latitude ne change pas.
                         */
                        try {
                            coordinate.y = searchRadius*inverseApparentRadius; // Hauteur en radians.
                            coordinate.x = coordinate.y/cos_y;                 // Largeur en radians.
                            XAffineTransform.inverseDeltaTransform(referencing, coordinate, coordinate);
                        } catch (NoninvertibleTransformException exception) {
                            // Should not happen
                            exceptionOccurred("run", exception);
                            coordinate.x=bounds.width;
                            coordinate.y=bounds.height;
                        }
                        searchWidth  = Math.min(Math.abs((int)Math.ceil(coordinate.x))+1, xmax-xmin);
                        searchHeight = Math.min(Math.abs((int)Math.ceil(coordinate.y))+1, ymax-y   );
                        if (!(searchWidth>0 && searchHeight>0)) {
                            continue;
                        }
                        /*
                         * Calcule une fois pour toutes les distances qui correspondent aux
                         * positions relatives des pixels.  On ne calcule ces distances que
                         * pour le premier quadrant. Les autres quadrants sont sym�triques.
                         */
                        int indice = searchWidth * searchHeight;
                        if (indice > precomputedDistance1D.length) precomputedDistance1D = new double[indice];
                        if (indice >    precomputedIndex1D.length)    precomputedIndex1D = new int   [indice];
                        if (indice >    precomputedIndex2D.length)    precomputedIndex2D = new int   [indice];
                        indice = 0;
                        for (int j=0; j<searchHeight; j++) {
                            for (int i=0; i<searchWidth; i++) {
                                coordinate.x = i+xCenter;
                                coordinate.y = j+y;
                                referencing.transform(coordinate, coordinate);
                                final double distance = Math.acos(sin_y*Math.sin(coordinate.y) +
                                                                  cos_y*Math.cos(coordinate.y) *
                                                                  Math.cos(coordinate.x-coordX))/inverseApparentRadius;
                                precomputedDistance1D[indice]=distance; // Must be store before 'index2D' computation.
                                assert(i             <= indice);        // Make sure distance is already computed.
                                assert(j*searchWidth <= indice);        // Make sure distance is already computed.
                                final int index1D  = (int)(distance/interval);
                                final int index2Dx = (int)(precomputedDistance1D[i            ]/interval); // Use distance at dy=0.
                                final int index2Dy = (int)(precomputedDistance1D[j*searchWidth]/interval); // Use distance at dx=0.
                                final int index2D  = (index2Dx<distance2Dx.length) ? index2Dx + index2Dy*distance2Dx.length : -1;
                                precomputedIndex1D[indice] = index1D;
                                precomputedIndex2D[indice] = index2D;
                                indice++;
                            }
                        }
                        assert indice == searchWidth*searchHeight;
                    }
                    /*
                     * Examine maintenant toutes les colonnes de la ligne courante.
                     * Pour chaque pixel qui a une valeur autre que <code>NaN</code>,
                     * on calculera le variogramme en utilisant les pixels de son
                     * voisinage.
                     */
                    for (int x=xmin; x<xmax; x++) {
                        if (isStopped()) {
                            /*
                             * Si l'utilisateur a demand� � interrompre le calcul,
                             * on arr�te maintenant et on retourne le r�sultat tel
                             * qu'il est. Le r�sultat ne sera pas corrompu puisque
                             * le r�sultat de cette boucle n'�tait ajout� qu'une
                             * fois compl�t� (et qu'on ne l'ajoutera pas ici).
                             */
                            return result;
                        }
                        final double value = iterator.getSampleDouble(x, y, band);
                        if (!Double.isNaN(value)) {
                            count++;
                            sum   += value;
                            sumSq += (value*value);
                            /*
                             * Examine maintenant les pixels dans la r�gion
                             * 'searchArea' autour du pixel central.
                             */
                            int indice = xmax-x;
                            if (indice==0) continue;
                            if (searchWidth>indice) searchWidth=indice;
                            indice=0;
                            final int stopX = x + searchWidth;
                            final int stopY = y + searchHeight;
                            for (int ay=y; ay<stopY; ay++) {
                                assert((indice % searchWidth)==0);
                                final int        baseIndice = indice;
                                final double distanceAlongY = precomputedDistance1D[indice];
                                      int           pixelXY = (ay-ymin)*width + (x-xmin);
                                for (int ax=x; ax<stopX; ax++) {
                                    final float compare = data[pixelXY++];
                                    if (!Float.isNaN(compare)) {
                                        assert(compare==iterator.getSampleFloat(ax, ay, band));
                                        final double delta2 = (compare-value)*(compare-value);
                                        int index = precomputedIndex1D[indice];
                                        if (index>=0 && index<count1D.length) {
                                            count1D   [index]++;
                                            sumSq1D   [index] += delta2;
                                            distance1D[index] += precomputedDistance1D[indice];
                                        }
                                        index = precomputedIndex2D[indice];
                                        if (index>=0 && index<count2D.length) {
                                            count2D    [index]++;
                                            sumSq2D    [index] += delta2;
                                            assert(indice-baseIndice < searchWidth);
                                            final double distanceAlongX = precomputedDistance1D[indice-baseIndice];
                                            distance2Dx[index % distance2Dx.length] += distanceAlongX;
                                            distance2Dy[index / distance2Dx.length] += distanceAlongY;
                                        }
                                    }
                                    indice++;
                                }
                            }
                            assert indice == searchWidth*searchHeight;
                        }
                    }
                    /*
                     * Ajoute maintenant le r�sultat du calcul aux statistiques de l'ensemble
                     * de la classe. On le fait apr�s chaque ligne plut�t que d'attendre la fin
                     * de la m�thode, afin de ne pas �tre oblig� de tout recommencer si on doit
                     * interrompre le calcul.
                     */
                    synchronized (result) {
                        result.sum   += sum;
                        result.sumSq += sumSq;
                        result.count += count;
                        for (int i=0; i<count1D.length; i++) {
                            result.   count1D [i] +=    count1D [i];
                            result.   sumSq1D [i] +=    sumSq1D [i];
                            result.distance1D [i] += distance1D [i];
                            result.distance2Dx[i] += distance2Dx[i];
                            result.distance2Dy[i] += distance2Dy[i];
                        }
                        for (int i=0; i<count2D.length; i++) {
                            result.count2D[i] += count2D[i];
                            result.sumSq2D[i] += sumSq2D[i];
                        }
                        result.lineCount++;
                    }
                    sum   = 0;
                    sumSq = 0;
                    count = 0;
                    Arrays.fill(   count1D,  0);
                    Arrays.fill(   count2D,  0);
                    Arrays.fill(   sumSq1D,  0);
                    Arrays.fill(   sumSq2D,  0);
                    Arrays.fill(distance1D,  0);
                    Arrays.fill(distance2Dx, 0);
                    Arrays.fill(distance2Dy, 0);
                    /*
                     * Enregistre l'�tat actuel des travaux, pour ne pas �tre oblig�
                     * de tout recommenc� en cas d'interruption.
                     */
                    if ((y & 0x3F)==0) {
                        save(imageEntry, result);
                    }
                }
            } else {
                /*
                 * Calcule du variogramme dans le cas plus g�n�ral o� on se
                 * fie � {@link CoordinateSystem} pour calculer les distances.
                 */
                throw new UnsupportedOperationException(String.valueOf(coordSystem)); // TODO
            }
            return result;
        }

        /**
         * Calcule la semi-variance de toutes les images provenant d'une s�rie.
         * Les arguments reconnus sont:
         *
         * <ul>
         *   <li><code>-mail</code>  informe des progr�s par courriel.</li>
         *   <li><code>-print</code> informe des progr�s sur le p�riph�rique de sortie standard.</li>
         *   <li><code>-show</code>  affiche un paneau indiquant qu'un calcul est en cours. Ce paneau
         *                           contient un bouton permettant l'arr�t du calcul � tout moment.</li>
         * </ul>
         */
        public static void main(final String[] args) {
            final Worker worker = new Worker(1852, 500000); // 0-500 km avec une r�solution de 1 nautique.
            try {
                worker.setDestination(new File("\\\\ADAGIO\\Analyses\\Semi-variances"));
            //  worker.setImages("(aucune)");
                worker.setup(args);
                worker.run();
                System.exit(0);
            } catch (Throwable error) {
                worker.exceptionOccurred("main", error);
                System.exit(1);
            }
        }
    }
}
