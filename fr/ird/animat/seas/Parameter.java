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
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RectangularShape;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.cv.SampleDimension;
import org.geotools.pt.CoordinatePoint;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.resources.Utilities;
import org.geotools.util.NumberRange;

// seagis dependencies
import fr.ird.animat.Observation;
import fr.ird.animat.impl.Animal;

import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.MinimumEvaluator;
import fr.ird.operator.coverage.MaximumEvaluator;
import fr.ird.operator.coverage.AverageEvaluator;
import fr.ird.operator.coverage.GradientEvaluator;


/**
 * Un param�tre environnemental � �valuer.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Parameter extends fr.ird.animat.impl.Parameter implements Comparable {
    /**
     * La r�gion � prospecter par d�faut autour de l'animal.   Cette r�gion est un argument
     * obligatoire pour le constructeur de {@link Evaluator}, mais ne sera g�n�ralement pas
     * pris en compte puisque notre impl�mentation de {@link fr.ird.animat.Animal} va utiliser
     * la m�thode {@link Evaluator#evaluate(Shape,double[])}. Cette r�gion par d�faut intervient
     * toutefois lorsque l'on demande � afficher une image des valeurs �valu�es.
     */
    private static final RectangularShape AREA = new Ellipse2D.Double(-1, -1, 2, 2);

    /**
     * L'objet � utiliser pour appliquer des op�rations sur les images.
     */
    private static final GridCoverageProcessor PROCESSOR = GridCoverageProcessor.getDefault();

    /**
     * Ensemble de param�tres d�j� cr��s. Les cl�s et les valeurs sont identiques. Cet ensemble
     * est utilis� afin d'obtenir l'impl�mentation sous-jacente d'un "RMI stub". C'est n�cessaire
     * lorsqu'un param�tre est envoy� vers le client, puis le client le renvoie vers le serveur.
     * Le serveur re�oit le "stub", alors qu'il pourrait disposer de l'impl�mentation elle-m�me.
     */
    private static final Map<fr.ird.animat.Parameter,Parameter> POOL = new HashMap<fr.ird.animat.Parameter,Parameter>();

    /**
     * La prochaine valeur � donner � {@link #rank}.
     */
    private static int nextRank = 0;

    /**
     * Un rang utilis� pour le classement des param�tres.
     * @see #compareTo
     */
    private final int rank;

    /**
     * Le poids � donner � ce param�tre.
     */
    private final float weight;

    /**
     * D�calage (en millisecondes) entre l'instant pr�sent de la simulation et le param�tre
     * observ� par l'animal. Une valeur n�gative signifiera que l'animal observera par exemple
     * la temp�rature qu'il faisait 5 jours auparavent.
     */
    final long timelag;

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
    private final String operation;

    /**
     * Nom de l'{@linkplain Evaluator �valuateur} � utiliser, or <code>null</code> si aucun.
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
     * Les arguments de l'�valuateur.
     */
    private final double[] evaluatorArgs;

    /**
     * <code>true</code> si un avertissement a �t� �mis � propos de {@link #evaluator}.
     */
    private transient boolean evaluatorWarning;

    /**
     * Le nombre de bandes dans les images de ce param�tre.
     */
    private final byte numSampleDimensions;

    /**
     * La plage de valeurs, ou <code>null</code> si elle n'est pas encore connue.
     */
    private NumberRange range;

    /**
     * Construit un param�tre.
     *
     * @param series    Nom de la s�rie d'images.
     * @param operation Nom de l'op�ration � appliquer, ou <code>null</code> si aucune.
     * @param evaluator Nom de l'{@linkplain Evaluator �valuateur} � utiliser. Ce nom
     *                  peut comprendre des arguments (sous forme de valeurs num�riques)
     *                  s�par�s par ':'.
     * @param weight    Poids � donner � ce param�tre.
     */
    Parameter(String series, String operation, String evaluator, float weight) {
        super(toString(series=series.trim(), operation, evaluator));
        double timelag = 0;
        if (true) {
            /*
             * Prend en compte le d�calage temporel
             * (par exemple le nombre "-5" dans "SST (synth�se) � -5j").
             */
            int length = series.length();
            int split = series.lastIndexOf('�');
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
        synchronized (Parameter.class) {
            this.rank = nextRank++;
            POOL.put(this,this);
        }
    }

    /**
     * Retourne l'impl�mentation du param�tre sp�cifi�, ou <code>null</code>
     * s'il n'y en a pas.
     */
    static Parameter getImplementation(final fr.ird.animat.Parameter parameter) {
        if (parameter instanceof Parameter) {
            return (Parameter) parameter;
        }
        synchronized (Parameter.class) {
            return POOL.get(parameter);
        }
    }
    
    /**
     * Compare cet objet avec l'objet sp�cifi� pour l'ordre. Cette comparaison est utilis�e
     * pour classer les objets dans l'ordre dans lesquels ils ont �t� cr��s.
     */
    public int compareTo(final Object object) {
        final Parameter that = (Parameter) object;
        if (this.rank < that.rank) return -1;
        if (this.rank > that.rank) return +1;
        assert equals(object);
        return 0;
    }

    /**
     * Retourne le nom de ce param�tre construit � partir des noms de s�ries,
     * d'op�ration et d'�valuateur.
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

    /**
     * Retourne le poids de ce param�tre dans le choix de la trajectoire de l'{@linkplain Animal
     * animal} sp�cifi�.
     *
     * @param  L'animal pour lequel on veut le poids de ce param�tre.
     * @return Un poids �gal ou sup�rieur � 0.
     */
    public float getWeight(final fr.ird.animat.Animal animal) {
        return weight;
    }

    /**
     * Retourne la plage de valeurs attendue pour ce param�tre, ou <code>null</code>
     * si elle n'est pas connue.
     */
    public NumberRange getRange() {
        return range;
    }

    /**
     * Retourne le nombre d'�l�ments valides dans le tableau retourn� par la m�thode
     * <code>environment.{@link Environment#getCoverage getCoverage}(this)</code>.
     * Ce nombre sera g�n�ralement de 1 ou 3.
     */
    protected int getNumSampleDimensions() {
        return numSampleDimensions;
    }

    /**
     * Retourne le nombre d'�l�ments valides dans le tableau retourn� par la m�thode
     * <code>environment.{@link Environment#getCoverage getCoverage}(this)</code>.
     * Ce nombre sera g�n�ralement de 1 ou 3.
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
     * Applique l'{@linkplain Evaluator �valuateur} sur l'image sp�cifi�e. Cette m�thode
     * est utilis�e par {@link Environment#getCoverage} lorsqu'il construit un objet
     * {@link Coverage} pour un param�tre donn�.
     *
     * @param  coverage L'image sur laquelle appliquer l'�valuateur.
     * @return La fonction bas�e sur l'image, ou <code>coverage</code>
     *         s'il n'y a pas d'�valuateur.
     */
    final Coverage applyEvaluator(GridCoverage coverage) {
        if (coverage == null) {
            return coverage;
        }
        if (operation != null) {
            coverage = PROCESSOR.doOperation(operation, coverage);
        }
        if (true) {
            final SampleDimension[] bands = coverage.geophysics(true).getSampleDimensions();
            final NumberRange candidate = bands[0].getRange();
            if (range == null) {
                range = candidate;
            } else if (!range.contains(candidate)) {
                range = NumberRange.wrap(range.union(candidate));
            }
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
                Logger.getLogger("fr.ird.animat").warning("Op�ration non-reconnue: "+evaluator);
                evaluatorWarning = true;
            }
        }
        return coverage;
    }
}
