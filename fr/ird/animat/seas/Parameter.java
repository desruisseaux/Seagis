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
import org.geotools.resources.Utilities;

// seagis dependencies
import fr.ird.animat.Observation;
import fr.ird.animat.impl.Animal;

import fr.ird.operator.coverage.Evaluator;
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
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 6728135550533618356L;

    /**
     * La région à prospecter par défaut autour de l'animal.   Cette région est un argument
     * obligatoire pour le constructeur de {@link Evaluator}, mais ne sera généralement pas
     * pris en compte puisque notre implémentation de {@link fr.ird.animat.Animal} va utiliser
     * la méthode {@link Evaluator#evaluate(Shape,double[])}. Cette région par défaut intervient
     * toutefois lorsque l'on demande à afficher une image des valeurs évaluées.
     */
    private static final RectangularShape AREA = new Ellipse2D.Double(-1, -1, 2, 2);

    /**
     * Le poids à donner à ce paramètre.
     */
    public final float weight;

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
    public final String series;

    /**
     * Nom de l'opération à appliquer, ou <code>null</code> si aucune.
     * Exemples:
     * <ul>
     *   <li>GradientMagnitude</li>
     * </ul>
     */
    public final String operation;

    /**
     * Nom de l'{@linkplain Evaluator évaluateur} à utiliser.
     * Exemples:
     * <ul>
     *   <li>Maximum</li>
     *   <li>Average</li>
     *   <li>Gradient</li>
     *   <li>Gradient:0.75</li>
     * </ul>
     */
    public final String evaluator;

    /**
     * Les arguments de l'évaluateur.
     */
    private final double[] evaluatorArgs;

    /**
     * <code>true</code> si un avertissement a été émis à propos de {@link #evaluator}.
     */
    private transient boolean evaluatorWarning;

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
        super(toString(series=series.trim(), operation, evaluator=evaluator.trim()));
        final StringTokenizer tokens = new StringTokenizer(evaluator, ":");
        this.weight    = weight;
        this.series    = series;
        this.operation = operation; // May be null.
        this.evaluator = tokens.nextToken();
        evaluatorArgs  = new double[tokens.countTokens()];
        for (int i=0; i<evaluatorArgs.length; i++) {
            evaluatorArgs[i] = Double.parseDouble(tokens.nextToken());
        }
        assert !tokens.hasMoreTokens();
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
    protected Coverage applyEvaluator(final GridCoverage coverage) {
        if (evaluator != null) {
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
        if (evaluator != null) {
            if (evaluator.equalsIgnoreCase("Maximum")) {
                return 3;
            }
        }
        return 1;
    }

    /**
     * Retourne le nom de ce paramètre construit à partir des noms de séries,
     * d'opération et d'évaluateur.
     */
    private static String toString(final String series, final String operation, final String evaluator) {
        final StringBuffer buffer = new StringBuffer(evaluator);
        buffer.append(" de \"");
        if (operation != null) {
            buffer.append(operation);
            buffer.append('[');
        }
        buffer.append(series);
        if (operation != null) {
            buffer.append(']');
        }
        buffer.append('"');
        return buffer.toString();
    }

    /**
     * Compare ce paramètre avec l'objet spécifié.
     */
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (super.equals(object)) {
            final Parameter that = (Parameter) object;
            return Utilities.equals(this.series,    that.series   ) &&
                   Utilities.equals(this.operation, that.operation) &&
                   Utilities.equals(this.evaluator, that.evaluator);
        }
        return false;
    }
}
