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
import java.awt.geom.Ellipse2D;
import java.awt.geom.RectangularShape;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;

// seagis dependencies
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
final class Parameter extends fr.ird.animat.Parameter {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -4751187755671520423L;

    /**
     * La région à prospecter par défaut autour de l'animal.  Cette région est un argument
     * obligatoire pour {@link Evaluator},   mais  ne sera généralement pas pris en compte
     * puisque notre implémentation de {@link fr.ird.animat.Animal} va utiliser la méthode
     * {@link Evaluator#evaluate(Shape,double[])}. Cette région par défaut intervient
     * toutefois lorsque l'on demande à afficher une image des valeurs évaluées.
     */
    private static final RectangularShape AREA = new Ellipse2D.Double(-1, -1, 2, 2);

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
    final String operation;

    /**
     * Nom de l'{@link Evaluator évaluateur} à utiliser.
     * Exemples:
     * <ul>
     *   <li>Maximum</li>
     *   <li>Average</li>
     *   <li>Gradient</li>
     *   <li>Gradient:0.75</li>
     * </ul>
     */
    final String evaluator;

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
     * @param evaluator Nom de l'{@link Evaluator évaluateur} à utiliser.
     */
    Parameter(String series, String operation, String evaluator) {
        super(toString(series=series.trim(), operation, evaluator=evaluator.trim()));
        final StringTokenizer tokens = new StringTokenizer(evaluator, ":");
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
     * Applique l'{@link Evaluator évaluateur} sur l'image spécifiée.
     *
     * @param  coverage L'image sur laquelle appliquer l'évaluateur.
     * @return La fonction basée sur l'image, ou <code>coverage</code>
     *         s'il n'y a pas d'évaluateur.
     */
    public Coverage applyEvaluator(final GridCoverage coverage) {
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
     * Retourne le nom de ce paramètre construit à partir des noms de séries,
     * d'opération et d'évaluateur.
     */
    private static String toString(final String series, final String operation, final String evaluator) {
        final StringBuffer buffer = new StringBuffer(evaluator);
        buffer.append(" de ");
        if (operation != null) {
            buffer.append(operation);
            buffer.append('[');
        }
        buffer.append(series);
        if (operation != null) {
            buffer.append(']');
        }
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
