/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 * Un param�tre environnemental � �valuer.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Parameter extends fr.ird.animat.Parameter {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -4751187755671520423L;

    /**
     * La r�gion � prospecter par d�faut autour de l'animal.  Cette r�gion est un argument
     * obligatoire pour {@link Evaluator},   mais  ne sera g�n�ralement pas pris en compte
     * puisque notre impl�mentation de {@link fr.ird.animat.Animal} va utiliser la m�thode
     * {@link Evaluator#evaluate(Shape,double[])}. Cette r�gion par d�faut intervient
     * toutefois lorsque l'on demande � afficher une image des valeurs �valu�es.
     */
    private static final RectangularShape AREA = new Ellipse2D.Double(-1, -1, 2, 2);

    /**
     * Nom de la s�rie d'images.
     * Exemples:
     * <ul>
     *   <li>Pompage d'Ekman</li>
     *   <li>SLA (Monde - TP/ERS)</li>
     *   <li>SST (synth�se)</li>
     *   <li>Chlorophylle-a (R�union)</li>
     * </ul>
     */
    final String series;

    /**
     * Nom de l'op�ration � appliquer, ou <code>null</code> si aucune.
     * Exemples:
     * <ul>
     *   <li>GradientMagnitude</li>
     * </ul>
     */
    final String operation;

    /**
     * Nom de l'{@link Evaluator �valuateur} � utiliser.
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
     * Les arguments de l'�valuateur.
     */
    private final double[] evaluatorArgs;

    /**
     * <code>true</code> si un avertissement a �t� �mis � propos de {@link #evaluator}.
     */
    private transient boolean evaluatorWarning;

    /**
     * Construit un param�tre.
     *
     * @param series    Nom de la s�rie d'images.
     * @param operation Nom de l'op�ration � appliquer, ou <code>null</code> si aucune.
     * @param evaluator Nom de l'{@link Evaluator �valuateur} � utiliser.
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
     * Applique l'{@link Evaluator �valuateur} sur l'image sp�cifi�e.
     *
     * @param  coverage L'image sur laquelle appliquer l'�valuateur.
     * @return La fonction bas�e sur l'image, ou <code>coverage</code>
     *         s'il n'y a pas d'�valuateur.
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
                Logger.getLogger("fr.ird.animat").warning("Op�ration non-reconnue: "+evaluator);
                evaluatorWarning = true;
            }
        }
        return coverage;
    }

    /**
     * Retourne le nom de ce param�tre construit � partir des noms de s�ries,
     * d'op�ration et d'�valuateur.
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
     * Compare ce param�tre avec l'objet sp�cifi�.
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
