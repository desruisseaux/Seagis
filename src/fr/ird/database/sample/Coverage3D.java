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
package fr.ird.database.sample;

// J2SE
import java.util.Date;
import java.awt.geom.Point2D;

// JAI
import javax.media.jai.PropertySource;

// Geotools
import org.geotools.pt.CoordinatePoint;
import org.geotools.cv.CannotEvaluateException;
import org.geotools.cv.PointOutsideCoverageException;


/**
 * Une couverture spatio-temporelle fournissant des données à des positions d'échantillons.
 * Cette interface permet d'ajouter à la classe {@link fr.ird.database.Coverage3D} de base
 * des fonctionalités propres à un échantillonage représenté par des enregistrements
 * {@link SampleEntry}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see fr.ird.database.Coverage3D
 */
public interface Coverage3D extends PropertySource {
    /**
     * Évalue les valeurs de la couverture pour un échantillon. La région géographique
     * ainsi que la date des données environnementales à utiliser sont déterminées à partir de
     * <code>position.{@link RelativePositionEntry#getCoordinate getCoordinate}(sample)</code> et
     * <code>position.{@link RelativePositionEntry#getTime getTime}(sample)</code>.
     *
     * @param  sample Échantillon pour lequel on veut les paramètres environnementaux.
     * @param  position Position relative à laquelle évaluer les paramètres environnementaux, ou
     *                  <code>null</code> pour les évaluer à la position exacte de l'échantillon.
     * @param  dest Tableau de destination, ou <code>null</code>.
     * @return Les paramètres environnementaux pour l'échantillon spécifié.
     */
    double[] evaluate(SampleEntry sample, RelativePositionEntry position, double[] dest);

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    double[] evaluate(Point2D point, Date time, double[] dest) throws CannotEvaluateException;

    /**
     * Returns a sequence of double values for a given point in the coverage. A value for
     * each sample dimension is included in the sequence.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    double[] evaluate(CoordinatePoint coord, double[] dest) throws CannotEvaluateException;
}
