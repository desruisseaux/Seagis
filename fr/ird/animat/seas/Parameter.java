/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat.seas;

// J2SE dependencies
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RectangularShape;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.resources.Utilities;

// seagis dependencies
import fr.ird.animat.Observation;
import fr.ird.animat.impl.Animal;

import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.MinimumEvaluator;
import fr.ird.operator.coverage.MaximumEvaluator;
import fr.ird.operator.coverage.AverageEvaluator;
import fr.ird.operator.coverage.GradientEvaluator;


/**
 * Un paramètre environnemental à évaluer.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Parameter extends fr.ird.animat.impl.Parameter {
    /**
     * La région à prospecter par défaut autour de l'animal.   Cette région est un argument
     * obligatoire pour le constructeur de {@link Evaluator}, mais ne sera généralement pas
     * pris en compte puisque notre implémentation de {@link fr.ird.animat.Animal} va utiliser
     * la méthode {@link Evaluator#evaluate(Shape,double[])}. Cette région par défaut intervient
     * toutefois lorsque l'on demande à afficher une image des valeurs évaluées.
     */
    private static final RectangularShape AREA = new Ellipse2D.Double(-1, -1, 2, 2);

    /**
     * L'objet à utiliser pour appliquer des opérations sur les images.
     */
    private static final GridCoverageProcessor PROCESSOR = GridCoverageProcessor.getDefault();

    /**
     * Le poids à donner à ce paramètre.
     */
    final float weight;

    /**
     * Décalage (en millisecondes) entre l'instant présent de la simulation et le paramètre
     * observé par l'animal. Une valeur négative signifiera que l'animal observera par exemple
     * la température qu'il faisait 5 jours auparavent.
     */
    final long timelag;

    /**
     * Nom de la série d'images.
     * Exemples:
     * <ul>
     *   <li>Pompage d'Ekman</li>
     *   <li>SLA (Monde - TP/ERS)</li>
     *   <li>SST (synthèse)</li>
     *   <li>Chlorophylle-a (Réunion)</li>
     * </ul>
     */
    final String series;

    /**
     * Nom de l'opération à appliquer, ou <code>null</code> si aucune.
     * Exemples:
     * <ul>
     *   <li>GradientMagnitude</li>
     * </ul>
     */
    private final String operation;

    /**
     * Nom de l'{@linkplain Evaluator évaluateur} à utiliser, or <code>null</code> si aucun.
     * Exemples:
     * <ul>
     *   <li>Minimum</li>
     *   <li>Maximum</li>
     *   <li>Average</li>
     *   <li>Gradient</li>
     *   <li>Gradient:0.75</li>
     * </ul>
     */
    private final String evaluator;

    /**
     * Les arguments de l'évaluateur.
     */
    private final double[] evaluatorArgs;

    /**
     * <code>true</code> si un avertissement a été émis à propos de {@link #evaluator}.
     */
    private transient boolean evaluatorWarning;

    /**
     * Le nombre de bandes dans les images de ce paramètre.
     */
    private final byte numSampleDimensions;

    /**
     * Construit un paramètre.
     *
     * @param series    Nom de la série d'images.
     * @param operation Nom de l'opération à appliquer, ou <code>null</code> si aucune.
     * @param evaluator Nom de l'{@linkplain Evaluator évaluateur} à utiliser. Ce nom
     *                  peut comprendre des arguments (sous forme de valeurs numériques)
     *                  séparés par ':'.
     * @param weight    Poids à donner à ce paramètre.
     */
    Parameter(String series, String operation, String evaluator, float weight) {
        super(toString(series=series.trim(), operation, evaluator));
        double timelag = 0;
        if (true) {
            /*
             * Prend en compte le décalage temporel
             * (par exemple le nombre "-5" dans "SST (synthèse) à -5j").
             */
            int length = series.length();
            int split = series.lastIndexOf('à');
            if (split>0 && split<length-1 && Character.isSpaceChar(series.charAt(split-1))
                                          && Character.isSpaceChar(series.charAt(split+1)))
            {
                if (Character.toLowerCase(series.charAt(length-1)) == 'j') {
                    length--;
                }
                try {
                    timelag = Double.parseDouble(series.substring(split+1, length));
                    series = series.substring(0, split-1).trim();
                } catch (NumberFormatException exception) {
                }
            }
        }
        this.weight    = weight;
        this.series    = series;
        this.timelag   = Math.round(timelag * 24L*60*60*1000);
        this.operation = operation; // May be null.
        if (evaluator != null) {
            final StringTokenizer tokens = new StringTokenizer(evaluator, ":");
            this.evaluator = tokens.nextToken();
            evaluatorArgs  = new double[tokens.countTokens()];
            for (int i=0; i<evaluatorArgs.length; i++) {
                evaluatorArgs[i] = Double.parseDouble(tokens.nextToken());
            }
            assert !tokens.hasMoreTokens();
        } else {
            this.evaluator     = null;
            this.evaluatorArgs = null;
        }
        numSampleDimensions = getNumSampleDimensions(evaluator);
    }

    /**
     * Retourne le nombre d'éléments valides dans le tableau retourné par la méthode
     * {@link #evaluate evaluate(...)}. Ce nombre sera généralement de 1 ou 3.
     */
    private static byte getNumSampleDimensions(final String evaluator) {
        if (evaluator != null) {
            if (evaluator.equalsIgnoreCase("Minimum")) {
                return 3;
            }
            if (evaluator.equalsIgnoreCase("Maximum")) {
                return 3;
            }
        }
        return 1;
    }

    /**
     * Applique l'{@linkplain Evaluator évaluateur} sur l'image spécifiée. Cette méthode
     * est utilisée par {@link Environment#getCoverage} lorsqu'il construit un objet
     * {@link Coverage} pour un paramètre donné.
     *
     * @param  coverage L'image sur laquelle appliquer l'évaluateur.
     * @return La fonction basée sur l'image, ou <code>coverage</code>
     *         s'il n'y a pas d'évaluateur.
     */
    final Coverage applyEvaluator(GridCoverage coverage) {
        if (operation != null) {
            coverage = PROCESSOR.doOperation(operation, coverage);
        }
        if (evaluator != null) {
            if (evaluator.equalsIgnoreCase("Minimum")) {
                return new MinimumEvaluator(coverage, AREA);
            }
            if (evaluator.equalsIgnoreCase("Maximum")) {
                return new MaximumEvaluator(coverage, AREA);
            }
            if (evaluator.equalsIgnoreCase("Average")) {
                return new AverageEvaluator(coverage, AREA);
            }
            if (evaluator.equalsIgnoreCase("Gradient")) {
                switch (evaluatorArgs.length) {
                    default: return new GradientEvaluator(coverage, AREA, evaluatorArgs[0]);
                    case 0:  return new GradientEvaluator(coverage, AREA);
                }
            }
            if (!evaluatorWarning) {
                Logger.getLogger("fr.ird.animat").warning("Opération non-reconnue: "+evaluator);
                evaluatorWarning = true;
            }
        }
        return coverage;
    }

    /**
     * Retourne la valeur de ce paramètre pour l'animal spécifié. Le tableau retourné peut avoir
     * une longueur de 1 ou 3. Les informations qu'il contient devront obligatoirement être dans
     * l'ordre suivant:
     * <ul>
     *   <li>La valeur du paramètre</li>
     *   <li>La longitude à laquelle cette valeur a été mesurée.</li>
     *   <li>La latitude à laquelle cette valeur a été mesurée.</li>
     * </ul>
     * Les deux derniers éléments peuvent être absents s'ils ne s'appliquent pas. Le nombre
     * d'éléments valides que contiendra le tableau est spécifié par {@link #getNumSampleDimensions}.
     *
     * @param animal L'animal pour lequel obtenir la valeur de ce paramètre.
     * @param coord  La position de cet animal, en degrés de longitude et de latitude.
     * @param perceptionArea La région jusqu'où s'étend la perception de cet animal.
     * @param dest Le tableau dans lequel mémoriser les valeurs de ce paramètre, ou <code>null</code>.
     * @return Le tableau <code>dest</code>, ou un nouveau tableau si <code>dest</code> était nul.
     */
    protected float[] evaluate(final Animal         animal,
                               final CoordinatePoint coord,
                               final Shape  perceptionArea,
                               final float[]          dest)
    {
        final Coverage coverage = animal.getPopulation().getEnvironment().getCoverage(this);
        if (coverage instanceof Evaluator) {
            return ((Evaluator) coverage).evaluate(perceptionArea, dest);
        } else {
            return coverage.evaluate(coord, dest);
        }
    }

    /**
     * Retourne le nombre d'éléments valides dans le tableau retourné par la méthode
     * {@link #evaluate evaluate(...)}. Ce nombre sera généralement de 1 ou 3.
     */
    protected int getNumSampleDimensions() {
        return numSampleDimensions;
    }

    /**
     * Retourne le nom de ce paramètre construit à partir des noms de séries,
     * d'opération et d'évaluateur.
     */
    private static String toString(final String series, final String operation, final String evaluator) {
        final StringBuffer buffer = new StringBuffer();
        if (evaluator != null) {
            buffer.append(evaluator);
            buffer.append(" de \"");
        }
        if (operation != null) {
            buffer.append(operation);
            buffer.append('[');
        }
        buffer.append(series);
        if (operation != null) {
            buffer.append(']');
        }
        if (evaluator != null) {
            buffer.append('"');
        }
        return buffer.toString();
    }
}
